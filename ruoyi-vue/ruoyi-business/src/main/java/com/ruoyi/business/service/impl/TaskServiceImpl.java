package com.ruoyi.business.service.impl;

import com.ruoyi.business.domain.LearningTask;
import com.ruoyi.business.domain.Notice;
import com.ruoyi.business.domain.TaskAssignment;
import com.ruoyi.business.domain.TaskSubmission;
import com.ruoyi.business.mapper.LearningTaskMapper;
import com.ruoyi.business.mapper.TaskAssignmentMapper;
import com.ruoyi.business.mapper.TaskSubmissionMapper;
import com.ruoyi.business.service.IMessageService;
import com.ruoyi.business.service.ITaskService;
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
 * 学习任务 Service 实现（P2）
 *
 * @author ruoyi
 */
@Service
public class TaskServiceImpl implements ITaskService {

    @Autowired
    private LearningTaskMapper taskMapper;

    @Autowired
    private TaskAssignmentMapper assignmentMapper;

    @Autowired
    private TaskSubmissionMapper submissionMapper;

    @Autowired
    private com.ruoyi.business.mapper.TaskTargetUserMapper taskTargetUserMapper;

    /** 通知统一走 MessageService 的发送通道（不在这里另写 notice 落库，保持触发点单一） */
    @Autowired
    private IMessageService messageService;

    // ------------------------------------------------------------------ 查询

    @Override
    public List<LearningTask> list(LearningTask query) {
        LearningTask q = query == null ? new LearningTask() : query;
        // 超管 scopeDeptId=null → 看全部；部门管理员传自己的部门
        Long scopeDeptId = isSuper() ? null : SecurityUtils.getDeptId();
        List<LearningTask> rows = taskMapper.selectTaskList(scopeDeptId, q.getPositionId(), q.getStatus(), q.getKeyword(), q.getUserId());
        rows.forEach(this::fillRemainDays);
        return rows;
    }

    @Override
    public List<Map<String, Object>> deptTrainees() {
        // 超管不限部门；其余按自己部门收范围
        Long deptId = isSuper() ? null : SecurityUtils.getDeptId();
        return taskMapper.selectDeptTrainees(deptId);
    }

    @Override
    public List<LearningTask> myTasks() {
        List<LearningTask> rows = taskMapper.selectMyTasks(SecurityUtils.getUserId());
        rows.forEach(this::fillRemainDays);
        return rows;
    }

    @Override
    public LearningTask detail(Long id) {
        if (id == null) {
            throw new ServiceException("任务ID不能为空");
        }
        LearningTask task = taskMapper.selectTaskDetail(id);
        if (task == null) {
            throw new ServiceException("任务不存在或已删除");
        }
        withTargetUsers(task);
        Long userId = SecurityUtils.getUserId();
        if (isSuper()) {
            fillRemainDays(task);
            return task;
        }
        if (isIntern()) {
            // 只能看被分配到的任务
            if (assignmentMapper.selectMine(id, userId) == null) {
                throw new ServiceException("任务不存在或未分配给你");
            }
        } else {
            // 部门管理员：任务必须属于本部门（带岗位看岗位、部门级任务看发布人部门）
            requireOwnTask(task);
        }
        fillRemainDays(task);
        return task;
    }

    @Override
    public List<Map<String, Object>> assignmentBoard(Long taskId) {
        LearningTask task = requireManageable(taskId);
        return taskMapper.selectAssignmentBoard(task.getId());
    }

