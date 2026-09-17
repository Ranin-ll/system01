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

    /** 实操题数量（已废弃，理论/实操拆分后不再使用） */
    private Integer subjectCount;

    /** 实操题题干（实操考核用） */
    private String subjectContent;

    /** 实操题参考附件路径（实操考核用，可选） */
    private String subjectAttachment;

    /** 适用岗位ID（可选） */
    private Long positionId;

    /** 发布部门ID（关联sys_dept） */
    private Long deptId;

    /** 状态：DRAFT待发布 / PUBLISHED已发布 / GRADING待批改 / DISABLED已停用 */
    private String status;

    private Date startTime;
    private Date endTime;

    /** 作答时长（分钟） */
    private Integer duration;

    /** 通过线 */
    private BigDecimal passLine;

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

    /** 待批改答卷数（展示字段） */
    @TableField(exist = false)
    private Integer pendingCount;

    /** 已提交答卷数（展示字段） */
    @TableField(exist = false)
    private Integer answeredCount;

    /** 当前登录账号数据范围 */
    @TableField(exist = false)
    private Long scopeDeptId;

    /**
     * 知识分布（配置页提交 / 回显用，落 exam_knowledge_rule）
     * 驱动抽题：每个知识点在其题库范围内随机抽题。
     */
    @TableField(exist = false)
    private java.util.List<ExamKnowledgeRule> knowledgeRules;

    /** 指定人员（正式考核；为空 = 本部门全体在培实习生） */
    @TableField(exist = false)
    private java.util.List<Long> participantIds;

    /** 人员范围：ALL 全部在培实习生 / ASSIGNED 指定人员 */
    @TableField(exist = false)
    private String assignMode;
}
