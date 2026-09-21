package com.ruoyi.business.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 学习任务讨论区帖子 task_post
 *
 * <p><b>两级展示、任意深度的 parent_id</b>：写库时 {@code parent_id} 指向<b>真实被回复的那条帖</b>
 * （所以数据层面是树），返回给前端时统一归拢到<b>主楼</b>下平铺展示，并在每条回复前带出
 * {@code replyToName}（「回复 @某某」）。这样既不用加 {@code reply_to_user_id} 列，
 * 也不会因为层级过深导致缩进爆掉。</p>
 *
 * <p><b>删除是软删</b>：只写 {@code deleted_by}，行不消失 —— 否则回复链会断。
 * 内容不再回传（前端渲染「该评论已被删除」），父帖被删<b>不会</b>牵连他人回复。</p>
 *
 * @author ruoyi
 */
@TableName("task_post")
public class TaskPost implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 单条内容上限（与前端 maxlength 保持一致） */
    public static final int MAX_LENGTH = 500;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long taskId;

    /** 发帖 / 回复人 */
    private Long userId;

    /** 父帖 id；为 null 即主楼 */
    private Long parentId;

    /** 纯文本内容 */
    private String content;

    /** 删除人（管理员删帖留痕）；非空即已删除 */
    private Long deletedBy;

    private Date createTime;

    // ------------------------------------------------------------------ 展示用（不落库）

    @TableField(exist = false)
    private String userName;

    @TableField(exist = false)
    private String nickName;

    @TableField(exist = false)
    private String avatar;

    /** 该帖作者是否就是当前登录人（前端决定是否显示「删除」） */
    @TableField(exist = false)
    private Boolean mine;

    /** 主楼：本条回复直接回的是谁（「回复 @某某」） */
    @TableField(exist = false)
    private String replyToName;

    /** 本条是否已删除（内容为空、前端给占位） */
    @TableField(exist = false)
    private Boolean deleted;

    /** 主楼下的全部回复（含更深层级，已按时间排序） */
    @TableField(exist = false)
    private List<TaskPost> replies = new ArrayList<>();

    public TaskPost() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(Long deletedBy) {
        this.deletedBy = deletedBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
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

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Boolean getMine() {
        return mine;
    }

    public void setMine(Boolean mine) {
        this.mine = mine;
    }

    public String getReplyToName() {
        return replyToName;
    }

    public void setReplyToName(String replyToName) {
        this.replyToName = replyToName;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public List<TaskPost> getReplies() {
        return replies;
    }

    public void setReplies(List<TaskPost> replies) {
        this.replies = replies;
    }
}
