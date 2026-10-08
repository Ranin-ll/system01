-- ============================================================================
-- 实习转正链路 —— 权限菜单 + 结构补齐
--
-- 背景（2026-09-29）：
--   转正链路此前只有「表 + 3 个 domain + 前端演示页（localStorage）」，
--   Mapper / Service / Controller 全缺，前端三处（超管规则页 / 部门审核页 /
--   实习生成绩页）都写着「接口尚未实现」。本次补齐完整闭环：
--
--     ① 超管或部门管理员设置转正要求  → promotion_rule
--     ② 实习生核对资格并提交转正申请  → promotion_application
--     ③ 部门管理员审批（部门终审即生效）
--     ④ 通过后 sys_user.user_status → FORMAL_TRAINEE + 角色换绑 + 自动发证
--
-- 状态机（沿用 2026-09-24 决策，跳过 SUPER_PENDING）：
--   DEPT_PENDING → PASSED / REJECTED
--   super_approve_by / super_approve_time 两列保留不写，留给超管代操作留痕。
--
-- 幂等：菜单用 INSERT ... SELECT WHERE NOT EXISTS；授权用 INSERT IGNORE。
-- ============================================================================
USE `intern_assessment`;

-- ---------------------------------------------------------------------------
-- ① 转正权限菜单（F 型按钮权限，挂在「部门运营」2019 下）
--    部门端 / 超管端侧栏由前端 dynamicRoutes 声明，不依赖 C 型菜单；
--    这里只补 perms，供后端 @ss.hasPermi 校验。
-- ---------------------------------------------------------------------------
INSERT INTO `sys_menu`
    (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`,
     `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT 2090, '转正要求设置', 2019, 90, '#', NULL, 1, 0, 'F', '0', '0', 'business:promotion:rule', '#',
       'admin', NOW(), '设置全局/本部门转正门槛（学习完成率、正式考核通过次数）'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2090);

INSERT INTO `sys_menu`
    (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`,
     `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT 2091, '转正申请列表', 2019, 91, '#', NULL, 1, 0, 'F', '0', '0', 'business:promotion:list', '#',
       'admin', NOW(), '查看本部门转正申请与资格核对清单'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2091);

INSERT INTO `sys_menu`
    (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`,
     `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT 2092, '转正申请查询', 2019, 92, '#', NULL, 1, 0, 'F', '0', '0', 'business:promotion:query', '#',
       'admin', NOW(), '查看转正申请详情与审核历史'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2092);

INSERT INTO `sys_menu`
    (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`,
     `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT 2093, '转正审批', 2019, 93, '#', NULL, 1, 0, 'F', '0', '0', 'business:promotion:audit', '#',
       'admin', NOW(), '部门管理员审批转正申请（通过即生效并发证）'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2093);

INSERT INTO `sys_menu`
    (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`,
     `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT 2094, '撤回转正', 2019, 94, '#', NULL, 1, 0, 'F', '0', '0', 'business:promotion:revoke', '#',
       'admin', NOW(), '超管纠错：退回 PRE_TRAINEE 并作废证书'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2094);

-- ---------------------------------------------------------------------------
-- ② 授权：admin(1) / SUPER_ADMIN(100) / DEPT_ADMIN(101)
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
  FROM `sys_role` r
  CROSS JOIN `sys_menu` m
 WHERE r.`role_id` IN (1, 100, 101)
   AND m.`menu_id` BETWEEN 2090 AND 2094;

-- ---------------------------------------------------------------------------
-- ③ 结构补齐（幂等）
--    · promotion_rule.dept_id 需要唯一（NULL 允许多行但业务只保留一行，用代码保证）
--    · promotion_application 需要「同一人只能有一条未结束申请」的查重索引
--    · certificate 需要「一人一证」的查重索引
-- ---------------------------------------------------------------------------
SET @has_idx := (SELECT COUNT(1) FROM `information_schema`.`STATISTICS`
                  WHERE `TABLE_SCHEMA` = 'intern_assessment'
                    AND `TABLE_NAME` = 'promotion_rule' AND `INDEX_NAME` = 'uk_rule_dept');
SET @sql := IF(@has_idx = 0,
    'ALTER TABLE `promotion_rule` ADD UNIQUE KEY `uk_rule_dept` (`dept_id`)',
    'SELECT ''promotion_rule.uk_rule_dept 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx := (SELECT COUNT(1) FROM `information_schema`.`STATISTICS`
                  WHERE `TABLE_SCHEMA` = 'intern_assessment'
                    AND `TABLE_NAME` = 'promotion_application' AND `INDEX_NAME` = 'idx_promo_status');
SET @sql := IF(@has_idx = 0,
    'ALTER TABLE `promotion_application` ADD KEY `idx_promo_status` (`status`, `user_id`)',
    'SELECT ''promotion_application.idx_promo_status 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx := (SELECT COUNT(1) FROM `information_schema`.`STATISTICS`
                  WHERE `TABLE_SCHEMA` = 'intern_assessment'
                    AND `TABLE_NAME` = 'certificate' AND `INDEX_NAME` = 'idx_cert_promo');
SET @sql := IF(@has_idx = 0,
    'ALTER TABLE `certificate` ADD KEY `idx_cert_promo` (`promotion_id`)',
    'SELECT ''certificate.idx_cert_promo 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- ④ 校验
-- ---------------------------------------------------------------------------
SELECT '=== 转正权限菜单 ===' AS section;
SELECT m.`menu_id`, m.`menu_name`, m.`perms`,
       GROUP_CONCAT(r.`role_key` ORDER BY r.`role_id`) AS roles
  FROM `sys_menu` m
  LEFT JOIN `sys_role_menu` rm ON rm.`menu_id` = m.`menu_id`
  LEFT JOIN `sys_role` r ON r.`role_id` = rm.`role_id`
 WHERE m.`menu_id` BETWEEN 2090 AND 2094
 GROUP BY m.`menu_id`, m.`menu_name`, m.`perms`;

SELECT '=== 转正规则 ===' AS section;
SELECT `id`, IF(`dept_id` IS NULL, '(全局)', `dept_id`) AS dept,
       `study_rate_min`, `exam_pass_times`, `enabled`
  FROM `promotion_rule`;
