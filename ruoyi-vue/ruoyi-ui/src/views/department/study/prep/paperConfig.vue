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
                <el-form-item label="每题分值">
                  <span>单选</span><el-input-number v-model="examForm.singleScore" :min="0" :max="100" :precision="1" size="small" style="width:110px" />
                  <span style="margin:0 6px">多选</span><el-input-number v-model="examForm.multiScore" :min="0" :max="100" :precision="1" size="small" style="width:110px" />
                  <span style="margin:0 6px">判断</span><el-input-number v-model="examForm.judgeScore" :min="0" :max="100" :precision="1" size="small" style="width:110px" />
                </el-form-item>
                <el-form-item>
                  <span class="dinp">题目数量 {{ questionTotal }} 题 · 满分 {{ totalScore }} 分</span>
                </el-form-item>
              </el-form>

              <div class="dcallout" :class="checkOk ? 'ok' : 'warn'" style="margin:0 0 12px">
                <i :class="checkOk ? 'el-icon-success' : 'el-icon-warning-outline'" />
                <span>
                  <template v-if="checkOk">校验通过：共 {{ bankRules.length }} 个题库 · 合计 {{ questionTotal }} 题（单选 {{ countOf('singleCount') }} + 多选 {{ countOf('multiCount') }} + 判断 {{ countOf('judgeCount') }}）· 满分 {{ totalScore }} 分 ✓</template>
                  <template v-else>{{ checkMessage }}</template>
                </span>
              </div>

              <div class="dgrid">
                <div class="c8">
                  <div class="dcard-h" style="padding:0 0 10px">
                    <div class="tt"><span class="idx">分</span><h3>组卷题库（每个题库分别设置抽题量）</h3></div>
                    <div class="dbtn-row">
                      <el-select v-model="addBankId" size="mini" placeholder="＋ 添加题库" style="width:200px" @change="addBankRule">
                        <el-option
                          v-for="b in bankMeta"
                          :key="b.bankId"
                          :label="b.bankName + '（可用 ' + b.totalCount + ' 题）'"
                          :value="b.bankId"
                          :disabled="bankChosen(b.bankId)"
                        />
                      </el-select>
                    </div>
                  </div>
                  <el-table :data="bankRules" size="mini" border empty-text="点右上角「＋ 添加题库」选择参与组卷的题库">
                    <el-table-column label="题库" min-width="160">
                      <template slot-scope="scope">
                        <el-select v-model="scope.row.bankId" size="mini" filterable style="width:100%">
                          <el-option v-for="b in bankMeta" :key="b.bankId" :label="b.bankName" :value="b.bankId" />
                        </el-select>
                      </template>
                    </el-table-column>
                    <el-table-column label="可用" width="76" align="center">
                      <template slot-scope="scope"><span class="muted">{{ bankAvailable(scope.row.bankId) }} 题</span></template>
                    </el-table-column>
                    <el-table-column label="单选" width="108" align="center">
                      <template slot-scope="scope">
                        <el-input-number v-model="scope.row.singleCount" :min="0" :max="bankAvailOf(scope.row.bankId, 'singleCount')" size="mini" style="width:92px" />
                      </template>
                    </el-table-column>
                    <el-table-column label="多选" width="108" align="center">
                      <template slot-scope="scope">
                        <el-input-number v-model="scope.row.multiCount" :min="0" :max="bankAvailOf(scope.row.bankId, 'multiCount')" size="mini" style="width:92px" />
                      </template>
                    </el-table-column>
                    <el-table-column label="判断" width="108" align="center">
                      <template slot-scope="scope">
                        <el-input-number v-model="scope.row.judgeCount" :min="0" :max="bankAvailOf(scope.row.bankId, 'judgeCount')" size="mini" style="width:92px" />
                      </template>
                    </el-table-column>
                    <el-table-column label="小计" width="70" align="center">
                      <template slot-scope="scope"><b>{{ ruleRowTotal(scope.row) }}</b></template>
                    </el-table-column>
                    <el-table-column label="操作" width="70" align="center">
                      <template slot-scope="scope">
                        <el-button type="text" size="mini" class="danger-text" @click="bankRules.splice(scope.$index, 1)">删除</el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                  <p class="dsec-note">抽题时在<b>每个题库内按题型随机取题</b>，再组合成卷。「抽题量 ≤ 题库可用」超了会抽不满卷，保存时后端会拦截。</p>
                </div>

                <div class="c4">
                  <div class="dcard-h" style="padding:0 0 10px">
                    <div class="tt"><span class="idx o">抽</span><h3>试抽一套（校验用）</h3></div>
                  </div>
                  <div class="dinp ta preview-box">
                    <div v-if="drawing" class="drawing"><i class="el-icon-loading" /> 正在抽题…</div>
                    <div v-else-if="previewQuestions.length">
                      <div v-for="q in previewQuestions" :key="q.seq" class="pq">{{ q.seq }} · {{ typeText(q.qtype) }} · {{ q.bankName }} · {{ q.stem }}</div>
                    </div>
                    <div v-else style="color:#98a2b3">点下方按钮，按当前组卷配置真实抽一套卷看看效果。</div>
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
  getExam, updateExam, publishExam, listExamBankOptions, tryDrawByBanks, saveExamConfig, getExamConfig
} from '@/api/business/exam'

