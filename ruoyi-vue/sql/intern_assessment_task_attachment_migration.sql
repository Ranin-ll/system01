-- ============================================================================
-- 学习任务附件相关迁移（幂等，可重复执行）
--
-- 一、task_attachment —— 任务资料表（部门管理员上传 → 实习生下载）
-- 二、task_submission.file_name —— 实习生作业附件的原始文件名
--
-- 说明：任务资料（发布方给）与作业附件（接收方交）是两件事，刻意分开放：
--      前者在 task_attachment（任务级、一对多），后者在 task_submission.file_url/file_name
--      （跟着某一次提交走，版本化）。
-- ============================================================================

-- ---------------------------------------------------------------- 一、任务资料表
CREATE TABLE IF NOT EXISTS `task_attachment`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT,
    `task_id`     bigint       NOT NULL COMMENT '所属任务',
    `file_name`   varchar(255) NOT NULL COMMENT '原始文件名（展示与下载都用它）',
    `file_url`    varchar(500) NOT NULL COMMENT '相对路径，如 /profile/upload/2026/09/18/xxx.pptx',
    `file_size`   bigint                DEFAULT '0' COMMENT '字节',
    `file_ext`    varchar(20)           DEFAULT NULL,
    `uploader_id` bigint                DEFAULT NULL COMMENT '上传人（部门管理员）',
    `deleted`     tinyint               DEFAULT '0' COMMENT '1=已删除（软删，便于留痕）',
    `create_time` datetime              DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_att_task` (`task_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='学习任务附件表';

-- ------------------------------------------------- 二、作业附件原始文件名（只加列）
-- MySQL 8 不支持 ADD COLUMN IF NOT EXISTS → 用 information_schema 判存在（幂等）
SET @col_exists = (SELECT COUNT(*)
                     FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'task_submission'
                      AND COLUMN_NAME = 'file_name');

SET @ddl = IF(@col_exists > 0,
              'SELECT 1',
              'ALTER TABLE `task_submission` ADD COLUMN `file_name` varchar(255) NULL COMMENT ''作业附件原始文件名'' AFTER `file_url`');

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
