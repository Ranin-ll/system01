package com.ruoyi.business.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 实操考核题目 exam_subject_item
 *
 * 实操与理论彻底拆分为两类独立考核后，实操题目不再从题库抽题，
 * 而由管理员逐条填写：题干 / 题目描述 / 参考图 / 附件 / 本题满分。
 * 实习生端逐题上传作答文件，管理员逐题打分后求和为整场成绩。
 */
@Data
@TableName("exam_subject_item")
public class ExamSubjectItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属实操考核ID */
    private Long examId;

    /** 题序（从 1 开始） */
    private Integer seq;

    /** 题干（题名） */
    private String title;

    /** 题目描述 / 作业要求 */
    private String description;

    /** 本题满分 */
    private BigDecimal score;

    /** 参考图 JSON：[{name,url}] */
    private String referenceImages;

    /** 参考附件 JSON：[{name,url}] */
    private String attachmentsJson;

    private Date createTime;
    private Date updateTime;
}
