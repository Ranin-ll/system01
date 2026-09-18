package com.ruoyi.business.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 学习任务 learning_task
 *
 * <p>生命周期：DRAFT ──发布──&gt; PUBLISHED ──结束/到期──&gt; ENDED。
 * <b>发布是一个事务</b>：改状态 + 按 {@code position_id} 展开写 {@code task_assignment} + 落一条 {@code notice}。</p>
 *
 * @author ruoyi
 */
@TableName("learning_task")
public class LearningTask implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_ENDED = "ENDED";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String taskName;

    /** 关联课程 */
    private Long courseId;

    /** 适用岗位（决定发给谁） */
    private Long positionId;

    /**
     * 发布对象（与「通知管理」的发送范围同一套逻辑）：
     * <ul>
     *   <li>{@code DEPT} = 本部门全体在培实习生（任务<b>不绑定岗位</b>，{@link #positionId} 为 null）</li>
     *   <li>{@code POSITION} = 该岗位全部在培实习生（任务归属该岗位）</li>
     *   <li>{@code USER} = 只发给指定人员（本部门任意岗位的在培实习生，任务不绑定岗位）</li>
     * </ul>
     *
     * <p>为什么只有 {@code POSITION} 档会写 {@link #positionId}：岗位决定课程可见性、
     * 批阅归属等既有口径。按部门 / 按人发的任务本来就不属于某一个岗位，
     * 硬塞一个岗位进去会让这些口径失真 —— 所以那两档把 position_id 置空，
     * 任务的归属改由「发布人所属部门」判定（见 LearningTaskMapper 的部门范围条件）。</p>
     */
    public static final String SCOPE_DEPT = "DEPT";
    public static final String SCOPE_POSITION = "POSITION";
    public static final String SCOPE_USER = "USER";

    private String assignScope;

    /** 任务目标 */
    private String target;

    /** 任务描述 */
    private String description;

    private BigDecimal theoryHours;

    private BigDecimal practiceHours;

    private Date startTime;

    private Date deadline;

    /** 是否需要交作业 */
    private Integer needAssignment;

    /**
     * 是否允许多次提交：1=允许（默认，可反复提交，version+1）；0=只允许一次。
     *
     * <p>注意 0 不等于「交过就永远不能交」——<b>被批阅人退回后仍可重新提交</b>
     * （退回本身就是要求重做），否则规则自相矛盾。</p>
     */
    private Integer allowResubmit;

    /** 是否启用讨论区 */
    private Integer discussionEnabled;

    /** 批阅人（部门管理员） */
    private Long reviewerId;

    private Long publisherId;

    private String status;

    private Date createTime;

    private Date updateTime;

    private Integer deleted;

    // ---------------- 查询/展示附加（不落库） ----------------

    /** 分配人数 */
    @TableField(exist = false)
    private Integer assignedCount;

    /** 已提交人数 */
    @TableField(exist = false)
    private Integer submittedCount;

    /** 待批阅人数 */
    @TableField(exist = false)
    private Integer pendingReviewCount;

    /** 讨论区未删除的帖子数（任务卡上的「讨论 N」角标） */
    @TableField(exist = false)
    private Integer postCount;

    /** 任务资料数量（任务卡上的「资料 N」角标） */
    @TableField(exist = false)
    private Integer attachmentCount;

    /**
     * 「按实习生筛选任务」用的查询参数（不是落库字段）：
     * 只返回<b>分配给该实习生</b>的任务，并带出 TA 的分配状态（{@link #assignStatus}）。
     */
    @TableField(exist = false)
    private Long userId;

    /** 指定人员的姓名（列表/详情回显用，GROUP_CONCAT） */
    @TableField(exist = false)
    private String targetNames;

    /** 我（实习生）最近一次提交的作业附件相对路径 —— 用于「交作业」弹窗回填 */
    @TableField(exist = false)
    private String fileUrl;

    /** 我最近一次提交的作业附件原始文件名 */
    @TableField(exist = false)
    private String fileName;

    /** 岗位名 */
    @TableField(exist = false)
    private String positionName;

    /** 发布人昵称（任务详情弹窗用） */
    @TableField(exist = false)
    private String publisherName;

    /**
     * 发布人所属部门（详情回显用）。
     *
     * <p>部门级任务（{@code assign_scope=DEPT}，不带岗位）没有 {@code position_id}，
     * 判断「这条任务归哪个部门」只能看发布人 —— 列表的部门范围条件与详情的归属校验都用它。</p>
     */
    @TableField(exist = false)
    private Long publisherDeptId;

    /** 批阅人昵称（任务详情弹窗用） */
    @TableField(exist = false)
    private String reviewerName;

    /** 剩余天数（负数=已逾期） */
    @TableField(exist = false)
    private Integer remainDays;

    /** 我的分配状态（实习生「我的任务」用） */
    @TableField(exist = false)
    private String assignStatus;

    /** 我的最近一次提交状态 / 批阅意见 */
    @TableField(exist = false)
    private String submissionStatus;

    @TableField(exist = false)
    private String reviewStatus;

    @TableField(exist = false)
    private String reviewComment;

    @TableField(exist = false)
    private Date submitTime;

    @TableField(exist = false)
    private Long assignmentId;

    /** 查询条件：状态 / 关键字 */
    @TableField(exist = false)
    private String keyword;

    /** 发布入参：可指定人员（留空=按岗位展开全体） */
    @TableField(exist = false)
    private List<Long> targetUserIds;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public Long getPositionId() {
        return positionId;
    }

    public void setPositionId(Long positionId) {
        this.positionId = positionId;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getTheoryHours() {
        return theoryHours;
    }

    public void setTheoryHours(BigDecimal theoryHours) {
        this.theoryHours = theoryHours;
    }

    public BigDecimal getPracticeHours() {
        return practiceHours;
    }

    public void setPracticeHours(BigDecimal practiceHours) {
        this.practiceHours = practiceHours;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getDeadline() {
        return deadline;
    }

    public void setDeadline(Date deadline) {
        this.deadline = deadline;
    }

    public Integer getNeedAssignment() {
        return needAssignment;
    }

    public void setNeedAssignment(Integer needAssignment) {
        this.needAssignment = needAssignment;
    }

    public Integer getAllowResubmit() {
        return allowResubmit;
    }

    public void setAllowResubmit(Integer allowResubmit) {
        this.allowResubmit = allowResubmit;
    }

    public Integer getDiscussionEnabled() {
        return discussionEnabled;
    }

    public void setDiscussionEnabled(Integer discussionEnabled) {
        this.discussionEnabled = discussionEnabled;
    }

    public Long getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(Long reviewerId) {
        this.reviewerId = reviewerId;
    }

    public Long getPublisherId() {
        return publisherId;
    }

    public void setPublisherId(Long publisherId) {
        this.publisherId = publisherId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public Integer getAssignedCount() {
        return assignedCount;
    }

    public void setAssignedCount(Integer assignedCount) {
        this.assignedCount = assignedCount;
    }

    public Integer getSubmittedCount() {
        return submittedCount;
    }

    public void setSubmittedCount(Integer submittedCount) {
        this.submittedCount = submittedCount;
    }

    public Integer getPendingReviewCount() {
        return pendingReviewCount;
    }

    public void setPendingReviewCount(Integer pendingReviewCount) {
        this.pendingReviewCount = pendingReviewCount;
    }

    public Integer getPostCount() {
        return postCount;
    }

    public void setPostCount(Integer postCount) {
        this.postCount = postCount;
    }

    public Integer getAttachmentCount() {
        return attachmentCount;
    }

    public void setAttachmentCount(Integer attachmentCount) {
        this.attachmentCount = attachmentCount;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getAssignScope() {
        return assignScope;
    }

    public void setAssignScope(String assignScope) {
        this.assignScope = assignScope;
    }

    public String getTargetNames() {
        return targetNames;
    }

    public void setTargetNames(String targetNames) {
        this.targetNames = targetNames;
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

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }

    public String getPublisherName() {
        return publisherName;
    }

    public void setPublisherName(String publisherName) {
        this.publisherName = publisherName;
    }

    public Long getPublisherDeptId() {
        return publisherDeptId;
    }

    public void setPublisherDeptId(Long publisherDeptId) {
        this.publisherDeptId = publisherDeptId;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public Integer getRemainDays() {
        return remainDays;
    }

    public void setRemainDays(Integer remainDays) {
        this.remainDays = remainDays;
    }

    public String getAssignStatus() {
        return assignStatus;
    }

    public void setAssignStatus(String assignStatus) {
        this.assignStatus = assignStatus;
    }

    public String getSubmissionStatus() {
        return submissionStatus;
    }

    public void setSubmissionStatus(String submissionStatus) {
        this.submissionStatus = submissionStatus;
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

    public Date getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(Date submitTime) {
        this.submitTime = submitTime;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public List<Long> getTargetUserIds() {
        return targetUserIds;
    }

    public void setTargetUserIds(List<Long> targetUserIds) {
        this.targetUserIds = targetUserIds;
    }
}
