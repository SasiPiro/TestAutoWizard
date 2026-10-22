package com.autowizard.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.autowizard.dto.AuthRequest;
import com.autowizard.dto.AuthResponse;
import com.autowizard.exception.ConflictException;
import com.autowizard.model.User;
import com.autowizard.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(AuthRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already registered");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        userRepository.save(user);

        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token);
    }

    /**
     * Delegates the check to Spring Security, which compares the password in constant time
     * and reports an unknown email and a wrong password with the same exception. No
     * transaction: BCrypt is slow on purpose and would hold a connection meanwhile.
     */
    public AuthResponse login(AuthRequest request) {
        // Throws AuthenticationException on failure: the exception handler turns it into a generic 401
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        String token = jwtService.generateToken(request.getEmail());
        return new AuthResponse(token);
    }
}
