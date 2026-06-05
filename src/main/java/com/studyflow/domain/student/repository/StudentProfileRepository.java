package com.studyflow.domain.student.repository;

import com.studyflow.domain.student.entity.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {
    Optional<StudentProfile> findById(Long id);

    Optional<StudentProfile> findByUserId(Long userId);
}
