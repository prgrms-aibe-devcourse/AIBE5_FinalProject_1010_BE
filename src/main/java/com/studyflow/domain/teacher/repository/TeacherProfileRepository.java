package com.studyflow.domain.teacher.repository;

import com.studyflow.domain.teacher.entity.TeacherProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TeacherProfileRepository extends JpaRepository<TeacherProfile, Long> {

    // 선생님 목록 페이지네이션 — user JOIN FETCH로 N+1 방지
    // 탈퇴하지 않은 활성 선생님만 조회 (isDeleted=0, isActive=true)
    @Query("SELECT tp FROM TeacherProfile tp " +
           "JOIN FETCH tp.user u " +
           "WHERE u.isDeleted = 0 AND u.isActive = true")
    Page<TeacherProfile> findAllWithUser(Pageable pageable);

    // 선생님 상세 조회 — user JOIN FETCH
    @Query("SELECT tp FROM TeacherProfile tp " +
           "JOIN FETCH tp.user u " +
           "WHERE tp.id = :id AND u.isDeleted = 0 AND u.isActive = true")
    Optional<TeacherProfile> findWithUserById(@Param("id") Long id);

    // 로그인한 선생님의 프로필 조회 — 수업 생성 시 teacherProfile 참조용
    Optional<TeacherProfile> findByUserId(Long userId);
}
