<template>
  <div class="s-page">
    <header class="s-head">
      <div>
        <span class="eyebrow">SUPER ADMIN · GLOBAL CONSOLE</span>
        <h1>全局工作台</h1>
        <p>全组织培养与考核的总览：在培规模、部门分布、课程与题库健康度、考核场次与异常预警。本页<b>只读全局数据</b>；业务写操作请到对应管理入口（课程 / 题库 / 实操题库对超管开放，任务类仍归部门管理员）。</p>
      </div>
      <div class="s-head-actions">
        <span class="s-ro"><i class="el-icon-view" /> 全局只读</span>
        <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="loadAll">刷新</el-button>
      </div>
    </header>

    <!-- 6 个全局 KPI -->
    <div class="s-kpis">
      <div
        v-for="k in kpis"
        :key="k.label"
        class="s-kpi link"
        :title="'前往：' + k.toLabel"
        @click="go(k.path, k.query)"
      >
        <i class="jump el-icon-top-right" />
        <div class="lb"><i class="dot" :style="{ background: k.color }" />{{ k.label }}</div>
        <div class="vl">{{ k.value }}<small v-if="k.unit">{{ k.unit }}</small></div>
        <div class="ft" :class="k.tone">{{ k.hint }}</div>
      </div>
    </div>

    <div class="s-grid">
      <!-- 部门分布 -->
      <section class="s-card s-c7">
        <div class="s-card-h">
          <div class="tt"><span class="s-idx">1</span><h3>部门分布 · 在培实习生</h3></div>
          <span class="hint">数据来源：register_application 实时聚合 · <b>点柱子 → 该部门详情</b></span>
        </div>
        <div v-if="deptStats.length" class="s-vchart">
          <div
            v-for="d in deptStats"
            :key="d.deptId"
            class="s-vcol link"
            :title="'查看 ' + d.name + ' 详情'"
            @click="goDept(d.deptId)"
          >
            <span class="bar" :class="{ hi: d.count === maxDeptCount }" :style="{ height: barHeight(d.count) }">{{ d.count }}</span>
            <span class="lb">{{ d.shortName }}</span>
          </div>
        </div>
        <div v-else class="s-empty"><i class="el-icon-data-analysis" /><span>暂无部门数据</span></div>
        <p class="s-note">口径：<b>已通过审核</b>的报名记录（status = PASSED）按部门归集。<b>学习完成率 / 考核通过率</b>不在本卡展示 —— 那两项已接真数据，见「培养分析看板」。</p>
      </section>

      <!-- 考核概览 -->
      <section class="s-card s-c5">
        <div class="s-card-h">
          <div class="tt"><span class="s-idx o">2</span><h3>考核运营概览</h3></div>
          <span class="hint">正式考核 · 跨部门</span>
        </div>
        <div v-if="exams.length" style="max-height:238px;overflow-y:auto">
          <table class="s-tbl">
            <thead><tr><th>考核名称</th><th style="width:74px">部门</th><th style="width:62px">环节</th><th style="width:74px">状态</th></tr></thead>
            <tbody>
              <tr v-for="e in exams" :key="e.id" class="row-link" title="查看：考核与成绩" @click="go('/super/ops/exams')">
                <td class="nm">{{ e.examName }}</td>
                <td>{{ e.deptName || '—' }}</td>
                <td>{{ e.examType === 'PRACTICAL' ? '实操' : '理论' }}</td>
                <td><span class="s-badge" :class="statusTone(e.status)">{{ statusText(e.status) }}</span></td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="s-empty"><i class="el-icon-finished" /><span>暂无正式考核</span></div>
        <p class="s-note">批次 = 一条 <code>exam</code> 记录；理论 / 实操为同批次下 <code>exam_type</code> 不同的两条记录，状态各自独立（可表达「理论已发布 · 实操草稿」的半场态）。</p>
      </section>

      <!-- 题库健康度 -->
      <section class="s-card s-c5">
        <div class="s-card-h">
          <div class="tt"><span class="s-idx g">3</span><h3>题库健康度</h3></div>
          <span class="hint">模拟 / 正式分库</span>
        </div>
        <div v-if="banks.length" style="max-height:238px;overflow-y:auto">
          <table class="s-tbl">
            <thead><tr><th>题库</th><th style="width:70px">类型</th><th style="width:64px">题量</th><th style="width:70px">状态</th></tr></thead>
            <tbody>
              <tr v-for="b in banks" :key="b.id" class="row-link" :title="'查看题库：' + b.bankName" @click="goBank(b.id)">
                <td class="nm">{{ b.bankName }}</td>
                <td>{{ b.bankType === 'FORMAL' ? '正式' : '模拟' }}</td>
                <td class="num">{{ b.questionCount || 0 }}</td>
                <td><span class="s-badge" :class="(b.questionCount || 0) > 0 ? 'ok' : 'warn'">{{ (b.questionCount || 0) > 0 ? '可用' : '空库' }}</span></td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="s-empty"><i class="el-icon-collection" /><span>暂无题库</span></div>
        <p class="s-note">判据：题量 &gt; 0 且 ≥ 单场组卷需求 × 2；空库会进下方异常预警。</p>
      </section>

      <!-- 异常预警 -->
      <section class="s-card s-c7">
        <div class="s-card-h">
          <div class="tt"><span class="s-idx o">4</span><h3>异常预警</h3></div>
          <span class="s-badge" :class="alerts.length ? 'warn' : 'ok'">{{ alerts.length }} 项</span>
        </div>
        <div v-if="alerts.length" class="s-steps">
          <div
            v-for="(a, i) in alerts"
            :key="i"
            class="s-step now link"
            :title="a.link ? ('前往处理：' + a.link) : ''"
            @click="goLink(a.link)"
          >
            <span class="mark">!</span>
            <div class="txt"><b>{{ a.title }}</b><span>{{ a.desc }}</span></div>
            <i class="jump el-icon-arrow-right" />
          </div>
        </div>
        <div v-else class="s-empty"><i class="el-icon-circle-check" /><span>暂无异常</span></div>
        <p class="s-note"><b>点任一条可直达处理位置</b>（空库 → 题库详情 / 草稿 → 考核与成绩 / 堆积 → 注册审核）。当前规则：空题库、草稿未发布考核、待审核报名堆积。<b>部门完成率 / 逾期未提交</b> 规则需跨模块聚合接口，属 P3 排期。</p>
      </section>

      <!-- 最近动态 -->
      <section class="s-card s-c12">
        <div class="s-card-h">
          <div class="tt"><span class="s-idx p">5</span><h3>最近动态（操作日志）</h3></div>
          <span class="hint">sys_oper_log · 共 {{ operTotal }} 条 · <b>点任一行 → 审计与合规</b></span>
        </div>
        <table v-if="operLogs.length" class="s-tbl">
          <thead><tr><th style="width:170px">时间</th><th style="width:150px">操作人</th><th style="width:150px">动作</th><th>请求</th><th style="width:80px">结果</th></tr></thead>
          <tbody>
            <tr v-for="(l, i) in operLogs" :key="i" class="row-link" title="查看：审计与合规" @click="go('/super/audit')">
              <td>{{ l.operTime }}</td>
              <td>{{ l.operName }}</td>
              <td>{{ l.title }}</td>
              <td style="color:#98a2b3">{{ l.operUrl }}</td>
              <td><span class="s-badge" :class="l.status === 0 ? 'ok' : 'red'">{{ l.status === 0 ? '成功' : '失败' }}</span></td>
            </tr>
          </tbody>
        </table>
        <div v-else class="s-empty"><i class="el-icon-time" /><span>暂无操作记录</span></div>
      </section>
    </div>
  </div>
