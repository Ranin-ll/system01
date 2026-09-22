-- ============================================================================
-- 模拟理论考核：套卷补「难易程度」与「题目内容偏向」
--   exam.difficulty   难易程度：EASY 简单 / MEDIUM 中等 / HARD 困难（可空=未设置）
--   exam.content_bias 题目内容偏向：给实习生看的文字备注（如「偏 Java 集合与并发」）
--   说明：两张字段对 FORMAL 也可见但非必填；当前仅「模拟理论考核」配置页使用
-- 幂等：先查 information_schema 再 ALTER，可重复执行
-- ============================================================================
USE `intern_assessment`;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam' AND COLUMN_NAME = 'difficulty');
SET @s := IF(@c = 0,
    'ALTER TABLE `exam` ADD COLUMN `difficulty` varchar(24) NULL DEFAULT NULL COMMENT ''难易程度：EASY简单/MEDIUM中等/HARD困难(空=未设置)'' AFTER `pass_line`',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam' AND COLUMN_NAME = 'content_bias');
SET @s := IF(@c = 0,
    'ALTER TABLE `exam` ADD COLUMN `content_bias` varchar(500) NULL DEFAULT NULL COMMENT ''题目内容偏向（给实习生看的侧重备注）'' AFTER `difficulty`',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SELECT COLUMN_NAME AS 列名, COLUMN_TYPE AS 类型, IS_NULLABLE AS 可空, COLUMN_COMMENT AS 注释
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam' AND COLUMN_NAME IN ('difficulty','content_bias')
ORDER BY ORDINAL_POSITION;
