package com.ruoyi.business.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 考核与运营规则默认值（单行配置，id 固定 1） assessment_config
 *
 * <p>超管「规则与配置」页的数据落点。存的是**默认值**：
 * 部门管理员发布批次时会把当时的默认值冻结进 exam_rule_snapshot，
 * 已发布批次按各自快照执行，因此改这里不会篡改历史（审计友好）。</p>
 *
 * @author ruoyi
 */
@TableName("assessment_config")
public class AssessmentConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 单行配置固定主键 */
    public static final Long SINGLETON_ID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /** 学习门槛：必修课程完成率(%) */
    private BigDecimal courseDoneThreshold;

    /** 理论权重(%) */
    private BigDecimal theoryWeight;

    /** 实操权重(%) */
    private BigDecimal practiceWeight;

    /** 理论通过线(分) */
    private BigDecimal theoryPassLine;

    /** 实操通过线(分) */
    private BigDecimal practicePassLine;

    /** 是否强制签署保密协议(1是 0否) */
    private Integer protocolRequired;

    /** 补考次数 */
    private Integer retakeCount;

    /** 证书编号规则 */
    private String certNoRule;

    /** 证书模板文件地址 */
    private String certTemplateUrl;

    /** 预警：题库题量须 ≥ 单场需求 × N */
    private Integer alertBankGapFactor;

    /** 预警：考核草稿停留天数上限 */
    private Integer alertExamDraftDays;

    /** 预警：报名待审核条数上限 */
    private Integer alertRegisterPending;

    /** 预警：部门完成率下限(%) */
    private BigDecimal alertDeptDoneFloor;

    /** 预警：考核逾期多少小时 */
    private Integer alertOverdueHours;

    private String updateBy;

    private Date updateTime;

    private Date createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getCourseDoneThreshold() {
        return courseDoneThreshold;
    }

    public void setCourseDoneThreshold(BigDecimal courseDoneThreshold) {
        this.courseDoneThreshold = courseDoneThreshold;
    }

    public BigDecimal getTheoryWeight() {
        return theoryWeight;
    }

    public void setTheoryWeight(BigDecimal theoryWeight) {
        this.theoryWeight = theoryWeight;
    }

    public BigDecimal getPracticeWeight() {
        return practiceWeight;
    }

    public void setPracticeWeight(BigDecimal practiceWeight) {
        this.practiceWeight = practiceWeight;
    }

    public BigDecimal getTheoryPassLine() {
        return theoryPassLine;
    }

    public void setTheoryPassLine(BigDecimal theoryPassLine) {
        this.theoryPassLine = theoryPassLine;
    }

    public BigDecimal getPracticePassLine() {
        return practicePassLine;
    }

    public void setPracticePassLine(BigDecimal practicePassLine) {
        this.practicePassLine = practicePassLine;
    }

    public Integer getProtocolRequired() {
        return protocolRequired;
    }

    public void setProtocolRequired(Integer protocolRequired) {
        this.protocolRequired = protocolRequired;
    }

    public Integer getRetakeCount() {
        return retakeCount;
    }

    public void setRetakeCount(Integer retakeCount) {
        this.retakeCount = retakeCount;
    }

    public String getCertNoRule() {
        return certNoRule;
    }

    public void setCertNoRule(String certNoRule) {
        this.certNoRule = certNoRule;
    }

    public String getCertTemplateUrl() {
        return certTemplateUrl;
    }

    public void setCertTemplateUrl(String certTemplateUrl) {
        this.certTemplateUrl = certTemplateUrl;
    }

    public Integer getAlertBankGapFactor() {
        return alertBankGapFactor;
    }

    public void setAlertBankGapFactor(Integer alertBankGapFactor) {
        this.alertBankGapFactor = alertBankGapFactor;
    }

    public Integer getAlertExamDraftDays() {
        return alertExamDraftDays;
    }

    public void setAlertExamDraftDays(Integer alertExamDraftDays) {
        this.alertExamDraftDays = alertExamDraftDays;
    }

    public Integer getAlertRegisterPending() {
        return alertRegisterPending;
    }

    public void setAlertRegisterPending(Integer alertRegisterPending) {
        this.alertRegisterPending = alertRegisterPending;
    }

    public BigDecimal getAlertDeptDoneFloor() {
        return alertDeptDoneFloor;
    }

    public void setAlertDeptDoneFloor(BigDecimal alertDeptDoneFloor) {
        this.alertDeptDoneFloor = alertDeptDoneFloor;
    }

    public Integer getAlertOverdueHours() {
        return alertOverdueHours;
    }

    public void setAlertOverdueHours(Integer alertOverdueHours) {
        this.alertOverdueHours = alertOverdueHours;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
