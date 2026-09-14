-- ============================================================================
-- 实习生学习考核系统 - 重复超级管理员测试账号修复
--
-- 若依登录查询要求 user_name 唯一。早期重复执行测试账号脚本时，可能
-- 产生多个 test_super_admin，导致登录时报 TooManyResultsException。
-- 本脚本保留已绑定 SUPER_ADMIN 角色的账号，其余重复记录改为可追溯的
-- 旧测试账号名，不删除用户数据，也不影响注册申请和审核记录。
-- ============================================================================

USE `intern_assessment`;

SET @keep_super_admin_id = (
    SELECT u.user_id
    FROM sys_user u
    JOIN sys_user_role ur ON ur.user_id = u.user_id
    JOIN sys_role r ON r.role_id = ur.role_id
    WHERE u.user_name = 'test_super_admin'
      AND u.del_flag = '0'
      AND r.role_key = 'SUPER_ADMIN'
      AND r.del_flag = '0'
    ORDER BY u.user_id
    LIMIT 1
);

SET @keep_super_admin_id = COALESCE(
    @keep_super_admin_id,
    (SELECT MIN(user_id) FROM sys_user WHERE user_name = 'test_super_admin' AND del_flag = '0')
);

UPDATE sys_user
SET user_name = CONCAT('old_super_', user_id),
    update_by = 'seed',
    update_time = NOW(),
    remark = 'duplicate test account; original user data preserved'
WHERE user_name = 'test_super_admin'
  AND del_flag = '0'
  AND user_id <> @keep_super_admin_id;

SET @keep_pre_trainee_id = (
    SELECT u.user_id
    FROM sys_user u
    JOIN sys_user_role ur ON ur.user_id = u.user_id
    JOIN sys_role r ON r.role_id = ur.role_id
    WHERE u.user_name = 'test_pre_trainee'
      AND u.del_flag = '0'
      AND r.role_key = 'PRE_TRAINEE'
      AND r.del_flag = '0'
    ORDER BY u.user_id
    LIMIT 1
);

UPDATE sys_user
SET user_name = CONCAT('old_pre_', user_id),
    update_by = 'seed',
    update_time = NOW(),
    remark = 'duplicate test account; original user data preserved'
WHERE user_name = 'test_pre_trainee'
  AND del_flag = '0'
  AND user_id <> @keep_pre_trainee_id;

SET @keep_formal_trainee_id = (
    SELECT u.user_id
    FROM sys_user u
    JOIN sys_user_role ur ON ur.user_id = u.user_id
    JOIN sys_role r ON r.role_id = ur.role_id
    WHERE u.user_name = 'test_formal_trainee'
      AND u.del_flag = '0'
      AND r.role_key = 'FORMAL_TRAINEE'
      AND r.del_flag = '0'
    ORDER BY u.user_id
    LIMIT 1
);

UPDATE sys_user
SET user_name = CONCAT('old_formal_', user_id),
    update_by = 'seed',
    update_time = NOW(),
    remark = 'duplicate test account; original user data preserved'
WHERE user_name = 'test_formal_trainee'
  AND del_flag = '0'
  AND user_id <> @keep_formal_trainee_id;
