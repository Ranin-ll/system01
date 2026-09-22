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

    <!-- 阶段二：作答 -->
    <template v-else-if="stage === 'exam'">
      <header class="exam-heading">
        <div>
          <span class="eyebrow">ANSWERING</span>
          <h1>{{ currentExamName }}</h1>
        </div>
        <div class="exam-head-right">
          <div v-if="remainingSeconds > 0" class="countdown" :class="countdownTone">
            <i class="el-icon-alarm-clock" />
            <span>剩余</span>
            <b>{{ countdownText }}</b>
          </div>
          <div v-if="examType === 'THEORY'" class="exam-progress"><el-progress :percentage="progress" :stroke-width="8" /></div>
        </div>
      </header>

      <!-- 实操考核：逐题展示题干/描述/参考/附件，并逐题上传作答文件 -->
      <div v-if="examType === 'PRACTICAL'" class="subject-card">
        <div class="subject-head">
          <span class="subject-label">实操题目（共 {{ subjectItems.length }} 道 · 请逐题上传作答文件）</span>
          <span class="subject-progress" :class="{ done: uploadedCount === subjectItems.length && subjectItems.length > 0 }">
            已上传 {{ uploadedCount }} / {{ subjectItems.length }} 题
          </span>
        </div>

        <div v-if="!subjectItems.length" class="subject-empty">
          <i class="el-icon-warning-outline" />
          <span>本次实操考核暂未配置题目，请联系管理员</span>
        </div>

        <div v-for="(s, index) in subjectItems" :key="s.itemId || s.subjectItemId || index" class="subject-q">
          <!-- 左卡片：描述 + 附件 + 上传按钮 -->
          <div class="s-q-left">
            <div class="s-q-head">
              <span class="s-q-index">{{ index + 1 }}</span>
              <div class="s-q-title">{{ s.title }}</div>
              <span class="s-q-score">{{ s.score || 0 }} 分</span>
            </div>
            <div v-if="s.description" class="s-q-stem">{{ s.description }}</div>

            <div v-if="subjectAttachments(s).length" class="s-q-attach">
              <span class="s-q-attach-label">参考附件：</span>
              <a
                v-for="(a, i) in subjectAttachments(s)"
                :key="i"
                :href="baseApi + a.url"
                target="_blank"
                class="attachment-link"
              ><i class="el-icon-paperclip" /> {{ a.name }}</a>
            </div>

            <div class="s-q-upload" :class="{ ok: !!subjectFiles[s.subjectItemId] }">
              <el-upload
                :auto-upload="false"
                :limit="1"
                :show-file-list="false"
                accept="*"
                :on-change="file => onSubjectItemFile(s, file)"
              >
                <el-button size="small" icon="el-icon-upload2" :loading="uploadingItemId === s.subjectItemId">
                  {{ subjectFiles[s.subjectItemId] ? '重新上传' : '上传作答文件' }}
                </el-button>
              </el-upload>
              <span v-if="subjectFiles[s.subjectItemId]" class="uploaded-name">
                <i class="el-icon-check" /> {{ subjectFiles[s.subjectItemId].name }}
              </span>
              <span v-else class="upload-waiting">本题尚未上传作答文件</span>
            </div>
          </div>

          <!-- 右卡片：参考图 -->
          <div class="s-q-right">
            <div class="s-q-right-label">参考图</div>
            <div v-if="subjectImages(s).length" class="s-q-images">
              <template v-for="(img, i) in subjectImages(s)">
                <video v-if="isVideo(img.url)" :key="'v' + i" :src="baseApi + img.url" controls class="s-q-image s-q-video" />
                <el-image
                  v-else
                  :key="'i' + i"
                  :src="baseApi + img.url"
                  :preview-src-list="subjectPreview(s)"
                  fit="cover"
                  class="s-q-image"
                />
              </template>
            </div>
            <div v-else class="s-q-no-image">暂无参考图</div>
          </div>
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

