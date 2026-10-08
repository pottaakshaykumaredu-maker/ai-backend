package com.placement.platform.controller;

import com.placement.platform.entity.Interview;
import com.placement.platform.repository.InterviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InterviewController {

    private final InterviewRepository interviewRepository;

    @GetMapping
    public ResponseEntity<List<Interview>> getInterviews(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long jobId) {
        if (studentId != null) return ResponseEntity.ok(interviewRepository.findByStudentId(studentId));
        if (jobId != null) return ResponseEntity.ok(interviewRepository.findByJobId(jobId));
        return ResponseEntity.ok(interviewRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Interview> scheduleInterview(@RequestBody Interview interview) {
        interview.setStatus("SCHEDULED");
        return ResponseEntity.ok(interviewRepository.save(interview));
    }
}
