<template>
  <div class="s-page" v-loading="loading">
    <div class="s-crumb">
      <el-button type="text" icon="el-icon-arrow-left" @click="goBack">返回考核与成绩</el-button>
      <span class="sp">/</span>
      <b>{{ exam.examName || '考核详情' }}</b>
      <span v-if="exam.examMode" class="s-badge" :class="exam.examMode === 'FORMAL' ? 'blue' : 'purple'">{{ exam.examMode === 'FORMAL' ? '正式' : '模拟' }}</span>
      <span v-if="exam.examType" class="s-badge">{{ exam.examType === 'PRACTICAL' ? '实操' : '理论' }}</span>
      <span v-if="exam.deptName" class="s-badge">{{ exam.deptName }}</span>
      <span v-if="exam.status" class="s-badge" :class="statusTone">{{ statusText }}</span>
    </div>

    <div v-if="loadError" class="s-empty big">
      <i class="el-icon-warning-outline" /><strong>无法加载该场考核</strong>
      <span>{{ loadError }}</span>
      <el-button size="small" @click="goBack">返回列表</el-button>
    </div>

    <template v-else-if="exam.id">
      <!-- ① 顶部 4 项关键指标 -->
      <div class="s-kpis">
        <div class="s-kpi">
          <div class="lb"><i class="dot" style="background:#1764f5" />卷面满分</div>
          <div class="vl">{{ fullScore > 0 ? fullScore : '未配置' }}<small v-if="fullScore > 0">分</small></div>
          <div class="ft">按当前组卷 / 题目清单算出</div>
        </div>
        <div class="s-kpi" :class="{ danger: checkLevel === 'danger' }">
          <div class="lb"><i class="dot" style="background:#f04438" />通过线</div>
          <div class="vl">{{ exam.passLine }}<small>分</small></div>
          <div class="ft" :class="checkLevel === 'danger' ? 'warn' : ''">{{ checkHint }}</div>
        </div>
        <div class="s-kpi">
          <div class="lb"><i class="dot" style="background:#7a5af8" />题量</div>
          <div class="vl">{{ countText }}</div>
          <div class="ft">{{ exam.examType === 'PRACTICAL' ? '逐题填写，不抽题' : '由组卷规则决定' }}</div>
        </div>
        <div class="s-kpi">
          <div class="lb"><i class="dot" style="background:#12b76a" />应考 · 实考</div>
          <div class="vl">{{ isFormal ? (rows.length + ' · ' + actualCount) : '—' }}</div>
          <div class="ft" :class="absentCount > 0 ? 'warn' : 'up'">
            {{ isFormal ? (absentCount > 0 ? absentCount + ' 人未交卷' : '全部已交卷') : '模拟考核为练习，不计应考' }}
          </div>
        </div>
      </div>

      <!-- ② 校验位：这一场能不能发布 -->
      <el-alert
        v-if="checkLevel !== 'ok'"
        :closable="false"
        :type="checkLevel === 'danger' ? 'error' : 'warning'"
        show-icon
        :title="checkTitle"
        :description="checkDesc"
        class="s-alert"
      />

      <div class="s-grid">
        <!-- ③ 详细配置 -->
        <section class="s-card s-c5">
          <div class="s-card-h"><div class="tt"><span class="s-idx">配</span><h3>详细信息配置</h3></div></div>
          <div class="s-kv">
            <div><span>考试时长</span><b>{{ exam.duration > 0 ? exam.duration + ' 分钟' : '不限时' }}</b></div>
            <div><span>时间窗</span><b>{{ windowText }}</b></div>
            <div><span>通过线</span><b>{{ exam.passLine }} 分</b></div>
            <div v-if="!isPractice"><span>题型与分值</span><b>{{ theorySpec }}</b></div>
            <div><span>名单模式</span><b>{{ assignText }}</b></div>
            <div><span>组卷题库</span><b>{{ bankText }}</b></div>
            <div><span>已作答 / 待批阅</span><b>{{ (exam.answeredCount || 0) + ' / ' + (exam.pendingCount || 0) }}</b></div>
            <div><span>发布 / 更新</span><b>{{ fmtTime(exam.publishedAt || exam.updateTime) }}</b></div>
          </div>
          <p class="s-note">
            名单模式为空名单时按「全员应考」处理（后端既有判定）。配置的修改在「配置」页进行。
            <el-button type="text" size="mini" @click="goConfig">去配置页</el-button>
          </p>
        </section>

        <!-- ④ 组卷可行性 -->
        <section class="s-card s-c7">
          <div class="s-card-h">
            <div class="tt"><span class="s-idx o">卷</span><h3>组卷可行性</h3></div>
            <div class="acts">
              <span class="s-badge" :class="drawBasisTag">{{ drawBasisText }}</span>
              <el-button size="mini" :loading="drawing" @click="tryDraw">试抽一次</el-button>
            </div>
          </div>

          <template v-if="isPractice">
            <div class="s-empty sm"><i class="el-icon-document-checked" /><span>实操考核逐题填写，不从题库抽题（题目清单在配置页维护）</span></div>
          </template>
          <template v-else>
            <table v-if="drawRows.length" class="s-tbl sm">
              <thead><tr><th>{{ drawKind === 'bank' ? '题库' : '章节（知识点）' }}</th><th v-if="drawKind === 'bank'" style="width:70px">单选</th><th v-if="drawKind === 'bank'" style="width:70px">多选</th><th v-if="drawKind === 'bank'" style="width:70px">判断</th><th style="width:80px">需抽题</th><th style="width:90px">判定</th></tr></thead>
              <tbody>
                <tr v-for="(r, i) in drawRows" :key="i">
                  <td class="nm">{{ r.name }}</td>
                  <td v-if="drawKind === 'bank'">{{ r.single || 0 }}</td>
                  <td v-if="drawKind === 'bank'">{{ r.multi || 0 }}</td>
                  <td v-if="drawKind === 'bank'">{{ r.judge || 0 }}</td>
                  <td>{{ r.total }}</td>
                  <td><span class="s-badge" :class="r.total > 0 ? 'ok' : 'warn'">{{ r.total > 0 ? '已配' : '未配' }}</span></td>
                </tr>
              </tbody>
            </table>
            <div v-else class="s-empty sm"><i class="el-icon-warning-outline" /><span>尚未配置组卷规则 —— 发布时后端会校验「知识分布 / 抽题量」，请到配置页补齐</span></div>
            <p class="s-note">
              判定以<b>后端试抽</b>为准（点右上角「试抽一次」）。⚠️ 「可用题量」不在这里猜 ——
              试抽成功即绿，失败会把后端给的原因原样显示。
            </p>
          </template>
        </section>

        <!-- ⑤ 分数统计 -->
        <section class="s-card s-c5">
          <div class="s-card-h">
            <div class="tt"><span class="s-idx g">分</span><h3>分数统计</h3></div>
            <span class="hint">{{ scoredRows.length }} 份已出分</span>
          </div>
          <template v-if="scoredRows.length >= 5">
            <div v-for="b in distribution" :key="b.label" class="bar-row">
              <span class="bar-lb">{{ b.label }}</span>
              <span class="bar-track"><i :style="{ width: b.pct + '%' }" /></span>
              <span class="bar-n">{{ b.count }}</span>
            </div>
            <p class="s-note">区间按卷面得分划分；样本 {{ scoredRows.length }} 份，分布仅供参考。</p>
          </template>
          <template v-else-if="scoredRows.length">
            <div class="s-empty sm">
              <i class="el-icon-data-analysis" />
              <span>样本不足（仅 {{ scoredRows.length }} 份），不画分布 —— 逐份分数见右侧名单</span>
            </div>
          </template>
          <div v-else class="s-empty sm"><i class="el-icon-document" /><span>还没有已出分的答卷</span></div>
        </section>

        <!-- ⑥ 应考名单 -->
        <section class="s-card s-c7">
          <div class="s-card-h">
            <div class="tt"><span class="s-idx">名</span><h3>应考名单</h3></div>
            <span class="hint">点有答卷的人查看逐题明细</span>
          </div>
          <table v-if="rows.length" class="s-tbl sm">
            <thead>
              <tr><th>姓名</th><th style="width:80px">部门</th><th style="width:64px">第 N 次</th><th style="width:76px">得分</th><th style="width:76px">结论</th><th style="width:86px">答卷状态</th><th style="width:100px">提交时间</th><th style="width:70px">操作</th></tr>
            </thead>
            <tbody>
              <tr v-for="r in rows" :key="r.userId">
                <td class="nm">{{ r.userName }}</td>
                <td>{{ r.deptName || '—' }}</td>
                <td>{{ r.sheetId ? (r.retakeSeq || 1) : '—' }}</td>
                <td>{{ r.finalScore != null ? r.finalScore : '待定' }}</td>
                <td><span class="s-badge" :class="toneOf(r)">{{ conclusion(r) }}</span></td>
                <td>{{ r.sheetId ? sheetStatus(r) : '未交卷' }}</td>
                <td>{{ r.submitTime ? String(r.submitTime).replace('T', ' ').slice(5, 16) : '—' }}</td>
                <td>
                  <el-button v-if="r.sheetId" type="text" size="mini" @click="goSheet(r)">明细</el-button>
                  <span v-else class="muted">—</span>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-else class="s-empty sm"><i class="el-icon-user" /><span>暂无应考名单</span></div>
        </section>
      </div>
    </template>
  </div>
