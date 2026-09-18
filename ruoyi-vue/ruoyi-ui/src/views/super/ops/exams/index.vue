<template>
  <div class="s-page">
    <header class="s-head">
      <div>
        <span class="eyebrow">SUPER ADMIN · OPERATION</span>
        <h1>考核运营总览</h1>
        <p>跨部门看「批次 × 环节」的发布状态与时间窗：哪些部门该开考却没开、哪些批次还停在草稿。<b>超管只读，不代部门发布</b>。</p>
      </div>
      <div class="s-head-actions">
        <span class="s-ro"><i class="el-icon-view" /> 全局只读</span>
        <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="loadAll">刷新</el-button>
      </div>
    </header>

    <div class="s-kpis">
      <div class="s-kpi">
        <div class="lb"><i class="dot" style="background:#1764f5" />批次总数</div>
        <div class="vl">{{ batches.length }}<small>个</small></div>
        <div class="ft">按考核名称归并的理论 + 实操环节</div>
      </div>
      <div class="s-kpi">
        <div class="lb"><i class="dot" style="background:#12b76a" />已发布环节</div>
        <div class="vl">{{ publishedCount }}<small>个</small></div>
        <div class="ft up">实习生端可见</div>
      </div>
      <div class="s-kpi">
        <div class="lb"><i class="dot" style="background:#f79009" />草稿环节</div>
        <div class="vl">{{ draftCount }}<small>个</small></div>
        <div class="ft warn">{{ draftCount ? '待部门管理员发布' : '无草稿' }}</div>
      </div>
      <div class="s-kpi">
        <div class="lb"><i class="dot" style="background:#7a5af8" />含指定人员</div>
        <div class="vl">{{ assignedCount }}<small>个</small></div>
        <div class="ft">仅名单内实习生可见</div>
      </div>
      <div class="s-kpi">
        <div class="lb"><i class="dot" style="background:#0e7490" />设了时间窗</div>
        <div class="vl">{{ windowCount }}<small>个</small></div>
        <div class="ft">到点自动开放 / 截止</div>
      </div>
      <div class="s-kpi">
        <div class="lb"><i class="dot" style="background:#667085" />覆盖部门</div>
        <div class="vl">{{ deptCount }}<small>个</small></div>
        <div class="ft">{{ deptCount < 5 ? '尚有部门未组织考核' : '全部部门已组织' }}</div>
      </div>
    </div>

    <div class="s-grid">
      <section class="s-card s-c12">
        <div class="s-card-h">
          <div class="tt"><span class="s-idx">批</span><h3>批次 × 环节明细</h3></div>
          <span class="hint">批次 = 一条 exam 记录；理论 / 实操为同批次两条记录</span>
        </div>
        <table v-if="batches.length" class="s-tbl">
          <thead>
            <tr><th>批次</th><th style="width:110px">部门</th><th style="width:100px">理论环节</th><th style="width:100px">实操环节</th><th style="width:74px">题量</th><th style="width:86px">通过线</th><th style="width:150px">时间窗</th><th style="width:110px">人员范围</th></tr>
          </thead>
          <tbody>
            <tr v-for="b in batches" :key="b.key">
              <td class="nm">{{ b.name }}</td>
              <td>{{ b.deptName || '—' }}</td>
              <td><span class="s-badge" :class="stageTone(b.theory)">{{ stageText(b.theory) }}</span></td>
              <td><span class="s-badge" :class="stageTone(b.practice)">{{ stageText(b.practice) }}</span></td>
              <td>{{ b.questionCount != null ? b.questionCount + ' 题' : '—' }}</td>
              <td>{{ b.passLine != null ? b.passLine + ' 分' : '—' }}</td>
              <td>{{ windowText(b) }}</td>
              <td><span class="s-badge" :class="b.assigned ? 'blue' : ''">{{ b.assigned ? '指定人员' : '全部在培' }}</span></td>
            </tr>
          </tbody>
        </table>
        <div v-else class="s-empty"><i class="el-icon-finished" /><span>暂无正式考核批次</span></div>
        <p class="s-note">
          <b>半场态：</b>理论已发布、实操仍草稿时，实习生端显示「理论已开放 · 实操待发布」——
          因为两个环节各自有独立 <code>status</code>。批次内部补考由 <code>exam_rule_snapshot.retake_count</code> 控制。
        </p>
      </section>
    </div>
  </div>
</template>

<script>
import { listExam } from '@/api/business/exam'

/**
 * 超管「考核运营总览」（全局只读）
 * 真实数据：/business/exam/list?examMode=FORMAL（超管命中全局范围）
 * 环节配对：同批次（同名）下的 THEORY / PRACTICAL 两条记录
 */
export default {
  name: 'SuperOpsExams',
  data() {
    return {
      loading: false,
      exams: []
    }
  },
  computed: {
    batches() {
      const map = {}
      this.exams.forEach(e => {
        const key = (e.examName || '未命名批次').trim()
        if (!map[key]) map[key] = { key, name: key, theory: null, practice: null, deptName: e.deptName }
        if (e.examType === 'PRACTICAL') map[key].practice = e
        else map[key].theory = e
      })
      return Object.keys(map).map(k => {
        const b = map[k]
        const main = b.theory || b.practice
        return Object.assign(b, {
          questionCount: main ? main.questionCount : null,
          passLine: main ? main.passLine : null,
          timeRange: main ? [main.startTime, main.endTime] : [null, null],
          assigned: false
        })
      })
    },
    allStages() {
      return this.exams
    },
    publishedCount() {
      return this.allStages.filter(e => e.status === 'PUBLISHED').length
    },
    draftCount() {
      return this.allStages.filter(e => e.status === 'DRAFT').length
    },
    assignedCount() {
      // 列表接口未返回名单规模，这里按 0 呈现；如需真实值需补「批次名单」接口
      return 0
    },
    windowCount() {
      return this.allStages.filter(e => e.startTime || e.endTime).length
    },
    deptCount() {
      const set = {}
      this.exams.forEach(e => { if (e.deptName) set[e.deptName] = 1 })
      return Object.keys(set).length
    }
  },
  created() {
    this.loadAll()
  },
  methods: {
    loadAll() {
      this.loading = true
      listExam({ pageNum: 1, pageSize: 200, examMode: 'FORMAL' })
        .then(res => { this.exams = res.rows || [] })
        .catch(() => { this.exams = [] })
        .finally(() => { this.loading = false })
    },
    stageText(exam) {
      if (!exam) return '未创建'
      return { DRAFT: '草稿', PUBLISHED: '已发布', GRADING: '待批改', DISABLED: '已停用', ENDED: '已结束' }[exam.status] || exam.status
    },
    stageTone(exam) {
      if (!exam) return ''
      return { DRAFT: 'warn', PUBLISHED: 'ok', GRADING: 'blue', DISABLED: 'red' }[exam.status] || ''
    },
    windowText(b) {
      const f = v => (v ? String(v).replace('T', ' ').slice(0, 16) : '')
      const s = f(b.timeRange[0])
      const e = f(b.timeRange[1])
      if (!s && !e) return '未设置'
      return (s || '--') + ' ~ ' + (e || '--')
    }
  }
}
</script>

<style lang="scss" scoped>
@import '~@/assets/styles/super-module.scss';
</style>
