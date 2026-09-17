package com.ruoyi.business.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 模拟考核个人成绩记录 practice_record
 */
@Data
public class PracticeRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 实习生ID */
    private Long userId;

    /** 模拟题库ID */
    private Long bankId;

    /** 部门ID */
    private Long deptId;

    /** 总题数 */
    private Integer totalCount;

    /** 正确题数 */
    private Integer correctCount;

    /** 得分（=正确题数） */
    private Integer score;

    private Date createTime;

    /** 题库名称（展示字段） */
    private String bankName;
}
