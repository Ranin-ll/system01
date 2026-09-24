<template>
  <div v-loading="loading" class="dept-page">
    <!-- 面包屑 -->
    <div class="dept-breadcrumb">
      部门管理员 <span>/</span> <b>工作台</b>
    </div>

    <!-- 部门身份条 -->
    <div class="dsec" style="padding:18px 22px">
      <div class="didentity">
        <span class="big-ph">{{ deptBadge }}</span>
        <div class="who">
          <b>{{ deptName }}</b>
          <p>部门管理员 · 培养周期 {{ cycleLabel }} · 岗位 {{ positionCount }} 个</p>
        </div>
        <div class="facts">
          <div class="fact"><span>在册实习生</span><b>{{ internCount }}</b></div>
          <div class="fact"><span>本轮已转正</span><b class="ok">{{ formalCount }}</b></div>
          <div class="fact">
            <span>今日待办</span>
            <b :class="todayTodo > 0 ? 'warn' : ''">{{ todayTodo }}</b>
          </div>
          <div class="fact"><span>最近活跃</span><b style="font-size:14px">{{ lastActiveText }}</b></div>
        </div>
      </div>
    </div>

    <div class="dgrid">
      <!-- ============ ② 待办中心 ============ -->
      <div class="dcard c12">
        <div class="dcard-h">
          <div class="tt">
            <span class="idx">2</span>
            <h3>待办中心</h3>
          </div>
          <span class="hint-text">点击直达对应列表（自动带筛选参数）</span>
        </div>
        <div class="dkpi-grid c6">
          <div v-for="item in todoItems" :key="item.key" class="dkpi clickable" @click="goTodo(item)">
            <div class="dkpi-label">
              <span class="d" :style="{ background: item.color }" />
              {{ item.label }}
            </div>
            <div class="dkpi-val" :class="{ mute: !item.value }">{{ item.value === null || item.value === undefined ? '--' : item.value }}</div>
            <div class="dkpi-sub">{{ item.hint }}</div>
          </div>
        </div>
      </div>

      <!-- ============ ③ 部门总览 KPI ============ -->
      <div v-for="item in overviewKpis" :key="item.key" class="dkpi c2">
        <div class="dkpi-label">
          <span class="d" :style="{ background: item.color }" />
          {{ item.label }}
        </div>
        <div class="dkpi-val">
          <template v-if="item.value === null || item.value === undefined">--</template>
          <template v-else>{{ item.value }}<small v-if="item.unit">{{ item.unit }}</small></template>
        </div>
        <div class="dkpi-sub" :class="item.tone">{{ item.hint }}</div>
        <div v-if="item.pct !== undefined" class="dbar-mini">
          <i :class="item.barTone" :style="{ width: item.pct + '%' }" />
        </div>
      </div>

      <!-- ============ ④ 培养漏斗（真实数据） ============ -->
      <div class="dcard c5">
        <div class="dcard-h">
          <div class="tt">
            <span class="idx g">4</span>
            <h3>培养漏斗</h3>
          </div>
          
        </div>
        <div
          v-for="row in funnel"
          :key="row.key"
          class="dhbar clickable"
          :title="'查看「' + row.label + '」的人员明细'"
          @click="go('/department/people/students')"
        >
          <div class="dhbar-head">
            <span class="dhbar-name">{{ row.label }}</span>
            <span class="dhbar-meta">{{ row.value }} 人</span>
          </div>
          <div class="dhbar-track">
            <div class="dhbar-fill" :class="row.tone" :style="{ width: row.pct + '%' }" />
          </div>
        </div>
      </div>

      <!-- ============ ⑤ 学习进度分布 ============ -->
      <div class="dcard c7">
        <div class="dcard-h">
          <div class="tt">
            <span class="idx">5</span>
            <h3>学习进度分布</h3>
          </div>
          <span class="hint-text">
            {{ internCount }} 名实习生 · 按完成率分档
            <template v-if="!studyDist.length">· 数据待接入</template>
            <template v-else>· <b>点柱子 → 实习生管理</b></template>
          </span>
        </div>
        <div class="dvcols">
          <div
            v-for="col in studyDist"
            :key="col.label"
            class="dvcol clickable"
            :title="'查看「' + col.label + '」的人员明细'"
            @click="go('/department/people/students')"
          >
            <div class="dvcol-bar" :class="col.tone" :style="{ height: col.pct + '%' }" />
            <span class="dvcol-x">{{ col.label }}</span>
          </div>
        </div>
        <div class="dcallout warn" style="margin:14px 0 0">
          <i class="el-icon-warning-outline" />
          <span>
            <b>{{ lowCount }} 人完成率不足 50%</b>，其中 {{ blockedCount }} 人因此未达正式考核门槛（需 70%）。
            建议直接在此处「发任务」催办。
          </span>
        </div>
      </div>

      <!-- ============ ⑥ 考核概览 ============ -->
      <div class="dcard c7">
        <div class="dcard-h">
          <div class="tt">
            <span class="idx">6</span>
            <h3>考核概览</h3>
          </div>
          <span class="hint-text">
            最近 5 个环节 · 点行进成绩管理
      <!-- 2026-09-22：本卡数据待接入，已移除「示例」标（没有就不显示假数） -->
          </span>
        </div>
        <table class="dtbl">
          <thead>
            <tr>
              <th>批次 / 考核名称</th><th>模式</th><th>环节</th><th>参与</th><th>平均分</th><th>通过率</th><th>状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in examRows" :key="row.key" class="row-link" @click="goScores">
              <td>
                <span class="strong">{{ row.name }}</span>
              </td>
              <td>{{ row.typeText }}</td>
              <td>{{ row.stageText }}</td>
              <td>{{ row.joined }}</td>
              <td>{{ row.avg }}</td>
              <td>{{ row.passRate }}</td>
              <td><span class="dbadge" :class="row.tone">{{ row.statusText }}</span></td>
            </tr>
          </tbody>
        </table>
        <p v-if="!examRows.length" class="dsec-note" style="margin-top:10px">本部门暂无考核记录。</p>
        <p class="dsec-note" style="margin-top:10px">同一实习生可在多个批次分阶段多次参加正式考核，每批次独立计分；理论 / 实操为独立环节，各自发布。</p>
      </div>

      <!-- ============ ⑦ 最薄弱知识点 Top5 ============ -->
      <div class="dcard c5">
        <div class="dcard-h">
          <div class="tt">
            <span class="idx o">7</span>
            <h3>最薄弱知识点 Top5</h3>
          </div>
          <span class="hint-text">
            本部门整体 · <b>点任一行 → 题库管理</b>
          </span>
        </div>
        <div
          v-for="row in weakPoints"
          :key="row.name"
          class="dhbar clickable"
          :title="'查看知识点「' + row.name + '」所在题库'"
          @click="go('/department/study/banks')"
        >
          <div class="dhbar-head">
            <span class="dhbar-name">{{ row.name }}</span>
            <span class="dhbar-meta"><b :class="row.tone">{{ row.rate }}%</b></span>
          </div>
          <div class="dhbar-track">
            <div class="dhbar-fill" :class="row.barTone" :style="{ width: row.rate + '%' }" />
          </div>
        </div>
      </div>

      <!-- ============ ⑧ 最近动态 ============ -->
      <div class="dcard c7">
        <div class="dcard-h">
          <div class="tt">
            <span class="idx g">8</span>
            <h3>最近动态</h3>
          </div>
          <span class="hint-text">真实数据 · 注册申请 / 考核发布 · <b>点任一条 → 对应处理页</b></span>
        </div>
        <div v-if="activities.length" class="dkv">
          <div
            v-for="(item, i) in activities"
            :key="i"
            class="clickable"
            :title="item.to ? ('前往：' + item.to) : ''"
            @click="go(item.to)"
          >
            <span>{{ item.time }}</span>
            <strong style="font-size:12.5px;font-weight:500">{{ item.text }}</strong>
            <i v-if="item.to" class="el-icon-arrow-right jump-inline" />
          </div>
        </div>
        <div v-else class="dempty small">
          <i class="el-icon-time" />
          <span>暂无动态</span>
        </div>
      </div>
    </div>

    <!-- 权限边界 -->
    
  </div>
