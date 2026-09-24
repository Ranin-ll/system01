import auth from '@/plugins/auth'
import router, { constantRoutes, dynamicRoutes } from '@/router'
import { getRouters } from '@/api/menu'
import Layout from '@/layout/index'
import ParentView from '@/components/ParentView'
import InnerLink from '@/layout/components/InnerLink'

const permission = {
  state: {
    routes: [],
    addRoutes: [],
    defaultRoutes: [],
    topbarRouters: [],
    sidebarRouters: []
  },
  mutations: {
    SET_ROUTES: (state, routes) => {
      state.addRoutes = routes
      state.routes = constantRoutes.concat(routes)
    },
    SET_DEFAULT_ROUTES: (state, routes) => {
      state.defaultRoutes = constantRoutes.concat(routes)
    },
    SET_TOPBAR_ROUTES: (state, routes) => {
      state.topbarRouters = routes
    },
    SET_SIDEBAR_ROUTERS: (state, routes) => {
      state.sidebarRouters = routes
    },
  },
  actions: {
    // 生成路由
    GenerateRoutes({ commit, rootGetters }) {
      return new Promise(resolve => {
        // 向后端请求路由数据
        getRouters().then(res => {
          const sdata = JSON.parse(JSON.stringify(res.data))
          const rdata = JSON.parse(JSON.stringify(res.data))
          const sidebarRoutes = filterAsyncRouter(sdata)
          const rewriteRoutes = filterAsyncRouter(rdata, false, true)
          const asyncRoutes = filterDynamicRoutes(dynamicRoutes);
          rewriteRoutes.push({ path: '*', redirect: '/404', hidden: true })
          router.addRoutes(asyncRoutes);
          commit('SET_ROUTES', rewriteRoutes)
          const sidebar = isInternRole(rootGetters.roles)
            ? buildInternSidebar(rootGetters.roles)
            : (isSuperAdminRole(rootGetters.roles)
              ? buildSuperSidebar(sidebarRoutes)
              : (isDeptAdminRole(rootGetters.roles)
                ? buildDeptAdminSidebar()
                : constantRoutes.concat(sidebarRoutes)))
          commit('SET_SIDEBAR_ROUTERS', sidebar)
          commit('SET_DEFAULT_ROUTES', sidebarRoutes)
          commit('SET_TOPBAR_ROUTES', sidebarRoutes)
          resolve(rewriteRoutes)
        })
      })
    }
  }
}

function isInternRole(roles = []) {
  return roles.indexOf('PRE_TRAINEE') > -1 || roles.indexOf('FORMAL_TRAINEE') > -1
}

function isDeptAdminRole(roles = []) {
  return roles.indexOf('DEPT_ADMIN') > -1
}

/** 超管：内置 admin（role_key='admin'）与业务超管角色 SUPER_ADMIN 都算 */
function isSuperAdminRole(roles = []) {
  return roles.indexOf('SUPER_ADMIN') > -1 || roles.indexOf('admin') > -1
}

