-- ============================================================
-- Course 더미 데이터: APPROVED 선생님 25명, 총 36개 수업
-- teacher1(수학)   : 2개 | teacher2(영어)  : 2개
-- teacher3(물리)   : 1개 | teacher4(화학)  : 2개
-- teacher5(국어)   : 2개 | teacher6(사회)  : 1개
-- teacher7(정보)   : 2개
-- teacher11(수학)  : 2개 | teacher12(영어) : 1개
-- teacher13(국어)  : 2개 | teacher14(생물) : 1개
-- teacher15(사회)  : 1개 | teacher16(수학) : 2개
-- teacher17(영어)  : 1개 | teacher18(정보) : 2개
-- teacher19(국어)  : 1개 | teacher20(화학) : 2개
-- teacher21(사회)  : 1개 | teacher22(수학) : 1개
-- teacher23(영어)  : 1개
-- teacher26(수학)  : 1개 | teacher27(영어) : 1개
-- teacher28(국어)  : 1개 | teacher29(화학) : 1개
-- teacher30(정보)  : 1개
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- subject 시딩 주의
-- SubjectDataInitializer 가 앱 기동 시 대분류 8개를 자동 시딩하므로
-- 별도 INSERT 불필요. 앱을 한 번 실행한 뒤 이 SQL을 실행할 것.
-- subject_id 는 대분류(category) 기준으로 연결.
--   teacher3(물리) → 과학탐구 | teacher4(화학) → 과학탐구
--   teacher6(사회) → 사회탐구
-- ============================================================

-- ============================================================
-- STEP 1: course INSERT
--         teacher_profile_id  : users → teacher_profile JOIN
--         subject_id          : subject category 로 조회
-- ============================================================

-- ----------------------------------------------------------
-- teacher1 (김민준 / 수학) — 2개
-- ----------------------------------------------------------

-- ① 수능 수학 1등급 만들기 [RECRUITING]
INSERT INTO course (teacher_profile_id, subject_id, title, description, target_grade,
                    max_students, duration_minutes, price_per_session, textbook,
                    curriculum_type, curriculum_detail, available_schedule,
                    first_class_date, thumbnail_url, recruit_deadline,
                    start_date, end_date, status, is_listed, is_public_audit,
                    teaching_mode, location, location_lat, location_lng,
                    next_class_at, created_at, updated_at)
SELECT
  tp.id,
  (SELECT id FROM subject WHERE category = 'MATH'),
  '수능 수학 1등급 만들기',
  '수능 수학에서 고득점을 목표로 하는 고3 전용 특강입니다. 출제 유형 분석부터 킬러 문항 공략까지 체계적으로 지도합니다.',
  'HIGH_3', 3, 90, 65000,
  '수능특강 수학영역 (EBS)',
  'FIXED',
  '1주차: 수1 수열·극한 핵심 개념 / 2주차: 수2 미분·적분 집중 / 3주차: 확통·기벡 / 4주차: 실전 모의고사 분석',
  '매주 화·목 오후 7시~8시 30분',
  '2026-07-01',
  NULL,
  '2026-06-28',
  '2026-07-01', '2026-08-30',
  'RECRUITING', TRUE, FALSE,
  'ONLINE', NULL, NULL, NULL,
  '2026-07-01 19:00:00',
  u.created_at, u.created_at
FROM users u
JOIN teacher_profile tp ON tp.user_id = u.id
WHERE u.email = 'teacher1@studyflow.com';

-- ② 고1 수학 개념 완성반 [IN_PROGRESS]
INSERT INTO course (teacher_profile_id, subject_id, title, description, target_grade,
                    max_students, duration_minutes, price_per_session, textbook,
                    curriculum_type, curriculum_detail, available_schedule,
                    first_class_date, thumbnail_url, recruit_deadline,
                    start_date, end_date, status, is_listed, is_public_audit,
                    teaching_mode, location, location_lat, location_lng,
                    next_class_at, created_at, updated_at)
SELECT
  tp.id,
  (SELECT id FROM subject WHERE category = 'MATH'),
  '고1 수학 개념 완성반',
  '고등학교 입학 후 수학이 갑자기 어려워진 학생들을 위한 기초 개념 완성반입니다. 교과서 중심으로 탄탄하게 쌓아 올립니다.',
  'HIGH_1', 2, 90, 50000,
  '수학(상)·수학(하) 교과서',
  'CUSTOM',
  '학생 수준에 따라 맞춤 진도 조정',
  '매주 월·수 오후 5시~6시 30분',
  '2026-06-02',
  NULL,
  '2026-05-25',
  '2026-06-02', '2026-08-01',
  'IN_PROGRESS', TRUE, FALSE,
  'ONLINE', NULL, NULL, NULL,
  '2026-06-25 17:00:00',
  u.created_at, u.created_at
FROM users u
JOIN teacher_profile tp ON tp.user_id = u.id
WHERE u.email = 'teacher1@studyflow.com';

-- ----------------------------------------------------------
-- teacher2 (이서연 / 영어) — 2개
-- ----------------------------------------------------------

-- ③ 수능 영어 독해·문법 집중반 [RECRUITING]
INSERT INTO course (teacher_profile_id, subject_id, title, description, target_grade,
                    max_students, duration_minutes, price_per_session, textbook,
                    curriculum_type, curriculum_detail, available_schedule,
                    first_class_date, thumbnail_url, recruit_deadline,
                    start_date, end_date, status, is_listed, is_public_audit,
                    teaching_mode, location, location_lat, location_lng,
                    next_class_at, created_at, updated_at)
SELECT
  tp.id,
  (SELECT id FROM subject WHERE category = 'ENGLISH'),
  '수능 영어 독해·문법 집중반',
  '수능 영어 1등급을 위한 독해 전략과 문법 완성반입니다. EBS 연계 지문 분석 및 빈칸·순서 유형을 집중 공략합니다.',
  'HIGH_2', 2, 60, 55000,
  '수능특강 영어영역 (EBS)',
  'FIXED',
  '1주차: 장문 독해 / 2주차: 빈칸 추론 / 3주차: 순서·삽입 / 4주차: 실전 모의',
  '매주 화·목 오후 6시~7시',
  '2026-07-03',
  NULL,
  '2026-06-30',
  '2026-07-03', '2026-08-28',
  'RECRUITING', TRUE, FALSE,
  'ONLINE', NULL, NULL, NULL,
  '2026-07-03 18:00:00',
  u.created_at, u.created_at
FROM users u
JOIN teacher_profile tp ON tp.user_id = u.id
WHERE u.email = 'teacher2@studyflow.com';

-- ④ 중등 영어 회화 기초반 [RECRUITING / OFFLINE]
INSERT INTO course (teacher_profile_id, subject_id, title, description, target_grade,
                    max_students, duration_minutes, price_per_session, textbook,
                    curriculum_type, curriculum_detail, available_schedule,
                    first_class_date, thumbnail_url, recruit_deadline,
                    start_date, end_date, status, is_listed, is_public_audit,
                    teaching_mode, location, location_lat, location_lng,
                    next_class_at, created_at, updated_at)
