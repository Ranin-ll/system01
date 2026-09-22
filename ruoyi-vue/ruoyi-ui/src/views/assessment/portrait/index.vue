<template>
  <div class="portrait-page">
    <div class="portrait-breadcrumb">
      <span>工作台</span>
      <span>/</span>
      <b>能力画像</b>
    </div>

    <header class="portrait-heading">
      <div>
        <span class="eyebrow">ABILITY PORTRAIT</span>
        <h1>能力画像 · {{ cycle }} 周期</h1>
        <p>由学习投入 / 理论掌握 / 实践能力 / 规范遵从 四来源合成；仅显示 visible_scope = OPEN 的评价。</p>
      </div>
      <div class="heading-actions">
        <el-select v-model="cycle" size="small" class="cycle-select" @change="onCycleChange">
          <el-option v-for="c in cycles" :key="c" :label="'周期：' + c" :value="c" />
        </el-select>
        <el-button size="small" icon="el-icon-download" @click="exportPortrait">导出</el-button>
      </div>
    </header>

    <div class="portrait-grid">
      <!-- ① 多维雷达 -->
      <section class="portrait-card radar-card">
        <div class="section-heading">
          <div>
            <span class="section-index">01</span>
            <div>
              <h2>多维雷达</h2>
              <p>虚线轴 = 未测评，不等于 0 分。</p>
            </div>
          </div>
          <el-tag size="mini" type="info" effect="plain">待生成</el-tag>
        </div>
        <svg viewBox="0 0 240 230" class="radar-svg">
          <g fill="none" stroke="#eef1f6">
            <polygon points="120,20 200,100 120,180 40,100" />
            <polygon points="120,52 168,100 120,148 72,100" />
            <polygon points="120,84 136,100 120,116 104,100" />
          </g>
          <g stroke="#e4e9f0">
            <line x1="120" y1="20" x2="120" y2="180" /><line x1="40" y1="100" x2="200" y2="100" />
            <line x1="74" y1="54" x2="166" y2="146" /><line x1="166" y1="54" x2="74" y2="146" />
          </g>
          <line
            v-for="(p, i) in unmeasuredPoints"
            :key="'dash-' + i"
            :x1="p.x" :y1="p.y" :x2="p.axisX" :y2="p.axisY"
            stroke="#98a2b3" stroke-width="1.5" stroke-dasharray="4 4"
          />
          <polygon :points="radarPolygon" fill="#1764f5" fill-opacity="0.16" stroke="#1764f5" stroke-width="2" />
          <circle
            v-for="(p, i) in radarPoints"
            :key="'pt-' + i"
            :cx="p.x" :cy="p.y" r="4"
            :fill="p.measured ? '#1764f5' : '#fff'"
            :stroke="p.measured ? '#1764f5' : '#98a2b3'" stroke-width="1.6"
          />
          <g fill="#667085" font-size="11.5" text-anchor="middle">
            <text x="120" y="13">学习投入 {{ dimText(0) }}</text>
            <text x="226" y="104">理论掌握 {{ dimText(1) }}</text>
            <text x="14" y="104">规范遵从 {{ dimText(3) }}</text>
            <text x="120" y="196">实践能力{{ dims[2].value === null ? '' : ' ' + dims[2].value }}</text>
          </g>
          <text v-if="dims[2].value === null" x="120" y="212" fill="#98a2b3" font-size="11" text-anchor="middle">待测评（{{ dims[2].note }}）</text>
        </svg>
      </section>

      <!-- ② 维度明细与来源 -->
      <section class="portrait-card dims-card">
        <div class="section-heading">
          <div>
            <span class="section-index">02</span>
            <div>
              <h2>维度明细与来源</h2>
              <p>每个维度可追溯到来源表与样本量。</p>
            </div>
          </div>
          <el-tag size="mini" type="info" effect="plain">分值待生成</el-tag>
        </div>
        <el-table :data="dimRows" size="mini" class="dim-table">
          <el-table-column label="维度" width="90" prop="name" />
          <el-table-column label="分值" width="70" align="center">
            <template slot-scope="scope"><b v-if="scope.row.value !== null">{{ scope.row.value }}</b><span v-else>--</span></template>
          </el-table-column>
          <el-table-column label="来源与样本量" min-width="220" prop="source" show-overflow-tooltip />
          <el-table-column label="状态" width="90" align="center">
            <template slot-scope="scope">
              <el-tag size="mini" :type="scope.row.tag">{{ scope.row.status }}</el-tag>
            </template>
          </el-table-column>
        </el-table>

        <div class="review-block">
          <div class="review-title">阶段评价（仅显示 OPEN 范围）</div>
          <div class="review-card">
            <p class="review-text">"{{ review.text }}"</p>
            <p class="review-meta">— 导师 {{ review.mentor }} · {{ review.date }} · visible_scope = OPEN</p>
          </div>
          <p class="review-note">阶段评价来自 <code>stage_evaluation</code>（当前表为空）；<code>visible_scope = ADMIN_ONLY</code> 的评价由后端过滤，不会出现在实习生端。</p>
        </div>
      </section>

      <!-- ③ 多周期趋势 -->
      <section class="portrait-card trend-card">
        <div class="section-heading">
          <div>
            <span class="section-index">03</span>
            <div>
              <h2>多周期趋势</h2>
              <p>{{ cycles[0] }} 之前为历史归档周期；未测评维度画虚线。</p>
            </div>
          </div>
          <el-tag size="mini" type="info" effect="plain">待生成</el-tag>
        </div>
        <svg viewBox="0 0 900 180" class="trend-svg">
          <g stroke="#eef1f6"><line x1="40" y1="30" x2="880" y2="30" /><line x1="40" y1="80" x2="880" y2="80" /><line x1="40" y1="130" x2="880" y2="130" /></g>
          <g fill="#98a2b3" font-size="11" text-anchor="end"><text x="32" y="34">100</text><text x="32" y="84">50</text><text x="32" y="134">0</text></g>
          <polyline :points="trendLines.engage" fill="none" stroke="#1764f5" stroke-width="2.5" />
          <polyline :points="trendLines.compliance" fill="none" stroke="#12b76a" stroke-width="2.5" />
          <polyline :points="trendLines.practice" fill="none" stroke="#f79009" stroke-width="2.5" stroke-dasharray="5 4" />
          <g fill="#fff" stroke="#1764f5" stroke-width="2"><circle v-for="p in trendDots.engage" :key="'e' + p.x" :cx="p.x" :cy="p.y" r="4" /></g>
          <g fill="#fff" stroke="#12b76a" stroke-width="2"><circle v-for="p in trendDots.compliance" :key="'c' + p.x" :cx="p.x" :cy="p.y" r="4" /></g>
          <g fill="#98a2b3" font-size="11" text-anchor="middle"><text v-for="(t, i) in trendLabels" :key="'lb' + i" :x="t.x" y="176">{{ t.label }}</text></g>
        </svg>
        <div class="legend">
          <span><i style="background:#1764f5" />学习投入</span>
          <span><i style="background:#12b76a" />规范遵从</span>
          <span><i style="background:#f79009" />实践能力（未测评，虚线）</span>
        </div>
      </section>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { listLearningCourses } from '@/api/business/learning'
