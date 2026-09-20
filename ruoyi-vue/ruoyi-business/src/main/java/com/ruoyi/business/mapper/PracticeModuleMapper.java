package com.ruoyi.business.mapper;

import com.ruoyi.business.domain.PracticeModule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 模拟考核模块 Mapper
 *
 * 约定：business 模块写操作一律手写 XML，不使用 BaseMapper。
 */
@Mapper
public interface PracticeModuleMapper {

    /** 管理端列表（按 scopeDeptId 限定数据范围，scopeDeptId 为 null 表示全部），附带模块内容数量 */
    List<PracticeModule> selectModuleList(PracticeModule query);

    /** 按ID查询（带部门名称与内容数量） */
    PracticeModule selectModuleById(@Param("id") Long id);

    /** 按名称查重（同部门内模块名唯一），excludeId 用于编辑时排除自身 */
    PracticeModule selectByName(@Param("deptId") Long deptId,
                                @Param("name") String name,
                                @Param("excludeId") Long excludeId);

    /** 某部门的全部模块（下拉选项用），enabledOnly=true 只取启用的 */
    List<PracticeModule> selectByDept(@Param("deptId") Long deptId,
                                      @Param("enabledOnly") boolean enabledOnly);

    /** 新增 */
    int insertModule(PracticeModule module);

    /** 修改（模块名 / 说明 / 排序 / 状态） */
    int updateModule(PracticeModule module);

    /** 逻辑删除 */
    int deleteModuleById(@Param("id") Long id);

    /** 模块下实操题数量（未删除） */
    int countSubjects(@Param("moduleId") Long moduleId);

    /** 模块下理论模拟考核数量（未删除） */
    int countExams(@Param("moduleId") Long moduleId);
}
