package com.autowizard;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "logging.level.com.autowizard=DEBUG")
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class JwtSecurityTests {

    @Autowired MockMvc mvc;

    @Test
    void validTokenFromLoginIsAccepted() throws Exception {
        String token = registerAndLogin();
        mvc.perform(get("/api/vehicles").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejectedWith401() throws Exception {
        String email = "forge-" + UUID.randomUUID() + "@test.it";
        register(email);
        // Everything a real token has, user id included: only the signature is wrong
        String forged = Jwts.builder()
                .claim("userId", 1L)
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(Keys.hmacShaKeyFor("an-attacker-key-that-is-long-enough-for-hs256"
                        .getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();

        mvc.perform(get("/api/vehicles").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingOrGarbageTokenIs401() throws Exception {
        mvc.perform(get("/api/vehicles")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/vehicles").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anInvalidTokenDoesNotBlockPublicEndpoints() throws Exception {
        String email = "public-" + UUID.randomUUID() + "@test.it";
        register(email);
        mvc.perform(post("/api/auth/login").header("Authorization", "Bearer expired.or.broken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password-123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void tokensAreNeverLoggedEvenAtDebugLevel(CapturedOutput output) throws Exception {
        String token = registerAndLogin();
        mvc.perform(get("/api/vehicles").header("Authorization", "Bearer " + token));
        mvc.perform(get("/api/vehicles").header("Authorization", "Bearer not.a.jwt"));

        assertThat(output.getAll()).doesNotContain(token).doesNotContain("Bearer ");
    }

    private String registerAndLogin() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@test.it";
        register(email);
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password-123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return body.replaceAll(".*\"token\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }

    private void register(String email) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password-123\"}"))
                .andExpect(status().isOk());
    }
}
