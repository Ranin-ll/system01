-- ============================================================================
-- 实习生学习考核系统 - 题库模块菜单与权限
-- 作用：题库管理（管理员）、参与考核（实习生）
-- 可重复执行，不依赖固定自增 ID。
-- ============================================================================

USE `intern_assessment`;

SET @assessment_id = (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'assessment' LIMIT 1);
SET @department_id = (SELECT menu_id FROM sys_menu WHERE parent_id = @assessment_id AND path = 'department' LIMIT 1);
SET @intern_id = (SELECT menu_id FROM sys_menu WHERE parent_id = @assessment_id AND path = 'intern' LIMIT 1);

-- ==============================
-- 1. 题库管理菜单（部门运营下，order_num=6）
-- ==============================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '题库管理', @department_id, 6, 'questionBank', 'business/questionBank/index', 1, 0, 'C', '0', '0', 'business:bank:list', 'edit', 'admin', NOW(), '部门管理员管理本部门题库与题目；实习生可查看题库参与考核'
WHERE @department_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @department_id AND perms = 'business:bank:list');

SET @bank_id = (SELECT menu_id FROM sys_menu WHERE parent_id = @department_id AND perms = 'business:bank:list' LIMIT 1);

-- 题库按钮权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '题库查询', @bank_id, 1, '#', '', 1, 0, 'F', '0', '0', 'business:bank:query', '#', 'admin', NOW(), ''
WHERE @bank_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @bank_id AND perms = 'business:bank:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '题库新增', @bank_id, 2, '#', '', 1, 0, 'F', '0', '0', 'business:bank:add', '#', 'admin', NOW(), ''
WHERE @bank_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @bank_id AND perms = 'business:bank:add');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '题库修改', @bank_id, 3, '#', '', 1, 0, 'F', '0', '0', 'business:bank:edit', '#', 'admin', NOW(), ''
WHERE @bank_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @bank_id AND perms = 'business:bank:edit');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '题库删除', @bank_id, 4, '#', '', 1, 0, 'F', '0', '0', 'business:bank:remove', '#', 'admin', NOW(), ''
WHERE @bank_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @bank_id AND perms = 'business:bank:remove');

-- 题目按钮权限（挂在题库菜单下，order_num 5-10）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '题目列表', @bank_id, 5, '#', '', 1, 0, 'F', '0', '0', 'business:question:list', '#', 'admin', NOW(), ''
WHERE @bank_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @bank_id AND perms = 'business:question:list');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '题目查询', @bank_id, 6, '#', '', 1, 0, 'F', '0', '0', 'business:question:query', '#', 'admin', NOW(), ''
WHERE @bank_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @bank_id AND perms = 'business:question:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '题目新增', @bank_id, 7, '#', '', 1, 0, 'F', '0', '0', 'business:question:add', '#', 'admin', NOW(), ''
WHERE @bank_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @bank_id AND perms = 'business:question:add');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '题目修改', @bank_id, 8, '#', '', 1, 0, 'F', '0', '0', 'business:question:edit', '#', 'admin', NOW(), ''
WHERE @bank_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @bank_id AND perms = 'business:question:edit');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '题目删除', @bank_id, 9, '#', '', 1, 0, 'F', '0', '0', 'business:question:remove', '#', 'admin', NOW(), ''
WHERE @bank_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @bank_id AND perms = 'business:question:remove');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '批量导入', @bank_id, 10, '#', '', 1, 0, 'F', '0', '0', 'business:question:import', '#', 'admin', NOW(), ''
WHERE @bank_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @bank_id AND perms = 'business:question:import');

-- ==============================
-- 2. 改造实习生"考试中心"菜单 → 真实考核页面
-- ==============================
UPDATE sys_menu
SET component = 'assessment/exam/index',
    path = 'exam',
    remark = '实习生参与考核答题（从题库抽题作答）'
WHERE parent_id = @intern_id AND path = 'exam' AND menu_type = 'C';

