import Vue from 'vue'
import Router from 'vue-router'

Vue.use(Router)

/* Layout */
import Layout from '@/layout'

/**
 * Note: 路由配置项
 *
 * hidden: true                     // 当设置 true 的时候该路由不会再侧边栏出现 如401，login等页面，或者如一些编辑页面/edit/1
 * alwaysShow: true                 // 当你一个路由下面的 children 声明的路由大于1个时，自动会变成嵌套的模式--如组件页面
 *                                  // 只有一个时，会将那个子路由当做根路由显示在侧边栏--如引导页面
 *                                  // 若你想不管路由下面的 children 声明的个数都显示你的根路由
 *                                  // 你可以设置 alwaysShow: true，这样它就会忽略之前定义的规则，一直显示根路由
 * redirect: noRedirect             // 当设置 noRedirect 的时候该路由在面包屑导航中不可被点击
 * name:'router-name'               // 设定路由的名字，一定要填写不然使用<keep-alive>时会出现各种问题
 * query: '{"id": 1, "name": "ry"}' // 访问路由的默认传递参数
 * roles: ['admin', 'common']       // 访问路由的角色权限
 * permissions: ['a:a:a', 'b:b:b']  // 访问路由的菜单权限
 * meta : {
    noCache: true                   // 如果设置为true，则不会被 <keep-alive> 缓存(默认 false)
    title: 'title'                  // 设置该路由在侧边栏和面包屑中展示的名字
    icon: 'svg-name'                // 设置该路由的图标，对应路径src/assets/icons/svg
    breadcrumb: false               // 如果设置为false，则不会在breadcrumb面包屑中显示
    activeMenu: '/system/user'      // 当路由设置了该属性，则会高亮相对应的侧边栏。
  }
 */

// 公共路由
export const constantRoutes = [
  {
    path: '/redirect',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '/redirect/:path(.*)',
        component: () => import('@/views/redirect')
      }
    ]
  },
  {
    path: '/login',
    component: () => import('@/views/login'),
    hidden: true
  },
  {
    path: '/register',
    component: () => import('@/views/register'),
    hidden: true
  },
  {
    path: '/404',
    component: () => import('@/views/error/404'),
    hidden: true
  },
  {
    path: '/401',
    component: () => import('@/views/error/401'),
    hidden: true
  },
  {
    path: '',
    component: Layout,
    redirect: 'index',
    children: [
      {
        path: 'index',
        component: () => import('@/views/index'),
        name: 'Index',
        meta: { title: '工作台', icon: 'dashboard', affix: true }
      }
    ]
  },
  {
    path: '/user',
    component: Layout,
    hidden: true,
    redirect: 'noredirect',
    children: [
      {
        path: 'profile',
        component: () => import('@/views/system/user/profile/index'),
        name: 'Profile',
        meta: { title: '个人中心', icon: 'user' }
      }
    ]
  },
  {
    path: '/messages',
    component: Layout,
    children: [
      {
        path: '',
        component: () => import('@/views/message/index'),
        name: 'MessageCenter',
        meta: { title: '消息中心', icon: 'message' }
      }
    ]
  }
]

