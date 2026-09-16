<template>
  <div class="workspace-page">
    <header class="workspace-head">
      <div>
        <span class="eyebrow">{{ eyebrow }}</span>
        <h1>{{ title }}</h1>
        <p>{{ subtitle }}</p>
      </div>
      <div class="head-actions">
        <el-button size="small" icon="el-icon-refresh" @click="refresh">刷新</el-button>
        <el-button size="small" type="primary" icon="el-icon-message" @click="go(messagePath)">消息中心</el-button>
      </div>
    </header>

    <section v-if="isIntern" class="identity-band">
      <div class="identity-main">
        <span class="identity-icon"><i class="el-icon-user-solid" /></span>
        <div><small>当前身份</small><strong>{{ isFormal ? '正式实习生' : '预备实习生' }}</strong><p>{{ deptName || '所属部门' }} · {{ mentorText }}</p></div>
      </div>
      <div class="identity-facts">
        <div><span>保密协议</span><b :class="protocolStatus === 1 ? 'is-success' : 'is-warning'">{{ protocolStatus === 1 ? '已签署' : '待签署' }}</b></div>
        <div><span>培养阶段</span><b>{{ isFormal ? '已转正' : '学习考核中' }}</b></div>
        <div><span>未读消息</span><b>2 条</b></div>
      </div>
    </section>

    <section class="metric-grid">
      <article v-for="metric in metrics" :key="metric.label" class="metric" :class="metric.tone">
        <div class="metric-label"><i :class="metric.icon" /><span>{{ metric.label }}</span></div>
        <strong>{{ metric.value }}</strong>
        <small>{{ metric.hint }}</small>
      </article>
    </section>

    <template v-if="isIntern">
      <div class="workspace-grid">
        <section class="panel task-panel">
          <div class="panel-head"><div><span class="section-index">01</span><h2>{{ isFormal ? '近期动态' : '当前任务' }}</h2></div><el-button type="text" @click="isFormal ? go(messagePath) : go('/assessment/intern/tasks')">查看全部</el-button></div>
          <div v-for="item in internTodos" :key="item.title" class="task-row">
            <i class="task-dot" :class="item.tone" />
            <div><b>{{ item.title }}</b><p>{{ item.description }}</p></div>
            <el-tag size="mini" :type="item.tag">{{ item.status }}</el-tag>
            <el-button type="text" size="mini" @click="go(item.path)">{{ item.action }}</el-button>
          </div>
        </section>

        <section class="panel progress-panel">
          <div class="panel-head"><div><span class="section-index">02</span><h2>培养档案</h2></div><el-tag size="mini" :type="isFormal ? 'success' : 'warning'">{{ isFormal ? '已完成培养' : '本期' }}</el-tag></div>
          <div v-for="step in progress" :key="step.label" class="progress-row">
            <div><span>{{ step.label }}</span><b>{{ step.value }}%</b></div>
            <el-progress :percentage="step.value" :show-text="false" :color="step.color" />
          </div>
        </section>
      </div>

      <section class="learning-band">
        <div class="learning-band-head">
          <div><span class="section-index">03</span><h2>在线学习</h2><p>{{ isFormal ? '已发布课程与历史学习记录持续保留，可按需回看。' : '课程由所属部门按岗位发布，完成进度与学习中心实时一致。' }}</p></div>
          <el-button type="text" @click="go('/assessment/intern/learning')">全部课程 <i class="el-icon-arrow-right" /></el-button>
        </div>
        <div v-loading="learningLoading" class="learning-dashboard-body">
          <aside class="learning-summary-panel">
            <el-progress type="circle" :percentage="learningProgress" :width="86" :stroke-width="8" :format="learningProgressFormat" :color="learningProgress === 100 ? '#23966f' : '#2878c7'" />
            <div class="learning-summary-copy"><span>必修课程完成率</span><strong>{{ completedRequiredCourses }}/{{ requiredCourseCount }} 门必修完成</strong><small>最近学习：{{ learningOverview.lastStudyTime || '尚未开始' }}</small></div>
            <div class="learning-facts"><span><b>{{ learningOverview.completedItems }}</b>已完成单项</span><span><b>{{ Math.max(learningOverview.itemCount - learningOverview.completedItems, 0) }}</b>待完成单项</span></div>
          </aside>
          <div class="dashboard-course-list">
            <button v-for="course in dashboardCourses" :key="course.id" type="button" class="dashboard-course-row" @click="openLearningCourse(course)">
              <span class="course-icon" :class="course.courseType === 'PRACTICE' ? 'blue' : 'green'"><i :class="course.courseType === 'PRACTICE' ? 'el-icon-video-play' : 'el-icon-document'" /></span>
              <span class="dashboard-course-main">
                <span class="dashboard-course-title"><b>{{ course.courseName }}</b><el-tag size="mini" effect="plain" :type="Number(course.isRequired) === 1 ? 'danger' : 'info'">{{ Number(course.isRequired) === 1 ? '必修' : '选修' }}</el-tag></span>
                <small class="dashboard-course-intro">{{ course.intro || '暂无课程简介' }}</small>
                <span class="dashboard-course-meta"><small>{{ course.courseType === 'PRACTICE' ? '视频实操' : '文档理论' }}</small><small>{{ course.chapterCount }} 章</small><small>{{ course.completedItems }}/{{ course.itemCount }} 项完成</small><small>{{ formatDuration(course.duration) }}</small></span>
                <span class="dashboard-course-progress"><i><em :style="{ width: course.progress + '%' }" /></i><b>{{ course.progress }}%</b></span>
              </span>
              <span class="dashboard-course-action">{{ courseAction(course) }} <i class="el-icon-arrow-right" /></span>
            </button>
            <div v-if="!learningLoading && !dashboardCourses.length" class="learning-empty"><i class="el-icon-reading" /><span><b>暂无已发布课程</b><small>部门管理员发布适用于当前岗位的课程后，将在这里显示。</small></span></div>
          </div>
        </div>
      </section>

      <section v-if="isPre" class="certification-section">
        <div class="section-title"><div><span class="section-index">04</span><h2>考核认证</h2><p>备考资料、模拟自测和正式考核按当前培养进度开放。</p></div><el-tag effect="plain">完成率达到 70% 后可考核</el-tag></div>
        <div class="entry-grid three-columns">
          <button type="button" class="entry" @click="go('/assessment/intern/study-guide')"><i class="el-icon-notebook-2" /><span><b>备考资料</b><small>考试指南与考核规则</small></span><em>查看</em></button>
          <button type="button" class="entry" @click="go('/assessment/intern/mock-exam')"><i class="el-icon-edit-outline" /><span><b>模拟考核</b><small>10 题随机自测，不计成绩</small></span><em>练习</em></button>
          <button type="button" class="entry" @click="go('/assessment/intern/exam')"><i class="el-icon-finished" /><span><b>正式考核</b><small>理论考试与实践提交</small></span><em>进入</em></button>
        </div>
      </section>

      <div class="workspace-grid records-grid">
        <section class="panel">
          <div class="panel-head"><div><span class="section-index">{{ isPre ? '05' : '04' }}</span><h2>考核记录</h2></div><el-button type="text" @click="go('/assessment/intern/scores')">历史记录</el-button></div>
          <div v-for="record in examRecords" :key="record.name" class="record-row"><div><b>{{ record.name }}</b><span>{{ record.time }}</span></div><el-tag size="mini" :type="record.tag">{{ record.status }}</el-tag><strong>{{ record.score }}</strong></div>
        </section>
        <section class="panel ability-panel">
          <div class="panel-head"><div><span class="section-index">{{ isPre ? '06' : '05' }}</span><h2>能力画像</h2></div><el-button type="text" @click="go('/assessment/intern/portrait')">查看详情</el-button></div>
          <div v-for="ability in abilities" :key="ability.label" class="ability-row"><span>{{ ability.label }}</span><div><i :style="{ width: ability.value + '%' }" /></div><b>{{ ability.value }}</b></div>
        </section>
      </div>

      <nav class="quick-nav" aria-label="工作台快捷入口">
        <span>快捷入口</span>
        <el-button size="small" plain icon="el-icon-reading" @click="go('/assessment/intern/learning')">在线学习</el-button>
        <el-button size="small" plain icon="el-icon-tickets" @click="go('/assessment/intern/scores')">考试记录</el-button>
        <el-button size="small" plain icon="el-icon-user" @click="go('/assessment/intern/profile')">个人信息</el-button>
        <el-button size="small" plain icon="el-icon-message" @click="go(messagePath)">消息中心</el-button>
      </nav>
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
import { formatLearningDuration, learningSummary } from '@/utils/learningPreview'

