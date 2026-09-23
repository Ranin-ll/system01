-- ==========================================================================
-- study_item 增加 media_seconds：视频文件的**真实时长（秒）**
--
-- 为什么要这一列（2026-09-23）：
--   1) 管理端填的 duration 是「预计时长（分钟）」，与视频实际长度经常对不上
--      （实测：资料写 10 分钟，视频其实只有 35 秒），列表/侧栏显示的时长是错的；
--   2) 后端算视频学习进度时没有真实时长，只能沿用「每秒最多涨 2%」的固定口径，
--      短片看完也到不了 100%（35 秒的视频自然增速是 2.9%/秒 > 2%/秒）→
--      表现为「看完了却没完成、进度条停在 60%」。
--   有了真实秒数，进度就能按「已看秒数 / 视频秒数」算，既准确又保留防作弊
--   （studyDuration 仍被真实在线时长钳制，报不了没看过的秒数）。
--
-- 幂等：已存在该列时跳过（MySQL 没有 ADD COLUMN IF NOT EXISTS，用 information_schema 判）。
-- ==========================================================================
SET @ddl = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'study_item'
        AND COLUMN_NAME = 'media_seconds'
    ),
    'SELECT ''media_seconds already exists'' AS skipped',
    'ALTER TABLE study_item ADD COLUMN media_seconds INT(11) DEFAULT NULL COMMENT ''视频文件真实时长（秒），上传时探测写入'' AFTER duration'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
