package com.studyflow.domain.teacher.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class TeacherProfileUpdateRequest {

    @Size(max = 200)
    private String address;

    @Size(max = 5000)
    private String introduction;

    @Size(max = 500)
    private String teachingStyle;

    // 전문 과목 id 목록 — null이면 미변경, 빈 배열이면 전체 해제
    private List<Long> specialtySubjectIds;
}
