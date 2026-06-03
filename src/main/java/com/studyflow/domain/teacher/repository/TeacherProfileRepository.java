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

    // 선생님 목록 검색/필터 — keyword(이름 포함), minNaegong(내공 점수 하한)
    // null 파라미터는 조건에서 제외됩니다 (IS NULL OR ...)
    @Query(value =
           "SELECT tp FROM TeacherProfile tp " +
           "JOIN FETCH tp.user u " +
           "WHERE u.isDeleted = 0 AND u.isActive = true " +
           "AND (:keyword IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:minNaegong IS NULL OR tp.naegongScore >= :minNaegong)",
           countQuery =
           "SELECT COUNT(tp) FROM TeacherProfile tp " +
           "JOIN tp.user u " +
           "WHERE u.isDeleted = 0 AND u.isActive = true " +
           "AND (:keyword IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:minNaegong IS NULL OR tp.naegongScore >= :minNaegong)")
    Page<TeacherProfile> findAllWithUserFiltered(
            @Param("keyword") String keyword,
            @Param("minNaegong") Integer minNaegong,
            Pageable pageable);

    // 선생님 상세 조회 — user JOIN FETCH
    @Query("SELECT tp FROM TeacherProfile tp " +
           "JOIN FETCH tp.user u " +
           "WHERE tp.id = :id AND u.isDeleted = 0 AND u.isActive = true")
    Optional<TeacherProfile> findWithUserById(@Param("id") Long id);

    // 로그인한 선생님의 프로필 조회 — 수업 생성 시 teacherProfile 참조용
    Optional<TeacherProfile> findByUserId(Long userId);
}
