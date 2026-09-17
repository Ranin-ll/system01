-- ============================================================================
-- 实习生学习考核系统 — 考核模块 schema 补桥脚本
-- ============================================================================
-- 背景：同事分支 feature/register-and-assessment 的代码与随附 SQL 脚本不同步。
--       下列 11 个字段被 Mapper / 实体直接使用，但任何建表脚本都没有创建，
--       属于同事在本机手工加过、却漏提交 DDL 的部分。不补会造成：
--         · 题库管理页打开即报 Unknown column 'b.bank_type'
--         · 模拟考核找不到本部门模拟题库（selectPracticeBankByDept 失效）
--         · 新增考核、答卷批改 INSERT/UPDATE 直接抛 Unknown column
--
-- 幂等：全部经 information_schema 判定后再执行，可重复运行。
-- 执行时机（重要）：
--   intern_assessment_question_bank.sql  →  本脚本  →  intern_assessment_exam_module.sql
--   （本脚本为 exam_module.sql 补上它 `AFTER bank_id` 所依赖的 exam.bank_id，
--     同时为 question_bank 补上 bank_type，故必须夹在两者之间。）
-- 数据库：intern_assessment
-- ============================================================================

USE `intern_assessment`;

-- ----------------------------------------------------------------------------
-- 1. exam：题库归属 + 分题型抽题数量与分值（Mapper 与 Exam.java 均依赖）
-- ----------------------------------------------------------------------------
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam' AND COLUMN_NAME = 'bank_id');
SET @s := IF(@c = 0,
  'ALTER TABLE `exam` ADD COLUMN `bank_id` BIGINT DEFAULT NULL COMMENT ''所属题库ID（关联question_bank）'' AFTER `exam_type`, ADD INDEX `idx_exam_bank` (`bank_id`)',
  'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam' AND COLUMN_NAME = 'question_count');
SET @s := IF(@c = 0,
  'ALTER TABLE `exam` ADD COLUMN `question_count` INT NOT NULL DEFAULT 10 COMMENT ''客观题抽题数量'' AFTER `bank_id`',
  'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam' AND COLUMN_NAME = 'single_count');
SET @s := IF(@c = 0,
  'ALTER TABLE `exam` ADD COLUMN `single_count` INT NOT NULL DEFAULT 0 COMMENT ''单选题数量'' AFTER `question_count`',
  'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam' AND COLUMN_NAME = 'multi_count');
SET @s := IF(@c = 0,
  'ALTER TABLE `exam` ADD COLUMN `multi_count` INT NOT NULL DEFAULT 0 COMMENT ''多选题数量'' AFTER `single_count`',
  'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam' AND COLUMN_NAME = 'judge_count');
SET @s := IF(@c = 0,
  'ALTER TABLE `exam` ADD COLUMN `judge_count` INT NOT NULL DEFAULT 0 COMMENT ''判断题数量'' AFTER `multi_count`',
  'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam' AND COLUMN_NAME = 'single_score');
SET @s := IF(@c = 0,
  'ALTER TABLE `exam` ADD COLUMN `single_score` DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT ''单选题每题分值'' AFTER `judge_count`',
  'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam' AND COLUMN_NAME = 'multi_score');
SET @s := IF(@c = 0,
  'ALTER TABLE `exam` ADD COLUMN `multi_score` DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT ''多选题每题分值'' AFTER `single_score`',
  'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam' AND COLUMN_NAME = 'judge_score');
