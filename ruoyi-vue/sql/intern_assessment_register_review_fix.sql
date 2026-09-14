-- ============================================================================
-- 注册审核菜单权限修复
-- 用途：修复已初始化数据库中部门管理员看不到注册审核入口的问题。
-- 可重复执行；按 role_key / perms 查找，不依赖固定自增 ID。
-- ============================================================================

USE intern_assessment;

SET @assessment_id = (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'assessment' LIMIT 1);
SET @department_id = (SELECT menu_id FROM sys_menu WHERE parent_id = @assessment_id AND path = 'department' LIMIT 1);
SET @register_id = (SELECT menu_id FROM sys_menu WHERE menu_type = 'C' AND perms = 'business:register:list' ORDER BY menu_id DESC LIMIT 1);

-- 旧版菜单可能还挂在“业务管理”下，迁移到“学习考核 / 部门运营”。
UPDATE sys_menu
SET parent_id = @department_id,
    order_num = 2,
    path = 'register-review',
    component = 'business/register/index',
    visible = '0',
    status = '0',
    remark = '部门管理员注册申请审核'
WHERE menu_id = @register_id;

SET @register_id = (SELECT menu_id FROM sys_menu WHERE menu_type = 'C' AND perms = 'business:register:list' ORDER BY menu_id DESC LIMIT 1);

SET @query_id = (SELECT menu_id FROM sys_menu WHERE parent_id = @register_id AND perms = 'business:register:query' LIMIT 1);
SET @audit_id = (SELECT menu_id FROM sys_menu WHERE parent_id = @register_id AND perms = 'business:register:audit' LIMIT 1);
SET @resubmit_id = (SELECT menu_id FROM sys_menu WHERE parent_id = @register_id AND perms = 'business:register:resubmit' LIMIT 1);

SET @super_role_id = (SELECT role_id FROM sys_role WHERE role_key = 'SUPER_ADMIN' AND del_flag = '0' LIMIT 1);
SET @dept_role_id = (SELECT role_id FROM sys_role WHERE role_key = 'DEPT_ADMIN' AND del_flag = '0' LIMIT 1);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m
WHERE r.role_id IN (@super_role_id, @dept_role_id)
  AND m.menu_id IN (@assessment_id, @department_id, @register_id, @query_id, @audit_id, @resubmit_id);

-- 刷新管理员测试账号对应角色的注册审核权限，避免需要手工在菜单管理页逐项授权。
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT ur.role_id, m.menu_id
FROM sys_user_role ur
JOIN sys_user u ON u.user_id = ur.user_id
JOIN sys_menu m
WHERE u.user_name IN (
    'test_super_admin',
    'test_dept_admin',
    'test_dev_admin',
    'test_design_admin',
    'test_qa_admin',
    'test_model_admin'
)
AND m.menu_id IN (@assessment_id, @department_id, @register_id, @query_id, @audit_id, @resubmit_id);
