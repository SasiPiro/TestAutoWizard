package com.autowizard.controller;

import com.autowizard.dto.MaintenanceTaskDTO;
import com.autowizard.exception.MaintenanceTaskException;
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
        try {
            return ResponseEntity.ok(maintenanceTaskService.createTask(taskDTO, currentUser.getId()));
        } catch (MaintenanceTaskException e) {
            throw e;
        } catch (Exception e) {
            throw new MaintenanceTaskException("Error creating task: " + e.getMessage(), e);
        }
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
        try {
            return ResponseEntity.ok(maintenanceTaskService.getPendingTasksByUser(currentUser.getId()));
        } catch (MaintenanceTaskException e) {
            throw e;
        } catch (Exception e) {
            throw new MaintenanceTaskException("Error getting pending tasks: " + e.getMessage(), e);
        }
    }

    @GetMapping("/pending/vehicle/{vehicleId}")
    public ResponseEntity<List<MaintenanceTaskDTO>> getPendingTasksByVehicle(
            @PathVariable Long vehicleId,
            @CurrentUser UserPrincipal currentUser) {
        log.debug("Getting pending tasks for vehicle: {} and user: {}", vehicleId, currentUser.getId());
        try {
            return ResponseEntity.ok(maintenanceTaskService.getPendingTasksByVehicle(vehicleId));
        } catch (MaintenanceTaskException e) {
            throw e;
        } catch (Exception e) {
            throw new MaintenanceTaskException("Error getting pending tasks: " + e.getMessage(), e);
        }
    }

    @GetMapping("/overdue/user")
    public ResponseEntity<List<MaintenanceTaskDTO>> getOverdueTasksByUser(
            @CurrentUser UserPrincipal currentUser) {
        log.debug("Getting overdue tasks for user: {}", currentUser.getId());
        try {
            return ResponseEntity.ok(maintenanceTaskService.getOverdueTasksByUser(currentUser.getId()));
        } catch (MaintenanceTaskException e) {
            throw e;
        } catch (Exception e) {
            throw new MaintenanceTaskException("Error getting overdue tasks: " + e.getMessage(), e);
        }
    }

    @GetMapping("/overdue/vehicle/{vehicleId}")
    public ResponseEntity<List<MaintenanceTaskDTO>> getOverdueTasksByVehicle(
            @PathVariable Long vehicleId,
            @CurrentUser UserPrincipal currentUser) {
        log.debug("Getting overdue tasks for vehicle: {} and user: {}", vehicleId, currentUser.getId());
        try {
            return ResponseEntity.ok(maintenanceTaskService.getOverdueTasksByVehicle(vehicleId));
        } catch (MaintenanceTaskException e) {
            throw e;
        } catch (Exception e) {
            throw new MaintenanceTaskException("Error getting overdue tasks: " + e.getMessage(), e);
        }
    }

    @GetMapping("/completed/user")
    public ResponseEntity<List<MaintenanceTaskDTO>> getCompletedTasksByUser(
            @CurrentUser UserPrincipal currentUser) {
        log.debug("Getting completed tasks for user: {}", currentUser.getId());
        try {
            return ResponseEntity.ok(maintenanceTaskService.getCompletedTasksByUser(currentUser.getId()));
        } catch (MaintenanceTaskException e) {
            throw e;
        } catch (Exception e) {
            throw new MaintenanceTaskException("Error getting completed tasks: " + e.getMessage(), e);
        }
    }

    @GetMapping("/completed/vehicle/{vehicleId}")
    public ResponseEntity<List<MaintenanceTaskDTO>> getCompletedTasksByVehicle(
            @PathVariable Long vehicleId,
            @CurrentUser UserPrincipal currentUser) {
        log.debug("Getting completed tasks for vehicle: {} and user: {}", vehicleId, currentUser.getId());
        try {
            return ResponseEntity.ok(maintenanceTaskService.getCompletedTasksByVehicle(vehicleId));
        } catch (MaintenanceTaskException e) {
            throw e;
        } catch (Exception e) {
            throw new MaintenanceTaskException("Error getting completed tasks: " + e.getMessage(), e);
        }
    }

    @GetMapping("/user")
    public ResponseEntity<List<MaintenanceTaskDTO>> getAllTasksByUser(
            @CurrentUser UserPrincipal currentUser) {
        log.debug("Getting all tasks for user: {}", currentUser.getId());
        try {
            return ResponseEntity.ok(maintenanceTaskService.getAllTasksByUser(currentUser.getId()));
        } catch (MaintenanceTaskException e) {
            throw e;
        } catch (Exception e) {
            throw new MaintenanceTaskException("Error getting all tasks: " + e.getMessage(), e);
        }
    }
} 