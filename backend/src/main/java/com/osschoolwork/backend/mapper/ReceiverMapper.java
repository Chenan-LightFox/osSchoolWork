package com.osschoolwork.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.osschoolwork.backend.entity.Receiver;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 收件关系 Mapper —— 提供邮件状态变更操作
 */
public interface ReceiverMapper extends BaseMapper<Receiver> {

    /**
     * 标记已读：将指定用户对指定邮件的 is_read 设为 1
     */
    @Update("UPDATE receiver SET is_read = 1 WHERE mail_id = #{mailId} AND receiver_id = #{userId}")
    int markAsRead(@Param("mailId") Long mailId, @Param("userId") Long userId);

    /**
     * 软删除：将邮件移入垃圾箱（deleted=1, folder='TRASH'）
     */
    @Update("UPDATE receiver SET deleted = 1, folder = 'TRASH' "
            + "WHERE mail_id = #{mailId} AND receiver_id = #{userId}")
    int softDelete(@Param("mailId") Long mailId, @Param("userId") Long userId);

    /**
     * 恢复：将邮件从垃圾箱移回收件箱（deleted=0, folder='INBOX'）
     */
    @Update("UPDATE receiver SET deleted = 0, folder = 'INBOX' "
            + "WHERE mail_id = #{mailId} AND receiver_id = #{userId}")
    int restoreMail(@Param("mailId") Long mailId, @Param("userId") Long userId);

    /**
     * 删除某邮件的所有收件关系（用于草稿更新时重建）
     */
    @org.apache.ibatis.annotations.Delete("DELETE FROM receiver WHERE mail_id = #{mailId}")
    int deleteByMailId(@Param("mailId") Long mailId);

    /**
     * 硬删除：彻底删除用户在垃圾箱中的收件关系
     */
    @org.apache.ibatis.annotations.Delete("DELETE FROM receiver "
            + "WHERE mail_id = #{mailId} AND receiver_id = #{userId} "
            + "AND deleted = 1 AND folder = 'TRASH'")
    int hardDelete(@Param("mailId") Long mailId, @Param("userId") Long userId);

    /**
     * 统计某邮件的剩余收件关系数
     */
    @org.apache.ibatis.annotations.Select("SELECT COUNT(*) FROM receiver WHERE mail_id = #{mailId}")
    int countByMailId(@Param("mailId") Long mailId);
}
