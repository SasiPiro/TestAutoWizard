package com.autowizard.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "vehicles")
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

    @Column(nullable = false, unique = true)
    private String licensePlate;

    @Column(nullable = false, unique = true)
    private String vin;

    private Long mileage;
    private Integer registrationYear;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL)
    private Set<Document> documents = new HashSet<>();

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL)
    private Set<Reminder> reminders = new HashSet<>();
} 