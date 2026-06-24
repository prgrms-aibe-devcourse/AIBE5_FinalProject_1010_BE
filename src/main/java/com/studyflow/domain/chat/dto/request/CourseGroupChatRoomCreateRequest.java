package com.studyflow.domain.chat.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 수업 단체 채팅방 생성 요청.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseGroupChatRoomCreateRequest {

    @NotNull(message = "수업 ID는 필수입니다.")
    private Long courseId;

    /**
     * 초대할 학생 ID 목록.
     *
     * 비어 있거나 null이면 해당 수업의 ACTIVE 수강생 전체를 초대한다.
     */
    private List<Long> studentIds;
}
