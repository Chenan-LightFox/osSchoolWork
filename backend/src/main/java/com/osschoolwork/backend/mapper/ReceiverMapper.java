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
}
