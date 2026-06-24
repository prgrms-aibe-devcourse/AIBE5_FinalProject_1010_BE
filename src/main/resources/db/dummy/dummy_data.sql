-- ============================================================
-- 더미 데이터: 선생님 10명, 학생 100명
-- 비밀번호 모두 Test1234! (BCrypt 해시)
-- is_verified: 전원 FALSE
-- created_at 분포 (총 110명):
--   6/19: 5명 | 6/20: 10명 | 6/21: 14명 | 6/22: 16명
--   6/23: 19명 | 6/24: 22명 | 6/25: 24명
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 선생님 Users (10명)
-- ============================================================
INSERT INTO users (email, password, name, phone, profile_image_url, role, social_provider, social_id, is_verified, gender, birth_date, is_active, deleted_at, is_deleted, marketing_agreed, created_at, updated_at) VALUES
-- 6/19 (2명)
('teacher1@studyflow.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu', '김민준', '010-1001-0001', NULL, 'TEACHER', 'LOCAL', NULL, FALSE, 'MALE',   '1990-03-15', TRUE, NULL, 0, FALSE, '2026-06-19 09:15:00', '2026-06-19 09:15:00'),
('teacher2@studyflow.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu', '이서연', '010-1001-0002', NULL, 'TEACHER', 'LOCAL', NULL, FALSE, 'FEMALE', '1988-07-22', TRUE, NULL, 0, FALSE, '2026-06-19 11:32:00', '2026-06-19 11:32:00'),
-- 6/20 (2명)
('teacher3@studyflow.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu', '박지훈', '010-1001-0003', NULL, 'TEACHER', 'LOCAL', NULL, FALSE, 'MALE',   '1992-11-08', TRUE, NULL, 0, FALSE, '2026-06-20 08:30:00', '2026-06-20 08:30:00'),
('teacher4@studyflow.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu', '최수진', '010-1001-0004', NULL, 'TEACHER', 'LOCAL', NULL, FALSE, 'FEMALE', '1991-05-30', TRUE, NULL, 0, FALSE, '2026-06-20 10:15:00', '2026-06-20 10:15:00'),
-- 6/21 (2명)
('teacher5@studyflow.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu', '정도현', '010-1001-0005', NULL, 'TEACHER', 'LOCAL', NULL, FALSE, 'MALE',   '1987-09-17', TRUE, NULL, 0, FALSE, '2026-06-21 09:00:00', '2026-06-21 09:00:00'),
('teacher6@studyflow.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu', '윤하은', '010-1001-0006', NULL, 'TEACHER', 'LOCAL', NULL, FALSE, 'FEMALE', '1993-02-14', TRUE, NULL, 0, FALSE, '2026-06-21 11:45:00', '2026-06-21 11:45:00'),
-- 6/22 (2명)
('teacher7@studyflow.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu', '임재원', '010-1001-0007', NULL, 'TEACHER', 'LOCAL', NULL, FALSE, 'MALE',   '1989-12-03', TRUE, NULL, 0, FALSE, '2026-06-22 08:45:00', '2026-06-22 08:45:00'),
('teacher8@studyflow.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu', '한지민', '010-1001-0008', NULL, 'TEACHER', 'LOCAL', NULL, FALSE, 'FEMALE', '1994-06-25', TRUE, NULL, 0, FALSE, '2026-06-22 10:30:00', '2026-06-22 10:30:00'),
-- 6/23 (2명)
('teacher9@studyflow.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu', '오승현', '010-1001-0009', NULL, 'TEACHER', 'LOCAL', NULL, FALSE, 'MALE',   '1986-04-11', TRUE, NULL, 0, FALSE, '2026-06-23 09:20:00', '2026-06-23 09:20:00'),
('teacher10@studyflow.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu', '신예린', '010-1001-0010', NULL, 'TEACHER', 'LOCAL', NULL, FALSE, 'FEMALE', '1995-08-29', TRUE, NULL, 0, FALSE, '2026-06-23 10:40:00', '2026-06-23 10:40:00');

-- ============================================================
-- TeacherProfile (10명)
-- ============================================================
INSERT INTO teacher_profile (user_id, career, major, admission_year, awards, address, teaching_style, introduction, naegong_score, is_listed, total_teaching_hours, created_at, updated_at)
SELECT u.id,
       t.career, t.major, t.admission_year, t.awards, t.address, t.teaching_style, t.introduction, t.naegong_score, t.is_listed, t.total_teaching_hours,
       u.created_at, u.created_at
