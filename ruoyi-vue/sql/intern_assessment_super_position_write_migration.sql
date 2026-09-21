-- ============================================================================
-- 超管可维护「岗位」（2026-09-20）
--
-- 背景：SUPER_ADMIN 此前对业务岗位表 position 只有 business:position:list，
--       缺 query / add / edit / remove —— 所以「新建岗位」会被 403 拦住。
--       而部门管理用的是 RuoYi 原生 system:dept:*，超管**本就是全套**，无需补。
--       （注意别被 system:post:* 迷惑 —— 那是 RuoYi 自带的 sys_post 表，
--         本项目业务岗位用的是自定义表 position + business:position:* 权限。）
--
-- 口径：超管可维护部门（原生页）与岗位（业务页），并可维护两者的「绑定关系」
--       （/business/position/bindings，走 Service 层的 requireSuperAdmin 强校验）。
--
-- 幂等：NOT EXISTS 判重，重复执行影响 0 行。
-- ============================================================================

SET @super_role_id = (SELECT role_id FROM sys_role
                      WHERE role_key = 'SUPER_ADMIN' AND del_flag = '0' LIMIT 1);

SELECT IF(@super_role_id IS NULL,
          '【错误】未找到 SUPER_ADMIN 角色',
          CONCAT('SUPER_ADMIN role_id=', @super_role_id)) AS precheck;

-- 补岗位的 query / add / edit / remove
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @super_role_id, m.menu_id
FROM sys_menu m
WHERE m.perms IN ('business:position:query', 'business:position:add',
                  'business:position:edit', 'business:position:remove')
  AND @super_role_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm
                  WHERE rm.role_id = @super_role_id AND rm.menu_id = m.menu_id);

SELECT ROW_COUNT() AS inserted_rows;

-- 自检①：SUPER_ADMIN 岗位权限（期望 5 个：list/query/add/edit/remove）
SELECT GROUP_CONCAT(m.perms ORDER BY m.perms) AS super_position_perms
FROM sys_role_menu rm JOIN sys_menu m ON m.menu_id = rm.menu_id
WHERE rm.role_id = @super_role_id AND m.perms LIKE 'business:position%';

-- 自检②：SUPER_ADMIN 部门权限（原生，期望本就 5 个）
SELECT GROUP_CONCAT(m.perms ORDER BY m.perms) AS super_dept_perms
FROM sys_role_menu rm JOIN sys_menu m ON m.menu_id = rm.menu_id
WHERE rm.role_id = @super_role_id AND m.perms LIKE 'system:dept%';

-- 自检③：部门管理员的岗位权限不应受影响（应仍为不完整或原有集合）
SELECT r.role_key, GROUP_CONCAT(m.perms ORDER BY m.perms) AS dept_admin_position_perms
FROM sys_role r JOIN sys_role_menu rm ON rm.role_id = r.role_id
JOIN sys_menu m ON m.menu_id = rm.menu_id
WHERE r.role_key = 'DEPT_ADMIN' AND m.perms LIKE 'business:position%'
GROUP BY r.role_key;
