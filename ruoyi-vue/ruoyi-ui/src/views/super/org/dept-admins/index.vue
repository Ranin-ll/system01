<!--
  ⚠️ 已弃用（2026-09-20）—— 本页内容已并入「督办看板」/super/todo 的「按人」视角。

  为什么弃用：本页（各部门管理员的待办量 + 催办）与「督办看板」（按事项的四类待办 + 催办）
  用的是**同一份聚合**（页面自己的说明里也写着"两个页面的数字必然一致"），
  却被拆成两个页面挂在两个菜单组下 —— 用户得跑两处才能拼出完整判断。

  现状：
    · 侧栏不再列出本页（store/modules/permission.js 已移除 org/dept-admins）
    · 路由 `org/dept-admins` 保留并重定向到 /super/todo?view=people（旧书签不 404）
    · 文件保留不删除（同事分支可能引用），逻辑不再维护

  替代去处：views/super/todo/index.vue 的「按人」视角（复用同一份 listDeptAdmins 数据）。
-->
<template>
  <div class="da-page">
    <header class="da-head">
      <div>
        <span class="eyebrow">SUPER ADMIN · DEPT ADMINS</span>
        <h1>部门管理员</h1>
        <p>各部门管理员的待办量与一键催办。督办<b>只发通知提醒</b>，不代部门执行业务写操作。</p>
      </div>
      <div class="da-actions">
        <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button size="small" type="primary" :disabled="!selection.length" @click="batchUrge">
          批量催办<span v-if="selection.length">（{{ selection.length }}）</span>
        </el-button>
      </div>
    </header>

    <!-- 加载失败：与「确实没有」分开，给原因 + 重试 -->
    <div v-if="error" class="err-box">
      <i class="el-icon-warning-outline" />
      <div>
        <b>加载失败</b>
        <p>{{ error }}</p>
      </div>
      <el-button size="mini" type="primary" @click="load">重试</el-button>
    </div>

    <template v-else>
      <div class="kpis">
        <div class="kpi"><div class="lb"><i class="dot" />部门管理员</div><div class="vl">{{ rows.length }}<small>人</small></div><div class="ft">覆盖 {{ deptCount }} 个部门</div></div>
        <div class="kpi"><div class="lb"><i class="dot o" />有待办</div><div class="vl">{{ withTodo.length }}<small>人</small></div><div class="ft" :class="{warn: totalTodos > 0}">待办合计 {{ totalTodos }} 项</div></div>
        <div class="kpi sm"><div class="lb">待审报名</div><div class="vl">{{ sumOf('pendingRegisters') }}</div></div>
        <div class="kpi sm"><div class="lb">待批作业</div><div class="vl">{{ sumOf('pendingReviews') }}</div></div>
        <div class="kpi sm"><div class="lb">逾期任务</div><div class="vl">{{ sumOf('overdueTasks') }}</div></div>
        <div class="kpi sm"><div class="lb">学习停滞</div><div class="vl">{{ sumOf('stalledStudies') }}</div></div>
      </div>

      <section class="card">
        <div class="toolbar">
          <span class="seg">
            <span :class="{ on: tab === 'ALL' }" @click="tab = 'ALL'">全部（{{ rows.length }}）</span>
            <span :class="{ on: tab === 'TODO' }" @click="tab = 'TODO'">有待办（{{ withTodo.length }}）</span>
            <span :class="{ on: tab === 'DISABLED' }" @click="tab = 'DISABLED'">已停用（{{ disabled.length }}）</span>
          </span>
          <span class="spacer" />
          <el-input v-model="kw" size="mini" clearable placeholder="搜部门 / 姓名 / 账号" style="width:180px" />
        </div>

        <el-table v-loading="loading" :data="filtered" size="mini" style="width:100%" @selection-change="onSel">
          <el-table-column type="selection" width="40" />
          <el-table-column prop="deptName" label="部门" width="110" />
          <el-table-column prop="nickName" label="姓名" min-width="130" show-overflow-tooltip />
          <el-table-column prop="userName" label="账号" width="150" show-overflow-tooltip />
          <el-table-column label="状态" width="76" align="center">
            <template slot-scope="s">
              <span class="tag" :class="s.row.status === '0' ? 'green' : 'gray'">{{ s.row.status === '0' ? '正常' : '已停用' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="待审报名" width="82" align="center">
            <template slot-scope="s"><span :class="{ warn: s.row.pendingRegisters > 0 }">{{ s.row.pendingRegisters }}</span></template>
          </el-table-column>
          <el-table-column label="待批作业" width="82" align="center">
            <template slot-scope="s"><span :class="{ warn: s.row.pendingReviews > 0 }">{{ s.row.pendingReviews }}</span></template>
          </el-table-column>
          <el-table-column label="逾期任务" width="82" align="center">
            <template slot-scope="s"><span :class="{ warn: s.row.overdueTasks > 0 }">{{ s.row.overdueTasks }}</span></template>
          </el-table-column>
          <el-table-column label="学习停滞" width="82" align="center">
            <template slot-scope="s"><span :class="{ warn: s.row.stalledStudies > 0 }">{{ s.row.stalledStudies }}</span></template>
          </el-table-column>
          <el-table-column label="合计" width="64" align="center">
            <template slot-scope="s"><b>{{ s.row.totalTodos }}</b></template>
          </el-table-column>
          <el-table-column label="操作" width="96" align="center">
            <template slot-scope="s">
              <el-button size="mini" type="text" :disabled="!s.row.totalTodos || s.row.status !== '0'"
                         @click="urgeOne(s.row)">催办</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="!loading && !filtered.length" class="empty">
          {{ rows.length ? '当前筛选下没有部门管理员' : '暂无部门管理员' }}
        </div>
        <div class="note">
          待办口径与「督办看板」<b>同源</b>（同一份聚合），所以两个页面的数字必然一致。<br />
          催办复用已通的<b>定向通知</b>通道（<code>msgType=URGE</code>），<b>同一人每天最多被催一次</b>；已停用的管理员不可催办。
        </div>
      </section>
    </template>
  </div>
</template>

<script>
import { listDeptAdmins, urgeTodo } from '@/api/business/superTodo'

export default {
  name: 'SuperDeptAdmins',
  data() {
    return {
      loading: false,
      error: '',
      rows: [],
      selection: [],
      tab: 'ALL',
      kw: ''
    }
  },
  computed: {
    withTodo() {
      return this.rows.filter(r => (r.totalTodos || 0) > 0)
    },
    disabled() {
      return this.rows.filter(r => r.status !== '0')
    },
    deptCount() {
      const s = new Set()
      this.rows.forEach(r => { if (r.deptId) s.add(r.deptId) })
      return s.size
    },
    totalTodos() {
      return this.rows.reduce((n, r) => n + (r.totalTodos || 0), 0)
    },
    filtered() {
      let list = this.rows
      if (this.tab === 'TODO') list = list.filter(r => (r.totalTodos || 0) > 0)
      else if (this.tab === 'DISABLED') list = list.filter(r => r.status !== '0')
      const k = this.kw.trim().toLowerCase()
      if (k) {
        list = list.filter(r =>
          (r.deptName || '').toLowerCase().indexOf(k) > -1 ||
          (r.nickName || '').toLowerCase().indexOf(k) > -1 ||
          (r.userName || '').toLowerCase().indexOf(k) > -1)
      }
      return list
    }
  },
  created() {
    this.load()
  },
  methods: {
    load() {
      this.loading = true
      this.error = ''
      return listDeptAdmins().then(res => {
        this.rows = res.data || []
      }).catch(err => {
        // ★ 失败不能吞成空数组，否则用户会去查库、发现明明有数据
        this.rows = []
        this.error = (err && err.message) ? err.message : '请求失败，请检查网络或权限后重试'
      }).finally(() => { this.loading = false })
    },
    sumOf(field) {
      return this.rows.reduce((n, r) => n + (r[field] || 0), 0)
    },
    onSel(sel) {
      this.selection = sel
    },
    urgeOne(row) {
      this.doUrge([row])
    },
    batchUrge() {
      if (!this.selection.length) return
      this.doUrge(this.selection)
    },
    doUrge(rows) {
      const names = rows.map(r => r.deptName + '·' + r.nickName).join('、')
      this.$prompt('附加说明（可空，会拼在自动生成的待办清单之后）：', '催办 · ' + names, {
        inputType: 'textarea',
        inputPlaceholder: '如：本周五前请完成处理',
        confirmButtonText: '发送催办',
        cancelButtonText: '取消'
      }).then(({ value }) => {
        const ownerIds = rows.map(r => r.userId)
        return urgeTodo({ ownerIds: ownerIds, content: value || '' }).then(res => {
          const d = res.data || {}
          const details = d.details || []
          if ((d.sent || 0) > 0) {
            this.$message.success('已催办 ' + d.sent + ' 人' + ((d.skipped || 0) > 0 ? ('，跳过 ' + d.skipped + ' 人') : ''))
          } else {
            this.$message.warning(details.length ? details.join('；') : '没有可催办的对象')
          }
          this.load()
        })
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
/* 自包含样式（不 @import 共享基线，避免 scoped 下 :root 失效） */
.da-page { padding: 18px 20px 32px; background: #eef2f7; min-height: calc(100vh - 84px); }
.da-head {
  display: flex; align-items: flex-start; justify-content: space-between; gap: 16px;
  background: #fff; border: 1px solid #e4e9f0; border-radius: 12px; padding: 18px 20px; margin-bottom: 14px;
  .eyebrow { color: #98a2b3; font-size: 11px; letter-spacing: .6px; }
  h1 { margin: 4px 0 6px; font-size: 20px; color: #1d2939; }
  p { margin: 0; color: #667085; font-size: 12.5px; line-height: 1.7; max-width: 640px; }
  .da-actions { display: flex; gap: 8px; flex: none; }
}
.err-box {
  display: flex; align-items: center; gap: 12px; background: #feecea; border: 1px solid #f6c9c4;
  border-radius: 12px; padding: 14px 18px; color: #b42318; font-size: 13px;
  i { font-size: 20px; }
  b { display: block; margin-bottom: 2px; }
  p { margin: 0; color: #b42318; font-size: 12.5px; }
  .el-button { margin-left: auto; }
}
.kpis { display: grid; grid-template-columns: 2fr 2fr repeat(4, 1fr); gap: 12px; margin-bottom: 14px; }
@media (max-width: 1200px) { .kpis { grid-template-columns: repeat(3, 1fr); } }
.kpi {
  background: #fff; border: 1px solid #e4e9f0; border-radius: 10px; padding: 14px 16px;
  .lb { display: flex; align-items: center; gap: 6px; color: #667085; font-size: 12px; margin-bottom: 8px;
        .dot { width: 6px; height: 6px; border-radius: 50%; background: #1764f5; &.o { background: #f79009; } } }
  .vl { font-size: 24px; font-weight: 600; color: #1d2939; small { font-size: 12px; color: #98a2b3; margin-left: 2px; font-weight: 400; } }
  .ft { margin-top: 6px; font-size: 11.5px; color: #98a2b3; &.warn { color: #b54708; } }
  &.sm .vl { font-size: 20px; }
}
.card { background: #fff; border: 1px solid #e4e9f0; border-radius: 12px; padding: 16px 18px; }
.toolbar { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; flex-wrap: wrap; }
.seg {
  display: inline-flex; border: 1px solid #e4e9f0; border-radius: 8px; overflow: hidden;
  span { padding: 0 12px; height: 28px; line-height: 28px; font-size: 12px; color: #667085; cursor: pointer; }
  span.on { background: #e8f1fd; color: #1764f5; }
}
.spacer { flex: 1; }
.tag {
  display: inline-block; padding: 0 7px; height: 20px; line-height: 20px; border-radius: 5px; font-size: 11px;
  background: #f2f4f7; color: #475467;
  &.green { background: #e7f7ef; color: #027a48; }
  &.gray { background: #f2f4f7; color: #98a2b3; }
}
.warn { color: #b42318; font-weight: 600; }
.empty { padding: 40px 0; text-align: center; color: #98a2b3; font-size: 12.5px; }
.note { margin-top: 12px; color: #98a2b3; font-size: 11.5px; line-height: 1.7; }
</style>
