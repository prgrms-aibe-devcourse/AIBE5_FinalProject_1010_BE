-- ============================================================
-- Course 더미 데이터: APPROVED 선생님 7명, 총 12개 수업
-- teacher1(수학)   : 2개 | teacher2(영어)  : 2개
-- teacher3(물리)   : 1개 | teacher4(화학)  : 2개
-- teacher5(국어)   : 2개 | teacher6(사회)  : 1개
-- teacher7(정보)   : 2개
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

SET FOREIGN_KEY_CHECKS = 1;
