<template>
  <div class="learning-page">
    <div class="learning-breadcrumb">
      <el-button type="text" icon="el-icon-arrow-left" @click="goBack">返回工作台</el-button>
      <span>/</span>
      <b>在线学习</b>
    </div>

    <header class="learning-heading">
      <div>
        <span class="eyebrow">LEARNING CENTER</span>
        <h1>在线学习</h1>
      </div>
      <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="loadCourses">刷新进度</el-button>
    </header>

    <section class="learning-overview">
      <div class="overview-main">
        <span class="overview-icon"><i class="el-icon-reading" /></span>
        <div>
          <span class="overview-label">当前岗位培养</span>
          <h2>{{ positionName }}</h2>
          <!-- 正式与预备实习生在这页**完全同款**：都按岗位看已发布课程、都能记录进度（后端同一套可见性条件）。
               转正后消失的只有「备考资料 / 模拟考核 / 正式考核」入口，课程本身照常可学。 -->
          <p>{{ deptName || '当前部门' }} · {{ isFormal ? '正式实习生，仍可学习本岗位已发布课程' : '预备实习生，按课程进度完成培养' }}</p>
        </div>
      </div>
      <div class="overview-progress">
        <el-progress type="circle" :percentage="summary.progress === null ? 0 : summary.progress" :width="84" :stroke-width="8" :format="progressFormat" />
        <span>{{ summary.progress === null ? '暂无课程' : '必修课程完成率' }}</span>
      </div>
      <div class="overview-facts">
        <div><span>课程总数</span><strong>{{ summary.courseCount }}</strong></div>
        <div><span>已完成课程</span><strong>{{ summary.completedCourses }}</strong></div>
        <div><span>学习中</span><strong>{{ summary.learningCourses }}</strong></div>
        <div><span>已完成单项</span><strong>{{ summary.completedItems }}/{{ summary.itemCount }}</strong></div>
      </div>
    </section>

    <section class="catalog-section">
      <div class="section-heading">
        <div>
          <span class="section-index">01</span>
          <div>
            <h2>课程目录</h2>
          </div>
        </div>
        <span class="catalog-count">{{ filteredCourses.length }} 门课程</span>
      </div>

      <div class="catalog-toolbar">
        <el-radio-group v-model="statusFilter" size="small">
          <el-radio-button label="ALL">全部课程</el-radio-button>
          <el-radio-button label="IN_PROGRESS">学习中</el-radio-button>
          <el-radio-button label="DONE">已完成</el-radio-button>
        </el-radio-group>
        <el-select v-model="typeFilter" size="small" class="type-filter" placeholder="课程类型">
          <el-option label="全部类型" value="ALL" />
          <el-option label="文档理论" value="THEORY" />
          <el-option label="视频实操" value="PRACTICE" />
        </el-select>
      </div>

      <div v-loading="loading" class="course-list">
        <article v-for="course in filteredCourses" :key="course.id" class="course-row">
          <div class="course-mark" :class="course.courseType === 'PRACTICE' ? 'practice' : 'theory'">
            <i :class="course.courseType === 'PRACTICE' ? 'el-icon-video-play' : 'el-icon-document'" />
          </div>
          <div class="course-content">
            <div class="course-title-line">
              <div>
                <span class="course-kicker">{{ course.courseType === 'PRACTICE' ? '视频实操课程' : '文档理论课程' }}</span>
                <h3>{{ course.courseName }}</h3>
              </div>
              <el-tag size="mini" :type="course.isRequired === 1 ? 'danger' : 'info'" effect="plain">{{ course.isRequired === 1 ? '必修' : '选修' }}</el-tag>
            </div>
            <p class="course-intro">{{ course.intro }}</p>
            <div class="course-meta">
              <span><i class="el-icon-menu" /> {{ course.chapterCount }} 个章节</span>
              <span><i class="el-icon-time" /> {{ formatDuration(course.duration) }}</span>
              <span><i class="el-icon-document-checked" /> {{ course.completedItems }}/{{ course.itemCount }} 个单项已完成</span>
            </div>
            <div class="course-progress-line">
              <el-progress :percentage="course.progress" :show-text="false" :color="progressColor(course.progress)" />
              <b>{{ course.progress }}%</b>
            </div>
            <span class="course-last-time">最近学习：{{ course.lastStudyTime }}</span>
          </div>
          <div class="course-action">
            <el-tag v-if="course.progress === 100" type="success" size="mini">已完成</el-tag>
            <el-tag v-else-if="course.progress > 0" type="primary" size="mini">学习中</el-tag>
            <el-tag v-else type="info" size="mini">未开始</el-tag>
            <el-button type="primary" plain size="small" icon="el-icon-arrow-right" @click="openCourse(course)">{{ actionText(course) }}</el-button>
          </div>
        </article>

        <div v-if="!loading && !filteredCourses.length" class="empty-state">
          <i class="el-icon-reading" />
          <strong>暂无符合条件的课程</strong>
          <span>课程发布后会按岗位自动出现在这里。</span>
        </div>
      </div>
    </section>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { listLearningCourses } from '@/api/business/learning'
