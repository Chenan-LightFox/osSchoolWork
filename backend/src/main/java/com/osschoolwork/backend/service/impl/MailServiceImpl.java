package com.osschoolwork.backend.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.osschoolwork.backend.dto.DraftRequest;
import com.osschoolwork.backend.dto.MailDetailView;
import com.osschoolwork.backend.dto.MailView;
import com.osschoolwork.backend.dto.SendMailRequest;
import com.osschoolwork.backend.entity.Attachment;
import com.osschoolwork.backend.entity.Mail;
import com.osschoolwork.backend.entity.Receiver;
import com.osschoolwork.backend.entity.User;
import com.osschoolwork.backend.exception.BusinessException;
import com.osschoolwork.backend.mapper.AttachmentMapper;
import com.osschoolwork.backend.mapper.MailMapper;
import com.osschoolwork.backend.mapper.ReceiverMapper;
import com.osschoolwork.backend.mapper.UserMapper;
import com.osschoolwork.backend.service.AttachmentService;
import com.osschoolwork.backend.service.MailService;
import com.osschoolwork.backend.websocket.WebSocketNotifier;

@Service
public class MailServiceImpl implements MailService {

    private final MailMapper mailMapper;
    private final ReceiverMapper receiverMapper;
    private final AttachmentMapper attachmentMapper;
    private final UserMapper userMapper;
    private final WebSocketNotifier webSocketNotifier;
    private final AttachmentService attachmentService;

    @Autowired
    public MailServiceImpl(MailMapper mailMapper,
                           ReceiverMapper receiverMapper,
                           AttachmentMapper attachmentMapper,
                           UserMapper userMapper,
                           WebSocketNotifier webSocketNotifier,
                           AttachmentService attachmentService) {
        this.mailMapper = mailMapper;
        this.receiverMapper = receiverMapper;
        this.attachmentMapper = attachmentMapper;
        this.userMapper = userMapper;
        this.webSocketNotifier = webSocketNotifier;
        this.attachmentService = attachmentService;
    }

    // ---------------------------------------------------------------
    // 查询
    // ---------------------------------------------------------------

    @Override
    public List<MailView> getInbox(Long userId) {
        return mailMapper.selectInbox(userId);
    }

    @Override
    public List<MailView> getSent(Long userId) {
        return mailMapper.selectSent(userId);
    }

    @Override
    public List<MailView> getTrash(Long userId) {
        return mailMapper.selectTrash(userId);
    }

    @Override
    public List<MailView> getDrafts(Long userId) {
        return mailMapper.selectDrafts(userId);
    }

    @Override
    public List<MailView> searchInbox(Long userId, String keyword) {
        return mailMapper.searchMails(userId, keyword == null ? "" : keyword.trim());
    }

