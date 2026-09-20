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

    /** 模拟题库ID（历史字段：一个题库会被多个模块的考核复用，已不作为展示口径） */
    private Long bankId;

    /** 来源考核ID（exam.id，模拟理论考核） */
    private Long examId;

    /** 部门ID */
    private Long deptId;

    /** 总题数 */
    private Integer totalCount;

    /** 正确题数 */
    private Integer correctCount;

    /** 得分（=正确题数） */
    private Integer score;

    private Date createTime;

    /** 题库名称（展示字段，历史口径：按「模块」归类后不再单独展示） */
    private String bankName;

    /** 来源考核名称（展示字段，exam.exam_name） */
    private String examName;

    /** 所属模块ID（展示字段，exam.module_id） */
    private Long moduleId;

    /** 所属模块名称（展示字段，practice_module.name） */
    private String moduleName;
}
