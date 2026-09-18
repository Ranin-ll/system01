-- ============================================================================
-- 实习生学习考核系统 - 本地审核测试账号
-- 可重复执行；密码统一为 admin123。
-- 有效测试账号固定为：1 个超级管理员、5 个部门管理员、5 个部门实习生。
-- 为覆盖 4 个业务角色，交付部门实习生为正式实习生，其余 4 人为预备实习生。
-- ============================================================================

USE `intern_assessment`;

START TRANSACTION;

SET @password_hash = '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2';
SET @super_role_id = (SELECT role_id FROM sys_role WHERE role_key = 'SUPER_ADMIN' AND del_flag = '0' LIMIT 1);
SET @dept_role_id = (SELECT role_id FROM sys_role WHERE role_key = 'DEPT_ADMIN' AND del_flag = '0' LIMIT 1);
SET @pre_role_id = (SELECT role_id FROM sys_role WHERE role_key = 'PRE_TRAINEE' AND del_flag = '0' LIMIT 1);
SET @formal_role_id = (SELECT role_id FROM sys_role WHERE role_key = 'FORMAL_TRAINEE' AND del_flag = '0' LIMIT 1);

SET @implementation_position_id = (SELECT id FROM position WHERE position_code = 'IMPLEMENTATION' AND deleted = 0 LIMIT 1);
SET @development_position_id = (SELECT id FROM position WHERE position_code = 'DEVELOPMENT' AND deleted = 0 LIMIT 1);
SET @design_position_id = (SELECT id FROM position WHERE position_code = 'DESIGN' AND deleted = 0 LIMIT 1);
SET @qa_position_id = (SELECT id FROM position WHERE position_code = 'QA' AND deleted = 0 LIMIT 1);
SET @modeling_position_id = (SELECT id FROM position WHERE position_code = 'MODELING' AND deleted = 0 LIMIT 1);

-- 迁移早期超过20 字符登录限制的实习生账号名，保留原 user_id 和业务关联。
UPDATE sys_user old_user
LEFT JOIN sys_user new_user ON new_user.user_name = 'test_impl_intern'
SET old_user.user_name = 'test_impl_intern'
WHERE old_user.user_name = 'test_delivery_trainee' AND new_user.user_id IS NULL;
UPDATE sys_user old_user
LEFT JOIN sys_user new_user ON new_user.user_name = 'test_dev_intern'
SET old_user.user_name = 'test_dev_intern'
WHERE old_user.user_name = 'test_development_trainee' AND new_user.user_id IS NULL;
UPDATE sys_user old_user
LEFT JOIN sys_user new_user ON new_user.user_name = 'test_design_intern'
SET old_user.user_name = 'test_design_intern'
WHERE old_user.user_name = 'test_design_trainee' AND new_user.user_id IS NULL;
UPDATE sys_user old_user
LEFT JOIN sys_user new_user ON new_user.user_name = 'test_qa_intern'
SET old_user.user_name = 'test_qa_intern'
WHERE old_user.user_name = 'test_qa_trainee' AND new_user.user_id IS NULL;
UPDATE sys_user old_user
LEFT JOIN sys_user new_user ON new_user.user_name = 'test_model_intern'
SET old_user.user_name = 'test_model_intern'
WHERE old_user.user_name = 'test_modeling_trainee' AND new_user.user_id IS NULL;

-- 超级管理员是全局角色，不绑定任何部门。
INSERT INTO sys_user
    (dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password,
     status, del_flag, create_by, create_time, remark)
SELECT NULL, 'test_super_admin', '测试超级管理员', '00', '', '13800000000', '0', '', @password_hash,
       '0', '0', 'seed', NOW(), '固定测试账号：超级管理员'
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE user_name = 'test_super_admin');

-- 每个部门一个部门管理员。管理员不占用实习生岗位，也不是导师账号。
INSERT INTO sys_user
    (dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password,
     status, del_flag, create_by, create_time, remark)
