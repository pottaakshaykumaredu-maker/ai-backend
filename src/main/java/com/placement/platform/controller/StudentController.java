package com.placement.platform.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.placement.platform.entity.Notification;
import com.placement.platform.entity.Student;
import com.placement.platform.entity.User;
import com.placement.platform.repository.NotificationRepository;
import com.placement.platform.repository.StudentRepository;
import com.placement.platform.repository.UserRepository;
import com.placement.platform.service.ResumeTextExtractionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StudentController {

    private final StudentRepository studentRepository;
    private final NotificationRepository notificationRepository;
    private final ResumeTextExtractionService resumeTextExtractionService;
    private final UserRepository userRepository;

    private final Path resumeDirectory =
            Paths.get("uploads", "resumes")
                    .toAbsolutePath()
                    .normalize();

    @GetMapping
    public ResponseEntity<List<Student>> getAllStudents() {
        return ResponseEntity.ok(studentRepository.findAll());
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(
            @RequestParam(required = false) Long userId) {

        if (userId != null) {
            // 1. Try finding existing student by userId
            Student student = studentRepository.findByUserId(userId).orElse(null);
            if (student != null) {
                return ResponseEntity.ok(student);
            }

            // 2. If not found, provision a new Student record immediately
            Student newStudent = new Student();
            newStudent.setUserId(userId);
            newStudent.setCgpa(0.0);
            
            // Attach user entity if available
            userRepository.findById(userId).ifPresent(newStudent::setUser);

            return ResponseEntity.ok(studentRepository.save(newStudent));
        }

        // Fallback when no userId is passed: find any student or create a default one
        Student fallback = studentRepository.findAll().stream().findFirst().orElseGet(() -> {
            Student s = new Student();
            s.setCgpa(0.0);
            return studentRepository.save(s);
        });

        return ResponseEntity.ok(fallback);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestBody Student updatedStudent) {

        Student targetStudent = null;

        if (updatedStudent.getId() != null) {
            targetStudent = studentRepository.findById(updatedStudent.getId()).orElse(null);
        } else if (updatedStudent.getUserId() != null) {
            targetStudent = studentRepository.findByUserId(updatedStudent.getUserId()).orElse(null);
        }

        if (targetStudent == null) {
            targetStudent = updatedStudent;
        } else {
            targetStudent.setPhone(updatedStudent.getPhone());
            targetStudent.setDateOfBirth(updatedStudent.getDateOfBirth());
            targetStudent.setGender(updatedStudent.getGender());
            targetStudent.setCollege(updatedStudent.getCollege());
            targetStudent.setDepartment(updatedStudent.getDepartment());
            targetStudent.setGraduationYear(updatedStudent.getGraduationYear());
            targetStudent.setCgpa(updatedStudent.getCgpa());
            targetStudent.setLocation(updatedStudent.getLocation());
            targetStudent.setBio(updatedStudent.getBio());
            targetStudent.setGithubUrl(updatedStudent.getGithubUrl());
            targetStudent.setLinkedinUrl(updatedStudent.getLinkedinUrl());
            targetStudent.setPortfolioUrl(updatedStudent.getPortfolioUrl());

            if (updatedStudent.getSkills() != null) {
                targetStudent.setSkills(updatedStudent.getSkills());
            }
        }

        return ResponseEntity.ok(studentRepository.save(targetStudent));
    }

    @PostMapping(
            value = "/resume/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> uploadResume(
            @RequestParam("resume") MultipartFile file,
            @RequestParam(required = false) Long userId) {

        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        Map.of("message", "Please select a PDF resume.")
                );
            }

            String originalFileName = file.getOriginalFilename();

            if (originalFileName == null ||
                    !originalFileName.toLowerCase().endsWith(".pdf")) {
                return ResponseEntity.badRequest().body(
                        Map.of("message", "Only PDF resumes are allowed.")
                );
            }

            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(
                        Map.of("message", "Resume must be smaller than 10 MB.")
                );
            }

            Student student = null;

            if (userId != null) {
                student = studentRepository.findByUserId(userId).orElse(null);
                if (student == null) {
                    student = new Student();
                    student.setUserId(userId);
                    student.setCgpa(0.0);
                    userRepository.findById(userId).ifPresent(student::setUser);
                    student = studentRepository.save(student);
                }
            } else {
                student = studentRepository.findAll()
                        .stream()
                        .findFirst()
                        .orElseGet(() -> {
                            Student s = new Student();
                            s.setCgpa(0.0);
                            return studentRepository.save(s);
                        });
            }

            Files.createDirectories(resumeDirectory);

            String storedFileName = UUID.randomUUID() + ".pdf";

            Path targetFile = resumeDirectory
                    .resolve(storedFileName)
                    .normalize();

            if (!targetFile.startsWith(resumeDirectory)) {
                return ResponseEntity.badRequest().body(
                        Map.of("message", "Invalid file path.")
                );
            }

            Files.copy(
                    file.getInputStream(),
                    targetFile,
                    StandardCopyOption.REPLACE_EXISTING
            );

            String resumeText = "";
            try {
                resumeText = resumeTextExtractionService.extractText(file);
            } catch (Exception ex) {
                System.err.println("Resume text extraction warning: " + ex.getMessage());
            }

            student.setResumeFileName(originalFileName);
            student.setResumeUrl("/api/students/resume/file/" + storedFileName);
            student.setResumeUploadedAt(LocalDateTime.now());
            student.setResumeText(resumeText);

            Student savedStudent = studentRepository.save(student);

            if (student.getUser() != null) {
                try {
                    Notification notification = new Notification();
                    notification.setUserId(student.getUser().getId());
                    notification.setMessage("Your resume was uploaded successfully.");
                    notification.setType("RESUME");
                    notificationRepository.save(notification);
                } catch (Exception ignored) {
                }
            }

            return ResponseEntity.ok(
                    Map.of(
                            "message", "Resume uploaded successfully",
                            "fileName", savedStudent.getResumeFileName() != null ? savedStudent.getResumeFileName() : originalFileName,
                            "uploadedAt", savedStudent.getResumeUploadedAt() != null ? savedStudent.getResumeUploadedAt().toString() : LocalDateTime.now().toString(),
                            "resumeUrl", savedStudent.getResumeUrl() != null ? savedStudent.getResumeUrl() : "",
                            "resumeTextLength", savedStudent.getResumeText() == null ? 0 : savedStudent.getResumeText().length()
                    )
            );

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "message", "Failed to upload or read resume.",
                            "error", e.getMessage() == null ? "Unknown error" : e.getMessage()
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "message", "Resume upload failed.",
                            "error", e.getMessage() == null ? "Unknown error" : e.getMessage()
                    )
            );
        }
    }

    @GetMapping("/resume/file/{fileName}")
    public ResponseEntity<Resource> getResumeFile(
            @PathVariable String fileName) {

        try {
            Path filePath = resumeDirectory
                    .resolve(fileName)
                    .normalize();

            if (!filePath.startsWith(resumeDirectory)) {
                return ResponseEntity.badRequest().build();
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + resource.getFilename() + "\""
                    )
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}