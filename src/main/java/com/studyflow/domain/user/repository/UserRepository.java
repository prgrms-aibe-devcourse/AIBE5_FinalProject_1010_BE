package com.studyflow.domain.user.repository;

import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.enums.SocialProvider;
import com.studyflow.domain.user.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // 주어진 email과 socialprovider로 사용자 조회하되, is_deleted가 0인(삭제되지 않은) 사용자만 조회
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.socialProvider = :socialProvider AND u.isDeleted = 0")
    Optional<User> findActiveByEmailAndSocialProvider(@Param("email") String email, @Param("socialProvider") SocialProvider socialProvider);

    Optional<User> findById(Long id);

    // 주어진 id로 사용자 조회하되, is_deleted가 0인(삭제되지 않은) 사용자만 조회
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.isDeleted = 0")
    Optional<User> findActiveById(Long id);

    long countByIsActiveTrue();

    long countByIsActiveTrueAndRole(UserRole role);

    // 비활성 유저: isActive=false, isDeleted=0
    long countByIsActiveFalseAndIsDeleted(Long isDeleted);

    long countByIsActiveFalseAndIsDeletedAndRole(Long isDeleted, UserRole role);

    // 탈퇴 유저: isDeleted != 0
    @Query("SELECT COUNT(u) FROM User u WHERE u.isDeleted <> 0")
    long countDeleted();

    @Query("SELECT COUNT(u) FROM User u WHERE u.isDeleted <> 0 AND u.role = :role")
    long countDeletedByRole(@Param("role") UserRole role);

    // 활성 유저 목록 (isActive=true) — role이 null이면 전체
    Page<User> findByIsActiveTrue(Pageable pageable);

    Page<User> findByIsActiveTrueAndRole(UserRole role, Pageable pageable);

    // 비활성 유저 목록 (isActive=false, isDeleted=0) — role이 null이면 전체
    Page<User> findByIsActiveFalseAndIsDeleted(Long isDeleted, Pageable pageable);

    Page<User> findByIsActiveFalseAndIsDeletedAndRole(Long isDeleted, UserRole role, Pageable pageable);

    // 탈퇴 유저 목록 (isDeleted != 0) — role이 null이면 전체
    @Query("SELECT u FROM User u WHERE u.isDeleted <> 0")
    Page<User> findAllDeleted(Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.isDeleted <> 0 AND u.role = :role")
    Page<User> findAllDeletedByRole(@Param("role") UserRole role, Pageable pageable);

    // 특정 날짜에 가입한 유저 수 (createdAt 기준)
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role AND CAST(u.createdAt AS LocalDate) = :date")
    long countByRoleAndCreatedAtDate(@Param("role") UserRole role, @Param("date") LocalDate date);

    // 특정 날짜에 탈퇴한 유저 수 (deletedAt 기준)
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role AND CAST(u.deletedAt AS LocalDate) = :date")
    long countByRoleAndDeletedAtDate(@Param("role") UserRole role, @Param("date") LocalDate date);
}
