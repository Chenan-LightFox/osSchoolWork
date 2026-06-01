package com.osschoolwork.backend.controller;

import com.osschoolwork.backend.common.ApiResponse;
import com.osschoolwork.backend.dto.MailDetailView;
import com.osschoolwork.backend.dto.MailView;
import com.osschoolwork.backend.dto.SendMailRequest;
import com.osschoolwork.backend.service.MailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @GetMapping("/{mailId}")
    public ApiResponse<MailDetailView> detail(@PathVariable Long mailId,
                                              HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(mailService.getMailDetail(userId, mailId));
    }

    @PostMapping("/send")
    public ApiResponse<Long> send(@Valid @RequestBody SendMailRequest request,
                                  HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        return ApiResponse.success(mailService.sendMail(userId, request));
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

    private Long getUserId(HttpServletRequest request) {
        Object value = request.getAttribute("userId");
        if (value == null) {
            throw new com.osschoolwork.backend.exception.BusinessException(401, "Unauthorized");
        }
        return (Long) value;
    }
}
