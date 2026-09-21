package com.ruoyi.business.domain;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * L1 部门详情 —— 超管「培养分析看板」下钻的第一层。
 *
 * <p><b>「本部门 vs 全局」怎么给</b>：{@code globalLearnAvgProgress} / {@code globalTaskDoneRate}
 * 由 Service 从 L0 的同一份矩阵里取（超管档 = 全公司；非超管档 = 其可见范围），
 * **不是另算一遍** —— 否则本页的「全局均值」会与 L0 看板漂移。</p>
 *
 * <p>考核类字段（模拟/正式/知识点）与前几层同样**留空**，由前端示例填充并打橙标：
 * 题库与考核模块待同事分支合并，详见
 * {@link com.ruoyi.business.service.impl.SuperAnalysisServiceImpl} 类注释。</p>
 *
 * @author ruoyi
 */
@Data
public class DeptStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ==================== 部门身份 ====================

    private Long deptId;
    private String deptName;
    /** 该部门绑定的岗位（dept_position，1:1；可能为空） */
    private Long positionId;
    private String positionName;

    // ==================== 本部门 KPI（真数据） ====================

    private Integer internCount;
    private Integer formalCount;
    /** 学习达标率(%)，分母 0 → null */
    private Integer learnRate;
    private Integer learnTotal;
    private Integer learnDone;
    /** 有学习记录的人数（用于标注 N） */
    private Integer learnPersons;
    private BigDecimal learnAvgProgress;
    private Integer taskTotal;
    private Integer taskDone;
    private Integer taskOverdue;
    private Integer taskDoneRate;

    // ==================== 全局对照（取自 L0 同一份矩阵） ====================

    private BigDecimal globalLearnAvgProgress;
    private Integer globalTaskDoneRate;
    private Integer globalInternCount;
    /** 全公司学习达标门槛（assessment_config） */
    private BigDecimal learnThreshold;

    // ==================== 明细块 ====================

    /** 岗位分布：[{positionId, positionName, cnt}] */
    private List<Map<String, Object>> positions;

    /** 该部门岗位下的课程：[{courseId, courseName, status, learnedCount, recordCount, doneCount, avgProgress}] */
    private List<Map<String, Object>> courses;

    /** 本部门在培实习生（含阶段、卡点、gate 输入项）—— 下钻到个人的入口 */
    private List<InternStageRow> interns;

    /** 本部门卡点计数：角色状态不一致 / 未分导师 / 未签协议 / 无学习记录 / 有逾期任务 */
    private Map<String, Object> blockers;
}
