-- ============================================================================
-- 实习生考核模块 - 数据库改造
-- 1. exam 表补充抽题数量字段
-- 2. answer_sheet 表加唯一约束（每个实习生每个考核只能参加一次）
-- ============================================================================

USE `intern_assessment`;

-- 1. exam 表：抽题数量（发布时从题库抽取的客观题数量）
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam' AND COLUMN_NAME = 'question_count');
SET @s := IF(@c = 0,
  'ALTER TABLE `exam` ADD COLUMN `question_count` INT NOT NULL DEFAULT 10 COMMENT ''客观题抽题数量'' AFTER `bank_id`',
  'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 1.2 exam 表：实操题数量（发布时从题库抽取的实操题数量）
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam' AND COLUMN_NAME = 'subject_count');
SET @s := IF(@c = 0,
  'ALTER TABLE `exam` ADD COLUMN `subject_count` INT NOT NULL DEFAULT 0 COMMENT ''实操题数量'' AFTER `question_count`',
  'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 1.1 exam 表：position_id 改为可空（考核主要挂题库 bank_id + 部门 dept_id，position_id 为历史遗留字段，不再必填）
ALTER TABLE `exam`
    MODIFY COLUMN `position_id` BIGINT DEFAULT NULL COMMENT '岗位ID（可选，考核主要挂题库和部门）';

-- 2. answer_sheet 表：唯一约束，保证一人一考一次
SET @c := (SELECT COUNT(*) FROM information_schema.STATISTICS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'answer_sheet' AND INDEX_NAME = 'uk_sheet_exam_user');
SET @s := IF(@c = 0,
  'ALTER TABLE `answer_sheet` ADD UNIQUE KEY `uk_sheet_exam_user` (`exam_id`, `user_id`)',
  'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 3. exam 表：实操考核字段（理论/实操拆分，实操考核管理员发布题干+附件）
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam' AND COLUMN_NAME = 'subject_content');
SET @s := IF(@c = 0,
  'ALTER TABLE `exam` ADD COLUMN `subject_content` VARCHAR(2000) DEFAULT NULL COMMENT ''实操题题干（实操考核）'' AFTER `subject_count`',
  'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam' AND COLUMN_NAME = 'subject_attachment');
SET @s := IF(@c = 0,
  'ALTER TABLE `exam` ADD COLUMN `subject_attachment` VARCHAR(500) DEFAULT NULL COMMENT ''实操题参考附件路径（实操考核）'' AFTER `subject_content`',
  'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 4. answer_sheet 表：实习生实操作答文件路径
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'answer_sheet' AND COLUMN_NAME = 'subject_answer');
SET @s := IF(@c = 0,
  'ALTER TABLE `answer_sheet` ADD COLUMN `subject_answer` VARCHAR(500) DEFAULT NULL COMMENT ''实操考核作答文件路径'' AFTER `submit_type`',
  'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 5. exam 表：考核性质（正式/模拟）
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam' AND COLUMN_NAME = 'exam_mode');
SET @s := IF(@c = 0,
  'ALTER TABLE `exam` ADD COLUMN `exam_mode` VARCHAR(16) NOT NULL DEFAULT ''FORMAL'' COMMENT ''考核性质：FORMAL正式/PRACTICE模拟'' AFTER `exam_type`',
  'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 旧结构将 PRACTICE/FORMAL 存在 exam_type；迁移后 exam_type 表示 THEORY/PRACTICAL。
UPDATE `exam`
SET `exam_mode` = `exam_type`, `exam_type` = 'THEORY'
WHERE `exam_type` IN ('PRACTICE', 'FORMAL');

-- 验证
SHOW COLUMNS FROM `exam` LIKE 'subject_%';
SHOW COLUMNS FROM `answer_sheet` LIKE 'subject_answer';
