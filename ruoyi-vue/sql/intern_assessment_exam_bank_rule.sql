-- ============================================================================
-- 多题库组卷配置迁移（幂等，可重复执行）
--
-- 背景：原组卷只支持「一个题库 + 按知识点分配题量」（exam_knowledge_rule）。
--       现改为「题库即知识模块」：一道题在录入时归属唯一题库，题库名即知识模块名；
--       组卷时勾选多个题库，并为每个题库分别指定 单选/多选/判断 的抽题数量。
--
-- 说明：
--   · 新增 exam_bank_rule（题库 × 题型 题量），是抽题的唯一依据；
--   · exam.bank_id 保留为「兼容/兜底」字段：未配置 exam_bank_rule 的老考核仍走单库抽题；
--   · exam.single_count / multi_count / judge_count 保留，语义变为
--     「各题库抽题量的汇总」（由后端在保存时自动回写），分值计算与页面展示继续可用；
--   · exam_knowledge_rule 保留不动（老数据不失效），新配置不再写它。
-- ============================================================================

USE `intern_assessment`;

CREATE TABLE IF NOT EXISTS `exam_bank_rule` (
  `id`            BIGINT   NOT NULL AUTO_INCREMENT,
  `exam_id`       BIGINT   NOT NULL COMMENT '考核ID(模拟/正式理论考试)',
  `bank_id`       BIGINT   NOT NULL COMMENT '题库ID(question_bank.id)，业务语义=知识模块',
  `single_count`  INT      NOT NULL DEFAULT 0 COMMENT '本库抽单选题数量',
  `multi_count`   INT      NOT NULL DEFAULT 0 COMMENT '本库抽多选题数量',
  `judge_count`   INT      NOT NULL DEFAULT 0 COMMENT '本库抽判断题数量',
  `sort_no`       INT      NOT NULL DEFAULT 0 COMMENT '排序(卷面按库顺序组卷)',
  `create_time`   DATETIME          DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ebr_exam` (`exam_id`),
  KEY `idx_ebr_bank` (`bank_id`)
) ENGINE=InnoDB COMMENT='考核多题库组卷配置(每个题库的抽题量)';
