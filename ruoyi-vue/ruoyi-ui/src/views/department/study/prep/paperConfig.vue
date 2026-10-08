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
                  <template v-if="checkOk && poolOnlyMode">校验通过：整池抽题 · 按卷面题型数量从整个题池抽 {{ questionTotal }} 题 · 满分 {{ totalScore }} 分 ✓</template>
                  <template v-else-if="checkOk">校验通过：{{ knowledgeRules.filter(r => rowTotal(r) > 0).length }} 个知识点 · 单选 {{ typeSums.SINGLE }} / 多选 {{ typeSums.MULTI }} / 判断 {{ typeSums.JUDGE }} 题 · 合计 {{ knowledgeCountSum }} 题 · 满分 {{ totalScore }} 分 ✓</template>
                  <template v-else>{{ checkMessage }}</template>
                </span>
              </div>

              <div class="dgrid">
                <div class="c8">
                  <div class="dcard-h" style="padding:0 0 10px">
                    <div class="tt"><span class="idx">分</span><h3>知识点配比（每个知识点抽几道题）</h3></div>
                    <div class="dbtn-row">
                      <el-button size="mini" icon="el-icon-refresh" @click="reloadPool">重新载入题池</el-button>
                      <el-button
                        size="mini"
                        :type="poolOnlyMode ? 'success' : ''"
                        icon="el-icon-s-grid"
                        @click="toggleWholePool"
                      >{{ poolOnlyMode ? '整池抽题（点此恢复按知识点）' : '不分配知识点，整池抽题' }}</el-button>
                    </div>
                  </div>

                  <el-alert
                    v-if="poolOnlyMode"
                    type="success"
                    :closable="false"
                    show-icon
                    style="margin-bottom:10px"
                    title="整池抽题模式：不按知识点分配，直接按上方的题型数量（单选 / 多选 / 判断）从本部门整个理论题池随机抽题。"
                  />

                  <el-table
                    :data="knowledgeRules"
                    size="mini"
                    border
                    :class="{ 'pool-off': poolOnlyMode }"
                    empty-text="本部门题池暂无知识点，请先到「题库管理」导入题目"
                  >
                    <el-table-column label="知识点（章节）" min-width="150">
                      <template slot-scope="scope">
                        <span>{{ scope.row.knowledgePoint }}</span>
                        <span v-if="!isKnownPoint(scope.row.knowledgePoint)" class="kp-warn">题池无此知识点</span>
                      </template>
                    </el-table-column>
                    <el-table-column label="题池可用" width="118" align="center">
                      <template slot-scope="scope">
                        <div class="avail-cell">
                          <b>{{ poolAvailable(scope.row.knowledgePoint) }}</b><small>题</small>
                          <span class="avail-sub">{{ poolTypeText(scope.row.knowledgePoint) }}</span>
                        </div>
                      </template>
                    </el-table-column>
                    <el-table-column label="单选" width="92" align="center">
                      <template slot-scope="scope">
                        <el-input-number
                          v-model="scope.row.singleCount"
                          :min="0"
                          :max="typeInputMax(scope.row, 'SINGLE')"
                          :disabled="poolOnlyMode"
                          size="mini"
                          style="width:80px"
                          @change="onTypeChange(scope.row, 'SINGLE')"
                        />
                      </template>
                    </el-table-column>
                    <el-table-column label="多选" width="92" align="center">
                      <template slot-scope="scope">
                        <el-input-number
                          v-model="scope.row.multiCount"
                          :min="0"
                          :max="typeInputMax(scope.row, 'MULTI')"
                          :disabled="poolOnlyMode"
                          size="mini"
                          style="width:80px"
                          @change="onTypeChange(scope.row, 'MULTI')"
                        />
                      </template>
                    </el-table-column>
                    <el-table-column label="判断" width="92" align="center">
                      <template slot-scope="scope">
                        <el-input-number
                          v-model="scope.row.judgeCount"
                          :min="0"
                          :max="typeInputMax(scope.row, 'JUDGE')"
                          :disabled="poolOnlyMode"
                          size="mini"
                          style="width:80px"
                          @change="onTypeChange(scope.row, 'JUDGE')"
                        />
                      </template>
                    </el-table-column>
                    <el-table-column label="小计" width="76" align="center">
                      <template slot-scope="scope">
                        <span v-if="rowTotal(scope.row) > 0" class="alloc-badge on">{{ rowTotal(scope.row) }} 题</span>
                        <span v-else class="alloc-badge">未分配</span>
                      </template>
                    </el-table-column>
                    <el-table-column label="操作" width="62" align="center">
                      <template slot-scope="scope">
                        <el-button type="text" size="mini" class="danger-text" :disabled="poolOnlyMode" @click="removeRule(scope.$index)">删除</el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                  <div class="type-sum">
                    <span :class="{ bad: typeSums.SINGLE !== paperCount('SINGLE') }">单选 已配 <b>{{ typeSums.SINGLE }}</b> / 卷面 <b>{{ paperCount('SINGLE') }}</b></span>
                    <span :class="{ bad: typeSums.MULTI !== paperCount('MULTI') }">多选 已配 <b>{{ typeSums.MULTI }}</b> / 卷面 <b>{{ paperCount('MULTI') }}</b></span>
                    <span :class="{ bad: typeSums.JUDGE !== paperCount('JUDGE') }">判断 已配 <b>{{ typeSums.JUDGE }}</b> / 卷面 <b>{{ paperCount('JUDGE') }}</b></span>
                  </div>
                  <p class="dsec-note">
                    题池知识点已在页面加载时自动带入（不需要手工挑选）。<b>每个知识点分别填 单选 / 多选 / 判断 各抽几题</b>，
                    三类「已配」合计须分别等于上方卷面结构里的该题型数量；某行全为 0 表示该知识点不参与抽题。
                    单行每个题型都不能超过该知识点该题型的「题池可用」。抽题时在<b>本部门理论题池内按知识点 + 题型抽取</b>，跨知识点去重。
                    若不想按知识点分配，点上方「不分配知识点，整池抽题」即可按题型从整个题池抽
                    （保存后本套卷即按整池抽题，再次进入本页需重新点一次）。
                  </p>
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

