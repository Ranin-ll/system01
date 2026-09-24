<template>
  <div class="s-page">
    <header class="s-head">
      <div>
        <span class="eyebrow">SUPER ADMIN · 培养运营</span>
        <h1>培养分析看板</h1>
        <p>
          实习生培养与学习考核情况的<strong>部门横向对比</strong>，各维度一张表打尽。
          <strong>人 / 学习 / 任务 / 考核（模拟 · 正式 · 知识点）全部为实时真数据</strong>；
          无样本一律显示「无样本 / 暂无」，<strong>不伪装 0%</strong>。
        </p>
      </div>
      <div class="s-head-actions">
        <span class="s-ro"><i class="el-icon-view" /> 全局只读</span>
        <!-- 2026-09-22：考核类已全部接真数据，页头不再需要「含示例数据」标 -->
        <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="loadAll">刷新</el-button>
      </div>
    </header>

    <!-- ★ 加载失败 ≠ 没有数据：两个状态两段文案，失败给原因 + 重试 -->
    <div v-if="error" class="s-callout warn">
      <i class="el-icon-warning-outline" />
      <div>
        <b>加载失败</b>
        <p style="margin:4px 0 8px">{{ error }}</p>
        <el-button size="mini" type="primary" @click="loadAll">重试</el-button>
      </div>
    </div>

    <template v-else>
      <!-- ==================== KPI 条（6，全部可点进对应页面） ==================== -->
      <div v-loading="loading" class="s-kpis">
        <div
          v-for="k in kpis"
          :key="k.key"
          class="s-kpi link"
          :title="'查看：' + k.toLabel"
          @click="go(k.path, k.query)"
        >
          <i class="jump el-icon-top-right" />
          <div class="lb"><i class="dot" :style="{ background: k.color }" />{{ k.label }}</div>
          <div class="vl">{{ k.value }}<small v-if="k.unit && k.value !== '--'">{{ k.unit }}</small></div>
          <div class="ft" :class="{ warn: k.warn }">{{ k.foot }}</div>
        </div>
      </div>

      <!-- ==================== 部门对比矩阵 ==================== -->
      <div class="s-grid">
        <section id="sec-matrix" class="s-card s-c12" :class="{ 'focus-flash': focusKey === 'sec-matrix' }">
          <div class="s-card-h">
            <div class="tt">
              <span class="s-idx">阵</span>
              <h3>部门对比矩阵</h3>
            </div>
            <span class="hint">行 = 部门 · 点任一行 → 部门详情 · 默认按综合健康度降序</span>
          </div>

          <div v-if="loading && !matrix.length" class="s-empty"><i class="el-icon-loading" /><span>加载中…</span></div>
          <div v-else-if="!matrix.length" class="s-empty"><i class="el-icon-data-analysis" /><span>暂无在培实习生</span></div>
          <table v-else class="s-tbl matrix">
            <thead>
              <tr>
                <th style="min-width:104px">部门</th>
                <th class="ctr" style="width:60px">在培</th>
                <th style="min-width:150px">学习进度均值</th>
                <th class="ctr" style="width:64px">达标项</th>
                <th class="ctr" style="width:72px">模拟人次</th>
                <th style="min-width:126px">模拟均分</th>
                <th style="min-width:140px">知识点正确率</th>
                <th style="width:126px">任务 应/已/逾</th>
                <th style="min-width:126px">综合健康度</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in matrix" :key="r.deptId" class="row-link" @click="goDept(r.deptId)">
                <td class="nm">{{ r.deptName }} <i class="el-icon-arrow-right drill" /></td>
                <td class="ctr"><b>{{ num(r.internCount) }}</b></td>

                <!-- 学习进度：无记录 → 「无记录」，不写 0% -->
                <td>
                  <template v-if="r.learnAvgProgress !== null">
                    <div class="mb">
                      <span class="t"><i :style="{ width: clamp(r.learnAvgProgress) + '%' }" /></span>
                      <b>{{ r.learnAvgProgress }}%</b>
                    </div>
                    <!-- ★ 部门里只有 1 人也有学习记录时不能当"部门水平"看，必须标 N -->
                    <span class="n1">N={{ num(r.learnPersons) }} 人</span>
                  </template>
                  <span v-else class="none">无记录 <data-tag :none="true" label="无样本" /></span>
                </td>

                <td class="ctr">
                  {{ num(r.learnDone) }}/{{ num(r.learnTotal) }}
                </td>

                <td class="ctr">
                  <template v-if="r.practiceCount !== null">{{ num(r.practiceCount) }}</template>
                  <span v-else class="none">--</span>
                </td>

                <td>
                  <template v-if="r.practiceAvg !== null">
                    <div class="mb">
                      <span class="t"><i class="p" :style="{ width: clamp(r.practiceAvg * 10) + '%' }" /></span>
                      <b>{{ r.practiceAvg }}</b>
                    </div>
                    <span class="n1"><data-tag :mock="r.mock.practiceAvg" /></span>
                  </template>
                  <span v-else class="none">暂无数据</span>
                </td>

                <td>
                  <template v-if="r.knowledgeRate !== null">
                    <div class="mb">
                      <span class="t"><i :class="toneOf(r.knowledgeRate)" :style="{ width: clamp(r.knowledgeRate) + '%' }" /></span>
                      <b>{{ r.knowledgeRate }}%</b>
                    </div>
                    <span class="n1">{{ num(r.knowledgeCorrect) }}/{{ num(r.knowledgeItems) }} 题次 <data-tag :mock="r.mock.knowledgeRate" /></span>
                  </template>
                  <span v-else class="none">无样本 <data-tag :none="true" /></span>
                </td>

                <td>
                  <span>{{ num(r.taskTotal) }} / {{ num(r.taskDone) }} / </span>
                  <span :class="{ bad: num(r.taskOverdue) > 0 }">{{ num(r.taskOverdue) }}</span>
                </td>

                <td>
                  <template v-if="r.health !== null">
                    <div class="mb">
                      <span class="t"><i :class="toneOf(r.health)" :style="{ width: clamp(r.health) + '%' }" /></span>
                      <b>{{ r.health }}</b>
                    </div>
                    <span class="n1">有效维度 {{ r.healthDims }}/4</span>
                  </template>
                  <span v-else class="none">数据不足 <data-tag :none="true" /></span>
                </td>
              </tr>
            </tbody>
          </table>

          
        </section>
      </div>

      <!-- ==================== 学习进度对比 + 培养状态进度 ==================== -->
      <div class="s-grid">
        <section id="sec-learn" class="s-card s-c5" :class="{ 'focus-flash': focusKey === 'sec-learn' }">
          <div class="s-card-h">
            <div class="tt"><span class="s-idx g">1</span><h3>部门学习进度对比</h3></div>
            <span class="hint">全公司均值 {{ ov.learnAvgProgress === null ? '--' : ov.learnAvgProgress + '%' }}</span>
          </div>
          <div
            v-for="r in matrix"
            :key="r.deptId"
            class="s-hbar link"
            :class="{ low: r.learnAvgProgress !== null && r.learnAvgProgress < num(ov.learnThreshold) }"
            :title="'查看 ' + r.deptName + ' 部门详情'"
            @click="goDept(r.deptId)"
          >
            <span class="nm" :title="r.deptName">{{ r.deptName }}</span>
            <span v-if="r.learnAvgProgress !== null" class="track"><i :style="{ width: clamp(r.learnAvgProgress) + '%' }" /></span>
            <span v-else class="track empty" />
            <span class="pc">{{ r.learnAvgProgress !== null ? r.learnAvgProgress + '%' : '无记录' }}</span>
          </div>
          <p class="s-note">
            橙色 = 低于达标门槛 {{ num(ov.learnThreshold) }}%。无学习记录的部门显示「无记录」，<b>不以 0% 占位</b>。
          </p>
        </section>

        <section id="sec-stage" class="s-card s-c7" :class="{ 'focus-flash': focusKey === 'sec-stage' }">
          <div class="s-card-h">
            <div class="tt"><span class="s-idx p">2</span><h3>培养状态进度</h3></div>
            
          </div>

          <div
            v-for="s in stages"
            :key="s.stage"
            class="fstep link"
            :title="'查看「' + s.label + '」阶段的人员明细'"
            @click="goPeople"
          >
            <span class="st">{{ s.label }} <i class="el-icon-arrow-right drill" /></span>
            <span class="fw">
              <span class="fb" :style="{ width: funnelWidth(s.cnt), background: stageColor(s.stage) }">{{ s.cnt }} 人</span>
            </span>
          </div>

          <div v-if="num(ov.statusConflict) > 0" class="s-callout warn">
            <i class="el-icon-warning-outline" />
            <div>
              <b>{{ num(ov.statusConflict) }} 人「状态待修正」</b>
              <p style="margin:4px 0 0">
                已持<strong>预备实习生角色</strong>，但 <code>user_status</code> 仍是 <code>WAIT_AUDIT</code>
                —— 角色已发、审核流程未闭环，会被同时算进「待审核」与「预备」两个阶段。
                这里按角色为主口径归一，并单独标出，<strong>不悄悄归类</strong>。
              </p>
            </div>
          </div>

          <div v-if="gateRows.length" class="s-steps" style="margin-top:12px">
            <div
              v-for="g in gateRows"
              :key="g.key"
              class="s-step link"
              :title="'查看人员明细核对「' + g.label + '」'"
              @click="goPeople"
            >
              <!-- ⚠️ 勾选态必须"全员满足才算过"：只看 pass 真假会把 0/17 显示成通过 -->
              <span class="mark" :class="gateCls(g)">{{ gateMark(g) }}</span>
              <div class="txt">
                <b>{{ g.label }} <data-tag :mock="g.mock" /></b>
                <span>{{ g.pass }}/{{ g.total }} 人满足 · {{ g.text }}</span>
              </div>
              <i class="jump el-icon-arrow-right" />
            </div>
          </div>

          
        </section>
      </div>

      <!-- ==================== 模拟考核 + 知识点热力（真数据） ==================== -->
      <div class="s-grid">
        <section id="sec-practice" class="s-card s-c5" :class="{ 'focus-flash': focusKey === 'sec-practice' }">
          <div class="s-card-h">
            <div class="tt"><span class="s-idx">3</span><h3>部门模拟考核均分</h3></div>
            <span class="hint">点任一行 → 部门详情</span>
          </div>
          <div
            v-for="r in matrix"
            :key="r.deptId"
            class="s-hbar link"
            :title="'查看 ' + r.deptName + ' 部门详情'"
            @click="goDept(r.deptId)"
          >
            <span class="nm" :title="r.deptName">{{ r.deptName }}</span>
            <span v-if="r.practiceAvg !== null" class="track"><i class="p" :style="{ width: clamp(r.practiceAvg * 10) + '%' }" /></span>
            <span v-else class="track empty" />
            <span class="pc">{{ r.practiceAvg !== null ? r.practiceAvg : '暂无' }}</span>
          </div>
          
        </section>

        <section id="sec-knowledge" class="s-card s-c7" :class="{ 'focus-flash': focusKey === 'sec-knowledge' }">
          <div class="s-card-h">
            <div class="tt"><span class="s-idx o">4</span><h3>知识点掌握热力（部门 × 知识点）</h3></div>
            <span class="hint">点部门名或格子 → 该部门详情</span>
          </div>

          <div v-if="!heatRows.length" class="s-empty"><i class="el-icon-data-analysis" /><span>暂无逐题明细</span></div>
          <div v-for="h in heatRows" :key="h.deptId" class="heat-row">
            <span
              class="heat-dept link"
              :title="'查看 ' + h.deptName + ' 部门详情'"
              @click="goDept(h.deptId)"
            >{{ h.deptName }}</span>
            <span class="heat-cells">
              <span
                v-for="c in h.cells"
                :key="c.point"
                class="heat-cell link"
                :class="heatClass(c)"
                :title="c.point + '：' + c.correct + '/' + c.items + ' 题次（点进 ' + h.deptName + ' 详情）'"
                @click="goDept(h.deptId)"
              >{{ c.point }}<b>{{ c.correct }}/{{ c.items }}</b></span>
            </span>
          </div>

          
        </section>
      </div>

      <!-- ==================== 异常预警 ==================== -->
      <div class="s-grid">
        <section id="sec-alert" class="s-card s-c12" :class="{ 'focus-flash': focusKey === 'sec-alert' }">
          <div class="s-card-h">
            <div class="tt"><span class="s-idx o">警</span><h3>异常预警</h3></div>
            
          </div>
          <div v-if="!alerts.length" class="s-empty"><i class="el-icon-circle-check" /><span>当前无预警</span></div>
          <div v-else class="s-steps">
            <div
              v-for="(a, i) in alerts"
              :key="a.key + i"
              class="s-step link"
              :title="a.link ? ('前往处理：' + a.link) : ''"
              @click="goLink(a.link)"
            >
              <span class="mark" :class="{ now: a.level === 'HIGH' }">{{ a.level === 'HIGH' ? '!' : '·' }}</span>
              <div class="txt">
                <b>{{ a.title }}</b>
                <span>{{ a.detail }}</span>
              </div>
              <span class="s-badge" :class="a.level === 'HIGH' ? 'red' : 'warn'">{{ a.level === 'HIGH' ? '高' : '中' }}</span>
              <i class="jump el-icon-arrow-right" />
            </div>
          </div>
          
        </section>
      </div>
    </template>
  </div>
