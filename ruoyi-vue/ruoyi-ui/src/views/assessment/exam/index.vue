<template>
  <div class="exam-page">
    <div class="exam-breadcrumb">
      <el-button type="text" icon="el-icon-arrow-left" @click="goBack">返回工作台</el-button>
      <span>/</span>
      <b>{{ isFormal ? '考核记录' : '正式考核' }}</b>
    </div>

    <!-- 阶段一：发布信息 + 考核场次 + 备考资料 + 资格校验 + 环节卡 -->
    <template v-if="stage === 'list'">
      <header class="exam-heading">
        <div>
          <span class="eyebrow">{{ isFormal ? 'ASSESSMENT HISTORY' : 'FORMAL ASSESSMENT' }}</span>
          <h1>{{ isFormal ? '考核记录' : '正式考核' }}</h1>
          <p>{{ isFormal ? '转正前的正式考核结果会继续保留，可在此查看。' : '由所属部门管理员按批次发布 · 可阶段性多次参加 · 通过后进入转正审批。' }}</p>
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
            <span class="pm-hint">共 {{ sessions.length }} 场 · 已参加 {{ attendedCount }} 场 · 通过 {{ passedCount }} 场</span>
          </div>
          <el-tag v-if="hasSampleWindow" size="mini" type="warning" effect="plain">部分场次时间窗为示例数据</el-tag>
        </div>

        <div v-if="!sessions.length" class="pm-empty"><i class="el-icon-document" /><span>部门尚未发布正式考核，发布后会按场次出现在这里。</span></div>

        <div v-else class="session-grid">
          <div v-for="s in sessions" :key="s.key" class="session-card" :class="s.state.cardTone">
            <div class="session-head">
              <b>{{ s.name }}</b>
              <span class="session-badge" :class="s.state.badgeTone">{{ s.state.label }}</span>
            </div>
            <div class="session-window">
              时间窗 {{ s.windowText }}
              <el-tag v-if="s.assignedOnly" size="mini" type="warning" effect="plain" style="margin-left:6px">指定人员</el-tag>
            </div>
            <div class="session-rows">
              <div class="session-row">
                <span>理论环节</span>
                <b :class="stageTone(s.theory)">{{ stageText(s.theory) }}</b>
              </div>
              <div class="session-row">
                <span>实操环节</span>
                <b :class="stageTone(s.practice)">{{ stageText(s.practice) }}</b>
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

        <div class="pm-note">
          <b>阶段性多次参加：</b>每个批次是独立计分单位，同一实习生可在多个批次分阶段参加；已通过批次成绩长期保留，未通过可在后续批次（补考场）重考。
          场次分组依据考核名称中的批次 / 期次标识（示例解析），建议后端补批次字段后在列表接口直接返回。
        </div>
      </section>

      <!-- ③ 备考资料 -->
      <section v-if="!isFormal" class="pm-card">
        <div class="pm-head">
          <div class="pm-title"><span class="pm-idx">资</span><h3>备考资料</h3><span class="pm-hint">部门管理员上传 · 当前批次</span></div>
          <el-button size="mini" icon="el-icon-notebook-2" @click="goGuide">打开备考资料页</el-button>
        </div>
        <div class="file-row">
          <span class="file-tag">PDF</span>
          <div class="file-name"><b>考试指南与考核规则</b><span>备考资料统一在「备考资料」页签查看与下载</span></div>
          <el-button size="mini" plain @click="goGuide">前往查看</el-button>
        </div>
      </section>

      <!-- ④⑤⑥ 资格校验 + 两个环节 -->
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
              <b>{{ remainTimes }} 次<em class="dsample">示例</em></b>
            </div>
          </div>
          <div class="pm-note">
            {{ learningGap ? '还差 ' + learningGap + '% 完成率解锁。建议先完成剩余必修章节。' : '学习门槛已满足，可在开放场次内参加考试。' }}
          </div>
        </section>

        <section class="pm-card">
          <div class="pm-head compact">
            <div class="pm-title"><span class="pm-idx">5</span><h3>理论考试</h3></div>
            <span class="session-badge info">线上考试</span>
          </div>
          <div class="kv-list">
            <div class="row"><span>考试时长</span><b>{{ theoryMeta.duration }}<em class="dsample">示例</em></b></div>
            <div class="row"><span>通过分 / 总分</span><b>{{ theoryMeta.passLine }} / 100 分</b></div>
            <div class="row"><span>题目数量</span><b>{{ theoryMeta.questionCount }} 题</b></div>
            <div class="row"><span>考试题型</span><b>单选 + 多选 + 判断</b></div>
          </div>
          <div class="pm-note">知识模块按批次规则抽题；进入后由后端随机组卷，交卷自动判分。</div>
        </section>

        <section class="pm-card">
          <div class="pm-head compact">
            <div class="pm-title"><span class="pm-idx purple">6</span><h3>实操考试</h3></div>
            <span class="session-badge purple">线上提交</span>
          </div>
          <div class="kv-list">
            <div class="row"><span>考试时长</span><b>{{ practiceMeta.duration }}<em class="dsample">示例</em></b></div>
            <div class="row"><span>通过分 / 总分</span><b>{{ practiceMeta.passLine }} / 100 分</b></div>
            <div class="row"><span>题目数量</span><b>{{ practiceMeta.questionCount }} 题</b></div>
            <div class="row"><span>作答方式</span><b>上传文件包</b></div>
          </div>
          <div class="pm-note">实操题目在进入考试后直看；提交后由管理员人工批阅。</div>
        </section>
      </div>

      <!-- 历史场次（正式实习生 / 已结束场次回看） -->
      <section v-if="isFormal || sessions.length === 0" class="pm-card">
        <div class="pm-head">
          <div class="pm-title"><span class="pm-idx">录</span><h3>考核记录</h3><span class="pm-hint">只读 · 含批阅中的记录</span></div>
        </div>
        <div v-if="!visibleExams.length" class="pm-empty"><i class="el-icon-document" /><span>{{ isFormal ? '暂无历史考核记录' : '暂无可参加的考核' }}</span></div>
        <table v-else class="pm-table">
          <thead><tr><th>考核名称</th><th>环节</th><th>状态</th><th>成绩</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="exam in visibleExams" :key="exam.examId">
              <td class="ellipsis">{{ exam.examName }}</td>
              <td>{{ exam.examType === 'PRACTICAL' ? '实操' : '理论' }}</td>
              <td><el-tag size="mini" effect="plain" :type="rowState(exam).tag">{{ rowState(exam).text }}</el-tag></td>
              <td>{{ exam.sheet && exam.sheet.status === 'PUBLISHED' ? exam.sheet.finalScore : '--' }}</td>
              <td><el-button type="text" size="mini" @click="goResult">查看成绩</el-button></td>
            </tr>
          </tbody>
        </table>
      </section>
    </template>

    <!-- 阶段二：作答 -->
    <template v-else-if="stage === 'exam'">
      <header class="exam-heading">
        <div><span class="eyebrow">ANSWERING</span><h1>{{ currentExamName }}</h1><p>{{ examType === 'PRACTICAL' ? '阅读题干，上传作答文件后交卷' : '共 ' + questions.length + ' 题 · 请完成后点击交卷' }}</p></div>
        <div v-if="examType === 'THEORY'" class="exam-progress"><el-progress :percentage="progress" :stroke-width="8" /></div>
      </header>

      <!-- 实操考核 -->
      <div v-if="examType === 'PRACTICAL'" class="subject-card">
        <div class="subject-label">实操题干</div>
        <div class="subject-content">{{ subjectContent }}</div>
        <div v-if="subjectAttachment" class="subject-attachment">
          <span>参考附件：</span>
          <a :href="baseApi + subjectAttachment" target="_blank" class="attachment-link"><i class="el-icon-paperclip" /> 点击查看 / 下载</a>
        </div>
        <div class="subject-upload">
          <span class="upload-label">请上传作答文件：</span>
          <el-upload :auto-upload="false" :limit="1" :show-file-list="true" accept="*" :on-change="onSubjectFile">
            <el-button size="small" icon="el-icon-upload">上传作答文件</el-button>
          </el-upload>
          <span v-if="subjectFile" class="uploaded-name"><i class="el-icon-check" /> 文件已上传</span>
        </div>
      </div>

      <!-- 理论考核 -->
      <div v-else>
        <div v-for="(q, index) in questions" :key="q.id" class="question-card">
          <div class="q-head">
            <span class="q-index">{{ index + 1 }}</span>
            <el-tag size="mini" effect="plain" :type="typeTag(q.qtype)">{{ typeLabel(q.qtype) }}</el-tag>
            <span class="q-score">{{ q.score }} 分</span>
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
      </div>

      <div class="submit-bar">
        <el-button size="medium" @click="backToList">放弃返回</el-button>
        <el-button type="primary" size="medium" :loading="submitting" @click="submitExam">交 卷</el-button>
      </div>
    </template>

    <!-- 阶段三：作答完成 -->
    <template v-else>
      <div class="done-state">
        <div class="done-icon"><i class="el-icon-success" /></div>
        <h2>作答完成</h2>
        <p>{{ examType === 'PRACTICAL' ? '你的作答文件已提交，管理员批阅后会发布成绩。' : '你的答卷已提交，成绩已自动判定，请返回考核列表查看结果。' }}</p>
        <el-button type="primary" size="medium" @click="goBack">返回首页</el-button>
      </div>
    </template>
  </div>
