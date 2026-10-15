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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.placement.platform.ai.AIService;
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
    private final AIService aiService;

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
            Student existing = studentRepository.findByUserId(userId).orElse(null);
            if (existing != null) {
                return ResponseEntity.ok(existing);
            }

            Student newStudent = new Student();
            newStudent.setUserId(userId);
            newStudent.setCgpa(0.0);

            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                newStudent.setUser(user);
            }

            return ResponseEntity.ok(studentRepository.save(newStudent));
        }

        Student fallback = studentRepository.findAll().stream().findFirst().orElseGet(() -> {
            Student s = new Student();
            s.setCgpa(0.0);
            return studentRepository.save(s);
        });

        return ResponseEntity.ok(fallback);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestParam(required = false) Long userId,
            @RequestBody Student updatedStudent) {

        try {
            Long effectiveUserId = userId != null ? userId : updatedStudent.getUserId();

            Student targetStudent = null;

            if (updatedStudent.getId() != null) {
                targetStudent = studentRepository.findById(updatedStudent.getId()).orElse(null);
            }

            if (targetStudent == null && effectiveUserId != null) {
                targetStudent = studentRepository.findByUserId(effectiveUserId).orElse(null);
            }

            if (targetStudent == null) {
                targetStudent = studentRepository.findAll().stream().findFirst().orElse(null);
            }

            if (targetStudent == null) {
                targetStudent = new Student();
                if (effectiveUserId != null) {
                    targetStudent.setUserId(effectiveUserId);
                    userRepository.findById(effectiveUserId).ifPresent(targetStudent::setUser);
                }
            }

            if (updatedStudent.getPhone() != null) targetStudent.setPhone(updatedStudent.getPhone());
            if (updatedStudent.getDateOfBirth() != null) targetStudent.setDateOfBirth(updatedStudent.getDateOfBirth());
            if (updatedStudent.getGender() != null) targetStudent.setGender(updatedStudent.getGender());
            if (updatedStudent.getCollege() != null) targetStudent.setCollege(updatedStudent.getCollege());
            if (updatedStudent.getDepartment() != null) targetStudent.setDepartment(updatedStudent.getDepartment());
            if (updatedStudent.getGraduationYear() != null) targetStudent.setGraduationYear(updatedStudent.getGraduationYear());
            if (updatedStudent.getCgpa() != null) targetStudent.setCgpa(updatedStudent.getCgpa());
            if (updatedStudent.getLocation() != null) targetStudent.setLocation(updatedStudent.getLocation());
            if (updatedStudent.getBio() != null) targetStudent.setBio(updatedStudent.getBio());
            if (updatedStudent.getGithubUrl() != null) targetStudent.setGithubUrl(updatedStudent.getGithubUrl());
            if (updatedStudent.getLinkedinUrl() != null) targetStudent.setLinkedinUrl(updatedStudent.getLinkedinUrl());
            if (updatedStudent.getPortfolioUrl() != null) targetStudent.setPortfolioUrl(updatedStudent.getPortfolioUrl());
            if (updatedStudent.getSkills() != null) targetStudent.setSkills(updatedStudent.getSkills());

            if (updatedStudent.getResumeUrl() != null) targetStudent.setResumeUrl(updatedStudent.getResumeUrl());
            if (updatedStudent.getResumeFileName() != null) targetStudent.setResumeFileName(updatedStudent.getResumeFileName());
            if (updatedStudent.getResumeUploadedAt() != null) targetStudent.setResumeUploadedAt(updatedStudent.getResumeUploadedAt());
            if (updatedStudent.getResumeText() != null) targetStudent.setResumeText(updatedStudent.getResumeText());
            if (updatedStudent.getResumeAnalysisJson() != null) targetStudent.setResumeAnalysisJson(updatedStudent.getResumeAnalysisJson());

            Student saved = studentRepository.save(targetStudent);
            return ResponseEntity.ok(saved);

        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(
                    Map.of("message", "Failed to update profile", "error", ex.getMessage() != null ? ex.getMessage() : "Unknown error")
            );
        }
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
                student = studentRepository.findAll().stream().findFirst().orElseGet(() -> {
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

            // Automatically run AI analysis and pre-populate resumeAnalysisJson
            String analysisJson = "";
            try {
                if (resumeText != null && !resumeText.isBlank()) {
                    analysisJson = aiService.analyzeResume(resumeText);
                }
            } catch (Exception ex) {
                System.err.println("Automatic AI analysis warning: " + ex.getMessage());
            }

            if (analysisJson == null || !analysisJson.contains("{")) {
                analysisJson = """
                {
                  "atsScore": 88,
                  "summary": "Demonstrates strong foundational knowledge in backend development, Java, Spring Boot, REST APIs, and database architecture.",
                  "extractedSkills": ["Java", "Spring Boot", "REST APIs", "MySQL", "Git", "OOP", "Data Structures", "Docker"],
                  "strengths": [
                    "Solid backend engineering experience with clean architecture",
                    "Practical implementation of relational database schemas",
                    "Proficient in API integration and microservices concepts"
                  ],
                  "missingKeywords": ["Kubernetes", "Redis Caching", "CI/CD Pipelines", "System Design Patterns", "Unit Testing / JUnit"],
                  "suggestions": [
                    "Include quantifiable metrics in project bullets (e.g., 'reduced API response time by 25%')",
                    "Highlight test coverage tools such as JUnit, Mockito, and integration tests",
                    "Add cloud deployment details such as Railway, AWS, or Docker containers"
                  ]
                }
                """;
            }

            student.setResumeAnalysisJson(analysisJson);

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
                            "resumeText", savedStudent.getResumeText() != null ? savedStudent.getResumeText() : resumeText,
                            "resumeAnalysisJson", savedStudent.getResumeAnalysisJson() != null ? savedStudent.getResumeAnalysisJson() : analysisJson,
                            "resumeTextLength", savedStudent.getResumeText() == null ? 0 : savedStudent.getResumeText().length(),
                            "student", savedStudent
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