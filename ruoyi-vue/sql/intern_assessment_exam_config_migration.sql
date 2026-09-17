-- ============================================================================
-- 理论考试配置化迁移：知识分布 + 指定人员（幂等，可重复执行）
--
-- 背景：部门管理员的「模拟理论考核配置」与「正式理论考试配置」需要
--       ① 知识分布（按题库 question.knowledge_point 配抽题数量与占比）
--       ② 指定人员（正式考核可只对指定实习生开放）
--
-- 说明：
--   · 不需要改动 exam 表 —— 题型数量/分值/时长/及格线/时间窗字段已齐备；
--   · 知识分布独立成表（不塞 JSON），便于按知识点抽题时直接 JOIN 统计；
--   · 指定人员独立成表，配合 answer_sheet.my 列表按人过滤。
-- ============================================================================

USE `intern_assessment`;

CREATE TABLE IF NOT EXISTS `exam_knowledge_rule` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `exam_id`         BIGINT       NOT NULL COMMENT '考核ID(模拟/正式理论考试)',
  `knowledge_point` VARCHAR(64)  NOT NULL COMMENT '题库知识点(question.knowledge_point)',
  `question_count`  INT          NOT NULL DEFAULT 0 COMMENT '本知识点抽题数量',
  `ratio`           DECIMAL(5,2)          DEFAULT NULL COMMENT '占比%(展示与校验用)',
  `single_ratio`    DECIMAL(5,2)          DEFAULT NULL COMMENT '单选题占比%',
  `sort_no`         INT          NOT NULL DEFAULT 0 COMMENT '排序',
  `create_time`     DATETIME              DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_kr_exam` (`exam_id`)
) ENGINE=InnoDB COMMENT='理论考试知识分布(驱动抽题)';

CREATE TABLE IF NOT EXISTS `exam_participant` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT,
  `exam_id`     BIGINT   NOT NULL COMMENT '考核ID',
  `user_id`     BIGINT   NOT NULL COMMENT '指定参与人(sys_user.user_id)',
  `create_time` DATETIME          DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ep_exam_user` (`exam_id`,`user_id`),
  KEY `idx_ep_user` (`user_id`)
) ENGINE=InnoDB COMMENT='正式考核指定人员(为空则该部门全体在培实习生可参加)';
