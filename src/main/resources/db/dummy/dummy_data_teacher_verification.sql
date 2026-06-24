-- ============================================================
-- TeacherVerification 더미 데이터: 활성 선생님 10명
-- ============================================================
--
-- [이미지 등록 프로세스 참고]
-- 정상 플로우: MultipartFile → FileService.uploadVerificationDocument()
--              → file_asset 테이블 INSERT → fileAsset.fileUrl 반환
--              → TeacherService.requestVerification(fileAssetId) 호출
--              → teacher_verification.document_url 에 fileAsset.fileUrl 저장
--
-- [직접 INSERT 시]
-- document_url 은 FK 가 아닌 순수 VARCHAR(500) 이므로
-- file_asset 레코드 없이 URL 문자열만 넣어도 됨.
-- (서비스 레이어의 fileAssetId 검증을 우회하는 것이므로 더미 전용)
--
-- [이미지 URL]
-- 제공된 GitHub blob URL → raw content URL 로 변환하여 사용
-- blob: https://github.com/.../blob/img/%23214-teacher-verification-img/teacher_verification_file.png
-- raw:  https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png
--
-- [is_processed 전략]
-- PENDING  → is_processed = 0  (unique constraint 으로 중복 제출 방지)
-- APPROVED → is_processed = id (INSERT 후 UPDATE 로 처리, 아래 STEP 2 참고)
-- REJECTED → is_processed = id (동일)
--
-- [상태 분포]
-- teacher1~7 : APPROVED (is_listed=TRUE 와 일치)
-- teacher8   : REJECTED (rejected_reason 포함, is_listed=FALSE 로 업데이트 필요)
-- teacher9~10: PENDING  (is_processed=0, is_listed=FALSE 로 업데이트 필요)
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- STEP 1: teacher_verification INSERT
-- ============================================================
INSERT INTO teacher_verification (user_id, document_type, document_url, status, is_processed, description, awards, career, major, admission_year, rejected_reason, reviewed_at, created_at, updated_at)
SELECT u.id,
       v.document_type, v.document_url, v.status, v.is_processed,
       v.description, v.awards, v.career, v.major, v.admission_year,
       v.rejected_reason, v.reviewed_at,
       v.created_at, v.created_at
