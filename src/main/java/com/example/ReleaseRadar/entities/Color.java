package com.example.ReleaseRadar.entities;

import com.example.ReleaseRadar.enums.CriticalityLevel;
import com.example.ReleaseRadar.enums.EventType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "colors")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Color {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "color_hex", length = 7)
    private String colorHex;

    @Column(name = "color_name", length = 40)
    private String colorName;

    @Enumerated(EnumType.STRING)
    private CriticalityLevel criticalityLevel;

    @Enumerated(EnumType.STRING)
    private EventType eventType;

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
