<script>
import CoursePage from './index'
import {
  addCourseChapter,
  addStudyItem,
  deleteCourseChapter,
  deleteStudyItem,
  getCourseContents,
  listCourseStudyRecords,
  saveCourseContents,
  updateCourseChapter,
  updateStudyItem,
  uploadStudyAsset
} from '@/api/business/courseContent'

function isDatabaseCourse(vm, course) {
  return Boolean(course && !vm.previewMode && /^\d+$/.test(String(course.id)))
}

function copy(value) {
  return JSON.parse(JSON.stringify(value))
}

export default Object.assign({}, CoursePage, {
  name: 'CourseRuntime',

  data() {
    const base = CoursePage.data ? CoursePage.data.call(this) : {}
    return Object.assign(base, { pendingAsset: null, assetUploadProgress: 0, assetUploadState: 'IDLE', assetUploadMessage: '' })
  },

  methods: Object.assign({}, CoursePage.methods, {
    handlePublish(course) {
      if (!isDatabaseCourse(this, course)) return CoursePage.methods.handlePublish.call(this, course)
      getCourseContents(course.id).then(response => {
        this.$set(this.contentStore, String(course.id), response.data || [])
        CoursePage.methods.handlePublish.call(this, course)
      }).catch(() => this.$modal.msgError('课程内容读取失败，暂不能发布'))
    },
    openContent(course) {
      this.currentCourse = course
      this.contentDirty = false
      this.contentDrawerOpen = true
      if (!isDatabaseCourse(this, course)) {
        this.ensureContent(course)
        return
      }
      const key = String(course.id)
      this.$set(this.contentStore, key, [])
      getCourseContents(course.id).then(response => {
        this.$set(this.contentStore, key, response.data || [])
      }).catch(() => {
        this.ensureContent(course)
        this.$modal.msgWarning('课程内容读取失败，当前显示演示数据')
      })
    },

    openRecords(course) {
      this.currentCourse = course
      this.recordStatusFilter = 'ALL'
      this.recordDrawerOpen = true
      if (!isDatabaseCourse(this, course)) return
      listCourseStudyRecords(course.id, { status: 'ALL' }).then(response => {
        this.$set(course, '_studyRecords', this.toRecordRows(response.data || [], course))
      }).catch(() => {
        this.$modal.msgWarning('学习记录读取失败')
      })
    },

    reloadContents() {
      if (!isDatabaseCourse(this, this.currentCourse)) return Promise.resolve()
      return getCourseContents(this.currentCourse.id).then(response => {
        this.$set(this.contentStore, String(this.currentCourse.id), response.data || [])
      })
    },

    markContentDirty() {
      this.contentDirty = true
    },

    saveContentDraft() {
      if (!isDatabaseCourse(this, this.currentCourse)) {
        this.saveContentStore()
        this.contentDirty = false
        this.$modal.msgSuccess('课程编排已保存')
        return
      }
      saveCourseContents(this.currentCourse.id, { chapters: copy(this.courseContents(this.currentCourse)) })
        .then(() => this.reloadContents())
        .then(() => {
          this.contentDirty = false
          this.$modal.msgSuccess('课程编排已保存')
        })
    },

    saveChapter() {
      if (!this.canEditContent(this.currentCourse)) return this.comingSoon()
      this.$refs.chapterForm.validate(valid => {
        if (!valid || !this.currentCourse) return
        if (!isDatabaseCourse(this, this.currentCourse)) {
          CoursePage.methods.saveChapter.call(this)
          return
        }
        const payload = {
          chapterName: this.chapterForm.chapterName,
          chapterIntro: this.chapterForm.chapterIntro,
          isRequired: this.chapterForm.isRequired
        }
        const request = this.chapterForm.id
          ? updateCourseChapter(this.chapterForm.id, payload)
          : addCourseChapter(this.currentCourse.id, payload)
        request.then(() => this.reloadContents()).then(() => {
          this.chapterDialogOpen = false
          this.$modal.msgSuccess('章节已保存')
        })
      })
    },

    removeChapter(chapter) {
      if (!this.canEditContent(this.currentCourse)) return this.comingSoon()
      this.$modal.confirm('确认删除章节“' + chapter.chapterName + '”？章节中的资料也会一并移除。').then(() => {
        if (!isDatabaseCourse(this, this.currentCourse)) {
          CoursePage.methods.removeChapter.call(this, chapter)
          return
        }
        return deleteCourseChapter(chapter.id).then(() => this.reloadContents()).then(() => {
          this.$modal.msgSuccess('章节已删除')
        })
      }).catch(() => {})
    },

    openItemDialog(chapter, item) {
      CoursePage.methods.openItemDialog.call(this, chapter, item)
      this.pendingAsset = null
      this.assetUploadProgress = 0
      this.assetUploadState = 'IDLE'
      this.assetUploadMessage = ''
    },
    handleAssetChange(file) {
      if (!file || !file.raw) return
      const raw = file.raw
      const parts = String(raw.name || '').split('.')
      this.pendingAsset = raw
      this.itemForm.fileName = raw.name
      this.itemForm.fileSize = raw.size || 0
      this.itemForm.fileExt = parts.length > 1 ? parts.pop().toLowerCase() : ''
      this.itemForm.assetStatus = 'LOCAL_ONLY'
      this.assetUploadProgress = 0
      this.assetUploadState = 'READY'
      this.assetUploadMessage = '文件已选择，保存资料后开始上传'
    },

    clearAsset() {
      this.pendingAsset = null
      this.itemForm.fileName = ''
      this.itemForm.fileSize = 0
      this.itemForm.fileExt = ''
      this.itemForm.assetStatus = 'UNBOUND'
      this.assetUploadProgress = 0
      this.assetUploadState = 'IDLE'
      this.assetUploadMessage = ''
      if (this.$refs.assetUpload) this.$refs.assetUpload.clearFiles()
    },

    assetUploadLabel() {
      const labels = {
        UPLOADING: '正在上传 ' + this.assetUploadProgress + '%',
        SUCCESS: '上传成功',
        ERROR: '上传失败，可重新选择后重试',
        READY: '待上传',
        IDLE: this.itemForm.assetStatus === 'UPLOADED' ? '已上传' : '待提交服务器'
      }
      return labels[this.assetUploadState] || labels.IDLE
    },

    uploadErrorMessage(error) {
      const message = error && error.message ? error.message : ''
      if (message.indexOf('Network Error') > -1) return '网络连接失败，请确认后端服务正常后重试'
      if (message.indexOf('413') > -1 || message.indexOf('size') > -1) return '文件大小超出限制，视频最大 500MB'
      return message || '上传失败，请重新选择文件后重试'
    },

    saveItem() {
      if (!this.canEditContent(this.currentCourse)) return this.comingSoon()
      this.$refs.itemForm.validate(valid => {
        if (!valid || !this.currentCourse) return
        if (!isDatabaseCourse(this, this.currentCourse)) {
          CoursePage.methods.saveItem.call(this)
          return
        }
        const payload = {
          itemTitle: this.itemForm.itemTitle,
          itemIntro: this.itemForm.itemIntro,
          itemType: this.itemForm.itemType,
          duration: this.itemForm.duration,
          isRequired: this.itemForm.isRequired,
          completionRule: this.itemForm.completionRule,
          quizJson: this.itemForm.quizJson,
          completionThreshold: this.itemForm.completionThreshold
        }
        const request = this.itemForm.id
          ? updateStudyItem(this.itemForm.id, payload)
          : addStudyItem(this.activeChapterId, payload)
        this.itemSubmitting = true
        let itemSaved = false
        request.then(response => {
          itemSaved = true
          const itemId = this.itemForm.id || (response.data && response.data.id)
          if (!this.pendingAsset || !itemId) return null
          const formData = new FormData()
          formData.append('file', this.pendingAsset)
          this.assetUploadState = 'UPLOADING'
          this.assetUploadProgress = 0
          this.assetUploadMessage = '正在上传 ' + this.itemForm.fileName
          return uploadStudyAsset(itemId, formData, event => {
            if (!event || !event.total) return
            this.assetUploadProgress = Math.min(99, Math.round(event.loaded * 100 / event.total))
            this.assetUploadMessage = '正在上传 ' + this.assetUploadProgress + '%'
          })
        }).then(() => this.reloadContents()).then(() => {
          if (this.pendingAsset) {
            this.assetUploadProgress = 100
            this.assetUploadState = 'SUCCESS'
            this.assetUploadMessage = '上传成功，文件已绑定到学习资料'
            this.itemForm.assetStatus = 'UPLOADED'
          }
          this.pendingAsset = null
          this.$modal.msgSuccess('学习资料已保存')
          window.setTimeout(() => { this.itemDialogOpen = false }, 800)
        }).catch(error => {
          this.assetUploadState = 'ERROR'
          this.assetUploadProgress = 100
          this.assetUploadMessage = this.uploadErrorMessage(error)
          if (itemSaved) {
            this.reloadContents().catch(() => {})
            this.$modal.msgWarning('基础信息已保存，但文件上传失败，请重新选择文件后重试')
          }
        }).finally(() => {
          this.itemSubmitting = false
        })
      })
    },

    removeItem(chapter, item) {
      if (!this.canEditContent(this.currentCourse)) return this.comingSoon()
      this.$modal.confirm('确认删除学习资料“' + item.itemTitle + '”？').then(() => {
        if (!isDatabaseCourse(this, this.currentCourse)) {
          CoursePage.methods.removeItem.call(this, chapter, item)
          return
        }
        return deleteStudyItem(item.id).then(() => this.reloadContents()).then(() => {
          this.$modal.msgSuccess('学习资料已删除')
        })
      }).catch(() => {})
    },

    courseRecords(course) {
      if (course && course._studyRecords) return course._studyRecords
      return CoursePage.methods.courseRecords.call(this, course)
    },

    toRecordRows(rows, course) {
      const groups = {}
      rows.forEach(row => {
        const key = String(row.userId)
        if (!groups[key]) {
          groups[key] = {
            name: row.studentName || '未命名学员',
            position: row.positionName || course.positionName || this.positionName(course.positionId) || '实习生',
            progressTotal: 0,
            completed: 0,
            total: 0,
            lastStudy: row.lastStudyTime || row.updateTime
          }
        }
        groups[key].progressTotal += Number(row.progress || 0)
        groups[key].completed += row.status === 'DONE' ? 1 : 0
        groups[key].total += 1
        if (String(row.lastStudyTime || row.updateTime || '') > String(groups[key].lastStudy || '')) groups[key].lastStudy = row.lastStudyTime || row.updateTime
      })
      return Object.keys(groups).map(key => {
        const item = groups[key]
        const progress = item.total ? Math.round(item.progressTotal / item.total) : 0
        return {
          name: item.name,
          position: item.position,
          progress,
          completed: item.completed,
          total: item.total,
          status: progress >= 100 ? 'DONE' : 'IN_PROGRESS',
          lastStudy: this.formatDate(item.lastStudy)
        }
      })
    }
  })
})
</script>
