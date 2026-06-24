-- ============================================================
-- TeacherVerification 더미 데이터: 활성 선생님 30명
-- ============================================================
--
-- [is_processed 전략]
-- PENDING  → is_processed = 0  (unique constraint 으로 중복 제출 방지)
-- APPROVED → is_processed = id (INSERT 후 UPDATE 로 처리)
-- REJECTED → is_processed = id (동일)
--
-- [상태 분포]
-- teacher1~7   : APPROVED (7명)
-- teacher8     : REJECTED
-- teacher9~10  : PENDING  (2명)
-- teacher11~23 : APPROVED (13명)
-- teacher24~25 : REJECTED (2명)  → 총 REJECTED  3명
-- teacher26~30 : APPROVED (5명)  → 총 APPROVED 25명
--                                   총 PENDING   2명
--
-- [이미지 URL]
-- raw: https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- STEP 1: teacher_verification INSERT (전원 is_processed=0)
-- ============================================================
INSERT INTO teacher_verification (user_id, document_type, document_url, status, is_processed, description, awards, career, major, admission_year, rejected_reason, reviewed_at, created_at, updated_at)
SELECT u.id,
       v.document_type, v.document_url, v.status, v.is_processed,
       v.description, v.awards, v.career, v.major, v.admission_year,
       v.rejected_reason, v.reviewed_at,
       v.created_at, v.created_at
