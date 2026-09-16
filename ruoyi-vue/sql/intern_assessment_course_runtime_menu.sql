-- 将历史“课程设计”菜单切换到真实课程内容页面。
-- 可重复执行；不改变课程、章节和学习记录数据。
USE `intern_assessment`;

UPDATE sys_menu
SET component = 'business/course/runtime',
    perms = 'business:course:list',
    remark = '课程管理：章节、文档、视频、学习记录'
WHERE menu_id = 2037;

UPDATE sys_menu
SET component = 'business/course/runtime'
WHERE menu_id = 2006;

SELECT menu_id, menu_name, path, component, perms
FROM sys_menu
WHERE menu_id IN (2006, 2037);