SELECT
  tp.id,
  (SELECT id FROM subject WHERE category = 'ENGLISH'),
  '중등 영어 회화 기초반',
  '중학교 3학년을 대상으로 한 영어 회화 기초반입니다. 원어민 발음 교정부터 실생활 영어 회화까지 체계적으로 지도합니다.',
  'MIDDLE_3', 4, 60, 45000,
  'English Go! (YBM)',
  'CUSTOM',
  '학생 수준에 따라 유연하게 진행',
  '매주 토 오전 10시~11시',
  '2026-07-05',
  NULL,
  '2026-07-01',
  '2026-07-05', '2026-09-27',
  'RECRUITING', TRUE, FALSE,
  'OFFLINE', '서울특별시 서초구 서초대로 305 (오피스 스터디룸)', 37.4910, 127.0070,
  '2026-07-05 10:00:00',
  u.created_at, u.created_at
FROM users u
JOIN teacher_profile tp ON tp.user_id = u.id
WHERE u.email = 'teacher2@studyflow.com';

-- ----------------------------------------------------------
-- teacher3 (박지훈 / 물리) — 1개
-- ----------------------------------------------------------

-- ⑤ 고등 물리학1 개념·문제풀이반 [RECRUITING]
INSERT INTO course (teacher_profile_id, subject_id, title, description, target_grade,
                    max_students, duration_minutes, price_per_session, textbook,
                    curriculum_type, curriculum_detail, available_schedule,
                    first_class_date, thumbnail_url, recruit_deadline,
                    start_date, end_date, status, is_listed, is_public_audit,
                    teaching_mode, location, location_lat, location_lng,
                    next_class_at, created_at, updated_at)
SELECT
  tp.id,
  (SELECT id FROM subject WHERE category = 'SCIENCE'),
  '고등 물리학1 개념·문제풀이반',
  '물리가 어렵게 느껴지는 학생들을 위한 개념 중심 수업입니다. 실험 영상과 시각 자료를 적극 활용하여 직관적으로 이해할 수 있도록 지도합니다.',
  'HIGH_1', 2, 90, 55000,
  '물리학1 교과서 (미래엔)',
  'FIXED',
  '1~4주: 역학 / 5~8주: 전자기 / 9~12주: 파동·빛 / 13~16주: 모의 문제 풀이',
  '매주 수·금 오후 5시~6시 30분',
  '2026-07-02',
  NULL,
  '2026-06-28',
  '2026-07-02', '2026-10-24',
  'RECRUITING', TRUE, FALSE,
  'ONLINE', NULL, NULL, NULL,
  '2026-07-02 17:00:00',
  u.created_at, u.created_at
FROM users u
JOIN teacher_profile tp ON tp.user_id = u.id
WHERE u.email = 'teacher3@studyflow.com';

-- ----------------------------------------------------------
-- teacher4 (최수진 / 화학) — 2개
-- ----------------------------------------------------------

-- ⑥ 화학1 기초 완성반 [IN_PROGRESS]
INSERT INTO course (teacher_profile_id, subject_id, title, description, target_grade,
                    max_students, duration_minutes, price_per_session, textbook,
                    curriculum_type, curriculum_detail, available_schedule,
                    first_class_date, thumbnail_url, recruit_deadline,
                    start_date, end_date, status, is_listed, is_public_audit,
                    teaching_mode, location, location_lat, location_lng,
                    next_class_at, created_at, updated_at)
SELECT
  tp.id,
  (SELECT id FROM subject WHERE category = 'SCIENCE'),
  '화학1 기초 완성반',
  '화학을 처음 배우는 고1·2를 위한 기초 개념 완성반입니다. 원소 주기율부터 화학 반응까지 시각화 자료로 쉽게 설명합니다.',
  'HIGH_1', 3, 80, 50000,
  '화학1 교과서 (천재교육)',
  'FIXED',
  '1~3주: 물질의 세계 / 4~6주: 원자 구조 / 7~9주: 화학 결합 / 10~12주: 산화·환원',
  '매주 월·목 오후 4시~5시 20분',
  '2026-06-05',
  NULL,
  '2026-05-28',
  '2026-06-05', '2026-08-28',
  'IN_PROGRESS', TRUE, FALSE,
  'ONLINE', NULL, NULL, NULL,
  '2026-06-26 16:00:00',
  u.created_at, u.created_at
FROM users u
JOIN teacher_profile tp ON tp.user_id = u.id
WHERE u.email = 'teacher4@studyflow.com';

-- ⑦ 화학2 심화반 [RECRUITING / OFFLINE]
INSERT INTO course (teacher_profile_id, subject_id, title, description, target_grade,
                    max_students, duration_minutes, price_per_session, textbook,
                    curriculum_type, curriculum_detail, available_schedule,
                    first_class_date, thumbnail_url, recruit_deadline,
                    start_date, end_date, status, is_listed, is_public_audit,
                    teaching_mode, location, location_lat, location_lng,
                    next_class_at, created_at, updated_at)
SELECT
  tp.id,
  (SELECT id FROM subject WHERE category = 'SCIENCE'),
  '화학2 심화반',
  '화학1을 이수한 고2·3 대상 심화반입니다. 열역학, 반응속도, 전기화학 등 심화 개념을 다루며 의치한 진학을 목표로 합니다.',
  'HIGH_2', 2, 90, 62000,
  '화학2 교과서 (미래엔)',
  'FIXED',
  '1~4주: 열역학 / 5~8주: 반응속도 / 9~12주: 전기화학 / 13~16주: 수능 기출 분석',
  '매주 토 오후 2시~3시 30분',
  '2026-07-05',
  NULL,
  '2026-07-01',
  '2026-07-05', '2026-10-24',
  'RECRUITING', TRUE, FALSE,
  'OFFLINE', '경기도 성남시 분당구 판교역로 235 (스터디카페 2층)', 37.3948, 127.1108,
  '2026-07-05 14:00:00',
  u.created_at, u.created_at
FROM users u
JOIN teacher_profile tp ON tp.user_id = u.id
WHERE u.email = 'teacher4@studyflow.com';

-- ----------------------------------------------------------
-- teacher5 (정도현 / 국어) — 2개
-- ----------------------------------------------------------

-- ⑧ 수능 국어 독해 완성반 [RECRUITING]
INSERT INTO course (teacher_profile_id, subject_id, title, description, target_grade,
                    max_students, duration_minutes, price_per_session, textbook,
                    curriculum_type, curriculum_detail, available_schedule,
                    first_class_date, thumbnail_url, recruit_deadline,
                    start_date, end_date, status, is_listed, is_public_audit,
                    teaching_mode, location, location_lat, location_lng,
                    next_class_at, created_at, updated_at)
SELECT
  tp.id,
  (SELECT id FROM subject WHERE category = 'KOREAN'),
  '수능 국어 독해 완성반',
  '수능 국어 1등급을 위한 독해·논리 집중반입니다. 17년 경력의 논술 강사가 화법·작문·독서·문학 전 영역을 체계적으로 지도합니다.',
  'HIGH_3', 3, 90, 72000,
  '수능특강 국어영역 (EBS)',
  'FIXED',
  '1~3주: 독서 지문 독해 전략 / 4~6주: 문학 갈래별 분석 / 7~9주: 화법·작문 / 10~12주: 실전 모의고사',
  '매주 화·금 오후 7시~8시 30분',
  '2026-07-01',
  NULL,
  '2026-06-27',
  '2026-07-01', '2026-09-26',
  'RECRUITING', TRUE, FALSE,
  'ONLINE', NULL, NULL, NULL,
  '2026-07-01 19:00:00',
  u.created_at, u.created_at
