package com.autowizard.maintenance;

import com.autowizard.shared.BusinessRuleException;
import com.autowizard.shared.ConflictException;
import com.autowizard.vehicle.Vehicle;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "maintenance_tasks")
public class MaintenanceTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
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
    // Money: exact decimals, never binary floating point
    @Column(precision = 10, scale = 2)
    private BigDecimal estimatedCost;

    @Column(precision = 10, scale = 2)
    private BigDecimal actualCost;

    @Column(nullable = false)
    private Long scheduledMileage;

    private Long completionMileage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    /**
     * Records the work as done. The vehicle's mileage follows: a service done at 61,200 km
     * means the car has at least 61,200 km.
     */
    public void complete(BigDecimal actualCost, long completionMileage, LocalDateTime completionDate, LocalDateTime now) {
        if (status == TaskStatus.COMPLETED) {
            throw new ConflictException("Task already completed");
        }
        if (completionDate.isAfter(now)) {
            throw new BusinessRuleException("Completion date cannot be in the future");
        }
        this.actualCost = actualCost;
        this.completionMileage = completionMileage;
        this.completionDate = completionDate;
        this.status = TaskStatus.COMPLETED;
        vehicle.recordMileage(completionMileage);
    }

    public enum TaskStatus {
        PENDING,
        /** Past its date or mileage and not done yet: set by NightlyMaintenanceJob. */
        OVERDUE,
        COMPLETED
    }

    public enum TaskPriority {
        LOW,
        MEDIUM,
        HIGH,
        URGENT
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
        if (!(o instanceof MaintenanceTask other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