</template>

<script>
/**
 * 超管「培养分析看板」L0（培养运营 → 培养分析看板）
 *
 * ★ 2026-09-22 起：**人 / 学习 / 任务 / 考核（模拟 · 正式 · 知识点）全部为真数据**
 *   （原先考核类由同目录 `_mock.js` 填充、理由是「等同事的题库/考核分支合并」—— 该前提已失效）
 * 分母为 0 的项显示「无样本 / 暂无 / --」，不伪装 0%
 *
 * ★ 一致性：KPI 与矩阵**同源**（后端 overview 与 dept-matrix 走同一个 buildMatrix()），
 *   所以两处数字必然一致，不会出现「统计与列表对不上」。
 */
import { getAnalysisOverview, getDeptMatrix, getStageProgress, getKnowledgeMatrix } from '@/api/business/analysis'
import DataTag from '@/components/DataTag'

export default {
  name: 'SuperOpsAnalysisOverview',
  components: { DataTag },
  data() {
    return {
      loading: false,
      error: '',
      ov: {},
      rawMatrix: [],
      knowledgeMatrix: [],
      stages: [],
      stageRows: [],
      /** ★ ?focus= 锚点：用**响应式**变量驱动高亮（手写 classList 会被 Vue 重渲染覆写） */
      focusKey: ''
    }
  },
  computed: {
    /** 矩阵 = 后端真数据（含考核类），按综合健康度排序 */
    matrix() {
      return this.rawMatrix
        .map(r => this.decorate(r))
        .sort((a, b) => (b.health === null ? -1 : b.health) - (a.health === null ? -1 : a.health))
    },

    /** 异常预警（后端已按 assessment_config 阈值判定，前端只渲染） */
    alerts() {
      return this.ov.alerts || []
    },

    /** 考核相关的全局 KPI：由后端真数据聚合（部门矩阵 + 逐人阶段行），与各卡同源 */
    examKpi() {
      let count = 0
      let scoreSum = 0
      let persons = 0
      let formalPublished = 0
      this.rawMatrix.forEach(r => {
        const c = Number(r.practiceCount)
        if (c > 0) {
          count += c
          scoreSum += Number(r.practiceAvg || 0) * c
          persons++
        }
        const fp = Number(r.formalPublished)
        if (fp > 0) formalPublished += fp
      })
      // 正式通过率 = 通过人数 / **有正式答卷的人数**（无答卷的人不参与，避免把「没考」算成「没通过」）
      const attended = this.stageRows.filter(r => r.formalPassed !== null && r.formalPassed !== undefined)
      const passed = attended.filter(r => Number(r.formalPassed) === 1).length
      return {
        practiceCount: count || null,
        practicePersons: persons,
        practiceAvg: count ? Math.round(scoreSum / count * 100) / 100 : null,
        formalPublished,
        formalPassed: passed,
        formalAttended: attended.length,
        // 分母为 0 → null（不伪装 0%）
        formalRate: attended.length ? Math.round(passed * 100 / attended.length) : null
      }
    },

    /**
     * KPI 条（6）—— 每张卡都带跳转目标，点进去就是对应详情页。
     * ★ 值全部取自上面同源的 ov / examKpi，不在模板里另算，避免两处不一致。
     */
    kpis() {
      const ov = this.ov
      const ek = this.examKpi
      const nil = v => v === null || v === undefined
      return [
        {
          key: 'intern', label: '在培实习生', color: '#1764f5',
          value: this.num(ov.internTotal), unit: '人',
          foot: '覆盖 ' + this.num(ov.deptCount) + ' 个部门 · 已转正 ' + this.num(ov.formalTotal) + ' 人',
          path: '/super/org/accounts', query: { tab: 'people' }, toLabel: '人员与账号 · 人员列表'
        },
        {
          key: 'learn', label: '学习达标率', color: '#12b76a',
          value: nil(ov.learnRate) ? '--' : ov.learnRate, unit: '%',
          foot: nil(ov.learnRate)
            ? '暂无学习记录'
            : this.num(ov.learnDone) + '/' + this.num(ov.learnTotal) + ' 条达标（门槛 ' + this.num(ov.learnThreshold) + '）· N=' + this.num(ov.learnPersons) + ' 人',
          path: '/super/ops/courses', toLabel: '课程与题库总览'
        },
        {
          key: 'practice', label: '模拟考核均分', color: '#7a5af8',
          value: ek.practiceAvg === null ? '--' : ek.practiceAvg, unit: '/10',
          foot: this.num(ek.practiceCount) + ' 人次 · N=' + ek.practicePersons + ' 人',
          warn: ek.practiceAvg !== null,
          path: '/super/ops/prep', toLabel: '模拟备考管理'
        },
        {
          key: 'formal', label: '正式通过率', color: '#f04438',
          value: ek.formalRate === null ? '--' : ek.formalRate, unit: '%',
          foot: '已发布 ' + this.num(ek.formalPublished) + ' 张 · 通过 ' + this.num(ek.formalPassed) + ' / 参加 ' + this.num(ek.formalAttended) + ' 人',
          hasFormal: ek.formalRate !== null,
          path: '/super/ops/exams', toLabel: '考核与成绩'
        },
        {
          key: 'task', label: '任务完成率', color: '#f79009',
          value: nil(ov.taskDoneRate) ? '--' : ov.taskDoneRate, unit: '%',
          foot: this.num(ov.taskDone) + '/' + this.num(ov.taskTotal) + ' 已交 · 逾期 ' + this.num(ov.taskOverdue),
          warn: this.num(ov.taskOverdue) > 0,
          path: '/super/todo', toLabel: '督办看板'
        },
        {
          key: 'block', label: '培养卡点', color: '#667085',
          value: this.num(ov.statusConflict), unit: '人',
          foot: '角色与状态不一致 · 未分导师 ' + this.num(ov.mentorMissing) + ' · 未签协议 ' + this.num(ov.protocolUnsigned),
          warn: this.num(ov.statusConflict) > 0,
          path: '/super/org/accounts', query: { tab: 'people' }, toLabel: '人员与账号 · 人员列表'
        }
      ]
    },

    /** 知识点热力（真数据：部门 × 章节矩阵，来自 /knowledge-matrix） */
    heatRows() {
      const byDept = {}
      ;(this.knowledgeMatrix || []).forEach(c => {
        const id = c.deptId
        if (!byDept[id]) byDept[id] = []
        byDept[id].push(c)
      })
      // 列顺序：以「题次最多」的部门的章节顺序为准，保证列稳定、且只列有样本的章节
      const deptLists = Object.keys(byDept).map(id => byDept[id])
      deptLists.sort((a, b) =>
        b.reduce((s, x) => s + Number(x.items || 0), 0) - a.reduce((s, x) => s + Number(x.items || 0), 0))
      const order = (deptLists[0] || []).filter(c => Number(c.items) >= 2).map(c => c.point)

      const rows = []
      this.matrix.forEach(r => {
        const list = byDept[r.deptId] || []
        const cells = order.map(p => {
          const hit = list.find(x => x.point === p)
          // 题次 < 2 视为样本不足（不参与热力着色）
          return hit && Number(hit.items) >= 2
            ? { point: p, items: Number(hit.items), correct: Number(hit.correct) }
            : { point: p, items: 0, correct: 0, thin: true }
        })
        if (cells.some(c => !c.thin)) {
          rows.push({ deptId: r.deptId, deptName: r.deptName, cells })
        }
      })
      return rows
    },

    /** 转正 gate 逐条勾选（5 条**全部为真数据**；「部门终审」因 promotion_application 无表 ⇒ 恒 0 人） */
    gateRows() {
      const total = this.stageRows.length
      if (!total) return []
      const th = Number(this.ov.learnThreshold || 70)
      let learnFail = 0
      let taskFail = 0
      let protoFail = 0
      let formalFail = 0
      this.stageRows.forEach(r => {
        const rate = r.learnTotal > 0 ? r.learnDone * 100 / r.learnTotal : 0
        if (rate < th) learnFail++
        if (Number(r.taskOverdue || 0) > 0) taskFail++
        if (!Number(r.protocolSigned || 0)) protoFail++
        const fp = r.formalPassed
        // 真值：1 通过 / 0 未通过 / null 未参加 ⇒ 只有「已通过」才算达标
        if (fp !== 1) formalFail++
      })
      return [
        { key: 'learn', label: '学习达标（≥ ' + th + '）', pass: total - learnFail, total, mock: false,
          text: learnFail + ' 人未达标' },
        { key: 'formal', label: '正式考试通过', pass: total - formalFail, total, mock: false,
          text: formalFail + ' 人未通过或未参加正式考核' },
        { key: 'task', label: '任务无逾期', pass: total - taskFail, total, mock: false,
          text: taskFail + ' 人有逾期任务' },
        { key: 'protocol', label: '协议已签', pass: total - protoFail, total, mock: false,
          text: protoFail + ' 人未签' },
        { key: 'approve', label: '部门终审通过', pass: 0, total, mock: false,
          text: 'promotion_application 尚无记录 —— 无人提交转正申请' }
      ]
    }
  },
  created() {
    this.loadAll()
  },
  watch: {
    /** 带 ?focus= 从预警点进来时，数据到位后再锚点定位 */
    '$route.query.focus'() {
      this.applyFocus()
    }
  },
  methods: {
    num(v) {
      return (v === null || v === undefined) ? 0 : v
    },
    clamp(v) {
      const n = Number(v)
      if (isNaN(n)) return 0
      return Math.max(0, Math.min(100, n))
    },
    /** 比率高→绿、中→蓝、低→红（本项目惯例：通过/优秀用绿，未达标用红） */
    toneOf(v) {
      const n = Number(v)
      if (n >= 80) return 'g'
      if (n >= 50) return ''
      return 'r'
    },
    /** 转正 gate 的勾选态：全员满足才算过（0/17 绝不能显示成通过） */
    gateCls(g) {
      if (g.pass >= g.total && g.total > 0) return 'done'
      if (g.pass > 0) return 'now'
      return ''
    },
    gateMark(g) {
      if (g.pass >= g.total && g.total > 0) return '✓'
      if (g.pass > 0) return '~'
      return '!'
    },
    heatClass(c) {
      if (c.thin || !c.items) return 'hn'
      const r = c.correct / c.items
      if (r >= 0.8) return 'h3'
      if (r >= 0.5) return 'h2'
      if (r > 0) return 'h1'
      return 'h0'
    },
    stageColor(stage) {
      return { WAIT_AUDIT: '#98a2b3', PRE_TRAINEE: '#1764f5', PENDING_PROMOTE: '#f79009', FORMAL_TRAINEE: '#12b76a' }[stage] || '#667085'
    },
    /**
     * 统一跳转：目标路由不存在则**不动** —— 不给死链（本项目铁律）。
     */
    go(path, query) {
      if (!path) return
      if (!this.$router.resolve(path).route.matched.length) return
      this.$router.push(query ? { path, query } : path).catch(() => {})
    },
    /** 后端的预警 link 可能是「/x/y」或「/x/y?focus=zzz」，这里拆开跳 */
    goLink(link) {
      if (!link) return
      const i = link.indexOf('?')
      if (i < 0) return this.go(link)
      const query = {}
      link.slice(i + 1).split('&').filter(Boolean).forEach(kv => {
        const parts = kv.split('=')
        query[decodeURIComponent(parts[0])] = decodeURIComponent(parts[1] || '')
      })
      this.go(link.slice(0, i), query)
    },
    /** 人员类下钻统一落「人员与账号 · 人员列表」（该页只支持 ?tab=people|register） */
    goPeople() {
      this.go('/super/org/accounts', { tab: 'people' })
    },
    /** 下钻到部门详情（L1） */
    goDept(deptId) {
      this.go('/super/ops/analysis/dept/' + deptId)
    },
    /**
     * ★ 实现后端约定的 `?focus=` 锚点：从预警点进来后滚到对应卡片并短暂高亮。
     * 后端当前取值 task / stage（其余按 sec-&lt;key&gt; 兜底）。
     */
    focusIdOf(key) {
      return ({ task: 'sec-stage', stage: 'sec-stage' })[key] || ('sec-' + key)
    },
    applyFocus() {
      const key = this.$route.query.focus
      if (!key) return
      const id = this.focusIdOf(key)
      this.focusKey = id
      this.$nextTick(() => {
        const el = document.getElementById(id)
        if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
      })
      window.clearTimeout(this._focusTimer)
      this._focusTimer = window.setTimeout(() => { this.focusKey = '' }, 3000)
    },
    funnelWidth(cnt) {
      const max = Math.max.apply(null, this.stages.map(s => Number(s.cnt) || 0).concat([1]))
      return Math.max((Number(cnt) || 0) / max * 100, 6) + '%'
    },

    /**
     * 把后端一行真数据合成展示行（保留 `{ value, mock }` 形状以兼容模板）。
     * ★ 考核类字段自 2026-09-22 起为后端真值，直接取 r.xxx —— **已无任何示例兜底**。
     */
    decorate(r) {
      // 2026-09-22：考核类已由后端提供真值，不再 fallback 示例数据（保持 {value, mock} 形状）
      const practiceAvg = { value: r.practiceAvg, mock: false }
      const knowledgeItems = { value: r.knowledgeItems, mock: false }
      const knowledgeCorrect = { value: r.knowledgeCorrect, mock: false }
      const practiceCount = { value: r.practiceCount, mock: false }
      const formalPublished = { value: r.formalPublished, mock: false }

      const knowledgeRate = (knowledgeCorrect.value && knowledgeItems.value)
        ? Math.round(knowledgeCorrect.value * 100 / knowledgeItems.value)
        : null

      // ★ 学习达标率：分子分母都是「记录数」；分母 0 → 不参与健康度（缺项，不做 0）
      const learnRate = Number(r.learnTotal) > 0
        ? Number(r.learnDone) * 100 / Number(r.learnTotal)
        : null
      const taskRate = Number(r.taskTotal) > 0
        ? Number(r.taskDone) * 100 / Number(r.taskTotal)
        : null
      const practiceNorm = practiceAvg.value !== null ? Number(practiceAvg.value) * 10 : null

      // 权重：学习 30 / 知识点 30 / 任务 20 / 模拟 20；★ 缺项把权重按剩余项重新归一
      const parts = [
        { w: 0.30, v: learnRate },
        { w: 0.30, v: knowledgeRate },
        { w: 0.20, v: taskRate },
        { w: 0.20, v: practiceNorm }
      ].filter(p => p.v !== null && p.v !== undefined)

      let health = null
      let healthDims = 0
      if (parts.length) {
        const wSum = parts.reduce((s, p) => s + p.w, 0)
        health = Math.round(parts.reduce((s, p) => s + p.v * p.w, 0) / wSum)
        healthDims = parts.length
      }

      return Object.assign({}, r, {
        practiceCount: practiceCount.value,
        practiceAvg: practiceAvg.value,
        formalPublished: formalPublished.value,
        knowledgeItems: knowledgeItems.value,
        knowledgeCorrect: knowledgeCorrect.value,
        knowledgeRate,
        health,
        healthDims,
        mock: {
          practiceCount: practiceCount.mock,
          practiceAvg: practiceAvg.mock,
          formalPublished: formalPublished.mock,
          knowledgeItems: knowledgeItems.mock,
          knowledgeCorrect: knowledgeCorrect.mock,
          knowledgeRate: knowledgeRate !== null && (knowledgeCorrect.mock || knowledgeItems.mock)
        }
      })
    },

    loadAll() {
      this.loading = true
      this.error = ''
      Promise.all([
        getAnalysisOverview(),
        getDeptMatrix(),
        getStageProgress(),
        getKnowledgeMatrix()
      ]).then(([ovRes, mxRes, stRes, knRes]) => {
        this.ov = ovRes.data || {}
        this.rawMatrix = mxRes.data || []
        const st = stRes.data || {}
        this.stages = st.stages || []
        this.stageRows = st.rows || []
        this.knowledgeMatrix = knRes.data || []
      }).catch(err => {
        // ★ 绝不把失败吞成空数组 —— 那会让「403/500」看起来像「暂无数据」
        this.error = (err && err.message) ? err.message
          : '接口请求失败（可能是权限不足或后端未启动），请稍后重试'
        this.rawMatrix = []
        this.knowledgeMatrix = []
        this.stages = []
        this.stageRows = []
      }).finally(() => {
        this.loading = false
        this.applyFocus()
      })
    }
  }
}
</script>

