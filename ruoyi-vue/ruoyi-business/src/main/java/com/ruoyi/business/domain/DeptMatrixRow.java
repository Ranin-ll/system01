package com.ruoyi.business.domain;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 部门对比矩阵的一行 —— 超管「培养分析看板」L0 部门 × 各维度。
 *
 * <p><b>本 VO 只承载「真数据」字段</b>：人 / 学习，以及由这两者派生的比率。
 * 模拟考核、正式考核、知识点三族字段见下方对应字段。</p>
 *
 * @author ruoyi
 */
@Data
public class DeptMatrixRow implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 部门 */
    private Long deptId;
    private String deptName;

    // ==================== 维度 A · 规模与结构（真数据） ====================

    /** 在培实习生（持 PRE_TRAINEE / FORMAL_TRAINEE 角色且账号有效） */
    private Integer internCount;
    /** 其中已转正（FORMAL_TRAINEE） */
    private Integer formalCount;

    // ==================== 维度 B · 学习（真数据） ====================

    /** 学习记录条数 */
    private Integer learnTotal;
    /** 其中达标条数（progress >= assessment_config.course_done_threshold） */
    private Integer learnDone;
    /** 有学习记录的人数（用于给「部门均值」标注 N，避免把小样本当部门水平） */
    private Integer learnPersons;
    /** 平均学习进度(%) */
    private BigDecimal learnAvgProgress;
    /** 视频累计时长（秒） */
    private Long videoSeconds;

    // ==================== 维度 C/D/F · 考核与知识点 ====================

    /** 模拟考核人次（🟠 待分支合并后补查询） */
    private Integer practiceCount;
    /** 模拟考核平均分（🟠） */
    private BigDecimal practiceAvg;
    /** 正式考核已发布答卷数（🟠） */
    private Integer formalPublished;
    /** 知识点题次（🟠） */
    private Integer knowledgeItems;
    /** 知识点答对题次（🟠） */
    private Integer knowledgeCorrect;
}
