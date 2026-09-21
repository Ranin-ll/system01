package com.ruoyi.business.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 督办待办项 —— 聚合结果，不映射单张表
 *
 * <p>四类待办统一成同一形状，前端只按 {@code todoType} 分组渲染，不再自己判来源：</p>
 * <ul>
 *   <li>{@link #TYPE_REGISTER} 待审报名 —— {@code register_application.status = 'WAIT_AUDIT'}</li>
 *   <li>{@link #TYPE_REVIEW} 待批作业 —— 每人<b>最新一版</b> {@code task_submission.review_status = 'PENDING'}</li>
 *   <li>{@link #TYPE_OVERDUE_TASK} 逾期任务 —— {@code task_assignment.status = 'OVERDUE'}</li>
 *   <li>{@link #TYPE_STALLED_STUDY} 学习停滞 —— 未完成且超 N 天无 {@code last_study_time}</li>
 * </ul>
 *
 * @author ruoyi
 */
@Data
public class SuperTodoItem implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TYPE_REGISTER = "REGISTER";
    public static final String TYPE_REVIEW = "REVIEW";
    public static final String TYPE_OVERDUE_TASK = "OVERDUE_TASK";
    public static final String TYPE_STALLED_STUDY = "STALLED_STUDY";

    /** 中文标签（由后端给出，避免前端维护第二份映射） */
    public static String label(String type) {
        if (TYPE_REGISTER.equals(type)) return "待审报名";
        if (TYPE_REVIEW.equals(type)) return "待批作业";
        if (TYPE_OVERDUE_TASK.equals(type)) return "逾期任务";
        if (TYPE_STALLED_STUDY.equals(type)) return "学习停滞";
        return type;
    }

    /** 待办类型 */
    private String todoType;
    /** 类型中文名 */
    private String todoLabel;

    /** 业务主键（报名 id / assignment id / study_record id） */
    private Long bizId;
    /** 关联任务 id（逾期、待批作业有） */
    private Long taskId;
    /** 标题 */
    private String title;
    /** 详情描述 */
    private String detail;

    private Long deptId;
    private String deptName;

    /** 责任人（部门管理员）—— 催办就发给这个人 */
    private Long ownerId;
    private String ownerName;

    /** 涉及的人（实习生 / 报名者） */
    private Long targetId;
    private String targetName;

    /** 截止时间（逾期、待批作业有） */
    private Date deadline;
    /** 已逾期小时数（>0 为已逾期；无截止时间的为 null） */
    private Integer overdueHours;
    /** 停滞天数（仅学习停滞有） */
    private Integer idleDays;
    /** 发生时间（报名提交时间 / 提交作业时间 / 最近学习时间） */
    private Date happenTime;
}