FROM users u
JOIN (
  SELECT 'teacher1@studyflow.com'  AS email, 'DIPLOMA'           AS document_type, 'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png' AS document_url, 'APPROVED' AS status, 0 AS is_processed, '서울대학교 수학과 졸업 후 10년간 수학 강의 경력을 보유하고 있습니다.' AS description, '전국수학올림피아드 금상, 대학 성적 우수 장학생' AS awards, '서울대학교' AS career, '수학과' AS major, '12학번' AS admission_year, NULL AS rejected_reason, '2026-06-19 23:10:00' AS reviewed_at, '2026-06-19 09:15:00' AS created_at UNION ALL
  SELECT 'teacher2@studyflow.com',  'ENROLLMENT_CERT', 'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png', 'APPROVED', 0, '연세대 영어영문학과 재학 중이며 TOEIC 990점 보유, 영어 회화 강의 3년 경력입니다.', 'TOEIC 990, 교내 영어토론대회 1위', '연세대학교', '영어영문학과', '14학번', NULL, '2026-06-20 09:00:00', '2026-06-19 11:32:00' UNION ALL
  SELECT 'teacher3@studyflow.com',  'DIPLOMA',          'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png', 'APPROVED', 0, '고려대 물리학과 졸업, 물리올림피아드 수상 경력 및 강의 경력 5년 보유.', '한국물리올림피아드 은상', '고려대학교', '물리학과', '16학번', NULL, '2026-06-20 15:30:00', '2026-06-20 08:30:00' UNION ALL
  SELECT 'teacher4@studyflow.com',  'TEACHER_CERTIFICATE', 'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png', 'APPROVED', 0, '화학 2급 정교사 자격증 보유, 성균관대 화학과 졸업 후 강의 활동 중입니다.', '대한화학회 우수논문상', '성균관대학교', '화학과', '15학번', NULL, '2026-06-20 22:00:00', '2026-06-20 10:15:00' UNION ALL
  SELECT 'teacher5@studyflow.com',  'DIPLOMA',          'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png', 'APPROVED', 0, '한양대 국어국문학과 졸업, 논술 전문 강사로 17년 활동 중입니다.', '전국 논술 지도 우수교사상', '한양대학교', '국어국문학과', '11학번', NULL, '2026-06-21 14:00:00', '2026-06-21 09:00:00' UNION ALL
  SELECT 'teacher6@studyflow.com',  'ENROLLMENT_CERT', 'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png', 'APPROVED', 0, '이화여대 사회학과 재학 중이며 사회탐구 전 과목 지도 경력 보유입니다.', '사회탐구 1등급 배출 다수', '이화여자대학교', '사회학과', '17학번', NULL, '2026-06-21 20:00:00', '2026-06-21 11:45:00' UNION ALL
  SELECT 'teacher7@studyflow.com',  'STUDENT_ID',       'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png', 'APPROVED', 0, 'KAIST 전산학부 재학 중이며 정보올림피아드 입상 경력, SW 강의 경험 보유.', '정보올림피아드 입상', 'KAIST', '전산학부', '13학번', NULL, '2026-06-22 18:30:00', '2026-06-22 08:45:00' UNION ALL
  SELECT 'teacher8@studyflow.com',  'DIPLOMA',          'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png', 'REJECTED', 0, '부산대 생물학과 졸업, 생명과학 실험 연계 수업 진행 중입니다.', '전국 생물탐구대회 대상', '부산대학교', '생물학과', '18학번', '제출하신 졸업증명서의 발급일이 3개월을 초과하였습니다. 최근 발급본으로 재제출 부탁드립니다.', '2026-06-23 11:00:00', '2026-06-22 10:30:00' UNION ALL
  SELECT 'teacher9@studyflow.com',  'TEACHER_CERTIFICATE', 'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png', 'PENDING', 0, '서강대 경제학과 졸업, 경제·사회 16년 강사 경력 보유. 교원자격증 첨부합니다.', '경제 논술 지도 수상', '서강대학교', '경제학과', '10학번', NULL, NULL, '2026-06-23 09:20:00' UNION ALL
  SELECT 'teacher10@studyflow.com', 'ENROLLMENT_CERT', 'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png', 'PENDING', 0, '중앙대 음악교육학과 재학 중이며 피아노·음악이론 강의 중입니다.', '전국 음악 콩쿠르 입상', '중앙대학교', '음악교육학과', '19학번', NULL, NULL, '2026-06-23 10:40:00' UNION ALL
  -- teacher11~23: APPROVED
  SELECT 'teacher11@studyflow.com', 'DIPLOMA',          'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png', 'APPROVED', 0, '연세대 수학교육학과 졸업, 수학올림피아드 수상 및 강의 경력 보유.', '전국 수학올림피아드 동상', '연세대학교', '수학교육학과', '15학번', NULL, '2026-06-20 10:00:00', '2026-06-19 13:05:00' UNION ALL
  SELECT 'teacher12@studyflow.com', 'ENROLLMENT_CERT', 'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png', 'APPROVED', 0, '고려대 영어영문학과 재학 중, Cambridge 영어강사 자격 보유 및 강의 경험 3년.', 'Cambridge 영어강사 자격증', '고려대학교', '영어영문학과', '13학번', NULL, '2026-06-21 09:00:00', '2026-06-20 09:10:00' UNION ALL
  SELECT 'teacher13@studyflow.com', 'DIPLOMA',          'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png', 'APPROVED', 0, '서울시립대 국어국문학과 졸업, 내신·수능 국어 지도 경력 보유.', NULL, '서울시립대학교', '국어국문학과', '16학번', NULL, '2026-06-21 15:00:00', '2026-06-20 14:50:00' UNION ALL
  SELECT 'teacher14@studyflow.com', 'DIPLOMA',          'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png', 'APPROVED', 0, '한양대 생명과학과 졸업, 생명과학 실험 연계 탐구 수업 전문.', NULL, '한양대학교', '생명과학과', '12학번', NULL, '2026-06-22 08:00:00', '2026-06-21 08:20:00' UNION ALL
  SELECT 'teacher15@studyflow.com', 'ENROLLMENT_CERT', 'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png', 'APPROVED', 0, '성균관대 사회학과 재학 중, 사탐 전 영역 강의 경력 보유.', NULL, '성균관대학교', '사회학과', '17학번', NULL, '2026-06-22 13:00:00', '2026-06-21 13:30:00' UNION ALL
  SELECT 'teacher16@studyflow.com', 'DIPLOMA',          'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png', 'APPROVED', 0, '이화여대 수학과 우등 졸업, 수학 심화 및 올림피아드 전문 강사입니다.', '이화여대 수학과 우등 졸업', '이화여자대학교', '수학과', '14학번', NULL, '2026-06-22 21:00:00', '2026-06-21 20:15:00' UNION ALL
  SELECT 'teacher17@studyflow.com', 'DIPLOMA',          'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png', 'APPROVED', 0, '한국외대 영어학과 졸업, 원어민 수준 영어 발음 및 회화 강의 10년 경력.', NULL, '한국외국어대학교', '영어학과', '11학번', NULL, '2026-06-23 09:00:00', '2026-06-22 09:40:00' UNION ALL
  SELECT 'teacher18@studyflow.com', 'STUDENT_ID',       'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png', 'APPROVED', 0, 'KAIST 전산학부 재학 중, 정보올림피아드 입상 및 AI·SW 강의 경험 보유.', '정보올림피아드 입상', 'KAIST', '전산학부', '18학번', NULL, '2026-06-23 14:00:00', '2026-06-22 13:25:00' UNION ALL
  SELECT 'teacher19@studyflow.com', 'DIPLOMA',          'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png', 'APPROVED', 0, '중앙대 국어국문학과 졸업, 국어 내신 전문 강사로 활동 중입니다.', NULL, '중앙대학교', '국어국문학과', '15학번', NULL, '2026-06-23 18:00:00', '2026-06-22 17:00:00' UNION ALL
  SELECT 'teacher20@studyflow.com', 'DIPLOMA',          'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png', 'APPROVED', 0, '부산대 화학과 졸업, 화학올림피아드 입상 및 화학 강의 경력 보유.', '한국 화학올림피아드 은상', '부산대학교', '화학과', '13학번', NULL, '2026-06-23 22:00:00', '2026-06-22 20:30:00' UNION ALL
  SELECT 'teacher21@studyflow.com', 'ENROLLMENT_CERT', 'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png', 'APPROVED', 0, '경희대 사회학과 재학 중, 수능 사탐 전 영역 강의 경력 보유.', NULL, '경희대학교', '사회학과', '17학번', NULL, '2026-06-24 09:00:00', '2026-06-23 08:15:00' UNION ALL
  SELECT 'teacher22@studyflow.com', 'DIPLOMA',          'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png', 'APPROVED', 0, '서강대 수학과 졸업, 수학 기초 완성 전문 강사로 활동 중입니다.', NULL, '서강대학교', '수학과', '16학번', NULL, '2026-06-24 12:30:00', '2026-06-23 11:30:00' UNION ALL
  SELECT 'teacher23@studyflow.com', 'DIPLOMA',          'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png', 'APPROVED', 0, '인하대 영어영문학과 졸업, TOEIC 970 보유 및 수능·내신 영어 전문 강사.', 'TOEIC 970', '인하대학교', '영어영문학과', '14학번', NULL, '2026-06-24 16:00:00', '2026-06-23 15:45:00' UNION ALL
  -- teacher24~26: REJECTED
  SELECT 'teacher24@studyflow.com', 'DIPLOMA',          'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png', 'REJECTED', 0, '세종대 생물학과 재학 중, 생명과학 기초 강의 경험 보유.', NULL, '세종대학교', '생물학과', '19학번', '재학증명서 발급일이 오래되었습니다. 최근 발급본(3개월 이내)으로 재제출 부탁드립니다.', '2026-06-24 10:00:00', '2026-06-23 19:20:00' UNION ALL
  SELECT 'teacher25@studyflow.com', 'ENROLLMENT_CERT', 'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png', 'REJECTED', 0, '건국대 지리교육학과 재학 중, 사회·지리 과목 강의 준비 중입니다.', NULL, '건국대학교', '지리교육학과', '16학번', '제출 서류가 재학증명서 원본이 아닌 사본으로 확인되었습니다. 원본으로 재제출 부탁드립니다.', '2026-06-24 15:00:00', '2026-06-24 08:55:00' UNION ALL
  -- teacher26~30: APPROVED
  SELECT 'teacher26@studyflow.com', 'DIPLOMA',          'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png', 'APPROVED', 0, '동국대 수학과 재학 중, 수학 문제풀이 전문으로 강의 중입니다.', NULL, '동국대학교', '수학과', '18학번', NULL, '2026-06-25 10:00:00', '2026-06-24 12:40:00' UNION ALL
  SELECT 'teacher27@studyflow.com', 'ENROLLMENT_CERT', 'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png', 'APPROVED', 0, '아주대 영어영문학과 재학 중, 영어 문법 기초 강의를 시작하고자 합니다.', NULL, '아주대학교', '영어영문학과', '20학번', NULL, '2026-06-25 14:00:00', '2026-06-24 16:10:00' UNION ALL
  SELECT 'teacher28@studyflow.com', 'DIPLOMA',          'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png', 'APPROVED', 0, '숙명여대 국어교육학과 재학 중, 국어 기초 문법 지도를 희망합니다.', NULL, '숙명여자대학교', '국어교육학과', '17학번', NULL, '2026-06-25 18:00:00', '2026-06-24 20:05:00' UNION ALL
  SELECT 'teacher29@studyflow.com', 'DIPLOMA',          'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png', 'APPROVED', 0, '경북대 화학과 졸업, 화학 기초 개념 강의를 준비 중입니다.', NULL, '경북대학교', '화학과', '15학번', NULL, '2026-06-25 23:00:00', '2026-06-25 10:15:00' UNION ALL
  SELECT 'teacher30@studyflow.com', 'STUDENT_ID',       'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png', 'APPROVED', 0, '홍익대 컴퓨터공학과 재학 중, 정보 과목 및 코딩 입문 강의를 희망합니다.', NULL, '홍익대학교', '컴퓨터공학과', '20학번', NULL, '2026-06-26 09:00:00', '2026-06-25 15:40:00'
) v ON u.email = v.email
WHERE u.role = 'TEACHER' AND u.is_deleted = 0;

