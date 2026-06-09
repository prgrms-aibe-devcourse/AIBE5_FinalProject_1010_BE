package com.studyflow.domain.admin.entity;

import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "user_count_statistics", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_count_statistics_date", columnNames = {"date"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCountStatistics extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private long newStudentCount;

    @Column(nullable = false)
    private long newTeacherCount;

    @Column(nullable = false)
    private long deletedStudentCount;

    @Column(nullable = false)
    private long deletedTeacherCount;

    @Builder
    private UserCountStatistics(LocalDate date, long newStudentCount, long newTeacherCount,
                                long deletedStudentCount, long deletedTeacherCount) {
        this.date = date;
        this.newStudentCount = newStudentCount;
        this.newTeacherCount = newTeacherCount;
        this.deletedStudentCount = deletedStudentCount;
        this.deletedTeacherCount = deletedTeacherCount;
    }
}
