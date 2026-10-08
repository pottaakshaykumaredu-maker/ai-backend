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
import com.placement.platform.repository.NotificationRepository;
import com.placement.platform.repository.StudentRepository;
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

    private final Path resumeDirectory =
            Paths.get("uploads", "resumes")
                    .toAbsolutePath()
                    .normalize();

    @GetMapping
    public ResponseEntity<List<Student>> getAllStudents() {

        return ResponseEntity.ok(
                studentRepository.findAll()
        );
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(
            @RequestParam(required = false) Long userId) {

        if (userId == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "userId is required")
            );
        }

        return studentRepository.findByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestBody Student updatedStudent) {

        if (updatedStudent.getId() == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Student id is required")
            );
        }

        return studentRepository.findById(updatedStudent.getId())
                .map(student -> {

                    student.setPhone(updatedStudent.getPhone());
                    student.setDateOfBirth(updatedStudent.getDateOfBirth());
                    student.setGender(updatedStudent.getGender());
                    student.setCollege(updatedStudent.getCollege());
                    student.setDepartment(updatedStudent.getDepartment());
                    student.setGraduationYear(
                            updatedStudent.getGraduationYear()
                    );
                    student.setCgpa(updatedStudent.getCgpa());
                    student.setLocation(updatedStudent.getLocation());
                    student.setBio(updatedStudent.getBio());
                    student.setGithubUrl(updatedStudent.getGithubUrl());
                    student.setLinkedinUrl(updatedStudent.getLinkedinUrl());
                    student.setPortfolioUrl(updatedStudent.getPortfolioUrl());

                    if (updatedStudent.getSkills() != null) {
                        student.setSkills(updatedStudent.getSkills());
                    }

                    return ResponseEntity.ok(
                            studentRepository.save(student)
                    );
                })
                .orElse(ResponseEntity.notFound().build());
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
                        Map.of(
                                "message",
                                "Please select a PDF resume."
                        )
                );
            }

            String originalFileName = file.getOriginalFilename();

            if (originalFileName == null ||
                    !originalFileName.toLowerCase().endsWith(".pdf")) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "message",
                                "Only PDF resumes are allowed."
                        )
                );
            }

            if (file.getSize() > 10 * 1024 * 1024) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "message",
                                "Resume must be smaller than 10 MB."
                        )
                );
            }

            Student student;

            if (userId != null) {

                student = studentRepository
                        .findByUserId(userId)
                        .orElse(null);

            } else {

                student = studentRepository
                        .findAll()
                        .stream()
                        .findFirst()
                        .orElse(null);
            }

            if (student == null) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "message",
                                "Student profile not found."
                        )
                );
            }

            Files.createDirectories(resumeDirectory);

            String storedFileName =
                    UUID.randomUUID() + ".pdf";

            Path targetFile =
                    resumeDirectory
                            .resolve(storedFileName)
                            .normalize();

            if (!targetFile.startsWith(resumeDirectory)) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "message",
                                "Invalid file path."
                        )
                );
            }

            Files.copy(
                    file.getInputStream(),
                    targetFile,
                    StandardCopyOption.REPLACE_EXISTING
            );

            String resumeText =
                    resumeTextExtractionService.extractText(file);

            student.setResumeFileName(originalFileName);

            student.setResumeUrl(
                    "http://localhost:9090/api/students/resume/file/"
                            + storedFileName
            );

            student.setResumeUploadedAt(
                    LocalDateTime.now()
            );

            student.setResumeText(resumeText);

            Student savedStudent =
                    studentRepository.save(student);

            if (student.getUser() != null) {

                try {

                    Notification notification =
                            new Notification();

                    notification.setUserId(
                            student.getUser().getId()
                    );

                    notification.setMessage(
                            "Your resume was uploaded successfully."
                    );

                    notification.setType(
                            "RESUME"
                    );

                    notificationRepository.save(notification);

                } catch (Exception ignored) {
                }
            }

            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            "Resume uploaded successfully",

                            "fileName",
                            savedStudent.getResumeFileName(),

                            "uploadedAt",
                            savedStudent
                                    .getResumeUploadedAt()
                                    .toString(),

                            "resumeUrl",
                            savedStudent.getResumeUrl(),

                            "resumeTextLength",
                            savedStudent.getResumeText() == null
                                    ? 0
                                    : savedStudent
                                            .getResumeText()
                                            .length()
                    )
            );

        } catch (IOException e) {

            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "message",
                            "Failed to upload or read resume.",
                            "error",
                            e.getMessage() == null
                                    ? "Unknown error"
                                    : e.getMessage()
                    )
            );

        } catch (Exception e) {

            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "message",
                            "Resume upload failed.",
                            "error",
                            e.getMessage() == null
                                    ? "Unknown error"
                                    : e.getMessage()
                    )
            );
        }
    }

    @GetMapping("/resume/file/{fileName}")
    public ResponseEntity<Resource> getResumeFile(
            @PathVariable String fileName) {

        try {

            Path filePath =
                    resumeDirectory
                            .resolve(fileName)
                            .normalize();

            if (!filePath.startsWith(resumeDirectory)) {

                return ResponseEntity.badRequest().build();
            }

            Resource resource =
                    new UrlResource(
                            filePath.toUri()
                    );

            if (!resource.exists() ||
                    !resource.isReadable()) {

                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .contentType(
                            MediaType.APPLICATION_PDF
                    )
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" +
                                    resource.getFilename() +
                                    "\""
                    )
                    .body(resource);

        } catch (Exception e) {

            return ResponseEntity
                    .internalServerError()
                    .build();
        }
    }
}