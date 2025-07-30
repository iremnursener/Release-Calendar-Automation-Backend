package com.example.ReleaseRadar.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "import_batch_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportBatchEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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