-- ============================================================================
-- 学习考核页面骨架菜单初始化脚本
-- 执行前请先执行 intern_assessment_menu.sql。脚本供首次初始化使用一次。
-- ============================================================================

USE `intern_assessment`;

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
VALUES ('学习考核', 0, 6, 'assessment', NULL, 1, 0, 'M', '0', '0', '', 'education', 'admin', NOW(), '实习生学习考核系统页面骨架');
SET @assessmentId = LAST_INSERT_ID();

-- 一级分组
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`) VALUES
('学习考核工作台', @assessmentId, 1, 'overview', 'assessment/index', 1, 0, 'C', '0', '0', 'assessment:overview:list', 'dashboard', 'admin', NOW(), '综合工作台'),
('实习生门户', @assessmentId, 2, 'intern', 'ParentView', 1, 0, 'M', '0', '0', '', 'user', 'admin', NOW(), '实习生端页面'),
('部门运营', @assessmentId, 3, 'department', 'ParentView', 1, 0, 'M', '0', '0', '', 'peoples', 'admin', NOW(), '部门管理员页面'),
('考核认证管理', @assessmentId, 4, 'manage', 'ParentView', 1, 0, 'M', '0', '0', '', 'guide', 'admin', NOW(), '考核认证配置页面'),
('系统运营', @assessmentId, 5, 'system', 'ParentView', 1, 0, 'M', '0', '0', '', 'system', 'admin', NOW(), '系统管理员页面');

SET @internId = (SELECT menu_id FROM sys_menu WHERE parent_id = @assessmentId AND path = 'intern' LIMIT 1);
SET @departmentId = (SELECT menu_id FROM sys_menu WHERE parent_id = @assessmentId AND path = 'department' LIMIT 1);
SET @manageId = (SELECT menu_id FROM sys_menu WHERE parent_id = @assessmentId AND path = 'manage' LIMIT 1);
SET @systemId = (SELECT menu_id FROM sys_menu WHERE parent_id = @assessmentId AND path = 'system' LIMIT 1);

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`) VALUES
('实习生工作台', @internId, 1, 'intern-dashboard', 'assessment/index', 1, 0, 'C', '0', '0', 'assessment:intern:list', 'dashboard', 'admin', NOW(), ''),
('学习中心', @internId, 2, 'learning', 'assessment/index', 1, 0, 'C', '0', '0', 'assessment:learning:list', 'reading', 'admin', NOW(), ''),
('考试中心', @internId, 3, 'exam', 'assessment/index', 1, 0, 'C', '0', '0', 'assessment:exam:list', 'form', 'admin', NOW(), ''),
('实践考核提交', @internId, 4, 'practice', 'assessment/index', 1, 0, 'C', '0', '0', 'assessment:practice:list', 'upload', 'admin', NOW(), ''),
('任务中心', @internId, 5, 'tasks', 'assessment/index', 1, 0, 'C', '0', '0', 'assessment:task:list', 'list', 'admin', NOW(), ''),
('考核成绩', @internId, 6, 'scores', 'assessment/index', 1, 0, 'C', '0', '0', 'assessment:score:list', 'chart', 'admin', NOW(), ''),
('能力画像', @internId, 7, 'portrait', 'assessment/index', 1, 0, 'C', '0', '0', 'assessment:portrait:list', 'peoples', 'admin', NOW(), ''),
('协议与证书', @internId, 8, 'agreements', 'assessment/index', 1, 0, 'C', '0', '0', 'assessment:agreement:list', 'example', 'admin', NOW(), ''),
('消息中心', @internId, 9, 'messages', 'assessment/index', 1, 0, 'C', '0', '0', 'assessment:message:list', 'message', 'admin', NOW(), ''),
('个人信息', @internId, 10, 'profile', 'assessment/index', 1, 0, 'C', '0', '0', 'assessment:profile:list', 'user', 'admin', NOW(), '');