FROM users u
JOIN (
  SELECT
    'teacher1@studyflow.com'  AS email,
    'DIPLOMA'                 AS document_type,
    'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png' AS document_url,
    'APPROVED'                AS status,
    0                         AS is_processed,
    '서울대학교 수학과 졸업 후 10년간 수학 강의 경력을 보유하고 있습니다.' AS description,
    '전국수학올림피아드 금상, 대학 성적 우수 장학생' AS awards,
    '서울대학교'              AS career,
    '수학과'                  AS major,
    '12학번'                  AS admission_year,
    NULL                      AS rejected_reason,
    '2026-06-19 23:10:00'    AS reviewed_at,
    '2026-06-19 09:15:00'    AS created_at
  UNION ALL SELECT
    'teacher2@studyflow.com', 'ENROLLMENT_CERT',
    'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png',
    'APPROVED', 0,
    '연세대 영어영문학과 재학 중이며 TOEIC 990점 보유, 영어 회화 강의 3년 경력입니다.',
    'TOEIC 990, 교내 영어토론대회 1위',
    '연세대학교', '영어영문학과', '14학번',
    NULL, '2026-06-20 09:00:00', '2026-06-19 11:32:00'
  UNION ALL SELECT
    'teacher3@studyflow.com', 'DIPLOMA',
    'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png',
    'APPROVED', 0,
    '고려대 물리학과 졸업, 물리올림피아드 수상 경력 및 강의 경력 5년 보유.',
    '한국물리올림피아드 은상',
    '고려대학교', '물리학과', '16학번',
    NULL, '2026-06-20 15:30:00', '2026-06-20 08:30:00'
  UNION ALL SELECT
    'teacher4@studyflow.com', 'TEACHER_CERTIFICATE',
    'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png',
    'APPROVED', 0,
    '화학 2급 정교사 자격증 보유, 성균관대 화학과 졸업 후 강의 활동 중입니다.',
    '대한화학회 우수논문상',
    '성균관대학교', '화학과', '15학번',
    NULL, '2026-06-20 22:00:00', '2026-06-20 10:15:00'
  UNION ALL SELECT
    'teacher5@studyflow.com', 'DIPLOMA',
    'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png',
    'APPROVED', 0,
    '한양대 국어국문학과 졸업, 논술 전문 강사로 17년 활동 중입니다.',
    '전국 논술 지도 우수교사상',
    '한양대학교', '국어국문학과', '11학번',
    NULL, '2026-06-21 14:00:00', '2026-06-21 09:00:00'
  UNION ALL SELECT
    'teacher6@studyflow.com', 'ENROLLMENT_CERT',
    'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png',
    'APPROVED', 0,
    '이화여대 사회학과 재학 중이며 사회탐구 전 과목 지도 경력 보유입니다.',
    '사회탐구 1등급 배출 다수',
    '이화여자대학교', '사회학과', '17학번',
    NULL, '2026-06-21 20:00:00', '2026-06-21 11:45:00'
  UNION ALL SELECT
    'teacher7@studyflow.com', 'STUDENT_ID',
    'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png',
    'APPROVED', 0,
    'KAIST 전산학부 재학 중이며 정보올림피아드 입상 경력, SW 강의 경험 보유.',
    '정보올림피아드 입상',
    'KAIST', '전산학부', '13학번',
    NULL, '2026-06-22 18:30:00', '2026-06-22 08:45:00'
  UNION ALL SELECT
    'teacher8@studyflow.com', 'DIPLOMA',
    'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png',
    'REJECTED', 0,
    '부산대 생물학과 졸업, 생명과학 실험 연계 수업 진행 중입니다.',
    '전국 생물탐구대회 대상',
    '부산대학교', '생물학과', '18학번',
    '제출하신 졸업증명서의 발급일이 3개월을 초과하였습니다. 최근 발급본으로 재제출 부탁드립니다.',
    '2026-06-23 11:00:00', '2026-06-22 10:30:00'
  UNION ALL SELECT
    'teacher9@studyflow.com', 'TEACHER_CERTIFICATE',
    'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png',
    'PENDING', 0,
    '서강대 경제학과 졸업, 경제·사회 16년 강사 경력 보유. 교원자격증 첨부합니다.',
    '경제 논술 지도 수상',
    '서강대학교', '경제학과', '10학번',
    NULL, NULL, '2026-06-23 09:20:00'
  UNION ALL SELECT
    'teacher10@studyflow.com', 'ENROLLMENT_CERT',
    'https://raw.githubusercontent.com/prgrms-aibe-devcourse/AIBE5_FinalProject_1010_BE/img/%23214-teacher-verification-img/teacher_verification_file.png',
    'PENDING', 0,
    '중앙대 음악교육학과 재학 중이며 피아노·음악이론 강의 중입니다.',
    '전국 음악 콩쿠르 입상',
    '중앙대학교', '음악교육학과', '19학번',
    NULL, NULL, '2026-06-23 10:40:00'
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
    'teacher1@studyflow.com', 'teacher2@studyflow.com',
    'teacher3@studyflow.com', 'teacher4@studyflow.com',
    'teacher5@studyflow.com', 'teacher6@studyflow.com',
    'teacher7@studyflow.com', 'teacher8@studyflow.com'
  );

-- ============================================================
-- STEP 3: APPROVED 선생님은 users.is_verified = TRUE 로 업데이트
-- ============================================================
UPDATE users
SET is_verified = TRUE
WHERE email IN (
  'teacher1@studyflow.com', 'teacher2@studyflow.com',
  'teacher3@studyflow.com', 'teacher4@studyflow.com',
  'teacher5@studyflow.com', 'teacher6@studyflow.com',
  'teacher7@studyflow.com'
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
  'teacher10@studyflow.com'   -- PENDING
);

SET FOREIGN_KEY_CHECKS = 1;
