package com.studyflow.domain.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VoiceCallSettingRequest {
    private boolean voiceCallEnabled;

    public VoiceCallSettingRequest(boolean voiceCallEnabled) {
        this.voiceCallEnabled = voiceCallEnabled;
    }
}
