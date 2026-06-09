package com.studyflow.domain.admin.dto;

/**
 * 관리자 회원 상세 조회 응답의 공통 타입.
 * role에 따라 AdminStudentDetailResponse / AdminTeacherDetailResponse / AdminUserDetailResponse 중 하나가 반환됩니다.
 */
public sealed interface AdminUserDetailInterface
        permits AdminStudentDetailResponse, AdminTeacherDetailResponse, AdminUserDetailResponse {
}
