<template>
  <div v-loading="loading" class="dept-page">
    <div class="dept-breadcrumb">
      人员管理 <span>/</span> <b>实习生管理</b>
    </div>

    <div class="dept-heading">
      <div>
        <span class="eyebrow">DEPARTMENT ADMIN</span>
        <h1>实习生管理</h1>
        <p>本部门实习生花名册与统计下钻。花名册基础信息、培养状态、导师均为真实数据。</p>
      </div>
      <div class="dept-heading-actions">
        <el-button size="small" @click="notReady('导出花名册')">导出</el-button>
      </div>
    </div>

    <!-- 统计卡：前 3 项真实，最后 1 项依赖 certificate -->
    <div class="dkpi-grid" style="margin-bottom:16px">
      <div v-for="item in kpis" :key="item.key" class="dkpi">
        <div class="dkpi-label">
          <span class="d" :style="{ background: item.color }" />
          {{ item.label }}
          <span v-if="item.sample" class="dsample">示例</span>
        </div>
        <div class="dkpi-val">{{ item.value }}<small>人</small></div>
        <div class="dkpi-sub" :class="item.tone">{{ item.hint }}</div>
      </div>
    </div>

    <div class="dsec">
      <!-- 筛选条 -->
      <div class="dfilter">
        <el-select v-model="query.positionName" size="small" placeholder="岗位：全部" clearable style="width:170px">
          <el-option v-for="p in positionOptions" :key="p" :label="p" :value="p" />
        </el-select>

        <div class="dchips" style="margin:0">
          <span
            v-for="tab in statusTabs"
            :key="tab.key"
            class="dchip"
            :class="{ on: query.status === tab.key }"
            @click="query.status = tab.key"
          >
            {{ tab.label }} <span class="n">{{ tab.count }}</span>
          </span>
        </div>

        <el-select v-model="query.mentorName" size="small" placeholder="导师：全部" clearable style="width:150px">
          <el-option v-for="m in mentorOptions" :key="m" :label="m" :value="m" />
        </el-select>

        <el-select v-model="query.entryRange" size="small" placeholder="入职时间" style="width:140px">
          <el-option label="全部" value="ALL" />
          <el-option label="近 30 天" value="D30" />
          <el-option label="近 90 天" value="D90" />
          <el-option label="更早" value="EARLIER" />
        </el-select>

        <el-input v-model="query.keyword" size="small" placeholder="姓名 / 账号" clearable prefix-icon="el-icon-search" style="width:180px" />

        <span class="grow" />
        <el-button size="small" type="primary" plain @click="notReady('批量发通知')">批量发通知</el-button>
      </div>

      <table class="dtbl">
        <thead>
          <tr>
            <th>姓名</th>
            <th>岗位</th>
            <th>培养状态</th>
            <th>导师</th>
            <th>保密协议 <span class="dsample">示例</span></th>
            <th>学习完成率 <span class="dsample">示例</span></th>
            <th>模拟正确率 <span class="dsample">示例</span></th>
            <th>正式考核 <span class="dsample">示例</span></th>
            <th>最近活跃</th>
            <th style="width:170px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in pagedRows" :key="row.id">
            <td>
              <span class="strong">{{ row.realName }}</span>
              <div v-if="row.loginAccount" class="sub-account">{{ row.loginAccount }}</div>
            </td>
            <td>{{ row.positionName || '—' }}</td>
            <td>
              <span class="dbadge" :class="statusMeta(row.userStatus).tone">{{ statusMeta(row.userStatus).text }}</span>
            </td>
            <td>
              <span v-if="row.mentorName">{{ row.mentorName }}</span>
              <span v-else class="muted">未分配</span>
            </td>
            <td>
              <span class="dbadge" :class="row.demo.protocol ? 'green' : 'red'">
                {{ row.demo.protocol ? '已签' : '未签' }}
              </span>
            </td>
            <td>{{ row.demo.studyRate }}%</td>
            <td>{{ row.demo.practiceRate }}%</td>
            <td>
              <span class="dbadge" :class="examTone(row.demo.examKey)">{{ row.demo.examText }}</span>
            </td>
            <td class="muted">{{ fmtDate(row.updateTime || row.createTime) }}</td>
            <td>
              <div class="acts">
                <el-button type="text" @click="openProfile(row)">查看档案</el-button>
                <span class="sep">|</span>
                <el-button v-if="row.userStatus === 'PENDING_PROMOTE'" type="text" @click="goPromotion(row)">审核转正</el-button>
                <el-button v-else-if="!row.mentorName" type="text" @click="notReady('分配导师')">分配导师</el-button>
                <el-button v-else type="text" @click="sendTask(row)">发任务</el-button>
              </div>
            </td>
          </tr>
          <tr v-if="!filteredRows.length && !loading">
            <td colspan="10">
              <div class="dempty small">
                <i class="el-icon-user" />
                <strong>没有符合条件的实习生</strong>
                <span>试着放宽筛选条件，或前往「注册审核」先通过注册申请。</span>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- 分页 -->
      <div v-if="filteredRows.length" class="dpager">
        <span class="muted">共 {{ filteredRows.length }} 条 · 每页 {{ query.pageSize }} 条</span>
        <span class="grow" />
        <el-button size="mini" :disabled="query.pageNum <= 1" @click="query.pageNum--">上一页</el-button>
        <el-button size="mini" type="primary">{{ query.pageNum }}</el-button>
        <el-button size="mini" :disabled="query.pageNum >= pageCount" @click="query.pageNum++">下一页</el-button>
      </div>
    </div>

    <!-- 数据说明 -->
    <div class="dgrid" style="margin-top:16px">
      <div class="dcallout ok c6" style="margin:0">
        <i class="el-icon-success" />
        <span>
          表格里「姓名 / 岗位 / 培养状态 / 导师 / 最近活跃」<b>已接真实接口</b>（<code>register_application</code> + <code>sys_user</code>）。
          带「示例」标记的 4 列需要后端出批量聚合接口，人数多时尤其必要。
        </span>
      </div>
      <div class="dcallout warn c6" style="margin:0">
        <i class="el-icon-warning-outline" />
        <span>
          4 个统计卡中，前 3 个来自 <code>sys_user.user_status</code> <b>可真实</b>；
          「已发证」需要 <code>certificate</code> 表有 Java 层，<b>本期先用示例数据并打标记</b>。
        </span>
      </div>
    </div>
  </div>
