<template>
  <div class="s-page">
    <header class="s-head">
      <div>
        <span class="eyebrow">
          <router-link to="/super/ops/analysis" class="crumb-link">培养分析看板</router-link>
          <i class="el-icon-arrow-right" />
          <router-link v-if="deptRouteOk" :to="deptRoute" class="crumb-link">{{ user.deptName || '部门' }}</router-link>
          <span v-else>{{ user.deptName || '部门' }}</span>
          <i class="el-icon-arrow-right" />
          个人档案
        </span>
        <h1>{{ user.nickName || user.userName || '个人档案' }}</h1>
        <p>
          培养状态进度 · 学习进度（逐项）· 任务交付 · 模拟与正式考核 · 知识点掌握 · 阶段评价。
          <strong>学习与任务为实时真数据</strong>；<strong>考核类为示例</strong>（题库与考核模块待同事分支合并）。
        </p>
      </div>
      <div class="s-head-actions">
        <span class="s-ro"><i class="el-icon-view" /> 全局只读</span>
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
      <!-- 身份条 -->
      <div class="idbar">
        <div class="av">{{ (user.nickName || user.userName || '?').slice(0, 1) }}</div>
        <div class="who">
          <b>{{ user.nickName || user.userName }}</b>
          <p>{{ user.deptName }} · {{ user.positionName || '未设岗位' }} · {{ user.phonenumber || '无手机号' }}</p>
        </div>
        <div class="kv">
          <div>培养状态<b><span class="s-badge" :class="stageCls(user.stage)">{{ user.stageLabel || user.userStatus }}</span></b></div>
          <div>导师<b>{{ user.mentorName || '未分配' }}</b></div>
          <div>协议<b>{{ Number(user.protocolSigned) === 1 ? '已签' : '未签' }}</b></div>
          <div>证书<b class="mut">未发放</b></div>
          <div>在培<b>{{ num(user.stayDays) }} 天</b></div>
        </div>
      </div>

      <!-- ⓪ 培养状态进度时间轴 -->
      <div class="s-grid">
        <section class="s-card s-c12">
          <div class="s-card-h">
            <div class="tt"><span class="s-idx p">⓪</span><h3>培养状态进度</h3></div>
            <span class="hint">阶段判定：以角色为主、user_status 为辅</span>
          </div>
          <div class="tl">
            <div v-for="(n, i) in timeline" :key="n.key" class="tn" :class="n.cls">
              <span v-if="i < timeline.length - 1" class="line" />
              <div class="dot">{{ n.mark }}</div>
              <b>{{ n.label }}</b>
              <s>{{ n.sub }}</s>
            </div>
          </div>

          <div v-if="Number(user.statusConflict) === 1" class="s-callout warn">
            <i class="el-icon-warning-outline" />
            <div>
              <b>状态待修正</b>
              <p style="margin:4px 0 0">
                该账号<strong>已持预备实习生角色</strong>，但 <code>user_status</code> 仍是
                <code>WAIT_AUDIT</code> —— 角色已发、审核流程未闭环。阶段按角色口径归一，此处单独标出。
              </p>
            </div>
          </div>

          <div class="s-steps gate" style="margin-top:14px">
            <div v-for="g in gate" :key="g.key" class="s-step">
              <span class="mark" :class="g.cls">{{ g.mark }}</span>
              <div class="txt">
                <b>{{ g.label }} <data-tag :mock="g.mock" /></b>
                <span>{{ g.text }}</span>
              </div>
            </div>
          </div>
          <p class="s-note">
            转正 gate 为<strong>硬条件逐条勾选</strong>：全部满足才可提交转正，能力分不参与自动否决（避免黑箱拒人）。
          </p>
        </section>
      </div>

      <!-- ① 学习进度 + ④ 任务交付 -->
      <div class="s-grid">
        <section class="s-card s-c7">
          <div class="s-card-h">
            <div class="tt"><span class="s-idx g">①</span><h3>学习进度（逐项）</h3></div>
            <span class="hint">{{ study.length }} 项 · 门槛 {{ num(threshold) }}</span>
          </div>
          <div v-if="!study.length" class="s-empty"><i class="el-icon-reading" /><span>暂无学习记录</span></div>
          <table v-else class="s-tbl">
            <thead>
              <tr><th style="min-width:170px">学习项</th><th style="width:64px">类型</th>
                <th style="min-width:130px">进度</th><th class="ctr" style="width:66px">时长</th>
                <th class="ctr" style="width:62px">读确认</th><th class="ctr" style="width:66px">状态</th></tr>
            </thead>
            <tbody>
              <tr v-for="s in study" :key="s.recordId">
                <td>
                  <b>{{ s.itemTitle || ('单项 #' + s.itemId) }}</b>
                  <div class="n1">{{ s.courseName || '（无课程）' }}</div>
                </td>
                <td><span class="s-badge" :class="typeCls(s.itemType)">{{ s.itemType }}</span></td>
                <td>
                  <div class="mb">
                    <span class="t"><i :class="reach(s.progress) ? 'g' : 'o'" :style="{ width: clamp(s.progress) + '%' }" /></span>
                    <b>{{ fmtPct(s.progress) }}</b>
                  </div>
                  <span class="n1">{{ reach(s.progress) ? '已达门槛' : '未达门槛' }}</span>
                </td>
                <td class="ctr">{{ num(s.studyDuration) }}s</td>
                <td class="ctr">
                  <span v-if="Number(s.readConfirm) === 1" class="s-badge ok">✓</span>
                  <span v-else class="none">未</span>
                </td>
                <td class="ctr">
                  <span class="s-badge" :class="s.status === 'DONE' ? 'ok' : ''">{{ s.status === 'DONE' ? '已完成' : '进行中' }}</span>
                </td>
              </tr>
            </tbody>
          </table>
          <p class="s-note">
            ★ <b>「进度」与「状态」是两件事</b>：实测存在
            <code>progress=100</code> 但 <code>status</code> 仍是 <code>IN_PROGRESS</code> 的项
            （完成判定还依赖其它条件）。本页两者都如实显示，<b>不替业务"修数"</b>。
          </p>
        </section>

        <section class="s-card s-c5">
          <div class="s-card-h">
            <div class="tt"><span class="s-idx o">④</span><h3>任务交付（逐任务）</h3></div>
            <span class="hint">{{ tasks.length }} 个 · 逾期 {{ num(user.taskOverdue) }}</span>
          </div>
          <div v-if="!tasks.length" class="s-empty"><i class="el-icon-tickets" /><span>暂无任务</span></div>
          <table v-else class="s-tbl">
            <thead>
              <tr><th style="min-width:140px">任务</th><th class="ctr" style="width:76px">状态</th>
                <th class="ctr" style="width:88px">审核</th><th class="ctr" style="width:70px">提交</th></tr>
            </thead>
            <tbody>
              <tr v-for="t in tasks" :key="t.assignmentId">
                <td>
                  <b>{{ t.taskName || ('任务 #' + t.taskId) }}</b>
                  <div class="n1">截止 {{ fmtDate(t.deadline) }}</div>
                </td>
                <td class="ctr"><span class="s-badge" :class="taskCls(t.status)">{{ taskLabel(t.status) }}</span></td>
                <td class="ctr">
                  <span v-if="t.reviewStatus" class="s-badge" :class="reviewCls(t.reviewStatus)">{{ reviewLabel(t.reviewStatus) }}</span>
                  <span v-else class="none">未提交</span>
                </td>
                <td class="ctr">
                  {{ num(t.submitCount) }} 次
                  <div v-if="num(t.submitCount) > 1" class="n1 bad">重交 {{ num(t.submitCount) - 1 }} 次</div>
                </td>
              </tr>
            </tbody>
          </table>
          <p class="s-note">
            ★ 审核结果是<b>三值</b>（待审核 / 通过 / 驳回），<b>不能当布尔用</b>；提交次数 &gt; 1 说明被驳回后重交过。
          </p>
        </section>
      </div>

      <!-- ② 模拟考核 + ⑤ 知识点 -->
      <div class="s-grid">
        <section class="s-card s-c5">
          <div class="s-card-h">
            <div class="tt"><span class="s-idx">②</span><h3>模拟考核（逐场）</h3></div>
            <span class="s-badge warn">示例数据 · 待分支合并</span>
          </div>
          <div v-if="!practice.length" class="s-empty"><i class="el-icon-tickets" /><span>暂无模拟考核记录</span></div>
          <template v-else>
            <div v-for="(p, i) in practice" :key="i" class="s-hbar">
              <span class="nm">{{ p.date }}</span>
              <span class="track"><i :class="p.score >= 8 ? 'g' : (p.score >= 6 ? '' : 'r')" :style="{ width: clamp(p.score * 10) + '%' }" /></span>
              <span class="pc">{{ p.correct }}/{{ p.total }}</span>
            </div>
            <p class="s-note">
              共 {{ practice.length }} 场 · 均分 {{ practiceAvg }} / {{ practice[0].total }}。
              ★ 实测后 5 场<b>分数完全不变</b>（都是 5 分）—— 这不是"发挥稳定"，而是<b>刷同一套题</b>；
              看板应看<b>去重后的知识覆盖</b>，而不是把多次简单平均当成能力值。
            </p>
          </template>
        </section>

        <section class="s-card s-c7">
          <div class="s-card-h">
            <div class="tt"><span class="s-idx r">⑤</span><h3>知识点掌握（最弱在上）</h3></div>
            <span class="s-badge warn">示例数据 · 待分支合并</span>
          </div>
          <div v-if="!knowledge.length" class="s-empty"><i class="el-icon-data-analysis" /><span>暂无逐题明细</span></div>
          <template v-else>
            <div v-for="k in knowledge" :key="k.point" class="s-hbar" :class="{ low: k.rate < 50 }">
              <span class="nm" :title="k.point">{{ k.point }}</span>
              <span class="track"><i :style="{ width: Math.max(k.rate, 1) + '%' }" /></span>
              <span class="pc">{{ k.correct }}/{{ k.items }}</span>
            </div>
            <p class="s-note">
              ★ 题次 &lt; 2 的知识点<b>不参与排序</b>（1 题次算出的 0%/100% 是噪声）。
              灰/橙低分项即薄弱点。
            </p>
          </template>
        </section>
      </div>

      <!-- ③ 正式考核 + ⑦ 阶段评价 -->
      <div class="s-grid">
        <section class="s-card s-c6">
          <div class="s-card-h">
            <div class="tt"><span class="s-idx">③</span><h3>正式考核</h3></div>
            <span class="s-badge warn">示例 · 本人无记录</span>
          </div>
          <div class="s-steps">
            <div class="s-step">
              <span class="mark">—</span>
              <div class="txt">
                <b class="mut">暂无本人正式考核记录</b>
                <span>全库仅 1 张已发布答卷（属设计部门实习生，0 分未通过）。此处显示说明文案，<b>不留空白骨架</b>。</span>
              </div>
            </div>
          </div>
        </section>

        <section class="s-card s-c6">
          <div class="s-card-h">
            <div class="tt"><span class="s-idx p">⑦</span><h3>阶段评价</h3></div>
            <span class="s-badge warn">表为空 · 待部门填写</span>
          </div>
          <div class="s-steps">
            <div class="s-step">
              <span class="mark">空</span>
              <div class="txt">
                <b class="mut">暂无阶段评价</b>
                <span><code>stage_evaluation</code> 全表 0 行 → 显示「暂无」+ 橙标，不显示空白骨架。</span>
              </div>
            </div>
          </div>
          <div class="s-callout warn">
            <i class="el-icon-warning-outline" />
            <div>
              <b>合规红线</b>
              <p style="margin:4px 0 0">
                <code>visible_scope='ADMIN_ONLY'</code> 的评语与人工维度分
                <b>只在管理端显示，不得进入实习生端画像</b>。实现时必须过滤。
              </p>
            </div>
          </div>
        </section>
      </div>
    </template>
  </div>
