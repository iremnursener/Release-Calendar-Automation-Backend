package com.example.ReleaseRadar.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Table(name = "import_batch_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportBatchEvent {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "batch_id")
    private ImportBatch batch;

    @ManyToOne
    @JoinColumn(name = "event_id")
    private Event event;

    private Integer tryCount;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}