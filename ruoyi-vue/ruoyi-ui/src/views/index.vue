<template>
  <div class="workspace-page">
    <header class="workspace-head">
      <div>
        <span class="eyebrow">{{ eyebrow }}</span>
        <h1>{{ isIntern ? '早上好，' + (nickName || '实习生') : title }}</h1>
        <p>{{ isIntern ? ((isFormal ? '正式实习生' : '预备实习生') + ' · ' + (deptName || '所属部门') + ' · ' + mentorText) : subtitle }}</p>
      </div>
      <div class="head-actions">
        <el-button size="small" icon="el-icon-refresh" @click="refresh">刷新</el-button>
        <el-button v-if="isIntern && nextCourse" size="small" type="primary" icon="el-icon-video-play" @click="go('/assessment/intern/learning/course/' + nextCourse.id)">继续学习</el-button>
        <el-button v-else-if="isIntern" size="small" type="primary" icon="el-icon-reading" @click="go('/assessment/intern/learning')">继续学习</el-button>
        <el-button v-else size="small" type="primary" icon="el-icon-message" @click="go(messagePath)">消息中心</el-button>
      </div>
    </header>

    <section v-if="!isIntern" class="metric-grid">
      <article v-for="metric in metrics" :key="metric.label" class="metric" :class="metric.tone">
        <div class="metric-label"><i :class="metric.icon" /><span>{{ metric.label }}</span></div>
        <strong>{{ metric.value }}</strong>
        <small>{{ metric.hint }}</small>
      </article>
    </section>

    <!-- ============ 实习生端看板（2026-09-23 二次改版：只保留「可确定的数据视图」——
         公告不再占用页面，统一走顶栏铃铛 → 消息中心；去掉「下一步做什么」这类推测性内容；
         每张卡都必须能对上一个真实接口字段）============ -->
    <template v-if="isIntern">
      <!-- 01 学习进度总览 -->
      <section class="i2-card">
        <div class="panel-head">
          <div><span class="section-index">01</span><h2>学习进度总览</h2></div>
          <el-button type="text" @click="go('/assessment/intern/learning')">去学习 ›</el-button>
        </div>
        <div v-loading="learningLoading" class="m-grid">
          <div class="m-card">
            <span class="m-lb">已完成单项</span>
            <b class="m-vl">{{ learningOverview.completedItems }}<em>/ {{ learningOverview.itemCount }} 项</em></b>
            <span class="m-ft">覆盖 {{ learningOverview.courseCount }} 门已发布课程</span>
          </div>
          <div class="m-card">
            <span class="m-lb">学习完成率</span>
            <b class="m-vl" :class="learningGap ? 'warn' : 'ok'">{{ learningProgress }}<em>%</em></b>
            <span class="m-bar"><i :class="learningGap ? 'warn' : 'ok'" :style="{ width: Math.min(learningProgress, 100) + '%' }" /><u class="gate" style="left: 70%" /></span>
            <span class="m-ft" :class="learningGap ? 'warn' : 'ok'">{{ learningGap ? '距 70% 转正门槛还差 ' + learningGap + '%' : '已达到 70% 转正门槛' }}</span>
          </div>
          <div class="m-card">
            <span class="m-lb">已学 / 预计时长</span>
            <b class="m-vl">{{ durationText(learnedSeconds) }}<em>/ {{ durationText(expectedSeconds) }}</em></b>
            <span class="m-ft">完成度按「已学秒数 ÷ 学习项总时长」计</span>
          </div>
          <div class="m-card">
            <span class="m-lb">分类型完成</span>
            <b class="m-vl">{{ itemStats.video.done }}<em>/ {{ itemStats.video.total }} 视频</em></b>
            <b class="m-vl slim">{{ itemStats.doc.done }}<em>/ {{ itemStats.doc.total }} 文档</em></b>
          </div>
        </div>
        <div class="fact-line">
          <span>进度最低项：<b>{{ weakestItemLabel }}</b></span>
          <span>最近学习：<b>{{ learningOverview.lastStudyTime }}</b></span>
          <span>数据源：<code>/business/learning/courses</code></span>
        </div>
      </section>

      <div class="i2-grid">
        <!-- 02 学习项完成矩阵 -->
        <section class="i2-card i2-span12">
          <div class="panel-head"><div><span class="section-index">02</span><h2>学习项完成矩阵</h2></div><span class="card-hint">一格一个学习单项，横向为课程内顺序，悬停看详情</span></div>
          <div v-loading="learningLoading" class="matrix">
            <div v-for="row in matrixRows" :key="row.id" class="mx-row">
              <span class="mx-nm">{{ row.courseName }}<em v-if="row.isRequired" class="req">必修</em></span>
              <span class="mx-cells">
                <span v-for="cell in row.cells" :key="cell.key" class="mx-cell" :class="cell.cls" :title="cell.title" />
              </span>
              <span class="mx-sum">{{ row.done }}/{{ row.total }}</span>
            </div>
            <div v-if="!matrixRows.length" class="note">暂无已发布课程。</div>
            <div v-if="matrixRows.length" class="legend">
              <span><i style="background:#12b76a" />已完成</span>
              <span><i style="background:#f79009" />学习中</span>
              <span><i style="background:#e4e9f0" />未开始</span>
            </div>
          </div>
        </section>

        <!-- 03 学习时长对比 -->
        <section class="i2-card i2-span6">
          <div class="panel-head"><div><span class="section-index">03</span><h2>学习时长对比</h2></div><span class="card-hint">上：已学 · 下：预计</span></div>
          <div v-if="durationRows.length">
            <div v-for="row in durationRows" :key="row.id" class="dur-row">
              <span class="dur-nm">{{ row.courseName }}</span>
              <span class="dur-bars">
                <span class="dur-bar"><i class="learned" :style="{ width: row.learnedPct + '%' }" /></span>
                <span class="dur-bar"><i class="expected" :style="{ width: row.expectedPct + '%' }" /></span>
              </span>
              <span class="dur-vv">{{ durationText(row.learned) }} / {{ durationText(row.expected) }}</span>
            </div>
            <div class="dur-sep">按类型合计（共用同一刻度）</div>
            <div v-for="row in durationTypeRows" :key="row.id" class="dur-row">
              <span class="dur-nm">{{ row.courseName }}</span>
              <span class="dur-bars">
                <span class="dur-bar"><i class="learned" :style="{ width: row.learnedPct + '%' }" /></span>
                <span class="dur-bar"><i class="expected" :style="{ width: row.expectedPct + '%' }" /></span>
              </span>
              <span class="dur-vv">{{ durationText(row.learned) }} / {{ durationText(row.expected) }}</span>
            </div>
          </div>
          <div v-else class="note">暂无已发布课程。</div>
          <div class="fact-line">
            <span>合计已学 <b>{{ durationText(learnedSeconds) }}</b></span>
            <span>预计 <b>{{ durationText(expectedSeconds) }}</b></span>
            <span>已完成 <b>{{ durationRatio }}%</b></span>
          </div>
        </section>

        <!-- 04 正式考核成绩 -->
        <section class="i2-card i2-span6">
          <div class="panel-head"><div><span class="section-index">04</span><h2>正式考核成绩</h2></div><el-button type="text" @click="go('/assessment/intern/learning#sec-exam')">全部场次 ›</el-button></div>
          <div v-if="!formalBoard.length" class="i2-empty compact"><i class="el-icon-medal" /><strong>暂无正式考核记录</strong><span>管理员发布场次并参加后，这里会按场次列出得分与及格线。</span></div>
          <template v-else>
            <div v-for="row in formalBoard" :key="row.key" class="ex-row">
              <span class="ex-nm">{{ row.examName }}</span>
              <span class="ex-bar"><i :class="row.tone" :style="{ width: Math.max(row.score, 2) + '%' }" /><u class="pass-line" style="left: 60%" /></span>
              <span class="ex-vv">{{ row.scoreText }}</span>
              <span class="session-badge" :class="row.badgeTone">{{ row.badgeText }}</span>
            </div>
            <div class="fact-line">
              <span>已参加 <b>{{ formalStats.joined }}/{{ formalStats.total }}</b> 场</span>
              <span>已通过 <b>{{ formalStats.passed }}</b> 场</span>
              <span>通过率 <b>{{ formalStats.rate === null ? '--' : formalStats.rate + '%' }}</b></span>
            </div>
          </template>
        </section>

        <!-- 05 模拟考核走势 -->
        <section class="i2-card i2-span6">
          <div class="panel-head"><div><span class="section-index">05</span><h2>模拟考核走势</h2></div><span class="card-hint">纵轴 = 百分制正确率</span></div>
          <div v-if="!mockStats.count" class="i2-empty compact"><i class="el-icon-data-line" /><strong>还没有模拟记录</strong><span>做完模拟理论 / 模拟实操后，这里会按次画出正确率走势。</span></div>
          <template v-else>
            <svg viewBox="0 0 320 118" class="trend-svg" role="img" aria-label="模拟考核正确率走势">
              <line x1="6" y1="18" x2="314" y2="18" stroke="#eef1f6" />
              <line x1="6" y1="58" x2="314" y2="58" stroke="#eef1f6" />
              <line x1="6" y1="98" x2="314" y2="98" stroke="#eef1f6" />
              <polyline :points="mockTrendLine" fill="none" stroke="#1764f5" stroke-width="2" />
              <line x1="6" :y1="mockTrend.avgY" x2="314" :y2="mockTrend.avgY" stroke="#f79009" stroke-width="1.5" stroke-dasharray="5 4" />
              <circle v-for="p in mockTrend.points" :key="p.key" :cx="p.x" :cy="p.y" r="3.2" :fill="p.percent >= 60 ? '#12b76a' : '#1764f5'" />
              <text x="6" y="114" fill="#98a2b3" font-size="9">{{ mockTrend.firstLabel }}</text>
              <text x="314" y="114" fill="#98a2b3" font-size="9" text-anchor="end">{{ mockTrend.lastLabel }}</text>
            </svg>
            <div class="fact-line">
              <span>次数 <b>{{ mockStats.count }}</b></span>
              <span>平均正确率 <b>{{ mockStats.accuracy }}%</b></span>
              <span>最高 <b>{{ mockStats.best }}</b> 分</span>
              <span>橙色虚线 = 平均线</span>
            </div>
          </template>
        </section>

        <!-- 06 模拟考核明细 -->
        <section class="i2-card i2-span6">
          <div class="panel-head"><div><span class="section-index">06</span><h2>模拟考核明细</h2></div><span class="card-hint">最近 {{ mockRows.length }} 次</span></div>
          <table v-if="mockRows.length" class="mock-table">
            <thead><tr><th>时间</th><th>题量</th><th>答对</th><th class="right">正确率</th></tr></thead>
            <tbody>
              <tr v-for="row in mockRows" :key="row.key">
                <td>{{ row.time }}</td>
                <td>{{ row.total }}</td>
                <td>{{ row.score }}</td>
                <td class="right"><b :class="row.percent >= 60 ? 'ok' : 'warn'">{{ row.percent }}%</b></td>
              </tr>
            </tbody>
          </table>
          <div v-else class="note">暂无模拟记录（数据源 <code>/business/practice/my</code>）。</div>
        </section>

        <!-- 07 转正资格（口径与「考核成绩与转正」页完全一致） -->
        <section class="i2-card i2-span8">
          <div class="panel-head"><div><span class="section-index">07</span><h2>转正资格</h2></div><span class="card-hint">{{ qualificationTip }}</span></div>
          <div class="qual-list">
            <div v-for="q in qualification" :key="q.key" class="qual-row" :class="q.pass ? 'is-ok' : 'is-no'">
              <span class="mk">{{ q.pass ? '✓' : '!' }}</span>
              <span class="lb">{{ q.label }}</span>
              <b class="vv">{{ q.value }}</b>
            </div>
          </div>
          <el-button type="text" class="qual-more" @click="go('/assessment/intern/learning#sec-result')">去成绩与转正页 ›</el-button>
        </section>

        <!-- 08 能力画像（保留卡片，维度未定义前只显示「暂无数据」） -->
        <section class="i2-card i2-span4">
          <div class="panel-head"><div><span class="section-index">08</span><h2>能力画像</h2></div><el-button type="text" @click="go('/assessment/intern/portrait')">详情 ›</el-button></div>
          <div class="i2-empty compact"><i class="el-icon-data-analysis" /><strong>暂无数据</strong><span>能力模型维度尚未定义，维度定下来后这里会展示各项得分。</span></div>
        </section>
      </div>

      <!-- 协议与证书条 —— 工作台的**最后一条** -->
      <div class="note band-note">电子证书统一在此查看与下载（考核成绩与转正页不重复展示）；未生成的记录点击不跳空页，而是提示生成条件。</div>
      <section class="record-band" aria-label="协议与证书">
        <span class="record-band-label">协议与证书：</span>
        <button
          v-for="record in internRecords"
          :key="record.key"
          type="button"
          class="record-pill"
          :class="record.tone"
          :title="record.tip"
          @click="openRecord(record)"
        >
          <i :class="record.icon" />
          <span>{{ record.label }}</span>
          <em>{{ record.status }}</em>
        </button>
      </section>
    </template>

    <template v-else>
      <div class="workspace-grid admin-grid">
        <section class="panel task-panel">
          <div class="panel-head"><div><span class="section-index">01</span><h2>待办事项</h2></div><el-button type="text" @click="comingSoon">查看全部</el-button></div>
          <div v-for="item in adminTodos" :key="item.title" class="task-row">
            <i class="task-dot" :class="item.tone" /><div><b>{{ item.title }}</b><p>{{ item.description }}</p></div><el-tag size="mini" :type="item.tag">{{ item.status }}</el-tag><el-button type="text" size="mini" @click="item.path ? go(item.path) : comingSoon()">去处理</el-button>
          </div>
        </section>
        <section class="panel scope-panel">
          <div class="panel-head"><div><span class="section-index">02</span><h2>{{ isDeptAdmin ? '部门概况' : '组织概况' }}</h2></div><el-tag size="mini" type="success">实时</el-tag></div>
          <div v-for="item in adminScope" :key="item.label" class="scope-row"><span>{{ item.label }}</span><b>{{ item.value }}</b><small>{{ item.hint }}</small></div>
        </section>
      </div>
      <section class="admin-actions">
        <div class="section-title"><div><span class="section-index">03</span><h2>常用功能</h2></div></div>
        <div class="entry-grid">
          <button v-for="entry in adminEntries" :key="entry.title" type="button" class="entry" @click="entry.path ? go(entry.path) : comingSoon()"><i :class="entry.icon" /><span><b>{{ entry.title }}</b><small>{{ entry.description }}</small></span><em>进入</em></button>
        </div>
      </section>
    </template>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { listLearningCourses } from '@/api/business/learning'
