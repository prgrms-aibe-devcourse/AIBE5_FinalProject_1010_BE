package com.studyflow.domain.review.repository;

import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByCourseOrderByCreatedAtDesc(Course course);
}