function emptyExamForm() {
  return {
    id: null,
    examName: '',
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
      bankMeta: [],
      bankRules: [],
      addBankId: null,
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
    /** 组卷抽题合计（各题库单选 + 多选 + 判断） */
    questionTotal() {
      return this.bankRules.reduce((sum, r) => sum + this.ruleRowTotal(r), 0)
    },
    totalScore() {
      return this.countOf('singleCount') * (Number(this.examForm.singleScore) || 0) +
        this.countOf('multiCount') * (Number(this.examForm.multiScore) || 0) +
        this.countOf('judgeCount') * (Number(this.examForm.judgeScore) || 0)
    },
    /** 超出题库可用题量的行 */
    overRows() {
      return this.bankRules.filter(r => {
        const avail = this.bankMetaOf(r.bankId)
        return (Number(r.singleCount) || 0) > avail.singleCount ||
          (Number(r.multiCount) || 0) > avail.multiCount ||
          (Number(r.judgeCount) || 0) > avail.judgeCount
      })
    },
    checkOk() {
      if (!this.bankRules.length) return false
      if (this.bankRules.some(r => !r.bankId)) return false
      if (this.questionTotal <= 0) return false
      return this.overRows.length === 0
    },
    checkMessage() {
      if (!this.bankRules.length) return '尚未配置组卷题库：点右上角「＋ 添加题库」选择参与组卷的题库，再分别设置每个题库的抽题数量。'
      if (this.bankRules.some(r => !r.bankId)) return '存在未选择题库的行，请选择对应题库或删除该行。'
      if (this.questionTotal <= 0) return '抽题数量合计为 0，请至少为一个题库设置抽题数量。'
      if (this.overRows.length) {
        return '「' + this.overRows.map(r => this.bankNameOf(r.bankId)).join('、') + '」的抽题数量超过该题库可用题量，会抽不满卷。'
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
    this.loadBankMeta()
  },
  methods: {
    goBack() {
      // 返回套卷列表：按角色回到各自前缀（部门端 /department/study/prep、超管端 /super/ops/prep）
      const isSuper = (this.$store.getters.roles || []).indexOf('SUPER_ADMIN') > -1
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
        this.bankRules = (data.bankRules || []).map((r, i) => ({
          bankId: r.bankId,
          bankName: r.bankName,
          singleCount: Number(r.singleCount) || 0,
          multiCount: Number(r.multiCount) || 0,
          judgeCount: Number(r.judgeCount) || 0,
          sortNo: r.sortNo || i + 1
        }))
      }).catch(() => { this.bankRules = [] })
    },
    /** 本部门可选题库（含各库按题型的可用题量） */
    loadBankMeta() {
      // 候选库 = 形态 × 用途：模拟理论卷只取「理论库 × 模拟/通用」（防抽到正式题库）
      listExamBankOptions(null, 'PRACTICE', 'THEORY').then(res => {
        this.bankMeta = (res.data || []).map(b => ({
          bankId: b.bankId,
          bankName: b.bankName,
          totalCount: Number(b.totalCount) || 0,
          singleCount: Number(b.singleCount) || 0,
          multiCount: Number(b.multiCount) || 0,
          judgeCount: Number(b.judgeCount) || 0
        }))
      }).catch(() => { this.bankMeta = [] })
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
    bankMetaOf(bankId) {
      return this.bankMeta.find(b => b.bankId === bankId) ||
        { bankName: '未选择题库', totalCount: 0, singleCount: 0, multiCount: 0, judgeCount: 0 }
    },
    bankNameOf(bankId) {
      return this.bankMetaOf(bankId).bankName || '未选择题库'
    },
    bankAvailable(bankId) {
      return this.bankMetaOf(bankId).totalCount
    },
    bankAvailOf(bankId, key) {
      return this.bankMetaOf(bankId)[key] || 0
    },
    bankChosen(bankId) {
      return this.bankRules.filter(r => r.bankId === bankId).length
    },
    ruleRowTotal(row) {
      return (Number(row.singleCount) || 0) + (Number(row.multiCount) || 0) + (Number(row.judgeCount) || 0)
    },
    /** 按题型汇总各库抽题量 */
    countOf(key) {
      return this.bankRules.reduce((sum, r) => sum + (Number(r[key]) || 0), 0)
    },
    addBankRule(bankId) {
      if (bankId === null || bankId === undefined || bankId === '') return
      if (this.bankChosen(bankId)) {
        this.$modal.msgWarning('该题库已在组卷列表中，请直接修改它的抽题量')
        this.addBankId = null
        return
      }
      this.bankRules.push({
        bankId,
        bankName: this.bankNameOf(bankId),
        singleCount: 0,
        multiCount: 0,
        judgeCount: 0,
        sortNo: this.bankRules.length + 1
      })
      this.addBankId = null
    },
    rulePayload() {
      return this.bankRules
        .filter(r => r.bankId)
        .map((r, i) => ({
          bankId: r.bankId,
          singleCount: Number(r.singleCount) || 0,
          multiCount: Number(r.multiCount) || 0,
          judgeCount: Number(r.judgeCount) || 0,
          sortNo: i + 1
        }))
    },
    onDurationPresetChange(v) {
      if (v !== -1) {
        this.examForm.duration = v
      } else if (!this.examForm.duration) {
        this.examForm.duration = 60
      }
    },
    /** 试抽一套（真实调后端按多题库组卷配置抽题） */
    drawOnce() {
      if (!this.checkOk) {
        this.$modal.msgWarning(this.checkMessage || '请先完成组卷配置')
        return
      }
      this.drawing = true
      tryDrawByBanks({ bankRules: this.rulePayload() }).then(res => {
        this.previewQuestions = res.data || []
        this.drawing = false
        if (!this.previewQuestions.length) {
          this.$modal.msgWarning('按当前配置抽不到题，请检查各题库题目是否充足')
        }
      }).catch(() => { this.drawing = false })
    },
    /** 保存并生效：保存基本信息与组卷配置 → 发布（草稿也会被发布出去） */
    saveExam() {
      if (!this.checkOk) {
        this.$modal.msgWarning(this.checkMessage || '配置校验未通过')
        return
      }
      this.saving = true
      const configPayload = {
        singleScore: this.examForm.singleScore,
        multiScore: this.examForm.multiScore,
        judgeScore: this.examForm.judgeScore,
        duration: this.examForm.duration,
        passLine: this.examForm.passLine,
        bankRules: this.rulePayload(),
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
