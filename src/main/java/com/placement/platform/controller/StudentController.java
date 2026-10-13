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

            // Copy profile fields safely
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

            // Copy resume & AI analysis fields
            if (updatedStudent.getResumeUrl() != null) targetStudent.setResumeUrl(updatedStudent.getResumeUrl());
            if (updatedStudent.getResumeFileName() != null) targetStudent.setResumeFileName(updatedStudent.getResumeFileName());
            if (updatedStudent.getResumeUploadedAt() != null) targetStudent.setResumeUploadedAt(updatedStudent.getResumeUploadedAt());
            if (updatedStudent.getResumeText() != null) targetStudent.setResumeText(updatedStudent.getResumeText());
            if (updatedStudent.getResumeAnalysisJson() != null) targetStudent.setResumeAnalysisJson(updatedStudent.getResumeAnalysisJson());

            Student saved = studentRepository.save(targetStudent);
            return ResponseEntity.ok(saved);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    Map.of("message", "Failed to update profile", "error", ex.getMessage() != null ? ex.getMessage() : "Unknown error")
            );
        }
    }