FROM users u
JOIN (
  SELECT 'teacher1@studyflow.com'  AS email, '서울대학교'   AS career, '수학과'       AS major, '12학번' AS admission_year, '전국수학올림피아드 금상'     AS awards, '서울 강남구'   AS address, '개념 중심의 단계별 수업'       AS teaching_style, '10년 경력의 수학 전문 강사입니다.'           AS introduction, 95 AS naegong_score, TRUE AS is_listed, 1200.0 AS total_teaching_hours UNION ALL
  SELECT 'teacher2@studyflow.com',  '연세대학교',   '영어영문학과', '14학번', 'TOEIC 990 보유',               '서울 서초구',  '회화 중심 실용 영어 수업',      '영미문학 전공, 회화·문법 전문 강사입니다.',     88, TRUE,  980.5  UNION ALL
  SELECT 'teacher3@studyflow.com',  '고려대학교',   '물리학과',    '16학번', '한국물리올림피아드 은상',        '서울 마포구',  '실험과 이론을 결합한 수업',      '물리의 재미를 알려드리는 강사입니다.',           82, TRUE,  750.0  UNION ALL
  SELECT 'teacher4@studyflow.com',  '성균관대학교', '화학과',      '15학번', '대한화학회 우수논문상',          '경기 성남시',  '시각화 자료 활용 수업',          '화학을 쉽고 재미있게 가르칩니다.',               79, TRUE,  620.5  UNION ALL
  SELECT 'teacher5@studyflow.com',  '한양대학교',   '국어국문학과','11학번', '전국 논술 지도 우수교사상',      '서울 송파구',  '독해력·논리력 강화 수업',        '국어·논술 17년 경력 강사입니다.',                91, TRUE,  1450.0 UNION ALL
  SELECT 'teacher6@studyflow.com',  '이화여자대학교','사회학과',   '17학번', '사회탐구 1등급 배출 다수',       '서울 용산구',  '시사 연계 개념 수업',            '사회탐구 전 과목 지도 가능합니다.',              75, TRUE,  480.0  UNION ALL
  SELECT 'teacher7@studyflow.com',  'KAIST',        '전산학부',    '13학번', '정보올림피아드 입상',            '대전 유성구',  '프로젝트 기반 코딩 수업',        '알고리즘과 SW 개발을 가르칩니다.',               85, TRUE,  860.5  UNION ALL
  SELECT 'teacher8@studyflow.com',  '부산대학교',   '생물학과',    '18학번', '전국 생물탐구대회 대상',         '부산 해운대구','생명과학 실험 연계 수업',        '생명과학 전공, 탐구 실험 전문 강사입니다.',      72, TRUE,  390.0  UNION ALL
  SELECT 'teacher9@studyflow.com',  '서강대학교',   '경제학과',    '10학번', '경제 논술 지도 수상',            '서울 영등포구','논리적 사고력 향상 수업',        '경제·사회 16년 강사 경력 보유입니다.',           93, TRUE,  1680.0 UNION ALL
  SELECT 'teacher10@studyflow.com', '중앙대학교',   '음악교육학과','19학번', '전국 음악 콩쿠르 입상',          '서울 동작구',  '기초부터 심화까지 체계적 수업',  '피아노·음악이론 전공 강사입니다.',               68, TRUE,  270.5
) t ON u.email = t.email
WHERE u.role = 'TEACHER';

