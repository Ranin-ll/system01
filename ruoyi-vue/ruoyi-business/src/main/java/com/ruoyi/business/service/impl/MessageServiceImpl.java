package com.ruoyi.business.service.impl;

import com.ruoyi.business.domain.Notice;
import com.ruoyi.business.domain.NoticeTarget;
import com.ruoyi.business.mapper.NoticeMapper;
import com.ruoyi.business.mapper.NoticeReadMapper;
import com.ruoyi.business.mapper.NoticeTargetMapper;
import com.ruoyi.business.service.IMessageService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息中心 Service 实现（P0：只做「收」）
 *
 * @author ruoyi
 */
@Service
public class MessageServiceImpl implements IMessageService {

    /** 工作台公告位最多展示条数 */
    private static final int ANNOUNCE_LIMIT = 3;

    /** 统一空态/异常文案：不区分「不存在」与「无权查看」，避免泄露消息存在性 */
    private static final String NOT_FOUND = "消息不存在或已被撤回";

    @Autowired
    private NoticeMapper noticeMapper;

    @Autowired
    private NoticeReadMapper noticeReadMapper;

    @Autowired
    private NoticeTargetMapper noticeTargetMapper;

    @Override
    public Map<String, Object> unreadCount() {
        Long userId = SecurityUtils.getUserId();
        Map<String, Object> result = new HashMap<>();
        result.put("total", noticeMapper.countUnread(userId));

        Map<String, Object> byType = new HashMap<>();
        for (Map<String, Object> row : noticeMapper.countUnreadByType(userId)) {
            Object type = row.get("msgType");
            Object cnt = row.get("cnt");
            if (type != null) {
                byType.put(String.valueOf(type), cnt == null ? 0 : ((Number) cnt).intValue());
            }
        }
        result.put("byType", byType);
        return result;
    }

    @Override
    public List<Notice> myList(Notice query) {
        Notice q = query == null ? new Notice() : query;
        return noticeMapper.selectMyList(SecurityUtils.getUserId(), q.getMsgType(),
                q.getReadFlag(), q.getTitle());
    }

