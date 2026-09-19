<template>
  <div class="intern-shell">
    <header class="shell-heading">
      <div>
        <span class="eyebrow">LEARNING &amp; ASSESSMENT</span>
        <h1>学习与考核</h1>
        <p>按顺序完成在线学习、备考、模拟自测与正式考核，考核成绩与转正在最后汇总。</p>
      </div>
    </header>

    <div class="shell-tabbar">
      <el-tabs v-model="activeTab" @tab-click="onTabClick">
        <el-tab-pane v-for="tab in tabs" :key="tab.name" :label="tab.label" :name="tab.name" />
      </el-tabs>
    </div>

    <section class="shell-body">
      <router-view />
    </section>
  </div>
</template>

<script>
import { visibleInternTabs } from '@/utils/internTabs'

/**
 * 「学习与考核」页签壳
 *
 * 只负责两件事：① 顶部标题 + 页签条；② 用 <router-view> 承载五个页签页。
 * 页签内容页（在线学习 / 备考资料 / 模拟考核 / 正式考核 / 考核成绩与转正）
 * 均为独立文件、自带页头，本壳不改动它们的任何数据逻辑。
 *
 * 页签高亮依据 route.name；下钻页（课程学习、模拟回顾、实操题详情）通过
 * meta.tab 指回所属页签，保证高亮不丢。
 */
export default {
  name: 'InternLearningShell',
  computed: {
    // 页签清单来自 utils/internTabs.js（与侧栏同源）；
    // 正式实习生已完成转正，备考资料与模拟考核对其不再显示。
    tabs() {
      return visibleInternTabs(this.$store.getters.roles || [])
    },
    activeTab: {
      get() {
        return this.$route.meta.tab || this.$route.name
      },
      set() {
        // 高亮由路由驱动，这里不需要本地状态
      }
    }
  },
  methods: {
    onTabClick(tab) {
      const target = this.tabs.find(t => t.name === tab.name)
      if (!target) return
      if (this.$route.path === target.path) return
      this.$router.push(target.path)
    }
  }
}
</script>

<style lang="scss" scoped>
.intern-shell { min-height: 100%; padding: 24px 26px 0; color: #283544; background: #f5f7fa; }
.shell-heading { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 16px; }
.eyebrow { color: #2878c7; font-size: 11px; letter-spacing: .08em; }
.shell-heading h1 { margin: 5px 0 7px; color: #1d2939; font-size: 28px; font-weight: 600; }
.shell-heading p { margin: 0; color: #667085; font-size: 13px; }
.shell-tabbar { padding: 0 18px; background: #fff; box-shadow: 0 1px 3px rgba(16, 24, 40, .04); }
.shell-tabbar .el-tabs__header { margin: 0; }
.shell-tabbar .el-tabs__item { height: 46px; color: #667085; font-size: 14px; line-height: 46px; }
.shell-tabbar .el-tabs__item.is-active { color: #1764f5; font-weight: 500; }
.shell-tabbar .el-tabs__nav-wrap::after { height: 1px; background-color: #edf0f4; }
.shell-body { padding-top: 18px; }
.shell-body ::v-deep .learning-page,
.shell-body ::v-deep .exam-page,
.shell-body ::v-deep .practice-page { padding-top: 0; }
@media (max-width: 700px) {
  .intern-shell { padding: 16px 12px 0; }
  .shell-heading { align-items: flex-start; flex-direction: column; gap: 12px; }
  .shell-heading h1 { font-size: 24px; }
  .shell-tabbar { padding: 0 10px; }
}
</style>
