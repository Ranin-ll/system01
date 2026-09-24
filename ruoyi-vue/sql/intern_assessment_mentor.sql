-- =============================================================================
-- 导师库（mentor）迁移 —— 2026-09-23
--
-- 背景：此前「导师」只是 sys_user 上的两个自由文本列（mentor_name / mentor_phone），
--       注册审核通过时手工敲进去，导致脏值（如「001」/「112」、「圣诞」/「2332」）。
-- 本次改造：导师抽成独立主表，实习生通过 sys_user.mentor_id 关联；
--       mentor_name / mentor_phone 两个文本列**保留**，作为冗余展示字段
--       （写 mentor_id 时同步回填），这样所有既有列表查询零改动。
--
-- 幂等：全部语句可重复执行。
-- 执行：本机无 mysql CLI，由 `.codex-tmp/apply_mentor_migration.py` 逐条执行（pymysql）。
-- =============================================================================

-- ① 导师主表
CREATE TABLE IF NOT EXISTS `mentor` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '导师ID',
  `mentor_name`  varchar(64)  NOT NULL                COMMENT '导师姓名',
  `mentor_phone` varchar(32)  NOT NULL                COMMENT '联系方式',
  `dept_id`      bigint       DEFAULT NULL            COMMENT '所属部门ID（sys_dept.dept_id；超管可跨部门）',
  `position_id`  bigint       DEFAULT NULL             COMMENT '岗位ID（position.id，可空）',
  `title`        varchar(64)  DEFAULT NULL            COMMENT '职务/头衔（可空）',
  `status`       char(1)      NOT NULL DEFAULT '0'    COMMENT '状态（0启用 1停用）',
  `remark`       varchar(255) DEFAULT NULL            COMMENT '备注',
  `create_by`    varchar(64)  DEFAULT ''              COMMENT '创建者',
  `create_time`  datetime     DEFAULT NULL            COMMENT '创建时间',
  `update_by`    varchar(64)  DEFAULT ''              COMMENT '更新者',
  `update_time`  datetime     DEFAULT NULL            COMMENT '更新时间',
  `deleted`      tinyint      NOT NULL DEFAULT 0      COMMENT '删除标志（0存在 1删除）',
  PRIMARY KEY (`id`),
  KEY `idx_mentor_dept` (`dept_id`),
  KEY `idx_mentor_status` (`status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='导师库';

-- ①b ★ 排序规则必须与 sys_user 一致！
--    本库默认排序规则是 utf8mb4_0900_ai_ci，而业务表（sys_user / register_application 等）
--    是 utf8mb4_general_ci。若 mentor 表用默认规则，任何 `mentor_name = sys_user.mentor_name`
--    的 JOIN 都会直接报 `1267 Illegal mix of collations`。故显式 CONVERT（幂等，可重复执行）。
ALTER TABLE `mentor` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- ② sys_user 增加 mentor_id（幂等：列不存在才加）
--    用 information_schema 判断后动态执行，避免重复 ALTER 报 Duplicate column。
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'mentor_id'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `sys_user` ADD COLUMN `mentor_id` bigint DEFAULT NULL COMMENT ''导师ID（mentor.id）'' AFTER `mentor_phone`',
  'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ③ 历史数据迁移：把 sys_user 上现存的 (姓名, 电话, 部门) 去重后收进 mentor 表。
--    ⚠️ 这里不写进 SQL 文件走脚本执行 —— INSERT ... SELECT 的目标表与来源表
--    在 MySQL 里做 NOT EXISTS 自引用容易踩 1093，改由 apply_mentor_migration.py
--    用 Python 去重后插入（可打印计数、可复核）。

-- ④ 权限菜单（导师管理页 + 按钮），grant 给 DEPT_ADMIN / SUPER_ADMIN / admin
--    同样由 Python 脚本按 menu_id 幂等插入（见脚本内的 MENU_ROWS）。
