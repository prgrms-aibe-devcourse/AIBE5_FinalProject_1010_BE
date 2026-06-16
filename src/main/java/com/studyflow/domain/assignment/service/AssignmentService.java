package com.studyflow.domain.assignment.service;

import com.studyflow.domain.assignment.dto.AssignmentRequest;
import com.studyflow.domain.assignment.dto.AssignmentResponse;
import com.studyflow.domain.assignment.entity.Assignment;
import com.studyflow.domain.assignment.exception.AssignmentAccessForbiddenException;
import com.studyflow.domain.assignment.exception.AssignmentNotFoundException;
import com.studyflow.domain.assignment.repository.AssignmentRepository;
import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.course.service.CourseAccessValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final CourseAccessValidator accessValidator;

    public List<AssignmentResponse> getAssignments(Long courseId, Long userId) {
        accessValidator.validateParticipantAndGetCourse(courseId, userId);
        return assignmentRepository.findByCourseIdOrderByCreatedAtDesc(courseId)
                .stream().map(AssignmentResponse::of).toList();
    }

    @Transactional
    public AssignmentResponse createAssignment(Long courseId, Long userId, AssignmentRequest req) {
        Course course = accessValidator.validateParticipantAndGetCourse(courseId, userId);
        accessValidator.validateTeacher(course, userId);
        Assignment assignment = Assignment.create(course, req.getTitle(), req.getContent(), req.getDueDate());
        return AssignmentResponse.of(assignmentRepository.save(assignment));
    }

    @Transactional
    public AssignmentResponse updateAssignment(Long courseId, Long assignmentId, Long userId, AssignmentRequest req) {
        Course course = accessValidator.validateParticipantAndGetCourse(courseId, userId);
        accessValidator.validateTeacher(course, userId);
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new AssignmentNotFoundException(assignmentId));
        if (!assignment.getCourse().getId().equals(courseId)) {
            throw new AssignmentAccessForbiddenException();
        }
        assignment.update(req.getTitle(), req.getContent(), req.getDueDate());
        return AssignmentResponse.of(assignment);
    }

    @Transactional
    public void deleteAssignment(Long courseId, Long assignmentId, Long userId) {
        Course course = accessValidator.validateParticipantAndGetCourse(courseId, userId);
        accessValidator.validateTeacher(course, userId);
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new AssignmentNotFoundException(assignmentId));
        if (!assignment.getCourse().getId().equals(courseId)) {
            throw new AssignmentAccessForbiddenException();
        }
        assignmentRepository.delete(assignment);
    }
}