/** 解析后端 [{name,url}] 形式的 JSON 字段（实操题目的参考 / 附件） */
function parseJsonList(json) {
  if (!json) return []
  try {
    const arr = typeof json === 'string' ? JSON.parse(json) : json
    return Array.isArray(arr) ? arr : []
  } catch (e) {
    return []
  }
}
/** 剩余考试次数：批次规则未接接口 ⇒ 留空（不再用固定 2 次假装） */
const DEMO_REMAIN_TIMES = null

export default {
  name: 'InternExam',
  data() {
    return {
      stage: 'list',
      loading: false,
      exams: [],
      currentExamName: '',
      examType: 'THEORY',
      /** 实操题目清单（后端下发：题干 / 描述 / 参考 / 附件 / 满分） */
      subjectItems: [],
      /** 逐题作答文件：{ [subjectItemId]: { path, name } } */
      subjectFiles: {},
      uploadingItemId: null,
      sheetId: null,
      questions: [],
      answers: [],
      submitting: false,
      /** 剩余作答秒数（服务端下发；0 = 不限时，不启动倒计时） */
      remainingSeconds: 0,
      countdownTimer: null,
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
    progress() {
      const answered = this.answers.filter(a => {
        if (Array.isArray(a)) return a.length > 0
        return !!a
      }).length
      return this.questions.length ? Math.round(answered / this.questions.length * 100) : 0
    },
    /** 实操：已上传作答文件的题目数 */
    uploadedCount() {
      return this.subjectItems.filter(s => !!this.subjectFiles[s.subjectItemId]).length
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
    this.loadList()
    if (!this.isFormal) this.loadLearning()
  },
  mounted() {
    window.addEventListener('beforeunload', this.handleBeforeUnload)
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
    window.removeEventListener('beforeunload', this.handleBeforeUnload)
    window.removeEventListener('focus', this.onFocus)
    document.removeEventListener('visibilitychange', this.onVisible)
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer)
      this.refreshTimer = null
    }
    this.clearCountdown()
  },
  beforeRouteLeave(to, from, next) {
    if (this.stage === 'exam' && !this.submitting) {
      this.$modal.confirm('退出后本次作答进度不会保存，也不会自动交卷，确定退出吗？').then(() => {
        this.clearCountdown()
        next()
      }).catch(() => next(false))
    } else {
      this.clearCountdown()
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
        return { label: '开始考试', primary: true, disabled: false, run: () => this.startExam(ctx.openExam) }
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
    startExam(exam) {
      this.loading = true
      startExam(exam.examId).then(res => {
        const data = res.data
        this.sheetId = data.sheetId
        this.currentExamName = data.examName
        this.examType = data.examType || 'THEORY'
        this.subjectItems = data.subjectItems || []
        this.subjectFiles = {}
        this.uploadingItemId = null
        this.questions = data.questions || []
        this.answers = this.questions.map(q => q.qtype === 'MULTI' ? [] : '')
        this.loading = false
        this.stage = 'exam'
        // 服务端下发剩余作答秒数：> 0 才启动倒计时（0 = 不限时）
        this.startCountdown(Number(data.remainingSeconds) || 0)
      }).catch(() => { this.loading = false })
    },
    /** 启动倒计时（以服务端剩余秒数为基准，本地每秒递减） */
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
      this.$modal.confirmOnly('考试时间已到，将自动提交本次作答。').then(() => {
        this.doSubmit('back')
      }).catch(() => {})
    },
    /** 实操：逐题上传作答文件（每题一份） */
    onSubjectItemFile(subject, file) {
      const formData = new FormData()
      formData.append('file', file.raw)
      this.uploadingItemId = subject.subjectItemId
      uploadFile(formData).then(res => {
        // 用 $set 写入对象新键，保证视图响应式更新
        this.$set(this.subjectFiles, subject.subjectItemId, { path: res.fileName, name: file.name })
        this.uploadingItemId = null
        this.$modal.msgSuccess('第 ' + subject.seq + ' 题作答文件上传成功')
      }).catch(() => { this.uploadingItemId = null })
    },
    /** 实操题目参考 / 附件（后端下发 JSON 字符串） */
    subjectImages(s) { return parseJsonList(s.referenceImages) },
    subjectAttachments(s) { return parseJsonList(s.attachments) },
    subjectPreview(s) { return this.subjectImages(s).filter(i => !this.isVideo(i.url)).map(i => this.baseApi + i.url) },
    isVideo(url) { return /\.(mp4|webm|ogg|ogv|mov|avi|m4v)$/i.test(String(url || '')) },
    submitExam() {
      if (this.examType === 'PRACTICAL') {
        const missing = this.subjectItems.filter(s => !this.subjectFiles[s.subjectItemId])
        if (missing.length === this.subjectItems.length) {
          this.$modal.msgWarning('请先上传作答文件')
          return
        }
        if (missing.length) {
          this.$modal.confirm('还有 ' + missing.length + ' 道题未上传作答文件，确定交卷吗？').then(() => this.doSubmit()).catch(() => {})
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
    /**
     * 交卷。
     * @param {String} [done] 传 'back' 表示倒计时到点自动交卷：提交成功后直接回考核列表；
     *                        不传则走「作答完成」页（手动点「交卷」）。
     */
    doSubmit(done) {
      this.submitting = true
      let answers
      if (this.examType === 'PRACTICAL') {
        // 逐题交卷：questionId 传实操题目ID，后端按题对齐写入作答明细
        answers = this.subjectItems
          .filter(s => !!this.subjectFiles[s.subjectItemId])
          .map(s => ({ questionId: String(s.subjectItemId), userAnswer: this.subjectFiles[s.subjectItemId].path }))
      } else {
        answers = this.questions.map((q, i) => {
          const ans = this.answers[i]
          return { questionId: q.id, userAnswer: Array.isArray(ans) ? ans.sort().join(',') : ans }
        })
      }
      submitExam({ sheetId: this.sheetId, answers }).then(() => {
        this.submitting = false
        this.clearCountdown()
        this.remainingSeconds = 0
        if (done === 'back') {
          // 自动交卷：直接回考核列表（loadList 会带上最新答卷状态）
          this.stage = 'list'
          this.loadList()
        } else {
          this.stage = 'done'
        }
      }).catch(() => {
        this.submitting = false
        // 自动交卷必须让用户看见失败并有机会重试，不能静默吞掉
        if (done === 'back') {
          this.$modal.alertError('自动提交失败，请点击「交卷」重试。').catch(() => {})
        }
      })
    },
    backToList() {
      this.$modal.confirm('退出后本次作答进度不会保存，也不会自动交卷，确定退出吗？').then(() => {
        this.clearCountdown()
        this.remainingSeconds = 0
        this.stage = 'list'
        this.loadList()
      }).catch(() => {})
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
      return { SINGLE: '单选', MULTI: '多选', JUDGE: '判断', SUBJECT: '主观' }[qtype] || qtype
    },
    typeTag(qtype) {
      return { SINGLE: 'success', MULTI: 'warning', JUDGE: 'primary', SUBJECT: 'info' }[qtype] || 'info'
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
.exam-progress { width: 240px; }
.exam-head-right { display: flex; align-items: center; gap: 16px; }
.countdown { display: inline-flex; align-items: center; gap: 6px; padding: 6px 14px; color: #1764f5; background: #e8f1fd; font-size: 13px; border-radius: 18px; }
.countdown b { font-size: 16px; letter-spacing: .04em; font-variant-numeric: tabular-nums; }
.countdown.warn { color: #b54708; background: #fff4e5; }
.countdown.danger { color: #b42318; background: #fee4e2; animation: cd-blink 1s steps(2, start) infinite; }
@keyframes cd-blink { 50% { opacity: .55; } }
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

/* 作答 / 完成（沿用既有实现） */
.subject-card { padding: 20px 22px; margin-bottom: 16px; background: #fff; border: 1px solid #e7ecf3; border-radius: 8px; }
.subject-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding-bottom: 14px; margin-bottom: 14px; border-bottom: 1px solid #edf0f4; }
.subject-label { color: #1764f5; font-size: 12px; font-weight: 600; }
.subject-progress { flex: none; padding: 3px 10px; color: #667085; background: #f2f4f7; font-size: 12px; border-radius: 11px; }
.subject-progress.done { color: #1a7a58; background: #e4f5ee; }
.subject-empty { display: flex; align-items: center; justify-content: center; gap: 8px; padding: 30px 14px; color: #98a2b3; font-size: 13px; }
.subject-empty i { color: #f0a020; font-size: 16px; }
.attachment-link { color: #1764f5; text-decoration: none; }
.attachment-link i { margin-right: 4px; }
.uploaded-name { color: #23966f; font-size: 12.5px; }
.uploaded-name i { margin-right: 3px; }
.upload-waiting { color: #98a2b3; font-size: 12.5px; }
/* 实操题目：左右两卡片 —— 左=描述+附件+上传，右=参考图 */
.subject-q { display: flex; align-items: stretch; gap: 16px; padding: 16px; margin-bottom: 12px; background: #f8fafc; border: 1px solid #eef1f5; border-radius: 10px; }
.s-q-left { flex: 1 1 auto; min-width: 0; display: flex; flex-direction: column; }
.s-q-right { flex: none; width: 300px; display: flex; flex-direction: column; padding: 12px 14px; background: #fff; border: 1px solid #eef1f6; border-radius: 8px; }
.s-q-right-label { margin-bottom: 9px; color: #8490a0; font-size: 12px; font-weight: 600; letter-spacing: 0.5px; }
.s-q-head { display: flex; align-items: center; gap: 9px; margin-bottom: 8px; }
.s-q-index { flex: none; display: inline-flex; width: 22px; height: 22px; align-items: center; justify-content: center; color: #fff; background: #7a5af8; font-size: 12px; border-radius: 50%; }
.s-q-title { flex: 1; min-width: 0; color: #1d2939; font-size: 14px; font-weight: 600; line-height: 1.6; }
.s-q-score { flex: none; color: #1764f5; font-size: 12px; font-weight: 600; }
.s-q-stem { margin-bottom: 10px; color: #475467; font-size: 13px; line-height: 1.75; white-space: pre-wrap; }
.s-q-images { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; }
.s-q-image { width: 100%; height: 108px; object-fit: cover; border: 1px solid #eef1f6; border-radius: 6px; cursor: zoom-in; background: #f2f4f7; }
.s-q-video { object-fit: cover; background: #000; cursor: default; }
.s-q-no-image { display: flex; align-items: center; justify-content: center; min-height: 108px; color: #b0b8c4; font-size: 12.5px; border: 1px dashed #e4e9f0; border-radius: 6px; }
.s-q-attach { margin-bottom: 10px; color: #667085; font-size: 12.5px; }
.s-q-attach-label { margin-right: 6px; color: #8490a0; }
.s-q-attach .attachment-link { margin-right: 14px; }
.s-q-upload { display: flex; align-items: center; gap: 12px; margin-top: auto; padding: 11px 13px; background: #fff; border: 1px dashed #d0d5dd; border-radius: 6px; }
.s-q-upload.ok { background: #f6fdfa; border: 1px solid #c7ecdc; }
.s-q-options { margin-top: 8px; display: grid; gap: 5px; }
.s-q-option { color: #475467; font-size: 13px; line-height: 1.6; }
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
  .subject-q { flex-direction: column; }
  .s-q-right { width: 100%; }
}
</style>