SELECT 103, 'test_dept_admin', '交付部门管理员', '00', '', '13800000103', '0', '', @password_hash,
       '0', '0', 'seed', NOW(), '固定测试账号：交付部门管理员'
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE user_name = 'test_dept_admin');
INSERT INTO sys_user
    (dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password,
     status, del_flag, create_by, create_time, remark)
SELECT 104, 'test_dev_admin', '开发部门管理员', '00', '', '13800000104', '0', '', @password_hash,
       '0', '0', 'seed', NOW(), '固定测试账号：开发部门管理员'
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE user_name = 'test_dev_admin');
INSERT INTO sys_user
    (dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password,
     status, del_flag, create_by, create_time, remark)
SELECT 105, 'test_design_admin', '设计部门管理员', '00', '', '13800000105', '0', '', @password_hash,
       '0', '0', 'seed', NOW(), '固定测试账号：设计部门管理员'
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE user_name = 'test_design_admin');
INSERT INTO sys_user
    (dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password,
     status, del_flag, create_by, create_time, remark)
SELECT 106, 'test_qa_admin', '质检部门管理员', '00', '', '13800000106', '0', '', @password_hash,
       '0', '0', 'seed', NOW(), '固定测试账号：质检部门管理员'
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE user_name = 'test_qa_admin');
INSERT INTO sys_user
    (dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password,
     status, del_flag, create_by, create_time, remark)
SELECT 107, 'test_model_admin', '建模部门管理员', '00', '', '13800000107', '0', '', @password_hash,
       '0', '0', 'seed', NOW(), '固定测试账号：建模部门管理员'
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE user_name = 'test_model_admin');

-- 每个部门一个对应岗位的实习生。导师仅为档案文本字段，不关联 sys_user。
INSERT INTO sys_user
    (dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password,
     status, del_flag, create_by, create_time, remark)
SELECT 103, 'test_impl_intern', '交付部门实习生', '00', '', '13900000103', '0', '', @password_hash,
       '0', '0', 'seed', NOW(), '固定测试账号：实施实习生（正式）'
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE user_name = 'test_impl_intern');
INSERT INTO sys_user
    (dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password,
     status, del_flag, create_by, create_time, remark)
SELECT 104, 'test_dev_intern', '开发部门实习生', '00', '', '13900000104', '0', '', @password_hash,
       '0', '0', 'seed', NOW(), '固定测试账号：开发实习生（预备）'
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE user_name = 'test_dev_intern');
INSERT INTO sys_user
    (dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password,
     status, del_flag, create_by, create_time, remark)
SELECT 105, 'test_design_intern', '设计部门实习生', '00', '', '13900000105', '0', '', @password_hash,
       '0', '0', 'seed', NOW(), '固定测试账号：设计实习生（预备）'
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE user_name = 'test_design_intern');
INSERT INTO sys_user
    (dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password,
     status, del_flag, create_by, create_time, remark)
SELECT 106, 'test_qa_intern', '质检部门实习生', '00', '', '13900000106', '0', '', @password_hash,
       '0', '0', 'seed', NOW(), '固定测试账号：质检实习生（预备）'
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE user_name = 'test_qa_intern');
INSERT INTO sys_user
    (dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password,
     status, del_flag, create_by, create_time, remark)
SELECT 107, 'test_model_intern', '建模部门实习生', '00', '', '13900000107', '0', '', @password_hash,
       '0', '0', 'seed', NOW(), '固定测试账号：建模实习生（预备）'
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE user_name = 'test_model_intern');

-- 每次执行都恢复固定账号资料，保证本地审核环境可重现。
UPDATE sys_user
SET dept_id = NULL, position_id = NULL, mentor_name = NULL, mentor_phone = NULL,
    nick_name = '测试超级管理员', phonenumber = '13800000000', password = @password_hash,
    user_status = 'NON_INTERN', protocol_status = 1, is_dept_admin = 0,
    status = '0', del_flag = '0', update_by = 'seed', update_time = NOW()
WHERE user_name = 'test_super_admin';

