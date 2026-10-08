package com.placement.platform.repository;

import com.placement.platform.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, Long> {
    List<Interview> findByStudentId(Long studentId);
    List<Interview> findByJobId(Long jobId);
    List<Interview> findByStatus(String status);
}
