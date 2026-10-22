package com.autowizard.service;

import com.autowizard.dto.ReminderResponse;
import com.autowizard.exception.NotFoundException;
import com.autowizard.model.MaintenanceTask;
import com.autowizard.model.Reminder;
import com.autowizard.model.User;
import com.autowizard.repository.MaintenanceTaskRepository;
import com.autowizard.repository.ReminderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reminders are created by the nightly job, not by the client, and are idempotent: a user
 * has at most one open (not yet notified) reminder per type, updated when its tasks change,
 * and a reminder is never repeated for the same tasks.
 */
@Slf4j
@Service
public class ReminderService {
    private final ReminderRepository reminderRepository;
    private final MaintenanceTaskRepository maintenanceTaskRepository;
    private final int upcomingDays;

    public ReminderService(ReminderRepository reminderRepository,
                           MaintenanceTaskRepository maintenanceTaskRepository,
                           @Value("${app.reminders.upcoming-days:7}") int upcomingDays) {
        this.reminderRepository = reminderRepository;
        this.maintenanceTaskRepository = maintenanceTaskRepository;
        this.upcomingDays = upcomingDays;
    }

    /** What to tell a user about a reminder that was created or changed. */
    public record ReminderNotice(Long reminderId, Long userId, String email, String title, String message) {
    }

    @Transactional(readOnly = true)
    public List<ReminderResponse> getOpenReminders(Long userId) {
        return reminderRepository.findByUserIdAndNotifiedFalseOrderByNotificationDateDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void markAsNotified(Long reminderId, Long userId) {
        Reminder reminder = reminderRepository.findByIdAndUserId(reminderId, userId)
                .orElseThrow(() -> new NotFoundException("Reminder not found"));
        reminder.setNotified(true);
        log.info("[REMINDER_NOTIFIED] reminderId={} userId={}", reminderId, userId);
    }

    /**
     * Brings every user's reminders in line with their tasks: overdue tasks, and pending
     * tasks due in the next {@code app.reminders.upcoming-days} days. Running it twice
     * changes nothing the second time.
     *
     * @return the reminders created or changed, to be notified
     */
    @Transactional
    public List<ReminderNotice> refreshReminders(LocalDateTime now) {
        List<ReminderNotice> notices = new ArrayList<>();
        notices.addAll(refresh(Reminder.ReminderType.OVERDUE_TASKS,
                maintenanceTaskRepository.findWithOwnerByStatus(MaintenanceTask.TaskStatus.OVERDUE), now));
        notices.addAll(refresh(Reminder.ReminderType.PENDING_TASKS,
                maintenanceTaskRepository.findPendingWithOwnerDueBetween(now, now.plusDays(upcomingDays)), now));
        return notices;
    }

    private List<ReminderNotice> refresh(Reminder.ReminderType type, List<MaintenanceTask> tasks, LocalDateTime now) {
        Map<User, Set<MaintenanceTask>> tasksByUser = tasks.stream()
                .collect(Collectors.groupingBy(task -> task.getVehicle().getOwner(), Collectors.toSet()));
        Map<Long, Reminder> latestByUser = reminderRepository.findLatestPerUser(type).stream()
                .collect(Collectors.toMap(reminder -> reminder.getUser().getId(), Function.identity()));

        // Nothing left to remind: an open reminder would only be noise
        Set<Long> usersWithTasks = tasksByUser.keySet().stream().map(User::getId).collect(Collectors.toSet());
        latestByUser.values().stream()
                .filter(reminder -> !reminder.isNotified() && !usersWithTasks.contains(reminder.getUser().getId()))
                .forEach(reminderRepository::delete);

        List<ReminderNotice> notices = new ArrayList<>();
        tasksByUser.forEach((user, userTasks) -> {
            Reminder latest = latestByUser.get(user.getId());
            if (latest != null && latest.getTasks().equals(userTasks)) {
                return; // already reminded about exactly these tasks, open or notified
            }
            Reminder reminder = latest != null && !latest.isNotified() ? latest : newReminder(user, type);
            reminder.setTasks(userTasks);
            reminder.setTitle(title(type));
            reminder.setMessage(message(type, userTasks.size()));
            reminder.setNotificationDate(now);
            reminderRepository.save(reminder);
            log.info("[REMINDER_REFRESH] outcome={} reminderId={} userId={} type={} tasks={}",
                    reminder == latest ? "updated" : "created", reminder.getId(), user.getId(), type, userTasks.size());
            notices.add(new ReminderNotice(reminder.getId(), user.getId(), user.getEmail(),
                    reminder.getTitle(), reminder.getMessage()));
        });
        return notices;
    }

    private static Reminder newReminder(User user, Reminder.ReminderType type) {
        Reminder reminder = new Reminder();
        reminder.setUser(user);
        reminder.setType(type);
        reminder.setNotified(false);
        return reminder;
    }

    private String title(Reminder.ReminderType type) {
        return type == Reminder.ReminderType.OVERDUE_TASKS ? "Manutenzioni scadute" : "Manutenzioni in arrivo";
    }

    private String message(Reminder.ReminderType type, int count) {
        return type == Reminder.ReminderType.OVERDUE_TASKS
                ? "Hai " + count + " manutenzioni che non hai eseguito nei tempi consigliati"
                : "Hai " + count + " manutenzioni da eseguire nei prossimi " + upcomingDays + " giorni";
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
