<template>
  <div class="dept-page">
    <div class="dept-breadcrumb">
      学习与考核管理 <span>/</span> <b>成绩管理</b>
    </div>

    <div class="dept-heading">
      <div>
        <span class="eyebrow">DEPARTMENT ADMIN</span>
        <h1>成绩管理</h1>
        <p>理论交卷即自动判分，实操由管理员逐题打分。按考核场次查看：谁交了、谁待评、谁已出分，以及本场各章节的得分率。</p>
      </div>
      <div class="dept-heading-actions">
        <el-button size="small" icon="el-icon-download" :disabled="!examId" @click="exportCsv">导出 CSV</el-button>
        <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="loadAll">刷新</el-button>
      </div>
    </div>

    <!-- 考核场次选择条 -->
    <div class="dfilter">
      <span class="dfilter-lb">考核场次</span>
      <el-select v-model="examId" size="small" placeholder="选择考核" style="width:300px" @change="onExamChange">
        <el-option v-for="e in exams" :key="e.id" :label="examLabel(e)" :value="e.id" />
      </el-select>
      <span v-if="currentExam" class="dtag" :class="currentExam.examType === 'PRACTICAL' ? 'green' : 'blue'">
        {{ currentExam.examType === 'PRACTICAL' ? '实操考核' : '理论考核' }}
      </span>
      <span v-if="currentExam" class="dtag">{{ statusText(currentExam.status) }}</span>
      <span class="grow" />
      <el-button v-if="examId" size="small" type="primary" :disabled="!canPublish" :loading="publishing" @click="publishAll">发布成绩</el-button>
    </div>

    <!-- 没有任何考核：整页空态（区分「没数据」与「筛没了」） -->
    <div v-if="!loading && !exams.length" class="dsec">
      <div class="dempty">
        <i class="el-icon-warning-outline" />
        <strong>本部门暂无考核批次</strong>
        <span>请先在「正式考核管理」创建并发布一场考核，实习生交卷后即可在此评分与查看成绩。</span>
        <el-button size="small" type="primary" @click="$router.push('/department/study/exam')">去正式考核管理</el-button>
      </div>
    </div>

    <template v-else-if="examId">
      <!-- ① 总览：全部来自当前场次的应考名单（真实数据），点卡即筛选 -->
      <div class="prep-kpi-row kpi6">
        <div
          v-for="k in kpiCards"
          :key="k.key"
          class="prep-kpi-card"
          :class="[k.tone, { clickable: k.filter, active: k.filter && statusFilter === k.filter }]"
          @click="k.filter && applyKpi(k.filter)"
        >
          <span class="prep-kpi-icon"><i :class="k.icon" /></span>
          <span class="prep-kpi-body">
            <span class="prep-kpi-value">{{ k.value }}<small>{{ k.unit }}</small></span>
            <span class="prep-kpi-label">{{ k.label }}</span>
          </span>
        </div>
      </div>

      <!-- ② 筛选条 -->
      <div class="prep-filter">
        <span class="dchips">
          <span
            v-for="c in statusChips"
            :key="c.key"
            class="dchip"
            :class="{ on: statusFilter === c.key }"
            @click="statusFilter = c.key"
          >
            {{ c.label }} <span class="n">{{ c.count }}</span>
          </span>
        </span>
        <el-button v-if="statusFilter !== 'ALL'" size="small" icon="el-icon-refresh-left" @click="statusFilter = 'ALL'">重置</el-button>
        <span class="prep-filter-sum">显示 {{ filteredRows.length }} / {{ rows.length }} 人</span>
      </div>

      <!-- 页签 -->
      <div class="dtabs page">
        <div class="dtab" :class="{ on: activeTab === 'grading' }" @click="activeTab = 'grading'">
          实操评分 <span v-if="!isTheoryExam" class="tab-n">{{ pendingRows.length }}</span>
        </div>
        <div class="dtab" :class="{ on: activeTab === 'summary' }" @click="switchToSummary">成绩汇总</div>
      </div>

      <!-- ============ ① 实操评分 ============ -->
      <div v-if="activeTab === 'grading'" class="dgrid">
        <div class="dcard c4">
          <div class="dcard-h">
            <div class="tt"><h3 style="font-size:14px">{{ isTheoryExam ? '答卷队列' : '待评分队列' }}</h3></div>
            <span class="hint-text">按提交时间</span>
          </div>

          <div v-if="queueRows.length" class="dtodo">
            <div
              v-for="row in queueRows"
              :key="row.userId"
              class="dtodo-row clickable"
              :class="{ sel: currentSheetId === row.sheetId }"
              @click="openSheet(row)"
            >
              <span class="dtodo-dot" :class="row.sheetStatus === 'PUBLISHED' ? 'g' : 'o'" />
              <div class="dtodo-main">
                <div class="dtodo-t">
                  {{ row.userName }}
                  <em v-if="row.retakeSeq > 1" class="retake">第 {{ row.retakeSeq }} 次</em>
                </div>
                <div class="dtodo-s">
                  已作答 {{ row.answeredFiles || 0 }} / {{ row.itemCount || 0 }} 题 ·
                  {{ fmtTime(row.submitTime) }} · {{ sheetStatusText(row) }}
                </div>
              </div>
            </div>
          </div>
          <div v-else-if="!allRows.length" class="dempty small">
            <i class="el-icon-document-checked" />
            <strong>本场还没有任何答卷</strong>
            <span>实习生交卷后，这里会出现待评分的队列。</span>
          </div>
          <div v-else class="dempty small">
            <i class="el-icon-search" />
            <strong>当前筛选下没有待评分的答卷</strong>
            <span>换个筛选条件，或点下方按钮看全部。</span>
            <el-button size="mini" @click="statusFilter = 'ALL'">查看全部</el-button>
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

            <div v-if="hasSubjective" class="dgrid" style="gap:12px">
              <div v-for="item in subjectiveItems" :key="item.id" class="c6">
                <div class="dsec" style="margin:0;padding:14px 16px">
                  <div class="dcard-h" style="margin-bottom:10px">
                    <div class="tt">
                      <span class="idx">{{ item.seq }}</span>
                      <h3 style="font-size:13px">第 {{ item.seq }} 题 · {{ item.qtype || '主观题' }}</h3>
                    </div>
                    <span class="hint-text">满分 {{ fmtScore(item.fullScore != null ? item.fullScore : item.score) }}</span>
                  </div>
                  <p class="q-stem">{{ item.stemSnapshot || item.stem }}</p>
                  <div class="dfield">
                    <label>得分（满分 {{ fmtScore(item.fullScore != null ? item.fullScore : item.score) }}）</label>
                    <el-input-number
                      v-model="item.manualScore"
                      size="small"
                      :min="0"
                      :max="Number(item.fullScore != null ? item.fullScore : item.score) || 100"
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
                本场答卷<b>全部为客观题</b>，已由后端自动判分写入
                <code>is_correct / final_score / pass_flag</code>，<b>无需人工录分</b>。
                如需总评语，可在下方补充后直接发布成绩。
              </span>
            </div>

            <div class="dkv" style="grid-template-columns:repeat(5,1fr);margin-top:16px">
              <div><span>实操总分</span><strong>{{ practiceTotal }} <small>/ 100</small></strong></div>
              <div><span>通过线</span><strong>{{ passLine }} <small>分</small></strong></div>
              <div><span>理论（自动）</span><strong>{{ fmtScore(sheet.aiScore) }} <small>/ 100</small></strong></div>
              <div><span>最终分（后端）</span><strong>{{ fmtScore(sheet.finalScore) }}</strong></div>
              <div><span>参考综合分</span><strong>{{ overallRef }} <small>参考</small></strong></div>
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

      <!-- ============ ② 成绩汇总 ============ -->
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
                <th>第 N 次</th>
                <th>理论 / 自动分</th>
                <th>实操 / 人工分</th>
                <th>最终分<em>后端</em></th>
                <th>参考综合分<em>40/60</em></th>
                <th>结论</th>
                <th>答卷状态</th>
                <th style="width:80px">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in filteredRows" :key="row.userId">
                <td><span class="strong">{{ row.userName }}</span></td>
                <td>{{ row.deptName || '—' }}</td>
                <td>{{ row.sheetId ? (row.retakeSeq || 1) : '—' }}</td>
                <td>
                  <template v-if="row.aiScore != null">{{ fmtScore(row.aiScore) }} <span class="dbadge blue">自动</span></template>
                  <template v-else>— <span class="dbadge">未出分</span></template>
                </td>
                <td>
                  <template v-if="row.manualScore != null">{{ fmtScore(row.manualScore) }} <span class="dbadge purple">人工</span></template>
                  <template v-else>— <span class="dbadge">待录分</span></template>
                </td>
                <td><span class="strong">{{ row.finalScore != null ? fmtScore(row.finalScore) : '待定' }}</span></td>
                <td><span class="text-muted">{{ refOverall(row) }}</span></td>
                <td><span class="dbadge" :class="conclusionTone(row)">{{ conclusionText(row) }}</span></td>
                <td><span class="text-muted">{{ row.sheetId ? sheetStatusText(row) : '未交卷' }}</span></td>
                <td><el-button type="text" @click="openProfile(row)">档案</el-button></td>
              </tr>
              <tr v-if="!rows.length">
                <td colspan="10">
                  <div class="dempty small">
                    <i class="el-icon-document" />
                    <strong>本场还没有答卷</strong>
                    <span>实习生交卷后这里会出现对应行。</span>
                  </div>
                </td>
              </tr>
              <tr v-else-if="!filteredRows.length">
                <td colspan="10">
                  <div class="dempty small">
                    <i class="el-icon-search" />
                    <strong>当前筛选下没有记录</strong>
                    <span>换个筛选条件看全部。</span>
                    <el-button size="mini" @click="statusFilter = 'ALL'">查看全部</el-button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
          <p class="dsec-note">
            <b>两个分数口径不同，不要混看：</b>
            「最终分」来自答卷的 <code>final_score</code>（后端写入）；
            「参考综合分」= 理论 × <b>{{ weight.theory }}%</b> + 实操 × <b>{{ weight.practice }}%</b>（权重读 <code>assessment_config</code>），
            仅在后端综合规则表 <code>exam_rule_snapshot</code> 接通前的<b>参考口径</b>。
            任一分缺失一律显示「待定」，不给「通过」—— 部门直接终审没有二次复核。
          </p>
        </div>

        <div class="dcard c4">
          <div class="dcard-h">
            <div class="tt"><span class="idx o">章</span><h3>本场章节得分率</h3></div>
            <span class="hint-text">
              <template v-if="chapterLoading">计算中…</template>
              <template v-else-if="chapterStats.length">基于 {{ answeredSheetCount }} 份答卷</template>
            </span>
          </div>

          <div v-if="chapterLoading" class="dempty small"><i class="el-icon-loading" /><span>正在拉取答卷明细…</span></div>
          <template v-else-if="chapterStats.length">
            <p class="dsec-note" style="margin-top:0">
              <span v-if="answeredSheetCount < 5" class="warn-note">
                <i class="el-icon-warning-outline" /> 样本不足（仅 {{ answeredSheetCount }} 份），仅作参考
              </span>
              <span v-else>口径：已作答的题参与得分率，<b>未作答单独统计</b>（不当作答错）。</span>
            </p>
            <div v-for="row in chapterStats" :key="row.name" class="chap-row">
              <div class="chap-head">
                <span class="chap-name">{{ row.name }}</span>
                <span class="chap-rate" :class="row.tone">{{ row.rate == null ? '无作答' : row.rate + '%' }}</span>
              </div>
              <div class="chap-bar">
                <i class="ok" :style="{ width: row.okPct + '%' }" />
                <i class="bad" :style="{ width: row.badPct + '%' }" />
                <i class="blank" :style="{ width: row.blankPct + '%' }" />
              </div>
              <div class="chap-meta">
                对 {{ row.ok }} · 错 {{ row.bad }} · 未作答 {{ row.blank }}
              </div>
            </div>
            <p class="dsec-note">
              数据来源：答卷明细的 <code>items[].knowledgePoint</code>（章节）× <code>isCorrect</code> / <code>userAnswer</code>，
              按章节聚合后<b>由低到高</b>排序 —— 排前面的就是最该补的章节。
            </p>
          </template>
          <div v-else class="dempty small">
            <i class="el-icon-data-analysis" />
            <strong>还没有可用于统计的答卷</strong>
            <span>本场有答卷后，这里会按章节算出得分率（未作答单独统计）。</span>
          </div>
        </div>
      </div>
    </template>

    <div v-else-if="loading" class="dsec">
      <div class="dempty"><i class="el-icon-loading" /><strong>正在加载考核…</strong></div>
    </div>
  </div>
