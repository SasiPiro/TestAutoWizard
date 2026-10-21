package com.autowizard.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Issues and verifies the HMAC-signed JWTs used by the API.
 *
 * <p>
 * With HMAC the same secret signs and verifies: whoever knows it can create a valid
 * token for any user. For this reason the secret has no default value and the
 * application refuses to start when it is missing, too short, or known to have been
 * exposed. A crash on deploy is better than an API that accepts forged tokens.
 * </p>
 */
@Service
public class JwtService {

    /** HS256 requires a key of at least 256 bits. */
    static final int MIN_SECRET_BYTES = 32;

    /**
     * SHA-256 of secrets that were committed to the repository and must never be used
     * again. Only the hash is stored, so this list does not leak them a second time.
     */
    static final Set<String> COMPROMISED_SECRET_SHA256 = Set.of(
            "3354cfcf65c7b2e6840ea6f880aede239750cb1d063950c4e8fd9fbf9ec73a32");

    private final Key signingKey;
    private final long jwtExpiration;

    @Autowired
    public JwtService(@Value("${jwt.secret:}") String secret,
                      @Value("${jwt.expiration:86400000}") long jwtExpiration) {
        this(secret, jwtExpiration, COMPROMISED_SECRET_SHA256);
    }

    JwtService(String secret, long jwtExpiration, Set<String> compromisedSha256) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("jwt.secret is not set: provide it with the JWT_SECRET environment "
                    + "variable (or run with the 'dev' profile for local development)");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes long");
        }
        if (compromisedSha256.contains(sha256(bytes))) {
            throw new IllegalStateException("jwt.secret has been exposed (it was committed to the repository): "
                    + "generate a new one");
        }
        this.signingKey = Keys.hmacShaKeyFor(bytes);   // built once, not on every call
        this.jwtExpiration = jwtExpiration;
    }

    public String generateToken(String email) {
        return generateToken(Map.of(), email);
    }

    public String generateToken(Map<String, Object> extraClaims, String email) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(email)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + jwtExpiration))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Verifies signature and expiration once and returns the claims, or empty if the
     * token is invalid for any reason (bad signature, expired, malformed).
     */
    public Optional<Claims> parse(String token) {
        try {
            return Optional.of(Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    static String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
