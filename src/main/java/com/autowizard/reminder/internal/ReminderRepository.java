package com.autowizard.reminder.internal;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {
    List<Reminder> findByUserIdAndNotifiedFalseOrderByNotificationDateDesc(Long userId);

    Optional<Reminder> findByIdAndUserId(Long id, Long userId);

    /**
     * The user's open reminders, locked until the end of the transaction: two completions in
     * quick succession update the same reminder from two listeners at once, and without the
     * lock each would remove its task from a stale copy.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reminder r WHERE r.user.id = :userId AND r.notified = false")
    List<Reminder> findOpenByUserIdForUpdate(Long userId);

    /** The most recent reminder of the type for every user who has one, with its user. */
    @Query("""
            SELECT r FROM Reminder r JOIN FETCH r.user
             WHERE r.type = :type
               AND r.id IN (SELECT MAX(r2.id) FROM Reminder r2 WHERE r2.type = :type GROUP BY r2.user.id)""")
    List<Reminder> findLatestPerUser(Reminder.ReminderType type);
}
