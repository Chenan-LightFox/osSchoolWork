package com.osschoolwork.backend.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.osschoolwork.backend.dto.AttachmentUploadResponse;
import com.osschoolwork.backend.entity.Attachment;
import com.osschoolwork.backend.entity.Mail;
import com.osschoolwork.backend.entity.Receiver;
import com.osschoolwork.backend.exception.BusinessException;
import com.osschoolwork.backend.mapper.AttachmentMapper;
import com.osschoolwork.backend.mapper.MailMapper;
import com.osschoolwork.backend.mapper.ReceiverMapper;
import com.osschoolwork.backend.service.AttachmentDownload;
import com.osschoolwork.backend.service.AttachmentService;

@Service
public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentMapper attachmentMapper;
    private final MailMapper mailMapper;
    private final ReceiverMapper receiverMapper;
    private final String uploadDir;

    @Autowired
    public AttachmentServiceImpl(AttachmentMapper attachmentMapper,
                                 MailMapper mailMapper,
                                 ReceiverMapper receiverMapper,
                                 @Value("${app.upload-dir:uploads}") String uploadDir) {
        this.attachmentMapper = attachmentMapper;
        this.mailMapper = mailMapper;
        this.receiverMapper = receiverMapper;
        this.uploadDir = uploadDir;
    }

    @Override
    public AttachmentUploadResponse upload(Long userId, Long mailId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "附件不能为空");
        }
        Mail mail = mailMapper.selectById(mailId);
        if (mail == null) {
            throw new BusinessException(404, "邮件不存在");
        }
        if (!mail.getSenderId().equals(userId)) {
            throw new BusinessException(403, "无权上传附件");
        }

        String originalName = sanitizeFilename(file.getOriginalFilename());
        String datePath = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String storedName = UUID.randomUUID().toString().replace("-", "") + "_" + originalName;
        String relativePath = datePath + "/" + storedName;

        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path targetDir = root.resolve(datePath).normalize();
        ensureWithinRoot(root, targetDir);
        try {
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(storedName).normalize();
            ensureWithinRoot(root, targetPath);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new BusinessException(500, "附件保存失败");
        }

        Attachment attachment = new Attachment();
        attachment.setMailId(mailId);
        attachment.setFileName(originalName);
        attachment.setFilePath(relativePath);
        attachment.setFileSize(file.getSize());
        attachment.setUploadTime(LocalDateTime.now());
        attachmentMapper.insert(attachment);

        return new AttachmentUploadResponse(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getFileSize(),
                attachment.getUploadTime()
        );
    }

    @Override
    public AttachmentDownload resolveDownload(Long userId, Long attachmentId) {
        Attachment attachment = attachmentMapper.selectById(attachmentId);
        if (attachment == null) {
            throw new BusinessException(404, "附件不存在");
        }
        Mail mail = mailMapper.selectById(attachment.getMailId());
        if (mail == null) {
            throw new BusinessException(404, "邮件不存在");
        }
        if (!hasAccess(userId, mail)) {
            throw new BusinessException(403, "无权下载该附件");
        }

        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path filePath = root.resolve(attachment.getFilePath()).normalize();
        ensureWithinRoot(root, filePath);
        String pathValue = Objects.requireNonNull(filePath.toString(), "filePath");
        Resource resource = new FileSystemResource(pathValue);
        if (!resource.exists()) {
            throw new BusinessException(404, "附件文件不存在");
        }

        String contentType;
        try {
            contentType = Files.probeContentType(filePath);
        } catch (IOException ignored) {
            contentType = null;
        }

        return new AttachmentDownload(attachment, resource, contentType);
    }

    private boolean hasAccess(Long userId, Mail mail) {
        if (mail.getSenderId().equals(userId)) {
            return true;
        }
        QueryWrapper<Receiver> wrapper = new QueryWrapper<Receiver>()
                .eq("mail_id", mail.getId())
                .eq("receiver_id", userId);
        return receiverMapper.selectCount(wrapper) > 0;
    }

    private void ensureWithinRoot(Path root, Path target) {
        if (!target.startsWith(root)) {
            throw new BusinessException(400, "非法附件路径");
        }
    }

    private String sanitizeFilename(String originalName) {
        if (originalName == null || originalName.trim().isEmpty()) {
            return "attachment";
        }
        String cleanName = Paths.get(originalName).getFileName().toString();
        return cleanName.replace("\r", "_").replace("\n", "_");
    }
}
