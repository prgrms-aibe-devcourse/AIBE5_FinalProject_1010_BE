package com.studyflow.domain.course.dto.notice;

/**
 * @deprecated {@link com.studyflow.domain.course.dto.common.CourseAttachmentInfo} 로 대체됨
 */
@Deprecated
public class NoticeAttachmentInfo extends com.studyflow.domain.course.dto.common.CourseAttachmentInfo {
    @Deprecated public NoticeAttachmentInfo() {}
    @Deprecated public NoticeAttachmentInfo(String url, String originalFileName, Long fileSize, String contentType) {
        super(url, originalFileName, fileSize, contentType);
    }
}
