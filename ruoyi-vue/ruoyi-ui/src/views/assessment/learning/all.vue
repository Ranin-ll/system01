<template>
  <div class="learning-all">
    <header class="all-heading">
      <div>
        <span class="eyebrow">LEARNING &amp; ASSESSMENT</span>
        <h1>学习与考核</h1>
        <p>在线学习、正式考核与考核成绩在本页展开；备考资料、模拟理论考核、模拟实操考核各自独立成页，从中间的入口卡进入。</p>
      </div>
    </header>

    <section
      v-for="section in sections"
      :id="section.anchor"
      :key="section.key"
      class="all-section"
    >
      <component :is="section.name" />
    </section>
  </div>
</template>

<script>
import { INTERN_SECTIONS, visibleInternSections } from '@/utils/internTabs'

// 把各分节的懒加载组件按 name 注册，模板里用 <component :is="section.name" />。
// 这些组件原本是 5 个独立页签页，现在只作为分节被本页堆叠渲染，
// 组件内部的数据逻辑一行没改（含各自的下钻跳转与倒计时）。
const sectionComponents = {}
INTERN_SECTIONS.forEach(section => {
  sectionComponents[section.name] = section.component
})

/**
 * 「学习与考核」单页（2026-09-23 由页签壳合并而来，当天又拆出若干独立页）
 *
 * 只负责三件事：① 页头；② 按 utils/internTabs.js 的顺序把各分节自上而下堆叠
 * （在线学习 → 考核入口卡栏 → 正式考核 → 考核成绩与转正）；③ 支持 #sec-* 锚点定位 ——
 * /learning/courses、/learning/result、/learning/exam 等仍重定向到本页并带上对应 hash，
 * 这样外部链接、通知跳转、跨页按钮都还能落到正确的那一段。
 *
 * 备考资料 / 模拟理论考核 / 模拟实操考核仍是独立页面（/learning/guide、/learning/mock/…），
 * 本页只在原来那几段的位置放一条横排入口卡栏（learning/entries.vue）；
 * 「正式考核」2026-09-23 下午从独立页回到本页当一栏（转正前后都要参加）。
 *
 * 各分节自带的「返回工作台 / 在线学习」面包屑已通过样式隐藏：合并后它既指不回
 * 独立页面，也与本页页头重复；分节自己的大标题与操作按钮保留。
 */
export default {
  // ⚠️ 不能叫 InternLearning：分节里的「在线学习」组件既注册成 components 的
  // InternLearning 键，它自己的 name 也叫 InternLearning。本页若同名，
  // <component :is="'InternLearning'"> 会解析成自己 → 无限递归
  // （表现为整页白屏 + RangeError: Maximum call stack size exceeded）。
  name: 'InternLearningAll',
  components: sectionComponents,
  data() {
    return {
      hashTimers: [],
      userScrolled: false
    }
  },
  computed: {
    // 分节清单与编号来自 utils/internTabs.js（正式实习生只见 3 段）
    sections() {
      return visibleInternSections(this.$store.getters.roles || [])
    }
  },
  watch: {
    // 从本页内部（或外部重定向）跳到同路径不同锚点时，组件实例会被复用，
    // $route.hash 变了但不会重新 created —— 必须监听 hash 才能滚过去。
    '$route.hash'() {
      this.scrollToHash()
    }
  },
  mounted() {
    this.bindUserScrollCancel()
    this.scrollToHash()
  },
  beforeDestroy() {
    this.clearHashTimers()
    this.unbindUserScrollCancel()
  },
  methods: {
    clearHashTimers() {
      this.hashTimers.forEach(timer => clearTimeout(timer))
      this.hashTimers = []
    },
    /** 用户自己动手滚了就放弃后续纠偏，别把人硬拽回去 */
    onUserScrollCancel() {
      this.userScrolled = true
      this.clearHashTimers()
    },
    bindUserScrollCancel() {
      ;['wheel', 'touchstart', 'keydown'].forEach(event =>
        window.addEventListener(event, this.onUserScrollCancel, { passive: true }))
    },
    unbindUserScrollCancel() {
      ;['wheel', 'touchstart', 'keydown'].forEach(event =>
        window.removeEventListener(event, this.onUserScrollCancel))
    },
    /**
     * 滚到 $route.hash 指定的分节。
     *
     * 为什么要分多次：5 段内容都是异步拉数据的，挂载瞬间各段还是空壳、
     * 页面总高不足一屏，此时 scrollTo 会被浏览器钳到 0（表现为"锚点没生效"）；
     * 等数据回来、页面长高之后，目标位置还会被上方的分节往下推。
     * 所以按几个时间点纠偏，直到用户自己滚动为止。
     */
    scrollToHash() {
      const hash = String(this.$route.hash || '').replace(/^#/, '')
      this.clearHashTimers()
      this.userScrolled = false
      if (!hash) return
      ;[0, 120, 320, 700, 1200, 2000].forEach(delay => {
        this.hashTimers.push(setTimeout(() => {
          if (this.userScrolled) return
          if (String(this.$route.hash || '').replace(/^#/, '') !== hash) return
          const target = document.getElementById(hash)
          if (!target) return
          window.scrollTo({ top: Math.max(target.getBoundingClientRect().top + window.scrollY - 12, 0) })
        }, delay))
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.learning-all { min-height: 100%; padding: 24px 26px 8px; color: #283544; background: #f5f7fa; }
.all-heading { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 16px; }
.eyebrow { color: #2878c7; font-size: 11px; letter-spacing: .08em; }
.all-heading h1 { margin: 5px 0 7px; color: #1d2939; font-size: 28px; font-weight: 600; }
.all-heading p { margin: 0; color: #667085; font-size: 13px; }

/* 每段之间用一条细分隔线断开，锚点定位时不会贴到视口最顶端 */
.all-section { padding-top: 6px; border-top: 1px solid #e7ecf3; }
.all-section + .all-section { margin-top: 26px; padding-top: 22px; }
.all-section::before { display: block; }

/* 各分节原本是独立页面，自带页头与内边距；合并后只收敛掉重复的**顶部**留白
   （备考资料 / 模拟理论考核 / 模拟实操考核仍是独立页，不在这里渲染）。
   左右内边距保持各分节原样 —— 那是它们自己的版心，本页不统一改。
   中间「考核入口」栏要与左右两栏的版心对齐，由 entries.vue 自己加等量内边距。 */
.all-section ::v-deep .learning-page,
.all-section ::v-deep .exam-page,
.all-section ::v-deep .result-page { padding-top: 0; }

/* 隐藏这几段的「返回工作台 / xxx」面包屑：它们是独立页时期的导航壳，
   合并后既指不回独立页，也与本页页头重复。
   （独立页上的面包屑照常保留 —— 在那里它是真实导航。） */
.all-section ::v-deep .learning-breadcrumb,
.all-section ::v-deep .exam-breadcrumb,
.all-section ::v-deep .result-breadcrumb { display: none; }

@media (max-width: 700px) {
  .learning-all { padding: 16px 12px 8px; }
  .all-heading { align-items: flex-start; flex-direction: column; gap: 12px; }
  .all-heading h1 { font-size: 24px; }
}
</style>