</template>

<script>
import { listDept } from '@/api/system/dept'
import { listPosition } from '@/api/business/position'
import { listRegister } from '@/api/business/register'
import { listExam } from '@/api/business/exam'
import { listCourse } from '@/api/business/course'
import { listBank } from '@/api/business/questionBank'
import { list as listOperlog } from '@/api/monitor/operlog'
import { getRuleConfig } from '@/api/business/rule'

const COMPANY_ROOT_ID = 100 // 兜底：sys_dept 中 parentId=0 的公司根节点

/**
 * 超管「全局工作台」
 *
 * 真实数据（均为既有接口，SUPER_ADMIN 授权后可直接读）：
 *  - /system/dept/list                    部门（公司根节点下即 5 个培养部门）
 *  - /business/position/list              岗位（5 个）
 *  - /business/register/list              报名 / 花名册（含 deptId、positionId、status）
 *  - /business/exam/list?examMode=FORMAL  正式考核场次（跨部门）
 *  - /business/course/list                课程（跨部门）
 *  - /business/question-bank/list         题库（含题量）
 *  - /monitor/operlog/list                操作日志（最近动态）
 *
 * 不做的部分（如实说明，不留假数）：部门学习完成率 / 考核通过率不在本卡渲染
 *   —— 这两项已在「培养分析看板」（/super/ops/analysis）接真数据。
 */