FROM users u
JOIN teacher_profile tp ON tp.user_id = u.id
WHERE u.email = 'teacher5@studyflow.com';

-- ⑨ 중등 국어 문법·작문반 [RECRUITING]
INSERT INTO course (teacher_profile_id, subject_id, title, description, target_grade,
                    max_students, duration_minutes, price_per_session, textbook,
                    curriculum_type, curriculum_detail, available_schedule,
                    first_class_date, thumbnail_url, recruit_deadline,
                    start_date, end_date, status, is_listed, is_public_audit,
                    teaching_mode, location, location_lat, location_lng,
                    next_class_at, created_at, updated_at)
SELECT
  tp.id,
  (SELECT id FROM subject WHERE category = 'KOREAN'),
  '중등 국어 문법·작문반',
  '중학교 2학년 대상 국어 문법과 글쓰기 역량 향상 수업입니다. 내신과 서술형 대비를 동시에 해결합니다.',
  'MIDDLE_2', 4, 60, 45000,
  '중학 국어 2-1 교과서 (미래엔)',
  'CUSTOM',
  '학생 수준과 학교 시험 일정에 맞춰 탄력적으로 운영',
  '매주 수 오후 4시~5시',
  '2026-07-02',
  NULL,
  '2026-06-29',
  '2026-07-02', '2026-09-24',
  'RECRUITING', TRUE, FALSE,
  'ONLINE', NULL, NULL, NULL,
  '2026-07-02 16:00:00',
  u.created_at, u.created_at
FROM users u
JOIN teacher_profile tp ON tp.user_id = u.id
WHERE u.email = 'teacher5@studyflow.com';

-- ----------------------------------------------------------
-- teacher6 (윤하은 / 사회) — 1개
-- ----------------------------------------------------------

-- ⑩ 사회탐구 생활과 윤리 개념반 [RECRUITING]
INSERT INTO course (teacher_profile_id, subject_id, title, description, target_grade,
                    max_students, duration_minutes, price_per_session, textbook,
                    curriculum_type, curriculum_detail, available_schedule,
                    first_class_date, thumbnail_url, recruit_deadline,
                    start_date, end_date, status, is_listed, is_public_audit,
                    teaching_mode, location, location_lat, location_lng,
                    next_class_at, created_at, updated_at)
SELECT
  tp.id,
  (SELECT id FROM subject WHERE category = 'SOCIAL_STUDIES'),
  '사회탐구 생활과 윤리 개념반',
  '사탐 생활과윤리를 선택한 고2 대상 개념 완성반입니다. 시사 이슈와 연계한 흥미로운 방식으로 핵심 개념을 익힙니다.',
  'HIGH_2', 3, 80, 47000,
  '생활과 윤리 교과서 (천재교육)',
  'FIXED',
  '1~3주: 현대 윤리의 이해 / 4~6주: 생명·환경 윤리 / 7~9주: 사회·윤리 / 10~12주: 통일·국제 윤리 / 13~15주: 기출 풀이',
  '매주 화·목 오후 5시~6시 20분',
  '2026-07-01',
  NULL,
  '2026-06-27',
  '2026-07-01', '2026-10-09',
  'RECRUITING', TRUE, FALSE,
  'ONLINE', NULL, NULL, NULL,
  '2026-07-01 17:00:00',
  u.created_at, u.created_at
FROM users u
JOIN teacher_profile tp ON tp.user_id = u.id
WHERE u.email = 'teacher6@studyflow.com';

-- ----------------------------------------------------------
-- teacher7 (임재원 / 정보·SW) — 2개
-- ----------------------------------------------------------

-- ⑪ 파이썬 기초 프로그래밍반 [RECRUITING]
INSERT INTO course (teacher_profile_id, subject_id, title, description, target_grade,
                    max_students, duration_minutes, price_per_session, textbook,
                    curriculum_type, curriculum_detail, available_schedule,
                    first_class_date, thumbnail_url, recruit_deadline,
                    start_date, end_date, status, is_listed, is_public_audit,
                    teaching_mode, location, location_lat, location_lng,
                    next_class_at, created_at, updated_at)
SELECT
  tp.id,
  (SELECT id FROM subject WHERE category = 'VOCATIONAL'),
  '파이썬 기초 프로그래밍반',
  '코딩을 처음 접하는 중학생을 위한 파이썬 입문반입니다. 변수·조건문·반복문·함수까지 실습 위주로 진행하며 정보올림피아드 기초를 다집니다.',
  'MIDDLE_3', 3, 90, 52000,
  '점프 투 파이썬 (이지스퍼블리싱)',
  'FIXED',
  '1~3주: 변수·자료형 / 4~6주: 조건문·반복문 / 7~9주: 함수·모듈 / 10~12주: 미니 프로젝트',
  '매주 토 오전 10시~11시 30분',
  '2026-07-05',
  NULL,
  '2026-07-01',
  '2026-07-05', '2026-09-27',
  'RECRUITING', TRUE, FALSE,
  'ONLINE', NULL, NULL, NULL,
  '2026-07-05 10:00:00',
  u.created_at, u.created_at
FROM users u
JOIN teacher_profile tp ON tp.user_id = u.id
WHERE u.email = 'teacher7@studyflow.com';

-- ⑫ 알고리즘·자료구조 입문반 [IN_PROGRESS]
INSERT INTO course (teacher_profile_id, subject_id, title, description, target_grade,
                    max_students, duration_minutes, price_per_session, textbook,
                    curriculum_type, curriculum_detail, available_schedule,
                    first_class_date, thumbnail_url, recruit_deadline,
                    start_date, end_date, status, is_listed, is_public_audit,
                    teaching_mode, location, location_lat, location_lng,
                    next_class_at, created_at, updated_at)
SELECT
  tp.id,
  (SELECT id FROM subject WHERE category = 'VOCATIONAL'),
  '알고리즘·자료구조 입문반',
  '파이썬 기초를 마친 고1 대상의 알고리즘 입문반입니다. 정렬·탐색·DFS/BFS 등 정보올림피아드·SW특기자 전형을 대비합니다.',
  'HIGH_1', 2, 100, 60000,
  '알고리즘 문제해결전략 (구종만)',
  'FIXED',
  '1~4주: 정렬·이분탐색 / 5~8주: 그래프 탐색 / 9~12주: DP / 13~16주: 기출 문제 풀이',
  '매주 수·금 오후 6시~7시 40분',
  '2026-06-04',
  NULL,
  '2026-05-28',
  '2026-06-04', '2026-09-19',
  'IN_PROGRESS', TRUE, FALSE,
  'ONLINE', NULL, NULL, NULL,
  '2026-06-25 18:00:00',
  u.created_at, u.created_at
FROM users u
JOIN teacher_profile tp ON tp.user_id = u.id
WHERE u.email = 'teacher7@studyflow.com';

-- ----------------------------------------------------------
-- teacher11 (남도윤 / 수학) — 2개
-- ----------------------------------------------------------