-- ============================================================
-- STEP 2: APPROVED / REJECTED 는 is_processed = id 로 업데이트
-- ============================================================
UPDATE teacher_verification tv
JOIN users u ON tv.user_id = u.id
SET tv.is_processed = tv.id
WHERE tv.status IN ('APPROVED', 'REJECTED')
  AND u.email IN (
    'teacher1@studyflow.com',  'teacher2@studyflow.com',
    'teacher3@studyflow.com',  'teacher4@studyflow.com',
    'teacher5@studyflow.com',  'teacher6@studyflow.com',
    'teacher7@studyflow.com',  'teacher8@studyflow.com',
    'teacher11@studyflow.com', 'teacher12@studyflow.com',
    'teacher13@studyflow.com', 'teacher14@studyflow.com',
    'teacher15@studyflow.com', 'teacher16@studyflow.com',
    'teacher17@studyflow.com', 'teacher18@studyflow.com',
    'teacher19@studyflow.com', 'teacher20@studyflow.com',
    'teacher21@studyflow.com', 'teacher22@studyflow.com',
    'teacher23@studyflow.com', 'teacher24@studyflow.com',
    'teacher25@studyflow.com', 'teacher26@studyflow.com',
    'teacher27@studyflow.com', 'teacher28@studyflow.com',
    'teacher29@studyflow.com', 'teacher30@studyflow.com'
  );

