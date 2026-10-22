package com.autowizard.service;

import com.autowizard.dto.CompleteTaskRequest;
import com.autowizard.dto.MaintenanceTaskRequest;
import com.autowizard.dto.MaintenanceTaskResponse;
import com.autowizard.exception.NotFoundException;
import com.autowizard.model.MaintenanceTask;
import com.autowizard.model.Vehicle;
import com.autowizard.repository.MaintenanceTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaintenanceTaskService {
    private final MaintenanceTaskRepository maintenanceTaskRepository;
    private final VehicleAccess vehicleAccess;
    private final TimeService timeService;

    @Transactional
    public MaintenanceTaskResponse createTask(MaintenanceTaskRequest request, Long userId) {
        Vehicle vehicle = vehicleAccess.requireOwned(request.vehicleId(), userId);

        MaintenanceTask task = new MaintenanceTask();
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setScheduledDate(request.scheduledDate());
        task.setStatus(MaintenanceTask.TaskStatus.PENDING);
        task.setPriority(request.priority());
        task.setScheduledMileage(request.scheduledMileage());
        task.setEstimatedCost(request.estimatedCost());
        task.setNotes(request.notes());
        task.setVehicle(vehicle);

        MaintenanceTask savedTask = maintenanceTaskRepository.save(task);
        log.info("[TASK_CREATE] outcome=success taskId={} vehicleId={}", savedTask.getId(), vehicle.getId());
        return toResponse(savedTask);
    }

    @Transactional
    public MaintenanceTaskResponse completeTask(Long taskId, CompleteTaskRequest request, Long userId) {
        MaintenanceTask task = maintenanceTaskRepository.findOwned(taskId, userId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        LocalDateTime now = timeService.now();
        LocalDateTime completionDate = request.completionDate() != null ? request.completionDate() : now;

        task.complete(request.actualCost(), request.completionMileage(), completionDate, now);
        log.info("[TASK_COMPLETE] outcome=success taskId={} vehicleId={} completionMileage={}",
                taskId, task.getVehicle().getId(), request.completionMileage());
        return toResponse(task);
    }

    @Transactional(readOnly = true)
    public List<MaintenanceTaskResponse> getTasksByVehicleId(Long vehicleId, Long userId) {
        vehicleAccess.requireOwned(vehicleId, userId);

        return maintenanceTaskRepository.findByVehicleId(vehicleId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MaintenanceTaskResponse> getPendingTasksByUser(Long userId) {
        return maintenanceTaskRepository.findByStatusAndVehicleOwnerId(MaintenanceTask.TaskStatus.PENDING, userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MaintenanceTaskResponse> getPendingTasksByVehicle(Long vehicleId, Long userId) {
        vehicleAccess.requireOwned(vehicleId, userId);
        return maintenanceTaskRepository.findByStatusAndVehicleId(MaintenanceTask.TaskStatus.PENDING, vehicleId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MaintenanceTaskResponse> getOverdueTasksByUser(Long userId) {
        return maintenanceTaskRepository.findByStatusAndVehicleOwnerId(MaintenanceTask.TaskStatus.OVERDUE, userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MaintenanceTaskResponse> getOverdueTasksByVehicle(Long vehicleId, Long userId) {
        vehicleAccess.requireOwned(vehicleId, userId);
        return maintenanceTaskRepository.findByStatusAndVehicleId(MaintenanceTask.TaskStatus.OVERDUE, vehicleId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MaintenanceTaskResponse> getCompletedTasksByUser(Long userId) {
        return maintenanceTaskRepository.findByStatusAndVehicleOwnerId(MaintenanceTask.TaskStatus.COMPLETED, userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MaintenanceTaskResponse> getCompletedTasksByVehicle(Long vehicleId, Long userId) {
        vehicleAccess.requireOwned(vehicleId, userId);
        return maintenanceTaskRepository.findByStatusAndVehicleId(MaintenanceTask.TaskStatus.COMPLETED, vehicleId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MaintenanceTaskResponse> getAllTasksByUser(Long userId) {
        return maintenanceTaskRepository.findByVehicleOwnerId(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private MaintenanceTaskResponse toResponse(MaintenanceTask task) {
        return new MaintenanceTaskResponse(task.getId(), task.getTitle(), task.getDescription(),
                task.getScheduledDate(), task.getStatus(), task.getPriority(), task.getCompletionDate(),
                task.getNotes(), task.getEstimatedCost(), task.getActualCost(), task.getScheduledMileage(),
                task.getCompletionMileage(), task.getVehicle().getId());
    }

    //TODO. funzioni per ottenere la delle task sia globale che per periodo di tempo
} 