-- ⑬ 중학 수학 기초 완성반 [RECRUITING]
INSERT INTO course (teacher_profile_id, subject_id, title, description, target_grade,
                    max_students, duration_minutes, price_per_session, textbook,
                    curriculum_type, curriculum_detail, available_schedule,
                    first_class_date, thumbnail_url, recruit_deadline,
                    start_date, end_date, status, is_listed, is_public_audit,
                    teaching_mode, location, location_lat, location_lng,
                    next_class_at, created_at, updated_at)
SELECT tp.id, (SELECT id FROM subject WHERE category = 'MATH'),
  '중학 수학 기초 완성반',
  '중학교 2~3학년을 위한 수학 기초 개념 완성반입니다. 연산부터 함수까지 단계별로 탄탄하게 쌓아 올립니다.',
  'MIDDLE_3', 4, 80, 45000, '중학 수학 3 교과서 (천재교육)', 'FIXED',
  '1~4주: 수와 연산 / 5~8주: 문자와 식 / 9~12주: 함수 / 13~15주: 통계',
  '매주 월·수 오후 4시~5시 20분', '2026-07-07', NULL, '2026-07-03',
  '2026-07-07', '2026-10-02', 'RECRUITING', TRUE, FALSE,
  'ONLINE', NULL, NULL, NULL, '2026-07-07 16:00:00', u.created_at, u.created_at
FROM users u JOIN teacher_profile tp ON tp.user_id = u.id WHERE u.email = 'teacher11@studyflow.com';

-- ⑭ 수능 수학 킬러 문항 집중반 [RECRUITING]
INSERT INTO course (teacher_profile_id, subject_id, title, description, target_grade,
                    max_students, duration_minutes, price_per_session, textbook,
                    curriculum_type, curriculum_detail, available_schedule,
                    first_class_date, thumbnail_url, recruit_deadline,
                    start_date, end_date, status, is_listed, is_public_audit,
                    teaching_mode, location, location_lat, location_lng,
                    next_class_at, created_at, updated_at)
SELECT tp.id, (SELECT id FROM subject WHERE category = 'MATH'),
  '수능 수학 킬러 문항 집중반',
  '수능 수학 29·30번 킬러 문항을 집중 공략하는 고3 심화반입니다. 미적분·기하·확통 킬러 유형을 완벽히 대비합니다.',
  'HIGH_3', 2, 90, 70000, '수능완성 수학영역 (EBS)', 'FIXED',
  '1~4주: 미적분 킬러 유형 / 5~8주: 기하 킬러 유형 / 9~12주: 확통 킬러 / 13~15주: 실전 모의',
  '매주 화·목 오후 8시~9시 30분', '2026-07-08', NULL, '2026-07-04',
  '2026-07-08', '2026-10-09', 'RECRUITING', TRUE, FALSE,
  'ONLINE', NULL, NULL, NULL, '2026-07-08 20:00:00', u.created_at, u.created_at
FROM users u JOIN teacher_profile tp ON tp.user_id = u.id WHERE u.email = 'teacher11@studyflow.com';

-- ----------------------------------------------------------
-- teacher12 (이지원 / 영어) — 1개
-- ----------------------------------------------------------

-- ⑮ 고등 영어 내신 완성반 [IN_PROGRESS]
INSERT INTO course (teacher_profile_id, subject_id, title, description, target_grade,
                    max_students, duration_minutes, price_per_session, textbook,
                    curriculum_type, curriculum_detail, available_schedule,
                    first_class_date, thumbnail_url, recruit_deadline,
                    start_date, end_date, status, is_listed, is_public_audit,
                    teaching_mode, location, location_lat, location_lng,
                    next_class_at, created_at, updated_at)
SELECT tp.id, (SELECT id FROM subject WHERE category = 'ENGLISH'),
  '고등 영어 내신 완성반',
  '고1·2 내신 영어 전 영역 완성반입니다. 교과서 본문 분석부터 서술형 대비까지 체계적으로 지도합니다.',
  'HIGH_1', 3, 70, 50000, '고1 영어 교과서 (YBM)', 'CUSTOM',
  '학교별 교과서 및 시험 일정에 맞게 탄력적으로 운영',
  '매주 화·금 오후 5시~6시 10분', '2026-06-03', NULL, '2026-05-27',
  '2026-06-03', '2026-08-26', 'IN_PROGRESS', TRUE, FALSE,
  'ONLINE', NULL, NULL, NULL, '2026-06-27 17:00:00', u.created_at, u.created_at
FROM users u JOIN teacher_profile tp ON tp.user_id = u.id WHERE u.email = 'teacher12@studyflow.com';

-- ----------------------------------------------------------
-- teacher13 (박민혁 / 국어) — 2개
-- ----------------------------------------------------------

-- ⑯ 고등 문학 집중반 [RECRUITING]
INSERT INTO course (teacher_profile_id, subject_id, title, description, target_grade,
                    max_students, duration_minutes, price_per_session, textbook,
                    curriculum_type, curriculum_detail, available_schedule,
                    first_class_date, thumbnail_url, recruit_deadline,
                    start_date, end_date, status, is_listed, is_public_audit,
                    teaching_mode, location, location_lat, location_lng,
                    next_class_at, created_at, updated_at)
SELECT tp.id, (SELECT id FROM subject WHERE category = 'KOREAN'),
  '고등 문학 집중반',
  '고2~3 대상 수능 문학 집중반입니다. 현대소설·시·고전 갈래별 출제 유형을 분석하고 실전 감각을 키웁니다.',
  'HIGH_2', 3, 90, 60000, '수능특강 문학 (EBS)', 'FIXED',
  '1~4주: 현대시 / 5~8주: 현대소설 / 9~11주: 고전시가 / 12~14주: 고전소설 / 15주: 실전',
  '매주 화·목 오후 6시~7시 30분', '2026-07-01', NULL, '2026-06-27',
  '2026-07-01', '2026-10-16', 'RECRUITING', TRUE, FALSE,
  'ONLINE', NULL, NULL, NULL, '2026-07-01 18:00:00', u.created_at, u.created_at
FROM users u JOIN teacher_profile tp ON tp.user_id = u.id WHERE u.email = 'teacher13@studyflow.com';

-- ⑰ 중등 국어 내신 대비반 [RECRUITING]
INSERT INTO course (teacher_profile_id, subject_id, title, description, target_grade,
                    max_students, duration_minutes, price_per_session, textbook,
                    curriculum_type, curriculum_detail, available_schedule,
                    first_class_date, thumbnail_url, recruit_deadline,
                    start_date, end_date, status, is_listed, is_public_audit,
                    teaching_mode, location, location_lat, location_lng,
                    next_class_at, created_at, updated_at)
SELECT tp.id, (SELECT id FROM subject WHERE category = 'KOREAN'),
  '중등 국어 내신 대비반',
  '중학교 1~3학년 국어 내신을 위한 맞춤 대비반입니다. 학교 시험 일정에 맞춰 핵심 개념과 서술형을 집중 지도합니다.',
  'MIDDLE_2', 4, 60, 42000, '중학 국어 교과서 (미래엔)', 'CUSTOM',
  '학교별 시험 범위에 맞게 유연하게 운영',
  '매주 토 오전 10시~11시', '2026-07-05', NULL, '2026-07-01',
  '2026-07-05', '2026-09-27', 'RECRUITING', TRUE, FALSE,
  'ONLINE', NULL, NULL, NULL, '2026-07-05 10:00:00', u.created_at, u.created_at
