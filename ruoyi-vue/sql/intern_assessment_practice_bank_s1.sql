-- ============================================================================
-- 题库形态（理论 / 实操）与实操题题库归属 —— S1 幂等迁移
--
-- 背景：
--   · 题库容器统一为 question_bank，新增 bank_kind 区分「理论题库 / 实操题库」；
--     与既有的 bank_type（FORMAL 正式 / PRACTICE 模拟 / COMMON 通用）正交。
--   · 实操题沿用现有 practice_subject 的字段模型，但补上「题库归属」：
--       bank_id        —— 所属实操题库（question_bank.id，bank_kind='PRACTICAL'）
--       suggest_score  —— 建议满分（挑进考核时带出默认分值）
--       chapter        —— 章节（与理论题库口径统一，便于按章节组织/统计）
--     module_id 放开为可空：实操题可以只归题库、不挂备考模块。
--
-- 特性：幂等（先查 information_schema 再 ALTER），可重复执行。
-- ============================================================================
USE `intern_assessment`;

-- ---------------------------------------------------------------------------
-- 1. question_bank 增加 bank_kind
-- ---------------------------------------------------------------------------
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'question_bank' AND COLUMN_NAME = 'bank_kind');
SET @s := IF(@c = 0,
    'ALTER TABLE `question_bank` ADD COLUMN `bank_kind` varchar(24) NOT NULL DEFAULT ''THEORY'' COMMENT ''题库形态：THEORY理论题库 / PRACTICAL实操题库'' AFTER `bank_type`',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- ---------------------------------------------------------------------------
-- 2. practice_subject 增加 bank_id / suggest_score / chapter
-- ---------------------------------------------------------------------------
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'practice_subject' AND COLUMN_NAME = 'bank_id');
SET @s := IF(@c = 0,
    'ALTER TABLE `practice_subject` ADD COLUMN `bank_id` bigint DEFAULT NULL COMMENT ''所属实操题库(question_bank.id，bank_kind=PRACTICAL)'' AFTER `id`',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'practice_subject' AND COLUMN_NAME = 'suggest_score');
SET @s := IF(@c = 0,
    'ALTER TABLE `practice_subject` ADD COLUMN `suggest_score` decimal(6,2) DEFAULT NULL COMMENT ''建议满分（挑进考核时带出默认分值）'' AFTER `difficulty`',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'practice_subject' AND COLUMN_NAME = 'chapter');
SET @s := IF(@c = 0,
    'ALTER TABLE `practice_subject` ADD COLUMN `chapter` varchar(64) DEFAULT NULL COMMENT ''章节（与理论题库口径统一）'' AFTER `suggest_score`',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- ---------------------------------------------------------------------------
-- 3. practice_subject.module_id 放开为可空（实操题可以只归题库、不挂模块）
-- ---------------------------------------------------------------------------
SET @n := (SELECT IS_NULLABLE FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'practice_subject' AND COLUMN_NAME = 'module_id');
SET @s := IF(@n = 'NO',
    'ALTER TABLE `practice_subject` MODIFY COLUMN `module_id` bigint DEFAULT NULL COMMENT ''所属模块ID(practice_module.id)，可空：只归题库不挂模块''',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- ---------------------------------------------------------------------------
-- 4. 校验
-- ---------------------------------------------------------------------------
SELECT 'question_bank.bank_kind' AS 列名, COUNT(*) AS 存在
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'question_bank' AND COLUMN_NAME = 'bank_kind'
UNION ALL
SELECT 'practice_subject.bank_id', COUNT(*) FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'practice_subject' AND COLUMN_NAME = 'bank_id'
UNION ALL
SELECT 'practice_subject.suggest_score', COUNT(*) FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'practice_subject' AND COLUMN_NAME = 'suggest_score'
UNION ALL
SELECT 'practice_subject.chapter', COUNT(*) FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'practice_subject' AND COLUMN_NAME = 'chapter'
UNION ALL
SELECT 'practice_subject.module_id 可空(1=是)', IF(IS_NULLABLE = 'YES', 1, 0) FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'practice_subject' AND COLUMN_NAME = 'module_id';

-- 理论题库数量 / 仍为未设置形态的数量（迁移后理论库应自动继承默认值 THEORY）
SELECT COUNT(*) AS 题库总数, SUM(bank_kind = 'THEORY') AS 理论库数, SUM(bank_kind = 'PRACTICAL') AS 实操库数
FROM `question_bank` WHERE deleted = 0;
