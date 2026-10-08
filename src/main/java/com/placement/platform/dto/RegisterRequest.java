package com.placement.platform.dto;

import lombok.Data;

@Data
public class RegisterRequest {

    private String name;

    private String email;

    private String username;

    private String collegeId;

    private String password;

    private String role;

    private String phone;

    private String department;

    private String college;

    private Double cgpa;

    private Integer graduationYear;
}