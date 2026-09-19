package com.ruoyi.business.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

/**
 * 协议模板 agreement_template
 *
 * <p>实习生首登时「协议门」读取 status = EFFECTIVE 的那一行作为签署内容；
 * 签署后 sign_count + 1。**模板只增不改**：发布新版本时把旧的置为 ARCHIVED，
 * 历史签署记录（agreement_signature）保留原版本号。</p>
 *
 * @author ruoyi
 */
@TableName("agreement_template")
public class AgreementTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 生效中 */
    public static final String STATUS_EFFECTIVE = "EFFECTIVE";
    /** 已归档 */
    public static final String STATUS_ARCHIVED = "ARCHIVED";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 协议名称 */
    private String agreementName;

    /** 版本号 */
    private String versionNo;

    /** 协议正文 */
    private String content;

    /** 附件地址（签字版扫描件等） */
    private String fileUrl;

    /** 生效时间 */
    private Date effectiveTime;

    /** 状态：EFFECTIVE 生效中 / ARCHIVED 已归档 */
    private String status;

    /** 已签署人数 */
    private Integer signCount;

    private String createBy;

    private Date createTime;

    private Date updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAgreementName() {
        return agreementName;
    }

    public void setAgreementName(String agreementName) {
        this.agreementName = agreementName;
    }

    public String getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(String versionNo) {
        this.versionNo = versionNo;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public Date getEffectiveTime() {
        return effectiveTime;
    }

    public void setEffectiveTime(Date effectiveTime) {
        this.effectiveTime = effectiveTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getSignCount() {
        return signCount;
    }

    public void setSignCount(Integer signCount) {
        this.signCount = signCount;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
