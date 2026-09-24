<template>
  <div class="dept-page">
    <div class="dept-breadcrumb">
      <el-button type="text" icon="el-icon-arrow-left" @click="goBack">返回套卷列表</el-button>
      <span>/</span>
      <b>{{ exam.examName || '套卷配置' }}</b>
    </div>

    <div class="dept-heading">
      <div>
        <span class="eyebrow">PAPER CONFIG</span>
        <h1>{{ exam.examName || '套卷配置' }}</h1>
        <p>{{ exam.description || '（未填写套卷描述）' }}</p>
      </div>
      <div class="dept-heading-actions">
        <el-button size="small" @click="openPaperDialog">编辑基础信息</el-button>
        <el-button size="small" type="primary" :loading="saving" @click="saveExam">保存并生效</el-button>
      </div>
    </div>

    <!-- ① 基础信息（只读摘要；改 → 右上角「编辑基础信息」） -->
    <div class="dsec">
      <div class="dsec-head">
        <div>
          <span class="dsec-no p">基</span>
          <div>
            <h2>基础信息</h2>
            <p>套卷名称 / 描述 / 难易程度 / 题目内容偏向 —— 点右上角「编辑基础信息」修改；这里只读展示。</p>
          </div>
        </div>
      </div>
      <div class="dsec-body" v-loading="loading">
        <div class="pc-grid">
          <div class="pc-item"><label>套卷名称</label><span class="strong">{{ exam.examName || '—' }}</span></div>
          <div class="pc-item">
            <label>难易程度</label>
            <span v-if="exam.difficulty" class="dbadge" :class="difficultyTone(exam.difficulty)">{{ difficultyText(exam.difficulty) }}</span>
            <span v-else class="muted">未设置</span>
          </div>
          <div class="pc-item">
            <label>发布状态</label>
            <span class="dbadge" :class="exam.status === 'PUBLISHED' ? 'green' : 'gray'">{{ examStatusText(exam.status) }}</span>
          </div>
          <div class="pc-item"><label>所属部门</label><span>{{ exam.deptName || '—' }}</span></div>
        </div>
        <div class="pc-item" style="margin-top:12px"><label>套卷描述</label><span>{{ exam.description || '未填写' }}</span></div>
        <div class="pc-item" style="margin-top:10px"><label>题目内容偏向</label><span>{{ exam.contentBias || '未填写' }}</span></div>
      </div>
    </div>

    <!-- ② 组卷配置 -->
    <div class="dsec">
      <div class="dsec-head">
        <div>
          <span class="dsec-no">配</span>
          <div>
            <h2>组卷配置</h2>
            <p>设置每题分值 / 时长 / 通过线，配置参与组卷的题库与抽题量，试抽校验后「保存并生效」即发布给实习生练习。</p>
          </div>
        </div>
        <div style="display:flex;align-items:center;gap:8px">
          <el-button size="mini" @click="goBack">取消</el-button>
          <el-button size="mini" type="primary" :loading="saving" @click="saveExam">保存并生效</el-button>
        </div>
      </div>

      <div class="dsec-body">
              <el-form :inline="true" size="small" style="margin-bottom:12px">
                <el-form-item label="考试时长">
                  <el-select v-model="durationPreset" size="small" style="width:160px" @change="onDurationPresetChange">
                    <el-option v-for="d in durationOptions" :key="d" :label="d > 0 ? d + ' 分钟' : '不限时（自测）'" :value="d" />
                    <el-option label="自定义…" :value="-1" />
                  </el-select>
                  <el-input-number v-if="durationPreset === -1" v-model="examForm.duration" :min="1" :max="600" size="small" style="width:120px;margin-left:8px" />
                </el-form-item>
                <el-form-item label="通过分数 / 总分">
                  <el-input-number v-model="examForm.passLine" :min="0" :max="500" :precision="1" size="small" />
                  <span style="margin-left:6px;color:#98a2b3">{{ examForm.passLine }} / {{ totalScore }} 分</span>
                </el-form-item>
              </el-form>

              <el-form :inline="true" size="small" style="margin-bottom:12px">
                <el-form-item label="卷面结构">
                  <span>单选</span><el-input-number v-model="examForm.singleCount" :min="0" :max="999" size="small" style="width:96px" /><span style="margin:0 4px">题 ×</span>
                  <el-input-number v-model="examForm.singleScore" :min="0" :max="100" :precision="1" size="small" style="width:96px" /><span style="margin-right:14px">分</span>
                  <span>多选</span><el-input-number v-model="examForm.multiCount" :min="0" :max="999" size="small" style="width:96px" /><span style="margin:0 4px">题 ×</span>
                  <el-input-number v-model="examForm.multiScore" :min="0" :max="100" :precision="1" size="small" style="width:96px" /><span style="margin-right:14px">分</span>
                  <span>判断</span><el-input-number v-model="examForm.judgeCount" :min="0" :max="999" size="small" style="width:96px" /><span style="margin:0 4px">题 ×</span>
                  <el-input-number v-model="examForm.judgeScore" :min="0" :max="100" :precision="1" size="small" style="width:96px" /><span>分</span>
                </el-form-item>
                <el-form-item>
                  <span class="dinp">题目数量 {{ questionTotal }} 题 · 满分 {{ totalScore }} 分</span>
                </el-form-item>
              </el-form>

              <div class="dcallout" :class="checkOk ? 'ok' : 'warn'" style="margin:0 0 12px">
                <i :class="checkOk ? 'el-icon-success' : 'el-icon-warning-outline'" />
                <span>
                  <template v-if="checkOk">校验通过：{{ knowledgeRules.length }} 个知识点 · 合计 {{ knowledgeCountSum }} 题（= 卷面题量 {{ questionTotal }} 题）· 满分 {{ totalScore }} 分 ✓</template>
                  <template v-else>{{ checkMessage }}</template>
                </span>
              </div>

              <div class="dgrid">
                <div class="c8">
                  <div class="dcard-h" style="padding:0 0 10px">
                    <div class="tt"><span class="idx">分</span><h3>知识点配比（每个知识点抽几道题）</h3></div>
                    <div class="dbtn-row">
                      <el-button size="mini" icon="el-icon-download" @click="importFromPool">从题池导入知识点</el-button>
                      <el-button size="mini" icon="el-icon-plus" @click="addKnowledgeRule(null)">添加知识点</el-button>
                    </div>
                  </div>
                  <el-table :data="knowledgeRules" size="mini" border empty-text="点右上角「从题池导入知识点」把本部门已有知识点带进来">
                    <el-table-column label="知识点（章节）" min-width="180">
                      <template slot-scope="scope">
                        <el-select
                          v-model="scope.row.knowledgePoint"
                          size="mini"
                          filterable
                          allow-create
                          default-first-option
                          placeholder="选择或输入知识点"
                          style="width:100%"
                        >
                          <el-option v-for="p in poolPoints" :key="p.knowledgePoint" :label="p.knowledgePoint" :value="p.knowledgePoint" />
                        </el-select>
                      </template>
                    </el-table-column>
                    <el-table-column label="题池可用" width="86" align="center">
                      <template slot-scope="scope"><span class="muted">{{ poolAvailable(scope.row.knowledgePoint) }} 题</span></template>
                    </el-table-column>
                    <el-table-column label="抽题数量" width="118" align="center">
                      <template slot-scope="scope">
                        <el-input-number v-model="scope.row.questionCount" :min="0" :max="Math.max(poolAvailable(scope.row.knowledgePoint), 999)" size="mini" style="width:100px" />
                      </template>
                    </el-table-column>
                    <el-table-column label="占比" width="76" align="center">
                      <template slot-scope="scope"><span class="muted">{{ ruleRatioText(scope.row) }}%</span></template>
                    </el-table-column>
                    <el-table-column label="操作" width="70" align="center">
                      <template slot-scope="scope">
                        <el-button type="text" size="mini" class="danger-text" @click="knowledgeRules.splice(scope.$index, 1)">删除</el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                  <p class="dsec-note">抽题时在<b>本部门理论题池内按知识点随机取题</b>，跨知识点去重。「抽题量 ≤ 题池该知识点可用」超了会抽不满卷，保存时后端会拦截。</p>
                </div>

                <div class="c4">
                  <div class="dcard-h" style="padding:0 0 10px">
                    <div class="tt"><span class="idx o">抽</span><h3>试抽一套（校验用）</h3></div>
                  </div>
                  <div class="dinp ta preview-box">
                    <div v-if="drawing" class="drawing"><i class="el-icon-loading" /> 正在抽题…</div>
                    <div v-else-if="previewQuestions.length">
                      <div v-for="q in previewQuestions" :key="q.seq" class="pq">{{ q.seq }} · {{ typeText(q.qtype) }} · {{ q.knowledgePoint || '未分类' }} · {{ q.stem }}</div>
                    </div>
                    <div v-else style="color:#98a2b3">点下方按钮，按当前知识点配比真实抽一套卷看看效果。</div>
                  </div>
                  <div class="dbtn-row" style="margin-top:12px">
                    <el-button size="small" :loading="drawing" @click="drawOnce">试抽一套</el-button>
                  </div>
                  <p class="dsec-note">保存后实习生即可看到本套卷并「开始练习」；模拟成绩仅本人可见、不计入正式成绩。</p>
                </div>
              </div>
      </div>
    </div>

    <!-- 基础信息弹窗（编辑） -->
    <el-dialog :title="paperDialogTitle" :visible.sync="paperDialogVisible" width="560px" append-to-body>
      <el-form ref="paperForm" :model="paperForm" :rules="paperRules" label-width="104px" size="small">
        <el-form-item label="套卷名称" prop="examName">
          <el-input v-model="paperForm.examName" placeholder="例如：基础卷 · 集合与并发" maxlength="60" show-word-limit />
        </el-form-item>
        <el-form-item label="套卷描述">
          <el-input v-model="paperForm.description" type="textarea" :rows="3" maxlength="300" show-word-limit placeholder="一句话说明这套卷考什么、适合什么阶段练（实习生可见）" />
        </el-form-item>
        <el-form-item label="难易程度">
          <el-radio-group v-model="paperForm.difficulty">
            <el-radio-button label="EASY">简单</el-radio-button>
            <el-radio-button label="MEDIUM">中等</el-radio-button>
            <el-radio-button label="HARD">困难</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="题目内容偏向">
          <el-input v-model="paperForm.contentBias" maxlength="200" show-word-limit placeholder="给实习生选卷参考，例如：偏 Java 集合与并发，重点考多线程与锁" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="paperDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitPaper">保存修改</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {
  getExam, updateExam, publishExam, listExamBankOptions, listKnowledgePoints, tryDraw, saveExamConfig, getExamConfig
} from '@/api/business/exam'
import { isSuperAdminRole } from '@/utils/permission'

