-- ============================================================================
-- 下线「章节检测（章节测试 / QUIZ）」迁移（幂等，可重复执行）
--
-- 背景：学习单项不再支持 QUIZ 类型，只保留 DOC（文档）与 VIDEO（视频）。
--       历史数据里存在 10 条 item_type='QUIZ' 的种子单项（含 1 条已被学习记录引用），
--       因此采用**软删**（deleted=1）而非物理删除：
--         · 课程目录 / 统计口径（item_count、avg_completion_rate、countUnboundAssets）均已过滤 deleted=0，
--           软删后各类进度不再把测试项算入分母；
--         · 保留原始行，迁就 study_record 的历史引用，便于追溯。
--
-- 说明：字段 quiz_json 保留（不再写入新值），如需彻底清理列可另行安排。
-- ============================================================================

USE `intern_assessment`;

UPDATE `study_item`
SET `deleted` = 1
WHERE `deleted` = 0
  AND `item_type` = 'QUIZ';
