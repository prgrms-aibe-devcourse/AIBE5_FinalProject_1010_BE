package com.studyflow.domain.course.repository;

import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.course.enums.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// JpaSpecificationExecutor: 동적 필터 조건(Specification) 기반 조회 기능 추가
public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {

    // 선생님별 수업 수 일괄 조회 결과를 담는 타입 안전 프로젝션
    interface TeacherCourseCount {
        Long getTeacherProfileId();
        Long getCount();
    }

    // teacherProfile → user, subject 까지 한 번에 페치 — 수업별 페이지 진입마다 사용하므로 N+1 방지용으로 분리
    @Query("SELECT c FROM Course c " +
           "JOIN FETCH c.teacherProfile tp " +
           "JOIN FETCH tp.user " +
           "JOIN FETCH c.subject " +
           "WHERE c.id = :courseId")
    Optional<Course> findWithTeacherAndSubjectById(@Param("courseId") Long courseId);

    // 선생님 상세 페이지 — 해당 선생님의 공개 수업 목록 (subject JOIN FETCH)
    @Query("SELECT c FROM Course c " +
           "JOIN FETCH c.subject " +
           "WHERE c.teacherProfile.id = :teacherProfileId " +
           "AND c.isListed = true " +
           "AND c.status IN :statuses")
    List<Course> findWithSubjectByTeacherProfileId(@Param("teacherProfileId") Long teacherProfileId,
                                                   @Param("statuses") List<CourseStatus> statuses);

    // 선생님 마이페이지 — 본인 수업 목록 (status 필터 선택적 적용, isListed 무관)
    @Query(value = "SELECT c FROM Course c " +
                   "JOIN FETCH c.subject " +
                   "WHERE c.teacherProfile.id = :teacherProfileId " +
                   "AND (:status IS NULL OR c.status = :status)",
           countQuery = "SELECT COUNT(c) FROM Course c " +
                        "WHERE c.teacherProfile.id = :teacherProfileId " +
                        "AND (:status IS NULL OR c.status = :status)")
    Page<Course> findWithSubjectByTeacherProfileIdAndStatus(@Param("teacherProfileId") Long teacherProfileId,
                                                            @Param("status") CourseStatus status,
                                                            Pageable pageable);

    // 여러 선생님의 공개 수업 수 일괄 조회 — 선생님 목록에서 N+1 방지
    // 반환: TeacherCourseCount{ teacherProfileId, count }
    @Query("SELECT c.teacherProfile.id AS teacherProfileId, COUNT(c) AS count " +
           "FROM Course c " +
           "WHERE c.teacherProfile.id IN :teacherProfileIds " +
           "AND c.isListed = true " +
           "AND c.status IN :statuses " +
           "GROUP BY c.teacherProfile.id")
    List<TeacherCourseCount> countCoursesByTeacherProfileIds(@Param("teacherProfileIds") List<Long> teacherProfileIds,
                                                             @Param("statuses") List<CourseStatus> statuses);

    // 검색 필터(Specification) 적용 + teacherProfile → user, subject 한 번에 페치
    // @EntityGraph로 ManyToOne 관계만 페치하기 때문에 컬렉션 페치가 없어 페이지네이션 메모리 경고 없음
    // CourseCardResponse.of()에서 teacherProfile, user, subject에 접근하므로 반드시 함께 페치해야 LazyInitializationException 방지
    @EntityGraph(attributePaths = {"teacherProfile", "teacherProfile.user", "subject"})
    Page<Course> findAll(Specification<Course> spec, Pageable pageable);
}
