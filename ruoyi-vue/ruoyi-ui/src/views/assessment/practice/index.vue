<template>
  <div class="practice-page">
    <div class="exam-breadcrumb">
      <el-button type="text" icon="el-icon-arrow-left" @click="goBack">返回工作台</el-button>
      <span>/</span>
      <b>{{ isFormal ? '模拟理论考核记录' : '模拟理论考核' }}</b>
    </div>

    <!-- ================= 列表页：套卷 + 练习记录（无阶段层级） ================= -->
    <template v-if="stage === 'list'">
      <header class="exam-heading">
        <div>
          <span class="eyebrow">{{ isFormal ? 'PRACTICE HISTORY' : 'THEORY PRACTICE' }}</span>
          <h1>{{ isFormal ? '模拟理论考核记录' : '模拟理论考核' }}</h1>
        </div>
        <el-button size="medium" icon="el-icon-refresh" @click="reload">刷新</el-button>
      </header>

      <!-- ① 模拟统计条 -->
      <div class="stat-strip">
        <div class="stat">
          <span>累计自测次数</span>
          <b>{{ records.length }}<small>次</small></b>
          <div class="sub">近 30 天 {{ recent30 }} 次</div>
        </div>
        <div class="stat">
          <span>理论平均正确率</span>
          <b>{{ avgRate }}<small>%</small></b>
          <svg v-if="sparkPoints" viewBox="0 0 90 22" class="spark">
            <polyline :points="sparkPoints" fill="none" stroke="#12b76a" stroke-width="1.8" />
          </svg>
          <div v-else class="sub">暂无记录</div>
        </div>
        <div class="stat">
          <span>理论最高正确率</span>
          <b>{{ maxRate }}<small>%</small></b>
          <div class="sub">单次自测峰值</div>
        </div>
        <div class="stat">
          <span>模拟理论考核</span>
          <b>{{ examTotal }}<small> 套</small></b>
          <div class="sub">{{ exams.length ? '已发布 ' + exams.length + ' 套' : '尚未发布' }}</div>
        </div>
        <div class="stat">
          <span>最近自测</span>
          <b class="sm-text">{{ lastRecord ? fmtTime(lastRecord.createTime) : '--' }}</b>
          <div class="sub" :class="{ ok: lastRecord }">{{ lastRecord ? '正确率 ' + rateOf(lastRecord) + '%' : '尚未开始' }}</div>
        </div>
      </div>

      <!-- ② 模拟理论考核套卷：直接全量列出已发布套卷（无阶段层级） -->
      <section v-if="!isFormal" class="pm-card">
        <div class="pm-head">
          <div class="pm-title"><span class="pm-idx">卷</span><h3>模拟理论考核套卷</h3></div>
          <el-button size="mini" icon="el-icon-refresh" @click="loadPapers">刷新</el-button>
        </div>
        <div v-if="contentLoading" class="pm-empty small"><i class="el-icon-loading" /><span>正在加载…</span></div>
        <div v-else-if="!exams.length" class="pm-empty small"><i class="el-icon-document" /><span>本部门暂未发布模拟理论考核套卷</span></div>
        <div v-else class="pm-exams">
          <div v-for="e in exams" :key="e.id" class="pm-exam">
            <div class="pm-exam-l">
              <b>{{ e.examName || '模拟理论考核卷' }}<span v-if="e.difficulty" class="pm-diff" :class="diffTone(e.difficulty)">{{ diffText(e.difficulty) }}</span></b>
              <span class="pm-exam-meta">
                共 {{ e.questionCount || 0 }} 题（单选 {{ e.singleCount || 0 }} · 多选 {{ e.multiCount || 0 }} · 判断 {{ e.judgeCount || 0 }}）
                · 满分 {{ e.fullScore != null ? e.fullScore : '--' }} 分
                · 通过线 {{ e.passLine != null ? e.passLine : 0 }} 分
                <template v-if="Number(e.duration) > 0"> · 限时 {{ e.duration }} 分钟</template>
              </span>
              <span v-if="e.contentBias" class="pm-exam-bias"><i class="el-icon-info" />内容偏向：{{ e.contentBias }}</span>
            </div>
            <el-button type="primary" size="small" icon="el-icon-caret-right" :loading="starting" @click="startPractice(e)">开始练习</el-button>
          </div>
        </div>
      </section>

      <!-- ③ 自测记录 -->
      <section class="pm-card">
        <div class="pm-head">
          <div class="pm-title"><span class="pm-idx">录</span><h3>模拟练习记录</h3></div>
        </div>
        <div v-loading="loading" class="pm-table-wrap" style="padding-top:12px">
          <div v-if="!records.length && !loading" class="pm-empty">
            <i class="el-icon-tickets" />
            <span>{{ isFormal ? '暂无转正前的模拟记录' : '还没有模拟记录，选一套卷开始练习吧' }}</span>
          </div>
          <table v-else class="pm-table">
            <thead><tr><th>时间</th><th>套卷</th><th>题数</th><th>正确率</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="row in pagedRecords" :key="row.id">
                <td>{{ fmtTime(row.createTime) }}</td>
                <td class="ellipsis">{{ row.examName || '—' }}</td>
                <td>{{ row.totalCount }} 题</td>
                <td><b :class="rateTone(row)">{{ rateOf(row) }}%</b></td>
                <td><el-button type="text" size="mini" @click="goRecordDetail(row)">查看详情</el-button></td>
              </tr>
            </tbody>
          </table>
          <el-pagination
            v-if="records.length > recordPageSize"
            class="pm-pager"
            background
            layout="total, sizes, prev, pager, next, jumper"
            :total="records.length"
            :current-page="recordPage"
            :page-size="recordPageSize"
            :page-sizes="[8, 10, 20, 50]"
            @current-change="onRecordPageChange"
            @size-change="onRecordPageSizeChange"
          />
        </div>
      </section>
    </template>


    <!-- ================= 作答 ================= -->
    <template v-else-if="stage === 'exam'">
      <header class="exam-heading">
        <div><span class="eyebrow">ANSWERING</span><h1>{{ bankName || '模拟理论考核' }}</h1><p>共 {{ questions.length }} 题</p></div>
        <div class="exam-head-right">
          <div v-if="remainingSeconds > 0" class="countdown" :class="countdownTone">
            <i class="el-icon-alarm-clock" />
            <span>剩余</span>
            <b>{{ countdownText }}</b>
          </div>
          <div class="exam-progress"><el-progress :percentage="progress" :stroke-width="8" /></div>
        </div>
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

    <!-- ================= 交卷结果（含逐题回顾） ================= -->
    <template v-else>
      <div class="result-card">
        <div class="result-score">
          <span class="score-label">得分</span>
          <span class="score-num">{{ result.score != null ? result.score : result.correctCount }}</span>
          <span class="score-total">/ {{ result.fullScore != null ? result.fullScore : result.totalCount }}</span>
        </div>
        <p class="result-desc">
          共 {{ result.totalCount }} 题，答对 {{ result.correctCount }} 题
          <template v-if="result.passLine != null"> · 通过线 {{ result.passLine }} 分 ·
            <b :class="result.passed ? 'good' : 'poor'">{{ result.passed ? '已达标' : '未达标' }}</b>
          </template>
        </p>

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
          <el-button size="medium" @click="goList">返回套卷列表</el-button>
          <el-button v-if="!isFormal && currentExamId" type="primary" size="medium" @click="restartSameExam">再练一次</el-button>
        </div>
      </div>
    </template>
  </div>
