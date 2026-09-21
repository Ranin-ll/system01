-- ============================================================================
-- 停用框架内置的「超级管理员」角色（role_id=1 / role_key='admin'）（2026-09-20）
--
-- 背景：`sys_role` 里有**两条**都叫「超级管理员」：
--   · role_id=1   role_key='admin'        ← RuoYi 初始化 SQL 自带，成员只有内置账号 admin(user_id=1)
--   · role_id=100 role_key='SUPER_ADMIN'  ← 本项目自建（迁移 SQL），成员 test_super_admin
--   超管「角色权限」页直接列 sys_role 全表 → 两条同名，看起来像重复数据。
--
-- 为什么保留（不删）：内置账号 admin(user_id=1) 是框架账号，RuoYi 的
--   SysRoleServiceImpl.checkRoleAllowed 明确保护 role_id=1（不允许改/删），
--   且 SecurityUtils.isAdmin(userId==1) 的身份旁路仍被大量代码依赖。
--
-- ★ 重要（务必知道）：`sys_role.status` **不参与角色解析**。
--   SysPermissionService:44 → SysRoleMapper.selectRolePermissionByUserId 的 WHERE
--   只有 `r.del_flag='0'`，**没有 status 条件** → 停用后登录仍会拿到该角色。
--   因此「停用」是**治理标记**（在原生「角色管理」页会显示为停用），
--   并不会收回内置账号在 Constants.SUPER_ADMIN="admin" 通配语义下的权限。
--   要真正收回权限，需另行「解绑 sys_user_role」或「软删 del_flag=2」—— 属单独决策。
--
-- 幂等：WHERE 带 status='0'，重复执行影响 0 行。
-- 回滚：UPDATE sys_role SET status='0' WHERE role_id=1;
-- ============================================================================

-- ① 前置检查：确认目标角色存在且当前是「正常」
SELECT role_id,
       role_key,
       role_name,
       status,
       CASE status WHEN '0' THEN '正常（将被停用）' WHEN '1' THEN '已停用（无需处理）' END AS 当前状态
  FROM sys_role
 WHERE role_id = 1;

-- ② 停用（仅当仍是「正常」时生效；顺带把停用原因写进 remark 便于后人追溯）
UPDATE sys_role
   SET status = '1',
       remark = '框架内置角色（通配 admin），已停用、勿分配；项目超管请用 SUPER_ADMIN',
       update_by = 'migration',
       update_time = NOW()
 WHERE role_id = 1
   AND role_key = 'admin'
   AND status = '0';

-- ③ 后置校验：两条「超级管理员」的最终状态一目了然
SELECT role_id,
       role_key,
       role_name,
       status,
       CASE status WHEN '0' THEN '启用' WHEN '1' THEN '已停用' END AS 状态说明,
       (SELECT COUNT(*) FROM sys_user_role ur WHERE ur.role_id = r.role_id) AS 成员数
  FROM sys_role r
 WHERE role_name = '超级管理员'
 ORDER BY role_id;
