package com.autowizard.vehicle;

import com.autowizard.identity.User;
import com.autowizard.shared.BusinessRuleException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/**
 * A vehicle is never deleted from the database: deleting it sets {@code deletedAt}, so its
 * maintenance history, documents and reminders stay consistent. Every query on vehicles
 * ignores deleted rows ({@link SQLRestriction}).
 */
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "vehicles")
@SQLRestriction("deleted_at IS NULL")
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;

    // Unique among vehicles that are not deleted: a deleted vehicle must not block registering
    // the same plate or VIN again (e.g. a car sold to another user). Checked by VehicleService
    // and guaranteed by partial unique indexes (V2 migration).
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    /** A mileage typed by the user: it can only grow. */
    public void updateMileage(long newMileage) {
        if (newMileage < mileage) {
            throw new BusinessRuleException("New mileage cannot be less than current mileage");
        }
        this.mileage = newMileage;
    }

    /** A mileage read from completed work: raises the current one, never lowers it. */
    public void recordMileage(long observedMileage) {
        if (observedMileage > mileage) {
            this.mileage = observedMileage;
        }
    }

    /**
     * Equal when the other object is the same row: same class (a Hibernate proxy is a
     * subclass) and the same non-null id, read through getId() so a proxy answers without
     * being initialized. The hash code is constant, so it does not change when the id is
     * assigned on insert and the entity stays findable in a HashSet.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vehicle other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
