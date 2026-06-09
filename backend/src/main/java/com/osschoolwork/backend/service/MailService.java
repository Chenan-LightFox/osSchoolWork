package com.osschoolwork.backend.service;

import com.osschoolwork.backend.dto.DraftRequest;
import com.osschoolwork.backend.dto.MailDetailView;
import com.osschoolwork.backend.dto.MailView;
import com.osschoolwork.backend.dto.SendMailRequest;

import java.util.List;

public interface MailService {

    List<MailView> getInbox(Long userId);

    List<MailView> getSent(Long userId);

    List<MailView> getTrash(Long userId);

    List<MailView> getDrafts(Long userId);

    List<MailView> searchInbox(Long userId, String keyword);

    MailDetailView getMailDetail(Long userId, Long mailId);

    Long sendMail(Long userId, SendMailRequest request);

    void markAsRead(Long userId, Long mailId);

    void trashMail(Long userId, Long mailId);

    void restoreMail(Long userId, Long mailId);

    /** 保存草稿，返回邮件 ID */
    Long saveDraft(Long userId, DraftRequest request);

    /** 更新草稿内容（含收件人） */
    void updateDraft(Long userId, Long mailId, DraftRequest request);

    /** 发送草稿（DRAFT → SENT） */
    void sendDraft(Long userId, Long mailId);

    /** 永久删除垃圾箱中的邮件（硬删除收件关系，无剩余收件人时清理邮件与附件） */
    void permanentDelete(Long userId, Long mailId);
}
