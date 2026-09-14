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
 * 注册申请单对象 register_application
 *
 * @author ruoyi
 */
@Data
@TableName("register_application")
public class RegisterApplication implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 申请ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 申请编号（重新提交沿用） */
    @Excel(name = "申请编号")
    private String applicationNo;

    /** 申请人用户ID */
    private Long userId;

    /** 姓名快照 */
    @Excel(name = "姓名")
    private String realName;

    /** 身份证号快照 */
    private String idCard;

    /** 登录账号快照 */
    @Excel(name = "登录账号")
    private String loginAccount;

    /** 岗位ID */
    private Long positionId;

    /** 部门ID */
    private Long deptId;

    /** 预计入职时间 */
    @Excel(name = "预计入职时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date expectedEntryDate;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "WAIT_AUDIT=待审核,REJECTED=已驳回,PASSED=已通过")
    private String status;

    /** 驳回原因 */
    private String rejectReason;

    /** 提交次数 */
    private Integer auditCount;

    /** 创建时间 */
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private Date createTime;

    /** 更新时间 */
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private Date updateTime;

    // ========== 扩展字段（用于查询展示） ==========

    /** 岗位名称 */
    @Excel(name = "岗位")
    @TableField(exist = false)
    private String positionName;

    /** 部门名称 */
    @Excel(name = "所属部门")
    @TableField(exist = false)
    private String deptName;

    /** 申请人账号当前状态 */
    @TableField(exist = false)
    private String accountStatus;

    /** 申请人业务状态 */
    @TableField(exist = false)
    private String userStatus;

    /** 当前导师名称 */
    @TableField(exist = false)
    private String mentorName;

    /** 当前导师联系方式 */
    @TableField(exist = false)
    private String mentorPhone;
}
