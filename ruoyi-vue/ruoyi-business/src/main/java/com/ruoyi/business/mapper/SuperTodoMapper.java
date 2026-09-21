package com.ruoyi.business.mapper;

import com.ruoyi.business.domain.SuperDeptAdminStat;
import com.ruoyi.business.domain.SuperTodoItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 超管督办视图 Mapper（只读聚合 + 催办频控查询）
 *
 * <p>四源待办的判定<b>只在这里写一次</b>；Service 负责把行归到「部门管理员」名下。
 * 所有查询都不带部门过滤 —— 调用方（Service）已强制校验超管身份。</p>
 *
 * @author ruoyi
 */
@Mapper
public interface SuperTodoMapper {

    /** 部门管理员清单（按角色 DEPT_ADMIN 判定，不用 is_dept_admin 单一字段） */
    List<SuperDeptAdminStat> selectDeptAdmins();

    /** 源①：待审报名 */
    List<SuperTodoItem> selectPendingRegisters();

    /** 源②：待批作业（每人<b>最新一版</b> review_status = PENDING） */
    List<SuperTodoItem> selectPendingReviews();

    /** 源③：逾期任务 */
    List<SuperTodoItem> selectOverdueTasks();

    /** 源④：学习停滞（未完成且超 idleDays 天无进展） */
    List<SuperTodoItem> selectStalledStudies(@Param("idleDays") int idleDays);

    /** 当天是否已给该人发过催办（频控：同一天不重复催同一人） */
    int countUrgeToday(@Param("userId") Long userId);
}
