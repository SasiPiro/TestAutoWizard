package com.autowizard.controller;

import com.autowizard.dto.MaintenanceTaskDTO;
import com.autowizard.security.CurrentUser;
import com.autowizard.security.UserPrincipal;
import com.autowizard.service.MaintenanceTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/maintenance-tasks")
@RequiredArgsConstructor
@Slf4j
public class MaintenanceTaskController {
    private final MaintenanceTaskService maintenanceTaskService;

    @PostMapping
    public ResponseEntity<MaintenanceTaskDTO> createTask(
            @RequestBody MaintenanceTaskDTO taskDTO,
            @CurrentUser UserPrincipal currentUser) {
        log.debug("Creating maintenance task for user: {}", currentUser.getId());
        return ResponseEntity.ok(maintenanceTaskService.createTask(taskDTO, currentUser.getId()));
    }

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<MaintenanceTaskDTO>> getTasksByVehicle(
            @PathVariable Long vehicleId,
            @CurrentUser UserPrincipal currentUser) {
        log.debug("Fetching tasks for vehicle {} and user: {}", vehicleId, currentUser.getId());
        return ResponseEntity.ok(maintenanceTaskService.getTasksByVehicleId(vehicleId, currentUser.getId()));
    }

    @GetMapping("/pending/user")
    public ResponseEntity<List<MaintenanceTaskDTO>> getPendingTasksByUser(
            @CurrentUser UserPrincipal currentUser) {
        log.debug("Getting pending tasks for user: {}", currentUser.getId());
        return ResponseEntity.ok(maintenanceTaskService.getPendingTasksByUser(currentUser.getId()));
    }

    @GetMapping("/pending/vehicle/{vehicleId}")
    public ResponseEntity<List<MaintenanceTaskDTO>> getPendingTasksByVehicle(
            @PathVariable Long vehicleId,
            @CurrentUser UserPrincipal currentUser) {
        log.debug("Getting pending tasks for vehicle: {} and user: {}", vehicleId, currentUser.getId());
        return ResponseEntity.ok(maintenanceTaskService.getPendingTasksByVehicle(vehicleId, currentUser.getId()));
    }

    @GetMapping("/overdue/user")
    public ResponseEntity<List<MaintenanceTaskDTO>> getOverdueTasksByUser(
            @CurrentUser UserPrincipal currentUser) {
        log.debug("Getting overdue tasks for user: {}", currentUser.getId());
        return ResponseEntity.ok(maintenanceTaskService.getOverdueTasksByUser(currentUser.getId()));
    }

    @GetMapping("/overdue/vehicle/{vehicleId}")
    public ResponseEntity<List<MaintenanceTaskDTO>> getOverdueTasksByVehicle(
            @PathVariable Long vehicleId,
            @CurrentUser UserPrincipal currentUser) {
        log.debug("Getting overdue tasks for vehicle: {} and user: {}", vehicleId, currentUser.getId());
        return ResponseEntity.ok(maintenanceTaskService.getOverdueTasksByVehicle(vehicleId, currentUser.getId()));
    }

    @GetMapping("/completed/user")
    public ResponseEntity<List<MaintenanceTaskDTO>> getCompletedTasksByUser(
            @CurrentUser UserPrincipal currentUser) {
        log.debug("Getting completed tasks for user: {}", currentUser.getId());
        return ResponseEntity.ok(maintenanceTaskService.getCompletedTasksByUser(currentUser.getId()));
    }

    @GetMapping("/completed/vehicle/{vehicleId}")
    public ResponseEntity<List<MaintenanceTaskDTO>> getCompletedTasksByVehicle(
            @PathVariable Long vehicleId,
            @CurrentUser UserPrincipal currentUser) {
        log.debug("Getting completed tasks for vehicle: {} and user: {}", vehicleId, currentUser.getId());
        return ResponseEntity.ok(maintenanceTaskService.getCompletedTasksByVehicle(vehicleId, currentUser.getId()));
    }

    @GetMapping("/user")
    public ResponseEntity<List<MaintenanceTaskDTO>> getAllTasksByUser(
            @CurrentUser UserPrincipal currentUser) {
        log.debug("Getting all tasks for user: {}", currentUser.getId());
        return ResponseEntity.ok(maintenanceTaskService.getAllTasksByUser(currentUser.getId()));
    }
} 