FROM users u JOIN teacher_profile tp ON tp.user_id = u.id WHERE u.email = 'teacher13@studyflow.com';

-- ----------------------------------------------------------
-- teacher14 (송유진 / 생명과학) — 1개
-- ----------------------------------------------------------

-- ⑱ 생명과학1 개념 완성반 [RECRUITING]
INSERT INTO course (teacher_profile_id, subject_id, title, description, target_grade,
                    max_students, duration_minutes, price_per_session, textbook,
                    curriculum_type, curriculum_detail, available_schedule,
                    first_class_date, thumbnail_url, recruit_deadline,
                    start_date, end_date, status, is_listed, is_public_audit,
                    teaching_mode, location, location_lat, location_lng,
                    next_class_at, created_at, updated_at)
SELECT tp.id, (SELECT id FROM subject WHERE category = 'SCIENCE'),
  '생명과학1 개념 완성반',
  '생명과학1 전 단원을 체계적으로 학습하는 고1·2 대상 개념 완성반입니다. 탐구 활동과 실험 영상을 적극 활용합니다.',
  'HIGH_1', 3, 80, 52000, '생명과학1 교과서 (미래엔)', 'FIXED',
  '1~4주: 세포와 물질대사 / 5~8주: 유전 / 9~11주: 항상성 / 12~14주: 생태계',
  '매주 월·수 오후 6시~7시 20분', '2026-07-07', NULL, '2026-07-03',
  '2026-07-07', '2026-10-14', 'RECRUITING', TRUE, FALSE,
  'ONLINE', NULL, NULL, NULL, '2026-07-07 18:00:00', u.created_at, u.created_at
FROM users u JOIN teacher_profile tp ON tp.user_id = u.id WHERE u.email = 'teacher14@studyflow.com';

-- ----------------------------------------------------------
-- teacher15 (조준서 / 사회) — 1개
-- ----------------------------------------------------------

-- ⑲ 사회탐구 한국지리 개념반 [RECRUITING]
INSERT INTO course (teacher_profile_id, subject_id, title, description, target_grade,
                    max_students, duration_minutes, price_per_session, textbook,
                    curriculum_type, curriculum_detail, available_schedule,
                    first_class_date, thumbnail_url, recruit_deadline,
                    start_date, end_date, status, is_listed, is_public_audit,
                    teaching_mode, location, location_lat, location_lng,
                    next_class_at, created_at, updated_at)
SELECT tp.id, (SELECT id FROM subject WHERE category = 'SOCIAL_STUDIES'),
  '사회탐구 한국지리 개념반',
  '수능 한국지리를 선택한 고2·3 대상 개념 완성반입니다. 지도·통계 자료를 활용한 흥미로운 수업으로 핵심을 파악합니다.',
  'HIGH_2', 3, 80, 48000, '한국지리 교과서 (비상교육)', 'FIXED',
  '1~4주: 국토와 자연환경 / 5~8주: 인문 환경 / 9~11주: 지역별 특성 / 12~14주: 기출 분석',
  '매주 화 오후 5시~6시 20분', '2026-07-01', NULL, '2026-06-27',
  '2026-07-01', '2026-10-07', 'RECRUITING', TRUE, FALSE,
  'ONLINE', NULL, NULL, NULL, '2026-07-01 17:00:00', u.created_at, u.created_at
FROM users u JOIN teacher_profile tp ON tp.user_id = u.id WHERE u.email = 'teacher15@studyflow.com';

-- ----------------------------------------------------------
-- teacher16 (강하은 / 수학) — 2개
-- ----------------------------------------------------------

-- ⑳ 수학 올림피아드 입문반 [RECRUITING]
INSERT INTO course (teacher_profile_id, subject_id, title, description, target_grade,
                    max_students, duration_minutes, price_per_session, textbook,
                    curriculum_type, curriculum_detail, available_schedule,
                    first_class_date, thumbnail_url, recruit_deadline,
                    start_date, end_date, status, is_listed, is_public_audit,
                    teaching_mode, location, location_lat, location_lng,
                    next_class_at, created_at, updated_at)
SELECT tp.id, (SELECT id FROM subject WHERE category = 'MATH'),
  '수학 올림피아드 입문반',
  '수학경시대회 및 올림피아드를 준비하는 중·고등학생 대상 심화반입니다. 정수론·조합론·기하를 체계적으로 다룹니다.',
  'HIGH_1', 2, 100, 75000, '올림피아드 수학의 지름길 (성지출판)', 'FIXED',
  '1~5주: 정수론 / 6~10주: 조합론 / 11~15주: 기하 / 16주: 모의 경시',
  '매주 토 오후 2시~3시 40분', '2026-07-05', NULL, '2026-07-01',
  '2026-07-05', '2026-10-31', 'RECRUITING', TRUE, FALSE,
  'ONLINE', NULL, NULL, NULL, '2026-07-05 14:00:00', u.created_at, u.created_at
FROM users u JOIN teacher_profile tp ON tp.user_id = u.id WHERE u.email = 'teacher16@studyflow.com';

-- ㉑ 고2 수학 개념+내신 완성반 [IN_PROGRESS]
INSERT INTO course (teacher_profile_id, subject_id, title, description, target_grade,
                    max_students, duration_minutes, price_per_session, textbook,
                    curriculum_type, curriculum_detail, available_schedule,
                    first_class_date, thumbnail_url, recruit_deadline,
                    start_date, end_date, status, is_listed, is_public_audit,
                    teaching_mode, location, location_lat, location_lng,
                    next_class_at, created_at, updated_at)
SELECT tp.id, (SELECT id FROM subject WHERE category = 'MATH'),
  '고2 수학 개념+내신 완성반',
  '수1·수2를 수강 중인 고2 대상 개념 완성 및 내신 대비반입니다. 주요 출제 유형 분석과 실전 문제풀이를 병행합니다.',
  'HIGH_2', 3, 90, 58000, '수학1·수학2 교과서 (금성출판사)', 'CUSTOM',
  '학교 시험 일정에 맞게 탄력적으로 진행',
  '매주 월·목 오후 5시~6시 30분', '2026-06-09', NULL, '2026-06-02',
  '2026-06-09', '2026-08-28', 'IN_PROGRESS', TRUE, FALSE,
  'ONLINE', NULL, NULL, NULL, '2026-06-26 17:00:00', u.created_at, u.created_at
FROM users u JOIN teacher_profile tp ON tp.user_id = u.id WHERE u.email = 'teacher16@studyflow.com';

-- ----------------------------------------------------------
-- teacher17 (윤태민 / 영어) — 1개
-- ----------------------------------------------------------

-- ㉒ 수능 영어 고난도 유형 특강 [RECRUITING]
INSERT INTO course (teacher_profile_id, subject_id, title, description, target_grade,
                    max_students, duration_minutes, price_per_session, textbook,
                    curriculum_type, curriculum_detail, available_schedule,
                    first_class_date, thumbnail_url, recruit_deadline,
                    start_date, end_date, status, is_listed, is_public_audit,
                    teaching_mode, location, location_lat, location_lng,
                    next_class_at, created_at, updated_at)
