<template>
  <div class="exam-manage app-container">
    <header class="page-heading">
      <div>
        <div class="eyebrow">学习与考核管理 / 正式考核管理</div>
        <div class="title-line">
          <h2>正式考核管理</h2>
          <el-tag size="mini" effect="plain" :type="isSuperAdmin ? 'warning' : 'success'">{{ isSuperAdmin ? '全局管理' : '本部门范围' }}</el-tag>
          <el-tag size="mini" effect="plain" type="info">理论与实操分列</el-tag>
        </div>
      </div>
      <div class="heading-actions">
        <el-button icon="el-icon-refresh" size="small" @click="loadList">刷新</el-button>
        <el-button v-hasPermi="['business:bank:add']" type="primary" icon="el-icon-plus" size="small" @click="handleAdd('THEORY')">新建考核</el-button>
      </div>
    </header>

    <el-form :inline="true" size="small" class="query-form" @submit.native.prevent>
      <el-form-item v-if="isSuperAdmin" label="所属部门">
        <el-select v-model="queryParams.deptId" clearable filterable placeholder="全部部门" style="width:180px" @change="handleQuery">
          <el-option v-for="dept in deptOptions" :key="dept.deptId" :label="dept.deptName" :value="dept.deptId" />
        </el-select>
      </el-form-item>
      <el-form-item label="考核名称">
        <el-input v-model="queryParams.examName" clearable placeholder="名称关键字" style="width:200px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" clearable placeholder="全部状态" style="width:130px" @change="handleQuery">
          <el-option label="待发布" value="DRAFT" />
          <el-option label="已发布" value="PUBLISHED" />
          <el-option label="待批改" value="GRADING" />
          <el-option label="已停用" value="DISABLED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="handleQuery">查询</el-button>
        <el-button icon="el-icon-refresh-left" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 理论 / 实操分列 -->
    <el-tabs v-model="activeType" class="type-tabs" @tab-click="handleTypeChange">
      <!-- 注意：name 不能给空串。element-ui 对空 name 会回退成面板索引（"0"），
           导致「全部」被当成一个真实类型传给后端 examType=0，列表恒为空 -->
      <el-tab-pane label="全部考核" name="ALL" />
      <el-tab-pane :label="'理论考核' + tabCount('THEORY', theoryTotal)" name="THEORY" />
      <el-tab-pane :label="'实操考核' + tabCount('PRACTICAL', practiceTotal)" name="PRACTICAL" />
    </el-tabs>

    <section class="dm-card">
      <div class="dm-head">
        <div class="dm-title">
          <span class="dm-idx">{{ typeIdx }}</span>
          <h3>{{ typeTitle }}列表</h3>
        </div>
        <span class="dm-meta">共 {{ total }} 条</span>
      </div>

      <el-table
        v-loading="loading"
        :data="list"
        size="small"
        highlight-current-row
        :empty-text="emptyText"
        @current-change="selectExam"
      >
        <el-table-column label="考核名称" min-width="230">
          <template slot-scope="scope">
            <div class="exam-cell">
              <strong>{{ scope.row.examName }}</strong>
              <small>{{ isSuperAdmin ? (scope.row.deptName || '未设置部门') : scope.row.examModeText }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="108" align="center">
          <template slot-scope="scope">
            <el-tag size="mini" effect="plain" :type="scope.row.examType === 'PRACTICAL' ? 'success' : 'primary'">
              {{ scope.row.examType === 'PRACTICAL' ? '实操考核' : '理论考核' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="卷面" min-width="170">
          <template slot-scope="scope">
            <span v-if="scope.row.examType === 'PRACTICAL'">
              {{ scope.row.subjectCount || 0 }} 道题 · 满分 {{ round1(scope.row.subjectTotalScore) }} 分
            </span>
            <span v-else>
              {{ scope.row.questionCount || 0 }} 题 · 满分 {{ theoryScore(scope.row) }} 分
            </span>
          </template>
        </el-table-column>
        <el-table-column label="时长" width="96" align="center">
          <template slot-scope="scope">
            <span>{{ scope.row.duration > 0 ? scope.row.duration + ' 分钟' : '不限时' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="时间窗" min-width="186">
          <template slot-scope="scope">
            <span class="muted">{{ windowText(scope.row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="通过线" width="84" align="center">
          <template slot-scope="scope"><span>{{ scope.row.passLine }} 分</span></template>
        </el-table-column>
        <el-table-column label="状态" width="92" align="center">
          <template slot-scope="scope">
            <el-tag size="mini" :type="statusTag(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="已作答 / 待批阅" width="128" align="center">
          <template slot-scope="scope">
            <strong class="num">{{ scope.row.answeredCount || 0 }}</strong>
            <span class="sep">/</span>
            <span class="num warn">{{ scope.row.pendingCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="176" align="center">
          <template slot-scope="scope">
            <el-button type="text" size="mini" icon="el-icon-setting" @click.stop="selectExam(scope.row)">配置</el-button>
            <el-button
              v-hasPermi="['business:bank:list']"
              type="text"
              size="mini"
              :icon="scope.row.examType === 'PRACTICAL' ? 'el-icon-edit-outline' : 'el-icon-view'"
              @click.stop="goGrading(scope.row)"
            >{{ scope.row.examType === 'PRACTICAL' ? '批改' : '查看答卷' }}</el-button>
            <el-button
              v-hasPermi="['business:bank:remove']"
              type="text"
              size="mini"
              icon="el-icon-delete"
              class="danger-text"
              @click.stop="handleDelete(scope.row)"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="loadList" />
    </section>

    <!-- 选中考核的配置卡 -->
    <exam-config-card
      v-if="selectedExam"
      :key="selectedExam.id"
      :exam="selectedExam"
      :is-super-admin="isSuperAdmin"
      :dept-id="selectedExam.deptId"
      @edit="handleEdit"
      @delete="handleDelete"
      @grade="goGrading"
      @status-change="handleChangeStatus"
      @refresh="reloadCurrent"
    />

    <!-- ================= 新建 / 编辑考核 ================= -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="960px" top="6vh" append-to-body class="exam-dialog">
      <!-- ① 考核类型：大卡选择（编辑时锁定，类型不可改） -->
      <div class="type-pick">
        <div class="pick-card" :class="{ on: form.examType === 'THEORY', locked: !!form.id }" @click="pickType('THEORY')">
          <div class="pick-icon"><i class="el-icon-document-checked" /></div>
          <div class="pick-body">
            <b>理论考核</b>
            <span>从题库组卷（多题库 × 单选/多选/判断配额），实习生线上答题，交卷即自动判分。</span>
          </div>
          <i v-if="form.examType === 'THEORY'" class="el-icon-success pick-check" />
        </div>
        <div class="pick-card green" :class="{ on: form.examType === 'PRACTICAL', locked: !!form.id }" @click="pickType('PRACTICAL')">
          <div class="pick-icon green"><i class="el-icon-upload2" /></div>
          <div class="pick-body">
            <b>实操考核</b>
            <span>逐题填写题干 / 题目描述 / 参考 / 附件与满分，实习生逐题交文件，管理员逐题打分。</span>
          </div>
          <i v-if="form.examType === 'PRACTICAL'" class="el-icon-success pick-check" />
        </div>
      </div>

      <el-form ref="examForm" :model="form" :rules="rules" label-width="100px" v-loading="detailLoading">
        <div class="form-sec">基本信息</div>
        <div class="fg2">
          <el-form-item v-if="isSuperAdmin" label="所属部门" prop="deptId">
            <el-select v-model="form.deptId" :disabled="!!form.id" filterable placeholder="请选择所属部门" style="width:100%">
              <el-option v-for="dept in deptOptions" :key="dept.deptId" :label="dept.deptName" :value="dept.deptId" />
            </el-select>
          </el-form-item>
          <el-form-item label="考核名称" prop="examName" :class="{ 'span2': !isSuperAdmin }">
            <el-input v-model="form.examName" maxlength="128" placeholder="例如：2026Q4 开发实习生·理论考核（第二期）" />
          </el-form-item>
        </div>
        <div class="fg2">
          <el-form-item label="考核时长">
            <el-select v-model="durationPreset" style="width:130px" @change="onDurationPresetChange">
              <el-option v-for="d in durationOptions" :key="d" :label="d > 0 ? d + ' 分钟' : '不限时'" :value="d" />
              <el-option label="自定义…" :value="-1" />
            </el-select>
            <el-input-number v-if="durationPreset === -1" v-model="form.duration" :min="1" :max="600" size="small" style="width:110px;margin-left:8px" />
            <span class="muted" style="margin-left:8px">{{ form.duration > 0 ? '到点自动交卷' : '不限时' }}</span>
          </el-form-item>
          <el-form-item label="通过线(分)">
            <el-input-number v-model="form.passLine" :min="0" :max="999" :precision="1" />
          </el-form-item>
        </div>

        <!-- ② 实操：题目清单（理论考核的组卷题库与每题分值在下方配置卡里设置） -->
        <template v-if="form.examType === 'PRACTICAL'">
          <div class="form-sec">实操题目（逐题填写 · 不从题库抽题）</div>
          <subject-item-editor v-model="form.subjectItems" />
        </template>
      </el-form>

      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { listExam, getExam, addExam, updateExam, delExam, changeExamStatus } from '@/api/business/exam'
import { listDept } from '@/api/system/dept'
import { mapGetters } from 'vuex'
import ExamConfigCard from './components/ExamConfigCard'
import SubjectItemEditor from './components/SubjectItemEditor'

const DURATION_OPTIONS = [0, 30, 45, 60, 90, 120, 150, 180, 240]

/** 解析后端 [{name,url}] 形式的 JSON 字段 */
function parseJsonList(json) {
  if (!json) return []
  try {
    const arr = typeof json === 'string' ? JSON.parse(json) : json
    return Array.isArray(arr) ? arr : []
  } catch (e) {
    return []
  }
}

export default {
  name: 'ExamManage',
  components: { ExamConfigCard, SubjectItemEditor },
  data() {
    return {
      loading: false,
      list: [],
      total: 0,
      theoryTotal: 0,
      practiceTotal: 0,
      /** 当前类型分栏：ALL 全部 / THEORY 理论 / PRACTICAL 实操 */
      activeType: 'ALL',
      queryParams: { pageNum: 1, pageSize: 10, examMode: 'FORMAL', examName: '', status: '', deptId: null },
      deptOptions: [],
      selectedExamId: null,
      dialogVisible: false,
      dialogTitle: '',
      detailLoading: false,
      durationOptions: DURATION_OPTIONS,
      durationPreset: 60,
      form: emptyForm('THEORY'),
      rules: {
        deptId: [{ required: true, message: '请选择所属部门', trigger: 'change' }],
        examName: [{ required: true, message: '请输入考核名称', trigger: 'blur' }]
      },
      submitting: false
    }
  },
  computed: {
    ...mapGetters(['roles']),
    isSuperAdmin() { return this.roles.indexOf('SUPER_ADMIN') > -1 },
    // 分栏类型归一化：只有 THEORY / PRACTICAL 算筛选，其余（含 element-ui 兜底的 "0"）一律当「全部」
    examTypeFilter() {
      return this.activeType === 'THEORY' || this.activeType === 'PRACTICAL' ? this.activeType : 'ALL'
    },
    typeTitle() {
      return { ALL: '考核', THEORY: '理论考核', PRACTICAL: '实操考核' }[this.examTypeFilter] || '考核'
    },
    typeIdx() {
      return { ALL: '考', THEORY: '理', PRACTICAL: '实' }[this.examTypeFilter] || '考'
    },
    emptyText() {
      if (this.examTypeFilter === 'THEORY') return '暂无理论考核，点「新建考核」并选择理论类型'
      if (this.examTypeFilter === 'PRACTICAL') return '暂无实操考核，点「新建考核」并选择实操类型'
      return '暂无考核，点「新建考核」创建第一条'
    },
    selectedExam() {
      if (!this.selectedExamId) return null
      return this.list.find(e => e.id === this.selectedExamId) || null
    }
  },
  created() {
    if (this.isSuperAdmin) this.loadDepartments()
    this.loadList()
    this.loadCounts()
  },
  methods: {
    loadDepartments() {
      listDept({ status: '0' }).then(res => {
        this.deptOptions = (res.data || []).filter(dept => dept.parentId !== 0)
      })
    },
    loadList() {
      this.loading = true
      const params = Object.assign({}, this.queryParams)
      if (this.examTypeFilter === 'ALL') {
        delete params.examType
      } else {
        params.examType = this.examTypeFilter
      }
      listExam(params).then(res => {
        this.list = res.rows || []
        this.total = res.total || 0
        this.loading = false
        // 选中项被过滤掉 / 首次进入时，默认选中第一条
        if (!this.list.some(e => e.id === this.selectedExamId)) {
          this.selectedExamId = this.list.length ? this.list[0].id : null
        }
      }).catch(() => { this.loading = false })
    },
    /** 分栏计数：各类型单独取一次总数 */
    loadCounts() {
      const base = { pageNum: 1, pageSize: 1, examMode: 'FORMAL' }
      listExam(Object.assign({}, base, { examType: 'THEORY' })).then(res => { this.theoryTotal = res.total || 0 }).catch(() => {})
      listExam(Object.assign({}, base, { examType: 'PRACTICAL' })).then(res => { this.practiceTotal = res.total || 0 }).catch(() => {})
    },
    tabCount(type, n) {
      // 只在对应分栏下显示数量，避免未筛选时被误读为"当前列表条数"
      return this.examTypeFilter === type ? '（' + n + '）' : ''
    },
    handleTypeChange() {
      this.queryParams.pageNum = 1
      this.selectedExamId = null
      this.loadList()
    },
    selectExam(row) {
      if (row && row.id) this.selectedExamId = row.id
    },
    reloadCurrent() {
      this.loadList()
      this.loadCounts()
    },
    goGrading(exam) {
      this.$router.push('/assessment/department/exam/grading/' + exam.id)
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.loadList()
    },
    resetQuery() {
      this.queryParams = { pageNum: 1, pageSize: 10, examMode: 'FORMAL', examName: '', status: '', deptId: null }
      this.loadList()
    },

    // ---------- 时长（下拉预设 + 自定义） ----------
    onDurationPresetChange(v) {
      if (v !== -1) {
        this.form.duration = v
      } else if (!this.form.duration) {
        this.form.duration = 60
      }
    },
    presetOfDuration(minutes) {
      const d = Number(minutes) || 0
      return this.durationOptions.indexOf(d) > -1 ? d : -1
    },
    pickType(type) {
      if (this.form.id) return // 已保存的考核不能改类型
      if (this.form.examType === type) return
      this.form.examType = type
      if (type === 'PRACTICAL') {
        if (!this.form.subjectItems.length) this.form.subjectItems = [emptySubjectItem()]
        this.durationPreset = this.presetOfDuration(180)
        this.form.duration = 180
      } else {
        this.form.singleScore = 2
        this.form.multiScore = 4
        this.form.judgeScore = 2
        this.durationPreset = this.presetOfDuration(60)
        this.form.duration = 60
      }
    },

    // ---------- 新建 / 编辑 ----------
    handleAdd(examType) {
      this.form = emptyForm(examType || 'THEORY')
      if (this.form.examType === 'PRACTICAL') this.form.subjectItems = [emptySubjectItem()]
      this.form.deptId = this.isSuperAdmin ? this.queryParams.deptId : null
      this.durationPreset = this.presetOfDuration(this.form.duration)
      this.dialogTitle = '新建考核'
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.examForm && this.$refs.examForm.clearValidate())
    },
    handleEdit(row) {
      this.dialogVisible = true
      this.detailLoading = true
      this.dialogTitle = '编辑考核'
      // 走详情接口：实操题目清单（题干/描述/参考/附件）只在详情里返回
      getExam(row.id).then(res => {
        const d = res.data || {}
        this.form = {
          id: d.id,
          deptId: d.deptId,
          examName: d.examName || '',
          examType: d.examType || 'THEORY',
          singleScore: num(d.singleScore, 2),
          multiScore: num(d.multiScore, 4),
          judgeScore: num(d.judgeScore, 2),
          passLine: num(d.passLine, 60),
          duration: num(d.duration, 0),
          subjectItems: (d.subjectItems || []).map(s => ({
            id: s.id,
            title: s.title || '',
            description: s.description || '',
            score: num(s.score, 0),
            images: parseJsonList(s.referenceImages),
            attachments: parseJsonList(s.attachmentsJson)
          }))
        }
        if (this.form.examType === 'PRACTICAL' && !this.form.subjectItems.length) {
          this.form.subjectItems = [emptySubjectItem()]
        }
        this.durationPreset = this.presetOfDuration(this.form.duration)
        this.detailLoading = false
        this.$nextTick(() => this.$refs.examForm && this.$refs.examForm.clearValidate())
      }).catch(() => {
        this.detailLoading = false
        this.dialogVisible = false
      })
    },
    submitForm() {
      this.$refs.examForm.validate(valid => {
        if (!valid) return
        const payload = {
          id: this.form.id,
          deptId: this.form.deptId,
          examName: (this.form.examName || '').trim(),
          examMode: 'FORMAL',
          examType: this.form.examType,
          passLine: this.form.passLine,
          duration: this.form.duration
        }
        if (this.form.examType === 'THEORY') {
          payload.singleScore = this.form.singleScore
          payload.multiScore = this.form.multiScore
          payload.judgeScore = this.form.judgeScore
        } else {
          const items = this.form.subjectItems || []
          if (!items.length) {
            this.$modal.msgWarning('实操考核至少需要一道题目：点「添加题目」填写题干')
            return
          }
          const blank = items.findIndex(s => !String(s.title || '').trim())
          if (blank > -1) {
            this.$modal.msgWarning('第 ' + (blank + 1) + ' 题还没填写题干，请补充或删除该题')
            return
          }
          payload.subjectItems = items.map(s => ({
            id: s.id || null,
            title: String(s.title).trim(),
            description: String(s.description || '').trim() || null,
            score: Number(s.score) || 0,
            // 后端按 JSON 字符串落库；空时必须传空串而不是 null，否则旧值残留
            referenceImages: s.images && s.images.length ? JSON.stringify(s.images) : '',
            attachmentsJson: s.attachments && s.attachments.length ? JSON.stringify(s.attachments) : ''
          }))
        }
        this.submitting = true
        const fn = payload.id ? updateExam : addExam
        fn(payload).then(() => {
          this.$modal.msgSuccess('保存成功')
          this.dialogVisible = false
          this.submitting = false
          const createdId = !payload.id
          this.reloadCurrent()
          if (createdId) this.$modal.msgSuccess('已创建，请在下方卡片配置组卷、每题分值与发布设置')
        }).catch(() => { this.submitting = false })
      })
    },
    handleDelete(row) {
      const tip = row.examType === 'PRACTICAL'
        ? `确认删除实操考核「${row.examName}」吗？该考核的题目清单将一并删除。`
        : `确认删除理论考核「${row.examName}」吗？`
      this.$modal.confirm(tip).then(() => {
        delExam(row.id).then(() => {
          this.$modal.msgSuccess('删除成功')
          if (this.selectedExamId === row.id) this.selectedExamId = null
          this.reloadCurrent()
        })
      }).catch(() => {})
    },
    handleChangeStatus(row, status) {
      const label = status === 'PUBLISHED' ? '启用' : '停用'
      this.$modal.confirm(`确认${label}「${row.examName}」吗？`).then(() => {
        changeExamStatus(row.id, status).then(() => {
          this.$modal.msgSuccess(`${label}成功`)
          this.reloadCurrent()
        })
      }).catch(() => {})
    },

    // ---------- 展示辅助 ----------
    statusLabel(s) {
      return { DRAFT: '待发布', PUBLISHED: '已发布', GRADING: '待批改', DISABLED: '已停用' }[s] || s
    },
    statusTag(s) {
      return { DRAFT: 'info', PUBLISHED: 'success', GRADING: 'warning', DISABLED: 'danger' }[s] || 'info'
    },
    theoryScore(row) {
      // 注意：round1 是本组件的方法，必须走 this，裸调用会 ReferenceError 并炸掉整行渲染
      return this.round1(
        (Number(row.singleCount) || 0) * (Number(row.singleScore) || 0) +
        (Number(row.multiCount) || 0) * (Number(row.multiScore) || 0) +
        (Number(row.judgeCount) || 0) * (Number(row.judgeScore) || 0)
      )
    },
    round1(v) {
      const n = Number(v) || 0
      return Math.round(n * 10) / 10
    },
    windowText(row) {
      const f = v => (v ? String(v).replace('T', ' ').slice(0, 16) : '')
      const start = f(row.startTime)
      const end = f(row.endTime)
      if (!start && !end) return '立即发布'
      return (start || '--') + ' ~ ' + (end || '--')
    }
  }
}

function num(v, fallback) {
  const n = Number(v)
  return isNaN(n) ? fallback : n
}

function emptySubjectItem() {
  return { id: null, title: '', description: '', score: 0, images: [], attachments: [] }
}

function emptyForm(examType) {
  const practical = examType === 'PRACTICAL'
  return {
    id: null,
    deptId: null,
    examName: '',
    examType: examType || 'THEORY',
    singleScore: 2,
    multiScore: 4,
    judgeScore: 2,
    passLine: 60,
    duration: practical ? 180 : 60,
    subjectItems: []
  }
}
</script>

<style lang="scss" scoped>
.page-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 18px; }
.eyebrow { color: #1764f5; font-size: 12px; letter-spacing: .05em; }
.title-line { display: flex; align-items: center; gap: 10px; }
.page-heading h2 { margin: 6px 0 8px; font-size: 22px; font-weight: 600; color: #1d2939; }
.heading-actions { flex: none; }
.query-form { margin-bottom: 8px; padding: 12px 14px 0; border: 1px solid #e7ecf3; background: #fff; }

.type-tabs { margin-bottom: 4px; }
.type-tabs ::v-deep .el-tabs__header { margin-bottom: 12px; }

.dm-card { margin-bottom: 14px; padding: 16px 18px 18px; background: #fff; border: 1px solid #e7ecf3; border-radius: 8px; }
.dm-head { display: flex; align-items: center; justify-content: space-between; gap: 14px; padding-bottom: 13px; margin-bottom: 12px; border-bottom: 1px solid #edf0f4; }
.dm-title { display: flex; align-items: center; gap: 9px; flex-wrap: wrap; }
.dm-title h3 { margin: 0; color: #1d2939; font-size: 16px; font-weight: 600; }
.dm-idx { display: inline-flex; width: 24px; height: 24px; align-items: center; justify-content: center; color: #1764f5; background: #e8f1fd; font-size: 12px; font-weight: 700; border-radius: 4px; }
.dm-meta { color: #667085; font-size: 12px; }
.exam-cell strong, .exam-cell small { display: block; }
.exam-cell strong { color: #1d2939; font-size: 14px; }
.exam-cell small { margin-top: 3px; color: #98a2b3; font-size: 12px; }
.num { color: #1764f5; font-size: 14px; }
.num.warn { color: #b54708; }
.sep { margin: 0 5px; color: #cfd6df; }
.muted { color: #98a2b3; font-size: 12px; }
.danger-text { color: #f56c6c; }

/* 弹窗：类型大卡 */
.type-pick { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 16px; }
.pick-card {
  position: relative; display: flex; align-items: flex-start; gap: 12px;
  padding: 13px 15px; background: #fff; border: 1.5px solid #e4e9f0; border-radius: 9px;
  cursor: pointer; transition: all .15s;
}
.pick-card:hover { border-color: #1764f5; }
.pick-card.on { border-color: #1764f5; background: #f6faff; box-shadow: 0 0 0 3px rgba(23, 100, 245, .07); }
.pick-card.on.green { border-color: #12a06a; background: #f5fcf9; box-shadow: 0 0 0 3px rgba(18, 160, 106, .07); }
.pick-card.locked { cursor: not-allowed; }
.pick-card.locked:hover { border-color: #e4e9f0; }
.pick-card.locked.on:hover { border-color: #1764f5; }
.pick-card.on.green.locked:hover { border-color: #12a06a; }
.pick-icon {
  flex: none; display: inline-flex; width: 34px; height: 34px; align-items: center; justify-content: center;
  color: #1764f5; background: #e8f1fd; font-size: 17px; border-radius: 8px;
}
.pick-icon.green { color: #12a06a; background: #e4f5ee; }
.pick-body { min-width: 0; }
.pick-body b { display: block; margin-bottom: 4px; color: #1d2939; font-size: 13.5px; }
.pick-body span { color: #667085; font-size: 11.5px; line-height: 1.65; }
.pick-check { position: absolute; top: 10px; right: 11px; color: #1764f5; font-size: 15px; }
.pick-card.on.green .pick-check { color: #12a06a; }

.form-sec { padding: 4px 0 10px; margin: 4px 0 14px; color: #344054; font-size: 13px; font-weight: 600; border-bottom: 1px solid #eef1f6; }
.fg2 { display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px; }
.fg2 .span2 { grid-column: span 2; }

@media (max-width: 1180px) {
  .type-pick { grid-template-columns: 1fr; }
  .fg2 { grid-template-columns: 1fr; }
  .fg2 .span2 { grid-column: span 1; }
}
</style>
