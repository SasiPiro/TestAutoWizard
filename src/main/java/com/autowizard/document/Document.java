package com.autowizard.document;

import com.autowizard.identity.User;
import com.autowizard.vehicle.Vehicle;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "documents")
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DocumentType type; // LIBRETTO DI CIRCOLAZIONE, ASSICURAZIONE, BOLLO, FATTURA MANUTENZIONE , LIBRETTO MANUTENZIONE PREGRESSA etc.
    

    @Column(nullable = false)
    private String contentType;

    // bytea. Not lazy: @Basic(fetch = LAZY) is ignored without bytecode enhancement, so the
    // queries that must not read the file select only the metadata (DocumentRepository).
    @Column(nullable = false)
    private byte[] content;

    private LocalDateTime uploadDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public enum DocumentType {
        LIBRETTO_DI_CIRCOLAZIONE,
        ASSICURAZIONE,
        BOLLO,
        FATTURA_MANUTENZIONE,
        LIBRETTO_MANUTENZIONE_PREGRESSA
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
        if (!(o instanceof Document other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
