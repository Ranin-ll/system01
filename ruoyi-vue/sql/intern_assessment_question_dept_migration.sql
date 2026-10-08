-- ============================================================================
-- 题目直接按部门归属（2026-09-23 重构）：question 表补 dept_id
--
-- 背景：旧设计 question.bank_id → question_bank.dept_id 间接控部门范围；
--       重构后 question.dept_id 直接归属部门（一个部门 = 一个理论题池，
--       理论考核按 exam_knowledge_rule「知识点配比」抽题），bank_id 列保留但不再读写。
--
-- 症状：同事只提交了代码（QuestionMapper.xml / ExamRuleMapper.xml 用 q.dept_id），
--       未提交本迁移 ⇒ 库缺列，以下链路全报
--       `Unknown column 'q.dept_id' in 'where clause'`：
--         · 正式考核 → GET /business/exam/config/{id}          （ExamRuleMapper.selectKnowledgeRules）
--         · 模拟考核 → GET /business/exam/bank/{deptId}/knowledge-points
--                                                              （ExamRuleMapper.selectDeptKnowledgePoints）
--         · 组卷抽题 selectQuestionsByPoint / 题池概览 selectDeptAvailable
--
-- 回填：存量题按 bank_id → question_bank.dept_id 补齐（实测本库 161 题 100% 可映射）。
-- 幂等：先查 information_schema 再 ALTER，可重复执行。
-- ============================================================================
USE `intern_assessment`;

-- 1) 加列 dept_id
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'question' AND COLUMN_NAME = 'dept_id');
SET @s := IF(@c = 0,
    'ALTER TABLE `question` ADD COLUMN `dept_id` BIGINT NULL DEFAULT NULL COMMENT ''所属部门ID（题目直接按部门归属，关联sys_dept）'' AFTER `id`',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 2) 加索引
SET @i := (SELECT COUNT(*) FROM information_schema.STATISTICS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'question' AND INDEX_NAME = 'idx_question_dept');
SET @s := IF(@i = 0,
    'ALTER TABLE `question` ADD INDEX `idx_question_dept` (`dept_id`)',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 3) 回填存量题：从所属题库的部门补齐
UPDATE `question` q
  JOIN `question_bank` b ON b.`id` = q.`bank_id`
   SET q.`dept_id` = b.`dept_id`
 WHERE q.`dept_id` IS NULL;

-- 4) 校验
SELECT COUNT(*) AS 题目总数, SUM(dept_id IS NULL) AS 未归属部门数 FROM `question`;
SELECT dept_id AS 部门, COUNT(*) AS 题量 FROM `question` GROUP BY dept_id ORDER BY dept_id;
