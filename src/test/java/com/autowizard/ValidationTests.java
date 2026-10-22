package com.autowizard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Locale;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Invalid input is rejected at the door with 400 and the reason for each field. */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
class ValidationTests {

    @Autowired MockMvc mvc;

    @Test
    void emptyRegistrationIsRejectedAndCreatesNoUser() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://autowizard.example/errors/validation-error"))
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void messagesAreInEnglishWhateverTheServerOrClientLocale() throws Exception {
        Locale serverLocale = Locale.getDefault();
        Locale.setDefault(Locale.ITALY);
        try {
            mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                            .header("Accept-Language", "it-IT")
                            .content("{\"email\":\"\",\"password\":\"secret-password\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.email").value("must not be blank"));
        } finally {
            Locale.setDefault(serverLocale);
        }
    }

    @Test
    void registrationNeedsARealEmailAndAPasswordOfAtLeast8Characters() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void incompleteVehicleIs400WithTheMissingFieldsNotADatabaseError() throws Exception {
        String token = new TestApi(mvc).newUser();
        mvc.perform(post("/api/vehicles").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"brand\":\"Fiat\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.model").exists())
                .andExpect(jsonPath("$.errors.licensePlate").exists())
                .andExpect(jsonPath("$.errors.vin").exists())
                .andExpect(jsonPath("$.errors.mileage").exists())
                .andExpect(jsonPath("$.errors.fuel").exists())
                .andExpect(jsonPath("$.errors.brand").doesNotExist());
    }

    @Test
    void fuelMustBeAKnownValue() throws Exception {
        String token = new TestApi(mvc).newUser();
        mvc.perform(post("/api/vehicles").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestApi.vehicleJson(1000).replace("BENZINA", "NUCLEAR")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void negativeMileageIsRejected() throws Exception {
        TestApi api = new TestApi(mvc);
        String token = api.newUser();
        long vehicleId = api.createVehicle(token);
        mvc.perform(patch("/api/vehicles/" + vehicleId + "/mileage").param("newMileage", "-1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void clientsCannotSetTheStatusOrTheIdOfANewTask() throws Exception {
        TestApi api = new TestApi(mvc);
        String token = api.newUser();
        long vehicleId = api.createVehicle(token);
        mvc.perform(post("/api/maintenance-tasks").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":999999,"status":"COMPLETED","actualCost":1,"title":"Gomme","description":"Cambio gomme",
                                 "scheduledDate":"2030-01-01T10:00:00","priority":"LOW","scheduledMileage":60000,
                                 "vehicleId":%d}""".formatted(vehicleId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.actualCost").value(nullValue()))
                .andExpect(jsonPath("$.id").value(not(999999)));
    }
}
