package com.studyflow.domain.chat.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 선생님-학생 1:1 채팅방 생성 요청.
 *
 * 현재 목표는 수업 생성 전, 선생님과 학생이 서로 조건을 맞춰보는 "문의 채팅"이다.
 * 그래서 courseId는 당장 필수로 받지 않고, teacherId/studentId만으로 채팅방을 생성한다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomCreateRequest {

    /**
     * 수업 ID.
     *
     * 기존 명세에 있던 필드라 남겨둔다.
     * 지금 1:1 매칭 문의 채팅에서는 아직 Course와 연결하지 않으므로 필수값이 아니다.
     * 나중에 Course 기반 채팅으로 확장할 때 다시 사용할 수 있다.
     */
    private Long courseId;

    @NotNull(message = "선생님 ID는 필수입니다.")
    private Long teacherId;

    @NotNull(message = "학생 ID는 필수입니다.")
    private Long studentId;
}