SET @s := IF(@c = 0,
  'ALTER TABLE `exam` ADD COLUMN `judge_score` DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT ''判断题每题分值'' AFTER `multi_score`',
  'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- ----------------------------------------------------------------------------
-- 2. question：主观题/实操题附件（QuestionMapper 与 Question.java 依赖）
-- ----------------------------------------------------------------------------
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'question' AND COLUMN_NAME = 'attachment_url');
SET @s := IF(@c = 0,
  'ALTER TABLE `question` ADD COLUMN `attachment_url` VARCHAR(500) DEFAULT NULL COMMENT ''题目附件路径（主观题/实操题）'' AFTER `options_json`',
  'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- ----------------------------------------------------------------------------
-- 3. answer_sheet：整卷人工评分与评语（AnswerSheetMapper 依赖）
--    注意：answer_sheet_item 上已有同名逐题字段，此处是「整卷」级别，不可混用。
-- ----------------------------------------------------------------------------
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'answer_sheet' AND COLUMN_NAME = 'manual_score');
SET @s := IF(@c = 0,
  'ALTER TABLE `answer_sheet` ADD COLUMN `manual_score` DECIMAL(8,2) DEFAULT NULL COMMENT ''人工调整总分'' AFTER `ai_score`',
  'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'answer_sheet' AND COLUMN_NAME = 'manual_comment');
SET @s := IF(@c = 0,
  'ALTER TABLE `answer_sheet` ADD COLUMN `manual_comment` TEXT DEFAULT NULL COMMENT ''人工总评语'' AFTER `manual_score`',
  'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- ----------------------------------------------------------------------------
-- 4. question_bank：题库类型（FORMAL 正式 / PRACTICE 模拟）
--    依赖：必须在 intern_assessment_question_bank.sql 建表之后执行。
--    PRACTICE 用于「模拟考核」，每部门一个，且不可删除。
-- ----------------------------------------------------------------------------
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'question_bank' AND COLUMN_NAME = 'bank_type');
SET @s := IF(@c = 0,
  'ALTER TABLE `question_bank` ADD COLUMN `bank_type` VARCHAR(24) NOT NULL DEFAULT ''FORMAL'' COMMENT ''题库类型：FORMAL正式/PRACTICE模拟'' AFTER `bank_name`, ADD INDEX `idx_bank_type` (`bank_type`)',
  'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- ----------------------------------------------------------------------------
-- 5. 为五个业务部门初始化正式题库和模拟题库
--    通过部门名称取 dept_id，避免在其它环境迁移时依赖固定主键。
--    只补缺失的题库，不覆盖管理员已经维护的数据。
-- ----------------------------------------------------------------------------
INSERT INTO `question_bank` (`bank_name`, `bank_type`, `dept_id`, `description`, `status`, `question_count`)
SELECT CONCAT(d.`dept_name`, '模拟题库'), 'PRACTICE', d.`dept_id`, '本部门模拟考核题库', 'ENABLED', 0
FROM `sys_dept` d
WHERE d.`dept_name` IN ('交付部门', '开发部门', '设计部门', '质检部门', '建模部门')
  AND d.`del_flag` = '0'
  AND NOT EXISTS (
      SELECT 1 FROM `question_bank` b
      WHERE b.`dept_id` = d.`dept_id` AND b.`bank_type` = 'PRACTICE' AND b.`deleted` = 0
  );

INSERT INTO `question_bank` (`bank_name`, `bank_type`, `dept_id`, `description`, `status`, `question_count`)
SELECT CONCAT(d.`dept_name`, '正式题库'), 'FORMAL', d.`dept_id`, '本部门正式考核题库', 'ENABLED', 0
FROM `sys_dept` d
WHERE d.`dept_name` IN ('交付部门', '开发部门', '设计部门', '质检部门', '建模部门')
  AND d.`del_flag` = '0'
  AND NOT EXISTS (
      SELECT 1 FROM `question_bank` b
      WHERE b.`dept_id` = d.`dept_id` AND b.`bank_type` = 'FORMAL' AND b.`deleted` = 0
  );

-- ----------------------------------------------------------------------------
-- 6. 验证
-- ----------------------------------------------------------------------------
SELECT 'exam 缺失列' AS `检查项`,
       GROUP_CONCAT(`COLUMN_NAME`) AS `已存在`,
       IF(COUNT(*) >= 7, 'OK', CONCAT('仅 ', COUNT(*), '/7')) AS `结果`
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam'
  AND `COLUMN_NAME` IN ('bank_id','single_count','multi_count','judge_count','single_score','multi_score','judge_score');

SELECT 'answer_sheet 缺失列' AS `检查项`,
       GROUP_CONCAT(`COLUMN_NAME`) AS `已存在`,
       IF(COUNT(*) >= 2, 'OK', CONCAT('仅 ', COUNT(*), '/2')) AS `结果`
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'answer_sheet'
  AND `COLUMN_NAME` IN ('manual_score','manual_comment');

SELECT `id`, `bank_name`, `bank_type`, `dept_id`, `status` FROM `question_bank` ORDER BY `id`;