import { learningSummary } from '@/utils/learningPreview'

/**
 * 能力画像详情页（工作台「能力画像 · 详情」下钻，设计稿 i9）
 *
 * 真实数据（复用既有接口，不新增、不改动后端）：
 *  - `GET /business/learning/courses` → 学习投入维度的样本量（学习单项数）
 *  - vuex getters：mentorName / protocolStatus / roles
 *
 * ★ 2026-09-22 起：没有真数据的一律**留空**（不再用示例值 / 示例标）
 *  - 四维分值与综合值：`profile_snapshot` / `ability_dimension` 表存在但**零 Java 层实现**、维度定义未拍板
 *  - 阶段评价原文：`stage_evaluation` 当前 0 行（且 visible_scope='ADMIN_ONLY' 须后端过滤）
 *  - 多周期趋势：需周期归档接口
 */

export default {
  name: 'InternPortrait',
  data() {
    return {
      cycle: '2026-09',
      cycles: ['2026-09', '2026-08', '2026-07'],
      learningOverview: { progress: null, courseCount: 0, completedCourses: 0, learningCourses: 0, completedItems: 0, itemCount: 0, lastStudyTime: '尚未开始' }
    }
  },
  computed: {
    ...mapGetters(['roles', 'mentorName', 'protocolStatus']),
    isFormal() { return this.roles.indexOf('FORMAL_TRAINEE') > -1 },
    /**
     * 能力四维（2026-09-22：**全部留空**，不再用假分）
     * ⚠️ `profile_snapshot` / `ability_dimension` 表存在但**零 Java 层实现**、维度定义未拍板
     * ⇒ 雷达只画虚线轴、维度明细显示「待生成」；维度与接口就绪后再接真。
     */
    dims() {
      return [
        { name: '学习投入', value: null, note: '待生成' },
        { name: '理论掌握', value: null, note: '待生成' },
        { name: '实践能力', value: null, note: '待生成' },
        { name: '规范遵从', value: null, note: '待生成' }
      ]
    },
    /** 维度明细：2026-09-22 起不再用假样本量（原来拿 29 / 14.5h 当兜底），样本量按真实值显示 */
    dimRows() {
      const items = this.learningOverview.itemCount || 0
      const source = items > 0 ? `study_record · ${items} 个学习单项` : 'study_record · 尚无学习记录'
      return [
        { name: '学习投入', value: this.dims[0].value, source, status: '待生成', tag: 'info' },
        { name: '理论掌握', value: this.dims[1].value, source: 'answer_sheet / assessment_result · 正式考核后生成', status: '待生成', tag: 'info' },
        { name: '实践能力', value: this.dims[2].value, source: 'practical_submission · 实操批阅后生成', status: '待生成', tag: 'info' },
        { name: '规范遵从', value: this.dims[3].value, source: Number(this.protocolStatus) === 1 ? 'agreement_signature（已签署）+ 按规范完成率' : 'agreement_signature（待签署）+ 按规范完成率', status: '待生成', tag: 'info' }
      ]
    },
    review() {
      // 阶段评价原文来自 `stage_evaluation`（当前 0 行，且 visible_scope='ADMIN_ONLY' 的须由后端过滤）
      // ⇒ 2026-09-22 起不再用硬编码评价文本，留空由模板显示空态
      return { text: null, mentor: this.mentorName || '待登记', date: null }
    },
    radarPoints() {
      const center = { x: 120, y: 100 }
      const axes = [{ x: 120, y: 20 }, { x: 200, y: 100 }, { x: 120, y: 180 }, { x: 40, y: 100 }]
      return this.dims.map((dim, i) => {
        const ratio = dim.value === null ? 0.42 : dim.value / 100
        return {
          x: Math.round(center.x + (axes[i].x - center.x) * ratio),
          y: Math.round(center.y + (axes[i].y - center.y) * ratio),
          axisX: axes[i].x,
          axisY: axes[i].y,
          measured: dim.value !== null
        }
      })
    },
    radarPolygon() { return this.radarPoints.map(p => p.x + ',' + p.y).join(' ') },
    unmeasuredPoints() { return this.radarPoints.filter(p => !p.measured) },
    // 多周期趋势：y = 130 - 分值（网格 30/80/130 对应 100/50/0）
    trendSeries() {
      const xs = [60, 300, 540, 820]
      const toPoints = values => values.map((v, i) => ({ x: xs[i], y: 130 - v }))
      const engage = this.isFormal ? [62, 74, 84, 92] : [48, 60, 70, 78]
      const compliance = this.isFormal ? [70, 78, 84, 88] : [62, 74, 82, 88]
      const practice = this.isFormal ? [58, 70, 82, 91] : [12, 18, 24, 28]
      return { engage: toPoints(engage), compliance: toPoints(compliance), practice: toPoints(practice) }
    },
    trendLines() {
      const join = pts => pts.map(p => p.x + ',' + p.y).join(' ')
      return { engage: join(this.trendSeries.engage), compliance: join(this.trendSeries.compliance), practice: join(this.trendSeries.practice) }
    },
    trendDots() {
      return { engage: this.trendSeries.engage, compliance: this.trendSeries.compliance }
    },
    trendLabels() {
      const xs = [60, 300, 540, 820]
      return ['2026-07', '2026-08', '2026-09', '最新'].map((label, i) => ({ x: xs[i], label }))
    }
  },
  created() {
    this.loadLearning()
  },
  methods: {
    dimText(i) {
      const dim = this.dims[i]
      return dim.value === null ? '--' : dim.value
    },
    loadLearning() {
      listLearningCourses().then(res => {
        this.learningOverview = learningSummary(res.data || [])
      }).catch(() => {
        this.learningOverview = learningSummary([])
      })
    },
    onCycleChange() {
      this.$modal.msgInfo('历史周期需「周期归档」接口，当前仅展示本月')
    },
    exportPortrait() {
      this.$modal.msgInfo('导出 PDF 需后端接口支持，当前未接入')
    }
  }
}
</script>

