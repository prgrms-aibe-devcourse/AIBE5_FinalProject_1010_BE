package com.studyflow.domain.course.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyflow.domain.course.dto.common.CourseAttachmentInfo;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

// List<CourseAttachmentInfo> ↔ TEXT(JSON) 변환 — course_notice.attachments 컬럼에 사용
@Slf4j
@Converter
@Component
@RequiredArgsConstructor
public class NoticeAttachmentListConverter
        implements AttributeConverter<List<CourseAttachmentInfo>, String> {

    private final ObjectMapper mapper;

    @Override
    public String convertToDatabaseColumn(List<CourseAttachmentInfo> list) {
        if (list == null || list.isEmpty()) return null;
        try {
            return mapper.writeValueAsString(list);
        } catch (Exception e) {
            log.error("첨부파일 목록 직렬화 실패: {}", e.getMessage(), e);
            throw new IllegalStateException("첨부파일 목록을 JSON으로 변환하는 데 실패했습니다.", e);
        }
    }

    @Override
    public List<CourseAttachmentInfo> convertToEntityAttribute(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return mapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("첨부파일 목록 역직렬화 실패 — json='{}': {}", json, e.getMessage(), e);
            throw new IllegalStateException("첨부파일 목록 JSON을 파싱하는 데 실패했습니다.", e);
        }
    }
}
