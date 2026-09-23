-- ============================================================================
-- S1：实操题库对实习生开放开关（模拟备考 → 模拟实操题库）
--   question_bank.practice_enabled：该实操题库是否对实习生开放浏览（1=开放）
--   实习生端只读：bank_kind='PRACTICAL' AND bank_type IN ('PRACTICE','COMMON') AND practice_enabled=1
-- 幂等：先查 information_schema 再 ALTER，可重复执行
-- ============================================================================
USE `intern_assessment`;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'question_bank' AND COLUMN_NAME = 'practice_enabled');
SET @s := IF(@c = 0,
    'ALTER TABLE `question_bank` ADD COLUMN `practice_enabled` tinyint NOT NULL DEFAULT 0 COMMENT ''是否对实习生开放浏览(1开放):仅实操题库有效'' AFTER `bank_kind`',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.STATISTICS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'question_bank' AND INDEX_NAME = 'idx_qb_practice');
SET @s := IF(@c = 0,
    'ALTER TABLE `question_bank` ADD INDEX `idx_qb_practice` (`dept_id`, `bank_kind`, `bank_type`, `practice_enabled`)',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SELECT 'practice_enabled' AS 列名, COUNT(*) AS 存在 FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'question_bank' AND COLUMN_NAME = 'practice_enabled'
UNION ALL
SELECT 'idx_qb_practice', COUNT(*) FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'question_bank' AND INDEX_NAME = 'idx_qb_practice';
