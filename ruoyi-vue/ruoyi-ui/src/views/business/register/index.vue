<template>
  <div class="app-container register-review-page">
    <div class="review-heading">
      <div>
        <div class="eyebrow">部门运营 / 注册审核</div>
        <h2>注册审核</h2>
        <p>核对实习生注册资料与岗位归属，审核时补充导师信息，审核通过后启用账号。</p>
      </div>
      <div class="scope-note">
        <i class="el-icon-lock" />
        <div>
          <span>数据范围</span>
          <strong>{{ scopeText }}</strong>
        </div>
      </div>
    </div>

    <el-row :gutter="14" class="summary-row">
      <el-col v-for="item in summaryCards" :key="item.key" :xs="12" :sm="6">
        <div class="summary-card" :class="item.tone">
          <div class="summary-icon"><i :class="item.icon" /></div>
          <div class="summary-content">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <small>{{ item.hint }}</small>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-card shadow="never" class="review-card">
      <div slot="header" class="card-header">
        <div>
          <span class="card-title">注册申请列表</span>
          
        </div>
        <el-button size="mini" icon="el-icon-refresh" @click="refreshAll">刷新</el-button>
      </div>

      <el-form
        ref="queryForm"
        :model="queryParams"
        size="small"
        :inline="true"
        class="query-form"
        @submit.native.prevent
      >
        <el-form-item label="姓名" prop="realName">
          <el-input
            v-model="queryParams.realName"
            placeholder="请输入姓名"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item label="登录账号" prop="loginAccount">
          <el-input
            v-model="queryParams.loginAccount"
            placeholder="手机号 / 账号"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item label="申请状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="全部状态" clearable>
            <el-option label="待审核" value="WAIT_AUDIT" />
            <el-option label="已驳回" value="REJECTED" />
            <el-option label="已通过" value="PASSED" />
          </el-select>
        </el-form-item>
        <el-form-item class="query-actions">
          <el-button type="primary" icon="el-icon-search" @click="handleQuery">查询</el-button>
          <el-button icon="el-icon-refresh-left" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-alert
        v-if="!isSuperScope"
        class="scope-alert"
        title="当前列表已按登录账号所属部门过滤，无法查看或处理其他部门申请。"
        type="info"
        :closable="false"
        show-icon
      />

      <el-table
        v-loading="loading"
        :data="registerList"
        stripe
        border
        :row-class-name="rowClassName"
        class="review-table"
        empty-text="暂无符合条件的注册申请"
      >
        <el-table-column label="申请人" min-width="150" fixed="left">
          <template slot-scope="scope">
            <div class="applicant-cell">
              <span class="avatar-mark">{{ nameInitial(scope.row.realName) }}</span>
              <div>
                <strong>{{ scope.row.realName }}</strong>
                <small>{{ scope.row.applicationNo }}</small>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="登录账号" prop="loginAccount" min-width="130" show-overflow-tooltip />
        <el-table-column label="身份证号" min-width="170">
          <template slot-scope="scope">{{ maskIdCard(scope.row.idCard) }}</template>
        </el-table-column>
        <el-table-column label="意向岗位 / 所属部门" min-width="180">
          <template slot-scope="scope">
            <div class="job-cell">
              <strong>{{ scope.row.positionName || '岗位缺失' }}</strong>
              <small><i class="el-icon-location-outline" /> {{ scope.row.deptName || '部门待配置' }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="预计入职" min-width="105">
          <template slot-scope="scope">{{ formatDate(scope.row.expectedEntryDate, 'yyyy-MM-dd') || '未填写' }}</template>
        </el-table-column>
        <el-table-column label="申请状态" min-width="92">
          <template slot-scope="scope">
            <el-tag :type="statusTagType(scope.row.status)" size="mini" effect="plain">
              {{ statusLabel(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="账号状态 / 业务身份" min-width="150">
          <template slot-scope="scope">
            <div class="stack-cell">
              <strong :class="isEnabled(scope.row.accountStatus) ? 'text-success' : 'text-warning'">
                {{ accountStatusLabel(scope.row.accountStatus) }}
              </strong>
              <small>{{ userStatusLabel(scope.row.userStatus) }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="导师（实习生管理页分配）" min-width="170">
          <template slot-scope="scope">
            <div class="stack-cell">
              <strong>{{ scope.row.mentorName || '待分配' }}</strong>
              <small>{{ scope.row.mentorPhone || '待分配' }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="审核情况" min-width="205">
          <template slot-scope="scope">
            <div class="audit-cell">
              <span>共提交 <b>{{ scope.row.auditCount || 0 }}</b> 次</span>
              <small class="muted-line">
                最近更新 {{ formatDate(scope.row.updateTime, 'yyyy-MM-dd HH:mm') || '—' }}
              </small>
              <small :class="auditHintTone(scope.row)">{{ auditHint(scope.row) }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="驳回原因" min-width="175" show-overflow-tooltip>
          <template slot-scope="scope">
            <span v-if="scope.row.rejectReason">{{ scope.row.rejectReason }}</span>
            <span v-else class="muted-text">—</span>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" min-width="145">
          <template slot-scope="scope">{{ formatDate(scope.row.createTime, 'yyyy-MM-dd HH:mm') }}</template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="150" align="center">
          <template slot-scope="scope">
            <template v-if="scope.row.status === 'WAIT_AUDIT'">
              <el-button
                type="text"
                size="mini"
                icon="el-icon-s-check"
                @click="handleAudit(scope.row, 'PASS')"
                v-hasPermi="['business:register:audit']"
              >通过</el-button>
              <el-button
                type="text"
                size="mini"
                class="danger-text"
                icon="el-icon-close"
                @click="handleAudit(scope.row, 'REJECT')"
                v-hasPermi="['business:register:audit']"
              >驳回</el-button>
            </template>
            <span v-else class="muted-text">已处理</span>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="total > 0"
        :total="total"
        :page.sync="queryParams.pageNum"
        :limit.sync="queryParams.pageSize"
        @pagination="getList"
      />
    </el-card>

    <el-dialog
      title="审核注册申请"
      :visible.sync="auditOpen"
      width="540px"
      append-to-body
      class="audit-dialog"
    >
      <div class="audit-person">
        <span class="avatar-mark large">{{ nameInitial(currentRow.realName) }}</span>
        <div>
          <strong>{{ currentRow.realName }}</strong>
          <span>{{ currentRow.loginAccount }} · {{ currentRow.positionName }} · {{ currentRow.deptName }}</span>
        </div>
      </div>
      <el-alert
        v-if="auditForm.action === 'PASS'"
        title="通过后账号自动启用并进入「预备实习」。导师不再在这里填写 —— 请到「实习生管理」页从导师库中选。"
        type="success"
        :closable="false"
        show-icon
      />
      <el-alert
        v-else
        title="驳回后账号保持停用，申请人可根据驳回原因修改资料后重新提交。"
        type="warning"
        :closable="false"
        show-icon
      />
      <el-form ref="auditForm" :model="auditForm" label-width="82px" class="audit-form">
        <el-form-item label="审核动作">
          <el-radio-group v-model="auditForm.action" size="small" class="audit-action" @change="handleActionChange">
            <el-radio-button label="PASS">通过申请</el-radio-button>
            <el-radio-button label="REJECT">驳回申请</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="auditForm.action === 'REJECT'" label="驳回原因" required>
          <el-input
            v-model="auditForm.reason"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请填写申请人需要补充或修改的内容"
          />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="auditOpen = false">取消</el-button>
        <el-button :type="auditForm.action === 'PASS' ? 'primary' : 'danger'" :loading="auditSubmitting" @click="submitAudit">
          {{ auditForm.action === 'PASS' ? '确认通过并启用账号' : '确认驳回' }}
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listRegister, auditRegister, getRegisterSummary } from '@/api/business/register'

export default {
  name: 'RegisterApplication',
  data() {
    return {
      loading: false,
      auditSubmitting: false,
      auditOpen: false,
      total: 0,
      registerList: [],
      currentRow: {},
      summary: {
        totalCount: 0,
        pendingCount: 0,
        passedCount: 0,
        rejectedCount: 0
      },
      auditForm: {
        id: undefined,
        action: 'PASS',
        reason: ''
      },
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        realName: undefined,
        loginAccount: undefined,
        status: undefined
      }
    }
  },
  computed: {
    isSuperScope() {
      const roles = this.$store.getters.roles || []
      return roles.indexOf('SUPER_ADMIN') > -1 || roles.indexOf('admin') > -1
    },
    scopeText() {
      return this.isSuperScope ? '全局申请' : '当前部门申请'
    },
    summaryCards() {
      return [
        { key: 'pending', label: '待审核', value: this.summary.pendingCount, hint: '需要及时处理', tone: 'orange', icon: 'el-icon-time' },
        { key: 'passed', label: '已通过', value: this.summary.passedCount, hint: '账号已启用', tone: 'green', icon: 'el-icon-success' },
        { key: 'rejected', label: '已驳回', value: this.summary.rejectedCount, hint: '等待重新提交', tone: 'red', icon: 'el-icon-warning-outline' },
        { key: 'total', label: '申请总数', value: this.summary.totalCount, hint: this.isSuperScope ? '全部部门' : '本部门累计', tone: 'blue', icon: 'el-icon-document' }
      ]
    }
  },
  created() {
    this.refreshAll()
  },
  methods: {
    refreshAll() {
      this.getList()
      this.getSummary()
    },
    getList() {
      this.loading = true
      listRegister(this.queryParams).then(response => {
        this.registerList = response.rows || []
        this.total = response.total || 0
      }).finally(() => {
        this.loading = false
      })
    },
    getSummary() {
      getRegisterSummary().then(response => {
        const data = response.data || {}
        this.summary = {
          totalCount: this.summaryValue(data, 'totalCount', 'total_count'),
          pendingCount: this.summaryValue(data, 'pendingCount', 'pending_count'),
          passedCount: this.summaryValue(data, 'passedCount', 'passed_count'),
          rejectedCount: this.summaryValue(data, 'rejectedCount', 'rejected_count')
        }
      })
    },
    summaryValue(data, camelKey, snakeKey) {
      const value = data[camelKey] !== undefined ? data[camelKey] : data[snakeKey]
      return Number(value || 0)
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.$refs.queryForm.resetFields()
      this.handleQuery()
    },
    handleAudit(row, action) {
      this.currentRow = Object.assign({}, row)
      this.auditForm = {
        id: row.id,
        action: action,
        reason: ''
      }
      this.auditOpen = true
    },
    handleActionChange(action) {
      if (action === 'PASS') {
        this.auditForm.reason = ''
      }
    },
    submitAudit() {
      if (this.auditForm.action === 'REJECT' && !this.auditForm.reason.trim()) {
        this.$modal.msgWarning('驳回必须填写原因')
        return
      }
      // ★ 2026-09-23：通过申请不再需要填导师（导师改到「实习生管理」页从导师库中选）
      this.auditSubmitting = true
      auditRegister(this.auditForm).then(() => {
        this.$modal.msgSuccess(this.auditForm.action === 'PASS' ? '审核通过，账号已启用；请到「实习生管理」页分配导师' : '申请已驳回')
        this.auditOpen = false
        this.refreshAll()
      }).finally(() => {
        this.auditSubmitting = false
      })
    },
    /** 行高亮：待审核的行加浅底色，便于一眼扫到 */
    rowClassName({ row }) {
      return row.status === 'WAIT_AUDIT' ? 'row-pending' : ''
    },
    /** 审核情况（由列表已有字段派生，不再单独请求审核记录） */
    auditHint(row) {
      if (row.status === 'WAIT_AUDIT') {
        return Number(row.auditCount || 0) > 1 ? '前次被驳回，待重新审核' : '等待部门管理员处理'
      }
      if (row.status === 'PASSED') {
        return row.mentorName ? '已通过并启用账号' : '已通过，待分配导师'
      }
      if (row.status === 'REJECTED') {
        return '已驳回，待申请人修改后重提'
      }
      return '—'
    },
    auditHintTone(row) {
      if (row.status === 'PASSED') return 'tone-success'
      if (row.status === 'REJECTED') return 'tone-danger'
      if (row.status === 'WAIT_AUDIT') return 'tone-warning'
      return 'muted-line'
    },
    nameInitial(name) {
      return (name || '?').slice(0, 1)
    },
    maskIdCard(idCard) {
      if (!idCard) return '-'
      if (idCard.length < 10) return idCard
      return idCard.slice(0, 6) + '********' + idCard.slice(-4)
    },
    statusLabel(status) {
      const map = { WAIT_AUDIT: '待审核', REJECTED: '已驳回', PASSED: '已通过' }
      return map[status] || status || '-'
    },
    statusTagType(status) {
      const map = { WAIT_AUDIT: 'warning', REJECTED: 'danger', PASSED: 'success' }
      return map[status] || 'info'
    },
    accountStatusLabel(status) {
      if (this.isEnabled(status)) return '已启用'
      if (status === 1 || status === '1') return '已停用'
      return status ? status : '未知'
    },
    isEnabled(status) {
      return status === 0 || status === '0'
    },
    formatDate(value, pattern) {
      if (!value) return ''
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return value
      const pad = number => String(number).padStart(2, '0')
      const replacements = {
        yyyy: date.getFullYear(),
        MM: pad(date.getMonth() + 1),
        dd: pad(date.getDate()),
        HH: pad(date.getHours()),
        mm: pad(date.getMinutes()),
        ss: pad(date.getSeconds())
      }
      return pattern.replace(/yyyy|MM|dd|HH|mm|ss/g, token => replacements[token])
    },
    userStatusLabel(status) {
      const map = {
        WAIT_AUDIT: '待审核',
        REJECTED: '已驳回',
        PRE_TRAINEE: '预备实习生',
        FORMAL_TRAINEE: '正式实习生'
      }
      return map[status] || status || '待审核'
    }
  }
}
</script>

<style lang="scss" scoped>
.register-review-page {
  color: #283544;
  background: #f6f8fb;
}

.review-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin: 2px 0 18px;
}

.eyebrow {
  margin-bottom: 7px;
  color: #3d82c7;
  font-size: 12px;
}

.review-heading h2 {
  margin: 0 0 8px;
  color: #243447;
  font-size: 24px;
  font-weight: 600;
}

.review-heading p {
  margin: 0;
  color: #748092;
  font-size: 13px;
}

.scope-note {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 150px;
  padding: 10px 14px;
  border: 1px solid #dfe8f2;
  background: #fff;
  color: #3d82c7;
}

.scope-note > i {
  font-size: 20px;
}

.scope-note span,
.scope-note strong {
  display: block;
}

.scope-note span {
  margin-bottom: 3px;
  color: #8995a5;
  font-size: 12px;
}

.scope-note strong {
  color: #33465b;
  font-size: 13px;
  font-weight: 600;
}

.summary-row {
  margin-bottom: 16px;
}

.summary-row .el-col {
  margin-bottom: 10px;
}

.summary-card {
  display: flex;
  align-items: center;
  min-height: 94px;
  padding: 17px 16px;
  border: 1px solid #e5ebf2;
  border-left: 4px solid #3d82c7;
  background: #fff;
}

.summary-card.orange { border-left-color: #d2872f; }
.summary-card.green { border-left-color: #2f9b79; }
.summary-card.red { border-left-color: #cd5b5b; }

.summary-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  margin-right: 12px;
  border-radius: 50%;
  background: #edf4ff;
  color: #3d82c7;
  font-size: 19px;
}

.orange .summary-icon { background: #fff5e8; color: #d2872f; }
.green .summary-icon { background: #edf9f3; color: #2f9b79; }
.red .summary-icon { background: #fff0f0; color: #cd5b5b; }

.summary-content span,
.summary-content small {
  display: block;
  color: #7c8896;
}

.summary-content span {
  font-size: 12px;
}

.summary-content strong {
  display: block;
  margin: 3px 0;
  color: #26384c;
  font-size: 24px;
  font-weight: 600;
  line-height: 1;
}

.summary-content small {
  font-size: 11px;
}

.review-card {
  border: 1px solid #e3e9f0;
  border-radius: 4px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  color: #2f4052;
  font-size: 16px;
  font-weight: 600;
}

.card-subtitle {
  margin-left: 12px;
  color: #8a96a4;
  font-size: 12px;
}

.query-form {
  padding-bottom: 4px;
  border-bottom: 1px solid #edf0f4;
}

.query-actions {
  margin-left: 4px;
}

.scope-alert {
  margin: 14px 0;
}

.review-table {
  margin-top: 14px;
}

.applicant-cell,
.job-cell {
  display: flex;
  align-items: center;
}

.applicant-cell > div,
.job-cell {
  min-width: 0;
}

.applicant-cell strong,
.applicant-cell small,
.job-cell strong,
.job-cell small {
  display: block;
}

.applicant-cell strong,
.job-cell strong {
  color: #33465b;
  font-weight: 600;
}

.applicant-cell small,
.job-cell small {
  margin-top: 4px;
  overflow: hidden;
  color: #8a96a4;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.job-cell small i {
  margin-right: 3px;
  color: #3d82c7;
}

.avatar-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 30px;
  height: 30px;
  margin-right: 9px;
  border-radius: 50%;
  background: #e8f1fc;
  color: #3472ad;
  font-size: 13px;
  font-weight: 600;
}

/* ---- 展平后的表格单元格 ---- */
.stack-cell strong,
.stack-cell small,
.audit-cell span,
.audit-cell small {
  display: block;
}

.stack-cell strong {
  color: #33465b;
  font-weight: 600;
}

.stack-cell small {
  margin-top: 4px;
  overflow: hidden;
  color: #8a96a4;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audit-cell span {
  color: #52637a;
  font-size: 12px;
}

.audit-cell span b {
  color: #2f4052;
  font-size: 13px;
}

.audit-cell small {
  margin-top: 3px;
  font-size: 11px;
  line-height: 1.5;
}

.muted-line { color: #9aa5b2; }
.muted-text { color: #b3bcc7; }

.danger-text { color: #cd5b5b; }

.tone-success { color: #2f9b79; }
.tone-danger { color: #cd5b5b; }
.tone-warning { color: #d2872f; }

.review-table ::v-deep .row-pending td {
  background: #fffaf1 !important;
}

.review-table ::v-deep th {
  background: #f7f9fc;
  color: #52637a;
  font-weight: 600;
}

/* ---- 审核弹窗头部 ---- */
.avatar-mark.large {
  width: 42px;
  height: 42px;
  font-size: 17px;
}

.audit-person {
  display: flex;
  align-items: center;
  margin-bottom: 18px;
}

.audit-person strong,
.audit-person span {
  display: block;
}

.audit-person strong {
  margin-bottom: 4px;
  color: #2e4052;
  font-size: 16px;
}

.audit-person span {
  color: #8490a0;
  font-size: 12px;
}

.audit-form {
  margin-top: 18px;
}

.audit-action {
  display: flex;
}

.audit-action ::v-deep .el-radio-button {
  flex: 1;
}

.audit-action ::v-deep .el-radio-button__inner {
  width: 100%;
}

.text-success { color: #2f9b79 !important; }
.text-warning { color: #d2872f !important; }

@media (max-width: 900px) {
  .card-subtitle {
    display: block;
    margin: 5px 0 0;
  }
}

@media (max-width: 767px) {
  .review-heading {
    display: block;
  }

  .scope-note {
    margin-top: 14px;
  }

  .query-actions {
    margin-left: 0;
  }
}
</style>
