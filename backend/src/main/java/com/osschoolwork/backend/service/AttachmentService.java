package com.osschoolwork.backend.service;

import org.springframework.web.multipart.MultipartFile;

import com.osschoolwork.backend.dto.AttachmentUploadResponse;

public interface AttachmentService {

    AttachmentUploadResponse upload(Long userId, Long mailId, MultipartFile file);

    AttachmentDownload resolveDownload(Long userId, Long attachmentId);
}
