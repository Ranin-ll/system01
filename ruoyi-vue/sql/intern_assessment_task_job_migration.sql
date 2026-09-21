-- ============================================================
-- 融谷 · 实习生学习考核系统
-- P3：注册「任务逾期标记 / 到期提醒」两个定时任务（平台 Quartz）
--
-- 说明：
--   · status: '0' = 正常（启用），'1' = 暂停 —— RuoYi sys_job 的既有口径
--   · job_group: 'DEFAULT'
--   · misfire_policy: '1' = 立即执行；concurrent: '1' = 禁止并发
--   · 超管可在「系统监控 › 定时任务」里暂停 / 改 cron / 立即执行一次
--
-- 幂等：按 job_name 判重，可重复执行（不会重复插入，也不会覆盖你改过的 cron）。
-- ============================================================

INSERT INTO `sys_job` (`job_name`, `job_group`, `invoke_target`, `cron_expression`,
                       `misfire_policy`, `concurrent`, `status`, `create_by`, `create_time`, `remark`)
SELECT '任务逾期标记', 'DEFAULT', 'taskOverdueJob.markOverdue()', '0 10 0 * * ?',
       '1', '1', '0', 'admin', NOW(), '每日 00:10：截止已过且未完成的分配置为 OVERDUE，并催办本人与批阅人'
WHERE NOT EXISTS (SELECT 1 FROM `sys_job` WHERE `job_name` = '任务逾期标记');

INSERT INTO `sys_job` (`job_name`, `job_group`, `invoke_target`, `cron_expression`,
                       `misfire_policy`, `concurrent`, `status`, `create_by`, `create_time`, `remark`)
SELECT '任务到期提醒', 'DEFAULT', 'taskOverdueJob.remindDueSoon()', '0 0 9 * * ?',
       '1', '1', '0', 'admin', NOW(), '每日 09:00：剩 3 天内且尚未提交的分配，提醒本人'
WHERE NOT EXISTS (SELECT 1 FROM `sys_job` WHERE `job_name` = '任务到期提醒');
