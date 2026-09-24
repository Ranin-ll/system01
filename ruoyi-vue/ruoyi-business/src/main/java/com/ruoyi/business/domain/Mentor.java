package com.ruoyi.business.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.annotation.Excel;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 导师对象 mentor
 *
 * <p>2026-09-23 新增。此前「导师」只是 {@code sys_user} 上的两个自由文本列
 * （{@code mentor_name} / {@code mentor_phone}），注册审核时手工敲入，产生了大量脏值
 * （如「001」/「112」、「圣诞」/「2332」、「(⊙﹏⊙)图」/「3544」）。
 * 现在导师抽成独立主表，实习生通过 {@code sys_user.mentor_id} 关联。</p>
 *
 * <p>★ {@code sys_user.mentor_name} / {@code mentor_phone} <b>保留不删</b>，
 * 作为「冗余展示字段」——写 {@code mentor_id} 时同步回填这两个列。
 * 这样所有既有的列表查询（注册申请列表、实习生花名册、人员与账号、超管分析）
 * 都无需改动 SQL 即可继续拿到导师姓名。</p>
 *
 * @author ruoyi
 */
@Data
@TableName("mentor")
public class Mentor implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 导师ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 导师姓名 */
    @Excel(name = "导师姓名")
    private String mentorName;

    /** 联系方式 */
    @Excel(name = "联系方式")
    private String mentorPhone;

    /** 所属部门ID（sys_dept.dept_id） */
    private Long deptId;

    /** 岗位ID（position.id，可空） */
    private Long positionId;

    /** 职务 / 头衔（可空） */
    @Excel(name = "职务")
    private String title;

    /** 状态（0启用 1停用） */
    private String status;

    /** 备注 */
    private String remark;

    /** 创建者 */
    private String createBy;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /** 更新者 */
    private String updateBy;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /** 删除标志（0代表存在 1代表删除） */
    private Integer deleted;

    // ---------------- 展示字段（非表列） ----------------

    /** 所属部门名称 */
    @TableField(exist = false)
    private String deptName;

    /** 岗位名称 */
    @TableField(exist = false)
    private String positionName;

    /** 当前带教的实习生人数（sys_user.mentor_id 命中数） */
    @TableField(exist = false)
    private Integer internCount;

    /** 关键字（姓名 / 联系方式模糊匹配，仅查询用） */
    @TableField(exist = false)
    private String keyword;
}
