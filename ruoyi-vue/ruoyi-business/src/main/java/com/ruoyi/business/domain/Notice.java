package com.ruoyi.business.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

/**
 * 消息／通知 notice
 *
 * <p>一张表承载三种载体，靠 {@code msgType} 区分：</p>
 * <ul>
 *   <li><b>公告 ANNOUNCE</b>：面向一片人公开告知（仅超管可发），双展示位＝工作台公告位 + 消息中心，可置顶、有有效期、<b>不计未读红点</b>；</li>
 *   <li><b>通知</b>（TASK / EXAM / AUDIT / URGE / SYSTEM）：点对点，只进消息中心 + 铃铛；</li>
 *   <li>范围由 {@code scopeType}(ALL/DEPT/POSITION/USER) + {@code scopeId} 决定，USER 时收件人见 {@code notice_target}。</li>
 * </ul>
 *
 * @author ruoyi
 */
@TableName("notice")
public class Notice implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 公告 */
    public static final String TYPE_ANNOUNCE = "ANNOUNCE";
    /** 已发布 / 草稿 / 已撤回 */
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_REVOKED = "REVOKED";
    /** 范围 */
    public static final String SCOPE_ALL = "ALL";
    public static final String SCOPE_DEPT = "DEPT";
    public static final String SCOPE_POSITION = "POSITION";
    public static final String SCOPE_USER = "USER";

    /**
     * 「部门管理员」虚拟岗位：按岗位发布时 {@code scopeId} 传 0 表示发给各部门管理员。
     *
     * <p>为什么要有它：部门管理员的 {@code position_id} 是 NULL（{@code dept_position} 只把
     * 实习生岗绑到部门），所以用 {@code position_id} 匹配永远选不到管理员 ——
     * 但业务上「管理员是管理员岗」，超管按岗位发消息时应当能把这条线的负责人一起带上。</p>
     *
     * <p>为什么哨兵值是 0：{@code position.id} 自增、真实岗位从 1 起，0 永不为真实岗位。
     * 受众判定走角色（{@code DEPT_ADMIN}），见 {@code NoticeMapper.xml} 的 {@code isDeptAdmin}。</p>
     */
    public static final Long POSITION_DEPT_ADMIN = 0L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    /** 发布范围：ALL / DEPT / POSITION / USER */
    private String scopeType;

    /** 范围对象ID（部门ID / 岗位ID）；USER 时为空，收件人在 notice_target */
    private Long scopeId;

    /** 消息类型：ANNOUNCE / TASK / EXAM / AUDIT / URGE / SYSTEM */
    private String msgType;

    /** 业务类型：TASK / EXAM / REGISTER / PROMOTION */
    private String bizType;

    /** 业务主键（点击跳转用） */
    private Long bizId;

    /** 显式跳转路径（可选，优先用 biz_* 推导） */
    private String linkUrl;

    private Integer isTop;

    private Date effectiveTo;

    /** 发送人（系统消息为空），沿用既有列 */
    private Long publisherId;

    private Date publishTime;

    /** DRAFT / PUBLISHED / REVOKED */
    private String status;

    private Date createTime;

    // ---------------- 查询附加字段（不落库） ----------------

    /** 当前用户对该条的已读时间；为空=未读 */
    @TableField(exist = false)
    private Date readTime;

    /** 发送人昵称（列表展示用） */
    @TableField(exist = false)
    private String publisherName;

    /** 查询条件：UNREAD / READ（空=全部） */
    @TableField(exist = false)
    private String readFlag;

    /** 发送方视角：送达人数（命中送达谓词的非停用账号数） */
    @TableField(exist = false)
    private Integer deliveredCount;

    /** 发送方视角：已读人数 */
    @TableField(exist = false)
    private Integer readCount;

    /**
     * 「按实习生筛选已发送通知」用的查询参数（不是落库字段）
     */
    @TableField(exist = false)
    private Long targetUserId;

    /**
     * 筛选到具体某个实习生时，TA 对这条通知的已读时间（null = 未读）
     */
    @TableField(exist = false)
    private java.util.Date targetReadTime;

    /** 发送入参：指定人员（scopeType=USER 时使用） */
    @TableField(exist = false)
    private java.util.List<Long> targetUserIds;

    public Integer getDeliveredCount() {
        return deliveredCount;
    }

    public void setDeliveredCount(Integer deliveredCount) {
        this.deliveredCount = deliveredCount;
    }

    public Integer getReadCount() {
        return readCount;
    }

    public void setReadCount(Integer readCount) {
        this.readCount = readCount;
    }

    public Long getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Long targetUserId) {
        this.targetUserId = targetUserId;
    }

    public java.util.Date getTargetReadTime() {
        return targetReadTime;
    }

    public void setTargetReadTime(java.util.Date targetReadTime) {
        this.targetReadTime = targetReadTime;
    }

    public java.util.List<Long> getTargetUserIds() {
        return targetUserIds;
    }

    public void setTargetUserIds(java.util.List<Long> targetUserIds) {
        this.targetUserIds = targetUserIds;
    }

    public String getReadFlag() {
        return readFlag;
    }

    public void setReadFlag(String readFlag) {
        this.readFlag = readFlag;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public Long getScopeId() {
        return scopeId;
    }

    public void setScopeId(Long scopeId) {
        this.scopeId = scopeId;
    }

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public Long getBizId() {
        return bizId;
    }

    public void setBizId(Long bizId) {
        this.bizId = bizId;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
    }

    public Integer getIsTop() {
        return isTop;
    }

    public void setIsTop(Integer isTop) {
        this.isTop = isTop;
    }

    public Date getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(Date effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public Long getPublisherId() {
        return publisherId;
    }

    public void setPublisherId(Long publisherId) {
        this.publisherId = publisherId;
    }

    public Date getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(Date publishTime) {
        this.publishTime = publishTime;
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

    public Date getReadTime() {
        return readTime;
    }

    public void setReadTime(Date readTime) {
        this.readTime = readTime;
    }

    public String getPublisherName() {
        return publisherName;
    }

    public void setPublisherName(String publisherName) {
        this.publisherName = publisherName;
    }
}