export default {
  name: 'Index',
  data() {
    return {
      learningLoading: false,
      previewCourses: [],
      learningOverview: { progress: null, courseCount: 0, completedCourses: 0, learningCourses: 0, completedItems: 0, itemCount: 0, lastStudyTime: '尚未开始' }
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
        { label: '培养状态', value: '已转正', hint: '正式实习生', tone: 'green', icon: 'el-icon-circle-check' }, { label: '课程完成率', value: this.learningProgress + '%', hint: '历史学习记录已保留', tone: 'blue', icon: 'el-icon-reading' }, { label: '综合成绩', value: '88 分', hint: '历史考核已通过', tone: 'violet', icon: 'el-icon-data-analysis' }, { label: '未读消息', value: '2 条', hint: '近期业务通知', tone: 'orange', icon: 'el-icon-message' }
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
        learningTodo, { title: '岗位课程目录', description: `${this.learningOverview.courseCount} 门课程，共 ${this.learningOverview.itemCount} 个学习单项`, status: '学习中心', tag: 'success', tone: 'green', path: '/assessment/intern/learning', action: '查看' }, { title: '参加 9 月正式考核', description: this.learningGap ? `学习完成率达到 70% 后开放，当前还差 ${this.learningGap}%` : '已达到学习门槛，等待考核安排', status: this.learningGap ? '未解锁' : '待安排', tag: this.learningGap ? 'info' : 'warning', tone: 'gray', path: '/assessment/intern/exam', action: '查看' }
      ]
    },
    progress() { return this.isFormal ? [{ label: '协议签署', value: 100, color: '#23966f' }, { label: '在线学习', value: this.learningProgress, color: '#2878c7' }, { label: '正式考核', value: 100, color: '#8055a7' }, { label: '转正审批', value: 100, color: '#23966f' }] : [{ label: '协议签署', value: this.protocolStatus === 1 ? 100 : 0, color: '#23966f' }, { label: '在线学习', value: this.learningProgress, color: '#2878c7' }, { label: '正式考核', value: 0, color: '#c98328' }, { label: '转正审批', value: 0, color: '#8055a7' }] },
    examRecords() { return this.isFormal ? [{ name: '2026 年 9 月正式考核', time: '2026-09-16', status: '已通过', score: '88 分', tag: 'success' }, { name: '理论考试', time: '2026-09-16', status: '已发布', score: '86 分', tag: 'primary' }, { name: '实践考核', time: '2026-09-17', status: '已发布', score: '91 分', tag: 'success' }] : [{ name: '安全规范模拟自测', time: '2026-09-08', status: '已完成', score: '86 分', tag: 'success' }, { name: '2026 年 9 月正式考核', time: '2026-09-16', status: '待参加', score: '--', tag: 'warning' }] },
    abilities() { return this.isFormal ? [{ label: '学习投入', value: 92 }, { label: '理论掌握', value: 86 }, { label: '实践能力', value: 91 }, { label: '规范遵从', value: 88 }] : [{ label: '学习投入', value: 80 }, { label: '理论掌握', value: 58 }, { label: '实践能力', value: 55 }, { label: '规范遵从', value: 82 }] },
    adminTodos() { return this.isDeptAdmin ? [{ title: '注册申请待审核', description: '5 条本部门申请等待处理', status: '待审核', tag: 'warning', tone: 'orange', path: '/assessment/department/register-review' }, { title: '草稿课程待完善', description: '补充章节和学习资料后即可发布', status: '待处理', tag: 'primary', tone: 'blue', path: '/assessment/department/courses' }, { title: '实践考核待批阅', description: '8 份提交物等待人工确认', status: '待批阅', tag: 'danger', tone: 'red', path: '/assessment/department/grading' }, { title: '阶段评价待补充', description: '3 名实习生画像信息待完善', status: '待处理', tag: 'primary', tone: 'blue', path: '/assessment/department/students' }] : [{ title: '本期考核安排待确认', description: '跨部门考试范围与时间需要复核', status: '待处理', tag: 'warning', tone: 'orange', path: '/assessment/manage/schedule' }, { title: '角色权限变更检查', description: '核对四类业务角色菜单范围', status: '检查中', tag: 'primary', tone: 'blue', path: '/assessment/system/role-permission' }, { title: '异常培养记录', description: '3 条记录需要管理员关注', status: '异常', tag: 'danger', tone: 'red', path: '/assessment/system/audit-log' }] },
    adminScope() { return this.isDeptAdmin ? [{ label: '预备实习生', value: '12 人', hint: '学习考核中' }, { label: '正式实习生', value: '6 人', hint: '保留历史档案' }, { label: '待分配导师', value: '3 人', hint: '审核后补充' }, { label: '学习达标', value: '14 人', hint: '可参加考核' }] : [{ label: '交付部门', value: '15 人', hint: '实施实习生' }, { label: '开发部门', value: '18 人', hint: '开发实习生' }, { label: '设计部门', value: '12 人', hint: '设计实习生' }, { label: '质检 / 建模', value: '23 人', hint: '两部门合计' }] },
    adminEntries() { return this.isDeptAdmin ? [{ title: '注册审核', description: '审核本部门申请并登记导师', icon: 'el-icon-user', path: '/assessment/department/register-review' }, { title: '课程管理', description: '维护岗位课程、章节和学习资料', icon: 'el-icon-reading', path: '/assessment/department/courses' }, { title: '实习生管理', description: '查看培养状态与学习进度', icon: 'el-icon-s-custom', path: '/assessment/department/students' }, { title: '实习批阅', description: '复核实践提交并发布成绩', icon: 'el-icon-edit-outline', path: '/assessment/department/grading' }, { title: '消息中心', description: '查看业务通知与处理提醒', icon: 'el-icon-message', path: '/assessment/department/messages' }] : [{ title: '考核认证管理', description: '考试、课程、题库与批阅流程', icon: 'el-icon-finished', path: '/assessment/manage/overview' }, { title: '组织岗位', description: '五部门与岗位绑定关系', icon: 'el-icon-office-building', path: '/assessment/system/organization' }, { title: '角色权限', description: '四类业务角色权限边界', icon: 'el-icon-lock', path: '/assessment/system/role-permission' }, { title: '审计日志', description: '关键业务操作留痕', icon: 'el-icon-document', path: '/assessment/system/audit-log' }] }
  },
  created() {
    this.loadLearningPreview()
  },
  activated() {
    this.loadLearningPreview()
  },
  methods: {
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
    go(path) { this.$router.push(path) },
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
@media (max-width: 1000px) { .entry-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }.identity-facts { gap: 16px; } }
@media (max-width: 760px) { .workspace-page { padding: 14px; }.workspace-head, .identity-band { align-items: flex-start; flex-direction: column; }.metric-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }.workspace-grid, .records-grid { grid-template-columns: 1fr; }.learning-dashboard-body { grid-template-columns: 1fr; }.learning-summary-panel { min-height: 0; }.identity-facts { width: 100%; justify-content: space-between; }.entry-grid, .entry-grid.three-columns { grid-template-columns: 1fr; }.task-row { align-items: flex-start; flex-wrap: wrap; padding: 12px 0; }.task-row > div { min-width: calc(100% - 22px); }.task-row .el-button { margin-left: 19px; }.dashboard-course-intro { white-space: normal; line-height: 1.5; } }
@media (max-width: 440px) { .metric-grid { grid-template-columns: 1fr; }.head-actions { width: 100%; }.head-actions .el-button { flex: 1; }.identity-facts { align-items: flex-start; flex-direction: column; gap: 10px; }.identity-facts div { display: flex; width: 100%; justify-content: space-between; }.identity-facts span { margin: 0; } }
</style>
