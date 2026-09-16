<template>
  <div class="practice-page">
    <div class="exam-breadcrumb">
      <el-button type="text" icon="el-icon-arrow-left" @click="goList">返回实操练习</el-button>
      <span>/</span>
      <b>实操题详情</b>
    </div>

    <header class="exam-heading">
      <div>
        <span class="eyebrow">PRACTICE · DETAIL</span>
        <h1>实操题详情</h1>
        <p>本页仅供查看题干与下载附件，无需提交作答，也不会提交给管理员。</p>
      </div>
    </header>

    <div v-loading="loading" class="detail-body">
      <div v-if="!loading && !subject" class="empty-state">
        <i class="el-icon-document-delete" />
        <span>实操题不存在或未发布</span>
      </div>

      <template v-else-if="subject">
        <div class="detail-block">
          <div class="detail-block-title"><i class="el-icon-document" /><span>题干</span></div>
          <div class="subject-content">{{ subject.content }}</div>
        </div>

        <div class="detail-block">
          <div class="detail-block-title">
            <i class="el-icon-paperclip" /><span>参考资料附件</span>
            <el-tag v-if="attachments.length" size="mini" effect="plain" type="primary">{{ attachments.length }} 个</el-tag>
          </div>
          <div v-if="attachments.length" class="attach-block">
            <a v-for="(a, i) in attachments" :key="i" class="attach-link" :href="baseApi + a.url" target="_blank" rel="noopener">
              <i class="el-icon-download" />{{ a.name || ('附件' + (i + 1)) }}
            </a>
          </div>
          <div v-else class="attach-none"><i class="el-icon-paperclip" /> 该题未提供附件</div>
        </div>

        <div class="detail-meta">发布时间：{{ fmtTime(subject.updateTime || subject.createTime) }}</div>
      </template>

      <div class="action-bar">
        <el-button size="medium" @click="goList">返回实操练习</el-button>
      </div>
    </div>
  </div>
</template>

<script>
import { getPracticeSubjectDetail } from '@/api/business/practice'
import practiceMixin from './practice-mixin'

export default {
  name: 'InternPracticeSubjectDetail',
  mixins: [practiceMixin],
  data() {
    return {
      loading: false,
      subject: null
    }
  },
  computed: {
    attachments() {
      return this.subject ? this.parseAttachments(this.subject.attachmentsJson) : []
    }
  },
  watch: {
    // 同一路由下切换题目ID时重新加载
    '$route.params.id'() {
      this.load()
    }
  },
  created() {
    this.load()
  },
  methods: {
    load() {
      const id = this.$route.params.id
      if (!id) {
        this.goList()
        return
      }
      this.loading = true
      this.subject = null
      getPracticeSubjectDetail(id).then(res => {
        this.subject = res.data || null
        this.loading = false
      }).catch(() => {
        // 未发布、非本部门或已删除时后端已提示，这里落回空态
        this.loading = false
        this.subject = null
      })
    },
    goList() {
      this.$router.push('/assessment/intern/practice-subject')
    }
  }
}
</script>

<style lang="scss" scoped>
@import '~@/assets/styles/practice-module.scss';

.detail-body { min-height: 200px; }
.detail-meta { color: #98a2b3; font-size: 12px; text-align: right; }
</style>
