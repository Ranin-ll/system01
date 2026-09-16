-- ============================================================================
-- 模拟考核 · 实操部分 - 数据库改造
--
-- 背景：模拟考核原先只有理论自测（从部门模拟题库随机抽 10 道客观题自动判分）。
--       现补充「实操」部分：管理员只负责发布实操题目（题干必填、附件可选且可多个），
--       实习生进入模拟考核的实操列表后查看题干、下载附件即可 —— 不需要上传作答，
--       也不会提交给管理员查看（纯自学资料，无作答无评分）。
--
-- 设计：附件用 JSON 数组存 [{name, url}]，与 question.options_json 的既有约定一致。
-- ============================================================================

USE `intern_assessment`;

-- ==============================
-- 1. 模拟实操题表
-- ==============================
CREATE TABLE IF NOT EXISTS `practice_subject` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '模拟实操题ID',
    `dept_id`          BIGINT       NOT NULL COMMENT '所属部门ID',
    `content`          TEXT         NOT NULL COMMENT '题干',
    `attachments_json` TEXT         COMMENT '附件列表JSON:[{"name":"说明.docx","url":"/profile/upload/..."}]，可为空',
    `status`           TINYINT      NOT NULL DEFAULT 1 COMMENT '状态:1启用(已发布) 0停用',
    `create_by`        VARCHAR(32)  DEFAULT NULL COMMENT '创建人',
    `create_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`          TINYINT      NOT NULL DEFAULT 0 COMMENT '删除标志:0正常 1删除',
    PRIMARY KEY (`id`),
    KEY `idx_psubject_dept` (`dept_id`),
    KEY `idx_psubject_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='模拟考核实操题（管理员发布，实习生自学查看）';

-- ==============================
-- 2. 菜单（学习考核 → 部门运营，order_num=8）
-- ==============================
SET @assessment_id = (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'assessment' LIMIT 1);
SET @department_id = (SELECT menu_id FROM sys_menu WHERE parent_id = @assessment_id AND path = 'department' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '模拟实操管理', @department_id, 8, 'practiceSubject', 'business/practiceSubject/index', 1, 0, 'C', '0', '0', 'business:psubject:list', 'documentation', 'admin', NOW(), '部门管理员发布模拟考核实操题（题干+可选多个附件），实习生自学查看与下载'
WHERE @department_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @department_id AND perms = 'business:psubject:list');

SET @psubject_id = (SELECT menu_id FROM sys_menu WHERE parent_id = @department_id AND perms = 'business:psubject:list' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '实操题查询', @psubject_id, 1, '#', '', 1, 0, 'F', '0', '0', 'business:psubject:query', '#', 'admin', NOW(), ''
WHERE @psubject_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @psubject_id AND perms = 'business:psubject:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '实操题新增', @psubject_id, 2, '#', '', 1, 0, 'F', '0', '0', 'business:psubject:add', '#', 'admin', NOW(), ''
WHERE @psubject_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @psubject_id AND perms = 'business:psubject:add');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '实操题修改', @psubject_id, 3, '#', '', 1, 0, 'F', '0', '0', 'business:psubject:edit', '#', 'admin', NOW(), ''
WHERE @psubject_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @psubject_id AND perms = 'business:psubject:edit');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '实操题删除', @psubject_id, 4, '#', '', 1, 0, 'F', '0', '0', 'business:psubject:remove', '#', 'admin', NOW(), ''
WHERE @psubject_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @psubject_id AND perms = 'business:psubject:remove');

-- ==============================
-- 3. 角色授权
-- ==============================
SET @ps_query_id  = (SELECT menu_id FROM sys_menu WHERE parent_id = @psubject_id AND perms = 'business:psubject:query' LIMIT 1);
SET @ps_add_id    = (SELECT menu_id FROM sys_menu WHERE parent_id = @psubject_id AND perms = 'business:psubject:add' LIMIT 1);
SET @ps_edit_id   = (SELECT menu_id FROM sys_menu WHERE parent_id = @psubject_id AND perms = 'business:psubject:edit' LIMIT 1);
SET @ps_remove_id = (SELECT menu_id FROM sys_menu WHERE parent_id = @psubject_id AND perms = 'business:psubject:remove' LIMIT 1);

SET @super_role_id  = (SELECT role_id FROM sys_role WHERE role_key = 'SUPER_ADMIN' AND del_flag = '0' LIMIT 1);
SET @dept_role_id   = (SELECT role_id FROM sys_role WHERE role_key = 'DEPT_ADMIN' AND del_flag = '0' LIMIT 1);

-- 3.1 部门管理员：菜单 + 全部按钮
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @dept_role_id, menu_id FROM sys_menu
WHERE menu_id IN (@assessment_id, @department_id, @psubject_id, @ps_query_id, @ps_add_id, @ps_edit_id, @ps_remove_id)
  AND @dept_role_id IS NOT NULL;

-- 3.2 超级管理员：只读（菜单 + 查询），显式收回写按钮
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @super_role_id, menu_id FROM sys_menu
WHERE menu_id IN (@assessment_id, @department_id, @psubject_id, @ps_query_id)
  AND @super_role_id IS NOT NULL;

DELETE FROM sys_role_menu
WHERE role_id = @super_role_id
  AND menu_id IN (@ps_add_id, @ps_edit_id, @ps_remove_id);

-- 说明：实习生无需新菜单/新权限 —— 实操列表通过 /business/practice-subject/published
--       暴露，沿用实习生已有的 business:bank:list 权限，入口在「模拟考核」页内。

-- ==============================
-- 验证
-- ==============================
SHOW COLUMNS FROM `practice_subject`;
SELECT '模拟实操模块已配置' AS result, @psubject_id AS 实操管理菜单ID;
