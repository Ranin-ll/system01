package com.ruoyi.business.job;

import com.ruoyi.business.service.ITaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 任务逾期 / 到期 定时任务
 *
 * <p>由平台「系统监控 › 定时任务」调度（RuoYi Quartz），
 * invokeTarget 形如 <code>taskOverdueJob.markOverdue()</code>。
 * 迁移脚本 <code>intern_assessment_task_job_migration.sql</code> 会写入两条 sys_job，
 * 超管可在「系统监控 › 定时任务」里暂停 / 改 cron / 立即执行一次。</p>
 *
 * <p><b>为什么走 Service 而不是直接写 Mapper</b>：逾期判定、去重、通知内容都在 Service 里，
 * 定时任务只是触发器；这样「立即执行一次」与定时执行的路径完全一致。</p>
 *
 * @author ruoyi
 */
@Component("taskOverdueJob")
public class TaskOverdueJob {

    @Autowired
    private ITaskService taskService;

    /**
     * 逾期标记：把截止已过且未完成的分配置为 OVERDUE，并给本人 + 批阅人发催办（当天去重）
     */
    public void markOverdue() {
        int n = taskService.markOverdue();
        System.out.println("[定时任务] 逾期标记完成，本次标记 " + n + " 条分配");
    }

    /**
     * 到期提醒：剩 3 天内且尚未提交的分配，给本人发提醒（当天去重）
     */
    public void remindDueSoon() {
        int n = taskService.remindDueSoon(3);
        System.out.println("[定时任务] 到期提醒完成，本次提醒 " + n + " 人");
    }
}
