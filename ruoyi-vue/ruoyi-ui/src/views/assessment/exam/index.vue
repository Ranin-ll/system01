<template>
  <div class="exam-page">
    <div class="exam-breadcrumb">
      <el-button type="text" icon="el-icon-arrow-left" @click="goBack">返回工作台</el-button>
      <span>/</span>
      <b>{{ isFormal ? '考核记录' : '正式考核' }}</b>
    </div>

    <!-- 阶段一：发布信息 + 考核场次 + 备考资料 + 资格校验 -->
    <template v-if="stage === 'list'">
      <header class="exam-heading">
        <div>
          <span class="eyebrow">{{ isFormal ? 'ASSESSMENT HISTORY' : 'FORMAL ASSESSMENT' }}</span>
          <h1>{{ isFormal ? '考核记录' : '正式考核' }}</h1>
        </div>
        <span v-if="!isFormal" class="head-badge">{{ headBadge }}</span>
      </header>

      <!-- ① 发布信息条 -->
      <section v-if="!isFormal" class="pub-strip">
        <div class="pub-fact"><span>当前批次</span><b>{{ currentBatchName }}</b></div>
        <div class="pub-fact"><span>发布部门</span><b>{{ deptName || '所属部门' }}</b></div>
        <div class="pub-fact"><span>场次</span><b>{{ sessions.length }} 场</b></div>
        <div class="pub-fact"><span>我已参加</span><b>{{ attendedCount }} 场</b></div>
        <div class="pub-fact right"><span>状态</span><b :class="currentBatchState.tone">{{ currentBatchState.text }}</b></div>
      </section>

      <!-- ② 考核场次 -->
      <section v-if="!isFormal" class="pm-card">
        <div class="pm-head">
          <div class="pm-title">
            <span class="pm-idx">场</span>
            <h3>考核场次</h3>
          </div>
          <el-button size="mini" icon="el-icon-refresh" @click="loadList">刷新</el-button>
        </div>

        <div v-if="!sessions.length" class="pm-empty"><i class="el-icon-document" /><span>部门尚未发布正式考核</span></div>

        <div v-else class="session-grid">
          <div v-for="s in sessions" :key="s.key" class="session-card" :class="s.state.cardTone">
            <div class="session-head">
              <b>{{ s.name }}</b>
              <span class="session-badge" :class="s.state.badgeTone">{{ s.state.label }}</span>
            </div>
            <div class="session-window">
              截止时间：{{ s.windowText }}
              <el-tag v-if="s.expired" size="mini" type="info" effect="plain" style="margin-left:6px">已截止</el-tag>
              <el-tag v-if="s.assigned" size="mini" type="warning" effect="plain" style="margin-left:6px">指定人员</el-tag>
            </div>
            <div class="session-rows">
              <div v-for="exam in s.exams" :key="exam.examId" class="session-row">
                <span>{{ exam.examType === 'PRACTICAL' ? '实操考核' : '理论考核' }}</span>
                <b :class="stageTone(exam)">{{ stageText(exam) }}</b>
              </div>
            </div>
            <div class="session-foot">
              <span class="session-meta">{{ s.footText }}</span>
              <el-button
                v-if="s.action"
                size="mini"
                :type="s.action.primary ? 'primary' : 'default'"
                :plain="!s.action.primary"
                :disabled="s.action.disabled"
                @click="s.action.run()"
              >{{ s.action.label }}</el-button>
            </div>
          </div>
        </div>
      </section>

      <!-- ③ 备考资料 -->
      <section v-if="!isFormal" class="pm-card">
        <div class="pm-head">
          <div class="pm-title"><span class="pm-idx">资</span><h3>备考资料</h3></div>
          <el-button size="mini" icon="el-icon-notebook-2" @click="goGuide">打开备考资料页</el-button>
        </div>
        <div class="file-row">
          <span class="file-tag">PDF</span>
          <div class="file-name"><b>考试指南与考核规则</b><span>备考资料统一在「备考资料」页签查看与下载</span></div>
          <el-button size="mini" plain @click="goGuide">前往查看</el-button>
        </div>
      </section>

      <!-- ④ 资格校验 -->
      <div v-if="!isFormal" class="trip-grid">
        <section class="pm-card">
          <div class="pm-head compact">
            <div class="pm-title"><span class="pm-idx warn">4</span><h3>资格校验</h3></div>
            <span class="session-badge" :class="qualified ? 'ok' : 'warn'">{{ qualified ? '已通过' : '未通过' }}</span>
          </div>
          <div class="chk-list">
            <div class="chk">
              <span class="mark" :class="learningProgress >= 70 ? 'ok' : 'no'">{{ learningProgress >= 70 ? '✓' : '!' }}</span>
              <span>必修完成率 ≥ 70%</span>
              <b :class="learningProgress >= 70 ? 'good' : 'warn'">当前 {{ learningProgress }}%</b>
            </div>
            <div class="chk">
              <span class="mark" :class="Number(protocolStatus) === 1 ? 'ok' : 'no'">{{ Number(protocolStatus) === 1 ? '✓' : '!' }}</span>
              <span>保密协议已签署</span>
              <b>{{ Number(protocolStatus) === 1 ? '已签署' : '待签署' }}</b>
            </div>
            <div class="chk">
              <span class="mark ok">✓</span>
              <span>剩余考试次数</span>
              <b>{{ remainTimes === null ? '--' : remainTimes + ' 次' }}</b>
            </div>
          </div>
        </section>
      </div>

      <!-- 历史场次（已参加的考核；通过与否都可查看详情） -->
      <section v-if="isFormal || recordExams.length || sessions.length === 0" class="pm-card">
        <div class="pm-head">
          <div class="pm-title"><span class="pm-idx">录</span><h3>考核记录</h3></div>
          <el-button size="mini" icon="el-icon-refresh" @click="loadList">刷新</el-button>
        </div>
        <div v-if="!recordExams.length" class="pm-empty"><i class="el-icon-document" /><span>{{ isFormal ? '暂无历史考核记录' : '暂无考核记录，先在上方场次里参加考核' }}</span></div>
        <table v-else class="pm-table">
          <thead><tr><th>考核名称</th><th>类别</th><th>状态</th><th>成绩</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="exam in recordExams" :key="exam.examId">
              <td class="ellipsis">{{ exam.examName }}</td>
              <td>{{ exam.examType === 'PRACTICAL' ? '实操' : '理论' }}</td>
              <td><el-tag size="mini" effect="plain" :type="rowState(exam).tag">{{ rowState(exam).text }}</el-tag></td>
              <td>{{ exam.sheet && exam.sheet.status === 'PUBLISHED' ? exam.sheet.finalScore : '--' }}</td>
              <td><el-button type="text" size="mini" @click="goResult({ exams: [exam] })">{{ exam.sheet && exam.sheet.status === 'PUBLISHED' ? '查看成绩' : '查看详情' }}</el-button></td>
            </tr>
          </tbody>
        </table>
      </section>
    </template>

  </div>
