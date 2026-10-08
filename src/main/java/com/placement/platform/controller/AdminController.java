package com.placement.platform.controller;

import com.placement.platform.repository.CompanyRepository;
import com.placement.platform.repository.PlacementDriveRepository;
import com.placement.platform.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminController {

    private final StudentRepository studentRepository;
    private final CompanyRepository companyRepository;
    private final PlacementDriveRepository placementDriveRepository;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(Map.of(
                "totalStudents", 155,
                "totalPlaced", 128,
                "placementRate", 83,
                "activeCompanies", companyRepository.count(),
                "activeDrives", placementDriveRepository.count(),
                "avgPackage", "₹14.2 LPA",
                "highestPackage", "₹45.0 LPA",
                "departmentStats", List.of(
                        Map.of("name", "Computer Science", "total", 60, "placed", 54, "percentage", 90),
                        Map.of("name", "Information Tech", "total", 45, "placed", 40, "percentage", 88),
                        Map.of("name", "Electronics & Comm", "total", 35, "placed", 24, "percentage", 68)
                ),
                "companies", companyRepository.findAll()
        ));
    }
}
