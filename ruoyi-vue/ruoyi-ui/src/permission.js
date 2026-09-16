import router from './router'
import store from './store'
import { Message } from 'element-ui'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'
import { isRelogin } from '@/utils/request'

NProgress.configure({ showSpinner: false })

const whiteList = ['/login', '/register']

const formalInternBlockedPaths = [
  '/assessment/intern/practice',
  '/assessment/intern/study-guide',
  '/assessment/intern/theory-exam',
  // 实操练习为独立页面（含详情页），需单独声明；
  // 其前缀 /assessment/intern/practice 亦已覆盖，此处显式列出避免后续调整时漏拦。
  '/assessment/intern/practice-subject'
]

function isIntern() {
  return store.getters.roles.indexOf('PRE_TRAINEE') > -1 || store.getters.roles.indexOf('FORMAL_TRAINEE') > -1
}

function protectedTarget(to) {
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
