package com.autowizard.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
//TODO FARE JOB 00:01 PER AGGIORNARE STATUS TASK
@Data
@Entity
@Table(name = "maintenance_tasks")
public class MaintenanceTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private LocalDateTime scheduledDate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TaskPriority priority;

    private LocalDateTime completionDate;
    private String notes;
    private Double estimatedCost;
    private Double actualCost;

    @Column(nullable = false)
    private Long scheduledMileage;

    private Long completionMileage;

    @ManyToOne
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    public enum TaskStatus {
        PENDING,
        OVERDUE,
        COMPLETED
    }

    public enum TaskPriority {
        LOW,
        MEDIUM,
        HIGH,
        URGENT
    }
} 