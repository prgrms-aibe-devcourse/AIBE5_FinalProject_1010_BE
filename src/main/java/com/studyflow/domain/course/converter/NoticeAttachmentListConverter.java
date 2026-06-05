package com.studyflow.domain.course.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyflow.domain.course.dto.notice.NoticeAttachmentInfo;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Collections;
import java.util.List;

// List<NoticeAttachmentInfo> ↔ TEXT(JSON) 변환 — course_notice.attachments 컬럼에 사용
@Converter
public class NoticeAttachmentListConverter
        implements AttributeConverter<List<NoticeAttachmentInfo>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<NoticeAttachmentInfo> list) {
        if (list == null || list.isEmpty()) return null;
        try {
            return MAPPER.writeValueAsString(list);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<NoticeAttachmentInfo> convertToEntityAttribute(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