UPDATE sys_user
SET dept_id = CASE user_name
        WHEN 'test_dept_admin' THEN 103 WHEN 'test_dev_admin' THEN 104
        WHEN 'test_design_admin' THEN 105 WHEN 'test_qa_admin' THEN 106
        WHEN 'test_model_admin' THEN 107 END,
    position_id = NULL, mentor_name = NULL, mentor_phone = NULL,
    nick_name = CASE user_name
        WHEN 'test_dept_admin' THEN '交付部门管理员' WHEN 'test_dev_admin' THEN '开发部门管理员'
        WHEN 'test_design_admin' THEN '设计部门管理员' WHEN 'test_qa_admin' THEN '质检部门管理员'
        WHEN 'test_model_admin' THEN '建模部门管理员' END,
    phonenumber = CASE user_name
        WHEN 'test_dept_admin' THEN '13800000103' WHEN 'test_dev_admin' THEN '13800000104'
        WHEN 'test_design_admin' THEN '13800000105' WHEN 'test_qa_admin' THEN '13800000106'
        WHEN 'test_model_admin' THEN '13800000107' END,
    password = @password_hash, user_status = 'NON_INTERN', protocol_status = 1,
    is_dept_admin = 1, status = '0', del_flag = '0', update_by = 'seed', update_time = NOW()
WHERE user_name IN ('test_dept_admin', 'test_dev_admin', 'test_design_admin', 'test_qa_admin', 'test_model_admin');

UPDATE sys_user
SET dept_id = CASE user_name
        WHEN 'test_impl_intern' THEN 103 WHEN 'test_dev_intern' THEN 104
        WHEN 'test_design_intern' THEN 105 WHEN 'test_qa_intern' THEN 106
        WHEN 'test_model_intern' THEN 107 END,
    position_id = CASE user_name
        WHEN 'test_impl_intern' THEN @implementation_position_id
        WHEN 'test_dev_intern' THEN @development_position_id
        WHEN 'test_design_intern' THEN @design_position_id
        WHEN 'test_qa_intern' THEN @qa_position_id
        WHEN 'test_model_intern' THEN @modeling_position_id END,
    mentor_name = CASE user_name
        WHEN 'test_impl_intern' THEN '张导师' WHEN 'test_dev_intern' THEN '李导师'
        WHEN 'test_design_intern' THEN '王导师' WHEN 'test_qa_intern' THEN '赵导师'
        WHEN 'test_model_intern' THEN '陈导师' END,
    mentor_phone = CASE user_name
        WHEN 'test_impl_intern' THEN '13700000103' WHEN 'test_dev_intern' THEN '13700000104'
        WHEN 'test_design_intern' THEN '13700000105' WHEN 'test_qa_intern' THEN '13700000106'
        WHEN 'test_model_intern' THEN '13700000107' END,
    password = @password_hash,
    user_status = CASE WHEN user_name = 'test_impl_intern' THEN 'FORMAL_TRAINEE' ELSE 'PRE_TRAINEE' END,
    protocol_status = CASE WHEN user_name = 'test_impl_intern' THEN 1 ELSE 0 END,
    is_dept_admin = 0, status = '0', del_flag = '0', update_by = 'seed', update_time = NOW()
WHERE user_name IN ('test_impl_intern', 'test_dev_intern', 'test_design_intern',
                    'test_qa_intern', 'test_model_intern');

-- 早期通用实习生账号保留记录但停用，避免与每部门唯一测试账号混淆。
UPDATE sys_user
SET user_status = 'DISABLED', status = '1', update_by = 'seed', update_time = NOW(),
    remark = '旧版通用测试账号，已由五部门测试账号替代'
WHERE user_name IN ('test_pre_trainee', 'test_formal_trainee');

-- 固定测试账号只保留一个业务角色。
DELETE ur FROM sys_user_role ur
JOIN sys_user u ON u.user_id = ur.user_id
WHERE u.user_name IN (
    'test_super_admin', 'test_dept_admin', 'test_dev_admin', 'test_design_admin',
    'test_qa_admin', 'test_model_admin', 'test_impl_intern',
    'test_dev_intern', 'test_design_intern', 'test_qa_intern',
    'test_model_intern', 'test_pre_trainee', 'test_formal_trainee'
);

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.user_id,
       CASE
           WHEN u.user_name = 'test_super_admin' THEN @super_role_id
           WHEN u.user_name IN ('test_dept_admin', 'test_dev_admin', 'test_design_admin', 'test_qa_admin', 'test_model_admin') THEN @dept_role_id
           WHEN u.user_name = 'test_impl_intern' THEN @formal_role_id
           ELSE @pre_role_id
       END
