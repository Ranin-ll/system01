-- ============================================================================
-- 转正要求规则表 promotion_rule
--
-- 背景（2026-09-24）：
--   转正链路此前只有 promotion_application / certificate 两张表定义，Java 层全缺。
--   本次补齐「部门管理员设定的转正要求」落库，供三端共同读取：
--     - 实习生端「考核成绩与转正」页：核对是否满足门槛，决定能否发起申请；
--     - 部门管理员端「实习转正审核」页：展示并调整门槛、判定候选人资格；
--     - 后端转正申请提交时的服务端资格校验（不信任前端）。
--
-- 规则口径（已与需求对齐）：
--   1) 转正资格**不按综合分数**判断，只按「通过的正式考核次数」判定；
--   2) 管理员会发布多次正式考核并设定「需要通过的次数 N」，
--      实习生通过考核数量 >= N 即开放申请；
--   3) 学习完成率门槛 study_rate_min 仍作为并行门槛保留（默认 0 = 不卡学习率）。
--
-- dept_id = NULL 表示「全局默认规则」；非 NULL 表示「部门级覆盖」。
-- 读取优先级：部门规则 > 全局规则 > 内置兜底 {studyRateMin:0, examPassTimes:1}。
--
-- 幂等：CREATE TABLE IF NOT EXISTS + 全局种子用 INSERT ... SELECT WHERE NOT EXISTS。
-- ============================================================================
USE `intern_assessment`;

CREATE TABLE IF NOT EXISTS `promotion_rule` (
    `id`              BIGINT      NOT NULL AUTO_INCREMENT,
    `dept_id`         BIGINT      DEFAULT NULL COMMENT '所属部门；NULL=全局默认规则',
    `study_rate_min`  INT         NOT NULL DEFAULT 0 COMMENT '学习完成率最低门槛(%)，0=不卡学习率',
    `exam_pass_times` INT         NOT NULL DEFAULT 1 COMMENT '正式考核至少通过次数',
    `enabled`         TINYINT     NOT NULL DEFAULT 1 COMMENT '是否启用(1启用/0停用)',
    `create_by`       VARCHAR(32) DEFAULT NULL COMMENT '创建人',
    `create_time`     DATETIME    DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_rule_dept` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='转正要求规则表';

-- 全局默认规则种子（仅在没有任何全局规则时插入）
INSERT INTO `promotion_rule` (`dept_id`, `study_rate_min`, `exam_pass_times`, `enabled`, `create_by`)
SELECT NULL, 0, 1, 1, 'system'
  FROM DUAL
 WHERE NOT EXISTS (
    SELECT 1 FROM `promotion_rule` WHERE `dept_id` IS NULL
 );

-- 校验
SELECT `id`, IF(`dept_id` IS NULL, '(全局)', `dept_id`) AS dept,
       `study_rate_min`, `exam_pass_times`, `enabled`
  FROM `promotion_rule`;