import { myPracticeRecords } from '@/api/business/practice'
import { myExamList } from '@/api/business/exam'
import { getUserProfile } from '@/api/system/user'
import { learningSummary } from '@/utils/learningPreview'

export default {
  name: 'Index',
  data() {
    return {
      learningLoading: false,
      previewCourses: [],
      learningOverview: { progress: null, courseCount: 0, completedCourses: 0, learningCourses: 0, completedItems: 0, itemCount: 0, lastStudyTime: '尚未开始' },
      /** 考核类真数据（2026-09-22 起接真 —— 这几块原先都是硬编码示例值） */
      practiceRecords: [],
      myFormalExams: [],
      entryDate: null
    }
  },
  computed: {
    ...mapGetters(['name', 'roles', 'nickName', 'userStatus', 'protocolStatus', 'mentorName', 'mentorPhone', 'deptName']),
    isPre() { return this.roles.indexOf('PRE_TRAINEE') > -1 },
    isFormal() { return this.roles.indexOf('FORMAL_TRAINEE') > -1 },
    isIntern() { return this.isPre || this.isFormal },
    isDeptAdmin() { return this.roles.indexOf('DEPT_ADMIN') > -1 },
    eyebrow() { return this.isIntern ? 'INTERNSHIP WORKSPACE' : (this.isDeptAdmin ? 'DEPARTMENT WORKSPACE' : 'MANAGEMENT WORKSPACE') },
    title() { return this.isIntern ? `${this.nickName || '实习生'}的工作台` : (this.isDeptAdmin ? `${this.deptName || '部门'}工作台` : '全局工作台') },
    subtitle() { return this.isIntern ? (this.isFormal ? '在线学习与历史培养记录' : '学习任务、考核安排与成长进度') : (this.isDeptAdmin ? '注册审核、培养进度与考核批阅' : '组织培养、考核运营与规则执行') },
    messagePath() { return '/messages' },
    mentorText() { return this.mentorName ? `导师：${this.mentorName}${this.mentorPhone ? ' · ' + this.mentorPhone : ''}` : '导师待登记' },
    learningProgress() { return this.learningOverview.progress === null ? 0 : this.learningOverview.progress },
    learningGap() { return Math.max(0, 70 - this.learningProgress) },
    // —— 设计稿 i2 新增：无后端接口的块一律示例数据并在界面标注「示例」，不用 0 占位 ——
    /**
     * 入职天数：由 /system/user/profile 的 createTime 算（2026-09-22 起为真值）
     * 取不到日期 → null，模板显示「—」（不留假数）
     */
    onboardDays() {
      if (!this.entryDate) return null
      const t = new Date(String(this.entryDate).replace(/-/g, '/')).getTime()
      if (isNaN(t)) return null
      return Math.max(0, Math.floor((Date.now() - t) / 86400000))
    },
    // ============ 学习类真数据（2026-09-23 工作台改版）============
    /** 全部已发布课程里的学习单项（后端 fillLearningContent 会把 chapters[].items 连进度一起下发） */
    allItems() {
      const rows = []
      ;(this.previewCourses || []).forEach(course => {
        (course.chapters || []).forEach(chapter => {
          (chapter.items || []).forEach(item => rows.push(Object.assign({ courseId: course.id, courseName: course.courseName, isRequired: course.isRequired }, item)))
        })
      })
      return rows
    },
    /** 分类型完成情况（只统计视频 / 文档；章节测试已下线，不再有其它类型） */
    itemStats() {
      const stat = { video: { done: 0, total: 0 }, doc: { done: 0, total: 0 } }
      this.allItems.forEach(item => {
        const bucket = item.itemType === 'VIDEO' ? stat.video : (item.itemType === 'DOC' ? stat.doc : null)
        if (!bucket) return
        bucket.total += 1
        if (item.status === 'DONE') bucket.done += 1
      })
      return stat
    },
    /** 已学秒数：study_record.study_duration 累加（真数，不是估算） */
    learnedSeconds() {
      return this.allItems.reduce((sum, item) => sum + Number(item.studyDuration || 0), 0)
    },
    /**
     * 预计总时长（秒）：视频优先取上传时探测的 mediaSeconds（文件真实时长），
     * 没有才退回人工填的 duration（分钟）—— 与学习页的时长口径一致。
     */
    expectedSeconds() {
      return this.allItems.reduce((sum, item) => sum + this.itemSeconds(item), 0)
    },
    /** 待补短板：未完成项里进度最低的一项（进度相同时优先必修） */
    weakItem() {
      const pending = this.allItems.filter(item => item.status !== 'DONE')
      if (!pending.length) return null
      return pending.slice().sort((a, b) => Number(a.progress || 0) - Number(b.progress || 0) || Number(b.isRequired || 0) - Number(a.isRequired || 0))[0]
    },
    /** 课程行：未完成项多的排前面（比原来的「按完成率倒序」更能指出该补哪门） */
    courseRows() {
      return (this.previewCourses || []).slice().map(course => Object.assign({}, course, {
        cls: course.progress === 100 ? 'done' : (course.progress < 50 ? 'low' : '')
      })).sort((a, b) => (Number(b.itemCount) - Number(b.completedItems)) - (Number(a.itemCount) - Number(a.completedItems)) || Number(a.progress) - Number(b.progress))
    },
    /** 01 里的那行数据文案：进度最低项（全部完成时给明确结论，不写建议） */
    weakestItemLabel() {
      if (!this.weakItem) return '全部学习项已完成'
      return this.weakItem.courseName + ' · ' + this.weakItem.itemTitle + '（' + Number(this.weakItem.progress || 0) + '%）'
    },
    /** 已学 ÷ 预计（预计为 0 时返回 0，不编数） */
    durationRatio() {
      const expected = this.expectedSeconds
      return expected ? Math.round(this.learnedSeconds * 100 / expected) : 0
    },
    /**
     * 02 学习项完成矩阵：每门课一行，一格 = 一个学习单项
     * 颜色只表达状态（已完成 / 学习中 / 未开始）
     */
    matrixRows() {
      return (this.previewCourses || []).map(course => {
        const items = (course.chapters || []).reduce((all, chapter) => all.concat(chapter.items || []), [])
        return {
          id: course.id,
          courseName: course.courseName,
          isRequired: Number(course.isRequired) === 1,
          done: items.filter(item => item.status === 'DONE').length,
          total: items.length,
          cells: items.map(item => {
            const statusCls = item.status === 'DONE' ? 'is-done' : (item.status === 'IN_PROGRESS' ? 'is-doing' : 'is-todo')
            return {
              key: item.id,
              cls: statusCls,
              title: [item.itemTitle,
                item.itemType === 'VIDEO' ? '视频' : (item.itemType === 'DOC' ? '文档' : '自测'),
                this.durationText(this.itemSeconds(item)),
                item.status === 'DONE' ? '已完成' : '进度 ' + Number(item.progress || 0) + '%'].join(' · ')
            }
          })
        }
      })
    },
    /** 03 的公共刻度：课程条与类型条共用同一个最大值，长短才能横向比较 */
    durationScale() {
      const seconds = []
      ;(this.previewCourses || []).forEach(course => {
        const items = (course.chapters || []).reduce((all, chapter) => all.concat(chapter.items || []), [])
        seconds.push(items.reduce((sum, item) => sum + Number(item.studyDuration || 0), 0))
        seconds.push(items.reduce((sum, item) => sum + this.itemSeconds(item), 0))
      })
      this.allItems.filter(item => item.itemType === 'VIDEO' || item.itemType === 'DOC').forEach(item => {
        seconds.push(Number(item.studyDuration || 0), this.itemSeconds(item))
      })
      return Math.max(1, ...seconds)
    },
    /** 03 上半块：每门课的「已学 / 预计」 */
    durationRows() {
      const scale = this.durationScale
      return (this.previewCourses || []).map(course => {
        const items = (course.chapters || []).reduce((all, chapter) => all.concat(chapter.items || []), [])
        const learned = items.reduce((sum, item) => sum + Number(item.studyDuration || 0), 0)
        const expected = items.reduce((sum, item) => sum + this.itemSeconds(item), 0)
        return {
          id: course.id,
          courseName: course.courseName,
          learned,
          expected,
          learnedPct: Math.round(learned * 100 / scale),
          expectedPct: Math.round(expected * 100 / scale)
        }
      })
    },
    /** 03 下半块：按类型合计（视频 / 文档） */
    durationTypeRows() {
      const scale = this.durationScale
      return ['VIDEO', 'DOC'].map(type => {
        const items = this.allItems.filter(item => item.itemType === type)
        const learned = items.reduce((acc, item) => acc + Number(item.studyDuration || 0), 0)
        const expected = items.reduce((acc, item) => acc + this.itemSeconds(item), 0)
        return {
          id: 'type-' + type,
          courseName: type === 'VIDEO' ? '视频合计' : '文档合计',
          learned,
          expected,
          learnedPct: Math.round(learned * 100 / scale),
          expectedPct: Math.round(expected * 100 / scale)
        }
      })
    },
    /** 05 模拟走势：每次成绩折算百分制后映射到 320×118 的 SVG 坐标（纵轴 0~100 → y 98~18） */
    mockTrend() {
      const list = (this.practiceRecords || []).slice().sort((a, b) => String(a.createTime || '').localeCompare(String(b.createTime || '')))
      const points = list.map((r, i) => {
        const total = Number(r.totalCount) || 0
        const percent = total ? Math.round(Number(r.score || 0) * 100 / total) : 0
        return {
          key: r.id || 'p' + i,
          x: list.length === 1 ? 160 : Math.round(6 + i * 308 / (list.length - 1)),
          y: Math.round(98 - Math.min(percent, 100) * 0.8),
          percent,
          date: String(r.createTime || '').slice(5, 10)
        }
      })
      const avg = this.mockStats.accuracy === null ? 0 : this.mockStats.accuracy
      return {
        points,
        avgY: Math.round(98 - Math.min(avg, 100) * 0.8),
        firstLabel: points.length ? points[0].date : '',
        lastLabel: points.length ? points[points.length - 1].date : ''
      }
    },
    mockTrendLine() {
      return this.mockTrend.points.map(p => p.x + ',' + p.y).join(' ')
    },
    /** 06 模拟明细：最近 8 次，新的在上 */
    mockRows() {
      return (this.practiceRecords || []).slice()
        .sort((a, b) => String(b.createTime || '').localeCompare(String(a.createTime || '')))
        .slice(0, 8)
        .map((r, i) => {
          const total = Number(r.totalCount) || 0
          return {
            key: r.id || 'm' + i,
            time: String(r.createTime || '').slice(5, 16),
            total,
            score: Number(r.score || 0),
            percent: total ? Math.round(Number(r.score || 0) * 100 / total) : 0
          }
        })
    },
    /**
     * 04 正式考核成绩：一场一行，得分按满分 100 画条 + 60 分及格线标记
     * 只列真有答卷的场次（sheet 非空）；没参加的场次去「正式考核」栏看
     */
    formalBoard() {
      return (this.myFormalExams || []).filter(exam => exam.sheet).slice()
        .sort((a, b) => String(this.examTimeOf(b)).localeCompare(String(this.examTimeOf(a))))
        .slice(0, 8)
        .map(exam => {
          const published = exam.sheet.status === 'PUBLISHED'
          const score = published && exam.sheet.finalScore != null ? Math.round(Number(exam.sheet.finalScore)) : 0
          const passed = published && this.isExamPassed(exam)
          return {
            key: 'e' + exam.examId,
            examName: exam.examName + '（' + (exam.examType === 'PRACTICAL' ? '实操' : '理论') + '）',
            score: Math.max(0, Math.min(score, 100)),
            tone: passed ? 'g' : 'warn',
            scoreText: published ? score + ' 分' : '--',
            badgeText: published ? (passed ? '已通过' : '未通过') : '批阅中',
            badgeTone: published ? (passed ? 'ok' : 'warn') : 'info'
          }
        })
    },
    /**
     * 模拟战绩：/business/practice/my 本人逐场记录
     * · 分数 = 答对题数，满分 = 该场题数 ⇒ 百分制 = score / totalCount × 100
     * · 该接口没有「理论/实操」字段（moduleName / bankName 是题库维度）⇒ 不硬拆类型，避免编数据
     */
    mockStats() {
      const list = (this.practiceRecords || []).slice()
      if (!list.length) return { count: 0, accuracy: null, best: null, recent: [] }
      let ok = 0
      let all = 0
      let best = 0
      list.forEach(r => {
        ok += Number(r.correctCount || 0)
        all += Number(r.totalCount || 0)
        best = Math.max(best, Number(r.score || 0))
      })
      const sorted = list.sort((a, b) => String(a.createTime || '').localeCompare(String(b.createTime || '')))
      const startIndex = Math.max(0, sorted.length - 5)
      const recent = sorted.slice(-5).map((r, i) => {
        const total = Number(r.totalCount) || 0
        return {
          key: r.id || 'p' + i,
          score: Number(r.score || 0),
          label: '第 ' + (startIndex + i + 1) + ' 次',
          percent: total ? Math.round(Number(r.score || 0) * 100 / total) : 0
        }
      })
      return { count: list.length, accuracy: all ? Math.round(ok * 1000 / all) / 10 : null, best, recent }
    },
    /** 正式考核战绩：sheet 为 null = 没参加过 */
    formalStats() {
      const list = this.myFormalExams || []
      const joined = list.filter(e => e.sheet).length
      const passed = list.filter(e => this.isExamPassed(e)).length
      return { total: list.length, joined, passed, rate: list.length ? Math.round(passed * 100 / list.length) : null }
    },
    /**
     * 转正资格：与「考核成绩与转正」页的 checklist 完全同口径（PASS_LINE = 70）
     * ① 学习完成率 ≥ 70% ② 正式考核已通过（理论 × 0.4 + 实操 × 0.6 综合 ≥ 60）
     * ③ 无未完成必修项 ④ 保密协议已签署
     */
    qualification() {
      const theory = this.latestExamScore('THEORY')
      const practical = this.latestExamScore('PRACTICAL')
      const total = theory != null && practical != null ? Math.round((theory * 0.4 + practical * 0.6) * 10) / 10 : null
      const required = (this.previewCourses || []).filter(course => Number(course.isRequired) === 1)
      const requiredDone = required.filter(course => course.progress === 100).length
      const signed = Number(this.protocolStatus) === 1
      return [
        { key: 'study', pass: this.learningProgress >= 70, label: '学习完成率 ≥ 70%', value: '当前 ' + this.learningProgress + '%' },
        { key: 'exam', pass: total != null && total >= 60, label: '正式考核已通过', value: total == null ? '暂无综合成绩' : '综合 ' + total + ' 分' },
        { key: 'required', pass: required.length > 0 && requiredDone === required.length, label: '无未完成必修项', value: required.length ? requiredDone + ' / ' + required.length + ' 门' : '暂无可修课程' },
        { key: 'protocol', pass: signed, label: '保密协议已签署', value: signed ? '已签署' : '待签署' }
      ]
    },
    qualificationTip() {
      const miss = this.qualification.filter(q => !q.pass).length
      return miss ? '还差 ' + miss + ' 项' : '四项已全部满足'
    },
    nextCourse() {
      return this.previewCourses.find(course => Number(course.isRequired) === 1 && course.progress < 100)
        || this.previewCourses.find(course => course.progress < 100)
        || this.previewCourses[0]
    },
    metrics() {
      if (this.isFormal) return [
        { label: '培养状态', value: '已转正', hint: '正式实习生', tone: 'green', icon: 'el-icon-circle-check' }, { label: '课程完成率', value: this.learningProgress + '%', hint: '已发布课程仍可学习', tone: 'blue', icon: 'el-icon-reading' }, { label: '综合成绩', value: '88 分', hint: '历史考核已通过', tone: 'violet', icon: 'el-icon-data-analysis' }, { label: '未读消息', value: '2 条', hint: '近期业务通知', tone: 'orange', icon: 'el-icon-message' }
      ]
      if (this.isPre) return [
        { label: '待学课程', value: this.previewCourses.filter(course => course.progress < 100).length + ' 门', hint: `共 ${this.learningOverview.courseCount} 门已发布课程`, tone: 'orange', icon: 'el-icon-time' }, { label: '课程完成率', value: this.learningProgress + '%', hint: this.learningGap ? `距考核资格还差 ${this.learningGap}%` : '已满足考核资格', tone: 'blue', icon: 'el-icon-reading' }, { label: '理论考试', value: '未开始', hint: this.learningGap ? '等待满足学习资格' : '已达到学习门槛', tone: 'violet', icon: 'el-icon-edit-outline' }, { label: '能力画像', value: '72', hint: '当前阶段综合值', tone: 'green', icon: 'el-icon-data-line' }
      ]
      if (this.isDeptAdmin) return [
        { label: '待审核申请', value: '5 人', hint: '注册申请', tone: 'orange', icon: 'el-icon-user' }, { label: '培养中', value: '18 人', hint: '本部门实习生', tone: 'blue', icon: 'el-icon-s-custom' }, { label: '待批阅', value: '8 份', hint: '实践考核提交物', tone: 'red', icon: 'el-icon-document-checked' }, { label: '本期通过率', value: '86%', hint: '已发布考核结果', tone: 'green', icon: 'el-icon-data-line' }
      ]
      return [
        { label: '业务部门', value: '5 个', hint: '固定培养范围', tone: 'blue', icon: 'el-icon-office-building' }, { label: '在培实习生', value: '68 人', hint: '全组织统计', tone: 'green', icon: 'el-icon-user' }, { label: '考核安排', value: '7 场', hint: '本期已发布', tone: 'violet', icon: 'el-icon-date' }, { label: '异常事项', value: '3 项', hint: '需要管理员关注', tone: 'red', icon: 'el-icon-warning-outline' }
      ]
    },
    internRecords() {
      const signed = Number(this.protocolStatus) === 1
      const certified = this.isFormal
      // 注：「签署凭证」这一项已按需求删除（2026-09-23），协议与证书条只留两项。
      // 签名图本身没有丢 —— 点「保密协议」进 /assessment/intern/agreements，
      // 那一页右侧的「签署凭证」卡片仍然展示本人手写签名、凭证编号与签署时间。
      return [
        {
          key: 'agreement',
          label: '保密协议',
          icon: 'el-icon-lock',
          status: signed ? '已签署' : '待签署',
          tone: signed ? 'is-ready' : 'is-pending',
          path: '/assessment/intern/agreements',
          tip: signed ? '协议已签署，可查看协议全文与签署记录' : '请先完成首次登录保密协议签署'
        },
        {
          key: 'certificate',
          label: '电子证书',
          icon: 'el-icon-medal',
          status: certified ? '已获得' : '未获得',
          tone: certified ? 'is-ready' : 'is-idle',
          path: '/assessment/intern/agreements',
          tip: certified ? '证书已生成，可在线查看与下载（考核成绩与转正页也有一句去向指引）' : '考核通过并经部门管理员审批转正后生成电子证书'
        }
      ]
    },
    adminTodos() { return this.isDeptAdmin ? [{ title: '注册申请待审核', description: '5 条本部门申请等待处理', status: '待审核', tag: 'warning', tone: 'orange', path: '/assessment/department/register-review' }, { title: '草稿课程待完善', description: '补充章节和学习资料后即可发布', status: '待处理', tag: 'primary', tone: 'blue', path: '/assessment/department/courses' }, { title: '实践考核待批阅', description: '8 份提交物等待人工确认', status: '待批阅', tag: 'danger', tone: 'red', path: '/assessment/department/grading' }, { title: '阶段评价待补充', description: '3 名实习生画像信息待完善', status: '待处理', tag: 'primary', tone: 'blue', path: '/assessment/department/students' }] : [{ title: '本期考核安排待确认', description: '跨部门考试范围与时间需要复核', status: '待处理', tag: 'warning', tone: 'orange', path: '/assessment/manage/schedule' }, { title: '角色权限变更检查', description: '核对四类业务角色菜单范围', status: '检查中', tag: 'primary', tone: 'blue', path: '/assessment/system/role-permission' }, { title: '异常培养记录', description: '3 条记录需要管理员关注', status: '异常', tag: 'danger', tone: 'red', path: '/assessment/system/audit-log' }] },
    adminScope() { return this.isDeptAdmin ? [{ label: '预备实习生', value: '12 人', hint: '学习考核中' }, { label: '正式实习生', value: '6 人', hint: '保留历史档案' }, { label: '待分配导师', value: '3 人', hint: '审核后补充' }, { label: '学习达标', value: '14 人', hint: '可参加考核' }] : [{ label: '交付部门', value: '15 人', hint: '实施实习生' }, { label: '开发部门', value: '18 人', hint: '开发实习生' }, { label: '设计部门', value: '12 人', hint: '设计实习生' }, { label: '质检 / 建模', value: '23 人', hint: '两部门合计' }] },
    adminEntries() { return this.isDeptAdmin ? [{ title: '注册审核', description: '审核本部门申请并登记导师', icon: 'el-icon-user', path: '/assessment/department/register-review' }, { title: '课程管理', description: '维护岗位课程、章节和学习资料', icon: 'el-icon-reading', path: '/assessment/department/courses' }, { title: '实习生管理', description: '查看培养状态与学习进度', icon: 'el-icon-s-custom', path: '/assessment/department/students' }, { title: '实习批阅', description: '复核实践提交并发布成绩', icon: 'el-icon-edit-outline', path: '/assessment/department/grading' }, { title: '消息中心', description: '查看业务通知与处理提醒', icon: 'el-icon-message', path: '/assessment/department/messages' }] : [{ title: '考核认证管理', description: '考试、课程、题库与批阅流程', icon: 'el-icon-finished', path: '/assessment/manage/overview' }, { title: '组织岗位', description: '五部门与岗位绑定关系', icon: 'el-icon-office-building', path: '/assessment/system/organization' }, { title: '角色权限', description: '四类业务角色权限边界', icon: 'el-icon-lock', path: '/assessment/system/role-permission' }, { title: '审计日志', description: '关键业务操作留痕', icon: 'el-icon-document', path: '/assessment/system/audit-log' }] }
  },
  created() {
    this.loadLearningPreview()
    this.loadExamStats()
  },
  activated() {
    this.loadLearningPreview()
    this.loadExamStats()
  },
  methods: {
    /**
     * 工作台考核类真数据（2026-09-22 起接真 —— 这三个接口都是实习生端现成的，无需新后端）
     * · 模拟正确率：/business/practice/my  本人逐场记录（correct_count / total_count 汇总）
     * · 考核成绩  ：/business/answer-sheet/my?examMode=FORMAL  本人正式答卷
     * · 入职天数  ：/system/user/profile 的 createTime（取不到就显示「—」，不留假数）
     */
    loadExamStats() {
      if (!this.isIntern) return
      myPracticeRecords().then(res => {
        this.practiceRecords = res.data || res.rows || []
      }).catch(() => { this.practiceRecords = [] })
      myExamList('FORMAL').then(res => {
        this.myFormalExams = res.data || []
      }).catch(() => { this.myFormalExams = [] })
      getUserProfile().then(res => {
        const u = (res && res.data) || {}
        this.entryDate = u.createTime || null
      }).catch(() => { this.entryDate = null })
    },
    loadLearningPreview() {
      if (!this.isIntern) return
      this.learningLoading = true
      listLearningCourses().then(response => {
        this.previewCourses = (response.data || []).map(course => Object.assign({
          chapters: [],
          chapterCount: 0,
          itemCount: 0,
          completedItems: 0,
          duration: 0,
          progress: 0,
          lastStudyTime: '尚未开始'
        }, course, { lastStudyTime: course.lastStudyTime || '尚未开始' }))
        this.learningOverview = learningSummary(this.previewCourses)
      }).catch(() => {
        this.previewCourses = []
        this.learningOverview = learningSummary([])
      }).finally(() => {
        this.learningLoading = false
      })
    },
    /** 学习项预计秒数：视频优先用上传时探测的真实时长，其次人工填的分钟 */
    itemSeconds(item) {
      const media = Number((item && item.mediaSeconds) || 0)
      if (item && item.itemType === 'VIDEO' && media > 0) return media
      return Number((item && item.duration) || 0) * 60
    },
    /** 秒 → 「35 分」/「1 小时 12 分」；null 才显示「--」（0 秒是真数据，显示 0 分） */
    durationText(seconds) {
      if (seconds === null || seconds === undefined) return '--'
      const minutes = Math.round(Number(seconds || 0) / 60)
      if (minutes < 1) return '0 分'
      if (minutes < 60) return minutes + ' 分'
      const hours = Math.floor(minutes / 60)
      const rest = minutes % 60
      return hours + ' 小时' + (rest ? ' ' + rest + ' 分' : '')
    },
    /** 某类正式考核最近一次已出分的成绩（未出分返回 null） */
    latestExamScore(type) {
      const hit = (this.myFormalExams || []).filter(e => e.examType === type && e.sheet && e.sheet.status === 'PUBLISHED' && e.sheet.finalScore != null).slice(-1)[0]
      return hit ? Number(hit.sheet.finalScore) : null
    },
    /** 场次时间（用于排序）：优先发布时间，其次截止 / 开始时间 */
    examTimeOf(exam) {
      return (exam && (exam.publishedAt || exam.endTime || exam.startTime)) || ''
    },
    /** 单场是否通过：有 passFlag 用 passFlag，否则拿最终分比及格线 */
    isExamPassed(exam) {
      if (!exam || !exam.sheet || exam.sheet.status !== 'PUBLISHED') return false
      if (exam.sheet.passFlag != null) return Number(exam.sheet.passFlag) === 1
      const line = exam.passLine == null ? 60 : Number(exam.passLine)
      return exam.sheet.finalScore != null && Number(exam.sheet.finalScore) >= line
    },
    openLearningCourse(course) {
      this.go('/assessment/intern/learning/course/' + course.id)
    },
    go(path) { this.$router.push(path) },
    openRecord(record) {
      // 未生成/待处理的记录不跳转，先说明原因，避免点进空页面。
      if (record.tone === 'is-idle' || record.tone === 'is-pending') {
        this.$modal.msgInfo(record.tip)
        return
      }
      this.go(record.path)
    },
    comingSoon() { this.$modal.msgInfo('功能开发中') },
    refresh() {
      this.loadLearningPreview()
      this.$modal.msgSuccess('工作台数据刷新中')
    }
  }
}
</script>

