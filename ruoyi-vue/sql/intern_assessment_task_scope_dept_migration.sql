-- =====================================================================
-- 任务的「发布对象」增加 DEPT 档（幂等，可重复执行）
-- ---------------------------------------------------------------------
-- 背景：任务的发布对象与「通知管理」的发送范围对齐，变成三档：
--   DEPT     = 本部门全体在培实习生（任务**不绑定岗位**，position_id 为 NULL）
--   POSITION = 该岗位全部在培实习生（任务归属该岗位）
--   USER     = 只发给指定人员（本部门任意岗位的在培实习生，任务不绑定岗位）
--
-- 为什么只有 POSITION 档写 position_id：岗位决定课程可见性、批阅归属等既有口径；
-- 按部门 / 按人发的任务本来就不属于某一个岗位，硬塞岗位会让这些口径失真。
-- 那两档的归属改由「发布人所属部门」判定 —— 见 LearningTaskMapper 的部门范围条件：
--     t.position_id IN (本部门岗位) OR (t.position_id IS NULL AND 发布人属于本部门)
--
-- 说明：assign_scope 是 varchar(20)，`POSITION` 本来就存得下，**不需要改类型**，
--       本脚本只更新列注释，让 DDL 能自我说明。无数据需要回填（存量任务全是 POSITION）。
-- =====================================================================

ALTER TABLE `learning_task`
  MODIFY COLUMN `assign_scope` varchar(20) DEFAULT 'POSITION'
  COMMENT '发布对象：DEPT=本部门全体（不绑岗位）；POSITION=该岗位全部；USER=指定人员（不绑岗位）';

-- 自检：存量任务应全部仍在岗位档（若有 DEPT/USER 但 position_id 不为空，说明要么是"指定人员且当时绑了岗位"的历史数据，
--       要么是漏清了岗位 —— 后者可用下面这条找出来人工确认）
-- SELECT id, task_name, assign_scope, position_id FROM learning_task WHERE deleted = 0 ORDER BY id;
