package com.ruoyi.business.domain;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 理论考试知识分布 exam_knowledge_rule
 *
 * 驱动抽题：该知识点的题目从题库 question.knowledge_point 匹配后随机抽取。
 * 模拟考核与正式理论考核共用本表。
 */
@Data
public class ExamKnowledgeRule implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 考核ID */
    private Long examId;

    /** 题库知识点 */
    private String knowledgePoint;

    /** 本知识点抽题数量 */
    private Integer questionCount;

    /** 占比%（展示与校验用） */
    private BigDecimal ratio;

    /** 单选题占比% */
    private BigDecimal singleRatio;

    private Integer sortNo;

    private Date createTime;

    /** 题库该知识点的可用题量（查询填充，非表字段） */
    private Integer availableCount;

    /** 题库该知识点的可用单选题量（查询填充，非表字段） */
    private Integer availableSingle;
}
