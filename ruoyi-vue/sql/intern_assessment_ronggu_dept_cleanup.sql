-- ============================================================
-- 融谷 · 实习生学习考核系统
-- 清除 RuoYi（若依）架构演示种子数据 —— 部门树 / 岗位 / 角色 / 账号 / 定时任务 / 公告 / 操作日志
--
-- 背景：sys_dept 里 102/108/109/101（深圳总公司、长沙分公司、市场部门、财务部门）
--       以及 sys_post、sys_job、sys_notice、sys_user(ry)、sys_role(common) 均为
--       RuoYi 框架自带的演示数据，不属于本系统业务。
--
-- 幂等：全部为带条件的 UPDATE / DELETE，可重复执行。
-- 执行前已核对：业务表（exam / register_application / course / practice_* /
--       audit_record / question_bank / promotion_application / stage_evaluation）
--       对 101/102/108/109 的引用数均为 0；用户仅分布在 103~107。
-- ============================================================

-- ---------- 1. 部门树：把 5 个真实培养部门挂到公司根节点 ----------
-- 原层级：融谷(100) › 深圳总公司(101) › 交付/开发/设计/质检/建模(103~107)
-- 新层级：融谷(100) › 交付/开发/设计/质检/建模(103~107)
UPDATE sys_dept
   SET parent_id = 100,
       ancestors = '0,100'
 WHERE dept_id BETWEEN 103 AND 107
   AND parent_id = 101;

-- ---------- 2. 清空 RuoYi 演示联系方式（负责人 / 电话 / 邮箱） ----------
UPDATE sys_dept
   SET leader = '',
       phone  = '',
       email  = ''
 WHERE email = 'ry@qq.com'
    OR phone = '15888888888'
    OR leader IN ('若依', '融谷');

-- ---------- 3. 删除 RuoYi 演示部门 ----------
-- 101 深圳总公司 / 102 长沙分公司 / 108 市场部门 / 109 财务部门
DELETE FROM sys_dept WHERE dept_id IN (101, 102, 108, 109);

-- ---------- 4. 清理悬空的数据权限（角色-部门） ----------
DELETE FROM sys_role_dept WHERE dept_id NOT IN (SELECT dept_id FROM sys_dept);

-- ---------- 5. RuoYi 演示岗位（ceo/se/hr/user） ----------
DELETE FROM sys_user_post
 WHERE post_id IN (SELECT post_id FROM sys_post WHERE post_code IN ('ceo', 'se', 'hr', 'user'));
DELETE FROM sys_post WHERE post_code IN ('ceo', 'se', 'hr', 'user');

-- ---------- 6. RuoYi 演示定时任务（ryTask.*） ----------
-- 注意：sys_job_log 无 job_id 列，用 invoke_target 关联
DELETE FROM sys_job_log WHERE invoke_target LIKE 'ryTask.%';
DELETE FROM sys_job     WHERE invoke_target LIKE 'ryTask.%';

-- ---------- 7. RuoYi 演示公告 ----------
DELETE FROM sys_notice WHERE notice_title LIKE '%若依%';

-- ---------- 8. RuoYi 演示账号 ry 与演示角色 common ----------
DELETE FROM sys_user_role WHERE user_id IN (SELECT user_id FROM sys_user WHERE user_name = 'ry');
DELETE FROM sys_user_post WHERE user_id IN (SELECT user_id FROM sys_user WHERE user_name = 'ry');
DELETE FROM sys_user      WHERE user_name = 'ry';

DELETE FROM sys_role_menu WHERE role_id IN (SELECT role_id FROM sys_role WHERE role_key = 'common');
DELETE FROM sys_role_dept WHERE role_id IN (SELECT role_id FROM sys_role WHERE role_key = 'common');
DELETE FROM sys_user_role WHERE role_id IN (SELECT role_id FROM sys_role WHERE role_key = 'common');
DELETE FROM sys_role      WHERE role_key = 'common';

-- ---------- 9. 清空含 com.ruoyi 包名的历史操作日志 ----------
-- 操作日志的 method 列会打印 com.ruoyi.web.controller.*，属可见的框架痕迹；
-- 表内 188 行均为演示期测试记录，清空不影响任何业务数据。
DELETE FROM sys_oper_log;

-- ---------- 10. 兜底：任何残留的「若依」字样 ----------
UPDATE sys_user SET email = '', phonenumber = '' WHERE email = 'ry@qq.com';
UPDATE sys_dict_data SET dict_label = REPLACE(dict_label, '若依', '') WHERE dict_label LIKE '%若依%';
UPDATE sys_dict_type SET dict_name  = REPLACE(dict_name,  '若依', '') WHERE dict_name  LIKE '%若依%';
