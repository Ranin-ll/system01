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
        <p>理论考核从题库组卷、交卷自动判分；实操考核逐题上传、管理员逐题打分。发布前先看列表里的「卷面 / 通过线」告警，再到下方配置区核对组卷。</p>
      </div>
      <div class="heading-actions">
        <el-button icon="el-icon-refresh" size="small" @click="reloadCurrent">刷新</el-button>
        <el-button v-hasPermi="['business:bank:add']" type="primary" icon="el-icon-plus" size="small" @click="handleAdd('THEORY')">新建考核</el-button>
      </div>
    </header>

    <!-- ① 总览：真实计数（examMode=FORMAL），点卡即按状态筛选 -->
    <section class="kpi-row">
      <button v-for="card in kpiCards" :key="card.key" type="button" class="kpi-card" :class="[{ active: activeKpi === card.key }, card.tone]" @click="applyKpi(card.key)">
        <span class="kpi-icon"><i :class="card.icon" /></span>
        <span class="kpi-body">
          <span class="kpi-value">{{ card.value }}<small>场</small></span>
          <span class="kpi-label">{{ card.label }}</span>
        </span>
        <span class="kpi-hint">{{ card.hint }}</span>
      </button>
    </section>

    <!-- ② 筛选 -->
    <section class="filter-bar">
      <el-select v-if="isSuperAdmin" v-model="queryParams.deptId" size="small" clearable filterable placeholder="全部部门" class="filter-select" @change="handleQuery">
        <el-option v-for="dept in deptOptions" :key="dept.deptId" :label="dept.deptName" :value="dept.deptId" />
      </el-select>
      <el-input v-model="queryParams.examName" size="small" clearable prefix-icon="el-icon-search" placeholder="考核名称关键字" class="filter-keyword" @keyup.enter.native="handleQuery" />
      <el-select v-model="queryParams.status" size="small" clearable placeholder="全部状态" class="filter-select" @change="handleQuery">
        <el-option label="待发布" value="DRAFT" />
        <el-option label="已发布" value="PUBLISHED" />
        <el-option label="待批改" value="GRADING" />
        <el-option label="已停用" value="DISABLED" />
      </el-select>
      <el-button type="primary" size="small" icon="el-icon-search" @click="handleQuery">查询</el-button>
      <el-button size="small" icon="el-icon-refresh-left" @click="resetQuery">重置</el-button>
      <span class="filter-summary">当前列表 {{ total }} 条</span>
    </section>

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
          <span class="dm-tip">点任意一行进入该场考核的配置页</span>
        </div>
        <span class="dm-meta">共 {{ total }} 条</span>
      </div>

      <el-table
        :key="'exam-table'"
        v-loading="loading"
        :data="tableList"
        size="small"
        class="clickable-table"
        @row-click="goConfig"
      >
        <el-table-column label="考核名称" min-width="220">
          <template slot-scope="scope">
            <div class="exam-cell">
              <span class="exam-name-line">
                <strong>{{ scope.row.examName }}</strong>
                <el-tag v-if="isTestData(scope.row)" size="mini" effect="plain" type="warning">疑似测试数据</el-tag>
              </span>
              <small>{{ isSuperAdmin ? (scope.row.deptName || '未设置部门') : scope.row.examModeText }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="考核形式" width="106" align="center">
          <template slot-scope="scope">
            <el-tag size="mini" effect="plain" :type="scope.row.examType === 'PRACTICAL' ? 'success' : 'primary'">
              {{ scope.row.examType === 'PRACTICAL' ? '实操考核' : '理论考核' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="卷面 / 通过线" min-width="210">
          <template slot-scope="scope">
            <div class="score-cell">
              <span>满分 <b>{{ examFullScore(scope.row) }}</b> 分 · 通过线 <b>{{ scope.row.passLine }}</b> 分</span>
              <small class="score-check" :class="scoreCheck(scope.row).level">
                <i :class="scoreCheck(scope.row).icon" />{{ scoreCheck(scope.row).text }}
              </small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="题目量" width="104" align="center">
          <template slot-scope="scope">
            <strong class="num">{{ scope.row.examType === 'PRACTICAL' ? (scope.row.subjectCount || 0) : (scope.row.questionCount || 0) }}</strong>
            <small class="unit">{{ scope.row.examType === 'PRACTICAL' ? '道' : '题' }}</small>
          </template>
        </el-table-column>
        <el-table-column label="考试安排" min-width="200">
          <template slot-scope="scope">
            <div class="plan-cell">
              <span>{{ scope.row.duration > 0 ? scope.row.duration + ' 分钟' : '不限时' }}</span>
              <small>{{ windowText(scope.row) }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="92" align="center">
          <template slot-scope="scope">
            <el-tag size="mini" :type="statusTag(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="已作答 / 待批阅" width="126" align="center">
          <template slot-scope="scope">
            <strong class="num">{{ scope.row.answeredCount || 0 }}</strong>
            <span class="sep">/</span>
            <span class="num warn">{{ scope.row.pendingCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" align="center">
          <template slot-scope="scope">
            <el-button type="text" size="mini" icon="el-icon-setting" @click.stop="goConfig(scope.row)">配置</el-button>
            <el-button v-hasPermi="['business:bank:edit']" type="text" size="mini" icon="el-icon-edit" @click.stop="handleEdit(scope.row)">编辑信息</el-button>
            <el-button
              v-if="scope.row.status === 'DRAFT'"
              v-hasPermi="['business:bank:edit']"
              type="text"
              size="mini"
              icon="el-icon-upload2"
              @click.stop="handlePublishQuick(scope.row)"
            >发布</el-button>
            <el-button
              v-else-if="scope.row.status === 'PUBLISHED' || scope.row.status === 'GRADING'"
              v-hasPermi="['business:bank:edit']"
              type="text"
              size="mini"
              icon="el-icon-video-pause"
              @click.stop="handleChangeStatus(scope.row, 'DISABLED')"
            >停用</el-button>
            <el-button
              v-else
              v-hasPermi="['business:bank:edit']"
              type="text"
              size="mini"
              icon="el-icon-video-play"
              @click.stop="handleChangeStatus(scope.row, 'PUBLISHED')"
            >启用</el-button>
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

        <template slot="empty">
          <div class="empty-block">
            <i class="el-icon-document" />
            <p>{{ emptyText }}</p>
            <el-button v-if="queryParams.examName || queryParams.status || queryParams.deptId" size="mini" @click="resetQuery">清空筛选</el-button>
          </div>
        </template>
      </el-table>
      <pagination v-show="total > 0 && activeKpi !== 'PROBLEM'" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="loadList" />
      <p v-if="activeKpi === 'PROBLEM'" class="problem-note">
        以上 {{ problemList.length }} 场考核按<b>当前配置发布不了</b>：到「配置」补齐组卷 / 实操题目，或把通过线降到卷面满分以内。
      </p>
    </section>

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
            <span>从实操题库选题带入（也可逐题填写题干 / 描述 / 参考 / 附件与满分），实习生逐题交文件，管理员逐题打分。</span>
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

        <div class="form-sec">考核说明</div>
        <el-form-item label="说明">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="这场考核考什么、面向哪些人、有哪些注意事项…（实习生端可见）"
          />
        </el-form-item>
        <p class="step-tip">
          <i class="el-icon-info" />
          保存后是<b>草稿</b>：请回到列表点「配置」进入配置页，完成
          <template v-if="form.examType === 'PRACTICAL'"><b>实操题目</b>（可从实操题库一键带入）</template>
          <template v-else><b>组卷题库与每题分值</b></template>
          与<b>发布设置</b>后再发布。
        </p>
      </el-form>

      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { listExam, getExam, addExam, updateExam, delExam, changeExamStatus, publishExam } from '@/api/business/exam'
import { listDept } from '@/api/system/dept'
import { mapGetters } from 'vuex'

const DURATION_OPTIONS = [0, 30, 45, 60, 90, 120, 150, 180, 240]

export default {
  name: 'ExamManage',
  components: {},
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
      submitting: false,

      // === 2026-09-21 改版新增（纯追加）===
      statusCounts: { ALL: 0, DRAFT: 0, PUBLISHED: 0, GRADING: 0, DISABLED: 0 },
      activeKpi: '',
      /** 全量正式考核：KPI「需处理」计数 + 该卡点击后的只读筛选都基于它（其余状态筛选走后端分页） */
      allFormal: []
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
      if (this.activeKpi === 'PROBLEM') return '没有需要处理的考核：当前所有考核的配置都能发布'
      if (this.examTypeFilter === 'THEORY') return '暂无理论考核，点「新建考核」并选择理论类型'
      if (this.examTypeFilter === 'PRACTICAL') return '暂无实操考核，点「新建考核」并选择实操类型'
      return '暂无考核，点「新建考核」创建第一条'
    },

    // === 2026-09-21 改版新增：总览五卡（计数来自真实接口，口径 examMode=FORMAL）===
    kpiCards() {
      const c = this.statusCounts
      return [
        { key: 'ALL', label: '全部正式考核', value: c.ALL, hint: '点此查看全部', icon: 'el-icon-files', tone: '' },
        { key: 'DRAFT', label: '待发布', value: c.DRAFT, hint: '点此只看待发布', icon: 'el-icon-edit-outline', tone: 'tone-orange' },
        { key: 'PUBLISHED', label: '已发布', value: c.PUBLISHED, hint: '点此只看已发布', icon: 'el-icon-circle-check', tone: 'tone-green' },
        { key: 'GRADING', label: '待批改', value: c.GRADING, hint: '点此只看待批改', icon: 'el-icon-edit', tone: 'tone-purple' },
        { key: 'PROBLEM', label: '需处理', value: this.needFixCount, hint: '满分 0 或通过线高于满分', icon: 'el-icon-warning-outline', tone: 'tone-red' }
      ]
    },
    /** 「需处理」= 按当前配置根本发布不了（卷面满分 0 / 通过线高于满分） */
    problemList() {
      return this.allFormal.filter(r => this.scoreCheck(r).level === 'danger')
    },
    needFixCount() { return this.problemList.length },
    /** 表格数据源：「需处理」走全量前端过滤（不分页），其余走后端分页 */
    tableList() {
      return this.activeKpi === 'PROBLEM' ? this.problemList : this.list
    }
  },
  created() {
    if (this.isSuperAdmin) this.loadDepartments()
    this.loadList()
    this.loadCounts()
  },
  mounted() {
    // 总览计数（新增，不影响既有 created 流程）
    this.loadStatusCounts()
    // 从配置页点「编辑」回来时（?edit=<id>）自动打开基本信息弹窗
    this.openEditFromQuery()
  },
  watch: {
    '$route.query.edit'() { this.openEditFromQuery() }
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
      // 切「理论 / 实操」分栏时退出「需处理」视图（那是跨状态的全量前端过滤）
      if (this.activeKpi === 'PROBLEM') this.activeKpi = ''
      this.loadList()
    },
    /** 进入该场考核的独立配置页（顶层路由，部门端/超管端列表都跳这里；?from= 记住来源以便返回） */
    goConfig(row) {
      if (!row || !row.id) return
      this.$router.push({ path: '/exam-config/' + row.id, query: { from: this.$route.path } }).catch(() => {})
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
          description: d.description || ''
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
        payload.description = String(this.form.description || '').trim() || null
        if (this.form.examType === 'THEORY') {
          payload.singleScore = this.form.singleScore
          payload.multiScore = this.form.multiScore
          payload.judgeScore = this.form.judgeScore
        }
        // 实操考核的题目清单不在弹窗里维护（两步式：题目在配置页从实操题库带入或逐题填写），
        // 因此这里不传 subjectItems —— 后端只在显式传时才覆写，避免误清空。
        this.submitting = true
        const fn = payload.id ? updateExam : addExam
        fn(payload).then(() => {
          this.$modal.msgSuccess('保存成功')
          this.dialogVisible = false
          this.submitting = false
          const createdId = !payload.id
          this.reloadCurrent()
          if (createdId) {
            this.$modal.msgSuccess('已创建草稿：在列表点「配置」完成' + (payload.examType === 'PRACTICAL' ? '实操题目' : '组卷') + '与发布设置后再发布')
          }
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
    },

    // === 2026-09-21 改版新增 ===
    /** 卷面满分：实操 = 各题满分之和；理论 = 单/多/判 分值×题量之和 */
    examFullScore(row) {
      return row.examType === 'PRACTICAL' ? this.round1(row.subjectTotalScore) : this.theoryScore(row)
    },
    /**
     * ★「按当前配置这一步能不能发布」的校验位（后端发布时也拦，但列表里要能提前看见）
     * 判据 = 卷面满分 vs 通过线；满分 0 = 还没组卷/还没加题。
     */
    scoreCheck(row) {
      const full = this.examFullScore(row)
      const pass = Number(row.passLine) || 0
      if (!full) {
        return {
          level: 'danger',
          icon: 'el-icon-warning-outline',
          text: row.examType === 'PRACTICAL' ? '尚未添加实操题目，无法发布' : '尚未组卷（题量 0），无法发布'
        }
      }
      if (pass > full) {
        return { level: 'danger', icon: 'el-icon-warning-outline', text: '通过线高于卷面满分，无法发布' }
      }
      if (pass === full) {
        return { level: 'warn', icon: 'el-icon-warning-outline', text: '通过线等于满分，必须全对才及格' }
      }
      return { level: 'ok', icon: 'el-icon-circle-check', text: '余量 ' + this.round1(full - pass) + ' 分' }
    },
    /** 总览计数：各状态各取一次 total；「全部」那次顺带把全量拉回来，供「需处理」卡计数与筛选 */
    loadStatusCounts() {
      const base = { pageNum: 1, pageSize: 1, examMode: 'FORMAL' }
      listExam({ pageNum: 1, pageSize: 200, examMode: 'FORMAL' }).then(res => {
        this.$set(this.statusCounts, 'ALL', res.total || 0)
        this.allFormal = res.rows || []
      }).catch(() => { this.allFormal = [] })
      ;['DRAFT', 'PUBLISHED', 'GRADING', 'DISABLED'].forEach(st => {
        listExam(Object.assign({}, base, { status: st }))
          .then(res => { this.$set(this.statusCounts, st, res.total || 0) })
          .catch(() => {})
      })
    },
    /** 点总览卡 = 按该状态筛选（再点一次取消）；「需处理」不是后端状态，改走全量前端过滤（见 tableList） */
    applyKpi(key) {
      const off = this.activeKpi === key
      this.activeKpi = off ? '' : key
      this.queryParams.status = (off || key === 'ALL' || key === 'PROBLEM') ? '' : key
      this.queryParams.pageNum = 1
      this.loadList()
      this.loadStatusCounts()
    },
    /** 列表行直接发布：走 publish 接口（后端会校验「通过线 ≤ 卷面满分」「组卷/题目可用」） */
    handlePublishQuick(row) {
      const check = this.scoreCheck(row)
      const tip = check.level === 'ok'
        ? `确认发布「${row.examName}」吗？`
        : `「${row.examName}」当前配置有问题：${check.text}。仍要尝试发布吗？`
      this.$modal.confirm(tip).then(() => {
        publishExam(row.id).then(() => {
          this.$modal.msgSuccess('已发布')
          this.reloadCurrent()
        }).catch(() => {})
      }).catch(() => {})
    },
    /** 疑似测试数据：名称为空 / 长度 ≤ 2 / 含 test·测试·临时（实测库里存在 m、l、12 这类残留） */
    isTestData(row) {
      const n = String((row && row.examName) || '').trim()
      return !n || n.length <= 2 || /test|测试|demo|临时/i.test(n)
    },
    /** ?edit=<id> → 打开编辑弹窗（编辑表单在列表页，配置页只负责跳回来；避免两处维护同一表单） */
    openEditFromQuery() {
      const id = this.$route.query.edit
      if (!id) return
      const row = this.list.find(e => String(e.id) === String(id)) || { id: Number(id) }
      this.handleEdit(row)
      const q = Object.assign({}, this.$route.query)
      delete q.edit
      this.$router.replace({ path: this.$route.path, query: q }).catch(() => {})
    }
  }
}

function num(v, fallback) {
  const n = Number(v)
  return isNaN(n) ? fallback : n
}

function emptyForm(examType) {
  const practical = examType === 'PRACTICAL'
  return {
    id: null,
    deptId: null,
    examName: '',
    description: '',
    examType: examType || 'THEORY',
    singleScore: 2,
    multiScore: 4,
    judgeScore: 2,
    passLine: 60,
    duration: practical ? 180 : 60
  }
}
</script>

<style lang="scss" scoped>
/* ============ 页头 ============ */
.page-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 16px; }
.eyebrow { color: #1764f5; font-size: 12px; letter-spacing: .05em; }
.title-line { display: flex; align-items: center; gap: 10px; }
.page-heading h2 { margin: 6px 0 8px; font-size: 22px; font-weight: 600; color: #1d2939; }
.page-heading p { margin: 0; max-width: 760px; color: #667085; font-size: 13px; line-height: 1.6; }
.heading-actions { flex: none; }

/* ============ ① 总览 KPI（点卡 = 按状态筛选；末卡「需处理」= 配置发布不了） ============ */
.kpi-row { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 12px; margin-bottom: 14px; }
.kpi-card {
  display: flex; align-items: center; gap: 12px; padding: 14px 16px; text-align: left;
  background: #fff; border: 1px solid #e7ecf3; border-radius: 6px; cursor: pointer;
  transition: border-color .15s, box-shadow .15s; font-family: inherit;
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
.kpi-card.tone-red .kpi-icon { color: #d9534f; background: #fdeeed; }
.kpi-card.tone-purple .kpi-icon { color: #7b5cf0; background: #f2eeff; }

/* 名称列：标题 + 「疑似测试数据」标 */
.exam-name-line { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }

/* 两步式提示（弹窗底部） */
.step-tip { display: flex; align-items: flex-start; gap: 7px; padding: 10px 13px; margin: 4px 0 0; color: #475467; background: #f6faff; border: 1px solid #dbe9ff; border-radius: 6px; font-size: 12.5px; line-height: 1.75; }
.step-tip i { margin-top: 2px; color: #1764f5; }

/* 「需处理」视图底部的说明 */
.problem-note { margin: 12px 0 0; padding: 10px 13px; color: #b54708; background: #fffbf5; border: 1px solid #fdeacd; border-radius: 6px; font-size: 12.5px; line-height: 1.75; }

/* ============ ② 筛选条 ============ */
.filter-bar {
  display: flex; flex-wrap: wrap; align-items: center; gap: 10px;
  margin-bottom: 12px; padding: 12px 16px; background: #fff;
  border: 1px solid #e7ecf3; border-radius: 6px;
}
.filter-keyword { width: 220px; }
.filter-select { width: 150px; }
.filter-summary { margin-left: auto; color: #667085; font-size: 12px; }

.type-tabs { margin-bottom: 4px; }
.type-tabs ::v-deep .el-tabs__header { margin-bottom: 12px; }

/* ============ ③ 列表 ============ */
.dm-card { margin-bottom: 14px; padding: 16px 18px 18px; background: #fff; border: 1px solid #e7ecf3; border-radius: 8px; }
.dm-head { display: flex; align-items: center; justify-content: space-between; gap: 14px; padding-bottom: 13px; margin-bottom: 12px; border-bottom: 1px solid #edf0f4; }
.dm-title { display: flex; align-items: center; gap: 9px; flex-wrap: wrap; }
.dm-title h3 { margin: 0; color: #1d2939; font-size: 16px; font-weight: 600; }
.dm-idx { display: inline-flex; width: 24px; height: 24px; align-items: center; justify-content: center; color: #1764f5; background: #e8f1fd; font-size: 12px; font-weight: 700; border-radius: 4px; }
.dm-meta { color: #667085; font-size: 12px; }
.dm-tip { color: #98a2b3; font-size: 12px; }
.exam-cell strong, .exam-cell small { display: block; }
.exam-cell strong { color: #1d2939; font-size: 14px; }
.exam-cell small { margin-top: 3px; color: #98a2b3; font-size: 12px; }
.num { color: #1764f5; font-size: 14px; }
.num.warn { color: #b54708; }
.unit { margin-left: 2px; color: #98a2b3; font-size: 11px; }
.sep { margin: 0 5px; color: #cfd6df; }
.muted { color: #98a2b3; font-size: 12px; }
.danger-text { color: #f56c6c; }

/* 卷面 / 通过线：带「能不能发」的校验位 */
.score-cell > span { display: block; color: #475467; font-size: 13px; }
.score-cell > span b { color: #1d2939; }
.score-check { display: block; margin-top: 3px; font-size: 11.5px; }
.score-check i { margin-right: 3px; }
.score-check.ok { color: #23966f; }
.score-check.warn { color: #b54708; }
.score-check.danger { color: #d9534f; }

.plan-cell > span { display: block; color: #475467; font-size: 13px; }
.plan-cell > small { display: block; margin-top: 3px; color: #98a2b3; font-size: 11.5px; }

/* ============ ④ 配置区 / 空态 ============ */
.clickable-table ::v-deep tbody tr { cursor: pointer; }
.clickable-table ::v-deep tbody tr:hover > td { background: #f6faff !important; }
.config-head {
  display: flex; align-items: center; gap: 12px; padding: 13px 18px;
  background: #f6faff; border: 1px solid #d6e6ff; border-bottom: none;
  border-radius: 8px 8px 0 0;
}
.config-idx { display: inline-flex; width: 26px; height: 26px; flex: none; align-items: center; justify-content: center; color: #fff; background: #1764f5; font-size: 12px; font-weight: 700; border-radius: 5px; }
.config-title { min-width: 0; flex: 1; }
.config-title h3 { margin: 0; overflow: hidden; color: #1d2939; font-size: 14.5px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.config-title p { margin: 3px 0 0; color: #667085; font-size: 12px; }


.empty-block { padding: 26px 0; text-align: center; }
.empty-block i { color: #d0d5dd; font-size: 34px; }
.empty-block p { margin: 10px 0 12px; color: #8490a0; font-size: 13px; }

/* ============ 弹窗：类型大卡 ============ */
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
  .kpi-row { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .type-pick { grid-template-columns: 1fr; }
  .fg2 { grid-template-columns: 1fr; }
  .fg2 .span2 { grid-column: span 1; }
  .filter-keyword { width: 100%; }
  .filter-summary { margin-left: 0; }
}
@media (max-width: 700px) {
  .kpi-row { grid-template-columns: minmax(0, 1fr); }
  .page-heading { flex-direction: column; }
}
</style>