</template>

<script>
import { listRegister } from '@/api/business/register'

const STATUS_MAP = {
  WAIT_AUDIT: { text: '注册待审', tone: 'orange' },
  PRE_TRAINEE: { text: '预备实习中', tone: 'blue' },
  PENDING_PROMOTE: { text: '转正审核中', tone: 'purple' },
  FORMAL_TRAINEE: { text: '已转正', tone: 'green' },
  DISABLED: { text: '已停用', tone: 'gray' },
  ARCHIVED: { text: '已归档', tone: 'gray' }
}

/** 无聚合接口的列：按行 ID 生成稳定的演示值（不随渲染变化） */
function demoOf(id) {
  const seed = (id || 1) * 9301 % 233280
  const r1 = Math.round((seed / 233280) * 60) + 35
  const r2 = Math.round(((seed * 7) % 233280) / 233280 * 55) + 40
  const pick = (seed * 13) % 3
  const exams = [
    { examKey: 'PASS', examText: '已通过' },
    { examKey: 'PENDING', examText: '待批阅' },
    { examKey: 'NONE', examText: '未参加' }
  ]
  return {
    protocol: seed % 2 === 0,
    studyRate: Math.min(r1, 100),
    practiceRate: Math.min(r2, 100),
    examKey: exams[pick].examKey,
    examText: exams[pick].examText
  }
}

