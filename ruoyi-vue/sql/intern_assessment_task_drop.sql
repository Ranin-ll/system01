-- =============================================================
-- 任务系统 6 张业务表 DROP 脚本
-- 数据库：intern_assessment
-- 说明：任务系统已整体废弃，管理员仅保留「发通知」。本脚本幂等，
--       仅删除任务域相关的 6 张表，不触碰通知/消息/考核等其它表。
-- 表清单：
--   1. learning_task       学习任务表
--   2. task_assignment     学习任务分配表
--   3. task_submission     作业提交与批阅表
--   4. task_post           学习任务讨论区帖子表
--   5. task_attachment     任务附件表
--   6. task_target_user    任务目标用户表
-- 执行前请先备份：mysqldump -uroot -p intern_assessment > backup.sql
-- =============================================================

DROP TABLE IF EXISTS `task_attachment`;
DROP TABLE IF EXISTS `task_target_user`;
DROP TABLE IF EXISTS `task_post`;
DROP TABLE IF EXISTS `task_submission`;
DROP TABLE IF EXISTS `task_assignment`;
DROP TABLE IF EXISTS `learning_task`;
