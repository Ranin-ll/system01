package com.ruoyi.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.business.domain.NoticeTarget;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知定向接收人 Mapper（仅 scope_type='USER' 时写入）
 *
 * @author ruoyi
 */
@Mapper
public interface NoticeTargetMapper extends BaseMapper<NoticeTarget> {
}
