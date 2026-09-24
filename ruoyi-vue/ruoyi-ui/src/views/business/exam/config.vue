<template>
  <div class="exam-config-page app-container">
    <!-- 顶部：返回 + 名称/标签 + 关键指标（只做展示，操作按钮在下方卡片里，避免重复） -->
    <header class="cfg-head">
      <div class="cfg-head-left">
        <el-button size="mini" icon="el-icon-back" @click="goBack">返回列表</el-button>
        <div class="cfg-title">
          <div class="eyebrow">{{ isSuperAdmin ? '学习考核 / 考核管理' : '学习与考核管理' }} / 考核配置</div>
          <div class="title-line">
            <h2>{{ exam ? exam.examName : (loadError ? '加载失败' : '加载中…') }}</h2>
            <template v-if="exam">
              <el-tag size="mini" effect="plain" :type="isPractice ? 'success' : 'primary'">{{ isPractice ? '实操考核' : '理论考核' }}</el-tag>
              <el-tag size="mini" :type="statusTag">{{ statusLabel }}</el-tag>
              <span v-if="isSuperAdmin" class="muted">{{ exam.deptName || '未设置部门' }}</span>
            </template>
          </div>
        </div>
      </div>

      <div v-if="exam" class="cfg-kpis">
        <div class="cfg-kpi" :class="{ danger: scoreLevel === 'danger' }">
          <span>卷面满分</span><b>{{ fullScore > 0 ? fullScore + ' 分' : '未配置' }}</b>
        </div>
        <div class="cfg-kpi" :class="{ danger: scoreLevel === 'danger' }">
          <span>通过线</span><b>{{ exam.passLine }} 分</b>
        </div>
        <div class="cfg-kpi"><span>题目量</span><b>{{ countText }}</b></div>
        <div class="cfg-kpi"><span>考核时长</span><b>{{ exam.duration > 0 ? exam.duration + ' 分钟' : '不限时' }}</b></div>
      </div>
    </header>

    <!-- ★「按当前配置能不能发布」的结论放在最上面一眼可见 -->
    <el-alert
      v-if="exam && scoreLevel !== 'ok'"
      :closable="false"
      :type="scoreLevel === 'danger' ? 'error' : 'warning'"
      show-icon
      :title="scoreAlertTitle"
      :description="scoreAlertDesc"
      class="cfg-alert"
    />

    <div v-if="loadError" class="cfg-empty">
      <i class="el-icon-warning-outline" />
      <p>{{ loadError }}</p>
      <el-button size="small" @click="goBack">返回列表</el-button>
    </div>

    <!-- 配置主体：组卷/题目清单 + 校验与试抽 + 发布设置（复用既有组件，逻辑零改动） -->
    <template v-else-if="exam">
      <!-- ② 组卷 / 题目清单 + 校验与试抽 + 发布设置 -->
      <exam-config-card
        :key="exam.id"
        :exam="exam"
        :is-super-admin="isSuperAdmin"
        :dept-id="exam.deptId"
        @edit="handleEdit"
        @delete="handleDelete"
        @grade="goGrading"
        @status-change="handleChangeStatus"
        @refresh="loadDetail"
      />
    </template>

    <div v-else class="cfg-empty">
      <i class="el-icon-loading" />
      <p>正在加载考核配置…</p>
    </div>
  </div>
</template>

<script>
import { getExam, delExam, changeExamStatus } from '@/api/business/exam'
import { mapGetters } from 'vuex'
import ExamConfigCard from './components/ExamConfigCard'
import { isSuperAdminRole } from '@/utils/permission'

