-- ============================================================================
-- 收敛为「单一超级管理员角色」+「超管归属公司根部门」（2026-09-20）
--
-- 背景（用户口径）：
--   ① 系统里只应有一个「超级管理员」角色，超管就是超管；
--   ② 超管属于公司根部门（融谷），**不属于任何子部门**；
--   ③ 系统必须始终有一个超管账号能正常登录使用，且**无法删除**。
--
-- 为什么要动：`sys_role` 里存在两条都叫「超级管理员」的角色 ——
--   · role_id=1   role_key='admin'       框架初始化自带，成员 user_id=1（内置 admin 账号）
--   · role_id=100 role_key='SUPER_ADMIN' 本项目自建，业务与前端认的就是它
--   两条并存导致角色页出现「两个超级管理员」。上一步先把 role_id=1 置为停用（治理标记），
--   但 `sys_role.status` 并不参与角色解析（`selectRolePermissionByUserId` 只筛 del_flag），
--   所以停用不解决根本问题 —— 本次直接**软删**（`del_flag='2'`），并把它唯一的成员
--   （user_id=1）改挂到 SUPER_ADMIN，保证内置账号权限不降级。
--
-- 安全说明：
--   · user_id=1 是 RuoYi 框架保护账号（`SysUserServiceImpl.checkUserAllowed` 拦截改/删/重置密码），
--     `SecurityUtils.isAdmin(userId==1)` 的身份旁路也仍被大量代码依赖 → 它就是「无法删除的那个超管账号」。
--   · 它当前 `user_status='WAIT_AUDIT'`（列默认值），语义上不对（它不是实习生），本次纠正为 `NON_INTERN`。
--   · 协议门禁只作用于实习生（`AgreementGate.vue` 的 `isIntern && protocolStatus!==1`），超管不受影响。
--
-- 幂等：全部语句带存在性判断 / `NOT (a <=> b)` 空值安全比较，重复执行影响 0 行。
-- 回滚（手工）：
--   UPDATE sys_role SET del_flag='0' WHERE role_id=1;
--   INSERT IGNORE INTO sys_user_role(user_id, role_id) VALUES (1, 1);
--   DELETE FROM sys_user_role WHERE user_id=1 AND role_id=100;
--   （部门归属与 user_status 按需自行改回）
-- ============================================================================

SET @root_dept  = (SELECT dept_id FROM sys_dept WHERE parent_id = 0 AND del_flag = '0' ORDER BY dept_id LIMIT 1);
SET @super_role = (SELECT role_id FROM sys_role WHERE role_key = 'SUPER_ADMIN' AND del_flag = '0' LIMIT 1);
SET @admin_role = (SELECT role_id FROM sys_role WHERE role_key = 'admin' LIMIT 1);

-- ① 前置检查：确认根部门与两个角色都定位到了
SELECT @root_dept AS 根部门ID, @super_role AS SUPER_ADMIN角色ID, @admin_role AS 框架admin角色ID,
       (SELECT dept_name FROM sys_dept WHERE dept_id = @root_dept) AS 根部门名;

-- ② 内置账号 user_id=1 改挂 SUPER_ADMIN（先建关联，再解绑旧角色，顺序不能反）
--    ⚠️ 必须放在「部门归位」之前 —— 否则 user_id=1 还没成为超管，就不会被下面的语句覆盖到。
INSERT INTO sys_user_role (user_id, role_id)
SELECT 1, @super_role
  FROM DUAL
 WHERE @super_role IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys_user_role WHERE user_id = 1 AND role_id = @super_role);

DELETE FROM sys_user_role WHERE user_id = 1 AND role_id = @admin_role;

