<template>
  <div class="dept-page">
    <div class="dept-breadcrumb">
      学习与考核管理 <span>/</span> <b>成绩管理</b>
    </div>

    <div class="dept-heading">
      <div>
        <span class="eyebrow">DEPARTMENT ADMIN</span>
        <h1>成绩管理</h1>
        <p>理论部分交卷即自动出分，管理员只需查看与导出；实操部分下载实习生答案文件包后人工录分。</p>
      </div>
      <div class="dept-heading-actions">
        <el-button size="small" @click="exportExcel">导出 Excel</el-button>
      </div>
    </div>

    <!-- 页签条 -->
    <div class="dtabs page">
      <div class="dtab" :class="{ on: activeTab === 'grading' }" @click="activeTab = 'grading'">
        实操评分 <span class="tab-n">{{ pendingRows.length }}</span>
      </div>
      <div class="dtab" :class="{ on: activeTab === 'summary' }" @click="activeTab = 'summary'">成绩汇总</div>
    </div>

    <!-- 批次 / 考核选择 -->
    <div class="dfilter">
      <el-select v-model="examId" size="small" placeholder="选择考核" style="width:280px" @change="onExamChange">
        <el-option
          v-for="e in exams"
          :key="e.id"
          :label="e.examName + '（' + modeText(e) + '）'"
          :value="e.id"
        />
      </el-select>
      <div v-if="activeTab === 'grading'" class="dchips" style="margin:0">
        <span
          v-for="tab in gradingTabs"
          :key="tab.key"
          class="dchip"
          :class="{ on: gradingFilter === tab.key }"
          @click="gradingFilter = tab.key"
        >
          {{ tab.label }} <span class="n">{{ tab.count }}</span>
        </span>
      </div>
      <span class="grow" />
      <el-button v-if="activeTab === 'grading'" size="small" :disabled="!examId" @click="publishAll">发布成绩</el-button>
      <el-button size="small" @click="loadAll">刷新</el-button>
    </div>

    <!-- ============ 页签 1 · 实操评分 ============ -->
    <div v-if="activeTab === 'grading' && examId" class="dgrid">
      <div class="dcard c4">
        <div class="dcard-h">
          <div class="tt"><h3 style="font-size:14px">待评分队列</h3></div>
          <span class="hint-text">按提交时间</span>
        </div>
        <div v-if="queueRows.length" class="dtodo">
          <div
            v-for="row in queueRows"
            :key="row.sheetId"
            class="dtodo-row clickable"
            :class="{ sel: currentSheetId === row.sheetId }"
            @click="openSheet(row)"
          >
            <span class="dtodo-dot" :class="row.sheetStatus === 'PUBLISHED' ? 'g' : 'o'" />
            <div class="dtodo-main">
              <div class="dtodo-t">{{ row.userName }} · {{ row.deptName || '' }}</div>
              <div class="dtodo-s">
                答案包{{ row.subjectAnswer ? '已提交' : '未提交' }} ·
                {{ fmtTime(row.submitTime) }} · {{ sheetStatusText(row) }}
              </div>
            </div>
          </div>
        </div>
        <div v-else class="dempty small">
          <i class="el-icon-document-checked" />
          <strong>没有待评分的答卷</strong>
          <span>切换筛选条件或选择其他考核批次。</span>
        </div>
        <p class="dsec-note">点任意一条，右侧加载评分面板；评完自动跳下一份。</p>
      </div>

      <div class="dcard c8">
        <template v-if="sheet">
          <div class="dcard-h">
            <div class="tt"><h3 style="font-size:14px">评分面板 · {{ sheet.userName }}</h3></div>
            <span class="dbadge green">理论已自动判分 {{ fmtScore(sheet.aiScore) }}</span>
          </div>

          <div class="dcallout ok">
            <i class="el-icon-folder-opened" />
            <span>
              <b>考生提交的答案文件包</b><br />
              <span v-if="sheet.subjectAnswer">
                <code>{{ sheet.subjectAnswer }}</code>
                <span class="dbtn-row" style="margin-top:9px">
                  <el-button size="mini" type="primary" @click="downloadAnswer">下载压缩包</el-button>
                  <el-button size="mini" @click="previewAnswer">在线预览</el-button>
                </span>
              </span>
              <span v-else>该答卷未上传答案文件包（可能为纯理论卷）。</span>
            </span>
          </div>

          <!-- 逐题评分（仅主观题需要人工分） -->
          <div v-if="hasSubjective" class="dgrid" style="gap:12px">
            <div v-for="item in subjectiveItems" :key="item.id" class="c6">
              <div class="dsec" style="margin:0;padding:14px 16px">
                <div class="dcard-h" style="margin-bottom:10px">
                  <div class="tt"><span class="idx">{{ item.seq }}</span><h3 style="font-size:13px">第 {{ item.seq }} 题 · {{ item.qtype || '主观题' }}</h3></div>
                  <span class="hint-text">满分 {{ fmtScore(item.score) }}</span>
                </div>
                <p class="q-stem">{{ item.stem }}</p>
                <div class="dfield">
                  <label>得分（满分 {{ fmtScore(item.score) }}）</label>
                  <el-input-number
                    v-model="item.manualScore"
                    size="small"
                    :min="0"
                    :max="Number(item.score) || 100"
                    :precision="1"
                    controls-position="right"
                    style="width:100%"
                  />
                </div>
                <div class="dfield">
                  <label>评语（实习生端可见）</label>
                  <el-input v-model="item.manualComment" type="textarea" :rows="2" size="small" placeholder="指出亮点与改进点" />
                </div>
              </div>
            </div>
          </div>

          <div v-else class="dcallout ok" style="margin-bottom:0">
            <i class="el-icon-success" />
            <span>
              本场答卷<b>全部为客观题</b>，已由 <code>AnswerSheetServiceImpl</code> 的自动判分分支
              写入 <code>is_correct / final_score / pass_flag</code>，<b>无需人工录分</b>。
              如需评语，可在下方「总评语」补充后直接发布成绩。
            </span>
          </div>

          <div class="dkv" style="grid-template-columns:repeat(4,1fr);margin-top:16px">
            <div><span>实操总分</span><strong>{{ practiceTotal }} <small>/ 100</small></strong></div>
            <div><span>通过线</span><strong>{{ passLine }} <small>分</small></strong></div>
            <div><span>理论（自动）</span><strong>{{ fmtScore(sheet.aiScore) }} <small>/ 100</small></strong></div>
            <div><span>综合分</span><strong>{{ overall }}</strong></div>
          </div>

          <div class="dfield" style="margin-top:14px">
            <label>总评语（实习生端可见）</label>
            <el-input v-model="sheet.manualComment" type="textarea" :rows="3" size="small" placeholder="例如：分页实现正确、分层清晰。不足：未处理前置校验…" />
          </div>

          <div class="dbtn-row right" style="margin-top:14px">
            <el-button size="small" @click="draft">存草稿</el-button>
            <el-button size="small" type="primary" :disabled="!hasSubjective" @click="submitAndNext">
              {{ hasSubjective ? '提交并评下一份' : '本场无需人工评分' }}
            </el-button>
          </div>
        </template>
        <div v-else class="dempty">
          <i class="el-icon-mouse" />
          <strong>请在左侧选择一份答卷</strong>
          <span>选中后此处展示答案文件包、逐题评分与总评语。</span>
        </div>
      </div>
    </div>

    <!-- ============ 页签 2 · 成绩汇总 ============ -->
    <div v-if="activeTab === 'summary'" class="dgrid">
      <div class="dcard c8">
        <div class="dcard-h">
          <div class="tt"><span class="idx">汇</span><h3>成绩汇总</h3></div>
          <span class="hint-text">理论「自动」· 实操「人工」· 分项独立出分</span>
        </div>
        <table class="dtbl">
          <thead>
            <tr>
              <th>姓名</th>
              <th>部门</th>
              <th>理论 / 自动分</th>
              <th>实操 / 人工分</th>
              <th>最终分</th>
              <th>结论</th>
              <th style="width:90px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in allRows" :key="row.userId">
              <td><span class="strong">{{ row.userName }}</span></td>
              <td>{{ row.deptName || '—' }}</td>
              <td>
                <template v-if="row.aiScore != null">{{ fmtScore(row.aiScore) }} <span class="dbadge blue">自动</span></template>
                <template v-else>— <span class="dbadge">未出分</span></template>
              </td>
              <td>
                <template v-if="row.manualScore != null">{{ fmtScore(row.manualScore) }} <span class="dbadge purple">人工</span></template>
                <template v-else>— <span class="dbadge">待录分</span></template>
              </td>
              <td><span class="strong">{{ row.finalScore != null ? fmtScore(row.finalScore) : '待定' }}</span></td>
              <td>
                <span class="dbadge" :class="conclusionTone(row)">{{ conclusionText(row) }}</span>
              </td>
              <td><el-button type="text" @click="openProfile(row)">档案</el-button></td>
            </tr>
            <tr v-if="!allRows.length">
              <td colspan="7">
                <div class="dempty small">
                  <i class="el-icon-document" />
                  <strong>该考核暂无答卷</strong>
                  <span>实习生交卷后这里会出现对应行。</span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <p class="dsec-note">
          综合分规则（理论 40% + 实操 60%）依赖 <code>exam_rule_snapshot</code>，该表尚未接通，故此处直接展示答卷自身的
          <code>final_score</code>。<b>任一分或未出分一律显示「待定」，不给「通过」</b> —— 因为部门直接终审没有二次复核。
        </p>
      </div>

      <div class="dcard c4">
        <div class="dcard-h">
          <div class="tt"><span class="idx o">弱</span><h3>本批次薄弱知识点</h3></div>
          <span class="hint-text">
            示例数据
            <span class="dsample">示例</span>
          </span>
        </div>
        <div v-for="row in weakPoints" :key="row.name" class="dhbar">
          <div class="dhbar-head">
            <span class="dhbar-name">{{ row.name }}</span>
            <span class="dhbar-meta"><b :class="row.tone">{{ row.rate }}%</b></span>
          </div>
          <div class="dhbar-track">
            <div class="dhbar-fill" :class="row.barTone" :style="{ width: row.rate + '%' }" />
          </div>
        </div>
        <p class="dsec-note">
          <b>现在就能做</b>：<code>question.knowledge_point</code> 在题库里有完整真实值，
          且 <code>/business/question/preview</code> 只清空 answer / analysis、<b>保留 knowledge_point</b>
          → 按知识点聚合正确率<b>纯前端即可算</b>，不必等后端。
        </p>
      </div>
    </div>

    <!-- 未选考核 -->
    <div v-if="!examId && !loading" class="dsec">
      <div class="dempty">
        <i class="el-icon-warning-outline" />
        <strong>本部门暂无考核批次</strong>
        <span>请先在「正式考核管理」创建并发布一场考核，实习生交卷后即可在此评分。</span>
        <el-button size="small" type="primary" @click="$router.push('/department/study/exam')">去正式考核管理</el-button>
      </div>
    </div>

    <!-- 判分能力说明 -->
    <div class="dcallout ok" style="margin-top:16px">
      <i class="el-icon-success" />
      <span>
        <b>两处判分都已实现，本页不用重造：</b><br />
        <b>理论</b> —— <code>AnswerSheetServiceImpl</code> 的 THEORY 分支用 <code>normalizeAnswer()</code>
        比对 <code>question.answer</code>，直接写 <code>is_correct / final_score / pass_flag</code> 并置 PUBLISHED，<b>零人工</b>。<br />
        <b>实操</b> —— <code>gradePractically(sheetId, manualScore, manualComment)</code> 已存在，写入
        <code>manual_score / manual_comment</code>；提交时 <code>subject_answer</code> 已存文件路径。
      </span>
    </div>
  </div>
