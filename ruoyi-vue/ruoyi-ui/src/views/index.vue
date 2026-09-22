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

    <template v-if="isIntern">
      <!-- 01 身份条 -->
      <section class="i2-card">
        <div class="panel-head"><div><span class="section-index">01</span><h2>我的身份</h2></div><span class="card-hint">入职天数按账号创建时间计算</span><el-button type="text" @click="go('/assessment/intern/portrait')">查看画像 ›</el-button></div>
        <div class="identity">
          <span class="big-ph">{{ (nickName || '实').charAt(0) }}</span>
          <div class="who"><b>{{ nickName || '实习生' }}</b><p>{{ isFormal ? '正式实习生' : '预备实习生' }} · {{ deptName || '所属部门' }} · {{ mentorText }}</p></div>
          <div class="facts">
            <div class="fact"><span>保密协议</span><b :class="Number(protocolStatus) === 1 ? 'ok' : 'warn'">{{ Number(protocolStatus) === 1 ? '已签署' : '待签署' }}</b></div>
            <div class="fact"><span>培养阶段</span><b>{{ isFormal ? '正式 · 已转正' : '预备 → 待转正' }}</b></div>
            <div class="fact"><span>距考核门槛</span><b :class="learningGap ? 'warn' : 'ok'">{{ learningGap ? '还差 ' + learningGap + '%' : '已达到' }}</b></div>
            <div class="fact"><span>入职天数</span><b>{{ onboardDays === null ? '—' : onboardDays + ' 天' }}</b></div>
          </div>
        </div>
      </section>

      <!-- 02 六个 KPI -->
      <div class="kpi-grid">
        <div class="kpi"><div class="kpi-lb"><i class="kpi-dot" style="background:#1764f5" />必修完成率</div><div class="kpi-vl">{{ learningProgress }}<small>%</small></div><div class="bar-mini"><i :style="{ width: learningProgress + '%' }" /></div><div class="kpi-ft" :class="learningGap ? 'warn' : 'up'">{{ learningGap ? '距 70% 门槛还差 ' + learningGap + '%' : '已达到考核门槛' }}</div></div>
        <div class="kpi"><div class="kpi-lb"><i class="kpi-dot" style="background:#12b76a" />累计学习时长</div><div class="kpi-vl">{{ studyHoursTotal === null ? '--' : studyHoursTotal }}<small v-if="studyHoursTotal !== null">小时</small></div><div class="kpi-ft">按周聚合接口就绪后显示，当前不计假数</div></div>
        <div class="kpi"><div class="kpi-lb"><i class="kpi-dot" style="background:#7a5af8" />已完成单项</div><div class="kpi-vl">{{ learningOverview.completedItems }}<small>/ {{ learningOverview.itemCount }}</small></div><div class="kpi-ft">覆盖 {{ learningOverview.courseCount }} 门已发布课程</div></div>
        <div class="kpi"><div class="kpi-lb"><i class="kpi-dot" style="background:#f79009" />待完成课程</div><div class="kpi-vl">{{ pendingCourses.length }}<small>门</small></div><div class="kpi-ft" :class="pendingRequiredCount ? 'warn' : ''">其中必修 {{ pendingRequiredCount }} 门</div></div>
        <div class="kpi"><div class="kpi-lb"><i class="kpi-dot" style="background:#f04438" />模拟正确率</div><div class="kpi-vl">{{ mockAccuracy === null ? '--' : mockAccuracy }}<small v-if="mockAccuracy !== null">%</small></div><div class="kpi-ft">{{ practiceRecords.length ? '基于本人 ' + practiceRecords.length + ' 次模拟记录' : '暂无模拟记录' }}</div></div>
        <div class="kpi hl"><div class="kpi-lb"><i class="kpi-dot" style="background:#1764f5" />能力综合值</div><div class="kpi-vl">--</div><div class="bar-mini"><i class="o" :style="{ width: portraitCompleteness + '%' }" /></div><div class="kpi-ft">能力模型维度待定义后生成（当前不计假数）</div></div>
      </div>

      <div class="i2-grid">
        <!-- 03 学习时长趋势（真数据：近 14 天逐日学习时长；无记录走空态） -->
        <section class="i2-card i2-span8">
          <div class="panel-head">
            <div><span class="section-index">03</span><h2>学习时长趋势</h2></div>
            <el-button type="text" @click="go('/assessment/intern/learning')">在线学习 ›</el-button>
          </div>
          <div v-if="!hasDailyData" class="i2-empty">
            <i class="el-icon-time" /><strong>暂无学习时长记录</strong>
            <span>开始学习课程后，这里会按天显示你的学习时长。</span>
          </div>
          <template v-else>
            <svg viewBox="0 0 900 210" class="trend-svg">
              <g stroke="#eef1f6" stroke-width="1"><line x1="46" y1="20" x2="880" y2="20" /><line x1="46" y1="60" x2="880" y2="60" /><line x1="46" y1="100" x2="880" y2="100" /><line x1="46" y1="140" x2="880" y2="140" /><line x1="46" y1="180" x2="880" y2="180" /></g>
              <g fill="#98a2b3" font-size="11" text-anchor="end">
                <text x="38" y="24">{{ trendMaxHours }}h</text>
                <text x="38" y="64">{{ (trendMaxHours * 0.75).toFixed(1) }}h</text>
                <text x="38" y="104">{{ (trendMaxHours * 0.5).toFixed(1) }}h</text>
                <text x="38" y="144">{{ (trendMaxHours * 0.25).toFixed(1) }}h</text>
                <text x="38" y="184">0</text>
              </g>
              <path :d="trendArea" fill="#e8f1fd" opacity="0.9" />
              <polyline :points="trendPolyline" fill="none" stroke="#1764f5" stroke-width="2.5" stroke-linejoin="round" />
              <g fill="#fff" stroke="#1764f5" stroke-width="2.5"><circle v-for="w in trendWeeks" :key="w.label" :cx="w.x" :cy="w.y" r="4" /></g>
              <rect :x="trendBadge.x" :y="trendBadge.y" width="104" height="26" rx="6" fill="#1764f5" />
              <text :x="trendBadge.x + 52" :y="trendBadge.y + 17" fill="#fff" font-size="12" text-anchor="middle">今日 {{ dailySeries[dailySeries.length - 1].hours }} h</text>
              <g fill="#98a2b3" font-size="11" text-anchor="middle"><text v-for="w in trendWeeks" :key="'lb-' + w.label" :x="w.x" y="200">{{ w.label }}</text></g>
            </svg>
            <div class="legend"><span><i style="background:#1764f5" />每日学习时长</span><span>近 14 天合计 {{ (dailySeries.reduce((s, d) => s + d.hours, 0)).toFixed(1) }} 小时</span></div>
            <div class="note">口径：本人在每个自然日的学习时长之和（<code>study_record.study_duration</code> 按 <code>last_study_time</code> 归日）；没有学习的天记 0。</div>
          </template>
        </section>

        <!-- 04 能力画像雷达 -->
        <section class="i2-card i2-span4">
          <div class="panel-head"><div><span class="section-index">04</span><h2>能力画像</h2></div><el-button type="text" @click="go('/assessment/intern/portrait')">详情 ›</el-button></div>
          <div class="portrait">
            <svg viewBox="0 0 200 190" class="radar-svg">
              <g fill="none" stroke="#eef1f6"><polygon points="100,22 162,84 100,146 38,84" /><polygon points="100,46 135,84 100,122 65,84" /><polygon points="100,70 108,84 100,98 92,84" /></g>
              <g stroke="#e4e9f0" stroke-width="1"><line x1="100" y1="22" x2="100" y2="146" /><line x1="38" y1="84" x2="162" y2="84" /><line x1="62" y1="46" x2="138" y2="122" /><line x1="138" y1="46" x2="62" y2="122" /></g>
              <polygon :points="radarPolygon" fill="#1764f5" fill-opacity="0.16" stroke="#1764f5" stroke-width="2" />
              <circle v-for="(p, i) in radarPoints" :key="i" :cx="p.x" :cy="p.y" r="3.4" :fill="p.measured ? '#1764f5' : '#fff'" :stroke="p.measured ? '#1764f5' : '#98a2b3'" stroke-width="1.6" />
              <g fill="#667085" font-size="11" text-anchor="middle"><text x="100" y="14">学习投入</text><text x="182" y="88">理论掌握</text><text x="18" y="88">规范遵从</text><text x="100" y="162">实践能力</text></g>
              <text v-if="radarPoints[1] && !radarPoints[1].measured" x="182" y="102" fill="#98a2b3" font-size="9" text-anchor="middle">待定义</text>
              <text v-if="radarPoints[2] && !radarPoints[2].measured" x="100" y="176" fill="#98a2b3" font-size="9" text-anchor="middle">待定义</text>
            </svg>
            <div class="portrait-meta">
              <div v-for="dim in portraitDims" :key="dim.name" class="dim-row">
                <span class="nm">{{ dim.name }}</span>
                <span class="bar"><i v-if="dim.value !== null" :style="{ width: dim.value + '%', background: dim.value >= 80 ? '#12b76a' : '#1764f5' }" /><i v-else class="hollow" /></span>
                <span class="vv">{{ dim.value === null ? '-- ' + dim.note : dim.value }}</span>
              </div>
              <div class="completeness">
                <div class="t"><span>数据完整度</span><b>{{ portraitCompleteness }}%（{{ isFormal ? '5 / 5' : '3 / 5' }} 来源）</b></div>
                <div class="bar-mini"><i class="o" :style="{ width: portraitCompleteness + '%' }" /></div>
              </div>
            </div>
          </div>
          <div class="note">能力模型维度尚未定义（<code>ability_dimension</code> 为空）⇒ 此处留空，等维度定下来后再生成。</div>
        </section>

        <!-- 05 课程完成情况 -->
        <section class="i2-card i2-span6">
          <div class="panel-head"><div><span class="section-index">05</span><h2>课程完成情况</h2></div><span class="card-hint">按完成率排序</span></div>
          <div v-loading="learningLoading">
            <button v-for="c in courseBars" :key="c.id" type="button" class="hbar" :class="c.cls" @click="openLearningCourse(c)">
              <span class="nm">{{ c.courseName }}</span>
              <span class="track"><i :style="{ width: Math.max(c.progress, 2) + '%' }" /></span>
              <span class="pc">{{ c.progress }}%</span>
            </button>
            <div v-if="!learningLoading && !courseBars.length" class="note">暂无已发布课程，部门管理员发布适用于当前岗位的课程后将在这里显示。</div>
          </div>
          <div class="note">绿色为已完成，橙色为低于 50% 的短板课程；点击行可直达课程。</div>
        </section>

        <!-- 06 学习时长分布（与 03 同源真数据；无记录走空态） -->
        <section class="i2-card i2-span6">
          <div class="panel-head">
            <div><span class="section-index">06</span><h2>学习时长分布</h2></div>
            <el-button type="text" @click="go('/assessment/intern/learning')">在线学习 ›</el-button>
          </div>
          <div v-if="!hasDailyData" class="i2-empty">
            <i class="el-icon-time" /><strong>暂无学习时长记录</strong>
            <span>有学习记录后这里会按天显示柱状分布。</span>
          </div>
          <template v-else>
            <div class="vchart">
              <div v-for="w in weeklyHours" :key="w.label" class="vcol">
                <span class="bar" :class="{ hi: w.hours > 0 && w.hours === weeklyHoursMax }" :style="{ height: Math.max(Math.round(w.hours / weeklyHoursMax * 100), w.hours > 0 ? 6 : 0) + '%' }">{{ w.hours }}</span>
                <span class="vcol-lb">{{ w.label }}</span>
              </div>
            </div>
            <div class="legend"><span><i style="background:#1764f5" />每日学习时长</span><span>近 14 天合计 {{ (dailySeries.reduce((s, d) => s + d.hours, 0)).toFixed(1) }} 小时</span></div>
          </template>
        </section>

        <!-- 07 考核成绩 -->
        <section class="i2-card i2-span6">
          <div class="panel-head"><div><span class="section-index">07</span><h2>考核成绩</h2></div><el-button type="text" @click="go('/assessment/intern/learning/result')">成绩与转正 ›</el-button></div>
          <div v-if="!hasExamScores" class="i2-empty">
            <i class="el-icon-medal" /><strong>暂无考核成绩</strong>
            <span>做过模拟自测或参加正式考核后，这里会显示你的成绩。</span>
          </div>
          <template v-else>
            <div class="vchart">
              <div v-for="b in examScoreBars" :key="b.label" class="vcol"><span class="bar" :class="b.score === null ? 'none' : 'g'" :style="{ height: (b.score === null ? 8 : Math.max(b.score, 4)) + '%' }">{{ b.score === null ? '--' : b.score }}</span><span class="vcol-lb">{{ b.label }}</span></div>
            </div>
            <div class="legend"><span><i style="background:#12b76a" />模拟自测（不计正式成绩）</span><span><i style="background:#f2f4f7" />未参加 / 未出分</span></div>
          </template>
        </section>

        <!-- 08 培养进度 -->
        <section class="i2-card i2-span6">
          <div class="panel-head"><div><span class="section-index">08</span><h2>培养进度</h2></div><el-button type="text" @click="go('/assessment/intern/learning/result')">成绩与转正 ›</el-button></div>
          <div class="steps">
            <div v-for="(s, i) in trainingSteps" :key="s.label" class="step" :class="s.state">
              <span class="mark">{{ s.state === 'done' ? '✓' : i + 1 }}</span>
              <div class="txt"><b>{{ s.label }}</b><span>{{ s.desc }}</span></div>
            </div>
          </div>
        </section>

        <!-- 09 下一步做什么 -->
        <section class="i2-card i2-span8">
          <div class="panel-head"><div><span class="section-index">09</span><h2>下一步做什么</h2></div><span class="card-hint">按紧急度排序</span></div>
          <div v-for="item in internTodos" :key="item.title" class="todo">
            <span class="tdot" :class="item.tone" />
            <div class="tx"><b>{{ item.title }}</b><span>{{ item.description }}</span></div>
            <el-button size="mini" :type="item.tone === 'blue' ? 'primary' : 'info'" plain @click="go(item.path)">{{ item.action }}</el-button>
          </div>
        </section>

        <!-- 10 公告 -->
        <section class="i2-card i2-span4">
          <div class="panel-head"><div><span class="section-index">10</span><h2>公告</h2></div><el-button type="text" @click="go(messagePath)">全部 ›</el-button></div>
          <button v-for="a in announcements" :key="a.id" type="button" class="notice" @click="go(messagePath)">
            <b>{{ a.title }}<em v-if="a.isTop" class="dsample">置顶</em></b>
            <span>{{ a.publishTime || a.createTime }}</span>
          </button>
          <div v-if="!announcements.length" class="note">暂无公告</div>
          <div v-else class="note">取自 <code>notice</code> 表有效公告（最多 3 条，置顶优先），接口 <code>/business/message/announcements</code>。</div>
        </section>
      </div>

      <!-- 11 协议与证书条 -->
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
      <div class="note band-note">电子证书统一在此查看与下载（考核成绩与转正页不重复展示）；未生成的记录点击不跳空页，而是提示生成条件。</div>
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
import { listLearningCourses, getDailyDuration } from '@/api/business/learning'
import { listAnnouncements } from '@/api/business/message'
import { myPracticeRecords } from '@/api/business/practice'
import { myExamList } from '@/api/business/exam'
import { getUserProfile } from '@/api/system/user'
import { formatLearningDuration, learningSummary } from '@/utils/learningPreview'

