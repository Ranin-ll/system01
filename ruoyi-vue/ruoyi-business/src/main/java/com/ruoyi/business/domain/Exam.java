package com.ruoyi.business.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 考核对象 exam
 */
@Data
@TableName("exam")
public class Exam implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 考核ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 考核名称 */
    private String examName;

    /** 类型：THEORY理论 / PRACTICAL实操 */
    private String examType;

    /** 考核性质：FORMAL正式 / PRACTICE模拟（练习） */
    private String examMode;

    /** 所属题库ID（理论考核用，关联question_bank） */
    private Long bankId;

    /** 客观题抽题数量（理论考核用，已由分题型数量替代） */
    private Integer questionCount;

    /** 单选题数量 */
    private Integer singleCount;

    /** 多选题数量 */
    private Integer multiCount;

    /** 判断题数量 */
    private Integer judgeCount;

    /** 单选题每题分值 */
    private java.math.BigDecimal singleScore;

    /** 多选题每题分值 */
    private java.math.BigDecimal multiScore;

    /** 判断题每题分值 */
    private java.math.BigDecimal judgeScore;

    /** 实操题数量（= 实操题目清单条数，发布时汇总回写，列表展示用） */
    private Integer subjectCount;

    /** 实操题题干（历史字段：实操改为「题目清单」后不再使用，保留兼容） */
    private String subjectContent;

    /** 实操题参考附件路径（历史字段，保留兼容） */
    private String subjectAttachment;

    /** 适用岗位ID（可选） */
    private Long positionId;

    /** 发布部门ID（关联sys_dept） */
    private Long deptId;

    /**
     * 所属模块ID（practice_module.id）
     *
     * 仅「模拟考核」使用：模块是模拟考核的顶层分组，一个模块下可以有多个理论模拟考核
     * 与多个实操模拟题；正式考核（exam_mode='FORMAL'）此字段为 NULL。
     */
    private Long moduleId;

    /** 状态：DRAFT待发布 / PUBLISHED已发布 / GRADING待批改 / DISABLED已停用 */
    private String status;

    private Date startTime;
    private Date endTime;

    /** 作答时长（分钟） */
    private Integer duration;

    /** 通过线 */
    private BigDecimal passLine;

    /** 难易程度：EASY 简单 / MEDIUM 中等 / HARD 困难（空=未设置） */
    private String difficulty;

    /** 题目内容偏向（给实习生看的侧重备注，如「偏 Java 集合与并发」） */
    private String contentBias;

    private Date publishedAt;
    private Long publisherId;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    private Integer deleted;

    /** 题库名称（展示字段） */
    @TableField(exist = false)
    private String bankName;

    /** 部门名称（展示字段） */
    @TableField(exist = false)
    private String deptName;

    /** 模块名称（展示字段，模拟考核用） */
    @TableField(exist = false)
    private String moduleName;

    /** 待批改答卷数（展示字段） */
    @TableField(exist = false)
    private Integer pendingCount;

    /** 已提交答卷数（展示字段） */
    @TableField(exist = false)
    private Integer answeredCount;

    /** 实操卷面满分（展示字段：各题满分之和） */
    @TableField(exist = false)
    private BigDecimal subjectTotalScore;

    /** 当前登录账号数据范围 */
    @TableField(exist = false)
    private Long scopeDeptId;

    /**
     * 多题库组卷配置（当前主用，落 exam_bank_rule）
     * 语义：按题库分配抽题量；每个题库分别配置 单选/多选/判断 的抽题数量。
     * 注：题库 = 一个部门的一门科目（**不是"知识模块"**，一个题库内含多个知识点）。
     * 配置非空时优先按它抽题；为空时回退到 bankId 单库 + knowledgeRules 老逻辑。
     */
    @TableField(exist = false)
    private java.util.List<ExamBankRule> bankRules;

    /**
     * 知识分布（历史兼容，落 exam_knowledge_rule）
     * 仅在未配置 bankRules 时作为单库内「按知识点分配题量」的老链路生效。
     */
    @TableField(exist = false)
    private java.util.List<ExamKnowledgeRule> knowledgeRules;

    /** 指定人员（正式考核；为空 = 本部门全体在培实习生） */
    @TableField(exist = false)
    private java.util.List<Long> participantIds;

    /** 人员范围：ALL 全部在培实习生 / ASSIGNED 指定人员 */
    @TableField(exist = false)
    private String assignMode;

    /**
     * 实操题目清单（实操考核专用，落 exam_subject_item）
     *
     * 实操不抽题：题目由管理员逐条填写「题干 / 描述 / 参考图 / 附件 / 本题满分」，
     * 发布后冻结；实习生端逐题上传作答文件，管理员逐题打分求和。
     */
    @TableField(exist = false)
    private java.util.List<ExamSubjectItem> subjectItems;
}
