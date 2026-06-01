package com.osschoolwork.backend.service;

import org.springframework.core.io.Resource;

import com.osschoolwork.backend.entity.Attachment;

public class AttachmentDownload {

    private final Attachment attachment;
    private final Resource resource;
    private final String contentType;

    public AttachmentDownload(Attachment attachment, Resource resource, String contentType) {
        this.attachment = attachment;
        this.resource = resource;
        this.contentType = contentType;
    }

    public Attachment getAttachment() {
        return attachment;
    }

    public Resource getResource() {
        return resource;
    }

    public String getContentType() {
        return contentType;
    }
}
