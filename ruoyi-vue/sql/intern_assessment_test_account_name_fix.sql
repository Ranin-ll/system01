-- 测试部门管理员账号名称修复：系统默认登录名最大长度为 20。
USE `intern_assessment`;

UPDATE sys_user SET user_name = 'test_dev_admin'
WHERE user_name = 'test_dept_admin_development';

UPDATE sys_user SET user_name = 'test_design_admin'
WHERE user_name = 'test_dept_admin_design';

UPDATE sys_user SET user_name = 'test_qa_admin'
WHERE user_name = 'test_dept_admin_qa';

UPDATE sys_user SET user_name = 'test_model_admin'
WHERE user_name = 'test_dept_admin_modeling';
