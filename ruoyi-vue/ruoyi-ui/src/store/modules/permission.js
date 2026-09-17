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
            : (isDeptAdminRole(rootGetters.roles) && !rootGetters.roles.includes('SUPER_ADMIN')
              ? buildDeptAdminSidebar()
              : constantRoutes.concat(sidebarRoutes))
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

  // 工作台：单项顶层目录（只有一个子项，RuoYi 侧栏会自动折叠成一级链接）
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
      ['messages/tasks', 'messages/notices'])
  ].filter(Boolean))
}

// 实习生端一级目录（设计稿 V1.2）：只保留「工作台」与「学习与考核」两项。
//
// 为什么在 store 里手搓，而不直接用后端菜单树（/getRouters）：
// 后端菜单树是给管理端用的，「学习考核」目录下还挂着部门运营 / 考核认证管理 /
// 系统运营等实习生用不到的目录，直接透传会让实习生侧栏出现一堆无用层级。
// 这里显式声明两项目录，只影响侧栏渲染（sidebarRouters 仅用于展示），
// 真正参与路由匹配的仍是 dynamicRoutes + 后端菜单生成的 rewriteRoutes。
//
// 「消息中心」不再占一级目录，改由顶栏铃铛承载（见 layout/components/Navbar.vue）。
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
