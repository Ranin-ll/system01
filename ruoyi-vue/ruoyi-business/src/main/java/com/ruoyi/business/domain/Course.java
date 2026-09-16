package com.ruoyi.business.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.annotation.Excel;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 课程对象 course
 *
 * @author ruoyi
 */
@Data
@TableName("course")
public class Course implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 课程ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 课程名称 */
    @Excel(name = "课程名称")
    private String courseName;

    /** 适用岗位ID */
    private Long positionId;

    /** 课程类型：THEORY理论/PRACTICE实操 */
    @Excel(name = "课程类型", readConverterExp = "THEORY=理论,PRACTICE=实操")
    private String courseType;

    /** 是否必修 */
    @Excel(name = "必修", readConverterExp = "0=选修,1=必修")
    private Integer isRequired;

    /** 封面 */
    private String coverUrl;

    /** 课程简介 */
    private String intro;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "DRAFT=草稿,PUBLISHED=已发布,DISABLED=停用")
    private String status;

    /** 发布时间 */
    private Date publishedAt;

    private String createBy;
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private Date createTime;
    /** 课程表当前未持久化更新人字段，保留为兼容若依通用实体。 */
    @TableField(exist = false)
    private String updateBy;
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private Date updateTime;
    private Integer deleted;

    // 扩展字段
    @TableField(exist = false)
    @Excel(name = "适用岗位")
    private String positionName;

    /** 适用部门名称（列表展示字段） */
    @TableField(exist = false)
    private String deptName;

    /** 当前登录账号的数据范围（仅供课程查询使用，不落库） */
    @TableField(exist = false)
    private Long scopeDeptId;

    @TableField(exist = false)
    private Integer chapterCount;

    @TableField(exist = false)
    private Integer itemCount;

    @TableField(exist = false)
    private Integer completedItems;

    @TableField(exist = false)
    private Integer duration;

    @TableField(exist = false)
    private Integer progress;

    @TableField(exist = false)
    private Date lastStudyTime;

    @TableField(exist = false)
    private List<CourseChapter> chapters = new ArrayList<>();

    @TableField(exist = false)
    private Integer studentCount;

    @TableField(exist = false)
    private Integer expectedStudentCount;

    @TableField(exist = false)
    private Double avgCompletionRate;
}
