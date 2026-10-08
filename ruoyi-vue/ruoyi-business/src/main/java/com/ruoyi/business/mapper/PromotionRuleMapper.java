package com.ruoyi.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.business.domain.PromotionRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 转正要求规则Mapper
 *
 * @author ruoyi
 */
@Mapper
public interface PromotionRuleMapper extends BaseMapper<PromotionRule> {

    /**
     * 全部规则（部门覆盖 + 全局默认），带部门名，按「全局在前、部门升序」排列。
     * 供超管「系统规则」页与部门端设置行展示。
     */
    List<PromotionRule> selectAllRules();

    /** 取某部门的覆盖规则（dept_id 精确匹配，不含全局）。无则返回 null。 */
    PromotionRule selectByDeptId(@Param("deptId") Long deptId);

    /** 取全局默认规则（dept_id IS NULL）。无则返回 null。 */
    PromotionRule selectGlobalRule();

    int insertRule(PromotionRule rule);

    int updateRule(PromotionRule rule);
}
