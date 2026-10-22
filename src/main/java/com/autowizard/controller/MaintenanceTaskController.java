package com.autowizard.controller;

import com.autowizard.dto.CompleteTaskRequest;
import com.autowizard.dto.MaintenanceTaskRequest;
import com.autowizard.dto.MaintenanceTaskResponse;
import com.autowizard.security.CurrentUser;
import com.autowizard.security.UserPrincipal;
import com.autowizard.service.MaintenanceTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/maintenance-tasks")
@RequiredArgsConstructor
public class MaintenanceTaskController {
    private final MaintenanceTaskService maintenanceTaskService;

    @PostMapping
    public ResponseEntity<MaintenanceTaskResponse> createTask(
            @Valid @RequestBody MaintenanceTaskRequest request,
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(maintenanceTaskService.createTask(request, currentUser.getId()));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<MaintenanceTaskResponse> completeTask(
            @PathVariable Long id,
            @Valid @RequestBody CompleteTaskRequest request,
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(maintenanceTaskService.completeTask(id, request, currentUser.getId()));
    }

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<MaintenanceTaskResponse>> getTasksByVehicle(
            @PathVariable Long vehicleId,
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(maintenanceTaskService.getTasksByVehicleId(vehicleId, currentUser.getId()));
    }

    @GetMapping("/pending/user")
    public ResponseEntity<List<MaintenanceTaskResponse>> getPendingTasksByUser(
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(maintenanceTaskService.getPendingTasksByUser(currentUser.getId()));
    }

    @GetMapping("/pending/vehicle/{vehicleId}")
    public ResponseEntity<List<MaintenanceTaskResponse>> getPendingTasksByVehicle(
            @PathVariable Long vehicleId,
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(maintenanceTaskService.getPendingTasksByVehicle(vehicleId, currentUser.getId()));
    }

    @GetMapping("/overdue/user")
    public ResponseEntity<List<MaintenanceTaskResponse>> getOverdueTasksByUser(
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(maintenanceTaskService.getOverdueTasksByUser(currentUser.getId()));
    }

    @GetMapping("/overdue/vehicle/{vehicleId}")
    public ResponseEntity<List<MaintenanceTaskResponse>> getOverdueTasksByVehicle(
            @PathVariable Long vehicleId,
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(maintenanceTaskService.getOverdueTasksByVehicle(vehicleId, currentUser.getId()));
    }

    @GetMapping("/completed/user")
    public ResponseEntity<List<MaintenanceTaskResponse>> getCompletedTasksByUser(
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(maintenanceTaskService.getCompletedTasksByUser(currentUser.getId()));
    }

    @GetMapping("/completed/vehicle/{vehicleId}")
    public ResponseEntity<List<MaintenanceTaskResponse>> getCompletedTasksByVehicle(
            @PathVariable Long vehicleId,
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(maintenanceTaskService.getCompletedTasksByVehicle(vehicleId, currentUser.getId()));
    }

    @GetMapping("/user")
    public ResponseEntity<List<MaintenanceTaskResponse>> getAllTasksByUser(
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(maintenanceTaskService.getAllTasksByUser(currentUser.getId()));
    }
} 