package com.autowizard.dto;

import lombok.Data;

@Data
public class VehicleDTO {
    private Long id;
    private String type;
    private String brand;
    private String model;
    private String licensePlate;
    private String vin;
    private Long mileage;
    private Integer registrationYear;
    private String fuel;
} 