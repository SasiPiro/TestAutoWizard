package com.autowizard.service;

import com.autowizard.dto.ReminderResponse;
import com.autowizard.exception.NotFoundException;
import com.autowizard.model.MaintenanceTask;
import com.autowizard.model.Reminder;
import com.autowizard.model.User;
import com.autowizard.repository.MaintenanceTaskRepository;
import com.autowizard.repository.ReminderRepository;
import com.autowizard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReminderService {
    private final ReminderRepository reminderRepository;
    private final MaintenanceTaskRepository maintenanceTaskRepository;
    private final UserRepository userRepository;

    @Transactional
    public void markAsNotified(Long reminderId, Long userId) {
        Reminder reminder = reminderRepository.findByIdAndUserId(reminderId, userId)
                .orElseThrow(() -> new NotFoundException("Reminder not found"));
        reminder.setNotified(true);
        reminderRepository.save(reminder);
    }

    @Transactional
    public Set<ReminderResponse> generatePendingTaskReminders(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        // Ottimizzazione: carica direttamente le task pendenti dell'utente
        List<MaintenanceTask> pendingTasks = maintenanceTaskRepository.findByStatusAndVehicleOwnerId(MaintenanceTask.TaskStatus.PENDING, userId);

        if (!pendingTasks.isEmpty()) {
            // Crea un reminder per le task pendenti
            Reminder reminder = new Reminder();
            reminder.setTitle("Manutenzioni Pendenti");
            reminder.setMessage("Hai " + pendingTasks.size() + " manutenzioni da eseguire");
            reminder.setNotificationDate(LocalDateTime.now());
            reminder.setType(Reminder.ReminderType.PENDING_TASKS);
            reminder.setTasks(new HashSet<>(pendingTasks));
            reminder.setNotified(false);
            reminder.setUser(user);
            reminderRepository.save(reminder);
        }
        // Recupera e ordina i reminder generati per l'utente
        return reminderRepository.findByUserIdAndTypeAndNotifiedFalseOrderByNotificationDateDesc(userId, Reminder.ReminderType.PENDING_TASKS)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toSet());
    }

    @Transactional
    public Set<ReminderResponse> generateOverdueTaskReminders(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        // Ottimizzazione: carica direttamente le task scadute dell'utente
        List<MaintenanceTask> overdueTasks = maintenanceTaskRepository.findByStatusAndVehicleOwnerId(MaintenanceTask.TaskStatus.OVERDUE, userId);

        if (!overdueTasks.isEmpty()) {
            // Crea un reminder per le task scadute
            Reminder reminder = new Reminder();
            reminder.setTitle("Manutenzioni Scadute");
            reminder.setMessage("Hai " + overdueTasks.size() + " manutenzioni che non hai eseguito nei tempi consigliati");
            reminder.setNotificationDate(LocalDateTime.now());
            reminder.setType(Reminder.ReminderType.OVERDUE_TASKS);
            reminder.setTasks(new HashSet<>(overdueTasks));
            reminder.setNotified(false);
            reminder.setUser(user);
            reminderRepository.save(reminder);
        }
        // Recupera e ordina i reminder generati per l'utente
        return reminderRepository.findByUserIdAndTypeAndNotifiedFalseOrderByNotificationDateDesc(userId, Reminder.ReminderType.OVERDUE_TASKS)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toSet());
    }

    private ReminderResponse toResponse(Reminder reminder) {
        return new ReminderResponse(reminder.getId(), reminder.getTitle(), reminder.getMessage(),
                reminder.getNotificationDate(), reminder.isNotified(),
                reminder.getTasks().stream().map(MaintenanceTask::getId).collect(Collectors.toSet()),
                reminder.getUser().getId(), reminder.getType(),
                // The vehicles the reminded tasks belong to
                reminder.getTasks().stream().map(task -> task.getVehicle().getId()).collect(Collectors.toSet()));
    }
} 