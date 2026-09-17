<template>
  <div class="practice-page">
    <div class="exam-breadcrumb">
      <el-button type="text" icon="el-icon-arrow-left" @click="goBack">返回工作台</el-button>
      <span>/</span>
      <b>{{ isFormal ? '模拟考核记录' : '模拟考核' }}</b>
    </div>

    <!-- 阶段一：顶部统计 + 理论模拟 + 实操题库 -->
    <template v-if="stage === 'list'">
      <header class="exam-heading">
        <div>
          <span class="eyebrow">{{ isFormal ? 'PRACTICE HISTORY' : 'PRACTICE CENTER' }}</span>
          <h1>{{ isFormal ? '模拟考核记录' : '模拟考核' }}</h1>
          <p>{{ isFormal ? '转正前的模拟考核记录会继续保留，可逐题回看。' : '不计成绩 · 可重复练习 · 用于考前热身；交卷自动判分，成绩仅本人可见。' }}</p>
        </div>
        <el-button size="medium" icon="el-icon-refresh" @click="loadRecords">刷新</el-button>
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
          <span>实操题量</span>
          <b>{{ subjects.length }}<small>题</small></b>
          <div class="sub">{{ groups.length }} 个方向</div>
        </div>
        <div class="stat">
          <span>题库覆盖</span>
          <b>{{ bankCount }}<small> 个库</small></b>
          <div class="sub">{{ bankNames || '尚未产生记录' }}</div>
        </div>
        <div class="stat">
          <span>最近自测</span>
          <b class="sm-text">{{ lastRecord ? fmtTime(lastRecord.createTime) : '--' }}</b>
          <div class="sub" :class="{ ok: lastRecord }">{{ lastRecord ? '正确率 ' + rateOf(lastRecord) + '%' : '尚未开始' }}</div>
        </div>
      </div>

      <!-- ② 理论模拟 -->
      <section class="pm-card">
        <div class="pm-head">
          <div class="pm-title"><span class="pm-idx">理</span><h3>理论模拟</h3><span class="pm-hint">题库随机抽 10 题 · 不计成绩</span></div>
          <el-button v-if="!isFormal" type="primary" size="small" icon="el-icon-caret-right" @click="startPractice">开始新自测</el-button>
        </div>
        <div class="pm-grid">
          <div class="pm-col5">
            <div class="pm-flat">
              <template v-if="practiceConfig.configured">
                按本部门管理员配置的<b>知识分布 + 题型配比</b>抽题：共 <b>{{ configTotal }}</b> 题
                （单选 {{ practiceConfig.singleCount || 0 }} · 多选 {{ practiceConfig.multiCount || 0 }} · 判断 {{ practiceConfig.judgeCount || 0 }}），
                每题 1 分，通过线 <b>{{ practiceConfig.passLine || 0 }}</b> 分；不计成绩、可无限次重复。
              </template>
              <template v-else>
                从本部门模拟题库中<b>随机抽取 10 题</b>组成一套自测卷（单选 / 多选 / 判断），交卷后给出对错与解析；不计成绩，可无限次重复。
              </template>
            </div>
            <div class="pm-note" v-if="practiceConfig.configured && configPoints.length">
              知识分布：<span v-for="(p, i) in configPoints" :key="p.knowledgePoint">{{ i ? ' · ' : '' }}{{ p.knowledgePoint }} {{ p.questionCount }} 题</span>
            </div>
            <div class="pm-note">随机抽题所以每次题面不同；错题可进入回顾页逐题复看。</div>
          </div>
          <div class="pm-col7">
            <div v-loading="loading" class="pm-table-wrap">
              <div v-if="!records.length && !loading" class="pm-empty">
                <i class="el-icon-tickets" />
                <span>{{ isFormal ? '暂无转正前的模拟记录' : '还没有模拟记录，先来一次吧' }}</span>
              </div>
              <table v-else class="pm-table">
                <thead><tr><th>时间</th><th>题数</th><th>正确率</th><th>题库</th><th>操作</th></tr></thead>
                <tbody>
                  <tr v-for="row in records.slice(0, 5)" :key="row.id">
                    <td>{{ fmtTime(row.createTime) }}</td>
                    <td>{{ row.totalCount }} 题</td>
                    <td><b :class="rateTone(row)">{{ rateOf(row) }}%</b></td>
                    <td class="ellipsis">{{ row.bankName || '--' }}</td>
                    <td><el-button type="text" size="mini" @click="goRecordDetail(row)">查看详情</el-button></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </section>

      <!-- ③ 实操题库 -->
      <template v-if="!isFormal">
        <section class="pm-card">
          <div class="pm-hero">
            <div class="pm-hero-l">
              <span class="pm-badge-blue">模拟试题库</span>
              <h3>{{ deptName ? deptName + '模拟试题' : '本部门模拟试题' }}</h3>
              <p>选择一个题目开始练习，完成后可按题目要求提交成果进行自检。</p>
            </div>
            <div class="pm-hero-stats">
              <div><b>{{ subjects.length }}</b><span>可练习题目</span></div>
              <div><b>{{ groups.length }}</b><span>方向分组</span></div>
              <div><b>{{ maxMinutes }}<small> 分钟</small></b><span>最长用时</span></div>
            </div>
          </div>

          <div v-if="loading && !subjects.length" class="pm-empty small"><i class="el-icon-loading" /><span>正在加载实操题…</span></div>
          <div v-else-if="!subjects.length" class="pm-empty small"><i class="el-icon-document" /><span>本部门暂未发布模拟实操题</span></div>

          <div v-for="g in groups" :key="g.name" class="pm-grp">
            <div class="pm-grp-head">
              <div><h4>{{ g.name }}</h4><p>{{ g.desc }}</p></div>
              <span class="pm-hint">{{ g.items.length }} 题</span>
            </div>
            <div class="pm-qgrid">
              <button v-for="(s, i) in g.items" :key="s.id" type="button" class="pm-qcard" @click="goSubjectDetail(s)">
                <span class="pm-thumb">
                  <img v-if="firstImage(s)" :src="baseApi + firstImage(s)" :alt="s.title">
                  <span v-else class="pm-thumb-ph" v-html="thumbSvg(i)" />
                </span>
                <span class="pm-qc-b">
                  <b>{{ s.title || '未命名实操题' }}</b>
                  <span class="pm-qc-f"><span>建议用时</span><b>{{ s.estimatedMinutes ? s.estimatedMinutes + ' 分钟' : '不限' }}</b></span>
                </span>
              </button>
            </div>
          </div>
          <div class="pm-note">实操题库为部门已发布内容，仅供查看题干、交付要求与附件，不会提交给管理员；点卡片打开完整题目。</div>
        </section>
      </template>
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
          <el-button size="medium" @click="goList">返回列表</el-button>
          <el-button v-if="!isFormal" type="primary" size="medium" @click="startPractice">再练一次</el-button>
        </div>
      </div>
    </template>
  </div>
