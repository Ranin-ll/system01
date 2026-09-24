<template>
  <div v-loading="loading" class="dept-page">
    <div class="dept-breadcrumb">
      人员管理 <span>/</span> <b>实习生管理</b>
    </div>

    <div class="dept-heading">
      <div>
        <span class="eyebrow">DEPARTMENT ADMIN</span>
        <h1>实习生管理</h1>
        <p>本部门实习生花名册与统计下钻。</p>
      </div>
      <div class="dept-heading-actions">
        <el-button size="small" @click="notReady('导出花名册')">导出</el-button>
      </div>
    </div>

    <!-- 统计卡 -->
    <div class="dkpi-grid" style="margin-bottom:16px">
      <div v-for="item in kpis" :key="item.key" class="dkpi">
        <div class="dkpi-label">
          <span class="d" :style="{ background: item.color }" />
          {{ item.label }}
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
            <th>保密协议</th>
            <th>学习完成率</th>
            <th>正式考核次数</th>
            <th>最近活跃</th>
            <th style="width:210px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in pagedRows" :key="row.userId">
            <td>
              <span class="strong">{{ row.nickName }}</span>
              <div v-if="row.userName" class="sub-account">{{ row.userName }}</div>
            </td>
            <td>{{ row.positionName || '—' }}</td>
            <td>
              <span class="dbadge" :class="statusMeta(row.stage).tone">{{ statusMeta(row.stage).text }}</span>
              <div v-if="row.statusConflict" class="sub-account warn-txt">角色已发 · 状态待修正</div>
            </td>
            <td>
              <span v-if="row.mentorName" class="strong-sm">{{ row.mentorName }}</span>
              <span v-else class="muted">未分配</span>
              <div v-if="row.mentorPhone" class="sub-account">{{ row.mentorPhone }}</div>
            </td>
            <!-- 保密协议（sys_user.protocol_status，真数据） -->
            <td>
              <span class="dbadge" :class="row.protocolSigned === 1 ? 'green' : 'orange'">
                {{ row.protocolSigned === 1 ? '已签' : '未签' }}
              </span>
            </td>
            <!-- 学习完成率：本人学习项进度均值；无记录 → 「暂无课程」，不显示 0% -->
            <td>
              <template v-if="row.learnTotal">
                <span class="strong-sm">{{ row.learnAvgProgress }}%</span>
                <div class="sub-account">达标 {{ row.learnDone }}/{{ row.learnTotal }} 项</div>
              </template>
              <span v-else class="muted" title="该实习生尚无任何学习记录">暂无课程</span>
            </td>
            <!-- 正式考核：★ 主值展示【参加次数】；有结果时下带一行通过与否 -->
            <td>
              <template v-if="row.formalTimes">
                <span class="strong-sm">{{ row.formalTimes }} 次</span>
                <div class="sub-account" :class="row.formalPassed === 1 ? 'ok-txt' : 'warn-txt'">
                  {{ row.formalPassed === 1 ? '已通过' : '未通过' }}
                </div>
              </template>
              <span v-else class="muted">未参加</span>
            </td>
            <td class="muted">{{ fmtDate(row.loginDate || row.createTime) }}</td>
            <td>
              <div class="acts">
                <el-button type="text" @click="openProfile(row)">查看档案</el-button>
                <span class="sep">|</span>
                <el-button v-if="row.userStatus === 'PENDING_PROMOTE'" type="text" @click="goPromotion(row)">审核转正</el-button>
                <el-button v-else-if="!row.mentorName" type="text" @click="notReady('分配导师')">分配导师</el-button>
              </div>
            </td>
          </tr>
          <tr v-if="!filteredRows.length && !loading">
            <td colspan="9">
              <div class="dempty small">
                <i class="el-icon-user" />
                <strong>没有符合条件的实习生</strong>
                <span>试着放宽筛选条件；本页只显示当前部门名下的实习生与注册待审人员。</span>
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
  </div>
</template>

<script>
// ★ 2026-09-23：花名册数据源由 `register_application` 换成**培养分析聚合接口**
//   （GET /business/super/analysis/stage-progress，权限 business:bank:list，部门管理员持有）。
//   原因：① 原口径只取「注册已通过」，与后端聚合口径（sys_user + 实习生角色）对不上
//          —— 部门端只有 1 人 vs 实际 7 人；
//        ② 「保密协议 / 学习完成率 / 正式考核」这几列的数据本来就在
//           后端这一份聚合里（protocolSigned / learnTotal+learnDone+learnAvgProgress /
//           formalPassed），不必再另做接口。
//   该接口**不需要传部门参数**，范围从 token 取（超管=全部、部门管理员=本部门）。
import { getStageProgress } from '@/api/business/analysis'
import { getMentorOptions, assignInternMentor, clearInternMentor } from '@/api/business/mentor'

