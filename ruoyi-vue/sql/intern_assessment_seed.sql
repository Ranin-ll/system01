-- 实习生学习考核系统：固定主数据初始化
-- 可重复执行；不删除申请、用户或审核历史。
-- 数据库：intern_assessment

USE `intern_assessment`;

START TRANSACTION;

-- 5 个固定业务部门，沿用平台初始化的稳定部门 ID。
UPDATE sys_dept SET dept_name = '交付部门', order_num = 1, status = '0', del_flag = '0', update_by = 'seed', update_time = NOW()
WHERE dept_id = 103;
UPDATE sys_dept SET dept_name = '开发部门', order_num = 2, status = '0', del_flag = '0', update_by = 'seed', update_time = NOW()
WHERE dept_id = 104;
UPDATE sys_dept SET dept_name = '设计部门', order_num = 3, status = '0', del_flag = '0', update_by = 'seed', update_time = NOW()
WHERE dept_id = 105;
UPDATE sys_dept SET dept_name = '质检部门', order_num = 4, status = '0', del_flag = '0', update_by = 'seed', update_time = NOW()
WHERE dept_id = 106;
UPDATE sys_dept SET dept_name = '建模部门', order_num = 5, status = '0', del_flag = '0', update_by = 'seed', update_time = NOW()
WHERE dept_id = 107;

-- 5 个固定岗位主数据。
INSERT INTO position (position_code, position_name, sort_no, status, deleted, create_by, create_time)
SELECT 'IMPLEMENTATION', '实施实习生', 1, 1, 0, 'seed', NOW()
WHERE NOT EXISTS (SELECT 1 FROM position WHERE position_code = 'IMPLEMENTATION');
INSERT INTO position (position_code, position_name, sort_no, status, deleted, create_by, create_time)
SELECT 'DEVELOPMENT', '开发实习生', 2, 1, 0, 'seed', NOW()
WHERE NOT EXISTS (SELECT 1 FROM position WHERE position_code = 'DEVELOPMENT');
INSERT INTO position (position_code, position_name, sort_no, status, deleted, create_by, create_time)
SELECT 'DESIGN', '设计实习生', 3, 1, 0, 'seed', NOW()
WHERE NOT EXISTS (SELECT 1 FROM position WHERE position_code = 'DESIGN');
INSERT INTO position (position_code, position_name, sort_no, status, deleted, create_by, create_time)
SELECT 'QA', '质检实习生', 4, 1, 0, 'seed', NOW()
WHERE NOT EXISTS (SELECT 1 FROM position WHERE position_code = 'QA');
INSERT INTO position (position_code, position_name, sort_no, status, deleted, create_by, create_time)
SELECT 'MODELING', '建模实习生', 5, 1, 0, 'seed', NOW()
WHERE NOT EXISTS (SELECT 1 FROM position WHERE position_code = 'MODELING');

UPDATE position SET position_name = '实施实习生', sort_no = 1, status = 1, deleted = 0, update_by = 'seed', update_time = NOW()
WHERE position_code = 'IMPLEMENTATION';
UPDATE position SET position_name = '开发实习生', sort_no = 2, status = 1, deleted = 0, update_by = 'seed', update_time = NOW()
WHERE position_code = 'DEVELOPMENT';
UPDATE position SET position_name = '设计实习生', sort_no = 3, status = 1, deleted = 0, update_by = 'seed', update_time = NOW()
WHERE position_code = 'DESIGN';
UPDATE position SET position_name = '质检实习生', sort_no = 4, status = 1, deleted = 0, update_by = 'seed', update_time = NOW()
WHERE position_code = 'QA';
UPDATE position SET position_name = '建模实习生', sort_no = 5, status = 1, deleted = 0, update_by = 'seed', update_time = NOW()
WHERE position_code = 'MODELING';

-- 岗位到部门一对一映射：部门决定注册审核的数据边界。
INSERT INTO dept_position (dept_id, position_id, status, create_time)
SELECT 103, id, 1, NOW() FROM position WHERE position_code = 'IMPLEMENTATION'
ON DUPLICATE KEY UPDATE position_id = VALUES(position_id), status = 1;
INSERT INTO dept_position (dept_id, position_id, status, create_time)
SELECT 104, id, 1, NOW() FROM position WHERE position_code = 'DEVELOPMENT'
ON DUPLICATE KEY UPDATE position_id = VALUES(position_id), status = 1;
INSERT INTO dept_position (dept_id, position_id, status, create_time)
SELECT 105, id, 1, NOW() FROM position WHERE position_code = 'DESIGN'
ON DUPLICATE KEY UPDATE position_id = VALUES(position_id), status = 1;
INSERT INTO dept_position (dept_id, position_id, status, create_time)
SELECT 106, id, 1, NOW() FROM position WHERE position_code = 'QA'
ON DUPLICATE KEY UPDATE position_id = VALUES(position_id), status = 1;
INSERT INTO dept_position (dept_id, position_id, status, create_time)
SELECT 107, id, 1, NOW() FROM position WHERE position_code = 'MODELING'
ON DUPLICATE KEY UPDATE position_id = VALUES(position_id), status = 1;

-- 4 个固定角色：不可删除，业务权限由菜单脚本分配。
INSERT INTO sys_role (role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark)
SELECT '超级管理员', 'SUPER_ADMIN', 1, '1', 1, 1, '0', '0', 'seed', NOW(), '固定角色：全局管理与统计'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key = 'SUPER_ADMIN');
INSERT INTO sys_role (role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark)
SELECT '部门管理员', 'DEPT_ADMIN', 2, '2', 1, 1, '0', '0', 'seed', NOW(), '固定角色：本部门业务管理'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key = 'DEPT_ADMIN');
INSERT INTO sys_role (role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark)
SELECT '预备实习生', 'PRE_TRAINEE', 3, '5', 1, 1, '0', '0', 'seed', NOW(), '固定角色：预备实习生'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key = 'PRE_TRAINEE');
INSERT INTO sys_role (role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark)
SELECT '正式实习生', 'FORMAL_TRAINEE', 4, '5', 1, 1, '0', '0', 'seed', NOW(), '固定角色：正式实习生'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key = 'FORMAL_TRAINEE');

UPDATE sys_role SET status = '0', del_flag = '0', role_name = '超级管理员', role_sort = 1 WHERE role_key = 'SUPER_ADMIN';
UPDATE sys_role SET status = '0', del_flag = '0', role_name = '部门管理员', role_sort = 2 WHERE role_key = 'DEPT_ADMIN';
UPDATE sys_role SET status = '0', del_flag = '0', role_name = '预备实习生', role_sort = 3 WHERE role_key = 'PRE_TRAINEE';
UPDATE sys_role SET status = '0', del_flag = '0', role_name = '正式实习生', role_sort = 4 WHERE role_key = 'FORMAL_TRAINEE';

COMMIT;

-- 验证查询（在 Navicat 中单独执行即可）。
-- SELECT d.dept_id, d.dept_name, p.position_code, p.position_name
-- FROM dept_position dp JOIN sys_dept d ON d.dept_id = dp.dept_id
-- JOIN position p ON p.id = dp.position_id WHERE dp.status = 1 ORDER BY dp.dept_id;
-- SELECT role_id, role_name, role_key FROM sys_role
-- WHERE role_key IN ('SUPER_ADMIN','DEPT_ADMIN','PRE_TRAINEE','FORMAL_TRAINEE');
