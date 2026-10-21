package com.autowizard.service;

import com.autowizard.dto.ReminderDTO;
import com.autowizard.exception.ReminderException;
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
        try {
            Reminder reminder = reminderRepository.findById(reminderId)
                    .orElseThrow(() -> new ReminderException("Reminder not found"));

            if (!reminder.getUser().getId().equals(userId)) {
                throw new ReminderException("Not authorized to update this reminder");
            }
            reminder.setNotified(true);
            reminderRepository.save(reminder);
        } catch (Exception e) {
            throw new ReminderException("Error marking reminder as notified: " + e.getMessage(), e);
        }
    }

    @Transactional
    public Set<ReminderDTO> generatePendingTaskReminders(Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ReminderException("User not found"));

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
                    .map(this::convertToDTO)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            throw new ReminderException("Error generating maintenance reminders: " + e.getMessage(), e);
        }
    }

    @Transactional
    public Set<ReminderDTO> generateOverdueTaskReminders(Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ReminderException("User not found"));

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
                    .map(this::convertToDTO)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            throw new ReminderException("Error generating maintenance reminders: " + e.getMessage(), e);
        }
    }

    private ReminderDTO convertToDTO(Reminder reminder) {
        try {
            ReminderDTO dto = new ReminderDTO();
            dto.setId(reminder.getId());
            dto.setTitle(reminder.getTitle());
            dto.setMessage(reminder.getMessage());
            dto.setNotificationDate(reminder.getNotificationDate());
            dto.setNotified(reminder.isNotified());
            dto.setTaskIds(reminder.getTasks().stream()
                    .map(MaintenanceTask::getId)
                    .collect(Collectors.toSet()));
            dto.setUserId(reminder.getUser().getId());
            dto.setType(reminder.getType());
            // Aggiungiamo gli ID dei veicoli associati alle task
            dto.setVehicleIds(reminder.getTasks().stream()
                    .map(task -> task.getVehicle().getId())
                    .collect(Collectors.toSet()));
            return dto;
        } catch (Exception e) {
            throw new ReminderException("Error converting reminder to DTO: " + e.getMessage(), e);
        }
    }
} 