-- ============================================================
-- 학생 Users (100명)
-- ============================================================
INSERT INTO users (email, password, name, phone, profile_image_url, role, social_provider, social_id, is_verified, gender, birth_date, is_active, deleted_at, is_deleted, marketing_agreed, created_at, updated_at) VALUES
-- 6/19 (3명: student001~003)
('student001@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','김지우','010-2001-0001',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2008-01-05',TRUE,NULL,0,TRUE, '2026-06-19 14:22:00','2026-06-19 14:22:00'),
('student002@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','이하은','010-2001-0002',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2007-03-18',TRUE,NULL,0,FALSE,'2026-06-19 16:48:00','2026-06-19 16:48:00'),
('student003@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','박서준','010-2001-0003',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2009-06-22',TRUE,NULL,0,TRUE, '2026-06-19 20:10:00','2026-06-19 20:10:00'),
-- 6/20 (8명: student004~011)
('student004@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','최민서','010-2001-0004',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2008-09-11',TRUE,NULL,0,FALSE,'2026-06-20 12:40:00','2026-06-20 12:40:00'),
('student005@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','정예준','010-2001-0005',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2007-12-30',TRUE,NULL,0,TRUE, '2026-06-20 13:55:00','2026-06-20 13:55:00'),
('student006@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','윤지아','010-2001-0006',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2009-02-14',TRUE,NULL,0,FALSE,'2026-06-20 15:20:00','2026-06-20 15:20:00'),
('student007@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','임도윤','010-2001-0007',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2008-04-07',TRUE,NULL,0,TRUE, '2026-06-20 16:10:00','2026-06-20 16:10:00'),
('student008@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','한수아','010-2001-0008',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2007-07-19',TRUE,NULL,0,TRUE, '2026-06-20 17:35:00','2026-06-20 17:35:00'),
('student009@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','오시우','010-2001-0009',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2009-10-03',TRUE,NULL,0,FALSE,'2026-06-20 18:50:00','2026-06-20 18:50:00'),
('student010@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','신나은','010-2001-0010',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2008-11-25',TRUE,NULL,0,TRUE, '2026-06-20 20:05:00','2026-06-20 20:05:00'),
('student011@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','강준호','010-2001-0011',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2007-01-17',TRUE,NULL,0,FALSE,'2026-06-20 21:30:00','2026-06-20 21:30:00'),
-- 6/21 (12명: student012~023)
('student012@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','조아린','010-2001-0012',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2009-05-08',TRUE,NULL,0,TRUE, '2026-06-21 13:10:00','2026-06-21 13:10:00'),
('student013@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','장민혁','010-2001-0013',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2008-08-21',TRUE,NULL,0,TRUE, '2026-06-21 14:25:00','2026-06-21 14:25:00'),
('student014@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','류채원','010-2001-0014',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2007-11-14',TRUE,NULL,0,FALSE,'2026-06-21 15:30:00','2026-06-21 15:30:00'),
('student015@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','권도현','010-2001-0015',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2009-03-29',TRUE,NULL,0,TRUE, '2026-06-21 16:15:00','2026-06-21 16:15:00'),
('student016@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','황소율','010-2001-0016',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2008-06-06',TRUE,NULL,0,FALSE,'2026-06-21 17:00:00','2026-06-21 17:00:00'),
('student017@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','문재현','010-2001-0017',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2007-09-12',TRUE,NULL,0,TRUE, '2026-06-21 18:20:00','2026-06-21 18:20:00'),
('student018@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','배지유','010-2001-0018',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2009-12-01',TRUE,NULL,0,TRUE, '2026-06-21 19:05:00','2026-06-21 19:05:00'),
('student019@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','서이준','010-2001-0019',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2008-02-23',TRUE,NULL,0,FALSE,'2026-06-21 19:50:00','2026-06-21 19:50:00'),
('student020@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','고아연','010-2001-0020',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2007-05-16',TRUE,NULL,0,TRUE, '2026-06-21 20:35:00','2026-06-21 20:35:00'),
('student021@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','남건우','010-2001-0021',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2009-07-28',TRUE,NULL,0,FALSE,'2026-06-21 21:15:00','2026-06-21 21:15:00'),
('student022@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','구나영','010-2001-0022',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2008-10-09',TRUE,NULL,0,TRUE, '2026-06-21 22:00:00','2026-06-21 22:00:00'),
('student023@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','노태양','010-2001-0023',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2007-01-31',TRUE,NULL,0,TRUE, '2026-06-21 22:40:00','2026-06-21 22:40:00'),
-- 6/22 (14명: student024~037)
('student024@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','도하율','010-2001-0024',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2009-04-20',TRUE,NULL,0,FALSE,'2026-06-22 12:15:00','2026-06-22 12:15:00'),
('student025@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','마준서','010-2001-0025',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2008-07-13',TRUE,NULL,0,TRUE, '2026-06-22 13:00:00','2026-06-22 13:00:00'),
('student026@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','바다솔','010-2001-0026',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2007-10-05',TRUE,NULL,0,FALSE,'2026-06-22 13:45:00','2026-06-22 13:45:00'),
('student027@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','사민준','010-2001-0027',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2009-12-27',TRUE,NULL,0,TRUE, '2026-06-22 14:30:00','2026-06-22 14:30:00'),
('student028@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','아지유','010-2001-0028',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2008-03-18',TRUE,NULL,0,TRUE, '2026-06-22 15:10:00','2026-06-22 15:10:00'),
('student029@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','차우진','010-2001-0029',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2007-06-09',TRUE,NULL,0,FALSE,'2026-06-22 15:55:00','2026-06-22 15:55:00'),
('student030@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','타소희','010-2001-0030',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2009-09-01',TRUE,NULL,0,TRUE, '2026-06-22 16:40:00','2026-06-22 16:40:00'),
('student031@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','파이준','010-2001-0031',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2008-11-22',TRUE,NULL,0,FALSE,'2026-06-22 17:25:00','2026-06-22 17:25:00'),
('student032@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','하세린','010-2001-0032',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2007-02-14',TRUE,NULL,0,TRUE, '2026-06-22 18:10:00','2026-06-22 18:10:00'),
('student033@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','김시온','010-2001-0033',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2009-05-06',TRUE,NULL,0,TRUE, '2026-06-22 18:55:00','2026-06-22 18:55:00'),
('student034@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','이다인','010-2001-0034',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2008-08-28',TRUE,NULL,0,FALSE,'2026-06-22 19:40:00','2026-06-22 19:40:00'),
('student035@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','박재민','010-2001-0035',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2007-11-19',TRUE,NULL,0,TRUE, '2026-06-22 20:25:00','2026-06-22 20:25:00'),
('student036@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','최유나','010-2001-0036',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2009-01-10',TRUE,NULL,0,FALSE,'2026-06-22 21:10:00','2026-06-22 21:10:00'),
('student037@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','정성민','010-2001-0037',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2008-04-02',TRUE,NULL,0,TRUE, '2026-06-22 21:55:00','2026-06-22 21:55:00'),
-- 6/23 (17명: student038~054)
('student038@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','윤서현','010-2001-0038',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2007-06-24',TRUE,NULL,0,TRUE, '2026-06-23 11:55:00','2026-06-23 11:55:00'),
('student039@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','임현준','010-2001-0039',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2009-09-15',TRUE,NULL,0,FALSE,'2026-06-23 13:10:00','2026-06-23 13:10:00'),
('student040@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','한예지','010-2001-0040',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2008-12-07',TRUE,NULL,0,TRUE, '2026-06-23 13:50:00','2026-06-23 13:50:00'),
('student041@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','오재혁','010-2001-0041',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2007-03-01',TRUE,NULL,0,FALSE,'2026-06-23 14:35:00','2026-06-23 14:35:00'),
('student042@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','신보미','010-2001-0042',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2009-05-23',TRUE,NULL,0,TRUE, '2026-06-23 15:15:00','2026-06-23 15:15:00'),
('student043@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','강태훈','010-2001-0043',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2008-08-14',TRUE,NULL,0,TRUE, '2026-06-23 16:00:00','2026-06-23 16:00:00'),
('student044@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','조하린','010-2001-0044',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2007-11-06',TRUE,NULL,0,FALSE,'2026-06-23 16:40:00','2026-06-23 16:40:00'),
('student045@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','장준혁','010-2001-0045',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2009-01-28',TRUE,NULL,0,TRUE, '2026-06-23 17:25:00','2026-06-23 17:25:00'),
('student046@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','류아현','010-2001-0046',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2008-04-20',TRUE,NULL,0,FALSE,'2026-06-23 18:05:00','2026-06-23 18:05:00'),
('student047@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','권민재','010-2001-0047',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2007-07-12',TRUE,NULL,0,TRUE, '2026-06-23 18:45:00','2026-06-23 18:45:00'),
('student048@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','황채은','010-2001-0048',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2009-10-04',TRUE,NULL,0,TRUE, '2026-06-23 19:30:00','2026-06-23 19:30:00'),
('student049@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','문시현','010-2001-0049',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2008-12-26',TRUE,NULL,0,FALSE,'2026-06-23 20:10:00','2026-06-23 20:10:00'),
('student050@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','배나연','010-2001-0050',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2007-03-19',TRUE,NULL,0,TRUE, '2026-06-23 20:50:00','2026-06-23 20:50:00'),
('student051@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','서준영','010-2001-0051',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2009-06-11',TRUE,NULL,0,FALSE,'2026-06-23 21:30:00','2026-06-23 21:30:00'),
('student052@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','고유진','010-2001-0052',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2008-09-03',TRUE,NULL,0,TRUE, '2026-06-23 22:05:00','2026-06-23 22:05:00'),
('student053@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','남도준','010-2001-0053',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2007-11-25',TRUE,NULL,0,TRUE, '2026-06-23 22:40:00','2026-06-23 22:40:00'),
('student054@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','구소연','010-2001-0054',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2009-02-17',TRUE,NULL,0,FALSE,'2026-06-23 23:15:00','2026-06-23 23:15:00'),
-- 6/24 (22명: student055~076)
('student055@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','노재민','010-2001-0055',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2008-05-09',TRUE,NULL,0,TRUE, '2026-06-24 08:20:00','2026-06-24 08:20:00'),
('student056@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','도하영','010-2001-0056',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2007-08-01',TRUE,NULL,0,FALSE,'2026-06-24 09:05:00','2026-06-24 09:05:00'),
('student057@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','마시윤','010-2001-0057',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2009-10-23',TRUE,NULL,0,TRUE, '2026-06-24 09:50:00','2026-06-24 09:50:00'),
('student058@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','바예린','010-2001-0058',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2008-01-15',TRUE,NULL,0,TRUE, '2026-06-24 10:35:00','2026-06-24 10:35:00'),
('student059@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','사건후','010-2001-0059',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2007-04-07',TRUE,NULL,0,FALSE,'2026-06-24 11:20:00','2026-06-24 11:20:00'),
('student060@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','아수민','010-2001-0060',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2009-06-29',TRUE,NULL,0,TRUE, '2026-06-24 12:05:00','2026-06-24 12:05:00'),
('student061@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','차민호','010-2001-0061',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2008-09-20',TRUE,NULL,0,FALSE,'2026-06-24 12:50:00','2026-06-24 12:50:00'),
('student062@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','타유빈','010-2001-0062',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2007-12-13',TRUE,NULL,0,TRUE, '2026-06-24 13:35:00','2026-06-24 13:35:00'),
('student063@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','파준성','010-2001-0063',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2009-03-05',TRUE,NULL,0,TRUE, '2026-06-24 14:20:00','2026-06-24 14:20:00'),
('student064@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','하민지','010-2001-0064',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2008-05-27',TRUE,NULL,0,FALSE,'2026-06-24 15:05:00','2026-06-24 15:05:00'),
('student065@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','김태민','010-2001-0065',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2007-08-18',TRUE,NULL,0,TRUE, '2026-06-24 15:50:00','2026-06-24 15:50:00'),
('student066@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','이소율','010-2001-0066',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2009-11-10',TRUE,NULL,0,FALSE,'2026-06-24 16:35:00','2026-06-24 16:35:00'),
('student067@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','박현우','010-2001-0067',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2008-02-02',TRUE,NULL,0,TRUE, '2026-06-24 17:20:00','2026-06-24 17:20:00'),
('student068@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','최다현','010-2001-0068',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2007-04-25',TRUE,NULL,0,TRUE, '2026-06-24 18:05:00','2026-06-24 18:05:00'),
('student069@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','정민규','010-2001-0069',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2009-07-17',TRUE,NULL,0,FALSE,'2026-06-24 18:50:00','2026-06-24 18:50:00'),
('student070@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','윤혜린','010-2001-0070',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2008-10-09',TRUE,NULL,0,TRUE, '2026-06-24 19:35:00','2026-06-24 19:35:00'),
('student071@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','임승준','010-2001-0071',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2007-01-01',TRUE,NULL,0,FALSE,'2026-06-24 20:20:00','2026-06-24 20:20:00'),
('student072@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','한보경','010-2001-0072',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2009-03-24',TRUE,NULL,0,TRUE, '2026-06-24 21:05:00','2026-06-24 21:05:00'),
('student073@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','오민찬','010-2001-0073',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2008-06-16',TRUE,NULL,0,TRUE, '2026-06-24 21:50:00','2026-06-24 21:50:00'),
('student074@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','신지원','010-2001-0074',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2007-09-08',TRUE,NULL,0,FALSE,'2026-06-24 22:35:00','2026-06-24 22:35:00'),
('student075@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','강주원','010-2001-0075',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2009-12-01',TRUE,NULL,0,TRUE, '2026-06-24 23:10:00','2026-06-24 23:10:00'),
('student076@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','조서아','010-2001-0076',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2008-02-22',TRUE,NULL,0,FALSE,'2026-06-24 23:45:00','2026-06-24 23:45:00'),
-- 6/25 (24명: student077~100)
('student077@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','장이준','010-2001-0077',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2007-05-14',TRUE,NULL,0,TRUE, '2026-06-25 08:10:00','2026-06-25 08:10:00'),
('student078@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','류민아','010-2001-0078',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2009-08-06',TRUE,NULL,0,TRUE, '2026-06-25 08:55:00','2026-06-25 08:55:00'),
('student079@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','권성준','010-2001-0079',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2008-10-28',TRUE,NULL,0,FALSE,'2026-06-25 09:40:00','2026-06-25 09:40:00'),
('student080@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','황다솔','010-2001-0080',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2007-01-20',TRUE,NULL,0,TRUE, '2026-06-25 10:25:00','2026-06-25 10:25:00'),
('student081@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','문재윤','010-2001-0081',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2009-04-12',TRUE,NULL,0,FALSE,'2026-06-25 11:10:00','2026-06-25 11:10:00'),
('student082@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','배수현','010-2001-0082',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2008-07-04',TRUE,NULL,0,TRUE, '2026-06-25 11:55:00','2026-06-25 11:55:00'),
('student083@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','서민준','010-2001-0083',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2007-09-26',TRUE,NULL,0,TRUE, '2026-06-25 12:40:00','2026-06-25 12:40:00'),
('student084@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','고채린','010-2001-0084',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2009-12-18',TRUE,NULL,0,FALSE,'2026-06-25 13:25:00','2026-06-25 13:25:00'),
('student085@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','남시준','010-2001-0085',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2008-03-10',TRUE,NULL,0,TRUE, '2026-06-25 14:10:00','2026-06-25 14:10:00'),
('student086@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','구하린','010-2001-0086',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2007-06-02',TRUE,NULL,0,FALSE,'2026-06-25 14:55:00','2026-06-25 14:55:00'),
('student087@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','노준혁','010-2001-0087',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2009-08-25',TRUE,NULL,0,TRUE, '2026-06-25 15:40:00','2026-06-25 15:40:00'),
('student088@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','도아린','010-2001-0088',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2008-11-17',TRUE,NULL,0,TRUE, '2026-06-25 16:25:00','2026-06-25 16:25:00'),
('student089@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','마재현','010-2001-0089',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2007-02-09',TRUE,NULL,0,FALSE,'2026-06-25 17:10:00','2026-06-25 17:10:00'),
('student090@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','바소율','010-2001-0090',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2009-05-01',TRUE,NULL,0,TRUE, '2026-06-25 17:55:00','2026-06-25 17:55:00'),
('student091@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','사윤호','010-2001-0091',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2008-07-23',TRUE,NULL,0,FALSE,'2026-06-25 18:40:00','2026-06-25 18:40:00'),
('student092@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','아채원','010-2001-0092',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2007-10-15',TRUE,NULL,0,TRUE, '2026-06-25 19:25:00','2026-06-25 19:25:00'),
('student093@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','차지훈','010-2001-0093',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2009-01-07',TRUE,NULL,0,TRUE, '2026-06-25 20:10:00','2026-06-25 20:10:00'),
('student094@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','타예은','010-2001-0094',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2008-03-30',TRUE,NULL,0,FALSE,'2026-06-25 20:55:00','2026-06-25 20:55:00'),
('student095@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','파도현','010-2001-0095',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2007-06-22',TRUE,NULL,0,TRUE, '2026-06-25 21:40:00','2026-06-25 21:40:00'),
('student096@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','하지수','010-2001-0096',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2009-09-14',TRUE,NULL,0,FALSE,'2026-06-25 22:25:00','2026-06-25 22:25:00'),
('student097@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','김준서','010-2001-0097',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2008-12-06',TRUE,NULL,0,TRUE, '2026-06-25 23:05:00','2026-06-25 23:05:00'),
('student098@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','이나현','010-2001-0098',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2007-02-28',TRUE,NULL,0,TRUE, '2026-06-25 23:30:00','2026-06-25 23:30:00'),
('student099@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','박승윤','010-2001-0099',NULL,'STUDENT','LOCAL',NULL,FALSE,'MALE',  '2009-05-21',TRUE,NULL,0,FALSE,'2026-06-25 23:45:00','2026-06-25 23:45:00'),
('student100@studyflow.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHhu','최서윤','010-2001-0100',NULL,'STUDENT','LOCAL',NULL,FALSE,'FEMALE','2008-08-13',TRUE,NULL,0,TRUE, '2026-06-25 23:58:00','2026-06-25 23:58:00');

-- ============================================================
-- StudentProfile (100명)
-- ============================================================
INSERT INTO student_profile (user_id, grade, interest_subjects, region, goal, created_at, updated_at)
SELECT u.id,
       s.grade, s.interest_subjects, s.region, s.goal,
       u.created_at, u.created_at
FROM users u
JOIN (
  SELECT 'student001@studyflow.com' AS email, '고1' AS grade, '수학,영어'       AS interest_subjects, '서울 강남구'  AS region, '수능 수학 1등급'       AS goal UNION ALL
  SELECT 'student002@studyflow.com', '고2', '영어,국어',       '서울 서초구',  '수능 영어 1등급'       UNION ALL
  SELECT 'student003@studyflow.com', '중3', '수학,과학',       '경기 성남시',  '특목고 진학'           UNION ALL
  SELECT 'student004@studyflow.com', '고1', '국어,사회',       '서울 송파구',  '내신 전 과목 1등급'    UNION ALL
  SELECT 'student005@studyflow.com', '고3', '수학,물리',       '인천 남동구',  'SKY 대학 진학'         UNION ALL
  SELECT 'student006@studyflow.com', '중2', '영어,음악',       '서울 마포구',  '영어 회화 마스터'      UNION ALL
  SELECT 'student007@studyflow.com', '고2', '화학,생물',       '경기 수원시',  '의대 진학'             UNION ALL
  SELECT 'student008@studyflow.com', '고1', '수학,영어',       '부산 해운대구','수능 최저 충족'        UNION ALL
  SELECT 'student009@studyflow.com', '중3', '국어,역사',       '대구 수성구',  '자사고 진학'           UNION ALL
  SELECT 'student010@studyflow.com', '고3', '영어,수학',       '서울 강서구',  '수시 논술 합격'        UNION ALL
  SELECT 'student011@studyflow.com', '고1', '물리,화학',       '경기 고양시',  '이공계 대학 진학'      UNION ALL
  SELECT 'student012@studyflow.com', '고2', '수학,영어',       '서울 노원구',  '수능 만점'             UNION ALL
  SELECT 'student013@studyflow.com', '중1', '수학,과학',       '경기 용인시',  '수학경시대회 입상'     UNION ALL
  SELECT 'student014@studyflow.com', '고3', '국어,사회',       '광주 서구',    '사범대 진학'           UNION ALL
  SELECT 'student015@studyflow.com', '고2', '영어,제2외국어',  '서울 동작구',  '외대 진학'             UNION ALL
  SELECT 'student016@studyflow.com', '중3', '수학,영어',       '경기 부천시',  '과학고 진학'           UNION ALL
  SELECT 'student017@studyflow.com', '고1', '생물,화학',       '대전 유성구',  '수의대 진학'           UNION ALL
  SELECT 'student018@studyflow.com', '고3', '수학,물리',       '울산 남구',    '공대 수석 입학'        UNION ALL
  SELECT 'student019@studyflow.com', '중2', '국어,영어',       '서울 은평구',  '독서량 늘리기'         UNION ALL
  SELECT 'student020@studyflow.com', '고2', '경제,사회',       '경기 안양시',  '경영대 진학'           UNION ALL
  SELECT 'student021@studyflow.com', '고1', '수학,정보',       '서울 구로구',  'SW 특기자 전형'        UNION ALL
  SELECT 'student022@studyflow.com', '중3', '영어,음악',       '경기 의정부시','예고 진학'             UNION ALL
  SELECT 'student023@studyflow.com', '고3', '수학,영어',       '인천 부평구',  '정시 합격'             UNION ALL
  SELECT 'student024@studyflow.com', '고2', '국어,한국사',     '서울 성북구',  '내신 관리'             UNION ALL
  SELECT 'student025@studyflow.com', '고1', '수학,과학',       '경기 파주시',  '영재원 입학'           UNION ALL
  SELECT 'student026@studyflow.com', '중1', '영어,미술',       '부산 동래구',  '영어 독서 습관 형성'   UNION ALL
  SELECT 'student027@studyflow.com', '고3', '물리,수학',       '대구 달서구',  '포스텍 진학'           UNION ALL
  SELECT 'student028@studyflow.com', '고2', '생물,화학',       '광주 북구',    '약대 진학'             UNION ALL
  SELECT 'student029@studyflow.com', '중3', '수학,영어',       '경기 평택시',  '특목고 진학'           UNION ALL
  SELECT 'student030@studyflow.com', '고1', '국어,영어',       '서울 양천구',  '언론고시 준비'         UNION ALL
  SELECT 'student031@studyflow.com', '고2', '수학,경제',       '경기 시흥시',  '상경계 진학'           UNION ALL
  SELECT 'student032@studyflow.com', '고3', '영어,수학',       '인천 연수구',  '수능 영어 만점'        UNION ALL
  SELECT 'student033@studyflow.com', '중2', '수학,과학',       '서울 관악구',  '수학 선행 완성'        UNION ALL
  SELECT 'student034@studyflow.com', '고1', '사회,국어',       '경기 하남시',  '내신 2등급 이내'       UNION ALL
  SELECT 'student035@studyflow.com', '고3', '수학,물리',       '대전 서구',    'KAIST 진학'            UNION ALL
  SELECT 'student036@studyflow.com', '고2', '영어,국어',       '서울 강북구',  '수시 최저 달성'        UNION ALL
  SELECT 'student037@studyflow.com', '중3', '수학,정보',       '경기 남양주시','정보올림피아드 입상'   UNION ALL
  SELECT 'student038@studyflow.com', '고1', '화학,생물',       '부산 사상구',  '이과 탐구 1등급'       UNION ALL
  SELECT 'student039@studyflow.com', '고3', '국어,역사',       '광주 광산구',  '인문계 대학 진학'      UNION ALL
  SELECT 'student040@studyflow.com', '고2', '수학,영어',       '서울 중랑구',  '수능 고득점'           UNION ALL
  SELECT 'student041@studyflow.com', '중1', '영어,수학',       '경기 김포시',  '기초 다지기'           UNION ALL
  SELECT 'student042@studyflow.com', '고1', '미술,국어',       '인천 서구',    '미대 진학 준비'        UNION ALL
  SELECT 'student043@studyflow.com', '고3', '수학,화학',       '대구 북구',    '의치한 진학'           UNION ALL
  SELECT 'student044@studyflow.com', '고2', '영어,사회',       '서울 동대문구','사회교육과 진학'       UNION ALL
  SELECT 'student045@studyflow.com', '중3', '수학,영어',       '경기 광주시',  '외고 진학'             UNION ALL
  SELECT 'student046@studyflow.com', '고1', '생물,화학',       '부산 북구',    '간호학과 진학'         UNION ALL
  SELECT 'student047@studyflow.com', '고3', '물리,수학',       '울산 중구',    '공과대학 수석'         UNION ALL
  SELECT 'student048@studyflow.com', '고2', '국어,한국사',     '서울 강동구',  '수능 국어 1등급'       UNION ALL
  SELECT 'student049@studyflow.com', '중2', '수학,과학',       '경기 구리시',  '중등 수학 완성'        UNION ALL
  SELECT 'student050@studyflow.com', '고1', '영어,음악',       '인천 남동구',  '실용음악과 진학'       UNION ALL
  SELECT 'student051@studyflow.com', '고3', '수학,영어',       '서울 강남구',  '수능 최고점 달성'      UNION ALL
  SELECT 'student052@studyflow.com', '고2', '사회,경제',       '경기 성남시',  '경제학과 진학'         UNION ALL
  SELECT 'student053@studyflow.com', '중3', '수학,물리',       '대전 동구',    '과학고 수시'           UNION ALL
  SELECT 'student054@studyflow.com', '고1', '국어,영어',       '부산 연제구',  '문과 내신 1등급'       UNION ALL
  SELECT 'student055@studyflow.com', '고3', '화학,생물',       '광주 남구',    '약학대학 진학'         UNION ALL
  SELECT 'student056@studyflow.com', '고2', '수학,정보',       '서울 서대문구','IT 특기자 전형'        UNION ALL
  SELECT 'student057@studyflow.com', '중1', '영어,미술',       '경기 안산시',  '기초 영어 완성'        UNION ALL
  SELECT 'student058@studyflow.com', '고1', '수학,과학',       '인천 계양구',  '수학 선행 3년'         UNION ALL
  SELECT 'student059@studyflow.com', '고3', '국어,사회',       '대구 중구',    '사회과학 계열 진학'    UNION ALL
  SELECT 'student060@studyflow.com', '고2', '영어,수학',       '서울 용산구',  '수능 영어 수학 1등급'  UNION ALL
  SELECT 'student061@studyflow.com', '중3', '수학,영어',       '경기 오산시',  '일반고 최상위권'       UNION ALL
  SELECT 'student062@studyflow.com', '고1', '화학,물리',       '부산 강서구',  '이과 탐구 만점'        UNION ALL
  SELECT 'student063@studyflow.com', '고3', '수학,영어',       '울산 북구',    '지방 거점 국립대 수석' UNION ALL
  SELECT 'student064@studyflow.com', '고2', '국어,역사',       '서울 성동구',  '역사교육과 진학'       UNION ALL
  SELECT 'student065@studyflow.com', '중2', '수학,과학',       '경기 화성시',  '수학경시 입상'         UNION ALL
  SELECT 'student066@studyflow.com', '고1', '영어,음악',       '인천 미추홀구','음대 입시 준비'        UNION ALL
  SELECT 'student067@studyflow.com', '고3', '수학,물리',       '대전 대덕구',  'KAIST·포스텍 중 선택'  UNION ALL
  SELECT 'student068@studyflow.com', '고2', '국어,영어',       '광주 동구',    '수능 언어영역 1등급'   UNION ALL
  SELECT 'student069@studyflow.com', '중3', '수학,정보',       '서울 종로구',  'SW마이스터고 진학'     UNION ALL
  SELECT 'student070@studyflow.com', '고1', '생물,화학',       '경기 이천시',  '보건계열 진학 준비'    UNION ALL
  SELECT 'student071@studyflow.com', '고3', '수학,영어',       '부산 금정구',  '부산대 의대 진학'      UNION ALL
  SELECT 'student072@studyflow.com', '고2', '사회,국어',       '서울 광진구',  '사범대 진학'           UNION ALL
  SELECT 'student073@studyflow.com', '중1', '영어,수학',       '경기 포천시',  '선행학습 시작'         UNION ALL
  SELECT 'student074@studyflow.com', '고1', '화학,수학',       '인천 강화군',  '화학올림피아드 준비'   UNION ALL
  SELECT 'student075@studyflow.com', '고3', '국어,사회',       '대구 동구',    '행정고시 준비'         UNION ALL
  SELECT 'student076@studyflow.com', '고2', '영어,수학',       '광주 서구',    '수시 1차 합격'         UNION ALL
  SELECT 'student077@studyflow.com', '중3', '수학,과학',       '서울 도봉구',  '과학고 최종 합격'      UNION ALL
  SELECT 'student078@studyflow.com', '고1', '국어,영어',       '경기 양주시',  '독서 습관 형성'        UNION ALL
  SELECT 'student079@studyflow.com', '고3', '물리,수학',       '부산 동구',    '반도체공학과 진학'     UNION ALL
  SELECT 'student080@studyflow.com', '고2', '생물,화학',       '울산 동구',    '치대 진학'             UNION ALL
  SELECT 'student081@studyflow.com', '중2', '영어,사회',       '서울 금천구',  '영어 토론 대회 준비'   UNION ALL
  SELECT 'student082@studyflow.com', '고1', '수학,정보',       '경기 과천시',  '알고리즘 기초 완성'    UNION ALL
  SELECT 'student083@studyflow.com', '고3', '국어,영어',       '인천 동구',    '논술 특기자 전형'      UNION ALL
  SELECT 'student084@studyflow.com', '고2', '화학,생물',       '대전 중구',    '수능 과탐 1등급'       UNION ALL
  SELECT 'student085@studyflow.com', '중3', '수학,영어',       '광주 북구',    '특목고 최종 합격'      UNION ALL
  SELECT 'student086@studyflow.com', '고1', '역사,사회',       '서울 중구',    '역사 심화 학습'        UNION ALL
  SELECT 'student087@studyflow.com', '고3', '수학,물리',       '경기 군포시',  '전기공학과 진학'       UNION ALL
  SELECT 'student088@studyflow.com', '고2', '영어,국어',       '부산 서구',    '수능 영어 만점'        UNION ALL
  SELECT 'student089@studyflow.com', '중1', '수학,과학',       '울산 울주군',  '기초 과학 탐구 역량'   UNION ALL
  SELECT 'student090@studyflow.com', '고1', '수학,화학',       '인천 옹진군',  '이과 선행 완성'        UNION ALL
  SELECT 'student091@studyflow.com', '고3', '국어,사회',       '서울 강남구',  '법학대 진학'           UNION ALL
  SELECT 'student092@studyflow.com', '고2', '영어,수학',       '경기 연천군',  '수능 고득점 달성'      UNION ALL
  SELECT 'student093@studyflow.com', '중3', '수학,정보',       '대구 달성군',  '소프트웨어고 진학'     UNION ALL
  SELECT 'student094@studyflow.com', '고1', '생물,화학',       '광주 남구',    '생명공학과 진학 준비'  UNION ALL
  SELECT 'student095@studyflow.com', '고3', '수학,영어',       '서울 강동구',  '수능 수학 만점'        UNION ALL
  SELECT 'student096@studyflow.com', '고2', '사회,역사',       '경기 양평군',  '역사교육과 진학'       UNION ALL
  SELECT 'student097@studyflow.com', '중2', '영어,미술',       '부산 중구',    '예술 중학교 준비'      UNION ALL
  SELECT 'student098@studyflow.com', '고1', '수학,물리',       '울산 남구',    '물리올림피아드 입상'   UNION ALL
  SELECT 'student099@studyflow.com', '고3', '화학,생물',       '인천 부평구',  '의예과 최종 합격'      UNION ALL
  SELECT 'student100@studyflow.com', '고2', '국어,영어',       '서울 서초구',  '수능 국어 1등급'
) s ON u.email = s.email
WHERE u.role = 'STUDENT';

SET FOREIGN_KEY_CHECKS = 1;

-- voice_call_enabled 컬럼은 User 엔티티에 추가되어 ddl-auto 로 자동 생성됨.
-- DB 레벨 DEFAULT 설정은 DummyDataInitializer.setVoiceCallEnabledDefault() 에서 처리.
