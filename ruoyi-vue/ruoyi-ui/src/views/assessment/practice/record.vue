<template>
  <div class="practice-page">
    <div class="exam-breadcrumb">
      <el-button type="text" icon="el-icon-arrow-left" @click="goList">返回模拟理论考核</el-button>
      <span>/</span>
      <b>模拟理论考核回顾</b>
    </div>

    <header class="exam-heading">
      <div>
        <span class="eyebrow">PRACTICE REVIEW</span>
        <h1>模拟理论考核回顾</h1>
      </div>
    </header>

    <div v-loading="loading" class="review-body">
      <div v-if="record" class="rv-summary">
        <div class="rv-item"><span class="rv-label">考核</span><b>{{ record.examName || '—' }}</b></div>
        <div class="rv-item"><span class="rv-label">成绩</span><b class="ok">{{ record.correctCount }} / {{ record.totalCount }}</b></div>
        <div class="rv-item"><span class="rv-label">正确题数</span><b>{{ record.correctCount }} 题</b></div>
        <div class="rv-item"><span class="rv-label">作答时间</span><b>{{ fmtTime(record.createTime) }}</b></div>
      </div>

      <div v-if="!loading && !items.length" class="empty-state small">
        <i class="el-icon-warning-outline" />
        <span>{{ record ? '该记录未保存题目明细（系统升级前产生的记录），仅可查看汇总成绩' : '记录不存在或无权查看' }}</span>
      </div>

      <div v-else class="qa-list">
        <div v-for="(item, i) in items" :key="i" class="qa-card" :class="item.correct ? 'ok' : 'no'">
          <div class="qa-head">
            <span class="qa-idx" :class="item.correct ? 'ok' : 'no'">{{ i + 1 }}</span>
            <el-tag size="mini" effect="plain" :type="typeTag(item.qtype)">{{ typeLabel(item.qtype) }}</el-tag>
            <span class="qa-flag" :class="item.correct ? 'ok' : 'no'">{{ item.correct ? '答对' : '答错' }}</span>
          </div>
          <div class="qa-stem">{{ item.stem }}</div>
          <div class="qa-opts">
            <div v-for="opt in parseOptions(item.optionsJson)" :key="opt.key" class="qa-opt"
                 :class="{ correct: isCorrectOpt(item, opt.key), picked: isPicked(item, opt.key) }">
              <span class="opt-key">{{ opt.key }}</span>
              <span class="opt-content">{{ opt.content }}</span>
              <span v-if="isCorrectOpt(item, opt.key)" class="opt-flag-tag ok">正确答案</span>
              <span v-if="isPicked(item, opt.key)" class="opt-flag-tag pick">我的选择</span>
            </div>
          </div>
          <div class="qa-ans">
            <span>我的作答：<b :class="item.correct ? 'ok' : 'bad'">{{ item.userAnswer || '未作答' }}</b></span>
            <span>正确答案：<b class="ok">{{ item.correctAnswer || '—' }}</b></span>
          </div>
          <div v-if="item.analysis" class="qa-analysis"><b>解析：</b>{{ item.analysis }}</div>
        </div>
      </div>

      <div class="action-bar">
        <el-button size="medium" @click="goList">返回列表</el-button>
        <el-button v-if="!isFormal" type="primary" size="medium" @click="rePractice">再练一次</el-button>
      </div>
    </div>
  </div>
</template>

<script>
import { practiceRecordDetail } from '@/api/business/practice'
import practiceMixin from './practice-mixin'

export default {
  name: 'InternMockExamRecord',
  mixins: [practiceMixin],
  data() {
    return {
      loading: false,
      record: null,
      items: []
    }
  },
  computed: {
    isFormal() {
      return this.$store.getters.roles.indexOf('FORMAL_TRAINEE') > -1
    }
  },
  watch: {
    // 同一路由下切换记录ID时重新加载
    '$route.params.recordId'() {
      this.load()
    }
  },
  created() {
    this.load()
  },
  methods: {
    load() {
      const recordId = this.$route.params.recordId
      if (!recordId) {
        this.goList()
        return
      }
      this.loading = true
      this.record = null
      this.items = []
      practiceRecordDetail(recordId).then(res => {
        const data = res.data || {}
        this.record = data.record || null
        this.items = (data.items || []).map(this.mapRecordItem)
        this.loading = false
      }).catch(() => {
        // 越权或记录不存在时后端已给出提示，这里仅落回空态
        this.loading = false
        this.record = null
        this.items = []
      })
    },
    goList() {
      this.$router.push('/assessment/intern/learning/mock/theory')
    },
    /** 回到自测页并自动开始新一次抽题 */
    rePractice() {
      this.$router.push({ path: '/assessment/intern/learning/mock/theory', query: { start: '1' } })
    }
  }
}
</script>

<style lang="scss" scoped>
@import '~@/assets/styles/practice-module.scss';

.review-body { min-height: 200px; }
</style>
