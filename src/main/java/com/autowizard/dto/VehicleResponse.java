package com.autowizard.dto;

import com.autowizard.model.FuelType;

public record VehicleResponse(
        Long id,
        String type,
        String brand,
        String model,
        String licensePlate,
        String vin,
        Long mileage,
        Integer registrationYear,
        FuelType fuel) {
}
