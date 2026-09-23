<template>
  <div class="answering-page">
    <!-- 交卷完成：整屏替换，避免交卷后还能点到试卷 -->
    <div v-if="stage === 'done'" class="done-wrap">
      <div class="done-state">
        <div class="done-icon"><i class="el-icon-success" /></div>
        <h2>作答完成</h2>
        <p>{{ examType === 'PRACTICAL' ? '你的作答文件已提交，管理员批阅后会发布成绩。' : '你的答卷已提交，成绩已自动判定，请返回考核列表查看结果。' }}</p>
        <el-button type="primary" size="medium" @click="leave">返回考核列表</el-button>
      </div>
    </div>

    <template v-else>
      <!-- 顶栏：考核名 / 保存状态 / 倒计时 / 作答进度 -->
      <header class="ap-bar">
        <div class="ap-bar-left">
          <span class="ap-brand">正式考核</span>
          <b class="ap-title">{{ currentExamName || '在线作答' }}</b>
          <span class="ap-type">{{ examType === 'PRACTICAL' ? '实操考核' : '理论考核' }}</span>
        </div>
        <div class="ap-bar-right">
          <span class="ap-save" :class="saveTone">
            <i :class="saveIcon" />
            <span>{{ saveText }}</span>
          </span>
          <el-button v-if="stage === 'exam'" size="mini" plain :loading="savingDraft" @click="saveDraftNow(true)">保存进度</el-button>
          <div v-if="remainingSeconds > 0" class="countdown" :class="countdownTone">
            <i class="el-icon-alarm-clock" />
            <span>剩余</span>
            <b>{{ countdownText }}</b>
          </div>
          <div v-if="examType === 'THEORY' && stage === 'exam'" class="exam-progress">
            <el-progress :percentage="progress" :stroke-width="8" />
          </div>
        </div>
      </header>

      <!-- 题号导航（仅理论卷）：点一下滚到那道题 -->
      <div v-if="stage === 'exam' && examType === 'THEORY' && questions.length" class="ap-nav">
        <span class="ap-nav-label">答题卡</span>
        <button
          v-for="(q, i) in questions"
          :key="'n' + q.id"
          class="ap-nav-item"
          :class="{ done: isAnswered(i) }"
          @click="scrollToQuestion(i)"
        >{{ i + 1 }}</button>
        <span class="ap-nav-legend">
          <i class="dot done" />已作答 {{ answeredCount }}
          <i class="dot" />未作答 {{ questions.length - answeredCount }}
        </span>
      </div>

      <div class="ap-body">
        <!-- 加载中 -->
        <div v-if="stage === 'loading'" class="ap-state">
          <i class="el-icon-loading" />
          <span>正在准备试题…</span>
        </div>

        <!-- 加载失败：给出明确原因与出口，不留空白 -->
        <div v-else-if="stage === 'error'" class="ap-state error">
          <i class="el-icon-warning-outline" />
          <span>{{ loadError || '无法打开本次考核，请返回列表重试' }}</span>
          <el-button size="small" @click="leave">返回考核列表</el-button>
        </div>

        <template v-else>
          <!-- 续答提示：让用户明确知道上次的作答已经回来了 -->
          <div v-if="resumed" class="ap-resume">
            <i class="el-icon-refresh-left" />
            <span>已恢复上次作答进度<template v-if="resumeSummary">（{{ resumeSummary }}）</template>，倒计时按上次开始时间继续，不会重置。</span>
          </div>

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
                  <!-- ★ 不能加 :limit="1"：auto-upload=false 时第二选择会被 limit 静默吞掉（不弹 exceed 提示、
                       文件列表又隐藏）→ 用户点「重新上传」完全没反应。改为每次 on-change 覆盖最新文件。 -->
                  <el-upload
                    :auto-upload="false"
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
                    <a :href="baseApi + subjectFiles[s.subjectItemId].path" target="_blank" class="attachment-link" style="margin-left:8px">查看</a>
                  </span>
                  <span v-else class="upload-waiting">本题尚未上传作答文件</span>
                </div>
              </div>

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
            <div v-for="(q, index) in questions" :key="q.id" :id="'ap-q-' + index" class="question-card">
              <div class="q-head">
                <span class="q-index">{{ index + 1 }}</span>
                <el-tag size="mini" effect="plain" :type="typeTag(q.qtype)">{{ typeLabel(q.qtype) }}</el-tag>
                <span class="q-score">{{ q.score }} 分</span>
              </div>
              <div class="q-stem">{{ q.stem }}</div>
              <div class="q-options">
                <template v-if="q.qtype === 'MULTI'">
                  <el-checkbox-group v-model="answers[index]" @change="markDirty">
                    <div v-for="opt in parseOptions(q.optionsJson)" :key="opt.key" class="option-item">
                      <el-checkbox :label="opt.key">{{ opt.key }}. {{ opt.content }}</el-checkbox>
                    </div>
                  </el-checkbox-group>
                </template>
                <template v-else>
                  <el-radio-group v-model="answers[index]" @change="markDirty">
                    <div v-for="opt in parseOptions(q.optionsJson)" :key="opt.key" class="option-item">
                      <el-radio :label="opt.key">{{ opt.key }}. {{ opt.content }}</el-radio>
                    </div>
                  </el-radio-group>
                </template>
              </div>
            </div>
          </div>
        </template>
      </div>

      <!-- 底栏：常驻，不随内容滚走 -->
      <footer v-if="stage === 'exam'" class="ap-footer">
        <span class="ap-foot-meta">
          <template v-if="examType === 'THEORY'">已作答 {{ answeredCount }} / {{ questions.length }} 题</template>
          <template v-else>已上传 {{ uploadedCount }} / {{ subjectItems.length }} 题</template>
          <span class="ap-foot-tip">· 作答进度自动保存，中途离开可回来继续</span>
        </span>
        <div class="ap-foot-actions">
          <el-button size="medium" :disabled="submitting" @click="giveUp">放弃作答</el-button>
          <el-button size="medium" plain :loading="savingDraft" @click="saveDraftNow(true)">保存进度</el-button>
          <el-button type="primary" size="medium" :loading="submitting" @click="confirmSubmit">交 卷</el-button>
        </div>
      </footer>
    </template>
  </div>