</template>

<script>
import { getRegisterSummary, listRegister } from '@/api/business/register'
import { listExam } from '@/api/business/exam'

export default {
  name: 'DeptDashboard',
  data() {
    return {
      loading: false,
      summary: { totalCount: 0, pendingCount: 0, passedCount: 0, rejectedCount: 0 },
      roster: [],
      exams: []
    }
  },
  computed: {
    /** 本部已通过注册的实习生（花名册口径） */
    interns() {
      return this.roster.filter(row => row.status === 'PASSED')
    },
    internCount() {
      return this.interns.length
    },
    deptName() {
      const found = this.roster.find(row => row.deptName)
      return found ? found.deptName : '本部门'
    },
    deptBadge() {
      return this.deptName.replace(/部门$/, '').slice(0, 1) || '部'
    },
    positionCount() {
      const set = {}
      this.interns.forEach(row => {
        if (row.positionName) set[row.positionName] = 1
      })
      return Object.keys(set).length || '—'
    },
    cycleLabel() {
      const d = new Date()
      return d.getFullYear() + '-Q' + (Math.floor(d.getMonth() / 3) + 1)
    },
    /** 按 user_status 统计（漏斗真实口径） */
    statusCount() {
      const c = { WAIT_AUDIT: 0, PRE_TRAINEE: 0, PENDING_PROMOTE: 0, FORMAL_TRAINEE: 0 }
      this.roster.forEach(row => {
        const s = row.userStatus || (row.status === 'WAIT_AUDIT' ? 'WAIT_AUDIT' : '')
        if (s in c) c[s] += 1
      })
      return c
    },
    formalCount() {
      return this.statusCount.FORMAL_TRAINEE
    },
    preCount() {
      return this.statusCount.PRE_TRAINEE
    },
    pendingPromoteCount() {
      return this.statusCount.PENDING_PROMOTE
    },
    pendingGrading() {
      return this.exams.reduce((sum, e) => sum + (e.pendingCount || 0), 0)
    },
    draftExams() {
      return this.exams.filter(e => e.status === 'DRAFT').length
    },
    /** 培养漏斗：四段真实数据 */
    funnel() {
      const c = this.statusCount
      const rows = [
        { key: 'audit', label: '注册待审', value: c.WAIT_AUDIT, tone: 'mid' },
        { key: 'pre', label: '实习中 PRE', value: c.PRE_TRAINEE, tone: 'avg' },
        { key: 'promote', label: '转正审核中', value: c.PENDING_PROMOTE, tone: 'mid' },
        { key: 'formal', label: '已转正', value: c.FORMAL_TRAINEE, tone: 'good' }
      ]
      const max = Math.max.apply(null, rows.map(r => r.value).concat([1]))
      return rows.map(r => Object.assign({}, r, { pct: Math.round(r.value / max * 100) }))
    },
    /** 今日待办（前 4 项为真实待办；后 2 项无接口 ⇒ 值为 null，界面显示 --） */
    todoItems() {
      return [
        { key: 'audit', label: '待审核注册', value: this.summary.pendingCount, color: '#1764f5', hint: '本部门累计提交 ' + this.summary.totalCount + ' 条', to: '/department/people/register-review' },
        { key: 'grading', label: '待批阅答卷', value: this.pendingGrading, color: '#f79009', hint: this.exams.length ? '来自 ' + this.exams.length + ' 场考核' : '暂无待批阅', to: '/department/study/scores' },
        { key: 'promote', label: '待转正审批', value: this.pendingPromoteCount, color: '#7a5af8', hint: this.pendingPromoteCount ? '已推荐待终审' : '暂无待审批', to: '/department/people/promotion' },
        { key: 'publish', label: '待发布考核', value: this.draftExams, color: '#12b76a', hint: this.draftExams ? '草稿待发布' : '暂无草稿', to: '/department/study/exam' }
        // ★ 2026-09-23：「进行中任务 / 未读消息」两张卡随「任务与通知」一起下线
      ]
    },
    todayTodo() {
      return this.todoItems.reduce((sum, item) => sum + (item.value || 0), 0)
    },
    /** ③ 六个概览指标 */
    overviewKpis() {
      return [
        { key: 'intern', label: '在册实习生', value: this.internCount, unit: '人', hint: '本部门 ' + this.positionCount + ' 个岗位', color: '#1764f5' },
        { key: 'pre', label: '预备实习中', value: this.preCount, unit: '人', hint: 'PRE_TRAINEE', color: '#1764f5' },
        { key: 'formal', label: '已转正', value: this.formalCount, unit: '人', hint: 'FORMAL_TRAINEE', tone: 'ok', color: '#12b76a' },
        { key: 'study', label: '学习完成率均值', value: null, unit: '%', hint: '待接入部门学习统计', tone: '', barTone: 'o', color: '#1764f5' },
        { key: 'practice', label: '模拟正确率均值', value: null, unit: '%', hint: '待接入模拟考核统计', tone: '', barTone: 'g', color: '#1764f5' },
        { key: 'pass', label: '正式考核通过率', value: null, unit: '%', hint: this.formalCount + ' 人已转正', tone: '', barTone: 'g', color: '#12b76a' }
      ]
    },
    /** 学习进度分布：待接入「部门学习完成率分档」接口（2026-09-22：先留空，不再用示例值） */
    studyDist() {
      return []
    },
    lowCount() {
      return 0
    },
    blockedCount() {
      return 0
    },
    /** 最薄弱知识点：待接入「部门 × 章节得分率」接口（超管端已有 knowledge-matrix，部门端待补） */
    weakPoints() {
      return []
    },
    /** ⑥ 考核概览：只显示真实的考核环节（不再用示例行补足） */
    examRows() {
      const statusMap = {
        DRAFT: { text: '草稿', tone: '' },
        PUBLISHED: { text: '已发布', tone: 'green' },
        RUNNING: { text: '进行中', tone: 'blue' },
        CLOSED: { text: '已结束', tone: 'gray' }
      }
      const real = this.exams.slice(0, 5).map(e => {
        const st = statusMap[e.status] || { text: e.status || '—', tone: '' }
        return {
          key: 'e' + e.id,
          name: e.examName,
          typeText: e.examMode === 'FORMAL' ? '正式' : (e.examMode === 'MOCK' ? '模拟' : '—'),
          stageText: e.examType === 'PRACTICAL' ? '实操' : '理论',
          joined: e.answeredCount != null ? e.answeredCount : '—',
          avg: '—',
          passRate: '—',
          statusText: st.text,
          tone: st.tone
        }
      })
      // 2026-09-22：不再用示例行补足 5 行（没有就空着），只显示真实的考核环节
      return real
    },
    /** ⑧ 最近动态：由注册申请 + 考核发布时间线合成（真实） */
    activities() {
      const list = []
      this.exams.forEach(e => {
        if (e.publishedAt) {
          list.push({ ts: new Date(e.publishedAt).getTime(), time: this.fmtDate(e.publishedAt), text: '发布考核「' + e.examName + '」', to: '/department/study/exam' })
        } else if (e.createTime) {
          list.push({ ts: new Date(e.createTime).getTime(), time: this.fmtDate(e.createTime), text: '创建考核草稿「' + e.examName + '」', to: '/department/study/exam' })
        }
      })
      this.roster.forEach(row => {
        const ts = row.updateTime || row.createTime
        if (!ts) return
        if (row.status === 'PASSED') {
          list.push({ ts: new Date(ts).getTime(), time: this.fmtDate(ts), text: '通过注册申请（' + row.realName + '）', to: row.userId ? ('/department/people/profile/' + row.userId) : '/department/people/students' })
        } else if (row.status === 'REJECTED') {
          list.push({ ts: new Date(ts).getTime(), time: this.fmtDate(ts), text: '驳回注册申请（' + row.realName + '）', to: '/department/people/register-review' })
        } else {
          list.push({ ts: new Date(ts).getTime(), time: this.fmtDate(ts), text: '提交注册申请（' + row.realName + '）', to: '/department/people/register-review' })
        }
      })
      return list.sort((a, b) => b.ts - a.ts).slice(0, 6)
    },
    lastActiveText() {
      const list = this.roster.map(r => r.updateTime || r.createTime).filter(Boolean)
      if (!list.length) return '—'
      const latest = new Date(Math.max.apply(null, list.map(t => new Date(t).getTime())))
      return this.fmtDate(latest)
    }
  },
  created() {
    this.loadAll()
  },
  methods: {
    loadAll() {
      this.loading = true
      Promise.all([
        getRegisterSummary().catch(() => null),
        listRegister({ pageNum: 1, pageSize: 200 }).catch(() => null),
        listExam({ pageNum: 1, pageSize: 50 }).catch(() => null)
      ]).then(([sumRes, rosterRes, examRes]) => {
        const s = sumRes && sumRes.data ? sumRes.data : {}
        this.summary = {
          totalCount: this.pick(s, 'totalCount', 'total_count'),
          pendingCount: this.pick(s, 'pendingCount', 'pending_count'),
          passedCount: this.pick(s, 'passedCount', 'passed_count'),
          rejectedCount: this.pick(s, 'rejectedCount', 'rejected_count')
        }
        this.roster = (rosterRes && rosterRes.rows) || []
        this.exams = (examRes && examRes.rows) || []
      }).finally(() => {
        this.loading = false
      })
    },
    pick(obj, camel, snake) {
      if (obj[camel] !== undefined) return Number(obj[camel]) || 0
      if (obj[snake] !== undefined) return Number(obj[snake]) || 0
      return 0
    },
    fmtDate(value) {
      const d = new Date(value)
      if (isNaN(d.getTime())) return '—'
      const p = n => (n < 10 ? '0' + n : '' + n)
      const today = new Date()
      const sameDay = d.toDateString() === today.toDateString()
      const yest = new Date(today.getTime() - 86400000)
      if (sameDay) return '今天 ' + p(d.getHours()) + ':' + p(d.getMinutes())
      if (d.toDateString() === yest.toDateString()) return '昨天 ' + p(d.getHours()) + ':' + p(d.getMinutes())
      return p(d.getMonth() + 1) + '-' + p(d.getDate())
    },
    /** 统一跳转：目标路由不存在则**不动** —— 不给死链（本项目铁律） */
    go(path, query) {
      if (!path) return
      if (!this.$router.resolve(path).route.matched.length) return
      this.$router.push(query ? { path, query } : path).catch(() => {})
    },
    goTodo(item) {
      this.go(item.to)
    },
    goScores() {
      this.$router.push('/department/study/scores')
    }
  }
}
</script>

