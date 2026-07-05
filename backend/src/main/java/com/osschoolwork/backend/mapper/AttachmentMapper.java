package com.osschoolwork.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.osschoolwork.backend.entity.Attachment;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 附件 Mapper —— 提供附件元信息查询
 */
public interface AttachmentMapper extends BaseMapper<Attachment> {

    /**
     * 按邮件 ID 查询所有附件
     */
    @Select("SELECT * FROM attachment WHERE mail_id = #{mailId}")
    List<Attachment> selectByMailId(@Param("mailId") Long mailId);
}
