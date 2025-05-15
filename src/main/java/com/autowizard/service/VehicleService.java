package com.autowizard.service;

import com.autowizard.dto.VehicleDTO;
import com.autowizard.model.Vehicle;
import com.autowizard.model.User;
import com.autowizard.repository.VehicleRepository;
import com.autowizard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VehicleService {
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    @Transactional
    public VehicleDTO createVehicle(VehicleDTO vehicleDTO, Long userId) {
        if (vehicleRepository.existsByLicensePlate(vehicleDTO.getLicensePlate())) {
            throw new RuntimeException("Vehicle with this license plate already exists");
        }
        if (vehicleRepository.existsByVin(vehicleDTO.getVin())) {
            throw new RuntimeException("Vehicle with this VIN already exists");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Vehicle vehicle = new Vehicle();
        vehicle.setType(vehicleDTO.getType());
        vehicle.setBrand(vehicleDTO.getBrand());
        vehicle.setModel(vehicleDTO.getModel());
        vehicle.setLicensePlate(vehicleDTO.getLicensePlate());
        vehicle.setVin(vehicleDTO.getVin());
        vehicle.setMileage(vehicleDTO.getMileage());
        vehicle.setRegistrationYear(vehicleDTO.getRegistrationYear());
        vehicle.setOwner(user);

        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        return convertToDTO(savedVehicle);
    }

    @Transactional
    public VehicleDTO updateVehicle(Long id, VehicleDTO vehicleDTO, Long userId) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        if (!vehicle.getOwner().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to update this vehicle");
        }

        vehicle.setType(vehicleDTO.getType());
        vehicle.setBrand(vehicleDTO.getBrand());
        vehicle.setModel(vehicleDTO.getModel());
        vehicle.setMileage(vehicleDTO.getMileage());
        vehicle.setRegistrationYear(vehicleDTO.getRegistrationYear());

        Vehicle updatedVehicle = vehicleRepository.save(vehicle);
        return convertToDTO(updatedVehicle);
    }

    @Transactional
    public void deleteVehicle(Long id, Long userId) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        if (!vehicle.getOwner().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to delete this vehicle");
        }

        vehicleRepository.delete(vehicle);
    }

    public List<VehicleDTO> getVehiclesByUserId(Long userId) {
        return vehicleRepository.findByOwnerId(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
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
        return dto;
    }
} 