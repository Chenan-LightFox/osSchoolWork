package com.osschoolwork.backend.controller;

import com.osschoolwork.backend.common.ApiResponse;
import com.osschoolwork.backend.entity.Attachment;
import com.osschoolwork.backend.service.AttachmentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/attachment")
public class AttachmentController {

    private final AttachmentService attachmentService;

    @Autowired
    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    /**
     * 上传附件并绑定到指定邮件
     */
    @PostMapping("/upload")
    public ApiResponse<Attachment> upload(@RequestParam("mailId") Long mailId,
                                          @RequestParam("file") MultipartFile file,
                                          HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(attachmentService.upload(userId, mailId, file));
    }

    /**
     * 下载附件
     */
    @GetMapping("/download/{attachmentId}")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long attachmentId,
                                                        HttpServletRequest request) {
        Long userId = getUserId(request);
        Attachment attachment = attachmentService.getForDownload(userId, attachmentId);
        InputStream stream = attachmentService.getFileStream(attachment);

        String encodedName = URLEncoder.encode(attachment.getFileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encodedName + "\"")
                .body(new InputStreamResource(stream));
    }

    /**
     * 删除单个附件
     */
    @DeleteMapping("/{attachmentId}")
    public ApiResponse<Void> deleteAttachment(@PathVariable Long attachmentId,
                                              HttpServletRequest request) {
        Long userId = getUserId(request);
        attachmentService.deleteAttachment(userId, attachmentId);
        return ApiResponse.success(null);
    }

    /**
     * 列出某邮件的所有附件
     */
    @GetMapping("/list/{mailId}")
    public ApiResponse<List<Attachment>> listByMail(@PathVariable Long mailId) {
        return ApiResponse.success(attachmentService.listByMailId(mailId));
    }

    private Long getUserId(HttpServletRequest request) {
        Object value = request.getAttribute("userId");
        if (value == null) {
            throw new com.osschoolwork.backend.exception.BusinessException(401, "Unauthorized");
        }
        return (Long) value;
    }
}
