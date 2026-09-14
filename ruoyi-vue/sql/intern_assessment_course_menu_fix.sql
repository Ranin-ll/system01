-- ============================================================================
-- 部门管理员课程管理菜单迁移
-- 作用：将课程管理迁移到“学习考核 / 部门运营”，并补齐角色权限。
-- 可重复执行，不依赖固定自增 ID。
-- ============================================================================

USE `intern_assessment`;

SET @assessment_id = (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'assessment' LIMIT 1);
SET @department_id = (SELECT menu_id FROM sys_menu WHERE parent_id = @assessment_id AND path = 'department' LIMIT 1);
SET @course_id = (SELECT menu_id FROM sys_menu WHERE menu_type = 'C' AND perms = 'business:course:list' ORDER BY menu_id DESC LIMIT 1);

-- 已存在的课程菜单直接迁移；新环境没有时补建。
UPDATE sys_menu
SET parent_id = @department_id,
    order_num = 3,
    path = 'courses',
    component = 'business/course/index',
    visible = '0',
    status = '0',
    remark = '部门管理员课程管理；按当前部门岗位隔离'
WHERE menu_id = @course_id;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '课程管理', @department_id, 3, 'courses', 'business/course/index', 1, 0, 'C', '0', '0', 'business:course:list', 'reading', 'admin', NOW(), '部门管理员课程管理；按当前部门岗位隔离'
WHERE @course_id IS NULL AND @department_id IS NOT NULL;

SET @course_id = (SELECT menu_id FROM sys_menu WHERE parent_id = @department_id AND perms = 'business:course:list' ORDER BY menu_id DESC LIMIT 1);

-- 课程按钮权限，缺失时补建。
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '课程查询', @course_id, 1, '#', '', 1, 0, 'F', '0', '0', 'business:course:query', '#', 'admin', NOW(), ''
WHERE @course_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @course_id AND perms = 'business:course:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '课程新增', @course_id, 2, '#', '', 1, 0, 'F', '0', '0', 'business:course:add', '#', 'admin', NOW(), ''
WHERE @course_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @course_id AND perms = 'business:course:add');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '课程修改', @course_id, 3, '#', '', 1, 0, 'F', '0', '0', 'business:course:edit', '#', 'admin', NOW(), ''
WHERE @course_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @course_id AND perms = 'business:course:edit');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '课程删除', @course_id, 4, '#', '', 1, 0, 'F', '0', '0', 'business:course:remove', '#', 'admin', NOW(), ''
WHERE @course_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @course_id AND perms = 'business:course:remove');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '课程发布', @course_id, 5, '#', '', 1, 0, 'F', '0', '0', 'business:course:publish', '#', 'admin', NOW(), ''
WHERE @course_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @course_id AND perms = 'business:course:publish');

SET @query_id = (SELECT menu_id FROM sys_menu WHERE parent_id = @course_id AND perms = 'business:course:query' LIMIT 1);
SET @add_id = (SELECT menu_id FROM sys_menu WHERE parent_id = @course_id AND perms = 'business:course:add' LIMIT 1);
SET @edit_id = (SELECT menu_id FROM sys_menu WHERE parent_id = @course_id AND perms = 'business:course:edit' LIMIT 1);
SET @remove_id = (SELECT menu_id FROM sys_menu WHERE parent_id = @course_id AND perms = 'business:course:remove' LIMIT 1);
SET @publish_id = (SELECT menu_id FROM sys_menu WHERE parent_id = @course_id AND perms = 'business:course:publish' LIMIT 1);
SET @super_role_id = (SELECT role_id FROM sys_role WHERE role_key = 'SUPER_ADMIN' AND del_flag = '0' LIMIT 1);
SET @dept_role_id = (SELECT role_id FROM sys_role WHERE role_key = 'DEPT_ADMIN' AND del_flag = '0' LIMIT 1);

-- 部门管理员拥有课程管理菜单及写权限。
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @dept_role_id, menu_id FROM sys_menu
WHERE menu_id IN (@assessment_id, @department_id, @course_id, @query_id, @add_id, @edit_id, @remove_id, @publish_id)
  AND @dept_role_id IS NOT NULL;

-- 超级管理员只读：保留菜单和查询，明确撤销课程写权限。
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @super_role_id, menu_id FROM sys_menu
WHERE menu_id IN (@assessment_id, @department_id, @course_id, @query_id)
  AND @super_role_id IS NOT NULL;

DELETE FROM sys_role_menu
WHERE role_id = @super_role_id
  AND menu_id IN (@add_id, @edit_id, @remove_id, @publish_id);
