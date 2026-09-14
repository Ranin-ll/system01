<template>
  <div class="course-detail-page">
    <div class="detail-breadcrumb">
      <el-button type="text" icon="el-icon-arrow-left" @click="backToLearning">返回在线学习</el-button>
      <span>/</span>
      <b>{{ course.courseName }}</b>
    </div>

    <section class="course-hero">
      <div class="hero-mark" :class="course.courseType === 'PRACTICE' ? 'practice' : 'theory'"><i :class="course.courseType === 'PRACTICE' ? 'el-icon-video-play' : 'el-icon-document'" /></div>
      <div class="hero-copy">
        <div class="hero-tags"><span>{{ course.courseType === 'PRACTICE' ? '视频实操课程' : '文档理论课程' }}</span><el-tag size="mini" :type="course.isRequired === 1 ? 'danger' : 'info'" effect="plain">{{ course.isRequired === 1 ? '必修' : '选修' }}</el-tag></div>
        <h1>{{ course.courseName }}</h1>
        <p>{{ course.intro }}</p>
        <div class="hero-meta"><span><i class="el-icon-user" /> {{ course.positionName }}</span><span><i class="el-icon-menu" /> {{ course.chapterCount }} 个章节</span><span><i class="el-icon-time" /> {{ formatDuration(course.duration) }}</span></div>
      </div>
      <div class="hero-progress"><span>课程完成率</span><strong>{{ course.progress }}%</strong><el-progress :percentage="course.progress" :show-text="false" :color="progressColor(course.progress)" /></div>
      <el-button type="primary" size="small" icon="el-icon-right" @click="startNext">{{ currentItem ? actionText(currentItem) : '开始学习' }}</el-button>
    </section>

    <section class="course-toolbar">
      <div class="course-tabs">
        <button v-for="tab in tabs" :key="tab.value" type="button" :class="{ active: itemFilter === tab.value }" @click="itemFilter = tab.value">{{ tab.label }} <em>{{ tab.count }}</em></button>
      </div>
      <el-button type="text" icon="el-icon-sort" @click="toggleChapters">{{ allExpanded ? '收起全部' : '展开全部' }}</el-button>
    </section>

    <section class="detail-layout">
      <aside class="chapter-sidebar">
        <div class="sidebar-title"><strong>课程目录</strong><span>{{ course.completedItems }}/{{ course.itemCount }} 已完成</span></div>
        <el-collapse v-model="activeChapters">
          <el-collapse-item v-for="(chapter, index) in visibleChapters" :key="chapter.id" :name="chapter.id">
            <template slot="title">
              <div class="chapter-title"><span class="chapter-number">{{ String(index + 1).padStart(2, '0') }}</span><span class="chapter-name">{{ chapter.chapterName }}</span><small>{{ chapterCompleted(chapter) }}/{{ chapter.items.length }}</small></div>
            </template>
            <button v-for="item in filteredItems(chapter)" :key="item.id" type="button" class="item-link" :class="{ selected: currentItem && currentItem.id === item.id }" @click="selectItem(item, chapter)">
              <span class="item-status" :class="item.status.toLowerCase()"><i :class="itemIcon(item)" /></span>
              <span class="item-link-copy"><b>{{ item.itemTitle }}</b><small>{{ item.itemType === 'VIDEO' ? '视频' : '文档' }} · {{ formatDuration(item.duration) }}</small></span>
              <span class="item-state">{{ itemState(item) }}</span>
            </button>
          </el-collapse-item>
        </el-collapse>
      </aside>

      <main class="study-area">
        <div v-if="currentItem" class="study-card">
          <div class="study-card-head">
            <div><span class="study-kicker">{{ currentChapter.chapterName }} / {{ itemTypeLabel(currentItem) }}</span><h2>{{ currentItem.itemTitle }}</h2></div>
            <el-tag :type="currentItem.status === 'DONE' ? 'success' : currentItem.status === 'IN_PROGRESS' ? 'primary' : 'info'" size="mini">{{ itemState(currentItem) }}</el-tag>
          </div>
          <div v-if="currentItem.itemType === 'DOC'" class="reader-wrap">
            <div ref="reader" class="document-reader" @scroll.passive="handleReaderScroll">
              <div class="document-sheet">
                <div class="document-label">岗位培养资料 · V1.0</div>
                <h3>{{ currentItem.itemTitle }}</h3>
                <p>本学习材料用于说明岗位工作中的基本规范、资料安全要求和交付协作方式。请完整阅读内容，并在阅读到文档末尾后确认完成。</p>
                <h4>一、学习目标</h4>
                <p>完成本单项后，你将了解当前岗位的基本工作边界，能够按照统一规范处理学习材料和项目交付资料。</p>
                <h4>二、岗位要求</h4>
                <p>工作过程中应遵守公司信息安全制度，使用授权账号和指定设备，不得擅自复制、外传或公开项目资料。遇到不确定事项时，应先向部门管理员或指定负责人确认。</p>
                <p>提交成果前请完成自检，确认文件命名、版本标识和交付说明完整。学习记录会保存最近学习时间和累计学习时长。</p>
                <h4>三、完成确认</h4>
                <p>当你阅读至此，说明已到达材料末尾。请确认已经理解本材料内容，再点击页面底部的“确认完成阅读”。</p>
                <div class="document-end">已到达文档末尾</div>
              </div>
            </div>
            <div class="reader-status"><span><i class="el-icon-reading" /> 阅读进度 {{ readerProgress }}%</span><span v-if="readerReachedEnd" class="reader-ready"><i class="el-icon-success" /> 已满足完成条件</span></div>
          </div>
          <div v-else class="video-wrap">
            <div class="video-stage">
              <div class="video-placeholder"><i class="el-icon-video-play" /><strong>视频学习内容</strong><span>视频资源接入后将在此播放，当前可体验进度交互。</span></div>
              <div class="video-controls"><el-button circle size="small" :icon="videoPlaying ? 'el-icon-video-pause' : 'el-icon-video-play'" @click="toggleVideo" /><span>{{ videoTimeLabel }}</span><el-slider v-model="videoProgress" :show-tooltip="false" @change="saveVideoProgress" /><span>100%</span></div>
            </div>
            <div class="video-status"><span><i class="el-icon-time" /> 最近播放进度 {{ videoProgress }}%</span><span v-if="videoProgress >= 100" class="reader-ready"><i class="el-icon-success" /> 已满足完成条件</span></div>
          </div>
          <div class="study-footer">
            <div><span class="save-state"><i class="el-icon-circle-check" /> {{ saveState }}</span><span>材料版本 V1.0</span></div>
            <div class="study-actions"><el-button size="small" icon="el-icon-arrow-left" :disabled="!previousItem" @click="goSibling(-1)">上一项</el-button><el-button v-if="currentItem.itemType === 'DOC'" type="primary" size="small" icon="el-icon-check" :disabled="!readerReachedEnd" @click="completeCurrent">确认完成阅读</el-button><el-button v-else type="primary" size="small" icon="el-icon-check" :disabled="videoProgress < 100" @click="completeCurrent">确认完成学习</el-button><el-button size="small" :disabled="!nextItem" @click="goSibling(1)">下一项 <i class="el-icon-arrow-right" /></el-button></div>
          </div>
        </div>
        <div v-else class="study-empty"><i class="el-icon-reading" /><strong>请选择一个学习单项</strong><span>从左侧课程目录开始学习。</span></div>
      </main>
    </section>

    <section class="detail-note"><i class="el-icon-info" /><span>学习记录会按单项自动保存；文档需阅读到末尾，视频需达到完成进度后才能标记完成。</span><el-button type="text" @click="comingSoon">学习规则</el-button></section>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { findPreviewCourse, flattenItems, formatLearningDuration, getPositionName, loadPreviewCourses, savePreviewCourses } from '@/utils/learningPreview'

