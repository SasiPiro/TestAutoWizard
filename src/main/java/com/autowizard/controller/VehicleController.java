package com.autowizard.controller;

import com.autowizard.dto.VehicleDTO;
import com.autowizard.service.VehicleService;
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
    public ResponseEntity<VehicleDTO> createVehicle(@RequestBody VehicleDTO vehicleDTO, @RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(vehicleService.createVehicle(vehicleDTO, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleDTO> getVehicleById(@PathVariable Long id, @RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(vehicleService.getVehicleById(id, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long id, @RequestAttribute("userId") Long userId) {
        vehicleService.deleteVehicle(id, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<VehicleDTO>> getVehicles(@RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(vehicleService.getVehiclesByUserId(userId));
    }

    @PatchMapping("/{id}/mileage")
    public ResponseEntity<VehicleDTO> updateVehicleMileage(
            @PathVariable Long id,
            @RequestParam Long newMileage,
            @RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(vehicleService.updateVehicleMileage(id, newMileage, userId));
    }
} 