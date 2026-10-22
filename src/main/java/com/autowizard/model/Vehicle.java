package com.autowizard.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * A vehicle is never deleted from the database: deleting it sets {@code deletedAt}, so its
 * maintenance history, documents and reminders stay consistent. Every query on vehicles
 * ignores deleted rows ({@link SQLRestriction}).
 */
@Data
@Entity
@Table(name = "vehicles")
@SQLRestriction("deleted_at IS NULL")
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;

    // Unique among vehicles that are not deleted: a deleted vehicle must not block registering
    // the same plate or VIN again (e.g. a car sold to another user). Checked by VehicleService.
    @Column(nullable = false)
    private String licensePlate;

    @Column(nullable = false)
    private String vin;

    @Column(nullable = false)
    private Long mileage;

    @Column(nullable = false)
    private Integer registrationYear;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FuelType fuel;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "avg_km_per_year")
    private Long avgKmPerYear;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL)
    private Set<Document> documents = new HashSet<>();

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL)
    private Set<MaintenanceTask> maintenanceTasks = new HashSet<>();
} 