package com.ruoyi.business.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 通用审核/审批记录对象 audit_record
 *
 * @author ruoyi
 */
@Data
@TableName("audit_record")
public class AuditRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 审核ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 业务类型：REGISTER注册/PROMOTION转正 */
    private String bizType;

    /** 业务单据ID */
    private Long bizId;

    /** 被审核/审批用户 */
    private Long userId;

    /** 审核人所在部门 */
    private Long deptId;

    /** 动作：PASS通过/REJECT驳回 */
    private String action;

    /** 前状态 */
    private String fromStatus;

    /** 后状态 */
    private String toStatus;

    /** 驳回/人工判断原因 */
    private String reason;

    /** 操作人 */
    private Long operatorId;

    /** 创建时间 */
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private Date createTime;

    // 扩展字段
    @TableField(exist = false)
    private String operatorName;

    @TableField(exist = false)
    private String deptName;
}