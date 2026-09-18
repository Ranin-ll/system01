package com.ruoyi.business.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

/**
 * 学习任务分配 task_assignment（per-user，天然就是「我的任务」）
 *
 * <p>状态：NOT_STARTED → IN_PROGRESS → DONE；超期未交 → OVERDUE（仍可补交）。
 * 唯一键 <code>(task_id, user_id)</code> 保证「重复发布不重复分配」。</p>
 *
 * @author ruoyi
 */
@TableName("task_assignment")
public class TaskAssignment implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String NOT_STARTED = "NOT_STARTED";
    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String DONE = "DONE";
    public static final String OVERDUE = "OVERDUE";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private Long userId;

    private String status;

    private Date finishTime;

    private Date createTime;

    public TaskAssignment() {
    }

    public TaskAssignment(Long taskId, Long userId) {
        this.taskId = taskId;
        this.userId = userId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Date finishTime) {
        this.finishTime = finishTime;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