// 动态路由，基于用户权限动态去加载
export const dynamicRoutes = [
  {
    path: '/assessment/intern',
    component: Layout,
    hidden: true,
    roles: ['PRE_TRAINEE', 'FORMAL_TRAINEE'],
    children: [
      {
        // 「学习与考核」单页（2026-09-23 由「页签壳 + 5 个子页」合并而来）
        //
        // 现状（2026-09-23 二次调整）：本页只堆叠「在线学习 → 考核入口卡栏 →
        // 考核成绩与转正」三段（顺序见 utils/internTabs.js）；备考资料 / 模拟考核 /
        // 正式考核已拉回独立页面，本页只保留横排入口卡。侧栏仍只留一个入口。
        path: 'learning',
        component: () => import('@/views/assessment/learning/all'),
        name: 'InternLearningAll',
        meta: { title: '学习与考核', activeMenu: '/assessment/intern/learning' }
      },
      // ---------------------------------------------------------------------
      // 路径兼容（2026-09-23 下午调整后）：
      //   · learning/guide                     → 独立页「备考资料」
      //   · learning/mock/{theory|practice}    → 独立页「模拟理论考核」「模拟实操考核」
      //   · learning/exam                      → **回到合并页分节**（正式考核在学习与考核页里自成栏）
      //   · learning/courses|result            → 重定向到合并页对应分节；
      //   · 用函数式 redirect 是为了**保留 query** —— 成绩页的 ?examId=（消息中心
      //     跳转）、模拟考核回顾页「再练一次」的 ?start=1[&examId=] 都依赖它。
      // ---------------------------------------------------------------------
      {
        path: 'learning/courses',
        redirect: to => ({ path: '/assessment/intern/learning', query: to.query, hash: '#sec-learning' })
      },
      {
        // 备考资料：2026-09-23 从合并页拉回为独立页（合并页只留入口卡）
        path: 'learning/guide',
        component: () => import('@/views/assessment/guide/index'),
        name: 'InternGuide',
        meta: { title: '备考资料', activeMenu: '/assessment/intern/learning' }
      },
      {
        // 旧「模拟考核」总入口 → 默认落到模拟理论考核（保留 query 供「再练一次」）
        path: 'learning/mock',
        redirect: to => ({ path: '/assessment/intern/learning/mock/theory', query: to.query })
      },
      {
        // 模拟理论考核：清单（列表）+ 自测 + 记录，2026-09-23 由原「模拟考核」拆出并去掉模块层级
        path: 'learning/mock/theory',
        component: () => import('@/views/assessment/practice/theory'),
        name: 'InternMockTheory',
        meta: { title: '模拟理论考核', activeMenu: '/assessment/intern/learning' }
      },
      {
        // 模拟实操考核：实操模拟题卡片，2026-09-23 由原「模拟考核 › 模块 › 实操题」拆出
        path: 'learning/mock/practice',
        component: () => import('@/views/assessment/practice/practical'),
        name: 'InternMockPractical',
        meta: { title: '模拟实操考核', activeMenu: '/assessment/intern/learning' }
      },
      {
        // 正式考核：2026-09-23 下午回到合并页当一栏（不再独立成页）
        path: 'learning/exam',
        redirect: to => ({ path: '/assessment/intern/learning', query: to.query, hash: '#sec-exam' })
      },
      {
        path: 'learning/result',
        redirect: to => ({ path: '/assessment/intern/learning', query: to.query, hash: '#sec-result' })
      },
      {
        // 下钻页：单场考核结果 ?exams=16,17 —— 只展示点击那一场
        // （一个场次可能含理论+实操两个环节）。它不是页签，保留为独立路由。
        path: 'learning/exam-result',
        component: () => import('@/views/assessment/exam/result'),
        name: 'InternExamResult',
        meta: { title: '单场考核结果', activeMenu: '/assessment/intern/learning' }
      },
      {
        path: 'learning/course/:courseId',
        component: () => import('@/views/assessment/learning/detail'),
        name: 'InternLearningCourse',
        meta: { title: '课程学习', activeMenu: '/assessment/intern/learning' }
      },
      {
        // 旧路径兼容：备考资料已拉回为独立页 /assessment/intern/learning/guide
        path: 'study-guide',
        redirect: to => ({ path: '/assessment/intern/learning/guide', query: to.query })
      },
      {
        // 旧路径兼容：顶层「参与考核」→ 学习与考核页的「正式考核」栏。
        // 工作台「参加 9 月正式考核 → 查看」等入口都指这里；正式考核 2026-09-23 已回到合并页当一栏。
        path: 'exam',
        redirect: to => ({ path: '/assessment/intern/learning', query: to.query, hash: '#sec-exam' })
      },
      {
        // 旧路径兼容：模拟考核总入口 → 模拟理论考核。
        // 用函数式 redirect 保留 query —— record.vue 的「再练一次」依赖 ?start=1 触发自动抽题。
        path: 'mock-exam',
        redirect: to => ({ path: '/assessment/intern/learning/mock/theory', query: to.query })
      },
      {
        path: 'mock-exam/record/:recordId(\\d+)',
        component: () => import('@/views/assessment/practice/record'),
        name: 'InternMockExamRecord',
        meta: { title: '模拟考核回顾', activeMenu: '/assessment/intern/learning' }
      },
      {
        path: 'practice-subject/:id(\\d+)',
        component: () => import('@/views/assessment/practice/subjectDetail'),
        name: 'InternPracticeSubjectDetail',
        meta: { title: '实操题详情', activeMenu: '/assessment/intern/learning' }
      },
      {
        // 能力画像详情（工作台「能力画像 · 详情」下钻，设计稿 i9）。
        // dynamicRoutes 先于后端菜单路由 addRoutes，会遮蔽 sys_menu 里同路径的骨架页菜单。
        path: 'portrait',
        component: () => import('@/views/assessment/portrait/index'),
        name: 'InternPortrait',
        meta: { title: '能力画像', activeMenu: '/index' }
      },
      {
        // 保密协议与签署凭证：工作台「保密协议」「签署凭证」两个入口的目标页。
        // 此前两处 pill 都指向该路径但从未注册 → 点进去是空白/404。
        path: 'agreements',
        component: () => import('@/views/assessment/agreement/index'),
        name: 'InternAgreement',
        meta: { title: '保密协议与签署凭证', activeMenu: '/index' }
      },
      {
        // 旧路径兼容：考核记录已并入「学习与考核」单页的 05 段
        path: 'scores',
        redirect: to => ({ path: '/assessment/intern/learning', query: to.query, hash: '#sec-result' })
      }
    ]
  },

  // ==========================================================================
  // ★ 2026-09-22 新增：实习生「正式考核 · 独立全屏作答页」
  //
  // 为什么单独一条**顶层路由、不挂 Layout**：作答时最怕被导航打断 —— 挂在 Layout 下就会有
  //   侧栏与页签条，误点即离开（旧实现把作答塞在列表页的 stage='exam' 里，就是这个毛病）。
  //   顶层不挂 Layout ⇒ 无侧栏、无页签条，整屏都是试卷。
  //
  // 进入方式：列表页点「开始考试」→ push 本路由，带 examId + from（回跳地址）。
  // 退出方式：交卷 / 放弃 → replace 回 from（默认回正式考核列表）。
  // 路由自带 examId ⇒ 刷新、直达、回退都能重新拉起（后端 startExam 会续答同一份答卷）。
  // ==========================================================================
  {
    path: '/assessment/intern/exam-answering',
    component: () => import('@/views/assessment/exam/answering'),
    hidden: true,
    roles: ['PRE_TRAINEE', 'FORMAL_TRAINEE'],
    meta: { title: '在线作答', activeMenu: '/assessment/intern/learning' }
  },

  // ==========================================================================
  // 课程预览（管理侧）
  //
  // 需求：课程管理里每条「符合发布要求且已保存 / 已发布」的课程，管理员可点「预览」，
  //       进去看到**和实习生一样的课程页面**。
  //
  // 为什么不复用实习生那条路由：`/assessment/intern` 整段带 `roles: ['PRE_TRAINEE','FORMAL_TRAINEE']`，
  // 管理员连路由都进不去。所以另起一条，用**权限**（business:course:query，超管与部门管理员都持有）
  // 管控可见性，并复用**同一个 detail.vue**（只是换成管理侧数据源），避免两套模板各自跑偏。
  //
  // 数据源：detail.vue 检测到 meta.preview 时改调
  //   GET /business/course/{id}          （课程本体）
  //   GET /business/course/{id}/contents （章节+资料，字段与实习生接口完全一致，只是进度为 null）
  // 且**不写任何学习进度**（管理员没有 study_record，也不该产生）。
  // ==========================================================================
  {
    path: '/course-preview',
    component: Layout,
    hidden: true,
    permissions: ['business:course:query'],
    children: [
      {
        path: 'course/:courseId',
        component: () => import('@/views/assessment/learning/detail'),
        name: 'CoursePreview',
        meta: { title: '课程预览', preview: true }
      }
    ]
  },

  // ==========================================================================
  // 部门管理员端（设计稿 §一：四目录 —— 工作台 / 人员管理 / 学习与考核管理 / 任务与通知）
  //
  // 与实习生端同一套思路：本段只声明「真实路由」，**侧栏分组**由
  // store/modules/permission.js 的 buildDeptAdminSidebar() 按四目录重排。
  // 之所以不在 router 里嵌套分组组件：分组纯属展示需求，路由保持扁平更好维护。
  //
  // 这里的 path 用了 'people/students' 这类两段式，是为了让侧栏分组节点
  // （/department/people）与其子项（students）拼出的链接与真实路由一致。
  // ==========================================================================
  {
    path: '/department',
    component: Layout,
    hidden: true,
    roles: ['DEPT_ADMIN'],
    children: [
      // ---------- ① 工作台 ----------
      {
        path: 'dashboard',
        component: () => import('@/views/department/dashboard/index'),
        name: 'DeptDashboard',
        meta: { title: '工作台', icon: 'dashboard', activeMenu: '/department/dashboard' }
      },

      // ---------- ② 人员管理 ----------
      {
        path: 'people/register-review',
        component: () => import('@/views/business/register/index'),
        name: 'DeptRegisterReview',
        meta: { title: '注册审核', icon: 'user', activeMenu: '/department/people/register-review' }
      },
      {
        path: 'people/students',
        component: () => import('@/views/department/people/students'),
        name: 'DeptStudents',
        meta: { title: '实习生管理', icon: 'peoples', activeMenu: '/department/people/students' }
      },
      {
        // 导师管理（2026-09-23 新增）：导师抽成独立 mentor 主表后，部门管理员在此维护
        // 本部门导师；「实习生管理」页分配导师时直接从这里选。
        // 与超管端共用同一页面组件（views/business/mentor/index.vue）。
        path: 'people/mentors',
        component: () => import('@/views/business/mentor/index'),
        name: 'DeptMentorManage',
        meta: { title: '导师管理', icon: 'user', activeMenu: '/department/people/mentors' }
      },
      {
        path: 'people/promotion',
        component: () => import('@/views/department/people/promotion'),
        name: 'DeptPromotion',
        meta: { title: '实习转正审核', icon: 'star', activeMenu: '/department/people/promotion' }
      },
      {
        // 个人全景档案（花名册下钻页）
        path: 'people/profile/:userId(\\d+)',
        component: () => import('@/views/department/people/profile'),
        name: 'DeptStudentProfile',
        hidden: true,
        meta: { title: '个人全景档案', activeMenu: '/department/people/students' }
      },

      // ---------- ③ 学习与考核管理 ----------
      {
        path: 'study/courses',
        component: () => import('@/views/business/course/runtime'),
        name: 'DeptCourse',
        meta: { title: '课程管理', icon: 'education', activeMenu: '/department/study/courses' }
      },
      {
        path: 'study/banks',
        component: () => import('@/views/business/questionBank/index'),
        name: 'DeptBank',
        meta: { title: '题库管理', icon: 'list', activeMenu: '/department/study/banks' }
      },
      {
        // ★ 2026-09-23：模拟实操题**直接在「模拟备考管理 › 模拟实操题」页签内维护**，不再单开页面。
        //   本路由保留并重定向（旧书签 / 旧链接不掉 404），落在页签上。
        path: 'study/psubjects',
        redirect: () => ({ path: '/department/study/prep', query: { tab: 'pbank' } })
      },
      {
        // ★ 2026-09-23：原「实操题库详情」已退场（实操题库概念取消）→ 重定向到模拟实操题页签。
        path: 'study/practice-bank-detail/:bankId',
        redirect: () => ({ path: '/department/study/prep', query: { tab: 'pbank' } })
      },
      {
        // ★ 2026-09-23：原「题库详情」已退场（题目直接按部门归属）→ 重定向到题库管理页。
        path: 'study/bank-detail/:bankId',
        redirect: () => ({ path: '/department/study/banks' })
      },
      {
        path: 'study/prep',
        component: () => import('@/views/department/study/prep/index'),
        name: 'DeptPrep',
        meta: { title: '模拟备考管理', icon: 'documentation', activeMenu: '/department/study/prep' }
      },
      {
        // 套卷配置（独立页）：从「模拟理论考核」列表点「配置」进来，不进侧栏。
        path: 'study/paper-config/:examId',
        component: () => import('@/views/department/study/prep/paperConfig'),
        name: 'DeptPaperConfig',
        hidden: true,
        meta: { title: '套卷配置', activeMenu: '/department/study/prep' }
      },
      {
        path: 'study/exam',
        component: () => import('@/views/business/exam/index'),
        name: 'DeptExam',
        meta: { title: '正式考核管理', icon: 'form', activeMenu: '/department/study/exam' }
      },
      {
        path: 'study/scores',
        component: () => import('@/views/department/study/scores/index'),
        name: 'DeptScores',
        meta: { title: '成绩管理', icon: 'chart', activeMenu: '/department/study/scores' }
      },

      // ---------- ④ 任务与通知 ----------
      {
        path: 'messages/tasks',
        component: () => import('@/views/department/messages/tasks'),
        name: 'DeptTasks',
        meta: { title: '任务管理', icon: 'job', activeMenu: '/department/messages/tasks' }
      },
      {
        // 任务批阅工作台：完成情况统计 + 内联批阅 + 资料 + 讨论（原来散在「任务管理」的弹窗里）
        path: 'messages/review',
        component: () => import('@/views/department/messages/review'),
        name: 'DeptTaskReview',
        meta: { title: '任务批阅', icon: 'edit', activeMenu: '/department/messages/review' }
      },
      {
        path: 'messages/notices',
        component: () => import('@/views/department/messages/notices'),
        name: 'DeptNotices',
        meta: { title: '通知管理', icon: 'message', activeMenu: '/department/messages/notices' }
      }
    ]
  },

  // ==========================================================================
  // 超管端（设计稿 V2 五目录：全局工作台 / 组织与人员 / 培养运营 / 规则与配置 / 审计与合规）
  //
  // 与实习生端、部门端同一套思路：本段只声明「真实路由」，**侧栏分组**由
  // store/modules/permission.js 的 buildSuperSidebar() 按设计稿五目录重排；
  // 「系统管理 / 系统监控 / 系统工具」三项继续复用平台原生菜单（由 DB 菜单提供）。
  // roles 同时匹配内置 admin（role_key='admin'）与业务超管角色 SUPER_ADMIN。
  // ==========================================================================
  {
    path: '/super',
    component: Layout,
    hidden: true,
    roles: ['SUPER_ADMIN', 'admin'],
    children: [
      {
        // ① 全局工作台（单项目录，侧栏会自动折叠成一级链接）
        path: 'dashboard',
        component: () => import('@/views/super/dashboard/index'),
        name: 'SuperDashboard',
        meta: { title: '全局工作台', icon: 'dashboard', activeMenu: '/super/dashboard' }
      },
      // ② 组织与人员
      {
        path: 'org/organization',
        component: () => import('@/views/super/org/organization/index'),
        name: 'SuperOrganization',
        meta: { title: '组织与岗位管理', icon: 'tree', activeMenu: '/super/org/organization' }
      },
      {
        path: 'org/roles',
        component: () => import('@/views/super/org/roles/index'),
        name: 'SuperRoles',
        meta: { title: '角色权限', icon: 'lock', activeMenu: '/super/org/roles' }
      },
      {
        // 人员与账号信息管理（2026-09-20 由超管自建，替换原先直接复用 `views/system/user/index` 的做法）：
        // 页签一 人员与账号（sys_user 全量，增删改 + 重置密码）、页签二 注册申请（通过 / 驳回）。
        // 原生「系统管理 › 用户管理」菜单未动，仍可单独使用。
        path: 'org/accounts',
        component: () => import('@/views/super/org/accounts/index'),
        name: 'SuperAccounts',
        meta: { title: '人员与账号', icon: 'user', activeMenu: '/super/org/accounts' }
      },
      {
        // 导师管理（2026-09-23 新增）：导师从 sys_user 的自由文本抽成独立 mentor 主表，
        // 本页维护导师库；实习生侧在「实习生管理」页从此库直接选。
        // 与部门端共用同一个页面组件（views/business/mentor/index.vue），
        // 范围由后端区分：超管不限部门，部门管理员只能动本部门。
        path: 'org/mentor',
        component: () => import('@/views/business/mentor/index'),
        name: 'SuperOrgMentor',
        meta: { title: '导师管理', icon: 'peoples', activeMenu: '/super/org/mentor' }
      },
      {
        // 2026-09-20：本页内容（各部门管理员的待办量 + 催办）与「督办看板」**同源**
        //（页面自己也写着"两个页面的数字必然一致"），已并入 /super/todo 的「按人」视角。
        // 这里保留路由并重定向 —— 与之前 org/positions 的处理一致，旧书签不会 404。
        //
        // ⚠️ 必须用**函数式** redirect：字符串形式会把 `?view=people` 整串当成 path
        //（vue-router 3 不会把 path 里的 query 拆出来），匹配不到就掉进 /404。
        path: 'org/dept-admins',
        name: 'SuperDeptAdmins',
        redirect: () => ({ path: '/super/todo', query: { view: 'people' } })
      },
      {
        // 超管维护业务岗位（position 表）：复用部门端的岗位管理页。
        // 部门本身用 RuoYi 原生「系统管理 › 部门管理」（超管已有 system:dept:* 全套权限）；
        // 两者的**绑定关系**在「组织岗位」页维护。
        path: 'org/positions',
        component: () => import('@/views/business/position/index'),
        name: 'SuperPositions',
        meta: { title: '岗位管理', icon: 'tree', activeMenu: '/super/org/positions' }
      },
      // ③ 培养运营（全局只读）
      {
        // 培养分析看板 L0（2026-09-20 新增）：部门横向对比 + 培养状态进度 + 预警。
        // 只读聚合，后端 /business/super/analysis/*；下钻页紧跟其后两条。
        path: 'ops/analysis',
        component: () => import('@/views/super/ops/analysis/overview'),
        name: 'SuperOpsAnalysis',
        meta: { title: '培养分析看板', icon: 'chart', activeMenu: '/super/ops/analysis' }
      },
      {
        // L1 部门详情（下钻第一层）。不进侧栏：数量随部门增长，activeMenu 恒指父页。
        path: 'ops/analysis/dept/:deptId',
        component: () => import('@/views/super/ops/analysis/dept'),
        name: 'SuperOpsAnalysisDept',
        hidden: true,
        meta: { title: '部门详情', activeMenu: '/super/ops/analysis' }
      },
      {
        // L3 个人档案（下钻到个人）。不进侧栏，同上。
        path: 'ops/analysis/intern/:userId',
        component: () => import('@/views/super/ops/analysis/intern'),
        name: 'SuperOpsAnalysisIntern',
        hidden: true,
        meta: { title: '个人档案', activeMenu: '/super/ops/analysis' }
      },
      {
        path: 'ops/courses',
        component: () => import('@/views/super/ops/courses/index'),
        name: 'SuperOpsCourses',
        meta: { title: '课程与题库总览', icon: 'reading', activeMenu: '/super/ops/courses' }
      },
      {
        // 超管「可写」课程（2026-09-20 规则调整）：复用部门端的**运行时**页面
        // （`course/runtime` 才接了真实接口；`course/index` 是含 previewMode 的基类/演示版）
        path: 'ops/course-admin',
        component: () => import('@/views/business/course/runtime'),
        name: 'SuperCourseAdmin',
        meta: { title: '课程管理', icon: 'edit', activeMenu: '/super/ops/course-admin' }
      },
      {
        // 超管「可写」题库：同理复用部门端的题库管理页
        path: 'ops/bank-admin',
        component: () => import('@/views/business/questionBank/index'),
        name: 'SuperBankAdmin',
        meta: { title: '题库管理', icon: 'collection', activeMenu: '/super/ops/bank-admin' }
      },
      {
        // ★ 2026-09-23：原「题库详情」已退场（题目直接按部门归属）→ 重定向到题库管理页。
        path: 'ops/bank-detail/:bankId',
        redirect: () => ({ path: '/super/ops/bank-admin' })
      },
      {
        // ★ 2026-09-23：模拟实操题**直接在「模拟备考管理 › 模拟实操题」页签内维护**，不再单开页面。
        //   必须指到 /super 前缀（超管访问 /department/** 会掉 404）。
        path: 'ops/psubjects',
        redirect: () => ({ path: '/super/ops/prep', query: { tab: 'pbank' } })
      },
      {
        // ★ 2026-09-23：原「实操题库详情」已退场（实操题库概念取消）→ 重定向到模拟实操题页签。
        path: 'ops/practice-bank-detail/:bankId',
        redirect: () => ({ path: '/super/ops/prep', query: { tab: 'pbank' } })
      },
      {
        // ★ 2026-09-22：独立「实操题库」页已下线；★ 2026-09-23 起实操题库概念整体退场，
        //   维护入口回到「模拟备考管理 › 模拟实操题」页签。路由**保留并重定向**，避免旧书签掉 404。
        path: 'ops/psubject-admin',
        redirect: () => ({ path: '/super/ops/prep', query: { tab: 'pbank' } })
      },
      {
        // 「考核与成绩」L0：总览（两个视角）+ KPI，下钻到 /exam-config/{id}
        path: 'ops/exams',
        component: () => import('@/views/super/ops/exams/index'),
        name: 'SuperOpsExams',
        meta: { title: '考核与成绩', icon: 'date', activeMenu: '/super/ops/exams' }
      },
      {
        // 模拟备考管理：**复用部门端同一页面**（该页已有超管适配 —— isSuperAdmin 会加载部门列表，
        // 且 listExam 不传 deptId ⇒ 超管天然拿全量），所以无需另写一套。
        // 补这个入口的原因：超管访问部门端 /department/study/prep 会 404（超管只有 DB 菜单树里的路径）。
        path: 'ops/prep',
        component: () => import('@/views/department/study/prep/index'),
        name: 'SuperOpsPrep',
        meta: { title: '模拟备考管理', icon: 'education', activeMenu: '/super/ops/prep' }
      },
      {
        // 套卷配置（独立页，不进侧栏）：超管走 /super 前缀，
        // 与部门端的 /department/study/paper-config 各自独立，避免互相跳成 404
        path: 'ops/paper-config/:examId',
        component: () => import('@/views/department/study/prep/paperConfig'),
        name: 'SuperPaperConfig',
        hidden: true,
        meta: { title: '套卷配置', activeMenu: '/super/ops/prep' }
      },
      {
        // 已下线（2026-09-22）：成绩与统计分析并入「考核与成绩」（ops/exams），侧栏不再单列。
        // 保留路由做 redirect 兜底，避免旧书签 / 收藏 404（与 org/dept-admins 同一做法）。
        path: 'ops/scores',
        redirect: () => ({ path: '/super/ops/exams' })
      },
      // ④ 任务与通知（单项目录；三端命名统一 —— 决策 6）
      {
        path: 'notify',
        component: () => import('@/views/super/notify/index'),
        name: 'SuperNotify',
        meta: { title: '通知中心', icon: 'message', activeMenu: '/super/notify' }
      },
      {
        // 督办看板：四源待办按责任人归集 + 批量催办（超管督办视图）
        path: 'todo',
        component: () => import('@/views/super/todo/index'),
        name: 'SuperTodo',
        meta: { title: '督办看板', icon: 'checkbox', activeMenu: '/super/todo' }
      },
      // ⑤ 规则与配置（单项目录）
      {
        path: 'rule',
        component: () => import('@/views/super/rule/index'),
        name: 'SuperRule',
        meta: { title: '规则与配置', icon: 'edit', activeMenu: '/super/rule' }
      },
      // ⑤ 审计与合规（单项目录）
      {
        path: 'audit',
        component: () => import('@/views/super/audit/index'),
        name: 'SuperAudit',
        meta: { title: '审计与合规', icon: 'documentation', activeMenu: '/super/audit' }
      }
    ]
  },

  {
    path: '/system/user-auth',
    component: Layout,
    hidden: true,
    permissions: ['system:user:edit'],
    children: [
      {
        path: 'role/:userId(\\d+)',
        component: () => import('@/views/system/user/authRole'),
        name: 'AuthRole',
        meta: { title: '分配角色', activeMenu: '/system/user' }
      }
    ]
  },
  {
    path: '/system/role-auth',
    component: Layout,
    hidden: true,
    permissions: ['system:role:edit'],
    children: [
      {
        path: 'user/:roleId(\\d+)',
        component: () => import('@/views/system/role/authUser'),
        name: 'AuthUser',
        meta: { title: '分配用户', activeMenu: '/system/role' }
      }
    ]
  },
  {
    path: '/system/dict-data',
    component: Layout,
    hidden: true,
    permissions: ['system:dict:list'],
    children: [
      {
        path: 'index/:dictId(\\d+)',
        component: () => import('@/views/system/dict/data'),
        name: 'Data',
        meta: { title: '字典数据', activeMenu: '/system/dict' }
      }
    ]
  },
  {
    path: '/monitor/job-log',
    component: Layout,
    hidden: true,
    permissions: ['monitor:job:list'],
    children: [
      {
        path: 'index/:jobId(\\d+)',
        component: () => import('@/views/monitor/job/log'),
        name: 'JobLog',
        meta: { title: '调度日志', activeMenu: '/monitor/job' }
      }
    ]
  },
  {
    path: '/tool/gen-edit',
    component: Layout,
    hidden: true,
    permissions: ['tool:gen:edit'],
    children: [
      {
        path: 'index/:tableId(\\d+)',
        component: () => import('@/views/tool/gen/editTable'),
        name: 'GenEdit',
        meta: { title: '修改生成配置', activeMenu: '/tool/gen' }
      }
    ]
  },

  // ============== 业务模块路由 ==============
  {
    path: '/business',
    component: Layout,
    redirect: '/business/position',
    name: 'Business',
    meta: { title: '业务管理', icon: 'education' },
    children: [
      {
        path: 'position',
        name: 'BusinessPosition',
        component: () => import('@/views/business/position/index'),
        meta: { title: '岗位管理', icon: 'peoples' },
        permissions: ['business:position:list']
      },
      {
        path: 'course',
        name: 'BusinessCourse',
        component: () => import('@/views/business/course/runtime'),
        meta: { title: '课程管理', icon: 'guide', activeMenu: '/assessment/department/courses' },
        permissions: ['business:course:list']
      },
      {
        path: 'register-review',
        name: 'BusinessRegister',
        component: () => import('@/views/business/register/index'),
        meta: { title: '注册审核', icon: 'user', activeMenu: '/assessment/department/register-review' },
        permissions: ['business:register:list']
      },
      {
        path: 'exam',
        name: 'BusinessExam',
        component: () => import('@/views/business/exam/index'),
        meta: { title: '考核管理', icon: 'list' },
        permissions: ['business:bank:list']
      },
    ]
  },
  {
    // 考核配置（独立页）：与列表分离；部门端/超管端两处列表都跳这里
    // 说明：不按路径前缀猜地址 —— 超管的列表在 DB 菜单树的 /assessment/department/exam，
    //       部门端在 dynamicRoutes 的 /department/study/exam，故统一用顶层路由 + ?from= 回跳。
    path: '/exam-config',
    component: Layout,
    hidden: true,
    permissions: ['business:bank:list'],
    children: [
      {
        path: ':examId(\\d+)',
        component: () => import('@/views/business/exam/config'),
        name: 'ExamConfig',
        meta: { title: '考核配置', activeMenu: '/assessment/department/exam' }
      }
    ]
  },
  {
    // 「考核与成绩」L1 单场详情（顶层 hidden；带 ?from= 回跳，activeMenu 指回 L0）
    path: '/exam-session',
    component: Layout,
    hidden: true,
    permissions: ['business:bank:list'],
    children: [
      {
        path: ':examId(\\d+)',
        component: () => import('@/views/super/ops/exams/session'),
        name: 'SuperExamSession',
        meta: { title: '考核详情', activeMenu: '/super/ops/exams' }
      }
    ]
  },
  {
    // 「考核与成绩」L2 单份答卷详情（顶层 hidden；?examId= 用于取同场样本做位次对照）
    path: '/exam-sheet',
    component: Layout,
    hidden: true,
    permissions: ['business:bank:list'],
    children: [
      {
        path: ':sheetId(\\d+)',
        component: () => import('@/views/super/ops/exams/sheet'),
        name: 'SuperExamSheet',
        meta: { title: '答卷明细', activeMenu: '/super/ops/exams' }
      }
    ]
  },
  {
    path: '/assessment/department/exam/grading',
    component: Layout,
    hidden: true,
    permissions: ['business:bank:list'],
    children: [
      {
        path: ':examId',
        component: () => import('@/views/business/exam/grading'),
        name: 'BusinessExamGrading',
        meta: { title: '答卷批改', activeMenu: '/assessment/department/exam' }
      }
    ]
  }
]

// 防止连续点击多次路由报错
let routerPush = Router.prototype.push;
let routerReplace = Router.prototype.replace;
// push
Router.prototype.push = function push(location) {
  return routerPush.call(this, location).catch(err => err)
}
// replace
Router.prototype.replace = function push(location) {
  return routerReplace.call(this, location).catch(err => err)
}

export default new Router({
  mode: 'history', // 去掉url中的#
  scrollBehavior: () => ({ y: 0 }),
  routes: constantRoutes
})
