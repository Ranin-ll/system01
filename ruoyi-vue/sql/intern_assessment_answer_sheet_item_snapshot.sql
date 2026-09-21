-- =============================================================================
-- 答卷题目明细 · 题干 / 满分快照迁移脚本（幂等 · 可重复执行）
-- -----------------------------------------------------------------------------
-- 背景：批阅界面报「null 的分数超过了本题满分 null 分」，且每题满分显示成 0 分。
--       根因：answer_sheet_item 只存 subject_item_id / question_id，题干与满分靠
--       `selectItemsBySheetId` 实时 LEFT JOIN question + exam_subject_item 取。
--       但这两张题目表是**管理员可编辑、可删除**的：管理员改版题目时会删旧行、插新行，
--       于是旧答卷仍指向已不存在的题目行 → JOIN 取不到值 → stem / score 变 NULL。
--       实测：exam 31 的题目原为 id 50/51，后来被重建为 54/55；
--             answer_sheet_item 18 号答卷仍指向 50/51 → 该卷每题题干与满分都是 NULL。
-- 做法：在 answer_sheet_item 上落两个快照列，进入考核（startExam）时写入，
--       查询改为 COALESCE(快照, 题目表) —— 快照优先，保证"批阅按当时考的那份卷"。
-- 回填：用题目表当前值回填历史行（能取到的就补上）；题目行已被删的取不到，
--       保持 NULL，前端显示「满分未记录」，**不做任何猜测性赋值**。
-- 说明：先查 information_schema 再 ALTER，重复执行不报错；不删任何已有列与数据。
-- 回滚：见文件末尾。
-- =============================================================================

USE `intern_assessment`;

-- -----------------------------------------------------------------------------
-- 0. 执行前快照
-- -----------------------------------------------------------------------------
SELECT COUNT(*) AS answer_sheet_item_rows_before FROM `answer_sheet_item`;

SELECT COUNT(*) AS null_stem_before FROM `answer_sheet_item` i
  LEFT JOIN `question` q ON q.id = i.question_id
  LEFT JOIN `exam_subject_item` si ON si.id = i.subject_item_id
 WHERE COALESCE(q.stem, si.title) IS NULL;

-- -----------------------------------------------------------------------------
-- 1. answer_sheet_item 增加题干快照列 stem_snapshot
-- -----------------------------------------------------------------------------
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'answer_sheet_item' AND COLUMN_NAME = 'stem_snapshot');
SET @s := IF(@c = 0,
  'ALTER TABLE `answer_sheet_item` ADD COLUMN `stem_snapshot` TEXT NULL COMMENT ''题干快照（进入考核时写入，防题目被改动后批阅取不到）'' AFTER `subject_item_id`',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- -----------------------------------------------------------------------------
-- 2. answer_sheet_item 增加本题满分快照列 full_score
-- -----------------------------------------------------------------------------
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'answer_sheet_item' AND COLUMN_NAME = 'full_score');
SET @s := IF(@c = 0,
  'ALTER TABLE `answer_sheet_item` ADD COLUMN `full_score` DECIMAL(6,2) NULL COMMENT ''本题满分快照（进入考核时写入）'' AFTER `stem_snapshot`',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- -----------------------------------------------------------------------------
-- 3. 回填历史行：优先题目表当前值，取不到就留 NULL（不猜测）
-- -----------------------------------------------------------------------------
UPDATE `answer_sheet_item` i
  LEFT JOIN `question` q ON q.id = i.question_id
  LEFT JOIN `exam_subject_item` si ON si.id = i.subject_item_id
   SET i.stem_snapshot = COALESCE(i.stem_snapshot, q.stem, si.title),
       i.full_score    = COALESCE(i.full_score,    q.score, si.score)
 WHERE i.stem_snapshot IS NULL OR i.full_score IS NULL;

-- -----------------------------------------------------------------------------
-- 4. 执行后核验
-- -----------------------------------------------------------------------------
SELECT COUNT(*) AS has_stem_snapshot_column FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'answer_sheet_item' AND COLUMN_NAME = 'stem_snapshot';

SELECT COUNT(*) AS has_full_score_column FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'answer_sheet_item' AND COLUMN_NAME = 'full_score';

-- 回填后仍缺快照的行（示例：18 号答卷这种"题目已被重建"的历史卷）
SELECT id, answer_sheet_id, seq, subject_item_id, question_id, stem_snapshot, full_score
  FROM `answer_sheet_item`
 WHERE stem_snapshot IS NULL OR full_score IS NULL
 ORDER BY answer_sheet_id, seq;

-- -----------------------------------------------------------------------------
-- 回滚
-- -----------------------------------------------------------------------------
-- SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
--            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'answer_sheet_item' AND COLUMN_NAME = 'full_score');
-- SET @s := IF(@c > 0, 'ALTER TABLE `answer_sheet_item` DROP COLUMN `full_score`', 'SELECT 1');
-- PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
--            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'answer_sheet_item' AND COLUMN_NAME = 'stem_snapshot');
-- SET @s := IF(@c > 0, 'ALTER TABLE `answer_sheet_item` DROP COLUMN `stem_snapshot`', 'SELECT 1');
-- PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
