-- =============================================================================
-- 模拟考核记录 · 记录「出自哪个考核 / 哪个模块」迁移脚本（幂等 · 可重复执行）
-- -----------------------------------------------------------------------------
-- 背景：实习生端「模拟考核记录」原来展示的是「题库」（practice_record.bank_id → question_bank）。
--       但一个题库会被多个模块的考核复用，历史记录里 bank_id 全是同一个值，
--       这一列没有任何区分度；用户看到的是"每条记录都写着同一个题库名"。
--       现在改为按「模块」归类，并要求在模块页里能看到该模块的考核记录 + 对应考核名称。
-- 做法：practice_record 增加 exam_id（指向 exam.id），查询时 LEFT JOIN exam + practice_module
--       取 exam_name / module_id / module_name 作为展示字段。
--       沿用 practice_record → question_bank 的「实时 JOIN 取展示名」风格，不存名称快照。
-- 注意：**历史记录的 exam_id 为 NULL**（落库时没有记录过），迁移里不做任何猜测性回填，
--       前端对 NULL 显示「—」。回填只能靠人工核对。
-- 说明：先查 information_schema 再 ALTER，重复执行不报错；不删任何已有列与数据。
-- 回滚：见文件末尾。
-- =============================================================================

USE `intern_assessment`;

-- -----------------------------------------------------------------------------
-- 0. 执行前快照
-- -----------------------------------------------------------------------------
SELECT COUNT(*) AS practice_record_rows_before FROM `practice_record`;

-- -----------------------------------------------------------------------------
-- 1. practice_record 增加 exam_id
-- -----------------------------------------------------------------------------
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'practice_record' AND COLUMN_NAME = 'exam_id');
SET @s := IF(@c = 0,
  'ALTER TABLE `practice_record` ADD COLUMN `exam_id` BIGINT DEFAULT NULL COMMENT ''来源考核ID(exam.id, 模拟理论考核)'' AFTER `bank_id`',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.STATISTICS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'practice_record' AND INDEX_NAME = 'idx_precord_exam');
SET @s := IF(@c = 0,
  'ALTER TABLE `practice_record` ADD KEY `idx_precord_exam` (`exam_id`)',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- -----------------------------------------------------------------------------
-- 2. 执行后核验
-- -----------------------------------------------------------------------------
SELECT COUNT(*) AS has_exam_id_column FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'practice_record' AND COLUMN_NAME = 'exam_id';

SELECT COUNT(*) AS has_exam_id_index FROM information_schema.STATISTICS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'practice_record' AND INDEX_NAME = 'idx_precord_exam';

SELECT COUNT(*) AS legacy_rows_without_exam_id FROM `practice_record` WHERE `exam_id` IS NULL;

-- -----------------------------------------------------------------------------
-- 3. 参考回填（**默认注释掉，需人工核对后再执行**）
-- -----------------------------------------------------------------------------
-- 本项目 dev 库的存量记录 bank_id 全是 6（开发部门模拟题库），
-- 而 dept 104「默认模块」下挂 bankId=6 的考核只有 exam 35（默认模块 · 理论自测）。
-- 如果确认这批测试记录都来自 exam 35，可手工执行下面这句；否则保持 NULL。
--
-- UPDATE `practice_record` SET `exam_id` = 35
--  WHERE `exam_id` IS NULL AND `bank_id` = 6 AND `dept_id` = 104;

-- -----------------------------------------------------------------------------
-- 回滚
-- -----------------------------------------------------------------------------
-- SET @c := (SELECT COUNT(*) FROM information_schema.STATISTICS
--            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'practice_record' AND INDEX_NAME = 'idx_precord_exam');
-- SET @s := IF(@c > 0, 'ALTER TABLE `practice_record` DROP KEY `idx_precord_exam`', 'SELECT 1');
-- PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
--            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'practice_record' AND COLUMN_NAME = 'exam_id');
-- SET @s := IF(@c > 0, 'ALTER TABLE `practice_record` DROP COLUMN `exam_id`', 'SELECT 1');
-- PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
