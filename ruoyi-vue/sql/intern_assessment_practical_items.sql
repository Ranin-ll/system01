-- ============================================================================
-- 正式实操考核：题目清单化（不再从题库抽题）
--
-- 背景：实操与理论彻底拆分为两类独立考核（一条考核 = 一类），
--       实操题目由管理员逐条填写「题干 / 题目描述 / 参考图 / 附件 / 本题满分」，
--       实习生端逐题上传作答文件，管理员逐题打分求和。
-- ============================================================================
USE `intern_assessment`;

-- 1) 实操考核题目表
CREATE TABLE IF NOT EXISTS `exam_subject_item`
(
    `id`                BIGINT       NOT NULL AUTO_INCREMENT,
    `exam_id`           BIGINT       NOT NULL COMMENT '所属实操考核ID(exam.id)',
    `seq`               INT          NOT NULL DEFAULT 1 COMMENT '题序(从1开始)',
    `title`             VARCHAR(255) NOT NULL COMMENT '题干(题名)',
    `description`       TEXT                  DEFAULT NULL COMMENT '题目描述/作业要求',
    `score`             DECIMAL(6, 2) NOT NULL DEFAULT 0 COMMENT '本题满分',
    `reference_images`  TEXT                  DEFAULT NULL COMMENT '参考图JSON [{name,url}]',
    `attachments_json`  TEXT                  DEFAULT NULL COMMENT '参考附件JSON [{name,url}]',
    `create_time`       DATETIME              DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_esi_exam` (`exam_id`, `seq`)
) ENGINE = InnoDB COMMENT ='实操考核题目(逐题填写)';

-- 2) 答卷明细挂到实操题目上（逐题作答文件 + 逐题人工打分）
SET @exist := (SELECT COUNT(*)
               FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'answer_sheet_item'
                 AND COLUMN_NAME = 'subject_item_id');
SET @sql := IF(@exist = 0,
               'ALTER TABLE `answer_sheet_item` ADD COLUMN `subject_item_id` BIGINT DEFAULT NULL COMMENT ''实操题目ID(exam_subject_item.id)'' AFTER `question_id`',
               'SELECT ''subject_item_id already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3) 存量实操考核数据迁移
--    改造前实操考核只有「一个题干 + 一个参考附件」；改为题目清单后，
--    这类考核会因为没有题目而无法发布/作答。这里把它们转成「一道题」。
INSERT INTO `exam_subject_item` (`exam_id`, `seq`, `title`, `description`, `score`,
                                 `reference_images`, `attachments_json`, `create_time`)
SELECT e.id,
       1,
       LEFT(e.subject_content, 100),
       e.subject_content,
       100,
       NULL,
       CASE
           WHEN e.subject_attachment IS NOT NULL AND e.subject_attachment <> ''
               THEN CONCAT('[{"name":"参考附件","url":"', e.subject_attachment, '"}]')
           ELSE NULL
           END,
       NOW()
FROM `exam` e
WHERE e.deleted = 0
  AND e.exam_type = 'PRACTICAL'
  AND e.subject_content IS NOT NULL
  AND e.subject_content <> ''
  AND NOT EXISTS (SELECT 1 FROM `exam_subject_item` si WHERE si.exam_id = e.id);

-- 题量字段与题目清单对齐（列表展示用）
UPDATE `exam` e
SET e.subject_count  = (SELECT COUNT(1) FROM `exam_subject_item` si WHERE si.exam_id = e.id),
    e.question_count = (SELECT COUNT(1) FROM `exam_subject_item` si WHERE si.exam_id = e.id)
WHERE e.deleted = 0
  AND e.exam_type = 'PRACTICAL';

-- 4) 校验
SHOW COLUMNS FROM `answer_sheet_item` LIKE 'subject_item_id';
SELECT COUNT(*) AS exam_subject_item_ready
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'exam_subject_item';
SELECT e.id, e.exam_name, e.subject_count, e.question_count,
       (SELECT COUNT(1) FROM `exam_subject_item` si WHERE si.exam_id = e.id) AS items
FROM `exam` e
WHERE e.deleted = 0 AND e.exam_type = 'PRACTICAL'
ORDER BY e.id;
