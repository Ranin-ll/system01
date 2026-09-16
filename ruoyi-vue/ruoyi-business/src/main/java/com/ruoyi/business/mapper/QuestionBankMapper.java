package com.ruoyi.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.business.domain.QuestionBank;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 题库Mapper
 */
@Mapper
public interface QuestionBankMapper extends BaseMapper<QuestionBank> {

    /** 查询题库列表（按部门数据范围过滤） */
    List<QuestionBank> selectBankList(QuestionBank bank);

    /** 按数据范围查询单个题库。scopeDeptId 为空表示全局范围。 */
    QuestionBank selectBankById(@Param("id") Long id, @Param("scopeDeptId") Long scopeDeptId);

    /** 新增题库 */
    int insertBank(QuestionBank bank);

    /** 修改题库 */
    int updateBank(QuestionBank bank);

    /** 逻辑删除题库 */
    int deleteBankById(@Param("id") Long id);

    /** 查询部门的模拟题库（bank_type=PRACTICE）。 */
    QuestionBank selectPracticeBankByDept(@Param("deptId") Long deptId);
}
