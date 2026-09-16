<template>
  <div class="practice-page">
    <div class="exam-breadcrumb">
      <el-button type="text" icon="el-icon-arrow-left" @click="goBack">返回工作台</el-button>
      <span>/</span>
      <b>实操练习</b>
    </div>

    <header class="exam-heading">
      <div>
        <span class="eyebrow">PRACTICE · HANDS-ON</span>
        <h1>实操练习</h1>
        <p>本部门已发布的实操练习题，可查看题干并下载附件。</p>
      </div>
    </header>

    <div class="subject-hint">
      <i class="el-icon-info" />
      <span>以下为本部门已发布的实操练习题，仅供查看题干与下载附件；无需提交作答，也不会提交给管理员。点击「查看详情」打开完整题干与附件。</span>
    </div>

    <div v-if="loading" class="empty-state"><i class="el-icon-loading" /><span>正在加载实操题…</span></div>
    <div v-else-if="!subjects.length" class="empty-state"><i class="el-icon-document" /><span>本部门暂未发布模拟实操题</span></div>
    <div v-else class="subject-list">
      <div v-for="(s, i) in subjects" :key="s.id" class="subject-card">
        <div class="subject-head">
          <span class="subject-idx">{{ i + 1 }}</span>
          <span class="subject-label">实操题 {{ i + 1 }}</span>
          <span class="subject-time">{{ fmtTime(s.updateTime || s.createTime) }}</span>
        </div>
        <div class="subject-content clamp">{{ s.content }}</div>
        <div class="subject-foot">
          <span v-if="attachCount(s)" class="attach-count"><i class="el-icon-paperclip" /> 附件 {{ attachCount(s) }} 个</span>
          <span v-else class="attach-none"><i class="el-icon-paperclip" /> 无附件</span>
          <el-button type="text" icon="el-icon-view" class="detail-btn" @click="goDetail(s)">查看详情</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { listPracticeSubjects } from '@/api/business/practice'
import practiceMixin from './practice-mixin'

export default {
  name: 'InternPracticeSubject',
  mixins: [practiceMixin],
  data() {
    return {
      loading: false,
      subjects: []
    }
  },
  created() {
    this.load()
  },
  methods: {
    load() {
      this.loading = true
      listPracticeSubjects().then(res => {
        this.subjects = res.data || []
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    attachCount(row) {
      return this.parseAttachments(row.attachmentsJson).length
    },
    /** 进入独立的实操题详情页 */
    goDetail(row) {
      this.$router.push('/assessment/intern/practice-subject/' + row.id)
    },
    goBack() {
      this.$router.push('/index')
    }
  }
}
</script>

<style lang="scss" scoped>
@import '~@/assets/styles/practice-module.scss';
</style>
