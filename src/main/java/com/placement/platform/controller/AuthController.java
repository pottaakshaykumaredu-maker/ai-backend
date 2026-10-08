package com.placement.platform.controller;

import com.placement.platform.dto.ApiResponse;
import com.placement.platform.dto.AuthResponse;
import com.placement.platform.dto.LoginRequest;
import com.placement.platform.dto.RegisterRequest;
import com.placement.platform.entity.Recruiter;
import com.placement.platform.entity.User;
import com.placement.platform.repository.RecruiterRepository;
import com.placement.platform.repository.UserRepository;
import com.placement.platform.security.JwtUtils;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final RecruiterRepository recruiterRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        if (isBlank(request.getEmail()) || isBlank(request.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Email and password are required"));
        }

        String email = request.getEmail().trim();

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null
                || !passwordEncoder.matches(
                        request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Invalid email or password"));
        }

        if ("ROLE_RECRUITER".equals(user.getRole())
                && recruiterRepository.findByUserId(user.getId()).isEmpty()) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(
                            "Recruiter profile is missing. Contact the administrator."));
        }

        String token = jwtUtils.generateToken(
                user.getEmail(), user.getRole());

        return ResponseEntity.ok(
                AuthResponse.builder()
                        .token(token)
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .build());
    }

    @Transactional
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {

        if (isBlank(request.getName())
                || isBlank(request.getEmail())
                || isBlank(request.getUsername())
                || isBlank(request.getCollegeId())
                || isBlank(request.getPassword())
                || isBlank(request.getRole())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(
                            "Name, email, username, college ID, password and role are required"));
        }

        String name = request.getName().trim();
        String email = request.getEmail().trim();
        String username = request.getUsername().trim();
        String collegeId = request.getCollegeId().trim();
        String role = normalizeRole(request.getRole());

        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Please enter a valid email address"));
        }

        if (request.getPassword().length() < 8
                || !request.getPassword().matches("^[A-Z].*")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(
                            "Password must contain at least 8 characters and start with an uppercase letter"));
        }

        if (!role.equals("ROLE_STUDENT")
                && !role.equals("ROLE_RECRUITER")
                && !role.equals("ROLE_ADMIN")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid role selected"));
        }

        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Email is already registered"));
        }

        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Username is already registered"));
        }

        if (userRepository.existsByCollegeId(collegeId)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("College ID is already registered"));
        }

        User newUser = User.builder()
                .name(name)
                .email(email)
                .username(username)
                .collegeId(collegeId)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .phone(isBlank(request.getPhone())
                        ? null : request.getPhone().trim())
                .build();

        newUser = userRepository.save(newUser);

        if ("ROLE_RECRUITER".equals(role)) {
            Recruiter recruiter = Recruiter.builder()
                    .user(newUser)
                    .position("Recruiter")
                    .build();

            recruiterRepository.save(recruiter);
        }

        // Registration does not automatically log the user in on the frontend.
        // The frontend will return to the login page after this response.
        String token = jwtUtils.generateToken(
                newUser.getEmail(), newUser.getRole());

        return ResponseEntity.ok(
                AuthResponse.builder()
                        .token(token)
                        .id(newUser.getId())
                        .name(newUser.getName())
                        .email(newUser.getEmail())
                        .role(newUser.getRole())
                        .build());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalizeRole(String role) {
        return role == null ? "" : role.trim().toUpperCase();
    }
}