import router from './router'
import store from './store'
import { Message } from 'element-ui'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'
import { isRelogin } from '@/utils/request'
import { FORMAL_INTERN_BLOCKED_PATHS } from '@/utils/internTabs'

NProgress.configure({ showSpinner: false })

const whiteList = ['/login', '/register']

const formalInternBlockedPaths = [
  '/assessment/intern/practice',
  '/assessment/intern/study-guide',
  '/assessment/intern/theory-exam',
  // 实操题详情页（/assessment/intern/practice-subject/:id）已被上面的
  // /assessment/intern/practice 前缀覆盖，无需重复声明；
  // 原独立「实操题库」列表页（/assessment/intern/practice-subject）已删除。
  // 「学习与考核」页签化后，备考资料 / 模拟考核的新地址挪到了 /learning 之下，
  // 上面几条旧前缀已拦不到，必须单独列出（见 utils/internTabs.js）。
  ...FORMAL_INTERN_BLOCKED_PATHS
]

function isIntern() {
  return store.getters.roles.indexOf('PRE_TRAINEE') > -1 || store.getters.roles.indexOf('FORMAL_TRAINEE') > -1
}

/**
 * 部门管理员（非超管）的落点工作台。
 *
 * 背景：`/index` 是「通用工作台」页（实习生端 i2 设计 + 超管全局视图共用）。
 * 部门管理员已有按 d2 设计落地的独立工作台 `/department/dashboard`，
 * 因此登录后（`/` → `/index`）以及任何指向 `/index` 的跳转，统一改投部门工作台。
 * 超管（`SUPER_ADMIN` / `admin`）继续使用 `/index`，行为不变。
 */
export const DEPT_DASHBOARD_PATH = '/department/dashboard'

function isDeptAdminOnly() {
  const roles = store.getters.roles || []
  return roles.indexOf('DEPT_ADMIN') > -1
    && roles.indexOf('SUPER_ADMIN') === -1
    && roles.indexOf('admin') === -1
}

function protectedTarget(to) {
  // 部门管理员不再进旧的通用工作台，直接落到部门工作台
  if (isDeptAdminOnly() && (to.path === '/index' || to.path === '/')) {
    return DEPT_DASHBOARD_PATH
  }
  if (isIntern() && Number(store.getters.protocolStatus) !== 1 && to.path !== '/index') {
    return '/index'
  }
  if (store.getters.roles.indexOf('FORMAL_TRAINEE') > -1 && formalInternBlockedPaths.some(path => to.path.indexOf(path) === 0)) {
    Message.warning('你已转为正式实习生，备考、模拟考核和正式考核入口已关闭，历史记录仍可查看')
    return '/index'
  }
  return ''
}

router.beforeEach((to, from, next) => {
  NProgress.start()
  if (getToken()) {
    to.meta.title && store.dispatch('settings/setTitle', to.meta.title)
    /* has token*/
    if (to.path === '/login') {
      next({ path: '/' })
      NProgress.done()
    } else if (whiteList.indexOf(to.path) !== -1) {
      next()
    } else {
      if (store.getters.roles.length === 0) {
        isRelogin.show = true
        // 判断当前用户是否已拉取完user_info信息
        store.dispatch('GetInfo').then(() => {
          isRelogin.show = false
          store.dispatch('GenerateRoutes').then(accessRoutes => {
            // 根据roles权限生成可访问的路由表
            router.addRoutes(accessRoutes) // 动态添加可访问路由表
            const target = protectedTarget(to)
            next(target ? { path: target, replace: true } : { ...to, replace: true }) // hack方法 确保addRoutes已完成
          })
        }).catch(err => {
            store.dispatch('LogOut').then(() => {
              Message.error(err)
              next({ path: '/' })
            })
          })
      } else {
        const target = protectedTarget(to)
        target ? next({ path: target, replace: true }) : next()
      }
    }
  } else {
    // 没有token
    if (whiteList.indexOf(to.path) !== -1) {
      // 在免登录白名单，直接进入
      next()
    } else {
      next(`/login?redirect=${encodeURIComponent(to.fullPath)}`) // 否则全部重定向到登录页
      NProgress.done()
    }
  }
})

router.afterEach(() => {
  NProgress.done()
})
