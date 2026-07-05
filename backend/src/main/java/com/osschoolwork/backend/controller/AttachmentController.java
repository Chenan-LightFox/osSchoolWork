package com.osschoolwork.backend.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.osschoolwork.backend.common.ApiResponse;
import com.osschoolwork.backend.dto.AttachmentUploadResponse;
import com.osschoolwork.backend.exception.BusinessException;
import com.osschoolwork.backend.service.AttachmentDownload;
import com.osschoolwork.backend.service.AttachmentService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/attachment")
public class AttachmentController {

    private final AttachmentService attachmentService;

    @Autowired
    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AttachmentUploadResponse> upload(@RequestParam("mailId") Long mailId,
                                                        @RequestParam("file") MultipartFile file,
                                                        HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(attachmentService.upload(userId, mailId, file));
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> download(@PathVariable("id") Long attachmentId,
                                             HttpServletRequest request) {
        Long userId = getUserId(request);
        AttachmentDownload download = attachmentService.resolveDownload(userId, attachmentId);

        String contentType = download.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.getAttachment().getFileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(download.getAttachment().getFileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(download.getResource());
    }

    private Long getUserId(HttpServletRequest request) {
        Object value = request.getAttribute("userId");
        if (value == null) {
            throw new BusinessException(401, "Unauthorized");
        }
        return (Long) value;
    }
}
