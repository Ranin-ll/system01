<template>
  <div class="result-page">
    <div class="result-breadcrumb">
      <span>学习与考核</span>
      <span>/</span>
      <b>考核成绩与转正</b>
    </div>

    <header class="result-heading">
      <div>
        <span class="eyebrow">RESULTS &amp; PROMOTION</span>
        <h1>考核成绩与转正</h1>
        <p>只统计正式考核成绩，模拟自测不计入；通过后可提交转正申请，部门管理员审核通过即生效并发证。</p>
      </div>
      <div class="heading-actions">
        <el-select v-model="demoStatus" size="small" class="demo-select" @change="onDemoStatusChange">
          <el-option v-for="s in demoSampleOptions" :key="s.value" :label="'演示状态：' + s.label" :value="s.value" />
        </el-select>
        <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="loadAll">刷新</el-button>
      </div>
    </header>

    <!-- ① 结果总览 -->
    <section class="overview-strip">
      <div class="overview-state" :class="latestState.tone">
        <span class="state-label">最近结果</span>
        <strong>{{ latestState.text }}</strong>
        <span class="state-sub">{{ latestState.sub }}</span>
      </div>
      <div class="overview-metrics">
        <div><span>理论得分</span><strong>{{ latestScores.theory }}</strong></div>
        <div><span>实操得分</span><strong>{{ latestScores.practice }}</strong></div>
        <div><span>综合分</span><strong :class="latestScores.passed ? 'pass' : 'muted'">{{ latestScores.total }}</strong></div>
        <div><span>通过线</span><strong>{{ latestPassLine }}</strong></div>
      </div>
    </section>

    <div class="rg-grid">
      <!-- ② 成绩明细（按场次 / 环节） -->
      <section class="rg-card">
        <div class="section-heading">
          <div>
            <span class="section-index">02</span>
            <div>
              <h2>成绩明细</h2>
              <p>按场次与环节列出；未参加的场次显示 -- 与去向。</p>
            </div>
          </div>
          <span class="section-count">{{ detailRows.length }} 条</span>
        </div>
        <div v-loading="loading" class="rg-body">
          <div v-if="!detailRows.length && !loading" class="empty-state small">
            <i class="el-icon-tickets" />
            <strong>暂无正式考核成绩</strong>
            <span>完成培养并通过正式考核后，成绩会出现在这里。</span>
          </div>
          <table v-else class="rg-table">
            <thead><tr><th>场次</th><th>环节</th><th>分数</th><th>状态</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="row in detailRows" :key="row.key">
                <td class="ellipsis">{{ row.batch }}</td>
                <td>{{ row.stage }}</td>
                <td><b>{{ row.score }}</b></td>
                <td><el-tag size="mini" effect="plain" :type="row.tag">{{ row.status }}</el-tag></td>
                <td>
                  <el-button v-if="row.sheetId" type="text" size="mini" @click="openDetail(row)">明细</el-button>
                  <span v-else class="muted">--</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <p class="rg-note">模拟自测成绩不在此表统计（见「模拟考核」页）；历史场次成绩长期保留。</p>
      </section>

      <!-- ③ 得分对比 -->
      <section class="rg-card">
        <div class="section-heading">
          <div>
            <span class="section-index">03</span>
            <div>
              <h2>得分对比</h2>
              <p>我的综合分 vs 部门平均 vs 通过线。</p>
            </div>
          </div>
          <el-tag v-if="deptAvgIsSample" size="mini" type="warning" effect="plain">部门平均为示例数据</el-tag>
        </div>
        <div class="rg-body">
          <div class="vchart">
            <div v-for="bar in compareBars" :key="bar.label" class="vcol">
              <span class="bar" :class="bar.tone" :style="{ height: Math.max(bar.height, 8) + '%' }">{{ bar.display }}</span>
              <span class="vcol-lb">{{ bar.label }}</span>
            </div>
          </div>
          <p class="rg-note">{{ compareNote }}</p>
        </div>
      </section>
    </div>

    <!-- ④ 薄弱模块分析 -->
    <section class="rg-card">
      <div class="section-heading">
        <div>
          <span class="section-index">04</span>
          <div>
            <h2>薄弱模块分析</h2>
            <p>按知识模块统计理论题得分率，橙色为低于 60% 的短板模块。</p>
          </div>
        </div>
        <el-tag v-if="demoMode" size="mini" type="warning" effect="plain">示例数据</el-tag>
      </div>
      <div v-if="weakModules.length" class="rg-body">
        <div class="hbar" v-for="m in weakModules" :key="m.module" :class="{ low: m.rate < 60 }">
          <span class="nm">{{ m.module }}</span>
          <span class="track"><i :style="{ width: m.rate + '%' }" /></span>
          <span class="pc">{{ m.rate }}%</span>
        </div>
        <p class="rg-note">
          共 {{ weakModules.length }} 个模块 · 最弱模块 <b>{{ weakest.module }}</b>（{{ weakest.rate }}%），建议优先补强。
        </p>
      </div>
      <div v-else class="rg-body">
        <div class="empty-state small">
          <i class="el-icon-data-analysis" />
          <strong>暂无法生成模块分析</strong>
          <span>需要后端按知识点提供得分率统计接口（实习生账号无权读取题库知识点）。</span>
        </div>
      </div>
    </section>

    <!-- ⑤ 转正申请 -->
    <section class="rg-card promo-card">
      <div class="section-heading">
        <div>
          <span class="section-index">05</span>
          <div>
            <h2>转正申请</h2>
            <p>资格齐备后提交；部门管理员审核通过即生效并发证（本决策跳过超管终审）。</p>
          </div>
        </div>
        <el-tag size="mini" type="warning" effect="plain">演示态 · promotion_application 待后端</el-tag>
      </div>

      <div class="promo-grid">
        <!-- 资格核对清单 -->
        <div class="promo-check">
          <div class="promo-sub">资格核对清单</div>
          <div class="chk-list">
            <div v-for="item in checklist" :key="item.key" class="chk">
              <span class="mark" :class="item.pass ? 'ok' : 'no'">{{ item.pass ? '✓' : '!' }}</span>
              <span>{{ item.label }}</span>
              <b :class="item.pass ? 'good' : 'warn'">{{ item.value }}</b>
            </div>
          </div>
          <p class="rg-note">
            <template v-if="canApply">四项均满足，可以提交转正申请。</template>
            <template v-else>仍有 {{ failCount }} 项未满足，提交按钮已置灰；补齐后自动开放。</template>
          </p>
        </div>

        <!-- 状态与申请表单 -->
        <div class="promo-flow">
          <div class="promo-sub">
            申请状态
            <span class="promo-state" :class="promotionState.tone">{{ promotionState.label }}</span>
          </div>
          <div class="steps">
            <div v-for="(s, i) in timeline" :key="s.label" class="step" :class="s.state">
              <span class="mark">{{ s.state === 'done' ? '✓' : (s.state === 'now' ? '●' : i + 1) }}</span>
              <div class="txt"><b>{{ s.label }}</b><span>{{ s.desc }}</span></div>
            </div>
          </div>

          <!-- 驳回原因 -->
          <div v-if="currentStatus === 'REJECTED'" class="reject-box">
            <b>驳回原因</b>
            <p>{{ rejectReason }}</p>
          </div>

          <!-- 申请表单（未提交 / 已驳回可编辑） -->
          <div v-if="canEditForm" class="apply-form">
            <el-input
              v-model="applyForm.note"
              type="textarea"
              :rows="3"
              maxlength="300"
              show-word-limit
              placeholder="请填写转正说明 / 阶段自评（将随申请提交给部门管理员）"
            />
            <div class="apply-row">
              <el-upload :auto-upload="false" :limit="1" :show-file-list="true" accept="*" :on-change="onApplyFile">
                <el-button size="small" icon="el-icon-paperclip">附加材料（可选）</el-button>
              </el-upload>
              <el-button
                type="primary"
                size="small"
                :disabled="!canApply"
                :loading="submitting"
                @click="submitApplication"
              >{{ currentStatus === 'REJECTED' ? '重新提交转正申请' : '提交转正申请' }}</el-button>
            </div>
            <p class="rg-note">提交后状态变为「待部门审核」，部门管理员终审通过即生效并发证；演示态下操作仅本地生效。</p>
          </div>

          <!-- 进行中 / 已通过的操作 -->
          <div v-else-if="currentStatus === 'PENDING'" class="apply-actions">
            <span class="rg-note">申请已提交 {{ formMeta.submittedAt }}，等待部门管理员审核。</span>
            <el-button size="small" @click="withdrawApplication">撤回申请</el-button>
          </div>
          <div v-else class="apply-actions">
            <span class="rg-note">审批通过 · 已转为正式实习生，电子证书已生成（在工作台底部的「协议与证书」栏查看与下载）。</span>
            <el-button size="small" type="primary" plain @click="goWorkspace">去工作台查看</el-button>
          </div>

          <!-- 证书去向指引（本页不展示证书本体，统一在工作台底部） -->
          <p class="rg-note cert-hint"><i class="el-icon-medal" /> {{ certHint }}</p>
        </div>
      </div>
    </section>

    <!-- 逐题明细弹窗 -->
    <el-dialog title="成绩明细" :visible.sync="dialogVisible" width="720px" append-to-body>
      <div v-if="current" class="dialog-summary">
        <div><span>场次 / 环节</span><strong>{{ current.batch }} · {{ current.stage }}</strong></div>
        <div><span>最终得分</span><strong>{{ current.score }}</strong></div>
        <div><span>通过线</span><strong>{{ current.passLine }}</strong></div>
        <div><span>结果</span><strong>{{ current.resultText }}</strong></div>
      </div>
      <el-table v-if="currentItems.length" :data="currentItems" size="mini" max-height="380" stripe>
        <el-table-column label="题号" width="60" align="center" prop="seq" />
        <el-table-column label="题型" width="70" align="center">
          <template slot-scope="scope">{{ typeLabel(scope.row.qtype) }}</template>
        </el-table-column>
        <el-table-column label="我的作答" min-width="140" prop="userAnswer" show-overflow-tooltip />
        <el-table-column label="结果" width="80" align="center">
          <template slot-scope="scope">
            <el-tag size="mini" :type="scope.row.isCorrect === 1 ? 'success' : 'danger'">
              {{ scope.row.isCorrect === 1 ? '正确' : '错误' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="得分" width="70" align="center">
          <template slot-scope="scope">{{ scope.row.manualScore != null ? scope.row.manualScore : scope.row.aiScore }}</template>
        </el-table-column>
      </el-table>
      <div v-else class="empty-state small"><span>本次考核无逐题明细（实操类考核由导师批阅打分）。</span></div>
    </el-dialog>
  </div>
</template>

<script>
import { myExamList, sheetDetail } from '@/api/business/exam'
import { listLearningCourses } from '@/api/business/learning'
import { learningSummary } from '@/utils/learningPreview'
import { parseTime } from '@/utils/ruoyi'
import { mapGetters } from 'vuex'

const DURATION_UNKNOW = '--'
/** 转正门槛（与部门端 promotion.vue 的 PASS_LINE 保持一致） */
const PASS_LINE = 70

/**
 * 模拟静态数据（后端接口就绪前用于撑起版面，页面上均打「示例」标记）
 *
 * 对应后端缺口：
 *  - 部门平均分：需 `answer_sheet` 按部门的聚合接口
 *  - 薄弱模块分析：需按 `question.knowledge_point` 聚合得分率
 *  - 电子证书：`certificate` 表存在但无接口（证书展示统一在工作台底部「协议与证书」栏，本页不再呈现）
 *  - 转正申请单：`promotion_application` 零 Java 层（本页演示态本地闭环）
 */
const DEMO_DEPT_AVG = 74
const DEMO_WEAK_MODULES = [
  { module: '开发流程与代码交付规范', total: 8, correct: 4, rate: 50 },
  { module: 'MySQL 与数据访问', total: 10, correct: 6, rate: 60 },
  { module: 'Spring Boot 与接口开发', total: 12, correct: 9, rate: 75 },
  { module: 'Java 语言与面向对象', total: 12, correct: 11, rate: 92 }
]
const DEMO_REJECT_REASON = '实操能力未达门槛（实操 58 分），建议延长培养期并补齐 Docker 部署相关课程后重新提交。'

/**
 * 考核成绩与转正（实习生端）
 *
 * 真实数据（均为既有接口，不新增、不改动后端）：
 *  - `GET /business/answer-sheet/my?examMode=FORMAL` → 场次与环节成绩
 *  - `GET /business/answer-sheet/detail/{sheetId}`   → 提交时间 / 逐题明细
 *  - `GET /business/learning/courses`               → 学习完成率（转正资格①）
 *  - vuex getters：protocolStatus（转正资格④）、roles、deptName
 *
 * 演示数据（打「演示态 / 示例」标记，后端就绪后替换）：
 *  - 部门平均分、薄弱模块、转正申请单状态机（证书展示已移到工作台底部）
 */
export default {
  name: 'InternResult',
  data() {
    return {
      loading: false,
      rows: [],
      records: [],
      dialogVisible: false,
      current: null,
      currentItems: [],
      // 转正申请（演示态）
      demoStatus: '',
      applyForm: { note: '', fileName: '' },
      formMeta: { submittedAt: '--' },
      submitting: false,
      learningOverview: { progress: null, courseCount: 0, completedCourses: 0, learningCourses: 0, completedItems: 0, itemCount: 0, lastStudyTime: '尚未开始' }
    }
  },
  computed: {
    ...mapGetters(['roles', 'protocolStatus', 'deptName']),
    isFormal() { return this.roles.indexOf('FORMAL_TRAINEE') > -1 },
    demoSampleOptions() {
      return [
        { value: 'PASSED', label: '已通过（转正生效）' },
        { value: 'PENDING', label: '待部门审核' },
        { value: 'REJECTED', label: '已驳回' },
        { value: 'UNSUBMITTED', label: '未提交' }
      ]
    },
    /** 转正申请状态：默认按角色推导；演示下拉可强制切换 */
    currentStatus() {
      if (this.demoStatus) return this.demoStatus
      return this.isFormal ? 'PASSED' : 'UNSUBMITTED'
    },
    promotionState() {
      return {
        UNSUBMITTED: { label: '未提交', tone: 'gray' },
        PENDING: { label: '待部门审核', tone: 'info' },
        PASSED: { label: '审批通过 · 已转正', tone: 'ok' },
        REJECTED: { label: '已驳回', tone: 'warn' }
      }[this.currentStatus] || { label: '--', tone: 'gray' }
    },
    canEditForm() { return this.currentStatus === 'UNSUBMITTED' || this.currentStatus === 'REJECTED' },
    rejectReason() { return DEMO_REJECT_REASON },
    learningProgress() { return this.learningOverview.progress === null ? 0 : this.learningOverview.progress },
    completedRequired() {
      const required = this.records.filter(r => Number(r.isRequired) === 1)
      return { done: required.filter(r => r.progress === 100).length, total: required.length }
    },
    /** 场次 / 环节成绩（真实） */
    detailRows() {
      const rows = this.records.slice(0, 8).map(row => {
        const passed = row.passFlag === 1
        return {
          key: 'r' + row.sheetId,
          batch: this.batchKeyOf(row.examName),
          stage: row.examType === 'PRACTICAL' ? '实操' : '理论',
          score: row.finalScore,
          status: passed ? '已通过' : '未通过',
          tag: passed ? 'success' : 'danger',
          sheetId: row.sheetId,
          passLine: row.passLine,
          resultText: passed ? '已通过' : '未通过',
          _items: row._items || []
        }
      })
      this.records.forEach(row => {
        if (row.finalScore == null) {
          rows.push({
            key: 'p' + row.examId,
            batch: this.batchKeyOf(row.examName),
            stage: row.examType === 'PRACTICAL' ? '实操' : '理论',
            score: '--',
            status: row.sheetStatus === 'IN_PROGRESS' ? '作答中' : '未参加',
            tag: 'info',
            sheetId: null,
            passLine: row.passLine,
            resultText: '未参加',
            _items: []
          })
        }
      })
      return rows
    },
    latest() { return this.records.length ? this.records[0] : null },
    latestState() {
      if (!this.latest) return { tone: 'gray', text: '--', sub: '尚未参加正式考核' }
      return this.latest.passFlag === 1
        ? { tone: 'green', text: '已通过', sub: this.batchKeyOf(this.latest.examName) }
        : { tone: 'red', text: '未通过', sub: this.batchKeyOf(this.latest.examName) }
    },
    latestScores() {
      if (!this.latest) return { theory: '--', practice: '--', total: '--', passed: false }
      const isPractical = this.latest.examType === 'PRACTICAL'
      const other = this.records.find(r => r.examType !== this.latest.examType && r.finalScore != null)
      const theory = isPractical ? (other ? other.finalScore : null) : this.latest.finalScore
      const practice = isPractical ? this.latest.finalScore : (other ? other.finalScore : null)
      const total = theory != null && practice != null ? Math.round((theory * 0.4 + practice * 0.6) * 10) / 10 : null
      return {
        theory: theory != null ? theory : '--',
        practice: practice != null ? practice : '--',
        total: total != null ? total : '--',
        passed: !!this.latest.passFlag && this.latest.passFlag === 1
      }
    },
    latestPassLine() {
      return this.latest && this.latest.passLine != null ? this.latest.passLine : '--'
    },
    compareBars() {
      const mine = Number(this.latestScores.total)
      const line = this.latestPassLine === '--' ? null : Number(this.latestPassLine)
      const avg = DEMO_DEPT_AVG
      const max = Math.max(mine || 0, line || 0, avg || 0, 100)
      return [
        { label: '我的综合', tone: mine >= PASS_LINE ? 'g' : 'none', height: mine ? Math.round(mine / max * 100) : 0, display: mine ? String(mine) : '--' },
        { label: '部门平均', tone: 'hi', height: Math.round(avg / max * 100), display: String(avg) },
        { label: '通过线', tone: 'o', height: line != null ? Math.round(line / max * 100) : 0, display: line != null ? String(line) : '--' }
      ]
    },
    compareNote() {
      if (!this.latest) return '暂无正式考核记录，完成考核并发布成绩后此处显示对比数据；部门平均为示例值。'
      return '部门平均分为示例数据，后端按部门聚合接口就绪后替换；综合分 = 理论 × 40% + 实操 × 60%，权重由批次规则决定。'
    },
    deptAvgIsSample() { return true },
    demoMode() { return true },
    weakModules() { return DEMO_WEAK_MODULES },
    weakest() {
      if (!this.weakModules.length) return { module: '--', rate: 0 }
      return this.weakModules.reduce((min, m) => (m.rate < min.rate ? m : min), this.weakModules[0])
    },
    /** 转正资格核对清单（三项真实 + 一项来自考核成绩） */
    checklist() {
      const signed = Number(this.protocolStatus) === 1
      return [
        {
          key: 'study',
          pass: this.learningProgress >= PASS_LINE,
          label: '学习完成率 ≥ ' + PASS_LINE + '%',
          value: '当前 ' + this.learningProgress + '%'
        },
        {
          key: 'exam',
          pass: Number(this.latestScores.total) >= 60,
          label: '正式考核已通过',
          value: this.latestScores.total === '--' ? '暂无成绩' : '综合 ' + this.latestScores.total + ' 分'
        },
        {
          key: 'required',
          pass: this.completedRequired.total > 0 && this.completedRequired.done === this.completedRequired.total,
          label: '无未完成必修项',
          value: this.completedRequired.total ? this.completedRequired.done + ' / ' + this.completedRequired.total + ' 门' : '--'
        },
        {
          key: 'protocol',
          pass: signed,
          label: '保密协议已签署',
          value: signed ? '已签署' : '待签署'
        }
      ]
    },
    failCount() { return this.checklist.filter(i => !i.pass).length },
    canApply() { return this.failCount === 0 },
    /** 申请状态时间线（四态演示） */
    timeline() {
      const status = this.currentStatus
      const submitted = this.formMeta.submittedAt
      const base = [
        { label: '提交转正申请', desc: status === 'UNSUBMITTED' ? '待提交 · 填写转正说明与阶段自评' : '已提交 · ' + submitted, state: status === 'UNSUBMITTED' ? 'todo' : 'done' },
        { label: '部门管理员审核', desc: status === 'UNSUBMITTED' ? '未开始' : (status === 'PENDING' ? (this.deptName || '所属部门') + '管理员审核中' : '已完成审核'), state: status === 'UNSUBMITTED' ? 'todo' : (status === 'PENDING' ? 'now' : 'done') },
        { label: '审批通过 · 即时生效', desc: status === 'PASSED' ? '已转为正式实习生（FORMAL_TRAINEE）' : (status === 'REJECTED' ? '本次未通过，修正后可重新提交' : '未开始'), state: status === 'PASSED' ? 'done' : 'todo' },
        { label: '自动发证', desc: status === 'PASSED' ? '证书已生成，可在工作台底部「协议与证书」栏查看' : '未开始', state: status === 'PASSED' ? 'done' : 'todo' }
      ]
      if (status === 'REJECTED') base[1].state = 'done'
      return base
    },
    /** 证书展示已移至工作台底部「协议与证书」栏，本页只呈现申请状态 */
    certHint() {
      if (this.currentStatus === 'PASSED') return '证书已生成，可在工作台底部「协议与证书」栏查看与下载'
      if (this.currentStatus === 'REJECTED') return '转正申请被驳回，修正并重新提交、审批通过后生成证书'
      if (this.currentStatus === 'PENDING') return '等待部门管理员审核通过后生成证书'
      return '正式考核通过并提交转正申请、审批通过后生成证书'
    }
  },
  created() {
    this.loadAll()
  },
  methods: {
    loadAll() {
      this.loading = true
      Promise.all([this.loadExams(), this.loadLearning()]).then(() => { this.loading = false }).catch(() => { this.loading = false })
    },
    loadExams() {
      return myExamList('FORMAL').then(res => {
        this.rows = res.data || []
        return this.buildRecords()
      }).catch(() => {
        this.rows = []
        this.records = []
      })
    },
    loadLearning() {
      return listLearningCourses().then(res => {
        this.learningOverview = learningSummary(res.data || [])
      }).catch(() => {
        this.learningOverview = learningSummary([])
      })
    },
    /** 场次名：考核名称里的批次 / 期次标识（示例解析，后端补批次字段后可直取） */
    batchKeyOf(examName) {
      const name = examName || '未命名场次'
      const matched = name.match(/(\d{4}Q\d)|(第[一二三四五六七八九十\d]+[期批场])/)
      return matched ? matched[0] : name
    },
    buildRecords() {
      const done = this.rows.filter(r => r.sheet && r.sheet.status === 'PUBLISHED')
      const pending = this.rows.filter(r => !r.sheet || r.sheet.status !== 'PUBLISHED')
      const tasks = done.slice(0, 8).map(row => sheetDetail(row.sheet.sheetId).then(res => {
        const sheet = (res.data && res.data.sheet) || {}
        return {
          examId: row.examId,
          examName: row.examName,
          examType: row.examType,
          passLine: row.passLine,
          sheetId: row.sheet.sheetId,
          sheetStatus: row.sheet.status,
          finalScore: row.sheet.finalScore,
          passFlag: row.sheet.passFlag,
          submitTime: sheet.submitTime,
          submitTimeText: sheet.submitTime ? parseTime(sheet.submitTime, '{y}-{m}-{d} {h}:{i}') : '--',
          durationText: this.durationText(sheet.startTime, sheet.submitTime),
          _items: res.data && res.data.items ? res.data.items : []
        }
      }).catch(() => ({
        examId: row.examId,
        examName: row.examName,
        examType: row.examType,
        passLine: row.passLine,
        sheetId: row.sheet.sheetId,
        sheetStatus: row.sheet.status,
        finalScore: row.sheet.finalScore,
        passFlag: row.sheet.passFlag,
        submitTime: null,
        submitTimeText: '--',
        durationText: DURATION_UNKNOW,
        _items: []
      })))
      return Promise.all(tasks).then(list => {
        list.sort((a, b) => new Date(b.submitTime || 0) - new Date(a.submitTime || 0))
        // 未出分/未参加的场次排在后面，用真实 exam 信息占位
        const rest = pending.map(row => ({
          examId: row.examId,
          examName: row.examName,
          examType: row.examType,
          passLine: row.passLine,
          sheetId: null,
          sheetStatus: row.sheet ? row.sheet.status : null,
          finalScore: null,
          passFlag: null,
          submitTime: null,
          submitTimeText: '--',
          durationText: DURATION_UNKNOW,
          _items: []
        }))
        this.records = list.concat(rest)
      })
    },
    durationText(start, end) {
      if (!start || !end) return DURATION_UNKNOW
      const ms = new Date(end) - new Date(start)
      if (!(ms > 0)) return DURATION_UNKNOW
      const minutes = Math.round(ms / 60000)
      return minutes >= 60 ? Math.floor(minutes / 60) + ' 小时 ' + (minutes % 60) + ' 分' : minutes + ' 分钟'
    },
    typeLabel(qtype) {
      return { SINGLE: '单选', MULTI: '多选', JUDGE: '判断' }[qtype] || (qtype || '--')
    },
    openDetail(row) {
      this.current = row
      this.currentItems = row._items || []
      this.dialogVisible = true
    },
    onDemoStatusChange() {
      if (this.currentStatus === 'PENDING' || this.currentStatus === 'PASSED') {
        this.formMeta.submittedAt = parseTime(new Date(), '{y}-{m}-{d} {h}:{i}')
      }
    },
    onApplyFile(file) {
      this.applyForm.fileName = file.name || ''
      this.$modal.msgSuccess('已选择附加材料：' + this.applyForm.fileName + '（演示态，未上传）')
    },
    submitApplication() {
      if (!this.canApply) {
        this.$modal.msgWarning('资格未齐备，请先补齐清单中的未满足项')
        return
      }
      if (!this.applyForm.note || !this.applyForm.note.trim()) {
        this.$modal.msgWarning('请先填写转正说明 / 阶段自评')
        return
      }
      this.submitting = true
      this.formMeta.submittedAt = parseTime(new Date(), '{y}-{m}-{d} {h}:{i}')
      this.demoStatus = 'PENDING'
      this.submitting = false
      this.$modal.msgSuccess('转正申请已提交，等待部门管理员审核（演示态：promotion_application 接口就绪后真实提交）')
    },
    withdrawApplication() {
      this.$modal.confirm('确认撤回本次转正申请吗？撤回后可重新编辑并提交。').then(() => {
        this.demoStatus = 'UNSUBMITTED'
        this.$modal.msgSuccess('已撤回转正申请（演示态）')
      }).catch(() => {})
    },
    /** 证书在工作台底部「协议与证书」栏，这里直接带去工作台 */
    goWorkspace() {
      this.$router.push('/index')
    }
  }
}
</script>

<style lang="scss" scoped>
.result-page { min-height: 100%; padding: 22px 24px 40px; color: #283544; background: #f3f5f8; }
.result-breadcrumb { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; color: #98a2b3; font-size: 12px; }
.result-breadcrumb b { color: #475467; font-weight: 500; }
.result-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; margin-bottom: 18px; }
.eyebrow { color: #2878c7; font-size: 11px; letter-spacing: .08em; }
.result-heading h1 { margin: 5px 0 7px; color: #1d2939; font-size: 26px; font-weight: 600; }
.result-heading p { margin: 0; max-width: 720px; color: #667085; font-size: 13px; line-height: 1.6; }
.heading-actions { display: flex; align-items: center; gap: 8px; flex: none; }
.demo-select { width: 210px; }

.overview-strip { display: flex; align-items: center; gap: 28px; margin-bottom: 16px; padding: 20px 24px; border-left: 4px solid #1764f5; background: #fff; box-shadow: 0 1px 3px rgba(16, 24, 40, .04); }
.overview-state { display: flex; min-width: 210px; flex-direction: column; gap: 5px; }
.state-label { color: #8490a0; font-size: 11px; }
.overview-state strong { font-size: 24px; font-weight: 600; }
.overview-state.green strong { color: #23966f; }
.overview-state.red strong { color: #d64545; }
.overview-state.gray strong { color: #98a2b3; }
.state-sub { color: #8490a0; font-size: 12px; }
.overview-metrics { display: grid; flex: 1; grid-template-columns: repeat(4, 1fr); gap: 22px; }
.overview-metrics div { padding-left: 18px; border-left: 1px solid #edf0f4; }
.overview-metrics span, .overview-metrics strong { display: block; }
.overview-metrics span { margin-bottom: 8px; color: #8490a0; font-size: 11px; }
.overview-metrics strong { color: #1d2939; font-size: 17px; font-weight: 600; }
.overview-metrics strong.muted { color: #98a2b3; }
.overview-metrics strong.pass { color: #067647; }

.rg-grid { display: grid; grid-template-columns: minmax(0, 7fr) minmax(0, 5fr); gap: 14px; }
.rg-card { margin-bottom: 14px; padding: 18px 20px 20px; background: #fff; border: 1px solid #e7ecf3; border-radius: 8px; }
.rg-grid .rg-card { margin-bottom: 14px; }
.section-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding-bottom: 14px; border-bottom: 1px solid #edf0f4; }
.section-heading > div { display: flex; align-items: flex-start; gap: 12px; }
.section-index { color: #1764f5; font-size: 11px; font-weight: 700; }
.section-heading h2 { margin: 0 0 5px; color: #1d2939; font-size: 17px; font-weight: 600; }
.section-heading p { margin: 0; color: #8490a0; font-size: 12px; }
.section-count { color: #667085; font-size: 12px; }
.rg-body { padding-top: 14px; }
.rg-note { margin: 10px 0 0; color: #98a2b3; font-size: 11.5px; line-height: 1.7; }
.rg-note b { color: #667085; }

.rg-table { width: 100%; border-collapse: collapse; }
.rg-table th { padding: 9px 10px; color: #8490a0; background: #f8fafc; font-size: 12px; font-weight: 500; text-align: left; }
.rg-table td { padding: 10px; border-bottom: 1px solid #edf0f4; color: #475467; font-size: 12.5px; }
.rg-table td b { color: #1d2939; font-size: 13.5px; }
.rg-table td.ellipsis { max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.muted { color: #b7c1cc; font-size: 12px; }

/* 柱图 */
.vchart { display: flex; align-items: flex-end; gap: 12px; height: 150px; padding: 4px 2px 0; }
.vcol { display: flex; height: 100%; flex: 1; align-items: center; flex-direction: column; justify-content: flex-end; gap: 7px; }
.vcol .bar { display: flex; width: 68%; max-width: 54px; min-height: 8%; align-items: flex-start; justify-content: center; padding-top: 5px; border-radius: 5px 5px 0 0; color: #fff; background: #1764f5; font-size: 12px; }
.vcol .bar.g { background: #12b76a; }
.vcol .bar.hi { background: #2878c7; }
.vcol .bar.o { background: #f79009; }
.vcol .bar.none { color: #98a2b3; background: #f2f4f7; }
.vcol-lb { color: #98a2b3; font-size: 11.5px; }

/* 模块横条 */
.hbar { display: flex; align-items: center; gap: 10px; min-height: 34px; }
.hbar .nm { width: 190px; flex: 0 0 auto; overflow: hidden; color: #475467; font-size: 12.5px; text-overflow: ellipsis; white-space: nowrap; }
.hbar .track { height: 9px; flex: 1; overflow: hidden; border-radius: 5px; background: #edf1f5; }
.hbar .track i { display: block; height: 100%; border-radius: 5px; background: #12b76a; }
.hbar.low .track i { background: #f79009; }
.hbar .pc { width: 46px; flex: 0 0 auto; color: #667085; font-size: 12px; text-align: right; }
.hbar.low .pc { color: #b54708; }

/* 转正申请 */
.promo-grid { display: grid; grid-template-columns: minmax(0, 4fr) minmax(0, 8fr); gap: 20px; padding-top: 14px; }
.promo-sub { margin-bottom: 12px; color: #475467; font-size: 12.5px; font-weight: 600; display: flex; align-items: center; gap: 8px; }
.promo-state { padding: 2px 9px; font-size: 11px; font-weight: 400; border-radius: 10px; background: #f2f4f7; color: #667085; }
.promo-state.ok { color: #1a7a58; background: #e4f5ee; }
.promo-state.info { color: #1764f5; background: #e2ecff; }
.promo-state.warn { color: #b54708; background: #fff4e5; }
.promo-state.gray { color: #98a2b3; background: #f2f4f7; }
.chk-list { display: grid; gap: 10px; }
.chk { display: flex; align-items: center; gap: 8px; color: #475467; font-size: 12.5px; }
.chk b { margin-left: auto; color: #1d2939; font-weight: 600; }
.chk b.good { color: #067647; }
.chk b.warn { color: #b54708; }
.chk .mark { display: inline-flex; width: 18px; height: 18px; flex: none; align-items: center; justify-content: center; color: #98a2b3; background: #f2f4f7; font-size: 11px; border-radius: 50%; }
.chk .mark.ok { color: #fff; background: #12b76a; }
.chk .mark.no { color: #fff; background: #f79009; }
.steps { display: grid; gap: 4px; }
.step { display: flex; align-items: center; gap: 12px; padding: 9px 0; border-bottom: 1px solid #edf0f4; }
.step:last-child { border-bottom: 0; }
.step .mark { display: flex; width: 26px; height: 26px; flex: none; align-items: center; justify-content: center; border-radius: 50%; color: #98a2b3; background: #f2f4f7; font-size: 12px; }
.step.done .mark { color: #fff; background: #12b76a; }
.step.now .mark { color: #fff; background: #1764f5; }
.step .txt b { display: block; color: #344054; font-size: 13px; }
.step .txt span { display: block; margin-top: 3px; color: #98a2b3; font-size: 11.5px; }
.step:not(.done):not(.now) .txt b { color: #98a2b3; }
.reject-box { margin-top: 12px; padding: 12px 14px; border: 1px solid #fbd5d5; background: #fffafa; border-radius: 6px; }
.reject-box b { color: #b42318; font-size: 12.5px; }
.reject-box p { margin: 6px 0 0; color: #7a271a; font-size: 12.5px; line-height: 1.6; }
.apply-form { margin-top: 14px; padding-top: 14px; border-top: 1px dashed #e7ecf3; }
.apply-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 12px; }
.apply-actions { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 14px; padding-top: 14px; border-top: 1px dashed #e7ecf3; }
.apply-actions .rg-note { margin: 0; }

/* 证书 */
.overview-strip, .rg-card { min-width: 0; }
.cert-hint { display: flex; align-items: center; gap: 6px; margin-top: 12px; padding-top: 12px; border-top: 1px dashed #e7ecf3; }
.cert-hint i { color: #b7c1cc; }
.dialog-summary { display: grid; margin-bottom: 14px; grid-template-columns: repeat(4, 1fr); gap: 14px; }
.dialog-summary span { display: block; margin-bottom: 6px; color: #8490a0; font-size: 11px; }
.dialog-summary strong { color: #1d2939; font-size: 14px; font-weight: 600; }
.empty-state { display: flex; align-items: center; justify-content: center; flex-direction: column; gap: 9px; color: #98a2b3; }
.empty-state.small { min-height: 120px; }
.empty-state i { color: #b7c1cc; font-size: 30px; }
.empty-state strong { color: #667085; font-size: 14px; font-weight: 500; }
.empty-state span { max-width: 520px; font-size: 12px; line-height: 1.6; text-align: center; }

@media (max-width: 1200px) {
  .rg-grid { grid-template-columns: 1fr; }
  .promo-grid { grid-template-columns: 1fr; }
}
@media (max-width: 760px) {
  .result-page { padding: 16px 12px 32px; }
  .result-heading { align-items: flex-start; flex-direction: column; gap: 12px; }
  .result-heading h1 { font-size: 22px; }
  .heading-actions { width: 100%; }
  .demo-select { flex: 1; width: auto; }
  .overview-strip { align-items: flex-start; flex-direction: column; gap: 16px; padding: 16px 18px; }
  .overview-metrics { width: 100%; grid-template-columns: repeat(2, 1fr); gap: 12px; }
  .overview-metrics div { padding-left: 10px; }
  .hbar .nm { width: 110px; }
  .apply-row, .apply-actions { align-items: flex-start; flex-direction: column; }
}
</style>
