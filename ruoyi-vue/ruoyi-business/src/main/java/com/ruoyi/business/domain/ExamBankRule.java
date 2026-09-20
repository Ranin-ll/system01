package com.ruoyi.business.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 考核多题库组卷配置 exam_bank_rule
 *
 * 语义：**题库即知识模块** —— 一道题录入时归属唯一题库，题库名就是知识模块名。
 * 组卷时勾选多个题库，并为每个题库分别指定 单选 / 多选 / 判断 的抽题数量，
 * 抽题时在各自题库内随机取题。
 *
 * 模拟考核与正式理论考核共用本表。
 */
@Data
public class ExamBankRule implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 考核ID */
    private Long examId;

    /** 题库ID（question_bank.id），业务语义 = 知识模块 */
    private Long bankId;

    /** 本库抽单选题数量 */
    private Integer singleCount;

    /** 本库抽多选题数量 */
    private Integer multiCount;

    /** 本库抽判断题数量 */
    private Integer judgeCount;

    /** 排序（卷面按库顺序组卷） */
    private Integer sortNo;

    private Date createTime;

    // ---------- 以下为非表字段：查询回显 / 前端展示用 ----------

    /** 题库名称（题库即知识模块名） */
    private String bankName;

    /** 本库该题型可用题量（配置校验：抽题量不得超出） */
    private Integer availableSingle;

    private Integer availableMulti;

    private Integer availableJudge;

    /** 本库可用题量合计 */
    private Integer availableTotal;

    /** 本库抽题量小计（单选+多选+判断） */
    public int getTotalCount() {
        return nz(singleCount) + nz(multiCount) + nz(judgeCount);
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }
}
