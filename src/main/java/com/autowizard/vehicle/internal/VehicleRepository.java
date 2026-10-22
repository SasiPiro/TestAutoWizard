package com.autowizard.vehicle.internal;

import com.autowizard.vehicle.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    // Explicit: the derived query joined users only to read the foreign key
    @Query("SELECT v FROM Vehicle v WHERE v.owner.id = :ownerId ORDER BY v.id")
    List<Vehicle> findByOwnerId(Long ownerId);
    Optional<Vehicle> findByLicensePlate(String licensePlate);
    Optional<Vehicle> findByVin(String vin);
    Optional<Vehicle> findByIdAndOwnerId(Long id, Long ownerId);
    boolean existsByLicensePlate(String licensePlate);
    boolean existsByVin(String vin);
} 