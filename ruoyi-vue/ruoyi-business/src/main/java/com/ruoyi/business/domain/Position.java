package com.ruoyi.business.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.annotation.Excel;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 岗位类型对象 position
 *
 * @author ruoyi
 */
@Data
@TableName("position")
public class Position implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 岗位ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 岗位编码 */
    @Excel(name = "岗位编码")
    private String positionCode;

    /** 岗位名称 */
    @Excel(name = "岗位名称")
    private String positionName;

    /** 排序 */
    @Excel(name = "排序")
    private Integer sortNo;

    /** 状态（1启用 0停用） */
    @Excel(name = "状态", readConverterExp = "0=停用,1=启用")
    private Integer status;

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private Date createTime;

    /** 更新人 */
    private String updateBy;

    /** 更新时间 */
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /** 删除标志（0代表存在 1代表删除） */
    private Integer deleted;

    /** 岗位归属部门ID（注册页展示字段） */
    @TableField(exist = false)
    private Long deptId;

    /** 岗位归属部门名称（注册页展示字段） */
    @TableField(exist = false)
    private String deptName;
}
