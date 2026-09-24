package com.ruoyi.business.mapper;

import com.ruoyi.business.domain.PracticeSubject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 模拟考核实操题 Mapper
 *
 * 约定：business 模块写操作一律手写 XML，不使用 BaseMapper。
 */
@Mapper
public interface PracticeSubjectMapper {

    /** 管理员/超管列表（按 scopeDeptId 限定数据范围，scopeDeptId 为 null 表示全部） */
    List<PracticeSubject> selectSubjectList(PracticeSubject query);

    /** 按ID查询（带部门名称） */
    PracticeSubject selectSubjectById(@Param("id") Long id);

    /** 实习生可见列表：本部门 + 已启用（★ 2026-09-23 起不分模块，按 sort_no 排序） */
    List<PracticeSubject> selectPublishedByDept(@Param("deptId") Long deptId);

    /** 新增 */
    int insertSubject(PracticeSubject subject);

    /** 修改（只更新题干、附件、状态） */
    int updateSubject(PracticeSubject subject);

    /** 逻辑删除 */
    int deleteSubjectById(@Param("id") Long id);
}
