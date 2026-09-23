-- ============================================================================
-- 模拟理论考核：套卷加「描述」字段（基础信息与组卷配置分离）
--   exam.description varchar(500) 套卷说明（如「覆盖 Java 基础与集合，适合入门自测」）
--   与 content_bias 的区别：
--     description  = 这套卷是什么（整体说明）
--     content_bias = 题目内容偏向（知识侧重，给实习生选卷参考）
-- 幂等：先查 information_schema 再 ALTER，可重复执行
-- ============================================================================
USE `intern_assessment`;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam' AND COLUMN_NAME = 'description');
SET @s := IF(@c = 0,
    'ALTER TABLE `exam` ADD COLUMN `description` varchar(500) NULL DEFAULT NULL COMMENT ''套卷描述/说明'' AFTER `exam_name`',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SELECT COLUMN_NAME AS 列名, COLUMN_TYPE AS 类型, IS_NULLABLE AS 可空, COLUMN_COMMENT AS 注释
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam'
  AND COLUMN_NAME IN ('description','difficulty','content_bias') ORDER BY ORDINAL_POSITION;
