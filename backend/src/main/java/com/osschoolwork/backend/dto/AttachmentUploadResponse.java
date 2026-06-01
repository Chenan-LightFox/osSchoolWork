package com.osschoolwork.backend.dto;

import java.time.LocalDateTime;

public class AttachmentUploadResponse {

    private Long id;
    private String fileName;
    private Long fileSize;
    private LocalDateTime uploadTime;

    public AttachmentUploadResponse() {
    }

    public AttachmentUploadResponse(Long id, String fileName, Long fileSize, LocalDateTime uploadTime) {
        this.id = id;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.uploadTime = uploadTime;
    }

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
