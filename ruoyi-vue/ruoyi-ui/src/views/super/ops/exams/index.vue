<template>
  <div class="s-page">
    <header class="s-head">
      <div>
        <span class="eyebrow">SUPER ADMIN · OPERATION</span>
        <h1>考核与成绩</h1>
        <p>
          跨部门看考核：哪些场次<b>该开考却没开</b>、哪些还停在草稿、每个部门的应考与实考情况。
          <b>正式考核</b>统计应考 / 实考 / 缺考 / 已出分 / 通过；<b>模拟考核</b>属练习（走
          <code>practice_record</code>，不产生答卷），只显示题量与状态、不计应考人次。
        </p>
      </div>
      <div class="s-head-actions">
        <span class="s-ro"><i class="el-icon-view" /> 全部门可见</span>
        <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="loadAll">刷新</el-button>
      </div>
    </header>

    <!-- ① 总览：真实计数，点卡即筛选 -->
    <div class="s-kpis kpi6">
      <div
        v-for="k in kpiCards"
        :key="k.key"
        class="s-kpi"
        :class="{ clickable: !!k.filter, active: k.filter && activeKpi === k.key }"
        @click="k.filter && applyKpi(k)"
      >
        <div class="lb"><i class="dot" :style="{ background: k.color }" />{{ k.label }}</div>
        <div class="vl">{{ k.value }}<small v-if="k.unit">{{ k.unit }}</small></div>
        <div class="ft" :class="k.tone">{{ k.hint }}</div>
      </div>
    </div>

    <!-- ② 筛选条 -->
    <div class="s-filter">
      <el-select v-model="filters.deptName" size="small" clearable filterable placeholder="全部部门" class="w150">
        <el-option v-for="d in deptOptions" :key="d" :label="d" :value="d" />
      </el-select>
      <el-select v-model="filters.mode" size="small" clearable placeholder="全部性质" class="w120">
        <el-option label="正式考核" value="FORMAL" />
        <el-option label="模拟考核" value="PRACTICE" />
      </el-select>
      <el-select v-model="filters.type" size="small" clearable placeholder="全部形式" class="w120">
        <el-option label="理论" value="THEORY" />
        <el-option label="实操" value="PRACTICAL" />
      </el-select>
      <el-select v-model="filters.status" size="small" clearable placeholder="全部状态" class="w120">
        <el-option label="待发布" value="DRAFT" />
        <el-option label="已发布" value="PUBLISHED" />
        <el-option label="已停用" value="DISABLED" />
      </el-select>
      <el-input v-model="filters.kw" size="small" clearable prefix-icon="el-icon-search" placeholder="搜考核名称 / 部门" class="w180" />
      <el-button v-if="hasFilter" size="small" icon="el-icon-refresh-left" @click="resetFilters">重置</el-button>
      <span class="s-filter-sum">显示 {{ filteredExams.length }} / {{ exams.length }} 场</span>
    </div>

    <!-- 视角切换（两个入口在 L1 合流，这里只是同一份数据的两种看法） -->
    <div class="s-viewsw">
      <button type="button" class="vs" :class="{ on: view === 'session' }" @click="view = 'session'">
        按场次 <em>{{ filteredExams.length }}</em>
      </button>
      <button type="button" class="vs" :class="{ on: view === 'dept' }" @click="view = 'dept'">
        按部门 <em>{{ deptRows.length }}</em>
      </button>
    </div>

    <!-- ③ 列表 -->
    <section v-if="view === 'session'" class="s-card s-c12">
      <div class="s-card-h">
        <div class="tt"><span class="s-idx">场</span><h3>考核场次</h3></div>
        <span class="hint">一条记录 = 一场考核；正式与模拟分列展示（模拟用徽标区分）</span>
      </div>

      <table v-if="filteredExams.length" class="s-tbl">
        <thead>
          <tr>
            <th>考核名称</th>
            <th style="width:96px">部门</th>
            <th style="width:118px">性质 / 形式</th>
            <th style="width:80px">题量</th>
            <th style="width:86px">通过线</th>
            <th style="width:158px">时间窗</th>
            <th style="width:150px">应考 · 实考 · 缺考</th>
            <th style="width:104px">已出分 · 通过</th>
            <th style="width:88px">状态</th>
            <th style="width:120px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="e in filteredExams" :key="e.id">
            <td class="nm">
              {{ e.examName }}
              <span v-if="isTestData(e)" class="s-badge warn sm">疑似测试数据</span>
            </td>
            <td>{{ e.deptName || '—' }}</td>
            <td>
              <span class="s-badge" :class="e.examMode === 'FORMAL' ? 'blue' : 'purple'">{{ modeText(e.examMode) }}</span>
              <span class="s-badge sm">{{ e.examType === 'PRACTICAL' ? '实操' : '理论' }}</span>
            </td>
            <td>{{ (e.examType === 'PRACTICAL' ? e.subjectCount : e.questionCount) || 0 }}</td>
            <td>{{ e.passLine != null ? e.passLine : '—' }}</td>
            <td>{{ windowText(e) }}</td>
            <td>
              <template v-if="e.examMode === 'FORMAL'">
                <span class="num">{{ stat(e).should }}</span>
                <span class="sep">·</span>
                <span class="num ok">{{ stat(e).actual }}</span>
                <span class="sep">·</span>
                <span class="num" :class="{ warn: stat(e).absent > 0 }">{{ stat(e).absent }}</span>
              </template>
              <span v-else class="muted" title="模拟考核属练习，不产生答卷">— 练习不计</span>
            </td>
            <td>
              <template v-if="e.examMode === 'FORMAL'">
                <span class="num">{{ stat(e).scored }}</span>
                <span class="sep">·</span>
                <span class="num ok">{{ stat(e).passed }}</span>
              </template>
              <span v-else class="muted">—</span>
            </td>
            <td><span class="s-badge" :class="statusTone(e.status)">{{ statusText(e.status) }}</span></td>
            <td>
              <el-button type="text" size="mini" @click="goSession(e)">详情</el-button>
              <el-button type="text" size="mini" @click="goConfig(e)">配置</el-button>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-else-if="!exams.length" class="s-empty">
        <i class="el-icon-finished" /><span>还没有任何考核场次</span>
      </div>
      <div v-else class="s-empty">
        <i class="el-icon-search" /><span>当前筛选下没有匹配的考核</span>
        <el-button size="mini" @click="resetFilters">清空筛选</el-button>
      </div>
      <p class="s-note">
        「应考」= 该场按<code>发布范围</code>（全部在培 / 指定人员）确定的应考人数；「实考」= 已交卷人数；「缺考」= 应考 − 实考。
        缺考为 0 时不着色；正式考核才有这三个数字。
      </p>
    </section>

    <section v-else class="s-card s-c12">
      <div class="s-card-h">
        <div class="tt"><span class="s-idx g">部</span><h3>按部门汇总</h3></div>
        <span class="hint">点任意一行 = 回到「按场次」并只看该部门</span>
      </div>

      <table v-if="deptRows.length" class="s-tbl">
        <thead>
          <tr>
            <th>部门</th>
            <th style="width:88px">场次</th>
            <th style="width:88px">正式 / 模拟</th>
            <th style="width:88px">应考</th>
            <th style="width:88px">实考</th>
            <th style="width:88px">缺考</th>
            <th style="width:88px">已出分</th>
            <th style="width:88px">通过</th>
            <th style="width:110px">通过率</th>
            <th style="width:80px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="d in deptRows" :key="d.deptName" class="clickable-row" @click="focusDept(d.deptName)">
            <td class="nm">{{ d.deptName }}</td>
            <td>{{ d.total }}</td>
            <td>{{ d.formal }} / {{ d.practice }}</td>
            <td>{{ d.should }}</td>
            <td class="num ok">{{ d.actual }}</td>
            <td class="num" :class="{ warn: d.absent > 0 }">{{ d.absent }}</td>
            <td>{{ d.scored }}</td>
            <td>{{ d.passed }}</td>
            <td>
              <template v-if="d.rate !== null">
                <span class="num" :class="{ ok: d.rate >= 60, warn: d.rate < 60 }">{{ d.rate }}%</span>
                <span class="s-badge sm">{{ d.scored }} 份</span>
              </template>
              <span v-else class="muted">无样本</span>
            </td>
            <td><el-button type="text" size="mini" @click.stop="focusDept(d.deptName)">只看该部门</el-button></td>
          </tr>
        </tbody>
      </table>
      <div v-else class="s-empty"><i class="el-icon-search" /><span>当前筛选下没有部门</span></div>
      <p class="s-note">
        通过率 = 通过人数 / <b>已出分人数</b>（无样本的部门显示「无样本」，不伪装 0%）。
        模拟考核不计入这里的应考人次（它是练习，不产生答卷）。
      </p>
    </section>
  </div>
