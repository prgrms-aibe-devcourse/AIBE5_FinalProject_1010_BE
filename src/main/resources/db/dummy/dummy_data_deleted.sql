-- ============================================================
-- 탈퇴 사용자 더미 데이터: 선생님 7명, 학생 8명 (총 15명)
-- 날짜별 분포 (불균형):
--   6/19: 1명 | 6/20: 4명 | 6/21: 2명 | 6/22: 0명
--   6/23: 4명 | 6/24: 3명 | 6/25: 1명
-- is_deleted = 해당 user.id (INSERT 후 UPDATE로 처리)
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- STEP 1: 탈퇴 사용자 INSERT (is_deleted=0으로 먼저 삽입)
-- ============================================================
INSERT INTO users (email, password, name, phone, profile_image_url, role, social_provider, social_id, is_verified, gender, birth_date, is_active, deleted_at, is_deleted, marketing_agreed, created_at, updated_at) VALUES

-- 6/19 탈퇴 (1명: 선생님)
('del_teacher01@studyflow.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu',
 '강태양', '010-9001-0001', NULL, 'TEACHER', 'LOCAL', NULL, FALSE, 'MALE', '1991-04-12',
 FALSE, '2026-06-19 21:45:00', 0, FALSE, '2026-06-18 10:20:00', '2026-06-19 21:45:00'),

-- 6/20 탈퇴 (4명: 학생2, 선생님2, 학생3, 학생4)
('del_student01@studyflow.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu',
 '조민채', '010-9002-0001', NULL, 'STUDENT', 'LOCAL', NULL, FALSE, 'FEMALE', '2008-07-03',
 FALSE, '2026-06-20 10:15:00', 0, FALSE, '2026-06-20 08:50:00', '2026-06-20 10:15:00'),

('del_teacher02@studyflow.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu',
 '한소희', '010-9001-0002', NULL, 'TEACHER', 'LOCAL', NULL, FALSE, 'FEMALE', '1989-02-28',
 FALSE, '2026-06-20 14:30:00', 0, FALSE, '2026-06-17 16:40:00', '2026-06-20 14:30:00'),

('del_student02@studyflow.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu',
 '임서율', '010-9002-0002', NULL, 'STUDENT', 'LOCAL', NULL, FALSE, 'MALE', '2007-11-19',
 FALSE, '2026-06-20 18:55:00', 0, TRUE, '2026-06-20 13:10:00', '2026-06-20 18:55:00'),

('del_student03@studyflow.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu',
 '신지호', '010-9002-0003', NULL, 'STUDENT', 'LOCAL', NULL, FALSE, 'MALE', '2009-03-22',
 FALSE, '2026-06-20 23:20:00', 0, FALSE, '2026-06-20 19:45:00', '2026-06-20 23:20:00'),

-- 6/21 탈퇴 (2명: 선생님3, 학생4)
('del_teacher03@studyflow.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu',
 '오준혁', '010-9001-0003', NULL, 'TEACHER', 'LOCAL', NULL, FALSE, 'MALE', '1993-08-15',
 FALSE, '2026-06-21 11:20:00', 0, FALSE, '2026-06-19 08:55:00', '2026-06-21 11:20:00'),

('del_student04@studyflow.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu',
 '배유진', '010-9002-0004', NULL, 'STUDENT', 'LOCAL', NULL, FALSE, 'FEMALE', '2007-06-08',
 FALSE, '2026-06-21 20:40:00', 0, TRUE, '2026-06-20 14:15:00', '2026-06-21 20:40:00'),

-- 6/22 탈퇴 없음

-- 6/23 탈퇴 (4명: 선생님4, 학생5, 선생님5, 학생6)
('del_teacher04@studyflow.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu',
 '문아름', '010-9001-0004', NULL, 'TEACHER', 'LOCAL', NULL, FALSE, 'FEMALE', '1990-12-05',
 FALSE, '2026-06-23 09:50:00', 0, FALSE, '2026-06-22 09:00:00', '2026-06-23 09:50:00'),