// ==========================================================================
// 超管端一级目录（V2 定稿五目录 → 2026-09-17 决策「三端命名统一」后为六目录）：
//   全局工作台（单链接）· 组织与人员 · 培养运营 · 任务与通知（单链接）· 规则与配置（单链接）· 审计与合规（单链接）
// 后面再接平台原生的「系统管理 / 系统监控 / 系统工具」三项（来自 DB 菜单）。
//
// 为什么在 store 里手搓，而不是直接用后端菜单树：
//   `sys_menu` 里超管仍是旧的「业务管理 / 学习考核（内含 部门运营 / 考核认证管理 / 系统运营）」结构，
//   与设计稿的五目录长期不一致，且其中多个页面指向骨架页。侧栏只影响展示，路由仍来自
//   dynamicRoutes（/super/**）+ DB 菜单，改造零 SQL、零冲突。
// ==========================================================================
function buildSuperSidebar(dbRoutes = []) {
  const root = dynamicRoutes.find(route => route.path === '/super')
  const all = (root ? root.children : []).filter(child => !child.hidden)

  // 组内 path 只取末段，使「组路径 + 末段」正好等于真实路由（同部门端做法）
  const group = (path, title, icon, segs) => {
    const children = segs
      .map(seg => all.find(child => child.path === seg))
      .filter(Boolean)
      .map(child => Object.assign({}, child, { path: child.path.split('/').pop() }))
    if (!children.length) return null
    return {
      path,
      component: Layout,
      alwaysShow: true,
      redirect: path + '/' + children[0].path,
      meta: { title, icon },
      children
    }
  }

  // 单项目录：包一层 Layout 子项，侧栏自动折叠成一级链接
  //
  // ⚠️ 子项必须写成**绝对真实路由**，不能沿用相对段。
  // SidebarItem 用 `path.resolve(basePath, childPath)` 拼最终链接，basePath 取目录自身的 path。
  // 例：目录 '/super/rule' + 子段 'rule' → 拼成 '/super/rule/rule' → 404。
  // 分组目录（group）之所以没这问题，是因为「组路径 + 末段」正好等于真实路由
  // （'/super/org' + 'organization' = '/super/org/organization'）；
  // 而单项目录的目录路径**本身就是**真实路由，再拼一段就多了一层。
  // 传入的 path 是目录（菜单项）路径，真实路由 = '/super' + '/' + child.path。
  const single = (path, seg) => {
    const child = all.find(item => item.path === seg)
    if (!child) return null
    const realPath = '/super/' + child.path
    return { path, component: Layout, children: [Object.assign({}, child, { path: realPath })] }
  }

  // 板块分组（跨前缀安全）：子项写**绝对路由**，组路径只作容器标识。
  //
  // 为什么需要它：group() 靠「组路径 + 末段 = 真实路由」的巧合，而「通知与督办」的两个子项
  // 来自 notify / todo 两个**不同前缀**，取末段会拼出 /super/notify/todo 这种错路径。
  // 改用绝对路由后这条易错规则在本函数里彻底消失。
  // 注意：**不改既有 group() / single()**（部门端仍在用 group()）。
  const absGroup = (path, title, icon, segs) => {
    const children = segs
      .map(seg => all.find(child => child.path === seg))
      .filter(Boolean)
      .map(child => Object.assign({}, child, { path: '/super/' + child.path }))
    if (!children.length) return null
    return {
      path,
      component: Layout,
      alwaysShow: true,
      redirect: children[0].path,
      meta: { title, icon },
      children
    }
  }

  // 原生系统三项（系统管理 / 系统监控 / 系统工具）直接透传 DB 菜单
  //
  // ⚠️ 必须**剥掉前导斜杠**再比对：后端 SysMenuServiceImpl.getRouterPath 对「一级目录」
  // （parentId=0 且 menu_type=M 且非外链）会自动补 '/'，实测 getRouters 下发的是
  // '/system' '/monitor' '/tool' 而不是 'system'。2026-09-22 前这里按无斜杠比较，
  // 导致 systemDirs 恒为空 —— 超管（含内置 admin）侧栏一直看不到这三项，与权限表无关。
  const systemDirs = dbRoutes.filter(route =>
    ['system', 'monitor', 'tool'].indexOf(String(route.path || '').replace(/^\//, '')) > -1)

  return [
    single('/super', 'dashboard'),
    // 「组织岗位」页已于 2026-09-20 整合部门/岗位管理（三个页签），
    // 原独立的 `org/positions` 入口从侧栏移除（路由保留，避免旧链接 404）。
    // 「部门管理员」页已于 2026-09-20 并入「督办看板」的「按人」视角（内容与看板同源）；
    // 路由保留并重定向到 /super/todo?view=people，侧栏不再单独列出 —— 本组回归纯「组织」语义。
    absGroup('/super/org', '组织与人员', 'peoples',
      // 「导师管理」2026-09-23 新增（org/mentor），与部门端共用同一页面组件
      ['org/organization', 'org/roles', 'org/accounts', 'org/mentor']),
    group('/super/ops', '培养运营', 'education',
      // ★ 2026-09-22：'ops/psubject-admin'（实操题库）已下线 —— 题库管理内点实操题库即可进入维护
      // ★ 顺序即侧栏顺序（group() 用 .map 保序，不走排序）：
      //   备考在前、考核在后 —— 先配置好「模拟备考管理」的套卷，再进「考核与成绩」看结果，符合使用动线。
      ['ops/analysis', 'ops/courses', 'ops/course-admin', 'ops/bank-admin', 'ops/prep', 'ops/exams']),
    // 「任务与通知」→「通知与督办」：多了「督办看板」，单项目录升级为分组。
    // 两个子项来自 notify / todo 两个前缀 → 必须用 absGroup（group 取末段会拼错）。
    absGroup('/super/notify', '通知与督办', 'message', ['notify', 'todo']),
    single('/super/rule', 'rule'),
    single('/super/audit', 'audit')
  ].filter(Boolean).concat(systemDirs)
}

// 部门管理员端一级目录（设计稿 §一）：四目录 —— 工作台 / 人员管理 / 学习与考核管理 / 任务与通知。
//
// 为什么同样在 store 里手搓，而不用后端菜单树：
// `sys_menu` 里部门管理员仍是老的「部门运营 / 考核认证管理」两目录，且其中 14 个页面
// 指向公共骨架页（`assessment/index`），与设计稿长期不一致。后端菜单树的改造属
// 「共同登记区」（同事分支也在动 `sys_menu`），本轮先用前端侧栏把设计稿落地，
// 不需要跑任何 SQL；后续若要转回菜单表，按 `router/index.js` 里 /department 那段
// 逐条建菜单行即可（path / component / meta 完全对应）。
//
// 注意：**超管（SUPER_ADMIN）不套用本侧栏**，继续使用后端菜单（保留系统管理）。
function buildDeptAdminSidebar() {
  const root = dynamicRoutes.find(route => route.path === '/department')
  const all = (root ? root.children : []).filter(child => !child.hidden)

  // 按目录分组：组内路径只取末段，使「组路径 + 末段」正好等于真实路由
  const group = (path, title, icon, segs) => {
    const children = segs
      .map(seg => all.find(child => child.path === seg))
      .filter(Boolean)
      .map(child => Object.assign({}, child, { path: child.path.split('/').pop() }))
    if (!children.length) return null
    return {
      path,
      component: Layout,
      alwaysShow: true,
      redirect: path + '/' + children[0].path,
      meta: { title, icon },
      children
    }
  }

  // 工作台：单项顶层目录（只有一个子项，侧栏会自动折叠成一级链接）
  const dashboard = all.find(child => child.path === 'dashboard')
  const dash = dashboard
    ? [{ path: '/department', component: Layout, children: [Object.assign({}, dashboard)] }]
    : []

  return dash.concat([
    group('/department/people', '人员管理', 'peoples',
      // ★ 顺序即侧栏顺序（group 用 .map 保序，不走排序）
      // 「导师管理」2026-09-23 新增，放在实习生管理之后、转正审核之前
      ['people/register-review', 'people/students', 'people/mentors', 'people/promotion']),
    group('/department/study', '学习与考核管理', 'education',
      ['study/courses', 'study/banks', 'study/prep', 'study/exam', 'study/scores'])
    // ★ 2026-09-23：「任务与通知」整组下线（用户口径：部门管理员只接收管理员通知，
    //   通知入口统一走顶栏「消息中心」铃铛）。路由保留，避免旧链接 404。
  ].filter(Boolean))
}

// 实习生端一级目录（设计稿 V1.2，2026-09-17 起加第三项：
//   ① 工作台  ② 学习与考核  ③ 任务与通知；
//   2026-09-23 起**删掉「任务与通知」**并把「学习与考核」收成单入口 —— 只留两项：
//   ① 工作台  ② 学习与考核）
//
// 为什么在 store 里手搓，而不直接用后端菜单树（/getRouters）：
// 后端菜单树是给管理端用的，「学习考核」目录下还挂着部门运营 / 考核认证管理 /
// 系统运营等实习生用不到的目录，直接透传会让实习生侧栏出现一堆无用层级。
// 这里显式声明目录，只影响侧栏渲染（sidebarRouters 仅用于展示），
// 真正参与路由匹配的仍是 dynamicRoutes + 后端菜单生成的 rewriteRoutes。
//
// 「消息中心」不再占侧栏位：入口只保留顶栏铃铛（未读红点，见 layout/components/Navbar.vue），
// 页面仍是 constantRoutes 的 `/messages`（含「通知 / 我的任务」两个页签），路由与页面都没删。
function buildInternSidebar(roles = []) {
  // ① 工作台：沿用 constantRoutes 的首页路由（affix，刷新后仍能定位）
  const dashboard = constantRoutes.filter(route => !route.hidden && route.path === '')

  // ② 学习与考核：**单入口，无二级菜单**（2026-09-23 起）
  //
  // 原先这里把 5 个页签摊成二级菜单（在线学习 / 备考资料 / 模拟考核 / 正式考核 /
  // 考核成绩与转正）。这些子页现已合并进 all.vue 单页并按顺序堆叠，
  // 侧栏只保留一个入口，点进去就是那一页。
  //
  // 子项 path 必须是空串：SidebarItem 用 path.resolve(basePath, childPath) 拼链接，
  // resolve('/assessment/intern/learning', '') 正好回到路径本身；若写成相对段
  // （如 'courses'）会被拼成 /assessment/intern/learning/courses，虽然那条有重定向
  // 兜底，但 :index 与 $route.path 对不上，侧栏高亮会丢。
  const learning = [{
    path: '/assessment/intern/learning',
    component: Layout,
    meta: { title: '学习与考核', icon: 'education' },
    children: [{
      path: '',
      meta: { title: '学习与考核', icon: 'education' }
    }]
  }]

  return dashboard.concat(learning)
}

// 遍历后台传来的路由字符串，转换为组件对象
function filterAsyncRouter(asyncRouterMap, lastRouter = false, type = false) {
  return asyncRouterMap.filter(route => {
    if (type && route.children) {
      route.children = filterChildren(route.children)
    }
    if (route.component) {
      // Layout ParentView 组件特殊处理
      if (route.component === 'Layout') {
        route.component = Layout
      } else if (route.component === 'ParentView') {
        route.component = ParentView
      } else if (route.component === 'InnerLink') {
        route.component = InnerLink
      } else {
        route.component = loadView(route.component)
      }
    }
    if (route.children != null && route.children && route.children.length) {
      route.children = filterAsyncRouter(route.children, route, type)
    } else {
      delete route['children']
      delete route['redirect']
    }
    return true
  })
}

function filterChildren(childrenMap, lastRouter = false) {
  var children = []
  childrenMap.forEach((el, index) => {
    if (el.children && el.children.length) {
      if (el.component === 'ParentView' && !lastRouter) {
        el.children.forEach(c => {
          c.path = el.path + '/' + c.path
          if (c.children && c.children.length) {
            children = children.concat(filterChildren(c.children, c))
            return
          }
          children.push(c)
        })
        return
      }
    }
    if (lastRouter) {
      el.path = lastRouter.path + '/' + el.path
      if (el.children && el.children.length) {
        children = children.concat(filterChildren(el.children, el))
        return
      }
    }
    children = children.concat(el)
  })
  return children
}

// 动态路由遍历，验证是否具备权限
export function filterDynamicRoutes(routes) {
  const res = []
  routes.forEach(route => {
    if (route.permissions) {
      if (auth.hasPermiOr(route.permissions)) {
        res.push(route)
      }
    } else if (route.roles) {
      if (auth.hasRoleOr(route.roles)) {
        res.push(route)
      }
    }
  })
  return res
}

export const loadView = (view) => {
  if (process.env.NODE_ENV === 'development') {
    return (resolve) => require([`@/views/${view}`], resolve)
  } else {
    // 使用 import 实现生产环境的路由懒加载
    return () => import(`@/views/${view}`)
  }
}

export default permission