SELECT tp.id, (SELECT id FROM subject WHERE category = 'ENGLISH'),
  '수능 영어 고난도 유형 특강',
  '수능 영어 1등급을 위한 고난도 유형(빈칸·순서·삽입) 집중 특강입니다. 원어민 수준 독해력으로 실수 없는 풀이법을 익힙니다.',
  'HIGH_3', 2, 80, 65000, '수능특강 영어 (EBS)', 'FIXED',
  '1~5주: 빈칸추론 전략 / 6~10주: 순서·삽입 유형 / 11~13주: 실전 모의',
  '매주 화·금 오후 7시~8시 20분', '2026-07-01', NULL, '2026-06-27',
  '2026-07-01', '2026-09-26', 'RECRUITING', TRUE, FALSE,
  'ONLINE', NULL, NULL, NULL, '2026-07-01 19:00:00', u.created_at, u.created_at
FROM users u JOIN teacher_profile tp ON tp.user_id = u.id WHERE u.email = 'teacher17@studyflow.com';

-- ----------------------------------------------------------
-- teacher18 (임소율 / 정보·SW) — 2개
-- ----------------------------------------------------------

-- ㉓ C언어 기초 프로그래밍반 [RECRUITING]
INSERT INTO course (teacher_profile_id, subject_id, title, description, target_grade,
                    max_students, duration_minutes, price_per_session, textbook,
                    curriculum_type, curriculum_detail, available_schedule,
                    first_class_date, thumbnail_url, recruit_deadline,
                    start_date, end_date, status, is_listed, is_public_audit,
                    teaching_mode, location, location_lat, location_lng,
                    next_class_at, created_at, updated_at)
SELECT tp.id, (SELECT id FROM subject WHERE category = 'VOCATIONAL'),
  'C언어 기초 프로그래밍반',
  '고등학생 대상 C언어 입문반입니다. 포인터·배열·구조체까지 탄탄히 다지며 SW특기자 전형 대비를 목표로 합니다.',
  'HIGH_1', 3, 90, 58000, 'C언어 기초와 활용 (생능출판)', 'FIXED',
  '1~4주: 기본 입출력·조건문 / 5~8주: 반복문·배열 / 9~12주: 포인터·함수 / 13~15주: 구조체',
  '매주 수·금 오후 6시~7시 30분', '2026-07-02', NULL, '2026-06-28',
  '2026-07-02', '2026-10-10', 'RECRUITING', TRUE, FALSE,
  'ONLINE', NULL, NULL, NULL, '2026-07-02 18:00:00', u.created_at, u.created_at
FROM users u JOIN teacher_profile tp ON tp.user_id = u.id WHERE u.email = 'teacher18@studyflow.com';

-- ㉔ AI·머신러닝 입문반 [RECRUITING]
INSERT INTO course (teacher_profile_id, subject_id, title, description, target_grade,
                    max_students, duration_minutes, price_per_session, textbook,
                    curriculum_type, curriculum_detail, available_schedule,
                    first_class_date, thumbnail_url, recruit_deadline,
                    start_date, end_date, status, is_listed, is_public_audit,
                    teaching_mode, location, location_lat, location_lng,
                    next_class_at, created_at, updated_at)
SELECT tp.id, (SELECT id FROM subject WHERE category = 'VOCATIONAL'),
  'AI·머신러닝 입문반',
  '파이썬 기초를 마친 고등학생 대상 AI 입문반입니다. scikit-learn으로 머신러닝 기본 모델을 직접 구현하며 AI 활용 능력을 기릅니다.',
  'HIGH_2', 2, 100, 72000, '혼자 공부하는 머신러닝+딥러닝 (한빛미디어)', 'FIXED',
  '1~4주: 지도학습(분류·회귀) / 5~8주: 비지도학습 / 9~12주: 신경망 입문 / 13~15주: 프로젝트',
  '매주 토 오후 2시~3시 40분', '2026-07-05', NULL, '2026-07-01',
  '2026-07-05', '2026-10-17', 'RECRUITING', TRUE, FALSE,
  'ONLINE', NULL, NULL, NULL, '2026-07-05 14:00:00', u.created_at, u.created_at
FROM users u JOIN teacher_profile tp ON tp.user_id = u.id WHERE u.email = 'teacher18@studyflow.com';

-- ----------------------------------------------------------
-- teacher19 (한재현 / 국어) — 1개
-- ----------------------------------------------------------

-- ㉕ 고1 국어 내신 기초반 [IN_PROGRESS]
INSERT INTO course (teacher_profile_id, subject_id, title, description, target_grade,
                    max_students, duration_minutes, price_per_session, textbook,
                    curriculum_type, curriculum_detail, available_schedule,
                    first_class_date, thumbnail_url, recruit_deadline,
                    start_date, end_date, status, is_listed, is_public_audit,
                    teaching_mode, location, location_lat, location_lng,
                    next_class_at, created_at, updated_at)
SELECT tp.id, (SELECT id FROM subject WHERE category = 'KOREAN'),
  '고1 국어 내신 기초반',
  '고1 국어 내신을 처음 준비하는 학생들을 위한 기초반입니다. 문법 개념과 문학 작품 분석법을 체계적으로 배웁니다.',
  'HIGH_1', 4, 70, 44000, '고1 국어 교과서 (미래엔)', 'CUSTOM',
  '학교 교과서 및 시험 일정에 맞게 탄력 운영',
  '매주 화·목 오후 4시~5시 10분', '2026-06-10', NULL, '2026-06-03',
  '2026-06-10', '2026-08-28', 'IN_PROGRESS', TRUE, FALSE,
  'ONLINE', NULL, NULL, NULL, '2026-06-26 16:00:00', u.created_at, u.created_at
FROM users u JOIN teacher_profile tp ON tp.user_id = u.id WHERE u.email = 'teacher19@studyflow.com';

-- ----------------------------------------------------------
-- teacher20 (오다인 / 화학) — 2개
-- ----------------------------------------------------------

-- ㉖ 화학1 심화·수능 대비반 [RECRUITING]
INSERT INTO course (teacher_profile_id, subject_id, title, description, target_grade,
                    max_students, duration_minutes, price_per_session, textbook,
                    curriculum_type, curriculum_detail, available_schedule,
                    first_class_date, thumbnail_url, recruit_deadline,
                    start_date, end_date, status, is_listed, is_public_audit,
                    teaching_mode, location, location_lat, location_lng,
                    next_class_at, created_at, updated_at)
SELECT tp.id, (SELECT id FROM subject WHERE category = 'SCIENCE'),
  '화학1 심화·수능 대비반',
  '화학1 개념을 완성한 고2·3 대상 수능 심화반입니다. 최근 5개년 기출 분석과 고난도 문제풀이를 집중적으로 다룹니다.',
  'HIGH_2', 2, 90, 63000, '수능특강 화학1 (EBS)', 'FIXED',
  '1~4주: 원자와 주기율 심화 / 5~8주: 화학 결합·반응속도 / 9~11주: 기출 집중 / 12~14주: 실전 모의',
  '매주 월·수 오후 7시~8시 30분', '2026-07-07', NULL, '2026-07-03',
  '2026-07-07', '2026-10-14', 'RECRUITING', TRUE, FALSE,
  'ONLINE', NULL, NULL, NULL, '2026-07-07 19:00:00', u.created_at, u.created_at
FROM users u JOIN teacher_profile tp ON tp.user_id = u.id WHERE u.email = 'teacher20@studyflow.com';