<style lang="scss" scoped>
@import '~@/assets/styles/department-module.scss';

.hint-text { color: #98a2b3; font-size: 11.5px; }
.dcard-h .hint-text { white-space: nowrap; }
.dtbl tr.row-link { cursor: pointer; }
code { padding: 1px 5px; color: #344054; font-size: 11.5px; background: #f2f4f7; border-radius: 4px; }
.dkpi-val small { margin-left: 2px; color: #667085; font-size: 12px; font-weight: 400; }
@media (max-width: 1280px) {
  .didentity { flex-wrap: wrap; }
  .didentity .facts { width: 100%; padding-top: 14px; border-top: 1px solid #e4e9f0; }
}
@media (max-width: 820px) {
  .didentity .facts { flex-wrap: wrap; gap: 14px 0; }
  .didentity .fact { padding: 0 14px; }
}

/* ==================== 2026-09-22：卡片内元素可点（与超管端统一 affordance） ====================
   ★ 只加视觉与鼠标态；跳转走 go()，目标路由不存在时不动（不给死链）。 */
.dhbar.clickable { padding: 6px 8px; margin-right: -8px; margin-left: -8px; border-radius: 8px; cursor: pointer; transition: background .15s; }
.dhbar.clickable:hover { background: #f7fbff; }
.dhbar.clickable:hover .dhbar-name { color: #1764f5; }
.dvcol.clickable { cursor: pointer; transition: filter .15s; }
.dvcol.clickable:hover { filter: brightness(1.06); }
.dvcol.clickable:hover .dvcol-x { color: #1764f5; }
.dkv > div.clickable { display: flex; align-items: center; gap: 6px; padding: 5px 8px; margin: 0 -8px; border-radius: 8px; cursor: pointer; transition: background .15s; }
.dkv > div.clickable:hover { background: #f7fbff; }
.dkv > div.clickable:hover strong { color: #1764f5; }
.jump-inline { margin-left: auto; color: #98a2b3; font-size: 12px; opacity: 0; transition: opacity .15s, color .15s; }
.dkv > div.clickable:hover .jump-inline { color: #1764f5; opacity: 1; }
</style>