export default {
  name: 'InternLearningCourse',
  data() {
    return {
      courses: [],
      course: { chapters: [], progress: 0, completedItems: 0, itemCount: 0 },
      currentItem: null,
      currentChapter: {},
      activeChapters: [],
      itemFilter: 'ALL',
      readerProgress: 0,
      readerReachedEnd: false,
      videoProgress: 0,
      videoPlaying: false,
      videoTimer: null,
      saveState: '进度已保存'
    }
  },
  computed: {
    ...mapGetters(['name', 'deptName']),
    positionName() {
      return getPositionName(this.deptName)
    },
    tabs() {
      const items = flattenItems(this.course)
      return [
        { label: '全部', value: 'ALL', count: items.length },
        { label: '文档', value: 'DOC', count: items.filter(item => item.itemType === 'DOC').length },
        { label: '视频', value: 'VIDEO', count: items.filter(item => item.itemType === 'VIDEO').length }
      ]
    },
    visibleChapters() {
      return this.course.chapters.filter(chapter => this.filteredItems(chapter).length)
    },
    allExpanded() {
      return this.activeChapters.length === this.visibleChapters.length && this.visibleChapters.length > 0
    },
    previousItem() {
      const items = flattenItems(this.course)
      const index = items.findIndex(item => this.currentItem && item.id === this.currentItem.id)
      return index > 0 ? items[index - 1] : null
    },
    nextItem() {
      const items = flattenItems(this.course)
      const index = items.findIndex(item => this.currentItem && item.id === this.currentItem.id)
      return index > -1 && index < items.length - 1 ? items[index + 1] : null
    },
    videoTimeLabel() {
      return this.videoProgress >= 100 ? '已完成' : Math.round(this.videoProgress * 0.8) + ' / 80 分钟'
    }
  },
  created() {
    this.loadCourse()
  },
  beforeDestroy() {
    this.stopVideo()
    this.persist()
  },
  methods: {
    loadCourse() {
      this.courses = loadPreviewCourses(this.name, this.deptName)
      const result = findPreviewCourse(this.courses, this.$route.params.courseId)
      this.course = result || this.courses[0] || { chapters: [], progress: 0, completedItems: 0, itemCount: 0, courseName: '课程不存在' }
      this.course.positionName = this.course.positionName || this.positionName
      this.activeChapters = this.course.chapters.length ? [this.course.chapters[0].id] : []
      const first = flattenItems(this.course).find(item => item.status !== 'DONE') || flattenItems(this.course)[0]
      if (first) {
        const chapter = this.course.chapters.find(item => item.items.some(child => child.id === first.id))
        this.selectItem(first, chapter)
      }
    },
    persist() {
      if (this.courses.length) savePreviewCourses(this.name, this.courses)
    },
    filteredItems(chapter) {
      return chapter.items.filter(item => this.itemFilter === 'ALL' || item.itemType === this.itemFilter)
    },
    chapterCompleted(chapter) {
      return chapter.items.filter(item => item.status === 'DONE').length
    },
    selectItem(item, chapter) {
      this.stopVideo()
      this.currentItem = item
      this.currentChapter = chapter || this.course.chapters.find(value => value.items.some(child => child.id === item.id)) || {}
      if (this.currentChapter.id && this.activeChapters.indexOf(this.currentChapter.id) === -1) this.activeChapters = [this.currentChapter.id]
      this.readerProgress = item.itemType === 'DOC' && item.readConfirm ? 100 : 0
      this.readerReachedEnd = item.itemType === 'DOC' && !!item.readConfirm
      this.videoProgress = item.itemType === 'VIDEO' ? Number(item.progress || 0) : 0
      if (item.status === 'NOT_STARTED') {
        item.status = 'IN_PROGRESS'
        item.lastStudyTime = this.nowText()
      }
      this.saveState = '进度已保存'
      this.persist()
    },
    toggleChapters() {
      this.activeChapters = this.allExpanded ? [] : this.visibleChapters.map(chapter => chapter.id)
    },
    itemIcon(item) {
      if (item.status === 'DONE') return 'el-icon-check'
      return item.itemType === 'VIDEO' ? 'el-icon-video-play' : 'el-icon-document'
    },
    itemTypeLabel(item) {
      return item.itemType === 'VIDEO' ? '视频实操' : '文档理论'
    },
    itemState(item) {
      if (item.status === 'DONE') return '已完成'
      if (item.status === 'IN_PROGRESS') return item.progress > 0 ? item.progress + '%' : '学习中'
      return '未开始'
    },
    actionText(item) {
      if (item.status === 'DONE') return '回看课程'
      return item.status === 'IN_PROGRESS' ? '继续学习' : '开始学习'
    },
    formatDuration(minutes) {
      return formatLearningDuration(minutes)
    },
    progressColor(progress) {
      return progress === 100 ? '#23966f' : progress > 0 ? '#2878c7' : '#aab4c0'
    },
    startNext() {
      if (this.currentItem) this.$nextTick(() => { const area = this.$refs.reader; if (area) area.scrollTop = 0 })
    },
    handleReaderScroll(event) {
      const target = event.target
      const remaining = target.scrollHeight - target.scrollTop - target.clientHeight
      this.readerProgress = Math.min(100, Math.round((target.scrollTop + target.clientHeight) / target.scrollHeight * 100))
      if (remaining < 16) {
        this.readerReachedEnd = true
        this.currentItem.readConfirm = true
        this.currentItem.progress = 100
        this.currentItem.lastStudyTime = this.nowText()
        this.persist()
      }
    },
    toggleVideo() {
      if (this.videoPlaying) {
        this.stopVideo()
        return
      }
      this.videoPlaying = true
      this.videoTimer = window.setInterval(() => {
        this.videoProgress = Math.min(100, this.videoProgress + 5)
        this.saveVideoProgress()
        if (this.videoProgress >= 100) this.stopVideo()
      }, 500)
    },
    stopVideo() {
      this.videoPlaying = false
      if (this.videoTimer) window.clearInterval(this.videoTimer)
      this.videoTimer = null
    },
    saveVideoProgress() {
      if (!this.currentItem || this.currentItem.itemType !== 'VIDEO') return
      this.currentItem.progress = this.videoProgress
      this.currentItem.lastStudyTime = this.nowText()
      this.currentItem.studyDuration = Math.max(Number(this.currentItem.studyDuration || 0), Math.round(this.videoProgress * 0.8))
      this.saveState = '进度已保存'
      this.persist()
    },
    completeCurrent() {
      if (!this.currentItem) return
      if (this.currentItem.itemType === 'DOC' && !this.readerReachedEnd) {
        this.$modal.msgWarning('请先阅读到文档末尾')
        return
      }
      if (this.currentItem.itemType === 'VIDEO' && this.videoProgress < 100) {
        this.$modal.msgWarning('视频进度达到 100% 后才能完成')
        return
      }
      this.currentItem.status = 'DONE'
      this.currentItem.progress = 100
      this.currentItem.studyDuration = this.currentItem.duration
      this.currentItem.readConfirm = this.currentItem.itemType === 'DOC' ? 1 : this.currentItem.readConfirm
      this.currentItem.finishTime = this.nowText()
      this.course.progress = Math.round(flattenItems(this.course).reduce((sum, item) => sum + Number(item.progress || 0), 0) / this.course.itemCount)
      this.course.completedItems = flattenItems(this.course).filter(item => item.status === 'DONE').length
      this.persist()
      this.$modal.msgSuccess('学习记录已保存，课程进度已更新')
      if (this.nextItem) this.selectItem(this.nextItem)
    },
    goSibling(step) {
      const target = step < 0 ? this.previousItem : this.nextItem
      if (target) this.selectItem(target)
    },
    nowText() {
      const date = new Date()
      const pad = value => String(value).padStart(2, '0')
      return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate()) + ' ' + pad(date.getHours()) + ':' + pad(date.getMinutes())
    },
    backToLearning() {
      this.$router.push('/assessment/intern/learning')
    },
    comingSoon() {
      this.$modal.msgInfo('功能开发中')
    }
  }
}
</script>

