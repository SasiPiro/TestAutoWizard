package com.autowizard.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "reminders")
public class Reminder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private LocalDateTime notificationDate;

    private boolean notified = false;

    @ManyToMany
    @JoinTable(
        name = "reminder_tasks",
        joinColumns = @JoinColumn(name = "reminder_id"),
        inverseJoinColumns = @JoinColumn(name = "task_id")
    )
    private Set<MaintenanceTask> tasks = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReminderType type;

    public enum ReminderType {
        PENDING_TASKS,        // Notifica per task pendenti
        OVERDUE_TASKS,        // Notifica per task scadute
        MAINTENANCE_SUMMARY   // Riepilogo periodico delle manutenzioni
    }
} 