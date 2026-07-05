package com.osschoolwork.backend.service;

import com.osschoolwork.backend.entity.Attachment;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

public interface AttachmentService {

    /** 上传附件并绑定到指定邮件，返回附件元信息 */
    Attachment upload(Long userId, Long mailId, MultipartFile file);

    /** 获取附件实体（含磁盘路径），用于下载 */
    Attachment getForDownload(Long userId, Long attachmentId);

    /** 删除单个附件（仅邮件发送者可操作） */
    void deleteAttachment(Long userId, Long attachmentId);

    /** 列出某邮件的所有附件 */
    List<Attachment> listByMailId(Long mailId);

    /** 按附件 ID 获取对应的文件输入流 */
    InputStream getFileStream(Attachment attachment);
}
