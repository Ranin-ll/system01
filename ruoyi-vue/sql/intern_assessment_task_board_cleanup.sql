-- =====================================================================
-- 清理「无实习生角色的账号」遗留在任务体系里的脏数据（幂等，可重复执行）
-- ---------------------------------------------------------------------
-- 背景：task_assignment 是**任务发布那一刻**按岗位展开的**快照**。
--       2026-09-18 之前，3 个无任何角色的遗留测试号（116/122/123）被铺进了
--       任务 9「001」的分配行，于是「提交与批阅」看板一直显示
--       「测试超级管理员 / 测试预备实习生 / 测试正式实习生 未开始」，
--       任务卡也显示「分配 5 人」（实际在培实习生只有 2 人）。
--       账号已由 intern_assessment_legacy_account_cleanup.sql 停用。
--
-- 安全性（已核对）：
--   * 这 3 个账号**从未提交过任何作业**（task_submission 0 行）→ 删分配行不丢数据；
--   * 只删 user 侧不存在 PRE_TRAINEE/FORMAL_TRAINEE 角色的行，
--     真实实习生的分配行（含已提交、已批阅的）一律不动；
--   * notice_target 只删「收件人无实习生角色」的通知，
--     评审人/管理员收到的「待批阅」通知不受影响（他们是 DEPT_ADMIN/SUPER_ADMIN）。
-- =====================================================================

-- ① 删掉指向「无实习生角色账号」的任务分配行
DELETE a FROM task_assignment a
 WHERE NOT EXISTS (
         SELECT 1 FROM sys_user_role ur JOIN sys_role r ON r.role_id = ur.role_id
          WHERE ur.user_id = a.user_id AND r.role_key IN ('PRE_TRAINEE', 'FORMAL_TRAINEE'));

-- ② 删掉发给这些账号的通知对象行（通知主表按需保留，避免误删别人的通知）
DELETE nt FROM notice_target nt
 WHERE NOT EXISTS (
         SELECT 1 FROM sys_user_role ur JOIN sys_role r ON r.role_id = ur.role_id
          WHERE ur.user_id = nt.user_id AND r.role_key IN ('PRE_TRAINEE', 'FORMAL_TRAINEE'))
   AND nt.user_id IN (SELECT u.user_id FROM sys_user u
                       WHERE u.user_status IN ('DISABLED', 'ARCHIVED'));

-- 自检：期望两条都为 0
-- SELECT COUNT(*) FROM task_assignment a WHERE NOT EXISTS (
--          SELECT 1 FROM sys_user_role ur JOIN sys_role r ON r.role_id = ur.role_id
--           WHERE ur.user_id = a.user_id AND r.role_key IN ('PRE_TRAINEE','FORMAL_TRAINEE'));
-- SELECT COUNT(*) FROM notice_target nt WHERE NOT EXISTS (
--          SELECT 1 FROM sys_user_role ur JOIN sys_role r ON r.role_id = ur.role_id
--           WHERE ur.user_id = nt.user_id AND r.role_key IN ('PRE_TRAINEE','FORMAL_TRAINEE'))
--    AND nt.user_id IN (SELECT user_id FROM sys_user WHERE user_status IN ('DISABLED','ARCHIVED'));
