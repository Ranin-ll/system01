-- ============================================================================
-- 超管端页面落地迁移（幂等，可重复执行）
--
-- 做三件事：
--   ① 把「学习考核 › 系统运营」下 5 个菜单从骨架页 assessment/index
--      改指向新的真实页面（views/super/**）；
--   ② 把「系统管理 / 系统监控 / 系统工具」整棵子树授给 SUPER_ADMIN 角色
--      （设计稿：系统管理权限仅超管持有；同时这些菜单行自带 system:* / monitor:* 权限串，
--        授权后超管才能读部门 / 角色 / 用户 / 操作日志）；
--   ③ 把「业务管理 › 岗位管理」授给 SUPER_ADMIN（组织岗位页需要 business:position:list）。
--
-- 不改动实习生端 / 部门管理员端任何菜单行，属「共同登记区」的追加式改动。
-- ============================================================================

USE `intern_assessment`;

-- ---------- ① 5 个菜单指向新页面 ----------
UPDATE `sys_menu` SET `component` = 'super/dashboard/index'      WHERE `menu_id` = 2044 AND `menu_name` = '全局工作台';
UPDATE `sys_menu` SET `component` = 'super/org/organization/index' WHERE `menu_id` = 2045 AND `menu_name` = '组织岗位';
UPDATE `sys_menu` SET `component` = 'super/org/roles/index'      WHERE `menu_id` = 2046 AND `menu_name` = '角色权限';
UPDATE `sys_menu` SET `component` = 'super/rule/index'           WHERE `menu_id` = 2047 AND `menu_name` = '系统规则';
UPDATE `sys_menu` SET `component` = 'super/audit/index'          WHERE `menu_id` = 2048 AND `menu_name` = '审计日志';

-- 目录名对齐设计稿语义（系统运营 → 超管运营）
UPDATE `sys_menu` SET `menu_name` = '超管运营' WHERE `menu_id` = 2021 AND `menu_name` = '系统运营';

-- ---------- ② 系统管理 / 监控 / 工具 子树授给 SUPER_ADMIN ----------
-- 一级目录本身
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 100, m.menu_id FROM `sys_menu` m
WHERE m.menu_id IN (1, 2, 3)
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.role_id = 100 AND rm.menu_id = m.menu_id);

-- 二级（系统管理/监控/工具 的直接子项，含其权限串）
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 100, m.menu_id FROM `sys_menu` m
WHERE m.parent_id IN (1, 2, 3)
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.role_id = 100 AND rm.menu_id = m.menu_id);

-- 三级（如「日志管理」下的操作日志 / 登录日志）
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 100, m.menu_id FROM `sys_menu` m
WHERE m.parent_id IN (SELECT menu_id FROM `sys_menu` WHERE parent_id IN (1, 2, 3))
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.role_id = 100 AND rm.menu_id = m.menu_id);

-- ---------- ③ 业务管理 › 岗位管理 授给 SUPER_ADMIN ----------
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 100, m.menu_id FROM `sys_menu` m
WHERE m.menu_id IN (2000, 2001)
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.role_id = 100 AND rm.menu_id = m.menu_id);
