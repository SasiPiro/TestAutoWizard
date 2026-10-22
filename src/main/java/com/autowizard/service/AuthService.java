package com.autowizard.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.autowizard.dto.AuthResponse;
import com.autowizard.dto.LoginRequest;
import com.autowizard.dto.RegisterRequest;
import com.autowizard.exception.ConflictException;
import com.autowizard.model.User;
import com.autowizard.repository.UserRepository;
import com.autowizard.security.UserPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            log.info("[AUTH_REGISTER] outcome=rejected email={} reason=duplicate_email", request.email());
            throw new ConflictException("Email already registered");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());

        userRepository.save(user);
        log.info("[AUTH_REGISTER] outcome=success email={} userId={}", user.getEmail(), user.getId());

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token);
    }

    /**
     * Delegates the check to Spring Security, which compares the password in constant time
     * and reports an unknown email and a wrong password with the same exception. No
     * transaction: BCrypt is slow on purpose and would hold a connection meanwhile.
     */
    public AuthResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (AuthenticationException e) {
            // Logged here because only here the attempted email is known; the exception
            // handler turns it into a generic 401. WARN: repeated failures are worth a look.
            log.warn("[AUTH_LOGIN] outcome=failure email={} reason={}", request.email(), e.getClass().getSimpleName());
            throw e;
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtService.generateToken(principal.getId(), principal.getEmail());
        log.info("[AUTH_LOGIN] outcome=success email={} userId={}", principal.getEmail(), principal.getId());
        return new AuthResponse(token);
    }
}