-- ㉗ 화학올림피아드 기초반 [RECRUITING / OFFLINE]
INSERT INTO course (teacher_profile_id, subject_id, title, description, target_grade,
                    max_students, duration_minutes, price_per_session, textbook,
                    curriculum_type, curriculum_detail, available_schedule,
                    first_class_date, thumbnail_url, recruit_deadline,
                    start_date, end_date, status, is_listed, is_public_audit,
                    teaching_mode, location, location_lat, location_lng,
                    next_class_at, created_at, updated_at)
SELECT tp.id, (SELECT id FROM subject WHERE category = 'SCIENCE'),
  '화학올림피아드 기초반',
  '화학경시대회 및 올림피아드를 준비하는 고등학생 대상 입문반입니다. 실험 이론부터 경시 유형까지 폭넓게 다룹니다.',
  'HIGH_1', 2, 100, 78000, '올림피아드 화학 (자유아카데미)', 'FIXED',
  '1~5주: 물질 구조 심화 / 6~10주: 반응 메커니즘 / 11~14주: 경시 기출 분석',
  '매주 토 오전 10시~11시 40분', '2026-07-05', NULL, '2026-07-01',
  '2026-07-05', '2026-10-17', 'RECRUITING', TRUE, FALSE,
  'OFFLINE', '부산광역시 금정구 부산대학로63번길 2 (스터디룸 301호)', 35.2341, 129.0851,
  '2026-07-05 10:00:00', u.created_at, u.created_at
FROM users u JOIN teacher_profile tp ON tp.user_id = u.id WHERE u.email = 'teacher20@studyflow.com';

-- ----------------------------------------------------------
-- teacher21 (권성민 / 사회) — 1개
-- ----------------------------------------------------------

-- ㉘ 사회탐구 사회·문화 개념반 [RECRUITING]
INSERT INTO course (teacher_profile_id, subject_id, title, description, target_grade,
                    max_students, duration_minutes, price_per_session, textbook,
                    curriculum_type, curriculum_detail, available_schedule,
                    first_class_date, thumbnail_url, recruit_deadline,
                    start_date, end_date, status, is_listed, is_public_audit,
                    teaching_mode, location, location_lat, location_lng,
                    next_class_at, created_at, updated_at)
SELECT tp.id, (SELECT id FROM subject WHERE category = 'SOCIAL_STUDIES'),
  '사회탐구 사회·문화 개념반',
  '수능 사회·문화를 선택한 고2 대상 개념 완성반입니다. 최신 시사와 통계 자료를 활용해 핵심 개념을 자연스럽게 익힙니다.',
  'HIGH_2', 4, 80, 47000, '사회·문화 교과서 (천재교육)', 'FIXED',
  '1~4주: 사회·문화 현상 / 5~8주: 사회 집단과 조직 / 9~11주: 사회 불평등 / 12~14주: 기출 분석',
  '매주 목 오후 6시~7시 20분', '2026-07-03', NULL, '2026-06-29',
  '2026-07-03', '2026-10-09', 'RECRUITING', TRUE, FALSE,
  'ONLINE', NULL, NULL, NULL, '2026-07-03 18:00:00', u.created_at, u.created_at
FROM users u JOIN teacher_profile tp ON tp.user_id = u.id WHERE u.email = 'teacher21@studyflow.com';

-- ----------------------------------------------------------
-- teacher22 (장유나 / 수학) — 1개
-- ----------------------------------------------------------

-- ㉙ 고1 수학 기초 탄탄반 [RECRUITING]
INSERT INTO course (teacher_profile_id, subject_id, title, description, target_grade,
                    max_students, duration_minutes, price_per_session, textbook,
                    curriculum_type, curriculum_detail, available_schedule,
                    first_class_date, thumbnail_url, recruit_deadline,
                    start_date, end_date, status, is_listed, is_public_audit,
                    teaching_mode, location, location_lat, location_lng,
                    next_class_at, created_at, updated_at)
SELECT tp.id, (SELECT id FROM subject WHERE category = 'MATH'),
  '고1 수학 기초 탄탄반',
  '수학이 갑자기 어려워진 고1을 위한 기초 완성반입니다. 다항식·방정식·부등식부터 차근차근 개념을 다집니다.',
  'HIGH_1', 4, 80, 48000, '수학(상) 교과서 (비상교육)', 'CUSTOM',
  '학생 수준에 따라 진도 조정, 내신 시험 직전 집중 대비',
  '매주 화·수 오후 4시~5시 20분', '2026-07-01', NULL, '2026-06-27',
  '2026-07-01', '2026-09-24', 'RECRUITING', TRUE, FALSE,
  'ONLINE', NULL, NULL, NULL, '2026-07-01 16:00:00', u.created_at, u.created_at
FROM users u JOIN teacher_profile tp ON tp.user_id = u.id WHERE u.email = 'teacher22@studyflow.com';

-- ----------------------------------------------------------
-- teacher23 (노준혁 / 영어) — 1개
-- ----------------------------------------------------------

-- ㉚ 수능 영어 독해 전략반 [RECRUITING]
INSERT INTO course (teacher_profile_id, subject_id, title, description, target_grade,
                    max_students, duration_minutes, price_per_session, textbook,
                    curriculum_type, curriculum_detail, available_schedule,
                    first_class_date, thumbnail_url, recruit_deadline,
                    start_date, end_date, status, is_listed, is_public_audit,
                    teaching_mode, location, location_lat, location_lng,
                    next_class_at, created_at, updated_at)
SELECT tp.id, (SELECT id FROM subject WHERE category = 'ENGLISH'),
  '수능 영어 독해 전략반',
  '수능 영어 장문 독해와 어휘 문제를 집중 공략하는 고3 대상 특강입니다. 독해 속도와 정확도를 동시에 향상시킵니다.',
  'HIGH_3', 3, 80, 58000, '수능완성 영어영역 (EBS)', 'FIXED',
  '1~4주: 장문 독해 전략 / 5~8주: 어휘·어법 집중 / 9~11주: 복합문항 / 12~13주: 실전 모의',
  '매주 화·목 오후 7시~8시 20분', '2026-07-01', NULL, '2026-06-27',
  '2026-07-01', '2026-09-25', 'RECRUITING', TRUE, FALSE,
  'ONLINE', NULL, NULL, NULL, '2026-07-01 19:00:00', u.created_at, u.created_at
FROM users u JOIN teacher_profile tp ON tp.user_id = u.id WHERE u.email = 'teacher23@studyflow.com';

-- ----------------------------------------------------------
-- teacher26 (배수아 / 수학) — 1개
-- ----------------------------------------------------------

-- ㉛ 고등 수학 기본기 완성반 [RECRUITING]
INSERT INTO course (teacher_profile_id, subject_id, title, description, target_grade,
                    max_students, duration_minutes, price_per_session, textbook,
                    curriculum_type, curriculum_detail, available_schedule,
                    first_class_date, thumbnail_url, recruit_deadline,
                    start_date, end_date, status, is_listed, is_public_audit,
                    teaching_mode, location, location_lat, location_lng,
                    next_class_at, created_at, updated_at)
