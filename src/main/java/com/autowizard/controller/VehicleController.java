package com.autowizard.controller;

import com.autowizard.dto.VehicleRequest;
import com.autowizard.dto.VehicleResponse;
import com.autowizard.security.CurrentUser;
import com.autowizard.security.UserPrincipal;
import com.autowizard.service.VehicleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {
    private final VehicleService vehicleService;

    @PostMapping
    public ResponseEntity<VehicleResponse> createVehicle(
            @Valid @RequestBody VehicleRequest request,
            @CurrentUser UserPrincipal currentUser) {
        log.debug("Creating vehicle for user: {}", currentUser.getId());
        return ResponseEntity.ok(vehicleService.createVehicle(request, currentUser.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponse> getVehicleById(
            @PathVariable Long id,
            @CurrentUser UserPrincipal currentUser) {
        log.debug("Fetching vehicle {} for user: {}", id, currentUser.getId());
        return ResponseEntity.ok(vehicleService.getVehicleById(id, currentUser.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehicle(
            @PathVariable Long id,
            @CurrentUser UserPrincipal currentUser) {
        log.debug("Deleting vehicle {} for user: {}", id, currentUser.getId());
        vehicleService.deleteVehicle(id, currentUser.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<VehicleResponse>> getVehicles(
            @CurrentUser UserPrincipal currentUser) {
        log.debug("Fetching all vehicles for user: {}", currentUser.getId());
        return ResponseEntity.ok(vehicleService.getVehiclesByUserId(currentUser.getId()));
    }

    @PatchMapping("/{id}/mileage")
    public ResponseEntity<VehicleResponse> updateVehicleMileage(
            @PathVariable Long id,
            @RequestParam @PositiveOrZero Long newMileage,
            @CurrentUser UserPrincipal currentUser) {
        log.debug("Updating mileage for vehicle {} to {} for user: {}", id, newMileage, currentUser.getId());
        return ResponseEntity.ok(vehicleService.updateVehicleMileage(id, newMileage, currentUser.getId()));
    }
} 