</template>

<script>
import { myExamList } from '@/api/business/exam'
import { listLearningCourses } from '@/api/business/learning'
import { learningSummary, formatLearningDuration } from '@/utils/learningPreview'
import { mapGetters } from 'vuex'

/** 剩余考试次数：批次规则未接接口 ⇒ 留空（不再用固定 2 次假装） */
const DEMO_REMAIN_TIMES = null

export default {
  name: 'InternExam',
  data() {
    return {
      // ★ 2026-09-22：作答环节已拆到独立的**全屏作答页**（views/assessment/exam/answering.vue，
      //   路由 /assessment/intern/exam-answering）。本页从此只负责「列表」这一段；
      //   stage 保留，只为让 refreshSilently 的「只在列表页轮询」判断继续成立。
      stage: 'list',
      loading: false,
      exams: [],
      /** 列表成绩静默轮询定时器（发布成绩后自动刷新用） */
      refreshTimer: null,
      learningOverview: { progress: null, courseCount: 0, completedCourses: 0, learningCourses: 0, completedItems: 0, itemCount: 0, lastStudyTime: '尚未开始' }
    }
  },
  computed: {
    ...mapGetters(['roles', 'protocolStatus', 'deptName']),
    baseApi() { return process.env.VUE_APP_BASE_API || '' },
    isFormal() { return this.roles.indexOf('FORMAL_TRAINEE') > -1 },
    /**
     * 考核记录：本人真正参加过的考核（有答卷且已交卷）。
     * 通过与否都进这张表，未通过的也能点「查看详情」进结果页。
     */
    recordExams() {
      return this.exams.filter(exam => exam.sheet && exam.sheet.status !== 'IN_PROGRESS')
    },
    examMode() { return this.$route.name === 'InternMockExam' ? 'PRACTICE' : 'FORMAL' },
    learningProgress() { return this.learningOverview.progress === null ? 0 : this.learningOverview.progress },
    learningGap() { return Math.max(0, 70 - this.learningProgress) },
    qualified() { return this.learningProgress >= 70 && Number(this.protocolStatus) === 1 },
    remainTimes() { return DEMO_REMAIN_TIMES },
    /** 场次分组：考核名称里的批次 / 期次标识（示例解析，后端补批次字段后可直取） */
    sessions() {
      const map = {}
      this.exams.forEach(exam => {
        const key = this.batchKeyOf(exam)
        if (!map[key]) map[key] = { key, name: key, exams: [] }
        map[key].exams.push(exam)
      })
      return Object.keys(map).map(key => this.decorateSession(map[key]))
    },
    attendedCount() { return this.sessions.filter(s => s.attended).length },
    passedCount() { return this.sessions.filter(s => s.passed).length },
    currentBatchName() {
      const running = this.sessions.find(s => s.running)
      return (running && running.name) || (this.sessions.length ? this.sessions[0].name : '暂未发布')
    },
    currentBatchState() {
      const running = this.sessions.find(s => s.running)
      if (running) return { text: running.state.text, tone: running.state.tone }
      if (!this.sessions.length) return { text: '暂未发布', tone: 'muted' }
      return { text: this.sessions[0].state.text, tone: this.sessions[0].state.tone }
    },
    headBadge() {
      const running = this.sessions.find(s => s.running)
      return running ? '● ' + running.name + ' 进行中' : '● 暂无进行中场次'
    }
  },
  created() {
    this.loadList()
    if (!this.isFormal) this.loadLearning()
  },
  mounted() {
    // 管理员发布成绩后，实习生这边要"及时"看到分数：
    // ① 回到本页（keep-alive 激活）/ 切回浏览器标签 / 窗口重新聚焦时静默拉一次；
    // ② 停留在列表页时每 30s 轮询一次。
    this.refreshTimer = setInterval(this.refreshSilently, 30000)
    document.addEventListener('visibilitychange', this.onVisible)
    window.addEventListener('focus', this.onFocus)
  },
  activated() {
    // 本页可能被 keep-alive 缓存，切回页签时重新取一次成绩
    this.refreshSilently()
  },
  beforeDestroy() {
    window.removeEventListener('focus', this.onFocus)
    document.removeEventListener('visibilitychange', this.onVisible)
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer)
      this.refreshTimer = null
    }
  },
  methods: {
    loadList() {
      this.loading = true
      myExamList(this.examMode).then(res => {
        this.exams = res.data || []
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    /** 静默刷新：不显示 loading、不打断答题界面，仅在列表页生效 */
    refreshSilently() {
      if (this.stage !== 'list' || document.hidden) return
      myExamList(this.examMode).then(res => {
        this.exams = res.data || []
      }).catch(() => {})
    },
    onVisible() {
      if (!document.hidden) this.refreshSilently()
    },
    onFocus() {
      this.refreshSilently()
    },
    loadLearning() {
      listLearningCourses().then(res => {
        this.learningOverview = learningSummary(res.data || [])
      }).catch(() => {
        this.learningOverview = learningSummary([])
      })
    },
    /**
     * 解析后端时间串（兼容 "2026-10-20T18:00:00" 与 "2026-10-20 18:00:00"）。
     * 统一换成 "y/m/d h:m:s" 再交给 Date，避免部分浏览器把带 "-" 的日期串当 UTC 或直接判为 Invalid。
     */
    parseTime(v) {
      if (!v) return null
      const t = new Date(String(v).replace('T', ' ').replace(/-/g, '/')).getTime()
      return isNaN(t) ? null : t
    },
    /**
     * 是否已过截止时间（后端 startExam 也会拦，这里让列表同步显示，避免"能点但点了报错"）。
     * **优先用后端下发的 `expired`**——它按服务器时间算，不受本机时钟偏差影响；
     * 仅在字段缺失时（老接口）才回退到客户端时间。
     */
    isExpired(exam) {
      if (exam && typeof exam.expired === 'boolean') return exam.expired
      const t = this.parseTime(exam && exam.endTime)
      return t !== null && t < Date.now()
    },
    /** 截止时间展示：2026-10-20 18:00；未设置截止时间则「不限」 */
    fmtDeadline(end) {
      return end ? String(end).replace('T', ' ').slice(0, 16) : '不限'
    },
    batchKeyOf(exam) {
      const name = exam.examName || '未命名场次'
      const matched = name.match(/(\d{4}Q\d)|(第[一二三四五六七八九十\d]+[期批场])/)
      return matched ? matched[0] : name
    },
    /** 场次状态机：已通过 / 待批阅 / 进行中 / 已截止 / 已结束 / 未解锁 */
    decorateSession(session) {
      // 作答中(IN_PROGRESS)的答卷视为「未提交」：答题时离开/刷新页面不保留进度、不交卷，
      // 遗留的 IN_PROGRESS 答卷不计入「已参加」，回到列表仍显示「开始考试」（后端 startExam 会作废重开）。
      const sheets = session.exams.map(e => e.sheet).filter(s => s && s.status !== 'IN_PROGRESS')
      const published = sheets.filter(s => s.status === 'PUBLISHED')
      // 「已通过」必须等本场次**所有**环节都出分且都通过：
      // 只看 published 会让"一个环节已发布通过、另一个还在批阅"的场次提前显示已通过，
      // 管理员发布另一环节后分数变了，列表看起来就像"没更新"。
      const allPublished = sheets.length > 0 && sheets.length === session.exams.length &&
        published.length === sheets.length
      const passed = allPublished && published.every(s => s.passFlag === 1)
      const pending = sheets.some(s => s.status !== 'PUBLISHED')
      // 已过截止时间的考核不再算「可开考」——否则按钮可点、点下去才被后端以「该考核已截止」拒绝
      const openExam = session.exams.find(e =>
        e.status === 'PUBLISHED' && (!e.sheet || e.sheet.status === 'IN_PROGRESS') && !this.isExpired(e))
      const ended = session.exams.length > 0 && session.exams.every(e => e.status !== 'PUBLISHED' && (!e.sheet || e.sheet.status === 'IN_PROGRESS'))
      const attended = sheets.length > 0
      const running = !attended && !!openExam
      // 「已截止」只在本场次还有过期考核、且已无任何可开考的考核时成立
      const expired = !attended && !openExam && session.exams.some(e => e.status === 'PUBLISHED' && this.isExpired(e))

      let state
      if (passed) state = { label: '已通过', tone: 'good', cardTone: 'done', badgeTone: 'ok' }
      else if (attended && pending) state = { label: '待批阅', tone: 'warn', cardTone: '', badgeTone: 'warn' }
      else if (running) state = { label: '进行中', tone: 'info', cardTone: 'running', badgeTone: 'info' }
      else if (expired) state = { label: '已截止', tone: 'muted', cardTone: '', badgeTone: 'muted' }
      else if (attended) state = { label: '已结束', tone: 'muted', cardTone: '', badgeTone: 'muted' }
      else if (ended) state = { label: '已结束', tone: 'muted', cardTone: '', badgeTone: 'muted' }
      else state = { label: '未开始', tone: 'muted', cardTone: 'idle', badgeTone: 'muted' }

      // 截止时间：同一场次有多个考核时取**最早（最紧）**的那个；全都没设则「不限」
      let deadlineRaw = null
      let deadlineTs = null
      session.exams.forEach(e => {
        const ts = this.parseTime(e.endTime)
        if (ts !== null && (deadlineTs === null || ts < deadlineTs)) {
          deadlineTs = ts
          deadlineRaw = e.endTime
        }
      })

      let footText
      // 已出分环节的得分合计：把数字直接摆到场次卡上，管理员发布后一眼能看到变化
      const scored = published.reduce((acc, s) => acc + (Number(s.finalScore) || 0), 0)
      if (passed) footText = '综合结果 已通过 · 得分合计 ' + scored
      else if (attended) footText = published.length
        ? '已出分 ' + published.length + '/' + sheets.length + ' 项 · 得分合计 ' + scored
        : '已完成 ' + sheets.length + ' 项考核'
      else if (running) footText = this.remainTimes === null ? '进行中' : '剩余次数 ' + this.remainTimes + ' 次'
      else if (expired) footText = '已过截止时间'
      else footText = '未解锁'

      const assignedOnly = session.exams.some(e => e.assigned)
      return Object.assign({}, session, {
        state,
        attended,
        passed,
        running,
        expired,
        assignedOnly,
        windowText: deadlineRaw ? this.fmtDeadline(deadlineRaw) : '不限',
        footText,
        action: this.buildAction(session, { passed, running, attended, openExam, expired })
      })
    },
    buildAction(session, ctx) {
      // 已参加过的场次一律可点开看详情：通过的看成绩，未通过的看作答与批阅明细。
      // 以前这里对 attended 直接 disabled，导致"没通过的考核看不到任何详情"。
      if (ctx.attended) {
        return { label: ctx.passed ? '查看成绩' : '查看详情', primary: false, disabled: false, run: () => this.goResult(session) }
      }
      if (ctx.expired) return { label: '已截止', primary: false, disabled: true, run: () => {} }
      // 【临时】停用资质审核门槛（学习进度≥70% + 保密协议），开放考试入口；提交前请还原为 qualified 判断
      if (ctx.openExam) {
        return { label: '开始考试', primary: true, disabled: false, run: () => this.goAnswering(ctx.openExam) }
      }
      if (ctx.running) return { label: '查看成绩', primary: false, disabled: false, run: () => this.goResult(session) }
      return { label: '未开始', primary: false, disabled: true, run: () => {} }
    },
    stageText(exam) {
      if (!exam) return '未发布'
      if (exam.sheet && exam.sheet.status === 'PUBLISHED') {
        return (exam.sheet.passFlag === 1 ? '已通过 ' : '未通过 ') + exam.sheet.finalScore + ' 分'
      }
      if (exam.sheet) return '批阅中'
      if (exam.status === 'PUBLISHED') return this.isExpired(exam) ? '已截止' : '已开放 · 可参加'
      return '未发布'
    },
    stageTone(exam) {
      if (!exam) return 'muted'
      if (exam.sheet && exam.sheet.status === 'PUBLISHED') return exam.sheet.passFlag === 1 ? 'good' : 'warn'
      if (exam.sheet) return 'warn'
      if (exam.status !== 'PUBLISHED') return 'muted'
      return this.isExpired(exam) ? 'muted' : 'info'
    },
    rowState(exam) {
      if (exam.sheet && exam.sheet.status === 'PUBLISHED') {
        return { text: exam.sheet.passFlag === 1 ? '已通过' : '未通过', tag: exam.sheet.passFlag === 1 ? 'success' : 'danger' }
      }
      if (exam.sheet) return { text: '批阅中', tag: 'warning' }
      if (exam.status === 'PUBLISHED') {
        return this.isExpired(exam) ? { text: '已截止', tag: 'info' } : { text: '可参加', tag: 'success' }
      }
      return { text: '已结束', tag: 'info' }
    },
    formatDuration(minutes) { return formatLearningDuration(minutes) },
    /**
     * ★ 2026-09-22：作答不再塞在本页（原 stage='exam'），改为跳**独立全屏作答页**。
     * 带 examId + from（回跳地址）—— 全屏页刷新 / 直达都能自己把题拉起来
     * （后端 startExam 对遗留的 IN_PROGRESS 答卷会**续答同一份**，不会重开）。
     */
    goAnswering(exam) {
      if (!exam || exam.examId == null) return
      this.$router.push({
        path: '/assessment/intern/exam-answering',
        query: { examId: exam.examId, from: this.$route.fullPath }
      })
    },
    /**
     * 查看成绩：带上本场次内所有考核ID，跳到「单场考核结果」页（只展示这一场）。
     * 不带参（或误传事件对象）时退回全部成绩页。
     */
    goResult(session) {
      const ids = ((session && session.exams) || []).map(e => e.examId).filter(id => id != null)
      if (!ids.length) {
        this.$router.push('/assessment/intern/learning/result')
        return
      }
      this.$router.push({ path: '/assessment/intern/learning/exam-result', query: { exams: ids.join(',') } })
    },
    goGuide() { this.$router.push('/assessment/intern/learning/guide') },
    goBack() {
      this.$router.push('/index')
    },
  }
}
</script>

<style lang="scss" scoped>
.exam-page { min-height: 100%; padding: 24px 26px 60px; color: #283544; background: #f5f7fa; }
.exam-breadcrumb { display: flex; align-items: center; gap: 8px; margin-bottom: 14px; color: #98a2b3; font-size: 12px; }
.exam-breadcrumb .el-button { padding: 0; color: #1764f5; font-size: 12px; }
.exam-breadcrumb b { color: #475467; font-weight: 500; }
.exam-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; margin-bottom: 18px; }
.eyebrow { color: #1764f5; font-size: 11px; letter-spacing: .08em; }
.exam-heading h1 { margin: 5px 0 7px; color: #1d2939; font-size: 26px; font-weight: 600; }
.head-badge { display: inline-flex; height: 26px; align-items: center; padding: 0 12px; color: #1a7a58; background: #e4f5ee; font-size: 12px; border-radius: 13px; }

/* ① 发布信息条 */
.pub-strip { display: flex; flex-wrap: wrap; align-items: center; gap: 28px; margin-bottom: 14px; padding: 13px 18px; background: #fff; border: 1px solid #e7ecf3; border-radius: 8px; }
.pub-fact span { display: block; margin-bottom: 5px; color: #8490a0; font-size: 11.5px; }
.pub-fact b { color: #1d2939; font-size: 13.5px; font-weight: 600; }
.pub-fact b.good { color: #067647; }
.pub-fact b.info { color: #1764f5; }
.pub-fact b.warn { color: #b54708; }
.pub-fact b.muted { color: #98a2b3; }
.pub-fact.right { margin-left: auto; }

/* 场次卡 */
.session-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; padding-top: 16px; }
.session-card { padding: 14px; border: 1px solid #e7ecf3; border-radius: 8px; background: #fff; }
.session-card.running { border-color: #bcd4f8; background: #f7fbff; }
.session-card.idle { border-style: dashed; }
.session-card.done { border-color: #cce9dc; }
.session-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.session-head b { color: #1d2939; font-size: 13.5px; font-weight: 600; }
.session-badge { flex: none; padding: 2px 9px; font-size: 11px; border-radius: 10px; background: #eef2f6; color: #667085; }
.session-badge.ok { color: #1a7a58; background: #e4f5ee; }
.session-badge.info { color: #1764f5; background: #e2ecff; }
.session-badge.warn { color: #b54708; background: #fff4e5; }
.session-badge.purple { color: #6f42c1; background: #f1ebfd; }
.session-badge.muted { color: #98a2b3; background: #f2f4f7; }
.session-window { margin-top: 8px; color: #98a2b3; font-size: 11.5px; }
.session-rows { display: grid; gap: 7px; margin-top: 11px; }
.session-row { display: flex; align-items: center; justify-content: space-between; color: #475467; font-size: 12px; }
.session-row b { font-weight: 600; }
.session-row b.good { color: #067647; }
.session-row b.warn { color: #b54708; }
.session-row b.info { color: #1764f5; }
.session-row b.muted { color: #98a2b3; }
.session-foot { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-top: 12px; padding-top: 10px; border-top: 1px dashed #e7ecf3; }
.session-meta { color: #98a2b3; font-size: 11.5px; }

/* 卡片骨架 */
.pm-card { margin-bottom: 14px; padding: 18px 20px 20px; background: #fff; border: 1px solid #e7ecf3; border-radius: 8px; }
.pm-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding-bottom: 14px; border-bottom: 1px solid #edf0f4; }
.pm-head.compact { padding-bottom: 12px; }
.pm-title { display: flex; align-items: center; gap: 9px; flex-wrap: wrap; }
.pm-title h3 { margin: 0; color: #1d2939; font-size: 17px; font-weight: 600; }
.pm-idx { display: inline-flex; width: 24px; height: 24px; align-items: center; justify-content: center; color: #1764f5; background: #e8f1fd; font-size: 12px; font-weight: 700; border-radius: 4px; }
.pm-idx.warn { color: #b54708; background: #fff4e5; }
.pm-idx.purple { color: #6f42c1; background: #f1ebfd; }
.pm-empty { display: flex; min-height: 150px; align-items: center; justify-content: center; flex-direction: column; gap: 8px; color: #98a2b3; }
.pm-empty i { color: #b7c1cc; font-size: 32px; }
.pm-empty span { font-size: 13px; }
.dsample { display: inline-block; margin-left: 5px; padding: 0 5px; color: #b54708; background: #fff4e5; font-size: 10px; font-style: normal; line-height: 15px; border-radius: 8px; }
.file-row { display: flex; align-items: center; gap: 14px; margin-top: 14px; padding: 13px 15px; background: #fafcff; border: 1px solid #e9eff7; border-radius: 6px; }
.file-tag { flex: none; padding: 4px 8px; color: #1764f5; background: #e2ecff; font-size: 11px; border-radius: 4px; }
.file-name { flex: 1; min-width: 0; }
.file-name b { display: block; color: #1d2939; font-size: 13.5px; }
.file-name span { display: block; margin-top: 4px; color: #98a2b3; font-size: 11.5px; }

/* 资格校验 */
.trip-grid { display: grid; grid-template-columns: 1fr; gap: 14px; }
.trip-grid .pm-card { margin-bottom: 0; }
.chk-list { display: grid; gap: 10px; margin-top: 14px; }
.chk { display: flex; align-items: center; gap: 8px; color: #475467; font-size: 12.5px; }
.chk b { margin-left: auto; color: #1d2939; font-weight: 600; }
.chk b.good { color: #067647; }
.chk b.warn { color: #b54708; }
.chk .mark { display: inline-flex; width: 18px; height: 18px; flex: none; align-items: center; justify-content: center; color: #98a2b3; background: #f2f4f7; font-size: 11px; border-radius: 50%; }
.chk .mark.ok { color: #fff; background: #12b76a; }
.chk .mark.no { color: #fff; background: #f79009; }
.kv-list { display: grid; gap: 9px; margin-top: 14px; }
.kv-list .row { display: flex; align-items: center; justify-content: space-between; color: #667085; font-size: 12.5px; }
.kv-list .row b { color: #1d2939; font-weight: 600; }

/* 记录表 */
.pm-table { width: 100%; margin-top: 14px; border-collapse: collapse; }
.pm-table th { padding: 9px 10px; color: #8490a0; background: #f8fafc; font-size: 12px; font-weight: 500; text-align: left; }
.pm-table td { padding: 10px; border-bottom: 1px solid #edf0f4; color: #475467; font-size: 12.5px; }
.pm-table td.ellipsis { max-width: 260px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }


@media (max-width: 1200px) {
  .session-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .trip-grid { grid-template-columns: 1fr; }
}
@media (max-width: 760px) {
  .exam-page { padding: 16px 12px 40px; }
  .session-grid { grid-template-columns: 1fr; }
  .pub-strip { gap: 16px; }
  .pub-fact.right { margin-left: 0; }
}
</style>
