package com.placement.platform.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 1000)
    private String message;

    private String type; // NEW_JOB, SHORTLISTED, INTERVIEW_SCHEDULED, SELECTION, PLACEMENT_DRIVE
    private String status = "UNREAD"; // UNREAD, READ
    private String actionUrl;
    private LocalDateTime createdAt = LocalDateTime.now();
}
