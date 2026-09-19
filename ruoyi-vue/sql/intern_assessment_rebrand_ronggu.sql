-- ============================================================================
-- 品牌替换迁移：去除「若依」痕迹，统一为「融谷」（幂等，可重复执行）
--
-- 背景：系统所属公司为融谷，前端/后端/脚本中的框架品牌字样已统一替换；
--       本脚本处理**数据库里**的残留（部门名、账号昵称、框架外链菜单）。
--
-- 说明：question.knowledge_point = '若依' 属于**考试内容**（考核实习生对该技术栈的掌握），
--       不在本次替换范围内，如需一并调整请单独确认。
-- ============================================================================

USE `intern_assessment`;

-- ① 企业根部门：若依科技 → 融谷（含负责人与邮箱）
UPDATE `sys_dept`
SET `dept_name` = '融谷',
    `leader`    = CASE WHEN `leader` = '若依' THEN '融谷' ELSE `leader` END,
    `email`     = CASE WHEN `email` LIKE '%ruoyi%' THEN NULL ELSE `email` END
WHERE `dept_id` = 100
  AND (`dept_name` = '若依科技' OR `leader` = '若依' OR `email` LIKE '%ruoyi%');

-- ② 内置账号昵称：若依 → 融谷管理员 / 融谷
UPDATE `sys_user` SET `nick_name` = '融谷管理员' WHERE `user_id` = 1 AND `nick_name` = '若依';
UPDATE `sys_user` SET `nick_name` = '融谷'       WHERE `user_name` = 'ry' AND `nick_name` = '若依';

-- ③ 下架框架官网外链菜单（若依官网 → ruoyi.vip）
DELETE FROM `sys_role_menu` WHERE `menu_id` = 4;
DELETE FROM `sys_menu`      WHERE `menu_id` = 4 AND `menu_name` = '若依官网';

-- ④ 兜底：其它菜单名里若仍有品牌字样，统一改名（保留菜单功能）
UPDATE `sys_menu` SET `menu_name` = '融谷官网' WHERE `menu_name` = '若依官网';
