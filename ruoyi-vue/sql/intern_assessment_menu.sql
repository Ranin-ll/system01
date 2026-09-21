-- ============================================================================
-- 业务模块菜单与按钮权限初始化脚本
-- 说明：执行此脚本后，业务管理菜单及按钮权限将在系统管理-菜单管理中可见
-- ============================================================================

USE `intern_assessment`;

-- ============== 1. 业务管理菜单 ==============
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`) VALUES
    ('业务管理', 0, 5, 'business', NULL, 1, 0, 'M', '0', '0', '', 'education', 'admin', NOW(), '业务管理目录');

-- 获取业务管理菜单ID（假设为 @parentId，根据实际环境变量调整）
SET @parentId = LAST_INSERT_ID();

-- 岗位管理菜单
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`) VALUES
    ('岗位管理', @parentId, 1, 'position', 'business/position/index', 1, 0, 'C', '0', '0', 'business:position:list', 'peoples', 'admin', NOW(), '岗位管理菜单');

SET @positionMenuId = LAST_INSERT_ID();

-- 岗位管理按钮权限
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`) VALUES
    ('岗位查询', @positionMenuId, 1, '#', '', 1, 0, 'F', '0', '0', 'business:position:query', '#', 'admin', NOW(), ''),
    ('岗位新增', @positionMenuId, 2, '#', '', 1, 0, 'F', '0', '0', 'business:position:add', '#', 'admin', NOW(), ''),
    ('岗位修改', @positionMenuId, 3, '#', '', 1, 0, 'F', '0', '0', 'business:position:edit', '#', 'admin', NOW(), ''),
    ('岗位删除', @positionMenuId, 4, '#', '', 1, 0, 'F', '0', '0', 'business:position:remove', '#', 'admin', NOW(), '');

-- 课程管理菜单
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`) VALUES
    ('课程管理', @parentId, 2, 'course', 'business/course/index', 1, 0, 'C', '0', '0', 'business:course:list', 'guide', 'admin', NOW(), '课程管理菜单');

SET @courseMenuId = LAST_INSERT_ID();

-- 课程管理按钮权限
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`) VALUES
    ('课程查询', @courseMenuId, 1, '#', '', 1, 0, 'F', '0', '0', 'business:course:query', '#', 'admin', NOW(), ''),
    ('课程新增', @courseMenuId, 2, '#', '', 1, 0, 'F', '0', '0', 'business:course:add', '#', 'admin', NOW(), ''),
    ('课程修改', @courseMenuId, 3, '#', '', 1, 0, 'F', '0', '0', 'business:course:edit', '#', 'admin', NOW(), ''),
    ('课程删除', @courseMenuId, 4, '#', '', 1, 0, 'F', '0', '0', 'business:course:remove', '#', 'admin', NOW(), ''),
    ('课程发布', @courseMenuId, 5, '#', '', 1, 0, 'F', '0', '0', 'business:course:publish', '#', 'admin', NOW(), '');

-- 注册审核菜单
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`) VALUES
    ('注册审核', @parentId, 3, 'register', 'business/register/index', 1, 0, 'C', '0', '0', 'business:register:list', 'user', 'admin', NOW(), '注册审核菜单');

SET @registerMenuId = LAST_INSERT_ID();

-- 注册审核按钮权限
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`) VALUES
    ('注册查询', @registerMenuId, 1, '#', '', 1, 0, 'F', '0', '0', 'business:register:query', '#', 'admin', NOW(), ''),
    ('注册审核', @registerMenuId, 2, '#', '', 1, 0, 'F', '0', '0', 'business:register:audit', '#', 'admin', NOW(), ''),
    ('注册重新提交', @registerMenuId, 3, '#', '', 1, 0, 'F', '0', '0', 'business:register:resubmit', '#', 'admin', NOW(), '');

-- ============== 2. 为超级管理员分配所有业务权限 ==============
-- 按 role_key 查找角色，兼容内置 admin 与业务超级管理员角色的不同自增 ID。
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r
JOIN `sys_menu` m
WHERE r.role_key IN ('admin', 'SUPER_ADMIN')
  AND m.menu_name IN (
    '业务管理','岗位管理','课程管理','注册审核','岗位查询','岗位新增','岗位修改','岗位删除',
    '课程查询','课程新增','课程修改','课程删除','课程发布',
    '注册查询','注册审核','注册重新提交'
);

-- 部门管理员至少需要注册审核菜单及其按钮权限。
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r
JOIN `sys_menu` m
WHERE r.role_key = 'DEPT_ADMIN'
  AND m.menu_name IN ('注册审核', '注册查询', '注册审核', '注册重新提交');
