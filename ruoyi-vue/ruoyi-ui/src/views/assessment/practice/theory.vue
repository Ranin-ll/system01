<template>
  <div class="practice-page">
    <div class="exam-breadcrumb">
      <template v-if="stage === 'list'">
        <el-button type="text" icon="el-icon-arrow-left" @click="goBack">返回学习与考核</el-button>
        <span>/</span>
        <b>模拟理论考核</b>
      </template>
      <template v-else>
        <el-button type="text" icon="el-icon-arrow-left" @click="backToList">返回考核列表</el-button>
        <span>/</span>
        <b>{{ bankName || '模拟理论考核' }}</b>
      </template>
    </div>

    <!-- ============ 阶段一：理论模拟考核清单（列表）+ 理论自测记录 ============ -->
    <template v-if="stage === 'list'">
      <header class="exam-heading">
        <div>
          <span class="eyebrow">MOCK · THEORY</span>
          <h1>模拟理论考核</h1>
          <p>本部门已发布的模拟理论考核，随时自测；交卷即出正确率与逐题解析，不计入转正成绩。</p>
        </div>
        <el-button size="medium" icon="el-icon-refresh" :loading="loading" @click="reload">刷新</el-button>
      </header>

      <!-- ① 考核清单：列表形式，直接列出各套考核（不再有「模块」层级） -->
      <section class="pm-card">
        <div class="pm-head">
          <div class="pm-title"><span class="pm-idx">理</span><h3>理论模拟考核</h3></div>
          <span class="pm-hint">{{ exams.length }} 套</span>
        </div>
        <div v-if="loading && !exams.length" class="pm-empty small"><i class="el-icon-loading" /><span>正在加载…</span></div>
        <div v-else-if="!exams.length" class="pm-empty small"><i class="el-icon-document" /><span>本部门暂未发布模拟理论考核</span></div>
        <div v-else class="pm-table-wrap" style="padding-top:6px">
          <table class="pm-table">
            <thead>
              <tr>
                <th>考核名称</th>
                <th>题型构成</th>
                <th>满分</th>
                <th>通过线</th>
                <th>限时</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="e in exams" :key="e.id">
                <td><b>{{ e.examName || '理论模拟自测' }}</b></td>
                <td>
                  共 {{ e.questionCount || 0 }} 题（单选 {{ e.singleCount || 0 }}
                  · 多选 {{ e.multiCount || 0 }} · 判断 {{ e.judgeCount || 0 }}）
                </td>
                <td>{{ e.fullScore != null ? e.fullScore : '--' }} 分</td>
                <td>{{ e.passLine != null ? e.passLine : 0 }} 分</td>
                <td>{{ Number(e.duration) > 0 ? e.duration + ' 分钟' : '不限时' }}</td>
                <td>
                  <el-button
                    v-if="!isFormal"
                    type="primary"
                    size="mini"
                    icon="el-icon-caret-right"
                    :loading="starting"
                    @click="startPractice(e)"
                  >开始自测</el-button>
                  <span v-else class="pm-hint">转正后仅可查看记录</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- ② 理论自测记录 -->
      <section class="pm-card">
        <div class="pm-head">
          <div class="pm-title"><span class="pm-idx">录</span><h3>理论自测记录</h3></div>
        </div>
        <div v-loading="loading" class="pm-table-wrap" style="padding-top:12px">
          <div v-if="!records.length && !loading" class="pm-empty">
            <i class="el-icon-tickets" />
            <span>还没有自测记录，选一套考核开始吧</span>
          </div>
          <table v-else class="pm-table">
            <thead>
              <tr><th>时间</th><th>考核</th><th>题数</th><th>正确率</th><th>操作</th></tr>
            </thead>
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

    <!-- ================= 阶段二：作答 ================= -->
    <template v-else-if="stage === 'exam'">
      <header class="exam-heading">
        <div>
          <span class="eyebrow">ANSWERING</span>
          <h1>{{ bankName || '模拟理论考核' }}</h1>
          <p>共 {{ questions.length }} 题</p>
        </div>
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

    <!-- ================= 阶段三：交卷结果（含逐题回顾） ================= -->
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
              <div
                v-for="opt in parseOptions(item.optionsJson)"
                :key="opt.key"
                class="qa-opt"
                :class="{ correct: isCorrectOpt(item, opt.key), picked: isPicked(item, opt.key) }"
              >
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
          <el-button size="medium" @click="backToList">返回考核列表</el-button>
          <el-button v-if="!isFormal && currentExamId" type="primary" size="medium" @click="restartSameExam">再练一次</el-button>
        </div>
      </div>
    </template>
  </div>
</template>

<script>
/**
 * 模拟理论考核（2026-09-23 由 practice/index.vue 拆出，独立成页）
 *
 * 与原「模拟考核」页的差别：
 *  1. **去掉「模块」层级** —— 原来要先选模块、再在模块里选考核；现在直接列出本部门
 *     已发布的全部模拟理论考核（后端 `moduleId` 传空即不过滤），一套一套自测。
 *  2. 清单改成**列表（表格）形式**：考核名称 / 题型构成 / 满分 / 通过线 / 限时 / 操作。
 *  3. 实操题从本页移出，独立为「模拟实操考核」页（卡片形式）。
 *
 * 三个阶段仍在一页内：list 清单+记录 → exam 作答（含倒计时）→ result 得分与逐题回顾。
 * 「再练一次」由回顾页带 `?start=1[&examId=]` 跳回本页，`created` 里自动开考。
 */
