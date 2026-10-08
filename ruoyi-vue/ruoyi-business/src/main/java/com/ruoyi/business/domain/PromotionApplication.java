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
 * 转正申请单对象 promotion_application
 *
 * <p>状态机（2026-09-24 决策：<b>部门终审即生效</b>，跳过 SUPER_PENDING）：
 * {@code DEPT_PENDING → PASSED / REJECTED}；
 * {@code super_approve_by / super_approve_time} 两列保留但业务不写，留给超管代操作留痕。</p>
 *
 * @author ruoyi
 */
@Data
@TableName("promotion_application")
public class PromotionApplication implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 待部门审核 */
    public static final String STATUS_DEPT_PENDING = "DEPT_PENDING";
    /** 已通过（部门终审即生效） */
    public static final String STATUS_PASSED = "PASSED";
    /** 已驳回 */
    public static final String STATUS_REJECTED = "REJECTED";
    /** 已作废（超管撤回转正时置，非驳回，可重新申请） */
    public static final String STATUS_REVOKED = "REVOKED";

    /** 申请ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 转正申请编号 */
    private String applicationNo;

    /** 申请人 */
    private Long userId;

    /** 所属部门 */
    private Long deptId;

    /** 状态：DEPT_PENDING待部门审核/PASSED已通过/REJECTED已驳回 */
    private String status;

    /** 门槛校验结果JSON（提交时冻结，便于回溯「当时依据」） */
    private String gateCheckJson;

    /** 补充材料说明 / 转正说明 / 阶段自评 */
    private String supplement;

    /** 部门审核人 */
    private Long deptApproveBy;

    /** 部门审核时间 */
    private Date deptApproveTime;

    /** 超管审批人（保留列，业务不写） */
    private Long superApproveBy;

    /** 超管审批时间（保留列，业务不写） */
    private Date superApproveTime;

    /** 驳回原因 */
    private String rejectReason;

    /** 生成证书编号（通过后回写） */
    private String certNo;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    // ========== 扩展字段（查询展示用，非表列） ==========

    /** 申请人登录账号 */
    @TableField(exist = false)
    private String userName;

    /** 申请人姓名 */
    @TableField(exist = false)
    private String realName;

    /** 申请人岗位名称 */
    @TableField(exist = false)
    private String positionName;

    /** 申请人岗位编码（证书编号 {岗位缩写} 取这里） */
    @TableField(exist = false)
    private String positionCode;

    /** 部门名称 */
    @TableField(exist = false)
    private String deptName;

    /** 申请人账号状态 */
    @TableField(exist = false)
    private String accountStatus;

    /** 申请人培养状态：PRE_TRAINEE / FORMAL_TRAINEE */
    @TableField(exist = false)
    private String userStatus;

    /** 部门审核人姓名 */
    @TableField(exist = false)
    private String deptApproveName;
}