    @Override
    public List<Map<String, Object>> submissionHistory(Long assignmentId) {
        requireDeptAdmin();
        if (assignmentId == null) {
            throw new ServiceException("分配ID不能为空");
        }
        TaskAssignment assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            throw new ServiceException("分配记录不存在");
        }
        // 必须先校验这条分配挂在「本部门可管的任务」下，否则等于能读任意部门的提交内容
        requireManageable(assignment.getTaskId());
        return taskMapper.selectSubmissionHistory(assignmentId);
    }

    // ------------------------------------------------------------------ 写

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LearningTask save(LearningTask task) {
        if (task == null || isBlank(task.getTaskName())) {
            throw new ServiceException("任务名称不能为空");
        }
        requireDeptAdmin();
        // 发布对象缺省 = 按岗位（与改动前行为一致）。★ 必须先归一化再判岗位，
        // 否则前端不传 assignScope 时会被误判成「非按岗位」而把 positionId 清掉。
        if (task.getAssignScope() == null || task.getAssignScope().trim().isEmpty()) {
            task.setAssignScope(LearningTask.SCOPE_POSITION);
        }
        // 只有「按岗位」发布的任务才绑定岗位；按部门 / 按人发的任务不属于某一个岗位
        // （硬塞岗位会让课程可见性、批阅归属等按岗位的口径失真）
        if (LearningTask.SCOPE_POSITION.equals(task.getAssignScope())) {
            if (task.getPositionId() == null) {
                throw new ServiceException("请选择适用岗位");
            }
            requireOwnPosition(task.getPositionId());
        } else {
            task.setPositionId(null);
        }

        Date now = new Date();
        if (task.getId() == null) {
            task.setPublisherId(SecurityUtils.getUserId());
            task.setReviewerId(task.getReviewerId() == null ? SecurityUtils.getUserId() : task.getReviewerId());
            task.setStatus(LearningTask.STATUS_DRAFT);
            task.setDeleted(0);
            task.setCreateTime(now);
            task.setUpdateTime(now);
            if (task.getNeedAssignment() == null) {
                task.setNeedAssignment(1);
            }
            if (task.getDiscussionEnabled() == null) {
                task.setDiscussionEnabled(0);
            }
            // 默认允许多次提交，与改动前行为一致
            if (task.getAllowResubmit() == null) {
                task.setAllowResubmit(1);
            }
            requireValidScope(task);
            taskMapper.insert(task);
            saveTargetUsers(task);
        } else {
            LearningTask db = requireManageable(task.getId());
            if (LearningTask.STATUS_ENDED.equals(db.getStatus())) {
                throw new ServiceException("已结束的任务不可修改");
            }
            if (LearningTask.STATUS_PUBLISHED.equals(db.getStatus())) {
                throw new ServiceException("已发布的任务请先结束，再修改");
            }
            task.setUpdateTime(now);
            if (task.getAssignScope() != null && !task.getAssignScope().trim().isEmpty()) {
                requireValidScope(task);
                // MyBatis-Plus 的 updateById 忽略 null 字段 → 「按岗位」改成「按部门/按人」时
                // 旧的 position_id 清不掉，必须显式置空（否则这条任务仍被算成属于旧岗位）
                if (!LearningTask.SCOPE_POSITION.equals(task.getAssignScope())) {
                    taskMapper.clearPosition(task.getId());
                }
            }
            taskMapper.updateById(task);
            // 名单整体替换（没传 targetUserIds 就不动，避免「只改标题」把名单清空）
            if (task.getTargetUserIds() != null) {
                saveTargetUsers(task);
            }
        }
        return withTargetUsers(taskMapper.selectTaskDetail(task.getId()));
    }

    /**
     * 回显「指定人员」名单 id（任务详情、保存返回都带上）。
     *
     * <p>为什么必须回显：编辑一条「指定人员」任务时，前端要拿 ids 把已勾的人重新选上。
     * 只回 {@code targetNames}（姓名串）不够 —— 前端表单里 {@code targetUserIds} 会是空数组，
     * 用户即使只改个标题，保存也会把名单整体清空，再发布就报「没有指定任何实习生」。</p>
     */
    private LearningTask withTargetUsers(LearningTask task) {
        if (task != null && LearningTask.SCOPE_USER.equals(task.getAssignScope())) {
            task.setTargetUserIds(taskTargetUserMapper.selectUserIds(task.getId()));
        }
        return task;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, String status) {
        requireDeptAdmin();
        LearningTask task = requireManageable(id);
        String target = status == null ? "" : status.trim().toUpperCase();
        if (!LearningTask.STATUS_PUBLISHED.equals(target) && !LearningTask.STATUS_ENDED.equals(target)) {
            throw new ServiceException("只支持 PUBLISHED / ENDED");
        }
        if (LearningTask.STATUS_PUBLISHED.equals(target)) {
            if (LearningTask.STATUS_PUBLISHED.equals(task.getStatus())) {
                throw new ServiceException("该任务已发布");
            }
            // ① 改状态
            LearningTask upd = new LearningTask();
            upd.setId(task.getId());
            upd.setStatus(LearningTask.STATUS_PUBLISHED);
            upd.setUpdateTime(new Date());
            taskMapper.updateById(upd);

            // ② 展开分配（唯一键 + INSERT IGNORE → 幂等）
            //    按部门 = 本部门全部在培实习生；按岗位 = 该岗位全部；指定人员 = 只发名单里的人
            List<Long> candidates;
            if (LearningTask.SCOPE_USER.equals(task.getAssignScope())) {
                candidates = taskTargetUserMapper.selectUserIds(task.getId());
            } else if (LearningTask.SCOPE_DEPT.equals(task.getAssignScope())) {
                // 部门级任务：归属部门 = 发布人所属部门（任务本身不带 position_id）
                candidates = taskMapper.selectCandidateUserIdsByDept(
                        task.getPublisherDeptId() == null ? SecurityUtils.getDeptId() : task.getPublisherDeptId());
            } else {
                candidates = taskMapper.selectCandidateUserIds(task.getPositionId());
            }
            if (candidates.isEmpty()) {
                String scope = task.getAssignScope();
                throw new ServiceException(LearningTask.SCOPE_USER.equals(scope)
                        ? "该任务没有指定任何实习生，请先在草稿里选择要发送的人"
                        : (LearningTask.SCOPE_DEPT.equals(scope)
                            ? "本部门下没有在培实习生，无需发布"
                            : "该岗位下没有在培实习生，无需发布"));
            }
            int assigned = 0;
            for (Long uid : candidates) {
                assigned += assignmentMapper.assignIgnore(task.getId(), uid);
            }

            // ③ 落一条 TASK 通知（走统一发送通道）
            //
            // ★ 定向发给「被分配到的人」，而不是整部门广播（SCOPE_DEPT）：
            //   通知受众必须等于任务受众。按部门广播时会出两类假通知 ——
            //   ① 同部门但岗位不同的实习生；② 任务发布之后才注册进来的实习生。
            //   他们都会收到「新任务：xxx」，点「去处理」进「我的任务」却是一片空白
            //   （因为 task_assignment 是在发布那一刻按岗位展开的，他们不在名单里）。
            Notice notice = new Notice();
            notice.setMsgType("TASK");
            notice.setScopeType(Notice.SCOPE_USER);
            notice.setTargetUserIds(candidates);
            notice.setBizType("TASK");
            notice.setBizId(task.getId());
            notice.setTitle("新任务：" + task.getTaskName());
            notice.setContent(buildTaskContent(task));
            messageService.send(notice);
            return;
        }
        // 结束
        LearningTask upd = new LearningTask();
        upd.setId(task.getId());
        upd.setStatus(LearningTask.STATUS_ENDED);
        upd.setUpdateTime(new Date());
        taskMapper.updateById(upd);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submit(Long taskId, TaskSubmission body) {
        if (!isIntern()) {
            throw new ServiceException("只有实习生可以提交作业");
        }
        if (body == null || (isBlank(body.getContent()) && isBlank(body.getFileUrl()))) {
            throw new ServiceException("作业内容与附件至少填一个");
        }
        LearningTask task = taskMapper.selectTaskDetail(taskId);
        if (task == null) {
            throw new ServiceException("任务不存在或已删除");
        }
        if (!LearningTask.STATUS_PUBLISHED.equals(task.getStatus())) {
            throw new ServiceException("任务未发布或已结束，无法提交");
        }
        Long userId = SecurityUtils.getUserId();
        TaskAssignment assignment = assignmentMapper.selectMine(taskId, userId);
        if (assignment == null) {
            throw new ServiceException("该任务未分配给你");
        }
        // 逾期判定：截止时间已过 → 记为 OVERDUE，但仍接收（可补交）
        boolean overdue = task.getDeadline() != null && task.getDeadline().before(new Date());
        TaskSubmission last = submissionMapper.selectLatest(assignment.getId());

        // 只允许提交一次的任务：已交过就挡住 —— 但**被退回的例外**（退回本身就是要求重做）
        if (!Integer.valueOf(1).equals(task.getAllowResubmit())
                && last != null && !TaskSubmission.REVIEW_REJECTED.equals(last.getReviewStatus())) {
            throw new ServiceException("该任务只允许提交一次，你已提交过（若被退回则可以重新提交）");
        }

        Date now = new Date();
        TaskSubmission sub = new TaskSubmission();
        sub.setAssignmentId(assignment.getId());
        sub.setTaskId(taskId);
        sub.setUserId(userId);
        sub.setContent(body.getContent());
        sub.setFileUrl(body.getFileUrl());
        sub.setFileName(body.getFileName());
        sub.setSubmitTime(now);
        sub.setStatus(overdue ? TaskSubmission.OVERDUE : TaskSubmission.SUBMITTED);
        sub.setReviewStatus("PENDING");
        sub.setVersion(last == null || last.getVersion() == null ? 1 : last.getVersion() + 1);
        sub.setCreateTime(now);
        sub.setUpdateTime(now);
        submissionMapper.insert(sub);

        // ★ 内容只保留最近一次：把更早版本的内容与附件清掉（行保留 → 提交次数与批阅记录完整）
        clearOlderSubmissions(assignment.getId(), sub.getId());

        // 分配状态推进
        TaskAssignment updA = new TaskAssignment();
        updA.setId(assignment.getId());
        updA.setStatus(TaskAssignment.IN_PROGRESS);
        assignmentMapper.updateById(updA);

        // 通知批阅人
        Long reviewer = task.getReviewerId();
        if (reviewer != null && !reviewer.equals(userId)) {
            Notice notice = new Notice();
            notice.setMsgType("TASK");
            notice.setScopeType(Notice.SCOPE_USER);
            notice.setTargetUserIds(java.util.Collections.singletonList(reviewer));
            notice.setBizType("TASK");
            notice.setBizId(taskId);
            notice.setTitle("有新的作业待批阅：" + task.getTaskName());
            notice.setContent(SecurityUtils.getUsername() + " 提交了作业，请及时批阅。");
            try {
                messageService.send(notice);
            } catch (Exception ignore) {
                // 通知失败不影响提交本身（跨部门等边界情况已由 send 的校验兜住）
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("submissionId", sub.getId());
        result.put("version", sub.getVersion());
        result.put("status", sub.getStatus());
        result.put("overdue", overdue);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void review(TaskSubmission body, String passFlag) {
        requireDeptAdmin();
        if (body == null || body.getId() == null) {
            throw new ServiceException("请选择要批阅的提交记录");
        }
        TaskSubmission db = submissionMapper.selectById(body.getId());
        if (db == null) {
            throw new ServiceException("提交记录不存在");
        }
        LearningTask task = requireManageable(db.getTaskId());

        Date now = new Date();
        boolean passed = !"REJECT".equalsIgnoreCase(passFlag == null ? "" : passFlag.trim());
        TaskSubmission upd = new TaskSubmission();
        upd.setId(db.getId());
        upd.setStatus(TaskSubmission.REVIEWED);
        upd.setReviewStatus(passed ? "PASSED" : "REJECTED");
        upd.setReviewComment(body.getReviewComment());
        upd.setReviewerId(SecurityUtils.getUserId());
        upd.setReviewTime(now);
        upd.setUpdateTime(now);
        submissionMapper.updateById(upd);

        // 通过 → 分配置 DONE；退回 → 回到进行中，允许重交
        TaskAssignment updA = new TaskAssignment();
        updA.setId(db.getAssignmentId());
        updA.setStatus(passed ? TaskAssignment.DONE : TaskAssignment.IN_PROGRESS);
        updA.setFinishTime(passed ? now : null);
        assignmentMapper.updateById(updA);

        // 回执给实习生（走统一发送通道）
        Notice notice = new Notice();
        notice.setMsgType("TASK");
        notice.setScopeType(Notice.SCOPE_USER);
        notice.setTargetUserIds(java.util.Collections.singletonList(db.getUserId()));
        notice.setBizType("TASK");
        notice.setBizId(task.getId());
        notice.setTitle((passed ? "任务已通过：" : "任务被退回：") + task.getTaskName());
        notice.setContent(passed
                ? "你的作业已通过批阅。" + (isBlank(upd.getReviewComment()) ? "" : "批语：" + upd.getReviewComment())
                : "你的作业被退回，请修改后重新提交。" + (isBlank(upd.getReviewComment()) ? "" : "批语：" + upd.getReviewComment()));
        messageService.send(notice);
    }

    // ------------------------------------------------------------------ P3 定时任务

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int markOverdue() {
        List<Map<String, Object>> rows = taskMapper.selectOverdueAssignments();
        int marked = 0;

        // ★ 2026-09-20 重构：原来「实习生 + reviewer(部门管理员)」共用一条通知，
        //   文案是给实习生的（"请尽快补交"），部门管理员看到很怪；
        //   且同一任务有多个逾期人时，reviewer 会**重复收到多条**。
        //   现在：① 实习生**每天最多一条**（首日起，提交后停止）；② reviewer 仅在**首次逾期**时收一条（按任务合并）。
        //   受众与频率：实习生=每日常规提醒，管理员=一次性知会（有督办看板兜底，不每日打扰）。
        Map<Long, List<String>> newOverdueByTask = new java.util.LinkedHashMap<>();
        Map<Long, Long> reviewerByTask = new java.util.LinkedHashMap<>();
        Map<Long, String> taskNameById = new java.util.LinkedHashMap<>();

        for (Map<String, Object> r : rows) {
            Long assignmentId = toLong(r.get("assignmentId"));
            Long taskId = toLong(r.get("taskId"));
            Long userId = toLong(r.get("userId"));
            Long reviewerId = toLong(r.get("reviewerId"));
            String taskName = r.get("taskName") == null ? "学习任务" : String.valueOf(r.get("taskName"));
            String userName = r.get("userName") == null ? "该实习生" : String.valueOf(r.get("userName"));

            // ★ 状态推进：只有「本次从非逾期变成逾期」才返回 1（SQL 已排除 OVERDUE），幂等。
            //   注意：这里**不再 continue** —— 已是 OVERDUE 的行也必须往下走，
            //   因为实习生侧要「每天最多提醒一次」（见 ①）。
            boolean newlyOverdue = assignmentMapper.markOverdue(assignmentId) > 0;
            if (newlyOverdue) {
                marked++;
            }

            // ① 给实习生本人：**每天最多一条**（countNotifiedToday 按「当天 + 任务 + 收件人」去重，
            //   所以首日会发、之后每天也各发一次；当天重复执行定时任务不会刷屏）。
            //   文案是「你的…请尽快补交」。
            if (userId != null && taskMapper.countNotifiedToday(taskId, userId, "URGE") == 0) {
                Notice mine = new Notice();
                mine.setMsgType("URGE");
                mine.setScopeType(Notice.SCOPE_USER);
                mine.setTargetUserIds(java.util.Collections.singletonList(userId));
                mine.setBizType("TASK");
                mine.setBizId(taskId);
                mine.setTitle("你的任务已逾期：" + taskName);
                mine.setContent("该任务已超过截止时间，请尽快补交。逾期仍可提交，但会记入培养评价。"
                        + "（此提醒每天最多一次，提交后自动停止。）");
                messageService.publishSystem(mine);
            }

            // ② 收集给 reviewer（批阅人/部门管理员）的：同一任务合并，姓名去重
            //   ★ 仅「首次逾期」那一次通知管理员 —— 不随实习生的每日提醒一起打扰他。
            if (newlyOverdue && reviewerId != null && !reviewerId.equals(userId)) {
                List<String> names = newOverdueByTask.get(taskId);
                if (names == null) {
                    names = new java.util.ArrayList<>();
                    newOverdueByTask.put(taskId, names);
                    reviewerByTask.put(taskId, reviewerId);
                    taskNameById.put(taskId, taskName);
                }
                if (!names.contains(userName)) {
                    names.add(userName);
                }
            }
        }

        // ③ 给每个任务的 reviewer 发一条（列出本次新逾期的所有人；同一任务每天最多一次）
        for (Map.Entry<Long, List<String>> e : newOverdueByTask.entrySet()) {
            Long taskId = e.getKey();
            List<String> names = e.getValue();
            Long reviewerId = reviewerByTask.get(taskId);
            if (reviewerId == null || taskMapper.countNotifiedToday(taskId, reviewerId, "URGE") > 0) {
                continue;
            }
            String who = String.join("、", names);
            String name = taskNameById.get(taskId);
            Notice toReviewer = new Notice();
            toReviewer.setMsgType("URGE");
            toReviewer.setScopeType(Notice.SCOPE_USER);
            toReviewer.setTargetUserIds(java.util.Collections.singletonList(reviewerId));
            toReviewer.setBizType("TASK");
            toReviewer.setBizId(taskId);
            toReviewer.setTitle(names.size() > 1
                    ? (names.size() + " 人的任务已逾期：" + name)
                    : (who + " 的任务已逾期：" + name));
            toReviewer.setContent(who + " 的该任务已超过截止时间，请督促尽快补交。");
            // 定时任务没有用户上下文 → 走系统发布通道（不做权限校验）
            messageService.publishSystem(toReviewer);
        }
        return marked;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int remindDueSoon(int days) {
        List<Map<String, Object>> rows = taskMapper.selectDueSoonAssignments(days);
        int reminded = 0;
        for (Map<String, Object> r : rows) {
            Long taskId = toLong(r.get("taskId"));
            Long userId = toLong(r.get("userId"));
            String taskName = r.get("taskName") == null ? "学习任务" : String.valueOf(r.get("taskName"));
            if (taskMapper.countNotifiedToday(taskId, userId, "TASK") > 0) {
                continue;
            }
            Notice notice = new Notice();
            notice.setMsgType("TASK");
            notice.setScopeType(Notice.SCOPE_USER);
            notice.setTargetUserIds(java.util.Collections.singletonList(userId));
            notice.setBizType("TASK");
            notice.setBizId(taskId);
            notice.setTitle("任务即将到期：" + taskName);
            notice.setContent("距离截止不到 " + days + " 天，请及时提交作业。");
            messageService.publishSystem(notice);
            reminded++;
        }
        return reminded;
    }

    private Long toLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        try {
            return Long.valueOf(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ 内部

    /**
     * 校验「发布对象」：指定人员时名单不能为空，且每个都必须是<b>该岗位</b>的在培实习生。
     *
     * <p>为什么必须限定在该岗位：岗位决定课程可见性、批阅归属等既有口径，
     * 让「开发实习生」收到一条设计岗的任务会把这些口径打乱。</p>
     */
    private void requireValidScope(LearningTask task) {
        if (!LearningTask.SCOPE_USER.equals(task.getAssignScope())) {
            // DEPT / POSITION 都不带名单；非法值统一收敛到 POSITION
            if (!LearningTask.SCOPE_DEPT.equals(task.getAssignScope())) {
                task.setAssignScope(LearningTask.SCOPE_POSITION);
            }
            return;
        }
        List<Long> ids = task.getTargetUserIds();
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("已选择「指定人员」，请至少勾选一位实习生");
        }
        // 指定人员必须是「本部门在培实习生」（不限岗位 —— 任务本身不绑定岗位）
        List<Long> allowed = deptCandidateUserIds();
        for (Long uid : ids) {
            if (uid != null && !allowed.contains(uid)) {
                throw new ServiceException("指定人员里含有不属于本部门的在培实习生，请重新选择");
            }
        }
    }

    /** 本部门在培实习生（按部门发布的展开对象，也是「指定人员」的可选范围） */
    private List<Long> deptCandidateUserIds() {
        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null) {
            throw new ServiceException("当前账号未配置部门");
        }
        return taskMapper.selectCandidateUserIdsByDept(deptId);
    }

    /** 整体替换指定人员名单（唯一键 + INSERT IGNORE → 幂等） */
    private void saveTargetUsers(LearningTask task) {
        taskTargetUserMapper.deleteByTask(task.getId());
        if (task.getTargetUserIds() == null) {
            return;
        }
        for (Long uid : task.getTargetUserIds()) {
            if (uid != null) {
                taskTargetUserMapper.insertIgnore(task.getId(), uid);
            }
        }
    }

    /**
     * 清空更早版本的提交内容（只保留最近一次）
     *
     * <p>做法：<b>保留行、清掉内容</b>，所以「交了 N 次」（行数）与「当时的批阅结果/批语」都还在，
     * 只是不再堆积旧内容与旧附件。磁盘上的旧附件也一并删除，避免留孤儿文件。</p>
     */
    private void clearOlderSubmissions(Long assignmentId, Long keepId) {
        List<String> oldUrls = submissionMapper.selectOlderFileUrls(assignmentId, keepId);
        int cleared = submissionMapper.clearOlderContent(assignmentId, keepId);
        if (cleared > 0) {
            for (String url : oldUrls) {
                deleteStoredFile(url);
            }
        }
    }

    /** 只删 profile 根目录内由本系统上传的文件，拒绝远程地址与路径穿越 */
    private void deleteStoredFile(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith(com.ruoyi.common.constant.Constants.RESOURCE_PREFIX + "/")) {
            return;
        }
        try {
            String relative = fileUrl.substring((com.ruoyi.common.constant.Constants.RESOURCE_PREFIX + "/").length());
            java.nio.file.Path root = java.nio.file.Paths.get(com.ruoyi.common.config.RuoYiConfig.getProfile())
                    .toAbsolutePath().normalize();
            java.nio.file.Path file = root.resolve(relative).normalize();
            if (file.startsWith(root)) {
                java.nio.file.Files.deleteIfExists(file);
            }
        } catch (Exception ignored) {
            // 清理失败不影响提交本身
        }
    }

    private void fillRemainDays(LearningTask t) {
        if (t == null || t.getDeadline() == null) {
            return;
        }
        long diff = t.getDeadline().getTime() - System.currentTimeMillis();
        t.setRemainDays((int) Math.floor(diff / 86400000.0));
    }

    private String buildTaskContent(LearningTask task) {
        StringBuilder sb = new StringBuilder();
        if (!isBlank(task.getTarget())) {
            sb.append("任务目标：").append(task.getTarget()).append('\n');
        }
        if (task.getDeadline() != null) {
            sb.append("截止时间：").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(task.getDeadline()));
        }
        if (isBlank(sb.toString())) {
            sb.append("请前往「消息中心 › 我的任务」查看详情并按时提交。");
        }
        return sb.toString();
    }

    /**
     * 任务必须存在，且当前部门管理员有管理权。
     *
     * <p>归属判定分两种：<b>带岗位</b>的任务看岗位是否属于本部门；
     * <b>不带岗位</b>的部门级任务（按部门 / 按人发布）看<b>发布人是否属于本部门</b>。</p>
     */
    private LearningTask requireManageable(Long taskId) {
        if (taskId == null) {
            throw new ServiceException("任务ID不能为空");
        }
        LearningTask task = taskMapper.selectTaskDetail(taskId);
        if (task == null) {
            throw new ServiceException("任务不存在或已删除");
        }
        requireOwnTask(task);
        return task;
    }

    /** 部门管理员的归属校验：带岗位按岗位判，部门级任务按发布人所属部门判 */
    private void requireOwnTask(LearningTask task) {
        if (task.getPositionId() != null) {
            requireOwnPosition(task.getPositionId());
            return;
        }
        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null) {
            throw new ServiceException("当前账号未配置部门");
        }
        if (task.getPublisherDeptId() == null || !deptId.equals(task.getPublisherDeptId())) {
            throw new ServiceException("该任务不属于你的部门");
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

    private void requireDeptAdmin() {
        if (isSuper()) {
            throw new ServiceException("超级管理员仅可查看任务，不能发布或批阅");
        }
        if (isIntern()) {
            throw new ServiceException("实习生无权执行该操作");
        }
    }

    private boolean isSuper() {
        return SecurityUtils.hasRole("SUPER_ADMIN") || SecurityUtils.isAdmin(SecurityUtils.getUserId());
    }

    private boolean isIntern() {
        return SecurityUtils.hasRole("PRE_TRAINEE") || SecurityUtils.hasRole("FORMAL_TRAINEE");
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
