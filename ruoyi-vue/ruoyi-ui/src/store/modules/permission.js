import auth from '@/plugins/auth'
import router, { constantRoutes, dynamicRoutes } from '@/router'
import { getRouters } from '@/api/menu'
import Layout from '@/layout/index'
import ParentView from '@/components/ParentView'
import InnerLink from '@/layout/components/InnerLink'
import { visibleInternTabs } from '@/utils/internTabs'

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

  // 原生系统三项（系统管理 / 系统监控 / 系统工具）直接透传 DB 菜单
  const systemDirs = dbRoutes.filter(route =>
    ['system', 'monitor', 'tool'].indexOf(route.path) > -1)

  return [
    single('/super', 'dashboard'),
    group('/super/org', '组织与人员', 'peoples', ['org/organization', 'org/roles', 'org/accounts']),
    group('/super/ops', '培养运营', 'education', ['ops/courses', 'ops/exams', 'ops/scores']),
    // 任务与通知：三端命名统一（决策 6）。单链接 → /super/notify，子项 path 保持相对段 'notify'，
    // resolve('/super/notify', '/super/notify') 仍回到自身（见上方 single() 的说明）。
    single('/super/notify', 'notify'),
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
      ['people/register-review', 'people/students', 'people/promotion']),
    group('/department/study', '学习与考核管理', 'education',
      ['study/courses', 'study/banks', 'study/prep', 'study/exam', 'study/scores']),
    group('/department/messages', '任务与通知', 'message',
      ['messages/tasks', 'messages/review', 'messages/notices'])
  ].filter(Boolean))
}

// 实习生端一级目录（设计稿 V1.2，2026-09-17 起加第三项）：
//   ① 工作台  ② 学习与考核  ③ 任务与通知
//
// 为什么在 store 里手搓，而不直接用后端菜单树（/getRouters）：
// 后端菜单树是给管理端用的，「学习考核」目录下还挂着部门运营 / 考核认证管理 /
// 系统运营等实习生用不到的目录，直接透传会让实习生侧栏出现一堆无用层级。
// 这里显式声明三项目录，只影响侧栏渲染（sidebarRouters 仅用于展示），
// 真正参与路由匹配的仍是 dynamicRoutes + 后端菜单生成的 rewriteRoutes。
//
// 「消息中心」有**两个入口**：顶栏铃铛（未读红点，见 layout/components/Navbar.vue）
// 与侧栏一级「任务与通知」（复用 constantRoutes 的 `/messages` 路由，该页含
// 「通知 / 我的任务」两个页签）。侧栏文案叫「任务与通知」，页面标题仍是「消息中心」——
// 侧栏是入口，页面是容器。
function buildInternSidebar(roles = []) {
  // ① 工作台：沿用 constantRoutes 的首页路由（affix，刷新后仍能定位）
  const dashboard = constantRoutes.filter(route => !route.hidden && route.path === '')

  // ② 学习与考核：复用 dynamicRoutes 里的页签子树，按角色裁剪可见页签
  const internRoot = dynamicRoutes.find(route => route.path === '/assessment/intern')
  const shell = internRoot && internRoot.children.find(child => child.path === 'learning')
  const visibleNames = visibleInternTabs(roles).map(tab => tab.name)
  const children = (shell ? shell.children : [])
    .filter(child => !child.hidden && visibleNames.indexOf(child.name) > -1)
    .map(child => Object.assign({}, child))

  const learning = children.length
    ? [{
        path: '/assessment/intern/learning',
        component: Layout,
        alwaysShow: true,
        redirect: '/assessment/intern/learning/' + children[0].path,
        meta: { title: '学习与考核', icon: 'education' },
        children
      }]
    : []

  // ③ 任务与通知：单链接，复用 constantRoutes 的 /messages。
  //    子项 path 保持空串 —— SidebarItem 用 path.resolve(basePath, childPath) 拼链接，
  //    resolve('/messages', '') 正好回到 /messages；若写成相对段（如 'tasks'）会被拼成
  //    /messages/tasks 而 404（README：单项目录必须让子项解析回目录路径本身）。
  //    只覆盖 meta.title：侧栏显示「任务与通知」，页面内的标题仍由页面自己决定。
  const messageCenter = constantRoutes
    .filter(route => !route.hidden && route.path === '/messages')
    .map(route => Object.assign({}, route, {
      children: (route.children || []).map(child => Object.assign({}, child, {
        meta: Object.assign({}, child.meta, { title: '任务与通知' })
      }))
    }))

  return dashboard.concat(learning).concat(messageCenter)
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
