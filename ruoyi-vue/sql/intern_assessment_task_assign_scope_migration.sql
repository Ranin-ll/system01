-- ============================================================================
-- 学习任务：发布对象可以是「整个岗位」或「指定实习生个人」
--
--   learning_task.assign_scope = 'POSITION'（默认）→ 发布时展开为该岗位全部在培实习生
--                              = 'USER'           → 只发给 task_target_user 里指定的人
--
--   约束：指定人员必须是**该岗位**的在培实习生（岗位仍需选，因为它决定课程可见性等既有口径）。
--
-- 幂等：MySQL 8 不支持 ADD COLUMN IF NOT EXISTS → 用 information_schema 判存在；
--       建表用 CREATE TABLE IF NOT EXISTS。
-- ============================================================================

SET @col_exists = (SELECT COUNT(*)
                     FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'learning_task'
                      AND COLUMN_NAME = 'assign_scope');

SET @ddl = IF(@col_exists > 0,
              'SELECT 1',
              'ALTER TABLE `learning_task` ADD COLUMN `assign_scope` varchar(20) DEFAULT ''POSITION'' COMMENT ''POSITION=按岗位全部；USER=指定人员'' AFTER `position_id`');

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 存量任务全部按「按岗位」处理（与改动前行为一致）
UPDATE `learning_task` SET `assign_scope` = 'POSITION' WHERE `assign_scope` IS NULL;

CREATE TABLE IF NOT EXISTS `task_target_user`
(
    `id`          bigint NOT NULL AUTO_INCREMENT,
    `task_id`     bigint NOT NULL COMMENT '所属任务',
    `user_id`     bigint NOT NULL COMMENT '被指定的实习生',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_target_task_user` (`task_id`, `user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='任务指定人员（assign_scope=USER 时生效）';
