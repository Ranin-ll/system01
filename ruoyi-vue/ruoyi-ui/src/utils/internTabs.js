/**
 * 实习生端「学习与考核」页签定义（设计稿 V1.2）
 *
 * 单一数据源：左侧栏（store/modules/permission.js）与页签壳
 * （views/assessment/learning/shell.vue）都从这里取，避免两处各写一份走偏。
 *
 * 角色差异：正式实习生已完成转正，「备考资料」与「模拟考核」对其关闭，
 * 仅保留「在线学习 / 正式考核 / 考核成绩与转正申请」，与 router/permission.js 的
 * formalInternBlockedPaths 拦截保持一致。
 */
export const INTERN_TABS = [
  { name: 'InternLearning', label: '在线学习', path: '/assessment/intern/learning/courses' },
  { name: 'InternGuide', label: '备考资料', path: '/assessment/intern/learning/guide' },
  { name: 'InternMockExam', label: '模拟考核', path: '/assessment/intern/learning/mock' },
  { name: 'InternLearningExam', label: '正式考核', path: '/assessment/intern/learning/exam' },
  { name: 'InternResult', label: '考核成绩与转正申请', path: '/assessment/intern/learning/result' }
]

// 正式实习生可见的页签
export const FORMAL_INTERN_TAB_NAMES = ['InternLearning', 'InternLearningExam', 'InternResult']

// 正式实习生被关闭的路径（备考资料 / 模拟考核的新地址）
export const FORMAL_INTERN_BLOCKED_PATHS = [
  '/assessment/intern/learning/guide',
  '/assessment/intern/learning/mock'
]

export function isFormalIntern(roles = []) {
  return roles.indexOf('FORMAL_TRAINEE') > -1 && roles.indexOf('PRE_TRAINEE') === -1
}

/** 按角色返回可见页签 */
export function visibleInternTabs(roles = []) {
  if (!isFormalIntern(roles)) {
    return INTERN_TABS.slice()
  }
  return INTERN_TABS.filter(tab => FORMAL_INTERN_TAB_NAMES.indexOf(tab.name) > -1)
}