-- ============================================================
-- STEP 3: APPROVED 선생님은 users.is_verified = TRUE 로 업데이트
-- ============================================================
UPDATE users
SET is_verified = TRUE
WHERE email IN (
  'teacher1@studyflow.com',  'teacher2@studyflow.com',
  'teacher3@studyflow.com',  'teacher4@studyflow.com',
  'teacher5@studyflow.com',  'teacher6@studyflow.com',
  'teacher7@studyflow.com',
  'teacher11@studyflow.com', 'teacher12@studyflow.com',
  'teacher13@studyflow.com', 'teacher14@studyflow.com',
  'teacher15@studyflow.com', 'teacher16@studyflow.com',
  'teacher17@studyflow.com', 'teacher18@studyflow.com',
  'teacher19@studyflow.com', 'teacher20@studyflow.com',
  'teacher21@studyflow.com', 'teacher22@studyflow.com',
  'teacher23@studyflow.com', 'teacher26@studyflow.com',
  'teacher27@studyflow.com', 'teacher28@studyflow.com',
  'teacher29@studyflow.com', 'teacher30@studyflow.com'
);

-- ============================================================
-- STEP 4: REJECTED / PENDING 선생님은 is_listed = FALSE 로 보정
-- ============================================================
UPDATE teacher_profile tp
JOIN users u ON tp.user_id = u.id
SET tp.is_listed = FALSE
WHERE u.email IN (
  'teacher8@studyflow.com',   -- REJECTED
  'teacher9@studyflow.com',   -- PENDING
  'teacher10@studyflow.com',  -- PENDING
  'teacher24@studyflow.com',  -- REJECTED
  'teacher25@studyflow.com'   -- REJECTED
);

SET FOREIGN_KEY_CHECKS = 1;
