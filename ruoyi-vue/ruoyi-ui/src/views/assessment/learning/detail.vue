<template>
  <div class="course-detail-page">
    <!-- 预览模式提示条：管理员在「课程管理」点「预览」进来，看到的就是实习生视角，但不产生任何学习记录 -->
    <div v-if="isPreview" class="preview-banner">
      <i class="el-icon-view" />
      <span>
        <b>预览模式</b> · 这是实习生打开该课程时看到的样子（按岗位匹配）。
        <em>预览不写入学习进度，也不会出现在任何人的学习记录里。</em>
      </span>
    </div>
    <div class="detail-breadcrumb">
      <el-button type="text" icon="el-icon-arrow-left" @click="backToLearning">{{ isPreview ? '返回课程管理' : '返回在线学习' }}</el-button>
      <span>/</span>
      <b>{{ course.courseName }}</b>
    </div>

    <!-- 预览取数失败（越权 / 已删除）：给明确原因与出口，而不是留个空壳 -->
    <section v-if="previewFailed" class="preview-empty">
      <i class="el-icon-warning-outline" />
      <strong>无法预览该课程</strong>
      <span>{{ previewError }}</span>
      <el-button size="small" @click="backToLearning">返回课程管理</el-button>
    </section>

    <section v-if="!previewFailed" class="course-hero">
      <div class="hero-mark" :class="course.courseType === 'PRACTICE' ? 'practice' : 'theory'"><i :class="course.courseType === 'PRACTICE' ? 'el-icon-video-play' : 'el-icon-document'" /></div>
      <div class="hero-copy">
        <div class="hero-tags"><span>{{ course.courseType === 'PRACTICE' ? '视频实操课程' : '文档理论课程' }}</span><el-tag size="mini" :type="course.isRequired === 1 ? 'danger' : 'info'" effect="plain">{{ course.isRequired === 1 ? '必修' : '选修' }}</el-tag></div>
        <h1>{{ course.courseName }}</h1>
        <p>{{ course.intro }}</p>
        <div class="hero-meta"><span><i class="el-icon-user" /> {{ course.positionName }}</span><span><i class="el-icon-menu" /> {{ course.chapterCount }} 个章节</span><span><i class="el-icon-time" /> {{ formatDuration(course.duration) }}</span></div>
      </div>
      <div v-if="isPreview" class="hero-progress"><span>预览模式</span><strong>—</strong><small class="preview-note">不统计进度</small></div>
      <div v-else class="hero-progress"><span>课程完成率</span><strong>{{ course.progress }}%</strong><el-progress :percentage="course.progress" :show-text="false" :color="progressColor(course.progress)" /></div>
      <el-button type="primary" size="small" icon="el-icon-right" @click="startNext">{{ currentItem ? actionText(currentItem) : '开始学习' }}</el-button>
    </section>

    <section v-if="!previewFailed" class="course-toolbar">
      <div class="course-tabs">
        <button v-for="tab in tabs" :key="tab.value" type="button" :class="{ active: itemFilter === tab.value }" @click="itemFilter = tab.value">{{ tab.label }} <em>{{ tab.count }}</em></button>
      </div>
      <el-button type="text" icon="el-icon-sort" @click="toggleChapters">{{ allExpanded ? '收起全部' : '展开全部' }}</el-button>
    </section>

    <section v-if="!previewFailed" class="detail-layout">
      <aside class="chapter-sidebar">
        <div class="sidebar-title"><strong>课程目录</strong><span>{{ course.completedItems }}/{{ course.itemCount }} 已完成</span></div>
        <el-collapse v-model="activeChapters">
          <el-collapse-item v-for="(chapter, index) in visibleChapters" :key="chapter.id" :name="chapter.id">
            <template slot="title">
              <div class="chapter-title"><span class="chapter-number">{{ String(index + 1).padStart(2, '0') }}</span><span class="chapter-copy"><span class="chapter-name">{{ chapter.chapterName }}</span><span v-if="chapter.chapterIntro" class="chapter-intro">{{ chapter.chapterIntro }}</span></span><small>{{ chapterCompleted(chapter) }}/{{ chapter.items.length }}</small></div>
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
            <div class="head-actions">
              <el-button v-if="currentItem.itemType === 'DOC' && currentItem.contentUrl" size="mini" icon="el-icon-download" @click="downloadAsset(currentItem)">下载资料</el-button>
              <el-tag :type="currentItem.status === 'DONE' ? 'success' : currentItem.status === 'IN_PROGRESS' ? 'primary' : 'info'" size="mini">{{ itemState(currentItem) }}</el-tag>
            </div>
          </div>
          <div v-if="currentItem.itemType === 'DOC'" class="reader-wrap">
            <div v-if="canInlinePreview(currentItem)" ref="reader" class="document-reader" @scroll.passive="handleReaderScroll">
              <iframe :key="currentItem.id" class="document-frame" :src="assetUrl(currentItem)" :title="currentItem.itemTitle" @load="documentLoaded" />
              <div class="document-end">资料已加载，可在新窗口查看完整内容</div>
            </div>
            <div v-else-if="canDocxPreview(currentItem)" ref="reader" class="document-reader docx-reader" @scroll.passive="handleReaderScroll">
              <div v-if="docxPreviewLoading" class="document-preview-state">
                <i class="el-icon-loading" />
                <strong>正在加载 Word 文档</strong>
                <span>文档内容较多时可能需要几秒钟。</span>
              </div>
              <div v-else-if="docxPreviewError" class="document-preview-state is-error">
                <i class="el-icon-warning-outline" />
                <strong>文档预览失败</strong>
                <span>{{ docxPreviewError }}</span>
                <el-button type="primary" plain icon="el-icon-download" @click="downloadAsset(currentItem)">下载资料</el-button>
              </div>
              <div v-show="!docxPreviewLoading && !docxPreviewError" ref="docxReader" class="docx-content" />
            </div>
            <div v-else-if="canPptxPreview(currentItem)" ref="reader" class="document-reader pptx-reader" @scroll.passive="handleReaderScroll">
              <div v-if="pptxPreviewLoading" class="document-preview-state">
                <i class="el-icon-loading" />
                <strong>正在加载演示文稿</strong>
                <span>幻灯片较多时可能需要几秒钟。</span>
              </div>
              <div v-else-if="pptxPreviewError" class="document-preview-state is-error">
                <i class="el-icon-warning-outline" />
                <strong>演示文稿预览失败</strong>
                <span>{{ pptxPreviewError }}</span>
                <el-button type="primary" plain icon="el-icon-download" @click="downloadAsset(currentItem)">下载资料</el-button>
              </div>
              <div v-show="!pptxPreviewLoading && !pptxPreviewError" ref="pptxReader" class="pptx-content" />
            </div>
            <div v-else class="download-panel">
              <i :class="currentItem.fileExt === 'zip' ? 'el-icon-folder-opened' : 'el-icon-document'" />
              <strong>{{ currentItem.fileName || currentItem.itemTitle }}</strong>
              <span>{{ currentItem.fileExt === 'zip' ? 'ZIP 压缩资料请下载后查看（下载后即可确认完成阅读）' : '该格式不支持在线预览，可下载后用本地软件打开' }}</span>
              <div class="download-panel-actions">
                <el-button type="primary" plain icon="el-icon-download" @click="downloadAsset(currentItem)">下载资料</el-button>
                <el-button v-if="currentItem.contentUrl" plain icon="el-icon-view" @click="openAsset(currentItem)">新窗口查看</el-button>
              </div>
            </div>
            <div v-if="currentItem.itemIntro" class="item-intro-panel"><span><i class="el-icon-document" /> 本节简介</span><p>{{ currentItem.itemIntro }}</p></div>
            <div class="reader-status"><span><i class="el-icon-reading" /> 阅读进度 {{ readerProgress }}%</span><span v-if="readerReachedEnd" class="reader-ready"><i class="el-icon-success" /> 已满足完成条件</span></div>
          </div>
          <div v-else-if="currentItem.itemType === 'VIDEO'" class="video-wrap">
            <div class="video-stage">
              <video v-if="currentItem.contentUrl" :key="currentItem.id" ref="courseVideo" class="course-video" controls controlsList="nodownload" :data-item-id="currentItem.id" :src="assetUrl(currentItem)" @timeupdate="handleVideoTimeUpdate" @pause="saveVideoProgress" @ended="completeVideo" />
              <div v-else class="video-placeholder"><i class="el-icon-warning-outline" /><strong>视频文件暂未上传</strong><span>请联系部门管理员补充该课程资料。</span></div>
            </div>
            <div v-if="currentItem.itemIntro" class="item-intro-panel"><span><i class="el-icon-document" /> 本节简介</span><p>{{ currentItem.itemIntro }}</p></div>
            <div class="video-status"><span><i class="el-icon-time" /> 最近播放进度 {{ videoProgress }}%</span><span v-if="videoProgress >= completionThreshold" class="reader-ready"><i class="el-icon-success" /> 已满足完成条件</span></div>
          </div>
          <div v-else class="download-panel">
            <i class="el-icon-document" />
            <strong>{{ currentItem.fileName || currentItem.itemTitle }}</strong>
            <span>该学习单项暂不支持在线学习，请下载后查看或联系部门管理员。</span>
            <el-button v-if="currentItem.contentUrl" type="primary" plain icon="el-icon-download" @click="downloadAsset(currentItem)">下载资料</el-button>
          </div>
          <div class="study-footer">
            <div><span class="save-state"><i class="el-icon-circle-check" /> {{ saveState }}</span><span>材料版本 V1.0</span></div>
            <div class="study-actions"><el-button size="small" icon="el-icon-arrow-left" :disabled="!previousItem" @click="goSibling(-1)">上一项</el-button><el-button v-if="!isPreview && currentItem.itemType === 'DOC'" type="primary" size="small" icon="el-icon-check" :disabled="!readerReachedEnd || saving" :loading="saving" @click="completeCurrent">确认完成阅读</el-button><el-button v-else-if="!isPreview && currentItem.itemType === 'VIDEO'" type="primary" size="small" icon="el-icon-check" :disabled="videoProgress < completionThreshold || saving" :loading="saving" @click="completeCurrent">确认完成学习</el-button><el-button size="small" :disabled="!nextItem" @click="goSibling(1)">下一项 <i class="el-icon-arrow-right" /></el-button></div>
          </div>
        </div>
        <div v-else class="study-empty"><i class="el-icon-reading" /><strong>请选择一个学习单项</strong><span>从左侧课程目录开始学习。</span></div>
      </main>
    </section>

    <section class="detail-note"><i class="el-icon-info" /><span>学习记录会按单项自动保存；文档支持在线学习与下载，阅读到末尾后确认完成；视频需达到完成进度后才能标记完成。</span><el-button type="text" @click="comingSoon">学习规则</el-button></section>
  </div>
