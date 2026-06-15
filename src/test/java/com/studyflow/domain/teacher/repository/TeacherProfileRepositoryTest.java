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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * findAllWithUserFiltered JPQL 통합 테스트
 *
 * 검증 목적: Hibernate + H2 환경에서 :param IS NULL / :xxxEmpty 패턴이 올바르게 동작하는지 확인.
 * MySQL에서도 동일하게 동작함을 별도 환경에서 추가 검증 권장.
 *
 * 테스트 데이터:
 *   - 홍길동 (MALE,   1990-01-01, 서울 강남구, 서울대학교, 활성)
 *   - 김영희 (FEMALE, 2000-01-01, 경기 성남시, 연세대학교, 활성)
 *   - 이탈퇴 (MALE,   1990-01-01, 비활성 soft-delete)
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
        User hong    = makeUser("hong@test.com",    "홍길동", Gender.MALE,   LocalDate.of(1990, 1, 1), true,  0L);
        User kim     = makeUser("kim@test.com",     "김영희", Gender.FEMALE, LocalDate.of(2000, 1, 1), true,  0L);
        User deleted = makeUser("deleted@test.com", "이탈퇴", Gender.MALE,   LocalDate.of(1990, 1, 1), false, 999L);

        em.persist(hong);
        em.persist(kim);
        em.persist(deleted);

        em.persist(makeProfile(hong, 800, "서울 강남구", "서울대학교"));
        em.persist(makeProfile(kim,  300, "경기 성남시", "연세대학교"));
        em.persist(TeacherProfile.createForUser(deleted));

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("모든 조건 비움 → 활성 선생님 전체 반환")
    void allEmpty_returnsAllActiveTeachers() {
        Page<TeacherProfile> result = search(null, null, null, null, null, null, null);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("keyword='' → 전체 반환 (LIKE '%%' 는 전체 매칭)")
    void emptyKeyword_treatedAsWildcard() {
        Page<TeacherProfile> result = search("", null, null, null, null, null, null);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("keyword 일치 → 이름에 키워드가 포함된 선생님만 반환")
    void keyword_returnsMatchingTeacher() {
        Page<TeacherProfile> result = search("홍", null, null, null, null, null, null);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getUser().getName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("keyword 불일치 → 빈 결과 반환")
    void keyword_noMatch_returnsEmpty() {
        Page<TeacherProfile> result = search("존재하지않는이름xyz", null, null, null, null, null, null);
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("gender=MALE → 남성 선생님만 반환")
    void gender_returnsMatchingTeacher() {
        Page<TeacherProfile> result = search(null, Gender.MALE, null, null, null, null, null);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getUser().getName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("출생일 범위 → 해당 구간에 태어난 선생님만 반환 (만 나이 필터)")
    void birthRange_returnsMatchingTeacher() {
        // 1985 ~ 1996 구간 → 홍길동(1990) 포함, 김영희(2000) 제외
        Page<TeacherProfile> result = search(null, null,
                LocalDate.of(1985, 1, 1), LocalDate.of(1996, 1, 1), null, null, null);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getUser().getName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("regions → 활동 지역이 일치하는 선생님만 반환")
    void regions_returnsMatchingTeacher() {
        Page<TeacherProfile> result = search(null, null, null, null,
                List.of("서울 강남구"), null, null);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getUser().getName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("regions 다중 → OR 매칭으로 여러 지역의 선생님 반환")
    void regions_multiple_returnsAll() {
        Page<TeacherProfile> result = search(null, null, null, null,
                List.of("서울 강남구", "경기 성남시"), null, null);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("universities → 대학교가 일치하는 선생님만 반환")
    void universities_returnsMatchingTeacher() {
        Page<TeacherProfile> result = search(null, null, null, null, null,
                List.of("연세대학교"), null);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getUser().getName()).isEqualTo("김영희");
    }

    @Test
    @DisplayName("복합 필터(keyword + gender) → 모든 조건을 만족하는 선생님만 반환")
    void combined_keywordAndGender() {
        Page<TeacherProfile> result = search("홍", Gender.MALE, null, null, null, null, null);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getUser().getName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("복합 필터 중 하나라도 불일치 → 빈 결과 반환")
    void combined_oneConditionFails() {
        // 홍길동은 MALE이므로 gender=FEMALE 조건에서 탈락
        Page<TeacherProfile> result = search("홍", Gender.FEMALE, null, null, null, null, null);
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("keyword='%' (미escape) → wildcard로 동작해 전체 반환 — 서비스 escape 필요성 확인")
    void percentKeyword_unescaped_actsAsWildcard() {
        Page<TeacherProfile> result = search("%", null, null, null, null, null, null);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("keyword='!%' (서비스 escape 적용 후) → 이름에 % 없으므로 빈 결과")
    void percentKeyword_escaped_returnsEmpty() {
        Page<TeacherProfile> result = search("!%", null, null, null, null, null, null);
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("탈퇴(soft-delete) 및 비활성 선생님은 결과에서 제외")
    void deletedAndInactive_excluded() {
        Page<TeacherProfile> result = search(null, null, null, null, null, null, null);
        assertThat(result.getContent())
                .extracting(tp -> tp.getUser().getName())
                .doesNotContain("이탈퇴");
    }

    // 서비스의 빈 목록 처리(더미 값 + Empty 플래그)를 모사해 리포지토리를 호출하는 헬퍼
    private Page<TeacherProfile> search(String keyword, Gender gender,
                                        LocalDate birthFrom, LocalDate birthTo,
                                        List<String> regions, List<String> universities,
                                        List<Long> subjectIds) {
        boolean regionsEmpty      = regions == null || regions.isEmpty();
        boolean universitiesEmpty = universities == null || universities.isEmpty();
        boolean subjectsEmpty     = subjectIds == null || subjectIds.isEmpty();
        return teacherProfileRepository.findAllWithUserFiltered(
                keyword, gender, birthFrom, birthTo,
                regionsEmpty,      regionsEmpty      ? List.of("")  : regions,
                universitiesEmpty, universitiesEmpty ? List.of("")  : universities,
                subjectsEmpty,     subjectsEmpty     ? List.of(-1L) : subjectIds,
                PageRequest.of(0, 10));
    }

    private TeacherProfile makeProfile(User user, int naegongScore, String address, String career) {
        TeacherProfile profile = TeacherProfile.createForUser(user);
        ReflectionTestUtils.setField(profile, "naegongScore", naegongScore);
        ReflectionTestUtils.setField(profile, "address", address);
        ReflectionTestUtils.setField(profile, "career", career);
        return profile;
    }

    // User 생성 헬퍼: @NoArgsConstructor(PROTECTED) 우회를 위해 리플렉션 사용
    private User makeUser(String email, String name, Gender gender, LocalDate birthDate,
                          boolean isActive, long isDeleted) throws Exception {
        Constructor<User> ctor = User.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        User user = ctor.newInstance();
        ReflectionTestUtils.setField(user, "email",          email);
        ReflectionTestUtils.setField(user, "name",           name);
        ReflectionTestUtils.setField(user, "role",           UserRole.TEACHER);
        ReflectionTestUtils.setField(user, "socialProvider", SocialProvider.LOCAL);
        ReflectionTestUtils.setField(user, "gender",         gender);
        ReflectionTestUtils.setField(user, "birthDate",      birthDate);
        ReflectionTestUtils.setField(user, "isActive",       isActive);
        ReflectionTestUtils.setField(user, "isDeleted",      isDeleted);
        return user;
    }
}