('del_student05@studyflow.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu',
 '고은서', '010-9002-0005', NULL, 'STUDENT', 'LOCAL', NULL, FALSE, 'FEMALE', '2008-10-17',
 FALSE, '2026-06-23 13:25:00', 0, FALSE, '2026-06-22 18:20:00', '2026-06-23 13:25:00'),

('del_teacher05@studyflow.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu',
 '서도훈', '010-9001-0005', NULL, 'TEACHER', 'LOCAL', NULL, FALSE, 'MALE', '1987-05-30',
 FALSE, '2026-06-23 17:10:00', 0, FALSE, '2026-06-18 11:30:00', '2026-06-23 17:10:00'),

('del_student06@studyflow.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu',
 '남현준', '010-9002-0006', NULL, 'STUDENT', 'LOCAL', NULL, FALSE, 'MALE', '2007-02-14',
 FALSE, '2026-06-23 22:35:00', 0, TRUE, '2026-06-23 10:50:00', '2026-06-23 22:35:00'),

-- 6/24 탈퇴 (3명: 선생님6, 학생7, 선생님7)
('del_teacher06@studyflow.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu',
 '윤재민', '010-9001-0006', NULL, 'TEACHER', 'LOCAL', NULL, FALSE, 'MALE', '1992-09-01',
 FALSE, '2026-06-24 11:05:00', 0, FALSE, '2026-06-21 13:10:00', '2026-06-24 11:05:00'),

('del_student07@studyflow.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu',
 '장하나', '010-9002-0007', NULL, 'STUDENT', 'LOCAL', NULL, FALSE, 'FEMALE', '2009-08-29',
 FALSE, '2026-06-24 16:30:00', 0, FALSE, '2026-06-24 12:00:00', '2026-06-24 16:30:00'),

('del_teacher07@studyflow.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu',
 '이나라', '010-9001-0007', NULL, 'TEACHER', 'LOCAL', NULL, FALSE, 'FEMALE', '1994-01-18',
 FALSE, '2026-06-24 21:15:00', 0, FALSE, '2026-06-23 20:45:00', '2026-06-24 21:15:00'),

-- 6/25 탈퇴 (1명: 학생8)
('del_student08@studyflow.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu',
 '권도영', '010-9002-0008', NULL, 'STUDENT', 'LOCAL', NULL, FALSE, 'MALE', '2008-04-25',
 FALSE, '2026-06-25 19:50:00', 0, TRUE, '2026-06-25 11:20:00', '2026-06-25 19:50:00');

-- ============================================================
-- STEP 2: is_deleted = 자신의 id 로 업데이트
-- ============================================================
UPDATE users
SET is_deleted = id
WHERE email IN (
  'del_teacher01@studyflow.com', 'del_teacher02@studyflow.com',
  'del_teacher03@studyflow.com', 'del_teacher04@studyflow.com',
  'del_teacher05@studyflow.com', 'del_teacher06@studyflow.com',
  'del_teacher07@studyflow.com',
  'del_student01@studyflow.com', 'del_student02@studyflow.com',
  'del_student03@studyflow.com', 'del_student04@studyflow.com',
  'del_student05@studyflow.com', 'del_student06@studyflow.com',
  'del_student07@studyflow.com', 'del_student08@studyflow.com'
);

-- ============================================================
-- STEP 3: TeacherProfile (탈퇴 선생님 7명)
-- ============================================================
INSERT INTO teacher_profile (user_id, career, major, admission_year, awards, address, teaching_style, introduction, naegong_score, is_listed, total_teaching_hours, created_at, updated_at)
SELECT u.id,
       t.career, t.major, t.admission_year, t.awards, t.address, t.teaching_style, t.introduction, t.naegong_score, t.is_listed, t.total_teaching_hours,
       u.created_at, u.created_at
