package com.autowizard.controller;

import com.autowizard.dto.VehicleRequest;
import com.autowizard.dto.VehicleResponse;
import com.autowizard.security.CurrentUser;
import com.autowizard.security.UserPrincipal;
import com.autowizard.service.VehicleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {
    private final VehicleService vehicleService;

    @PostMapping
    public ResponseEntity<VehicleResponse> createVehicle(
            @Valid @RequestBody VehicleRequest request,
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(vehicleService.createVehicle(request, currentUser.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponse> getVehicleById(
            @PathVariable Long id,
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(vehicleService.getVehicleById(id, currentUser.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehicle(
            @PathVariable Long id,
            @CurrentUser UserPrincipal currentUser) {
        vehicleService.deleteVehicle(id, currentUser.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<VehicleResponse>> getVehicles(
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(vehicleService.getVehiclesByUserId(currentUser.getId()));
    }

    @PatchMapping("/{id}/mileage")
    public ResponseEntity<VehicleResponse> updateVehicleMileage(
            @PathVariable Long id,
            @RequestParam @PositiveOrZero Long newMileage,
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(vehicleService.updateVehicleMileage(id, newMileage, currentUser.getId()));
    }
} 