</template>

<script>
import {
  startPractice, submitPractice, myPracticeRecords,
  listPracticeExams
} from '@/api/business/practice'
import practiceMixin from './practice-mixin'
import { mapGetters } from 'vuex'

export default {
  name: 'InternPractice',
  mixins: [practiceMixin],
  data() {
    return {
      /** list 套卷列表 / exam 作答 / result 结果 */
      stage: 'list',
      loading: false,
      contentLoading: false,
      records: [],
      /** 本部门已发布的模拟理论考核套卷 */
      exams: [],
      /** 记录列表分页 */
      recordPage: 1,
      recordPageSize: 8,
      bankId: null,
      bankName: '',
      questions: [],
      answers: [],
      result: { totalCount: 0, correctCount: 0, details: [] },
      submitting: false,
      starting: false,
      currentExamId: null,
      // 自测倒计时：配置了时长才启动（0 = 不限时）
      remainingSeconds: 0,
      countdownTimer: null
    }
  },
  computed: {
    ...mapGetters(['deptName']),
    isFormal() {
      return this.$store.getters.roles.indexOf('FORMAL_TRAINEE') > -1
    },
    progress() {
      const answered = this.answers.filter(a => {
        if (Array.isArray(a)) return a.length > 0
        return !!a
      }).length
      return this.questions.length ? Math.round(answered / this.questions.length * 100) : 0
    },
    /** 正确率序列（旧 → 新），用于平均 / 峰值 / 迷你折线 */
    rateSeries() {
      return this.records.slice().reverse().map(r => this.rateOf(r))
    },
    /** 顶层「理论自测记录」当前页 */
    pagedRecords() {
      return this.pageSlice(this.records, this.recordPage, this.recordPageSize)
    },
    avgRate() {
      const s = this.rateSeries
      if (!s.length) return '--'
      return Math.round(s.reduce((a, b) => a + b, 0) / s.length)
    },
    maxRate() {
      const s = this.rateSeries
      return s.length ? Math.max.apply(null, s) : '--'
    },
    sparkPoints() {
      const s = this.rateSeries.slice(-6)
      if (s.length < 2) return ''
      const step = 84 / (s.length - 1)
      return s.map((v, i) => Math.round(3 + i * step) + ',' + Math.round(17 - v / 100 * 13)).join(' ')
    },
    recent30() {
      const limit = Date.now() - 30 * 24 * 3600 * 1000
      return this.records.filter(r => r.createTime && new Date(r.createTime).getTime() >= limit).length
    },
    lastRecord() {
      if (!this.records.length) return null
      return this.records.slice().sort((a, b) => new Date(b.createTime || 0) - new Date(a.createTime || 0))[0]
    },
    examTotal() {
      return this.exams.length
    },
    /** 倒计时展示：≥1 小时用 HH:MM:SS，否则 MM:SS */
    countdownText() {
      const total = this.remainingSeconds
      const h = Math.floor(total / 3600)
      const m = Math.floor((total % 3600) / 60)
      const s = total % 60
      const pad = n => (n < 10 ? '0' + n : '' + n)
      return h > 0 ? pad(h) + ':' + pad(m) + ':' + pad(s) : pad(m) + ':' + pad(s)
    },
    /** 倒计时紧迫度：≤1 分钟红、≤5 分钟橙 */
    countdownTone() {
      if (this.remainingSeconds <= 60) return 'danger'
      if (this.remainingSeconds <= 300) return 'warn'
      return ''
    }
  },
  created() {
    this.reload()
    const q = this.$route.query
    const autoStart = !this.isFormal && q.start === '1'
    // 先清掉 query，避免刷新时重复触发
    if (autoStart) {
      this.$router.replace({ path: this.$route.path })
    }
    // 从回顾页「再练一次」跳回时自动按同一套卷开考
    if (autoStart) {
      this.startPracticeById(q.examId ? Number(q.examId) : null)
    }
  },
  mounted() {
    window.addEventListener('beforeunload', this.handleBeforeUnload)
  },
  beforeDestroy() {
    window.removeEventListener('beforeunload', this.handleBeforeUnload)
    this.clearCountdown()
  },
  beforeRouteLeave(to, from, next) {
    if (this.stage === 'exam' && !this.submitting) {
      this.$modal.confirm('退出后本次作答进度不会保存，确定退出吗？').then(() => {
        this.clearCountdown()
        next()
      }).catch(() => next(false))
    } else {
      this.clearCountdown()
      next()
    }
  },
  methods: {
    /** 通用分页切片：页码越界时回落到最后一页，避免删/加数据后停在空白页 */
    pageSlice(list, page, size) {
      const arr = list || []
      const totalPages = Math.max(1, Math.ceil(arr.length / size))
      const p = Math.min(Math.max(1, page || 1), totalPages)
      const start = (p - 1) * size
      return arr.slice(start, start + size)
    },
    onRecordPageChange(p) { this.recordPage = p },
    onRecordPageSizeChange(size) { this.recordPageSize = size; this.recordPage = 1 },
    reload() {
      this.loadRecords()
      if (!this.isFormal) this.loadPapers()
    },
    loadRecords() {
      this.loading = true
      myPracticeRecords().then(res => {
        this.records = res.data || []
        this.loading = false
        this.recordPage = 1
      }).catch(() => { this.loading = false })
    },
    /** 本部门已发布的模拟理论考核套卷（无阶段层级，直接全量列出） */
    loadPapers() {
      this.contentLoading = true
      listPracticeExams().then(res => {
        this.exams = res.data || []
        this.contentLoading = false
      }).catch(() => { this.exams = []; this.contentLoading = false })
    },
    rateOf(row) {
      const total = Number(row.totalCount) || 0
      if (!total) return 0
      return Math.round((Number(row.correctCount) || 0) / total * 100)
    },
    rateTone(row) {
      const rate = this.rateOf(row)
      if (rate >= 80) return 'good'
      if (rate >= 60) return 'mid'
      return 'poor'
    },
    /** 套卷难易程度 */
    diffText(d) {
      return { EASY: '简单', MEDIUM: '中等', HARD: '困难' }[d] || ''
    },
    diffTone(d) {
      return { EASY: 'easy', MEDIUM: 'mid', HARD: 'hard' }[d] || 'mid'
    },
    /** 套卷「开始练习」 */
    startPractice(exam) {
      this.startPracticeById(exam ? exam.id : null)
    },
    /** 从记录页「再练一次」回到页面时按考核ID直接开考 */
    restartSameExam() {
      this.startPracticeById(this.currentExamId)
    },
    startPracticeById(examId) {
      if (this.isFormal) {
        this.$modal.msgWarning('正式实习生仅可查看转正前的模拟考核记录')
        return
      }
      this.starting = true
      startPractice(examId).then(res => {
        const data = res.data
        this.currentExamId = data.examId || examId || null
        this.bankId = data.bankId
        this.bankName = data.configName || data.bankName
        this.questions = data.questions || []
        this.answers = this.questions.map(q => q.qtype === 'MULTI' ? [] : '')
        this.starting = false
        this.stage = 'exam'
        // 部门配置了时长才倒计时（分钟 → 秒）
        const minutes = Number(data.duration) || 0
        this.startCountdown(minutes > 0 ? minutes * 60 : 0)
      }).catch(() => { this.starting = false })
    },
    /** 启动倒计时（本地每秒递减） */
    startCountdown(seconds) {
      this.clearCountdown()
      this.remainingSeconds = seconds > 0 ? seconds : 0
      if (this.remainingSeconds <= 0) {
        return
      }
      this.countdownTimer = setInterval(() => {
        if (this.remainingSeconds <= 1) {
          this.remainingSeconds = 0
          this.clearCountdown()
          this.onTimeUp()
          return
        }
        this.remainingSeconds -= 1
      }, 1000)
    },
    clearCountdown() {
      if (this.countdownTimer) {
        clearInterval(this.countdownTimer)
        this.countdownTimer = null
      }
    },
    /**
     * 倒计时归零：提示「将自动提交」，确认后自动交卷并返回考核列表。
     * 注意：旧写法 `this.$modal.alert(x).catch(...)` 会因 alert 没 return 而抛
     * TypeError（异常在定时器回调里被静默吞掉，导致后面的 doSubmit 根本不执行）。
     */
    onTimeUp() {
      if (this.stage !== 'exam' || this.submitting) {
        return
      }
      this.clearCountdown()
      this.remainingSeconds = 0
      this.$modal.confirmOnly('自测时间已到，将自动提交本次作答。').then(() => {
        this.doSubmit('back')
      }).catch(() => {})
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
    /**
     * 交卷。
     * @param {String} [done] 传 'back' 表示倒计时到点自动交卷：提交成功后直接回考核列表；
     *                        不传则进「结果」页（手动交卷，看当次得分与逐题回顾）。
     */
    doSubmit(done) {
      this.submitting = true
      const answers = this.questions.map((q, i) => {
        const ans = this.answers[i]
        return { questionId: q.id, userAnswer: Array.isArray(ans) ? ans.slice().sort().join(',') : ans }
      })
      submitPractice({ examId: this.currentExamId, bankId: this.bankId, answers }).then(res => {
        this.result = res.data
        this.submitting = false
        this.clearCountdown()
        this.remainingSeconds = 0
        if (done === 'back') {
          this.backToExamList()
        } else {
          this.stage = 'result'
          this.loadRecords()
        }
      }).catch(() => {
        this.submitting = false
        // 自动交卷必须让用户看见失败并有机会重试，不能静默吞掉
        if (done === 'back') {
          this.$modal.alertError('自动提交失败，请点击「交卷」重试。').catch(() => {})
        }
      })
    },
    /** 回到套卷列表 */
    backToExamList() {
      this.stage = 'list'
      this.loadRecords()
      this.loadPapers()
    },
    /** 进入独立的回顾页查看当次题目与作答 */
    goRecordDetail(row) {
      this.$router.push('/assessment/intern/mock-exam/record/' + row.id)
    },
    backToList() {
      this.$modal.confirm('退出后本次作答进度不会保存，确定退出吗？').then(() => {
        this.clearCountdown()
        this.remainingSeconds = 0
        this.stage = 'list'
        this.loadRecords()
        this.loadPapers()
      }).catch(() => {})
    },
    goList() {
      this.stage = 'list'
      this.reload()
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
.exam-heading { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 18px; }
.eyebrow { color: #1764f5; font-size: 11px; letter-spacing: .08em; }
.exam-heading h1 { margin: 5px 0 7px; color: #1d2939; font-size: 26px; font-weight: 600; }
.exam-heading p { margin: 0; color: #667085; font-size: 13px; }
.exam-progress { width: 240px; }
.exam-head-right { display: flex; align-items: center; gap: 16px; }
.countdown { display: inline-flex; align-items: center; gap: 6px; padding: 6px 14px; color: #1764f5; background: #e8f1fd; font-size: 13px; border-radius: 18px; }
.countdown b { font-size: 16px; letter-spacing: .04em; font-variant-numeric: tabular-nums; }
.countdown.warn { color: #b54708; background: #fff4e5; }
.countdown.danger { color: #b42318; background: #fee4e2; animation: cd-blink 1s steps(2, start) infinite; }
@keyframes cd-blink { 50% { opacity: .55; } }

/* ① 统计条 */
.stat-strip { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 12px; margin-bottom: 14px; }
.stat { min-width: 0; padding: 14px 15px; background: #fff; border: 1px solid #e7ecf3; border-radius: 8px; }
.stat > span { color: #8490a0; font-size: 12px; }
.stat > b { display: block; margin: 9px 0 6px; color: #1d2939; font-size: 22px; font-weight: 600; }
.stat > b.sm-text { font-size: 15px; }
.stat > b small { margin-left: 3px; color: #98a2b3; font-size: 12px; font-weight: 400; }
.stat .sub { color: #98a2b3; font-size: 11px; }
.stat .sub.ok { color: #067647; }
.spark { display: block; width: 90px; height: 22px; margin-top: 2px; }

/* 卡片与两栏 */
.pm-card { margin-bottom: 14px; padding: 18px 20px 20px; background: #fff; border: 1px solid #e7ecf3; border-radius: 8px; }
.pm-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding-bottom: 14px; border-bottom: 1px solid #edf0f4; }
.pm-title { display: flex; align-items: center; gap: 9px; }
.pm-title h3 { margin: 0; color: #1d2939; font-size: 17px; font-weight: 600; }
.pm-idx { display: inline-flex; width: 24px; height: 24px; align-items: center; justify-content: center; color: #1764f5; background: #e8f1fd; font-size: 12px; font-weight: 700; border-radius: 4px; }
.pm-hint { color: #98a2b3; font-size: 12px; }
.pm-table-wrap { min-height: 120px; }
.pm-table { width: 100%; border-collapse: collapse; }
.pm-table th { padding: 9px 10px; color: #8490a0; background: #f8fafc; font-size: 12px; font-weight: 500; text-align: left; }
.pm-table td { padding: 10px; border-bottom: 1px solid #edf0f4; color: #475467; font-size: 12.5px; }
.pm-table td b.good { color: #067647; }
.pm-table td b.mid { color: #d9930d; }
.pm-table td b.poor { color: #b54708; }
.pm-table td.ellipsis { max-width: 150px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.pm-pager { margin-top: 14px; text-align: right; }
.pm-empty { display: flex; min-height: 130px; align-items: center; justify-content: center; flex-direction: column; gap: 8px; color: #98a2b3; }
.pm-empty.small { min-height: 110px; }
.pm-empty i { color: #b7c1cc; font-size: 32px; }
.pm-empty span { font-size: 13px; }

/* ③ 套卷列表 */
.pm-exams { padding-top: 6px; }
.pm-exam { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 14px 4px; border-bottom: 1px solid #edf0f4; }
.pm-exam:last-child { border-bottom: 0; }
.pm-exam-l { min-width: 0; }
.pm-exam-l > b { display: block; margin-bottom: 6px; color: #1d2939; font-size: 14.5px; font-weight: 600; }
.pm-exam-meta { color: #8490a0; font-size: 12px; }
.pm-diff { display: inline-block; padding: 1px 8px; margin-left: 8px; font-size: 11.5px; font-weight: 500; border-radius: 10px; vertical-align: 1px; }
.pm-diff.easy { color: #067647; background: #ecfdf3; }
.pm-diff.mid { color: #b54708; background: #fffaeb; }
.pm-diff.hard { color: #b42318; background: #fef3f2; }
.pm-exam-bias { display: block; margin-top: 6px; color: #475467; font-size: 12px; line-height: 1.6; }
.pm-exam-bias i { margin-right: 4px; color: #1764f5; }


/* 作答 / 结果（沿用既有实现） */
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

@media (max-width: 1200px) {
  .stat-strip { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
@media (max-width: 900px) {
  .pm-exam { align-items: flex-start; flex-direction: column; gap: 10px; }
}
@media (max-width: 640px) {
  .practice-page { padding: 16px 12px 40px; }
  .stat-strip { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
</style>
