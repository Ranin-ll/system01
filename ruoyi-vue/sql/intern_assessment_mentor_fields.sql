-- 实习生导师档案字段迁移
-- 导师不是系统角色，不创建导师用户；审核通过时只保存姓名和联系方式。
USE `intern_assessment`;

SET @add_mentor_name_sql = IF(
    EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'mentor_name'),
    'SELECT 1',
    'ALTER TABLE sys_user ADD COLUMN mentor_name VARCHAR(64) DEFAULT NULL COMMENT ''导师姓名（审核时人工登记）'' AFTER position_id'
);
PREPARE stmt FROM @add_mentor_name_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_mentor_phone_sql = IF(
    EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'mentor_phone'),
    'SELECT 1',
    'ALTER TABLE sys_user ADD COLUMN mentor_phone VARCHAR(32) DEFAULT NULL COMMENT ''导师联系方式（审核时人工登记）'' AFTER mentor_name'
);
PREPARE stmt FROM @add_mentor_phone_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 清理旧版本错误绑定的审核员导师关系，并删除旧的角色关联字段；旧列存在时才执行。
SET @clear_old_mentor_sql = IF(
    EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'mentor_user_id'),
    'UPDATE sys_user SET mentor_user_id = NULL WHERE mentor_user_id IS NOT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @clear_old_mentor_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_old_mentor_sql = IF(
    EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'mentor_user_id'),
    'ALTER TABLE sys_user DROP COLUMN mentor_user_id',
    'SELECT 1'
);
PREPARE stmt FROM @drop_old_mentor_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
