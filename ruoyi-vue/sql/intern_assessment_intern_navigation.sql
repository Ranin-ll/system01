-- 实习生侧栏只显示工作台和消息中心。
-- 其余页面保留路由与权限，由工作台内入口访问。
USE `intern_assessment`;

SET @intern_menu_id = (
    SELECT menu_id FROM sys_menu
    WHERE menu_type = 'M' AND path = 'intern'
    ORDER BY menu_id DESC LIMIT 1
);

UPDATE sys_menu
SET visible = CASE
    WHEN path IN ('intern-dashboard', 'messages') THEN '0'
    ELSE '1'
END,
update_by = 'navigation', update_time = NOW()
WHERE parent_id = @intern_menu_id AND menu_type = 'C';