</template>

<script>
import { renderAsync } from 'docx-preview/dist/docx-preview.js'
import { init as initPptxPreview } from 'pptx-preview'
import { mapGetters } from 'vuex'
import { getLearningCourse, saveLearningProgress } from '@/api/business/learning'
// 预览模式（管理员视角）用的管理侧接口：课程本体 + 章节/资料
import { getCourse } from '@/api/business/course'
import { getCourseContents } from '@/api/business/courseContent'
import { flattenItems, formatLearningDuration, getPositionName } from '@/utils/learningPreview'

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
      docxPreviewLoading: false,
      docxPreviewError: '',
      pptxPreviewLoading: false,
      pptxPreviewError: '',
      videoProgress: 0,
      videoPlaying: false,
      videoTimer: null,
      saveState: '进度已保存',
      saving: false,
      progressTimer: null,
      /** 预览取数失败原因（空串 = 正常） */
      previewError: ''
    }
  },
  computed: {
    ...mapGetters(['name', 'deptName']),
    positionName() {
      return getPositionName(this.deptName)
    },
    /**
     * 预览模式：管理员在「课程管理」里点「预览」进来（路由 meta.preview = true）。
     * 数据改走管理侧接口，**不写任何学习进度**（管理员没有 study_record，也不该产生记录）。
     */
    isPreview() {
      return !!this.$route.meta.preview
    },
    /** 预览取数失败（典型：该课程不属于你管理的部门，或已被删除）→ 给明确说明，别留空壳 */
    previewFailed() {
      return this.isPreview && this.previewError !== ''
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
    },
    completionThreshold() {
      return Number((this.currentItem && this.currentItem.completionThreshold) || 100)
    }
  },
  created() {
    this.loadCourse()
  },
  beforeDestroy() {
    this.stopVideo()
    this.destroyPptxPreview()
    this.cancelPendingSave()
  },
  methods: {
    loadCourse() {
      if (this.isPreview) {
        this.loadPreviewCourse()
        return
      }
      getLearningCourse(this.$route.params.courseId).then(response => {
        this.course = response.data || { chapters: [] }
        this.course.chapters = this.course.chapters || []
        this.course.positionName = this.course.positionName || this.positionName
        this.activeChapters = this.course.chapters.length ? [this.course.chapters[0].id] : []
        const first = flattenItems(this.course).find(item => item.status !== 'DONE') || flattenItems(this.course)[0]
        if (first) {
          const chapter = this.course.chapters.find(item => item.items.some(child => child.id === first.id))
          this.selectItem(first, chapter)
        }
      }).catch(() => {
        this.$modal.msgError('课程不存在、已停用或不适用于当前岗位')
        this.backToLearning()
      })
    },
    /**
     * 预览模式：走管理侧接口取数，并归一化成实习生页面的数据形态。
     *
     * `GET /business/course/{id}/contents` 返回的章节/资料字段与实习生接口**完全一致**
     * （仅进度类字段为 null），所以只需把进度补成「未开始」，就能复用同一套模板渲染 ——
     * 这正是「预览 = 实习生看到的页面」而不是另做一套近似版的原因。
     * 注意：课程本体走 `GET /business/course/{id}`，**不做岗位/部门适用性校验**（管理员要能预览任何课程）。
     */
    loadPreviewCourse() {
      const courseId = this.$route.params.courseId
      Promise.all([getCourse(courseId), getCourseContents(courseId)]).then(([courseRes, contentRes]) => {
        const course = courseRes.data || {}
        const chapters = (contentRes.data || []).map(chapter => Object.assign({}, chapter, {
          items: (chapter.items || []).map(item => Object.assign({}, item, {
            // 模板里用 item.status.toLowerCase() 挂样式类，null 会直接抛错 → 统一置为未开始
            status: 'NOT_STARTED',
            progress: 0,
            readConfirm: 0,
            studyDuration: 0
          }))
        }))
        const flat = []
        chapters.forEach(chapter => (chapter.items || []).forEach(item => flat.push(item)))
        course.chapters = chapters
        course.chapterCount = chapters.length
        course.itemCount = flat.length
        course.completedItems = 0
        course.progress = 0
        course.duration = flat.reduce((sum, item) => sum + (Number(item.duration) || 0), 0)
        course.positionName = course.positionName || this.positionName
        this.course = course
        this.activeChapters = chapters.length ? [chapters[0].id] : []
        const first = flat[0]
        if (first) {
          const chapter = chapters.find(row => row.items.some(child => child.id === first.id))
          this.selectItem(first, chapter)
        }
      }).catch(() => {
        this.previewError = '课程不存在、已被删除，或不属于你管理的部门范围'
        this.$modal.msgError('无法预览该课程：' + this.previewError)
      })
    },
    filteredItems(chapter) {
      return chapter.items.filter(item => this.itemFilter === 'ALL' || item.itemType === this.itemFilter)
    },
    chapterCompleted(chapter) {
      return chapter.items.filter(item => item.status === 'DONE').length
    },
    selectItem(item, chapter) {
      this.stopVideo()
      this.destroyPptxPreview()
      this.cancelPendingSave()
      this.currentItem = item
      this.currentChapter = chapter || this.course.chapters.find(value => value.items.some(child => child.id === item.id)) || {}
      if (this.currentChapter.id && this.activeChapters.indexOf(this.currentChapter.id) === -1) this.activeChapters = [this.currentChapter.id]
      this.readerProgress = item.itemType === 'DOC' && item.readConfirm ? 100 : 0
      this.readerReachedEnd = item.itemType === 'DOC' && !!item.readConfirm
      this.docxPreviewLoading = false
      this.docxPreviewError = ''
      this.pptxPreviewLoading = false
      this.pptxPreviewError = ''
      this.videoProgress = item.itemType === 'VIDEO' ? Number(item.progress || 0) : 0
      // 预览模式不改状态：没有「某个人」在学习，应当保持实习生打开前的干净样子（未开始）
      if (!this.isPreview && item.status === 'NOT_STARTED') {
        item.status = 'IN_PROGRESS'
        item.lastStudyTime = this.nowText()
      }
      this.saveProgress(false, item)
      if (this.canDocxPreview(item)) this.$nextTick(() => this.loadDocxPreview(item))
      if (this.canPptxPreview(item)) this.$nextTick(() => this.loadPptxPreview(item))
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
      if (!target || !target.scrollHeight) return
      const remaining = target.scrollHeight - target.scrollTop - target.clientHeight
      this.readerProgress = Math.min(100, Math.round((target.scrollTop + target.clientHeight) / target.scrollHeight * 100))
      if (remaining < 16) {
        this.readerReachedEnd = true
        this.currentItem.readConfirm = true
        this.currentItem.progress = 100
        this.currentItem.lastStudyTime = this.nowText()
        this.queueProgressSave()
      }
    },
    documentLoaded() {
      this.readerProgress = Math.max(this.readerProgress, 100)
      this.readerReachedEnd = true
      this.currentItem.readConfirm = true
      this.queueProgressSave()
    },
    stopVideo() {
      this.videoPlaying = false
      if (this.videoTimer) window.clearInterval(this.videoTimer)
      this.videoTimer = null
    },
    saveVideoProgress(event) {
      const video = event && event.target
      const itemId = Number(video && video.dataset.itemId)
      if (!this.currentItem || this.currentItem.itemType !== 'VIDEO' || itemId !== Number(this.currentItem.id)) return
      this.currentItem.progress = this.videoProgress
      this.currentItem.lastStudyTime = this.nowText()
      this.currentItem.studyDuration = Math.max(Number(this.currentItem.studyDuration || 0), Math.round(video.currentTime || 0))
      this.queueProgressSave(this.currentItem)
    },
    handleVideoTimeUpdate(event) {
      const video = event.target
      const itemId = Number(video.dataset.itemId)
      if (!this.currentItem || itemId !== Number(this.currentItem.id) || !video.duration) return
      this.videoProgress = Math.max(this.videoProgress, Math.min(100, Math.round(video.currentTime / video.duration * 100)))
      this.saveVideoProgress(event)
    },
    completeVideo() {
      this.videoProgress = 100
      this.completeCurrent()
    },
    completeCurrent() {
      if (!this.currentItem) return
      if (this.currentItem.itemType === 'DOC' && !this.readerReachedEnd) {
        this.$modal.msgWarning('请先阅读到文档末尾')
        return
      }
      if (this.currentItem.itemType === 'VIDEO' && this.videoProgress < this.completionThreshold) {
        this.$modal.msgWarning('视频进度达到 ' + this.completionThreshold + '% 后才能完成')
        return
      }
      this.saveProgress(true).then(saved => {
        if (!saved) return
        this.currentItem.status = 'DONE'
        this.currentItem.progress = 100
        this.currentItem.readConfirm = this.currentItem.itemType === 'DOC' ? 1 : this.currentItem.readConfirm
        this.currentItem.finishTime = this.nowText()
        this.syncCourseProgress()
        this.$modal.msgSuccess('学习记录已保存，课程进度已更新')
      })
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
    cancelPendingSave() {
      if (this.progressTimer) window.clearTimeout(this.progressTimer)
      this.progressTimer = null
    },
    queueProgressSave(item = this.currentItem) {
      if (!item || !item.id) return
      this.saveState = '正在保存...'
      this.cancelPendingSave()
      this.progressTimer = window.setTimeout(() => {
        this.progressTimer = null
        this.saveProgress(false, item)
      }, 600)
    },
    saveProgress(completed, item = this.currentItem) {
      if (!item || !item.id) return Promise.resolve(false)
      // 预览模式（管理员视角）**不写任何学习进度** —— 管理员没有 study_record，
      // 且 /business/learning/items/{id}/progress 本身只对实习生角色开放（会 403）。
      if (this.isPreview) return Promise.resolve(false)
      this.saving = Boolean(completed)
      const isCurrentItem = () => this.currentItem && Number(this.currentItem.id) === Number(item.id)
      const payload = {
        progress: Number(item.itemType === 'VIDEO' && isCurrentItem() ? this.videoProgress : item.progress || 0),
        studyDuration: Number(item.studyDuration || 0),
        readConfirm: item.itemType === 'DOC' && isCurrentItem() ? (this.readerReachedEnd ? 1 : 0) : (item.readConfirm ? 1 : 0),
        completed: Boolean(completed)
      }
      return saveLearningProgress(item.id, payload).then(response => {
        if (isCurrentItem()) {
          const saved = response.data || {}
          this.currentItem.status = saved.status || this.currentItem.status
          this.currentItem.progress = saved.progress === undefined ? this.currentItem.progress : saved.progress
          this.currentItem.studyDuration = saved.studyDuration === undefined ? this.currentItem.studyDuration : saved.studyDuration
          this.currentItem.readConfirm = saved.readConfirm === undefined ? this.currentItem.readConfirm : saved.readConfirm
          this.currentItem.lastStudyTime = saved.lastStudyTime || this.currentItem.lastStudyTime
          this.currentItem.finishTime = saved.finishTime || this.currentItem.finishTime
          this.saveState = '进度已保存'
          this.syncCourseProgress()
        }
        return true
      }).catch(error => {
        if (isCurrentItem()) this.saveState = (error && error.message) || '保存失败，请刷新课程后重试'
        return false
      }).finally(() => {
        if (isCurrentItem()) this.saving = false
      })
    },
    syncCourseProgress() {
      const items = flattenItems(this.course)
      this.course.itemCount = items.length
      this.course.completedItems = items.filter(item => item.status === 'DONE').length
      this.course.progress = items.length ? Math.round(items.reduce((sum, item) => sum + Number(item.progress || 0), 0) / items.length) : 0
    },
    assetUrl(item) {
      if (!item || !item.contentUrl) return ''
      if (/^https?:\/\//i.test(item.contentUrl)) return item.contentUrl
      return process.env.VUE_APP_BASE_API + item.contentUrl
    },
    canInlinePreview(item) {
      return ['pdf', 'txt'].indexOf(String(item.fileExt || '').toLowerCase()) > -1 && Boolean(item.contentUrl)
    },
    canDocxPreview(item) {
      return String(item.fileExt || '').toLowerCase() === 'docx' && Boolean(item.contentUrl)
    },
    canPptxPreview(item) {
      return String(item.fileExt || '').toLowerCase() === 'pptx' && Boolean(item.contentUrl)
    },
    loadDocxPreview(item) {
      if (!this.canDocxPreview(item)) return
      const itemId = Number(item.id)
      const container = this.$refs.docxReader
      if (!container) return
      this.docxPreviewLoading = true
      this.docxPreviewError = ''
      container.innerHTML = ''
      fetch(this.assetUrl(item))
        .then(response => {
          if (!response.ok) throw new Error('文档文件读取失败')
          return response.blob()
        })
        .then(blob => renderAsync(blob, container, null, {
          inWrapper: true,
          ignoreWidth: false,
          ignoreHeight: false,
          breakPages: true
        }))
        .then(() => {
          if (!this.currentItem || Number(this.currentItem.id) !== itemId) return
          this.docxPreviewLoading = false
          this.$nextTick(() => this.handleReaderScroll({ target: this.$refs.reader }))
        })
        .catch(error => {
          if (!this.currentItem || Number(this.currentItem.id) !== itemId) return
          this.docxPreviewLoading = false
          this.docxPreviewError = (error && error.message) || '当前 Word 文档无法解析'
        })
    },
    loadPptxPreview(item) {
      if (!this.canPptxPreview(item)) return
      const itemId = Number(item.id)
      const container = this.$refs.pptxReader
      if (!container) return
      this.pptxPreviewLoading = true
      this.pptxPreviewError = ''
      container.innerHTML = ''
      fetch(this.assetUrl(item))
        .then(response => {
          if (!response.ok) throw new Error('演示文稿文件读取失败')
          return response.arrayBuffer()
        })
        .then(buffer => {
          if (!this.currentItem || Number(this.currentItem.id) !== itemId) return null
          const reader = this.$refs.reader
          const availableWidth = (reader && reader.clientWidth) || 960
          const availableHeight = (reader && reader.clientHeight) || 540
          const width = Math.max(280, Math.min(availableWidth, availableHeight * 16 / 9))
          this._pptxPreviewer = initPptxPreview(container, {
            width: Math.round(width),
            height: Math.round(width * 9 / 16),
            mode: 'slide'
          })
          return this._pptxPreviewer.preview(buffer)
        })
        .then(() => {
          if (!this.currentItem || Number(this.currentItem.id) !== itemId) return
          this.pptxPreviewLoading = false
          this.$nextTick(() => this.handleReaderScroll({ target: this.$refs.reader }))
        })
        .catch(error => {
          if (!this.currentItem || Number(this.currentItem.id) !== itemId) return
          this.pptxPreviewLoading = false
          this.pptxPreviewError = (error && error.message) || '当前 PPTX 文件无法解析'
        })
    },
    destroyPptxPreview() {
      if (this._pptxPreviewer && typeof this._pptxPreviewer.destroy === 'function') {
        this._pptxPreviewer.destroy()
      }
      this._pptxPreviewer = null
    },
    /** 新窗口打开（仅用于查看，不改变完成状态） */
    openAsset(item) {
      const url = this.assetUrl(item)
      if (!url) return this.$modal.msgWarning('资料文件暂未上传')
      window.open(url, '_blank', 'noopener')
    },
    /**
     * 下载资料（走 /common/download/resource，带 Content-Disposition 附件头）。
     * 说明：下载不直接标记完成；对不支持在线预览的格式，下载后放行「确认完成阅读」。
     */
    downloadAsset(item) {
      if (!item || !item.contentUrl) return this.$modal.msgWarning('资料文件暂未上传')
      this.$download.resource(item.contentUrl)
      const previewable = this.canInlinePreview(item) || this.canDocxPreview(item) || this.canPptxPreview(item)
      if (!previewable) {
        this.readerReachedEnd = true
        this.$modal.msgSuccess('资料已开始下载，可点击「确认完成阅读」标记本条学习完成')
      } else {
        this.$modal.msgSuccess('资料已开始下载，也可在页面内在线阅读到末尾后确认完成')
      }
    },
    backToLearning() {
      // 预览模式是从「课程管理」window.open 出来的新标签页：
      // 优先回来源页（?from= 由课程管理页带上，超管/部门管理员各自的路径不同，所以不能写死），
      // 没有来源参数就尝试关掉标签页。
      if (this.isPreview) {
        const from = this.$route.query.from
        if (from) {
          this.$router.push(from)
        } else {
          window.close()
        }
        return
      }
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
/* 预览模式（管理员在课程管理点「预览」进入）：醒目但不抢戏的浅色提示条 */
.preview-banner {
  display: flex; align-items: flex-start; gap: 8px; margin-bottom: 12px; padding: 10px 14px;
  color: #175cd3; background: #eff8ff; border: 1px solid #b2ddff; border-radius: 8px; font-size: 12.5px; line-height: 1.6;
  i { margin-top: 2px; }
  em { color: #475467; font-style: normal; }
}
.hero-progress .preview-note { color: #98a2b3; font-size: 11px; }
/* 预览失败态：越权 / 课程已删除时的说明卡 */
.preview-empty {
  display: flex; align-items: center; flex-direction: column; gap: 8px;
  padding: 56px 20px; color: #667085; background: #fff; border: 1px solid #e4e9f0; border-radius: 8px; text-align: center;
  i { color: #f79009; font-size: 30px; }
  strong { color: #101828; font-size: 15px; }
  span { font-size: 12.5px; }
}
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
.chapter-title { display: flex; width: calc(100% - 18px); align-items: center; gap: 8px; }.chapter-number { color: #1764f5; font-size: 10px; font-weight: 700; }.chapter-copy { min-width: 0; flex: 1; }.chapter-name, .chapter-intro { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.chapter-name { font-size: 12px; }.chapter-intro { margin-top: 3px; color: #98a2b3; font-size: 10px; }.chapter-title small { color: #98a2b3; font-size: 10px; }
.item-link { display: flex; width: 100%; align-items: center; gap: 8px; padding: 10px 7px; border: 0; color: #667085; text-align: left; background: transparent; cursor: pointer; }.item-link:hover, .item-link.selected { background: #f2f7ff; }.item-link.selected .item-link-copy b { color: #1764f5; }.item-status { display: flex; width: 23px; height: 23px; flex: 0 0 23px; align-items: center; justify-content: center; color: #98a2b3; border-radius: 50%; background: #f0f2f5; font-size: 12px; }.item-status.done { color: #fff; background: #23966f; }.item-status.in_progress { color: #fff; background: #2878c7; }.item-link-copy { min-width: 0; flex: 1; }.item-link-copy b, .item-link-copy small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.item-link-copy b { color: #475467; font-size: 11px; font-weight: 500; }.item-link-copy small { margin-top: 3px; color: #98a2b3; font-size: 10px; }.item-state { flex: 0 0 auto; color: #98a2b3; font-size: 10px; }
.study-area { min-width: 0; padding: 20px 24px 24px; background: #fbfcfe; }.study-card { min-height: 510px; border: 1px solid #e4e9f0; background: #fff; }.study-card-head { display: flex; align-items: flex-start; justify-content: space-between; padding: 18px 20px; border-bottom: 1px solid #edf0f4; }.study-kicker { color: #2878c7; font-size: 11px; }.study-card-head h2 { margin: 6px 0 0; color: #1d2939; font-size: 17px; font-weight: 600; }
.reader-wrap, .video-wrap { padding: 18px 20px; }.document-reader { display: flex; height: min(70vh, 760px); min-height: 560px; flex-direction: column; overflow: hidden; border: 1px solid #e1e6ed; background: #f2f4f7; }.docx-reader { overflow-y: auto; }.pptx-reader { align-items: center; justify-content: center; background: #202733; }.document-frame { display: block; width: 100%; min-height: 0; flex: 1; border: 0; background: #fff; }.document-end { flex: 0 0 auto; margin: 0 16px; padding: 11px 0; border-top: 1px dashed #ccd5df; color: #23966f; font-size: 12px; text-align: center; }.docx-content, .pptx-content { width: 100%; min-height: 100%; }.docx-content { background: #e9edf2; }.pptx-content { display: flex; align-items: center; justify-content: center; overflow: hidden; background: #202733; }.document-preview-state { display: flex; min-height: 100%; align-items: center; justify-content: center; flex-direction: column; gap: 9px; color: #667085; background: #f8fafc; text-align: center; }.document-preview-state > i { color: #2878c7; font-size: 38px; }.document-preview-state strong { color: #344054; font-size: 15px; }.document-preview-state span { max-width: 80%; font-size: 11px; }.document-preview-state.is-error > i { color: #d97706; }.docx-content ::v-deep .docx-wrapper { min-height: 100%; padding: 24px 12px; background: #e9edf2; }.docx-content ::v-deep .docx-wrapper > section.docx { max-width: calc(100% - 24px); margin: 0 auto 18px; box-shadow: 0 2px 8px rgba(16, 24, 40, .12); }.pptx-content ::v-deep > div { margin: auto; }.download-panel { display: flex; min-height: 520px; align-items: center; justify-content: center; flex-direction: column; gap: 10px; border: 1px solid #e1e6ed; background: #f8fafc; color: #667085; text-align: center; }.download-panel > i { color: #4d91ce; font-size: 42px; }.download-panel strong { max-width: 80%; overflow: hidden; color: #344054; font-size: 15px; text-overflow: ellipsis; white-space: nowrap; }.download-panel span { font-size: 11px; }.study-card-head .head-actions { display: flex; align-items: center; gap: 8px; flex: none; }.download-panel-actions { display: flex; align-items: center; gap: 10px; }.reader-status, .video-status { display: flex; align-items: center; justify-content: space-between; padding-top: 11px; color: #8490a0; font-size: 11px; }.reader-ready { color: #23966f; }.reader-ready i { margin-right: 3px; }
.item-intro-panel { margin-top: 12px; padding: 13px 15px; border-left: 3px solid #2878c7; background: #f7faff; }.item-intro-panel span { color: #2878c7; font-size: 12px; font-weight: 600; }.item-intro-panel i { margin-right: 5px; }.item-intro-panel p { margin: 7px 0 0; color: #5f6f82; font-size: 12px; line-height: 1.7; white-space: pre-wrap; }
.video-stage { display: grid; width: 100%; aspect-ratio: 16 / 9; align-items: stretch; overflow: hidden; background: #111827; }.course-video { display: block; width: 100%; height: 100%; background: #111827; object-fit: contain; }.video-placeholder { display: flex; height: 100%; align-items: center; justify-content: center; flex-direction: column; gap: 9px; color: #d8e2ef; text-align: center; }.video-placeholder i { color: #e7b76d; font-size: 43px; }.video-placeholder strong { font-size: 16px; font-weight: 500; }.video-placeholder span { color: #9caec2; font-size: 11px; }.video-status { padding: 11px 0 0; }
.study-footer { display: flex; align-items: center; justify-content: space-between; gap: 14px; padding: 15px 20px; border-top: 1px solid #edf0f4; }.study-footer > div:first-child { display: flex; flex-wrap: wrap; gap: 12px; color: #98a2b3; font-size: 11px; }.save-state { color: #23966f; }.save-state i { margin-right: 3px; }.study-actions { display: flex; gap: 7px; }.study-empty { display: flex; height: 420px; align-items: center; justify-content: center; flex-direction: column; gap: 9px; color: #98a2b3; }.study-empty i { color: #b7c1cc; font-size: 34px; }.study-empty strong { color: #667085; font-size: 14px; font-weight: 500; }.study-empty span { font-size: 12px; }.detail-note { display: flex; align-items: center; gap: 8px; margin-top: 14px; padding: 12px 16px; border: 1px solid #dbeafe; color: #667085; background: #f5f9ff; font-size: 11px; }.detail-note > i { color: #2878c7; font-size: 15px; }.detail-note span { flex: 1; }.detail-note .el-button { padding: 0; font-size: 11px; }
@media (max-width: 900px) { .course-hero { flex-wrap: wrap; }.hero-copy { min-width: calc(100% - 86px); }.hero-progress { margin-left: 86px; }.detail-layout { grid-template-columns: 270px minmax(0, 1fr); } }
@media (max-width: 700px) { .course-detail-page { padding: 16px 12px 28px; }.course-hero { align-items: flex-start; padding: 18px; }.hero-mark { width: 52px; height: 52px; flex-basis: 52px; font-size: 23px; }.hero-copy { min-width: calc(100% - 70px); }.hero-copy h1 { font-size: 18px; }.hero-copy p { white-space: normal; line-height: 1.5; }.hero-progress { width: calc(100% - 70px); margin-left: 70px; }.course-hero > .el-button { width: 100%; }.course-toolbar { padding: 0 12px; }.course-tabs { gap: 18px; }.detail-layout { display: block; }.chapter-sidebar { border-right: 0; border-bottom: 1px solid #e4e9f0; }.chapter-sidebar ::v-deep .el-collapse-item__content { max-height: 220px; overflow-y: auto; }.study-area { padding: 12px; }.study-card-head { padding: 14px; }.reader-wrap, .video-wrap { padding: 12px; }.document-reader { height: min(65vh, 640px); min-height: 460px; }.download-panel { min-height: 420px; }.document-sheet { padding: 24px 20px 32px; }.study-footer { align-items: flex-start; flex-direction: column; padding: 14px; }.study-actions { width: 100%; justify-content: flex-end; flex-wrap: wrap; }.detail-note .el-button { display: none; } }
</style>
