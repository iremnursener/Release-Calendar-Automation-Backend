package com.example.ReleaseRadar.entities;


import com.example.ReleaseRadar.enums.ReminderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Table(name = "email_reminders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "event_id")
    private Event event;
    @ManyToOne
    @JoinColumn(name = "template_id")
    private EmailTemplate template;
    @ManyToOne
    @JoinColumn(name = "group_id")
    private RecipientGroup group;
    private LocalDateTime sendDatetime;
    @Enumerated(EnumType.STRING)
    private ReminderStatus status;
    private LocalDateTime lastTryAt;
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
