<template>
  <el-dialog
    :visible="visible"
    title="从实操题库选题"
    width="920px"
    top="6vh"
    append-to-body
    :close-on-click-modal="false"
    @update:visible="$emit('update:visible', $event)"
  >
    <p class="psp-tip">
      选中题目后会<b>复制成这场考核的题目清单</b>（题名 / 题干 / 考核要点 / 交付要求 / 参考图与附件一起带过去），
      导入后仍可逐题改满分、单独补附件。题库里的题目本身不会被改动。
    </p>

    <div class="psp-body">
      <!-- 左：题库 -->
      <aside class="psp-side" v-loading="bankLoading">
        <div class="psp-side-head">实操题库<span>{{ banks.length }} 个</span></div>
        <div
          v-for="b in banks"
          :key="b.bankId"
          class="psp-bank"
          :class="{ on: current && current.bankId === b.bankId }"
          @click="pickBank(b)"
        >
          <strong>{{ b.bankName }}</strong>
          <small>{{ typeText(b.bankType) }} · 可用 {{ b.totalCount || 0 }} 题</small>
        </div>
        <div v-if="!bankLoading && !banks.length" class="psp-empty small">
          <i class="el-icon-folder-opened" />
          <p>本部门没有实操题库。可先到「题库管理 → 实操题库」新建并录入题目。</p>
        </div>
      </aside>

      <!-- 右：题目 -->
      <section class="psp-main" v-loading="subjectLoading">
        <div class="psp-main-head">
          <el-input
            v-model="keyword"
            size="mini"
            clearable
            prefix-icon="el-icon-search"
            placeholder="搜题名 / 题干 / 考核要点"
            class="psp-keyword"
          />
          <span class="psp-count">显示 {{ filtered.length }} / {{ subjects.length }} 道</span>
        </div>

        <div v-if="!current" class="psp-empty">
          <i class="el-icon-hand-up" />
          <p>左侧选择一个实操题库，这里会列出它包含的题目。</p>
        </div>
        <div v-else-if="!subjects.length" class="psp-empty">
          <i class="el-icon-tickets" />
          <p>这个题库还没有录入题目。</p>
        </div>
        <div v-else-if="!filtered.length" class="psp-empty">
          <i class="el-icon-search" />
          <p>当前关键词下没有匹配的题目。</p>
        </div>
        <div v-else class="psp-list">
          <label
            v-for="s in filtered"
            :key="s.id"
            class="psp-row"
            :class="{ disabled: isAdded(s), on: !!picked[s.id] }"
          >
            <el-checkbox
              :value="!!picked[s.id]"
              :disabled="isAdded(s)"
              @change="toggle(s, $event)"
            />
            <span class="psp-row-body">
              <span class="psp-row-title">
                {{ s.title }}
                <el-tag v-if="isAdded(s)" size="mini" effect="plain" type="info">已添加</el-tag>
              </span>
              <span class="psp-row-meta">
                {{ s.direction || '未分方向' }} · {{ diffText(s.difficulty) }}
                <template v-if="s.estimatedMinutes"> · 建议 {{ s.estimatedMinutes }} 分钟</template>
                <template v-if="s.suggestScore != null"> · 建议满分 {{ s.suggestScore }} 分</template>
              </span>
            </span>
            <el-input-number
              v-model="scoreOf[s.id]"
              size="mini"
              :min="0"
              :max="1000"
              :precision="1"
              controls-position="right"
              class="psp-score"
              :disabled="isAdded(s)"
            />
          </label>
        </div>
      </section>
    </div>

    <span slot="footer">
      <span class="psp-foot">已选 <b>{{ pickedCount }}</b> 道</span>
      <span class="psp-bulk">
        选中项统一满分
        <el-input-number v-model="bulkScore" size="mini" :min="0" :max="1000" :precision="1" controls-position="right" style="width:94px" />
        <el-button size="mini" @click="applyBulkScore">应用</el-button>
      </span>
      <el-button size="small" @click="$emit('update:visible', false)">取消</el-button>
      <el-button size="small" type="primary" :disabled="!pickedCount" @click="confirm">导入所选题目</el-button>
    </span>
  </el-dialog>
