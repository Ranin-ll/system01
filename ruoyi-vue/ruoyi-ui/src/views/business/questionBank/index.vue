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
        <p class="page-note"><b>题库</b> 按<b>科目 / 课程</b>划分，可自由新建、上传题目、编辑与删除；题目按「<b>知识点（章节）</b>」归类；「<b>使用范围</b>」只是可选标签（正式考核用 / 模拟考核用），不影响题库本身。</p>
      </div>
      <div class="heading-actions">
        <el-button icon="el-icon-refresh" size="small" @click="loadBanks">刷新数据</el-button>
        <el-button v-hasPermi="['business:bank:add']" type="primary" icon="el-icon-plus" size="small" @click="handleAddBank">新建题库</el-button>
      </div>
    </header>

    <!-- ① 总览：先看清家底，点一下即按该口径筛选 -->
    <section class="kpi-row">
      <button v-for="card in kpiCards" :key="card.key" type="button" class="kpi-card" :class="[{ active: activeKpi === card.key }, card.tone]" @click="applyKpi(card.key)">
        <span class="kpi-icon"><i :class="card.icon" /></span>
        <span class="kpi-body">
          <span class="kpi-value">{{ card.value }}<small v-if="card.unit">{{ card.unit }}</small></span>
          <span class="kpi-label">{{ card.label }}</span>
        </span>
        <span class="kpi-hint">{{ card.hint }}</span>
      </button>
    </section>

    <!-- ①.5 形态切换：理论题库 / 实操题库 -->
    <section class="kind-bar">
      <button
        v-for="k in kindTabs()"
        :key="k.value"
        type="button"
        class="kind-tab"
        :class="{ on: filters.bankKind === k.value }"
        @click="switchKind(k.value)"
      >
        {{ k.label }}
        <span class="kind-n">{{ k.count }}</span>
      </button>
      <span class="kind-hint">理论题库按题型组卷抽题；实操题库供考核挑选实操题目（逐题作业）</span>
    </section>

    <!-- ② 筛选条：关键字 / 部门 / 用途 / 状态 -->
    <section class="filter-bar">
      <el-input v-model="filters.keyword" size="small" clearable prefix-icon="el-icon-search" placeholder="搜索题库名称或说明" class="filter-keyword" />
      <el-select v-if="isSuperAdmin" v-model="filters.deptId" size="small" clearable placeholder="全部部门" class="filter-select">
        <el-option v-for="dept in deptOptions" :key="dept.deptId" :label="dept.deptName" :value="dept.deptId" />
      </el-select>
      <el-select v-model="filters.bankType" size="small" clearable placeholder="全部用途" class="filter-select">
        <el-option label="正式考核题库" value="FORMAL" />
        <el-option label="模拟考核题库" value="PRACTICE" />
        <el-option label="通用题库" value="COMMON" />
      </el-select>
      <el-select v-model="filters.status" size="small" clearable placeholder="全部状态" class="filter-select">
        <el-option label="启用" value="ENABLED" />
        <el-option label="停用" value="DISABLED" />
      </el-select>
      <span class="filter-summary">显示 {{ filteredBanks.length }} / {{ bankList.length }} 个题库</span>
      <el-button type="text" size="small" icon="el-icon-sort" @click="toggleSortByCount">{{ sortByCount ? '题目最多优先' : '按默认顺序' }}</el-button>
      <el-button v-if="hasActiveFilter" type="text" size="small" icon="el-icon-refresh-left" @click="resetFilters">重置筛选</el-button>
    </section>

    <!-- ③ 题库目录 -->
    <section class="content-panel">
      <div class="panel-heading">
        <div>
          <h3>题库目录</h3>
          <p>题库按部门归属；点「管理题目」进入详情页维护题目、查看题型与难度分布。</p>
        </div>
      </div>

      <el-table :key="'bank-table'" v-loading="bankLoading" :data="filteredBanks" stripe class="bank-table">
        <el-table-column label="题库名称" min-width="240">
          <template slot-scope="scope">
            <div class="bank-cell">
              <span class="bank-icon" :class="{ practice: scope.row.bankType === 'PRACTICE' }">
                <i :class="scope.row.bankType === 'PRACTICE' ? 'el-icon-edit-outline' : 'el-icon-collection'" />
              </span>
              <div class="bank-main">
                <strong>{{ scope.row.bankName }}</strong>
                <small>{{ scope.row.description || '暂无说明' }}</small>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="形态" width="96" align="center">
          <template slot-scope="scope">
            <el-tag size="mini" effect="plain" :type="scope.row.bankKind === 'PRACTICAL' ? 'warning' : ''">{{ bankKindLabel(scope.row.bankKind) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="用途标签" min-width="120" align="center">
          <template slot-scope="scope">
            <el-tag size="mini" effect="plain" :type="bankTypeTag(scope.row.bankType)">{{ bankTypeLabel(scope.row.bankType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="所属部门" min-width="120">
          <template slot-scope="scope"><span class="dept-text"><i class="el-icon-office-building" />{{ scope.row.deptName || '未设置' }}</span></template>
        </el-table-column>
        <el-table-column label="题目数" min-width="110" align="center">
          <template slot-scope="scope">
            <strong class="table-number">{{ scope.row.questionCount || 0 }}</strong>
            <small class="table-sub">{{ (scope.row.questionCount || 0) > 0 ? '题' : '未导入' }}</small>
          </template>
        </el-table-column>
        <el-table-column label="知识点(章节)" min-width="118" align="center">
          <template slot-scope="scope">
            <strong class="table-number">{{ scope.row.knowledgePointCount || 0 }}</strong>
            <small class="table-sub">{{ (scope.row.knowledgePointCount || 0) > 0 ? '个标签' : '未标注' }}</small>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template slot-scope="scope"><el-tag size="mini" effect="plain" :type="scope.row.status === 'ENABLED' ? 'success' : 'info'">{{ scope.row.status === 'ENABLED' ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="230" align="center">
          <template slot-scope="scope">
            <el-button type="text" size="mini" icon="el-icon-s-management" @click.stop="goDetail(scope.row)">管理题目</el-button>
            <el-button v-hasPermi="['business:bank:edit']" type="text" size="mini" icon="el-icon-edit" @click.stop="handleEditBank(scope.row)">编辑</el-button>
            <el-button v-hasPermi="['business:bank:remove']" type="text" size="mini" icon="el-icon-delete" class="danger-text" @click.stop="handleDeleteBank(scope.row)">删除</el-button>
          </template>
        </el-table-column>

        <template slot="empty">
          <div class="empty-block">
            <i class="el-icon-collection" />
            <p v-if="bankList.length">当前筛选条件下没有匹配的题库</p>
            <p v-else>暂无题库，点击右上角「新建题库」开始配置</p>
            <el-button v-if="hasActiveFilter" size="mini" @click="resetFilters">清空筛选</el-button>
          </div>
        </template>
      </el-table>
    </section>

    <!-- 题库编辑弹窗（保持原逻辑） -->
    <el-dialog :title="bankDialogTitle" :visible.sync="bankDialogVisible" width="520px" append-to-body>
      <el-form ref="bankForm" :model="bankForm" :rules="bankRules" label-width="90px">
        <el-form-item v-if="isSuperAdmin && !bankForm.id" label="所属部门" prop="deptId">
          <el-select v-model="bankForm.deptId" placeholder="请选择所属部门" style="width:100%">
            <el-option v-for="dept in deptOptions" :key="dept.deptId" :label="dept.deptName" :value="dept.deptId" />
          </el-select>
        </el-form-item>
        <el-form-item label="题库名称" prop="bankName"><el-input v-model="bankForm.bankName" placeholder="按科目 / 课程命名，例如：Java 后端基础" maxlength="128" /></el-form-item>
        <el-form-item label="题库形态" prop="bankKind">
          <el-radio-group v-model="bankForm.bankKind">
            <el-radio label="THEORY">理论题库</el-radio>
            <el-radio label="PRACTICAL">实操题库</el-radio>
          </el-radio-group>
          <div class="form-tip">理论题库按题型组卷抽题；实操题库存放「逐题作业」型题目（题干 / 方向 / 交付要求 / 附件 / 建议满分）。</div>
        </el-form-item>
        <el-form-item label="用途标签" prop="bankType">
          <el-radio-group v-model="bankForm.bankType">
            <el-radio label="FORMAL">正式考核题库</el-radio>
            <el-radio label="PRACTICE">模拟考核题库</el-radio>
            <el-radio label="COMMON">通用题库</el-radio>
          </el-radio-group>
          <div class="form-tip">通用题库在正式考核与模拟考核中都可被选用；正式 / 模拟题库只在对应考核里可选。</div>
        </el-form-item>
        <el-form-item label="题库说明"><el-input v-model="bankForm.description" type="textarea" :rows="3" placeholder="填写题库说明（可选）" maxlength="500" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="bankForm.status"><el-radio label="ENABLED">启用</el-radio><el-radio label="DISABLED">停用</el-radio></el-radio-group></el-form-item>
      </el-form>
      <span slot="footer"><el-button @click="bankDialogVisible = false">取消</el-button><el-button type="primary" :loading="bankSubmitting" @click="submitBank">确定</el-button></span>
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
        bankName: [{ required: true, message: '请输入题库名称', trigger: 'blur' }],
        bankType: [{ required: true, message: '请选择用途标签', trigger: 'change' }],
        bankKind: [{ required: true, message: '请选择题库形态', trigger: 'change' }]
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
      importResult: null,

      // === 2026-09-21 改版新增（纯追加，不影响既有逻辑）===
      filters: { keyword: '', deptId: null, bankType: '', status: '', bankKind: 'THEORY' },
      activeKpi: '',
      sortByCount: false
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
    },

    // === 2026-09-21 改版新增：筛选 + 总览（全部由已加载的 bankList 计算，不改接口）===
    /** 按 关键字 / 部门 / 类型 / 状态 过滤后的题库 */
    filteredBanks() {
      const f = this.filters
      const kw = (f.keyword || '').trim().toLowerCase()
      let list = (this.bankList || []).filter(bank => {
        if (kw) {
          const hay = `${bank.bankName || ''} ${bank.description || ''}`.toLowerCase()
          if (hay.indexOf(kw) === -1) return false
        }
        if (f.deptId && bank.deptId !== f.deptId) return false
        if (f.bankType && bank.bankType !== f.bankType) return false
        if (f.status && bank.status !== f.status) return false
        if (f.bankKind && (bank.bankKind || 'THEORY') !== f.bankKind) return false
        return true
      })
      if (this.sortByCount) {
        list = list.slice().sort((a, b) => (b.questionCount || 0) - (a.questionCount || 0))
      }
      return list
    },
    hasActiveFilter() {
      const f = this.filters
      return !!(f.keyword || f.deptId || f.bankType || f.status || this.sortByCount)
    },
    /** 总览四格：值全部来自真实列表 */
    kpiCards() {
      const list = this.bankList || []
      const sum = list.reduce((acc, bank) => acc + (bank.questionCount || 0), 0)
      return [
        { key: 'all', label: '题库总数', value: list.length, unit: '个', hint: '点此清空筛选', icon: 'el-icon-collection', tone: '' },
        { key: 'questions', label: '题目总数', value: sum, unit: '题', hint: '点此按题量排序', icon: 'el-icon-tickets', tone: 'tone-purple' },
        { key: 'enabled', label: '启用中', value: list.filter(b => b.status === 'ENABLED').length, unit: '个', hint: '点此只看启用', icon: 'el-icon-circle-check', tone: 'tone-green' },
        { key: 'practice', label: '模拟考核用', value: list.filter(b => b.bankType === 'PRACTICE').length, unit: '个', hint: '点此只看模拟考核用', icon: 'el-icon-edit-outline', tone: 'tone-orange' }
      ]
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
        // 题目列表已移到「题库详情」独立页 —— 这里不再自动选中题库（原来会多打一次 loadQuestions）
      }).catch(() => { this.bankLoading = false })
    },
    /**
     * 查看详情 —— 进入独立的「题库详情」页（统计 + 题目管理）。
     *
     * <p>为什么用独立页而不是下方同页面板：详情页要放题型 / 难度 / 知识点分布等统计，
     * 同页横排塞不下；独立页也便于分享链接、刷新不丢位置。</p>
     * <p>详情页挂在 `/super` 与 `/department` 两处，按当前路径前缀拼目标路由。</p>
     */
    goDetail(row) {
      const base = this.$route.path.indexOf('/super') === 0
        ? '/super/ops/bank-detail/'
        : '/department/study/bank-detail/'
      this.$router.push(base + row.id).catch(() => {})
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
      this.bankForm = { id: null, deptId: null, bankName: '', description: '', status: 'ENABLED', bankType: 'COMMON', bankKind: this.filters.bankKind || 'THEORY' }
      this.bankDialogVisible = true
    },
    handleEditBank(row) {
      this.bankForm = { id: row.id, deptId: row.deptId, bankName: row.bankName, description: row.description, status: row.status, bankType: row.bankType || 'COMMON', bankKind: row.bankKind || 'THEORY' }
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
      const tagTip = row.bankType === 'FORMAL' ? '该题库为「正式考核题库」，删除后引用它的正式考核将无题可抽。'
        : row.bankType === 'PRACTICE' ? '该题库为「模拟考核题库」，删除后未配组卷的模拟考核将无题可抽（会回退到本部门其它模拟/通用题库）。'
          : row.bankType === 'COMMON' ? '该题库为「通用题库」，正式与模拟考核都可能引用它。' : '该题库未设置用途标签。'
      this.$modal.confirm(`确认删除题库「${row.bankName}」吗？${tagTip}题库下的题目将一并删除。`).then(() => {
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
    },

    // === 2026-09-21 改版新增 ===
    /** 点总览卡 = 一键套用该口径（再点一次取消） */
    applyKpi(key) {
      if (this.activeKpi === key) { this.resetFilters(); return }
      this.activeKpi = key
      this.filters.status = key === 'enabled' ? 'ENABLED' : ''
      this.filters.bankType = key === 'practice' ? 'PRACTICE' : ''
      this.sortByCount = key === 'questions'
      if (key === 'all') { this.filters.status = ''; this.filters.bankType = ''; this.sortByCount = false }
    },
    resetFilters() {
      this.filters = { keyword: '', deptId: null, bankType: '', status: '', bankKind: this.filters.bankKind || 'THEORY' }
      this.activeKpi = ''
      this.sortByCount = false
    },
    toggleSortByCount() {
      this.sortByCount = !this.sortByCount
      this.activeKpi = this.sortByCount ? 'questions' : ''
    },
    /** 形态切换条（数量按当前筛选结果算，不含形态条件本身） */
    kindTabs() {
      const list = this.bankList || []
      const cnt = k => list.filter(b => (b.bankKind || 'THEORY') === k).length
      return [
        { value: 'THEORY', label: '理论题库', count: cnt('THEORY') },
        { value: 'PRACTICAL', label: '实操题库', count: cnt('PRACTICAL') }
      ]
    },
    switchKind(k) {
      this.filters.bankKind = k
    },
    bankKindLabel(k) { return { THEORY: '理论', PRACTICAL: '实操' }[k] || '理论' },
    bankTypeLabel(t) { return { FORMAL: '正式考核题库', PRACTICE: '模拟考核题库', COMMON: '通用题库' }[t] || '未设置' },
    bankTypeTag(t) { return t === 'PRACTICE' ? 'warning' : (t === 'COMMON' ? 'success' : 'primary') },
  }
}
</script>

<style lang="scss" scoped>
/* ============ 页头 ============ */
.page-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 16px; }
.eyebrow { color: #1764f5; font-size: 12px; letter-spacing: .05em; }
.page-note { margin: 6px 0 0; color: #8490a0; font-size: 12px; }
.page-note b { color: #475467; }
.form-tip { margin-top: 4px; color: #98a2b3; font-size: 12px; line-height: 1.6; }
.kind-bar { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 12px; }
.kind-tab {
  padding: 7px 16px; color: #475467; background: #fff; border: 1px solid #e7ecf3;
  border-radius: 6px; cursor: pointer; font-family: inherit; font-size: 13px; transition: all .15s;
}
.kind-tab:hover { border-color: #b9d2ff; color: #1764f5; }
.kind-tab.on { color: #1764f5; background: #f6faff; border-color: #1764f5; font-weight: 500; }
.kind-n { margin-left: 6px; padding: 0 6px; color: #667085; background: #eef1f6; border-radius: 9px; font-size: 11px; }
.kind-tab.on .kind-n { color: #1764f5; background: #e8f1fd; }
.kind-hint { margin-left: auto; color: #98a2b3; font-size: 12px; }
@media (max-width: 900px) { .kind-hint { margin-left: 0; } }
.title-line { display: flex; align-items: center; gap: 10px; }
.page-heading h2 { margin: 6px 0 8px; font-size: 22px; font-weight: 600; color: #1d2939; }
.page-heading p { margin: 0; color: #667085; font-size: 13px; }
.heading-actions { flex: none; }

/* ============ ① 总览 KPI（可点即筛选） ============ */
.kpi-row { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin-bottom: 14px; }
.kpi-card {
  display: flex; align-items: center; gap: 12px; padding: 14px 16px; text-align: left;
  background: #fff; border: 1px solid #e7ecf3; border-radius: 6px; cursor: pointer;
  transition: border-color .15s, box-shadow .15s, transform .15s;
  font-family: inherit;
}
.kpi-card:hover { border-color: #b9d2ff; box-shadow: 0 2px 8px rgba(23, 100, 245, .08); }
.kpi-card.active { border-color: #1764f5; box-shadow: 0 0 0 2px rgba(23, 100, 245, .12); }
.kpi-icon { display: flex; width: 40px; height: 40px; flex: none; align-items: center; justify-content: center; color: #1764f5; background: #edf4ff; font-size: 20px; border-radius: 8px; }
.kpi-body { display: flex; min-width: 0; flex: 1; flex-direction: column; }
.kpi-value { color: #1d2939; font-size: 22px; font-weight: 600; line-height: 1.2; }
.kpi-value small { margin-left: 3px; color: #667085; font-size: 12px; font-weight: 400; }
.kpi-label { margin-top: 2px; color: #667085; font-size: 12px; }
.kpi-hint { flex: none; align-self: flex-start; color: #b6bfcc; font-size: 11px; }
.kpi-card.active .kpi-hint { color: #1764f5; }
.kpi-card.tone-green .kpi-icon { color: #23966f; background: #eaf7f1; }
.kpi-card.tone-orange .kpi-icon { color: #e6a23c; background: #fdf6ec; }
.kpi-card.tone-purple .kpi-icon { color: #7b5cf0; background: #f2eeff; }

/* ============ ② 筛选条 ============ */
.filter-bar {
  display: flex; flex-wrap: wrap; align-items: center; gap: 10px;
  margin-bottom: 14px; padding: 12px 16px; background: #fff;
  border: 1px solid #e7ecf3; border-radius: 6px;
}
.filter-keyword { width: 240px; }
.filter-select { width: 150px; }
.filter-summary { margin-left: auto; color: #667085; font-size: 12px; }

/* ============ ③ 题库目录 ============ */
.content-panel { margin-bottom: 16px; padding: 18px 20px; background: #fff; border: 1px solid #e7ecf3; border-radius: 6px; }
.panel-heading { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.panel-heading h3 { margin: 0 0 5px; font-size: 16px; font-weight: 600; color: #1d2939; }
.panel-heading p { margin: 0; color: #8490a0; font-size: 12px; }
.bank-cell { display: flex; align-items: center; gap: 12px; }
.bank-icon { display: flex; width: 38px; height: 38px; flex: none; align-items: center; justify-content: center; color: #fff; background: #1764f5; font-size: 18px; border-radius: 6px; }
.bank-icon.practice { background: #e6a23c; }
.bank-main { min-width: 0; }
.bank-main strong { display: block; color: #1d2939; font-size: 14px; }
.bank-main small { display: block; max-width: 420px; margin-top: 3px; overflow: hidden; color: #98a2b3; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.dept-text { color: #475467; font-size: 13px; }
.dept-text i { margin-right: 4px; color: #98a2b3; }
.table-number { color: #1764f5; font-size: 16px; font-weight: 600; }
.table-sub { display: block; margin-top: 2px; color: #98a2b3; font-size: 11px; }
.danger-text { color: #f56c6c; }

/* ============ 空态（区分「没有」与「筛没了」） ============ */
.empty-block { padding: 26px 0; text-align: center; }
.empty-block i { color: #d0d5dd; font-size: 34px; }
.empty-block p { margin: 10px 0 12px; color: #8490a0; font-size: 13px; }

/* ============ 窄屏 ============ */
@media (max-width: 1100px) {
  .kpi-row { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .filter-keyword { width: 100%; }
  .filter-summary { margin-left: 0; }
}
@media (max-width: 700px) {
  .kpi-row { grid-template-columns: minmax(0, 1fr); }
  .page-heading { flex-direction: column; }
  .content-panel { padding: 14px 12px; }
}
</style>