import {
  startPractice, submitPractice, myPracticeRecords, listPracticeExams
} from '@/api/business/practice'
import practiceMixin from './practice-mixin'
import { mapGetters } from 'vuex'

export default {
  name: 'InternMockTheory',
  mixins: [practiceMixin],
  data() {
    return {
      /** list 清单+自测记录 / exam 作答 / result 结果 */
      stage: 'list',
      loading: false,
      /** 本部门已发布的模拟理论考核（不再按模块过滤） */
      exams: [],
      records: [],
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
    pagedRecords() {
      return this.pageSlice(this.records, this.recordPage, this.recordPageSize)
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
      this.loadExams()
      this.loadRecords()
    },
    /** 考核清单：不传 moduleId —— 后端按「本部门 + 已发布 + 模拟理论」返回全部 */
    loadExams() {
      this.loading = true
      listPracticeExams().then(res => {
        this.exams = res.data || []
        this.loading = false
      }).catch(() => {
        this.exams = []
        this.loading = false
      })
    },
    loadRecords() {
      this.loading = true
      myPracticeRecords().then(res => {
        this.records = res.data || []
        this.loading = false
        this.recordPage = 1
      }).catch(() => { this.loading = false })
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
    /** 清单里某套考核「开始自测」 */
    startPractice(exam) {
      this.startPracticeById(exam ? exam.id : null)
    },
    /** 从记录页「再练一次」回到页面时按考核ID直接开考 */
    restartSameExam() {
      this.startPracticeById(this.currentExamId)
    },
    startPracticeById(examId) {
      if (this.isFormal) {
        this.$modal.msgWarning('正式实习生仅可查看转正前的模拟理论考核记录')
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
          this.backToList()
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
    /** 回到考核清单（并刷新记录，刚交的这次要出现在下面） */
    backToList() {
      this.clearCountdown()
      this.remainingSeconds = 0
      this.stage = 'list'
      this.questions = []
      this.answers = []
      this.loadRecords()
    },
    /** 进入独立的回顾页查看当次题目与作答 */
    goRecordDetail(row) {
      this.$router.push('/assessment/intern/mock-exam/record/' + row.id)
    },
    /**
     * 返回来源页：「学习与考核」的**考核入口卡栏**（本页就是从那张「模拟理论考核」卡进来的）。
     * 原先写的是工作台 —— 二级页应该回它的上一级，而不是跳过上层直接回首页。
     */
    goBack() {
      this.$router.push({ path: '/assessment/intern/learning', hash: '#sec-entries' })
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
@import '~@/assets/styles/practice-module.scss';

/* ---- 作答页头部：倒计时与进度 ---- */
.exam-progress { width: 240px; }
.exam-head-right { display: flex; align-items: center; gap: 16px; }
.countdown { display: inline-flex; align-items: center; gap: 6px; padding: 6px 14px; color: #1764f5; background: #e8f1fd; font-size: 13px; border-radius: 18px; }
.countdown b { font-size: 16px; letter-spacing: .04em; font-variant-numeric: tabular-nums; }
.countdown.warn { color: #b54708; background: #fff4e5; }
.countdown.danger { color: #b42318; background: #fee4e2; animation: cd-blink 1s steps(2, start) infinite; }
@keyframes cd-blink { 50% { opacity: .55; } }

/* ---- 作答 ---- */
.question-card { margin-bottom: 16px; padding: 20px 22px; background: #fff; border: 1px solid #e7ecf3; border-radius: 8px; }
.q-head { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.q-index { display: inline-flex; width: 24px; height: 24px; align-items: center; justify-content: center; color: #fff; background: #1764f5; font-size: 13px; border-radius: 50%; }
.q-stem { margin-bottom: 14px; color: #1d2939; font-size: 15px; line-height: 1.7; }
.q-options .option-item { padding: 9px 12px; margin-bottom: 6px; border: 1px solid #edf0f4; border-radius: 6px; }
.q-options .option-item:hover { background: #f8fafc; }
.option-item .el-radio, .option-item .el-checkbox { display: block; margin-right: 0; line-height: 1.6; }
.submit-bar { display: flex; justify-content: flex-end; gap: 12px; margin-top: 8px; padding: 16px 0; }

/* ---- 结果（逐题回顾的 .qa-* 由公共样式提供，这里只补结果页外壳） ---- */
.result-card { padding: 30px; background: #fff; border: 1px solid #e7ecf3; border-radius: 10px; text-align: center; }
.result-score { display: flex; align-items: baseline; justify-content: center; gap: 8px; margin-bottom: 6px; }
.score-label { color: #667085; font-size: 14px; }
.score-num { color: #1764f5; font-size: 52px; font-weight: 700; line-height: 1; }
.score-total { color: #98a2b3; font-size: 20px; }
.result-desc { color: #667085; font-size: 14px; margin-bottom: 24px; }
.result-desc b.good { color: #23966f; }
.result-desc b.poor { color: #f56c6c; }
.result-actions { display: flex; justify-content: center; gap: 12px; margin-top: 24px; }
.result-card .qa-card { background: #fbfcfe; }

@media (max-width: 900px) {
  .pm-table th:first-child, .pm-table td:first-child { padding-left: 0; }
}
</style>
