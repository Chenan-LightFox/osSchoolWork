package com.osschoolwork.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 邮件详情视图（含收件人、抄送、附件信息）
 */
public class MailDetailView {

    private Long id;
    private Long senderId;
    private String senderName;
    private String senderEmail;
    private String subject;
    private String content;
    private LocalDateTime sendTime;
    private List<ReceiverView> receivers;     // 收件人 + 抄送
    private List<AttachmentView> attachments; // 附件列表

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

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

    public LocalDateTime getSendTime() {
        return sendTime;
    }

    public void setSendTime(LocalDateTime sendTime) {
        this.sendTime = sendTime;
    }

    public List<ReceiverView> getReceivers() {
        return receivers;
    }

    public void setReceivers(List<ReceiverView> receivers) {
        this.receivers = receivers;
    }

    public List<AttachmentView> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<AttachmentView> attachments) {
        this.attachments = attachments;
    }

    /**
     * 收件关系视图（嵌套）
     */
    public static class ReceiverView {
        private Long userId;
        private String username;
        private String email;
        private String type;  // TO / CC

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    /**
     * 附件视图（嵌套）
     */
    public static class AttachmentView {
        private Long id;
        private String fileName;
        private Long fileSize;
        private LocalDateTime uploadTime;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public Long getFileSize() {
            return fileSize;
        }

        public void setFileSize(Long fileSize) {
            this.fileSize = fileSize;
        }

        public LocalDateTime getUploadTime() {
            return uploadTime;
        }

        public void setUploadTime(LocalDateTime uploadTime) {
            this.uploadTime = uploadTime;
        }
    }
}
