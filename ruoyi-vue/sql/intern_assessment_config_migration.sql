-- ============================================================
-- 融谷 · 实习生学习考核系统
-- 新增「考核与运营规则默认值」单行配置表（超管「规则与配置」页的数据落点）
--
-- 背景（2026-09-17 核实）：
--   `theory_weight / practice_weight / theory_pass_line / practice_pass_line /
--    course_done_threshold / protocol_required / retake_count` 这 7 个字段
--   此前**只存在于 exam_rule_snapshot** —— 那是「每次发布冻结的快照」表（per-exam），
--    全库零代码引用；**并没有存全局默认值的表**。本脚本补上这张表。
--
-- 生效说明：
--   本表存「默认值」。已发布批次仍按各自快照执行（快照写入链待考核域打通）。
--   因此改这里的值不会篡改历史批次，符合审计友好原则。
--
-- 幂等：CREATE TABLE IF NOT EXISTS + INSERT ... WHERE NOT EXISTS，可重复执行。
-- ============================================================

CREATE TABLE IF NOT EXISTS `assessment_config` (
  `id`                    BIGINT       NOT NULL                COMMENT '主键，固定为 1（单行配置）',
  `course_done_threshold` DECIMAL(5,2) NOT NULL DEFAULT 70.00  COMMENT '学习门槛：必修课程完成率(%)',
  `theory_weight`         DECIMAL(5,2) NOT NULL DEFAULT 40.00  COMMENT '理论权重(%)',
  `practice_weight`       DECIMAL(5,2) NOT NULL DEFAULT 60.00  COMMENT '实操权重(%)',
  `theory_pass_line`      DECIMAL(5,2) NOT NULL DEFAULT 60.00  COMMENT '理论通过线(分)',
  `practice_pass_line`    DECIMAL(5,2) NOT NULL DEFAULT 60.00  COMMENT '实操通过线(分)',
  `protocol_required`     TINYINT      NOT NULL DEFAULT 1      COMMENT '是否强制签署保密协议(1是 0否)',
  `retake_count`          INT          NOT NULL DEFAULT 2      COMMENT '补考次数',
  `cert_no_rule`          VARCHAR(128) NOT NULL DEFAULT '{岗位缩写}-{年}-{月日}-{序号}'
                                                               COMMENT '证书编号规则（占位符模板）',
  `cert_template_url`     VARCHAR(255)          DEFAULT NULL   COMMENT '证书模板文件地址（相对路径）',
  `alert_bank_gap_factor` INT          NOT NULL DEFAULT 2      COMMENT '预警：题库题量须 ≥ 单场需求 × N',
  `alert_exam_draft_days` INT          NOT NULL DEFAULT 30     COMMENT '预警：考核草稿停留天数上限',
  `alert_register_pending` INT         NOT NULL DEFAULT 3      COMMENT '预警：报名待审核条数上限',
  `alert_dept_done_floor` DECIMAL(5,2) NOT NULL DEFAULT 70.00  COMMENT '预警：部门完成率下限(%)',
  `alert_overdue_hours`   INT          NOT NULL DEFAULT 24     COMMENT '预警：考核逾期多少小时',
  `update_by`             VARCHAR(64)           DEFAULT NULL   COMMENT '最后修改人',
  `update_time`           DATETIME              DEFAULT NULL   COMMENT '最后修改时间',
  `create_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '考核与运营规则默认值（单行配置）';

INSERT INTO `assessment_config` (`id`)
SELECT 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `assessment_config` WHERE `id` = 1);
