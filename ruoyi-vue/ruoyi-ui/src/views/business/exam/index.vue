<template>
  <div class="exam-page app-container">
    <header class="page-heading">
      <div>
        <div class="eyebrow">学习考核 / 部门运营</div>
        <div class="title-line">
          <h2>考核管理</h2>
          <el-tag size="mini" effect="plain" :type="readOnly ? 'info' : 'success'">{{ readOnly ? '全局只读' : '本部门范围' }}</el-tag>
        </div>
        <p>{{ readOnly ? '查看全组织考核安排。' : '创建理论考核与实操考核，发布考核、批改答卷并发布成绩。' }}</p>
      </div>
      <div class="heading-actions">
        <el-button icon="el-icon-refresh" size="small" @click="loadList">刷新</el-button>
        <el-button v-if="!readOnly" v-hasPermi="['business:bank:add']" type="primary" icon="el-icon-plus" size="small" @click="handleAdd">新建考核</el-button>
      </div>
    </header>

    <el-table v-loading="loading" :data="examList" stripe empty-text="暂无考核">
      <el-table-column label="考核名称" min-width="200">
        <template slot-scope="scope">
          <div class="exam-cell"><strong>{{ scope.row.examName }}</strong><small>{{ examTypeDesc(scope.row) }}</small></div>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="90" align="center">
        <template slot-scope="scope">
          <el-tag size="mini" :type="scope.row.examType === 'PRACTICAL' ? 'danger' : 'success'">{{ scope.row.examType === 'PRACTICAL' ? '实操' : '理论' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="部门" width="130" align="center">
        <template slot-scope="scope">{{ scope.row.deptName || '-' }}</template>
      </el-table-column>
      <el-table-column label="已作答" width="90" align="center">
        <template slot-scope="scope"><strong class="num">{{ scope.row.answeredCount || 0 }}</strong></template>
      </el-table-column>
      <el-table-column label="状态" width="150" align="center">
        <template slot-scope="scope">
          <el-badge v-if="scope.row.status === 'GRADING'" :value="scope.row.pendingCount || 0" :max="99" class="status-badge">
            <el-tag size="mini" :type="statusTag(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
          </el-badge>
          <el-tag v-else size="mini" :type="statusTag(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="330" align="center">
        <template slot-scope="scope">
          <el-button type="text" size="mini" icon="el-icon-view" @click="goGrading(scope.row)">查看详情</el-button>
          <el-button v-if="scope.row.status === 'DRAFT' && !readOnly" v-hasPermi="['business:bank:edit']" type="text" size="mini" icon="el-icon-upload" class="primary-text" @click="handlePublish(scope.row)">发布</el-button>
          <el-button v-if="scope.row.status === 'PUBLISHED' && !readOnly" v-hasPermi="['business:bank:edit']" type="text" size="mini" @click="handleChangeStatus(scope.row, 'DISABLED')">停用</el-button>
          <el-button v-if="scope.row.status === 'DISABLED' && !readOnly" v-hasPermi="['business:bank:edit']" type="text" size="mini" class="primary-text" @click="handleChangeStatus(scope.row, 'PUBLISHED')">启用</el-button>
          <el-button v-if="(scope.row.status === 'DRAFT' || scope.row.status === 'PUBLISHED' || scope.row.status === 'DISABLED') && !readOnly" v-hasPermi="['business:bank:edit']" type="text" size="mini" icon="el-icon-edit" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button v-if="!readOnly" v-hasPermi="['business:bank:remove']" type="text" size="mini" icon="el-icon-delete" class="danger-text" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="loadList" />

    <!-- 新建/编辑弹窗 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="560px" append-to-body>
      <el-form ref="examForm" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="考核名称" prop="examName"><el-input v-model="form.examName" placeholder="请输入考核名称" /></el-form-item>
        <el-form-item label="考核类型" prop="examType">
          <el-radio-group v-model="form.examType" :disabled="!!form.id">
            <el-radio-button label="THEORY">理论考核</el-radio-button>
            <el-radio-button label="PRACTICAL">实操考核</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <template v-if="form.examType === 'THEORY'">
          <el-form-item label="所属题库" prop="bankId">
            <el-select v-model="form.bankId" placeholder="选择题库" style="width:100%">
              <el-option v-for="b in bankOptions" :key="b.id" :label="b.bankName" :value="b.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="单选题数量"><el-input-number v-model="form.singleCount" :min="0" :max="100" /></el-form-item>
          <el-form-item label="单选每题分"><el-input-number v-model="form.singleScore" :min="0" :max="100" :precision="1" /></el-form-item>
          <el-form-item label="多选题数量"><el-input-number v-model="form.multiCount" :min="0" :max="100" /></el-form-item>
          <el-form-item label="多选每题分"><el-input-number v-model="form.multiScore" :min="0" :max="100" :precision="1" /></el-form-item>
          <el-form-item label="判断题数量"><el-input-number v-model="form.judgeCount" :min="0" :max="100" /></el-form-item>
          <el-form-item label="判断每题分"><el-input-number v-model="form.judgeScore" :min="0" :max="100" :precision="1" /></el-form-item>
        </template>

        <template v-else>
          <el-form-item label="实操题干" prop="subjectContent">
            <el-input v-model="form.subjectContent" type="textarea" :rows="4" placeholder="请输入实操考核题干（要求实习生完成的任务说明）" />
          </el-form-item>
          <el-form-item label="参考附件">
            <el-upload :auto-upload="false" :limit="1" :show-file-list="false" accept="*" :on-change="onSubjectFile">
              <el-button size="small" icon="el-icon-upload2">选择附件（可选）</el-button>
            </el-upload>
            <div v-if="form.subjectAttachment" class="attachment-tip"><i class="el-icon-paperclip" /> 已上传参考附件（点击重新选择可替换）</div>
          </el-form-item>
        </template>

        <el-form-item label="通过线(分)"><el-input-number v-model="form.passLine" :min="0" :max="100" :precision="1" /></el-form-item>
      </el-form>
      <span slot="footer"><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button></span>
    </el-dialog>
  </div>
</template>

<script>
import { listExam, addExam, updateExam, delExam, publishExam, changeExamStatus, uploadFile } from '@/api/business/exam'
import { listBank } from '@/api/business/questionBank'
import { mapGetters } from 'vuex'

export default {
  name: 'ExamManage',
  data() {
    return {
      loading: false,
      examList: [],
      total: 0,
      queryParams: { pageNum: 1, pageSize: 10, examMode: '' },
      bankOptions: [],
      dialogVisible: false,
      dialogTitle: '',
      form: { id: null, examName: '', examMode: 'FORMAL', examType: 'THEORY', bankId: null, singleCount: 5, multiCount: 3, judgeCount: 2, singleScore: 2, multiScore: 4, judgeScore: 2, subjectContent: '', subjectAttachment: '', passLine: 60 },
      rules: {
        examName: [{ required: true, message: '请输入考核名称', trigger: 'blur' }],
        bankId: [{ required: true, message: '请选择题库', trigger: 'change' }],
        subjectContent: [{ required: true, message: '请输入实操题干', trigger: 'blur' }]
      },
      submitting: false
    }
  },
  computed: {
    ...mapGetters(['roles']),
    readOnly() { return this.roles.indexOf('SUPER_ADMIN') > -1 }
  },
  created() {
    this.loadList()
    this.loadBanks()
  },
  methods: {
    loadList() {
      this.loading = true
      listExam(this.queryParams).then(res => {
        this.examList = res.rows || []
        this.total = res.total || 0
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    loadBanks() {
      listBank({}).then(res => { this.bankOptions = res.rows || [] })
    },
    examTypeDesc(row) {
      if (row.examType === 'PRACTICAL') {
        return '实操考核 · 通过线 ' + row.passLine + ' 分'
      }
      const parts = []
      if (row.singleCount > 0) parts.push('单选 ' + row.singleCount)
      if (row.multiCount > 0) parts.push('多选 ' + row.multiCount)
      if (row.judgeCount > 0) parts.push('判断 ' + row.judgeCount)
      return (row.bankName || '未关联题库') + ' · ' + parts.join(' / ') + ' · 通过线 ' + row.passLine + ' 分'
    },
    handleAdd() {
      this.form = { id: null, examName: '', examMode: 'FORMAL', examType: 'THEORY', bankId: null, singleCount: 5, multiCount: 3, judgeCount: 2, singleScore: 2, multiScore: 4, judgeScore: 2, subjectContent: '', subjectAttachment: '', passLine: 60 }
      this.dialogTitle = '新建考核'
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.examForm && this.$refs.examForm.clearValidate())
    },
    handleEdit(row) {
      this.form = {
        id: row.id, examName: row.examName, examMode: row.examMode || 'FORMAL', examType: row.examType || 'THEORY',
        bankId: row.bankId, singleCount: row.singleCount || 0, multiCount: row.multiCount || 0, judgeCount: row.judgeCount || 0,
        singleScore: row.singleScore, multiScore: row.multiScore, judgeScore: row.judgeScore,
        subjectContent: row.subjectContent || '', subjectAttachment: row.subjectAttachment || '',
        passLine: row.passLine
      }
      this.dialogTitle = '编辑考核'
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.examForm && this.$refs.examForm.clearValidate())
    },
    onSubjectFile(file) {
      const formData = new FormData()
      formData.append('file', file.raw)
      uploadFile(formData).then(res => {
        this.form.subjectAttachment = res.fileName
        this.$modal.msgSuccess('附件上传成功')
      })
    },
    submitForm() {
      this.$refs.examForm.validate(valid => {
        if (!valid) return
        this.submitting = true
        const payload = { ...this.form }
        if (payload.examType === 'THEORY') {
          payload.subjectContent = null
          payload.subjectAttachment = null
        } else {
          payload.bankId = null
          payload.questionCount = null
          payload.singleCount = null
          payload.multiCount = null
          payload.judgeCount = null
          payload.singleScore = null
          payload.multiScore = null
          payload.judgeScore = null
        }
        const fn = payload.id ? updateExam : addExam
        fn(payload).then(() => {
          this.$modal.msgSuccess('保存成功')
          this.dialogVisible = false
          this.submitting = false
          this.loadList()
        }).catch(() => { this.submitting = false })
      })
    },
    handlePublish(row) {
      this.$modal.confirm(`确认发布考核「${row.examName}」吗？发布后实习生可参加。`).then(() => {
        publishExam(row.id).then(() => { this.$modal.msgSuccess('发布成功'); this.loadList() })
      }).catch(() => {})
    },
    handleDelete(row) {
      this.$modal.confirm(`确认删除考核「${row.examName}」吗？`).then(() => {
        delExam(row.id).then(() => { this.$modal.msgSuccess('删除成功'); this.loadList() })
      }).catch(() => {})
    },
    goGrading(row) {
      this.$router.push('/assessment/department/exam/grading/' + row.id)
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.loadList()
    },
    handleChangeStatus(row, status) {
      const label = status === 'PUBLISHED' ? '启用' : '停用'
      this.$modal.confirm(`确认${label}考核「${row.examName}」吗？`).then(() => {
        changeExamStatus(row.id, status).then(() => { this.$modal.msgSuccess(`${label}成功`); this.loadList() })
      }).catch(() => {})
    },
    statusLabel(s) {
      return { DRAFT: '待发布', PUBLISHED: '已发布', GRADING: '待批改', DISABLED: '已停用' }[s] || s
    },
    statusTag(s) {
      return { DRAFT: 'info', PUBLISHED: 'success', GRADING: 'warning', DISABLED: 'danger' }[s] || 'info'
    }
  }
}
</script>

<style lang="scss" scoped>
.page-heading { margin-bottom: 18px; }
.eyebrow { color: #1764f5; font-size: 12px; letter-spacing: .05em; }
.title-line { display: flex; align-items: center; gap: 10px; }
.page-heading h2 { margin: 6px 0 8px; font-size: 22px; font-weight: 600; color: #1d2939; }
.page-heading p { margin: 0; color: #667085; font-size: 13px; }
.heading-actions { text-align: right; }
.exam-cell strong, .exam-cell small { display: block; }
.exam-cell strong { color: #1d2939; font-size: 14px; }
.exam-cell small { color: #98a2b3; font-size: 12px; margin-top: 3px; }
.num { color: #1764f5; font-size: 15px; }
.primary-text { color: #1764f5; }
.danger-text { color: #f56c6c; }
.attachment-tip { margin-top: 6px; color: #23966f; font-size: 12px; }
.attachment-tip i { margin-right: 4px; }
.status-badge { display: inline-block; margin-right: 14px; }
.status-badge ::v-deep .el-badge__content { right: 0; top: 0; transform: none; }
</style>