-- 实习生侧栏仅展示工作台和消息中心，其余页面从工作台进入。
UPDATE sys_menu
SET visible = CASE WHEN path IN ('intern-dashboard', 'messages') THEN '0' ELSE '1' END
WHERE parent_id = @internId AND menu_type = 'C';

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`) VALUES
('部门工作台', @departmentId, 1, 'department-dashboard', 'assessment/index', 1, 0, 'C', '0', '0', 'assessment:department:list', 'dashboard', 'admin', NOW(), ''),
('实习生管理', @departmentId, 2, 'students', 'assessment/index', 1, 0, 'C', '0', '0', 'assessment:student:list', 'peoples', 'admin', NOW(), ''),
('实习批阅', @departmentId, 3, 'grading', 'assessment/index', 1, 0, 'C', '0', '0', 'assessment:grading:list', 'edit', 'admin', NOW(), '');

-- 将注册审核入口挂到部门运营，部门管理员登录后可直接处理本部门申请。
SET @registerReviewId = (
    SELECT menu_id FROM sys_menu
    WHERE menu_type = 'C' AND perms = 'business:register:list'
    ORDER BY menu_id DESC LIMIT 1
);
UPDATE sys_menu
SET parent_id = @departmentId, order_num = 2, path = 'register-review',
    component = 'business/register/index', remark = '部门管理员注册申请审核'
WHERE menu_id = @registerReviewId;

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`) VALUES
('考核认证总览', @manageId, 1, 'overview', 'assessment/index', 1, 0, 'C', '0', '0', 'assessment:manage:list', 'guide', 'admin', NOW(), ''),
('考试安排', @manageId, 2, 'schedule', 'assessment/index', 1, 0, 'C', '0', '0', 'assessment:schedule:list', 'date', 'admin', NOW(), ''),
('课程设计', @manageId, 3, 'course-design', 'assessment/index', 1, 0, 'C', '0', '0', 'assessment:course:list', 'reading', 'admin', NOW(), ''),
('线下学习', @manageId, 4, 'offline-learning', 'assessment/index', 1, 0, 'C', '0', '0', 'assessment:offline:list', 'office-building', 'admin', NOW(), ''),
('理论题库', @manageId, 5, 'theory-bank', 'assessment/index', 1, 0, 'C', '0', '0', 'assessment:theory:list', 'document-copy', 'admin', NOW(), ''),
('实操题库', @manageId, 6, 'practice-bank', 'assessment/index', 1, 0, 'C', '0', '0', 'assessment:practicebank:list', 'monitor', 'admin', NOW(), ''),
('常见问题', @manageId, 7, 'faq', 'assessment/index', 1, 0, 'C', '0', '0', 'assessment:faq:list', 'question', 'admin', NOW(), ''),
('实习考核', @manageId, 8, 'intern-exam', 'assessment/index', 1, 0, 'C', '0', '0', 'assessment:internexam:list', 'finished', 'admin', NOW(), ''),
('考核批阅', @manageId, 9, 'review', 'assessment/index', 1, 0, 'C', '0', '0', 'assessment:review:list', 'edit-outline', 'admin', NOW(), '');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`) VALUES
('全局工作台', @systemId, 1, 'global-dashboard', 'assessment/index', 1, 0, 'C', '0', '0', 'assessment:global:list', 'dashboard', 'admin', NOW(), ''),
('组织岗位', @systemId, 2, 'organization', 'assessment/index', 1, 0, 'C', '0', '0', 'assessment:organization:list', 'tree-table', 'admin', NOW(), ''),
('角色权限', @systemId, 3, 'role-permission', 'assessment/index', 1, 0, 'C', '0', '0', 'assessment:role:list', 'password', 'admin', NOW(), ''),
('系统规则', @systemId, 4, 'system-rules', 'assessment/index', 1, 0, 'C', '0', '0', 'assessment:rule:list', 'edit', 'admin', NOW(), ''),
('审计日志', @systemId, 5, 'audit-log', 'assessment/index', 1, 0, 'C', '0', '0', 'assessment:audit:list', 'log', 'admin', NOW(), '');

-- 当前阶段给内置管理员与业务超级管理员完整菜单，按 role_key 授权，避免依赖固定自增 ID。
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m
WHERE r.role_key IN ('admin', 'SUPER_ADMIN')
  AND (m.menu_id = @assessmentId OR m.parent_id IN (@assessmentId, @internId, @departmentId, @manageId, @systemId));
