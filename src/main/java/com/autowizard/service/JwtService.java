package com.autowizard.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public String generateToken(String email) {
        log.debug("Generating token for email: {}", email);
        return generateToken(new HashMap<>(), email);
    }

    public String generateToken(Map<String, Object> extraClaims, String email) {
        log.debug("Generating token with extra claims for email: {}", email);
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(email)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token) {
        try {
            boolean isValid = !isTokenExpired(token);
            log.debug("Token validation result: {}", isValid);
            return isValid;
        } catch (ExpiredJwtException e) {
            log.error("Token is expired: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.error("Token is unsupported: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.error("Token is malformed: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            log.error("Token signature is invalid: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Error validating token: {}", e.getMessage());
            return false;
        }
    }

    public String extractEmail(String token) {
        try {
            String email = extractClaim(token, Claims::getSubject);
            log.debug("Extracted email from token: {}", email);
            return email;
        } catch (Exception e) {
            log.error("Error extracting email from token: {}", e.getMessage());
            return null;
        }
    }

    private boolean isTokenExpired(String token) {
        try {
            boolean isExpired = extractExpiration(token).before(new Date());
            log.debug("Token expiration check: {}", isExpired);
            return isExpired;
        } catch (Exception e) {
            log.error("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.error("Token is expired: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            log.error("Token is unsupported: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            log.error("Token is malformed: {}", e.getMessage());
            throw e;
        } catch (SignatureException e) {
            log.error("Token signature is invalid: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error extracting claims from token: {}", e.getMessage());
            throw e;
        }
    }

    private Key getSigningKey() {
        try {
            byte[] keyBytes = secretKey.getBytes();
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            log.error("Error generating signing key: {}", e.getMessage());
            throw e;
        }
    }
} 