</template>

<script>
import { startExam, submitExam, saveDraft, uploadFile } from '@/api/business/exam'

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
/** 自动保存间隔（毫秒）：20s —— 够及时，又不至于把接口打爆 */
const AUTO_SAVE_INTERVAL = 20000

export default {
  name: 'InternExamAnswering',
  data() {
    return {
      /** loading（拉题中）/ exam（作答中）/ done（已交卷）/ error（拉题失败） */
      stage: 'loading',
      loadError: '',
      sheetId: null,
      currentExamName: '',
      examType: 'THEORY',
      /** 后端标记：本次是「续答」而不是新开 —— 用于提示 + 说明倒计时不重置 */
      resumed: false,
      questions: [],
      answers: [],
      subjectItems: [],
      subjectFiles: {},
      uploadingItemId: null,
      submitting: false,
      /** 剩余作答秒数（服务端下发；0 = 不限时，不启动倒计时） */
      remainingSeconds: 0,
      countdownTimer: null,
      /** 进度保存 */
      draftTimer: null,
      savingDraft: false,
      dirty: false,
      lastSavedAt: null
    }
  },
  computed: {
    baseApi() { return process.env.VUE_APP_BASE_API || '' },
    examId() { return this.$route.query.examId },
    /** 回跳地址：由列表页带过来；缺失时按考核性质给默认值（避免掉到 404） */
    backPath() { return this.$route.query.from || '/assessment/intern/learning/exam' },
    progress() {
      return this.questions.length ? Math.round(this.answeredCount / this.questions.length * 100) : 0
    },
    answeredCount() {
      return this.answers.filter(a => (Array.isArray(a) ? a.length > 0 : !!a)).length
    },
    /** 实操：已上传作答文件的题目数 */
    uploadedCount() {
      return this.subjectItems.filter(s => !!this.subjectFiles[s.subjectItemId]).length
    },
    /** 续答提示里的一句话摘要：让用户一眼看出恢复了几题 */
    resumeSummary() {
      if (!this.resumed) return ''
      if (this.examType === 'PRACTICAL') {
        return this.uploadedCount > 0 ? '已恢复 ' + this.uploadedCount + ' 道题的作答文件' : ''
      }
      return this.answeredCount > 0 ? '已恢复 ' + this.answeredCount + ' 题答案' : ''
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
    },
    saveText() {
      if (this.savingDraft) return '保存中…'
      if (this.dirty) return '有未保存的改动'
      if (this.lastSavedAt) return '已保存 ' + this.fmtClock(this.lastSavedAt)
      return '进度自动保存'
    },
    saveTone() {
      if (this.savingDraft) return 'saving'
      if (this.dirty) return 'dirty'
      return this.lastSavedAt ? 'saved' : ''
    },
    saveIcon() {
      if (this.savingDraft) return 'el-icon-loading'
      if (this.dirty) return 'el-icon-edit-outline'
      return 'el-icon-cloudy'
    }
  },
  created() {
    if (!this.examId) {
      this.stage = 'error'
      this.loadError = '缺少考核参数，请从考核列表点「开始考试」进入'
      return
    }
    this.loadPaper()
  },
  mounted() {
    document.addEventListener('visibilitychange', this.onVisibility)
    window.addEventListener('beforeunload', this.handleBeforeUnload)
  },
  beforeDestroy() {
    document.removeEventListener('visibilitychange', this.onVisibility)
    window.removeEventListener('beforeunload', this.handleBeforeUnload)
    this.stopCountdown()
    this.stopAutoSave()
  },
  /**
   * 离开守卫：作答中先落一次进度再放行（SPA 内跳转不会中断 XHR，所以这里能等到结果）。
   * 不再用「进度不会保存」的吓人文案 —— 现在真的会保存。
   */
  beforeRouteLeave(to, from, next) {
    if (this.stage !== 'exam' || this.submitting) {
      this.teardown()
      next()
      return
    }
    this.$modal.confirm('退出后本次作答进度会保存在服务器，下次进入可继续作答（不会自动交卷）。确定退出吗？').then(() => {
      this.saveDraftNow(false).then(() => { this.teardown(); next() }).catch(() => { this.teardown(); next() })
    }).catch(() => next(false))
  },
  methods: {
    /** 拉题 / 续答：后端 startExam 对 IN_PROGRESS 的答卷会**续答同一份**并回传 resumed=true */
    loadPaper() {
      this.stage = 'loading'
      startExam(this.examId).then(res => {
        this.applyPaper(res.data || {})
      }).catch(err => {
        // 后端会带业务原因（已截止 / 已参加过 / 不在参考名单…），原样透出，不要伪装成空态
        this.stage = 'error'
        this.loadError = (err && err.message) || '无法打开本次考核，请返回列表重试'
      })
    },
    applyPaper(data) {
      this.sheetId = data.sheetId
      this.currentExamName = data.examName
      this.examType = data.examType || 'THEORY'
      this.resumed = !!data.resumed

      // 理论：把上次的答案回填（多选按逗号拆回数组，与交卷时的 join(',') 对称）
      this.questions = data.questions || []
      this.answers = this.questions.map(q => {
        const ua = q.userAnswer
        if (q.qtype === 'MULTI') {
          return ua ? String(ua).split(',').map(s => s.trim()).filter(s => s) : []
        }
        return ua == null ? '' : String(ua)
      })

      // 实操：把上次上传的文件回填（只剩 path，故用文件名兜底展示）
      this.subjectItems = data.subjectItems || []
      const files = {}
      this.subjectItems.forEach(s => {
        if (s.userAnswer) files[s.subjectItemId] = { path: s.userAnswer, name: this.fileNameOf(s.userAnswer) }
      })
      this.subjectFiles = files

      this.uploadingItemId = null
      this.dirty = false
      this.savingDraft = false
      this.lastSavedAt = this.resumed ? new Date() : null
      this.stage = 'exam'
      this.startCountdown(Number(data.remainingSeconds) || 0)
      this.startAutoSave()
    },
    /**
     * 从上传路径里还原一个可读的文件名。
     * 后端 `FileUploadUtils.extractFilename` 的命名是 **`{原basename}_{Seq}.{ext}`**
     * （Seq = `yyyyMMddHHmmssA###`，见 `Seq.getId(uploadSeqType)`）——
     * ★ 原名在**前**、流水号在**后**，所以必须剥**尾部**那一段；
     *   早先按「uuid_原名」写成就取第一个 `_` 之后，会把原名丢掉、只显示流水号。
     */
    fileNameOf(path) {
      const seg = decodeURIComponent(String(path || '').split('/').pop() || '')
      const m = seg.match(/^(.*)_\d{14}A\d+(\.[^.]+)?$/)
      return m ? m[1] + (m[2] || '') : seg
    },
    fmtClock(d) {
      const pad = n => (n < 10 ? '0' + n : '' + n)
      return pad(d.getHours()) + ':' + pad(d.getMinutes()) + ':' + pad(d.getSeconds())
    },
    /* ---------------- 进度保存 ---------------- */
    markDirty() { this.dirty = true },
    startAutoSave() {
      this.stopAutoSave()
      this.draftTimer = setInterval(() => {
        if (this.dirty && !this.submitting && !this.savingDraft) this.saveDraftNow(false)
      }, AUTO_SAVE_INTERVAL)
    },
    stopAutoSave() {
      if (this.draftTimer) { clearInterval(this.draftTimer); this.draftTimer = null }
    },
    /** 切到后台（切标签 / 最小化）时立刻落一次进度 */
    onVisibility() {
      if (document.hidden && this.stage === 'exam' && this.dirty) this.saveDraftNow(false)
    },
    /**
     * 有未保存改动时才拦一下刷新/关闭。
     * 之所以敢拦：正常路径（自动保存 / 切后台 / 路由离开）已经把进度写库了，dirty 通常为 false。
     */
    handleBeforeUnload(e) {
      if (this.stage !== 'exam' || this.submitting || !this.dirty) return
      this.saveDraftNow(false)
      e.preventDefault()
      e.returnValue = ''
    },
    /** 组包：交卷送**全部**题目（含未作答，保持原语义）；存草稿只送**有内容**的（后端也是这个口径） */
    buildAnswers(draftOnly) {
      if (this.examType === 'PRACTICAL') {
        return this.subjectItems
          .filter(s => !!this.subjectFiles[s.subjectItemId])
          .map(s => ({ questionId: String(s.subjectItemId), userAnswer: this.subjectFiles[s.subjectItemId].path }))
      }
      const all = this.questions.map((q, i) => {
        const ans = this.answers[i]
        return { questionId: q.id, userAnswer: Array.isArray(ans) ? ans.slice().sort().join(',') : (ans == null ? '' : ans) }
      })
      return draftOnly ? all.filter(a => String(a.userAnswer).length > 0) : all
    },
    /** 保存进度（返回 Promise：路由离开守卫要等它落库） */
    saveDraftNow(fromUser) {
      if (this.stage !== 'exam' || this.submitting || this.savingDraft || !this.sheetId) {
        return Promise.resolve(false)
      }
      const answers = this.buildAnswers(true)
      // 一题都没作答 / 一个文件都没传：没有可保存的内容，静默跳过（手动点击才提示）
      if (!answers.length) {
        if (fromUser) this.$modal.msgWarning('还没有可保存的作答内容')
        return Promise.resolve(false)
      }
      this.savingDraft = true
      return saveDraft({ sheetId: this.sheetId, answers }).then(() => {
        this.savingDraft = false
        this.dirty = false
        this.lastSavedAt = new Date()
        if (fromUser) this.$modal.msgSuccess('作答进度已保存')
        return true
      }).catch(err => {
        this.savingDraft = false
        // 保存失败**必须**让用户看见（否则会出现「以为存了、回来却空了」）
        this.$modal.msgError((err && err.message) || '作答进度保存失败，请检查网络后重试')
        return false
      })
    },
    /* ---------------- 倒计时 ---------------- */
    startCountdown(seconds) {
      this.stopCountdown()
      this.remainingSeconds = seconds > 0 ? seconds : 0
      if (this.remainingSeconds <= 0) return
      this.countdownTimer = setInterval(() => {
        if (this.remainingSeconds <= 1) {
          this.remainingSeconds = 0
          this.stopCountdown()
          this.onTimeUp()
          return
        }
        this.remainingSeconds -= 1
      }, 1000)
    },
    stopCountdown() {
      if (this.countdownTimer) { clearInterval(this.countdownTimer); this.countdownTimer = null }
    },
    onTimeUp() {
      if (this.stage !== 'exam' || this.submitting) return
      this.stopCountdown()
      this.remainingSeconds = 0
      this.$modal.confirmOnly('考试时间已到，将自动提交本次作答。').then(() => {
        this.doSubmit('back')
      }).catch(() => {})
    },
    /* ---------------- 作答 ---------------- */
    isAnswered(i) {
      const a = this.answers[i]
      return Array.isArray(a) ? a.length > 0 : !!a
    },
    scrollToQuestion(i) {
      const el = document.getElementById('ap-q-' + i)
      if (el && el.scrollIntoView) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
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
        this.dirty = true
        this.$modal.msgSuccess('第 ' + (subject.seq || '') + ' 题作答文件上传成功')
        // 上传完立刻落库，避免用户「传了就走」导致文件丢了关联
        this.saveDraftNow(false)
      }).catch(() => { this.uploadingItemId = null })
    },
    subjectImages(s) { return parseJsonList(s.referenceImages) },
    subjectAttachments(s) { return parseJsonList(s.attachments) },
    subjectPreview(s) { return this.subjectImages(s).filter(i => !this.isVideo(i.url)).map(i => this.baseApi + i.url) },
    isVideo(url) { return /\.(mp4|webm|ogg|ogv|mov|avi|m4v)$/i.test(String(url || '')) },
    parseOptions(json) {
      try { return JSON.parse(json || '[]') } catch (e) { return [] }
    },
    typeLabel(qtype) {
      return { SINGLE: '单选', MULTI: '多选', JUDGE: '判断', SUBJECT: '主观' }[qtype] || qtype
    },
    typeTag(qtype) {
      return { SINGLE: 'success', MULTI: 'warning', JUDGE: 'primary', SUBJECT: 'info' }[qtype] || 'info'
    },
    /* ---------------- 交卷 / 退出 ---------------- */
    confirmSubmit() {
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
      const unanswered = this.questions.length - this.answeredCount
      if (unanswered > 0) {
        this.$modal.confirm(`还有 ${unanswered} 题未作答，确定交卷吗？`).then(() => this.doSubmit()).catch(() => {})
        return
      }
      this.$modal.confirm('确认交卷吗？提交后不可修改。').then(() => this.doSubmit()).catch(() => {})
    },
    /**
     * 交卷。
     * @param {String} [done] 传 'back' 表示倒计时到点自动交卷：提交成功后直接回列表；
     *                        不传则走「作答完成」屏。
     */
    doSubmit(done) {
      this.submitting = true
      this.stopAutoSave()
      submitExam({ sheetId: this.sheetId, answers: this.buildAnswers(false) }).then(() => {
        this.submitting = false
        this.stopCountdown()
        this.remainingSeconds = 0
        this.dirty = false
        if (done === 'back') {
          this.leave()
        } else {
          this.stage = 'done'
        }
      }).catch(() => {
        this.submitting = false
        this.startAutoSave()
        // 交卷失败必须让用户看见并有机会重试，不能静默吞掉
        if (done === 'back') {
          this.$modal.alertError('自动提交失败，请点击「交卷」重试。').catch(() => {})
        } else {
          this.$modal.alertError('交卷失败，请重试（作答进度已自动保留）。').catch(() => {})
        }
      })
    },
    /** 放弃作答：进度已保存，明确告诉用户「还能回来继续」 */
    giveUp() {
      this.$modal.confirm('退出后本次作答进度会保存在服务器，下次进入可继续作答（不会自动交卷）。确定退出吗？').then(() => {
        this.saveDraftNow(false).then(() => this.leave()).catch(() => this.leave())
      }).catch(() => {})
    },
    teardown() {
      this.stopCountdown()
      this.stopAutoSave()
      this.remainingSeconds = 0
    },
    /** 回列表：replace 不留作答页历史（避免浏览器后退又回到试卷） */
    leave() {
      this.teardown()
      this.$router.replace(this.backPath)
    }
  }
}
</script>

