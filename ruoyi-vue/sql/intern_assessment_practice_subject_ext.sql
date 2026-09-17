-- =============================================================================
-- 模拟实操题库 扩展迁移脚本（幂等 · 可重复执行 · 不重建业务库）
-- -----------------------------------------------------------------------------
-- 背景：`practice_subject` 原来只有「题干 content + 附件 attachments_json」两个内容字段，
--       撑不起设计稿里实操题的完整信息结构（方向分组 / 交付要求 / 开发约束 /
--       建议用时 / 提交格式 / 命名规则 / 参考图）。
-- 说明：全部改动先查 information_schema 再 ALTER，重复执行不会报错。
-- 影响面：仅 practice_subject 一张表；不改任何已有列、不删数据。
-- 回滚：见文件末尾（保留原列，新增列可直接 DROP）。
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 0. 执行前快照（建议先跑一次，便于比对）
-- -----------------------------------------------------------------------------
SELECT COUNT(*) AS practice_subject_rows FROM `practice_subject`;

-- -----------------------------------------------------------------------------
-- 1. title 题名（卡片与详情页标题；旧数据回填为题干前 40 字）
-- -----------------------------------------------------------------------------
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'practice_subject' AND COLUMN_NAME = 'title');
SET @s := IF(@c = 0,
  'ALTER TABLE `practice_subject` ADD COLUMN `title` VARCHAR(200) DEFAULT NULL COMMENT ''题名（卡片/详情页标题）'' AFTER `dept_id`',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- -----------------------------------------------------------------------------
-- 2. direction 方向分组（设计稿：后端接口开发 / 数据访问开发 / 前端页面开发 …）
-- -----------------------------------------------------------------------------
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'practice_subject' AND COLUMN_NAME = 'direction');
SET @s := IF(@c = 0,
  'ALTER TABLE `practice_subject` ADD COLUMN `direction` VARCHAR(64) DEFAULT NULL COMMENT ''方向分组'' AFTER `title`',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- -----------------------------------------------------------------------------
-- 3. direction_desc 方向说明（分组标题右侧的一句话）
-- -----------------------------------------------------------------------------
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'practice_subject' AND COLUMN_NAME = 'direction_desc');
SET @s := IF(@c = 0,
  'ALTER TABLE `practice_subject` ADD COLUMN `direction_desc` VARCHAR(255) DEFAULT NULL COMMENT ''方向说明'' AFTER `direction`',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- -----------------------------------------------------------------------------
-- 4. difficulty 难度（EASY / MEDIUM / HARD）
-- -----------------------------------------------------------------------------
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'practice_subject' AND COLUMN_NAME = 'difficulty');
SET @s := IF(@c = 0,
  'ALTER TABLE `practice_subject` ADD COLUMN `difficulty` VARCHAR(16) DEFAULT ''MEDIUM'' COMMENT ''难度 EASY/MEDIUM/HARD'' AFTER `direction_desc`',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- -----------------------------------------------------------------------------
-- 5. estimated_minutes 建议用时（分钟）
-- -----------------------------------------------------------------------------
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'practice_subject' AND COLUMN_NAME = 'estimated_minutes');
SET @s := IF(@c = 0,
  'ALTER TABLE `practice_subject` ADD COLUMN `estimated_minutes` INT DEFAULT NULL COMMENT ''建议用时（分钟）'' AFTER `difficulty`',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- -----------------------------------------------------------------------------
-- 6. deliverables 交付要求（每行一条，前端按换行渲染 <ul>）
-- -----------------------------------------------------------------------------
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'practice_subject' AND COLUMN_NAME = 'deliverables');
SET @s := IF(@c = 0,
  'ALTER TABLE `practice_subject` ADD COLUMN `deliverables` TEXT DEFAULT NULL COMMENT ''交付要求（换行分隔）'' AFTER `estimated_minutes`',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- -----------------------------------------------------------------------------
-- 7. dev_constraints 开发约束（每行一条）
--    注意：列名不用 `constraints`——它是 MySQL 8 关键字，避免转义地狱。
-- -----------------------------------------------------------------------------
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'practice_subject' AND COLUMN_NAME = 'dev_constraints');
SET @s := IF(@c = 0,
  'ALTER TABLE `practice_subject` ADD COLUMN `dev_constraints` TEXT DEFAULT NULL COMMENT ''开发约束（换行分隔）'' AFTER `deliverables`',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- -----------------------------------------------------------------------------
-- 8. submit_format 提交格式
-- -----------------------------------------------------------------------------
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'practice_subject' AND COLUMN_NAME = 'submit_format');
SET @s := IF(@c = 0,
  'ALTER TABLE `practice_subject` ADD COLUMN `submit_format` VARCHAR(255) DEFAULT NULL COMMENT ''提交格式'' AFTER `dev_constraints`',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- -----------------------------------------------------------------------------
-- 9. naming_rule 命名规则
-- -----------------------------------------------------------------------------
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'practice_subject' AND COLUMN_NAME = 'naming_rule');
SET @s := IF(@c = 0,
  'ALTER TABLE `practice_subject` ADD COLUMN `naming_rule` VARCHAR(255) DEFAULT NULL COMMENT ''命名规则'' AFTER `submit_format`',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- -----------------------------------------------------------------------------
-- 10. reference_images 参考图 JSON：[{"name":"原型图.png","url":"/profile/upload/..."}]
-- -----------------------------------------------------------------------------
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'practice_subject' AND COLUMN_NAME = 'reference_images');
SET @s := IF(@c = 0,
  'ALTER TABLE `practice_subject` ADD COLUMN `reference_images` TEXT DEFAULT NULL COMMENT ''参考图JSON'' AFTER `naming_rule`',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- -----------------------------------------------------------------------------
-- 11. sort_no 排序号（同方向内手工排序，越小越靠前）
-- -----------------------------------------------------------------------------
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'practice_subject' AND COLUMN_NAME = 'sort_no');
SET @s := IF(@c = 0,
  'ALTER TABLE `practice_subject` ADD COLUMN `sort_no` INT DEFAULT 0 COMMENT ''排序号'' AFTER `reference_images`',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- -----------------------------------------------------------------------------
-- 12. 组合索引：部门 + 方向 + 状态（列表页与实习生端按方向分组都用它）
-- -----------------------------------------------------------------------------
SET @c := (SELECT COUNT(*) FROM information_schema.STATISTICS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'practice_subject' AND INDEX_NAME = 'idx_psubject_dir');
SET @s := IF(@c = 0,
  'ALTER TABLE `practice_subject` ADD KEY `idx_psubject_dir` (`dept_id`, `direction`, `status`)',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- -----------------------------------------------------------------------------
-- 13. 存量数据回填（只回填 NULL，不覆盖已有值）
-- -----------------------------------------------------------------------------
UPDATE `practice_subject`
SET `title` = LEFT(REPLACE(REPLACE(`content`, '\r', ' '), '\n', ' '), 40)
WHERE `deleted` = 0 AND (`title` IS NULL OR `title` = '') AND `content` IS NOT NULL;

UPDATE `practice_subject`
SET `direction` = '通用'
WHERE `deleted` = 0 AND (`direction` IS NULL OR `direction` = '');

UPDATE `practice_subject`
SET `direction_desc` = '本方向实操练习题'
WHERE `deleted` = 0 AND `direction` = '通用' AND (`direction_desc` IS NULL OR `direction_desc` = '');

UPDATE `practice_subject`
SET `sort_no` = `id`
WHERE `deleted` = 0 AND (`sort_no` IS NULL OR `sort_no` = 0);

-- -----------------------------------------------------------------------------
-- 14. 执行后校验
-- -----------------------------------------------------------------------------
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'practice_subject'
ORDER BY ORDINAL_POSITION;

SELECT `id`, `dept_id`, `title`, `direction`, `difficulty`, `estimated_minutes`, `status`
FROM `practice_subject` WHERE `deleted` = 0 ORDER BY `dept_id`, `direction`, `sort_no` LIMIT 30;

SELECT `direction`, COUNT(*) AS cnt
FROM `practice_subject` WHERE `deleted` = 0
GROUP BY `direction` ORDER BY cnt DESC;

-- =============================================================================
-- 回滚（仅在需要时手工执行；原列未动，删除新增列即可）
-- =============================================================================
-- ALTER TABLE `practice_subject`
--   DROP INDEX `idx_psubject_dir`,
--   DROP COLUMN `sort_no`, DROP COLUMN `reference_images`, DROP COLUMN `naming_rule`,
--   DROP COLUMN `submit_format`, DROP COLUMN `dev_constraints`, DROP COLUMN `deliverables`,
--   DROP COLUMN `estimated_minutes`, DROP COLUMN `difficulty`, DROP COLUMN `direction_desc`,
--   DROP COLUMN `direction`, DROP COLUMN `title`;
