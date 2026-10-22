package com.autowizard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
class TaskCompletionTests {

    @Autowired MockMvc mvc;

    @Test
    void completingATaskStoresTheWorkAndUpdatesTheVehicleMileage() throws Exception {
        TestApi api = new TestApi(mvc);
        String token = api.newUser();
        long vehicleId = api.createVehicle(token, 58_000);
        long taskId = api.createTask(token, vehicleId, "2026-01-01T10:00:00", 60_000);

        mvc.perform(complete(taskId, token, """
                        {"actualCost":210.00,"completionMileage":61200,"completionDate":"2026-01-15T17:30:00"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.actualCost").value(210.0))
                .andExpect(jsonPath("$.completionMileage").value(61200))
                .andExpect(jsonPath("$.completionDate").value("2026-01-15T17:30:00"));

        mvc.perform(get("/api/vehicles/" + vehicleId).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.mileage").value(61200));
        mvc.perform(get("/api/maintenance-tasks/completed/vehicle/" + vehicleId).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[0].id").value(taskId));
    }

    @Test
    void aCompletedTaskCannotBeCompletedAgain() throws Exception {
        TestApi api = new TestApi(mvc);
        String token = api.newUser();
        long taskId = api.createTask(token, api.createVehicle(token), "2026-01-01T10:00:00", 60_000);

        mvc.perform(complete(taskId, token, "{\"completionMileage\":60000}")).andExpect(status().isOk());
        mvc.perform(complete(taskId, token, "{\"completionMileage\":60000}")).andExpect(status().isConflict());
    }

    @Test
    void completionNeedsAMileageAndATaskOfTheCaller() throws Exception {
        TestApi api = new TestApi(mvc);
        String token = api.newUser();
        long taskId = api.createTask(token, api.createVehicle(token), "2026-01-01T10:00:00", 60_000);

        mvc.perform(complete(taskId, token, "{\"actualCost\":10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.completionMileage").exists());
        mvc.perform(complete(taskId, api.newUser(), "{\"completionMileage\":60000}"))
                .andExpect(status().isNotFound());
    }

    private static org.springframework.test.web.servlet.RequestBuilder complete(long taskId, String token, String body) {
        return patch("/api/maintenance-tasks/" + taskId + "/complete").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
