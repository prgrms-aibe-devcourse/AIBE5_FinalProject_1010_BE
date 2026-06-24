package com.studyflow.domain.student.service;

import com.studyflow.domain.enrollment.enums.EnrollmentRequestStatus;
import com.studyflow.domain.enrollment.enums.EnrollmentStatus;
import com.studyflow.domain.enrollment.repository.EnrollmentRepository;
import com.studyflow.domain.enrollment.repository.EnrollmentRequestRepository;
import com.studyflow.domain.student.dto.StudentEnrollmentRequestResponse;
import com.studyflow.domain.student.dto.StudentEnrolledCourseResponse;
import com.studyflow.domain.student.dto.StudentProfileResponse;
import com.studyflow.domain.student.dto.StudentProfileUpdateRequest;
import com.studyflow.domain.student.entity.StudentProfile;
import com.studyflow.domain.student.exception.StudentProfileNotFoundException;
import com.studyflow.domain.student.repository.StudentProfileRepository;
import com.studyflow.domain.user.exception.UserNotFoundException;
import com.studyflow.domain.user.repository.UserRepository;
import com.studyflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class StudentService {

    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentRequestRepository enrollmentRequestRepository;

    @Transactional(readOnly = true)
    public StudentProfileResponse getMyProfile(Long userId) {
        userRepository.findActiveById(userId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        StudentProfile profile = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> StudentProfileNotFoundException.ofUserId(userId));

        return new StudentProfileResponse(profile);
    }

    public StudentProfileResponse updateMyProfile(Long userId, StudentProfileUpdateRequest request) {
        userRepository.findActiveById(userId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        StudentProfile profile = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> StudentProfileNotFoundException.ofUserId(userId));

        profile.update(request.getGoal(), request.getGrade(), request.getInterestSubjects(), request.getRegion());

        return new StudentProfileResponse(profile);
    }

    @Transactional(readOnly = true)
    public Page<StudentEnrollmentRequestResponse> getMyEnrollmentRequests(
            Long userId, EnrollmentRequestStatus status, Pageable pageable) {
        userRepository.findActiveById(userId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        return enrollmentRequestRepository
                .findByUserId(userId, status, pageable)
                .map(StudentEnrollmentRequestResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<StudentEnrolledCourseResponse> getMyCourses(Long userId, EnrollmentStatus status, Pageable pageable) {
        userRepository.findActiveById(userId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        return enrollmentRepository
                .findWithCourseAndSubjectByUserId(userId, status, pageable)
                .map(StudentEnrolledCourseResponse::from);
    }
}