-- ==============================
-- 3. 分配角色权限
-- ==============================
SET @bank_query_id   = (SELECT menu_id FROM sys_menu WHERE parent_id = @bank_id AND perms = 'business:bank:query' LIMIT 1);
SET @bank_add_id     = (SELECT menu_id FROM sys_menu WHERE parent_id = @bank_id AND perms = 'business:bank:add' LIMIT 1);
SET @bank_edit_id    = (SELECT menu_id FROM sys_menu WHERE parent_id = @bank_id AND perms = 'business:bank:edit' LIMIT 1);
SET @bank_remove_id  = (SELECT menu_id FROM sys_menu WHERE parent_id = @bank_id AND perms = 'business:bank:remove' LIMIT 1);
SET @question_list_id   = (SELECT menu_id FROM sys_menu WHERE parent_id = @bank_id AND perms = 'business:question:list' LIMIT 1);
SET @question_query_id  = (SELECT menu_id FROM sys_menu WHERE parent_id = @bank_id AND perms = 'business:question:query' LIMIT 1);
SET @question_add_id    = (SELECT menu_id FROM sys_menu WHERE parent_id = @bank_id AND perms = 'business:question:add' LIMIT 1);
SET @question_edit_id   = (SELECT menu_id FROM sys_menu WHERE parent_id = @bank_id AND perms = 'business:question:edit' LIMIT 1);
SET @question_remove_id = (SELECT menu_id FROM sys_menu WHERE parent_id = @bank_id AND perms = 'business:question:remove' LIMIT 1);
SET @question_import_id = (SELECT menu_id FROM sys_menu WHERE parent_id = @bank_id AND perms = 'business:question:import' LIMIT 1);
SET @exam_center_id    = (SELECT menu_id FROM sys_menu WHERE parent_id = @intern_id AND path = 'exam' LIMIT 1);

SET @super_role_id  = (SELECT role_id FROM sys_role WHERE role_key = 'SUPER_ADMIN' AND del_flag = '0' LIMIT 1);
SET @dept_role_id   = (SELECT role_id FROM sys_role WHERE role_key = 'DEPT_ADMIN' AND del_flag = '0' LIMIT 1);
SET @pre_role_id    = (SELECT role_id FROM sys_role WHERE role_key = 'PRE_TRAINEE' AND del_flag = '0' LIMIT 1);
SET @formal_role_id = (SELECT role_id FROM sys_role WHERE role_key = 'FORMAL_TRAINEE' AND del_flag = '0' LIMIT 1);

-- 3.1 部门管理员：题库管理全权限 + 所有题目按钮 + 考试中心菜单
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @dept_role_id, menu_id FROM sys_menu
WHERE menu_id IN (@assessment_id, @department_id, @bank_id, @bank_query_id, @bank_add_id, @bank_edit_id, @bank_remove_id,
                   @question_list_id, @question_query_id, @question_add_id, @question_edit_id, @question_remove_id, @question_import_id)
  AND @dept_role_id IS NOT NULL;

-- 3.2 超级管理员：继承部门管理员全部管理权限，后端数据范围为全局
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @super_role_id, menu_id FROM sys_menu
WHERE menu_id IN (@assessment_id, @department_id, @bank_id, @bank_query_id, @bank_add_id, @bank_edit_id, @bank_remove_id,
                  @question_list_id, @question_query_id, @question_add_id, @question_edit_id, @question_remove_id, @question_import_id)
  AND @super_role_id IS NOT NULL;

-- 3.3 仅预备实习生参与备考、模拟与正式考核；正式实习生保留历史记录入口但不新增考试入口
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @pre_role_id, menu_id FROM sys_menu
WHERE menu_id IN (@assessment_id, @intern_id, @bank_id, @exam_center_id)
  AND @pre_role_id IS NOT NULL;

-- 清理旧版本脚本可能遗留的正式实习生新考核入口；历史记录由只读接口和隐藏路由提供。
DELETE FROM sys_role_menu
WHERE role_id = @formal_role_id
  AND menu_id IN (@bank_id, @exam_center_id);

-- ==============================
-- 完成
-- ==============================
SELECT '题库模块菜单权限已配置' AS result,
       @bank_id AS 题库菜单ID,
       @exam_center_id AS 考试中心菜单ID;