export default {
  name: 'SuperDashboard',
  data() {
    return {
      loading: false,
      depts: [],
      positions: [],
      interns: [],
      exams: [],
      courses: [],
      banks: [],
      operLogs: [],
      /** 「规则与配置」页维护的默认值（异常预警阈值从这里读，不再是写死的 3） */
      ruleConfig: {},
      operTotal: 0
    }
  },
  computed: {
    passedInterns() {
      return this.interns.filter(r => r.status === 'PASSED')
    },
    waitingInterns() {
      return this.interns.filter(r => r.status === 'WAIT_AUDIT')
    },
    /** 公司根节点：sys_dept 里 parentId=0 的那一行（当前为「融谷」） */
    companyRootId() {
      const root = this.depts.find(d => Number(d.parentId) === 0)
      return root ? Number(root.deptId) : COMPANY_ROOT_ID
    },
    /** 培养部门 = 公司根节点的直属子部门（交付 / 开发 / 设计 / 质检 / 建模） */
    trainDepts() {
      const root = this.companyRootId
      return this.depts.filter(d => Number(d.parentId) === root && Number(d.deptId) !== root)
    },
    /** 部门 → 在培人数 */
    deptStats() {
      const map = {}
      this.passedInterns.forEach(r => {
        if (!r.deptId) return
        if (!map[r.deptId]) map[r.deptId] = { deptId: r.deptId, count: 0 }
        map[r.deptId].count++
      })
      return Object.keys(map).map(k => {
        const dept = this.depts.find(d => d.deptId === Number(k))
        const name = dept ? dept.deptName : ('部门 ' + k)
        return Object.assign(map[k], { name, shortName: name.replace('部门', '') })
      }).sort((a, b) => b.count - a.count)
    },
    maxDeptCount() {
      return this.deptStats.reduce((max, d) => Math.max(max, d.count), 0)
    },
    emptyBanks() {
      return this.banks.filter(b => !b.questionCount || b.questionCount === 0)
    },
    draftExams() {
      return this.exams.filter(e => e.status === 'DRAFT')
    },
    kpis() {
      return [
        {
          label: '在培实习生', value: this.passedInterns.length, unit: '人', color: '#1764f5',
          hint: this.waitingInterns.length ? ('另有 ' + this.waitingInterns.length + ' 人待审核') : '无待审核报名',
          tone: this.waitingInterns.length ? 'warn' : '',
          path: '/super/org/accounts', query: { tab: 'people' }, toLabel: '人员与账号 · 人员列表'
        },
        {
          label: '业务部门', value: this.trainDepts.length, unit: '个', color: '#12b76a',
          hint: this.positions.length + ' 个岗位一一绑定', tone: '',
          path: '/super/org/organization', toLabel: '组织与岗位管理'
        },
        {
          label: '已发布课程', value: this.courses.filter(c => c.status === 'PUBLISHED').length, unit: '门', color: '#7a5af8',
          hint: '跨部门合计 ' + this.courses.length + ' 门', tone: '',
          path: '/super/ops/courses', toLabel: '课程与题库总览'
        },
        {
          label: '正式考核场次', value: this.exams.length, unit: '场', color: '#f79009',
          hint: this.draftExams.length ? (this.draftExams.length + ' 场仍为草稿') : '无草稿待发布',
          tone: this.draftExams.length ? 'warn' : '',
          path: '/super/ops/exams', toLabel: '考核与成绩'
        },
        {
          label: '题库总题量', value: this.banks.reduce((sum, b) => sum + (b.questionCount || 0), 0), unit: '题', color: '#0e7490',
          hint: this.emptyBanks.length ? (this.emptyBanks.length + ' 个空库需补充') : '题库均可用',
          tone: this.emptyBanks.length ? 'warn' : '',
          path: '/super/ops/bank-admin', toLabel: '题库管理'
        },
        {
          label: '操作日志', value: this.operTotal, unit: '条', color: '#667085',
          hint: '全局操作留痕（含部门管理员）', tone: '',
          path: '/super/audit', toLabel: '审计与合规'
        }
      ]
    },
    alerts() {
      const list = []
      const pendingLimit = Number(this.ruleConfig.alertRegisterPending) || 3
      this.emptyBanks.forEach(b => {
        list.push({ title: b.bankName + ' 为空库', desc: '题量 0，无法组卷；请通知对应部门管理员导入题目', link: '/super/ops/bank-detail/' + b.id })
      })
      this.draftExams.forEach(e => {
        list.push({ title: '考核仍为草稿：' + e.examName, desc: (e.deptName || '未知部门') + ' · 未发布前实习生在正式考核页看不到该场次', link: '/super/ops/exams' })
      })
      if (this.waitingInterns.length > pendingLimit) {
        list.push({ title: '待审核报名 ' + this.waitingInterns.length + ' 条', desc: '超过阈值 ' + pendingLimit + ' 条（可在「规则与配置」调整）；建议提醒部门管理员处理', link: '/super/org/accounts?tab=register' })
      }
      return list.slice(0, 6)
    }
  },
  created() {
    this.loadAll()
  },
  methods: {
    /** 统一跳转：目标路由不存在则**不动** —— 不给死链（本项目铁律） */
    go(path, query) {
      if (!path) return
      if (!this.$router.resolve(path).route.matched.length) return
      this.$router.push(query ? { path, query } : path).catch(() => {})
    },
    /** 后端的预警 link 可能是「/x/y」或「/x/y?a=b」，这里拆开跳 */
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
    /** 部门柱 → L1 部门详情 */
    goDept(deptId) {
      this.go('/super/ops/analysis/dept/' + deptId)
    },
    /** 题库行 → 题库详情（带题目列表） */
    goBank(bankId) {
      this.go('/super/ops/bank-detail/' + bankId)
    },
    loadAll() {
      this.loading = true
      Promise.all([this.loadDepts(), this.loadPositions(), this.loadInterns(), this.loadExams(), this.loadCourses(), this.loadBanks(), this.loadLogs(), this.loadRuleConfig()])
        .finally(() => { this.loading = false })
    },
    loadDepts() {
      return listDept({ status: '0' }).then(res => { this.depts = res.data || [] }).catch(() => { this.depts = [] })
    },
    loadPositions() {
      return listPosition({ pageNum: 1, pageSize: 100 }).then(res => { this.positions = res.rows || [] }).catch(() => { this.positions = [] })
    },
    loadInterns() {
      return listRegister({ pageNum: 1, pageSize: 500 }).then(res => { this.interns = res.rows || [] }).catch(() => { this.interns = [] })
    },
    loadExams() {
      return listExam({ pageNum: 1, pageSize: 100, examMode: 'FORMAL' }).then(res => { this.exams = res.rows || [] }).catch(() => { this.exams = [] })
    },
    loadCourses() {
      return listCourse({ pageNum: 1, pageSize: 200 }).then(res => { this.courses = res.rows || [] }).catch(() => { this.courses = [] })
    },
    loadBanks() {
      return listBank({ pageNum: 1, pageSize: 200 }).then(res => { this.banks = res.rows || [] }).catch(() => { this.banks = [] })
    },
    loadLogs() {
      return listOperlog({ pageNum: 1, pageSize: 8 }).then(res => {
        this.operLogs = res.rows || []
        this.operTotal = res.total || 0
      }).catch(() => { this.operLogs = [] })
    },
    loadRuleConfig() {
      return getRuleConfig().then(res => { this.ruleConfig = res.data || {} }).catch(() => { this.ruleConfig = {} })
    },
    barHeight(count) {
      const max = this.maxDeptCount || 1
      return Math.max(Math.round(count / max * 100), 8) + '%'
    },
    statusText(s) {
      return { DRAFT: '草稿', PUBLISHED: '已发布', GRADING: '待批改', DISABLED: '已停用', ENDED: '已结束' }[s] || s
    },
    statusTone(s) {
      return { DRAFT: 'warn', PUBLISHED: 'ok', GRADING: 'blue', DISABLED: 'red', ENDED: '' }[s] || ''
    }
  }
}
</script>

<style lang="scss" scoped>
@import '~@/assets/styles/super-module.scss';

/* ==================== 2026-09-22：全卡片可点（与培养分析看板统一 affordance） ====================
   ★ 只加视觉与鼠标态；跳转走 go() / goLink() / goDept() / goBank()，目标路由不存在时不动。 */
.link { position: relative; cursor: pointer; transition: background .15s, box-shadow .15s, border-color .15s; }
.link:hover { background: #f7fbff; }
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
.s-step.link:hover .jump { color: #1764f5; opacity: 1; }
.s-step.link .txt b { transition: color .15s; }
.s-step.link:hover .txt b { color: #1764f5; }
/* 柱状图的柱子：hover 抬升 + 标签变蓝 */
.s-vcol.link:hover .bar { filter: brightness(1.08); }
.s-vcol.link:hover .lb { color: #1764f5; }
.s-tbl tbody tr.row-link { cursor: pointer; }
.s-tbl tbody tr.row-link:hover { background: #f7fbff; }
</style>
