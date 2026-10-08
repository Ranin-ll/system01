package com.ruoyi.business.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 转正审核候选人（部门端「实习转正审核」左列表 + 右详情一行）。
 *
 * <p>一行 = 一位本部门在培实习生，附带「最新一条转正申请」与「实时资格核对」。
 * 因为左列表与右详情是同一份数据的两种投影，所以只出一个接口，
 * 避免两处口径漂移（与 {@code InternStageRow} 同一思路）。</p>
 *
 * @author ruoyi
 */
@Data
public class PromotionCandidate implements Serializable {

    private static final long serialVersionUID = 1L;

    // ==================== 身份 ====================

    private Long userId;
    private String userName;
    private String nickName;
    private Long deptId;
    private String deptName;
    private Long positionId;
    private String positionName;
    /** 培养状态：PRE_TRAINEE / FORMAL_TRAINEE / PENDING_PROMOTE */
    private String userStatus;
    /** 岗位编码（证书编号 {岗位缩写} 取这里） */
    private String positionCode;

    // ==================== 资格（实时） ====================

    private BigDecimal studyRate;
    private Integer protocolSigned;
    private Integer passedExamTimes;
    private Integer examTimes;
    /** 完整资格核对（含门槛与逐项清单） */
    private PromotionGate gate;

    // ==================== 最新一条申请 ====================

    private Long applicationId;
    private String applicationNo;
    /** DEPT_PENDING / PASSED / REJECTED；无申请时为 null */
    private String applicationStatus;
    private String rejectReason;
    private Date submittedAt;
    private Date decidedAt;
    private String deptApproveName;
    private String supplement;
    private String certNo;

    // ==================== 派生展示字段 ====================

    /** 列表左侧圆点色：green 资格齐备 / orange 有待补 / red 未达门槛 / gray 无申请 */
    @TableField(exist = false)
    private String tone;
}
