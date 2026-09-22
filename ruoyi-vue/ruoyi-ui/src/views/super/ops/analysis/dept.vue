<template>
  <div class="s-page">
    <header class="s-head">
      <div>
        <span class="eyebrow">
          <router-link to="/super/ops/analysis" class="crumb-link">培养分析看板</router-link>
          <i class="el-icon-arrow-right" />
          部门详情
        </span>
        <h1>{{ dept.deptName || '部门详情' }}</h1>
        <p>
          本部门培养情况内部统计：岗位分布 · 课程完成率 · 学习与任务卡点 · 实习生明细（可下钻到个人）。
          <strong>本部门数取自 L0 同一份聚合</strong>，与看板不会漂移。
        </p>
      </div>
      <div class="s-head-actions">
        <span class="s-ro"><i class="el-icon-view" /> 全局只读</span>
        <span v-if="dept.globalInternCount" class="s-ro">
          本部门 {{ num(dept.internCount) }} 人 / 全局 {{ num(dept.globalInternCount) }} 人
        </span>
        <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="load">刷新</el-button>
      </div>
    </header>

    <div v-if="error" class="s-callout warn">
      <i class="el-icon-warning-outline" />
      <div>
        <b>加载失败</b>
        <p style="margin:4px 0 8px">{{ error }}</p>
        <el-button size="mini" type="primary" @click="load">重试</el-button>
      </div>
    </div>

    <template v-else>
      <!-- KPI 4（全部可点：落对应模块页 / 人员列表） -->
      <div v-loading="loading" class="s-kpis kpi4">
        <div class="s-kpi link" title="查看：人员与账号 · 人员列表" @click="goPeople">
          <i class="jump el-icon-top-right" />
          <div class="lb"><i class="dot" style="background:#1764f5" />在培实习生</div>
          <div class="vl">{{ num(dept.internCount) }}<small>人</small></div>
          <div class="ft">已转正 {{ num(dept.formalCount) }} 人</div>
        </div>
        <div class="s-kpi link" title="查看：课程与题库总览" @click="go('/super/ops/courses')">
          <i class="jump el-icon-top-right" />
          <div class="lb"><i class="dot" style="background:#12b76a" />学习达标率</div>
          <div class="vl">{{ dept.learnRate === null || dept.learnRate === undefined ? '--' : dept.learnRate }}<small v-if="dept.learnRate !== null && dept.learnRate !== undefined">%</small></div>
          <div class="ft">
            <template v-if="dept.learnTotal">达标 {{ num(dept.learnDone) }}/{{ num(dept.learnTotal) }} 条（门槛 {{ num(dept.learnThreshold) }}）· N={{ num(dept.learnPersons) }} 人</template>
            <template v-else>本部门暂无学习记录</template>
          </div>
        </div>
        <div class="s-kpi link" title="查看：督办看板" @click="go('/super/todo')">
          <i class="jump el-icon-top-right" />
          <div class="lb"><i class="dot" style="background:#f79009" />任务完成率</div>
          <div class="vl">{{ dept.taskDoneRate === null || dept.taskDoneRate === undefined ? '--' : dept.taskDoneRate }}<small v-if="dept.taskDoneRate !== null && dept.taskDoneRate !== undefined">%</small></div>
          <div class="ft" :class="{ warn: num(dept.taskOverdue) > 0 }">
            {{ num(dept.taskDone) }}/{{ num(dept.taskTotal) }} 已交 · 逾期 {{ num(dept.taskOverdue) }}
          </div>
        </div>
        <div class="s-kpi link" title="查看：人员与账号 · 人员列表" @click="goPeople">
          <i class="jump el-icon-top-right" />
          <div class="lb"><i class="dot" style="background:#667085" />培养卡点</div>
          <div class="vl">{{ num(blockers.statusConflict) }}<small>人</small></div>
          <div class="ft" :class="{ warn: num(blockers.statusConflict) > 0 }">
            状态待修正 · 未分导师 {{ num(blockers.mentorMissing) }} · 未签协议 {{ num(blockers.protocolUnsigned) }}
          </div>
        </div>
      </div>

      <!-- 本部门 vs 全局 -->
      <div class="s-grid">
        <section class="s-card s-c6">
          <div class="s-card-h">
            <div class="tt"><span class="s-idx">比</span><h3>本部门 vs 全局</h3></div>
            <span class="hint">全局值取自 L0 看板的同一份聚合</span>
          </div>
          <div class="vs-row">
            <span class="vs-nm">学习进度均值</span>
            <div class="vs-bar">
              <div class="vs-line">
                <span class="vs-lb">本部门</span>
                <span class="track"><i :style="{ width: clamp(dept.learnAvgProgress) + '%' }" /></span>
                <b>{{ dept.learnAvgProgress === null ? '无记录' : dept.learnAvgProgress + '%' }}</b>
              </div>
              <div class="vs-line">
                <span class="vs-lb">全局</span>
                <span class="track"><i class="gray" :style="{ width: clamp(dept.globalLearnAvgProgress) + '%' }" /></span>
                <b>{{ dept.globalLearnAvgProgress === null ? '--' : dept.globalLearnAvgProgress + '%' }}</b>
              </div>
            </div>
          </div>
          <div class="vs-row">
            <span class="vs-nm">任务完成率</span>
            <div class="vs-bar">
              <div class="vs-line">
                <span class="vs-lb">本部门</span>
                <span class="track"><i :style="{ width: clamp(dept.taskDoneRate) + '%' }" /></span>
                <b>{{ dept.taskDoneRate === null ? '--' : dept.taskDoneRate + '%' }}</b>
              </div>
              <div class="vs-line">
                <span class="vs-lb">全局</span>
                <span class="track"><i class="gray" :style="{ width: clamp(dept.globalTaskDoneRate) + '%' }" /></span>
                <b>{{ dept.globalTaskDoneRate === null ? '--' : dept.globalTaskDoneRate + '%' }}</b>
              </div>
            </div>
          </div>
        </section>

        <section class="s-card s-c6">
          <div class="s-card-h">
            <div class="tt"><span class="s-idx g">岗</span><h3>岗位分布</h3></div>
            <span class="hint">本部门在培实习生 · 点任一行 → 人员列表</span>
          </div>
          <div v-if="!(dept.positions || []).length" class="s-empty"><i class="el-icon-user" /><span>本部门暂无在培实习生</span></div>
          <div
            v-for="p in dept.positions"
            :key="String(p.positionId)"
            class="s-hbar link"
            :title="'查看「' + (p.positionName || '未设岗位') + '」的人员明细'"
            @click="goPeople"
          >
            <span class="nm" :title="p.positionName || '未设岗位'">{{ p.positionName || '未设岗位' }}</span>
            <span class="track"><i :style="{ width: posWidth(p.cnt) + '%' }" /></span>
            <span class="pc">{{ p.cnt }} 人</span>
          </div>
          <p class="s-note">
            该部门绑定的岗位由 <code>dept_position</code> 维护（1:1）；<b>未设岗位</b> 表示账号上没填岗位。
          </p>
        </section>
      </div>

      <!-- 课程完成率 + 薄弱知识点 -->
      <div class="s-grid">
        <section class="s-card s-c7">
          <div class="s-card-h">
            <div class="tt"><span class="s-idx">课</span><h3>本部门课程完成率</h3></div>
            <span class="hint">只列本部门岗位对应的课程 · 门槛 {{ num(dept.learnThreshold) }} · 点任一行 → 课程与题库总览</span>
          </div>
          <div v-if="!(dept.courses || []).length" class="s-empty"><i class="el-icon-reading" /><span>该岗位下暂无课程</span></div>
          <table v-else class="s-tbl">
            <thead>
              <tr><th>课程</th><th style="width:82px">状态</th><th class="ctr" style="width:64px">必学</th>
                <th class="ctr" style="width:76px">已学人数</th><th style="width:150px">平均进度</th></tr>
            </thead>
            <tbody>
              <tr
                v-for="c in dept.courses"
                :key="c.courseId"
                class="row-link"
                :title="'查看课程：' + c.courseName"
                @click="go('/super/ops/courses')"
              >
                <td class="nm">{{ c.courseName }} <i class="el-icon-arrow-right drill" /></td>
                <td><span class="s-badge" :class="courseCls(c.status)">{{ courseLabel(c.status) }}</span></td>
                <td class="ctr">{{ c.isRequired ? '是' : '否' }}</td>
                <td class="ctr">
                  {{ num(c.learnedCount) }}
                  <data-tag v-if="!num(c.learnedCount)" :none="true" label="无人学" />
                </td>
                <td>
                  <div v-if="c.avgProgress !== null && c.avgProgress !== undefined" class="mb">
                    <span class="t"><i :class="Number(c.avgProgress) >= num(dept.learnThreshold) ? 'g' : ''" :style="{ width: clamp(c.avgProgress) + '%' }" /></span>
                    <b>{{ c.avgProgress }}%</b>
                  </div>
                  <span v-else class="none">无记录 <data-tag :none="true" /></span>
                </td>
              </tr>
            </tbody>
          </table>
          <p class="s-note">
            ★ <b>无人学的课程也列出来</b>（显示「无人学」）—— 它们恰恰是课程覆盖的空洞，藏掉就看不出问题了。
            平均进度为 <code>NULL</code> 时显示「无记录」，<b>不以 0% 占位</b>。
          </p>
        </section>

        <section class="s-card s-c5">
          <div class="s-card-h">
            <div class="tt"><span class="s-idx o">弱</span><h3>本部门薄弱知识点 Top5</h3></div>
            <span class="hint">点任一行 → 题库管理（知识点归属）</span>
          </div>
          <div v-if="!weakPoints.length" class="s-empty"><i class="el-icon-data-analysis" /><span>本部门暂无逐题明细</span></div>
          <div
            v-for="k in weakPoints"
            :key="k.point"
            class="s-hbar low link"
            :title="'查看知识点「' + k.point + '」所在题库'"
            @click="go('/super/ops/bank-admin')"
          >
            <span class="nm">{{ k.point }}</span>
            <span class="track"><i :style="{ width: Math.max(k.rate, 1) + '%' }" /></span>
            <span class="pc">{{ k.correct }}/{{ k.items }}</span>
          </div>
          <div class="s-callout">
            <i class="el-icon-position" />
            <div>
              <b>把它变成行动，而不只是看一眼</b>
              <p style="margin:4px 0 0">
                结论算得再准，超管看完也只能自己知道。此处应支持<b>「通知该部门管理员」</b> ——
                复用已通的定向通知能力（<code>scope_type='USER'</code> 定向到该部门管理员），
                正文自动拼上薄弱知识点与得分率。<b>零新表、零新接口</b>，待 S5/S6 一起接。
              </p>
            </div>
          </div>
        </section>
      </div>

      <!-- 实习生明细（下钻入口） -->
      <div class="s-grid">
        <section class="s-card s-c12">
          <div class="s-card-h">
            <div class="tt"><span class="s-idx p">人</span><h3>本部门实习生明细</h3></div>
            <span class="hint">点击任意一行 → 个人档案（下钻）</span>
          </div>
          <div v-if="!(dept.interns || []).length" class="s-empty"><i class="el-icon-user" /><span>本部门暂无在培实习生</span></div>
          <table v-else class="s-tbl intern-tbl">
            <thead>
              <tr>
                <th style="min-width:150px">姓名 / 账号</th>
                <th style="width:110px">岗位</th>
                <th class="ctr" style="width:84px">培养状态</th>
                <th class="ctr" style="width:88px">导师</th>
                <th style="min-width:146px">学习进度</th>
                <th class="ctr" style="width:86px">任务</th>
                <th class="ctr" style="width:64px">在培</th>
                <th class="ctr" style="width:78px">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in dept.interns" :key="r.userId" class="row-link" @click="goIntern(r.userId)">
                <td>
                  <b>{{ r.nickName || r.userName }}</b>
                  <small class="n1"> · {{ r.userName }}</small>
                  <data-tag v-if="Number(r.statusConflict) === 1" :mock="true" label="状态待修正" />
                </td>
                <td>{{ r.positionName || '未设岗位' }}</td>
                <td class="ctr"><span class="s-badge" :class="stageCls(r.stage)">{{ r.stageLabel }}</span></td>
                <td class="ctr">
                  <span v-if="r.mentorName">{{ r.mentorName }}</span>
                  <span v-else class="none">未分配</span>
                </td>
                <td>
                  <div v-if="r.learnAvgProgress !== null && r.learnAvgProgress !== undefined" class="mb">
                    <span class="t"><i :style="{ width: clamp(r.learnAvgProgress) + '%' }" /></span>
                    <b>{{ r.learnAvgProgress }}%</b>
                  </div>
                  <span v-else class="none">无学习记录 <data-tag :none="true" /></span>
                  <span v-if="num(r.learnTotal)" class="n1">达标 {{ num(r.learnDone) }}/{{ num(r.learnTotal) }} 项</span>
                </td>
                <td class="ctr">
                  {{ num(r.taskDone) }}/{{ num(r.taskTotal) }}
                  <div v-if="num(r.taskOverdue)" class="n1 bad">逾期 {{ num(r.taskOverdue) }}</div>
                </td>
                <td class="ctr">{{ num(r.stayDays) }}<div class="n1">天</div></td>
                <td class="ctr"><span class="op">档案 →</span></td>
              </tr>
            </tbody>
          </table>
          <p class="s-note">
            ★ 「在培天数」自建档起算（库里**没有阶段流转日志**，所以这是近似值，不是"当前阶段停留天数"）。
          </p>
        </section>
      </div>
    </template>
  </div>
