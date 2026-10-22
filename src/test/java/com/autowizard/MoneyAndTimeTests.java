package com.autowizard;

import com.autowizard.shared.TimeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("dev")
@AutoConfigureMockMvc
class MoneyAndTimeTests {

    static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 21, 9, 30);

    @Autowired MockMvc mvc;

    @MockitoBean TimeService timeService;

    @Test
    void costsAreStoredAndReturnedAsDecimals() throws Exception {
        TestApi api = new TestApi(mvc);
        String token = api.newUser();
        long vehicleId = api.createVehicle(token);

        mvc.perform(post("/api/maintenance-tasks").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(task(vehicleId, "1234567.89")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/maintenance-tasks/vehicle/" + vehicleId).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[0].estimatedCost").value(1234567.89));
    }

    @Test
    void moreThanTwoDecimalsIsRejected() throws Exception {
        TestApi api = new TestApi(mvc);
        String token = api.newUser();
        mvc.perform(post("/api/maintenance-tasks").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(task(api.createVehicle(token), "180.555")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.estimatedCost").exists());
    }

    @Test
    void timestampsComeFromTheInjectedClock() throws Exception {
        when(timeService.now()).thenReturn(NOW);
        TestApi api = new TestApi(mvc);
        String token = api.newUser();
        long vehicleId = api.createVehicle(token);

        mvc.perform(multipart("/api/documents/upload")
                        .file(new MockMultipartFile("file", "bollo.pdf", "application/pdf", "%PDF-1.4".getBytes()))
                        .param("type", "BOLLO").param("vehicleId", String.valueOf(vehicleId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadDate").value("2026-10-21T09:30:00"));
    }

    private static String task(long vehicleId, String estimatedCost) {
        return """
                {"title":"Freni","description":"Pastiglie anteriori","scheduledDate":"2030-01-01T10:00:00",
                 "priority":"HIGH","scheduledMileage":60000,"estimatedCost":%s,"vehicleId":%d}"""
                .formatted(estimatedCost, vehicleId);
    }
}
