package com.osschoolwork.backend.service;

import com.osschoolwork.backend.dto.MailDetailView;
import com.osschoolwork.backend.dto.MailView;
import com.osschoolwork.backend.dto.SendMailRequest;

import java.util.List;

public interface MailService {

    List<MailView> getInbox(Long userId);

    List<MailView> getSent(Long userId);

    List<MailView> searchInbox(Long userId, String keyword);

    MailDetailView getMailDetail(Long userId, Long mailId);

    Long sendMail(Long userId, SendMailRequest request);

    void markAsRead(Long userId, Long mailId);

    void trashMail(Long userId, Long mailId);
}
