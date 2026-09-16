package com.ruoyi.business.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 题库对象 question_bank
 */
@Data
@TableName("question_bank")
public class QuestionBank implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 题库ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 题库名称 */
    private String bankName;

    /** 题库类型：FORMAL正式 / PRACTICE模拟（部门自带，不可删除） */
    private String bankType;

    /** 所属部门ID（关联sys_dept） */
    private Long deptId;

    /** 题库说明 */
    private String description;

    /** 状态：ENABLED启用 / DISABLED停用 */
    private String status;

    /** 题目数量（冗余） */
    private Integer questionCount;

    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(exist = false)
    private String updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    private Integer deleted;

    /** 所属部门名称（列表展示字段） */
    @TableField(exist = false)
    private String deptName;

    /** 当前登录账号的数据范围（仅供查询使用，不落库） */
    @TableField(exist = false)
    private Long scopeDeptId;
}
