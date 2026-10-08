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
 * 转正要求规则对象 promotion_rule
 *
 * <p>{@code deptId == null} 表示「全局默认规则」；非 null 表示「部门级覆盖」。
 * 读取优先级：<b>部门规则 &gt; 全局规则 &gt; 内置兜底 {studyRateMin:0, examPassTimes:1}</b>。</p>
 *
 * @author ruoyi
 */
@Data
@TableName("promotion_rule")
public class PromotionRule implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 内置兜底规则（库中读不到任何规则时使用） */
    public static final int DEFAULT_STUDY_RATE_MIN = 0;
    public static final int DEFAULT_EXAM_PASS_TIMES = 1;

    /** 规则ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属部门；null=全局默认规则 */
    private Long deptId;

    /** 学习完成率最低门槛(%)，0=不卡学习率 */
    private Integer studyRateMin;

    /** 正式考核至少通过次数 */
    private Integer examPassTimes;

    /** 是否启用(1启用/0停用) */
    private Integer enabled;

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    // ========== 扩展字段（查询展示用，非表列） ==========

    /** 部门名称（全局规则时为空） */
    @TableField(exist = false)
    private String deptName;

    /** 规则来源：DEPT部门规则 / GLOBAL全局规则 / DEFAULT内置兜底 */
    @TableField(exist = false)
    private String source;
}