/** 三种题型：键 / 中文名 / 行上的字段名 / 题池里的可用量字段名 */
const TYPE_META = [
  { key: 'SINGLE', name: '单选', rowKey: 'singleCount', availKey: 'singleCount' },
  { key: 'MULTI', name: '多选', rowKey: 'multiCount', availKey: 'multiCount' },
  { key: 'JUDGE', name: '判断', rowKey: 'judgeCount', availKey: 'judgeCount' }
]

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
      /** 本部门题池的知识点及题量（含分题型可用量） */
      poolPoints: [],
      /** 已保存的知识点配比（getExamConfig 原样，用于自动带入知识点时回填「已分配」数量） */
      savedRules: [],
      /** 知识点配比明细（落 exam_knowledge_rule）：题池全部知识点 + 已存配置回填的数量 */
      knowledgeRules: [],
      /**
       * ★ 2026-09-24：整池抽题模式 —— 不按知识点分配，只按卷面题型数量从整个题池抽。
       * true 时 knowledgePayload() 提交空配比、校验只查题型数量与整池可用量。
       */
      poolOnlyMode: false,
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
    /** 知识点抽题数量合计（各行「单选+多选+判断」之和） */
    knowledgeCountSum() {
      return this.knowledgeRules.reduce((sum, r) => sum + this.rowTotal(r), 0)
    },
    /** 各题型的「已配」合计 */
    typeSums() {
      const t = { SINGLE: 0, MULTI: 0, JUDGE: 0 }
      this.knowledgeRules.forEach(r => {
        t.SINGLE += Number(r.singleCount) || 0
        t.MULTI += Number(r.multiCount) || 0
        t.JUDGE += Number(r.judgeCount) || 0
      })
      return t
    },
    totalScore() {
      return (Number(this.examForm.singleCount) || 0) * (Number(this.examForm.singleScore) || 0) +
        (Number(this.examForm.multiCount) || 0) * (Number(this.examForm.multiScore) || 0) +
        (Number(this.examForm.judgeCount) || 0) * (Number(this.examForm.judgeScore) || 0)
    },
    /** 整池的分题型可用量（整池抽题模式下用它做题型校验） */
    poolTypeTotal() {
      const t = { SINGLE: 0, MULTI: 0, JUDGE: 0 }
      this.poolPoints.forEach(p => {
        t.SINGLE += Number(p.singleCount) || 0
        t.MULTI += Number(p.multiCount) || 0
        t.JUDGE += Number(p.judgeCount) || 0
      })
      return t
    },
    /** 「某一行某题型」超过该知识点该题型可用量的组合（题池查不到该知识点 = 可用 0） */
    overRows() {
      if (this.poolOnlyMode) return []
      const bad = []
      this.knowledgeRules.forEach(r => {
        TYPE_META.forEach(t => {
          const need = Number(r[t.rowKey]) || 0
          const have = this.poolAvailableOf(r.knowledgePoint, t.key)
          if (need > have) {
            bad.push({ point: r.knowledgePoint, name: t.name, need: need, have: have })
          }
        })
      })
      return bad
    },
    /** 整池抽题模式用：整池的分题型可用量 vs 卷面题型需求（非整池模式恒 ok） */
    poolQuota() {
      const need = {
        SINGLE: Number(this.examForm.singleCount) || 0,
        MULTI: Number(this.examForm.multiCount) || 0,
        JUDGE: Number(this.examForm.judgeCount) || 0
      }
      if (!this.poolOnlyMode) {
        return { need: need, have: { SINGLE: 0, MULTI: 0, JUDGE: 0 }, lacks: [], ok: true }
      }
      const t = this.poolTypeTotal
      const have = { SINGLE: t.SINGLE, MULTI: t.MULTI, JUDGE: t.JUDGE }
      const lacks = []
      TYPE_META.forEach(x => {
        if (need[x.key] > have[x.key]) lacks.push({ name: x.name, need: need[x.key], have: have[x.key] })
      })
      return { need: need, have: have, lacks: lacks, ok: lacks.length === 0 }
    },
    checkOk() {
      if (this.questionTotal <= 0) return false
      if (this.totalScore <= 0) return false
      if (this.poolOnlyMode) {
        // 整池抽题：不要求知识点配比，只要题型数量能被整池覆盖
        return this.poolQuota.ok
      }
      if (!this.knowledgeRules.length) return false
      if (this.knowledgeRules.some(r => !String(r.knowledgePoint || '').trim())) return false
      if (this.knowledgeCountSum <= 0) return false
      if (this.overRows.length) return false
      // ★ 分题型配平：三类「已配」必须分别等于卷面该题型数量
      return TYPE_META.every(t => this.typeSums[t.key] === this.paperCount(t.key))
    },
    checkMessage() {
      if (this.questionTotal <= 0) return '卷面题量为 0：请在上方「卷面结构」里填单选题 / 多选题 / 判断题的数量。'
      if (this.totalScore <= 0) return '每题分值都是 0，卷面满分为 0。请在上方「卷面结构」里设置每题分值。'
      if (this.poolOnlyMode) {
        if (this.poolQuota.ok) return ''
        return '卷面题型数量超出本部门题池的可用题量：'
          + this.poolQuota.lacks.map(l => l.name + '需 ' + l.need + ' 题、现有 ' + l.have + ' 题').join('；')
          + '。请调整卷面题型数量，或先到「题库管理」补齐题目。'
      }
      if (!this.knowledgeRules.length) return '本部门题池还没有知识点：请先到「题库管理」导入题目，再回来配置。'
      if (this.knowledgeRules.some(r => !String(r.knowledgePoint || '').trim())) return '存在未选知识点的行，请删除该行。'
      if (this.knowledgeCountSum <= 0) return '抽题数量合计为 0，请为知识点分配单选 / 多选 / 判断数量；或不分配知识点、改用「整池抽题」。'
      if (this.overRows.length) {
        return this.overRows.map(b => '「' + b.point + '」' + b.name + '要 ' + b.need + ' 题、题池只有 ' + b.have + ' 题').join('；')
          + '。请调小对应数量。'
      }
      const bad = TYPE_META.filter(t => this.typeSums[t.key] !== this.paperCount(t.key))
        .map(t => t.name + ' 已配 ' + this.typeSums[t.key] + ' / 卷面 ' + this.paperCount(t.key))
      if (bad.length) {
        return '各题型的配比合计须分别等于卷面该题型数量（' + bad.join('；') + '），请按题型逐类配平。'
      }
      return ''
    }
  },
  async created() {
    this.examId = this.$route.params.examId ? Number(this.$route.params.examId) : null
    if (!this.examId) {
      this.$modal.msgWarning('缺少套卷ID，请从套卷列表点「配置」进入')
      this.goBack()
      return
    }
    // ★ 2026-09-24：必须「先 await 取完题池 + 已存配置，再渲染知识点表」。
    //   知识点表是自动带入的，且抽题数量输入框的 :max 依赖题池可用题量；
    //   若并行加载，行会在题池数据到位前渲染，el-input-number 会把已存的抽题数量夹成 0（静默数据损坏）。
    await this.loadExam()
    await this.loadPool()
    this.buildKnowledgeRules()
  },
  methods: {
    goBack() {
      // 返回套卷列表：按角色回到各自前缀（部门端 /department/study/prep、超管端 /super/ops/prep）
      const isSuper = isSuperAdminRole(this.roles) || isSuperAdminRole()
      this.$router.push((isSuper ? '/super/ops/prep' : '/department/study/prep') + '?tab=module')
    },
    /** 套卷本体 + 组卷配置（返回 Promise，供 created 串行等待） */
    loadExam() {
      this.loading = true
      const p1 = getExam(this.examId).then(res => {
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
      // 已存的知识点配比先暂存到 savedRules，等题池到位后由 buildKnowledgeRules() 合并回填
      const p2 = getExamConfig(this.examId).then(res => {
        const data = res.data || {}
        this.savedRules = (data.knowledgeRules || []).map((r, i) => ({
          knowledgePoint: r.knowledgePoint || '',
          questionCount: Number(r.questionCount) || 0,
          singleCount: Number(r.singleCount) || 0,
          multiCount: Number(r.multiCount) || 0,
          judgeCount: Number(r.judgeCount) || 0,
          sortNo: r.sortNo || i + 1
        }))
      }).catch(() => { this.savedRules = [] })
      return Promise.all([p1, p2])
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
          totalCount: Number(p.totalCount) || 0,
          singleCount: Number(p.singleCount) || 0,
          multiCount: Number(p.multiCount) || 0,
          judgeCount: Number(p.judgeCount) || 0
        }))
      })
    },
    /**
     * 把「题池全部知识点」自动带成配比行，并用已存配置回填**分题型**数量。
     * 已存配置里若含题池已不存在的知识点，也**保留该行且不动数量**，交给校验条提示。
     */
    buildKnowledgeRules() {
      // 若已存配比里有任何一行分配 > 0 → 不是整池模式（整池保存出来就是空配比，无法反推，
      // 所以只做「有分配 ⇒ 一定是按知识点」这一侧的判定，绝不反推整池）
      this.poolOnlyMode = false
      const rows = []
      const seen = {}
      const push = (point, s, m, j) => {
        if (!point || seen[point]) return
        seen[point] = true
        rows.push({
          knowledgePoint: point,
          singleCount: Math.max(0, Math.floor(Number(s) || 0)),
          multiCount: Math.max(0, Math.floor(Number(m) || 0)),
          judgeCount: Math.max(0, Math.floor(Number(j) || 0))
        })
      }
      this.savedRules.forEach(r => {
        // 兼容旧数据：只有 questionCount、没分题型的老行，把总数放进「单选」列，由管理员按题型重新配平
        const typed = (Number(r.singleCount) || 0) + (Number(r.multiCount) || 0) + (Number(r.judgeCount) || 0) > 0
        push(String(r.knowledgePoint || '').trim(),
          typed ? r.singleCount : r.questionCount,
          typed ? r.multiCount : 0,
          typed ? r.judgeCount : 0)
      })
      this.poolPoints.forEach(p => push(p.knowledgePoint, 0, 0, 0))
      this.knowledgeRules = rows
    },
    /** 重新载入题池：保留当前表内已填的分题型数量，按最新题池重建行 */
    reloadPool() {
      const keep = {}
      this.knowledgeRules.forEach(r => {
        if (r.knowledgePoint) {
          keep[r.knowledgePoint] = {
            singleCount: Number(r.singleCount) || 0,
            multiCount: Number(r.multiCount) || 0,
            judgeCount: Number(r.judgeCount) || 0
          }
        }
      })
      this.loadPool().then(() => {
        this.savedRules = Object.keys(keep).map(k => Object.assign({ knowledgePoint: k }, keep[k]))
        this.buildKnowledgeRules()
        this.$modal.msgSuccess('已按最新题池重新载入知识点')
      })
    },
    /** 题池某知识点的可用**总**题量（查不到 = 0） */
    poolAvailable(point) {
      if (!point) return 0
      const p = this.poolPoints.find(x => x.knowledgePoint === point)
      return p ? p.totalCount : 0
    },
    /** 题池某知识点**某题型**的可用量（查不到 = 0） */
    poolAvailableOf(point, type) {
      if (!point) return 0
      const p = this.poolPoints.find(x => x.knowledgePoint === point)
      if (!p) return 0
      const meta = TYPE_META.find(t => t.key === type)
      return meta ? (Number(p[meta.availKey]) || 0) : 0
    },
    /** 题池该知识点的分题型可用量文案（展示用） */
    poolTypeText(point) {
      const p = this.poolPoints.find(x => x.knowledgePoint === point)
      if (!p) return '题池无此知识点'
      return '单' + (Number(p.singleCount) || 0) + ' · 多' + (Number(p.multiCount) || 0) + ' · 判' + (Number(p.judgeCount) || 0)
    },
    /** 行小计 = 单选 + 多选 + 判断 */
    rowTotal(row) {
      if (!row) return 0
      return (Number(row.singleCount) || 0) + (Number(row.multiCount) || 0) + (Number(row.judgeCount) || 0)
    },
    /** 卷面某题型的目标数量 */
    paperCount(type) {
      if (type === 'SINGLE') return Number(this.examForm.singleCount) || 0
      if (type === 'MULTI') return Number(this.examForm.multiCount) || 0
      if (type === 'JUDGE') return Number(this.examForm.judgeCount) || 0
      return 0
    },
    /**
     * 某行某题型输入框的上限 = **max(当前值, min(该知识点该题型的可用量, 卷面该题型数量 − 其它行该题型之和))**。
     *
     * 三件事一次解决（都是实测踩出来的）：
     *   ① 取「该知识点该题型的可用量」→ 到达上限时**加号自动禁用**，填不出抽不满的量；
     *   ② 再取「卷面该题型数量 − 其它行之和」→ **别的行已把该题型配额占满时，这一行的加号直接禁用**，
     *      不会出现「点一下加号 +1、总量已超又要回退」的鬼畜状态（用户实测反馈）；
     *   ③ 最后与**当前值**取 max → 已存的历史值不会被 el-input-number 立刻夹成 0（静默数据损坏），
     *      且当前值已在/超过上限时减号仍可用 → **不会「加了一个就再也减不回 0」**（用户实测反馈）。
     *
     * ⚠️ 上限只能在 :max 里做，**不要在 @change 里事后回退**：那会让 el-input-number 的内部
     *    currentValue 与 v-model 脱节（输入框显示 1、模型却是 0，加减都失灵）。
     */
    typeInputMax(row, type) {
      const meta = TYPE_META.find(t => t.key === type)
      if (!meta) return 0
      const cur = Number(row && row[meta.rowKey]) || 0
      const avail = this.poolAvailableOf(row && row.knowledgePoint, type)
      const others = this.knowledgeRules.reduce((s, r) => s + (r === row ? 0 : (Number(r[meta.rowKey]) || 0)), 0)
      const remaining = Math.max(0, this.paperCount(type) - others)
      return Math.max(cur, Math.min(avail, remaining))
    },
    /**
     * 分题型数量变更：**只做归一化**（把输入框清空产生的 undefined/NaN 落回 0），不做上限回退。
     * 上限由 typeInputMax 在 :max 上拦住，这里回退会让组件内部值与模型脱节。
     */
    onTypeChange(row, type) {
      const meta = TYPE_META.find(t => t.key === type)
      if (!meta) return
      let v = Number(row[meta.rowKey])
      if (!isFinite(v) || v < 0) v = 0
      v = Math.floor(v)
      if (v !== Number(row[meta.rowKey])) {
        this.$set(row, meta.rowKey, v)
      }
    },
    /** 该知识点是否在本部门题池里（不在 → 显示提示标） */
    isKnownPoint(point) {
      if (!point) return true
      return this.poolPoints.some(x => x.knowledgePoint === point)
    },
    /** 删除一行配比 */
    removeRule(index) {
      this.knowledgeRules.splice(index, 1)
    },
    /**
     * 切换「整池抽题」模式。
     * 进入时清掉所有知识点分配（并把它们留在表里灰显，便于随时切回来）。
     */
    toggleWholePool() {
      if (!this.poolOnlyMode) {
        if (!this.questionTotal) {
          this.$modal.msgWarning('请先在上方「卷面结构」里填单选题 / 多选题 / 判断题的数量')
          return
        }
        this.knowledgeRules.forEach(r => { r.singleCount = 0; r.multiCount = 0; r.judgeCount = 0 })
        this.poolOnlyMode = true
        this.$modal.msgSuccess('已切换到整池抽题：按题型数量从本部门整个理论题池随机抽，不再按知识点分配')
      } else {
        this.poolOnlyMode = false
        this.$modal.msgSuccess('已恢复按知识点配比抽题')
      }
    },
    /** 提交用的知识点配比（分题型）；整池抽题模式下提交空（后端按题型从全池抽） */
    knowledgePayload() {
      if (this.poolOnlyMode) return []
      return this.knowledgeRules
        .filter(r => String(r.knowledgePoint || '').trim())
        .map((r, i) => {
          const s = Number(r.singleCount) || 0
          const m = Number(r.multiCount) || 0
          const j = Number(r.judgeCount) || 0
          return {
            knowledgePoint: String(r.knowledgePoint).trim(),
            singleCount: s,
            multiCount: m,
            judgeCount: j,
            questionCount: s + m + j,
            sortNo: i + 1
          }
        })
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
      // ★ 2026-09-24：把卷面题型数量一并传过去 —— 后端试抽现在会按「知识点配比 + 题型配额」抽，
      //   不传的话预览就退化成「不分题型」，与真实卷不一致。
      tryDraw({
        deptId: this.exam && this.exam.deptId,
        singleCount: Number(this.examForm.singleCount) || 0,
        multiCount: Number(this.examForm.multiCount) || 0,
        judgeCount: Number(this.examForm.judgeCount) || 0,
        knowledgeRules: this.knowledgePayload()
      }).then(res => {
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

/* 知识点配比表：题池可用（总数 + 分题型）/ 已分配 徽标 */
.avail-cell { display: flex; flex-direction: column; line-height: 1.35; }
.avail-cell b { color: #1d2939; font-size: 13px; }
.avail-cell small { margin-left: 2px; color: #98a2b3; font-size: 11px; }
.avail-sub { margin-top: 1px; color: #98a2b3; font-size: 11px; }
.alloc-badge { display: inline-block; padding: 1px 6px; color: #98a2b3; background: #f2f4f7; font-size: 11px; border-radius: 3px; }
.alloc-badge.on { color: #1764f5; background: #edf4ff; font-weight: 600; }
/* 题池里已不存在的知识点：给个醒目标（这类行填了也抽不出题） */
.kp-warn { margin-left: 6px; padding: 0 5px; color: #d9534f; background: #fdeeed; font-size: 11px; border-radius: 3px; }
/* 整池抽题模式下，知识点表只作参考，整体降透明度 */
.pool-off ::v-deep .el-table__body-wrapper { opacity: .45; }
/* 分题型「已配 / 卷面」小结条 */
.type-sum { display: flex; flex-wrap: wrap; gap: 16px; padding: 9px 13px; margin-top: 10px; color: #475467; background: #f8fafc; border: 1px solid #eef1f6; border-radius: 6px; font-size: 12.5px; }
.type-sum b { color: #1d2939; font-size: 14px; }
.type-sum .bad { color: #d9534f; }
.type-sum .bad b { color: #d9534f; }
</style>
