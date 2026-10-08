package com.placement.platform.controller;

import com.placement.platform.entity.Application;
import com.placement.platform.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ApplicationController {

    private final ApplicationRepository applicationRepository;

    @GetMapping
    public ResponseEntity<List<Application>> getApplications(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long jobId) {
        if (studentId != null) return ResponseEntity.ok(applicationRepository.findByStudentId(studentId));
        if (jobId != null) return ResponseEntity.ok(applicationRepository.findByJobId(jobId));
        return ResponseEntity.ok(applicationRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Application> apply(@RequestBody Application application) {
        application.setStatus("APPLIED");
        return ResponseEntity.ok(applicationRepository.save(application));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Application> updateStatus(@PathVariable Long id, @RequestBody Application update) {
        return applicationRepository.findById(id).map(app -> {
            app.setStatus(update.getStatus());
            return ResponseEntity.ok(applicationRepository.save(app));
        }).orElse(ResponseEntity.notFound().build());
    }
}
