<template>
  <div class="entries-page">
    <div class="entries-heading">
      <div>
        <span class="eyebrow">ASSESSMENT ENTRIES</span>
        <h1>考核入口</h1>
        <p>备考资料、模拟理论考核、模拟实操考核各自有独立页面，从这里进入。</p>
      </div>
    </div>

    <!-- 横排入口卡栏：三张卡并排 -->
    <div class="entry-bar">
      <button
        v-for="item in entries"
        :key="item.key"
        type="button"
        class="entry-card"
        :class="item.tone"
        @click="go(item.path)"
      >
        <span class="entry-icon"><i :class="item.icon" /></span>
        <span class="entry-body">
          <b>{{ item.title }}</b>
          <span>{{ item.desc }}</span>
        </span>
        <span class="entry-go">进入<i class="el-icon-arrow-right" /></span>
      </button>
    </div>
  </div>
</template>

<script>
/**
 * 「学习与考核」页中间的**横向入口卡栏**（2026-09-23 下午定稿）
 *
 * 这三张卡指向三个独立页：
 *   备考资料 → /assessment/intern/learning/guide
 *   模拟理论考核 → /assessment/intern/learning/mock/theory（清单列表 + 自测 + 记录）
 *   模拟实操考核 → /assessment/intern/learning/mock/practice（实操题卡片）
 *
 * 「正式考核」不在这里 —— 它自己在「学习与考核」页里占一栏（见 utils/internTabs.js 的 sec-exam）。
 *
 * 角色差异：正式实习生这 3 个入口全部关闭，因此**整栏不渲染**
 * （internTabs.js 的 FORMAL_INTERN_SECTION_KEYS 不含 entries），本组件无需再按角色过滤；
 * 路由层另有 permission.js 的 formalInternBlockedPaths 兜底直连。
 */
export default {
  name: 'InternAssessEntries',
  data() {
    return {
      entries: [
        {
          key: 'guide',
          title: '备考资料',
          desc: '考试指南与考核规则，支持在线预览与下载',
          icon: 'el-icon-notebook-2',
          tone: 'is-guide',
          path: '/assessment/intern/learning/guide'
        },
        {
          key: 'theory',
          title: '模拟理论考核',
          desc: '按套自测，交卷即出正确率与逐题解析',
          icon: 'el-icon-edit-outline',
          tone: 'is-theory',
          path: '/assessment/intern/learning/mock/theory'
        },
        {
          key: 'practical',
          title: '模拟实操考核',
          desc: '参考示例、题干与附件，只看不答',
          icon: 'el-icon-video-camera',
          tone: 'is-practical',
          path: '/assessment/intern/learning/mock/practice'
        }
      ]
    }
  },
  methods: {
    go(path) {
      this.$router.push(path)
    }
  }
}
</script>

<style lang="scss" scoped>
/* 本栏是合并页里的「中间那一栏」，左右两栏（在线学习 26px / 成绩与转正 24px）
   各自带内边距，本栏若不加，入口卡就会比它们的白卡左右各宽出一圈（宽度对不齐）。
   这里补上与「在线学习」相同的 26px，让三栏的版心落在同一列上。
   底部 40px 与另两栏的 padding-bottom 一致，分隔线上下留白才对称。 */
.entries-page { padding: 0 26px 40px; color: #283544; }
/* 与在线学习 / 成绩与转正两栏的分节标题保持同一套尺度：eyebrow 11px + 标题 26px + 说明 13px。
   （原来这里是 h2/20px，夹在左右两栏的大标题中间显得又小又不像同级。） */
.entries-heading { margin-bottom: 18px; }
.eyebrow { color: #2878c7; font-size: 11px; letter-spacing: .08em; }
.entries-heading h1 { margin: 5px 0 7px; color: #1d2939; font-size: 26px; font-weight: 600; }
.entries-heading p { margin: 0; color: #667085; font-size: 13px; }

/* 横着的卡片栏：三个入口并排 */
.entry-bar { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }

.entry-card {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
  /* 高度对齐左右两栏的白卡（约 130px），太扁会像一行按钮而不像入口卡 */
  min-height: 132px;
  padding: 24px 22px;
  border: 1px solid #e4e9f0;
  border-radius: 6px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: border-color .15s, box-shadow .15s, transform .15s;
}
.entry-card:hover { border-color: #1764f5; box-shadow: 0 6px 18px rgba(23, 100, 245, .1); transform: translateY(-1px); }
.entry-card:hover .entry-go { color: #1764f5; }
.entry-card:hover .entry-go i { transform: translateX(2px); }

.entry-icon {
  display: flex;
  width: 52px;
  height: 52px;
  flex: 0 0 52px;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #e8f1fd;
  color: #1764f5;
  font-size: 24px;
}
.entry-card.is-guide .entry-icon { background: #eaf7f1; color: #23966f; }
.entry-card.is-theory .entry-icon { background: #fff4e5; color: #b54708; }
.entry-card.is-practical .entry-icon { background: #e8f1fd; color: #1764f5; }

.entry-body { min-width: 0; flex: 1; }
.entry-body b { display: block; margin-bottom: 8px; color: #1d2939; font-size: 17px; font-weight: 600; }
.entry-body span { display: block; color: #667085; font-size: 13px; line-height: 1.6; }

.entry-go {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 3px;
  color: #98a2b3;
  font-size: 12px;
  transition: color .15s;
}
.entry-go i { font-size: 12px; transition: transform .15s; }

@media (max-width: 1100px) {
  .entry-bar { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 700px) {
  .entries-page { padding: 0 12px 28px; }
  .entry-bar { grid-template-columns: minmax(0, 1fr); }
  .entry-card { min-height: 116px; padding: 18px 16px; }
}
</style>
