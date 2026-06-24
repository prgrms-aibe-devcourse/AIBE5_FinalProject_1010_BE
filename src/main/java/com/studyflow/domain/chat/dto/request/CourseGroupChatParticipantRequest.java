package com.studyflow.domain.chat.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 수업 단체 채팅방 참여자 초대 요청.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseGroupChatParticipantRequest {

    @NotEmpty(message = "학생 ID 목록은 필수입니다.")
    private List<Long> studentIds;
}
