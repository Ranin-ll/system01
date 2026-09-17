package com.ruoyi.business.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 逐题作答与评分明细 answer_sheet_item
 */
@Data
@TableName("answer_sheet_item")
public class AnswerSheetItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long answerSheetId;
    private Long questionId;
    private Integer seq;
    private String qtype;
    private Integer isSubjective;
    private String userAnswer;
    private Integer isCorrect;
    private BigDecimal aiScore;
    private BigDecimal manualScore;
    private String manualComment;
    private Long manualUserId;
    private Date manualTime;

    /** 题干（展示字段） */
    @TableField(exist = false)
    private String stem;

    /** 正确答案（展示字段） */
    @TableField(exist = false)
    private String answer;

    /** 题干附件（展示字段） */
    @TableField(exist = false)
    private String attachmentUrl;

    /** 题目分值（展示字段） */
    @TableField(exist = false)
    private BigDecimal score;
}
