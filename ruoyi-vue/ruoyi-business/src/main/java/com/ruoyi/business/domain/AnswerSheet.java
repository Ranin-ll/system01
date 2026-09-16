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
 * 答卷对象 answer_sheet
 */
@Data
@TableName("answer_sheet")
public class AnswerSheet implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long examId;
    private Long userId;

    /** 类型：PRACTICE模拟 / FORMAL正式 */
    private String sheetType;

    /** 状态：IN_PROGRESS作答中 / SUBMITTED已交卷 / SCORING批改中 / PUBLISHED成绩已发布 */
    private String status;

    private Date startTime;
    private Date submitTime;
    private String submitType;

    /** 实操考核作答文件路径（实习生上传） */
    private String subjectAnswer;

    private Integer questionCount;

    /** 客观题得分（自动判分） */
    private BigDecimal aiScore;

    /** 实操题得分（人工打分） */
    private BigDecimal manualScore;

    /** 人工评语 */
    private String manualComment;

    /** 最终总分 */
    private BigDecimal finalScore;

    /** 是否通过：0否 1是 */
    private Integer passFlag;

    private Integer retakeSeq;
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /** 考核名称（展示字段） */
    @TableField(exist = false)
    private String examName;

    /** 实习生姓名（展示字段） */
    @TableField(exist = false)
    private String userName;

    /** 实习生登录账号（展示字段） */
    @TableField(exist = false)
    private String loginName;

    /** 部门名称（展示字段） */
    @TableField(exist = false)
    private String deptName;

    /** 当前登录账号数据范围 */
    @TableField(exist = false)
    private Long scopeDeptId;
}
