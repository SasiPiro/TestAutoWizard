package com.autowizard.vehicle.internal;

import com.autowizard.vehicle.FuelType;

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
