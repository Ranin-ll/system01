<template>
  <div class="question-bank-page app-container">
    <header class="page-heading">
      <div>
        <div class="eyebrow">学习考核 / 部门运营</div>
        <div class="title-line">
          <h2>题库管理</h2>
          <el-tag size="mini" effect="plain" :type="isSuperAdmin ? 'warning' : 'success'">{{ isSuperAdmin ? '全局管理' : '本部门范围' }}</el-tag>
        </div>
        <p>{{ isSuperAdmin ? '查看全组织题库与题目，可按部门筛选并维护任一部门数据。' : '维护本部门题库与题目，题目支持 Excel 批量导入；发布后实习生可在考试中心参与考核。' }}</p>
      </div>
      <div class="heading-actions">
        <el-button icon="el-icon-refresh" size="small" @click="loadBanks">刷新数据</el-button>
        <el-button v-hasPermi="['business:bank:add']" type="primary" icon="el-icon-plus" size="small" @click="handleAddBank">新建题库</el-button>
      </div>
    </header>

    <section class="scope-strip">
      <div class="scope-main"><span class="scope-icon"><i class="el-icon-office-building" /></span><div><strong>{{ isSuperAdmin ? '全局题库管理' : (deptName || '当前部门') }}</strong><span>{{ isSuperAdmin ? '可查看并维护五个部门的题库数据' : '仅管理本部门题库与题目' }}</span></div></div>
      <div class="scope-meta"><span><i class="el-icon-lock" /> 数据范围由登录账号决定</span></div>
    </section>

    <!-- 题库列表 -->
    <section class="content-panel">
      <div class="panel-heading"><div><h3>题库目录</h3><p>题库按部门归属，点击「管理题目」维护题库内的题目。</p></div></div>

      <el-table v-loading="bankLoading" :data="bankList" stripe class="bank-table" empty-text="暂无题库，点击“新建题库”开始配置" @row-click="selectBank">
        <el-table-column label="题库名称" min-width="200">
          <template slot-scope="scope">
            <div class="bank-cell"><span class="bank-icon"><i class="el-icon-collection" /></span><div><strong>{{ scope.row.bankName }} <el-tag v-if="scope.row.bankType === 'PRACTICE'" size="mini" type="warning" effect="plain">模拟题库</el-tag></strong><small>{{ scope.row.description || '暂无说明' }}</small></div></div>
          </template>
        </el-table-column>
        <el-table-column label="所属部门" min-width="130">
          <template slot-scope="scope"><span class="dept-text"><i class="el-icon-office-building" />{{ scope.row.deptName || '未设置' }}</span></template>
        </el-table-column>
        <el-table-column label="题目数" width="90" align="center">
          <template slot-scope="scope"><strong class="table-number">{{ scope.row.questionCount || 0 }}</strong></template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template slot-scope="scope"><el-tag size="mini" effect="plain" :type="scope.row.status === 'ENABLED' ? 'success' : 'info'">{{ scope.row.status === 'ENABLED' ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="230" align="center">
          <template slot-scope="scope">
            <el-button type="text" size="mini" icon="el-icon-s-management" @click.stop="selectBank(scope.row)">管理题目</el-button>
            <el-button v-hasPermi="['business:bank:edit']" type="text" size="mini" icon="el-icon-edit" @click.stop="handleEditBank(scope.row)">编辑</el-button>
            <el-button v-if="scope.row.bankType !== 'PRACTICE'" v-hasPermi="['business:bank:remove']" type="text" size="mini" icon="el-icon-delete" class="danger-text" @click.stop="handleDeleteBank(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <!-- 题目管理 -->
    <section v-if="currentBank" class="content-panel question-panel">
      <div class="panel-heading">
        <div><h3>题目管理 <el-tag size="mini" type="primary" effect="plain">{{ currentBank.bankName }}</el-tag></h3><p>维护单选、多选、判断题目，支持 Excel 批量导入。</p></div>
        <div class="panel-actions">
          <el-button v-hasPermi="['business:question:import']" size="mini" icon="el-icon-upload2" @click="openImport">批量导入</el-button>
          <el-button v-hasPermi="['business:question:add']" type="primary" plain size="mini" icon="el-icon-plus" @click="handleAddQuestion">新增题目</el-button>
        </div>
      </div>

      <el-form :inline="true" size="small" class="query-form" @submit.native.prevent>
        <el-form-item label="题型">
          <el-select v-model="queryParams.qtype" clearable placeholder="全部题型" style="width:120px" @change="loadQuestions">
            <el-option label="单选题" value="SINGLE" /><el-option label="多选题" value="MULTI" /><el-option label="判断题" value="JUDGE" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="queryParams.stem" clearable placeholder="题干/知识点" style="width:180px" @keyup.enter.native="loadQuestions" />
        </el-form-item>
        <el-form-item><el-button type="primary" icon="el-icon-search" @click="loadQuestions">查询</el-button><el-button icon="el-icon-refresh-left" @click="resetQuery">重置</el-button></el-form-item>
      </el-form>

      <el-table v-loading="questionLoading" :data="questionList" stripe class="question-table" empty-text="该题库暂无题目，点击“新增题目”或“批量导入”">
        <el-table-column label="题干" min-width="320" show-overflow-tooltip>
          <template slot-scope="scope">
            <div class="stem-cell"><el-tag size="mini" :type="typeTag(scope.row.qtype)" effect="plain">{{ typeLabel(scope.row.qtype) }}</el-tag><span>{{ scope.row.stem }}</span></div>
          </template>
        </el-table-column>
        <el-table-column label="答案" width="90" align="center">
          <template slot-scope="scope"><strong class="answer-text">{{ scope.row.answer }}</strong></template>
        </el-table-column>
        <el-table-column label="难度" width="80" align="center">
          <template slot-scope="scope">{{ difficultyLabel(scope.row.difficulty) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template slot-scope="scope"><el-tag size="mini" effect="plain" :type="scope.row.status === 1 ? 'success' : 'info'">{{ scope.row.status === 1 ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center">
          <template slot-scope="scope">
            <el-button v-hasPermi="['business:question:edit']" type="text" size="mini" icon="el-icon-edit" @click="handleEditQuestion(scope.row)">编辑</el-button>
            <el-button v-hasPermi="['business:question:remove']" type="text" size="mini" icon="el-icon-delete" class="danger-text" @click="handleDeleteQuestion(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="questionTotal > 0" :total="questionTotal" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="loadQuestions" />
    </section>

    <!-- 题库编辑弹窗 -->
    <el-dialog :title="bankDialogTitle" :visible.sync="bankDialogVisible" width="520px" append-to-body>
      <el-form ref="bankForm" :model="bankForm" :rules="bankRules" label-width="90px">
        <el-form-item v-if="isSuperAdmin && !bankForm.id" label="所属部门" prop="deptId">
          <el-select v-model="bankForm.deptId" placeholder="请选择所属部门" style="width:100%">
            <el-option v-for="dept in deptOptions" :key="dept.deptId" :label="dept.deptName" :value="dept.deptId" />
          </el-select>
        </el-form-item>
        <el-form-item label="题库名称" prop="bankName"><el-input v-model="bankForm.bankName" placeholder="请输入题库名称" maxlength="128" /></el-form-item>
        <el-form-item label="题库说明"><el-input v-model="bankForm.description" type="textarea" :rows="3" placeholder="填写题库说明（可选）" maxlength="500" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="bankForm.status"><el-radio label="ENABLED">启用</el-radio><el-radio label="DISABLED">停用</el-radio></el-radio-group></el-form-item>
      </el-form>
      <span slot="footer"><el-button @click="bankDialogVisible = false">取消</el-button><el-button type="primary" :loading="bankSubmitting" @click="submitBank">确定</el-button></span>
    </el-dialog>

    <!-- 题目编辑弹窗 -->
    <el-dialog :title="questionDialogTitle" :visible.sync="questionDialogVisible" width="680px" append-to-body>
      <el-form ref="questionForm" :model="questionForm" :rules="questionRules" label-width="90px">
        <el-form-item label="题型" prop="qtype">
          <el-radio-group v-model="questionForm.qtype" @change="onQtypeChange">
            <el-radio-button label="SINGLE">单选题</el-radio-button>
            <el-radio-button label="MULTI">多选题</el-radio-button>
            <el-radio-button label="JUDGE">判断题</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="题干" prop="stem"><el-input v-model="questionForm.stem" type="textarea" :rows="3" placeholder="请输入题干" /></el-form-item>

        <template v-if="questionForm.qtype !== 'JUDGE'">
          <el-form-item v-for="opt in optionKeys" :key="opt" :label="'选项 ' + opt">
            <el-input v-model="questionForm.options[opt]" :placeholder="'选项 ' + opt + ' 内容'" />
          </el-form-item>
        </template>
        <el-form-item v-else label="选项">
          <div class="judge-options"><span class="judge-item"><b>A</b> 对</span><span class="judge-item"><b>B</b> 错</span></div>
        </el-form-item>

        <el-form-item label="正确答案" prop="answer">
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
        <el-form-item label="难度">
          <el-select v-model="questionForm.difficulty" style="width:160px"><el-option label="简单" value="EASY" /><el-option label="中等" value="MEDIUM" /><el-option label="困难" value="HARD" /></el-select>
        </el-form-item>
        <el-form-item label="知识点"><el-input v-model="questionForm.knowledgePoint" placeholder="知识点（可选）" maxlength="64" /></el-form-item>
      </el-form>
      <span slot="footer"><el-button @click="questionDialogVisible = false">取消</el-button><el-button type="primary" :loading="questionSubmitting" @click="submitQuestion">确定</el-button></span>
    </el-dialog>

    <!-- 批量导入弹窗 -->
    <el-dialog title="Excel 批量导入" :visible.sync="importVisible" width="560px" append-to-body>
      <div class="import-guide">
        <el-steps :active="importStep" align-center>
          <el-step title="下载模板" /><el-step title="填写题目" /><el-step title="上传导入" />
        </el-steps>
        <div class="import-body">
          <p>请先下载模板，按格式填写题目后上传。支持单选、多选、判断三种题型，一行一题。</p>
          <div class="import-actions">
            <el-button icon="el-icon-download" @click="downloadTemplate">下载 Excel 模板</el-button>
            <el-upload ref="upload" :auto-upload="false" :show-file-list="true" :limit="1" accept=".xlsx,.xls" :on-change="onFileChange">
              <el-button type="primary" icon="el-icon-upload2">选择文件</el-button>
            </el-upload>
          </div>
          <el-button type="primary" style="width:100%;margin-top:14px" :loading="importing" :disabled="!uploadFile" @click="submitImport">开始导入</el-button>
          <el-alert v-if="importResult" :title="importResultText" :type="importResult.failed > 0 ? 'warning' : 'success'" :closable="false" show-icon style="margin-top:14px" />
          <div v-if="importResult && importResult.errors && importResult.errors.length" class="import-errors">
            <div v-for="(err, i) in importResult.errors" :key="i">{{ err }}</div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listBank, addBank, updateBank, delBank } from '@/api/business/questionBank'
import { listQuestion, addQuestion, updateQuestion, delQuestion, downloadTemplate as downloadQuestionTemplate, importQuestions, uploadFile } from '@/api/business/question'
import { listDept } from '@/api/system/dept'
import { mapGetters } from 'vuex'

export default {
  name: 'QuestionBank',
  data() {
    return {
      bankLoading: false,
      bankList: [],
      currentBank: null,
      deptOptions: [],

      queryParams: { pageNum: 1, pageSize: 10, qtype: '', stem: '' },
      questionLoading: false,
      questionList: [],
      questionTotal: 0,

      bankDialogVisible: false,
      bankDialogTitle: '',
      bankForm: { id: null, deptId: null, bankName: '', description: '', status: 'ENABLED' },
      bankRules: {
        deptId: [{ required: true, message: '请选择所属部门', trigger: 'change' }],
        bankName: [{ required: true, message: '请输入题库名称', trigger: 'blur' }]
      },
      bankSubmitting: false,

      questionDialogVisible: false,
      questionDialogTitle: '',
      questionForm: { id: null, qtype: 'SINGLE', stem: '', options: { A: '', B: '', C: '', D: '' }, answerList: [], analysis: '', difficulty: 'MEDIUM', knowledgePoint: '' },
      questionRules: { stem: [{ required: true, message: '请输入题干', trigger: 'blur' }] },
      questionSubmitting: false,

      optionKeys: ['A', 'B', 'C', 'D'],
      importVisible: false,
      importStep: 0,
      uploadFile: null,
      importing: false,
      importResult: null
    }
  },
  computed: {
    ...mapGetters(['deptName', 'roles']),
    isSuperAdmin() { return this.roles.indexOf('SUPER_ADMIN') > -1 },
    bankDialogTitle() { return this.bankForm.id ? '编辑题库' : '新建题库' },
    questionDialogTitle() { return this.questionForm.id ? '编辑题目' : '新增题目' },
    validOptionKeys() { return this.optionKeys.filter(k => (this.questionForm.options[k] || '').trim() !== '') },
    importResultText() {
      if (!this.importResult) return ''
      return `共 ${this.importResult.total} 条，成功 ${this.importResult.success} 条，失败 ${this.importResult.failed} 条`
    }
  },
  created() {
    if (this.isSuperAdmin) this.loadDepartments()
    this.loadBanks()
  },
  methods: {
    loadDepartments() {
      listDept({ status: '0' }).then(res => {
        this.deptOptions = (res.data || []).filter(dept => dept.parentId !== 0)
      })
    },
    loadBanks() {
      this.bankLoading = true
      listBank({}).then(res => {
        this.bankList = res.rows || []
        this.bankLoading = false
        if (!this.currentBank && this.bankList.length) this.selectBank(this.bankList[0])
      }).catch(() => { this.bankLoading = false })
    },
    selectBank(row) {
      this.currentBank = row
      this.queryParams.pageNum = 1
      this.loadQuestions()
    },
    loadQuestions() {
      if (!this.currentBank) return
      this.questionLoading = true
      const params = { ...this.queryParams, bankId: this.currentBank.id }
      listQuestion(params).then(res => {
        this.questionList = res.rows || []
        this.questionTotal = res.total || 0
        this.questionLoading = false
      }).catch(() => { this.questionLoading = false })
    },
    resetQuery() {
      this.queryParams = { pageNum: 1, pageSize: 10, qtype: '', stem: '' }
      this.loadQuestions()
    },
    handleAddBank() {
      this.bankForm = { id: null, deptId: null, bankName: '', description: '', status: 'ENABLED' }
      this.bankDialogVisible = true
    },
    handleEditBank(row) {
      this.bankForm = { id: row.id, deptId: row.deptId, bankName: row.bankName, description: row.description, status: row.status }
      this.bankDialogVisible = true
    },
    submitBank() {
      this.$refs.bankForm.validate(valid => {
        if (!valid) return
        this.bankSubmitting = true
        const fn = this.bankForm.id ? updateBank : addBank
        fn(this.bankForm).then(() => {
          this.$modal.msgSuccess('保存成功')
          this.bankDialogVisible = false
          this.bankSubmitting = false
          this.loadBanks()
        }).catch(() => { this.bankSubmitting = false })
      })
    },
    handleDeleteBank(row) {
      this.$modal.confirm(`确认删除题库「${row.bankName}」吗？题库下的题目将一并处理。`).then(() => {
        delBank(row.id).then(() => {
          this.$modal.msgSuccess('删除成功')
          if (this.currentBank && this.currentBank.id === row.id) this.currentBank = null
          this.loadBanks()
        })
      }).catch(() => {})
    },
    handleAddQuestion() {
      this.questionForm = { id: null, qtype: 'SINGLE', stem: '', options: { A: '', B: '', C: '', D: '' }, answerList: [], analysis: '', difficulty: 'MEDIUM', knowledgePoint: '' }
      this.questionDialogVisible = true
    },
    handleEditQuestion(row) {
      this.questionForm = {
        id: row.id, qtype: row.qtype, stem: row.stem,
        options: { A: '', B: '', C: '', D: '' },
        answerList: row.answer ? row.answer.split(',') : [],
        analysis: row.analysis, difficulty: row.difficulty, knowledgePoint: row.knowledgePoint
      }
      this.parseOptions(row.optionsJson)
      this.questionDialogVisible = true
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
        const payload = {
          id: this.questionForm.id,
          bankId: this.currentBank.id,
          qtype: this.questionForm.qtype,
          stem: this.questionForm.stem,
          analysis: this.questionForm.analysis,
          difficulty: this.questionForm.difficulty,
          knowledgePoint: this.questionForm.knowledgePoint,
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
          this.loadBanks()
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
          this.loadBanks()
        })
      }).catch(() => {})
    },
    openImport() {
      this.importResult = null
      this.uploadFile = null
      this.importStep = 0
      this.importVisible = true
    },
    onFileChange(file) {
      this.uploadFile = file.raw
      this.importStep = 1
    },
    submitImport() {
      if (!this.uploadFile) { this.$modal.msgWarning('请先选择 Excel 文件'); return }
      const formData = new FormData()
      formData.append('file', this.uploadFile)
      this.importing = true
      this.importStep = 2
      importQuestions(this.currentBank.id, formData).then(res => {
        this.importResult = res.data
        this.importing = false
        this.importStep = 3
        this.loadQuestions()
        this.loadBanks()
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
    typeLabel(qtype) {
      return { SINGLE: '单选', MULTI: '多选', JUDGE: '判断' }[qtype] || qtype
    },
    typeTag(qtype) {
      return { SINGLE: 'success', MULTI: 'warning', JUDGE: 'primary' }[qtype] || 'info'
    },
    difficultyLabel(d) {
      return { EASY: '简单', MEDIUM: '中等', HARD: '困难' }[d] || d
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
.scope-strip { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; padding: 14px 18px; border: 1px solid #e7ecf3; border-radius: 6px; background: #fff; }
.scope-main { display: flex; align-items: center; gap: 12px; }
.scope-icon { display: flex; width: 38px; height: 38px; align-items: center; justify-content: center; color: #1764f5; background: #edf4ff; font-size: 19px; border-radius: 6px; }
.scope-main strong, .scope-main span { display: block; }
.scope-main strong { color: #1d2939; font-size: 14px; }
.scope-main span { color: #8490a0; font-size: 12px; margin-top: 2px; }
.scope-meta { color: #8490a0; font-size: 12px; }
.content-panel { margin-bottom: 16px; padding: 18px 20px; background: #fff; border: 1px solid #e7ecf3; border-radius: 6px; }
.panel-heading { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.panel-heading h3 { margin: 0 0 5px; font-size: 16px; font-weight: 600; color: #1d2939; }
.panel-heading p { margin: 0; color: #8490a0; font-size: 12px; }
.panel-actions { display: flex; gap: 8px; }
.bank-cell { display: flex; align-items: center; gap: 12px; }
.bank-icon { display: flex; width: 38px; height: 38px; align-items: center; justify-content: center; color: #fff; background: #1764f5; font-size: 18px; border-radius: 6px; }
.bank-cell strong, .bank-cell small { display: block; }
.bank-cell strong { color: #1d2939; font-size: 14px; }
.bank-cell small { color: #98a2b3; font-size: 12px; margin-top: 3px; max-width: 320px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.dept-text { color: #475467; font-size: 13px; }
.dept-text i { margin-right: 4px; color: #98a2b3; }
.table-number { color: #1764f5; font-size: 16px; }
.danger-text { color: #f56c6c; }
.query-form { padding: 4px 0 10px; border-bottom: 1px solid #edf0f4; }
.stem-cell { display: flex; align-items: center; gap: 8px; }
.stem-cell span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.answer-text { color: #23966f; }
.judge-options { display: flex; gap: 16px; }
.judge-item { display: inline-flex; align-items: center; gap: 8px; color: #475467; }
.judge-item b { display: inline-flex; width: 24px; height: 24px; align-items: center; justify-content: center; background: #edf4ff; color: #1764f5; border-radius: 4px; }
.import-guide { padding-top: 6px; }
.import-body { margin-top: 18px; color: #475467; font-size: 13px; line-height: 1.7; }
.import-actions { display: flex; align-items: center; gap: 14px; margin-top: 12px; }
.import-errors { max-height: 160px; margin-top: 10px; padding: 10px; overflow-y: auto; color: #e6a23c; font-size: 12px; background: #fdf6ec; border: 1px solid #faecd8; border-radius: 4px; line-height: 1.8; }
</style>
