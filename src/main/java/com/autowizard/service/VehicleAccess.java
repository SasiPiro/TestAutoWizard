package com.autowizard.service;

import com.autowizard.exception.NotFoundException;
import com.autowizard.model.Vehicle;
import com.autowizard.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The single place where "this vehicle belongs to this user" is checked. Every
 * vehicle-scoped operation goes through it, so the check cannot be forgotten again.
 */
@Component
@RequiredArgsConstructor
public class VehicleAccess {

    private final VehicleRepository vehicleRepository;

    /** Returns the vehicle if it belongs to the user; otherwise 404, never 403 (do not reveal existence). */
    public Vehicle requireOwned(Long vehicleId, Long userId) {
        return vehicleRepository.findByIdAndOwnerId(vehicleId, userId)
                .orElseThrow(() -> new NotFoundException("Vehicle not found"));
    }
}
