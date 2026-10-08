package com.placement.platform.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.placement.platform.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByCollegeId(String collegeId);

    Boolean existsByEmail(String email);

    Boolean existsByUsername(String username);

    Boolean existsByCollegeId(String collegeId);
}