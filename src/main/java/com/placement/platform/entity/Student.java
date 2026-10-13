package com.placement.platform.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    // Direct getter/setter for userId to support easy JSON serialization and API lookups
    @Column(name = "user_id_ref")
    private Long userId;

    private String phone;

    private LocalDate dateOfBirth;

    private String gender;

    private String college;

    private String department;

    private Integer graduationYear;

    private Double cgpa;

    private String location;

    @Column(length = 2000)
    private String bio;

    private String githubUrl;

    private String linkedinUrl;

    private String portfolioUrl;

    @ElementCollection
    @CollectionTable(
        name = "student_skills",
        joinColumns = @JoinColumn(name = "student_id")
    )
    @Column(name = "skill")
    @Builder.Default
    private List<String> skills = new ArrayList<>();

    private String resumeUrl;

    private String resumeFileName;

    private LocalDateTime resumeUploadedAt;

    @Column(columnDefinition = "TEXT")
    private String resumeText;

    @Column(columnDefinition = "TEXT")
    private String resumeAnalysisJson;

    @JsonProperty("userId")
    public Long getResolvedUserId() {
        if (userId != null) return userId;
        if (user != null) return user.getId();
        return null;
    }

    @PrePersist
    protected void onCreate() {
        if (skills == null) {
            skills = new ArrayList<>();
        }
        if (userId == null && user != null) {
            userId = user.getId();
        }
    }
}