</template>

<script>
import { listExam, gradingList, sheetDetail, gradeSheet, publishResult } from '@/api/business/exam'

/** 参考综合分的权重（与 assessment_config 保持一致；后端综合规则表接通后可直接删掉这两个常量） */
const THEORY_WEIGHT = 40
const PRACTICE_WEIGHT = 60

export default {
  name: 'DeptScores',
  data() {
    return {
      loading: false,
      publishing: false,
      activeTab: 'grading',
      /** ③ 筛选：ALL 全部 / SUBMITTED 已交卷 / TODO 待评分 / GRADED 已出分 */
      statusFilter: 'ALL',
      examId: null,
      exams: [],
      rows: [],
      sheet: null,
      sheetItems: [],
      currentSheetId: null,
      // === 改版新增 ===
      /** 本场章节得分率（真数据，按试卷明细聚合） */
      chapterStats: [],
      chapterLoading: false,
      weight: { theory: THEORY_WEIGHT, practice: PRACTICE_WEIGHT }
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
    /** 理论卷：交卷即自动判分，不存在「待人工评分」 */
    isTheoryExam() {
      return !!this.currentExam && this.currentExam.examType !== 'PRACTICAL'
    },
    /** 已出分但未发布（成绩要发布后实习生才可见） */
    unpublishedRows() {
      return this.allRows.filter(r => r.finalScore != null && r.sheetStatus !== 'PUBLISHED')
    },
    /** 待处理：实操卷 = 待人工评分；理论卷 = 待发布 */
    pendingRows() {
      if (this.isTheoryExam) return this.unpublishedRows
      return this.allRows.filter(r => r.manualScore == null && r.sheetStatus !== 'PUBLISHED')
    },
    pendingLabel() {
      return this.isTheoryExam ? '待发布' : '待评分'
    },
    /** 已出分：拿到了最终分或已发布 */
    gradedRows() {
      return this.allRows.filter(r => r.finalScore != null || r.sheetStatus === 'PUBLISHED')
    },
    submittedRows() { return this.allRows },
    /** 有答卷份数（章节统计的样本量） */
    answeredSheetCount() { return this.allRows.length },
    /** ③ 列表（评分队列与汇总表都用它） */
    filteredRows() {
      if (this.statusFilter === 'SUBMITTED') return this.submittedRows
      if (this.statusFilter === 'TODO') return this.pendingRows
      if (this.statusFilter === 'GRADED') return this.gradedRows
      return this.rows
    },
    /** 队列只列「有答卷」的人；未交卷的人仅在汇总表里以「未交卷」出现
     *  （否则点未交卷的人会拿 undefined 的 sheetId 去请求，后端报参数类型不匹配） */
    queueRows() { return this.filteredRows.filter(r => r.sheetId) },
    statusChips() {
      return [
        { key: 'ALL', label: '全部', count: this.rows.length },
        { key: 'SUBMITTED', label: '已交卷', count: this.submittedRows.length },
        { key: 'TODO', label: this.pendingLabel, count: this.pendingRows.length },
        { key: 'GRADED', label: '已出分', count: this.gradedRows.length }
      ]
    },
    /** ① 总览六卡：全部由当前场次的应考名单算（取不到就显示 —，不编数） */
    kpiCards() {
      const scored = this.rows.filter(r => r.finalScore != null || r.sheetStatus === 'PUBLISHED')
      const scores = scored.map(r => Number(r.finalScore)).filter(n => !isNaN(n))
      const avg = scores.length ? Math.round(scores.reduce((a, b) => a + b, 0) / scores.length * 10) / 10 : null
      const passed = scored.filter(r => r.passFlag === 1 || r.passFlag === '1').length
      const rate = scored.length ? Math.round(passed / scored.length * 100) : null
      return [
        { key: 'ALL', label: '应考人数', value: this.rows.length, unit: '人', icon: 'el-icon-user', filter: 'ALL' },
        { key: 'SUBMITTED', label: '已交卷', value: this.submittedRows.length, unit: '人', icon: 'el-icon-upload2', tone: 'tone-orange', filter: 'SUBMITTED' },
        { key: 'TODO', label: this.pendingLabel, value: this.pendingRows.length, unit: '人', icon: 'el-icon-edit-outline', tone: 'tone-red', filter: 'TODO' },
        { key: 'GRADED', label: '已出分', value: this.gradedRows.length, unit: '人', icon: 'el-icon-circle-check', tone: 'tone-green', filter: 'GRADED' },
        { key: 'AVG', label: '平均分', value: avg == null ? '—' : avg, unit: avg == null ? '' : '分', icon: 'el-icon-data-line', tone: 'tone-purple' },
        { key: 'RATE', label: '通过率', value: rate == null ? '—' : rate, unit: rate == null ? '' : '%', icon: 'el-icon-medal', tone: 'tone-green' }
      ]
    },
    /** 能否发布：存在「已出分但未发布」的答卷即可（实操卷需先评完分才会出分） */
    canPublish() {
      return this.unpublishedRows.length > 0
    },
    subjectiveItems() {
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
        const full = Number(i.fullScore != null ? i.fullScore : i.score) || 0
        weight += full
        total += Number(i.manualScore) || 0
      })
      if (!weight) return Number(total).toFixed(1)
      return (total / weight * 100).toFixed(1)
    },
    /** 参考综合分（面板里那一份） */
    overallRef() {
      return this.refOverall(this.sheet || {})
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
      this.statusFilter = 'ALL'
      this.activeTab = 'grading'
      this.chapterStats = []
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
    switchToSummary() {
      this.activeTab = 'summary'
      if (!this.chapterStats.length) this.loadChapterStats()
    },
    /**
     * 本场章节得分率（真数据）
     * 数据源：逐份答卷的 detail.items[]（带 knowledgePoint / isCorrect / userAnswer / fullScore）
     * 口径：已作答的题参与得分率；未作答单独统计，不并入分母。
     */
    loadChapterStats() {
      const sheets = this.allRows
      if (!sheets.length) { this.chapterStats = []; return }
      this.chapterLoading = true
      Promise.all(sheets.map(r => sheetDetail(r.sheetId).catch(() => null))).then(list => {
        const map = {}
        list.forEach(res => {
          const items = (res && res.data && res.data.items) || []
          items.forEach(it => {
            const name = it.knowledgePoint || '未标注章节'
            if (!map[name]) map[name] = { name, ok: 0, bad: 0, blank: 0 }
            const answered = it.userAnswer !== null && it.userAnswer !== undefined && String(it.userAnswer).trim() !== ''
            if (!answered) map[name].blank++
            else if (it.isCorrect === 1 || it.isCorrect === '1') map[name].ok++
            else map[name].bad++
          })
        })
        const list2 = Object.keys(map).map(k => {
          const m = map[k]
          const answered = m.ok + m.bad
          const total = answered + m.blank
          const rate = answered ? Math.round(m.ok / answered * 100) : null
          return Object.assign(m, {
            rate,
            okPct: total ? m.ok / total * 100 : 0,
            badPct: total ? m.bad / total * 100 : 0,
            blankPct: total ? m.blank / total * 100 : 0,
            tone: rate == null ? 'mid' : (rate < 60 ? 'poor' : (rate < 80 ? 'mid' : 'good'))
          })
        })
        // 排前面的 = 最该补的：得分率低的在前；无作答的垫到最后
        list2.sort((a, b) => {
          if (a.rate == null && b.rate == null) return b.blank - a.blank
          if (a.rate == null) return 1
          if (b.rate == null) return -1
          return a.rate - b.rate
        })
        this.chapterStats = list2
        this.chapterLoading = false
      }).catch(() => { this.chapterLoading = false })
    },
    openSheet(row) {
      // 防御：没有答卷的人（sheetId 为空）不能进评分面板，否则会请求 /detail/undefined
      if (!row || !row.sheetId) return
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
      const scope = '本次将把「' + (this.currentExam ? this.currentExam.examName : '') + '」的 ' +
        this.submittedRows.length + ' 份答卷成绩置为已发布（实习生端可见），且暂不支持撤回。'
      this.$confirm(scope, '发布成绩', {
        confirmButtonText: '确认发布',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.publishing = true
        return publishResult(this.examId, { disableExam: true })
      }).then(() => {
        this.publishing = false
        this.$message.success('成绩已发布')
        this.loadGrading()
      }).catch(() => { this.publishing = false })
    },
    /** ① KPI 点卡 = 套用筛选（再点一次取消） */
    applyKpi(key) {
      this.statusFilter = this.statusFilter === key ? 'ALL' : key
    },
    /**
     * 导出 CSV：成绩汇总 + 章节得分率（**前端直接生成，不依赖后端导出接口**）
     * 带 UTF-8 BOM，Excel 打开中文不乱码；字段含逗号/引号时做转义。
     */
    exportCsv() {
      if (!this.examId) { this.$message.warning('请先选择考核场次'); return }
      const esc = v => {
        const s = v === null || v === undefined ? '' : String(v)
        return /[",\n]/.test(s) ? '"' + s.replace(/"/g, '""') + '"' : s
      }
      const lines = []
      const examName = this.currentExam ? this.currentExam.examName : ('考核 ' + this.examId)
      lines.push(['成绩汇总', examName, '导出时间：' + this.nowText()].map(esc).join(','))
      lines.push(['姓名', '部门', '第N次', '理论自动分', '实操人工分', '最终分(后端)', '参考综合分', '结论', '答卷状态', '提交时间'].map(esc).join(','))
      this.filteredRows.forEach(r => {
        lines.push([
          r.userName, r.deptName || '', r.sheetId ? (r.retakeSeq || 1) : '',
          r.aiScore == null ? '' : this.fmtScore(r.aiScore),
          r.manualScore == null ? '' : this.fmtScore(r.manualScore),
          r.finalScore == null ? '待定' : this.fmtScore(r.finalScore),
          this.refOverall(r).replace('（参考）', ''),
          this.conclusionText(r), r.sheetId ? this.sheetStatusText(r) : '未交卷',
          r.submitTime ? this.fmtTime(r.submitTime) : ''
        ].map(esc).join(','))
      })
      if (this.chapterStats.length) {
        lines.push('')
        lines.push(['章节得分率（未作答单独统计）', '基于 ' + this.answeredSheetCount + ' 份答卷'].map(esc).join(','))
        lines.push(['章节', '答对', '答错', '未作答', '得分率(%)'].map(esc).join(','))
        this.chapterStats.forEach(c => {
          lines.push([c.name, c.ok, c.bad, c.blank, c.rate == null ? '' : c.rate].map(esc).join(','))
        })
      }
      const csv = '\ufeff' + lines.join('\r\n') + '\r\n'
      const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = '成绩_' + examName.replace(/[\\/:*?"<>|]/g, '_') + '_' + this.todayText() + '.csv'
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
      this.$message.success('已导出 ' + this.filteredRows.length + ' 条成绩记录')
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
    examLabel(e) {
      return e.examName + '（' + this.modeText(e) + '·' + (e.examType === 'PRACTICAL' ? '实操' : '理论') + '）'
    },
    modeText(exam) {
      if (exam.examMode === 'FORMAL') return '正式'
      if (exam.examMode === 'PRACTICE') return '模拟'
      return exam.examMode || '考核'
    },
    statusText(s) {
      return { DRAFT: '待发布', PUBLISHED: '已发布', GRADING: '待批改', DISABLED: '已停用' }[s] || s || ''
    },
    sheetStatusText(row) {
      if (row.sheetStatus === 'PUBLISHED') return '已发布'
      if (row.sheetStatus === 'IN_PROGRESS') return '答题中'
      if (row.manualScore != null) return '已评分'
      if (row.finalScore != null) return '待发布'
      return '待评分'
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
    /** 参考综合分（理论 × 40% + 实操 × 60%）—— 与后端 final_score 并列展示，并标注来源 */
    refOverall(row) {
      const t = Number(row.aiScore)
      const p = Number(row.manualScore)
      const hasT = row.aiScore != null && !isNaN(t)
      const hasP = row.manualScore != null && !isNaN(p)
      if (!hasT && !hasP) return '—'
      if (hasT && hasP) {
        return (t * this.weight.theory / 100 + p * this.weight.practice / 100).toFixed(1) + '（参考）'
      }
      return (hasT ? t.toFixed(1) + '（仅理论）' : p.toFixed(1) + '（仅实操）')
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
    },
    nowText() {
      const d = new Date()
      const p = n => (n < 10 ? '0' + n : '' + n)
      return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + ' ' + p(d.getHours()) + ':' + p(d.getMinutes())
    },
    todayText() {
      const d = new Date()
      const p = n => (n < 10 ? '0' + n : '' + n)
      return d.getFullYear() + p(d.getMonth() + 1) + p(d.getDate())
    }
  }
}
</script>

<style lang="scss" scoped>
@import '~@/assets/styles/department-module.scss';

/* ① 总览六卡（复用备考页那套视觉，本页 6 列） */
.prep-kpi-row { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap: 12px; margin-bottom: 14px; }
.prep-kpi-row.kpi6 { grid-template-columns: repeat(6, minmax(0, 1fr)); }
.prep-kpi-card { display: flex; align-items: center; gap: 11px; padding: 13px 15px; background: #fff; border: 1px solid #e7ecf3; border-radius: 8px; transition: all .15s; }
.prep-kpi-card.clickable { cursor: pointer; }
.prep-kpi-card.clickable:hover { border-color: #b9d2ff; box-shadow: 0 2px 8px rgba(23, 100, 245, .08); }
.prep-kpi-card.active { border-color: #1764f5; box-shadow: 0 0 0 2px rgba(23, 100, 245, .12); }
.prep-kpi-icon { display: flex; width: 36px; height: 36px; flex: none; align-items: center; justify-content: center; color: #1764f5; background: #edf4ff; font-size: 18px; border-radius: 8px; }
.prep-kpi-body { display: flex; min-width: 0; flex-direction: column; }
.prep-kpi-value { color: #1d2939; font-size: 20px; font-weight: 600; line-height: 1.2; }
.prep-kpi-value small { margin-left: 3px; color: #667085; font-size: 12px; font-weight: 400; }
.prep-kpi-label { margin-top: 2px; color: #667085; font-size: 12px; }
.prep-kpi-card.tone-green .prep-kpi-icon { color: #23966f; background: #eaf7f1; }
.prep-kpi-card.tone-orange .prep-kpi-icon { color: #e6a23c; background: #fdf6ec; }
.prep-kpi-card.tone-red .prep-kpi-icon { color: #d9534f; background: #fdeeed; }
.prep-kpi-card.tone-purple .prep-kpi-icon { color: #7b5cf0; background: #f2eeff; }

/* ② 筛选条 */
.prep-filter { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; padding: 0 0 10px; }
.prep-filter-sum { margin-left: auto; color: #667085; font-size: 12px; }

.dfilter { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; margin-bottom: 16px; }
.dfilter .grow { flex: 1; }
.dfilter-lb { color: #667085; font-size: 12.5px; }
.dtag { padding: 2px 8px; color: #475467; background: #f2f4f7; border-radius: 4px; font-size: 11.5px; }
.dtag.blue { color: #1764f5; background: #edf4ff; }
.dtag.green { color: #23966f; background: #eaf7f1; }

.dtabs.page { padding: 0 8px; margin: 0 0 18px; background: #fff; border: 1px solid #e4e9f0; border-radius: 10px; }
.dtab .tab-n { margin-left: 4px; color: #98a2b3; font-size: 11px; }
.dtab.on .tab-n { color: #1764f5; }

.dtodo-row.clickable { cursor: pointer; border-radius: 8px; transition: background .15s; }
.dtodo-row.clickable:hover { background: #f7f9fc; }
.dtodo-row.sel { margin: 0 -10px; padding-right: 10px; padding-left: 10px; background: #e8f1fd; }
.dtodo-row.sel .dtodo-t { color: #1764f5; }
.retake { margin-left: 6px; padding: 1px 5px; color: #e6a23c; background: #fdf6ec; border-radius: 3px; font-size: 11px; font-style: normal; }

.q-stem { margin: 0 0 10px; color: #344054; font-size: 12.5px; line-height: 1.7; }
code { padding: 1px 5px; color: #344054; font-size: 11.5px; background: #f2f4f7; border-radius: 4px; }
.dkv strong small { color: #98a2b3; font-size: 11px; font-weight: 400; }
.hint-text { color: #98a2b3; font-size: 11.5px; }
.text-muted { color: #8490a0; font-size: 12.5px; }
.dtbl th em { margin-left: 4px; color: #98a2b3; font-size: 10.5px; font-style: normal; font-weight: 400; }

/* ③ 章节得分率（三态堆叠条：对 / 错 / 未作答） */
.chap-row { margin-bottom: 12px; }
.chap-head { display: flex; align-items: baseline; justify-content: space-between; margin-bottom: 5px; }
.chap-name { color: #344054; font-size: 12.5px; }
.chap-rate { font-size: 12.5px; font-weight: 600; }
.chap-rate.poor { color: #d9534f; }
.chap-rate.mid { color: #e6a23c; }
.chap-rate.good { color: #23966f; }
.chap-bar { display: flex; height: 8px; overflow: hidden; background: #f2f4f7; border-radius: 4px; }
.chap-bar i { display: block; height: 100%; }
.chap-bar i.ok { background: #23966f; }
.chap-bar i.bad { background: #f04438; }
.chap-bar i.blank { background: #d0d5dd; }
.chap-meta { margin-top: 4px; color: #98a2b3; font-size: 11.5px; }
.warn-note { display: inline-flex; align-items: center; gap: 4px; color: #b54708; }

@media (max-width: 1400px) { .prep-kpi-row, .prep-kpi-row.kpi6 { grid-template-columns: repeat(3, minmax(0, 1fr)); } }
@media (max-width: 900px) {
  .prep-kpi-row, .prep-kpi-row.kpi6 { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .prep-filter-sum { margin-left: 0; }
}
</style>