<style lang="scss" scoped>
@import '~@/assets/styles/super-module.scss';

/* ⚠️ 本文件被 @import 进 scoped 样式，绝不能用 :root —— 会被编译成 :root[data-v-x] 永不匹配 */

/* 矩阵表内的迷你条 */
.matrix {
  th.ctr, td.ctr { text-align: center; }
  td { vertical-align: middle; }

  tbody tr.row-link { cursor: pointer; }
  tbody tr.row-link:hover { background: #f7fbff; }
  .drill { color: $blue; font-size: 11px; opacity: 0; }
  tbody tr.row-link:hover .drill { opacity: 1; }
}
.mb {
  display: flex;
  align-items: center;
  gap: 7px;

  .t {
    height: 7px;
    flex: 1;
    min-width: 42px;
    overflow: hidden;
    background: #edf1f5;
    border-radius: 4px;

    i {
      display: block;
      height: 100%;
      background: $blue;
      border-radius: 4px;

      &.g { background: $green; }
      &.p { background: $purple; }
      &.r { background: $red; }
    }
  }

  b {
    min-width: 44px;
    color: $ink-2;
    font-size: 12px;
    text-align: right;
    font-variant-numeric: tabular-nums;
  }
}
.n1 {
  display: block;
  margin-top: 2px;
  color: $ink-4;
  font-size: 10.5px;
}
.none { color: $ink-4; font-size: 12px; }
.bad { color: #b42318; font-weight: 600; }

/* 学习进度条的「无记录」占位：斜纹，一眼区别于 0% */
.s-hbar .track.empty {
  background: repeating-linear-gradient(45deg, #f2f4f7, #f2f4f7 5px, #e8ebf1 5px, #e8ebf1 10px);
}
.s-hbar .track i.p { background: $purple; }

/* 漏斗 */
.fstep {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 7px;

  .st { width: 74px; flex: none; color: $ink-3; font-size: 12px; }
  .fw { flex: 1; }

  .fb {
    display: flex;
    height: 26px;
    align-items: center;
    padding: 0 10px;
    color: #fff;
    font-size: 12px;
    font-weight: 600;
    border-radius: 6px;
    transition: width .3s;
  }
}

/* 知识点热力 */
.heat-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 8px;
}
.heat-dept {
  width: 74px;
  flex: none;
  padding-top: 5px;
  color: $ink-3;
  font-size: 12px;
}
.heat-cells { display: flex; flex: 1; flex-wrap: wrap; gap: 6px; }
.heat-cell {
  display: inline-flex;
  min-width: 78px;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  padding: 4px 8px;
  font-size: 11px;
  border-radius: 5px;

  b { font-variant-numeric: tabular-nums; }
}
.h0 { color: #b42318; background: $red-soft; }
.h1 { color: $orange-ink; background: $orange-soft; }
.h2 { color: $blue; background: $blue-soft; }
.h3 { color: #1a7a58; background: $green-soft; }
/* 注意：不要用 #98a2b3 配浅灰底（对比度太低，看着像空白） */
.hn { color: #475467; background: #f2f4f7; }

/* ==================== 2026-09-22：全卡片可点（统一 affordance） ====================
   ★ 只加视觉与鼠标态，不改任何数据结构；跳转一律走 go() / goDept() / goPeople() / goLink()，
     目标路由不存在时不动 —— 不给死链（项目铁律）。 */
.link { position: relative; cursor: pointer; transition: background .15s, box-shadow .15s, border-color .15s; }
.link:hover { background: #f7fbff; }

/* KPI 卡：hover 抬一层 + 右上角出现跳转箭头 */
.s-kpi.link { position: relative; }
.s-kpi.link:hover { border-color: #cfe0fb; box-shadow: 0 2px 10px rgba(23, 100, 245, .12); }
.s-kpi .jump,
.s-step .jump {
  position: absolute;
  top: 12px;
  right: 12px;
  color: #98a2b3;
  font-size: 13px;
  opacity: 0;
  transition: opacity .15s, color .15s;
}
.s-kpi.link:hover .jump,
.s-step.link:hover .jump { color: $blue; opacity: 1; }

/* 横条行（学习进度 / 模拟均分）与漏斗阶段 */
.s-hbar.link:hover { background: #f7fbff; border-radius: 6px; }
.fstep.link:hover { background: #f7fbff; border-radius: 6px; }
.fstep.link .drill { color: $blue; font-size: 11px; opacity: 0; }
.fstep.link:hover .drill { opacity: 1; }

/* 热力图：部门名与格子（文字类 hover 用下划线，不要铺底色） */
.heat-dept.link:hover { background: transparent; text-decoration: underline; text-underline-offset: 2px; }
.heat-cell.link { transition: filter .15s; }
.heat-cell.link:hover { filter: brightness(1.05); text-decoration: underline; text-underline-offset: 2px; }

/* 预警行 / gate 行：标题变蓝，明确"可点进去" */
.s-step.link .txt b { transition: color .15s; }
.s-step.link:hover .txt b { color: $blue; }

/* ?focus= 锚点：滚过去后闪两下，明确「就是这里」 */
@keyframes focusFlash {
  0% { box-shadow: 0 0 0 0 rgba(23, 100, 245, .40); }
  100% { box-shadow: 0 0 0 14px rgba(23, 100, 245, 0); }
}
.focus-flash { animation: focusFlash 1.2s ease-out 2; border-radius: 10px; }
</style>
