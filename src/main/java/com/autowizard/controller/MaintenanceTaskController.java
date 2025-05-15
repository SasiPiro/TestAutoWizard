package com.autowizard.controller;

import com.autowizard.dto.MaintenanceTaskDTO;
import com.autowizard.service.MaintenanceTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/maintenance-tasks")
@RequiredArgsConstructor
public class MaintenanceTaskController {
    private final MaintenanceTaskService maintenanceTaskService;

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<MaintenanceTaskDTO>> getTasksByVehicle(
            @PathVariable Long vehicleId,
            @RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(maintenanceTaskService.getTasksByVehicleId(vehicleId, userId));
    }
} 