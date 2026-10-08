package com.placement.platform.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(nullable = false)
    private String title;

    @Column(length = 4000)
    private String description;

    @ElementCollection
    @CollectionTable(name = "job_skills", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "skill")
    private List<String> skills = new ArrayList<>();

    private String salary;
    private String location;
    private String jobType; // Full Time, Internship, Part Time
    private String experience;
    private String education;
    private String eligibility;
    private LocalDate deadline;

    @Column(nullable = false)
    private String status = "ACTIVE"; // ACTIVE, CLOSED, DRAFT

    private LocalDateTime createdAt = LocalDateTime.now();
    private Integer applicantsCount = 0;
}
