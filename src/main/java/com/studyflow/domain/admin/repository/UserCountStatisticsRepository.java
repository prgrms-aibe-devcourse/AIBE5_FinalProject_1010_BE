package com.studyflow.domain.admin.repository;

import com.studyflow.domain.admin.entity.UserCountStatistics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface UserCountStatisticsRepository extends JpaRepository<UserCountStatistics, Long> {

    Optional<UserCountStatistics> findByDate(LocalDate date);
}
