<template>
  <div class="practice-page">
    <div class="exam-breadcrumb">
      <el-button type="text" icon="el-icon-arrow-left" @click="goBack">返回正式考核</el-button>
      <span>/</span>
      <b>单场考核结果</b>
    </div>

    <header class="exam-heading">
      <div>
        <span class="eyebrow">EXAM RESULT</span>
        <h1>{{ batchName }}</h1>
      </div>
      <el-button size="medium" icon="el-icon-refresh" :loading="loading" @click="load">刷新</el-button>
    </header>

    <div v-loading="loading" class="er-body">
      <div v-if="!loading && !exams.length" class="empty-state small">
        <i class="el-icon-document" />
        <span>未找到该场考核，或你没有参加权限。</span>
      </div>

      <template v-else>
        <!-- 本场汇总 -->
        <section class="er-summary">
          <div v-for="c in summaryCells" :key="c.label" class="er-cell">
            <span class="er-label">{{ c.label }}</span>
            <b :class="c.tone">{{ c.value }}</b>
          </div>
        </section>

        <!-- 每个环节一张卡（同一场次可能含理论 + 实操） -->
        <section v-for="e in exams" :key="e.examId" class="er-card">
          <div class="er-card-head">
            <div class="er-title">
              <span class="er-badge" :class="e.examType === 'PRACTICAL' ? 'prac' : 'theory'">
                {{ e.examType === 'PRACTICAL' ? '实操' : '理论' }}
              </span>
              <h2>{{ e.examName || '未命名考核' }}</h2>
            </div>
            <div class="er-card-meta">
              <span>通过线 {{ passLineOf(e) }} 分</span>
              <el-tag size="mini" effect="plain" :type="stateOf(e).tag">{{ stateOf(e).text }}</el-tag>
            </div>
          </div>

          <div class="er-score-row">
            <span>得分 <b class="er-score" :class="scoreTone(e)">{{ scoreOf(e) }}</b></span>
            <span class="er-sep">/</span>
            <span>卷面满分 <b>{{ fullOf(e) }}</b></span>
          </div>

          <div v-if="!e.sheet" class="er-hint">本环节未参加</div>
          <div v-else-if="!itemsOf(e.examId).length" class="er-hint">
            本环节无逐题明细（实操类考核由导师批阅打分）
          </div>
          <table v-else class="er-table">
            <thead>
              <tr><th>题号</th><th>题型</th><th>题干</th><th>我的作答</th><th>正确答案</th><th>得分</th><th>结果</th></tr>
            </thead>
            <tbody>
              <tr v-for="it in itemsOf(e.examId)" :key="it.id">
                <td class="ctr">{{ it.seq }}</td>
                <td class="ctr">{{ typeLabel(it.qtype) }}</td>
                <td class="stem">
                  {{ it.stem || '（题目已被修改，无题干快照）' }}
                  <ul v-if="optsOf(it).length" class="er-opts">
                    <li
                      v-for="o in optsOf(it)"
                      :key="o.key"
                      :class="{ right: rightOf(it, o.key), wrong: pickedOf(it, o.key) && !rightOf(it, o.key) }"
                    >
                      <b>{{ o.key }}</b><span>{{ o.content }}</span>
                      <em v-if="pickedOf(it, o.key)">我的选择</em>
                    </li>
                  </ul>
                </td>
                <td class="answer">
                  <a v-if="isFile(it)" :href="baseApi + it.userAnswer" target="_blank" download>下载作答文件</a>
                  <span v-else class="ans-text">{{ it.userAnswer || '未作答' }}</span>
                </td>
                <td class="ctr">{{ it.answer || '—' }}</td>
                <td class="ctr"><b>{{ itemScore(it) }}</b> / {{ it.score == null ? '—' : it.score }}</td>
                <td class="ctr">
                  <el-tag v-if="isSubjective(it)" size="mini" effect="plain" type="info">人工</el-tag>
                  <el-tag v-else size="mini" :type="it.isCorrect === 1 ? 'success' : 'danger'">
                    {{ it.isCorrect === 1 ? '正确' : '错误' }}
                  </el-tag>
                </td>
              </tr>
            </tbody>
          </table>

          <div v-if="commentOf(e.examId)" class="er-comment">
            <b>导师评语：</b>{{ commentOf(e.examId) }}
          </div>
        </section>

        <div class="action-bar">
          <el-button size="medium" @click="goBack">返回正式考核</el-button>
          <el-button size="medium" type="primary" @click="goAll">查看全部成绩</el-button>
        </div>
      </template>
    </div>
  </div>
</template>

<script>
import { myExamList, mySheetDetail } from '@/api/business/exam'

