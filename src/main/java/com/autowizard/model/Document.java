package com.autowizard.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "documents")
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DocumentType type; // LIBRETTO DI CIRCOLAZIONE, ASSICURAZIONE, BOLLO, FATTURA MANUTENZIONE , LIBRETTO MANUTENZIONE PREGRESSA etc.
    

    @Column(nullable = false)
    private String contentType;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] content;

    private LocalDateTime uploadDate;

    @ManyToOne
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public enum DocumentType {
        LIBRETTO_DI_CIRCOLAZIONE,
        ASSICURAZIONE,
        BOLLO,
        FATTURA_MANUTENZIONE,
        LIBRETTO_MANUTENZIONE_PREGRESSA
    }
}
