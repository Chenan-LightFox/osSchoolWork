package com.osschoolwork.backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.osschoolwork.backend.common.ApiResponse;
import com.osschoolwork.backend.dto.MailDetailView;
import com.osschoolwork.backend.dto.MailView;
import com.osschoolwork.backend.dto.SendMailRequest;
import com.osschoolwork.backend.service.MailService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mail")
public class MailController {

    private final MailService mailService;

    @Autowired
    public MailController(MailService mailService) {
        this.mailService = mailService;
    }

    @GetMapping("/inbox")
    public ApiResponse<List<MailView>> inbox(HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(mailService.getInbox(userId));
    }

    @GetMapping("/sent")
    public ApiResponse<List<MailView>> sent(HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(mailService.getSent(userId));
    }

    @GetMapping("/search")
    public ApiResponse<List<MailView>> search(@RequestParam(value = "q", required = false) String keyword,
                                              HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(mailService.searchInbox(userId, keyword));
    }

    @GetMapping("/trash")
    public ApiResponse<List<MailView>> trash(HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(mailService.getTrash(userId));
    }

    @GetMapping("/{mailId}")
    public ApiResponse<MailDetailView> detail(@PathVariable Long mailId,
                                              HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(mailService.getMailDetail(userId, mailId));
    }

    @PostMapping(value = "/send", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Long> send(@Valid @RequestBody SendMailRequest request,
                                  HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        return ApiResponse.success(mailService.sendMail(userId, request));
    }

    @PostMapping(value = "/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Long> sendWithAttachments(@Valid @RequestPart("payload") SendMailRequest request,
                                                 @RequestPart(value = "files", required = false)
                                                 List<MultipartFile> files,
                                                 HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        return ApiResponse.success(mailService.sendMailWithAttachments(userId, request, files));
    }

    @PostMapping("/{mailId}/read")
    public ApiResponse<Void> markRead(@PathVariable Long mailId,
                                      HttpServletRequest request) {
        Long userId = getUserId(request);
        mailService.markAsRead(userId, mailId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{mailId}")
    public ApiResponse<Void> delete(@PathVariable Long mailId,
                                    HttpServletRequest request) {
        Long userId = getUserId(request);
        mailService.trashMail(userId, mailId);
        return ApiResponse.success(null);
    }

    @PutMapping("/{mailId}/restore")
    public ApiResponse<Void> restore(@PathVariable Long mailId,
                                     HttpServletRequest request) {
        Long userId = getUserId(request);
        mailService.restoreMail(userId, mailId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{mailId}/permanent")
    public ApiResponse<Void> permanentDelete(@PathVariable Long mailId,
                                              HttpServletRequest request) {
        Long userId = getUserId(request);
        mailService.permanentDelete(userId, mailId);
        return ApiResponse.success(null);
    }

    // ==================== 垃圾邮件 ====================

    @GetMapping("/spam")
    public ApiResponse<List<MailView>> spam(HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(mailService.getSpam(userId));
    }

    @PutMapping("/{mailId}/spam")
    public ApiResponse<Void> markSpam(@PathVariable Long mailId,
                                       HttpServletRequest request) {
        Long userId = getUserId(request);
        mailService.markAsSpam(userId, mailId);
        return ApiResponse.success(null);
    }

    @PutMapping("/{mailId}/not-spam")
    public ApiResponse<Void> markNotSpam(@PathVariable Long mailId,
                                          HttpServletRequest request) {
        Long userId = getUserId(request);
        mailService.markAsNotSpam(userId, mailId);
        return ApiResponse.success(null);
    }

    // ==================== 草稿 ====================

    @GetMapping("/drafts")
    public ApiResponse<List<MailView>> drafts(HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(mailService.getDrafts(userId));
    }

    @PostMapping("/draft")
    public ApiResponse<Long> saveDraft(@RequestBody SendMailRequest body,
                                       @RequestParam(value = "draftId", required = false) Long draftId,
                                       HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(mailService.saveDraft(userId, body, draftId));
    }

    @PostMapping("/draft/{draftId}/send")
    public ApiResponse<Long> sendDraft(@PathVariable Long draftId,
                                       @Valid @RequestBody SendMailRequest body,
                                       HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(mailService.sendDraft(userId, draftId, body));
    }

    @DeleteMapping("/draft/{draftId}")
    public ApiResponse<Void> deleteDraft(@PathVariable Long draftId,
                                          HttpServletRequest request) {
        Long userId = getUserId(request);
        mailService.deleteDraft(userId, draftId);
        return ApiResponse.success(null);
    }

    private Long getUserId(HttpServletRequest request) {
        Object value = request.getAttribute("userId");
        if (value == null) {
            throw new com.osschoolwork.backend.exception.BusinessException(401, "Unauthorized");
        }
        return (Long) value;
    }
}