<style lang="scss" scoped>
/* 全屏作答：整页 100vh 三栏（顶栏 / 可滚区 / 底栏），底栏常驻不随内容滚走 */
.answering-page { position: fixed; top: 0; right: 0; bottom: 0; left: 0; display: flex; flex-direction: column; color: #283544; background: #f5f7fa; }
.ap-bar { flex: none; display: flex; align-items: center; justify-content: space-between; gap: 16px; height: 60px; padding: 0 24px; background: #fff; border-bottom: 1px solid #e7ecf3; }
.ap-bar-left { display: flex; align-items: center; gap: 12px; min-width: 0; }
.ap-brand { flex: none; padding: 3px 10px; color: #1764f5; background: #e8f1fd; font-size: 12px; border-radius: 4px; }
.ap-title { overflow: hidden; color: #1d2939; font-size: 16px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.ap-type { flex: none; color: #98a2b3; font-size: 12px; }
.ap-bar-right { display: flex; flex: none; align-items: center; gap: 14px; }
.ap-save { display: inline-flex; align-items: center; gap: 5px; color: #98a2b3; font-size: 12px; }
.ap-save.saving { color: #1764f5; }
.ap-save.dirty { color: #b54708; }
.ap-save.saved { color: #1a7a58; }
.exam-progress { width: 200px; }
.countdown { display: inline-flex; align-items: center; gap: 6px; padding: 6px 14px; color: #1764f5; background: #e8f1fd; font-size: 13px; border-radius: 18px; }
.countdown b { font-size: 16px; letter-spacing: .04em; font-variant-numeric: tabular-nums; }
.countdown.warn { color: #b54708; background: #fff4e5; }
.countdown.danger { color: #b42318; background: #fee4e2; animation: cd-blink 1s steps(2, start) infinite; }
@keyframes cd-blink { 50% { opacity: .55; } }

/* 答题卡（题号导航） */
.ap-nav { flex: none; display: flex; flex-wrap: wrap; align-items: center; gap: 7px; padding: 9px 24px; background: #fbfcfe; border-bottom: 1px solid #edf0f4; }
.ap-nav-label { margin-right: 4px; color: #8490a0; font-size: 12px; }
.ap-nav-item { width: 26px; height: 26px; color: #667085; background: #fff; border: 1px solid #dfe4ec; font-size: 12px; border-radius: 4px; cursor: pointer; }
.ap-nav-item:hover { border-color: #1764f5; color: #1764f5; }
.ap-nav-item.done { color: #fff; background: #1764f5; border-color: #1764f5; }
.ap-nav-legend { display: inline-flex; align-items: center; gap: 6px; margin-left: auto; color: #98a2b3; font-size: 12px; }
.ap-nav-legend .dot { display: inline-block; width: 8px; height: 8px; background: #fff; border: 1px solid #dfe4ec; border-radius: 50%; }
.ap-nav-legend .dot.done { background: #1764f5; border-color: #1764f5; }

/* 可滚动的试卷区 */
.ap-body { flex: 1; overflow-y: auto; padding: 20px 24px 28px; }
.ap-resume { display: flex; align-items: center; gap: 8px; margin-bottom: 14px; padding: 11px 15px; color: #1a7a58; background: #eaf8f1; border: 1px solid #c7ecdc; font-size: 12.5px; border-radius: 7px; }
.ap-state { display: flex; min-height: 420px; align-items: center; justify-content: center; flex-direction: column; gap: 10px; color: #98a2b3; font-size: 13px; }
.ap-state i { color: #b7c1cc; font-size: 34px; }
.ap-state.error i { color: #f0a020; }

/* 操作栏 */
.ap-footer { flex: none; display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 13px 24px; background: #fff; border-top: 1px solid #e7ecf3; }
.ap-foot-meta { color: #475467; font-size: 12.5px; }
.ap-foot-tip { color: #98a2b3; }
.ap-foot-actions { display: flex; gap: 12px; }

/* 实操题卡片 */
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

/* 理论题卡片 */
.question-card { margin-bottom: 16px; padding: 20px 22px; background: #fff; border: 1px solid #e7ecf3; border-radius: 8px; }
.q-head { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.q-index { display: inline-flex; width: 24px; height: 24px; align-items: center; justify-content: center; color: #fff; background: #1764f5; font-size: 13px; border-radius: 50%; }
.q-score { margin-left: auto; color: #8490a0; font-size: 12px; }
.q-stem { margin-bottom: 14px; color: #1d2939; font-size: 15px; line-height: 1.7; }
.q-options .option-item { padding: 9px 12px; margin-bottom: 6px; border: 1px solid #edf0f4; border-radius: 6px; }
.q-options .option-item:hover { background: #f8fafc; }
.option-item .el-radio, .option-item .el-checkbox { display: block; margin-right: 0; line-height: 1.6; }

/* 完成态 */
.done-wrap { display: flex; flex: 1; align-items: center; justify-content: center; padding: 24px; }
.done-state { display: flex; width: 100%; max-width: 560px; min-height: 380px; align-items: center; justify-content: center; flex-direction: column; gap: 8px; background: #fff; border: 1px solid #e7ecf3; border-radius: 10px; }
.done-icon { color: #23966f; font-size: 64px; }
.done-state h2 { margin: 8px 0; color: #1d2939; font-size: 22px; font-weight: 600; }
.done-state p { color: #667085; font-size: 14px; margin-bottom: 16px; text-align: center; }

@media (max-width: 760px) {
  .ap-bar { height: auto; flex-direction: column; align-items: flex-start; gap: 8px; padding: 12px 14px; }
  .ap-bar-right { flex-wrap: wrap; }
  .ap-body { padding: 14px 12px 20px; }
  .ap-footer { flex-direction: column; align-items: stretch; }
  .subject-q { flex-direction: column; }
  .s-q-right { width: 100%; }
}
</style>
