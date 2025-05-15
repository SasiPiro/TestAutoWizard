package com.autowizard.repository;

import com.autowizard.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByOwnerId(Long ownerId);
    boolean existsByLicensePlate(String licensePlate);
    boolean existsByVin(String vin);
} 