FROM sys_user u
WHERE u.user_name IN (
    'test_super_admin', 'test_dept_admin', 'test_dev_admin', 'test_design_admin',
    'test_qa_admin', 'test_model_admin', 'test_impl_intern',
    'test_dev_intern', 'test_design_intern', 'test_qa_intern', 'test_model_intern'
);

-- 业务菜单权限。部门管理员只能处理所属部门数据，数据边界由后端再次校验。
SET @root_menu_id = (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'assessment' LIMIT 1);
SET @intern_menu_id = (SELECT menu_id FROM sys_menu WHERE parent_id = @root_menu_id AND path = 'intern' LIMIT 1);
SET @department_menu_id = (SELECT menu_id FROM sys_menu WHERE parent_id = @root_menu_id AND path = 'department' LIMIT 1);
SET @manage_menu_id = (SELECT menu_id FROM sys_menu WHERE parent_id = @root_menu_id AND path = 'manage' LIMIT 1);
SET @system_menu_id = (SELECT menu_id FROM sys_menu WHERE parent_id = @root_menu_id AND path = 'system' LIMIT 1);
SET @register_review_menu_id = (SELECT menu_id FROM sys_menu WHERE menu_type = 'C' AND perms = 'business:register:list' ORDER BY menu_id DESC LIMIT 1);

DELETE FROM sys_role_menu
WHERE role_id IN (@super_role_id, @dept_role_id, @pre_role_id, @formal_role_id)
  AND (menu_id = @root_menu_id
       OR menu_id IN (@intern_menu_id, @department_menu_id, @manage_menu_id, @system_menu_id)
       OR menu_id IN (SELECT menu_id FROM sys_menu
                      WHERE parent_id IN (@intern_menu_id, @department_menu_id, @manage_menu_id, @system_menu_id)));

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @super_role_id, menu_id FROM sys_menu
WHERE menu_id IN (@root_menu_id, @department_menu_id, @manage_menu_id, @system_menu_id, @register_review_menu_id)
   OR parent_id IN (@manage_menu_id, @system_menu_id, @register_review_menu_id);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @dept_role_id, menu_id FROM sys_menu
WHERE menu_id IN (@root_menu_id, @department_menu_id, @manage_menu_id, @register_review_menu_id)
   OR parent_id IN (@department_menu_id, @manage_menu_id, @register_review_menu_id);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @pre_role_id, menu_id FROM sys_menu
WHERE menu_id IN (@root_menu_id, @intern_menu_id) OR parent_id = @intern_menu_id;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @formal_role_id, menu_id FROM sys_menu
WHERE menu_id IN (@root_menu_id, @intern_menu_id) OR parent_id = @intern_menu_id;

COMMIT;

-- Navicat 核验：查询账号、部门、岗位和角色是否按 1+5+5 生效。
SELECT u.user_name, u.nick_name, d.dept_name, p.position_name, u.user_status,
       u.mentor_name, u.mentor_phone, r.role_key
FROM sys_user u
LEFT JOIN sys_dept d ON d.dept_id = u.dept_id
LEFT JOIN position p ON p.id = u.position_id
LEFT JOIN sys_user_role ur ON ur.user_id = u.user_id
LEFT JOIN sys_role r ON r.role_id = ur.role_id
WHERE u.user_name IN (
    'test_super_admin', 'test_dept_admin', 'test_dev_admin', 'test_design_admin',
    'test_qa_admin', 'test_model_admin', 'test_impl_intern',
    'test_dev_intern', 'test_design_intern', 'test_qa_intern', 'test_model_intern'
)
ORDER BY u.user_id;
