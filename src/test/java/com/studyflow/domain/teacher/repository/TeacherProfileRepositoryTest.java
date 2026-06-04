package com.studyflow.domain.teacher.repository;

import com.studyflow.domain.teacher.entity.TeacherProfile;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.enums.Gender;
import com.studyflow.domain.user.enums.SocialProvider;
import com.studyflow.domain.user.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * findAllWithUserFiltered JPQL 통합 테스트
 *
 * 검증 목적: Hibernate + H2 환경에서 :param IS NULL 패턴이 올바르게 동작하는지 확인.
 * MySQL에서도 동일하게 동작함을 별도 환경에서 추가 검증 권장.
 *
 * 테스트 데이터:
 *   - 홍길동 (naegongScore=800, 활성)
 *   - 김철수 (naegongScore=300, 활성)
 *   - 이탈퇴 (naegongScore=0,   비활성 soft-delete)
 */
@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class TeacherProfileRepositoryTest {

    @Autowired
    private TeacherProfileRepository teacherProfileRepository;

    @Autowired
    private TestEntityManager em;

    @BeforeEach
    void setUp() throws Exception {
        User hong    = makeUser("hong@test.com",    "홍길동", true,  0L);
        User kim     = makeUser("kim@test.com",     "김철수", true,  0L);
        User deleted = makeUser("deleted@test.com", "이탈퇴", false, 999L);

        em.persist(hong);
        em.persist(kim);
        em.persist(deleted);

        TeacherProfile hongProfile = TeacherProfile.createForUser(hong);
        ReflectionTestUtils.setField(hongProfile, "naegongScore", 800);

        TeacherProfile kimProfile = TeacherProfile.createForUser(kim);
        ReflectionTestUtils.setField(kimProfile, "naegongScore", 300);

        em.persist(hongProfile);
        em.persist(kimProfile);
        em.persist(TeacherProfile.createForUser(deleted));

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("keyword=null, minNaegong=null → 활성 선생님 전체 반환 (:param IS NULL 조건 동작 확인)")
    void allNull_returnsAllActiveTeachers() {
        Page<TeacherProfile> result = teacherProfileRepository
                .findAllWithUserFiltered(null, null, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("keyword='' → 전체 반환 (서비스 정규화 전 빈 문자열이 유입된 경우; LIKE '%%' 는 전체 매칭)")
    void emptyKeyword_treatedAsWildcard() {
        Page<TeacherProfile> result = teacherProfileRepository
                .findAllWithUserFiltered("", null, PageRequest.of(0, 10));

        // 빈 문자열은 IS NULL=false 이므로 LIKE '%%' 조건으로 평가 → 전체 반환
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("keyword 일치 → 이름에 키워드가 포함된 선생님만 반환")
    void keyword_returnsMatchingTeacher() {
        Page<TeacherProfile> result = teacherProfileRepository
                .findAllWithUserFiltered("홍", null, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getUser().getName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("keyword 불일치 → 빈 결과 반환")
    void keyword_noMatch_returnsEmpty() {
        Page<TeacherProfile> result = teacherProfileRepository
                .findAllWithUserFiltered("존재하지않는이름xyz", null, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("minNaegong=null → 내공 점수 조건 없이 전체 반환 (:minNaegong IS NULL 조건 동작 확인)")
    void minNaegongNull_returnsAllActiveTeachers() {
        Page<TeacherProfile> result = teacherProfileRepository
                .findAllWithUserFiltered(null, null, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("minNaegong=500 → 내공 점수 이상인 선생님만 반환")
    void minNaegong_returnsHighScoreTeachers() {
        Page<TeacherProfile> result = teacherProfileRepository
                .findAllWithUserFiltered(null, 500, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getUser().getName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("keyword + minNaegong 복합 필터 → 두 조건 모두 만족하는 선생님만 반환")
    void combined_keywordAndMinNaegong() {
        Page<TeacherProfile> result = teacherProfileRepository
                .findAllWithUserFiltered("홍", 500, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getUser().getName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("keyword 매칭되지만 minNaegong 미달 → 빈 결과 반환")
    void keyword_matchesButMinNaegongFails() {
        Page<TeacherProfile> result = teacherProfileRepository
                .findAllWithUserFiltered("김", 500, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("keyword='%' (미escape) → wildcard로 동작해 전체 반환 — 서비스 escape 필요성 확인")
    void percentKeyword_unescaped_actsAsWildcard() {
        Page<TeacherProfile> result = teacherProfileRepository
                .findAllWithUserFiltered("%", null, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("keyword='!%' (서비스 escape 적용 후) → 이름에 % 없으므로 빈 결과")
    void percentKeyword_escaped_returnsEmpty() {
        Page<TeacherProfile> result = teacherProfileRepository
                .findAllWithUserFiltered("!%", null, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("탈퇴(soft-delete) 및 비활성 선생님은 결과에서 제외")
    void deletedAndInactive_excluded() {
        Page<TeacherProfile> result = teacherProfileRepository
                .findAllWithUserFiltered(null, null, PageRequest.of(0, 10));

        assertThat(result.getContent())
                .extracting(tp -> tp.getUser().getName())
                .doesNotContain("이탈퇴");
    }

    // User 생성 헬퍼: @NoArgsConstructor(PROTECTED) 우회를 위해 리플렉션 사용
    private User makeUser(String email, String name, boolean isActive, long isDeleted) throws Exception {
        Constructor<User> ctor = User.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        User user = ctor.newInstance();
        ReflectionTestUtils.setField(user, "email",          email);
        ReflectionTestUtils.setField(user, "name",           name);
        ReflectionTestUtils.setField(user, "role",           UserRole.TEACHER);
        ReflectionTestUtils.setField(user, "socialProvider", SocialProvider.LOCAL);
        ReflectionTestUtils.setField(user, "gender",         Gender.MALE);
        ReflectionTestUtils.setField(user, "birthDate",      LocalDate.of(1990, 1, 1));
        ReflectionTestUtils.setField(user, "isActive",       isActive);
        ReflectionTestUtils.setField(user, "isDeleted",      isDeleted);
        return user;
    }
}
