package com.autowizard.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTests {

    private static final String STRONG = "a-strong-test-secret-of-more-than-32-bytes";

    @Test
    void refusesMissingOrShortSecrets() {
        assertThatThrownBy(() -> new JwtService("", 1000)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtService(null, 1000)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtService("too-short", 1000)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void refusesASecretKnownToBeExposed() {
        String exposedHash = JwtService.sha256(STRONG.getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> new JwtService(STRONG, 1000, Set.of(exposedHash)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exposed");
    }

    @Test
    void theCommittedSecretIsInTheBlockList() {
        assertThat(JwtService.COMPROMISED_SECRET_SHA256).isNotEmpty();
    }

    @Test
    void issuedTokensVerifyAndTamperedOnesDoNot() {
        JwtService jwt = new JwtService(STRONG, 60_000);
        String token = jwt.generateToken("someone@test.it");

        assertThat(jwt.parse(token)).hasValueSatisfying(c -> assertThat(c.getSubject()).isEqualTo("someone@test.it"));
        assertThat(jwt.parse(token.substring(0, token.length() - 2) + "xx")).isEmpty();
        assertThat(new JwtService("another-strong-secret-of-more-than-32-bytes", 60_000).parse(token)).isEmpty();
    }

    @Test
    void expiredTokensDoNotVerify() {
        JwtService jwt = new JwtService(STRONG, -1_000);
        assertThat(jwt.parse(jwt.generateToken("someone@test.it"))).isEmpty();
    }
}