export default {
  name: 'Index',
  data() {
    return {
      learningLoading: false,
      previewCourses: [],
      learningOverview: { progress: null, courseCount: 0, completedCourses: 0, learningCourses: 0, completedItems: 0, itemCount: 0, lastStudyTime: '尚未开始' },
      /** 工作台「公告位」：当前有效公告（最多 3 条，置顶优先），来自 /business/message/announcements */
      announcements: [],
      /** 考核类真数据（2026-09-22 起接真 —— 这几块原先都是硬编码示例值） */
      practiceRecords: [],
      myFormalExams: [],
      entryDate: null,
      /** 近 14 天逐日学习时长（真数据；空数组 = 一直没有学习记录） */
      dailyDurations: []
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
    /** 累计学习时长：仍缺「按周聚合学习时长」的后端接口（本轮未接，模板显示「—」，不再用 14.5 这类假数） */
    studyHoursTotal() { return null },
    /**
     * 模拟正确率：本人全部模拟记录的 correct / total 汇总（2026-09-22 起为真值）
     * 无记录 → null（模板显「--」）
     */
    mockAccuracy() {
      const list = this.practiceRecords || []
      let ok = 0
      let all = 0
      list.forEach(r => {
        ok += Number(r.correctCount || 0)
        all += Number(r.totalCount || 0)
      })
      if (!all) return null
      return Math.round(ok * 1000 / all) / 10
    },
    pendingCourses() { return this.previewCourses.filter(course => course.progress < 100) },
    pendingRequiredCount() { return this.pendingCourses.filter(course => Number(course.isRequired) === 1).length },
    courseBars() {
      return this.previewCourses.slice().sort((a, b) => b.progress - a.progress).slice(0, 6).map(course => Object.assign({}, course, {
        cls: course.progress === 100 ? 'done' : (course.progress < 50 ? 'low' : '')
      }))
    },
    /** 近 14 天学习时长序列（真数据；接口只给有记录的天，缺的天补 0 = 当天确实没学） */
    dailySeries() {
      const N = 14
      const map = {}
      ;(this.dailyDurations || []).forEach(d => { map[String(d.date)] = Number(d.seconds) || 0 })
      const out = []
      for (let i = N - 1; i >= 0; i--) {
        const dt = new Date(Date.now() - i * 86400000)
        const p = n => (n < 10 ? '0' + n : '' + n)
        const key = dt.getFullYear() + '-' + p(dt.getMonth() + 1) + '-' + p(dt.getDate())
        const sec = map[key] || 0
        out.push({ label: (dt.getMonth() + 1) + '/' + dt.getDate(), seconds: sec, hours: Math.round(sec / 36) / 100 })
      }
      return out
    },
    /** 序列里是否有学习记录（全 0 → 走空态，而不是画一条 0 线假装"有数据"） */
    hasDailyData() { return (this.dailyDurations || []).length > 0 },
    trendMaxHours() {
      const m = Math.max.apply(null, this.dailySeries.map(d => d.hours))
      return m > 0 ? Math.round(m * 10) / 10 : 0
    },
    /**
     * 趋势线坐标（真数据）：x 均匀分布，y 按本序列最大值归一
     * ⚠️ 口径已变：原设计画「完成率 %」，但**历史完成率没有数据源** ⇒ 改画「每日学习时长（小时）」
     */
    trendWeeks() {
      const list = this.dailySeries
      const maxSec = Math.max(1, ...list.map(d => d.seconds))
      const stepX = list.length > 1 ? 834 / (list.length - 1) : 0
      return list.map((d, i) => ({
        label: d.label,
        value: d.hours,
        x: Math.round(46 + i * stepX),
        y: Math.round(180 - (d.seconds / maxSec) * 160)
      }))
    },
    trendPolyline() { return this.trendWeeks.map(w => w.x + ',' + w.y).join(' ') },
    trendArea() {
      const weeks = this.trendWeeks
      return 'M' + weeks[0].x + ',180 L' + weeks.map(w => w.x + ',' + w.y).join(' L') + ' L' + weeks[weeks.length - 1].x + ',180 Z'
    },
    trendBadge() {
      const last = this.trendWeeks[this.trendWeeks.length - 1]
      return { x: Math.min(Math.max(last.x - 46, 8), 800), y: Math.max(last.y - 40, 8) }
    },
    /**
     * 能力画像维度（2026-09-22：**留空**，不再用假分）
     * ⚠️ 依赖 `ability_dimension`（当前 0 行、维度定义未拍板）⇒ 不写死分数：
     *    雷达只画空心点、各维度条显示「待定义」；维度定下来后再接真数据。
     */
    portraitDims() {
      return [
        { name: '学习投入', value: null, note: '待定义' },
        { name: '理论掌握', value: null, note: '待定义' },
        { name: '实践能力', value: null, note: '待定义' },
        { name: '规范遵从', value: null, note: '待定义' }
      ]
    },
    portraitCompleteness() { return 0 },
    radarPoints() {
      const center = { x: 100, y: 84 }
      const axes = [{ x: 100, y: 22 }, { x: 162, y: 84 }, { x: 100, y: 146 }, { x: 38, y: 84 }]
      return this.portraitDims.map((dim, i) => {
        const ratio = dim.value === null ? 0.42 : dim.value / 100
        return { x: Math.round(center.x + (axes[i].x - center.x) * ratio), y: Math.round(center.y + (axes[i].y - center.y) * ratio), measured: dim.value !== null }
      })
    },
    radarPolygon() { return this.radarPoints.map(p => p.x + ',' + p.y).join(' ') },
    /** 时长分布（与 03 卡同源的真数据，只是展示形式不同） */
    weeklyHours() {
      return this.dailySeries.map(d => ({ label: d.label, hours: d.hours }))
    },
    weeklyHoursMax() {
      const m = Math.max.apply(null, this.weeklyHours.map(w => w.hours))
      return m > 0 ? m : 1
    },
    /**
     * 考核成绩柱（2026-09-22 接真）
     * · 模拟自测：本人最近 3 场（`practice_record`，正确率折算成百分制，便于与正式同轴）
     * · 正式理论 / 实操：本人正式答卷各取最近一场的 final_score
     * 没有记录的项 score=null ⇒ 模板画灰色空柱并显示「--」（不留假数）
     */
    examScoreBars() {
      const bars = []
      const recs = (this.practiceRecords || []).slice(-3)
      recs.forEach((r, i) => {
        const total = Number(r.totalCount) || 0
        const sc = Number(r.score) || 0
        bars.push({ label: '自测 ' + (i + 1), score: total > 0 ? Math.round(sc * 100 / total) : null })
      })
      const formals = (this.myFormalExams || []).filter(e => e.finalScore != null)
      const theory = formals.filter(e => e.examType !== 'PRACTICAL').pop()
      const practical = formals.filter(e => e.examType === 'PRACTICAL').pop()
      bars.push({ label: '正式理论', score: theory ? Number(theory.finalScore) : null })
      bars.push({ label: '正式实操', score: practical ? Number(practical.finalScore) : null })
      return bars
    },
    /** 是否有任何一项出了分（全为 null 时走空态，而不是画一排空柱） */
    hasExamScores() { return this.examScoreBars.some(b => b.score !== null) },
    trainingSteps() {
      if (this.isFormal) return [
        { label: '签署保密协议', state: 'done', desc: '已完成签署' },
        { label: '在线学习', state: 'done', desc: '课程已完成，记录保留可回看' },
        { label: '正式考核', state: 'done', desc: '理论 + 实操均已通过' },
        { label: '转正发证', state: 'done', desc: '已转正，电子证书已生成' }
      ]
      const signed = Number(this.protocolStatus) === 1
      return [
        { label: '签署保密协议', state: signed ? 'done' : 'now', desc: signed ? '已完成签署' : '首次登录时需完成签署' },
        { label: '在线学习', state: signed ? 'now' : 'todo', desc: '完成率 ' + this.learningProgress + '%' + (this.learningGap ? ' · 还差 ' + this.learningGap + '% 解锁考核' : ' · 已达考核门槛') },
        { label: '正式考核', state: 'todo', desc: this.learningGap ? '理论 + 实操，未解锁' : '理论 + 实操，等待考核安排' },
        { label: '转正发证', state: 'todo', desc: '考核通过 + 部门管理员审批转正后生成电子证书' }
      ]
    },
    dashboardCourses() {
      return this.previewCourses.slice().sort((left, right) => {
        const rank = course => course.progress > 0 && course.progress < 100 ? 0 : (course.progress === 0 ? 1 : 2)
        return rank(left) - rank(right) || Number(right.isRequired || 0) - Number(left.isRequired || 0)
      }).slice(0, 3)
    },
    nextCourse() {
      return this.previewCourses.find(course => Number(course.isRequired) === 1 && course.progress < 100)
        || this.previewCourses.find(course => course.progress < 100)
        || this.previewCourses[0]
    },
    requiredCourses() {
      const required = this.previewCourses.filter(course => Number(course.isRequired) === 1)
      return required.length ? required : this.previewCourses
    },
    requiredCourseCount() {
      return this.requiredCourses.length
    },
    completedRequiredCourses() {
      return this.requiredCourses.filter(course => course.progress === 100).length
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
    internTodos() {
      const course = this.nextCourse
      const learningTodo = course
        ? { title: course.progress > 0 && course.progress < 100 ? '继续：' + course.courseName : (course.progress === 100 ? '回看：' + course.courseName : '开始：' + course.courseName), description: `已完成 ${course.completedItems}/${course.itemCount} 个学习单项，当前进度 ${course.progress}%`, status: course.progress === 100 ? '已完成' : (course.progress > 0 ? '进行中' : '未开始'), tag: course.progress === 100 ? 'success' : (course.progress > 0 ? 'primary' : 'warning'), tone: course.progress === 100 ? 'green' : (course.progress > 0 ? 'blue' : 'orange'), path: '/assessment/intern/learning/course/' + course.id, action: course.progress === 100 ? '回看' : (course.progress > 0 ? '继续' : '开始') }
        : { title: '暂无岗位课程', description: '等待部门管理员发布适用于当前岗位的课程', status: '暂无', tag: 'info', tone: 'gray', path: '/assessment/intern/learning', action: '查看' }
      if (this.isFormal) return [
        learningTodo, { title: '9 月考核结果已归档', description: '理论、实践及阶段评价均可查阅', status: '已完成', tag: 'success', tone: 'green', path: '/assessment/intern/scores', action: '查看' }, { title: '导师阶段评价已发布', description: '能力画像已同步最新评价', status: '已发布', tag: 'success', tone: 'green', path: '/assessment/intern/portrait', action: '查看' }
      ]
      return [
        learningTodo, { title: '岗位课程目录', description: `${this.learningOverview.courseCount} 门课程，共 ${this.learningOverview.itemCount} 个学习单项`, status: '学习中心', tag: 'success', tone: 'green', path: '/assessment/intern/learning', action: '查看' }, { title: '参加 9 月正式考核', description: this.learningGap ? `学习完成率达到 70% 后开放，当前还差 ${this.learningGap}%` : '已达到学习门槛，等待考核安排', status: this.learningGap ? '未解锁' : '待安排', tag: this.learningGap ? 'info' : 'warning', tone: 'gray', path: '/assessment/intern/exam', action: '查看' }, { title: '提交转正申请', description: this.learningGap ? `考核通过并满足资格后可提交，当前还差 ${this.learningGap}% 完成率` : '考核通过后提交转正申请，部门管理员终审即生效并发证', status: this.learningGap ? '未解锁' : '可申请', tag: this.learningGap ? 'info' : 'warning', tone: this.learningGap ? 'gray' : 'blue', path: '/assessment/intern/learning/result', action: '查看' }
      ]
    },
    progress() { return this.isFormal ? [{ label: '协议签署', value: 100, color: '#23966f' }, { label: '在线学习', value: this.learningProgress, color: '#2878c7' }, { label: '正式考核', value: 100, color: '#8055a7' }, { label: '转正审批', value: 100, color: '#23966f' }] : [{ label: '协议签署', value: this.protocolStatus === 1 ? 100 : 0, color: '#23966f' }, { label: '在线学习', value: this.learningProgress, color: '#2878c7' }, { label: '正式考核', value: 0, color: '#c98328' }, { label: '转正审批', value: 0, color: '#8055a7' }] },
    examRecords() { return this.isFormal ? [{ name: '2026 年 9 月正式考核', time: '2026-09-16', status: '已通过', score: '88 分', tag: 'success' }, { name: '理论考试', time: '2026-09-16', status: '已发布', score: '86 分', tag: 'primary' }, { name: '实践考核', time: '2026-09-17', status: '已发布', score: '91 分', tag: 'success' }] : [{ name: '安全规范模拟自测', time: '2026-09-08', status: '已完成', score: '86 分', tag: 'success' }, { name: '2026 年 9 月正式考核', time: '2026-09-16', status: '待参加', score: '--', tag: 'warning' }] },
    abilities() { return this.isFormal ? [{ label: '学习投入', value: 92 }, { label: '理论掌握', value: 86 }, { label: '实践能力', value: 91 }, { label: '规范遵从', value: 88 }] : [{ label: '学习投入', value: 80 }, { label: '理论掌握', value: 58 }, { label: '实践能力', value: 55 }, { label: '规范遵从', value: 82 }] },
    internRecords() {
      const signed = Number(this.protocolStatus) === 1
      const certified = this.isFormal
      const items = [
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
      if (signed) {
        items.push({
          key: 'signature',
          label: '签署凭证',
          icon: 'el-icon-document-checked',
          status: '可查看',
          tone: 'is-ready',
          path: '/assessment/intern/agreements',
          tip: '查看本次签署的时间、终端与凭证编号'
        })
      }
      return items
    },
    adminTodos() { return this.isDeptAdmin ? [{ title: '注册申请待审核', description: '5 条本部门申请等待处理', status: '待审核', tag: 'warning', tone: 'orange', path: '/assessment/department/register-review' }, { title: '草稿课程待完善', description: '补充章节和学习资料后即可发布', status: '待处理', tag: 'primary', tone: 'blue', path: '/assessment/department/courses' }, { title: '实践考核待批阅', description: '8 份提交物等待人工确认', status: '待批阅', tag: 'danger', tone: 'red', path: '/assessment/department/grading' }, { title: '阶段评价待补充', description: '3 名实习生画像信息待完善', status: '待处理', tag: 'primary', tone: 'blue', path: '/assessment/department/students' }] : [{ title: '本期考核安排待确认', description: '跨部门考试范围与时间需要复核', status: '待处理', tag: 'warning', tone: 'orange', path: '/assessment/manage/schedule' }, { title: '角色权限变更检查', description: '核对四类业务角色菜单范围', status: '检查中', tag: 'primary', tone: 'blue', path: '/assessment/system/role-permission' }, { title: '异常培养记录', description: '3 条记录需要管理员关注', status: '异常', tag: 'danger', tone: 'red', path: '/assessment/system/audit-log' }] },
    adminScope() { return this.isDeptAdmin ? [{ label: '预备实习生', value: '12 人', hint: '学习考核中' }, { label: '正式实习生', value: '6 人', hint: '保留历史档案' }, { label: '待分配导师', value: '3 人', hint: '审核后补充' }, { label: '学习达标', value: '14 人', hint: '可参加考核' }] : [{ label: '交付部门', value: '15 人', hint: '实施实习生' }, { label: '开发部门', value: '18 人', hint: '开发实习生' }, { label: '设计部门', value: '12 人', hint: '设计实习生' }, { label: '质检 / 建模', value: '23 人', hint: '两部门合计' }] },
    adminEntries() { return this.isDeptAdmin ? [{ title: '注册审核', description: '审核本部门申请并登记导师', icon: 'el-icon-user', path: '/assessment/department/register-review' }, { title: '课程管理', description: '维护岗位课程、章节和学习资料', icon: 'el-icon-reading', path: '/assessment/department/courses' }, { title: '实习生管理', description: '查看培养状态与学习进度', icon: 'el-icon-s-custom', path: '/assessment/department/students' }, { title: '实习批阅', description: '复核实践提交并发布成绩', icon: 'el-icon-edit-outline', path: '/assessment/department/grading' }, { title: '消息中心', description: '查看业务通知与处理提醒', icon: 'el-icon-message', path: '/assessment/department/messages' }] : [{ title: '考核认证管理', description: '考试、课程、题库与批阅流程', icon: 'el-icon-finished', path: '/assessment/manage/overview' }, { title: '组织岗位', description: '五部门与岗位绑定关系', icon: 'el-icon-office-building', path: '/assessment/system/organization' }, { title: '角色权限', description: '四类业务角色权限边界', icon: 'el-icon-lock', path: '/assessment/system/role-permission' }, { title: '审计日志', description: '关键业务操作留痕', icon: 'el-icon-document', path: '/assessment/system/audit-log' }] }
  },
  created() {
    this.loadLearningPreview()
    this.loadAnnouncements()
    this.loadExamStats()
    this.loadDailyDuration()
  },
  activated() {
    this.loadLearningPreview()
    this.loadAnnouncements()
    this.loadExamStats()
    this.loadDailyDuration()
  },
  methods: {
    /** 近 14 天逐日学习时长（真数据；接口只返回有记录的天，缺的天在前端补 0） */
    loadDailyDuration() {
      if (!this.isIntern) return
      getDailyDuration(14).then(res => {
        this.dailyDurations = res.data || []
      }).catch(() => { this.dailyDurations = [] })
    },
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
    /** 工作台公告位：任何人可见（公示），不写已读、不计未读红点 */
    loadAnnouncements() {
      listAnnouncements().then(res => {
        this.announcements = res.data || []
      }).catch(() => { this.announcements = [] })
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
    learningProgressFormat(percentage) {
      return this.learningOverview.progress === null ? '--' : percentage + '%'
    },
    formatDuration(minutes) {
      return formatLearningDuration(minutes)
    },
    courseAction(course) {
      return course.progress === 100 ? '回看课程' : (course.progress > 0 ? '继续学习' : '开始学习')
    },
    openLearningCourse(course) {
      this.go('/assessment/intern/learning/course/' + course.id)
    },
    /** 统一跳转：目标路由不存在则**不动** —— 不给死链（本项目铁律） */
    go(path, query) {
      if (!path) return
      if (!this.$router.resolve(path).route.matched.length) return
      this.$router.push(query ? { path, query } : path).catch(() => {})
    },
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
.dsample { display: inline-block; margin-left: 6px; padding: 0 6px; border-radius: 8px; background: #fff4e5; color: #b54708; font-size: 10px; font-style: normal; line-height: 16px; }
/* 01 身份条 */
.identity { display: flex; align-items: center; gap: 14px; }
.big-ph { display: flex; width: 52px; height: 52px; flex: 0 0 auto; align-items: center; justify-content: center; border-radius: 50%; color: #fff; background: #1764f5; font-size: 22px; font-weight: 600; }
.who { flex: 1; min-width: 0; }.who b { display: block; font-size: 16px; }.who p { margin: 5px 0 0; color: #667085; font-size: 12px; }
.facts { display: flex; gap: 30px; }
.fact span, .fact b { display: block; }
.fact span { margin-bottom: 5px; color: #8a94a3; font-size: 12px; }
.fact b { font-size: 14px; }.fact b.ok { color: #168561; }.fact b.warn { color: #c27516; }
/* 02 KPI */
.kpi-grid { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap: 12px; margin-bottom: 14px; }
.kpi { min-width: 0; padding: 14px 15px; border: 1px solid #e4e9f0; border-radius: 6px; background: #fff; }
.kpi.hl { border-color: #c3d9fb; background: #f5f9ff; }
.kpi-lb { display: flex; align-items: center; gap: 6px; color: #667085; font-size: 12px; }
.kpi-dot { width: 7px; height: 7px; border-radius: 50%; }
.kpi-vl { margin: 10px 0 8px; font-size: 24px; font-weight: 700; }
.kpi-vl small { margin-left: 3px; color: #98a2b3; font-size: 12px; font-weight: 400; }
.bar-mini { height: 5px; overflow: hidden; border-radius: 3px; background: #edf1f5; }
.bar-mini i { display: block; height: 100%; border-radius: 3px; background: #1764f5; }
.bar-mini i.o { background: repeating-linear-gradient(90deg, #a9c8f7 0 5px, transparent 5px 9px); }
.kpi-ft { margin-top: 9px; color: #7a8694; font-size: 11px; }
.kpi-ft.warn { color: #c27516; }.kpi-ft.up { color: #168561; }
/* 03 趋势 */
.trend-svg { display: block; width: 100%; height: auto; max-height: 230px; }
.legend { display: flex; flex-wrap: wrap; gap: 18px; margin-top: 10px; color: #667085; font-size: 12px; }
.legend span { display: inline-flex; align-items: center; gap: 6px; }
.legend i { width: 10px; height: 10px; border-radius: 2px; }
.note { margin-top: 10px; color: #98a2b3; font-size: 11px; line-height: 1.6; }
/* 04 雷达 */
.portrait { display: flex; align-items: center; gap: 14px; }
.radar-svg { width: 190px; flex: 0 0 auto; }
.portrait-meta { flex: 1; min-width: 0; }
.dim-row { display: flex; align-items: center; gap: 8px; min-height: 30px; }
.dim-row .nm { width: 60px; flex: 0 0 auto; color: #667085; font-size: 12px; }
.dim-row .bar { height: 7px; flex: 1; overflow: hidden; border-radius: 4px; background: #edf1f5; }
.dim-row .bar i { display: block; height: 100%; }
.dim-row .bar i.hollow { width: 100%; background: repeating-linear-gradient(90deg, #cfd6e0 0 5px, transparent 5px 9px); }
.dim-row .vv { width: 62px; flex: 0 0 auto; color: #475467; font-size: 11px; text-align: right; }
.completeness { margin-top: 10px; padding-top: 10px; border-top: 1px solid #edf0f4; }
.completeness .t { display: flex; justify-content: space-between; margin-bottom: 7px; color: #667085; font-size: 11px; }
/* 05 课程横条 */
.hbar { display: flex; width: 100%; align-items: center; gap: 10px; min-height: 36px; padding: 0; border: 0; color: inherit; text-align: left; background: transparent; cursor: pointer; }
.hbar .nm { width: 168px; flex: 0 0 auto; overflow: hidden; color: #475467; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.hbar .track { height: 9px; flex: 1; overflow: hidden; border-radius: 5px; background: #edf1f5; }
.hbar .track i { display: block; height: 100%; border-radius: 5px; background: #2878c7; }
.hbar.done .track i { background: #12b76a; }
.hbar.low .track i { background: #f79009; }
.hbar .pc { width: 42px; flex: 0 0 auto; color: #667085; font-size: 12px; text-align: right; }
.hbar:hover .nm { color: #1764f5; }
/* 06/07 柱图 */
.vchart { display: flex; align-items: flex-end; gap: 10px; height: 170px; padding: 6px 4px 0; }
.vcol { display: flex; height: 100%; flex: 1; min-width: 0; align-items: center; flex-direction: column; justify-content: flex-end; gap: 7px; }
.vcol .bar { display: flex; width: 70%; max-width: 46px; min-height: 8%; align-items: flex-start; justify-content: center; padding-top: 5px; border-radius: 5px 5px 0 0; color: #fff; background: #1764f5; font-size: 11px; }
.vcol .bar.hi { background: #0e4fd6; }
.vcol .bar.g { background: #12b76a; }
.vcol .bar.none { color: #98a2b3; background: #f2f4f7; }
.vcol-lb { color: #98a2b3; font-size: 11px; }
/* 08 培养步骤 */
.steps { display: grid; gap: 4px; }
.step { display: flex; align-items: center; gap: 12px; padding: 9px 0; border-bottom: 1px solid #edf0f4; }
.step:last-child { border-bottom: 0; }
.step .mark { display: flex; width: 26px; height: 26px; flex: 0 0 auto; align-items: center; justify-content: center; border-radius: 50%; color: #98a2b3; background: #f2f4f7; font-size: 12px; }
.step.done .mark { color: #fff; background: #12b76a; }
.step.now .mark { color: #fff; background: #1764f5; }
.step .txt b { display: block; color: #344054; font-size: 13px; }
.step .txt span { display: block; margin-top: 3px; color: #98a2b3; font-size: 11px; }
.step:not(.done):not(.now) .txt b { color: #98a2b3; }
/* 09 待办 */
.todo { display: flex; align-items: center; gap: 11px; min-height: 60px; border-bottom: 1px solid #edf0f4; }
.todo:last-child { border-bottom: 0; }
.tdot { width: 8px; height: 8px; flex: 0 0 auto; border-radius: 50%; background: #1764f5; }
.tdot.orange { background: #f79009; }.tdot.green { background: #12b76a; }.tdot.gray { background: #98a2b3; }.tdot.red { background: #f04438; }
.todo .tx { flex: 1; min-width: 0; }
.todo .tx b { display: block; color: #344054; font-size: 13px; }
.todo .tx span { display: block; margin-top: 4px; color: #7a8694; font-size: 12px; }
/* 10 通知 */
.notice { display: block; width: 100%; padding: 9px 0; border: 0; border-bottom: 1px solid #edf0f4; color: inherit; text-align: left; background: transparent; cursor: pointer; }
.notice:last-of-type { border-bottom: 0; }
.notice b { display: block; color: #344054; font-size: 13px; }
.notice span { display: block; margin-top: 4px; overflow: hidden; color: #98a2b3; font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }
.notice:hover b { color: #1764f5; }
.band-note { margin-top: 8px; }
@media (max-width: 1200px) { .kpi-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }.i2-span8, .i2-span4, .i2-span6 { grid-column: span 12; }.facts { gap: 16px; } }
@media (max-width: 760px) { .kpi-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }.identity { align-items: flex-start; flex-direction: column; }.facts { width: 100%; justify-content: space-between; }.portrait { flex-direction: column; }.hbar .nm { width: 110px; } }
@media (max-width: 1000px) { .entry-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }.identity-facts { gap: 16px; } }
@media (max-width: 760px) { .workspace-page { padding: 14px; }.workspace-head, .identity-band { align-items: flex-start; flex-direction: column; }.metric-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }.workspace-grid, .records-grid { grid-template-columns: 1fr; }.learning-dashboard-body { grid-template-columns: 1fr; }.learning-summary-panel { min-height: 0; }.identity-facts { width: 100%; justify-content: space-between; }.entry-grid, .entry-grid.three-columns { grid-template-columns: 1fr; }.task-row { align-items: flex-start; flex-wrap: wrap; padding: 12px 0; }.task-row > div { min-width: calc(100% - 22px); }.task-row .el-button { margin-left: 19px; }.dashboard-course-intro { white-space: normal; line-height: 1.5; } }
@media (max-width: 440px) { .metric-grid { grid-template-columns: 1fr; }.head-actions { width: 100%; }.head-actions .el-button { flex: 1; }.identity-facts { align-items: flex-start; flex-direction: column; gap: 10px; }.identity-facts div { display: flex; width: 100%; justify-content: space-between; }.identity-facts span { margin: 0; } }
/* 无数据空态（2026-09-22：能接真数据的接真，接不到就空着，不留假数） */
.i2-empty { display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 46px 14px; text-align: center; }
.i2-empty i { color: #d0d5dd; font-size: 30px; }
.i2-empty strong { color: #1d2939; font-size: 14px; }
.i2-empty span { color: #8490a0; font-size: 12.5px; line-height: 1.7; }
</style>