export default {
  name: 'InternExamResult',
  data() {
    return {
      loading: false,
      exams: [],
      /** examId -> 逐题明细 */
      details: {}
    }
  },
  computed: {
    baseApi() {
      return process.env.VUE_APP_BASE_API || ''
    },
    wantedIds() {
      const raw = String(this.$route.query.exams || '')
      return raw.split(',').map(s => Number(s)).filter(n => !isNaN(n) && n > 0)
    },
    batchName() {
      if (this.exams.length === 1) return this.exams[0].examName || '考核结果'
      return this.exams.length ? this.exams[0].examName + ' 等 ' + this.exams.length + ' 个环节' : '考核结果'
    },
    summaryCells() {
      const total = this.exams.reduce((acc, e) => acc + (Number(this.scoreOf(e)) || 0), 0)
      const full = this.exams.reduce((acc, e) => acc + (Number(this.fullOf(e)) || 0), 0)
      const published = this.exams.filter(e => e.sheet && e.sheet.status === 'PUBLISHED')
      const pending = this.exams.some(e => e.sheet && e.sheet.status !== 'PUBLISHED')
      const allPassed = published.length > 0 && published.every(e => e.sheet.passFlag === 1)
      let result = '待批阅'
      let tone = 'warn'
      if (published.length && !pending) {
        result = allPassed ? '已通过' : '未通过'
        tone = allPassed ? 'ok' : 'bad'
      } else if (!published.length) {
        result = '未参加'
        tone = 'muted'
      }
      return [
        { label: '环节数', value: this.exams.length + ' 项', tone: '' },
        { label: '得分合计', value: this.round1(total), tone: '' },
        { label: '卷面满分', value: full ? this.round1(full) : '—', tone: '' },
        { label: '已出分环节', value: published.length + ' / ' + this.exams.length, tone: '' },
        { label: '本场结果', value: result, tone }
      ]
    }
  },
  watch: {
    '$route.query.exams'() { this.load() }
  },
  created() {
    this.load()
  },
  methods: {
    load() {
      if (!this.wantedIds.length) {
        this.exams = []
        this.details = {}
        return
      }
      this.loading = true
      myExamList('FORMAL').then(res => {
        const all = (res && res.data) || []
        this.exams = this.wantedIds
          .map(id => all.find(e => e.examId === id))
          .filter(e => !!e)
        // 逐题明细：只对有答卷的环节拉取
        this.exams.filter(e => e.sheet && e.sheet.sheetId).forEach(e => this.loadDetail(e))
        this.loading = false
      }).catch(() => {
        this.exams = []
        this.details = {}
        this.loading = false
      })
    },
    loadDetail(exam) {
      // 走「本人」接口：管理员用的 /detail 带部门管理范围校验，实习生调用会被拒，
      // 这正是"未通过的考核看不到详情"的原因。
      mySheetDetail(exam.sheet.sheetId).then(res => {
        const items = (res.data && res.data.items) || []
        this.$set(this.details, exam.examId, items)
      }).catch(() => {
        this.$set(this.details, exam.examId, [])
      })
    },
    itemsOf(examId) {
      return this.details[examId] || []
    },
    /** 成绩：优先最终分，其次人工分、AI 分 */
    scoreOf(e) {
      if (!e || !e.sheet) return '—'
      const s = e.sheet
      if (s.finalScore != null) return this.round1(s.finalScore)
      if (s.manualScore != null) return this.round1(s.manualScore)
      if (s.aiScore != null) return this.round1(s.aiScore)
      return '—'
    },
    scoreTone(e) {
      if (!e || !e.sheet || e.sheet.status !== 'PUBLISHED') return 'muted'
      return e.sheet.passFlag === 1 ? 'ok' : 'bad'
    },
    /** 卷面满分：按该答卷的逐题满分求和 */
    fullOf(e) {
      const items = this.itemsOf(e.examId)
      if (!items.length) return '—'
      const sum = items.reduce((acc, it) => acc + (Number(it.score) || 0), 0)
      return sum ? this.round1(sum) : '—'
    },
    passLineOf(e) {
      return e.passLine == null ? '—' : e.passLine
    },
    stateOf(e) {
      if (!e.sheet) return { text: '未参加', tag: 'info' }
      if (e.sheet.status === 'PUBLISHED') {
        return e.sheet.passFlag === 1 ? { text: '已通过', tag: 'success' } : { text: '未通过', tag: 'danger' }
      }
      return { text: '批阅中', tag: 'warning' }
    },
    /** 主观题：显示人工分，未打分显示「待评」 */
    itemScore(it) {
      if (this.isSubjective(it)) {
        return it.manualScore == null ? '待评' : this.round1(it.manualScore)
      }
      const v = it.aiScore != null ? it.aiScore : (it.manualScore != null ? it.manualScore : null)
      return v == null ? '—' : this.round1(v)
    },
    isSubjective(it) {
      return it.isSubjective === 1 || it.isSubjective === '1'
    },
    /** 选项 JSON → [{ key, content }] */
    optsOf(it) {
      try {
        const arr = JSON.parse((it && it.optionsJson) || '[]')
        return Array.isArray(arr) ? arr : []
      } catch (e) {
        return []
      }
    },
    answerKeys(val) {
      return String(val || '').toUpperCase().split(',').map(s => s.trim()).filter(s => !!s)
    },
    /** 本人是否选了该选项 */
    pickedOf(it, key) {
      return this.answerKeys(it && it.userAnswer).indexOf(String(key).toUpperCase()) > -1
    },
    /** 该选项是否为正确答案 */
    rightOf(it, key) {
      return this.answerKeys(it && it.answer).indexOf(String(key).toUpperCase()) > -1
    },
    /** 实操题的作答是文件路径 */
    isFile(it) {
      const a = String(it.userAnswer || '')
      return this.isSubjective(it) && /\.(pdf|docx?|xlsx?|pptx?|zip|rar|7z|txt|md|png|jpe?g|gif|mp4|webm|mov)$/i.test(a)
    },
    commentOf(examId) {
      const items = this.itemsOf(examId)
      const c = items.map(it => it.manualComment).filter(t => !!t)
      return c.length ? c.join('；') : ''
    },
    typeLabel(qtype) {
      return { SINGLE: '单选', MULTI: '多选', JUDGE: '判断', SUBJECT: '主观' }[qtype] || qtype || '—'
    },
    round1(v) {
      const n = Number(v)
      return isNaN(n) ? '—' : Math.round(n * 10) / 10
    },
    /** 返回「正式考核」栏（2026-09-23 起正式考核是学习与考核页里的分节，直接带锚点定位） */
    goBack() {
      this.$router.push({ path: '/assessment/intern/learning', hash: '#sec-exam' })
    },
    /** 去「考核成绩与转正」栏看全部成绩 */
    goAll() {
      this.$router.push({ path: '/assessment/intern/learning', hash: '#sec-result' })
    }
  }
}
</script>