export default {
  name: 'ExamConfig',
  components: { ExamConfigCard },
  data() {
    return {
      exam: null,
      loadError: '',
    }
  },
  computed: {
    ...mapGetters(['roles']),
    isSuperAdmin() { return isSuperAdminRole(this.roles) },
    isPractice() { return this.exam && this.exam.examType === 'PRACTICAL' },
    statusLabel() {
      return { DRAFT: '待发布', PUBLISHED: '已发布', GRADING: '待批改', DISABLED: '已停用' }[this.exam && this.exam.status] || '—'
    },
    statusTag() {
      return { DRAFT: 'info', PUBLISHED: 'success', GRADING: 'warning', DISABLED: 'danger' }[this.exam && this.exam.status] || 'info'
    },
    /** 返回到进入本页的那个列表：优先用 ?from=（部门端 /department/study/exam、超管端 /assessment/department/exam 都能回对） */
    listPath() {
      return this.$route.query.from || '/department/study/exam'
    },
    /** 卷面满分：实操=各题满分之和；理论=单/多/判 分值×题量之和 */
    fullScore() {
      const e = this.exam
      if (!e) return 0
      const n = v => Number(v) || 0
      if (this.isPractice) return Math.round(n(e.subjectTotalScore) * 10) / 10
      return Math.round((n(e.singleCount) * n(e.singleScore) + n(e.multiCount) * n(e.multiScore) + n(e.judgeCount) * n(e.judgeScore)) * 10) / 10
    },
    countText() {
      const e = this.exam
      if (!e) return '—'
      return this.isPractice ? (e.subjectCount || 0) + ' 道' : (e.questionCount || 0) + ' 题'
    },
    /** ok / warn / danger —— 与列表页 scoreCheck 同一口径 */
    scoreLevel() {
      const e = this.exam
      if (!e) return 'ok'
      const pass = Number(e.passLine) || 0
      if (!this.fullScore) return 'danger'
      if (pass > this.fullScore) return 'danger'
      if (pass === this.fullScore) return 'warn'
      return 'ok'
    },
    scoreAlertTitle() {
      return this.scoreLevel === 'danger' ? '当前配置无法发布' : '通过线等于卷面满分'
    },
    scoreAlertDesc() {
      const e = this.exam
      const pass = Number(e && e.passLine) || 0
      if (!this.fullScore) {
        return this.isPractice
          ? '尚无实操题目：请在下方「实操题目」区逐题添加题干与满分，否则发布时会被后端拦截。'
          : '尚未组卷：请在下方「组卷题库与分值」区添加题库并设置抽题量，否则发布时会被后端拦截。'
      }
      if (pass > this.fullScore) {
        return '通过线 ' + pass + ' 分高于卷面满分 ' + this.fullScore + ' 分，发布时会被后端拦截。请降低通过线，或提高题目分值与抽题量。'
      }
      return '通过线 ' + pass + ' 分等于卷面满分 ' + this.fullScore + ' 分，实习生必须全部答对才及格，请确认是否符合预期。'
    }
  },
  created() {
    this.loadDetail()
  },
  watch: {
    // 同一路由换 examId（例如从别的入口跳过来）时重新加载
    '$route.params.examId'() { this.loadDetail() }
  },
  methods: {
    loadDetail() {
      const id = this.$route.params.examId
      if (!id) {
        this.loadError = '缺少考核 id，无法打开配置页。'
        return
      }
      this.loadError = ''
      // ⚠️ 不要在这里把 exam 置空：v-else-if="exam" 会因此把配置卡「卸载重建」，
      //    用户在卡里尚未保存的改动（如组卷抽题量）会被一起冲掉。刷新只做「就地替换」。
      getExam(id).then(res => {
        const d = res && res.data
        if (!d) {
          this.loadError = '未找到该考核（可能已被删除）。'
          return
        }
        this.exam = d
      }).catch(() => {
        this.loadError = '加载考核配置失败：考核可能已被删除，或当前账号无权限查看。'
      })
    },
    goBack() {
      this.$router.push(this.listPath).catch(() => {})
    },
    handleEdit(exam) {
      // 基本信息编辑弹窗在列表页（同一套表单），带 ?edit= 回去自动打开
      this.$router.push({ path: this.listPath, query: { edit: exam.id } }).catch(() => {})
    },
    goGrading(exam) {
      this.$router.push('/assessment/department/exam/grading/' + exam.id).catch(() => {})
    },
    handleDelete(exam) {
      const tip = exam.examType === 'PRACTICAL'
        ? `确认删除实操考核「${exam.examName}」吗？该考核的题目清单将一并删除。`
        : `确认删除理论考核「${exam.examName}」吗？`
      this.$modal.confirm(tip).then(() => {
        delExam(exam.id).then(() => {
          this.$modal.msgSuccess('删除成功')
          this.goBack()
        })
      }).catch(() => {})
    },
    handleChangeStatus(exam, status) {
      const label = status === 'PUBLISHED' ? '启用' : '停用'
      this.$modal.confirm(`确认${label}「${exam.examName}」吗？`).then(() => {
        changeExamStatus(exam.id, status).then(() => {
          this.$modal.msgSuccess(`${label}成功`)
          this.loadDetail()
        })
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.cfg-head {
  display: flex; align-items: flex-start; justify-content: space-between; gap: 18px;
  flex-wrap: wrap; margin-bottom: 14px; padding: 16px 18px; background: #fff;
  border: 1px solid #e7ecf3; border-radius: 8px;
}
.cfg-head-left { display: flex; align-items: flex-start; gap: 14px; min-width: 0; }
.cfg-title { min-width: 0; }
.eyebrow { color: #1764f5; font-size: 12px; letter-spacing: .05em; }
.title-line { display: flex; align-items: center; gap: 9px; flex-wrap: wrap; }
.title-line h2 { margin: 6px 0 0; color: #1d2939; font-size: 20px; font-weight: 600; }
.muted { color: #98a2b3; font-size: 12px; }

.cfg-kpis { display: flex; align-items: stretch; gap: 10px; flex-wrap: wrap; }
.cfg-kpi {
  min-width: 108px; padding: 8px 12px; text-align: right;
  background: #f8fafc; border: 1px solid #eef1f6; border-radius: 6px;
}
.cfg-kpi span { display: block; color: #8490a0; font-size: 11.5px; }
.cfg-kpi b { color: #1d2939; font-size: 15px; font-weight: 600; }
.cfg-kpi.danger { background: #fdeeed; border-color: #f7d4d1; }
.cfg-kpi.danger b { color: #d9534f; }

.cfg-alert { margin-bottom: 14px; }


.cfg-empty {
  padding: 46px 18px; text-align: center; background: #fff;
  border: 1px dashed #dfe5ee; border-radius: 8px;
}
.cfg-empty i { color: #d0d5dd; font-size: 32px; }
.cfg-empty p { margin: 12px 0 14px; color: #8490a0; font-size: 13px; }

/* 配置主体在独立页里给足宽度：概览列收窄、组卷区变宽（窄屏自动堆叠由组件内 media query 负责） */
.exam-config-page ::v-deep .dm-card { padding: 20px 22px 22px; }
.exam-config-page ::v-deep .dm-fields { gap: 10px; }

@media (max-width: 1280px) {
  .cfg-head { flex-direction: column; }
  .cfg-kpi { flex: 1; min-width: 0; text-align: left; }
}
</style>
