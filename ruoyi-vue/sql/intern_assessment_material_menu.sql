-- ============================================================================
-- 备考资料（material）菜单与按钮权限
--
-- 说明：备考资料是「模拟备考管理」页（study/prep）内的一个页签，前端为静态路由，
--       无需新建独立页面菜单；这里只补 REST 接口所需的按钮权限标识
--       business:material:*，并授权给部门管理员与超级管理员。
-- 实习生端只读接口复用已有 business:bank:list 权限，无需新增。
-- ============================================================================
USE `intern_assessment`;

SET @assessment_id = (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'assessment' LIMIT 1);
SET @department_id = (SELECT menu_id FROM sys_menu WHERE parent_id = @assessment_id AND path = 'department' LIMIT 1);

-- 1. 备考资料按钮权限（挂在「部门运营」目录下，menu_type=F 仅作权限标识）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '备考资料列表', @department_id, 9, '#', '', 1, 0, 'F', '0', '0', 'business:material:list', '#', 'admin', NOW(), ''
WHERE @department_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @department_id AND perms = 'business:material:list');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '备考资料查询', @department_id, 10, '#', '', 1, 0, 'F', '0', '0', 'business:material:query', '#', 'admin', NOW(), ''
WHERE @department_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @department_id AND perms = 'business:material:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '备考资料新增', @department_id, 11, '#', '', 1, 0, 'F', '0', '0', 'business:material:add', '#', 'admin', NOW(), ''
WHERE @department_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @department_id AND perms = 'business:material:add');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '备考资料修改', @department_id, 12, '#', '', 1, 0, 'F', '0', '0', 'business:material:edit', '#', 'admin', NOW(), ''
WHERE @department_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @department_id AND perms = 'business:material:edit');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '备考资料删除', @department_id, 13, '#', '', 1, 0, 'F', '0', '0', 'business:material:remove', '#', 'admin', NOW(), ''
WHERE @department_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @department_id AND perms = 'business:material:remove');

-- 2. 角色授权：部门管理员 + 超级管理员
SET @super_role_id = (SELECT role_id FROM sys_role WHERE role_key = 'SUPER_ADMIN' AND del_flag = '0' LIMIT 1);
SET @dept_role_id  = (SELECT role_id FROM sys_role WHERE role_key = 'DEPT_ADMIN' AND del_flag = '0' LIMIT 1);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role_id, menu_id
FROM sys_menu, (SELECT @dept_role_id AS role_id) r
WHERE menu_id IN (
      SELECT menu_id FROM sys_menu WHERE parent_id = @department_id AND perms LIKE 'business:material:%')
  AND r.role_id IS NOT NULL;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role_id, menu_id
FROM sys_menu, (SELECT @super_role_id AS role_id) r
WHERE menu_id IN (
      SELECT menu_id FROM sys_menu WHERE parent_id = @department_id AND perms LIKE 'business:material:%')
  AND r.role_id IS NOT NULL;

SELECT '备考资料权限已配置' AS result, COUNT(*) AS 权限条数
FROM sys_menu WHERE parent_id = @department_id AND perms LIKE 'business:material:%';