<style lang="scss" scoped>
@import '~@/assets/styles/practice-module.scss';

.er-body { min-height: 200px; }
.er-summary { display: flex; flex-wrap: wrap; gap: 26px; margin-bottom: 18px; padding: 16px 18px; background: #fff; box-shadow: 0 1px 3px rgba(16, 24, 40, .04); }
.er-cell { display: flex; align-items: baseline; gap: 8px; }
.er-label { color: #98a2b3; font-size: 12px; }
.er-cell b { color: #1d2939; font-size: 17px; font-weight: 600; }
.er-cell b.ok { color: #1a9e6a; }
.er-cell b.bad { color: #d92d20; }
.er-cell b.warn { color: #d9841a; }
.er-cell b.muted { color: #98a2b3; }
.er-card { margin-bottom: 16px; padding: 18px 20px 20px; background: #fff; box-shadow: 0 1px 3px rgba(16, 24, 40, .04); }
.er-card-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding-bottom: 12px; border-bottom: 1px solid #edf0f4; }
.er-title { display: flex; align-items: center; gap: 10px; min-width: 0; }
.er-title h2 { margin: 0; color: #1d2939; font-size: 16px; font-weight: 600; }
.er-badge { display: inline-flex; padding: 2px 8px; color: #fff; font-size: 11px; border-radius: 3px; }
.er-badge.theory { background: #1764f5; }
.er-badge.prac { background: #23966f; }
.er-card-meta { display: flex; align-items: center; gap: 12px; color: #8490a0; font-size: 12px; white-space: nowrap; }
.er-score-row { display: flex; align-items: baseline; gap: 8px; padding: 14px 0 4px; color: #667085; font-size: 12px; }
.er-score-row b { color: #1d2939; font-size: 16px; font-weight: 600; }
.er-score-row b.er-score { font-size: 22px; }
.er-score-row b.ok { color: #1a9e6a; }
.er-score-row b.bad { color: #d92d20; }
.er-score-row b.muted { color: #98a2b3; }
.er-sep { color: #d0d5dd; }
.er-hint { padding: 14px 0; color: #98a2b3; font-size: 12px; }
.er-table { width: 100%; margin-top: 8px; border-collapse: collapse; }
.er-table th { padding: 9px 10px; color: #8490a0; background: #f7f9fc; font-size: 12px; font-weight: 500; text-align: left; }
.er-table td { padding: 10px; color: #475467; border-bottom: 1px solid #edf0f4; font-size: 12px; vertical-align: top; }
.er-table td.ctr { text-align: center; }
.er-table td.stem { max-width: 320px; color: #1d2939; }
.er-opts { margin: 6px 0 0; padding: 0; list-style: none; }
.er-opts li { display: flex; align-items: baseline; gap: 6px; padding: 1px 0; color: #667085; font-weight: 400; }
.er-opts li b { flex: none; color: #98a2b3; }
.er-opts li.right span { color: #1a7a58; }
.er-opts li.wrong span { color: #d92d20; }
.er-opts li em { flex: none; padding: 0 5px; color: #1764f5; font-size: 11px; font-style: normal; }
.er-table td.answer a { color: #1764f5; text-decoration: none; }
.er-table td.answer .ans-text { word-break: break-all; }
.er-comment { margin-top: 12px; padding: 10px 12px; color: #475467; background: #f7f9fc; font-size: 12px; line-height: 1.6; }
@media (max-width: 700px) {
  .er-summary { gap: 14px; }
  .er-card { padding: 14px 12px; }
  .er-card-head { align-items: flex-start; flex-direction: column; }
  .er-table td.stem { max-width: 140px; }
}
</style>
