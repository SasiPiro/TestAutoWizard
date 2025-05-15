package com.autowizard.service;

import com.autowizard.dto.MaintenanceTaskDTO;
import com.autowizard.model.MaintenanceTask;
import com.autowizard.model.Vehicle;
import com.autowizard.repository.MaintenanceTaskRepository;
import com.autowizard.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaintenanceTaskService {
    private final MaintenanceTaskRepository maintenanceTaskRepository;
    private final VehicleRepository vehicleRepository;

    public List<MaintenanceTaskDTO> getTasksByVehicleId(Long vehicleId, Long userId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        if (!vehicle.getOwner().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to view tasks for this vehicle");
        }

        return maintenanceTaskRepository.findByVehicleId(vehicleId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private MaintenanceTaskDTO convertToDTO(MaintenanceTask task) {
        MaintenanceTaskDTO dto = new MaintenanceTaskDTO();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setScheduledDate(task.getScheduledDate());
        dto.setStatus(task.getStatus());
        dto.setPriority(task.getPriority());
        dto.setCompletionDate(task.getCompletionDate());
        dto.setNotes(task.getNotes());
        dto.setCost(task.getCost());
        dto.setScheduledMileage(task.getScheduledMileage());
        dto.setCompletionMileage(task.getCompletionMileage());
        dto.setVehicleId(task.getVehicle().getId());
        return dto;
    }
} 