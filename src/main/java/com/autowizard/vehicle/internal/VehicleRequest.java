package com.autowizard.vehicle.internal;

import com.autowizard.vehicle.FuelType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** What a client may set on a vehicle: no id, no owner. */
public record VehicleRequest(
        @NotBlank @Size(max = 50) String type,
        @NotBlank @Size(max = 50) String brand,
        @NotBlank @Size(max = 50) String model,
        @NotBlank @Pattern(regexp = "[A-Z0-9]{5,10}", message = "must be 5 to 10 uppercase letters or digits")
        String licensePlate,
        @NotBlank @Size(min = 11, max = 17) String vin,
        @NotNull @PositiveOrZero Long mileage,
        @NotNull @Min(1900) Integer registrationYear,
        @NotNull FuelType fuel) {
}