import { formatLearningDuration, getPositionName, learningSummary } from '@/utils/learningPreview'

export default {
  name: 'InternLearning',
  data() {
    return {
      loading: false,
      statusFilter: 'ALL',
      typeFilter: 'ALL',
      courses: [],
      summary: { progress: null, courseCount: 0, completedCourses: 0, learningCourses: 0, completedItems: 0, itemCount: 0 }
    }
  },
  computed: {
    ...mapGetters(['name', 'deptName', 'roles']),
    isFormal() {
      return this.roles.indexOf('FORMAL_TRAINEE') > -1
    },
    positionName() {
      return (this.courses[0] && this.courses[0].positionName) || getPositionName(this.deptName)
    },
    filteredCourses() {
      return this.courses.filter(course => {
        const statusMatched = this.statusFilter === 'ALL'
          || (this.statusFilter === 'DONE' && course.progress === 100)
          || (this.statusFilter === 'IN_PROGRESS' && course.progress > 0 && course.progress < 100)
        const typeMatched = this.typeFilter === 'ALL' || course.courseType === this.typeFilter
        return statusMatched && typeMatched
      })
    }
  },
  created() {
    this.loadCourses()
  },
  methods: {
    loadCourses() {
      this.loading = true
      listLearningCourses().then(response => {
        this.courses = (response.data || []).map(course => Object.assign({
          chapters: [], chapterCount: 0, itemCount: 0, completedItems: 0,
          duration: 0, progress: 0, lastStudyTime: '尚未开始'
        }, course, { lastStudyTime: course.lastStudyTime || '尚未开始' }))
        this.summary = learningSummary(this.courses)
      }).catch(() => {
        this.courses = []
        this.summary = learningSummary([])
      }).finally(() => {
        this.loading = false
      })
    },
    progressFormat(percentage) {
      return this.summary.progress === null ? '--' : percentage + '%'
    },
    progressColor(progress) {
      if (progress === 100) return '#23966f'
      if (progress > 0) return '#2878c7'
      return '#aab4c0'
    },
    formatDuration(minutes) {
      return formatLearningDuration(minutes)
    },
    actionText(course) {
      if (course.progress === 100) return '回看课程'
      if (course.progress > 0) return '继续学习'
      return '开始学习'
    },
    openCourse(course) {
      this.$router.push('/assessment/intern/learning/course/' + course.id)
    },
    goBack() {
      this.$router.push('/index')
    }
  }
}
</script>

