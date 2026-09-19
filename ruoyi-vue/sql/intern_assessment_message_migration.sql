-- ============================================================
-- 融谷 · 实习生学习考核系统
-- P0：消息中心地基 —— notice 扩列 + notice_target / notice_read 两表 + 演示数据
--
-- 背景（2026-09-17 核实）：
--   notice 表已有 scope_type(ALL/DEPT/POSITION) + scope_id + is_top + effective_to，
--   但 **没有「指定人员」的接收关系表**、**没有「已读回执」表** →
--   未读红点、送达/已读统计都无法实现。本脚本补齐。
--
-- 幂等：notice 逐列先查 information_schema 再 ALTER；两张新表 CREATE TABLE IF NOT EXISTS；
--       演示数据用 INSERT ... WHERE NOT EXISTS 保证不重复。可反复执行。
-- ============================================================

-- ---------- 1. notice 扩列（逐列幂等） ----------
-- msg_type：ANNOUNCE公告 / TASK任务 / EXAM考核 / AUDIT审核 / URGE催办 / SYSTEM系统
-- biz_type + biz_id：业务锚点（点击跳转用）
-- link_url：显式跳转路径（优先用 biz_* 推导，二者兼容）
-- status：DRAFT / PUBLISHED / REVOKED
-- 说明：发送人沿用既有 publisher_id，不另加 sender_id（避免同义列）
SET @sql := (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notice' AND COLUMN_NAME = 'msg_type') = 0,
  'ALTER TABLE notice ADD COLUMN msg_type VARCHAR(24) NOT NULL DEFAULT ''ANNOUNCE'' COMMENT ''消息类型:ANNOUNCE/TASK/EXAM/AUDIT/URGE/SYSTEM'' AFTER scope_id',
  'SELECT 1'));
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notice' AND COLUMN_NAME = 'biz_type') = 0,
  'ALTER TABLE notice ADD COLUMN biz_type VARCHAR(24) DEFAULT NULL COMMENT ''业务类型:TASK/EXAM/REGISTER/PROMOTION'' AFTER msg_type',
  'SELECT 1'));
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notice' AND COLUMN_NAME = 'biz_id') = 0,
  'ALTER TABLE notice ADD COLUMN biz_id BIGINT DEFAULT NULL COMMENT ''业务主键（点击跳转用）'' AFTER biz_type',
  'SELECT 1'));
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notice' AND COLUMN_NAME = 'link_url') = 0,
  'ALTER TABLE notice ADD COLUMN link_url VARCHAR(255) DEFAULT NULL COMMENT ''显式跳转路径（可选，优先用 biz_* 推导）'' AFTER biz_id',
  'SELECT 1'));
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notice' AND COLUMN_NAME = 'status') = 0,
  'ALTER TABLE notice ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT ''PUBLISHED'' COMMENT ''DRAFT/PUBLISHED/REVOKED'' AFTER publish_time',
  'SELECT 1'));
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ---------- 2. 指定人员接收关系 ----------
CREATE TABLE IF NOT EXISTS `notice_target` (
  `id`        BIGINT NOT NULL AUTO_INCREMENT,
  `notice_id` BIGINT NOT NULL COMMENT '通知ID',
  `user_id`   BIGINT NOT NULL COMMENT '接收人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notice_user` (`notice_id`, `user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '通知定向接收人（scope_type=USER 时使用）';

-- ---------- 3. 已读回执 ----------
CREATE TABLE IF NOT EXISTS `notice_read` (
  `id`        BIGINT NOT NULL AUTO_INCREMENT,
  `notice_id` BIGINT NOT NULL COMMENT '通知ID',
  `user_id`   BIGINT NOT NULL COMMENT '已读人',
  `read_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '已读时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notice_user` (`notice_id`, `user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '通知已读回执（唯一键即幂等）';

-- ---------- 4. 演示数据（真实行，不是页面硬编码；不需要可整段删） ----------
-- 4.1 全公司公告
INSERT INTO `notice` (`title`, `content`, `scope_type`, `scope_id`, `is_top`, `effective_to`,
                      `publisher_id`, `publish_time`, `create_time`, `msg_type`, `status`)
SELECT '关于 2026 Q3 培养与考核安排的通知',
       '各部门实习生：Q3 正式考核将于 09-20 开始，理论环节 60 分钟、实操环节 5 天内提交。请提前完成必修课程，未完成将无法进入正式考核。',
       'ALL', NULL, 1, DATE_ADD(NOW(), INTERVAL 30 DAY), 100, NOW(), NOW(), 'ANNOUNCE', 'PUBLISHED'
WHERE NOT EXISTS (SELECT 1 FROM `notice` WHERE `title` = '关于 2026 Q3 培养与考核安排的通知');

-- 4.2 定向通知（给测试实习生）
INSERT INTO `notice` (`title`, `content`, `scope_type`, `scope_id`, `is_top`, `effective_to`,
                      `publisher_id`, `publish_time`, `create_time`, `msg_type`, `status`)
SELECT '请尽快完成保密协议签署',
       '你好，你的账号尚未完成保密协议签署。请点击下方按钮前往签署，签署后方可使用在线学习与考核功能。',
       'USER', NULL, 0, DATE_ADD(NOW(), INTERVAL 7 DAY), 100, NOW(), NOW(), 'SYSTEM', 'PUBLISHED'
WHERE NOT EXISTS (SELECT 1 FROM `notice` WHERE `title` = '请尽快完成保密协议签署');

INSERT INTO `notice_target` (`notice_id`, `user_id`)
SELECT n.id, u.user_id
  FROM `notice` n
  JOIN `sys_user` u ON u.user_name = 'test_dev_intern' AND u.del_flag = '0'
 WHERE n.title = '请尽快完成保密协议签署'
   AND NOT EXISTS (SELECT 1 FROM `notice_target` t WHERE t.notice_id = n.id AND t.user_id = u.user_id);

-- 4.3 部门通知（开发部门，考核提醒）
INSERT INTO `notice` (`title`, `content`, `scope_type`, `scope_id`, `is_top`, `effective_to`,
                      `publisher_id`, `publish_time`, `create_time`, `msg_type`, `status`)
SELECT '开发部门本周任务提交提醒',
       '请各位在 09-19 前完成「数据库索引优化实操」任务并提交作业，逾期将计入培养评价。',
       'DEPT', 104, 0, DATE_ADD(NOW(), INTERVAL 7 DAY), 101, NOW(), NOW(), 'TASK', 'PUBLISHED'
WHERE NOT EXISTS (SELECT 1 FROM `notice` WHERE `title` = '开发部门本周任务提交提醒');