</template>

<script>
import { getExam, getExamConfig, gradingList, tryDrawByBanks, tryDrawPaper } from '@/api/business/exam'

/**
 * 超管「考核与成绩」L1 —— 单场考核详情
 *
 * 路由：`/exam-session/:examId?from=`（顶层 hidden，照 /exam-config 做法；
 * 同一个列表页在不同角色下 URL 不同，所以**用 ?from= 回跳，绝不按路径前缀猜**）
 *
 * 四块内容：① 关键指标 ② 能否发布的校验位 ③ 详细配置 ④ **组卷可行性**（含试抽）
 *           ⑤ 分数统计（样本 <5 不画分布）⑥ 应考名单 → 点有答卷者进 L2
 *
 * ★ 组卷是**双轨制**（实测）：`exam_bank_rule`（题库 × 题型）非空走它；
 *   否则退回 `exam_knowledge_rule`（章节配比）—— 这里要把"本场走哪条"显式标出来，
 *   否则运营根本不知道自己配的规则有没有生效。
 */
export default {
  name: 'SuperExamSession',
  data() {
    return {
      loading: false,
      loadError: '',
      exam: {},
      cfg: {},
      rows: [],
      drawing: false
    }
  },
  computed: {
    isPractice() { return this.exam.examType === 'PRACTICAL' },
    isFormal() { return this.exam.examMode === 'FORMAL' },
    statusText() {
      return { DRAFT: '待发布', PUBLISHED: '已发布', GRADING: '待批改', DISABLED: '已停用' }[this.exam.status] || ''
    },
    statusTone() {
      return { DRAFT: 'warn', PUBLISHED: 'ok', GRADING: 'blue', DISABLED: 'red' }[this.exam.status] || ''
    },
    /** 卷面满分：实操 = 各题满分之和（后端回写）；理论 = 单/多/判 分值 × 题量 */
    fullScore() {
      const n = v => Number(v) || 0
      if (this.isPractice) return Math.round(n(this.exam.subjectTotalScore) * 10) / 10
      const rules = this.cfg.bankRules || []
      if (rules.length) {
        const s = rules.reduce((a, r) => a + n(r.singleCount), 0)
        const m = rules.reduce((a, r) => a + n(r.multiCount), 0)
        const j = rules.reduce((a, r) => a + n(r.judgeCount), 0)
        return Math.round((s * n(this.exam.singleScore) + m * n(this.exam.multiScore) + j * n(this.exam.judgeScore)) * 10) / 10
      }
      return Math.round((n(this.exam.singleCount) * n(this.exam.singleScore) +
        n(this.exam.multiCount) * n(this.exam.multiScore) +
        n(this.exam.judgeCount) * n(this.exam.judgeScore)) * 10) / 10
    },
    countText() {
      const n = this.isPractice ? (this.exam.subjectCount || 0) : (this.exam.questionCount || 0)
      return n + (this.isPractice ? ' 道' : ' 题')
    },
    theorySpec() {
      const parts = []
      if (Number(this.exam.singleCount)) parts.push('单选 ' + this.exam.singleCount + '×' + this.exam.singleScore)
      if (Number(this.exam.multiCount)) parts.push('多选 ' + this.exam.multiCount + '×' + this.exam.multiScore)
      if (Number(this.exam.judgeCount)) parts.push('判断 ' + this.exam.judgeCount + '×' + this.exam.judgeScore)
      return parts.join(' + ') || '待配置'
    },
    windowText() {
      const f = v => (v ? String(v).replace('T', ' ').slice(0, 16) : '')
      const s = f(this.cfg.startTime)
      const e = f(this.cfg.endTime)
      if (!s && !e) return '未设置'
      return (s || '--') + ' ~ ' + (e || '--')
    },
    assignText() {
      if (this.cfg.assignMode === 'ASSIGNED') return '指定 ' + ((this.cfg.participantIds || []).length) + ' 人'
      return '全部在培实习生'
    },
    bankText() {
      const rules = this.cfg.bankRules || []
      if (rules.length) return rules.map(r => r.bankName || ('库 ' + r.bankId)).join(' + ')
      return this.exam.bankName || '未配置'
    },
    /** 本场实际走哪条抽题链路（双轨制，必须显式） */
    drawKind() {
      if ((this.cfg.bankRules || []).length) return 'bank'
      if ((this.cfg.knowledgeRules || []).length) return 'knowledge'
      return 'fallback'
    },
    drawBasisText() {
      return { bank: '当前生效：题库 × 题型', knowledge: '当前生效：章节（知识点）配比', fallback: '未配组卷：单库兜底' }[this.drawKind]
    },
    drawBasisTag() {
      return this.drawKind === 'fallback' ? 'warn' : 'ok'
    },
    drawRows() {
      const n = v => Number(v) || 0
      if (this.drawKind === 'bank') {
        return (this.cfg.bankRules || []).map(r => ({
          name: r.bankName || ('库 ' + r.bankId),
          single: n(r.singleCount), multi: n(r.multiCount), judge: n(r.judgeCount),
          total: n(r.singleCount) + n(r.multiCount) + n(r.judgeCount)
        }))
      }
      if (this.drawKind === 'knowledge') {
        return (this.cfg.knowledgeRules || []).map(r => ({
          name: r.knowledgePoint || r.point || '未命名章节',
          total: n(r.pickCount || r.count || r.questionCount)
        }))
      }
      return []
    },
    /** 能否发布的校验位 */
    checkLevel() {
      const pass = Number(this.exam.passLine) || 0
      if (!this.fullScore) return 'danger'
      if (pass > this.fullScore) return 'danger'
      if (pass === this.fullScore) return 'warn'
      return 'ok'
    },
    checkTitle() {
      return this.checkLevel === 'danger' ? '当前配置无法发布' : '通过线等于卷面满分'
    },
    checkDesc() {
      const pass = Number(this.exam.passLine) || 0
      if (!this.fullScore) {
        return this.isPractice
          ? '尚无实操题目：到配置页逐题添加题干与满分（发布时后端会拦截）'
          : '尚未组卷：到配置页添加题库并设置抽题量（发布时后端会拦截）'
      }
      if (pass > this.fullScore) {
        return '通过线 ' + pass + ' 分高于卷面满分 ' + this.fullScore + ' 分 —— 发布会被后端拦截，请降低通过线或提高题量/分值'
      }
      return '通过线 ' + pass + ' 分等于卷面满分 ' + this.fullScore + ' 分，实习生必须全部答对才及格'
    },
    checkHint() {
      const pass = Number(this.exam.passLine) || 0
      if (!this.fullScore) return '卷面满分 0，无法发布'
      if (pass > this.fullScore) return '高于卷面满分，无法发布'
      if (pass === this.fullScore) return '等于满分，须全对'
      return '余量 ' + (Math.round((this.fullScore - pass) * 10) / 10) + ' 分'
    },
    actualCount() { return this.rows.filter(r => r.sheetId).length },
    absentCount() { return this.rows.filter(r => !r.sheetId).length },
    scoredRows() { return this.rows.filter(r => r.finalScore != null) },
    distribution() {
      const list = this.scoredRows.map(r => Number(r.finalScore)).filter(n => !isNaN(n))
      if (!list.length) return []
      const full = this.fullScore || 100
      const buckets = [
        { label: '0-59%', min: 0, max: full * 0.6 - 0.001 },
        { label: '60-79%', min: full * 0.6, max: full * 0.8 - 0.001 },
        { label: '80-89%', min: full * 0.8, max: full * 0.9 - 0.001 },
        { label: '≥90%', min: full * 0.9, max: Number.MAX_VALUE }
      ]
      const counts = buckets.map(b => ({ label: b.label, count: list.filter(v => v >= b.min && v <= b.max).length }))
      const max = Math.max.apply(null, counts.map(c => c.count)) || 1
      return counts.map(c => Object.assign(c, { pct: Math.round(c.count / max * 100) }))
    }
  },
  created() {
    this.load()
  },
  methods: {
    load() {
      const id = this.$route.params.examId
      if (!id) { this.loadError = '缺少考核 id'; return }
      this.loading = true
      this.loadError = ''
      Promise.all([
        getExam(id).catch(() => null),
        getExamConfig(id).catch(() => null),
        gradingList(id).catch(() => null)
      ]).then(([examRes, cfgRes, rowsRes]) => {
        const exam = (examRes && examRes.data) || null
        if (!exam) { this.loadError = '未找到该考核（可能已被删除）'; this.loading = false; return }
        this.exam = exam
        this.cfg = (cfgRes && cfgRes.data) || {}
        this.rows = (rowsRes && rowsRes.data) || []
        this.loading = false
      }).catch(() => {
        this.loadError = '加载失败：可能是权限不足或后端未启动'
        this.loading = false
      })
    },
    /** 组卷可行性 = 直接试抽（判定在后端，不在这里猜"够不够"） */
    tryDraw() {
      const rules = this.cfg.bankRules || []
      const krules = this.cfg.knowledgeRules || []
      if (!rules.length && !krules.length) {
        this.$modal.msgWarning('本场未配置组卷规则，无法试抽（到配置页补齐后再来）')
        return
      }
      this.drawing = true
      const req = rules.length
        ? tryDrawByBanks({ bankRules: rules.map((r, i) => ({
            bankId: r.bankId, singleCount: r.singleCount, multiCount: r.multiCount,
            judgeCount: r.judgeCount, sortNo: i + 1
          })) })
        : tryDrawPaper(this.exam.bankId, { knowledgeRules: krules })
      req.then(res => {
        this.drawing = false
        const list = (res && res.data) || []
        if (!list.length) {
          this.$modal.msgWarning('按当前配置抽不到题：请检查各题库题量是否充足')
          return
        }
        const typeText = t => ({ SINGLE: '单选', MULTI: '多选', JUDGE: '判断' }[t] || t)
        this.$alert(
          list.map(q => (q.seq || '') + ' · ' + typeText(q.qtype) + ' · ' + (q.bankName || '') + ' · ' + (q.stem || '')).join('<br/>'),
          '试抽结果（' + list.length + ' 题）',
          { dangerouslyUseHTMLString: true }
        )
      }).catch(err => {
        this.drawing = false
        // 失败原因原样呈现（后端给的是"哪个库不够"这类可执行信息）
        this.$modal.msgError((err && err.message) ? err.message : '试抽失败')
      })
    },
    goBack() {
      this.$router.push(this.$route.query.from || '/super/ops/exams').catch(() => {})
    },
    goConfig() {
      this.$router.push({ path: '/exam-config/' + this.exam.id, query: { from: this.$route.fullPath } }).catch(() => {})
    },
    goSheet(row) {
      this.$router.push({ path: '/exam-sheet/' + row.sheetId, query: { from: this.$route.fullPath, examId: this.exam.id } }).catch(() => {})
    },
    conclusion(r) {
      if (r.finalScore == null) return '待定'
      if (r.passFlag === 1 || r.passFlag === '1') return '通过'
      if (r.passFlag === 0 || r.passFlag === '0') return '未通过'
      return Number(r.finalScore) >= Number(this.exam.passLine || 60) ? '通过' : '未通过'
    },
    toneOf(r) {
      const t = this.conclusion(r)
      return { '通过': 'ok', '未通过': 'red', '待定': 'warn' }[t] || ''
    },
    sheetStatus(r) {
      if (r.sheetStatus === 'PUBLISHED') return '已发布'
      if (r.sheetStatus === 'IN_PROGRESS') return '答题中'
      if (r.manualScore != null) return '已评分'
      if (r.finalScore != null) return '待发布'
      return '待评分'
    },
    fmtTime(v) {
      if (!v) return '—'
      return String(v).replace('T', ' ').slice(0, 16)
    }
  }
}
</script>

