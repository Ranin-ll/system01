-- ============================================================================
-- 清理遗留僵尸账号（B 类，2026-09-20）
--
-- 背景：改版过程中留下的 5 个「僵尸账号」—— 三个特征同时命中：
--   ① 账号已停用（status='1'）      → 根本登录不了
--   ② 无任何角色（sys_user_role 空） → 就算能登录也看不到任何菜单
--   ③ 零业务痕迹                    → 不关联报名 / 学习 / 任务 / 答卷 / 讨论任何数据
--   命名也自证是遗留物：old_* 与 test_pre_trainee / test_formal_trainee 已被
--   规范账号（125-129）取代。扫描明细见 artifacts/遗留账号清理清单.md。
--
-- ★ 安全设计（重要）：软删语句的 WHERE 里**内嵌**了上面 ①② 两条判据，而不仅仅是按账号名删。
--   换个环境执行时，只有当同名账号「已被停用 且 无任何角色」才会被清；若某环境里同名账号
--   是正常启用的（status='0'），本脚本一行都不会动。
--   ⚠️ 因此**执行顺序不能反**：必须先做带判据的软删（②），再清理关联行（③）。
--      若先清关联行，②里的 NOT EXISTS(角色) 判据就会被架空。
--
-- 备份：清理前已导出 `sql/_backup_legacy_accounts_2026-09-20.sql`（5 行，可回滚）。
-- 删除方式：**逻辑删除**（del_flag='2'），与页面上的「删除」口径一致，可恢复。
--
-- 幂等：②带 del_flag='0' → 重复执行影响 0 行。
-- 回滚：执行 `sql/_backup_legacy_accounts_2026-09-20.sql`（每条 INSERT 前带 DELETE，可重复执行）。
-- ============================================================================

-- ① 前置检查：应恰好 5 条（已清理过则返回 0 条）
SELECT u.user_id, u.user_name, u.nick_name, u.dept_id, u.status, u.user_status,
       (SELECT COUNT(*) FROM sys_user_role ur WHERE ur.user_id = u.user_id) AS 角色数
  FROM sys_user u
 WHERE u.del_flag = '0'
   AND u.user_name IN ('test_pre_trainee','test_formal_trainee','old_super_116','old_pre_122','old_formal_123')
   AND u.status = '1'
   AND u.user_status = 'DISABLED'
   AND NOT EXISTS (SELECT 1 FROM sys_user_role ur WHERE ur.user_id = u.user_id)
 ORDER BY u.user_id;

-- ② 逻辑删除账号本体（判据内嵌，顺序必须先于 ③）
UPDATE sys_user u
   SET u.del_flag = '2',
       u.update_by = 'migration',
       u.update_time = NOW()
 WHERE u.del_flag = '0'
   AND u.user_name IN ('test_pre_trainee','test_formal_trainee','old_super_116','old_pre_122','old_formal_123')
   AND u.status = '1'
   AND u.user_status = 'DISABLED'
   AND NOT EXISTS (SELECT 1 FROM sys_user_role ur WHERE ur.user_id = u.user_id);

-- ③ 清理关联行：只针对「刚刚被本脚本软删」的那几个账号（用 del_flag='2' + 账号名双限定，
--    避免误伤其他已软删的历史账号）。这 5 个本就没有角色/岗位行，属防御性执行。
DELETE ur FROM sys_user_role ur
  JOIN sys_user u ON u.user_id = ur.user_id
 WHERE u.del_flag = '2'
   AND u.user_name IN ('test_pre_trainee','test_formal_trainee','old_super_116','old_pre_122','old_formal_123');

DELETE up FROM sys_user_post up
  JOIN sys_user u ON u.user_id = up.user_id
 WHERE u.del_flag = '2'
   AND u.user_name IN ('test_pre_trainee','test_formal_trainee','old_super_116','old_pre_122','old_formal_123');

-- ④ 后置校验：在册账号数（清理前 29） / 无角色告警（清理前 5，应为 0）
SELECT (SELECT COUNT(*) FROM sys_user WHERE del_flag = '0') AS 在册账号数,
       (SELECT COUNT(*) FROM sys_user WHERE del_flag = '2') AS 已软删账号数,
       (SELECT COUNT(*) FROM sys_user u WHERE u.del_flag = '0'
          AND NOT EXISTS (SELECT 1 FROM sys_user_role ur
                            JOIN sys_role r ON r.role_id = ur.role_id AND r.del_flag = '0'
                           WHERE ur.user_id = u.user_id)) AS 无角色账号数_应为0,
       (SELECT COUNT(*) FROM register_application WHERE status = 'WAIT_AUDIT') AS 待审注册_本次未动;
