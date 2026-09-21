<template>
  <div class="s-page">
    <header class="s-head">
      <div>
        <span class="eyebrow">SUPER ADMIN · OPERATION</span>
        <h1>成绩与统计分析</h1>
        <p>全局成绩分布、部门对比与共性薄弱知识点。数据来自正式考核答卷（<b>仿真自测不计入</b>）。其中「按知识点聚合」与「部门均值」需跨部门聚合接口。</p>
      </div>
      <div class="s-head-actions">
        <span class="s-ro"><i class="el-icon-view" /> 全局只读</span>
        <span class="s-ro sample"><i class="el-icon-time" /> 聚合接口待补</span>
        <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="loadAll">刷新</el-button>
      </div>
    </header>

    <div class="s-kpis">
      <div class="s-kpi">
        <div class="lb"><i class="dot" style="background:#1764f5" />已发布成绩</div>
        <div class="vl">{{ records.length }}<small>条</small></div>
        <div class="ft">正式考核答卷（PUBLISHED）</div>
      </div>
      <div class="s-kpi">
        <div class="lb"><i class="dot" style="background:#12b76a" />通过</div>
        <div class="vl">{{ passCount }}<small>条</small></div>
        <div class="ft up">{{ records.length ? '通过率 ' + passRate + '%' : '暂无成绩' }}</div>
      </div>
      <div class="s-kpi">
        <div class="lb"><i class="dot" style="background:#f04438" />未通过</div>
        <div class="vl">{{ records.length - passCount }}<small>条</small></div>
        <div class="ft">可按批次下钻到个人</div>
      </div>
      <div class="s-kpi">
        <div class="lb"><i class="dot" style="background:#7a5af8" />平均分</div>
        <div class="vl">{{ avgScore }}<small>分</small></div>
        <div class="ft">跨部门合计</div>
      </div>
      <div class="s-kpi">
        <div class="lb"><i class="dot" style="background:#0e7490" />最高分</div>
        <div class="vl">{{ maxScore }}<small>分</small></div>
        <div class="ft">单场最高</div>
      </div>
      <div class="s-kpi">
        <div class="lb"><i class="dot" style="background:#667085" />覆盖批次</div>
        <div class="vl">{{ batchCount }}<small>个</small></div>
        <div class="ft">已发布成绩的考核</div>
      </div>
    </div>

    <div class="s-grid">
      <section class="s-card s-c7">
        <div class="s-card-h">
          <div class="tt"><span class="s-idx">布</span><h3>综合分分布</h3></div>
          <span class="hint">按 10 分区间</span>
        </div>
        <div v-if="records.length" class="s-vchart">
          <div v-for="d in distribution" :key="d.label" class="s-vcol">
            <span class="bar" :class="{ hi: d.hi }" :style="{ height: d.height }">{{ d.count }}</span>
            <span class="lb">{{ d.label }}</span>
          </div>
        </div>
        <div v-else class="s-empty"><i class="el-icon-data-analysis" /><span>暂无已发布成绩</span></div>
        <p class="s-note">通过线取批次配置（当前实现多为 60 分）；未出分 / 未参加的记录不参与分布，也不以 0 占位。</p>
      </section>

      <section class="s-card s-c5">
        <div class="s-card-h">
          <div class="tt"><span class="s-idx g">题</span><h3>各批次成绩</h3></div>
          <span class="hint">按得分率排序</span>
        </div>
        <div v-if="byBatch.length" class="s-steps">
          <div v-for="b in byBatch" :key="b.name" class="s-step">
            <span class="mark">{{ b.count }}</span>
            <div class="txt">
              <b>{{ b.name }}</b>
              <span>平均 {{ b.avg }} 分 · 通过 {{ b.pass }} / {{ b.count }} 人 · 通过线 {{ b.passLine != null ? b.passLine : '--' }}</span>
            </div>
          </div>
        </div>
        <div v-else class="s-empty"><i class="el-icon-tickets" /><span>暂无成绩记录</span></div>
      </section>

      <section class="s-card s-c7">
        <div class="s-card-h">
          <div class="tt"><span class="s-idx o">点</span><h3>薄弱知识点 Top5（示例）</h3></div>
          <span class="s-badge warn">待聚合接口</span>
        </div>
        <div class="s-hbar low" v-for="k in weakPoints" :key="k.name">
          <span class="nm">{{ k.name }}</span>
          <span class="track"><i :style="{ width: k.rate + '%' }" /></span>
          <span class="pc">{{ k.rate }}%</span>
        </div>
        <p class="s-note">
          真实口径：<code>question.knowledge_point</code> × <code>answer_sheet_item.is_correct</code> 按知识点聚合得分率（全局版）。
          部门端已有单部门实现，全局版需新增一个聚合接口 —— 当前展示为示例值。
        </p>
      </section>

      <section class="s-card s-c5">
        <div class="s-card-h">
          <div class="tt"><span class="s-idx p">导</span><h3>导出与合规</h3></div>
          <span class="s-badge warn">接口待补</span>
        </div>
        <div class="s-steps">
          <div class="s-step done"><span class="mark">1</span><div class="txt"><b>部门成绩汇总（Excel）</b><span>按部门 / 批次导出成绩台账</span></div></div>
          <div class="s-step done"><span class="mark">2</span><div class="txt"><b>实习生明细台账（Excel）</b><span>含培养状态与成绩</span></div></div>
          <div class="s-step done"><span class="mark">3</span><div class="txt"><b>题库覆盖度报表（Excel）</b><span>知识点 × 题量 × 覆盖情况</span></div></div>
        </div>
        <div class="s-callout warn">
          <i class="el-icon-warning-outline" />
          <span>
            <b>合规要求：</b>超管导出含个人成绩 / 身份信息的数据，必须记录
            「<b>谁、何时、导出了哪份、多少行</b>」写入 <code>audit_record</code>（字段：<code>export_time / export_by / data_scope / row_count / file_hash</code>），
            并在「审计与合规」页可查。
          </span>
        </div>
      </section>
    </div>
  </div>
