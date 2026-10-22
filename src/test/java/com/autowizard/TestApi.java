package com.autowizard;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shortcuts for the requests most tests start with. Every user, plate and VIN is random,
 * so tests that share an application context (and its database) stay independent.
 */
final class TestApi {

    static final String PASSWORD = "password-123";

    private final MockMvc mvc;

    TestApi(MockMvc mvc) {
        this.mvc = mvc;
    }

    static String randomEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@test.it";
    }

    /** Registers a new user and returns the bearer token. */
    String newUser() throws Exception {
        return register(randomEmail("user"));
    }

    String register(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    /** Creates a vehicle with a random plate and VIN and returns its id. */
    long createVehicle(String token) throws Exception {
        return createVehicle(token, 50_000);
    }

    long createVehicle(String token, long mileage) throws Exception {
        String body = mvc.perform(post("/api/vehicles").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleJson(mileage)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    static String vehicleJson(long mileage) {
        String random = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return """
                {"type":"CAR","brand":"Fiat","model":"Panda","licensePlate":"%s","vin":"%s",
                 "mileage":%d,"registrationYear":2019,"fuel":"BENZINA"}"""
                .formatted(random.substring(0, 7), random.substring(7, 24), mileage);
    }

    /** Creates a maintenance task on the vehicle and returns its id. */
    long createTask(String token, long vehicleId, String scheduledDate, long scheduledMileage) throws Exception {
        String body = mvc.perform(post("/api/maintenance-tasks").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Tagliando","description":"Olio e filtri","scheduledDate":"%s",
                                 "priority":"MEDIUM","scheduledMileage":%d,"estimatedCost":180.50,"vehicleId":%d}"""
                                .formatted(scheduledDate, scheduledMileage, vehicleId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}