</template>

<script>
import { listExam, gradingList } from '@/api/business/exam'

/**
 * 超管「考核与成绩」L0（培养运营 → 考核与成绩）
 *
 * 三段式：① 总览 KPI（点卡即筛选）→ ② 筛选条 → ③ 列表（两个视角）+ 两种空态
 *
 * 数据源：`/business/exam/list`（超管不限部门，含正式 + 模拟，**不传 examMode**）
 *   + 逐场 `/business/answer-sheet/grading/{examId}`（只为 **FORMAL** 场次调）
 *
 * ★ 口径守卫（都是实测踩出来的）
 *   1. 只有正式考核才有"应考 / 实考 / 缺考" —— 模拟考核走 practice_record、不产生答卷；
 *      若对模拟卷调 grading 会拿到"全部在培名单但无答卷"，会把它误算成"全员缺考"。
 *   2. 考核名称 ≤2 字或含 test/测试/临时 ⇒ 打「疑似测试数据」标（库里存在 12 / 22 / 111 这类残留）。
 *   3. 时间窗未设置就写「未设置」，不显示空白。
 *   4. 通过线有两套量纲（模拟按 10 分制、正式按 100 分制）⇒ 不做跨性质均值，只逐场展示。
 */
export default {
  name: 'SuperOpsExams',
  data() {
    return {
      loading: false,
      exams: [],
      statsMap: {},
      view: 'session',
      activeKpi: '',
      filters: { deptName: '', mode: '', type: '', status: '', kw: '' }
    }
  },
  computed: {
    deptOptions() {
      const set = {}
      this.exams.forEach(e => { if (e.deptName) set[e.deptName] = 1 })
      return Object.keys(set).sort()
    },
    hasFilter() {
      const f = this.filters
      return !!(f.deptName || f.mode || f.type || f.status || f.kw)
    },
    filteredExams() {
      const f = this.filters
      const kw = String(f.kw || '').trim().toLowerCase()
      return this.exams.filter(e => {
        if (f.deptName && e.deptName !== f.deptName) return false
        if (f.mode && e.examMode !== f.mode) return false
        if (f.type && e.examType !== f.type) return false
        if (f.status && e.status !== f.status) return false
        if (kw) {
          const hay = [e.examName, e.deptName].map(v => String(v || '')).join(' ').toLowerCase()
          if (hay.indexOf(kw) === -1) return false
        }
        return true
      })
    },
    /** 全局人次（只为正式考核累计） */
    totals() {
      let should = 0
      let actual = 0
      let absent = 0
      let scored = 0
      let passed = 0
      this.filteredExams.forEach(e => {
        if (e.examMode !== 'FORMAL') return
        const s = this.stat(e)
        should += s.should
        actual += s.actual
        absent += s.absent
        scored += s.scored
        passed += s.passed
      })
      return { should, actual, absent, scored, passed }
    },
    kpiCards() {
      const t = this.totals
      const draft = this.filteredExams.filter(e => e.status === 'DRAFT').length
      const depts = {}
      this.filteredExams.forEach(e => { if (e.deptName) depts[e.deptName] = 1 })
      return [
        { key: 'ALL', label: '考核场次', value: this.filteredExams.length, unit: '场', color: '#1764f5',
          hint: '正式 ' + this.filteredExams.filter(e => e.examMode === 'FORMAL').length + ' · 模拟 ' +
                this.filteredExams.filter(e => e.examMode === 'PRACTICE').length, filter: 'ALL' },
        { key: 'DEPT', label: '覆盖部门', value: Object.keys(depts).length, unit: '个', color: '#0e7490',
          hint: '点此看按部门汇总', tone: 'up', filter: 'DEPT' },
        { key: 'SHOULD', label: '应考人次', value: t.should, unit: '人', color: '#7a5af8',
          hint: '仅统计正式考核', filter: 'FORMAL' },
        { key: 'ACTUAL', label: '实考 · 缺考', value: t.actual + ' · ' + t.absent, color: '#12b76a',
          hint: t.absent > 0 ? t.absent + ' 人未交卷' : '暂无缺考', tone: t.absent > 0 ? 'warn' : 'up', filter: 'FORMAL' },
        { key: 'SCORED', label: '已出分 · 通过', value: t.scored + ' · ' + t.passed, color: '#f79009',
          hint: t.scored ? '通过率 ' + Math.round(t.passed * 100 / t.scored) + '%' : '暂无已出分', filter: 'FORMAL' },
        { key: 'DRAFT', label: '待发布', value: draft, unit: '场', color: '#667085',
          hint: draft ? '点此只看待发布' : '无草稿', tone: draft ? 'warn' : '', filter: 'DRAFT' }
      ]
    },
    /** 按部门聚合（正式与模拟分开计数，人次只算正式） */
    deptRows() {
      const map = {}
      this.filteredExams.forEach(e => {
        const key = e.deptName || '未设置部门'
        if (!map[key]) {
          map[key] = { deptName: key, total: 0, formal: 0, practice: 0, should: 0, actual: 0, absent: 0, scored: 0, passed: 0 }
        }
        const d = map[key]
        d.total++
        if (e.examMode === 'FORMAL') {
          d.formal++
          const s = this.stat(e)
          d.should += s.should
          d.actual += s.actual
          d.absent += s.absent
          d.scored += s.scored
          d.passed += s.passed
        } else {
          d.practice++
        }
      })
      return Object.keys(map).map(k => {
        const d = map[k]
        return Object.assign(d, { rate: d.scored ? Math.round(d.passed * 100 / d.scored) : null })
      }).sort((a, b) => b.total - a.total)
    }
  },
  created() {
    this.loadAll()
  },
  methods: {
    loadAll() {
      this.loading = true
      listExam({ pageNum: 1, pageSize: 100 })
        .then(res => {
          this.exams = (res && res.rows) || []
          return this.loadStats()
        })
        .catch(() => { this.exams = [] })
        .finally(() => { this.loading = false })
    },
    /** 逐场拉应考名单（**只为正式考核**，模拟考核不计人次） */
    loadStats() {
      const formals = this.exams.filter(e => e.examMode === 'FORMAL')
      if (!formals.length) { this.statsMap = {}; return Promise.resolve() }
      return Promise.all(formals.map(e => gradingList(e.id).catch(() => null))).then(list => {
        const map = {}
        formals.forEach((e, i) => {
          const rows = (list[i] && list[i].data) || []
          map[e.id] = {
            should: rows.length,
            actual: rows.filter(r => r.sheetId).length,
            absent: rows.filter(r => !r.sheetId).length,
            scored: rows.filter(r => r.finalScore != null).length,
            passed: rows.filter(r => r.passFlag === 1 || r.passFlag === '1').length
          }
        })
        this.statsMap = map
      })
    },
    stat(e) {
      return this.statsMap[e.id] || { should: 0, actual: 0, absent: 0, scored: 0, passed: 0 }
    },
    /** KPI 点卡：切换筛选（再点取消） */
    applyKpi(k) {
      const off = this.activeKpi === k.key
      this.activeKpi = off ? '' : k.key
      if (k.filter === 'DEPT') {
        this.view = 'dept'
        return
      }
      this.view = 'session'
      if (off || k.filter === 'ALL') {
        this.filters.mode = ''
        this.filters.status = ''
      } else if (k.filter === 'FORMAL') {
        this.filters.mode = 'FORMAL'
        this.filters.status = ''
      } else if (k.filter === 'DRAFT') {
        this.filters.status = 'DRAFT'
      }
    },
    resetFilters() {
      this.filters = { deptName: '', mode: '', type: '', status: '', kw: '' }
      this.activeKpi = ''
    },
    /** 按部门视角 → 只看该部门（回到按场次视角，两个视角在同一条链路上合流） */
    focusDept(deptName) {
      this.filters.deptName = deptName
      this.activeKpi = ''
      this.view = 'session'
    },
    /** 进 L1 单场详情（顶层路由 + ?from= 回跳） */
    goSession(e) {
      this.$router.push({ path: '/exam-session/' + e.id, query: { from: this.$route.path } }).catch(() => {})
    },
    /** 进该场考核的配置页（现成的、超管可用；用 from 记住来源以便返回） */
    goConfig(e) {
      this.$router.push({ path: '/exam-config/' + e.id, query: { from: this.$route.path } }).catch(() => {})
    },
    /** 名称 ≤2 字或含 test/测试/demo/临时 ⇒ 疑似测试数据（库里存在 12 / 22 / 111 这类残留） */
    isTestData(e) {
      const n = String((e && e.examName) || '').trim()
      return !n || n.length <= 2 || /test|测试|demo|临时/i.test(n)
    },
    modeText(m) {
      return { FORMAL: '正式', PRACTICE: '模拟' }[m] || (m || '—')
    },
    statusText(s) {
      return { DRAFT: '待发布', PUBLISHED: '已发布', GRADING: '待批改', DISABLED: '已停用' }[s] || s || '—'
    },
    statusTone(s) {
      return { DRAFT: 'warn', PUBLISHED: 'ok', GRADING: 'blue', DISABLED: 'red' }[s] || ''
    },
    windowText(e) {
      const f = v => (v ? String(v).replace('T', ' ').slice(0, 16) : '')
      const s = f(e.startTime)
      const t = f(e.endTime)
      if (!s && !t) return '未设置'
      return (s || '--') + ' ~ ' + (t || '--')
    }
  }
}
</script>