export default {
  name: 'DeptStudents',
  data() {
    return {
      loading: false,
      roster: [],
      query: { positionName: '', mentorName: '', status: 'ALL', entryRange: 'ALL', keyword: '', pageNum: 1, pageSize: 20 }
    }
  },
  computed: {
    /** 花名册 = 注册申请已通过的人（注册待审归「注册审核」页处理） */
    interns() {
      return this.roster
        .filter(row => row.status === 'PASSED')
        .map(row => Object.assign({}, row, { demo: demoOf(row.id) }))
    },
    kpis() {
      const by = key => this.interns.filter(r => r.userStatus === key).length
      const pre = by('PRE_TRAINEE')
      const promoting = by('PENDING_PROMOTE')
      const formal = by('FORMAL_TRAINEE')
      return [
        { key: 'all', label: '在册实习生', value: this.interns.length, hint: '本部门 ' + this.positionOptions.length + ' 个岗位', color: '#1764f5' },
        { key: 'pre', label: '预备实习中', value: pre, hint: 'PRE_TRAINEE', color: '#1764f5' },
        { key: 'promoting', label: '转正审核中', value: promoting, hint: '已推荐待终审', tone: 'warn', color: '#f79009' },
        { key: 'formal', label: '已转正', value: formal, hint: '转正即生效并自动发证', tone: 'ok', color: '#12b76a', sample: true }
      ]
    },
    positionOptions() {
      return this.uniq(this.interns.map(r => r.positionName))
    },
    mentorOptions() {
      return this.uniq(this.interns.map(r => r.mentorName))
    },
    statusTabs() {
      const count = key => (key === 'ALL' ? this.interns.length : this.interns.filter(r => r.userStatus === key).length)
      return [
        { key: 'ALL', label: '全部', count: count('ALL') },
        { key: 'PRE_TRAINEE', label: '预备', count: count('PRE_TRAINEE') },
        { key: 'PENDING_PROMOTE', label: '审核中', count: count('PENDING_PROMOTE') },
        { key: 'FORMAL_TRAINEE', label: '已转正', count: count('FORMAL_TRAINEE') },
        { key: 'DISABLED', label: '已停用', count: count('DISABLED') }
      ]
    },
    filteredRows() {
      const q = this.query
      const kw = (q.keyword || '').trim().toLowerCase()
      return this.interns.filter(row => {
        if (q.status !== 'ALL' && row.userStatus !== q.status) return false
        if (q.positionName && row.positionName !== q.positionName) return false
        if (q.mentorName && row.mentorName !== q.mentorName) return false
        if (kw) {
          const hay = ((row.realName || '') + ' ' + (row.loginAccount || '')).toLowerCase()
          if (hay.indexOf(kw) < 0) return false
        }
        if (q.entryRange !== 'ALL') {
          const t = row.expectedEntryDate ? new Date(row.expectedEntryDate).getTime() : null
          if (!t) return q.entryRange === 'EARLIER'
          const days = (Date.now() - t) / 86400000
          if (q.entryRange === 'D30' && days > 30) return false
          if (q.entryRange === 'D90' && days > 90) return false
          if (q.entryRange === 'EARLIER' && days <= 90) return false
        }
        return true
      })
    },
    pageCount() {
      return Math.max(1, Math.ceil(this.filteredRows.length / this.query.pageSize))
    },
    pagedRows() {
      const start = (this.query.pageNum - 1) * this.query.pageSize
      return this.filteredRows.slice(start, start + this.query.pageSize)
    }
  },
  watch: {
    'query.pageSize'() {
      this.query.pageNum = 1
    },
    filteredRows() {
      if (this.query.pageNum > this.pageCount) this.query.pageNum = 1
    }
  },
  created() {
    this.loadRoster()
  },
  methods: {
    loadRoster() {
      this.loading = true
      listRegister({ pageNum: 1, pageSize: 200 }).then(res => {
        this.roster = (res && res.rows) || []
      }).catch(() => {
        this.roster = []
      }).finally(() => {
        this.loading = false
      })
    },
    uniq(list) {
      const seen = {}
      const out = []
      list.forEach(v => {
        if (v && !seen[v]) {
          seen[v] = 1
          out.push(v)
        }
      })
      return out
    },
    statusMeta(status) {
      return STATUS_MAP[status] || { text: status || '—', tone: 'gray' }
    },
    examTone(key) {
      return { PASS: 'green', PENDING: 'orange', NONE: 'gray' }[key] || 'gray'
    },
    fmtDate(value) {
      if (!value) return '—'
      const d = new Date(value)
      if (isNaN(d.getTime())) return '—'
      const p = n => (n < 10 ? '0' + n : '' + n)
      const today = new Date()
      if (d.toDateString() === today.toDateString()) return '今天 ' + p(d.getHours()) + ':' + p(d.getMinutes())
      const yest = new Date(today.getTime() - 86400000)
      if (d.toDateString() === yest.toDateString()) return '昨天 ' + p(d.getHours()) + ':' + p(d.getMinutes())
      return p(d.getMonth() + 1) + '-' + p(d.getDate())
    },
    openProfile(row) {
      this.$router.push('/department/people/profile/' + (row.userId || row.id))
    },
    goPromotion(row) {
      this.$router.push({ path: '/department/people/promotion', query: { id: String(row.userId || row.id) } })
    },
    sendTask(row) {
      this.$router.push({ path: '/department/messages/tasks', query: { to: row.realName || '' } })
    },
    notReady(action) {
      this.$message({ message: '「' + action + '」所需的接口尚未落地（见设计方案 §实施状态）', type: 'warning' })
    }
  }
}
</script>

<style lang="scss" scoped>
@import '~@/assets/styles/department-module.scss';

.dfilter { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; margin-bottom: 16px; }
.dfilter .grow { flex: 1; }
.dpager { display: flex; align-items: center; padding-top: 14px; }
.dpager .grow { flex: 1; }
.dtbl .muted { color: #98a2b3; }
.dtbl .sub-account { margin-top: 3px; color: #98a2b3; font-size: 11px; }
.dtbl .acts .sep { color: #d0d5dd; }
.dkpi-val small { margin-left: 2px; color: #667085; font-size: 12px; font-weight: 400; }
code { padding: 1px 5px; color: #344054; font-size: 11.5px; background: #f2f4f7; border-radius: 4px; }
</style>
