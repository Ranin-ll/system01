-- =====================================================================
-- 遗留测试账号清理（幂等，可重复执行）
-- ---------------------------------------------------------------------
-- 背景：2026-09-09 建的 3 个测试号只有 user_status（培养状态），
--       **一个角色都没有**，却被所有「只看 user_status」的 SQL 当成实习生：
--       混进部门管理员/超管的实习生名单、「指定人员」下拉，还会被分配任务。
--       其中一个昵称叫「测试超级管理员」，容易被误判成「超管跑进了实习生队列」。
--
-- 判定口径（本项目统一）：实习生 = user_status ∈ (PRE_TRAINEE, FORMAL_TRAINEE, PENDING_PROMOTE)
--                          **且** 持有 PRE_TRAINEE / FORMAL_TRAINEE 角色。
--       代码层已在 LearningTaskMapper 等查询里补了角色存在性判断；
--       本脚本把数据本身也收拾干净 —— 停用（而非删除），账号与历史数据都保留。
-- 注：真超管 test_super_admin(id=100) 的 user_status 是 NON_INTERN，从来不在名单里。
-- =====================================================================

-- 停用：改两处，缺一不可
--   ① sys_user.user_status = 'DISABLED' —— 业务状态机兜底态，各业务查询/名单不再算它
--   ② sys_user.status      = '1'        —— 若依的「账号停用」，登录被拒 + 不再算作「有效账号」
-- 为什么必须同时改 ②：通知的送达谓词（NoticeMapper 的 visibleTo / audienceOf）用的是
-- `u.del_flag='0' AND u.status='0'`（"账号是否有效"），不看业务状态。
-- 只改 ① 时，这些废号仍会出现在「按部门 / 全体」通知的回执名单里（实测踩到），
-- 而且它们还能正常登录，只是进去什么都看不到。
UPDATE sys_user
   SET user_status = 'DISABLED',
       status       = '1',
       update_time  = NOW(),
       remark       = CONCAT(IFNULL(remark, ''), ' [2026-09-18 停用：无任何角色的遗留测试号，曾被误判为在培实习生]')
 WHERE user_id IN (116, 122, 123)
   AND (user_status <> 'DISABLED' OR status <> '1');

-- 自检：确认这 3 个号已停用、且系统里不再有「在培状态但无实习生角色」的账号
-- SELECT u.user_id, u.user_name, u.nick_name, u.user_status,
--        IFNULL(GROUP_CONCAT(r.role_key), '（无角色）') AS roles
--   FROM sys_user u
--   LEFT JOIN sys_user_role ur ON ur.user_id = u.user_id
--   LEFT JOIN sys_role r ON r.role_id = ur.role_id
--  WHERE u.del_flag = '0'
--    AND u.user_status IN ('PRE_TRAINEE', 'FORMAL_TRAINEE', 'PENDING_PROMOTE')
--  GROUP BY u.user_id
-- HAVING roles = '（无角色）';
-- → 期望：0 行
