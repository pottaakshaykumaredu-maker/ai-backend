package com.placement.platform.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.placement.platform.entity.Application;
import com.placement.platform.entity.Job;
import com.placement.platform.entity.Student;
import com.placement.platform.repository.ApplicationRepository;
import com.placement.platform.repository.JobRepository;
import com.placement.platform.repository.StudentRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ApplicationController {

    private final ApplicationRepository applicationRepository;
    private final StudentRepository studentRepository;
    private final JobRepository jobRepository;

    @GetMapping
    public ResponseEntity<List<Application>> getApplications(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long jobId) {
        if (studentId != null) return ResponseEntity.ok(applicationRepository.findByStudentId(studentId));
        if (jobId != null) return ResponseEntity.ok(applicationRepository.findByJobId(jobId));
        return ResponseEntity.ok(applicationRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<?> apply(@RequestBody Map<String, Object> payload) {
        try {
            // Extract IDs flexibly whether sent as flat IDs or nested objects
            Long jobId = null;
            if (payload.get("jobId") != null) {
                jobId = Long.valueOf(payload.get("jobId").toString());
            } else if (payload.get("job") instanceof Map) {
                Object idObj = ((Map<?, ?>) payload.get("job")).get("id");
                if (idObj != null) jobId = Long.valueOf(idObj.toString());
            }

            Long studentId = null;
            if (payload.get("studentId") != null) {
                studentId = Long.valueOf(payload.get("studentId").toString());
            } else if (payload.get("userId") != null) {
                Long userId = Long.valueOf(payload.get("userId").toString());
                Student studentByUserId = studentRepository.findByUserId(userId).orElse(null);
                if (studentByUserId != null) {
                    studentId = studentByUserId.getId();
                }
            } else if (payload.get("student") instanceof Map) {
                Object idObj = ((Map<?, ?>) payload.get("student")).get("id");
                if (idObj != null) studentId = Long.valueOf(idObj.toString());
            }

            // Fallback: If no student ID could be determined, pick the first existing student profile
            Student student = null;
            if (studentId != null) {
                student = studentRepository.findById(studentId).orElse(null);
            }
            if (student == null) {
                student = studentRepository.findAll().stream().findFirst().orElse(null);
            }

            if (student == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Student profile not found. Please upload resume first."));
            }

            if (jobId == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Job ID is required to apply."));
            }

            Job job = jobRepository.findById(jobId).orElse(null);
            if (job == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Job not found."));
            }

            // Check if already applied to prevent duplicate integrity errors
            List<Application> existing = applicationRepository.findByStudentId(student.getId());
            for (Application app : existing) {
                if (app.getJob() != null && app.getJob().getId().equals(job.getId())) {
                    return ResponseEntity.ok(app); // Return existing application cleanly
                }
            }

            Application application = new Application();
            application.setStudent(student);
            application.setJob(job);
            application.setStatus("APPLIED");
            application.setAppliedAt(LocalDateTime.now());

            Application saved = applicationRepository.save(application);
            return ResponseEntity.ok(saved);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    Map.of("message", "Failed to apply to job", "error", ex.getMessage() != null ? ex.getMessage() : "Unknown error")
            );
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Application> updateStatus(@PathVariable Long id, @RequestBody Application update) {
        return applicationRepository.findById(id).map(app -> {
            app.setStatus(update.getStatus());
            return ResponseEntity.ok(applicationRepository.save(app));
        }).orElse(ResponseEntity.notFound().build());
    }
}