<template>
  <div class="practice-page">
    <div class="exam-breadcrumb">
      <el-button type="text" icon="el-icon-arrow-left" @click="goBack">返回学习与考核</el-button>
      <span>/</span>
      <b>模拟实操考核</b>
    </div>

    <header class="exam-heading">
      <div>
        <span class="eyebrow">MOCK · PRACTICAL</span>
        <h1>模拟实操考核</h1>
        <p>本部门已发布的实操模拟题，点开看参考示例、题干与附件（只看不答）。</p>
      </div>
      <el-button size="medium" icon="el-icon-refresh" :loading="loading" @click="loadSubjects">刷新</el-button>
    </header>

    <!-- 实操模拟题：卡片形式，直接把本部门已发布的题铺开（不再有「模块」层级） -->
    <section class="pm-card">
      <div class="pm-head">
        <div class="pm-title"><span class="pm-idx">实</span><h3>实操模拟题</h3></div>
        <span class="pm-hint">{{ subjects.length }} 题</span>
      </div>
      <div v-if="loading && !subjects.length" class="pm-empty small"><i class="el-icon-loading" /><span>正在加载…</span></div>
      <div v-else-if="!subjects.length" class="pm-empty small"><i class="el-icon-document" /><span>本部门暂未发布实操模拟题</span></div>
      <div v-else class="pm-qgrid">
        <button v-for="(s, i) in subjects" :key="s.id" type="button" class="pm-qcard" @click="goSubjectDetail(s)">
          <span class="pm-thumb">
            <video v-if="firstImage(s) && isVideo(firstImage(s))" :src="baseApi + firstImage(s)" muted />
            <img v-else-if="firstImage(s)" :src="baseApi + firstImage(s)" :alt="s.title">
            <span v-else class="pm-thumb-ph" v-html="thumbSvg(i)" />
          </span>
          <span class="pm-qc-b">
            <b>{{ s.title || '未命名实操题' }}</b>
            <span class="pm-qc-f">
              <span>建议用时</span>
              <b>{{ s.estimatedMinutes ? s.estimatedMinutes + ' 分钟' : '不限' }}</b>
            </span>
          </span>
        </button>
      </div>
    </section>
  </div>
</template>

<script>
/**
 * 模拟实操考核（2026-09-23 由 practice/index.vue 的「模块内实操题」拆出，独立成页）
 *
 * 与原来「模拟考核 › 某模块 › 实操题」的差别：
 *  1. **去掉「模块」层级** —— 一次拉回本部门已发布的全部实操模拟题
 *     （后端 `moduleId` 传空即不过滤），直接铺成卡片。
 *  2. 与「模拟理论考核」分成两页：那边是清单+作答+自测记录，这边只看题不答题。
 *
 * 点卡片进实操题详情页（practice/subjectDetail.vue），详情页「返回」回到本页。
 */
import { listPracticeSubjects } from '@/api/business/practice'
import practiceMixin from './practice-mixin'

export default {
  name: 'InternMockPractical',
  mixins: [practiceMixin],
  data() {
    return {
      loading: false,
      subjects: []
    }
  },
  created() {
    this.loadSubjects()
  },
  methods: {
    /** 实操题清单：不传 moduleId —— 后端按「本部门 + 已发布」返回全部 */
    loadSubjects() {
      this.loading = true
      listPracticeSubjects().then(res => {
        this.subjects = res.data || []
        this.loading = false
      }).catch(() => {
        this.subjects = []
        this.loading = false
      })
    },
    /** 实操题参考（referenceImages 存 JSON 数组） */
    firstImage(subject) {
      const list = this.parseAttachments(subject.referenceImages)
      const first = list.length ? list[0] : null
      if (!first) return ''
      return typeof first === 'string' ? first : (first.url || '')
    },
    /** 无参考时用内联 SVG 占位（与设计稿缩略图一致） */
    thumbSvg(i) {
      const layouts = [
        '<svg viewBox="0 0 160 104" preserveAspectRatio="none"><rect x="14" y="18" width="132" height="70" rx="8" fill="#f2f7ff"/><rect x="14" y="18" width="132" height="15" rx="8" fill="#d7e7fc"/><rect x="26" y="42" width="46" height="6" rx="3" fill="#cfe0fb"/><rect x="26" y="54" width="70" height="6" rx="3" fill="#cfe0fb"/><rect x="26" y="66" width="34" height="6" rx="3" fill="#cfe0fb"/></svg>',
        '<svg viewBox="0 0 160 104" preserveAspectRatio="none"><rect x="26" y="30" width="108" height="16" rx="8" fill="#d7e7fc"/><rect x="26" y="48" width="108" height="16" rx="8" fill="#cfe0fb"/><rect x="26" y="66" width="108" height="16" rx="8" fill="#e0ebfd"/></svg>',
        '<svg viewBox="0 0 160 104" preserveAspectRatio="none"><rect x="14" y="18" width="132" height="70" rx="8" fill="#f2f7ff"/><rect x="14" y="18" width="132" height="16" rx="8" fill="#e0ebfd"/><rect x="24" y="46" width="112" height="10" rx="4" fill="#cfe0fb"/><rect x="24" y="62" width="52" height="18" rx="4" fill="#dbe8fc"/><rect x="84" y="62" width="52" height="18" rx="4" fill="#e0ebfd"/></svg>'
      ]
      return layouts[i % layouts.length]
    },
    /** 打开实操题详情页（不再需要带来源模块） */
    goSubjectDetail(subject) {
      this.$router.push('/assessment/intern/practice-subject/' + subject.id)
    },
    /**
     * 返回来源页：「学习与考核」的**考核入口卡栏**（本页就是从那张「模拟实操考核」卡进来的）。
     * 原先写的是工作台 —— 二级页应该回它的上一级，而不是跳过上层直接回首页。
     */
    goBack() {
      this.$router.push({ path: '/assessment/intern/learning', hash: '#sec-entries' })
    }
  }
}
</script>

<style lang="scss" scoped>
/* 页面外壳 / 卡头 / 实操题卡片样式都在公共样式里，本页无需额外定义 */
@import '~@/assets/styles/practice-module.scss';
</style>