// ★ 键用后端归一后的 `stage`（以角色为主、user_status 为辅），不再用原始 userStatus
const STATUS_MAP = {
  WAIT_AUDIT: { text: '注册待审', tone: 'orange' },
  PRE_TRAINEE: { text: '预备实习中', tone: 'blue' },
  PENDING_PROMOTE: { text: '转正审核中', tone: 'purple' },
  FORMAL_TRAINEE: { text: '已转正', tone: 'green' },
  DISABLED: { text: '已停用', tone: 'gray' },
  ARCHIVED: { text: '已归档', tone: 'gray' }
}

export default {
  name: 'DeptStudents',
  data() {
    return {
      loading: false,
      roster: [],
      mentorDialog: {
        visible: false,
        submitting: false,
        optionsLoading: false,
        row: null,
        mentorId: undefined,
        options: []
      },
      query: { positionName: '', mentorName: '', status: 'ALL', entryRange: 'ALL', keyword: '', pageNum: 1, pageSize: 20 }
    }
  },
  computed: {
    /**
     * 花名册 = 后端培养分析聚合的逐人明细（与「培养状态 / 转正 gate」同一份口径）。
     * ★ 2026-09-23 起**不再**过滤掉注册待审的人 —— 他们照样在本部门名下，
     *   由「培养状态」列标出「注册待审」，避免两个页面人数对不上。
     */
    interns() {
      return this.roster
    },
    kpis() {
      const by = key => this.interns.filter(r => r.stage === key).length
      const waiting = by('WAIT_AUDIT')
      const pre = by('PRE_TRAINEE')
      const promoting = by('PENDING_PROMOTE')
      const formal = by('FORMAL_TRAINEE')
      return [
        { key: 'all', label: '本部门人员', value: this.interns.length, hint: '含注册待审 ' + waiting + ' 人', color: '#1764f5' },
        { key: 'pre', label: '预备实习中', value: pre, hint: 'PRE_TRAINEE', color: '#1764f5' },
        { key: 'promoting', label: '转正审核中', value: promoting, hint: '已推荐待终审', tone: 'warn', color: '#f79009' },
        { key: 'formal', label: '已转正', value: formal, hint: '转正即生效并自动发证', tone: 'ok', color: '#12b76a' }
      ]
    },
    positionOptions() {
      return this.uniq(this.interns.map(r => r.positionName))
    },
    mentorOptions() {
      return this.uniq(this.interns.map(r => r.mentorName))
    },
    statusTabs() {
      const count = key => (key === 'ALL' ? this.interns.length : this.interns.filter(r => r.stage === key).length)
      return [
        { key: 'ALL', label: '全部', count: count('ALL') },
        { key: 'WAIT_AUDIT', label: '注册待审', count: count('WAIT_AUDIT') },
        { key: 'PRE_TRAINEE', label: '预备', count: count('PRE_TRAINEE') },
        { key: 'PENDING_PROMOTE', label: '审核中', count: count('PENDING_PROMOTE') },
        { key: 'FORMAL_TRAINEE', label: '已转正', count: count('FORMAL_TRAINEE') }
      ]
    },
    filteredRows() {
      const q = this.query
      const kw = (q.keyword || '').trim().toLowerCase()
      return this.interns.filter(row => {
        if (q.status !== 'ALL' && row.stage !== q.status) return false
        if (q.positionName && row.positionName !== q.positionName) return false
        if (q.mentorName && row.mentorName !== q.mentorName) return false
        if (kw) {
          const hay = ((row.nickName || '') + ' ' + (row.userName || '')).toLowerCase()
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
      // 培养分析聚合接口：一次拿到全部门逐人明细（含协议 / 学习 / 模拟 / 正式考核）
      getStageProgress().then(res => {
        const d = (res && res.data) || {}
        this.roster = d.rows || []
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
    notReady(action) {
      this.$message({ message: '「' + action + '」功能暂未开放', type: 'warning' })
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
.dtbl .strong-sm { font-weight: 600; }
.dtbl .warn-txt { color: #b54708; }
.dtbl .ok-txt { color: #027a48; }
.mentor-head { margin-bottom: 14px; color: #344054; }
.mentor-head .muted { color: #98a2b3; font-size: 12px; }
.field-tip { margin-top: 3px; color: #98a2b3; font-size: 11px; }
.dkpi-val small { margin-left: 2px; color: #667085; font-size: 12px; font-weight: 400; }
code { padding: 1px 5px; color: #344054; font-size: 11.5px; background: #f2f4f7; border-radius: 4px; }
</style>
