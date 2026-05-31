package com.osschoolwork.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public class SendMailRequest {

    @NotBlank
    @Size(min = 1, max = 500)
    private String subject;

    private String content;

    /** 收件人邮箱列表（TO） */
    private List<String> to;

    /** 抄送邮箱列表（CC） */
    private List<String> cc;

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getTo() {
        return to;
    }

    public void setTo(List<String> to) {
        this.to = to;
    }

    public List<String> getCc() {
        return cc;
    }

    public void setCc(List<String> cc) {
        this.cc = cc;
    }
}