function emptyExamForm() {
  return {
    id: null,
    examName: '',
    singleCount: 0,
    multiCount: 0,
    judgeCount: 0,
    singleScore: 1,
    multiScore: 1,
    judgeScore: 1,
    duration: 0,
    passLine: 6
  }
}

function emptyPaperForm() {
  return { id: null, examName: '', description: '', difficulty: '', contentBias: '' }
}

/**
 * 套卷配置（独立页）：从「模拟备考管理 → 模拟理论考核」列表点「配置」进来。
 * 页面职责：基础信息只读摘要 + 组卷配置（分值/时长/通过线/题库抽题量/试抽）→ 保存并生效。
 */
export default {
  name: 'DeptPaperConfig',
  data() {
    return {
      loading: false,
      saving: false,
      examId: null,
      /** 套卷本体（基础信息只读展示） */
      exam: {},
      /** 组卷配置表单（只含需要 examId 才能改的项） */
      examForm: emptyExamForm(),
      /** 本部门题池概览（可用题量，按题型） */
      poolMeta: null,
      /** 本部门题池的知识点及题量 */
      poolPoints: [],
      /** 知识点配比明细（落 exam_knowledge_rule） */
      knowledgeRules: [],
      drawing: false,
      previewQuestions: [],
      durationOptions: [0, 30, 45, 60, 90, 120, 180, 240],
      durationPreset: 0,
      // 基础信息弹窗
      paperDialogVisible: false,
      paperDialogTitle: '',
      paperForm: emptyPaperForm(),
      paperRules: {
        examName: [{ required: true, message: '请输入套卷名称', trigger: 'blur' }]
      }
    }
  },
  computed: {
    /** 卷面题量（= 题型数量合计，须与知识点抽题合计一致） */
    questionTotal() {
      return (Number(this.examForm.singleCount) || 0) + (Number(this.examForm.multiCount) || 0) + (Number(this.examForm.judgeCount) || 0)
    },
    /** 知识点抽题数量合计 */
    knowledgeCountSum() {
      return this.knowledgeRules.reduce((sum, r) => sum + (Number(r.questionCount) || 0), 0)
    },
    totalScore() {
      return (Number(this.examForm.singleCount) || 0) * (Number(this.examForm.singleScore) || 0) +
        (Number(this.examForm.multiCount) || 0) * (Number(this.examForm.multiScore) || 0) +
        (Number(this.examForm.judgeCount) || 0) * (Number(this.examForm.judgeScore) || 0)
    },
    /** 超出题池该知识点可用题量的行 */
    overRows() {
      return this.knowledgeRules.filter(r => (Number(r.questionCount) || 0) > this.poolAvailable(r.knowledgePoint))
    },
    checkOk() {
      if (!this.knowledgeRules.length) return false
      if (this.knowledgeRules.some(r => !String(r.knowledgePoint || '').trim())) return false
      if (this.knowledgeCountSum <= 0) return false
      if (this.questionTotal <= 0 || this.questionTotal !== this.knowledgeCountSum) return false
      if (this.totalScore <= 0) return false
      return this.overRows.length === 0
    },
    checkMessage() {
      if (!this.knowledgeRules.length) return '尚未配置知识点配比：点右上角「从题池导入知识点」把本部门已有知识点带进来，再逐个填抽题数量。'
      if (this.knowledgeRules.some(r => !String(r.knowledgePoint || '').trim())) return '存在未选知识点的行，请选择知识点或删除该行。'
      if (this.knowledgeCountSum <= 0) return '抽题数量合计为 0，请至少为一个知识点设置抽题数量。'
      if (this.questionTotal <= 0) return '卷面题量为 0：请在上方「卷面结构」里填单选题 / 多选题 / 判断题的数量。'
      if (this.questionTotal !== this.knowledgeCountSum) {
        return '知识点抽题数量合计（' + this.knowledgeCountSum + ' 题）须等于卷面题量（' + this.questionTotal + ' 题）。'
      }
      if (this.totalScore <= 0) return '每题分值都是 0，卷面满分为 0。请在上方「卷面结构」里设置每题分值。'
      if (this.overRows.length) {
        return '「' + this.overRows.map(r => r.knowledgePoint).join('、') + '」的抽题数量超过题池该知识点可用题量，会抽不满卷。'
      }
      return ''
    }
  },
  created() {
    this.examId = this.$route.params.examId ? Number(this.$route.params.examId) : null
    if (!this.examId) {
      this.$modal.msgWarning('缺少套卷ID，请从套卷列表点「配置」进入')
      this.goBack()
      return
    }
    this.loadExam()
    this.loadPool()
  },
  methods: {
    goBack() {
      // 返回套卷列表：按角色回到各自前缀（部门端 /department/study/prep、超管端 /super/ops/prep）
      const isSuper = isSuperAdminRole(this.roles) || isSuperAdminRole()
      this.$router.push((isSuper ? '/super/ops/prep' : '/department/study/prep') + '?tab=module')
    },
    /** 套卷本体 + 组卷配置 */
    loadExam() {
      this.loading = true
      getExam(this.examId).then(res => {
        const d = res.data || {}
        this.exam = d
        this.examForm = {
          id: d.id,
          examName: d.examName || '',
          singleCount: Number(d.singleCount) || 0,
          multiCount: Number(d.multiCount) || 0,
          judgeCount: Number(d.judgeCount) || 0,
          singleScore: Number(d.singleScore) || 0,
          multiScore: Number(d.multiScore) || 0,
          judgeScore: Number(d.judgeScore) || 0,
          duration: Number(d.duration) || 0,
          passLine: Number(d.passLine) || 0
        }
        this.durationPreset = this.durationOptions.indexOf(this.examForm.duration) > -1 ? this.examForm.duration : -1
        this.loading = false
      }).catch(() => { this.loading = false })
      getExamConfig(this.examId).then(res => {
        const data = res.data || {}
        this.knowledgeRules = (data.knowledgeRules || []).map((r, i) => ({
          knowledgePoint: r.knowledgePoint || '',
          questionCount: Number(r.questionCount) || 0,
          sortNo: r.sortNo || i + 1
        }))
      }).catch(() => { this.knowledgeRules = [] })
    },
    /**
     * 拉取本部门题池：可用题量（按题型）+ 知识点清单。
     * ★ 2026-09-23：题库概念退场，一个部门只有一个理论题池（bank-options 固定返回 1 条）。
     */
    loadPool() {
      return Promise.all([
        listExamBankOptions(null, 'PRACTICE').catch(() => ({ data: [] })),
        listKnowledgePoints(0).catch(() => ({ data: [] }))
      ]).then(([optRes, kpRes]) => {
        const opt = (optRes.data || [])[0] || null
        this.poolMeta = opt
          ? {
            bankName: opt.bankName,
            totalCount: Number(opt.totalCount) || 0,
            singleCount: Number(opt.singleCount) || 0,
            multiCount: Number(opt.multiCount) || 0,
            judgeCount: Number(opt.judgeCount) || 0
          }
          : null
        this.poolPoints = (kpRes.data || []).map(p => ({
          knowledgePoint: p.knowledgePoint,
          totalCount: Number(p.totalCount) || 0
        }))
      })
    },
    /** 题池某知识点的可用题量（查不到时不设上限，避免把已存值夹成 0） */
    poolAvailable(point) {
      if (!point) return Number.MAX_SAFE_INTEGER
      const p = this.poolPoints.find(x => x.knowledgePoint === point)
      return p ? p.totalCount : Number.MAX_SAFE_INTEGER
    },
    /** 单个知识点占总题量的比例（展示用，四舍五入到整数） */
    ruleRatioText(row) {
      const total = this.knowledgeCountSum
      if (!total) return '0'
      return Math.round((Number(row.questionCount) || 0) * 100 / total)
    },
    /** 添加一行知识点配比 */
    addKnowledgeRule(point) {
      this.knowledgeRules.push({ knowledgePoint: point || '', questionCount: 0 })
    },
    /** 「从题池导入知识点」：把题池里已有知识点带进来（已存在的跳过，保留已填数量） */
    importFromPool() {
      if (!this.poolPoints.length) {
        this.$modal.msgWarning('本部门题池还没有题目，请先到「题库管理」导入题目')
        return
      }
      const exist = {}
      this.knowledgeRules.forEach(r => { if (r.knowledgePoint) exist[r.knowledgePoint] = true })
      const added = []
      this.poolPoints.forEach(p => {
        if (exist[p.knowledgePoint]) return
        exist[p.knowledgePoint] = true
        added.push({ knowledgePoint: p.knowledgePoint, questionCount: 0 })
      })
      if (!added.length) {
        this.$modal.msgWarning('题池里的知识点都已在本表里，未重复添加')
        return
      }
      this.knowledgeRules = this.knowledgeRules.concat(added)
      this.$modal.msgSuccess('已带入 ' + added.length + ' 个知识点，请逐个填写抽题数量')
    },
    knowledgePayload() {
      return this.knowledgeRules
        .filter(r => String(r.knowledgePoint || '').trim())
        .map((r, i) => ({
          knowledgePoint: String(r.knowledgePoint).trim(),
          questionCount: Number(r.questionCount) || 0,
          sortNo: i + 1
        }))
    },
    // ---- 基础信息弹窗 ----
    openPaperDialog() {
      this.paperForm = {
        id: this.exam.id,
        examName: this.exam.examName || '',
        description: this.exam.description || '',
        difficulty: this.exam.difficulty || '',
        contentBias: this.exam.contentBias || ''
      }
      this.paperDialogTitle = '编辑套卷基础信息'
      this.paperDialogVisible = true
      this.$nextTick(() => this.$refs.paperForm && this.$refs.paperForm.clearValidate())
    },
    submitPaper() {
      this.$refs.paperForm.validate(valid => {
        if (!valid) return
        this.saving = true
        const base = {
          examName: (this.paperForm.examName || '').trim(),
          description: this.paperForm.description || null,
          difficulty: this.paperForm.difficulty || null,
          contentBias: this.paperForm.contentBias || null
        }
        updateExam(Object.assign({ id: this.paperForm.id }, base)).then(() => {
          this.exam = Object.assign({}, this.exam, {
            examName: base.examName,
            description: base.description || '',
            difficulty: base.difficulty || '',
            contentBias: base.contentBias || ''
          })
          this.examForm.examName = base.examName
          this.saving = false
          this.paperDialogVisible = false
          this.$modal.msgSuccess('套卷基础信息已更新')
        }).catch(() => { this.saving = false })
      })
    },
    // ---- 组卷配置 ----
    onDurationPresetChange(v) {
      if (v !== -1) {
        this.examForm.duration = v
      } else if (!this.examForm.duration) {
        this.examForm.duration = 60
      }
    },
    /** 试抽一套（真实调后端，按知识点配比从本部门题池抽题） */
    drawOnce() {
      if (!this.checkOk) {
        this.$modal.msgWarning(this.checkMessage || '请先完成知识点配比')
        return
      }
      this.drawing = true
      tryDraw({ deptId: this.exam && this.exam.deptId, knowledgeRules: this.knowledgePayload() }).then(res => {
        this.previewQuestions = res.data || []
        this.drawing = false
        if (!this.previewQuestions.length) {
          this.$modal.msgWarning('按当前配置抽不到题，请检查题池各知识点的题目是否充足')
        }
      }).catch(() => { this.drawing = false })
    },
    /** 保存并生效：保存基本信息与知识点配比 → 发布（草稿也会被发布出去） */
    saveExam() {
      if (!this.checkOk) {
        this.$modal.msgWarning(this.checkMessage || '配置校验未通过')
        return
      }
      this.saving = true
      const configPayload = {
        singleCount: Number(this.examForm.singleCount) || 0,
        multiCount: Number(this.examForm.multiCount) || 0,
        judgeCount: Number(this.examForm.judgeCount) || 0,
        singleScore: this.examForm.singleScore,
        multiScore: this.examForm.multiScore,
        judgeScore: this.examForm.judgeScore,
        duration: this.examForm.duration,
        passLine: this.examForm.passLine,
        knowledgeRules: this.knowledgePayload(),
        assignMode: 'ALL'
      }
      updateExam(Object.assign({ id: this.examId }, configPayload)).then(() => {
        saveExamConfig(this.examId, configPayload).then(() => {
          publishExam(this.examId).then(() => {
            this.saving = false
            this.$modal.msgSuccess('套卷已保存并生效，实习生即可开始练习')
            this.goBack()
          }).catch(() => { this.saving = false })
        }).catch(() => { this.saving = false })
      }).catch(() => { this.saving = false })
    },
    // ---- 展示辅助 ----
    examStatusText(s) {
      return { DRAFT: '草稿', PUBLISHED: '已发布', DISABLED: '已停用', GRADING: '批改中' }[s] || s
    },
    difficultyText(d) {
      return { EASY: '简单', MEDIUM: '中等', HARD: '困难' }[d] || '未设置'
    },
    difficultyTone(d) {
      return { EASY: 'green', MEDIUM: 'orange', HARD: 'red' }[d] || 'gray'
    },
    typeText(t) {
      return { SINGLE: '单选', MULTI: '多选', JUDGE: '判断' }[t] || t
    }
  }
}
</script>

<style lang="scss" scoped>
@import '~@/assets/styles/department-module.scss';

/* 基础信息摘要 */
.pc-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 12px; }
.pc-item { padding: 10px 12px; background: #f8fafc; border: 1px solid #eef1f6; border-radius: 8px; }
.pc-item label { display: block; margin-bottom: 5px; color: #98a2b3; font-size: 11.5px; }
.pc-item span { color: #344054; font-size: 13px; line-height: 1.6; }
.pc-item span.strong { color: #1d2939; font-size: 14px; font-weight: 600; }
</style>