</template>

<script>
/**
 * L3 个人档案（超管「培养分析看板」下钻到个人）
 *
 * 真数据（`GET /business/super/analysis/intern/{userId}`）：
 *   身份 / 培养状态 / 学习逐项 / 任务逐笔（含最新一次审核结果）
 *   ★ 这两个查询补的是「超管读缺口」—— `/business/learning/**`、`/business/practice/**`
 *     的类级注解只放实习生角色；本模块另写只读路径，**没去动那两处的类级注解**。
 *
 * 示例（`_mock.js`）：模拟考核逐场 / 知识点掌握 / 正式考核
 * 无数据：阶段评价（`stage_evaluation` 0 行）
 */
import { getInternDetail } from '@/api/business/analysis'
import DataTag from '@/components/DataTag'
import { mockIntern, mockInternKnowledge, mockInternPractice } from './_mock'

/** 阶段顺序（与后端 InternStageRow.stage 的取值一致） */
const STAGE_ORDER = ['WAIT_AUDIT', 'PRE_TRAINEE', 'PENDING_PROMOTE', 'FORMAL_TRAINEE']

export default {
  name: 'SuperOpsAnalysisIntern',
  components: { DataTag },
  data() {
    return {
      loading: false,
      error: '',
      user: {},
      study: [],
      tasks: [],
      threshold: 70
    }
  },
  computed: {
    userId() {
      return Number(this.$route.params.userId)
    },
    deptRoute() {
      return '/super/ops/analysis/dept/' + this.user.deptId
    },
    /** 下钻回部门：目标路由不存在就不给链接（不给死链） */
    deptRouteOk() {
      return !!this.user.deptId && this.$router.resolve(this.deptRoute).route.matched.length > 0
    },

    /** 培养进度时间轴：已过阶段 ✔ / 当前高亮 / 未达灰 + 列出缺什么 */
    timeline() {
      const idx = STAGE_ORDER.indexOf(this.user.stage)
      const nodes = [
        { key: 'PENDING', label: '提交报名', sub: '已建档' },
        { key: 'PRE_TRAINEE', label: '预备实习生', sub: this.mentorSub() },
        { key: 'PENDING_PROMOTE', label: '待转正', sub: '需 5 条硬条件全部满足' },
        { key: 'FORMAL_TRAINEE', label: '正式实习生', sub: '部门终审通过 + 发证' }
      ]
      // 节点下标与阶段下标对齐：node0=报名、node1=预备、node2=待转正、node3=正式。
      // ★ 当前节点 = max(idx, 1)：待审核（idx=0）时"报名"已完成、当前卡在"预备"这一步。
      // ★ 已到终点（正式）时 cur = 节点数 → 全部标为已完成（否则最后一步会一直显示成"进行中"）。
      let cur
      if (idx < 0) {
        cur = 1
      } else if (idx >= STAGE_ORDER.length - 1) {
        cur = nodes.length
      } else {
        cur = Math.max(idx, 1)
      }
      return nodes.map((n, i) => {
        let cls = ''
        let mark = String(i + 1)
        if (i < cur) {
          cls = 'ok'
          mark = '✓'
        } else if (i === cur) {
          cls = 'cur'
          mark = '●'
        }
        return Object.assign({}, n, { cls, mark })
      })
    },

    /** 转正 gate：逐条勾选（学习/任务/协议为真数据；正式考试为示例；终审无数据） */
    gate() {
      const th = Number(this.threshold || 70)
      const total = Number(this.user.learnTotal || 0)
      const done = Number(this.user.learnDone || 0)
      const learnRate = total > 0 ? done * 100 / total : 0
      const formalPassed = mockIntern(this.userId).formalPassed
      const rows = [
        { key: 'learn', label: '学习达标（≥ ' + th + '）',
          ok: total > 0 && learnRate >= th, mock: false,
          text: total > 0 ? (done + '/' + total + ' 项达标，达标率 ' + Math.round(learnRate) + '%') : '暂无学习记录' },
        { key: 'formal', label: '正式考试通过', ok: formalPassed === 1, mock: true,
          text: formalPassed === undefined || formalPassed === null ? '无本人考核记录' : (formalPassed === 1 ? '已通过' : '未通过') },
        { key: 'task', label: '任务无逾期', ok: Number(this.user.taskOverdue || 0) === 0, mock: false,
          text: Number(this.user.taskOverdue || 0) > 0 ? ('逾期 ' + this.user.taskOverdue + ' 个') : '无逾期' },
        { key: 'protocol', label: '协议已签', ok: Number(this.user.protocolSigned || 0) === 1, mock: false,
          text: Number(this.user.protocolSigned || 0) === 1 ? '已签署' : '未签署（转正硬条件）' },
        { key: 'approve', label: '部门终审通过', ok: false, mock: false,
          text: 'promotion_application 尚无记录 —— 未提交转正申请' }
      ]
      return rows.map(g => Object.assign(g, {
        cls: g.ok ? 'done' : '',
        mark: g.ok ? '✓' : '!'
      }))
    },

    /** 模拟考核（示例）：按分数升序，便于看趋势 */
    practice() {
      const list = mockInternPractice(this.userId)
      return list.map(p => Object.assign({}, p))
    },
    practiceAvg() {
      if (!this.practice.length) return '--'
      const s = this.practice.reduce((a, p) => a + Number(p.score || 0), 0)
      return Math.round(s / this.practice.length * 100) / 100
    },

    /** 知识点（示例）：题次 >= 2、按正确率升序（最弱在上） */
    knowledge() {
      return mockInternKnowledge(this.userId)
        .filter(k => k.items >= 2)
        .map(k => Object.assign({}, k, { rate: Math.round(k.correct * 100 / k.items) }))
        .sort((a, b) => a.rate - b.rate)
    }
  },
  created() {
    this.load()
  },
  watch: {
    '$route.params.userId'() {
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
    fmtPct(v) {
      if (v === null || v === undefined) return '--'
      const n = Number(v)
      return (Math.round(n * 10) / 10) + '%'
    },
    fmtDate(v) {
      if (!v) return '—'
      return String(v).slice(0, 10)
    },
    reach(p) {
      return Number(p) >= Number(this.threshold || 70)
    },
    mentorSub() {
      const m = this.user.mentorName
      const p = Number(this.user.protocolSigned) === 1 ? '协议已签' : '协议未签'
      return m ? ('导师 ' + m + ' · ' + p) : ('未分配导师 · ' + p)
    },
    stageCls(stage) {
      return { WAIT_AUDIT: '', PRE_TRAINEE: 'blue', PENDING_PROMOTE: 'warn', FORMAL_TRAINEE: 'ok' }[stage] || ''
    },
    typeCls(t) {
      return { DOC: '', VIDEO: 'purple' }[t] || ''
    },
    taskCls(s) {
      return { DONE: 'ok', OVERDUE: 'red', NOT_STARTED: '' }[s] || ''
    },
    taskLabel(s) {
      return { DONE: '已交', OVERDUE: '逾期', NOT_STARTED: '未开始' }[s] || s
    },
    /** ⚠️ review_status 是三值，不能当布尔 */
    reviewCls(s) {
      return { PASSED: 'ok', REJECTED: 'red', PENDING: 'warn' }[s] || ''
    },
    reviewLabel(s) {
      return { PASSED: '通过', REJECTED: '驳回', PENDING: '待审核' }[s] || s
    },
    load() {
      this.loading = true
      this.error = ''
      const uid = this.userId
      if (!uid) {
        this.error = '缺少人员参数'
        this.loading = false
        return
      }
      getInternDetail(uid).then(res => {
        const d = res.data || {}
        this.user = d.user || {}
        this.study = d.study || []
        this.tasks = d.tasks || []
        this.threshold = d.threshold || 70
      }).catch(err => {
        this.error = (err && err.message) ? err.message
          : '接口请求失败（可能是权限不足或该人员不在你的可见范围），请稍后重试'
        this.user = {}
        this.study = []
        this.tasks = []
      }).finally(() => { this.loading = false })
    }
  }
}
</script>

<style lang="scss" scoped>
@import '~@/assets/styles/super-module.scss';

/* ⚠️ 不能出现 :root */

.crumb-link { color: $blue; }
.mut { color: $ink-4; }

/* 身份条 */
.idbar {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
  margin-bottom: 14px;
  background: #fff;
  border: 1px solid $line;
  border-radius: 8px;

  .av {
    width: 46px;
    height: 46px;
    flex: none;
    display: flex;
    align-items: center;
    justify-content: center;
    color: $blue;
    background: $blue-soft;
    font-size: 18px;
    font-weight: 700;
    border-radius: 12px;
  }
  .who b { color: $ink; font-size: 15px; }
  .who p { margin: 3px 0 0; color: $ink-3; font-size: 12px; }
  .kv {
    display: flex;
    gap: 20px;
    flex-wrap: wrap;
    margin-left: auto;

    div { color: $ink-4; font-size: 11.5px; }
    b { display: block; margin-top: 3px; color: $ink-2; font-size: 12.5px; }
  }
}

/* 时间轴 */
.tl { display: flex; align-items: stretch; }
.tn {
  flex: 1;
  position: relative;
  padding: 0 6px;

  .dot {
    position: relative;
    z-index: 2;
    width: 26px;
    height: 26px;
    margin: 0 auto 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: $ink-3;
    background: #f2f4f7;
    font-size: 12px;
    font-weight: 700;
    border-radius: 50%;
  }
  .line {
    position: absolute;
    top: 13px;
    left: 50%;
    width: 100%;
    height: 2px;
    background: $line;
    z-index: 1;
  }
  b { display: block; color: $ink-3; font-size: 12px; text-align: center; }
  s { display: block; margin-top: 3px; color: $ink-4; font-size: 11px; line-height: 1.55; text-align: center; text-decoration: none; }

  &.ok {
    .dot { color: #1a7a58; background: $green-soft; }
    .line { background: $green; }
    b { color: #1a7a58; }
  }
  &.cur {
    .dot { color: #fff; background: $blue; box-shadow: 0 0 0 4px $blue-soft; }
    b { color: $blue; }
  }
}

/* 卡片内表格迷你条 */
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
    i {
      display: block;
      height: 100%;
      background: $blue;
      border-radius: 4px;
      &.g { background: $green; }
      &.o { background: $orange; }
      &.r { background: $red; }
    }
  }
  b { min-width: 44px; color: $ink-2; font-size: 12px; text-align: right; font-variant-numeric: tabular-nums; }
}
.n1 { color: $ink-4; font-size: 10.5px; }
.none { color: $ink-4; font-size: 12px; }
.bad { color: #b42318; }

.s-hbar .track i.r { background: $red; }
.s-hbar .track i.g { background: $green; }
.s-hbar.low .track i { background: $orange; }

.gate .mark { font-size: 12px; }
</style>
