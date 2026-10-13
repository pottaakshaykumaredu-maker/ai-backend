package com.placement.platform.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.placement.platform.entity.Student;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    @Query("SELECT s FROM Student s WHERE (s.user IS NOT NULL AND s.user.id = :userId) OR s.id = :userId")
    Optional<Student> findByUserId(@Param("userId") Long userId);

    Optional<Student> findByUser_Id(Long userId);
}