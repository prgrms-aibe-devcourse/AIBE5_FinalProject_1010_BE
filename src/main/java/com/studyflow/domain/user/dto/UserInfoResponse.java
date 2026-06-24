package com.studyflow.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.enums.UserRole;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL) // null 필드는 응답에서 제외 (isVerified 등)
public class UserInfoResponse {

    private final String name;
    private final String email;
    private final LocalDate birthDate;
    private final String gender;
    private final boolean marketingAgreed;
    private final String phone;
    private final String profileImageUrl;
    private final String role;
    private final String socialProvider;
    private final Boolean isVerified; // TEACHER인 경우에만 포함
    private final Boolean voiceCallEnabled;

    public UserInfoResponse(User user) {
        this.name            = user.getName();
        this.email           = user.getEmail();
        this.birthDate       = user.getBirthDate();
        this.gender          = user.getGender() != null ? user.getGender().name() : null;
        this.marketingAgreed = user.isMarketingAgreed();
        this.phone           = user.getPhone();
        this.profileImageUrl = user.getProfileImageUrl();
        this.role            = user.getRole().name();
        this.socialProvider  = user.getSocialProvider().name();
        this.isVerified      = user.getRole() == UserRole.TEACHER ? user.isVerified() : null;
        this.voiceCallEnabled = user.isVoiceCallEnabled();
    }
}
