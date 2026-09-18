package com.ruoyi.business.service.impl;

import com.ruoyi.business.domain.LearningTask;
import com.ruoyi.business.domain.TaskPost;
import com.ruoyi.business.mapper.LearningTaskMapper;
import com.ruoyi.business.mapper.TaskAssignmentMapper;
import com.ruoyi.business.mapper.TaskPostMapper;
import com.ruoyi.business.service.ITaskPostService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 学习任务讨论区 Service 实现
 *
 * @author ruoyi
 */
@Service
public class TaskPostServiceImpl implements ITaskPostService {

    @Autowired
    private TaskPostMapper postMapper;

    @Autowired
    private LearningTaskMapper taskMapper;

    @Autowired
    private TaskAssignmentMapper assignmentMapper;

    // ------------------------------------------------------------------ 读

    @Override
    public Map<String, Object> list(Long taskId) {
        LearningTask task = requireVisibleTask(taskId);
        Long me = SecurityUtils.getUserId();
        boolean superUser = isSuper();
        boolean ended = LearningTask.STATUS_ENDED.equals(task.getStatus());
        boolean enabled = Integer.valueOf(1).equals(task.getDiscussionEnabled());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("enabled", enabled);
        data.put("ended", ended);
        // 超管只读；任务结束后归档只读
        data.put("canPost", enabled && !ended && !superUser);
        data.put("canModerate", enabled && !superUser && !isIntern());

        List<TaskPost> rows = new ArrayList<>();
        if (enabled) {
            rows = assemble(postMapper.selectByTask(task.getId()), me);
        }
        data.put("rows", rows);
        data.put("total", countVisible(rows));
        return data;
    }

    /**
     * 把「任意深度的 parent_id 树」压成「主楼 + 其下全部回复平铺」。
     *
     * <p>回复条数按主楼累加，所以 {@code total} 与前端看到的气泡数一致。</p>
     */
    private List<TaskPost> assemble(List<TaskPost> all, Long me) {
        Map<Long, TaskPost> byId = new HashMap<>();
        for (TaskPost p : all) {
            p.setMine(me != null && me.equals(p.getUserId()));
            byId.put(p.getId(), p);
        }
        List<TaskPost> roots = new ArrayList<>();
        Map<Long, TaskPost> rootsById = new LinkedHashMap<>();
        for (TaskPost p : all) {
            TaskPost root = rootOf(p, byId);
            if (root == null) {
                roots.add(p);
                rootsById.put(p.getId(), p);
            } else {
                p.setReplyToName(displayName(byId.get(p.getParentId())));
                root.getReplies().add(p);
            }
        }
        // 已删除且<b>无人回复</b>的主楼直接不显示 —— 没有信息可保留，纯占位是噪音；
        // 有回复的保留为「该评论已被删除」，否则回复会集体失去上下文。
        roots.removeIf(r -> Boolean.TRUE.equals(r.getDeleted()) && r.getReplies().isEmpty());
        return roots;
    }

    /** 向上回溯到主楼；自身即主楼时返回 null。带环保护。 */
    private TaskPost rootOf(TaskPost p, Map<Long, TaskPost> byId) {
        if (p.getParentId() == null) {
            return null;
        }
        Set<Long> seen = new HashSet<>();
        seen.add(p.getId());
        TaskPost cur = byId.get(p.getParentId());
        while (cur != null && cur.getParentId() != null && seen.add(cur.getId())) {
            cur = byId.get(cur.getParentId());
        }
        return cur;
    }

    private int countVisible(List<TaskPost> roots) {
        int n = 0;
        for (TaskPost r : roots) {
            n += 1 + r.getReplies().size();
        }
        return n;
    }