<style lang="scss" scoped>
.learning-page { min-height: 100%; padding: 24px 26px 40px; color: #283544; background: #f5f7fa; }
.learning-breadcrumb { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; color: #98a2b3; font-size: 12px; }
.learning-breadcrumb .el-button { padding: 0; color: #2878c7; font-size: 12px; }
.learning-breadcrumb b { color: #475467; font-weight: 500; }
.learning-heading { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 18px; }
.eyebrow, .course-kicker { color: #2878c7; font-size: 11px; letter-spacing: .08em; }
.learning-heading h1 { margin: 5px 0 7px; color: #1d2939; font-size: 28px; font-weight: 600; }
.learning-heading p { margin: 0; color: #667085; font-size: 13px; }
.learning-overview { display: flex; align-items: center; gap: 28px; min-height: 138px; margin-bottom: 22px; padding: 24px 28px; border-left: 4px solid #1764f5; background: #fff; box-shadow: 0 1px 3px rgba(16, 24, 40, .04); }
.overview-main { display: flex; min-width: 300px; align-items: center; gap: 16px; flex: 1; }
.overview-icon { display: flex; width: 54px; height: 54px; align-items: center; justify-content: center; color: #fff; background: #1764f5; font-size: 27px; }
.overview-label { color: #8490a0; font-size: 12px; }
.overview-main h2 { margin: 5px 0 7px; color: #1d2939; font-size: 20px; font-weight: 600; }
.overview-main p { margin: 0; color: #667085; font-size: 12px; }
.overview-progress { display: flex; min-width: 100px; align-items: center; flex-direction: column; gap: 6px; color: #667085; font-size: 11px; }
.overview-facts { display: grid; min-width: 330px; grid-template-columns: repeat(4, 1fr); gap: 22px; }
.overview-facts div { padding-left: 18px; border-left: 1px solid #edf0f4; }
.overview-facts span, .overview-facts strong { display: block; }
.overview-facts span { margin-bottom: 8px; color: #8490a0; font-size: 11px; }
.overview-facts strong { color: #1d2939; font-size: 17px; font-weight: 600; }
.catalog-section { padding: 22px 24px 25px; background: #fff; box-shadow: 0 1px 3px rgba(16, 24, 40, .04); }
.section-heading { display: flex; align-items: center; justify-content: space-between; padding-bottom: 18px; border-bottom: 1px solid #edf0f4; }
.section-heading > div { display: flex; align-items: flex-start; gap: 12px; }
.section-index { color: #1764f5; font-size: 11px; font-weight: 700; }
.section-heading h2 { margin: 0; color: #1d2939; font-size: 18px; font-weight: 600; }
.catalog-count { color: #667085; font-size: 12px; }
.catalog-toolbar { display: flex; align-items: center; justify-content: space-between; padding: 18px 0 10px; }
.type-filter { width: 130px; }
.course-list { min-height: 210px; }
.course-row { display: flex; align-items: center; gap: 18px; padding: 20px 8px; border-bottom: 1px solid #edf0f4; }
.course-row:last-child { border-bottom: 0; }
.course-mark { display: flex; width: 54px; height: 54px; flex: 0 0 54px; align-items: center; justify-content: center; color: #fff; font-size: 24px; }
.course-mark.theory { background: #23966f; }
.course-mark.practice { background: #2878c7; }
.course-content { min-width: 0; flex: 1; }
.course-title-line { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.course-title-line h3 { margin: 4px 0 0; color: #1d2939; font-size: 16px; font-weight: 600; }
.course-intro { margin: 8px 0 10px; overflow: hidden; color: #667085; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.course-meta { display: flex; flex-wrap: wrap; gap: 15px; color: #8490a0; font-size: 11px; }
.course-meta i { margin-right: 3px; color: #98a2b3; }
.course-progress-line { display: flex; align-items: center; gap: 12px; margin-top: 12px; }
.course-progress-line .el-progress { max-width: 360px; flex: 1; }
.course-progress-line b { width: 36px; color: #344054; font-size: 12px; text-align: right; }
.course-last-time { display: block; margin-top: 7px; color: #98a2b3; font-size: 11px; }
.course-action { display: flex; min-width: 105px; align-items: flex-end; flex-direction: column; gap: 11px; }
.empty-state { display: flex; min-height: 220px; align-items: center; justify-content: center; flex-direction: column; gap: 9px; color: #98a2b3; }
.empty-state i { color: #b7c1cc; font-size: 34px; }
.empty-state strong { color: #667085; font-size: 14px; font-weight: 500; }
.empty-state span { font-size: 12px; }
@media (max-width: 1000px) { .learning-overview { flex-wrap: wrap; }.overview-main { min-width: 240px; }.overview-facts { min-width: 100%; }.overview-facts div:first-child { border-left: 0; padding-left: 0; } }
@media (max-width: 700px) { .learning-page { padding: 16px 12px 28px; }.learning-heading { align-items: flex-start; flex-direction: column; gap: 12px; }.learning-heading h1 { font-size: 24px; }.learning-overview { align-items: flex-start; flex-direction: column; gap: 18px; padding: 20px; }.overview-main { min-width: 0; width: 100%; }.overview-progress { align-items: flex-start; flex-direction: row; }.overview-facts { width: 100%; gap: 12px; }.overview-facts div { padding-left: 10px; }.catalog-section { padding: 18px 14px; }.section-heading { align-items: flex-start; gap: 10px; }.catalog-toolbar { align-items: flex-start; flex-direction: column; gap: 10px; }.type-filter { width: 100%; }.course-row { align-items: flex-start; flex-wrap: wrap; gap: 12px; padding: 17px 0; }.course-mark { width: 42px; height: 42px; flex-basis: 42px; font-size: 19px; }.course-content { width: calc(100% - 58px); flex: none; }.course-action { width: 100%; min-width: 0; align-items: flex-end; flex-direction: row; justify-content: flex-end; }.course-intro { white-space: normal; line-height: 1.5; } }
</style>
