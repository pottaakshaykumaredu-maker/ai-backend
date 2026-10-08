package com.placement.platform.controller;

import com.placement.platform.entity.PlacementDrive;
import com.placement.platform.repository.PlacementDriveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/drives")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PlacementDriveController {

    private final PlacementDriveRepository placementDriveRepository;

    @GetMapping
    public ResponseEntity<List<PlacementDrive>> getAllDrives() {
        return ResponseEntity.ok(placementDriveRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<PlacementDrive> createDrive(@RequestBody PlacementDrive drive) {
        drive.setStatus("UPCOMING");
        return ResponseEntity.ok(placementDriveRepository.save(drive));
    }

    @PostMapping("/{id}/register")
    public ResponseEntity<PlacementDrive> registerForDrive(@PathVariable Long id, @RequestBody Map<String, Long> payload) {
        Long studentId = payload.getOrDefault("studentId", 1L);
        return placementDriveRepository.findById(id).map(drive -> {
            if (!drive.getRegisteredStudentIds().contains(studentId)) {
                drive.getRegisteredStudentIds().add(studentId);
            }
            return ResponseEntity.ok(placementDriveRepository.save(drive));
        }).orElse(ResponseEntity.notFound().build());
    }
}