SELECT tp.id, (SELECT id FROM subject WHERE category = 'MATH'),
  '고등 수학 기본기 완성반',
  '수학 기초가 부족한 고1·2를 위한 기본기 완성반입니다. 개념 이해와 반복 문제풀이로 자신감을 회복합니다.',
  'HIGH_1', 4, 80, 46000, '수학(상) 교과서 (동아출판)', 'CUSTOM',
  '학생 수준 진단 후 맞춤 커리큘럼 구성',
  '매주 월·수 오후 5시~6시 20분', '2026-07-07', NULL, '2026-07-03',
  '2026-07-07', '2026-09-24', 'RECRUITING', TRUE, FALSE,
  'ONLINE', NULL, NULL, NULL, '2026-07-07 17:00:00', u.created_at, u.created_at
FROM users u JOIN teacher_profile tp ON tp.user_id = u.id WHERE u.email = 'teacher26@studyflow.com';

-- ----------------------------------------------------------
-- teacher27 (서현준 / 영어) — 1개
-- ----------------------------------------------------------

-- ㉜ 중등 영어 문법 기초반 [RECRUITING]
INSERT INTO course (teacher_profile_id, subject_id, title, description, target_grade,
                    max_students, duration_minutes, price_per_session, textbook,
                    curriculum_type, curriculum_detail, available_schedule,
                    first_class_date, thumbnail_url, recruit_deadline,
                    start_date, end_date, status, is_listed, is_public_audit,
                    teaching_mode, location, location_lat, location_lng,
                    next_class_at, created_at, updated_at)
SELECT tp.id, (SELECT id FROM subject WHERE category = 'ENGLISH'),
  '중등 영어 문법 기초반',
  '중학교 1~2학년을 위한 영어 문법 기초반입니다. 품사·시제·문장 구조를 체계적으로 익히며 내신 기초를 다집니다.',
  'MIDDLE_1', 4, 60, 40000, '그래머 인 유즈 (Cambridge)', 'FIXED',
  '1~4주: 품사와 문장 성분 / 5~8주: 시제 / 9~11주: to부정사·동명사 / 12~13주: 실전',
  '매주 화·목 오후 4시~5시', '2026-07-08', NULL, '2026-07-04',
  '2026-07-08', '2026-09-25', 'RECRUITING', TRUE, FALSE,
  'ONLINE', NULL, NULL, NULL, '2026-07-08 16:00:00', u.created_at, u.created_at
FROM users u JOIN teacher_profile tp ON tp.user_id = u.id WHERE u.email = 'teacher27@studyflow.com';

-- ----------------------------------------------------------
-- teacher28 (안지수 / 국어) — 1개
-- ----------------------------------------------------------

-- ㉝ 중등 국어 독해력 강화반 [RECRUITING]
INSERT INTO course (teacher_profile_id, subject_id, title, description, target_grade,
                    max_students, duration_minutes, price_per_session, textbook,
                    curriculum_type, curriculum_detail, available_schedule,
                    first_class_date, thumbnail_url, recruit_deadline,
                    start_date, end_date, status, is_listed, is_public_audit,
                    teaching_mode, location, location_lat, location_lng,
                    next_class_at, created_at, updated_at)
SELECT tp.id, (SELECT id FROM subject WHERE category = 'KOREAN'),
  '중등 국어 독해력 강화반',
  '중학교 전 학년 대상 독해력 향상반입니다. 비문학 지문 독해 전략과 문법 기초를 동시에 잡아 내신 국어 실력을 키웁니다.',
  'MIDDLE_2', 4, 60, 42000, '중학 국어 독해 100 (이룸이앤비)', 'FIXED',
  '1~4주: 설명문 독해 / 5~8주: 논설문 독해 / 9~11주: 문학 감상 / 12~13주: 내신 대비',
  '매주 수 오후 4시~5시', '2026-07-02', NULL, '2026-06-28',
  '2026-07-02', '2026-09-24', 'RECRUITING', TRUE, FALSE,
  'ONLINE', NULL, NULL, NULL, '2026-07-02 16:00:00', u.created_at, u.created_at
FROM users u JOIN teacher_profile tp ON tp.user_id = u.id WHERE u.email = 'teacher28@studyflow.com';

-- ----------------------------------------------------------
-- teacher29 (전도현 / 화학) — 1개
-- ----------------------------------------------------------

-- ㉞ 화학 기초 개념반 [RECRUITING]
INSERT INTO course (teacher_profile_id, subject_id, title, description, target_grade,
                    max_students, duration_minutes, price_per_session, textbook,
                    curriculum_type, curriculum_detail, available_schedule,
                    first_class_date, thumbnail_url, recruit_deadline,
                    start_date, end_date, status, is_listed, is_public_audit,
                    teaching_mode, location, location_lat, location_lng,
                    next_class_at, created_at, updated_at)
SELECT tp.id, (SELECT id FROM subject WHERE category = 'SCIENCE'),
  '화학 기초 개념반',
  '화학을 처음 배우거나 기초가 부족한 고1 대상 개념반입니다. 원소·원자·분자 개념부터 차근차근 쌓아 올립니다.',
  'HIGH_1', 4, 70, 44000, '화학1 교과서 (미래엔)', 'FIXED',
  '1~4주: 물질의 세계 / 5~8주: 원자 구조 / 9~11주: 화학 결합 기초 / 12~13주: 내신 대비',
  '매주 화·금 오후 4시~5시 10분', '2026-07-01', NULL, '2026-06-27',
  '2026-07-01', '2026-09-26', 'RECRUITING', TRUE, FALSE,
  'ONLINE', NULL, NULL, NULL, '2026-07-01 16:00:00', u.created_at, u.created_at
FROM users u JOIN teacher_profile tp ON tp.user_id = u.id WHERE u.email = 'teacher29@studyflow.com';

-- ----------------------------------------------------------
-- teacher30 (최보라 / 정보) — 1개
-- ----------------------------------------------------------

-- ㉟ 정보 과목 기초반 [RECRUITING]
INSERT INTO course (teacher_profile_id, subject_id, title, description, target_grade,
                    max_students, duration_minutes, price_per_session, textbook,
                    curriculum_type, curriculum_detail, available_schedule,
                    first_class_date, thumbnail_url, recruit_deadline,
                    start_date, end_date, status, is_listed, is_public_audit,
                    teaching_mode, location, location_lat, location_lng,
                    next_class_at, created_at, updated_at)
SELECT tp.id, (SELECT id FROM subject WHERE category = 'VOCATIONAL'),
  '정보 과목 기초반 (스크래치·파이썬 입문)',
  '중학교 정보 교과 및 코딩 입문을 위한 기초반입니다. 스크래치로 논리력을 키우고 파이썬 기초까지 연결합니다.',
  'MIDDLE_1', 4, 60, 40000, '중학 정보 교과서 (비상교육)', 'FIXED',
  '1~4주: 스크래치 기초 / 5~8주: 알고리즘 사고 / 9~11주: 파이썬 입문 / 12~13주: 미니 프로젝트',
  '매주 토 오전 10시~11시', '2026-07-05', NULL, '2026-07-01',
  '2026-07-05', '2026-09-27', 'RECRUITING', TRUE, FALSE,
  'ONLINE', NULL, NULL, NULL, '2026-07-05 10:00:00', u.created_at, u.created_at
FROM users u JOIN teacher_profile tp ON tp.user_id = u.id WHERE u.email = 'teacher30@studyflow.com';

SET FOREIGN_KEY_CHECKS = 1;
