<template>
  <div class="exam-page">
    <div class="exam-breadcrumb">
      <el-button type="text" icon="el-icon-arrow-left" @click="goBack">返回工作台</el-button>
      <span>/</span>
      <b>参与考核</b>
    </div>

    <!-- 阶段一：考核列表 -->
    <template v-if="stage === 'list'">
      <header class="exam-heading">
        <div><span class="eyebrow">{{ isFormal ? 'ASSESSMENT HISTORY' : (examMode === 'PRACTICE' ? 'PRACTICE CENTER' : 'EXAM CENTER') }}</span><h1>{{ isFormal ? '考核记录' : (examMode === 'PRACTICE' ? '模拟考核' : '参与考核') }}</h1><p>{{ isFormal ? '转正前的考核结果会继续保留，可在此查看。' : (examMode === 'PRACTICE' ? '选择模拟考核进行练习，巩固所学内容。' : '选择已发布的正式考核参加，每人每场考核仅可作答一次。') }}</p></div>
      </header>

      <section v-loading="loading" class="bank-section">
        <div v-if="!visibleExams.length && !loading" class="empty-state">
          <i class="el-icon-document" />
          <strong>{{ isFormal ? '暂无历史考核记录' : '暂无可参加的考核' }}</strong>
          <span>{{ isFormal ? '转正前参加过的考核会保留在这里。' : '部门发布考核后，会出现在这里。' }}</span>
        </div>
        <div v-for="exam in visibleExams" :key="exam.examId" class="exam-row">
          <div class="exam-icon"><i :class="exam.examType === 'PRACTICAL' ? 'el-icon-upload2' : 'el-icon-document'" /></div>
          <div class="exam-info">
            <h3>{{ exam.examName }}</h3>
            <p>
              <el-tag size="mini" :type="exam.examType === 'PRACTICAL' ? 'danger' : 'success'" effect="plain">{{ exam.examType === 'PRACTICAL' ? '实操考核' : '理论考核' }}</el-tag>
              <span class="exam-desc">{{ exam.examType === 'PRACTICAL' ? '上传文件作答' : '客观 ' + exam.questionCount + ' 题' }} · 通过线 {{ exam.passLine }} 分</span>
            </p>
          </div>
          <div class="exam-status">
            <template v-if="!exam.sheet">
              <el-tag v-if="exam.status === 'PUBLISHED'" size="mini" type="success" effect="plain">可参加</el-tag>
              <el-tag v-else size="mini" type="info" effect="plain">已结束</el-tag>
            </template>
            <template v-else-if="exam.sheet.status === 'PUBLISHED'">
              <el-tag v-if="exam.sheet.passFlag === 1" size="mini" type="success">通关 {{ exam.sheet.finalScore }} 分</el-tag>
              <el-tag v-else size="mini" type="danger">未通过 {{ exam.sheet.finalScore }} 分</el-tag>
            </template>
            <template v-else>
              <el-tag size="mini" type="warning">批阅中</el-tag>
            </template>
          </div>
          <div class="exam-action">
            <el-button v-if="!isFormal && !exam.sheet && exam.status === 'PUBLISHED'" type="primary" size="small" icon="el-icon-arrow-right" @click="startExam(exam)">参加考核</el-button>
            <span v-else-if="exam.sheet && exam.sheet.status === 'PUBLISHED'" class="done-text">已完成</span>
          </div>
        </div>
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
      submitting: false
    }
  },
  computed: {
    baseApi() { return process.env.VUE_APP_BASE_API || '' },
    isFormal() { return this.$store.getters.roles.indexOf('FORMAL_TRAINEE') > -1 },
    visibleExams() { return this.isFormal ? this.exams.filter(exam => !!exam.sheet) : this.exams },
    examMode() { return this.$route.name === 'InternMockExam' ? 'PRACTICE' : 'FORMAL' },
    progress() {
      const answered = this.answers.filter(a => {
        if (Array.isArray(a)) return a.length > 0
        return !!a
      }).length
      return this.questions.length ? Math.round(answered / this.questions.length * 100) : 0
    }
  },
  created() {
    this.loadList()
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
.exam-heading { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 20px; }
.eyebrow { color: #1764f5; font-size: 11px; letter-spacing: .08em; }
.exam-heading h1 { margin: 5px 0 7px; color: #1d2939; font-size: 26px; font-weight: 600; }
.exam-heading p { margin: 0; color: #667085; font-size: 13px; }
.exam-progress { width: 240px; }
.bank-section { min-height: 220px; padding: 10px 4px; }
.exam-row { display: flex; align-items: center; gap: 16px; padding: 20px 16px; margin-bottom: 14px; background: #fff; border: 1px solid #e7ecf3; border-radius: 8px; }
.exam-icon { display: flex; width: 50px; height: 50px; flex: 0 0 50px; align-items: center; justify-content: center; color: #fff; background: #1764f5; font-size: 24px; border-radius: 8px; }
.exam-info { flex: 1; min-width: 0; }
.exam-info h3 { margin: 0 0 5px; color: #1d2939; font-size: 16px; font-weight: 600; }
.exam-info p { margin: 0; color: #667085; font-size: 12px; display: flex; align-items: center; gap: 8px; }
.exam-desc { color: #667085; font-size: 12px; }
.exam-status { min-width: 110px; text-align: center; }
.exam-action { min-width: 110px; text-align: right; }
.done-text { color: #98a2b3; font-size: 13px; }
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
.empty-state { display: flex; min-height: 240px; align-items: center; justify-content: center; flex-direction: column; gap: 9px; color: #98a2b3; }
.empty-state i { color: #b7c1cc; font-size: 40px; }
.empty-state strong { color: #667085; font-size: 15px; }
.empty-state span { font-size: 12px; }
</style>
