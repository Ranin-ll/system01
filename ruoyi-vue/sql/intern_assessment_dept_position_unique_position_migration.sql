-- ============================================================================
-- 「一个岗位只能属于一个部门」（2026-09-20 用户口径）
--
-- 背景：dept_position 是多对多关联表，已有 UNIQUE KEY uk_dept_pos(dept_id, position_id)
--       —— 它只保证「同一部门的同一岗位不重复」，**不保证**「一个岗位只属一个部门」。
--
-- 口径：部门可以有多个岗位；但一个岗位只能属于一个部门。
--       原因：注册流程是「选岗位 → 自动匹配部门」，后端用
--       `SELECT dept_id FROM dept_position WHERE position_id = ? LIMIT 1`
--       反查部门。若一个岗位属多个部门，这个反查会随机落到某个部门 → 注册归属不确定。
--
-- 本脚本给 position_id 单独加唯一索引，把这条口径固化到数据库层（应用层也有一道校验）。
-- 幂等：先查 information_schema.statistics 判存在，存在则跳过。
-- ============================================================================

SET @db = DATABASE();

-- ① 前置检查：现有数据是否已有「一个岗位绑多个部门」的违规行（有的话必须先处理，否则建索引会失败）
SELECT position_id, COUNT(*) AS dept_count, GROUP_CONCAT(dept_id) AS dept_ids
FROM dept_position
GROUP BY position_id
HAVING COUNT(*) > 1;

-- ② 幂等建唯一索引
SET @has_idx = (SELECT COUNT(*) FROM information_schema.statistics
                WHERE table_schema = @db AND table_name = 'dept_position' AND index_name = 'uk_position');

SET @sql = IF(@has_idx > 0,
              'SELECT ''uk_position 已存在，跳过'' AS msg',
              'ALTER TABLE dept_position ADD UNIQUE KEY uk_position (position_id)');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ③ 自检：dept_position 现在应有两个唯一索引
SELECT index_name,
       GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns_in_index
FROM information_schema.statistics
WHERE table_schema = @db AND table_name = 'dept_position'
GROUP BY index_name
ORDER BY index_name;

-- ④ 自检：每个岗位应只对应一个部门
SELECT COUNT(*) AS positions_bound_to_multiple_depts
FROM (SELECT position_id FROM dept_position GROUP BY position_id HAVING COUNT(*) > 1) t;
