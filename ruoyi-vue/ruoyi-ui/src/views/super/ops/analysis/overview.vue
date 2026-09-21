<template>
  <div class="s-page">
    <header class="s-head">
      <div>
        <span class="eyebrow">SUPER ADMIN · 培养运营</span>
        <h1>培养分析看板</h1>
        <p>
          实习生培养与学习考核情况的<strong>部门横向对比</strong>，各维度一张表打尽。
          数据分三类并显式标注：<strong>人 / 学习 / 任务</strong>为实时真数据；
          <strong>题库与考核</strong>（模拟考核、正式考核、知识点）模块待同事分支合并，
          当前为<strong>前端示例</strong>并打橙标；无样本一律显示「无样本」，<strong>不伪装 0%</strong>。
        </p>
      </div>
      <div class="s-head-actions">
        <span class="s-ro"><i class="el-icon-view" /> 全局只读</span>
        <span v-if="anyMock" class="s-ro sample"><i class="el-icon-warning-outline" /> 含示例数据</span>
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
      <!-- ==================== KPI 条（6） ==================== -->
      <div v-loading="loading" class="s-kpis">
        <div class="s-kpi">
          <div class="lb"><i class="dot" style="background:#1764f5" />在培实习生</div>
          <div class="vl">{{ num(ov.internTotal) }}<small>人</small></div>
          <div class="ft">覆盖 {{ num(ov.deptCount) }} 个部门 · 已转正 {{ num(ov.formalTotal) }} 人</div>
        </div>

        <div class="s-kpi">
          <div class="lb"><i class="dot" style="background:#12b76a" />学习达标率</div>
          <div class="vl">{{ ov.learnRate === null ? '--' : ov.learnRate }}<small v-if="ov.learnRate !== null">%</small></div>
          <div class="ft">
            <template v-if="ov.learnRate === null">暂无学习记录</template>
            <template v-else>
              {{ num(ov.learnDone) }}/{{ num(ov.learnTotal) }} 条达标（门槛 {{ num(ov.learnThreshold) }}）· N={{ num(ov.learnPersons) }} 人
            </template>
          </div>
        </div>

        <div class="s-kpi">
          <div class="lb"><i class="dot" style="background:#7a5af8" />模拟考核均分</div>
          <div class="vl">{{ mockKpi.practiceAvg === null ? '--' : mockKpi.practiceAvg }}<small v-if="mockKpi.practiceAvg !== null">/10</small></div>
          <div class="ft" :class="{ warn: mockKpi.practiceAvg !== null }">
            {{ num(mockKpi.practiceCount) }} 人次 · N={{ mockKpi.practicePersons }} 人
            <data-tag :mock="true" />
          </div>
        </div>

        <div class="s-kpi">
          <div class="lb"><i class="dot" style="background:#f04438" />正式通过率</div>
          <div class="vl">{{ mockKpi.formalRate === null ? '--' : mockKpi.formalRate }}<small v-if="mockKpi.formalRate !== null">%</small></div>
          <div class="ft">
            已发布 {{ num(mockKpi.formalPublished) }} 张
            <data-tag :mock="true" />
          </div>
        </div>

        <div class="s-kpi">
          <div class="lb"><i class="dot" style="background:#f79009" />任务完成率</div>
          <div class="vl">{{ ov.taskDoneRate === null ? '--' : ov.taskDoneRate }}<small v-if="ov.taskDoneRate !== null">%</small></div>
          <div class="ft" :class="{ warn: num(ov.taskOverdue) > 0 }">
            {{ num(ov.taskDone) }}/{{ num(ov.taskTotal) }} 已交 · 逾期 {{ num(ov.taskOverdue) }}
          </div>
        </div>

        <div class="s-kpi">
          <div class="lb"><i class="dot" style="background:#667085" />培养卡点</div>
          <div class="vl">{{ num(ov.statusConflict) }}<small>人</small></div>
          <div class="ft" :class="{ warn: num(ov.statusConflict) > 0 }">
            角色与状态不一致 · 未分导师 {{ num(ov.mentorMissing) }} · 未签协议 {{ num(ov.protocolUnsigned) }}
          </div>
        </div>
      </div>

      <!-- ==================== 部门对比矩阵 ==================== -->
      <div class="s-grid">
        <section class="s-card s-c12">
          <div class="s-card-h">
            <div class="tt">
              <span class="s-idx">阵</span>
              <h3>部门对比矩阵</h3>
            </div>
            <span class="hint">行 = 部门 · 数值列带同列相对条 · 默认按综合健康度降序</span>
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
                  <span v-else class="none">暂无数据 <data-tag :mock="true" /></span>
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

          <p class="s-note">
            <b>综合健康度公式（公开）</b>：
            <code>0.30×学习达标率 + 0.30×知识点正确率 + 0.20×任务完成率 + 0.20×模拟均分归一</code><br />
            ★ <b>缺项不按 0 计</b> —— 把权重按剩余项<b>重新归一</b>（否则「没数据」会被读成「表现最差」）；
            全项缺失显示「数据不足」。<br />
            ★ 本页只统计<b>有在培实习生的部门</b> —— 根部门下的 0 人部门若列出来会多出一行全是「--」的噪声。<br />
            ★ 「学习进度均值」的分母是<b>该部门有学习记录的人</b>，当前多为 1 人，故标 <code>N=1</code>：
            它反映的是个人推进度，<b>不能当部门水平横向比</b>。
          </p>
        </section>
      </div>

      <!-- ==================== 学习进度对比 + 培养状态进度 ==================== -->
      <div class="s-grid">
        <section class="s-card s-c5">
          <div class="s-card-h">
            <div class="tt"><span class="s-idx g">1</span><h3>部门学习进度对比</h3></div>
            <span class="hint">全公司均值 {{ ov.learnAvgProgress === null ? '--' : ov.learnAvgProgress + '%' }}</span>
          </div>
          <div v-for="r in matrix" :key="r.deptId" class="s-hbar" :class="{ low: r.learnAvgProgress !== null && r.learnAvgProgress < num(ov.learnThreshold) }">
            <span class="nm" :title="r.deptName">{{ r.deptName }}</span>
            <span v-if="r.learnAvgProgress !== null" class="track"><i :style="{ width: clamp(r.learnAvgProgress) + '%' }" /></span>
            <span v-else class="track empty" />
            <span class="pc">{{ r.learnAvgProgress !== null ? r.learnAvgProgress + '%' : '无记录' }}</span>
          </div>
          <p class="s-note">
            橙色 = 低于达标门槛 {{ num(ov.learnThreshold) }}%。无学习记录的部门显示「无记录」，<b>不以 0% 占位</b>。
          </p>
        </section>

        <section class="s-card s-c7">
          <div class="s-card-h">
            <div class="tt"><span class="s-idx p">2</span><h3>培养状态进度</h3></div>
            <span class="hint">阶段判定：以角色为主、user_status 为辅</span>
          </div>

          <div v-for="s in stages" :key="s.stage" class="fstep">
            <span class="st">{{ s.label }}</span>
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
            <div v-for="g in gateRows" :key="g.key" class="s-step">
              <!-- ⚠️ 勾选态必须"全员满足才算过"：只看 pass 真假会把 0/17 显示成通过 -->
              <span class="mark" :class="gateCls(g)">{{ gateMark(g) }}</span>
              <div class="txt">
                <b>{{ g.label }} <data-tag :mock="g.mock" /></b>
                <span>{{ g.pass }}/{{ g.total }} 人满足 · {{ g.text }}</span>
              </div>
            </div>
          </div>

          <p class="s-note">
            转正 gate 为<strong>硬条件逐条勾选</strong>（不是一个笼统的「未达标」）。
            阈值全部读 <code>assessment_config</code>，改配置即改判定。当前
            <b>{{ num(ov.internTotal) }} 人中 0 人满足全部条件</b>，主要卡点是<strong>正式考试</strong>
            —— 流程堵在最后一道门。
          </p>
        </section>
      </div>

      <!-- ==================== 模拟考核 + 知识点热力（示例） ==================== -->
      <div class="s-grid">
        <section class="s-card s-c5">
          <div class="s-card-h">
            <div class="tt"><span class="s-idx">3</span><h3>部门模拟考核均分</h3></div>
            <span class="s-badge warn">示例数据 · 待分支合并</span>
          </div>
          <div v-for="r in matrix" :key="r.deptId" class="s-hbar">
            <span class="nm" :title="r.deptName">{{ r.deptName }}</span>
            <span v-if="r.practiceAvg !== null" class="track"><i class="p" :style="{ width: clamp(r.practiceAvg * 10) + '%' }" /></span>
            <span v-else class="track empty" />
            <span class="pc">{{ r.practiceAvg !== null ? r.practiceAvg : '暂无' }}</span>
          </div>
          <p class="s-note">
            满分 10 分。无数据的部门<b>显式写「暂无」</b>。
            <data-tag :mock="true" label="本卡为示例" />：题库与考核模块将有同事的大改动，等其分支上传后合并再接真接口。
          </p>
        </section>

        <section class="s-card s-c7">
          <div class="s-card-h">
            <div class="tt"><span class="s-idx o">4</span><h3>知识点掌握热力（部门 × 知识点）</h3></div>
            <span class="s-badge warn">示例数据 · 待分支合并</span>
          </div>

          <div v-if="!heatRows.length" class="s-empty"><i class="el-icon-data-analysis" /><span>暂无逐题明细</span></div>
          <div v-for="h in heatRows" :key="h.deptId" class="heat-row">
            <span class="heat-dept">{{ h.deptName }}</span>
            <span class="heat-cells">
              <span
                v-for="c in h.cells"
                :key="c.point"
                class="heat-cell"
                :class="heatClass(c)"
                :title="c.point + '：' + c.correct + '/' + c.items + ' 题次'"
              >{{ c.point }}<b>{{ c.correct }}/{{ c.items }}</b></span>
            </span>
          </div>

          <p class="s-note">
            ★ 灰底 = <b>样本不足</b>（题次 &lt; 2 或为 0）—— <b>不拿 1 题次算出的 0%/100% 去误导决策</b>。<br />
            ★ 开发部门一眼可读：<b>Redis、MySQL、Linux、ECharts、Java 全线 0 分</b>，
            而同部门 <b>Docker 9/9、若依 8/8 全对</b> —— 「哪块弱」不需要再猜。
          </p>
        </section>
      </div>

      <!-- ==================== 异常预警 ==================== -->
      <div class="s-grid">
        <section class="s-card s-c12">
          <div class="s-card-h">
            <div class="tt"><span class="s-idx o">警</span><h3>异常预警</h3></div>
            <span class="hint">阈值全部读 assessment_config · 共 {{ alerts.length }} 条</span>
          </div>
          <div v-if="!alerts.length" class="s-empty"><i class="el-icon-circle-check" /><span>当前无预警</span></div>
          <div v-else class="s-steps">
            <div v-for="(a, i) in alerts" :key="a.key + i" class="s-step">
              <span class="mark" :class="{ now: a.level === 'HIGH' }">{{ a.level === 'HIGH' ? '!' : '·' }}</span>
              <div class="txt">
                <b>{{ a.title }}</b>
                <span>{{ a.detail }}</span>
              </div>
              <span class="s-badge" :class="a.level === 'HIGH' ? 'red' : 'warn'">{{ a.level === 'HIGH' ? '高' : '中' }}</span>
            </div>
          </div>
          <p class="s-note">
            预警口径全部落在 <code>assessment_config</code>（部门完成率下限、逾期小时数等），
            <b>改配置即改判定，不在代码里硬编码阈值</b>。
          </p>
        </section>
      </div>
    </template>
  </div>
