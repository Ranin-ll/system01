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
          <span class="card-subtitle">申请提交后自动写入数据库，部门管理员只能处理本部门申请</span>
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
        class="review-table"
        empty-text="暂无符合条件的注册申请"
      >
        <el-table-column label="申请人" min-width="170">
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
        <el-table-column label="登录账号" prop="loginAccount" min-width="135" />
        <el-table-column label="岗位 / 自动匹配部门" min-width="205">
          <template slot-scope="scope">
            <div class="job-cell">
              <strong>{{ scope.row.positionName || '-' }}</strong>
              <small><i class="el-icon-location-outline" /> {{ scope.row.deptName || '部门待配置' }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="预计入职" prop="expectedEntryDate" width="115">
          <template slot-scope="scope">{{ formatDate(scope.row.expectedEntryDate, 'yyyy-MM-dd') || '未填写' }}</template>
        </el-table-column>
        <el-table-column label="申请状态" width="100">
          <template slot-scope="scope">
            <el-tag :type="statusTagType(scope.row.status)" size="mini" effect="plain">
              {{ statusLabel(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="账号状态" width="100">
          <template slot-scope="scope">{{ accountStatusLabel(scope.row.accountStatus) }}</template>
        </el-table-column>
        <el-table-column label="提交时间" prop="createTime" min-width="155">
          <template slot-scope="scope">{{ formatDate(scope.row.createTime, 'yyyy-MM-dd HH:mm') }}</template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="190" align="center">
          <template slot-scope="scope">
            <el-button type="text" size="mini" icon="el-icon-view" @click="handleDetail(scope.row)">查看详情</el-button>
            <el-button
              v-if="scope.row.status === 'WAIT_AUDIT'"
              type="text"
              size="mini"
              icon="el-icon-s-check"
              @click="handleAudit(scope.row, 'PASS')"
              v-hasPermi="['business:register:audit']"
            >审核</el-button>
            <el-button
              v-else
              type="text"
              size="mini"
              icon="el-icon-document"
              @click="handleDetail(scope.row)"
            >查看记录</el-button>
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
        title="请补充导师姓名和联系方式。导师是实习生档案信息，不会创建导师角色，也不会绑定当前审核员。"
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
        <template v-if="auditForm.action === 'PASS'">
          <el-form-item label="导师姓名" required>
            <el-input v-model="auditForm.mentorName" maxlength="64" placeholder="请输入实习生导师姓名" />
          </el-form-item>
          <el-form-item label="导师联系方式" required>
            <el-input v-model="auditForm.mentorPhone" maxlength="32" placeholder="请输入手机号或其他联系方式" />
          </el-form-item>
        </template>
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

    <el-drawer
      title="注册申请详情"
      :visible.sync="detailOpen"
      direction="rtl"
      size="560px"
      append-to-body
      class="detail-drawer"
    >
      <div v-loading="detailLoading" class="detail-panel">
        <template v-if="currentRow.id">
          <div class="detail-heading">
            <div class="applicant-cell">
              <span class="avatar-mark large">{{ nameInitial(currentRow.realName) }}</span>
              <div>
                <h3>{{ currentRow.realName }}</h3>
                <span>{{ currentRow.applicationNo }}</span>
              </div>
            </div>
            <el-tag :type="statusTagType(currentRow.status)" effect="plain">{{ statusLabel(currentRow.status) }}</el-tag>
          </div>

          <div class="detail-section">
            <div class="section-title"><i class="el-icon-user" />申请人资料</div>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="登录账号">{{ currentRow.loginAccount || '-' }}</el-descriptions-item>
              <el-descriptions-item label="身份证号">{{ maskIdCard(currentRow.idCard) }}</el-descriptions-item>
              <el-descriptions-item label="意向岗位">{{ currentRow.positionName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="所属部门">
                <span class="department-value"><i class="el-icon-location-outline" />{{ currentRow.deptName || '待配置' }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="预计入职">{{ currentRow.expectedEntryDate || '未填写' }}</el-descriptions-item>
              <el-descriptions-item label="提交次数">{{ currentRow.auditCount || 0 }} 次</el-descriptions-item>
            </el-descriptions>
          </div>

          <div class="detail-section">
            <div class="section-title"><i class="el-icon-s-management" />账号与导师</div>
            <div class="account-grid">
              <div><span>登录状态</span><strong :class="isEnabled(currentRow.accountStatus) ? 'text-success' : 'text-warning'">{{ accountStatusLabel(currentRow.accountStatus) }}</strong></div>
              <div><span>业务身份</span><strong>{{ userStatusLabel(currentRow.userStatus) }}</strong></div>
              <div><span>导师姓名</span><strong>{{ currentRow.mentorName || '待审核补充' }}</strong></div>
              <div><span>导师联系方式</span><strong>{{ currentRow.mentorPhone || '待审核补充' }}</strong></div>
            </div>
          </div>

          <div v-if="currentRow.rejectReason" class="reject-reason">
            <span>最近驳回原因</span>
            <p>{{ currentRow.rejectReason }}</p>
          </div>

          <div class="detail-section">
            <div class="section-title"><i class="el-icon-time" />审核历史</div>
            <el-timeline v-if="history.length">
              <el-timeline-item
                v-for="record in history"
                :key="record.id"
                :timestamp="formatDate(record.createTime, 'yyyy-MM-dd HH:mm')"
                :type="record.action === 'PASS' ? 'success' : 'danger'"
                placement="top"
              >
                <strong>{{ record.action === 'PASS' ? '审核通过' : '驳回申请' }}</strong>
                <p>{{ record.operatorName || '系统管理员' }} · {{ record.deptName || '系统范围' }}</p>
                <p v-if="record.reason" class="timeline-reason">{{ record.reason }}</p>
              </el-timeline-item>
            </el-timeline>
            <div v-else class="history-empty">暂无审核动作，当前申请等待处理。</div>
          </div>

          <div v-if="currentRow.status === 'WAIT_AUDIT'" class="detail-actions">
            <el-button type="primary" icon="el-icon-check" @click="handleAudit(currentRow, 'PASS')">通过申请</el-button>
            <el-button type="danger" plain icon="el-icon-close" @click="handleAudit(currentRow, 'REJECT')">驳回申请</el-button>
          </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import { listRegister, getRegister, listRegisterHistory, auditRegister, getRegisterSummary } from '@/api/business/register'

export default {
  name: 'RegisterApplication',
  data() {
    return {
      loading: false,
      detailLoading: false,
      auditSubmitting: false,
      detailOpen: false,
      auditOpen: false,
      total: 0,
      registerList: [],
      currentRow: {},
      history: [],
      summary: {
        totalCount: 0,
        pendingCount: 0,
        passedCount: 0,
        rejectedCount: 0
      },
      auditForm: {
        id: undefined,
        action: 'PASS',
        reason: '',
        mentorName: '',
        mentorPhone: ''
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
    handleDetail(row) {
      this.detailOpen = true
      this.detailLoading = true
      this.currentRow = Object.assign({}, row)
      this.history = []
      Promise.all([getRegister(row.id), listRegisterHistory(row.id)]).then(([detailResponse, historyResponse]) => {
        this.currentRow = detailResponse.data || this.currentRow
        this.history = historyResponse.data || []
      }).finally(() => {
        this.detailLoading = false
      })
    },
    handleAudit(row, action) {
      this.currentRow = Object.assign({}, row)
      this.auditForm = {
        id: row.id,
        action: action,
        reason: '',
        mentorName: '',
        mentorPhone: ''
      }
      this.auditOpen = true
    },
    handleActionChange(action) {
      if (action === 'PASS') {
        this.auditForm.reason = ''
      } else {
        this.auditForm.mentorName = ''
        this.auditForm.mentorPhone = ''
      }
    },
    submitAudit() {
      if (this.auditForm.action === 'REJECT' && !this.auditForm.reason.trim()) {
        this.$modal.msgWarning('驳回必须填写原因')
        return
      }
      if (this.auditForm.action === 'PASS' && !this.auditForm.mentorName.trim()) {
        this.$modal.msgWarning('审核通过必须填写导师姓名')
        return
      }
      if (this.auditForm.action === 'PASS' && !this.auditForm.mentorPhone.trim()) {
        this.$modal.msgWarning('审核通过必须填写导师联系方式')
        return
      }
      this.auditSubmitting = true
      auditRegister(this.auditForm).then(() => {
        this.$modal.msgSuccess(this.auditForm.action === 'PASS' ? '审核通过，账号已启用并保存导师信息' : '申请已驳回')
        this.auditOpen = false
        this.refreshAll()
        if (this.detailOpen && this.currentRow.id) {
          this.handleDetail({ id: this.currentRow.id })
        }
      }).finally(() => {
        this.auditSubmitting = false
      })
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

.detail-panel {
  min-height: 100%;
  padding: 4px 22px 28px;
}

.detail-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0 18px;
  border-bottom: 1px solid #edf0f4;
}

.detail-heading h3 {
  margin: 0 0 4px;
  color: #2b3d50;
  font-size: 19px;
  font-weight: 600;
}

.detail-heading span:not(.avatar-mark) {
  color: #8a96a4;
  font-size: 12px;
}

.detail-section {
  margin-top: 22px;
}

.section-title {
  margin-bottom: 10px;
  color: #35485c;
  font-size: 14px;
  font-weight: 600;
}

.section-title i {
  margin-right: 6px;
  color: #3d82c7;
}

.department-value {
  color: #3472ad;
}

.department-value i {
  margin-right: 4px;
}

.account-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 9px;
}

.account-grid > div {
  min-height: 67px;
  padding: 11px;
  border: 1px solid #e6ebf1;
  background: #fafbfd;
}

.account-grid span,
.account-grid strong {
  display: block;
}

.account-grid span {
  margin-bottom: 7px;
  color: #8490a0;
  font-size: 11px;
}

.account-grid strong {
  color: #35485c;
  font-size: 13px;
  font-weight: 600;
}

.text-success { color: #2f9b79 !important; }
.text-warning { color: #d2872f !important; }

.reject-reason {
  margin-top: 20px;
  padding: 12px 14px;
  border-left: 3px solid #cd5b5b;
  background: #fff7f7;
}

.reject-reason span {
  color: #a24b4b;
  font-size: 12px;
  font-weight: 600;
}

.reject-reason p {
  margin: 7px 0 0;
  color: #6c5960;
  font-size: 13px;
  line-height: 1.6;
}

.detail-section ::v-deep .el-timeline {
  padding-left: 4px;
}

.detail-section ::v-deep .el-timeline-item__content {
  color: #3e5063;
  font-size: 13px;
}

.detail-section ::v-deep .el-timeline-item__timestamp {
  color: #9aa5b2;
  font-size: 11px;
}

.detail-section ::v-deep .el-timeline-item__content p {
  margin: 5px 0 0;
  color: #8490a0;
  font-size: 12px;
}

.detail-section ::v-deep .el-timeline-item__content .timeline-reason {
  padding: 7px 9px;
  background: #fafbfd;
  color: #687688;
  line-height: 1.5;
}

.history-empty {
  padding: 15px;
  border: 1px dashed #dbe3ec;
  color: #8a96a4;
  font-size: 12px;
  text-align: center;
}

.detail-actions {
  display: flex;
  gap: 10px;
  margin-top: 24px;
  padding-top: 18px;
  border-top: 1px solid #edf0f4;
}

@media (max-width: 900px) {
  .card-subtitle {
    display: block;
    margin: 5px 0 0;
  }

  .account-grid {
    grid-template-columns: 1fr;
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
