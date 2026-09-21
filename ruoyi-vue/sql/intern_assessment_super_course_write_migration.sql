-- ============================================================================
-- 超管可写「课程与题库」（2026-09-20 规则调整）
--
-- 背景：原规则是「超管对业务实例一律只读」。实测权限分配是**不对称**的：
--         SUPER_ADMIN 的课程权限只有 business:course:list / query（写权限从未授予）；
--         而题库 business:bank:* 当时就授全了（add/edit/remove 都有）。
--       所以「超管管不了课程」是两层叠加：① 没授权 ② Service 里 managerScopeDeptId() 直接抛异常。
--
-- 本脚本只解决 ①（授权）；② 已由代码改动解决：
--   CourseServiceImpl#managerScopeDeptId()         超管返回 null（不限部门），不再抛异常
--   CourseServiceImpl#checkPositionScope()         deptId 为 null 时只校验岗位存在
--   CourseContentServiceImpl#managerScopeDeptId()  超管直接放行
--
-- 幂等：可重复执行（NOT EXISTS 判重，第二次影响 0 行）。
-- 不改任何既有接口的签名 / 返回结构 / 权限注解。
-- ============================================================================

SET @super_role_id = (SELECT role_id FROM sys_role
                      WHERE role_key = 'SUPER_ADMIN' AND del_flag = '0' LIMIT 1);

SELECT IF(@super_role_id IS NULL,
          '【错误】未找到 SUPER_ADMIN 角色，脚本将不生效',
          CONCAT('SUPER_ADMIN 角色存在 (role_id=', @super_role_id, ')，继续')) AS precheck;

-- 给 SUPER_ADMIN 补上课程写权限（已有则跳过）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @super_role_id, m.menu_id
FROM sys_menu m
WHERE m.perms IN ('business:course:add', 'business:course:edit',
                  'business:course:remove', 'business:course:publish')
  AND @super_role_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm
                  WHERE rm.role_id = @super_role_id AND rm.menu_id = m.menu_id);

SELECT ROW_COUNT() AS inserted_rows;

-- 自检①：SUPER_ADMIN 的课程权限（期望 6 个：add/edit/list/query/remove/publish）
SELECT GROUP_CONCAT(m.perms ORDER BY m.perms) AS super_course_perms
FROM sys_role_menu rm
         JOIN sys_menu m ON m.menu_id = rm.menu_id
WHERE rm.role_id = @super_role_id
  AND m.perms LIKE 'business:course%';

-- 自检②：题库权限应仍是全套（本次不动）
SELECT GROUP_CONCAT(m.perms ORDER BY m.perms) AS super_bank_perms
FROM sys_role_menu rm
         JOIN sys_menu m ON m.menu_id = rm.menu_id
WHERE rm.role_id = @super_role_id
  AND m.perms LIKE 'business:bank%';

-- 自检③：部门管理员权限未受影响（应仍为全套 7 个）
SELECT GROUP_CONCAT(m.perms ORDER BY m.perms) AS dept_course_perms
FROM sys_role_menu rm
         JOIN sys_menu m ON m.menu_id = rm.menu_id
         JOIN sys_role r ON r.role_id = rm.role_id
WHERE r.role_key = 'DEPT_ADMIN'
  AND m.perms LIKE 'business:course%';
