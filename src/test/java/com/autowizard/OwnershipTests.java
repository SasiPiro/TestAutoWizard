package com.autowizard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A vehicle and everything attached to it is visible only to its owner. For anyone else
 * it does not exist: 404, the same answer as for an id that was never used.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("dev")
@AutoConfigureMockMvc
class OwnershipTests {

    @Autowired MockMvc mvc;

    TestApi api;
    String owner;
    String stranger;
    long vehicleId;

    @BeforeEach
    void ownerWithAVehicle() throws Exception {
        api = new TestApi(mvc);
        owner = api.newUser();
        stranger = api.newUser();
        vehicleId = api.createVehicle(owner);
        api.createTask(owner, vehicleId, "2030-01-01T10:00:00", 60_000);
    }

    @Test
    void anotherUserGets404OnEveryVehicleScopedEndpoint() throws Exception {
        for (RequestBuilder request : vehicleScopedRequests(stranger, vehicleId)) {
            mvc.perform(request).andExpect(status().isNotFound());
        }
    }

    @Test
    void aVehicleThatDoesNotExistGetsTheSame404() throws Exception {
        for (RequestBuilder request : vehicleScopedRequests(owner, Long.MAX_VALUE)) {
            mvc.perform(request).andExpect(status().isNotFound());
        }
    }

    @Test
    void theOwnerCanReadEverything() throws Exception {
        for (String path : List.of("/api/vehicles/%d", "/api/maintenance-tasks/vehicle/%d",
                "/api/maintenance-tasks/pending/vehicle/%d", "/api/maintenance-tasks/overdue/vehicle/%d",
                "/api/maintenance-tasks/completed/vehicle/%d", "/api/documents/vehicle/%d")) {
            mvc.perform(get(path.formatted(vehicleId)).header("Authorization", "Bearer " + owner))
                    .andExpect(status().isOk());
        }
    }

    private static List<RequestBuilder> vehicleScopedRequests(String token, long vehicleId) {
        String auth = "Bearer " + token;
        return List.of(
                get("/api/vehicles/" + vehicleId).header("Authorization", auth),
                patch("/api/vehicles/" + vehicleId + "/mileage").param("newMileage", "90000").header("Authorization", auth),
                delete("/api/vehicles/" + vehicleId).header("Authorization", auth),
                get("/api/maintenance-tasks/vehicle/" + vehicleId).header("Authorization", auth),
                get("/api/maintenance-tasks/pending/vehicle/" + vehicleId).header("Authorization", auth),
                get("/api/maintenance-tasks/overdue/vehicle/" + vehicleId).header("Authorization", auth),
                get("/api/maintenance-tasks/completed/vehicle/" + vehicleId).header("Authorization", auth),
                post("/api/maintenance-tasks").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"x","description":"x","scheduledDate":"2030-01-01T10:00:00",
                                 "priority":"LOW","scheduledMileage":1,"vehicleId":%d}""".formatted(vehicleId)),
                get("/api/documents/vehicle/" + vehicleId).header("Authorization", auth),
                multipart("/api/documents/upload")
                        .file(new MockMultipartFile("file", "a.pdf", "application/pdf", "%PDF-1.4".getBytes()))
                        .param("type", "ASSICURAZIONE").param("vehicleId", String.valueOf(vehicleId))
                        .header("Authorization", auth));
    }
}