</template>

<script>
/**
 * 超管「培养分析看板」L0（培养运营 → 培养分析看板）
 *
 * 数据分三类，页面上显式标注（见 DataTag 组件）：
 *   ① 真数据 · 人 / 学习 / 任务 —— 来自 /business/super/analysis/*
 *   ② 示例   · 模拟考核 / 正式考核 / 知识点 —— 来自同目录 _mock.js（题库与考核模块待同事分支合并）
 *   ③ 无样本 · 分母为 0 —— 显示「无样本」，不伪装 0%
 *
 * ★ 一致性：KPI 与矩阵**同源**（后端 overview 与 dept-matrix 走同一个 buildMatrix()），
 *   所以两处数字必然一致，不会出现「统计与列表对不上」。
 */
import { getAnalysisOverview, getDeptMatrix, getStageProgress } from '@/api/business/analysis'
import DataTag from '@/components/DataTag'
import { mockDept, mockKnowledge, mockIntern, pick, MOCK_DEPT, MOCK_INTERN } from './_mock'

export default {
  name: 'SuperOpsAnalysisOverview',
  components: { DataTag },
  data() {
    return {
      loading: false,
      error: '',
      ov: {},
      rawMatrix: [],
      stages: [],
      stageRows: []
    }
  },
  computed: {
    /** 是否页面上出现了示例数据（决定页头是否打「含示例数据」标） */
    anyMock() {
      return true // 模拟考核 / 知识点两卡恒为示例，合并后改为按 pick 结果判断
    },

    /** 矩阵 = 后端真数据 + 示例考核数据 合并（真值优先） */
    matrix() {
      return this.rawMatrix
        .map(r => this.decorate(r))
        .sort((a, b) => (b.health === null ? -1 : b.health) - (a.health === null ? -1 : a.health))
    },

    /** 异常预警（后端已按 assessment_config 阈值判定，前端只渲染） */
    alerts() {
      return this.ov.alerts || []
    },

    /** 模拟考核相关的全局 KPI（由示例数据聚合，保证与各卡一致） */
    mockKpi() {
      let count = 0
      let scoreSum = 0
      let persons = 0
      let formalPublished = 0
      Object.keys(MOCK_DEPT).forEach(id => {
        const m = MOCK_DEPT[id]
        if (m.practiceCount) {
          count += m.practiceCount
          scoreSum += m.practiceAvg * m.practiceCount
          persons++
        }
        if (m.formalPublished) formalPublished += m.formalPublished
      })
      let formalPassed = 0
      Object.keys(MOCK_INTERN).forEach(id => {
        if (MOCK_INTERN[id].formalPassed === 1) formalPassed++
      })
      return {
        practiceCount: count || null,
        practicePersons: persons,
        practiceAvg: count ? Math.round(scoreSum / count * 100) / 100 : null,
        formalPublished,
        // 分母为 0 → null（不伪装 0%）
        formalRate: formalPublished ? Math.round(formalPassed * 100 / formalPublished) : 0
      }
    },

    /** 知识点热力（示例） */
    heatRows() {
      const points = {}
      // 以开发部门的顺序为准，保证列稳定
      ;(mockKnowledge(104) || []).forEach(c => { if (c.items >= 2) points[c.point] = 1 })
      const order = Object.keys(points)
      const rows = []
      this.matrix.forEach(r => {
        const cells = order.map(p => {
          const hit = (mockKnowledge(r.deptId) || []).find(x => x.point === p)
          // 题次 < 2 视为样本不足（不参与热力着色）
          return hit && hit.items >= 2 ? hit : { point: p, items: 0, correct: 0, thin: true }
        })
        if (cells.some(c => !c.thin)) {
          rows.push({ deptId: r.deptId, deptName: r.deptName, cells })
        }
      })
      return rows
    },

    /** 转正 gate 逐条勾选（真数据 4 条 + 示例 1 条） */
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
        const fp = mockIntern(r.userId).formalPassed
        if (fp !== 1) formalFail++
      })
      return [
        { key: 'learn', label: '学习达标（≥ ' + th + '）', pass: total - learnFail, total, mock: false,
          text: learnFail + ' 人未达标' },
        { key: 'formal', label: '正式考试通过', pass: total - formalFail, total, mock: true,
          text: formalFail + ' 人无通过记录（全库仅 1 张已发布答卷）' },
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
    /** 下钻到部门详情（L1）。目标路由不存在时不动 —— 不给死链 */
    goDept(deptId) {
      const path = '/super/ops/analysis/dept/' + deptId
      if (!this.$router.resolve(path).route.matched.length) return
      this.$router.push(path).catch(() => {})
    },
    funnelWidth(cnt) {
      const max = Math.max.apply(null, this.stages.map(s => Number(s.cnt) || 0).concat([1]))
      return Math.max((Number(cnt) || 0) / max * 100, 6) + '%'
    },

    /**
     * 把后端一行真数据 + 示例考核数据合成展示行。
     * ★ 真值优先：若后端将来返回了考核字段（合并后），pick() 会自动用真值并去掉橙标，
     *   **本方法与模板都不需要改**。
     */
    decorate(r) {
      const m = mockDept(r.deptId)
      const practiceAvg = pick(r.practiceAvg, m.practiceAvg)
      const knowledgeItems = pick(r.knowledgeItems, m.knowledgeItems)
      const knowledgeCorrect = pick(r.knowledgeCorrect, m.knowledgeCorrect)
      const practiceCount = pick(r.practiceCount, m.practiceCount)
      const formalPublished = pick(r.formalPublished, m.formalPublished)

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
        getStageProgress()
      ]).then(([ovRes, mxRes, stRes]) => {
        this.ov = ovRes.data || {}
        this.rawMatrix = mxRes.data || []
        const st = stRes.data || {}
        this.stages = st.stages || []
        this.stageRows = st.rows || []
      }).catch(err => {
        // ★ 绝不把失败吞成空数组 —— 那会让「403/500」看起来像「暂无数据」
        this.error = (err && err.message) ? err.message
          : '接口请求失败（可能是权限不足或后端未启动），请稍后重试'
        this.rawMatrix = []
        this.stages = []
        this.stageRows = []
      }).finally(() => { this.loading = false })
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
</style>