</template>

<script>
/**
 * L1 部门详情（超管「培养分析看板」下钻第一层）
 *
 * 真数据：部门 KPI / 岗位分布 / 课程完成率 / 实习生明细 —— 全部来自
 *         `GET /business/super/analysis/dept-stats?deptId=`
 * 真数据：薄弱知识点（`dept-stats.knowledge`，2026-09-22 已接真）
 *
 * ★ 一致性：本页的「本部门」与「全局」对照值都取自后端 `overview()` 的同一份聚合，
 *   所以与 L0 看板**不会漂移**（后端 deptStats 内部直接复用 overview()）。
 */
import { getDeptStats } from '@/api/business/analysis'
import DataTag from '@/components/DataTag'

export default {
  name: 'SuperOpsAnalysisDept',
  components: { DataTag },
  data() {
    return {
      loading: false,
      error: '',
      dept: {},
      blockers: {}
    }
  },
  computed: {
    deptId() {
      return this.$route.params.deptId
    },
    /** 薄弱知识点（真数据：来自 dept-stats 的 knowledge 字段）：按正确率升序，题次 < 2 视为噪声剔除 */
    weakPoints() {
      const list = this.dept.knowledge || []
      return list
        .filter(k => k.items >= 2)
        .map(k => Object.assign({}, k, { rate: Math.round(k.correct * 100 / k.items) }))
        .sort((a, b) => a.rate - b.rate)
        .slice(0, 5)
    }
  },
  created() {
    this.load()
  },
  watch: {
    // 同路由换参数时组件复用，必须监听（本项目踩过「点了没反应」）
    '$route.params.deptId'() {
      this.load()
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
    posWidth(cnt) {
      const max = Math.max.apply(null, (this.dept.positions || []).map(p => Number(p.cnt) || 0).concat([1]))
      return Math.max((Number(cnt) || 0) / max * 100, 6)
    },
    courseCls(s) {
      return { PUBLISHED: 'ok', DRAFT: 'warn', DISABLED: '' }[s] || ''
    },
    courseLabel(s) {
      return { PUBLISHED: '已发布', DRAFT: '草稿', DISABLED: '已停用' }[s] || s
    },
    stageCls(stage) {
      return { WAIT_AUDIT: '', PRE_TRAINEE: 'blue', PENDING_PROMOTE: 'warn', FORMAL_TRAINEE: 'ok' }[stage] || ''
    },
    /** 下钻到个人档案 */
    /** 统一跳转：目标路由不存在则**不动** —— 不给死链（本项目铁律） */
    go(path, query) {
      if (!path) return
      if (!this.$router.resolve(path).route.matched.length) return
      this.$router.push(query ? { path, query } : path).catch(() => {})
    },
    /** 人员类下钻统一落「人员与账号 · 人员列表」 */
    goPeople() {
      this.go('/super/org/accounts', { tab: 'people' })
    },
    goIntern(userId) {
      this.go('/super/ops/analysis/intern/' + userId)
    },
    load() {
      this.loading = true
      this.error = ''
      getDeptStats(this.deptId).then(res => {
        this.dept = res.data || {}
        this.blockers = this.dept.blockers || {}
      }).catch(err => {
        this.error = (err && err.message) ? err.message
          : '接口请求失败（可能是权限不足或该部门不在你的可见范围），请稍后重试'
        this.dept = {}
        this.blockers = {}
      }).finally(() => { this.loading = false })
    }
  }
}
</script>

<style lang="scss" scoped>
@import '~@/assets/styles/super-module.scss';

/* ⚠️ 不能出现 :root（scoped 会编译成 :root[data-v-x]，令牌全失效） */

.crumb-link { color: $blue; }

.kpi4 { grid-template-columns: repeat(4, minmax(0, 1fr)); }

/* 本部门 vs 全局 */
.vs-row { display: flex; align-items: flex-start; gap: 12px; margin-bottom: 14px; }
.vs-nm { width: 100px; flex: none; padding-top: 2px; color: $ink-3; font-size: 12px; }
.vs-bar { flex: 1; }
.vs-line {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;

  .vs-lb { width: 44px; flex: none; color: $ink-4; font-size: 11px; }
  .track {
    height: 8px;
    flex: 1;
    overflow: hidden;
    background: #edf1f5;
    border-radius: 4px;
    i { display: block; height: 100%; background: $blue; border-radius: 4px; }
    i.gray { background: #b7c1cc; }
  }
  b { min-width: 52px; color: $ink-2; font-size: 12px; text-align: right; font-variant-numeric: tabular-nums; }
}

/* 表内迷你条与文字 */
.mb {
  display: flex;
  align-items: center;
  gap: 7px;

  .t {
    height: 7px;
    flex: 1;
    min-width: 40px;
    overflow: hidden;
    background: #edf1f5;
    border-radius: 4px;
    i { display: block; height: 100%; background: $blue; border-radius: 4px; }
    i.g { background: $green; }
  }
  b { min-width: 44px; color: $ink-2; font-size: 12px; text-align: right; font-variant-numeric: tabular-nums; }
}
.n1 { color: $ink-4; font-size: 10.5px; }
.none { color: $ink-4; font-size: 12px; }
.bad { color: #b42318; }
.op { color: $blue; }

.intern-tbl tbody tr.row-link { cursor: pointer; }
.intern-tbl tbody tr.row-link:hover { background: #f7fbff; }

.s-hbar.low .track i { background: $orange; }

@media (max-width: 1280px) {
  .kpi4 { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

/* ==================== 2026-09-22：全卡片可点（与 L0 统一 affordance） ====================
   ★ 只加视觉与鼠标态；跳转走 go() / goPeople() / goIntern()，目标路由不存在时不动（不给死链）。 */
.link { position: relative; cursor: pointer; transition: background .15s, box-shadow .15s, border-color .15s; }
.link:hover { background: #f7fbff; }
.s-kpi.link:hover { border-color: #cfe0fb; box-shadow: 0 2px 10px rgba(23, 100, 245, .12); }
.s-kpi .jump {
  position: absolute;
  top: 12px;
  right: 12px;
  color: #98a2b3;
  font-size: 13px;
  opacity: 0;
  transition: opacity .15s, color .15s;
}
.s-kpi.link:hover .jump { color: $blue; opacity: 1; }
.s-hbar.link:hover { background: #f7fbff; border-radius: 6px; }
.s-tbl tbody tr.row-link .drill { color: $blue; font-size: 11px; opacity: 0; }
.s-tbl tbody tr.row-link:hover .drill { opacity: 1; }
</style>
