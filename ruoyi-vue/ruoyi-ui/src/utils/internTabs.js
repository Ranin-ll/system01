/**
 * 实习生端「学习与考核」页分节定义
 *
 * 演进：
 *  1. 最初是页签壳（learning/shell.vue）+ 5 个页签子页
 *     （在线学习 / 备考资料 / 模拟考核 / 正式考核 / 考核成绩与转正），侧栏按 5 个页签摊成二级菜单；
 *  2. 2026-09-23 上午：取消页签与子路由，5 段内容合并进 all.vue 单页堆叠；侧栏只留一个入口；
 *  3. 2026-09-23 中午：备考资料 / 模拟考核 / 正式考核拉回独立页，本页只放一条横排入口卡栏；
 *  4. 2026-09-23 下午（当前）：**正式考核回到本页自成栏**；入口卡栏改为 3 张
 *     —— 备考资料 / 模拟理论考核 / 模拟实操考核（原来那一个「模拟考核」已拆成两页，
 *     并去掉「模块」层级，进去直接看各套考核）。
 *
 * 本文件是**唯一数据源**：合并页（views/assessment/learning/all.vue）按这里的顺序
 * 渲染各段，anchor 供 #hash 定位（如 /learning/exam → ...#sec-exam）。
 *
 * 角色差异：正式实习生已完成转正，「备考资料 / 模拟理论考核 / 模拟实操考核」三个入口全部关闭，
 * 因此**整条入口卡栏对正式实习生不渲染**（表单里 FORMAL_INTERN_SECTION_KEYS 不含 entries）；
 * 正式考核本身仍在页内 —— 转正后照样要参加。路由层另有 permission.js 的
 * formalInternBlockedPaths 兜底直连。
 */

export const INTERN_SECTIONS = [
  {
    key: 'learning',
    name: 'InternLearning',
    label: '在线学习',
    anchor: 'sec-learning',
    component: () => import('@/views/assessment/learning/index')
  },
  {
    // 横排入口卡栏：备考资料 / 模拟理论考核 / 模拟实操考核（三段内容均为独立页）
    key: 'entries',
    name: 'InternAssessEntries',
    label: '考核入口',
    anchor: 'sec-entries',
    component: () => import('@/views/assessment/learning/entries')
  },
  {
    // 正式考核：2026-09-23 下午从独立页回到本页当一栏（转正前后都要参加，不适合外置）
    key: 'exam',
    name: 'InternExam',
    label: '正式考核',
    anchor: 'sec-exam',
    component: () => import('@/views/assessment/exam/index')
  },
  {
    key: 'result',
    name: 'InternResult',
    label: '考核成绩与转正',
    anchor: 'sec-result',
    component: () => import('@/views/assessment/result/index')
  }
]

// 正式实习生可见的分节：不含「考核入口」（那 3 个入口对正式实习生全部关闭）
export const FORMAL_INTERN_SECTION_KEYS = ['learning', 'exam', 'result']

export function isFormalIntern(roles = []) {
  return roles.indexOf('FORMAL_TRAINEE') > -1 && roles.indexOf('PRE_TRAINEE') === -1
}

/**
 * 按角色返回可见分节，并**重排编号**——若将来某段对某些角色收起，
 * 编号必须连续（01 / 02 / 03），不能出现 01 / 03。
 */
export function visibleInternSections(roles = []) {
  const allowed = isFormalIntern(roles)
    ? INTERN_SECTIONS.filter(section => FORMAL_INTERN_SECTION_KEYS.indexOf(section.key) > -1)
    : INTERN_SECTIONS.slice()

  return allowed.map((section, index) => Object.assign({}, section, {
    order: index < 9 ? '0' + (index + 1) : String(index + 1)
  }))
}
