package com.autowizard.reminder.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {
    List<Reminder> findByUserIdAndNotifiedFalseOrderByNotificationDateDesc(Long userId);

    Optional<Reminder> findByIdAndUserId(Long id, Long userId);

    /** The most recent reminder of the type for every user who has one, with its user. */
    @Query("""
            SELECT r FROM Reminder r JOIN FETCH r.user
             WHERE r.type = :type
               AND r.id IN (SELECT MAX(r2.id) FROM Reminder r2 WHERE r2.type = :type GROUP BY r2.user.id)""")
    List<Reminder> findLatestPerUser(Reminder.ReminderType type);
}