</template>

<script>
import { startPractice, submitPractice, myPracticeRecords, listPracticeSubjects } from '@/api/business/practice'
import practiceMixin from './practice-mixin'
import { mapGetters } from 'vuex'

export default {
  name: 'InternPractice',
  mixins: [practiceMixin],
  data() {
    return {
      stage: 'list',
      loading: false,
      records: [],
      subjects: [],
      bankId: null,
      bankName: '',
      questions: [],
      answers: [],
      result: { totalCount: 0, correctCount: 0, details: [] },
      submitting: false,
      // 本部门管理员配置的模拟考核（后端按配置抽题，页面展示配置口径）
      practiceConfig: { configured: false, singleCount: 0, multiCount: 0, judgeCount: 0, passLine: null },
      configPoints: []
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
    bankCount() {
      const ids = {}
      this.records.forEach(r => { if (r.bankId) ids[r.bankId] = 1 })
      return Object.keys(ids).length
    },
    bankNames() {
      const names = []
      this.records.forEach(r => { if (r.bankName && names.indexOf(r.bankName) < 0) names.push(r.bankName) })
      return names.slice(0, 2).join(' / ')
    },
    /** 实操题按方向分组（后端 direction / directionDesc 真实字段） */
    configTotal() {
      return (this.practiceConfig.singleCount || 0) + (this.practiceConfig.multiCount || 0) + (this.practiceConfig.judgeCount || 0)
    },
    groups() {
      const map = {}
      this.subjects.forEach(s => {
        const name = s.direction || '未分组方向'
        if (!map[name]) map[name] = { name, desc: s.directionDesc || '', items: [] }
        map[name].items.push(s)
      })
      return Object.keys(map).map(k => map[k])
    },
    maxMinutes() {
      return this.subjects.reduce((max, s) => Math.max(max, Number(s.estimatedMinutes) || 0), 0) || '--'
    }
  },
  created() {
    this.loadRecords()
    if (!this.isFormal) this.loadSubjects()
    // 从回顾页「再练一次」跳回时自动开始抽题（先清掉 query，避免刷新重复触发）
    if (!this.isFormal && this.$route.query.start === '1') {
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
    loadSubjects() {
      this.loading = true
      listPracticeSubjects().then(res => {
        this.subjects = res.data || []
        this.loading = false
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
    /** 实操题参考图（referenceImages 存 JSON 数组） */
    firstImage(subject) {
      const list = this.parseAttachments(subject.referenceImages)
      const first = list.length ? list[0] : null
      if (!first) return ''
      return typeof first === 'string' ? first : (first.url || '')
    },
    /** 无参考图时用内联 SVG 占位（与设计稿缩略图一致） */
    thumbSvg(i) {
      const layouts = [
        '<svg viewBox="0 0 160 104" preserveAspectRatio="none"><rect x="14" y="18" width="132" height="70" rx="8" fill="#f2f7ff"/><rect x="14" y="18" width="132" height="15" rx="8" fill="#d7e7fc"/><rect x="26" y="42" width="46" height="6" rx="3" fill="#cfe0fb"/><rect x="26" y="54" width="70" height="6" rx="3" fill="#cfe0fb"/><rect x="26" y="66" width="34" height="6" rx="3" fill="#cfe0fb"/></svg>',
        '<svg viewBox="0 0 160 104" preserveAspectRatio="none"><rect x="26" y="30" width="108" height="16" rx="8" fill="#d7e7fc"/><rect x="26" y="48" width="108" height="16" rx="8" fill="#cfe0fb"/><rect x="26" y="66" width="108" height="16" rx="8" fill="#e0ebfd"/></svg>',
        '<svg viewBox="0 0 160 104" preserveAspectRatio="none"><rect x="14" y="18" width="132" height="70" rx="8" fill="#f2f7ff"/><rect x="14" y="18" width="132" height="16" rx="8" fill="#e0ebfd"/><rect x="24" y="46" width="112" height="10" rx="4" fill="#cfe0fb"/><rect x="24" y="62" width="52" height="18" rx="4" fill="#dbe8fc"/><rect x="84" y="62" width="52" height="18" rx="4" fill="#e0ebfd"/></svg>'
      ]
      return layouts[i % layouts.length]
    },
    startPractice() {
      if (this.isFormal) {
        this.$modal.msgWarning('正式实习生仅可查看转正前的模拟考核记录')
        return
      }
      this.loading = true
      startPractice().then(res => {
        const data = res.data
        this.bankId = data.bankId
        this.bankName = data.bankName
        this.practiceConfig = {
          configured: !!data.configured,
          singleCount: data.singleCount || 0,
          multiCount: data.multiCount || 0,
          judgeCount: data.judgeCount || 0,
          passLine: data.passLine
        }
        this.configPoints = data.points || []
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
    /** 打开实操题详情页（与题库卡片同一落点） */
    goSubjectDetail(subject) {
      this.$router.push('/assessment/intern/practice-subject/' + subject.id)
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
.exam-heading { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 18px; }
.eyebrow { color: #1764f5; font-size: 11px; letter-spacing: .08em; }
.exam-heading h1 { margin: 5px 0 7px; color: #1d2939; font-size: 26px; font-weight: 600; }
.exam-heading p { margin: 0; color: #667085; font-size: 13px; }
.exam-progress { width: 240px; }

/* ① 统计条 */
.stat-strip { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap: 12px; margin-bottom: 14px; }
.stat { min-width: 0; padding: 14px 15px; background: #fff; border: 1px solid #e7ecf3; border-radius: 8px; }
.stat > span { color: #8490a0; font-size: 12px; }
.stat > b { display: block; margin: 9px 0 6px; color: #1d2939; font-size: 22px; font-weight: 600; }
.stat > b.sm-text { font-size: 15px; }
.stat > b small { margin-left: 3px; color: #98a2b3; font-size: 12px; font-weight: 400; }
.stat .sub { color: #98a2b3; font-size: 11px; }
.stat .sub.ok { color: #067647; }
.spark { display: block; width: 90px; height: 22px; margin-top: 2px; }

/* ② / ③ 卡片与两栏 */
.pm-card { margin-bottom: 14px; padding: 18px 20px 20px; background: #fff; border: 1px solid #e7ecf3; border-radius: 8px; }
.pm-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding-bottom: 14px; border-bottom: 1px solid #edf0f4; }
.pm-title { display: flex; align-items: center; gap: 9px; }
.pm-title h3 { margin: 0; color: #1d2939; font-size: 17px; font-weight: 600; }
.pm-idx { display: inline-flex; width: 24px; height: 24px; align-items: center; justify-content: center; color: #1764f5; background: #e8f1fd; font-size: 12px; font-weight: 700; border-radius: 4px; }
.pm-hint { color: #98a2b3; font-size: 12px; }
.pm-grid { display: grid; grid-template-columns: minmax(0, 5fr) minmax(0, 7fr); gap: 18px; padding-top: 16px; }
.pm-flat { padding: 12px 14px; color: #475467; background: #f8fafc; border: 1px solid #eef1f5; border-radius: 6px; font-size: 12.5px; line-height: 1.7; }
.pm-note { margin-top: 10px; color: #98a2b3; font-size: 11.5px; line-height: 1.6; }
.pm-table-wrap { min-height: 150px; }
.pm-table { width: 100%; border-collapse: collapse; }
.pm-table th { padding: 9px 10px; color: #8490a0; background: #f8fafc; font-size: 12px; font-weight: 500; text-align: left; }
.pm-table td { padding: 10px; border-bottom: 1px solid #edf0f4; color: #475467; font-size: 12.5px; }
.pm-table td b.good { color: #067647; }
.pm-table td b.mid { color: #d9930d; }
.pm-table td b.poor { color: #b54708; }
.pm-table td.ellipsis { max-width: 150px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.pm-empty { display: flex; min-height: 150px; align-items: center; justify-content: center; flex-direction: column; gap: 8px; color: #98a2b3; }
.pm-empty.small { min-height: 110px; }
.pm-empty i { color: #b7c1cc; font-size: 32px; }
.pm-empty span { font-size: 13px; }

/* ③ 实操题库 */
.pm-hero { display: flex; align-items: center; justify-content: space-between; gap: 24px; padding: 18px 20px; margin-bottom: 16px; border-radius: 8px; background: linear-gradient(96deg, #eef4ff 0%, #f7fbff 100%); }
.pm-hero-l h3 { margin: 8px 0 6px; color: #1d2939; font-size: 19px; font-weight: 600; }
.pm-hero-l p { margin: 0; color: #667085; font-size: 12.5px; }
.pm-badge-blue { display: inline-block; padding: 2px 9px; color: #1764f5; background: #e2ecff; font-size: 11px; border-radius: 10px; }
.pm-hero-stats { display: flex; gap: 26px; }
.pm-hero-stats div { text-align: center; }
.pm-hero-stats b { display: block; color: #1764f5; font-size: 20px; font-weight: 600; }
.pm-hero-stats b small { font-size: 12px; font-weight: 400; }
.pm-hero-stats span { color: #8490a0; font-size: 11px; }
.pm-grp { margin-bottom: 18px; }
.pm-grp-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 11px; }
.pm-grp-head h4 { margin: 0 0 4px; color: #1d2939; font-size: 14.5px; font-weight: 600; }
.pm-grp-head p { margin: 0; color: #98a2b3; font-size: 11.5px; }
.pm-qgrid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.pm-qcard { padding: 0; overflow: hidden; text-align: left; background: #fff; border: 1px solid #e7ecf3; border-radius: 8px; cursor: pointer; transition: border-color .15s, box-shadow .15s; }
.pm-qcard:hover { border-color: #a9c8f7; box-shadow: 0 6px 16px rgba(23, 100, 245, .08); }
.pm-thumb { display: block; height: 104px; background: #f4f7fc; }
.pm-thumb img { width: 100%; height: 100%; object-fit: cover; }
.pm-thumb-ph { display: block; height: 100%; }
.pm-thumb-ph ::v-deep svg { width: 100%; height: 100%; }
.pm-qc-b { display: block; padding: 11px 13px 13px; }
.pm-qc-b > b { display: block; color: #1d2939; font-size: 13px; font-weight: 600; line-height: 1.5; }
.pm-qc-f { display: flex; align-items: center; justify-content: space-between; margin-top: 9px; color: #98a2b3; font-size: 11.5px; }
.pm-qc-f b { color: #1764f5; font-weight: 600; }

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
  .pm-qgrid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 900px) {
  .pm-grid { grid-template-columns: 1fr; }
  .pm-hero { align-items: flex-start; flex-direction: column; }
}
@media (max-width: 640px) {
  .practice-page { padding: 16px 12px 40px; }
  .stat-strip { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .pm-qgrid { grid-template-columns: 1fr; }
}
</style>
