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

        if (userId == null) {
            // Fallback: try finding the first available student or return an empty 404
            return studentRepository.findAll().stream().findFirst()
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }

        return studentRepository.findByUserId(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    // If user exists but has no profile yet, return a blank template instead of failing with 400
                    return userRepository.findById(userId)
                            .map(user -> {
                                Student newStudent = new Student();
                                newStudent.setUser(user);
                                newStudent.setUserId(user.getId());
                                return ResponseEntity.ok(studentRepository.save(newStudent));
                            })
                            .orElse(ResponseEntity.notFound().build());
                });
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
            // Auto-create or save if not yet existing
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
                    // Automatically provision a student entity for this user if one doesn't exist
                    User user = userRepository.findById(userId).orElse(null);
                    student = new Student();
                    student.setUser(user);
                    student.setUserId(userId);
                    student = studentRepository.save(student);
                }
            } else {
                student = studentRepository.findAll()
                        .stream()
                        .findFirst()
                        .orElse(null);
            }

            if (student == null) {
                return ResponseEntity.badRequest().body(
                        Map.of("message", "Student profile not found.")
                );
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
            // Use relative path so it functions both locally and on Railway production
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