package com.ruoyi.business.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

/**
 * 学习任务附件 task_attachment
 *
 * <p><b>任务级</b>资源 —— 部门管理员发布任务时上传的参考资料，实习生可查看下载。
 * 与 {@code task_submission.file_url}（实习生交上来的作业附件）是两件事，不要混用。</p>
 *
 * <p>{@code file_url} 存的是相对路径（{@code /profile/upload/...}），前端统一拼
 * {@code process.env.VUE_APP_BASE_API + fileUrl} —— dev 下由 vue.config.js 的代理转发到 8080，
 * 直接写死 `http://localhost:8080` 会在部署后失效。</p>
 *
 * @author ruoyi
 */
@TableName("task_attachment")
public class TaskAttachment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long taskId;

    /** 原始文件名 */
    private String fileName;

    /** 相对路径 */
    private String fileUrl;

    /** 字节 */
    private Long fileSize;

    private String fileExt;

    private Long uploaderId;

    private Integer deleted;

    private Date createTime;

    // ------------------------------------------------------------------ 展示用（不落库）

    @TableField(exist = false)
    private String uploaderName;

    public TaskAttachment() {
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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileExt() {
        return fileExt;
    }

    public void setFileExt(String fileExt) {
        this.fileExt = fileExt;
    }

    public Long getUploaderId() {
        return uploaderId;
    }

    public void setUploaderId(Long uploaderId) {
        this.uploaderId = uploaderId;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUploaderName() {
        return uploaderName;
    }

    public void setUploaderName(String uploaderName) {
        this.uploaderName = uploaderName;
    }
}
