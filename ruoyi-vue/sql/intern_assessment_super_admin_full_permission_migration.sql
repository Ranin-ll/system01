-- ============================================================================
-- 超级管理员（SUPER_ADMIN / role_id=100）授予「最高权限」= 绑定 sys_menu 全部菜单
--
-- 背景（2026-09-22）：
--   同事调整权限表后，SUPER_ADMIN 角色只绑了 62/161 个菜单，
--   连 `system:user:list` 都没有 → `test_super_admin` 调 /business/super/personnel/*
--   直接 403「没有权限，请联系管理员授权」，超管页面也大面积打不开。
--
-- 为什么绑「全部菜单」就够：
--   1) 接口鉴权 `@PreAuthorize("@ss.hasPermi('xxx')")` 取的是
--      SysPermissionService.getMenuPermission(user)：非 user_id=1 的用户，
--      权限 = 其角色在 sys_role_menu 上绑到的 sys_menu.perms 之并集。
--      已核对：代码里 113 个 hasPermi 权限串 **全部** 存在于 sys_menu.perms，无遗漏，
--      因此「绑满 sys_menu」= 拿到所有接口权限。
--   2) 前端菜单(getRouters)同理按 sys_role_menu 下发。
--
-- 为什么不改 role_id=1（role_key='admin'）：
--   按 intern_assessment_disable_builtin_admin_role_migration.sql 的既定口径，
--   内置账号 admin(user_id=1) 走 SecurityUtils.isAdmin(userId==1) 身份旁路，
--   天然持有 `*:*:*` 与全部菜单，与 sys_role_menu 无关；
--   该角色是框架内置、被 checkRoleAllowed 保护，属治理标记，**不要给它加权限**。
--
-- 影响面：仅 sys_role_menu 增量插入；不改 sys_menu / sys_role / 用户角色关系。
-- 幂等：NOT EXISTS 去重，可反复执行；重复执行影响 0 行。
--
-- 回滚（恢复到执行前的 62 个菜单）：
--   DELETE rm FROM sys_role_menu rm
--     JOIN sys_menu m ON m.menu_id = rm.menu_id
--    WHERE rm.role_id = 100 AND m.menu_id NOT IN (<执行前保留的 menu_id 列表>);
--   —— 建议回滚前先用下方的「① 快照」把原 menu_id 列表导出留档。
-- ============================================================================

-- ① 执行前快照：记录当前绑定的 menu_id，便于回滚
SELECT GROUP_CONCAT(rm.menu_id ORDER BY rm.menu_id) AS super_admin_menu_ids_before
  FROM sys_role_menu rm
 WHERE rm.role_id = 100;

-- ② 前置体检：确认角色存在、且确实存在缺口
SELECT r.role_id,
       r.role_key,
       r.role_name,
       (SELECT COUNT(*) FROM sys_menu)                        AS menu_total,
       (SELECT COUNT(*) FROM sys_role_menu rm WHERE rm.role_id = r.role_id) AS menu_bound,
       (SELECT COUNT(*) FROM sys_menu) -
       (SELECT COUNT(*) FROM sys_role_menu rm WHERE rm.role_id = r.role_id) AS menu_missing
  FROM sys_role r
 WHERE r.role_id = 100;

-- ③ 补齐：把 sys_menu 里缺失的菜单全部绑给 SUPER_ADMIN（幂等）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 100, m.menu_id
  FROM sys_menu m
 WHERE NOT EXISTS (
       SELECT 1 FROM sys_role_menu rm
        WHERE rm.role_id = 100 AND rm.menu_id = m.menu_id);

-- ④ 后置校验：应 缺失=0
SELECT r.role_id,
       r.role_key,
       r.role_name,
       (SELECT COUNT(*) FROM sys_menu)                        AS menu_total,
       (SELECT COUNT(*) FROM sys_role_menu rm WHERE rm.role_id = r.role_id) AS menu_bound,
       (SELECT COUNT(*) FROM sys_menu) -
       (SELECT COUNT(*) FROM sys_role_menu rm WHERE rm.role_id = r.role_id) AS menu_missing
  FROM sys_role r
 WHERE r.role_id = 100;

-- ⑤ 逐角色总览（顺带看其它角色现状，便于向用户说明影响面）
SELECT r.role_id,
       r.role_key,
       r.role_name,
       r.status,
       (SELECT COUNT(*) FROM sys_role_menu rm WHERE rm.role_id = r.role_id) AS menu_bound,
       (SELECT COUNT(*) FROM sys_user_role ur WHERE ur.role_id = r.role_id) AS member_count
  FROM sys_role r
 ORDER BY r.role_id;
