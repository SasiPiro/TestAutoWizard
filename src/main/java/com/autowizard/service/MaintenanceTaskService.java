package com.autowizard.service;

import com.autowizard.dto.MaintenanceTaskDTO;
import com.autowizard.exception.MaintenanceTaskException;
import com.autowizard.model.MaintenanceTask;
import com.autowizard.model.Vehicle;
import com.autowizard.repository.MaintenanceTaskRepository;
import com.autowizard.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaintenanceTaskService {
    private final MaintenanceTaskRepository maintenanceTaskRepository;
    private final VehicleRepository vehicleRepository;

    @Transactional
    public MaintenanceTaskDTO createTask(MaintenanceTaskDTO taskDTO, Long userId) {
        log.debug("Creating maintenance task for user: {}", userId);
        Vehicle vehicle = vehicleRepository.findById(taskDTO.getVehicleId())
                .orElseThrow(() -> new MaintenanceTaskException("Vehicle not found"));

        if (!vehicle.getOwner().getId().equals(userId)) {
            throw new MaintenanceTaskException("Vehicle does not belong to user");
        }

        MaintenanceTask task = new MaintenanceTask();
        task.setTitle(taskDTO.getTitle());
        task.setDescription(taskDTO.getDescription());
        task.setScheduledDate(taskDTO.getScheduledDate());
        task.setStatus(MaintenanceTask.TaskStatus.PENDING);
        task.setPriority(taskDTO.getPriority());
        task.setScheduledMileage(taskDTO.getScheduledMileage());
        task.setNotes(taskDTO.getNotes());
        task.setVehicle(vehicle);

        MaintenanceTask savedTask = maintenanceTaskRepository.save(task);
        return convertToDTO(savedTask);
    }

    @Transactional(readOnly = true)
    public List<MaintenanceTaskDTO> getTasksByVehicleId(Long vehicleId, Long userId) {
        log.debug("Fetching tasks for vehicle {} and user: {}", vehicleId, userId);
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new MaintenanceTaskException("Vehicle not found"));

        if (!vehicle.getOwner().getId().equals(userId)) {
            throw new MaintenanceTaskException("Vehicle does not belong to user");
        }

        return maintenanceTaskRepository.findByVehicleId(vehicleId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MaintenanceTaskDTO> getPendingTasksByUser(Long userId) {
        try {
            return maintenanceTaskRepository.findByStatusAndVehicleOwnerId(MaintenanceTask.TaskStatus.PENDING, userId)
                    .stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new MaintenanceTaskException("Error fetching pending tasks for user: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<MaintenanceTaskDTO> getPendingTasksByVehicle(Long vehicleId) {
        try {
            return maintenanceTaskRepository.findByStatusAndVehicleId(MaintenanceTask.TaskStatus.PENDING, vehicleId)
                    .stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new MaintenanceTaskException("Error fetching pending tasks for vehicle: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<MaintenanceTaskDTO> getOverdueTasksByUser(Long userId) {
        try {
            return maintenanceTaskRepository.findByStatusAndVehicleOwnerId(MaintenanceTask.TaskStatus.OVERDUE, userId)
                    .stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new MaintenanceTaskException("Error fetching overdue tasks for user: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<MaintenanceTaskDTO> getOverdueTasksByVehicle(Long vehicleId) {
        try {
            return maintenanceTaskRepository.findByStatusAndVehicleId(MaintenanceTask.TaskStatus.OVERDUE, vehicleId)
                    .stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new MaintenanceTaskException("Error fetching overdue tasks for vehicle: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<MaintenanceTaskDTO> getCompletedTasksByUser(Long userId) {
        try {
            return maintenanceTaskRepository.findByStatusAndVehicleOwnerId(MaintenanceTask.TaskStatus.COMPLETED, userId)
                    .stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new MaintenanceTaskException("Error fetching completed tasks for user: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<MaintenanceTaskDTO> getCompletedTasksByVehicle(Long vehicleId) {
        try {
            return maintenanceTaskRepository.findByStatusAndVehicleId(MaintenanceTask.TaskStatus.COMPLETED, vehicleId)
                    .stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new MaintenanceTaskException("Error fetching completed tasks for vehicle: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<MaintenanceTaskDTO> getAllTasksByUser(Long userId) {
        try {
            return maintenanceTaskRepository.findByVehicleOwnerId(userId)
                    .stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new MaintenanceTaskException("Error fetching all tasks for user: " + e.getMessage(), e);
        }
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