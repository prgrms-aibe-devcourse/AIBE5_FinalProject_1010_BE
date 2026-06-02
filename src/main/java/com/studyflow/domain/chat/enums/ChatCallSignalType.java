package com.studyflow.domain.chat.enums;

/**
 * 1:1 보이스톡 WebRTC 신호 타입.
 *
 * 서버는 음성 데이터를 저장하거나 중계하지 않고, 같은 채팅방 참여자에게
 * WebRTC 연결에 필요한 신호만 브로드캐스트한다.
 */
public enum ChatCallSignalType {
    INVITE,
    ACCEPT,
    REJECT,
    OFFER,
    ANSWER,
    ICE,
    HANGUP
}