-- ③ 把所有 SUPER_ADMIN 持有者的部门归到根部门（含 dept_id 为 NULL 的）
--    `<=>` 为 MySQL 空值安全等值，`NOT (a <=> b)` 才能同时命中 NULL 与不同的值。
UPDATE sys_user u
   SET u.dept_id = @root_dept,
       u.update_by = 'migration',
       u.update_time = NOW()
 WHERE u.del_flag = '0'
   AND @root_dept IS NOT NULL
   AND NOT (u.dept_id <=> @root_dept)
   AND EXISTS (SELECT 1 FROM sys_user_role ur
                WHERE ur.user_id = u.user_id AND ur.role_id = @super_role);

-- ④ 软删框架自带的 admin 角色 —— 从此「超级管理员」只剩一条
UPDATE sys_role
   SET del_flag = '2',
       status = '1',
       remark = '已于 2026-09-20 收敛：系统只保留一个超级管理员角色（SUPER_ADMIN）。本角色原为框架内置，现软删保留痕迹，请勿再分配。',
       update_by = 'migration',
       update_time = NOW()
 WHERE role_id = @admin_role
   AND role_key = 'admin'
   AND del_flag = '0';

-- ⑤ 内置超管账号不是实习生：把列默认值 WAIT_AUDIT 纠正为非实习生
UPDATE sys_user
   SET user_status = 'NON_INTERN',
       update_by = 'migration',
       update_time = NOW()
 WHERE user_id = 1
   AND del_flag = '0'
   AND user_status = 'WAIT_AUDIT';

-- ⑥ 后置校验 1：名为「超级管理员」的角色只剩 1 条活跃
SELECT role_id, role_key, role_name, status, del_flag,
       (SELECT COUNT(*) FROM sys_user_role ur WHERE ur.role_id = r.role_id) AS 成员数,
       CASE del_flag WHEN '0' THEN '活跃' WHEN '2' THEN '已软删' ELSE del_flag END AS 说明
  FROM sys_role r
 WHERE role_name = '超级管理员'
 ORDER BY role_id;

-- ⑦ 后置校验 2：超管账号清单（应全部落在根部门、账号正常、可登录）
SELECT u.user_id, u.user_name, u.nick_name, u.dept_id, d.dept_name, u.status, u.user_status, u.del_flag,
       (SELECT GROUP_CONCAT(r2.role_key ORDER BY r2.role_id) FROM sys_user_role ur2
          JOIN sys_role r2 ON r2.role_id = ur2.role_id
         WHERE ur2.user_id = u.user_id AND r2.del_flag = '0') AS 角色
  FROM sys_user u
  LEFT JOIN sys_dept d ON d.dept_id = u.dept_id
 WHERE EXISTS (SELECT 1 FROM sys_user_role ur JOIN sys_role r ON r.role_id = ur.role_id
                WHERE ur.user_id = u.user_id AND r.role_key = 'SUPER_ADMIN')
 ORDER BY u.user_id;

-- ⑧ 后置校验 3：关键指标（活跃超级管理员角色数必须为 1）
SELECT (SELECT COUNT(*) FROM sys_role WHERE role_name = '超级管理员' AND del_flag = '0') AS 活跃超级管理员角色数_应为1,
       (SELECT COUNT(*) FROM sys_role WHERE role_key = 'admin' AND del_flag = '0')      AS 残留框架admin角色_应为0,
       (SELECT COUNT(*) FROM sys_user u
          JOIN sys_user_role ur ON ur.user_id = u.user_id
          JOIN sys_role r ON r.role_id = ur.role_id AND r.del_flag = '0'
         WHERE r.role_key = 'SUPER_ADMIN' AND u.del_flag = '0')                          AS 在册超管账号数,
       (SELECT COUNT(*) FROM sys_user u
          JOIN sys_user_role ur ON ur.user_id = u.user_id
          JOIN sys_role r ON r.role_id = ur.role_id AND r.del_flag = '0'
         WHERE r.role_key = 'SUPER_ADMIN' AND u.del_flag = '0'
           AND u.status = '0' AND NOT (u.dept_id <=> @root_dept))                        AS 超管未落在根部门或账号停用_应为0;
