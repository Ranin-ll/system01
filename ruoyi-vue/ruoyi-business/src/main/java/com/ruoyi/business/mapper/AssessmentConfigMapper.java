package com.ruoyi.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.business.domain.AssessmentConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 考核与运营规则默认值 Mapper（单行配置，id 固定 1）
 *
 * @author ruoyi
 */
@Mapper
public interface AssessmentConfigMapper extends BaseMapper<AssessmentConfig> {
}
