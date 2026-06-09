package com.osschoolwork.backend.dto;

import java.util.List;

/**
 * 草稿保存/更新请求 —— 与 SendMailRequest 字段相同，但 subject 非必填
 */
public class DraftRequest {

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
