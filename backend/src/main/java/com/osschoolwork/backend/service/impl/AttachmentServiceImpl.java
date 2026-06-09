package com.osschoolwork.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.osschoolwork.backend.config.AppProperties;
import com.osschoolwork.backend.entity.Attachment;
import com.osschoolwork.backend.entity.Mail;
import com.osschoolwork.backend.entity.Receiver;
import com.osschoolwork.backend.exception.BusinessException;
import com.osschoolwork.backend.mapper.AttachmentMapper;
import com.osschoolwork.backend.mapper.MailMapper;
import com.osschoolwork.backend.mapper.ReceiverMapper;
import com.osschoolwork.backend.service.AttachmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentMapper attachmentMapper;
    private final MailMapper mailMapper;
    private final ReceiverMapper receiverMapper;
    private final Path uploadRoot;

    @Autowired
    public AttachmentServiceImpl(AttachmentMapper attachmentMapper,
                                 MailMapper mailMapper,
                                 ReceiverMapper receiverMapper,
                                 AppProperties appProperties) {
        this.attachmentMapper = attachmentMapper;
        this.mailMapper = mailMapper;
        this.receiverMapper = receiverMapper;
        this.uploadRoot = Paths.get(appProperties.getUploadDir()).toAbsolutePath().normalize();
    }

    @Transactional
    @Override
    public Attachment upload(Long userId, Long mailId, MultipartFile file) {
        // 1. 校验邮件属于当前用户（仅发件人可上传附件）
        Mail mail = mailMapper.selectById(mailId);
        if (mail == null) {
            throw new BusinessException(404, "邮件不存在");
        }
        if (!mail.getSenderId().equals(userId)) {
            throw new BusinessException(403, "无权上传附件");
        }

        // 2. 校验文件
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "文件不能为空");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new BusinessException(400, "文件名不能为空");
        }

        // 3. 写入磁盘：{uploadDir}/{userId}/{mailId}/{uuid}_{原始文件名}
        Path dir = uploadRoot.resolve(String.valueOf(userId))
                             .resolve(String.valueOf(mailId));
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new BusinessException(500, "创建上传目录失败");
        }

        String storedName = UUID.randomUUID().toString().substring(0, 8)
                + "_" + originalName;
        Path target = dir.resolve(storedName);
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException(500, "文件写入失败");
        }

        // 4. 写入数据库
        Attachment attachment = new Attachment();
        attachment.setMailId(mailId);
        attachment.setFileName(originalName);
        attachment.setFilePath(target.toString());
        attachment.setFileSize(file.getSize());
        attachment.setUploadTime(LocalDateTime.now());
        attachmentMapper.insert(attachment);

        return attachment;
    }

    @Override
    public Attachment getForDownload(Long userId, Long attachmentId) {
        Attachment attachment = attachmentMapper.selectById(attachmentId);
        if (attachment == null) {
            throw new BusinessException(404, "附件不存在");
        }
        // 权限：发件人或收件人均可下载
        Mail mail = mailMapper.selectById(attachment.getMailId());
        if (mail == null) {
            throw new BusinessException(404, "关联邮件不存在");
        }
        if (!hasAccess(userId, mail)) {
            throw new BusinessException(403, "无权下载该附件");
        }
        return attachment;
    }

    @Transactional
    @Override
    public void deleteAttachment(Long userId, Long attachmentId) {
        Attachment attachment = attachmentMapper.selectById(attachmentId);
        if (attachment == null) {
            throw new BusinessException(404, "附件不存在");
        }
        Mail mail = mailMapper.selectById(attachment.getMailId());
        if (mail == null) {
            throw new BusinessException(404, "关联邮件不存在");
        }
        if (!mail.getSenderId().equals(userId)) {
            throw new BusinessException(403, "无权删除该附件");
        }

        // 删除磁盘文件
        try {
            Path filePath = Paths.get(attachment.getFilePath());
            Files.deleteIfExists(filePath);
        } catch (IOException ignored) {
            // 文件可能已不存在，继续清理数据库
        }

        // 删除数据库记录
        attachmentMapper.deleteById(attachmentId);
    }

    @Override
    public List<Attachment> listByMailId(Long mailId) {
        return attachmentMapper.selectByMailId(mailId);
    }

    @Override
    public InputStream getFileStream(Attachment attachment) {
        try {
            Path path = Paths.get(attachment.getFilePath());
            return Files.newInputStream(path);
        } catch (IOException e) {
            throw new BusinessException(500, "读取附件文件失败");
        }
    }

    private boolean hasAccess(Long userId, Mail mail) {
        if (mail.getSenderId().equals(userId)) {
            return true;
        }
        QueryWrapper<Receiver> wrapper = new QueryWrapper<>();
        wrapper.eq("mail_id", mail.getId()).eq("receiver_id", userId);
        return receiverMapper.selectCount(wrapper) > 0;
    }
}
