package com.ruoyi.business.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 部门在培实习生查询（含待转正）。
 *
 * <p>供「通知管理」与 TraineeSelect 公共选择器共用。</p>
 */
@Mapper
public interface DeptTraineeMapper {

    /** 本部门在培实习生（含待转正）。deptId 传 null 表示不限（超管）。 */
    List<Map<String, Object>> selectDeptTrainees(@Param("deptId") Long deptId);
}