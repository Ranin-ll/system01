package com.ruoyi.business.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 模拟考核逐题明细 practice_record_item
 *
 * 交卷时按题落库，含题干/选项/解析「快照」，保证题目后续被修改或删除后，
 * 历史记录仍可完整回看。
 */
@Data
public class PracticeRecordItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 模拟记录ID（practice_record.id） */
    private Long recordId;

    /** 题目ID */
    private Long questionId;

    /** 题序（从1开始） */
    private Integer sortNo;

    /** 题型：SINGLE单选 / MULTI多选 / JUDGE判断 */
    private String qtype;

    /** 题干快照 */
    private String stem;

    /** 选项快照JSON */
    private String optionsJson;

    /** 学员作答 */
    private String userAnswer;

    /** 正确答案 */
    private String correctAnswer;

    /** 是否正确：1是 0否 */
    private Integer isCorrect;

    /** 答案解析快照 */
    private String analysis;

    private Date createTime;
}
