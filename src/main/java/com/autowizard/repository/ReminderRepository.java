package com.autowizard.repository;

import com.autowizard.model.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {
    List<Reminder> findByUserIdAndNotifiedFalse(Long userId);
    Optional<Reminder> findByIdAndUserId(Long id, Long userId);
    List<Reminder> findByUserIdAndNotificationDateBeforeAndNotifiedFalse(Long userId, LocalDateTime date);
    List<Reminder> findByUserIdAndTypeAndNotifiedFalse(Long userId, Reminder.ReminderType type);
    List<Reminder> findByTypeAndNotifiedFalseOrderByNotificationDateDesc(Reminder.ReminderType type);
    List<Reminder> findByUserIdAndTypeAndNotifiedFalseOrderByNotificationDateDesc(Long userId, Reminder.ReminderType type);
} 