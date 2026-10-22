package com.autowizard.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String firstName;
    private String lastName;
    private boolean enabled = true;
    private String resetPasswordToken;

    @Column(name = "birthdate")
    private LocalDate birthDate;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    private Set<Vehicle> vehicles = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private Set<Document> documents = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private Set<Reminder> reminders = new HashSet<>();

    /**
     * Equal when the other object is the same row: same class (a Hibernate proxy is a
     * subclass) and the same non-null id, read through getId() so a proxy answers without
     * being initialized. The hash code is constant, so it does not change when the id is
     * assigned on insert and the entity stays findable in a HashSet.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
