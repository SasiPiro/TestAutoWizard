package com.autowizard.identity.internal;

import com.autowizard.identity.UserPrincipal;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Authenticates requests carrying "Authorization: Bearer &lt;jwt&gt;".
 *
 * <p>
 * The token is parsed and verified once (signature + expiration). A request without a
 * valid token simply continues unauthenticated: public endpoints work, protected ones
 * get 401 from the authentication entry point configured in {@link SecurityConfig}.
 * The principal is built from the claims (user id and email), not loaded from the database.
 * The filter never writes the response itself and never logs the header or the token.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final List<GrantedAuthority> USER_AUTHORITIES = List.of(new SimpleGrantedAuthority("ROLE_USER"));

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            Optional<Claims> claims = jwtService.parse(header.substring(BEARER_PREFIX.length()));
            Optional<Long> userId = claims.flatMap(JwtService::userIdOf);
            if (userId.isEmpty()) {
                log.debug("[JWT_REJECTED] path={}", request.getRequestURI());
            } else {
                authenticate(new UserPrincipal(userId.get(), claims.get().getSubject(), null, USER_AUTHORITIES), request);
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * The signature proves the claims were issued by this application: no database lookup
     * per request. The trade-off: a token stays valid until it expires.
     */
    private static void authenticate(UserPrincipal principal, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
