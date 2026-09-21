package com.ruoyi.business.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;

/**
 * 通知定向接收人 notice_target
 *
 * <p>仅当 {@code notice.scopeType = 'USER'} 时使用：{@code scope_id} 是单值，
 * 存不下多选人员，故用本表承接（唯一键 notice_id + user_id）。</p>
 *
 * @author ruoyi
 */
@TableName("notice_target")
public class NoticeTarget implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long noticeId;

    private Long userId;

    public NoticeTarget() {
    }

    public NoticeTarget(Long noticeId, Long userId) {
        this.noticeId = noticeId;
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNoticeId() {
        return noticeId;
    }

    public void setNoticeId(Long noticeId) {
        this.noticeId = noticeId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
