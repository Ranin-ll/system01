package com.ruoyi.business.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * 部门管理员及其待办量 —— 超管「督办看板」与「部门管理员」页共用
 *
 * <p>待办量由 {@code SuperTodoServiceImpl} 用四类待办明细在内存聚合（数据量小、口径唯一），
 * 不写在 SQL 里，避免把同一套判定复制四遍。</p>
 *
 * @author ruoyi
 */
@Data
public class SuperDeptAdminStat implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String userName;
    private String nickName;
    private Long deptId;
    private String deptName;

    /** 若依账号状态：0 正常 / 1 停用 */
    private String status;
    /** 联系电话（取自 phonenumber，可能为空） */
    private String phone;

    /** 待审报名 */
    private Integer pendingRegisters;
    /** 待批作业 */
    private Integer pendingReviews;
    /** 逾期任务 */
    private Integer overdueTasks;
    /** 学习停滞 */
    private Integer stalledStudies;
    /** 四项合计 */
    private Integer totalTodos;
}
