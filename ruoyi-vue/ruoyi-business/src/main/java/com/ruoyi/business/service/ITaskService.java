package com.ruoyi.business.service;

import com.ruoyi.business.domain.LearningTask;
import com.ruoyi.business.domain.TaskSubmission;

import java.util.List;
import java.util.Map;

/**
 * 学习任务 Service（P2）
 *
 * <p>能力口径（与消息一致）：<b>超管只读</b>；部门管理员限本部门岗位，可建/发布/结束/批阅；
 * 实习生只能看与提交<b>自己被分配</b>的任务。</p>
 *
 * <p><b>发布是一个事务</b>：任务状态 PUBLISHED + 按岗位展开 {@code task_assignment} + 落一条 TASK 通知。
 * 三者任一失败即回滚 —— 避免出现「通知发了但任务没分配」。</p>
 *
 * @author ruoyi
 */
public interface ITaskService {

    /** 任务列表（超管看全部；部门管理员看本部门岗位） */
    List<LearningTask> list(LearningTask query);

    /**
     * 本部门在培实习生（含待转正）
     *
     * <p>「任务管理 / 通知管理 / 任务批阅」三页的「按实习生筛选」都靠它。
     * <b>不要用 `/system/user/list`</b> —— 那个要 {@code system:user:list} 权限，部门管理员是 403。</p>
     */
    List<Map<String, Object>> deptTrainees();

    /** 新建 / 修改（部门管理员） */
    LearningTask save(LearningTask task);

    /** 发布 / 结束（发布时会展开分配并通知） */
    void changeStatus(Long id, String status);

    /** 我的任务（实习生） */
    List<LearningTask> myTasks();

    /** 任务详情（按角色校验可见性） */
    LearningTask detail(Long id);

    /** 提交情况看板（含未提交者） */
    List<Map<String, Object>> assignmentBoard(Long taskId);

    /**
     * 某个分配的<b>历次提交</b>（批阅记录）
     *
     * <p>重复提交走 {@code version + 1}，旧版本不能丢 —— 批阅人要能看到
     * 「上一次被退回时交的是什么、当时给了什么批语」。</p>
     */
    List<Map<String, Object>> submissionHistory(Long assignmentId);

    /** 提交作业（实习生；重复提交走 version+1） */
    Map<String, Object> submit(Long taskId, TaskSubmission body);

    /** 批阅（部门管理员）：写批语 + assignment 置 DONE + 回执通知实习生 */
    void review(TaskSubmission body, String passFlag);

    // ------------------------- P3：供定时任务调用（无用户上下文） -------------------------

    /**
     * 逾期标记：把截止已过且未完成的分配置为 {@code OVERDUE}，并给
     * <b>本人 + 批阅人</b>发一条催办消息（<code>msg_type='URGE'</code>，当天去重）。
     *
     * @return 本次标记的数量
     */
    int markOverdue();

    /**
     * 到期提醒：剩 {@code days} 天内且<b>尚未提交</b>的分配，给本人发提醒（当天去重）。
     *
     * @return 本次提醒的数量
     */
    int remindDueSoon(int days);
}
