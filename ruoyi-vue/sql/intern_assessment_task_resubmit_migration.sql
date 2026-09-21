-- ============================================================================
-- 学习任务：是否允许重复提交
--
--   allow_resubmit = 1（默认，保持既有行为）：实习生可反复提交，每次 version + 1
--   allow_resubmit = 0：每人只能提交一次；**但被批阅人退回后仍可重新提交**
--                       （退回本身就是要求重做，若连重交都禁止就自相矛盾了）
--
-- 幂等：MySQL 8 不支持 ADD COLUMN IF NOT EXISTS → 用 information_schema 判存在。
-- ============================================================================

SET @col_exists = (SELECT COUNT(*)
                     FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'learning_task'
                      AND COLUMN_NAME = 'allow_resubmit');

SET @ddl = IF(@col_exists > 0,
              'SELECT 1',
              'ALTER TABLE `learning_task` ADD COLUMN `allow_resubmit` tinyint DEFAULT 1 COMMENT ''1=允许多次提交；0=仅一次（被退回后仍可重交）'' AFTER `need_assignment`');

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 存量任务按「允许」处理（与改动前行为一致，不影响已有数据）
UPDATE `learning_task` SET `allow_resubmit` = 1 WHERE `allow_resubmit` IS NULL;

-- ============================================================================
-- 存量数据对齐：同一分配下只保留最新一版的「内容与附件」
--   （行保留 → 提交次数与批阅痕迹完整；旧内容与旧附件清掉）
--   注：磁盘上的旧附件需要另行清理，脚本执行方按清理前的 file_url 列表删文件。
-- ============================================================================
UPDATE `task_submission` s
  JOIN (SELECT assignment_id, MAX(version) AS mv
          FROM `task_submission` GROUP BY assignment_id) t
    ON t.assignment_id = s.assignment_id
   SET s.content = NULL, s.file_url = NULL, s.file_name = NULL
 WHERE s.version < t.mv;
