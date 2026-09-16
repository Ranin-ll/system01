-- ============================================================================
-- 实习生学习考核系统 - 题库模块建表脚本
-- 依赖：需先执行 intern_assessment_business.sql（question 表已创建）
-- 说明：新增 question_bank 表（按部门归属），改造现有 question 表（加 bank_id）
-- ============================================================================

USE `intern_assessment`;

-- ============================================================================
-- 步骤 1：新建题库表（按部门归属，部门管理员管理本部门题库）
-- ============================================================================

DROP TABLE IF EXISTS `question_bank`;
CREATE TABLE `question_bank` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '题库ID',
    `bank_name`     VARCHAR(128) NOT NULL COMMENT '题库名称',
    `dept_id`       BIGINT       NOT NULL COMMENT '所属部门ID（关联sys_dept）',
    `description`   VARCHAR(500)          DEFAULT NULL COMMENT '题库说明',
    `status`        VARCHAR(24)  NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED启用 / DISABLED停用',
    `question_count` INT         NOT NULL DEFAULT 0 COMMENT '题目数量（冗余，方便列表展示）',
    `create_by`     VARCHAR(32)           DEFAULT NULL,
    `create_time`   DATETIME              DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_bank_dept` (`dept_id`)
) ENGINE=InnoDB COMMENT='题库表（按部门归属，部门管理员管理本部门题库）';

-- ============================================================================
-- 步骤 2：改造现有 question 表，使其归属题库
-- ============================================================================

-- 2.1 添加所属题库字段
ALTER TABLE `question`
    ADD COLUMN `bank_id` BIGINT DEFAULT NULL COMMENT '所属题库ID（关联question_bank）' AFTER `id`,
    ADD INDEX `idx_question_bank` (`bank_id`);

-- 2.2 调整 position_id 为可空（题目通过 bank_id → dept_id 控制部门范围，position_id 仅作为可选的岗位适用标记）
ALTER TABLE `question`
    MODIFY `position_id` BIGINT DEFAULT NULL COMMENT '适用岗位（可选）';

-- 2.3 重建题目编号唯一索引：同一题库内编号唯一（原为全局唯一）
ALTER TABLE `question`
    DROP INDEX `uk_question_no`,
    ADD UNIQUE KEY `uk_bank_question_no` (`bank_id`, `question_no`);

-- ============================================================================
-- 验证
-- ============================================================================
SELECT 'question_bank 表已创建' AS `步骤1`,
       COUNT(*) AS `题库数`
FROM `question_bank`;

SHOW COLUMNS FROM `question` LIKE 'bank_id';