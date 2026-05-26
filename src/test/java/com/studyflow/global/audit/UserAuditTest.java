package com.studyflow.global.audit;

import com.studyflow.domain.user.entity.Role;
import com.studyflow.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UserAuditTest {

    @Autowired
    private EntityManager em;

    @Test
    void whenPersistUser_thenAuditFieldsAreSet() {
        User user = User.builder()
                .email("audit@test.com")
                .password("password")
                .name("Audit Test")
                .phone("010-0000-0000")
                .role(Role.STUDENT)
                .build();

        em.persist(user);
        em.flush();
        em.clear();

        User found = em.find(User.class, user.getId());

        System.out.println("Created At " + found.getCreatedAt());
        System.out.println("Updated At " + found.getUpdatedAt());
        assertNotNull(found.getCreatedAt(), "createdAt should be set by JPA Auditing");
        assertNotNull(found.getUpdatedAt(), "updatedAt should be set by JPA Auditing");
    }
}

