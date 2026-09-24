package com.ruoyi.business.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 培养状态进度 —— 逐人一行（超管「培养分析看板」的漏斗 / 卡点 / 个人时间轴三处共用同一份数据）。
 *
 * <p><b>为什么逐人返回而不是后端聚合好漏斗</b>：漏斗（各阶段人数）、卡点清单、下钻名单
 * 三者本质是同一份数据的三种投影。后端只出「逐人明细」，
 * 前端按需聚合 —— 这样三个视图**不可能出现数字对不上**。</p>
 *
 * <p><b>阶段判定口径（★ 以角色为主、user_status 为辅）</b>：实测库里存在
 * 「已持 PRE_TRAINEE 角色、但 user_status 仍是 WAIT_AUDIT」的行（角色已发、审核未闭环）。
 * 若直接按 user_status 分档，这 6 个人会被归进「待审」而与「预备」重复计数。
 * 因此这里按 user_status 分档的同时，用 {@code statusConflict} 单独标记出来，
 * 由前端显式呈现为「状态待修正」卡点，**不悄悄归类**。</p>
 *
 * @author ruoyi
 */
@Data
public class InternStageRow implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String userName;
    private String nickName;
    private Long deptId;
    private String deptName;
    /** 岗位名（dept_position / sys_user.position_id → position.position_name） */
    private String positionName;
    /** 手机号（L3 身份条用；列表页不展示） */
    private String phonenumber;

    /** 原始 user_status（兜底显示用） */
    private String userStatus;
    /** 归一后的阶段：WAIT_AUDIT / PRE_TRAINEE / PENDING_PROMOTE / FORMAL_TRAINEE */
    private String stage;
    /** 阶段中文名（Service 里补，避免前端再维护一份映射） */
    private String stageLabel;
    /** 1 = 已持实习生角色但 user_status 仍为 WAIT_AUDIT（角色与状态不一致，需修正） */
    private Integer statusConflict;

    /** 导师（sys_user.mentor_name，**文本字段非关联 ID**，可能为空或脏值） */
    private String mentorName;
    /** 导师联系方式（与 mentorName 配对展示；2026-09-23 加） */
    private String mentorPhone;
    /** 1 = 协议已签 */
    private Integer protocolSigned;

    /** 自建档起的天数（近似「在培天数」；库里没有阶段流转日志，故只能给近似值） */
    private Integer stayDays;

    // ==================== 花名册（部门端「实习生管理」页）展示所需 ====================
    // 2026-09-23 加：该页的花名册改为直接吃本结构（不再走 register_application），
    // 所以把该页要显示的字段一并在这里带出，避免再发一次请求、也避免两处口径漂移。

    /** 预计入职日期（花名册展示 + 「入职时间」筛选） */
    private Date expectedEntryDate;
    /** 建档时间（最近活跃的兜底值） */
    private Date createTime;
    /** 最近登录时间 —— 这才是真正的「最近活跃」；为空时前端回落到 createTime */
    private Date loginDate;

    // ==================== 转正 gate 的「真数据」输入项 ====================

    /** 本人学习记录条数 */
    private Integer learnTotal;
    /** 本人达标条数（progress >= 门槛） */
    private Integer learnDone;
    /** 本人学习进度均值（无记录 → null，**不伪装 0%**） */
    private java.math.BigDecimal learnAvgProgress;
    /** 本人应交任务数 */
    private Integer taskTotal;
    /** 本人逾期任务数 */
    private Integer taskOverdue;

    // ==================== 模拟考核（2026-09-23 加，花名册「模拟正确率」列） ====================

    /** 本人模拟练习场次（无记录 = 0） */
    private Integer practiceTimes;
    /**
     * 本人模拟考核正确率（%）= 对题总数 ÷ 总题数 × 100。
     * ★ 无记录 → {@code null}，**不伪装成 0%**（项目铁律：分母 0 不得显示 0%）。
     */
    private java.math.BigDecimal practiceAccuracy;

    /**
     * 正式考试是否通过（1 通过 / 0 未通过；**无正式答卷时保持 null**，
     * 前端显示「未参加」，不伪装成「未通过」）。
     */
    private Integer formalPassed;

    /**
     * 正式考核<b>参加次数</b>（= 该人已产生的正式答卷数，口径与 {@link #formalPassed} 同一份查询）。
     *
     * <p>2026-09-23 加：部门端「实习生管理」花名册的「正式考核」列改为展示参加次数
     * （原来是「已通过 / 未通过」）。无答卷 → {@code null}，前端显示「未参加」。</p>
     */
    private Integer formalTimes;
}
