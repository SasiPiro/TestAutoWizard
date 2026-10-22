package com.autowizard.service;

import com.autowizard.dto.VehicleDTO;
import com.autowizard.exception.BusinessRuleException;
import com.autowizard.exception.ConflictException;
import com.autowizard.exception.NotFoundException;
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
    private final UserRepository userRepository;
    private final VehicleAccess vehicleAccess;

    @Transactional
    public VehicleDTO createVehicle(VehicleDTO vehicleDTO, Long userId) {
        log.debug("Creating vehicle for user: {}", userId);
        if (vehicleRepository.existsByLicensePlate(vehicleDTO.getLicensePlate())) {
            throw new ConflictException("A vehicle with this license plate is already registered");
        }
        if (vehicleRepository.existsByVin(vehicleDTO.getVin())) {
            throw new ConflictException("A vehicle with this VIN is already registered");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Vehicle vehicle = new Vehicle();
        vehicle.setType(vehicleDTO.getType());
        vehicle.setBrand(vehicleDTO.getBrand());
        vehicle.setModel(vehicleDTO.getModel());
        vehicle.setLicensePlate(vehicleDTO.getLicensePlate());
        vehicle.setVin(vehicleDTO.getVin());
        vehicle.setMileage(vehicleDTO.getMileage());
        vehicle.setRegistrationYear(vehicleDTO.getRegistrationYear());
        vehicle.setFuel(vehicleDTO.getFuel());
        vehicle.setOwner(user);

        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        return convertToDTO(savedVehicle);
    }

    @Transactional(readOnly = true)
    public List<VehicleDTO> getVehiclesByUserId(Long userId) {
        log.debug("Fetching all vehicles for user: {}", userId);
        return vehicleRepository.findByOwnerId(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VehicleDTO getVehicleById(Long id, Long userId) {
        log.debug("Fetching vehicle {} for user: {}", id, userId);
        Vehicle vehicle = vehicleAccess.requireOwned(id, userId);

        return convertToDTO(vehicle);
    }

    @Transactional
    public void deleteVehicle(Long id, Long userId) {
        log.debug("Deleting vehicle {} for user: {}", id, userId);
        Vehicle vehicle = vehicleAccess.requireOwned(id, userId);

        vehicleRepository.delete(vehicle);
    }

    @Transactional
    public VehicleDTO updateVehicleMileage(Long id, Long newMileage, Long userId) {
        log.debug("Updating mileage for vehicle {} to {} for user: {}", id, newMileage, userId);
        Vehicle vehicle = vehicleAccess.requireOwned(id, userId);

        if (newMileage < vehicle.getMileage()) {
            throw new BusinessRuleException("New mileage cannot be less than current mileage");
        }

        vehicle.setMileage(newMileage);
        Vehicle updatedVehicle = vehicleRepository.save(vehicle);
        return convertToDTO(updatedVehicle);
    }

    private VehicleDTO convertToDTO(Vehicle vehicle) {
        VehicleDTO dto = new VehicleDTO();
        dto.setId(vehicle.getId());
        dto.setType(vehicle.getType());
        dto.setBrand(vehicle.getBrand());
        dto.setModel(vehicle.getModel());
        dto.setLicensePlate(vehicle.getLicensePlate());
        dto.setVin(vehicle.getVin());
        dto.setMileage(vehicle.getMileage());
        dto.setRegistrationYear(vehicle.getRegistrationYear());
        dto.setFuel(vehicle.getFuel());
        return dto;
    }
} 