</template>

<script>
import { listExam, gradingList, sheetDetail, gradeSheet, publishResult } from '@/api/business/exam'

export default {
  name: 'DeptScores',
  data() {
    return {
      loading: false,
      activeTab: 'grading',
      gradingFilter: 'PENDING',
      examId: null,
      exams: [],
      rows: [],
      sheet: null,
      sheetItems: [],
      currentSheetId: null,
      weakPoints: [
        { name: 'Docker', rate: 38, tone: 'poor', barTone: 'poor' },
        { name: '异常处理', rate: 45, tone: 'poor', barTone: 'poor' },
        { name: 'MySQL 索引', rate: 62, tone: 'mid', barTone: 'mid' },
        { name: '若依框架', rate: 81, tone: 'good', barTone: 'good' }
      ]
    }
  },
  computed: {
    currentExam() {
      return this.exams.find(e => e.id === this.examId) || null
    },
    passLine() {
      return this.currentExam && this.currentExam.passLine != null ? this.currentExam.passLine : 60
    },
    /** 有答卷的行 */
    allRows() {
      return this.rows.filter(r => r.sheetId)
    },
    pendingRows() {
      return this.allRows.filter(r => r.manualScore == null && r.sheetStatus !== 'PUBLISHED')
    },
    gradedRows() {
      return this.allRows.filter(r => r.manualScore != null || r.sheetStatus === 'PUBLISHED')
    },
    gradingTabs() {
      return [
        { key: 'PENDING', label: '待评分', count: this.pendingRows.length },
        { key: 'GRADED', label: '已评分', count: this.gradedRows.length },
        { key: 'ALL', label: '全部', count: this.allRows.length }
      ]
    },
    queueRows() {
      const scope = this.gradingFilter === 'PENDING' ? this.pendingRows
        : (this.gradingFilter === 'GRADED' ? this.gradedRows : this.allRows)
      return scope
    },
    subjectiveItems() {
      // 只挑真正的主观题：理论卷全部自动判分，不能让管理员手工改分
      return this.sheetItems.filter(i => i.isSubjective === 1 || i.isSubjective === '1')
    },
    hasSubjective() {
      return this.subjectiveItems.length > 0
    },
    practiceTotal() {
      const items = this.subjectiveItems
      if (!items.length) return '—'
      let total = 0
      let weight = 0
      items.forEach(i => {
        const full = Number(i.score) || 0
        weight += full
        total += Number(i.manualScore) || 0
      })
      if (!weight) return Number(total).toFixed(1)
      // 换算为百分制，与设计稿「实操总分 / 100」口径一致
      return (total / weight * 100).toFixed(1)
    },
    overall() {
      const theory = Number(this.sheet && this.sheet.aiScore)
      const practice = Number(this.practiceTotal)
      const hasTheory = this.sheet && this.sheet.aiScore != null
      const hasPractice = this.hasSubjective && !isNaN(practice)
      if (hasTheory && hasPractice) return (theory * 0.4 + practice * 0.6).toFixed(1) + ' 理论40%+实操60%'
      if (hasTheory) return theory.toFixed(1) + '（仅理论 · 自动判分）'
      if (hasPractice) return practice + '（仅实操）'
      return '待定'
    }
  },
  created() {
    this.loadAll()
  },
  methods: {
    loadAll() {
      this.loading = true
      listExam({ pageNum: 1, pageSize: 50 }).then(res => {
        this.exams = (res && res.rows) || []
        if (!this.examId && this.exams.length) this.examId = this.exams[0].id
        if (this.examId) this.loadGrading()
      }).catch(() => {
        this.exams = []
      }).finally(() => {
        this.loading = false
      })
    },
    onExamChange() {
      this.rows = []
      this.sheet = null
      this.currentSheetId = null
      this.loadGrading()
    },
    loadGrading() {
      if (!this.examId) return
      gradingList(this.examId).then(res => {
        this.rows = (res && res.data) || []
        const first = this.queueRows.find(r => r.sheetId) || this.allRows[0]
        if (first) this.openSheet(first)
      }).catch(() => {
        this.rows = []
      })
    },
    openSheet(row) {
      this.currentSheetId = row.sheetId
      sheetDetail(row.sheetId).then(res => {
        const data = (res && res.data) || {}
        this.sheet = Object.assign({}, data.sheet || {}, { userName: row.userName })
        this.sheetItems = (data.items || []).map(item => Object.assign({}, item))
      }).catch(() => {
        this.sheet = null
        this.sheetItems = []
      })
    },
    collectItems() {
      return this.subjectiveItems.map(i => ({
        id: i.id,
        manualScore: i.manualScore == null ? null : Number(i.manualScore),
        manualComment: i.manualComment || ''
      }))
    },
    gradePayload() {
      const items = this.collectItems()
      const scored = items.filter(i => i.manualScore != null)
      const avg = scored.length
        ? scored.reduce((s, i) => s + i.manualScore, 0) / scored.length
        : null
      return {
        sheetId: this.sheet.id,
        manualScore: avg == null ? null : Number(avg.toFixed(1)),
        manualComment: this.sheet.manualComment || '',
        items
      }
    },
    draft() {
      this.$message.success('已暂存到本地（未提交）。点击「提交并评下一份」才写库。')
    },
    submitAndNext() {
      if (!this.sheet) return
      const payload = this.gradePayload()
      if (payload.manualScore == null) {
        this.$message.warning('请至少给一道题打分')
        return
      }
      gradeSheet(payload).then(() => {
        this.$message.success('已提交「' + this.sheet.userName + '」的实操评分')
        const list = this.queueRows
        const idx = list.findIndex(r => r.sheetId === this.currentSheetId)
        this.loadGrading()
        const next = list[idx + 1]
        if (next) {
          setTimeout(() => this.openSheet(next), 200)
        } else {
          this.sheet = null
          this.$message.info('本批次已评完')
        }
      }).catch(() => {})
    },
    publishAll() {
      this.$confirm('将本场考核的答卷成绩置为已发布（实习生端可见），是否继续？', '发布成绩', {
        confirmButtonText: '确认发布',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => publishResult(this.examId, { disableExam: true })).then(() => {
        this.$message.success('成绩已发布')
        this.loadGrading()
      }).catch(() => {})
    },
    exportExcel() {
      this.$message({ message: '导出依赖成绩汇总导出接口（待补），当前可先用表格数据人工导出', type: 'warning' })
    },
    openProfile(row) {
      this.$router.push('/department/people/profile/' + row.userId)
    },
    previewAnswer() {
      this.$message.info('在线预览依赖文件包解压/预览服务（待补）')
    },
    downloadAnswer() {
      const url = this.sheet && this.sheet.subjectAnswer
      if (!url) return
      window.open(url, '_blank')
    },
    modeText(exam) {
      if (exam.examMode === 'FORMAL') return '正式'
      if (exam.examMode === 'MOCK') return '模拟'
      return exam.examMode || '考核'
    },
    sheetStatusText(row) {
      if (row.sheetStatus === 'PUBLISHED') return '已发布'
      if (row.manualScore != null) return '已评分'
      if (row.subjectAnswer) return '待评分'
      return '未交卷'
    },
    conclusionText(row) {
      if (row.finalScore == null) return '待定'
      if (row.passFlag === 1 || row.passFlag === '1') return '通过'
      if (row.passFlag === 0 || row.passFlag === '0') return '未通过'
      const score = Number(row.finalScore)
      return score >= this.passLine ? '通过' : '未通过'
    },
    conclusionTone(row) {
      const text = this.conclusionText(row)
      return { '通过': 'green', '未通过': 'red', '待定': 'orange' }[text] || 'gray'
    },
    fmtScore(value) {
      if (value == null || value === '') return '—'
      const n = Number(value)
      return isNaN(n) ? '—' : n.toFixed(1)
    },
    fmtTime(value) {
      if (!value) return '—'
      const d = new Date(value)
      if (isNaN(d.getTime())) return '—'
      const p = n => (n < 10 ? '0' + n : '' + n)
      return p(d.getMonth() + 1) + '-' + p(d.getDate()) + ' ' + p(d.getHours()) + ':' + p(d.getMinutes())
    }
  }
}
</script>

<style lang="scss" scoped>
@import '~@/assets/styles/department-module.scss';

.dtabs.page { padding: 0 8px; margin: 0 0 18px; background: #fff; border: 1px solid #e4e9f0; border-radius: 10px; }
.dtab .tab-n { margin-left: 4px; color: #98a2b3; font-size: 11px; }
.dtab.on .tab-n { color: #1764f5; }
.dfilter { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; margin-bottom: 16px; }
.dfilter .grow { flex: 1; }

.dtodo-row.clickable { cursor: pointer; border-radius: 8px; transition: background .15s; }
.dtodo-row.clickable:hover { background: #f7f9fc; }
.dtodo-row.sel { margin: 0 -10px; padding-right: 10px; padding-left: 10px; background: #e8f1fd; }
.dtodo-row.sel .dtodo-t { color: #1764f5; }

.q-stem { margin: 0 0 10px; color: #344054; font-size: 12.5px; line-height: 1.7; }
code { padding: 1px 5px; color: #344054; font-size: 11.5px; background: #f2f4f7; border-radius: 4px; }
.dkv strong small { color: #98a2b3; font-size: 11px; font-weight: 400; }
.hint-text { color: #98a2b3; font-size: 11.5px; }
</style>
