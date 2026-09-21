package com.ruoyi.business.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

/**
 * 作业提交与批阅 task_submission
 *
 * <p>状态：SUBMITTED（已提交）→ REVIEWED（已批阅）；逾期提交记为 OVERDUE。
 * 重复提交走 <code>version + 1</code>。</p>
 *
 * @author ruoyi
 */
@TableName("task_submission")
public class TaskSubmission implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String SUBMITTED = "SUBMITTED";
    public static final String REVIEWED = "REVIEWED";
    public static final String OVERDUE = "OVERDUE";

    /**
     * ⚠️ {@code review_status} 是**三个值**，不要当布尔用（{@code 'PENDING'} 也是真值）：
     * {@code PENDING} 还没批 / {@code PASSED} 通过 / {@code REJECTED} 退回。
     * 前端曾把 {@code PENDING} 当成「已批阅」，导致待批阅的提交显示成「退回」且批语框不出现。
     */
    public static final String REVIEW_PENDING = "PENDING";
    public static final String REVIEW_PASSED = "PASSED";
    public static final String REVIEW_REJECTED = "REJECTED";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 任务分配ID */
    private Long assignmentId;

    private Long taskId;

    private Long userId;

    /** 作业内容（纯文本） */
    private String content;

    /** 作业附件地址 */
    private String fileUrl;

    /** 作业附件原始文件名（2026-09-18 新增列；展示用，避免让批阅人看到哈希后的文件名） */
    private String fileName;

    private Date submitTime;

    /** SUBMITTED / REVIEWED / OVERDUE */
    private String status;

    /** 批阅状态 */
    private String reviewStatus;

    /** 批阅意见 */
    private String reviewComment;

    private Long reviewerId;

    private Date reviewTime;

    private Integer version;

    private Date createTime;

    private Date updateTime;

    // ---------------- 展示附加（不落库） ----------------

    @TableField(exist = false)
    private String userName;

    @TableField(exist = false)
    private String nickName;

    @TableField(exist = false)
    private String deptName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Date getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(Date submitTime) {
        this.submitTime = submitTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public Long getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(Long reviewerId) {
        this.reviewerId = reviewerId;
    }

    public Date getReviewTime() {
        return reviewTime;
    }

    public void setReviewTime(Date reviewTime) {
        this.reviewTime = reviewTime;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
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

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }
}
