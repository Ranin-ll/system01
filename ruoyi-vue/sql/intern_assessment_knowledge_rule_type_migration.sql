-- ============================================================================
-- 知识点配比从「单一总数」改为「按题型分别分配」
--
-- 背景：原先 exam_knowledge_rule 只有 question_count（该知识点抽几题），
--       具体抽成 单选/多选/判断 各几道，是后端按卷面的全局题型比例**猜**的
--       （PaperDrawer.splitByTypeRatio）。管理员无法指定「这个知识点我要 3 单选 1 多选」。
--
-- 本次加三列显式记录分题型数量：
--   single_count / multi_count / judge_count
--   question_count 继续保留为**行小计**（= 三者之和），保证旧读路径与列表展示不破。
--
-- 回填（针对本次改动前已有的存量行）：按所属 exam 的卷面题型比例拆分 question_count，
--   judge = floor(q*j/T)、multi = floor(q*m/T)、single = q - multi - judge
--   （余数给单选，保证 single+multi+judge 严格 == question_count）。
--   exam 没设题型数量（T=0）时：全部计入 single_count（等价于旧的「按知识点抽 N 题不分题型」）。
--
-- 幂等：先查 information_schema 再 ALTER，可重复执行。
-- ============================================================================
USE `intern_assessment`;

-- 1) 加三列
SET @s := (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `exam_knowledge_rule` ADD COLUMN `single_count` INT NOT NULL DEFAULT 0 COMMENT ''该知识点抽单选数量'' AFTER `question_count`',
    'SELECT 1') FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam_knowledge_rule' AND COLUMN_NAME = 'single_count');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s := (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `exam_knowledge_rule` ADD COLUMN `multi_count` INT NOT NULL DEFAULT 0 COMMENT ''该知识点抽多选数量'' AFTER `single_count`',
    'SELECT 1') FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam_knowledge_rule' AND COLUMN_NAME = 'multi_count');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s := (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `exam_knowledge_rule` ADD COLUMN `judge_count` INT NOT NULL DEFAULT 0 COMMENT ''该知识点抽判断数量'' AFTER `multi_count`',
    'SELECT 1') FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam_knowledge_rule' AND COLUMN_NAME = 'judge_count');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 2) 回填存量行（只补三列都还是 0 且 question_count > 0 的行）
UPDATE `exam_knowledge_rule` r
  JOIN `exam` e ON e.`id` = r.`exam_id`
   SET r.`judge_count` = CASE WHEN (e.`single_count` + e.`multi_count` + e.`judge_count`) > 0
                              THEN FLOOR(r.`question_count` * e.`judge_count` / (e.`single_count` + e.`multi_count` + e.`judge_count`))
                              ELSE 0 END,
       r.`multi_count` = CASE WHEN (e.`single_count` + e.`multi_count` + e.`judge_count`) > 0
                              THEN FLOOR(r.`question_count` * e.`multi_count` / (e.`single_count` + e.`multi_count` + e.`judge_count`))
                              ELSE 0 END
 WHERE r.`question_count` > 0 AND r.`single_count` = 0 AND r.`multi_count` = 0 AND r.`judge_count` = 0;

UPDATE `exam_knowledge_rule` r
  JOIN `exam` e ON e.`id` = r.`exam_id`
   SET r.`single_count` = r.`question_count` - r.`multi_count` - r.`judge_count`
 WHERE r.`question_count` > 0 AND r.`single_count` = 0;

-- 3) 校验
SELECT COUNT(*) AS 总行数,
       SUM(single_count + multi_count + judge_count = question_count) AS 小计一致行数,
       SUM(single_count + multi_count + judge_count) AS 分题型合计,
       SUM(question_count) AS 小计合计
  FROM `exam_knowledge_rule`;
