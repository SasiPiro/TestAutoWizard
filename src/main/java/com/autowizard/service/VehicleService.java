package com.autowizard.service;

import com.autowizard.dto.VehicleRequest;
import com.autowizard.dto.VehicleResponse;
import com.autowizard.exception.BusinessRuleException;
import com.autowizard.exception.ConflictException;
import com.autowizard.model.Vehicle;
import com.autowizard.model.User;
import com.autowizard.repository.VehicleRepository;
import com.autowizard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleService {
    private final VehicleRepository vehicleRepository;
    private final TimeService timeService;
    private final UserRepository userRepository;
    private final VehicleAccess vehicleAccess;

    @Transactional
    public VehicleResponse createVehicle(VehicleRequest request, Long userId) {
        if (vehicleRepository.existsByLicensePlate(request.licensePlate())) {
            throw new ConflictException("A vehicle with this license plate is already registered");
        }
        if (vehicleRepository.existsByVin(request.vin())) {
            throw new ConflictException("A vehicle with this VIN is already registered");
        }

        User user = userRepository.getReferenceById(userId);

        Vehicle vehicle = new Vehicle();
        vehicle.setType(request.type());
        vehicle.setBrand(request.brand());
        vehicle.setModel(request.model());
        vehicle.setLicensePlate(request.licensePlate());
        vehicle.setVin(request.vin());
        vehicle.setMileage(request.mileage());
        vehicle.setRegistrationYear(request.registrationYear());
        vehicle.setFuel(request.fuel());
        vehicle.setOwner(user);

        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        log.info("[VEHICLE_CREATE] outcome=success vehicleId={} userId={}", savedVehicle.getId(), userId);
        return toResponse(savedVehicle);
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> getVehiclesByUserId(Long userId) {
        return vehicleRepository.findByOwnerId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VehicleResponse getVehicleById(Long id, Long userId) {
        Vehicle vehicle = vehicleAccess.requireOwned(id, userId);

        return toResponse(vehicle);
    }

    @Transactional
    public void deleteVehicle(Long id, Long userId) {
        Vehicle vehicle = vehicleAccess.requireOwned(id, userId);

        // Soft delete: tasks, documents and reminders keep pointing to an existing row
        vehicle.setDeletedAt(timeService.now());
        log.info("[VEHICLE_DELETE] outcome=success vehicleId={} userId={}", id, userId);
    }

    @Transactional
    public VehicleResponse updateVehicleMileage(Long id, Long newMileage, Long userId) {
        Vehicle vehicle = vehicleAccess.requireOwned(id, userId);

        if (newMileage < vehicle.getMileage()) {
            throw new BusinessRuleException("New mileage cannot be less than current mileage");
        }

        long previousMileage = vehicle.getMileage();
        vehicle.setMileage(newMileage);
        Vehicle updatedVehicle = vehicleRepository.save(vehicle);
        log.info("[VEHICLE_MILEAGE] outcome=success vehicleId={} from={} to={}", id, previousMileage, newMileage);
        return toResponse(updatedVehicle);
    }

    private VehicleResponse toResponse(Vehicle vehicle) {
        return new VehicleResponse(vehicle.getId(), vehicle.getType(), vehicle.getBrand(), vehicle.getModel(),
                vehicle.getLicensePlate(), vehicle.getVin(), vehicle.getMileage(), vehicle.getRegistrationYear(),
                vehicle.getFuel());
    }
} 