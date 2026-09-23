-- ============================================================================
-- S4：存量实操题归库 + 阶段改名（幂等 / 可回滚）
--   背景：17 条 practice_subject 的 bank_id 全为 NULL（原按 module_id 挂），
--         新模型下实操题按「题库」开放给实习生 -> 需归入实操题库。
--   口径：按部门建/复用「<部门名>实操题库」(bank_kind=PRACTICAL × bank_type=PRACTICE)，
--         把该部门未归库的存量题迁入，并置为对实习生开放(practice_enabled=1)。
--   回滚：见 sql/_backup_practice_bank_migration_*.sql
-- 幂等：建库走 NOT EXISTS；迁题只动 bank_id IS NULL
-- ============================================================================
USE `intern_assessment`;

-- ① 为 103/104/105 各建一个「<部门名>实操题库」（已存在则跳过）
INSERT INTO `question_bank`
  (`bank_name`, `bank_type`, `bank_kind`, `practice_enabled`, `dept_id`, `description`,
   `status`, `question_count`, `create_by`, `create_time`, `update_time`, `deleted`)
SELECT CONCAT(d.dept_name, '实操题库'), 'PRACTICE', 'PRACTICAL', 1, d.dept_id,
       '存量实操题归集库；可在「模拟备考管理 → 模拟实操题库」中勾选开放范围，或直接改名/拆分。',
       'ENABLED', 0, 'migration', NOW(), NOW(), 0
FROM `sys_dept` d
WHERE d.dept_id IN (103, 104, 105)
  AND NOT EXISTS (
        SELECT 1 FROM `question_bank` b
        WHERE b.dept_id = d.dept_id AND b.bank_kind = 'PRACTICAL'
          AND b.bank_type = 'PRACTICE' AND b.deleted = 0);

-- ② 存量题归库（只动未归库的；按 dept 对应到该部门实操题库）
UPDATE `practice_subject` s
JOIN `question_bank` b
  ON b.dept_id = s.dept_id AND b.bank_kind = 'PRACTICAL' AND b.bank_type = 'PRACTICE' AND b.deleted = 0
SET s.bank_id = b.id, s.update_time = NOW()
WHERE s.bank_id IS NULL AND (s.deleted IS NULL OR s.deleted = 0);

-- ③ 回填题库题量
UPDATE `question_bank` b
SET b.question_count = (SELECT COUNT(*) FROM `practice_subject` s
                        WHERE s.bank_id = b.id AND (s.deleted IS NULL OR s.deleted = 0))
WHERE b.bank_kind = 'PRACTICAL' AND b.deleted = 0;

-- ④ 阶段改名：占位名「默认模块」->「第一阶段」
UPDATE `practice_module`
SET name = '第一阶段',
    description = '模拟理论的阶段分组：该阶段下可建多套模拟理论套卷，实习生可重复练习。',
    update_time = NOW()
WHERE name = '默认模块' AND (deleted IS NULL OR deleted = 0);

-- ⑤ 还原 S1 测试遗留：java1(25) 关闭对实习生开放
UPDATE `question_bank` SET practice_enabled = 0, update_time = NOW() WHERE id = 25 AND practice_enabled = 1;

-- ⑥ 核对：仍应 0 条未归库
SELECT (SELECT COUNT(*) FROM `practice_subject` WHERE bank_id IS NULL AND (deleted IS NULL OR deleted=0)) AS 未归库剩余,
       (SELECT COUNT(*) FROM `practice_subject` WHERE bank_id IS NOT NULL AND (deleted IS NULL OR deleted=0)) AS 已归库;
