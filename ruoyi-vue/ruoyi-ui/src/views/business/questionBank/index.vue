<template>
  <div class="question-bank-page app-container">
    <header class="page-heading">
      <div>
        <div class="eyebrow">学习考核 / 部门运营</div>
        <div class="title-line">
          <h2>题库管理</h2>
          <el-tag size="mini" effect="plain" :type="isSuperAdmin ? 'warning' : 'success'">{{ isSuperAdmin ? '全局管理' : '本部门范围' }}</el-tag>
        </div>
        <p>
          {{ isSuperAdmin
            ? '查看全组织理论题目，可维护任一部门的题目池。'
            : '维护本部门理论题目池。理论考核按知识点从这里抽题。' }}
        </p>
      </div>
      <div class="heading-actions">
        <el-button icon="el-icon-refresh" size="small" @click="loadQuestions">刷新数据</el-button>
      </div>
    </header>

    <!-- 筛选条：所属部门（仅超管）+ 题干关键字 + 题型 -->
    <section class="filter-bar">
      <el-select
        v-if="isSuperAdmin"
        v-model="filters.deptId"
        size="small"
        clearable
        placeholder="全部部门"
        class="filter-select"
        @change="applyQuestionFilter"
      >
        <el-option v-for="dept in deptOptions" :key="dept.deptId" :label="dept.deptName" :value="dept.deptId" />
      </el-select>
      <el-input
        v-model="filters.keyword"
        size="small"
        clearable
        prefix-icon="el-icon-search"
        placeholder="搜索题干"
        class="filter-keyword"
        @keyup.enter.native="applyQuestionFilter"
        @clear="applyQuestionFilter"
      />
      <el-select v-model="filters.qtype" size="small" clearable placeholder="全部题型" class="filter-select" @change="applyQuestionFilter">
        <el-option label="单选题" value="SINGLE" />
        <el-option label="多选题" value="MULTI" />
        <el-option label="判断题" value="JUDGE" />
      </el-select>
      <span class="filter-summary">显示 {{ questionList.length }} / 共 {{ questionTotal }} 题</span>
      <el-button v-if="hasActiveFilter" type="text" size="small" icon="el-icon-refresh-left" @click="resetFilters">重置筛选</el-button>
    </section>

    <!-- 题目列表：一屏罗列全部理论题，知识点是归类维度 -->
    <section class="content-panel">
      <div class="panel-heading">
        <div>
          <h3>全部题目</h3>
          <p>{{ poolHint }}</p>
        </div>
        <div class="panel-actions">
          <el-button v-hasPermi="['business:question:list']" size="small" icon="el-icon-download" @click="handleExport">导出 Excel</el-button>
          <el-button v-hasPermi="['business:question:import']" size="small" icon="el-icon-upload2" @click="openImport">Excel 导入</el-button>
          <el-button v-hasPermi="['business:question:add']" type="primary" size="small" icon="el-icon-plus" @click="handleAddQuestion">新增题目</el-button>
        </div>
      </div>

      <el-table v-loading="questionLoading" :data="questionList" stripe class="bank-table">
        <el-table-column label="题型" width="86" align="center">
          <template slot-scope="s"><el-tag size="mini" effect="plain" :type="typeTag(s.row.qtype)">{{ typeLabel(s.row.qtype) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="题干" min-width="320" show-overflow-tooltip>
          <template slot-scope="s">{{ s.row.stem }}</template>
        </el-table-column>
        <el-table-column prop="answer" label="答案" width="80" align="center" />
        <el-table-column label="知识点（章节）" width="150">
          <template slot-scope="s"><span v-if="s.row.knowledgePoint">{{ s.row.knowledgePoint }}</span><span v-else class="muted">—</span></template>
        </el-table-column>
        <el-table-column v-if="isSuperAdmin" label="所属部门" width="120">
          <template slot-scope="s"><span v-if="s.row.deptName">{{ s.row.deptName }}</span><span v-else class="muted">—</span></template>
        </el-table-column>
        <el-table-column label="状态" width="76" align="center">
          <template slot-scope="s"><el-tag size="mini" effect="plain" :type="s.row.status === 1 ? 'success' : 'info'">{{ s.row.status === 1 ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="130" align="center">
          <template slot-scope="s">
            <el-button v-hasPermi="['business:question:edit']" type="text" size="mini" @click="handleEditQuestion(s.row)">编辑</el-button>
            <el-button v-hasPermi="['business:question:remove']" type="text" size="mini" class="danger-text" @click="handleDeleteQuestion(s.row)">删除</el-button>
          </template>
        </el-table-column>
        <template slot="empty">
          <div class="empty-block">
            <i class="el-icon-tickets" />
            <p v-if="hasActiveFilter">当前筛选条件下没有匹配的题目</p>
            <p v-else>本部门题目池还是空的，点右上角「新增题目」或「Excel 导入」开始录入</p>
            <el-button v-if="hasActiveFilter" size="mini" @click="resetFilters">清空筛选</el-button>
          </div>
        </template>
      </el-table>

      <pagination v-show="questionTotal > 0" :total="questionTotal" :page.sync="questionQuery.pageNum" :limit.sync="questionQuery.pageSize" @pagination="loadQuestions" />
    </section>

    <!-- 题目编辑弹窗 -->
    <el-dialog :title="questionDialogTitle" :visible.sync="questionDialogVisible" width="680px" append-to-body>
      <el-form ref="questionForm" :model="questionForm" :rules="questionRules" label-width="100px">
        <el-form-item v-if="isSuperAdmin" label="所属部门" prop="deptId">
          <el-select v-model="questionForm.deptId" placeholder="请选择题目所属部门" style="width:100%">
            <el-option v-for="dept in deptOptions" :key="dept.deptId" :label="dept.deptName" :value="dept.deptId" />
          </el-select>
        </el-form-item>
        <el-form-item label="题型" prop="qtype">
          <el-radio-group v-model="questionForm.qtype" @change="onQtypeChange">
            <el-radio-button label="SINGLE">单选题</el-radio-button>
            <el-radio-button label="MULTI">多选题</el-radio-button>
            <el-radio-button label="JUDGE">判断题</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="题干" prop="stem">
          <el-input v-model="questionForm.stem" type="textarea" :rows="3" placeholder="请输入题干" />
        </el-form-item>
        <template v-if="questionForm.qtype !== 'JUDGE'">
          <el-form-item v-for="opt in optionKeys" :key="opt" :label="'选项 ' + opt">
            <el-input v-model="questionForm.options[opt]" :placeholder="'选项 ' + opt + ' 内容'" />
          </el-form-item>
        </template>
        <el-form-item v-else label="选项">
          <div class="judge-options"><span class="judge-item"><b>A</b> 对</span><span class="judge-item"><b>B</b> 错</span></div>
        </el-form-item>
        <el-form-item label="正确答案" prop="answerList">
          <el-select v-if="questionForm.qtype === 'MULTI'" v-model="questionForm.answerList" multiple placeholder="选择多个正确答案" style="width:100%">
            <el-option v-for="opt in validOptionKeys" :key="opt" :label="opt" :value="opt" />
          </el-select>
          <el-select v-else-if="questionForm.qtype === 'JUDGE'" v-model="questionForm.answerList" placeholder="选择正确答案" style="width:100%">
            <el-option label="A（对）" value="A" /><el-option label="B（错）" value="B" />
          </el-select>
          <el-select v-else v-model="questionForm.answerList" placeholder="选择正确答案" style="width:100%">
            <el-option v-for="opt in validOptionKeys" :key="opt" :label="opt" :value="opt" />
          </el-select>
        </el-form-item>
        <el-form-item label="解析"><el-input v-model="questionForm.analysis" type="textarea" :rows="2" placeholder="答案解析（可选）" /></el-form-item>
        <el-form-item label="知识点（章节）">
          <el-input v-model="questionForm.knowledgePoint" placeholder="必填：理论考核按知识点抽题" maxlength="64" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="questionDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="questionSubmitting" @click="submitQuestion">确定</el-button>
      </span>
    </el-dialog>

    <!-- Excel 批量导入弹窗 -->
    <el-dialog title="Excel 批量导入" :visible.sync="importVisible" width="560px" append-to-body @close="resetImport">
      <el-form label-width="90px">
        <el-form-item v-if="isSuperAdmin" label="导入到部门" required>
          <el-select v-model="importDeptId" placeholder="请选择目标部门" style="width:100%">
            <el-option v-for="dept in deptOptions" :key="dept.deptId" :label="dept.deptName" :value="dept.deptId" />
          </el-select>
        </el-form-item>
        <el-form-item v-else label="导入到">
          <span class="import-target"><i class="el-icon-collection" />{{ deptName || '本部门' }}题目池</span>
        </el-form-item>
      </el-form>
      <el-upload
        ref="upload"
        drag
        action="#"
        :auto-upload="false"
        :limit="1"
        :on-change="onFileChange"
        :on-remove="onFileRemove"
        :on-exceed="onFileExceed"
        :file-list="fileList"
        accept=".xlsx,.xls"
      >
        <i class="el-icon-upload" />
        <div class="el-upload__text">将 Excel 拖到此处，或<em>点击选择文件</em></div>
        <div slot="tip" class="el-upload__tip">表头须为：题型 / 题干 / 选项A-D / 正确答案 / 解析 / 难度 / 知识点</div>
      </el-upload>
      <el-alert v-if="importResult" :closable="false" show-icon style="margin-top:12px"
                :type="importResult.failed > 0 ? 'warning' : 'success'" :title="importResultText" />
      <div v-if="importResult && importResult.errors && importResult.errors.length" class="import-errors">
        <p v-for="(e, idx) in importResult.errors.slice(0, 8)" :key="idx">{{ e }}</p>
      </div>
      <span slot="footer">
        <el-button @click="importVisible = false">关闭</el-button>
        <el-button type="primary" :loading="importing" @click="submitImport">开始导入</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { listQuestion, addQuestion, updateQuestion, delQuestion, downloadTemplate as downloadQuestionTemplate, importQuestions, exportQuestions } from '@/api/business/question'
import { listDept } from '@/api/system/dept'
import { mapGetters } from 'vuex'
import { isSuperAdminRole } from '@/utils/permission'

export default {
  name: 'QuestionBank',
  data() {
    return {
      questionLoading: false,
      questionList: [],
      questionTotal: 0,
      deptOptions: [],

      questionDialogVisible: false,
      questionForm: { id: null, deptId: null, qtype: 'SINGLE', stem: '', options: { A: '', B: '', C: '', D: '' }, answerList: [], analysis: '', knowledgePoint: '' },
      questionRules: {
        deptId: [{ required: true, message: '请选择所属部门', trigger: 'change' }],
        stem: [{ required: true, message: '请输入题干', trigger: 'blur' }]
      },
      questionSubmitting: false,

      optionKeys: ['A', 'B', 'C', 'D'],

      importVisible: false,
      uploadFile: null,
      importing: false,
      importResult: null,
      importDeptId: null,
      fileList: [],

      // 筛选（题目维度）：★ 2026-09-23 起题目直接按部门归属，题库不再是一级导航
      filters: { keyword: '', qtype: '', deptId: null },
      questionQuery: { pageNum: 1, pageSize: 10 }
    }
  },
  computed: {
    ...mapGetters(['deptName', 'deptId', 'roles']),
    isSuperAdmin() { return isSuperAdminRole(this.roles) },
    questionDialogTitle() { return this.questionForm.id ? '编辑题目' : '新增题目' },
    validOptionKeys() { return this.optionKeys.filter(k => (this.questionForm.options[k] || '').trim() !== '') },
    importResultText() {
      if (!this.importResult) return ''
      return `共 ${this.importResult.total} 条，成功 ${this.importResult.success} 条，失败 ${this.importResult.failed} 条`
    },
    /** 面板副标题：说明「这个池子里是什么」 */
    poolHint() {
      if (this.isSuperAdmin) {
        const d = (this.deptOptions || []).find(x => x.deptId === this.filters.deptId)
        return d ? `${d.deptName} · 按知识点归类，理论考核据此抽题` : '全部部门的理论题目 · 按知识点归类'
      }
      return `${this.deptName || '本部门'} · 按知识点归类，理论考核据此抽题`
    },
    hasActiveFilter() {
      const f = this.filters
      return !!(f.keyword || f.qtype || f.deptId)
    }
  },
  created() {
    if (this.isSuperAdmin) this.loadDepartments()
    this.loadQuestions()
  },
  methods: {
    loadDepartments() {
      listDept({ status: '0' }).then(res => {
        this.deptOptions = (res.data || []).filter(dept => dept.parentId !== 0)
      })
    },
    /** 拉取题目列表：所属部门（超管）+ 题型 + 题干关键字，全部走后端分页 */
    loadQuestions() {
      this.questionLoading = true
      const f = this.filters
      const params = {
        pageNum: this.questionQuery.pageNum,
        pageSize: this.questionQuery.pageSize,
        deptId: f.deptId || undefined,
        qtype: f.qtype || undefined,
        stem: (f.keyword || '').trim() || undefined
      }
      listQuestion(params).then(res => {
        this.questionList = res.rows || []
        this.questionTotal = res.total || 0
        this.questionLoading = false
      }).catch(() => { this.questionLoading = false })
    },
    applyQuestionFilter() {
      this.questionQuery.pageNum = 1
      this.loadQuestions()
    },
    resetFilters() {
      this.filters = { keyword: '', qtype: '', deptId: null }
      this.applyQuestionFilter()
    },

    handleAddQuestion() {
      this.questionForm = {
        id: null,
        deptId: this.isSuperAdmin ? (this.filters.deptId || null) : this.deptId,
        qtype: 'SINGLE',
        stem: '',
        options: { A: '', B: '', C: '', D: '' },
        answerList: [],
        analysis: '',
        knowledgePoint: ''
      }
      this.questionDialogVisible = true
      this.$nextTick(() => { this.$refs.questionForm && this.$refs.questionForm.clearValidate() })
    },
    handleEditQuestion(row) {
      this.questionForm = {
        id: row.id,
        deptId: row.deptId,
        qtype: row.qtype,
        stem: row.stem,
        options: { A: '', B: '', C: '', D: '' },
        answerList: row.answer ? row.answer.split(',') : [],
        analysis: row.analysis,
        knowledgePoint: row.knowledgePoint
      }
      this.parseOptions(row.optionsJson)
      this.questionDialogVisible = true
      this.$nextTick(() => { this.$refs.questionForm && this.$refs.questionForm.clearValidate() })
    },
    parseOptions(json) {
      try {
        const arr = JSON.parse(json || '[]')
        arr.forEach(o => { this.$set(this.questionForm.options, o.key, o.content) })
      } catch (e) { /* ignore */ }
    },
    onQtypeChange() {
      this.questionForm.answerList = []
    },
    submitQuestion() {
      this.$refs.questionForm.validate(valid => {
        if (!valid) return
        if (!this.questionForm.answerList || !this.questionForm.answerList.length) {
          this.$modal.msgWarning('请选择正确答案')
          return
        }
        if (!(this.questionForm.knowledgePoint || '').trim()) {
          this.$modal.msgWarning('请填写知识点（理论考核按知识点抽题）')
          return
        }
        const payload = {
          id: this.questionForm.id,
          deptId: this.isSuperAdmin ? this.questionForm.deptId : undefined,
          qtype: this.questionForm.qtype,
          stem: this.questionForm.stem,
          analysis: this.questionForm.analysis,
          knowledgePoint: this.questionForm.knowledgePoint.trim(),
          optionsJson: JSON.stringify(this.buildOptions()),
          answer: [...this.questionForm.answerList].sort().join(',')
        }
        this.questionSubmitting = true
        const fn = this.questionForm.id ? updateQuestion : addQuestion
        fn(payload).then(() => {
          this.$modal.msgSuccess('保存成功')
          this.questionDialogVisible = false
          this.questionSubmitting = false
          this.loadQuestions()
        }).catch(() => { this.questionSubmitting = false })
      })
    },
    buildOptions() {
      if (this.questionForm.qtype === 'JUDGE') {
        return [{ key: 'A', content: '对' }, { key: 'B', content: '错' }]
      }
      return this.optionKeys.filter(k => (this.questionForm.options[k] || '').trim() !== '')
        .map(k => ({ key: k, content: this.questionForm.options[k].trim() }))
    },
    handleDeleteQuestion(row) {
      this.$modal.confirm('确认删除该题目吗？').then(() => {
        delQuestion(row.id).then(() => {
          this.$modal.msgSuccess('删除成功')
          this.loadQuestions()
        })
      }).catch(() => {})
    },

    /** 导出：部门账号导本部门；超管需先选定部门（或按当前筛选部门） */
    handleExport() {
      const deptId = this.isSuperAdmin ? (this.filters.deptId || null) : this.ownDeptId()
      if (this.isSuperAdmin && !deptId) {
        this.$modal.msgWarning('请先在筛选条选择要导出的部门')
        return
      }
      exportQuestions(deptId).then(res => {
        const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
        const link = document.createElement('a')
        link.href = URL.createObjectURL(blob)
        link.download = '题目导出.xlsx'
        link.click()
        URL.revokeObjectURL(link.href)
      })
    },
    openImport() {
      this.resetImport()
      this.importDeptId = this.isSuperAdmin ? (this.filters.deptId || null) : null
      this.importVisible = true
    },
    resetImport() {
      this.importResult = null
      this.uploadFile = null
      this.fileList = []
    },
    onFileChange(file, list) {
      // el-upload 在 :limit 下会把 file-list 交回，取最后一项作为当前选择
      this.fileList = list || []
      this.uploadFile = file && file.raw ? file.raw : (this.fileList.length ? this.fileList[this.fileList.length - 1].raw : null)
    },
    onFileRemove(file, list) {
      this.fileList = list || []
      this.uploadFile = this.fileList.length ? this.fileList[this.fileList.length - 1].raw : null
    },
    onFileExceed(files) {
      // 超过 limit 时不静默忽略：替换为最新选择的文件，保证「选了就能导」
      const f = files[0]
      this.fileList = [{ name: f.name, raw: f }]
      this.uploadFile = f
      if (this.$refs.upload) this.$refs.upload.clearFiles()
    },
    submitImport() {
      const deptId = this.isSuperAdmin ? this.importDeptId : this.ownDeptId()
      if (this.isSuperAdmin && !deptId) { this.$modal.msgWarning('请先选择导入到的部门'); return }
      if (!this.uploadFile) { this.$modal.msgWarning('请先选择 Excel 文件'); return }
      const formData = new FormData()
      formData.append('file', this.uploadFile)
      this.importing = true
      importQuestions(deptId, formData).then(res => {
        this.importResult = res.data
        this.importing = false
        this.loadQuestions()
      }).catch(() => { this.importing = false })
    },
    downloadTemplate() {
      downloadQuestionTemplate().then(res => {
        const blob = new Blob([res])
        const link = document.createElement('a')
        link.href = URL.createObjectURL(blob)
        link.download = '题目导入模板.xlsx'
        link.click()
        URL.revokeObjectURL(link.href)
      })
    },

    /** 部门账号自己的部门ID（后端对部门账号会强制本部门，这里传 0 仅为占位，避免 undefined 进 URL） */
    ownDeptId() {
      return this.deptId || 0
    },
    typeLabel(qtype) {
      return { SINGLE: '单选', MULTI: '多选', JUDGE: '判断' }[qtype] || qtype
    },
    typeTag(qtype) {
      return { SINGLE: 'success', MULTI: 'warning', JUDGE: 'primary' }[qtype] || 'info'
    }
  }
}
</script>

<style lang="scss" scoped>
/* ============ 页头 ============ */
.page-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 16px; }
.eyebrow { color: #1764f5; font-size: 12px; letter-spacing: .05em; }
.title-line { display: flex; align-items: center; gap: 10px; }
.page-heading h2 { margin: 6px 0 8px; font-size: 22px; font-weight: 600; color: #1d2939; }
.page-heading p { margin: 0; color: #667085; font-size: 13px; }
.heading-actions { flex: none; }

/* ============ 筛选条 ============ */
.filter-bar {
  display: flex; flex-wrap: wrap; align-items: center; gap: 10px;
  margin-bottom: 14px; padding: 12px 16px; background: #fff;
  border: 1px solid #e7ecf3; border-radius: 6px;
}
.filter-keyword { width: 240px; }
.filter-select { width: 150px; }
.filter-summary { margin-left: auto; color: #667085; font-size: 12px; }

/* ============ 题目列表 ============ */
.content-panel { margin-bottom: 16px; padding: 18px 20px; background: #fff; border: 1px solid #e7ecf3; border-radius: 6px; }
.panel-heading { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.panel-heading h3 { margin: 0 0 5px; font-size: 16px; font-weight: 600; color: #1d2939; }
.panel-heading p { margin: 0; color: #8490a0; font-size: 12px; }
.panel-actions { display: flex; align-items: center; gap: 8px; }

.muted { color: #9aa5b2; }
.danger-text { color: #f56c6c; }
.judge-options { display: flex; gap: 18px; color: #52637a; }
.judge-item b { margin-right: 6px; color: #1764f5; }
.import-errors { margin-top: 10px; padding: 10px 12px; border-radius: 4px; background: #fff7f7; color: #b54708; font-size: 12px; line-height: 1.7; }
.import-target { color: #475467; font-size: 13px; }
.import-target i { margin-right: 4px; color: #98a2b3; }

/* ============ 空态（区分「没有」与「筛没了」） ============ */
.empty-block { padding: 26px 0; text-align: center; }
.empty-block i { color: #d0d5dd; font-size: 34px; }
.empty-block p { margin: 10px 0 12px; color: #8490a0; font-size: 13px; }

/* ============ 窄屏 ============ */
@media (max-width: 1100px) {
  .filter-keyword { width: 100%; }
  .filter-summary { margin-left: 0; }
}
@media (max-width: 700px) {
  .page-heading { flex-direction: column; }
  .content-panel { padding: 14px 12px; }
}
</style>
