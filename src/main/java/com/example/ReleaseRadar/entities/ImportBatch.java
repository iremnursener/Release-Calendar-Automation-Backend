package com.example.ReleaseRadar.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "import_batches")
public class ImportBatch {
    @Id
    private UUID batchId;

    private String fileName;

    @ManyToOne
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    private String notes;

    private Boolean isActive;


    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
