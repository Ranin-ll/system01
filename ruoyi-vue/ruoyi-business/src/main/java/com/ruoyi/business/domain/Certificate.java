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
 * 电子证书对象 certificate
 *
 * <p>转正审批通过时由服务端自动签发：编号规则取 {@code assessment_config.cert_no_rule}，
 * 模板取 {@code assessment_config.cert_template_url}，并回写 {@code promotion_application.cert_no}。</p>
 *
 * @author ruoyi
 */
@Data
@TableName("certificate")
public class Certificate implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 已生成 */
    public static final String STATUS_GENERATED = "GENERATED";
    /** 已作废（超管撤回转正时用） */
    public static final String STATUS_REVOKED = "REVOKED";

    /** 证书ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 证书编号（全局唯一） */
    private String certNo;

    /** 持证人 */
    private Long userId;

    /** 关联转正申请 */
    private Long promotionId;

    /** 证书模板/图片地址 */
    private String templateUrl;

    /** 状态：GENERATED已生成/REVOKED已作废 */
    private String status;

    /** 生成时间 */
    private Date generateTime;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    // ========== 扩展字段 ==========

    /** 持证人姓名 */
    @TableField(exist = false)
    private String userName;
}
