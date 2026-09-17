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
 * 题目对象 question
 */
@Data
@TableName("question")
public class Question implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 题目ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属题库ID（关联question_bank） */
    private Long bankId;

    /** 题目编号（题库内唯一） */
    private String questionNo;

    /** 适用岗位ID（可选） */
    private Long positionId;

    /** 题型：SINGLE单选 / MULTI多选 / JUDGE判断 / SUBJECT主观 */
    private String qtype;

    /** 难度：EASY / MEDIUM / HARD */
    private String difficulty;

    /** 知识点 */
    private String knowledgePoint;

    /** 题干 */
    private String stem;

    /** 选项JSON（客观题）：[{"key":"A","content":"..."}] */
    private String optionsJson;

    /** 题干附件路径（实操题参考文件） */
    private String attachmentUrl;

    /** 正确答案（客观题）：单选"A"，多选"A,C,D"，判断"A/B" */
    private String answer;

    /** 解析 */
    private String analysis;

    /** 默认分值 */
    private BigDecimal score;

    /** 是否主观题：0否 1是 */
    private Integer isSubjective;

    /** 状态：1启用 0停用 */
    private Integer status;

    /** 导入批次号 */
    private String importBatch;

    /** 版本 */
    private Integer version;

    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(exist = false)
    private String updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    private Integer deleted;

    /** 题库名称（列表展示字段） */
    @TableField(exist = false)
    private String bankName;

    /** 所属部门名称（列表展示字段） */
    @TableField(exist = false)
    private String deptName;

    /** 当前登录账号的数据范围（仅供查询使用，不落库） */
    @TableField(exist = false)
    private Long scopeDeptId;
}
