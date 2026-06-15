package com.studyflow.domain.teacher.repository;

import com.studyflow.domain.teacher.entity.TeacherProfile;
import com.studyflow.domain.user.enums.Gender;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TeacherProfileRepository extends JpaRepository<TeacherProfile, Long> {

    // 선생님 목록 검색/필터 — 선생님 찾기 페이지용
    //  · keyword     : 이름 부분 일치 (서비스에서 !, %, _ escape 처리 후 전달, ESCAPE '!')
    //  · gender      : 성별 (null이면 무시)
    //  · birthFrom/To: 만 나이 범위를 출생일 범위로 변환한 값 (null이면 해당 경계 무시)
    //  · regions     : 활동 지역(address) 정확 일치 — 빈 목록이면 regionsEmpty=true로 무시
    //  · universities: 대학교(career) 정확 일치 — 빈 목록이면 universitiesEmpty=true로 무시
    //  · subjectIds  : 전문 과목 중 하나라도 포함하면 노출(OR) — 빈 목록이면 subjectsEmpty=true로 무시
    // null/빈 목록 파라미터는 (:xxxEmpty = true OR ...) 패턴으로 조건에서 제외됩니다.
    // 과목은 컬렉션 조인이라 DISTINCT로 중복 행을 제거합니다 (specialtySubjects는 fetch하지 않아 페이지네이션 안전).
    @Query(value =
           "SELECT DISTINCT tp FROM TeacherProfile tp " +
           "JOIN FETCH tp.user u " +
           "LEFT JOIN tp.specialtySubjects s " +
           "WHERE u.isDeleted = 0 AND u.isActive = true " +
           "AND (:keyword IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!') " +
           "AND (:gender IS NULL OR u.gender = :gender) " +
           "AND (:birthFrom IS NULL OR u.birthDate >= :birthFrom) " +
           "AND (:birthTo IS NULL OR u.birthDate <= :birthTo) " +
           "AND (:regionsEmpty = true OR tp.address IN :regions) " +
           "AND (:universitiesEmpty = true OR tp.career IN :universities) " +
           "AND (:subjectsEmpty = true OR s.id IN :subjectIds)",
           countQuery =
           "SELECT COUNT(DISTINCT tp) FROM TeacherProfile tp " +
           "JOIN tp.user u " +
           "LEFT JOIN tp.specialtySubjects s " +
           "WHERE u.isDeleted = 0 AND u.isActive = true " +
           "AND (:keyword IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!') " +
           "AND (:gender IS NULL OR u.gender = :gender) " +
           "AND (:birthFrom IS NULL OR u.birthDate >= :birthFrom) " +
           "AND (:birthTo IS NULL OR u.birthDate <= :birthTo) " +
           "AND (:regionsEmpty = true OR tp.address IN :regions) " +
           "AND (:universitiesEmpty = true OR tp.career IN :universities) " +
           "AND (:subjectsEmpty = true OR s.id IN :subjectIds)")
    Page<TeacherProfile> findAllWithUserFiltered(
            @Param("keyword") String keyword,
            @Param("gender") Gender gender,
            @Param("birthFrom") LocalDate birthFrom,
            @Param("birthTo") LocalDate birthTo,
            @Param("regionsEmpty") boolean regionsEmpty,
            @Param("regions") List<String> regions,
            @Param("universitiesEmpty") boolean universitiesEmpty,
            @Param("universities") List<String> universities,
            @Param("subjectsEmpty") boolean subjectsEmpty,
            @Param("subjectIds") List<Long> subjectIds,
            Pageable pageable);

    // 선생님 상세 조회 — user JOIN FETCH
    @Query("SELECT tp FROM TeacherProfile tp " +
           "JOIN FETCH tp.user u " +
           "WHERE tp.id = :id AND u.isDeleted = 0 AND u.isActive = true")
    Optional<TeacherProfile> findWithUserById(@Param("id") Long id);

    // 로그인한 선생님의 프로필 조회 — 수업 생성 시 teacherProfile 참조용
    Optional<TeacherProfile> findByUserId(Long userId);
}
