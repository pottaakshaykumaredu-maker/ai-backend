package com.placement.platform.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "interviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long applicationId;
    private Long jobId;
    private Long studentId;

    private String studentName;
    private String jobTitle;
    private String companyName;

    private LocalDate interviewDate;
    private String interviewTime;
    private String interviewType; // Technical, HR, Final
    private String meetingLink;
    private String location;

    @Column(length = 2000)
    private String notes;

    private String status = "SCHEDULED"; // SCHEDULED, COMPLETED, CANCELLED
    private LocalDateTime createdAt = LocalDateTime.now();
}
