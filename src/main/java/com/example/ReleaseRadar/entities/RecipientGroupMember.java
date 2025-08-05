package com.example.ReleaseRadar.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recipient_group_members")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipientGroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private RecipientGroup group;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}