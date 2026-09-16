-- ============================================================================
-- 考核管理菜单
-- ============================================================================
USE `intern_assessment`;

SET @assessment_id = (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'assessment' LIMIT 1);
SET @department_id = (SELECT menu_id FROM sys_menu WHERE parent_id = @assessment_id AND path = 'department' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '考核管理', @department_id, 7, 'exam', 'business/exam/index', 1, 0, 'C', '0', '0', 'business:bank:list', 'documentation', 'admin', NOW(), '创建/发布考核，批改答卷并发布成绩'
WHERE @department_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @department_id AND path = 'exam' AND menu_type = 'C');

SET @exam_menu_id = (SELECT menu_id FROM sys_menu WHERE parent_id = @department_id AND path = 'exam' AND menu_type = 'C' LIMIT 1);

SET @super_role_id = (SELECT role_id FROM sys_role WHERE role_key = 'SUPER_ADMIN' AND del_flag = '0' LIMIT 1);
SET @dept_role_id = (SELECT role_id FROM sys_role WHERE role_key = 'DEPT_ADMIN' AND del_flag = '0' LIMIT 1);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @dept_role_id, @exam_menu_id WHERE @dept_role_id IS NOT NULL AND @exam_menu_id IS NOT NULL;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @super_role_id, @exam_menu_id WHERE @super_role_id IS NOT NULL AND @exam_menu_id IS NOT NULL;

SELECT '考核管理菜单已配置' AS result, @exam_menu_id AS 菜单ID;