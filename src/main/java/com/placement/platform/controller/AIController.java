package com.placement.platform.controller;

import com.placement.platform.ai.AIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AIController {

    private final AIService aiService;

    @PostMapping(value = "/resume-analyze", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> analyzeResume(@RequestBody Map<String, String> payload) {
        String text = payload.getOrDefault("resumeText", "");
        return ResponseEntity.ok(aiService.analyzeResume(text));
    }

    @PostMapping(value = "/job-match", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> matchJob(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(aiService.matchJob(payload.toString(), ""));
    }

    @PostMapping(value = "/skill-gap", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> skillGap(@RequestBody Map<String, Object> payload) {
        String role = (String) payload.getOrDefault("targetRole", "Senior Backend Engineer");
        return ResponseEntity.ok(aiService.analyzeSkillGap("", role));
    }

    @PostMapping(value = "/interview-prep", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> interviewPrep(@RequestBody Map<String, String> payload) {
        String job = payload.getOrDefault("jobTitle", "Software Engineer");
        String company = payload.getOrDefault("companyName", "TechCorp");
        String type = payload.getOrDefault("interviewType", "Technical");
        return ResponseEntity.ok(aiService.generateInterviewQuestions(job, company, type));
    }

    @PostMapping(value = "/mock-interview-chat", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> mockChat(@RequestBody Map<String, Object> payload) {
        String reply = aiService.mockInterviewChat(payload.toString(), "SDE");
        return ResponseEntity.ok(Map.of("reply", reply));
    }
}