<style lang="scss" scoped>
.course-detail-page { min-height: 100%; padding: 22px 26px 36px; color: #283544; background: #f5f7fa; }
.detail-breadcrumb { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; color: #98a2b3; font-size: 12px; }
.detail-breadcrumb .el-button { padding: 0; color: #2878c7; font-size: 12px; }
.detail-breadcrumb b { max-width: 420px; overflow: hidden; color: #475467; font-weight: 500; text-overflow: ellipsis; white-space: nowrap; }
.course-hero { display: flex; align-items: center; gap: 18px; margin-bottom: 14px; padding: 24px 26px; background: #fff; box-shadow: 0 1px 3px rgba(16, 24, 40, .04); }
.hero-mark { display: flex; width: 68px; height: 68px; flex: 0 0 68px; align-items: center; justify-content: center; color: #fff; font-size: 30px; }
.hero-mark.theory { background: #23966f; }.hero-mark.practice { background: #2878c7; }
.hero-copy { min-width: 0; flex: 1; }
.hero-tags { display: flex; align-items: center; gap: 8px; color: #2878c7; font-size: 11px; }
.hero-copy h1 { margin: 7px 0 6px; color: #1d2939; font-size: 22px; font-weight: 600; }
.hero-copy p { margin: 0 0 10px; overflow: hidden; color: #667085; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.hero-meta { display: flex; flex-wrap: wrap; gap: 16px; color: #8490a0; font-size: 11px; }
.hero-meta i { margin-right: 3px; }
.hero-progress { width: 120px; flex: 0 0 120px; }.hero-progress span, .hero-progress strong { display: block; }.hero-progress span { margin-bottom: 5px; color: #8490a0; font-size: 11px; }.hero-progress strong { margin-bottom: 6px; color: #1d2939; font-size: 21px; font-weight: 600; }
.course-toolbar { display: flex; align-items: center; justify-content: space-between; padding: 0 20px; border-bottom: 1px solid #e4e9f0; background: #fff; }
.course-tabs { display: flex; gap: 26px; }.course-tabs button { position: relative; padding: 16px 0 13px; border: 0; color: #8490a0; background: transparent; cursor: pointer; font-size: 13px; }.course-tabs button.active { color: #1764f5; font-weight: 600; }.course-tabs button.active::after { position: absolute; right: 0; bottom: -1px; left: 0; height: 2px; background: #1764f5; content: ''; }.course-tabs em { margin-left: 4px; color: #98a2b3; font-size: 11px; font-style: normal; }
.detail-layout { display: grid; grid-template-columns: 320px minmax(0, 1fr); min-height: 560px; background: #fff; box-shadow: 0 1px 3px rgba(16, 24, 40, .04); }
.chapter-sidebar { border-right: 1px solid #e4e9f0; }.sidebar-title { display: flex; align-items: center; justify-content: space-between; padding: 18px 18px 14px; border-bottom: 1px solid #edf0f4; }.sidebar-title strong { color: #344054; font-size: 14px; }.sidebar-title span { color: #98a2b3; font-size: 11px; }
.chapter-sidebar ::v-deep .el-collapse { border-top: 0; }.chapter-sidebar ::v-deep .el-collapse-item__header { height: auto; min-height: 54px; padding: 0 14px; border-bottom: 1px solid #f0f2f5; color: #344054; line-height: 1.4; }.chapter-sidebar ::v-deep .el-collapse-item__wrap { border-bottom: 1px solid #edf0f4; }.chapter-sidebar ::v-deep .el-collapse-item__content { padding: 0 10px 9px; }
.chapter-title { display: flex; width: calc(100% - 18px); align-items: center; gap: 8px; }.chapter-number { color: #1764f5; font-size: 10px; font-weight: 700; }.chapter-name { min-width: 0; flex: 1; overflow: hidden; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }.chapter-title small { color: #98a2b3; font-size: 10px; }
.item-link { display: flex; width: 100%; align-items: center; gap: 8px; padding: 10px 7px; border: 0; color: #667085; text-align: left; background: transparent; cursor: pointer; }.item-link:hover, .item-link.selected { background: #f2f7ff; }.item-link.selected .item-link-copy b { color: #1764f5; }.item-status { display: flex; width: 23px; height: 23px; flex: 0 0 23px; align-items: center; justify-content: center; color: #98a2b3; border-radius: 50%; background: #f0f2f5; font-size: 12px; }.item-status.done { color: #fff; background: #23966f; }.item-status.in_progress { color: #fff; background: #2878c7; }.item-link-copy { min-width: 0; flex: 1; }.item-link-copy b, .item-link-copy small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.item-link-copy b { color: #475467; font-size: 11px; font-weight: 500; }.item-link-copy small { margin-top: 3px; color: #98a2b3; font-size: 10px; }.item-state { flex: 0 0 auto; color: #98a2b3; font-size: 10px; }
.study-area { min-width: 0; padding: 20px 24px 24px; background: #fbfcfe; }.study-card { min-height: 510px; border: 1px solid #e4e9f0; background: #fff; }.study-card-head { display: flex; align-items: flex-start; justify-content: space-between; padding: 18px 20px; border-bottom: 1px solid #edf0f4; }.study-kicker { color: #2878c7; font-size: 11px; }.study-card-head h2 { margin: 6px 0 0; color: #1d2939; font-size: 17px; font-weight: 600; }
.reader-wrap, .video-wrap { padding: 18px 20px; }.document-reader { height: 330px; overflow-y: auto; border: 1px solid #e1e6ed; background: #f2f4f7; }.document-sheet { max-width: 670px; min-height: 610px; margin: 0 auto; padding: 28px 38px 38px; background: #fff; }.document-label { color: #2878c7; font-size: 10px; }.document-sheet h3 { margin: 12px 0 20px; color: #1d2939; font-size: 19px; font-weight: 600; }.document-sheet h4 { margin: 22px 0 8px; color: #344054; font-size: 13px; }.document-sheet p { margin: 0 0 13px; color: #667085; font-size: 12px; line-height: 1.9; }.document-end { margin-top: 26px; padding-top: 15px; border-top: 1px dashed #ccd5df; color: #23966f; font-size: 12px; text-align: center; }.reader-status, .video-status { display: flex; align-items: center; justify-content: space-between; padding-top: 11px; color: #8490a0; font-size: 11px; }.reader-ready { color: #23966f; }.reader-ready i { margin-right: 3px; }
.video-stage { min-height: 366px; padding: 16px 16px 12px; background: #1f2937; }.video-placeholder { display: flex; height: 295px; align-items: center; justify-content: center; flex-direction: column; gap: 9px; color: #d8e2ef; text-align: center; }.video-placeholder i { color: #68a6ed; font-size: 43px; }.video-placeholder strong { font-size: 16px; font-weight: 500; }.video-placeholder span { color: #9caec2; font-size: 11px; }.video-controls { display: flex; align-items: center; gap: 9px; color: #b8c6d5; font-size: 10px; }.video-controls .el-slider { flex: 1; }.video-controls ::v-deep .el-slider__runway { margin: 0; background: #4b596a; }.video-controls ::v-deep .el-slider__bar { background: #4d9ae8; }.video-controls ::v-deep .el-slider__button { width: 10px; height: 10px; }.video-status { padding: 11px 0 0; }
.study-footer { display: flex; align-items: center; justify-content: space-between; gap: 14px; padding: 15px 20px; border-top: 1px solid #edf0f4; }.study-footer > div:first-child { display: flex; flex-wrap: wrap; gap: 12px; color: #98a2b3; font-size: 11px; }.save-state { color: #23966f; }.save-state i { margin-right: 3px; }.study-actions { display: flex; gap: 7px; }.study-empty { display: flex; height: 420px; align-items: center; justify-content: center; flex-direction: column; gap: 9px; color: #98a2b3; }.study-empty i { color: #b7c1cc; font-size: 34px; }.study-empty strong { color: #667085; font-size: 14px; font-weight: 500; }.study-empty span { font-size: 12px; }.detail-note { display: flex; align-items: center; gap: 8px; margin-top: 14px; padding: 12px 16px; border: 1px solid #dbeafe; color: #667085; background: #f5f9ff; font-size: 11px; }.detail-note > i { color: #2878c7; font-size: 15px; }.detail-note span { flex: 1; }.detail-note .el-button { padding: 0; font-size: 11px; }
@media (max-width: 900px) { .course-hero { flex-wrap: wrap; }.hero-copy { min-width: calc(100% - 86px); }.hero-progress { margin-left: 86px; }.detail-layout { grid-template-columns: 270px minmax(0, 1fr); } }
@media (max-width: 700px) { .course-detail-page { padding: 16px 12px 28px; }.course-hero { align-items: flex-start; padding: 18px; }.hero-mark { width: 52px; height: 52px; flex-basis: 52px; font-size: 23px; }.hero-copy { min-width: calc(100% - 70px); }.hero-copy h1 { font-size: 18px; }.hero-copy p { white-space: normal; line-height: 1.5; }.hero-progress { width: calc(100% - 70px); margin-left: 70px; }.course-hero > .el-button { width: 100%; }.course-toolbar { padding: 0 12px; }.course-tabs { gap: 18px; }.detail-layout { display: block; }.chapter-sidebar { border-right: 0; border-bottom: 1px solid #e4e9f0; }.chapter-sidebar ::v-deep .el-collapse-item__content { max-height: 220px; overflow-y: auto; }.study-area { padding: 12px; }.study-card-head { padding: 14px; }.reader-wrap, .video-wrap { padding: 12px; }.document-reader { height: 330px; }.document-sheet { padding: 24px 20px 32px; }.study-footer { align-items: flex-start; flex-direction: column; padding: 14px; }.study-actions { width: 100%; justify-content: flex-end; flex-wrap: wrap; }.detail-note .el-button { display: none; } }
</style>