</template>

<script>
import { listExamBankOptions } from '@/api/business/exam'
import { listPracticeSubject } from '@/api/business/practiceSubject'

function parseJsonList(v) {
  if (!v) return []
  try {
    const a = typeof v === 'string' ? JSON.parse(v) : v
    return Array.isArray(a) ? a : []
  } catch (e) { return [] }
}

export default {
  name: 'PracticeSubjectPicker',
  props: {
    visible: { type: Boolean, default: false },
    /** 超管按所选部门取库；部门管理员传 undefined（后端按登录人部门） */
    deptId: { type: Number, default: null },
    isSuperAdmin: { type: Boolean, default: false },
    /** 已有题目名（用于「已添加」判定与去重） */
    existingTitles: { type: Array, default: () => [] }
  },
  data() {
    return {
      bankLoading: false,
      banks: [],
      current: null,
      subjectLoading: false,
      subjects: [],
      keyword: '',
      picked: {},
      scoreOf: {},
      /** 「选中项统一满分」的输入值（实操题通常等分，避免逐题填） */
      bulkScore: 10
    }
  },
  computed: {
    filtered() {
      const kw = String(this.keyword || '').trim().toLowerCase()
      if (!kw) return this.subjects
      return this.subjects.filter(s => {
        const hay = [s.title, s.content, s.devConstraints, s.deliverables, s.direction]
          .map(v => String(v || '')).join('\n').toLowerCase()
        return hay.indexOf(kw) > -1
      })
    },
    pickedCount() { return Object.keys(this.picked).filter(k => this.picked[k]).length }
  },
  watch: {
    visible(v) { if (v) this.open() }
  },
  methods: {
    open() {
      this.keyword = ''
      this.picked = {}
      this.scoreOf = {}
      this.bankLoading = true
      // ⚠️ 不传 examMode：实操题库的用途可能是「模拟用(PRACTICE)」也可能是「通用(COMMON)」，
      //    传 FORMAL 会被后端按用途过滤成 0 个库（实测：105 的实操库用途是 PRACTICE）。
      listExamBankOptions(this.isSuperAdmin ? this.deptId : undefined, undefined, 'PRACTICAL').then(res => {
        this.banks = (res.data || []).map(b => Object.assign({}, b, {
          totalCount: Number(b.totalCount) || 0
        }))
        this.bankLoading = false
        if (this.banks.length) this.pickBank(this.banks[0])
      }).catch(() => {
        this.bankLoading = false
        this.banks = []
      })
    },
    pickBank(b) {
      this.current = b
      this.subjectLoading = true
      this.subjects = []
      this.keyword = ''
      listPracticeSubject({ bankId: b.bankId, pageNum: 1, pageSize: 200 }).then(res => {
        this.subjects = (res && res.rows) || []
        this.subjects.forEach(s => {
          if (this.scoreOf[s.id] === undefined) {
            this.$set(this.scoreOf, s.id, Number(s.suggestScore) || 0)
          }
        })
        this.subjectLoading = false
      }).catch(() => {
        this.subjectLoading = false
        this.subjects = []
      })
    },
    isAdded(s) {
      return (this.existingTitles || []).indexOf(String(s.title || '').trim()) > -1
    },
    toggle(s, checked) {
      this.$set(this.picked, s.id, !!checked)
    },
    /** 把已勾选题目的满分统一改为输入值（题库的「建议满分」常为 0，等分场景多） */
    applyBulkScore() {
      const v = Number(this.bulkScore)
      if (isNaN(v) || v < 0) { this.$modal.msgWarning('请输入不小于 0 的分值'); return }
      const ids = Object.keys(this.picked).filter(k => this.picked[k])
      if (!ids.length) { this.$modal.msgWarning('请先勾选要导入的题目'); return }
      ids.forEach(id => { this.$set(this.scoreOf, id, v) })
      this.$modal.msgSuccess('已把 ' + ids.length + ' 道题的满分设为 ' + v + ' 分')
    },
    confirm() {
      const rows = this.subjects.filter(s => this.picked[s.id])
      if (!rows.length) return
      // 交给父组件做「题库题 → 考核题目清单」的映射（父组件持有 subjectItems 结构）
      this.$emit('confirm', rows.map(s => ({
        title: s.title,
        content: s.content,
        devConstraints: s.devConstraints,
        deliverables: s.deliverables,
        submitFormat: s.submitFormat,
        namingRule: s.namingRule,
        score: Number(this.scoreOf[s.id]) || 0,
        images: parseJsonList(s.referenceImages),
        attachments: parseJsonList(s.attachmentsJson)
      })))
      this.$emit('update:visible', false)
    },
    typeText(t) { return { PRACTICE: '模拟题库', COMMON: '通用题库', FORMAL: '正式题库' }[String(t || '').toUpperCase()] || '通用题库' },
    diffText(d) { return { EASY: '简单', MEDIUM: '中等', HARD: '困难' }[String(d || '').toUpperCase()] || '中等' }
  }
}
</script>

