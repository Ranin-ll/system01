package com.ruoyi.business.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 已读回执 Mapper
 *
 * <p>唯一键 <code>uk_notice_user(notice_id, user_id)</code> 就是幂等保证：
 * 重复打开同一条详情不会写两行，已读时间也保持不变（<code>read_time = read_time</code> 是空更新）。</p>
 *
 * @author ruoyi
 */
@Mapper
public interface NoticeReadMapper {

    /**
     * 标记已读（幂等）
     */
    @Insert("INSERT INTO notice_read (notice_id, user_id, read_time) VALUES (#{noticeId}, #{userId}, NOW()) "
            + "ON DUPLICATE KEY UPDATE read_time = read_time")
    int markRead(@Param("noticeId") Long noticeId, @Param("userId") Long userId);
}
