package com.osschoolwork.backend.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.osschoolwork.backend.dto.MailDetailView;
import com.osschoolwork.backend.dto.MailView;
import com.osschoolwork.backend.dto.SendMailRequest;

public interface MailService {

    List<MailView> getInbox(Long userId);

    List<MailView> getSent(Long userId);

    List<MailView> searchInbox(Long userId, String keyword);

    MailDetailView getMailDetail(Long userId, Long mailId);

    Long sendMail(Long userId, SendMailRequest request);

    Long sendMailWithAttachments(Long userId, SendMailRequest request, List<MultipartFile> files);

    void markAsRead(Long userId, Long mailId);

    void trashMail(Long userId, Long mailId);

    List<MailView> getTrash(Long userId);

    void restoreMail(Long userId, Long mailId);
}
