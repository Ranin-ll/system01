-- =============================================================================
-- 模拟考核 · 「模块」层 迁移脚本（幂等 · 可重复执行）
-- -----------------------------------------------------------------------------
-- 背景：模拟考核原先直接「管理员建实操题 / 配一套理论考核」，没有中间分组。
--       现引入「模块」作为顶层分组：
--         模块 (practice_module)
--           ├── 理论模拟考核  N 个（exam, exam_mode='PRACTICE' AND exam_type='THEORY'）
--           └── 实操模拟题    M 个（practice_subject）
--       实习生端先选模块 → 再看模块下的考核列表。
--       模块取代原「方向(direction)」的顶层分组地位（direction 字段保留，降级为模块内的次级标签）。
-- 说明：全部改动先查 information_schema 再 ALTER，重复执行不报错；不删任何已有列与数据。
-- 回滚：见文件末尾。
-- =============================================================================

USE `intern_assessment`;

-- -----------------------------------------------------------------------------
-- 0. 执行前快照
-- -----------------------------------------------------------------------------
SELECT COUNT(*) AS practice_subject_rows_before FROM `practice_subject`;
SELECT COUNT(*) AS exam_rows_before FROM `exam`;

-- -----------------------------------------------------------------------------
-- 1. 模块表 practice_module
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `practice_module` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '模块ID',
    `dept_id`     BIGINT       NOT NULL COMMENT '所属部门ID',
    `name`        VARCHAR(100) NOT NULL COMMENT '模块名称',
    `description` VARCHAR(500)          DEFAULT NULL COMMENT '模块说明（一句话）',
    `sort_no`     INT          NOT NULL DEFAULT 0 COMMENT '排序号（越小越靠前）',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
    `create_by`   VARCHAR(32)           DEFAULT NULL COMMENT '创建人',
    `create_time` DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '删除标志:0正常 1删除',
    PRIMARY KEY (`id`),
    KEY `idx_pmodule_dept` (`dept_id`, `status`),
    KEY `idx_pmodule_sort` (`dept_id`, `sort_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='模拟考核模块（模块下挂理论模拟考核与实操模拟题）';

-- -----------------------------------------------------------------------------
-- 2. practice_subject 增加 module_id
-- -----------------------------------------------------------------------------
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'practice_subject' AND COLUMN_NAME = 'module_id');
SET @s := IF(@c = 0,
  'ALTER TABLE `practice_subject` ADD COLUMN `module_id` BIGINT DEFAULT NULL COMMENT ''所属模块ID(practice_module.id)'' AFTER `dept_id`',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.STATISTICS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'practice_subject' AND INDEX_NAME = 'idx_psubject_module');
SET @s := IF(@c = 0,
  'ALTER TABLE `practice_subject` ADD KEY `idx_psubject_module` (`dept_id`, `module_id`, `status`)',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- -----------------------------------------------------------------------------
-- 3. exam 增加 module_id（仅模拟理论考核会写入，正式考核保持 NULL）
-- -----------------------------------------------------------------------------
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam' AND COLUMN_NAME = 'module_id');
SET @s := IF(@c = 0,
  'ALTER TABLE `exam` ADD COLUMN `module_id` BIGINT DEFAULT NULL COMMENT ''所属模块ID(仅模拟考核使用)'' AFTER `dept_id`',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.STATISTICS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam' AND INDEX_NAME = 'idx_exam_module');
SET @s := IF(@c = 0,
  'ALTER TABLE `exam` ADD KEY `idx_exam_module` (`dept_id`, `module_id`, `exam_mode`, `status`)',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- -----------------------------------------------------------------------------
-- 4. 存量数据归集：为已有实操题但不属于任何模块的部门建「默认模块」并回填
-- -----------------------------------------------------------------------------
INSERT INTO `practice_module` (`dept_id`, `name`, `description`, `sort_no`, `status`, `create_by`, `create_time`, `update_time`, `deleted`)
SELECT DISTINCT s.`dept_id`, '默认模块', '历史实操题归集的默认模块，可在「模拟备考管理」中改名或拆分。', 0, 1, 'migration', NOW(), NOW(), 0
FROM `practice_subject` s
WHERE s.`deleted` = 0
  AND (s.`module_id` IS NULL OR s.`module_id` = 0)
  AND NOT EXISTS (
      SELECT 1 FROM `practice_module` m
      WHERE m.`dept_id` = s.`dept_id` AND m.`name` = '默认模块' AND m.`deleted` = 0
  );

UPDATE `practice_subject` s
JOIN `practice_module` m ON m.`dept_id` = s.`dept_id` AND m.`name` = '默认模块' AND m.`deleted` = 0
SET s.`module_id` = m.`id`
WHERE s.`deleted` = 0 AND (s.`module_id` IS NULL OR s.`module_id` = 0);

-- -----------------------------------------------------------------------------
-- 5. 菜单与按钮权限 business:pmodule:*
-- -----------------------------------------------------------------------------
SET @assessment_id = (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'assessment' LIMIT 1);
SET @department_id = (SELECT menu_id FROM sys_menu WHERE parent_id = @assessment_id AND path = 'department' LIMIT 1);

-- 说明：模块管理不再单开页面菜单（入口在「模拟备考管理」页内），这里只补按钮级权限标识。
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '模拟模块列表', @department_id, 14, '#', '', 1, 0, 'F', '0', '0', 'business:pmodule:list', '#', 'admin', NOW(), ''
WHERE @department_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @department_id AND perms = 'business:pmodule:list');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '模拟模块查询', @department_id, 15, '#', '', 1, 0, 'F', '0', '0', 'business:pmodule:query', '#', 'admin', NOW(), ''
WHERE @department_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @department_id AND perms = 'business:pmodule:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '模拟模块新增', @department_id, 16, '#', '', 1, 0, 'F', '0', '0', 'business:pmodule:add', '#', 'admin', NOW(), ''
WHERE @department_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @department_id AND perms = 'business:pmodule:add');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '模拟模块修改', @department_id, 17, '#', '', 1, 0, 'F', '0', '0', 'business:pmodule:edit', '#', 'admin', NOW(), ''
WHERE @department_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @department_id AND perms = 'business:pmodule:edit');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '模拟模块删除', @department_id, 18, '#', '', 1, 0, 'F', '0', '0', 'business:pmodule:remove', '#', 'admin', NOW(), ''
WHERE @department_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @department_id AND perms = 'business:pmodule:remove');

-- 角色授权：部门管理员 + 超级管理员
SET @super_role_id = (SELECT role_id FROM sys_role WHERE role_key = 'SUPER_ADMIN' AND del_flag = '0' LIMIT 1);
SET @dept_role_id  = (SELECT role_id FROM sys_role WHERE role_key = 'DEPT_ADMIN' AND del_flag = '0' LIMIT 1);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @dept_role_id, menu_id FROM sys_menu
WHERE parent_id = @department_id AND perms LIKE 'business:pmodule:%' AND @dept_role_id IS NOT NULL;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @super_role_id, menu_id FROM sys_menu
WHERE parent_id = @department_id AND perms LIKE 'business:pmodule:%' AND @super_role_id IS NOT NULL;

-- -----------------------------------------------------------------------------
-- 6. 执行后校验
-- -----------------------------------------------------------------------------
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'practice_module'
ORDER BY ORDINAL_POSITION;

SELECT `id`, `dept_id`, `name`, `status`, `sort_no` FROM `practice_module` WHERE `deleted` = 0 ORDER BY `dept_id`, `sort_no`, `id`;

SELECT `module_id`, COUNT(*) AS cnt FROM `practice_subject` WHERE `deleted` = 0 GROUP BY `module_id`;

SELECT '模拟模块迁移完成' AS result, @department_id AS 部门运营菜单ID;
