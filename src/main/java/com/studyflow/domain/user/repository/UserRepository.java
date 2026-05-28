package com.studyflow.domain.user.repository;

import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.enums.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // 주어진 email과 socialprovider로 사용자 조회하되, is_deleted가 0인(삭제되지 않은) 사용자만 조회
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.socialProvider = :socialProvider AND u.isDeleted = 0")
    Optional<User> findActiveByEmailAndSocialProvider(@Param("email") String email, @Param("socialProvider") SocialProvider socialProvider);

    Optional<User> findById(Long id);
}