<style lang="scss" scoped>
.workspace-page { min-height: calc(100vh - 50px); padding: 22px; color: #202b3c; background: #f3f5f8; }
.workspace-head, .head-actions, .identity-band, .identity-main, .identity-facts, .panel-head, .panel-head > div, .section-title, .section-title > div, .task-row, .record-row, .ability-row, .metric-label, .quick-nav { display: flex; align-items: center; }
.workspace-head { justify-content: space-between; gap: 18px; margin-bottom: 16px; }.eyebrow { color: #1764f5; font-size: 11px; font-weight: 600; letter-spacing: 0; }.workspace-head h1 { margin: 7px 0 5px; font-size: 24px; letter-spacing: 0; }.workspace-head p { margin: 0; color: #667085; }.head-actions { gap: 8px; }
.identity-band { justify-content: space-between; gap: 20px; margin-bottom: 14px; padding: 17px 19px; border-left: 4px solid #1764f5; background: #fff; box-shadow: 0 5px 18px rgba(31, 47, 70, .04); }.identity-main { gap: 13px; }.identity-icon { display: flex; width: 44px; height: 44px; align-items: center; justify-content: center; border-radius: 6px; color: #fff; background: #1764f5; font-size: 20px; }.identity-main small, .identity-main strong, .identity-main p { display: block; }.identity-main small { color: #8a94a3; }.identity-main strong { margin: 3px 0; font-size: 17px; }.identity-main p { margin: 0; color: #667085; font-size: 12px; }.identity-facts { gap: 34px; }.identity-facts div { min-width: 90px; }.identity-facts span, .identity-facts b { display: block; }.identity-facts span { margin-bottom: 5px; color: #8a94a3; font-size: 12px; }.identity-facts b { font-size: 14px; }.is-success { color: #168561; }.is-warning { color: #c27516; }
.metric-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin-bottom: 14px; }.metric { min-height: 112px; padding: 15px 16px; border: 1px solid #e4e9f0; border-top: 3px solid #1764f5; border-radius: 6px; background: #fff; }.metric.orange { border-top-color: #d78a2c; }.metric.green { border-top-color: #23966f; }.metric.violet { border-top-color: #8055a7; }.metric.red { border-top-color: #c95151; }.metric-label { gap: 7px; color: #667085; font-size: 12px; }.metric-label i { font-size: 15px; }.metric strong { display: block; margin: 12px 0 6px; font-size: 24px; }.metric small { color: #7a8694; }
.workspace-grid { display: grid; grid-template-columns: minmax(0, 1.45fr) minmax(300px, .85fr); gap: 14px; margin-bottom: 14px; }.panel { min-width: 0; padding: 16px; border: 1px solid #e4e9f0; border-radius: 6px; background: #fff; }.panel-head { justify-content: space-between; gap: 12px; margin-bottom: 9px; }.panel-head > div, .section-title > div { gap: 9px; }.panel-head h2, .section-title h2 { margin: 0; font-size: 16px; }.section-index { color: #1764f5; font-size: 11px; font-weight: 700; }.task-row { min-height: 64px; gap: 11px; border-bottom: 1px solid #edf0f4; }.task-row:last-child { border-bottom: 0; }.task-dot { flex: 0 0 auto; width: 8px; height: 8px; border-radius: 50%; background: #1764f5; }.task-dot.orange { background: #d78a2c; }.task-dot.red { background: #c95151; }.task-dot.green { background: #23966f; }.task-dot.gray { background: #98a2b3; }.task-row > div { flex: 1; min-width: 0; }.task-row b, .task-row p { display: block; }.task-row p { margin: 5px 0 0; color: #7a8694; font-size: 12px; }.progress-row { margin: 13px 0 18px; }.progress-row > div { display: flex; justify-content: space-between; margin-bottom: 7px; color: #475467; font-size: 13px; }.progress-row b { color: #667085; }
.learning-band { display: grid; grid-template-columns: minmax(260px, .75fr) minmax(0, 1.65fr); gap: 22px; margin-bottom: 14px; padding: 20px; border: 1px solid #dce5f1; border-left: 4px solid #1764f5; background: #fff; }.band-copy h2 { margin: 8px 0; font-size: 19px; }.band-copy p { margin: 0 0 18px; color: #667085; line-height: 1.6; }.course-list { display: grid; gap: 8px; }.course-row { display: flex; min-width: 0; align-items: center; gap: 12px; padding: 10px 12px; border: 1px solid #e6ebf1; border-radius: 4px; color: inherit; text-align: left; background: #fafbfd; cursor: pointer; }.course-row:hover { border-color: #a9c8f7; background: #f5f9ff; }.course-icon { display: flex; width: 36px; height: 36px; flex: 0 0 auto; align-items: center; justify-content: center; border-radius: 4px; color: #fff; background: #2878c7; }.course-icon.green { background: #23966f; }.course-icon.orange { background: #d78a2c; }.course-row > span:nth-child(2) { flex: 1; min-width: 0; }.course-row b, .course-row small { display: block; }.course-row b { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.course-row small { margin-top: 4px; color: #7a8694; }.course-row em { color: #1764f5; font-size: 12px; font-style: normal; }
.learning-band { display: block; padding: 20px; }.learning-band-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 16px; }.learning-band-head h2 { margin: 7px 0; font-size: 19px; }.learning-band-head p { margin: 0; color: #667085; line-height: 1.6; }.learning-dashboard-body { display: grid; grid-template-columns: 245px minmax(0, 1fr); gap: 16px; }.learning-summary-panel { display: flex; min-height: 238px; align-items: center; justify-content: center; flex-direction: column; padding: 16px; border: 1px solid #e2e8f0; background: #f8fbff; }.learning-summary-copy { margin-top: 11px; text-align: center; }.learning-summary-copy span, .learning-summary-copy strong, .learning-summary-copy small { display: block; }.learning-summary-copy span { color: #667085; font-size: 11px; }.learning-summary-copy strong { margin-top: 5px; color: #344054; font-size: 13px; }.learning-summary-copy small { margin-top: 5px; color: #98a2b3; font-size: 10px; }.learning-facts { display: flex; width: 100%; justify-content: center; gap: 24px; margin-top: 16px; padding-top: 12px; border-top: 1px solid #e3ebf4; color: #8490a0; font-size: 10px; }.learning-facts span { text-align: center; }.learning-facts b { display: block; margin-bottom: 3px; color: #2878c7; font-size: 16px; }.dashboard-course-list { display: grid; align-content: start; gap: 8px; min-width: 0; }.dashboard-course-row { display: flex; min-width: 0; align-items: center; gap: 12px; padding: 12px; border: 1px solid #e3e9f0; color: inherit; text-align: left; background: #fff; cursor: pointer; }.dashboard-course-row:hover { border-color: #9fc2ef; background: #f7fbff; }.dashboard-course-main { min-width: 0; flex: 1; }.dashboard-course-title { display: flex; align-items: center; gap: 7px; min-width: 0; }.dashboard-course-title b { overflow: hidden; color: #344054; font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }.dashboard-course-intro { display: block; margin-top: 5px; overflow: hidden; color: #8490a0; font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }.dashboard-course-meta { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 8px; color: #98a2b3; font-size: 10px; }.dashboard-course-meta small { font-size: 10px; }.dashboard-course-progress { display: flex; align-items: center; gap: 8px; margin-top: 9px; }.dashboard-course-progress > i { display: block; height: 5px; flex: 1; overflow: hidden; border-radius: 3px; background: #e8eef5; }.dashboard-course-progress > i em { display: block; height: 100%; background: #2878c7; }.dashboard-course-progress b { width: 34px; color: #2878c7; font-size: 11px; text-align: right; }.dashboard-course-action { flex: 0 0 auto; color: #1764f5; font-size: 11px; }.learning-empty { display: flex; min-height: 238px; align-items: center; justify-content: center; gap: 10px; border: 1px dashed #d7e1eb; color: #98a2b3; }.learning-empty > i { color: #b7c5d3; font-size: 28px; }.learning-empty b, .learning-empty small { display: block; }.learning-empty b { color: #667085; font-size: 13px; }.learning-empty small { margin-top: 5px; font-size: 11px; }
.certification-section, .admin-actions { margin-bottom: 14px; padding: 18px; border: 1px solid #e4e9f0; background: #fff; }.section-title { justify-content: space-between; margin-bottom: 13px; }.section-title p { margin: 5px 0 0 28px; color: #7a8694; font-size: 12px; }.entry-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; }.entry-grid.three-columns { grid-template-columns: repeat(3, minmax(0, 1fr)); }.entry { display: flex; min-width: 0; align-items: center; gap: 11px; padding: 15px; border: 1px solid #e2e8f0; border-radius: 5px; color: inherit; text-align: left; background: #fff; cursor: pointer; }.entry:hover { border-color: #9fc2f3; box-shadow: 0 5px 14px rgba(32, 64, 106, .07); }.entry > i { flex: 0 0 auto; color: #1764f5; font-size: 23px; }.entry > span { flex: 1; min-width: 0; }.entry b, .entry small { display: block; }.entry small { margin-top: 5px; overflow: hidden; color: #7a8694; text-overflow: ellipsis; white-space: nowrap; }.entry em { color: #1764f5; font-size: 12px; font-style: normal; }
.records-grid { grid-template-columns: 1fr 1fr; }.record-row { min-height: 52px; gap: 12px; border-bottom: 1px solid #edf0f4; }.record-row:last-child { border-bottom: 0; }.record-row > div { flex: 1; }.record-row b, .record-row span { display: block; }.record-row span { margin-top: 4px; color: #8a94a3; font-size: 11px; }.record-row > strong { width: 48px; text-align: right; font-size: 13px; }.ability-row { gap: 10px; min-height: 36px; }.ability-row > span { width: 65px; color: #667085; font-size: 12px; }.ability-row > div { height: 7px; flex: 1; overflow: hidden; border-radius: 4px; background: #edf1f5; }.ability-row i { display: block; height: 100%; background: #2878c7; }.ability-row b { width: 28px; text-align: right; font-size: 12px; }.quick-nav { flex-wrap: wrap; gap: 8px; color: #667085; font-size: 12px; }.quick-nav > span { margin-right: 4px; }.scope-row { display: grid; grid-template-columns: 1fr auto; gap: 4px 12px; padding: 11px 0; border-bottom: 1px solid #edf0f4; }.scope-row:last-child { border-bottom: 0; }.scope-row b { color: #1764f5; }.scope-row small { grid-column: 1 / -1; color: #8a94a3; }.admin-actions { margin-bottom: 0; }
.record-band { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; margin-top: 14px; padding: 14px 18px; border: 1px solid #e4e9f0; border-radius: 6px; background: #fff; }.record-band-label { color: #344054; font-size: 14px; }.record-pill { display: inline-flex; align-items: center; gap: 7px; min-height: 34px; padding: 0 16px; border: 0; border-radius: 8px; background: #e8f1fd; color: #1764f5; font-size: 14px; cursor: pointer; transition: background .15s; }.record-pill:hover { background: #d7e7fc; }.record-pill > i { font-size: 15px; }.record-pill em { padding: 1px 7px; border-radius: 10px; background: rgba(23, 100, 245, .12); font-size: 11px; font-style: normal; }.record-pill.is-pending { background: #fff4e5; color: #b54708; }.record-pill.is-pending em { background: rgba(181, 71, 8, .12); }.record-pill.is-idle { background: #f2f4f7; color: #98a2b3; }.record-pill.is-idle em { background: rgba(152, 162, 179, .18); }
/* —— 实习生工作台 i2 区块（设计稿 11 块） —— */
.i2-card { min-width: 0; margin-bottom: 14px; padding: 16px 18px; border: 1px solid #e4e9f0; border-radius: 6px; background: #fff; }
.i2-grid { display: grid; grid-template-columns: repeat(12, minmax(0, 1fr)); gap: 14px; }
.i2-grid .i2-card { margin-bottom: 0; }
.i2-span4 { grid-column: span 4; }.i2-span6 { grid-column: span 6; }.i2-span8 { grid-column: span 8; }
.card-hint { color: #98a2b3; font-size: 12px; }
/* 03 趋势 */
/* 05 模拟走势（SVG 折线，纵轴 0~100%） */
.trend-svg { display: block; width: 100%; height: 132px; }
.legend { display: flex; flex-wrap: wrap; gap: 18px; margin-top: 10px; color: #667085; font-size: 12px; }
.legend span { display: inline-flex; align-items: center; gap: 6px; }
.legend i { width: 10px; height: 10px; border-radius: 2px; }
.note { margin-top: 10px; color: #98a2b3; font-size: 11px; line-height: 1.6; }
/* 04 雷达 */
.band-note { margin: 14px 0 0; }
/* ============ 实习生端看板（2026-09-23 改版新增）============ */
.m-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; }
.m-card { min-width: 0; padding: 12px 14px; border: 1px solid #e4e9f0; border-radius: 6px; background: #fbfcfe; }
.m-lb { display: block; color: #667085; font-size: 12px; }
.m-vl { display: block; margin: 7px 0 0; color: #1d2939; font-size: 22px; font-weight: 700; line-height: 1.2; }
.m-vl em { margin-left: 4px; color: #98a2b3; font-size: 12px; font-style: normal; font-weight: 400; }
.m-vl.slim { margin-top: 4px; font-size: 15px; }
.m-vl.ok { color: #168561; }.m-vl.warn { color: #c27516; }
.m-bar { position: relative; display: block; height: 5px; margin-top: 9px; border-radius: 3px; background: #edf1f5; }
.m-bar i { display: block; height: 100%; border-radius: 3px; background: #1764f5; }
.m-bar i.ok { background: #12b76a; }.m-bar i.warn { background: #f79009; }
.m-bar u.gate { position: absolute; top: -3px; width: 2px; height: 11px; background: #f04438; }
.m-ft { display: block; margin-top: 7px; color: #7a8694; font-size: 11px; line-height: 1.5; }
.m-ft.warn { color: #c27516; }.m-ft.ok { color: #168561; }
.session-badge { flex: none; padding: 2px 9px; border-radius: 10px; background: #eef2f6; color: #667085; font-size: 11px; }
.session-badge.ok { color: #1a7a58; background: #e4f5ee; }
.session-badge.info { color: #1764f5; background: #e2ecff; }
.session-badge.warn { color: #b54708; background: #fff4e5; }
.session-badge.muted { color: #98a2b3; background: #f2f4f7; }
.qual-list { display: grid; gap: 8px; }
.qual-row { display: flex; align-items: center; gap: 9px; min-height: 32px; color: #475467; font-size: 12.5px; }
.qual-row .mk { display: flex; width: 18px; height: 18px; flex: 0 0 auto; align-items: center; justify-content: center; border-radius: 50%; background: #f2f4f7; color: #98a2b3; font-size: 11px; }
.qual-row.is-ok .mk { color: #fff; background: #12b76a; }
.qual-row.is-no .mk { color: #fff; background: #f79009; }
.qual-row .vv { margin-left: auto; color: #1d2939; font-size: 12px; font-weight: 600; }
.qual-row.is-no .vv { color: #b54708; }
.qual-more { margin-top: 10px; padding: 0; }
.i2-empty.compact { padding: 22px 10px; }
.i2-span12 { grid-column: span 12; }
/* 卡片底部的事实行（只描述数据，不给建议） */
.fact-line { display: flex; flex-wrap: wrap; gap: 5px 18px; margin-top: 12px; padding-top: 10px; border-top: 1px dashed #e4e9f0; color: #7a8694; font-size: 11.5px; }
.fact-line b { color: #344054; font-weight: 600; }
.fact-line code { padding: 1px 5px; border-radius: 4px; background: #f2f4f7; color: #667085; font-size: 10.5px; }
.req { margin-left: 6px; padding: 0 5px; border-radius: 8px; background: #eef4ff; color: #1764f5; font-size: 10px; font-style: normal; }
/* 02 学习项完成矩阵 */
.matrix { display: grid; gap: 8px; }
.mx-row { display: flex; align-items: center; gap: 12px; }
.mx-nm { width: 200px; flex: 0 0 auto; overflow: hidden; color: #475467; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.mx-cells { display: flex; flex: 1; min-width: 0; flex-wrap: wrap; gap: 5px; }
.mx-cell { width: 22px; height: 22px; border-radius: 4px; background: #e4e9f0; }
.mx-cell.is-done { background: #12b76a; }
.mx-cell.is-doing { background: #f79009; }
.mx-sum { width: 46px; flex: 0 0 auto; color: #667085; font-size: 12px; text-align: right; }
/* 03 学习时长对比（上蓝=已学，下灰=预计） */
.dur-row { display: flex; align-items: center; gap: 10px; min-height: 44px; }
.dur-nm { width: 180px; flex: 0 0 auto; overflow: hidden; color: #475467; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.dur-bars { display: grid; flex: 1; min-width: 0; gap: 4px; }
.dur-bar { height: 7px; overflow: hidden; border-radius: 4px; background: #f2f4f7; }
.dur-bar i { display: block; height: 100%; border-radius: 4px; }
.dur-bar i.learned { background: #1764f5; }
.dur-bar i.expected { background: #cdd6e2; }
.dur-vv { width: 128px; flex: 0 0 auto; color: #667085; font-size: 11px; text-align: right; }
.dur-sep { margin: 6px 0 0; padding-top: 9px; border-top: 1px dashed #e4e9f0; color: #98a2b3; font-size: 11px; }
/* 04 正式考核成绩（得分条 + 60 分及格线） */
.ex-row { display: flex; align-items: center; gap: 10px; min-height: 40px; }
.ex-nm { width: 150px; flex: 0 0 auto; overflow: hidden; color: #475467; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.ex-bar { position: relative; height: 9px; flex: 1; min-width: 0; border-radius: 5px; background: #edf1f5; }
.ex-bar i { display: block; height: 100%; border-radius: 5px; background: #1764f5; }
.ex-bar i.g { background: #12b76a; }
.ex-bar i.warn { background: #f79009; }
.ex-bar u.pass-line { position: absolute; top: -3px; width: 2px; height: 15px; background: #f04438; }
.ex-vv { width: 50px; flex: 0 0 auto; color: #667085; font-size: 12px; text-align: right; }
/* 06 模拟明细表 */
.mock-table { width: 100%; border-collapse: collapse; font-size: 12px; }
.mock-table th { padding: 6px 0; border-bottom: 1px solid #edf0f4; color: #98a2b3; font-size: 11px; font-weight: 400; text-align: left; }
.mock-table td { padding: 7px 0; border-bottom: 1px solid #f2f4f7; color: #475467; }
.mock-table tr:last-child td { border-bottom: 0; }
.mock-table .right { text-align: right; }
.mock-table b.ok { color: #168561; }
.mock-table b.warn { color: #c27516; }
@media (max-width: 1200px) { .i2-span8, .i2-span4, .i2-span6 { grid-column: span 12; }.m-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 760px) { .m-grid { grid-template-columns: 1fr; }.mx-nm, .dur-nm { width: 110px; }.dur-vv { width: 96px; }.ex-nm { width: 96px; } }
@media (max-width: 1000px) { .entry-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }.identity-facts { gap: 16px; } }
@media (max-width: 760px) { .workspace-page { padding: 14px; }.workspace-head, .identity-band { align-items: flex-start; flex-direction: column; }.metric-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }.workspace-grid, .records-grid { grid-template-columns: 1fr; }.learning-dashboard-body { grid-template-columns: 1fr; }.learning-summary-panel { min-height: 0; }.identity-facts { width: 100%; justify-content: space-between; }.entry-grid, .entry-grid.three-columns { grid-template-columns: 1fr; }.task-row { align-items: flex-start; flex-wrap: wrap; padding: 12px 0; }.task-row > div { min-width: calc(100% - 22px); }.task-row .el-button { margin-left: 19px; }.dashboard-course-intro { white-space: normal; line-height: 1.5; } }
@media (max-width: 440px) { .metric-grid { grid-template-columns: 1fr; }.head-actions { width: 100%; }.head-actions .el-button { flex: 1; }.identity-facts { align-items: flex-start; flex-direction: column; gap: 10px; }.identity-facts div { display: flex; width: 100%; justify-content: space-between; }.identity-facts span { margin: 0; } }
/* 无数据空态（2026-09-22：能接真数据的接真，接不到就空着，不留假数） */
.i2-empty { display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 46px 14px; text-align: center; }
.i2-empty i { color: #d0d5dd; font-size: 30px; }
.i2-empty strong { color: #1d2939; font-size: 14px; }
.i2-empty span { color: #8490a0; font-size: 12.5px; line-height: 1.7; }
</style>