    @Override
    public List<Notice> announcements() {
        return noticeMapper.selectAnnouncements(SecurityUtils.getUserId(), ANNOUNCE_LIMIT);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Notice detail(Long id) {
        if (id == null) {
            throw new ServiceException("消息ID不能为空");
        }
        Long userId = SecurityUtils.getUserId();
        Notice notice = noticeMapper.selectVisibleById(id, userId);
        if (notice == null) {
            throw new ServiceException(NOT_FOUND);
        }
        // 「打开即已读」：唯一键保证幂等，重复打开不会重复写
        if (notice.getReadTime() == null) {
            noticeReadMapper.markRead(id, userId);
            // 回填，前端拿到详情时即可显示"刚刚已读"
            notice.setReadTime(new java.util.Date());
        }
        return notice;
    }

    @Override
    public void markRead(Long id) {
        if (id == null) {
            throw new ServiceException("消息ID不能为空");
        }
        Long userId = SecurityUtils.getUserId();
        // 先校验可见性，避免对别人的消息写已读
        if (noticeMapper.selectVisibleById(id, userId) == null) {
            throw new ServiceException(NOT_FOUND);
        }
        noticeReadMapper.markRead(id, userId);
    }

    @Override
    public int markAllRead(String msgType) {
        return noticeMapper.markAllRead(SecurityUtils.getUserId(), msgType);
    }

    // ------------------------------------------------------------------ P1 发送方

    /** 允许的消息类型（公告 / 人工通知 / 由业务事件产生的五类） */
    private static final java.util.List<String> ALLOWED_TYPES = java.util.Arrays.asList(
            Notice.TYPE_ANNOUNCE, "NOTIFY", "TASK", "EXAM", "AUDIT", "URGE", "SYSTEM");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Notice send(Notice req) {
        if (req == null) {
            throw new ServiceException("消息内容不能为空");
        }
        String title = req.getTitle() == null ? "" : req.getTitle().trim();
        if (title.isEmpty()) {
            throw new ServiceException("标题不能为空");
        }
        if (isBlank(req.getContent())) {
            throw new ServiceException("正文不能为空");
        }
        String msgType = isBlank(req.getMsgType()) ? Notice.TYPE_ANNOUNCE : req.getMsgType().trim().toUpperCase();
        if (!ALLOWED_TYPES.contains(msgType)) {
            throw new ServiceException("消息类型不合法");
        }
        String scopeType = isBlank(req.getScopeType()) ? Notice.SCOPE_ALL : req.getScopeType().trim().toUpperCase();

        boolean superAdmin = SecurityUtils.hasRole("SUPER_ADMIN") || SecurityUtils.isAdmin(SecurityUtils.getUserId());
        Long myDept = SecurityUtils.getDeptId();

        // ① 公告：只有超管能发
        if (Notice.TYPE_ANNOUNCE.equals(msgType) && !superAdmin) {
            throw new ServiceException("只有超级管理员可以发布公告");
        }
        // ② 全体：只有超管能发
        if (Notice.SCOPE_ALL.equals(scopeType) && !superAdmin) {
            throw new ServiceException("部门管理员只能向本部门或本部门人员发送通知");
        }
        // ③ 部门管理员的三个范围都要锁在本部门
        if (!superAdmin) {
            if (myDept == null) {
                throw new ServiceException("当前账号未配置部门，无法发送通知");
            }
            if (Notice.SCOPE_DEPT.equals(scopeType)) {
                if (req.getScopeId() == null) {
                    throw new ServiceException("请选择要发送的部门");
                }
                if (!myDept.equals(req.getScopeId())) {
                    throw new ServiceException("部门管理员只能向本部门发送通知");
                }
            } else if (Notice.SCOPE_POSITION.equals(scopeType)) {
                if (req.getScopeId() == null) {
                    throw new ServiceException("请选择要发送的岗位");
                }
                // 「部门管理员岗」是全局受众（会同时发给其他部门的管理员）→ 与「全体」同级，仅超管可发
                if (Notice.POSITION_DEPT_ADMIN.equals(req.getScopeId())) {
                    throw new ServiceException("只有超级管理员可以向「部门管理员」发布");
                }
                if (noticeMapper.countDeptPosition(myDept, req.getScopeId()) == 0) {
                    throw new ServiceException("该岗位不属于你的部门");
                }
            } else if (Notice.SCOPE_USER.equals(scopeType)) {
                if (req.getTargetUserIds() == null || req.getTargetUserIds().isEmpty()) {
                    throw new ServiceException("请选择接收人");
                }
                if (noticeMapper.countUsersOutsideDept(req.getTargetUserIds(), myDept) > 0) {
                    throw new ServiceException("只能向本部门人员发送通知");
                }
            }
        }
        // ④ 通用必填校验
        if (Notice.SCOPE_USER.equals(scopeType)) {
            if (req.getTargetUserIds() == null || req.getTargetUserIds().isEmpty()) {
                throw new ServiceException("请选择接收人");
            }
        } else if (!Notice.SCOPE_ALL.equals(scopeType) && req.getScopeId() == null) {
            throw new ServiceException("请选择范围对象");
        }

        Date now = new Date();
        Notice n = new Notice();
        n.setTitle(title);
        n.setContent(req.getContent());
        n.setMsgType(msgType);
        n.setScopeType(scopeType);
        n.setScopeId(Notice.SCOPE_USER.equals(scopeType) ? null : req.getScopeId());
        n.setBizType(req.getBizType());
        n.setBizId(req.getBizId());
        n.setLinkUrl(req.getLinkUrl());
        n.setIsTop(req.getIsTop() == null ? 0 : req.getIsTop());
        n.setEffectiveTo(req.getEffectiveTo());
        n.setPublisherId(SecurityUtils.getUserId());
        n.setPublishTime(now);
        n.setStatus(Notice.STATUS_PUBLISHED);
        n.setCreateTime(now);
        return doPublish(n, req.getTargetUserIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Notice publishSystem(Notice notice) {
        if (notice == null || isBlank(notice.getTitle()) || isBlank(notice.getContent())) {
            throw new ServiceException("系统消息的标题与正文不能为空");
        }
        Date now = new Date();
        Notice n = new Notice();
        n.setTitle(notice.getTitle());
        n.setContent(notice.getContent());
        n.setMsgType(isBlank(notice.getMsgType()) ? "SYSTEM" : notice.getMsgType());
        n.setScopeType(isBlank(notice.getScopeType()) ? Notice.SCOPE_USER : notice.getScopeType());
        n.setScopeId(Notice.SCOPE_USER.equals(n.getScopeType()) ? null : notice.getScopeId());
        n.setBizType(notice.getBizType());
        n.setBizId(notice.getBizId());
        n.setLinkUrl(notice.getLinkUrl());
        n.setIsTop(notice.getIsTop() == null ? 0 : notice.getIsTop());
        n.setEffectiveTo(notice.getEffectiveTo());
        // 系统消息：无发送人
        n.setPublisherId(null);
        n.setPublishTime(now);
        n.setStatus(Notice.STATUS_PUBLISHED);
        n.setCreateTime(now);
        return doPublish(n, notice.getTargetUserIds());
    }

    /** 落库 + 写定向接收人（send 与 publishSystem 共用，避免两套写入逻辑走偏） */
    private Notice doPublish(Notice n, java.util.List<Long> targetUserIds) {
        noticeMapper.insert(n);
        if (Notice.SCOPE_USER.equals(n.getScopeType()) && targetUserIds != null) {
            for (Long uid : targetUserIds) {
                if (uid != null) {
                    noticeTargetMapper.insert(new NoticeTarget(n.getId(), uid));
                }
            }
        }
        return n;
    }

    @Override
    public List<Notice> sentList(Notice query) {
        Notice q = query == null ? new Notice() : query;
        return noticeMapper.selectSentList(SecurityUtils.getUserId(), q.getMsgType(), q.getTitle(), q.getTargetUserId());
    }

    @Override
    public List<Map<String, Object>> recipients(Long id) {
        if (id == null) {
            throw new ServiceException("消息ID不能为空");
        }
        Notice n = noticeMapper.selectById(id);
        if (n == null) {
            throw new ServiceException(NOT_FOUND);
        }
        if (!canManage(n)) {
            throw new ServiceException("只能查看自己发送的通知");
        }
        return noticeMapper.selectRecipients(id);
    }

    @Override
    public int estimate(String scopeType, Long scopeId) {
        String st = isBlank(scopeType) ? Notice.SCOPE_ALL : scopeType.trim().toUpperCase();
        // USER 档没有「范围对象」可算（收件人在请求里）→ 返回 0，前端用数组长度覆盖
        if (Notice.SCOPE_USER.equals(st)) {
            return 0;
        }
        // 非全体必须有范围对象，否则算出来是 0 人，容易被误读成"没人会收到"
        if (!Notice.SCOPE_ALL.equals(st) && scopeId == null) {
            return 0;
        }
        return noticeMapper.selectAudienceCount(st, scopeId);
    }

    @Override
    public void revoke(Long id) {
        if (id == null) {
            throw new ServiceException("消息ID不能为空");
        }
        Notice n = noticeMapper.selectById(id);
        if (n == null) {
            throw new ServiceException(NOT_FOUND);
        }
        if (!canManage(n)) {
            throw new ServiceException("只能撤回自己发送的通知");
        }
        if (!Notice.STATUS_PUBLISHED.equals(n.getStatus())) {
            throw new ServiceException("该通知已撤回");
        }
        noticeMapper.revoke(id);
    }

    /** 发送人本人或超管 */
    private boolean canManage(Notice n) {
        if (SecurityUtils.hasRole("SUPER_ADMIN") || SecurityUtils.isAdmin(SecurityUtils.getUserId())) {
            return true;
        }
        return n.getPublisherId() != null && n.getPublisherId().equals(SecurityUtils.getUserId());
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