</template>

<script>
import { myExamList, startExam, submitExam, uploadFile } from '@/api/business/exam'
import { listLearningCourses } from '@/api/business/learning'
import { learningSummary, formatLearningDuration } from '@/utils/learningPreview'
import { mapGetters } from 'vuex'

/** 环节默认值（exam 列表接口暂未返回时长/题量，页面标注「示例」） */
const DEMO_THEORY = { duration: '60 分钟', questionCount: 20, passLine: 60 }
const DEMO_PRACTICE = { duration: '180 分钟', questionCount: 2, passLine: 60 }
/** 剩余考试次数（批次规则未接接口，页面标注「示例」） */
const DEMO_REMAIN_TIMES = 2

export default {
  name: 'InternExam',
  data() {
    return {
      stage: 'list',
      loading: false,
      exams: [],
      currentExamName: '',
      examType: 'THEORY',
      subjectContent: '',
      subjectAttachment: '',
      subjectFile: '',
      sheetId: null,
      questions: [],
      answers: [],
      submitting: false,
      learningOverview: { progress: null, courseCount: 0, completedCourses: 0, learningCourses: 0, completedItems: 0, itemCount: 0, lastStudyTime: '尚未开始' }
    }
  },
  computed: {
    ...mapGetters(['roles', 'protocolStatus', 'deptName']),
    baseApi() { return process.env.VUE_APP_BASE_API || '' },
    isFormal() { return this.roles.indexOf('FORMAL_TRAINEE') > -1 },
    visibleExams() { return this.isFormal ? this.exams.filter(exam => !!exam.sheet) : this.exams },
    examMode() { return this.$route.name === 'InternMockExam' ? 'PRACTICE' : 'FORMAL' },
    progress() {
      const answered = this.answers.filter(a => {
        if (Array.isArray(a)) return a.length > 0
        return !!a
      }).length
      return this.questions.length ? Math.round(answered / this.questions.length * 100) : 0
    },
    learningProgress() { return this.learningOverview.progress === null ? 0 : this.learningOverview.progress },
    learningGap() { return Math.max(0, 70 - this.learningProgress) },
    qualified() { return this.learningProgress >= 70 && Number(this.protocolStatus) === 1 },
    remainTimes() { return DEMO_REMAIN_TIMES },
    /** 场次分组：考核名称里的批次 / 期次标识（示例解析，后端补批次字段后可直取） */
    sessions() {
      const map = {}
      this.exams.forEach(exam => {
        const key = this.batchKeyOf(exam)
        if (!map[key]) map[key] = { key, name: key, exams: [], theory: null, practice: null }
        map[key].exams.push(exam)
        if (exam.examType === 'PRACTICAL') map[key].practice = exam
        else map[key].theory = exam
      })
      return Object.keys(map).map(key => this.decorateSession(map[key]))
    },
    /** 是否有场次缺时间窗（缺则打示例标记） */
    hasSampleWindow() {
      return this.sessions.some(s => !s.exams.some(e => e.startTime || e.endTime))
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
    },
    theoryMeta() {
      const theory = this.sessions.map(s => s.theory).filter(Boolean)[0]
      return {
        duration: DEMO_THEORY.duration,
        questionCount: (theory && theory.questionCount) || DEMO_THEORY.questionCount,
        passLine: (theory && theory.passLine) || DEMO_THEORY.passLine
      }
    },
    practiceMeta() {
      const practice = this.sessions.map(s => s.practice).filter(Boolean)[0]
      return {
        duration: DEMO_PRACTICE.duration,
        questionCount: DEMO_PRACTICE.questionCount,
        passLine: (practice && practice.passLine) || DEMO_PRACTICE.passLine
      }
    }
  },
  created() {
    this.loadList()
    if (!this.isFormal) this.loadLearning()
  },
  mounted() {
    window.addEventListener('beforeunload', this.handleBeforeUnload)
  },
  beforeDestroy() {
    window.removeEventListener('beforeunload', this.handleBeforeUnload)
  },
  beforeRouteLeave(to, from, next) {
    if (this.stage === 'exam' && !this.submitting) {
      this.$modal.confirm('退出后本次作答进度不会保存，也不会自动交卷，确定退出吗？').then(() => {
        next()
      }).catch(() => next(false))
    } else {
      next()
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
    loadLearning() {
      listLearningCourses().then(res => {
        this.learningOverview = learningSummary(res.data || [])
      }).catch(() => {
        this.learningOverview = learningSummary([])
      })
    },
    /** 时间窗展示：2026-10-08 09:00 ~ 2026-10-20 18:00 */
    fmtWindow(start, end) {
      const f = v => v ? String(v).replace('T', ' ').slice(0, 16) : '--'
      return f(start) + ' ~ ' + f(end)
    },
    batchKeyOf(exam) {
      const name = exam.examName || '未命名场次'
      const matched = name.match(/(\d{4}Q\d)|(第[一二三四五六七八九十\d]+[期批场])/)
      return matched ? matched[0] : name
    },
    /** 场次状态机：已通过 / 待批阅 / 进行中 / 已结束 / 未解锁 */
    decorateSession(session) {
      const sheets = session.exams.map(e => e.sheet).filter(Boolean)
      const published = sheets.filter(s => s.status === 'PUBLISHED')
      const passed = published.length > 0 && published.every(s => s.passFlag === 1)
      const pending = sheets.some(s => s.status !== 'PUBLISHED')
      const openExam = session.exams.find(e => !e.sheet && e.status === 'PUBLISHED')
      const ended = session.exams.length > 0 && session.exams.every(e => !e.sheet && e.status !== 'PUBLISHED')
      const attended = sheets.length > 0
      const running = !attended && !!openExam

      let state
      if (passed) state = { label: '已通过', tone: 'good', cardTone: 'done', badgeTone: 'ok' }
      else if (attended && pending) state = { label: '待批阅', tone: 'warn', cardTone: '', badgeTone: 'warn' }
      else if (running) state = { label: '进行中', tone: 'info', cardTone: 'running', badgeTone: 'info' }
      else if (attended) state = { label: '已结束', tone: 'muted', cardTone: '', badgeTone: 'muted' }
      else if (ended) state = { label: '已结束', tone: 'muted', cardTone: '', badgeTone: 'muted' }
      else state = { label: '未开始', tone: 'muted', cardTone: 'idle', badgeTone: 'muted' }

      const windows = session.exams
        .map(e => (e.startTime || e.endTime) ? this.fmtWindow(e.startTime, e.endTime) : '')
        .filter(t => !!t)
      const assignedOnly = session.exams.some(e => e.assigned)
      return Object.assign({}, session, {
        state,
        attended,
        passed,
        running,
        assignedOnly,
        windowText: windows.length ? windows[0] : '以管理员发布为准',
        footText: passed ? '综合结果 已通过' : (attended ? '已完成 ' + sheets.length + ' 个环节' : (running ? '剩余次数 ' + this.remainTimes + ' 次' : '未解锁')),
        action: this.buildAction(session, { passed, running, attended, openExam })
      })
    },
    buildAction(session, ctx) {
      if (ctx.passed) return { label: '查看成绩', primary: false, disabled: false, run: () => this.goResult() }
      if (ctx.attended) return { label: '已提交', primary: false, disabled: true, run: () => {} }
      if (ctx.openExam && this.qualified) {
        return { label: '开始考试', primary: true, disabled: false, run: () => this.startExam(ctx.openExam) }
      }
      if (ctx.openExam && !this.qualified) {
        return { label: '未解锁', primary: false, disabled: true, run: () => {} }
      }
      if (ctx.running) return { label: '查看成绩', primary: false, disabled: false, run: () => this.goResult() }
      return { label: '未开始', primary: false, disabled: true, run: () => {} }
    },
    stageText(exam) {
      if (!exam) return '未发布'
      if (exam.sheet && exam.sheet.status === 'PUBLISHED') {
        return (exam.sheet.passFlag === 1 ? '已通过 ' : '未通过 ') + exam.sheet.finalScore + ' 分'
      }
      if (exam.sheet) return '批阅中'
      if (exam.status === 'PUBLISHED') return '已开放 · 可参加'
      return '未发布'
    },
    stageTone(exam) {
      if (!exam) return 'muted'
      if (exam.sheet && exam.sheet.status === 'PUBLISHED') return exam.sheet.passFlag === 1 ? 'good' : 'warn'
      if (exam.sheet) return 'warn'
      return exam.status === 'PUBLISHED' ? 'info' : 'muted'
    },
    rowState(exam) {
      if (exam.sheet && exam.sheet.status === 'PUBLISHED') {
        return { text: exam.sheet.passFlag === 1 ? '已通过' : '未通过', tag: exam.sheet.passFlag === 1 ? 'success' : 'danger' }
      }
      if (exam.sheet) return { text: '批阅中', tag: 'warning' }
      return exam.status === 'PUBLISHED' ? { text: '可参加', tag: 'success' } : { text: '已结束', tag: 'info' }
    },
    formatDuration(minutes) { return formatLearningDuration(minutes) },
    startExam(exam) {
      this.loading = true
      startExam(exam.examId).then(res => {
        const data = res.data
        this.sheetId = data.sheetId
        this.currentExamName = data.examName
        this.examType = data.examType || 'THEORY'
        this.subjectContent = data.subjectContent || ''
        this.subjectAttachment = data.subjectAttachment || ''
        this.subjectFile = ''
        this.questions = data.questions || []
        this.answers = this.questions.map(q => q.qtype === 'MULTI' ? [] : '')
        this.loading = false
        this.stage = 'exam'
      }).catch(() => { this.loading = false })
    },
    onSubjectFile(file) {
      const formData = new FormData()
      formData.append('file', file.raw)
      uploadFile(formData).then(res => {
        this.subjectFile = res.fileName
        this.$modal.msgSuccess('作答文件上传成功')
      })
    },
    submitExam() {
      if (this.examType === 'PRACTICAL') {
        if (!this.subjectFile) {
          this.$modal.msgWarning('请先上传作答文件')
          return
        }
        this.$modal.confirm('确认提交作答文件吗？提交后不可修改。').then(() => this.doSubmit()).catch(() => {})
        return
      }
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
      let answers
      if (this.examType === 'PRACTICAL') {
        answers = [{ questionId: '0', userAnswer: this.subjectFile }]
      } else {
        answers = this.questions.map((q, i) => {
          const ans = this.answers[i]
          return { questionId: q.id, userAnswer: Array.isArray(ans) ? ans.sort().join(',') : ans }
        })
      }
      submitExam({ sheetId: this.sheetId, answers }).then(() => {
        this.submitting = false
        this.stage = 'done'
      }).catch(() => { this.submitting = false })
    },
    backToList() {
      this.$modal.confirm('退出后本次作答进度不会保存，也不会自动交卷，确定退出吗？').then(() => {
        this.stage = 'list'
        this.loadList()
      }).catch(() => {})
    },
    goResult() { this.$router.push('/assessment/intern/learning/result') },
    goGuide() { this.$router.push('/assessment/intern/learning/guide') },
    goBack() {
      this.$router.push('/index')
    },
    handleBeforeUnload(e) {
      if (this.stage === 'exam') {
        e.preventDefault()
        e.returnValue = ''
      }
    },
    parseOptions(json) {
      try { return JSON.parse(json || '[]') } catch (e) { return [] }
    },
    typeLabel(qtype) {
      return { SINGLE: '单选', MULTI: '多选', JUDGE: '判断' }[qtype] || qtype
    },
    typeTag(qtype) {
      return { SINGLE: 'success', MULTI: 'warning', JUDGE: 'primary' }[qtype] || 'info'
    }
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
.exam-heading p { margin: 0; color: #667085; font-size: 13px; }
.exam-progress { width: 240px; }
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
.pm-hint { color: #98a2b3; font-size: 12px; }
.pm-note { margin-top: 12px; color: #98a2b3; font-size: 11.5px; line-height: 1.7; }
.pm-note b { color: #667085; }
.pm-empty { display: flex; min-height: 150px; align-items: center; justify-content: center; flex-direction: column; gap: 8px; color: #98a2b3; }
.pm-empty i { color: #b7c1cc; font-size: 32px; }
.pm-empty span { font-size: 13px; }
.dsample { display: inline-block; margin-left: 5px; padding: 0 5px; color: #b54708; background: #fff4e5; font-size: 10px; font-style: normal; line-height: 15px; border-radius: 8px; }
.file-row { display: flex; align-items: center; gap: 14px; margin-top: 14px; padding: 13px 15px; background: #fafcff; border: 1px solid #e9eff7; border-radius: 6px; }
.file-tag { flex: none; padding: 4px 8px; color: #1764f5; background: #e2ecff; font-size: 11px; border-radius: 4px; }
.file-name { flex: 1; min-width: 0; }
.file-name b { display: block; color: #1d2939; font-size: 13.5px; }
.file-name span { display: block; margin-top: 4px; color: #98a2b3; font-size: 11.5px; }

/* 资格校验 + 环节三卡 */
.trip-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }
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

/* 作答 / 完成（沿用既有实现） */
.subject-card { padding: 20px 22px; margin-bottom: 16px; background: #fff; border: 1px solid #e7ecf3; border-radius: 8px; }
.subject-label { color: #1764f5; font-size: 12px; font-weight: 600; margin-bottom: 8px; }
.subject-content { color: #1d2939; font-size: 15px; line-height: 1.7; white-space: pre-wrap; }
.subject-attachment { margin: 12px 0; color: #667085; font-size: 13px; }
.attachment-link { color: #1764f5; text-decoration: none; }
.attachment-link i { margin-right: 4px; }
.subject-upload { display: flex; align-items: center; gap: 12px; padding: 14px; background: #f8fafc; border: 1px dashed #d0d5dd; border-radius: 6px; }
.upload-label { color: #475467; font-size: 13px; }
.uploaded-name { color: #23966f; font-size: 12px; }
.uploaded-name i { margin-right: 3px; }
.question-card { margin-bottom: 16px; padding: 20px 22px; background: #fff; border: 1px solid #e7ecf3; border-radius: 8px; }
.q-head { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.q-index { display: inline-flex; width: 24px; height: 24px; align-items: center; justify-content: center; color: #fff; background: #1764f5; font-size: 13px; border-radius: 50%; }
.q-score { margin-left: auto; color: #8490a0; font-size: 12px; }
.q-stem { margin-bottom: 14px; color: #1d2939; font-size: 15px; line-height: 1.7; }
.q-options .option-item { padding: 9px 12px; margin-bottom: 6px; border: 1px solid #edf0f4; border-radius: 6px; }
.q-options .option-item:hover { background: #f8fafc; }
.option-item .el-radio, .option-item .el-checkbox { display: block; margin-right: 0; line-height: 1.6; }
.submit-bar { display: flex; justify-content: flex-end; gap: 12px; margin-top: 8px; padding: 16px 0; }
.done-state { display: flex; min-height: 420px; align-items: center; justify-content: center; flex-direction: column; gap: 8px; background: #fff; border: 1px solid #e7ecf3; border-radius: 10px; }
.done-icon { color: #23966f; font-size: 64px; }
.done-state h2 { margin: 8px 0; color: #1d2939; font-size: 22px; font-weight: 600; }
.done-state p { color: #667085; font-size: 14px; margin-bottom: 16px; }

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
