-- ============================================================================
-- 模拟考核记录逐题明细 - 数据库改造
--
-- 背景：原 practice_record 仅存汇总（总题数/正确数/得分），实习生无法回看
--       具体题目、自己的作答以及正确答案。现新增明细表，交卷时按题落库。
--
-- 设计：题干/选项/解析采用「快照」存储 —— 模拟题库的题目后续可能被修改或
--       删除，快照能保证历史记录长期可回看、内容不漂移。
-- ============================================================================

USE `intern_assessment`;

-- 0. 兼容：若 practice_record 尚未创建则一并补齐（历史上是手工建的）
CREATE TABLE IF NOT EXISTS `practice_record` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id`       BIGINT       NOT NULL COMMENT '实习生ID',
    `bank_id`       BIGINT       DEFAULT NULL COMMENT '模拟题库ID',
    `dept_id`       BIGINT       DEFAULT NULL COMMENT '部门ID',
    `total_count`   INT          NOT NULL DEFAULT 10 COMMENT '总题数',
    `correct_count` INT          NOT NULL DEFAULT 0 COMMENT '正确题数',
    `score`         INT          NOT NULL DEFAULT 0 COMMENT '得分(正确题数)',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '作答时间',
    PRIMARY KEY (`id`),
    KEY `idx_practice_user` (`user_id`),
    KEY `idx_practice_bank` (`bank_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='模拟考核个人成绩记录';

-- 1. 逐题明细表
CREATE TABLE IF NOT EXISTS `practice_record_item` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '明细ID',
    `record_id`      BIGINT       NOT NULL COMMENT '模拟记录ID（practice_record.id）',
    `question_id`    BIGINT       DEFAULT NULL COMMENT '题目ID',
    `sort_no`        INT          NOT NULL DEFAULT 0 COMMENT '题序（从1开始）',
    `qtype`          VARCHAR(24)  DEFAULT NULL COMMENT '题型:SINGLE单选/MULTI多选/JUDGE判断',
    `stem`           TEXT         COMMENT '题干快照',
    `options_json`   TEXT         COMMENT '选项快照JSON:[{"key":"A","content":"..."}]',
    `user_answer`    VARCHAR(255) DEFAULT NULL COMMENT '学员作答',
    `correct_answer` VARCHAR(255) DEFAULT NULL COMMENT '正确答案',
    `is_correct`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否正确:1是 0否',
    `analysis`       TEXT         COMMENT '答案解析快照',
    `create_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_pri_record` (`record_id`),
    KEY `idx_pri_question` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='模拟考核记录逐题明细';

-- 验证
SHOW COLUMNS FROM `practice_record_item`;
