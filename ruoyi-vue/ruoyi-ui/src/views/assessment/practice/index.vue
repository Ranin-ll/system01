<template>
  <div class="practice-page">
    <div class="exam-breadcrumb">
      <el-button type="text" icon="el-icon-arrow-left" @click="goBack">返回工作台</el-button>
      <span>/</span>
      <b>模拟考核</b>
    </div>

    <!-- 阶段一：入口 + 历史成绩 -->
    <template v-if="stage === 'list'">
      <header class="exam-heading">
        <div>
          <span class="eyebrow">PRACTICE CENTER</span>
          <h1>模拟考核</h1>
          <p>每次随机抽取 10 道题，交卷自动判分，成绩仅供本人查看，不计入正式成绩。</p>
        </div>
        <el-button size="medium" icon="el-icon-suitcase" @click="goSubjectList">实操练习</el-button>
      </header>

      <div class="start-card">
        <div class="start-info">
          <div class="start-icon"><i class="el-icon-edit-outline" /></div>
          <div>
            <h3>开始一次模拟考核</h3>
            <p>从本部门模拟题库随机抽 10 题（单选 / 多选 / 判断），每题 1 分，满分 10 分。</p>
          </div>
        </div>
        <el-button type="primary" size="medium" icon="el-icon-caret-right" @click="startPractice">开始模拟考核</el-button>
      </div>

      <section class="record-section">
        <div class="section-title">
          <h3>我的模拟记录</h3>
          <span class="section-tip">点击「查看详情」进入回顾页，可回看当次题目、你的作答与正确答案</span>
        </div>
        <div v-if="!records.length" class="empty-state"><i class="el-icon-tickets" /><span>还没有模拟记录，先来一次吧</span></div>
        <el-table v-else :data="records" stripe>
          <el-table-column label="成绩" width="140" align="center">
            <template slot-scope="scope"><strong class="score-text">{{ scope.row.correctCount }} / {{ scope.row.totalCount }}</strong></template>
          </el-table-column>
          <el-table-column label="正确题数" width="110" align="center">
            <template slot-scope="scope">{{ scope.row.correctCount }} 题</template>
          </el-table-column>
          <el-table-column label="题库" min-width="150">
            <template slot-scope="scope">{{ scope.row.bankName || '-' }}</template>
          </el-table-column>
          <el-table-column label="时间" width="180" align="center">
            <template slot-scope="scope">{{ fmtTime(scope.row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center">
            <template slot-scope="scope">
              <el-button type="text" icon="el-icon-view" @click="goRecordDetail(scope.row)">查看详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </template>

    <!-- 阶段二：作答 -->
    <template v-else-if="stage === 'exam'">
      <header class="exam-heading">
        <div><span class="eyebrow">ANSWERING</span><h1>{{ bankName || '模拟考核' }}</h1><p>共 {{ questions.length }} 题 · 请完成后点击交卷</p></div>
        <div class="exam-progress"><el-progress :percentage="progress" :stroke-width="8" /></div>
      </header>

      <div v-for="(q, index) in questions" :key="q.id" class="question-card">
        <div class="q-head">
          <span class="q-index">{{ index + 1 }}</span>
          <el-tag size="mini" effect="plain" :type="typeTag(q.qtype)">{{ typeLabel(q.qtype) }}</el-tag>
        </div>
        <div class="q-stem">{{ q.stem }}</div>
        <div class="q-options">
          <template v-if="q.qtype === 'MULTI'">
            <el-checkbox-group v-model="answers[index]">
              <div v-for="opt in parseOptions(q.optionsJson)" :key="opt.key" class="option-item">
                <el-checkbox :label="opt.key">{{ opt.key }}. {{ opt.content }}</el-checkbox>
              </div>
            </el-checkbox-group>
          </template>
          <template v-else>
            <el-radio-group v-model="answers[index]">
              <div v-for="opt in parseOptions(q.optionsJson)" :key="opt.key" class="option-item">
                <el-radio :label="opt.key">{{ opt.key }}. {{ opt.content }}</el-radio>
              </div>
            </el-radio-group>
          </template>
        </div>
      </div>

      <div class="submit-bar">
        <el-button size="medium" @click="backToList">放弃返回</el-button>
        <el-button type="primary" size="medium" :loading="submitting" @click="submitPractice">交 卷</el-button>
      </div>
    </template>

    <!-- 阶段三：交卷结果（含逐题回顾） -->
    <template v-else>
      <div class="result-card">
        <div class="result-score">
          <span class="score-label">得分</span>
          <span class="score-num">{{ result.correctCount }}</span>
          <span class="score-total">/ {{ result.totalCount }}</span>
        </div>
        <p class="result-desc">共 {{ result.totalCount }} 题，答对 {{ result.correctCount }} 题</p>

        <div class="qa-list">
          <div v-for="(item, i) in result.details" :key="i" class="qa-card" :class="item.correct ? 'ok' : 'no'">
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

        <div class="result-actions">
          <el-button size="medium" @click="goList">返回列表</el-button>
          <el-button type="primary" size="medium" @click="startPractice">再练一次</el-button>
        </div>
      </div>
    </template>
  </div>
</template>

<script>
import { startPractice, submitPractice, myPracticeRecords } from '@/api/business/practice'
import practiceMixin from './practice-mixin'

export default {
  name: 'InternPractice',
  mixins: [practiceMixin],
  data() {
    return {
      stage: 'list',
      loading: false,
      records: [],
      bankId: null,
      bankName: '',
      questions: [],
      answers: [],
      result: { totalCount: 0, correctCount: 0, details: [] },
      submitting: false
    }
  },
  computed: {
    progress() {
      const answered = this.answers.filter(a => {
        if (Array.isArray(a)) return a.length > 0
        return !!a
      }).length
      return this.questions.length ? Math.round(answered / this.questions.length * 100) : 0
    }
  },
  created() {
    this.loadRecords()
    // 从回顾页「再练一次」跳回时自动开始抽题（先清掉 query，避免刷新重复触发）
    if (this.$route.query.start === '1') {
      this.$router.replace({ path: this.$route.path })
      this.startPractice()
    }
  },
  mounted() {
    window.addEventListener('beforeunload', this.handleBeforeUnload)
  },
  beforeDestroy() {
    window.removeEventListener('beforeunload', this.handleBeforeUnload)
  },
  beforeRouteLeave(to, from, next) {
    if (this.stage === 'exam' && !this.submitting) {
      this.$modal.confirm('退出后本次作答进度不会保存，确定退出吗？').then(() => next()).catch(() => next(false))
    } else {
      next()
    }
  },
  methods: {
    loadRecords() {
      this.loading = true
      myPracticeRecords().then(res => {
        this.records = res.data || []
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    startPractice() {
      this.loading = true
      startPractice().then(res => {
        const data = res.data
        this.bankId = data.bankId
        this.bankName = data.bankName
        this.questions = data.questions || []
        this.answers = this.questions.map(q => q.qtype === 'MULTI' ? [] : '')
        this.loading = false
        this.stage = 'exam'
      }).catch(() => { this.loading = false })
    },
    submitPractice() {
      const unanswered = this.answers.filter(a => {
        if (Array.isArray(a)) return a.length === 0
        return !a
      }).length
      if (unanswered > 0) {
        this.$modal.confirm(`还有 ${unanswered} 题未作答，确定交卷吗？`).then(() => this.doSubmit()).catch(() => {})
        return
      }
      this.doSubmit()
    },
    doSubmit() {
      this.submitting = true
      const answers = this.questions.map((q, i) => {
        const ans = this.answers[i]
        return { questionId: q.id, userAnswer: Array.isArray(ans) ? ans.slice().sort().join(',') : ans }
      })
      submitPractice({ bankId: this.bankId, answers }).then(res => {
        this.result = res.data
        this.submitting = false
        this.stage = 'result'
        this.loadRecords()
      }).catch(() => { this.submitting = false })
    },
    /** 进入独立的回顾页查看当次题目与作答 */
    goRecordDetail(row) {
      this.$router.push('/assessment/intern/mock-exam/record/' + row.id)
    },
    /** 进入独立的实操练习页 */
    goSubjectList() {
      this.$router.push('/assessment/intern/practice-subject')
    },
    backToList() {
      this.$modal.confirm('退出后本次作答进度不会保存，确定退出吗？').then(() => {
        this.stage = 'list'
        this.loadRecords()
      }).catch(() => {})
    },
    goList() {
      this.stage = 'list'
      this.loadRecords()
    },
    goBack() {
      this.$router.push('/index')
    },
    handleBeforeUnload(e) {
      if (this.stage === 'exam') {
        e.preventDefault()
        e.returnValue = ''
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.practice-page { min-height: 100%; padding: 24px 26px 60px; color: #283544; background: #f5f7fa; }
.exam-breadcrumb { display: flex; align-items: center; gap: 8px; margin-bottom: 14px; color: #98a2b3; font-size: 12px; }
.exam-breadcrumb .el-button { padding: 0; color: #1764f5; font-size: 12px; }
.exam-breadcrumb b { color: #475467; font-weight: 500; }
.exam-heading { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 20px; }
.eyebrow { color: #1764f5; font-size: 11px; letter-spacing: .08em; }
.exam-heading h1 { margin: 5px 0 7px; color: #1d2939; font-size: 26px; font-weight: 600; }
.exam-heading p { margin: 0; color: #667085; font-size: 13px; }
.exam-progress { width: 240px; }
.start-card { display: flex; align-items: center; justify-content: space-between; padding: 22px 26px; margin-bottom: 24px; background: #fff; border: 1px solid #e7ecf3; border-radius: 10px; }
.start-info { display: flex; align-items: center; gap: 16px; }
.start-icon { display: flex; width: 52px; height: 52px; align-items: center; justify-content: center; color: #fff; background: #1764f5; font-size: 26px; border-radius: 10px; }
.start-info h3 { margin: 0 0 6px; color: #1d2939; font-size: 17px; font-weight: 600; }
.start-info p { margin: 0; color: #667085; font-size: 13px; }
.record-section { padding: 10px 2px; }
.section-title { display: flex; align-items: baseline; gap: 12px; margin-bottom: 14px; }
.section-title h3 { margin: 0; color: #1d2939; font-size: 16px; font-weight: 600; }
.section-tip { color: #98a2b3; font-size: 12px; }
.score-text { color: #1764f5; font-size: 16px; }
.empty-state { display: flex; min-height: 120px; align-items: center; justify-content: center; flex-direction: column; gap: 8px; color: #98a2b3; }
.empty-state i { font-size: 32px; color: #b7c1cc; }
.empty-state span { font-size: 13px; }
.question-card { margin-bottom: 16px; padding: 20px 22px; background: #fff; border: 1px solid #e7ecf3; border-radius: 8px; }
.q-head { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.q-index { display: inline-flex; width: 24px; height: 24px; align-items: center; justify-content: center; color: #fff; background: #1764f5; font-size: 13px; border-radius: 50%; }
.q-stem { margin-bottom: 14px; color: #1d2939; font-size: 15px; line-height: 1.7; }
.q-options .option-item { padding: 9px 12px; margin-bottom: 6px; border: 1px solid #edf0f4; border-radius: 6px; }
.q-options .option-item:hover { background: #f8fafc; }
.option-item .el-radio, .option-item .el-checkbox { display: block; margin-right: 0; line-height: 1.6; }
.submit-bar { display: flex; justify-content: flex-end; gap: 12px; margin-top: 8px; padding: 16px 0; }
.result-card { padding: 30px; background: #fff; border: 1px solid #e7ecf3; border-radius: 10px; text-align: center; }
.result-score { display: flex; align-items: baseline; justify-content: center; gap: 8px; margin-bottom: 6px; }
.score-label { color: #667085; font-size: 14px; }
.score-num { color: #1764f5; font-size: 52px; font-weight: 700; line-height: 1; }
.score-total { color: #98a2b3; font-size: 20px; }
.result-desc { color: #667085; font-size: 14px; margin-bottom: 24px; }
.result-actions { display: flex; justify-content: center; gap: 12px; margin-top: 24px; }

/* 逐题回顾 */
.qa-list { max-width: 680px; margin: 0 auto; text-align: left; }
.qa-card { padding: 16px 18px; margin-bottom: 14px; background: #fff; border: 1px solid #e7ecf3; border-radius: 8px; }
.result-card .qa-card { background: #fbfcfe; }
.qa-card.ok { border-left: 4px solid #23966f; }
.qa-card.no { border-left: 4px solid #f56c6c; }
.qa-head { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.qa-idx { display: inline-flex; width: 22px; height: 22px; align-items: center; justify-content: center; color: #fff; font-size: 12px; border-radius: 50%; }
.qa-idx.ok { background: #23966f; }
.qa-idx.no { background: #f56c6c; }
.qa-flag { font-size: 12px; font-weight: 600; }
.qa-flag.ok { color: #23966f; }
.qa-flag.no { color: #f56c6c; }
.qa-stem { margin-bottom: 10px; color: #1d2939; font-size: 14px; line-height: 1.7; }
.qa-opts { margin-bottom: 10px; }
.qa-opt { display: flex; align-items: center; gap: 8px; padding: 7px 10px; margin-bottom: 5px; color: #475467; font-size: 13px; background: #fff; border: 1px solid #edf0f4; border-radius: 6px; }
.qa-opt .opt-key { display: inline-flex; flex: none; width: 18px; height: 18px; align-items: center; justify-content: center; color: #667085; font-size: 11px; background: #f0f2f5; border-radius: 4px; }
.qa-opt .opt-content { flex: 1; }
.qa-opt .opt-flag-tag { flex: none; padding: 1px 7px; font-size: 11px; border-radius: 9px; }
.qa-opt .opt-flag-tag.ok { color: #1a7a58; background: #e4f5ee; }
.qa-opt .opt-flag-tag.pick { color: #1764f5; background: #e6efff; }
.qa-opt.correct { border-color: #b7e3d2; background: #f2fbf7; }
.qa-opt.correct .opt-key { color: #fff; background: #23966f; }
.qa-opt.picked { border-color: #b9d3ff; }
.qa-ans { display: flex; flex-wrap: wrap; gap: 22px; color: #667085; font-size: 13px; }
.qa-ans b.ok { color: #23966f; }
.qa-ans b.bad { color: #f56c6c; }
.qa-analysis { margin-top: 8px; padding: 8px 10px; color: #667085; font-size: 12px; line-height: 1.6; background: #f7f9fc; border-radius: 6px; }
.qa-analysis b { color: #475467; }
</style>
