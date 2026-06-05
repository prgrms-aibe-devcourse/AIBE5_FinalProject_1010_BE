package com.studyflow.domain.file.controller;

import com.studyflow.domain.file.dto.response.FileUploadResponse;
import com.studyflow.domain.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 채팅 이미지 업로드 API.
     *
     * 요청:
     * POST /api/v1/files/chat/images
     * Content-Type: multipart/form-data
     * key: file
     *
     * 응답으로 받은 fileId를 WebSocket 이미지 메시지 전송 payload의 fileIds에 넣는다.
     */
    @PostMapping("/chat/images")
    public FileUploadResponse uploadChatImage(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal Long userId
    ) {
        return fileService.uploadChatImage(userId, file);
    }

    /**
     * 공지사항 첨부파일 업로드 API.
     *
     * POST /api/v1/files/notice/attachments
     * 허용: 이미지(jpg/png/webp) + PDF, 최대 20MB
     */
    @PostMapping("/notice/attachments")
    public FileUploadResponse uploadNoticeAttachment(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal Long userId
    ) {
        return fileService.uploadNoticeAttachment(userId, file);
    }

    /**
     * 게시판 첨부파일 업로드 API.
     *
     * POST /api/v1/files/post/attachments
     * 허용: 이미지(jpg/png/webp) + PDF, 최대 20MB
     */
    @PostMapping("/post/attachments")
    public FileUploadResponse uploadPostAttachment(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal Long userId
    ) {
        return fileService.uploadPostAttachment(userId, file);
    }
}
