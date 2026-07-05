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

    @Override
    public List<MailView> getInbox(Long userId) {
        return mailMapper.selectInbox(userId);
    }

    @Override
    public List<MailView> getSent(Long userId) {
        return mailMapper.selectSent(userId);
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

        List<Receiver> receiverList = receiverMapper.selectList(new QueryWrapper<Receiver>().eq("mail_id", mailId));
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

    @Transactional
    @Override
    public Long sendMail(Long userId, SendMailRequest request) {
        SendResult result = createMailAndReceivers(userId, request);
        webSocketNotifier.notifyNewMail(result.receiverIds, result.mail.getId(), userId, result.mail.getSubject());
        return result.mail.getId();
    }

    @Transactional
    @Override
    public Long sendMailWithAttachments(Long userId, SendMailRequest request, List<MultipartFile> files) {
        SendResult result = createMailAndReceivers(userId, request);
        if (files != null) {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }
                attachmentService.upload(userId, result.mail.getId(), file);
            }
        }
        webSocketNotifier.notifyNewMail(result.receiverIds, result.mail.getId(), userId, result.mail.getSubject());
        return result.mail.getId();
    }

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
    public List<MailView> getTrash(Long userId) {
        return mailMapper.selectTrash(userId);
    }

    @Override
    public void restoreMail(Long userId, Long mailId) {
        Mail mail = mailMapper.selectById(mailId);
        if (mail == null) {
            throw new BusinessException(404, "邮件不存在");
        }
        int updated = receiverMapper.restoreMail(mailId, userId);
        if (updated == 0) {
            throw new BusinessException(404, "邮件不存在或不在垃圾箱中");
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

    private SendResult createMailAndReceivers(Long userId, SendMailRequest request) {
        Set<String> toEmails = request.getTo() == null ? new LinkedHashSet<>() : new LinkedHashSet<>(request.getTo());
        Set<String> ccEmails = request.getCc() == null ? new LinkedHashSet<>() : new LinkedHashSet<>(request.getCc());
        if (toEmails.isEmpty() && ccEmails.isEmpty()) {
            throw new BusinessException(400, "请至少填写一个收件人");
        }

        Set<String> allEmails = new LinkedHashSet<>(toEmails);
        allEmails.addAll(ccEmails);
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

        Mail mail = new Mail();
        mail.setSenderId(userId);
        mail.setSubject(request.getSubject());
        mail.setContent(request.getContent());
        mail.setStatus("SENT");
        mail.setSendTime(LocalDateTime.now());
        mailMapper.insert(mail);

        List<Receiver> receivers = new ArrayList<>();
        for (String email : toEmails) {
            User receiverUser = userByEmail.get(email);
            if (receiverUser == null) {
                continue;
            }
            Receiver receiver = new Receiver();
            receiver.setMailId(mail.getId());
            receiver.setReceiverId(receiverUser.getId());
            receiver.setReceiverType("TO");
            receiver.setIsRead(0);
            receiver.setDeleted(0);
            receiver.setFolder("INBOX");
            receivers.add(receiver);
        }
        for (String email : ccEmails) {
            if (toEmails.contains(email)) {
                continue;
            }
            User receiverUser = userByEmail.get(email);
            if (receiverUser == null) {
                continue;
            }
            Receiver receiver = new Receiver();
            receiver.setMailId(mail.getId());
            receiver.setReceiverId(receiverUser.getId());
            receiver.setReceiverType("CC");
            receiver.setIsRead(0);
            receiver.setDeleted(0);
            receiver.setFolder("INBOX");
            receivers.add(receiver);
        }
        for (Receiver receiver : receivers) {
            receiverMapper.insert(receiver);
        }
        Set<Long> receiverIds = receivers.stream()
                .map(Receiver::getReceiverId)
                .collect(Collectors.toSet());
        return new SendResult(mail, receiverIds);
    }

    private static class SendResult {
        private final Mail mail;
        private final Set<Long> receiverIds;

        private SendResult(Mail mail, Set<Long> receiverIds) {
            this.mail = mail;
            this.receiverIds = receiverIds;
        }
    }
}
