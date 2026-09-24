<template>
  <div class="app-container mentor-page">
    <div class="page-heading">
      <div>
        <div class="eyebrow">组织与人员 / 导师管理</div>
        <h2>导师管理</h2>
        <p>维护导师库。实习生可在「实习生管理」页从这里直接选一位导师；注册审核时不再填写导师。</p>
      </div>
      <div class="scope-note">
        <i class="el-icon-lock" />
        <div>
          <span>数据范围</span>
          <strong>{{ isSuperScope ? '全局导师' : '本部门导师' }}</strong>
        </div>
      </div>
    </div>

    <!-- ① 总览 KPI（一律按已加载的真实数据算，不为好看编数） -->
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

    <el-card shadow="never" class="mentor-card">
      <div slot="header" class="card-header">
        <div>
          <span class="card-title">导师列表</span>
          <span class="card-subtitle">停用后不再出现在下拉选项里；有实习生在带的导师不能删除</span>
        </div>
        <div class="head-actions">
          <el-button size="mini" icon="el-icon-refresh" @click="refreshAll">刷新</el-button>
          <el-button
            type="primary"
            size="mini"
            icon="el-icon-plus"
            v-hasPermi="['business:mentor:add']"
            @click="handleAdd"
          >新增导师</el-button>
        </div>
      </div>

      <!-- ② 筛选条 -->
      <el-form
        ref="queryForm"
        :model="queryParams"
        size="small"
        :inline="true"
        class="query-form"
        @submit.native.prevent
      >
        <el-form-item label="姓名" prop="mentorName">
          <el-input
            v-model="queryParams.mentorName"
            placeholder="请输入导师姓名"
            clearable
            style="width: 160px"
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item label="联系方式" prop="mentorPhone">
          <el-input
            v-model="queryParams.mentorPhone"
            placeholder="手机号 / 其他"
            clearable
            style="width: 160px"
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item v-if="isSuperScope" label="所属部门" prop="deptId">
          <el-select v-model="queryParams.deptId" placeholder="全部部门" clearable style="width: 150px">
            <el-option v-for="d in deptOptions" :key="d.deptId" :label="d.deptName" :value="d.deptId" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="启用中" value="0" />
            <el-option label="已停用" value="1" />
          </el-select>
        </el-form-item>
        <el-form-item class="query-actions">
          <el-button type="primary" icon="el-icon-search" @click="handleQuery">查询</el-button>
          <el-button icon="el-icon-refresh-left" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="list-meta">
        <span class="filtered">显示 {{ mentorList.length }} / 命中 {{ total }}</span>
        <span v-if="isFiltered" class="filter-hint">
          已按条件筛选
          <el-button type="text" size="mini" @click="resetQuery">清空筛选</el-button>
        </span>
      </div>

      <el-table
        v-loading="loading"
        :data="mentorList"
        stripe
        border
        class="mentor-table"
        empty-text="暂无导师"
      >
        <el-table-column label="导师" min-width="150" fixed="left">
          <template slot-scope="scope">
            <div class="name-cell">
              <span class="avatar-mark">{{ nameInitial(scope.row.mentorName) }}</span>
              <div>
                <strong>{{ scope.row.mentorName }}</strong>
                <small>{{ scope.row.title || '未填写职务' }}</small>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="联系方式" prop="mentorPhone" min-width="135" show-overflow-tooltip />
        <el-table-column label="所属部门" min-width="120">
          <template slot-scope="scope">{{ scope.row.deptName || '未设置' }}</template>
        </el-table-column>
        <el-table-column label="岗位" min-width="120">
          <template slot-scope="scope">
            <span v-if="scope.row.positionName">{{ scope.row.positionName }}</span>
            <span v-else class="muted-text">—</span>
          </template>
        </el-table-column>
        <el-table-column label="在带实习生" min-width="105" align="center">
          <template slot-scope="scope">
            <span :class="scope.row.internCount > 0 ? 'count-on' : 'muted-text'">
              {{ scope.row.internCount || 0 }} 人
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="92">
          <template slot-scope="scope">
            <el-tag :type="scope.row.status === '0' ? 'success' : 'info'" size="mini" effect="plain">
              {{ scope.row.status === '0' ? '启用中' : '已停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="160" show-overflow-tooltip>
          <template slot-scope="scope">
            <span v-if="scope.row.remark">{{ scope.row.remark }}</span>
            <span v-else class="muted-text">—</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="140">
          <template slot-scope="scope">{{ formatDate(scope.row.createTime, 'yyyy-MM-dd HH:mm') }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template slot-scope="scope">
            <el-button
              type="text"
              size="mini"
              icon="el-icon-edit"
              v-hasPermi="['business:mentor:edit']"
              @click="handleEdit(scope.row)"
            >编辑</el-button>
            <el-button
              type="text"
              size="mini"
              :icon="scope.row.status === '0' ? 'el-icon-turn-off' : 'el-icon-open'"
              v-hasPermi="['business:mentor:edit']"
              @click="toggleStatus(scope.row)"
            >{{ scope.row.status === '0' ? '停用' : '启用' }}</el-button>
            <el-button
              type="text"
              size="mini"
              class="danger-text"
              icon="el-icon-delete"
              v-hasPermi="['business:mentor:remove']"
              @click="handleDelete(scope.row)"
            >删除</el-button>
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

    <!-- 新增 / 编辑 -->
    <el-dialog
      :title="dialogTitle"
      :visible.sync="dialogOpen"
      width="540px"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-form ref="form" :model="form" :rules="rules" label-width="96px" size="small">
        <el-form-item label="导师姓名" prop="mentorName">
          <el-input v-model="form.mentorName" maxlength="64" placeholder="请输入导师姓名" />
        </el-form-item>
        <el-form-item label="联系方式" prop="mentorPhone">
          <el-input v-model="form.mentorPhone" maxlength="32" placeholder="手机号或其他联系方式" />
        </el-form-item>
        <el-form-item label="所属部门" prop="deptId">
          <el-select
            v-model="form.deptId"
            :disabled="!isSuperScope"
            placeholder="请选择所属部门"
            style="width: 100%"
          >
            <el-option v-for="d in deptOptions" :key="d.deptId" :label="d.deptName" :value="d.deptId" />
          </el-select>
          <div v-if="!isSuperScope" class="field-tip">部门管理员只能维护本部门的导师</div>
        </el-form-item>
        <el-form-item label="职务/头衔" prop="title">
          <el-input v-model="form.title" maxlength="64" placeholder="选填，如「前端组长」" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="255" show-word-limit placeholder="选填" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogOpen = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listMentor, addMentor, updateMentor, delMentor } from '@/api/business/mentor'
import { listDept } from '@/api/system/dept'

export default {
  name: 'MentorManage',
  data() {
    return {
      loading: false,
      submitting: false,
      dialogOpen: false,
      total: 0,
      mentorList: [],
      deptOptions: [],
      queryParams: {
        pageNum: 1,
        pageSize: 20,
        mentorName: undefined,
        mentorPhone: undefined,
        deptId: undefined,
        status: undefined
      },
      form: this.emptyForm(),
      rules: {
        mentorName: [{ required: true, message: '请填写导师姓名', trigger: 'blur' }],
        mentorPhone: [{ required: true, message: '请填写联系方式', trigger: 'blur' }],
        deptId: [{ required: true, message: '请选择所属部门', trigger: 'change' }]
      }
    }
  },
  computed: {
    isSuperScope() {
      const roles = this.$store.getters.roles || []
      return roles.indexOf('SUPER_ADMIN') > -1 || roles.indexOf('admin') > -1
    },
    dialogTitle() {
      return this.form.id ? '编辑导师' : '新增导师'
    },
    isFiltered() {
      const q = this.queryParams
      return !!(q.mentorName || q.mentorPhone || q.deptId || q.status)
    },
    /** KPI 一律用「已加载的真实数据」算；不编数、不估算 */
    summaryCards() {
      const rows = this.mentorList
      const enabled = rows.filter(r => r.status === '0').length
      const disabled = rows.filter(r => r.status !== '0').length
      const interning = rows.reduce((sum, r) => sum + Number(r.internCount || 0), 0)
      const scopeHint = this.isFiltered ? '按当前筛选' : '当前页'
      return [
        { key: 'total', label: '导师总数', value: this.total, hint: '命中记录数', tone: 'blue', icon: 'el-icon-user' },
        { key: 'enabled', label: '启用中', value: enabled, hint: scopeHint + '可选', tone: 'green', icon: 'el-icon-circle-check' },
        { key: 'disabled', label: '已停用', value: disabled, hint: scopeHint + '不可选', tone: 'gray', icon: 'el-icon-remove-outline' },
        { key: 'intern', label: '在带实习生', value: interning, hint: scopeHint + '合计', tone: 'orange', icon: 'el-icon-s-claim' }
      ]
    }
  },
  created() {
    this.loadDeptOptions()
    this.getList()
  },
  methods: {
    emptyForm() {
      return {
        id: undefined,
        mentorName: '',
        mentorPhone: '',
        deptId: undefined,
        title: '',
        status: '0',
        remark: ''
      }
    },
    refreshAll() {
      this.loadDeptOptions()
      this.getList()
    },
    /** 部门下拉：超管需要，用来选导师归属部门；部门管理员不需要（后端强制本部门） */
    loadDeptOptions() {
      if (!this.isSuperScope) {
        return
      }
      listDept({ status: '0' }).then(response => {
        this.deptOptions = this.flattenDepts(response.data || [])
      }).catch(() => {
        this.deptOptions = []
      })
    },
    flattenDepts(list) {
      const out = []
      const walk = nodes => {
        (nodes || []).forEach(node => {
          if (node.deptId) {
            out.push({ deptId: node.deptId, deptName: node.deptName })
          }
          if (node.children && node.children.length) {
            walk(node.children)
          }
        })
      }
      walk(list)
      return out
    },
    getList() {
      this.loading = true
      listMentor(this.queryParams).then(response => {
        this.mentorList = response.rows || []
        this.total = response.total || 0
      }).finally(() => {
        this.loading = false
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.$refs.queryForm.resetFields()
      this.handleQuery()
    },
    handleAdd() {
      this.form = this.emptyForm()
      this.dialogOpen = true
      this.$nextTick(() => {
        this.$refs.form && this.$refs.form.clearValidate()
      })
    },
    handleEdit(row) {
      this.form = {
        id: row.id,
        mentorName: row.mentorName,
        mentorPhone: row.mentorPhone,
        deptId: row.deptId,
        title: row.title || '',
        status: row.status || '0',
        remark: row.remark || ''
      }
      this.dialogOpen = true
      this.$nextTick(() => {
        this.$refs.form && this.$refs.form.clearValidate()
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        this.submitting = true
        const payload = Object.assign({}, this.form)
        const action = payload.id ? updateMentor(payload) : addMentor(payload)
        action.then(() => {
          this.$modal.msgSuccess(payload.id ? '导师信息已更新' : '导师已新增')
          this.dialogOpen = false
          this.getList()
        }).finally(() => {
          this.submitting = false
        })
      })
    },
    toggleStatus(row) {
      const next = row.status === '0' ? '1' : '0'
      const label = next === '0' ? '启用' : '停用'
      this.$modal.confirm(`确认${label}导师「${row.mentorName}」？` + (next === '1' ? '停用后将不再出现在分配下拉中。' : '')).then(() => {
        return updateMentor({
          id: row.id,
          mentorName: row.mentorName,
          mentorPhone: row.mentorPhone,
          deptId: row.deptId,
          title: row.title,
          status: next,
          remark: row.remark
        })
      }).then(() => {
        this.$modal.msgSuccess(`已${label}`)
        this.getList()
      }).catch(() => {})
    },
    handleDelete(row) {
      this.$modal.confirm(`确认删除导师「${row.mentorName}」？`).then(() => {
        return delMentor(row.id)
      }).then(() => {
        this.$modal.msgSuccess('导师已删除')
        this.getList()
      }).catch(() => {})
    },
    nameInitial(name) {
      return (name || '?').slice(0, 1)
    },
    formatDate(value, pattern) {
      if (!value) return ''
      const date = new Date(String(value).replace(/-/g, '/'))
      if (Number.isNaN(date.getTime())) return String(value)
      const pad = n => String(n).padStart(2, '0')
      const map = {
        yyyy: date.getFullYear(),
        MM: pad(date.getMonth() + 1),
        dd: pad(date.getDate()),
        HH: pad(date.getHours()),
        mm: pad(date.getMinutes()),
        ss: pad(date.getSeconds())
      }
      return pattern.replace(/yyyy|MM|dd|HH|mm|ss/g, token => map[token])
    }
  }
}
</script>

<style lang="scss" scoped>
// ★ 本页在部门端与超管端共用，所以样式自带、不依赖任一端的公共样式表。
// ★ 铁律：@import 进 scoped 时不能用 :root（会编译成 :root[data-v-x] 永不匹配），令牌一律用 SCSS 变量。
$ink-1: #243447;
$ink-2: #33465b;
$ink-3: #748092;
$ink-4: #8a96a4;
$line: #e5ebf2;
$line-soft: #edf0f4;
$blue: #3d82c7;
$green: #2f9b79;
$orange: #d2872f;
$red: #cd5b5b;
$panel: #ffffff;
$page-bg: #f6f8fb;

.mentor-page {
  color: $ink-1;
  background: $page-bg;
}

.page-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin: 2px 0 18px;
}

.eyebrow {
  margin-bottom: 7px;
  color: $blue;
  font-size: 12px;
}

.page-heading h2 {
  margin: 0 0 8px;
  color: $ink-1;
  font-size: 24px;
  font-weight: 600;
}

.page-heading p {
  margin: 0;
  color: $ink-3;
  font-size: 13px;
}

.scope-note {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 150px;
  padding: 10px 14px;
  border: 1px solid #dfe8f2;
  background: $panel;
  color: $blue;
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
  color: $ink-4;
  font-size: 12px;
}

.scope-note strong {
  color: $ink-2;
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
  border: 1px solid $line;
  border-left: 4px solid $blue;
  background: $panel;
}

.summary-card.orange { border-left-color: $orange; }
.summary-card.green { border-left-color: $green; }
.summary-card.gray { border-left-color: #a9b4c0; }

.summary-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  margin-right: 12px;
  border-radius: 50%;
  background: #edf4ff;
  color: $blue;
  font-size: 19px;
}

.summary-card.orange .summary-icon { background: #fff5e8; color: $orange; }
.summary-card.green .summary-icon { background: #edf9f3; color: $green; }
.summary-card.gray .summary-icon { background: #f2f4f7; color: #7b8794; }

.summary-content span,
.summary-content small {
  display: block;
  color: $ink-3;
}

.summary-content span {
  font-size: 12px;
}

.summary-content strong {
  display: block;
  margin: 3px 0;
  color: $ink-1;
  font-size: 24px;
  font-weight: 600;
  line-height: 1;
}

.summary-content small {
  font-size: 11px;
}

.mentor-card {
  border: 1px solid $line;
  border-radius: 4px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  color: $ink-2;
  font-size: 16px;
  font-weight: 600;
}

.card-subtitle {
  margin-left: 12px;
  color: $ink-4;
  font-size: 12px;
}

.head-actions {
  display: flex;
  gap: 8px;
}

.query-form {
  padding-bottom: 4px;
  border-bottom: 1px solid $line-soft;
}

.query-actions {
  margin-left: 4px;
}

.list-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 12px 0;
  color: $ink-4;
  font-size: 12px;
}

.filtered {
  color: $blue;
  font-weight: 600;
}

.filter-hint {
  color: $orange;
}

.mentor-table ::v-deep th {
  background: #f7f9fc;
  color: #52637a;
  font-weight: 600;
}

.name-cell {
  display: flex;
  align-items: center;
}

.name-cell strong,
.name-cell small {
  display: block;
}

.name-cell strong {
  color: $ink-2;
  font-weight: 600;
}

.name-cell small {
  margin-top: 3px;
  overflow: hidden;
  color: $ink-4;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
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

.muted-text { color: #b3bcc7; }
.danger-text { color: $red; }
.count-on { color: $green; font-weight: 600; }
.field-tip { margin-top: 3px; color: $ink-4; font-size: 11px; }

@media (max-width: 900px) {
  .page-heading { display: block; }
  .scope-note { margin-top: 14px; }
  .card-subtitle { display: block; margin: 5px 0 0; }
}
</style>
