-- ============================================================================
-- 实习生端「学习与考核」两级目录改造（幂等迁移脚本）
--
-- 目标：实习生侧栏只保留「工作台 + 学习与考核」两项，**去掉中间的「实习生门户」包装层**。
--   · 工作台        → 真实数据看板 views/index.vue（component = 'index'）
--   · 学习与考核    → 页签壳 assessment/learning/shell，内部 5 个页签：
--                      在线学习 / 备考资料 / 模拟考核 / 正式考核 / 正式考核成绩
--   · 其余子菜单    → 全部隐藏（保留路由与权限标识，便于后续回填）
--
-- 关键点（决定 URL 不变）：
--   两项从「实习生门户」(path=intern) 上移到「学习考核」(path=assessment) 下，
--   因此 path 必须带上 `intern/` 前缀，最终 URL 与改造前完全一致：
--     工作台      : /assessment + intern/intern-dashboard → /assessment/intern/intern-dashboard
--     学习与考核  : /assessment + intern/learning         → /assessment/intern/learning
--   → 页面内的硬编码跳转点（views/index.vue 等 12 处）**无需修改**。
--
-- 幂等性：全部为 UPDATE，按 (menu_type, path) 精确定位；重复执行结果一致。
-- 说明：**不修改** 老的一次性初始化脚本 intern_assessment_intern_navigation.sql。
-- ============================================================================

USE `intern_assessment`;

-- 定位父菜单：学习考核(path=assessment) 与 实习生门户(path=intern)
SET @assessment_menu_id = (
    SELECT menu_id FROM sys_menu
    WHERE menu_type = 'M' AND path = 'assessment'
    ORDER BY menu_id DESC LIMIT 1
);
SET @portal_menu_id = (
    SELECT menu_id FROM sys_menu
    WHERE menu_type = 'M' AND path = 'intern'
    ORDER BY menu_id DESC LIMIT 1
);

-- ① 工作台：上移到「学习考核」下，保留可见，指向真实数据看板
UPDATE sys_menu
SET parent_id  = @assessment_menu_id,
    menu_name  = '工作台',
    path       = 'intern/intern-dashboard',
    component  = 'index',
    icon       = 'dashboard',
    visible    = '0',
    order_num  = 2,
    update_by  = 'two-level-menu',
    update_time = NOW()
WHERE menu_type = 'C' AND path = 'intern-dashboard';

-- ② 学习与考核：上移到「学习考核」下，改为页签壳
UPDATE sys_menu
SET parent_id  = @assessment_menu_id,
    menu_name  = '学习与考核',
    path       = 'intern/learning',
    component  = 'assessment/learning/shell',
    icon       = 'education',
    visible    = '0',
    order_num  = 3,
    update_by  = 'two-level-menu',
    update_time = NOW()
WHERE menu_type = 'C' AND path IN ('learning', 'intern/learning');

-- ③ 隐藏「实习生门户」包装层（其下 8 个子菜单的路由与权限保留）
UPDATE sys_menu
SET visible    = '1',
    update_by  = 'two-level-menu',
    update_time = NOW()
WHERE menu_id = @portal_menu_id;

-- ④ 其余子菜单全部隐藏（保留权限标识与角色授权，仅从侧栏移除）
UPDATE sys_menu
SET visible    = '1',
    update_by  = 'two-level-menu',
    update_time = NOW()
WHERE parent_id = @portal_menu_id
  AND menu_type = 'C'
  AND path NOT IN ('intern-dashboard', 'learning');

-- ⑤ 「学习考核工作台」(admin 专用) 顺延到第一位，避免与新项重号
UPDATE sys_menu
SET order_num = 1, update_by = 'two-level-menu', update_time = NOW()
WHERE parent_id = @assessment_menu_id AND menu_type = 'C' AND order_num = 1 AND menu_name = '学习考核工作台';

-- ---------------------------------------------------------------------------
-- 校验：应输出两行 visible='0'（工作台 / 学习与考核），且其下无第三项
-- ---------------------------------------------------------------------------
SELECT menu_id, menu_name, path, component, visible, order_num
FROM sys_menu
WHERE parent_id = @assessment_menu_id AND menu_type = 'C'
ORDER BY order_num;
