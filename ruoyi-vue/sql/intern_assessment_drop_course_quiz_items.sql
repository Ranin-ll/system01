-- ============================================================================
-- 下线「章节测试 / 章节自测」（item_type = 'QUIZ'）
--
-- 背景（2026-09-23）：
--   课程只保留「文档 / 视频」两类学习单项，章节测试以后不再使用。
--   · 后端 LearningServiceImpl 早已不再处理 QUIZ（代码里只剩注释）
--   · 课程编辑器的「资料类型」也只有 文档 / 视频，新建不出 QUIZ
--   · 但库里仍留着 10 条种子数据（各课程第二章一条、标题「章节自测」），
--     它们会占 item_count、拉低完成率、在课程目录与工作台矩阵里多出一格
--
-- 做法：
--   ① study_item：QUIZ 项**软删**（deleted = 1）。
--      所有学习/管理查询都带 `si.deleted = 0`，软删即彻底消失（完成率分母随之变小，这是预期）。
--   ② study_record：**零进度**的 QUIZ 记录物理删除 —— 该表没有 deleted 列，
--      而超管「逐学习项」查询（SuperAnalysisMapper.selectInternStudy）只按 user_id 过滤、
--      不过滤 item.deleted，留着会在实习生详情里露出一行没有对应资料的记录。
--      带进度的记录**保留**（不误伤真实学习历史），需要人工确认后再处理。
--
-- 幂等：软删带 `deleted = 0` 条件；删除带 `progress = 0 AND study_duration = 0` 条件，可重复执行。
-- 回滚：单项 `UPDATE study_item SET deleted = 0 WHERE item_type = 'QUIZ';`
--       学习记录需从执行前的快照恢复。
-- ============================================================================

-- ① 执行前快照（建议留档）
SELECT GROUP_CONCAT(id ORDER BY id) AS quiz_items_before
  FROM study_item WHERE item_type = 'QUIZ' AND deleted = 0;
SELECT GROUP_CONCAT(id ORDER BY id) AS quiz_records_before
  FROM study_record WHERE item_type = 'QUIZ';

-- ② 软删 QUIZ 学习单项
UPDATE study_item
   SET deleted = 1, update_time = NOW()
 WHERE item_type = 'QUIZ' AND deleted = 0;

-- ③ 删除零进度的 QUIZ 学习记录
DELETE FROM study_record
 WHERE item_type = 'QUIZ'
   AND IFNULL(progress, 0) = 0
   AND IFNULL(study_duration, 0) = 0;

-- ④ 校验：两项都应为 0
SELECT (SELECT COUNT(*) FROM study_item   WHERE item_type = 'QUIZ' AND deleted = 0) AS quiz_items_left,
       (SELECT COUNT(*) FROM study_record WHERE item_type = 'QUIZ')                AS quiz_records_left;
