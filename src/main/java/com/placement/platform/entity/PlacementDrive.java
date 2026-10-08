package com.placement.platform.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "placement_drives")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlacementDrive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long companyId;
    private String companyName;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    private LocalDate date;
    private LocalDate registrationDeadline;
    private Double minCgpa;

    @ElementCollection
    private List<String> allowedDepartments = new ArrayList<>();

    private Integer graduationYear = 2026;

    @ElementCollection
    private List<Long> jobIds = new ArrayList<>();

    private String status = "UPCOMING"; // UPCOMING, ONGOING, COMPLETED

    @ElementCollection
    private List<Long> registeredStudentIds = new ArrayList<>();

    @ElementCollection
    private List<Long> shortlistedStudentIds = new ArrayList<>();

    private LocalDateTime createdAt = LocalDateTime.now();
}