<style lang="scss" scoped>
@import '~@/assets/styles/super-module.scss';

.s-crumb { display: flex; align-items: center; gap: 8px; margin-bottom: 14px; color: #667085; font-size: 13px; flex-wrap: wrap; }
.s-crumb .sp { color: #cfd6df; }
.s-crumb b { color: #1d2939; }
.s-alert { margin-bottom: 14px; }

.s-kv { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
.s-kv > div { padding: 9px 11px; background: #f8fafc; border-radius: 6px; }
.s-kv span { display: block; color: #98a2b3; font-size: 11.5px; }
.s-kv b { display: block; margin-top: 3px; color: #344054; font-size: 12.5px; font-weight: 500; word-break: break-word; }

.s-card-h .acts { display: flex; align-items: center; gap: 8px; }

.s-tbl.sm th, .s-tbl.sm td { padding: 6px 8px; font-size: 12.5px; }

.bar-row { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.bar-lb { width: 62px; color: #667085; font-size: 12px; }
.bar-track { flex: 1; height: 10px; overflow: hidden; background: #f2f4f7; border-radius: 5px; }
.bar-track i { display: block; height: 100%; background: #1764f5; }
.bar-n { width: 26px; text-align: right; color: #344054; font-size: 12px; }

.s-kpi.danger { border-color: #f7d4d1; background: #fdeeed; }
.s-kpi.danger .vl { color: #d9534f; }
.s-empty.sm { padding: 26px 12px; }
.s-empty.big { padding: 60px 12px; }
.muted { color: #98a2b3; }
.s-badge.purple { color: #7b5cf0; background: #f2eeff; }
.s-badge.red { color: #d9534f; background: #fdeeed; }

@media (max-width: 1200px) { .s-kv { grid-template-columns: minmax(0, 1fr); } }
</style>