FROM users u
JOIN (
  SELECT 'del_teacher01@studyflow.com' AS email, '경희대학교'       AS career, '수학교육학과'  AS major, '15학번' AS admission_year, NULL                           AS awards, '서울 강북구'  AS address, '기초 개념 중심 수업'         AS teaching_style, '수학을 쉽게 가르치는 강사입니다.'      AS introduction, 60 AS naegong_score, FALSE AS is_listed, 120.0 AS total_teaching_hours UNION ALL
  SELECT 'del_teacher02@studyflow.com', '인하대학교',   '영어교육학과',  '13학번', '전국 영어말하기대회 지도 수상', '인천 남동구',  '발음 교정 중심 수업',           '영어 발음과 회화 전문 강사입니다.',              55, FALSE, 85.5  UNION ALL
  SELECT 'del_teacher03@studyflow.com', '건국대학교',   '물리학과',      '17학번', NULL,                           '서울 광진구',  '개념과 문제풀이 병행 수업',     '물리 기초부터 심화까지 지도합니다.',             48, FALSE, 42.0  UNION ALL
  SELECT 'del_teacher04@studyflow.com', '세종대학교',   '국어교육학과',  '14학번', NULL,                           '서울 광진구',  '읽기·쓰기 통합 수업',           '국어 문법과 독해 전문 강사입니다.',              52, FALSE, 65.0  UNION ALL
  SELECT 'del_teacher05@studyflow.com', '아주대학교',   '화학공학과',    '11학번', '전국 화학 경시 입상',           '경기 수원시',  '실생활 연계 화학 수업',         '화학·과학 15년 경력 보유입니다.',                70, TRUE,  540.0 UNION ALL
  SELECT 'del_teacher06@studyflow.com', '동국대학교',   '역사교육학과',  '16학번', NULL,                           '서울 중구',    '스토리텔링 역사 수업',          '한국사·세계사 통합 지도합니다.',                 45, FALSE, 30.0  UNION ALL
  SELECT 'del_teacher07@studyflow.com', '숙명여자대학교','수학과',        '18학번', NULL,                           '서울 용산구',  '단계별 맞춤 수업',              '수학 기초 확립 전문 강사입니다.',                38, FALSE, 15.5
) t ON u.email = t.email
WHERE u.role = 'TEACHER';

-- ============================================================
-- STEP 4: StudentProfile (탈퇴 학생 8명)
-- ============================================================
INSERT INTO student_profile (user_id, grade, interest_subjects, region, goal, created_at, updated_at)
SELECT u.id,
       s.grade, s.interest_subjects, s.region, s.goal,
       u.created_at, u.created_at
FROM users u
JOIN (
  SELECT 'del_student01@studyflow.com' AS email, '고2' AS grade, '수학,영어' AS interest_subjects, '서울 강북구'  AS region, '내신 성적 향상'      AS goal UNION ALL
  SELECT 'del_student02@studyflow.com', '고1', '국어,사회',   '경기 안산시',  '수능 준비 시작'      UNION ALL
  SELECT 'del_student03@studyflow.com', '중2', '수학,과학',   '서울 노원구',  '중등 선행 완성'      UNION ALL
  SELECT 'del_student04@studyflow.com', '고3', '영어,수학',   '경기 의왕시',  '수능 최저 달성'      UNION ALL
  SELECT 'del_student05@studyflow.com', '고2', '화학,생물',   '서울 동대문구','이과 내신 2등급'     UNION ALL
  SELECT 'del_student06@studyflow.com', '고1', '수학,물리',   '부산 남구',    '이공계 진학 준비'    UNION ALL
  SELECT 'del_student07@studyflow.com', '중3', '국어,영어',   '경기 광명시',  '고등학교 입학 준비'  UNION ALL
  SELECT 'del_student08@studyflow.com', '고2', '수학,영어',   '인천 서구',    '수능 고득점 목표'
) s ON u.email = s.email
WHERE u.role = 'STUDENT';

SET FOREIGN_KEY_CHECKS = 1;

-- voice_call_enabled 컬럼은 User 엔티티에 추가되어 ddl-auto 로 자동 생성됨.
-- DB 레벨 DEFAULT 설정은 DummyDataInitializer.setVoiceCallEnabledDefault() 에서 처리.
