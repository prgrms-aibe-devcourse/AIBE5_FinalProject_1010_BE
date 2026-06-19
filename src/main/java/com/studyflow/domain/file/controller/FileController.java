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
     * QnA(질문게시판) 이미지 업로드 API.
     *
     * 요청:
     * POST /api/v1/files/qna/images
     * Content-Type: multipart/form-data
     * key: file
     * 허용: 이미지(jpg/png/webp), 최대 10MB
     *
     * 응답으로 받은 fileId를 질문/답변 작성 요청의 imageFileIds 배열에 넣는다(여러 장이면 각각 업로드 후 모아서).
     */
    @PostMapping("/qna/images")
    public FileUploadResponse uploadQnaImage(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal Long userId
    ) {
        return fileService.uploadQnaImage(userId, file);
    }

    /**
     * 프로필 이미지 업로드 API.
     *
     * 요청:
     * POST /api/v1/files/profile/images
     * Content-Type: multipart/form-data
     * key: file
     * 허용: 이미지(jpg/png/webp), 최대 10MB
     *
     * 응답으로 받은 fileUrl을 회원 정보 수정 API(PATCH /api/v1/users/me)의 profileImageUrl 필드에 전달한다.
     */
    @PostMapping("/profile/images")
    public FileUploadResponse uploadProfileImage(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal Long userId
    ) {
        return fileService.uploadProfileImage(userId, file);
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

    /**
     * Classroom document upload API.
     *
     * POST /api/v1/files/classroom/documents
     * Allowed: PDF, max 20MB. The uploaded file can be used as a whiteboard background.
     */
    @PostMapping("/classroom/documents")
    public FileUploadResponse uploadClassroomDocument(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal Long userId
    ) {
        return fileService.uploadClassroomDocument(userId, file);
    }

    /**
     * 선생님 인증 서류 업로드 API.
     *
     * POST /api/v1/files/verification/documents
     * 허용: 이미지(jpg/png/webp) + PDF, 최대 20MB
     * 응답의 fileUrl을 인증 신청 API(POST /api/v1/teachers/verification)의 documentUrl 필드에 전달한다.
     */
    @PostMapping("/verification/documents")
    public FileUploadResponse uploadVerificationDocument(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal Long userId
    ) {
        return fileService.uploadVerificationDocument(userId, file);
    }

}