<style lang="scss" scoped>
@import '~@/assets/styles/super-module.scss';

/* ① 总览 6 卡（可点即筛选） */
.s-kpis.kpi6 { grid-template-columns: repeat(6, minmax(0, 1fr)); }
.s-kpi.clickable { cursor: pointer; transition: border-color .15s, box-shadow .15s; }
.s-kpi.clickable:hover { border-color: #b9d2ff; box-shadow: 0 2px 8px rgba(23, 100, 245, .08); }
.s-kpi.active { border-color: #1764f5; box-shadow: 0 0 0 2px rgba(23, 100, 245, .12); }
.s-kpi .vl small { margin-left: 3px; }

/* ② 筛选条 */
.s-filter { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; padding: 12px 14px; margin-bottom: 12px; background: #fff; border: 1px solid #e7ecf3; border-radius: 8px; }
.s-filter-sum { margin-left: auto; color: #667085; font-size: 12px; }
.w150 { width: 150px; }
.w120 { width: 120px; }
.w180 { width: 180px; }

/* 视角切换 */
.s-viewsw { display: flex; gap: 8px; margin-bottom: 12px; }
.vs { padding: 6px 14px; color: #475467; background: #fff; border: 1px solid #e7ecf3; border-radius: 6px; font-size: 13px; cursor: pointer; transition: all .15s; }
.vs:hover { border-color: #b9d2ff; }
.vs.on { color: #1764f5; border-color: #1764f5; background: #f6faff; font-weight: 500; }
.vs em { margin-left: 4px; color: #98a2b3; font-size: 11.5px; font-style: normal; }
.vs.on em { color: #1764f5; }

/* ③ 列表 */
.s-tbl .clickable-row { cursor: pointer; }
.s-tbl .clickable-row:hover > td { background: #f6faff; }
.s-tbl .nm { color: #1d2939; }
.s-tbl .num { color: #344054; }
.s-tbl .num.ok { color: #23966f; }
.s-tbl .num.warn { color: #d9534f; }
.s-tbl .sep { margin: 0 4px; color: #cfd6df; }
.s-tbl .muted { color: #98a2b3; }
.s-badge.sm { margin-left: 5px; padding: 0 5px; font-size: 10.5px; }
.s-badge.purple { color: #7b5cf0; background: #f2eeff; }

@media (max-width: 1400px) { .s-kpis.kpi6 { grid-template-columns: repeat(3, minmax(0, 1fr)); } }
@media (max-width: 900px) {
  .s-kpis.kpi6 { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .s-filter-sum { margin-left: 0; }
}
</style>