    @Override
    public MailDetailView getMailDetail(Long userId, Long mailId) {
        Mail mail = mailMapper.selectById(mailId);
        if (mail == null) {
            throw new BusinessException(404, "邮件不存在");
        }
        if (!hasAccess(userId, mail)) {
            throw new BusinessException(403, "无权查看该邮件");
        }

        MailDetailView detail = new MailDetailView();
        detail.setId(mail.getId());
        detail.setSubject(mail.getSubject());
        detail.setContent(mail.getContent());
        detail.setSendTime(mail.getSendTime());

        User sender = userMapper.selectById(mail.getSenderId());
        if (sender != null) {
            detail.setSenderId(sender.getId());
            detail.setSenderName(sender.getUsername());
            detail.setSenderEmail(sender.getEmail());
        }

        List<Receiver> receiverList = receiverMapper.selectList(
                new QueryWrapper<Receiver>().eq("mail_id", mailId));
        Set<Long> receiverIds = receiverList.stream()
                .map(Receiver::getReceiverId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, User> receiverUsers = userMapper.selectBatchIds(receiverIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        detail.setReceivers(receiverList.stream().map(r -> {
            MailDetailView.ReceiverView receiverView = new MailDetailView.ReceiverView();
            User receiverUser = receiverUsers.get(r.getReceiverId());
            receiverView.setUserId(r.getReceiverId());
            receiverView.setUsername(receiverUser != null ? receiverUser.getUsername() : null);
            receiverView.setEmail(receiverUser != null ? receiverUser.getEmail() : null);
            receiverView.setType(r.getReceiverType());
            return receiverView;
        }).collect(Collectors.toList()));

        List<Attachment> attachments = attachmentMapper.selectByMailId(mailId);
        detail.setAttachments(attachments.stream().map(a -> {
            MailDetailView.AttachmentView attachmentView = new MailDetailView.AttachmentView();
            attachmentView.setId(a.getId());
            attachmentView.setFileName(a.getFileName());
            attachmentView.setFileSize(a.getFileSize());
            attachmentView.setUploadTime(a.getUploadTime());
            return attachmentView;
        }).collect(Collectors.toList()));

        return detail;
    }

    // ---------------------------------------------------------------
    // 发送
    // ---------------------------------------------------------------

    @Transactional
    @Override
    public Long sendMail(Long userId, SendMailRequest request) {
        Mail mail = createMail(userId, request.getSubject(), request.getContent(), "SENT");
        createReceivers(mail.getId(), request.getTo(), request.getCc());
        return mail.getId();
    }

    // ---------------------------------------------------------------
    // 状态变更
    // ---------------------------------------------------------------

    @Override
    public void markAsRead(Long userId, Long mailId) {
        Mail mail = mailMapper.selectById(mailId);
        if (mail == null) {
            throw new BusinessException(404, "邮件不存在");
        }
        if (mail.getSenderId().equals(userId)) {
            return;
        }
        int updated = receiverMapper.markAsRead(mailId, userId);
        if (updated == 0) {
            throw new BusinessException(404, "邮件不存在或无权限");
        }
    }

    @Override
    public void trashMail(Long userId, Long mailId) {
        Mail mail = mailMapper.selectById(mailId);
        if (mail == null) {
            throw new BusinessException(404, "邮件不存在");
        }
        int updated = receiverMapper.softDelete(mailId, userId);
        if (updated == 0) {
            throw new BusinessException(404, "邮件不存在或无权限");
        }
    }

    @Override
    public void restoreMail(Long userId, Long mailId) {
        Mail mail = mailMapper.selectById(mailId);
        if (mail == null) {
            throw new BusinessException(404, "邮件不存在");
        }
        int updated = receiverMapper.restoreMail(mailId, userId);
        if (updated == 0) {
            throw new BusinessException(404, "邮件不在垃圾箱中");
        }
    }

    @Transactional
    @Override
    public void permanentDelete(Long userId, Long mailId) {
        Mail mail = mailMapper.selectById(mailId);
        if (mail == null) {
            throw new BusinessException(404, "邮件不存在");
        }

        // 硬删除当前用户在垃圾箱中的收件关系
        int deleted = receiverMapper.hardDelete(mailId, userId);
        if (deleted == 0) {
            throw new BusinessException(404, "邮件不在垃圾箱中");
        }

        // 若该邮件已无任何收件关系，清理附件与邮件本身
        int remaining = receiverMapper.countByMailId(mailId);
        if (remaining == 0) {
            attachmentMapper.deleteByMailId(mailId);
            mailMapper.deleteById(mailId);
        }
    }

    // ---------------------------------------------------------------
    // 草稿
    // ---------------------------------------------------------------

    @Transactional
    @Override
    public Long saveDraft(Long userId, DraftRequest request) {
        String subject = request.getSubject() == null ? "" : request.getSubject().trim();
        String content = request.getContent() == null ? "" : request.getContent();
        Mail mail = createMail(userId, subject, content, "DRAFT");
        createReceivers(mail.getId(), request.getTo(), request.getCc());
        return mail.getId();
    }

    @Transactional
    @Override
    public void updateDraft(Long userId, Long mailId, DraftRequest request) {
        Mail mail = mailMapper.selectById(mailId);
        if (mail == null) {
            throw new BusinessException(404, "草稿不存在");
        }
        if (!mail.getSenderId().equals(userId)) {
            throw new BusinessException(403, "无权修改该草稿");
        }
        if (!"DRAFT".equals(mail.getStatus())) {
            throw new BusinessException(400, "只能修改草稿状态下的邮件");
        }

        // 更新草稿字段
        mail.setSubject(request.getSubject() == null ? "" : request.getSubject().trim());
        mail.setContent(request.getContent() == null ? "" : request.getContent());
        mail.setSendTime(LocalDateTime.now());
        mailMapper.updateById(mail);

        // 重建收件关系
        if (request.getTo() != null || request.getCc() != null) {
            receiverMapper.deleteByMailId(mailId);
            createReceivers(mailId, request.getTo(), request.getCc());
        }
    }

    @Transactional
    @Override
    public void sendDraft(Long userId, Long mailId) {
        Mail mail = mailMapper.selectById(mailId);
        if (mail == null) {
            throw new BusinessException(404, "草稿不存在");
        }
        if (!mail.getSenderId().equals(userId)) {
            throw new BusinessException(403, "无权发送该草稿");
        }
        if (!"DRAFT".equals(mail.getStatus())) {
            throw new BusinessException(400, "该邮件不是草稿状态");
        }

        // 检查至少有一个收件人
        QueryWrapper<Receiver> wrapper = new QueryWrapper<>();
        wrapper.eq("mail_id", mailId);
        long receiverCount = receiverMapper.selectCount(wrapper);
        if (receiverCount == 0) {
            throw new BusinessException(400, "请至少填写一个收件人");
        }

        mail.setStatus("SENT");
        mail.setSendTime(LocalDateTime.now());
        mailMapper.updateById(mail);
    }

    // ---------------------------------------------------------------
    // 内部工具方法
    // ---------------------------------------------------------------

    /**
     * 创建邮件记录并插入数据库
     */
    private Mail createMail(Long userId, String subject, String content, String status) {
        Mail mail = new Mail();
        mail.setSenderId(userId);
        mail.setSubject(subject);
        mail.setContent(content);
        mail.setStatus(status);
        mail.setSendTime(LocalDateTime.now());
        mailMapper.insert(mail);
        return mail;
    }

    /**
     * 根据邮箱列表创建收件关系（不校验——调用方自行保证合法性）
     */
    private void createReceivers(Long mailId, List<String> toEmails, List<String> ccEmails) {
        Set<String> toSet = toEmails == null ? new LinkedHashSet<>() : new LinkedHashSet<>(toEmails);
        Set<String> ccSet = ccEmails == null ? new LinkedHashSet<>() : new LinkedHashSet<>(ccEmails);
        if (toSet.isEmpty() && ccSet.isEmpty()) {
            return;
        }

        Set<String> allEmails = new LinkedHashSet<>(toSet);
        allEmails.addAll(ccSet);

        QueryWrapper<User> userQuery = new QueryWrapper<>();
        userQuery.in("email", allEmails);
        List<User> users = userMapper.selectList(userQuery);

        Set<String> foundEmails = users.stream()
                .map(User::getEmail)
                .collect(Collectors.toSet());
        List<String> missingEmails = allEmails.stream()
                .filter(email -> !foundEmails.contains(email))
                .collect(Collectors.toList());
        if (!missingEmails.isEmpty()) {
            throw new BusinessException(400, "以下收件人不存在: " + String.join(", ", missingEmails));
        }

        Map<String, User> userByEmail = users.stream()
                .collect(Collectors.toMap(User::getEmail, Function.identity()));

        List<Receiver> receivers = new ArrayList<>();

        for (String email : toSet) {
            User receiverUser = userByEmail.get(email);
            if (receiverUser == null) continue;
            receivers.add(buildReceiver(mailId, receiverUser.getId(), "TO"));
        }
        for (String email : ccSet) {
            if (toSet.contains(email)) continue;
            User receiverUser = userByEmail.get(email);
            if (receiverUser == null) continue;
            receivers.add(buildReceiver(mailId, receiverUser.getId(), "CC"));
        }

        for (Receiver receiver : receivers) {
            receiverMapper.insert(receiver);
        }
    }

    private Receiver buildReceiver(Long mailId, Long receiverId, String type) {
        Receiver receiver = new Receiver();
        receiver.setMailId(mailId);
        receiver.setReceiverId(receiverId);
        receiver.setReceiverType(type);
        receiver.setIsRead(0);
        receiver.setDeleted(0);
        receiver.setFolder("INBOX");
        return receiver;
    }

        private SendResult(Mail mail, Set<Long> receiverIds) {
            this.mail = mail;
            this.receiverIds = receiverIds;
        }
    }
}
