package com.placement.platform.controller;

import com.placement.platform.entity.Job;
import com.placement.platform.repository.JobRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class JobController {

    private final JobRepository jobRepository;

    @GetMapping
    public ResponseEntity<List<Job>> getAllJobs() {

        return ResponseEntity.ok(
                jobRepository.findAll()
        );
    }

    @PostMapping
    public ResponseEntity<Job> createJob(
            @RequestBody Job job) {

        return ResponseEntity.ok(
                jobRepository.save(job)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Job> getJobById(
            @PathVariable Long id) {

        return jobRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(
                        ResponseEntity.notFound().build()
                );
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Job> updateStatus(
            @PathVariable Long id,
            @RequestBody Job update) {

        return jobRepository.findById(id)
                .map(job -> {

                    job.setStatus(
                            update.getStatus()
                    );

                    return ResponseEntity.ok(
                            jobRepository.save(job)
                    );
                })
                .orElse(
                        ResponseEntity.notFound().build()
                );
    }
}