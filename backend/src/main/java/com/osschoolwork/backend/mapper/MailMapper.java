package com.osschoolwork.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.osschoolwork.backend.dto.MailView;
import com.osschoolwork.backend.entity.Mail;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 邮件 Mapper —— 提供后端 Service 层所需的所有邮件查询
 */
public interface MailMapper extends BaseMapper<Mail> {

    /**
     * 收件箱：当前用户作为收件人（TO/CC）且未被删除的邮件列表
     */
    @Select("SELECT m.id, m.sender_id, u.username AS sender_name, u.email AS sender_email, "
            + "m.subject, LEFT(m.content, 100) AS content_preview, "
            + "r.is_read, r.receiver_type, m.send_time, "
            + "IF((SELECT COUNT(*) FROM attachment a WHERE a.mail_id = m.id) > 0, TRUE, FALSE) AS has_attachment "
            + "FROM mail m "
            + "JOIN receiver r ON r.mail_id = m.id "
            + "JOIN user u ON u.id = m.sender_id "
            + "WHERE r.receiver_id = #{userId} "
            + "AND r.deleted = 0 "
            + "AND r.folder = 'INBOX' "
            + "ORDER BY m.send_time DESC")
    List<MailView> selectInbox(@Param("userId") Long userId);

    /**
     * 发件箱：当前用户发送的邮件列表（仅已发送，不含草稿）
     */
    @Select("SELECT m.id, m.sender_id, u.username AS sender_name, u.email AS sender_email, "
            + "m.subject, LEFT(m.content, 100) AS content_preview, "
            + "0 AS is_read, 'SENT' AS receiver_type, m.send_time, "
            + "IF((SELECT COUNT(*) FROM attachment a WHERE a.mail_id = m.id) > 0, TRUE, FALSE) AS has_attachment "
            + "FROM mail m "
            + "JOIN user u ON u.id = m.sender_id "
            + "WHERE m.sender_id = #{userId} "
            + "AND m.status = 'SENT' "
            + "ORDER BY m.send_time DESC")
    List<MailView> selectSent(@Param("userId") Long userId);

    /**
     * 邮件搜索：按关键词匹配主题或正文（仅搜索当前用户收件箱）
     */
    @Select("SELECT m.id, m.sender_id, u.username AS sender_name, u.email AS sender_email, "
            + "m.subject, LEFT(m.content, 100) AS content_preview, "
            + "r.is_read, r.receiver_type, m.send_time, "
            + "IF((SELECT COUNT(*) FROM attachment a WHERE a.mail_id = m.id) > 0, TRUE, FALSE) AS has_attachment "
            + "FROM mail m "
            + "JOIN receiver r ON r.mail_id = m.id "
            + "JOIN user u ON u.id = m.sender_id "
            + "WHERE r.receiver_id = #{userId} "
            + "AND r.deleted = 0 "
            + "AND r.folder = 'INBOX' "
            + "AND (m.subject LIKE CONCAT('%', #{keyword}, '%') "
            + "  OR m.content LIKE CONCAT('%', #{keyword}, '%') "
            + "  OR u.username LIKE CONCAT('%', #{keyword}, '%')) "
            + "ORDER BY m.send_time DESC")
    List<MailView> searchMails(@Param("userId") Long userId, @Param("keyword") String keyword);
}