<style lang="scss" scoped>
.psp-tip { margin: 0 0 12px; color: #667085; font-size: 12.5px; line-height: 1.8; }
.psp-body { display: grid; grid-template-columns: 234px minmax(0, 1fr); gap: 12px; align-items: start; }
.psp-side { padding: 8px; border: 1px solid #e7ecf3; border-radius: 8px; }
.psp-side-head { display: flex; align-items: center; justify-content: space-between; padding: 3px 5px 9px; color: #344054; font-size: 12.5px; font-weight: 600; }
.psp-side-head span { color: #98a2b3; font-size: 11.5px; font-weight: 400; }
.psp-bank { padding: 9px 10px; margin-bottom: 6px; border: 1px solid #eef1f6; border-radius: 6px; cursor: pointer; transition: all .15s; }
.psp-bank:hover { border-color: #b9d2ff; }
.psp-bank.on { border-color: #1764f5; background: #f6faff; }
.psp-bank strong { display: block; color: #1d2939; font-size: 12.5px; }
.psp-bank small { display: block; margin-top: 3px; color: #98a2b3; font-size: 11.5px; }

.psp-main { min-height: 300px; border: 1px solid #e7ecf3; border-radius: 8px; }
.psp-main-head { display: flex; align-items: center; gap: 10px; padding: 9px 12px; border-bottom: 1px solid #edf0f4; }
.psp-keyword { width: 260px; }
.psp-count { margin-left: auto; color: #667085; font-size: 11.5px; }
.psp-list { max-height: 380px; overflow-y: auto; padding: 4px 0; }
.psp-row { display: flex; align-items: center; gap: 11px; padding: 9px 12px; cursor: pointer; border-bottom: 1px solid #f6f8fb; }
.psp-row:hover { background: #fafcff; }
.psp-row.on { background: #f6faff; }
.psp-row.disabled { cursor: not-allowed; opacity: .62; }
.psp-row-body { flex: 1; min-width: 0; }
.psp-row-title { display: block; color: #1d2939; font-size: 12.5px; }
.psp-row-title .el-tag { margin-left: 6px; }
.psp-row-meta { display: block; margin-top: 3px; color: #98a2b3; font-size: 11.5px; }
.psp-score { width: 96px; flex: none; }
.psp-foot { float: left; color: #667085; font-size: 12.5px; line-height: 32px; }
.psp-foot b { color: #1764f5; }
.psp-bulk { margin-right: 12px; color: #667085; font-size: 12.5px; }
.psp-bulk .el-input-number { margin: 0 6px; vertical-align: middle; }

.psp-empty { padding: 46px 14px; text-align: center; }
.psp-empty.small { padding: 22px 8px; }
.psp-empty i { color: #d0d5dd; font-size: 28px; }
.psp-empty p { margin: 10px 0 0; color: #8490a0; font-size: 12.5px; line-height: 1.7; }

@media (max-width: 900px) { .psp-body { grid-template-columns: minmax(0, 1fr); } }
</style>