</template>

<script>
/**
 * 超管「成绩与统计分析」
 *
 * 真实数据：/business/answer-sheet/my?examMode=FORMAL 仅在「当前登录人」维度可用，
 * 超管这里是**跨人**统计，需要聚合接口 —— 因此本页先按「已发布成绩记录」维度做骨架构图，
 * 有接口后再换成科室/人员维度。（当前数据源见 loadAll 注释）
 */
export default {
  name: 'SuperOpsScores',
  data() {
    return {
      loading: false,
      records: [],
      weakPoints: [
        { name: 'Docker 部署', rate: 47 },
        { name: 'MySQL 索引优化', rate: 58 },
        { name: '代码交付规范', rate: 63 },
        { name: '接口异常处理', rate: 69 },
        { name: '多表关联查询', rate: 72 }
      ]
    }
  },
  computed: {
    passCount() {
      return this.records.filter(r => r.passFlag === 1).length
    },
    passRate() {
      return this.records.length ? Math.round(this.passCount / this.records.length * 100) : 0
    },
    avgScore() {
      if (!this.records.length) return '--'
      const sum = this.records.reduce((s, r) => s + Number(r.finalScore || 0), 0)
      return Math.round(sum / this.records.length * 10) / 10
    },
    maxScore() {
      if (!this.records.length) return '--'
      return Math.max.apply(null, this.records.map(r => Number(r.finalScore || 0)))
    },
    batchCount() {
      const set = {}
      this.records.forEach(r => { if (r.batchName) set[r.batchName] = 1 })
      return Object.keys(set).length
    },
    distribution() {
      const buckets = [
        { label: '<60', min: 0, max: 59.999 },
        { label: '60-69', min: 60, max: 69.999 },
        { label: '70-79', min: 70, max: 79.999 },
        { label: '80-89', min: 80, max: 89.999 },
        { label: '≥90', min: 90, max: 1e9 }
      ]
      const counts = buckets.map(b => ({
        label: b.label,
        count: this.records.filter(r => Number(r.finalScore || 0) >= b.min && Number(r.finalScore || 0) <= b.max).length
      }))
      const max = counts.reduce((m, c) => Math.max(m, c.count), 0) || 1
      return counts.map(c => Object.assign(c, {
        height: Math.max(Math.round(c.count / max * 100), c.count ? 10 : 6) + '%',
        hi: c.count === max && c.count > 0
      }))
    },
    byBatch() {
      const map = {}
      this.records.forEach(r => {
        const key = r.batchName || '未命名批次'
        if (!map[key]) map[key] = { name: key, scores: [], pass: 0, passLine: r.passLine }
        map[key].scores.push(Number(r.finalScore || 0))
        if (r.passFlag === 1) map[key].pass++
      })
      return Object.keys(map).map(k => {
        const b = map[k]
        const sum = b.scores.reduce((s, v) => s + v, 0)
        return {
          name: b.name,
          count: b.scores.length,
          pass: b.pass,
          passLine: b.passLine,
          avg: b.scores.length ? Math.round(sum / b.scores.length * 10) / 10 : '--'
        }
      }).sort((a, b) => b.avg - a.avg)
    }
  },
  created() {
    this.loadAll()
  },
  methods: {
    /**
     * 数据源说明：后端暂无「全局成绩聚合」接口（部门端是单人视角）。
     * 待补接口：GET /business/answer-sheet/all?examMode=FORMAL&deptId=  返回
     *   [{ examName, userId, userName, deptName, finalScore, passFlag, passLine }]
     * 接口就绪前 records 保持为空 → 页面显示空态与「接口待补」标注，不伪造数字。
     */
    loadAll() {
      this.loading = true
      this.records = []
      this.loading = false
    },
  }
}
</script>

<style lang="scss" scoped>
@import '~@/assets/styles/super-module.scss';
</style>