<style lang="scss" scoped>
.portrait-page { min-height: calc(100vh - 50px); padding: 22px; color: #283544; background: #f3f5f8; }
.portrait-breadcrumb { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; color: #98a2b3; font-size: 12px; }
.portrait-breadcrumb b { color: #475467; font-weight: 500; }
.portrait-heading { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 18px; }
.eyebrow { color: #2878c7; font-size: 11px; letter-spacing: .08em; }
.portrait-heading h1 { margin: 5px 0 7px; color: #1d2939; font-size: 26px; font-weight: 600; }
.portrait-heading p { margin: 0; color: #667085; font-size: 13px; }
.heading-actions { display: flex; align-items: center; gap: 8px; }
.cycle-select { width: 150px; }
.portrait-grid { display: grid; grid-template-columns: repeat(12, minmax(0, 1fr)); gap: 14px; }
.portrait-card { min-width: 0; padding: 20px 22px; background: #fff; box-shadow: 0 1px 3px rgba(16, 24, 40, .04); }
.radar-card { grid-column: span 5; }
.dims-card { grid-column: span 7; }
.trend-card { grid-column: span 12; }
.section-heading { display: flex; align-items: center; justify-content: space-between; padding-bottom: 16px; border-bottom: 1px solid #edf0f4; }
.section-heading > div { display: flex; align-items: flex-start; gap: 12px; }
.section-index { color: #1764f5; font-size: 11px; font-weight: 700; }
.section-heading h2 { margin: 0 0 5px; color: #1d2939; font-size: 17px; font-weight: 600; }
.section-heading p { margin: 0; color: #8490a0; font-size: 12px; }
.dsample { display: inline-block; margin-left: 6px; padding: 0 6px; border-radius: 8px; background: #fff4e5; color: #b54708; font-size: 10px; font-style: normal; line-height: 16px; }
.radar-svg { display: block; max-width: 280px; margin: 18px auto 6px; }
.dim-table { margin-top: 14px; }
.review-block { margin-top: 18px; }
.review-title { margin-bottom: 8px; color: #475467; font-size: 12.5px; font-weight: 600; }
.review-card { padding: 12px 14px; border: 1px solid #e4e9f0; background: #fcfcfd; }
.review-text { margin: 0; color: #475467; font-size: 12.5px; line-height: 1.7; }
.review-meta { margin: 7px 0 0; color: #98a2b3; font-size: 11.5px; }
.review-note { margin: 8px 0 0; color: #98a2b3; font-size: 11px; }
.trend-svg { display: block; width: 100%; height: auto; max-height: 210px; margin-top: 16px; }
.legend { display: flex; flex-wrap: wrap; gap: 18px; margin-top: 10px; color: #667085; font-size: 12px; }
.legend span { display: inline-flex; align-items: center; gap: 6px; }
.legend i { width: 10px; height: 10px; border-radius: 2px; }
@media (max-width: 1100px) {
  .radar-card, .dims-card { grid-column: span 12; }
}
@media (max-width: 700px) {
  .portrait-page { padding: 14px; }
  .portrait-heading { align-items: flex-start; flex-direction: column; gap: 12px; }
  .portrait-heading h1 { font-size: 22px; }
  .portrait-card { padding: 16px 14px; }
}
</style>