    // ------------------------------------------------------------------ 写

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long add(Long taskId, String content, Long parentId) {
        LearningTask task = requireVisibleTask(taskId);
        requireDiscussionOpen(task);
        requireNotEnded(task);
        if (isSuper()) {
            throw new ServiceException("超级管理员仅可查看讨论，不能发言");
        }
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) {
            throw new ServiceException("内容不能为空");
        }
        if (text.length() > TaskPost.MAX_LENGTH) {
            throw new ServiceException("内容不能超过 " + TaskPost.MAX_LENGTH + " 字");
        }
        if (parentId != null) {
            TaskPost parent = postMapper.selectById(parentId);
            // 必须同属一个任务 —— 否则可以把回复挂到别的任务上（越权写入）
            if (parent == null || !taskId.equals(parent.getTaskId())) {
                throw new ServiceException("被回复的评论不存在");
            }
        }
        TaskPost post = new TaskPost();
        post.setTaskId(taskId);
        post.setUserId(SecurityUtils.getUserId());
        post.setParentId(parentId);
        post.setContent(text);
        post.setCreateTime(new Date());
        postMapper.insert(post);
        return post.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long postId) {
        if (postId == null) {
            throw new ServiceException("评论ID不能为空");
        }
        TaskPost post = postMapper.selectById(postId);
        if (post == null) {
            throw new ServiceException("评论不存在");
        }
        // 复用同一套可见性判定 —— 能看见才谈得上删
        requireVisibleTask(post.getTaskId());
        Long me = SecurityUtils.getUserId();
        boolean mine = me != null && me.equals(post.getUserId());
        if (!mine) {
            if (isSuper()) {
                throw new ServiceException("超级管理员仅可查看讨论，不能删帖");
            }
            if (isIntern()) {
                throw new ServiceException("只能删除自己的评论");
            }
            // 部门管理员：requireVisibleTask 已校验任务岗位属于本部门
        }
        if (postMapper.softDelete(postId, me) <= 0) {
            throw new ServiceException("该评论已被删除");
        }
    }

    // ------------------------------------------------------------------ 内部

    /**
     * 任务存在 + 当前人对该任务有可见权（与 TaskServiceImpl.detail 同一口径）
     */
    private LearningTask requireVisibleTask(Long taskId) {
        if (taskId == null) {
            throw new ServiceException("任务ID不能为空");
        }
        LearningTask task = taskMapper.selectTaskDetail(taskId);
        if (task == null) {
            throw new ServiceException("任务不存在或已删除");
        }
        if (isSuper()) {
            return task;
        }
        if (isIntern()) {
            if (LearningTask.STATUS_DRAFT.equals(task.getStatus())) {
                throw new ServiceException("任务尚未发布");
            }
            if (assignmentMapper.selectMine(taskId, SecurityUtils.getUserId()) == null) {
                throw new ServiceException("该任务未分配给你");
            }
            return task;
        }
        requireOwnPosition(task.getPositionId());
        return task;
    }

    private void requireDiscussionOpen(LearningTask task) {
        if (!Integer.valueOf(1).equals(task.getDiscussionEnabled())) {
            throw new ServiceException("该任务未开启讨论区");
        }
    }

    private void requireNotEnded(LearningTask task) {
        if (LearningTask.STATUS_ENDED.equals(task.getStatus())) {
            throw new ServiceException("任务已结束，讨论区已归档，不能再发言");
        }
    }

    /** 岗位必须属于当前用户的部门 */
    private void requireOwnPosition(Long positionId) {
        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null) {
            throw new ServiceException("当前账号未配置部门");
        }
        if (positionId == null || taskMapper.countDeptPosition(deptId, positionId) == 0) {
            throw new ServiceException("该岗位不属于你的部门");
        }
    }

    private String displayName(TaskPost p) {
        if (p == null) {
            return null;
        }
        if (p.getNickName() != null && !p.getNickName().trim().isEmpty()) {
            return p.getNickName();
        }
        return p.getUserName();
    }

    private boolean isSuper() {
        return SecurityUtils.hasRole("SUPER_ADMIN") || SecurityUtils.isAdmin(SecurityUtils.getUserId());
    }

    private boolean isIntern() {
        return SecurityUtils.hasRole("PRE_TRAINEE") || SecurityUtils.hasRole("FORMAL_TRAINEE");
    }
}
