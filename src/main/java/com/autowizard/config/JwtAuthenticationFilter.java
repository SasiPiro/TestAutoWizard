package com.autowizard.config;

import com.autowizard.security.UserPrincipal;
import com.autowizard.service.JwtService;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Authenticates requests carrying "Authorization: Bearer &lt;jwt&gt;".
 *
 * <p>
 * The token is parsed and verified once (signature + expiration). A request without a
 * valid token simply continues unauthenticated: public endpoints work, protected ones
 * get 401 from the authentication entry point configured in {@link SecurityConfig}.
 * The filter never writes the response itself and never logs the header or the token.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            Optional<String> email = jwtService.parse(header.substring(BEARER_PREFIX.length()))
                    .map(Claims::getSubject);
            if (email.isEmpty()) {
                log.debug("[JWT_REJECTED] path={}", request.getRequestURI());
            } else {
                authenticate(email.get(), request);
            }
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String email, HttpServletRequest request) {
        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUsername(email);
        } catch (UsernameNotFoundException e) {
            // Valid signature but the user no longer exists: treat as unauthenticated
            log.debug("[JWT_REJECTED] path={} reason=user_not_found", request.getRequestURI());
            return;
        }
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        if (userDetails instanceof UserPrincipal principal) {
            request.setAttribute("userId", principal.getId());
        }
    }
}
