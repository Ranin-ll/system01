<template>
  <div class="practice-page">
    <div class="exam-breadcrumb">
      <el-button type="text" icon="el-icon-arrow-left" @click="goBack">返回</el-button>
      <span>/</span>
      <b>模拟考核</b>
      <template v-if="fromModuleId">
        <span>/</span>
        <b>{{ moduleLabel }}</b>
      </template>
      <span>/</span>
      <b>实操题详情</b>
    </div>

    <div v-loading="loading" class="detail-wrap">
      <div v-if="!loading && !subject" class="empty-state">
        <i class="el-icon-document-delete" />
        <span>实操题不存在或未发布</span>
      </div>

      <template v-else-if="subject">
        <header class="exam-heading">
          <div>
            <span class="eyebrow">PRACTICE · DETAIL</span>
            <h1>{{ subject.title || '实操题详情' }}</h1>
          </div>
        </header>

        <div class="grid">
          <!-- 左：参考 + 缩略图 -->
          <div class="card c7">
            <div class="bigimg">
              <video v-if="currentImage && isVideo(currentImage.url)" :src="baseApi + currentImage.url" controls class="bigimg-video" />
              <img v-else-if="currentImage" :src="baseApi + currentImage.url" :alt="currentImage.name">
              <span v-else class="bigimg-ph" v-html="placeholderSvg" />
            </div>
            <div v-if="images.length > 1" class="tstrip">
              <div
                v-for="(img, i) in images"
                :key="i"
                class="t"
                :class="{ on: i === activeIndex }"
                @click="activeIndex = i"
              >
                <video v-if="isVideo(img.url)" :src="baseApi + img.url" muted />
                <img v-else :src="baseApi + img.url" :alt="img.name">
              </div>
            </div>
            <div v-else-if="!images.length" class="note" style="text-align:center">该题未提供参考</div>
          </div>

          <!-- 右：基本信息与题干 -->
          <div class="card c5">
            <h3 class="p-title">{{ subject.title || '未命名实操题' }}</h3>

            <div class="info2">
              <div><span>所属模块</span><b>{{ subject.moduleName || '未归属模块' }}</b></div>
              <div><span>建议用时</span><b>{{ subject.estimatedMinutes ? subject.estimatedMinutes + ' 分钟' : '不限' }}</b></div>
              <div><span>难度</span><b>{{ difficultyText(subject.difficulty) }}</b></div>
              <div><span>发布时间</span><b>{{ fmtTime(subject.updateTime || subject.createTime) }}</b></div>
            </div>

            <div class="grp-sub">题干</div>
            <div class="stem">{{ subject.content }}</div>

            <div v-if="attachments.length" class="attach-wrap">
              <div class="grp-sub">题目附件</div>
              <a v-for="(a, i) in attachments" :key="i" class="attach-link" :href="baseApi + a.url" target="_blank" rel="noopener">
                <i class="el-icon-download" />{{ a.name || ('附件' + (i + 1)) }}
              </a>
            </div>
            <div v-else class="note" style="margin-top:12px"><i class="el-icon-paperclip" /> 该题未提供附件</div>
          </div>
        </div>
      </template>
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
      subject: null,
      activeIndex: 0
    }
  },
  computed: {
    /** 来源模块（从模块内实操题卡片进来时携带 ?moduleId=），用于返回模块内内容 */
    fromModuleId() {
      const v = this.$route.query.moduleId
      return v == null || v === '' ? null : String(v)
    },
    moduleLabel() {
      return (this.subject && this.subject.moduleName) || '当前模块'
    },
    images() {
      return this.subject ? this.parseAttachments(this.subject.referenceImages) : []
    },
    currentImage() {
      return this.images.length ? this.images[Math.min(this.activeIndex, this.images.length - 1)] : null
    },
    attachments() {
      return this.subject ? this.parseAttachments(this.subject.attachmentsJson) : []
    },
    /** 无参考时的大图占位（与设计稿同款线框风格） */
    placeholderSvg() {
      return '<svg viewBox="0 0 520 300" preserveAspectRatio="none">' +
        '<rect x="30" y="40" width="460" height="220" rx="12" fill="#f2f7ff"/>' +
        '<rect x="30" y="40" width="460" height="34" rx="12" fill="#d7e7fc"/>' +
        '<circle cx="52" cy="57" r="5" fill="#b6d0f8"/><circle cx="70" cy="57" r="5" fill="#b6d0f8"/><circle cx="88" cy="57" r="5" fill="#b6d0f8"/>' +
        '<rect x="60" y="98" width="140" height="10" rx="5" fill="#b6d0f8"/>' +
        '<rect x="60" y="122" width="210" height="10" rx="5" fill="#cfe0fb"/>' +
        '<rect x="90" y="146" width="170" height="10" rx="5" fill="#dbe8fc"/>' +
        '<rect x="90" y="170" width="120" height="10" rx="5" fill="#cfe0fb"/>' +
        '<rect x="60" y="194" width="90" height="10" rx="5" fill="#b6d0f8"/>' +
        '<rect x="330" y="98" width="130" height="106" rx="8" fill="#e0ebfd"/></svg>'
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
        this.goBack()
        return
      }
      this.loading = true
      this.subject = null
      this.activeIndex = 0
      getPracticeSubjectDetail(id).then(res => {
        this.subject = res.data || null
        this.loading = false
      }).catch(() => {
        // 未发布、非本部门或已删除时后端已提示，这里落回空态
        this.loading = false
        this.subject = null
      })
    },
    difficultyText(v) {
      return { EASY: '简单', MEDIUM: '中等', HARD: '困难' }[v] || '中等'
    },
    /**
     * 返回上一级：从模块内进来时回到「模拟考核 › 该模块」的模块内容页，
     * 否则回落到「模拟考核」模块列表。不再跳转到已下线的独立「实操题库」页。
     */
    goBack() {
      const mid = this.fromModuleId
      if (mid) {
        this.$router.push({ path: '/assessment/intern/learning/mock', query: { moduleId: mid } })
      } else {
        this.$router.push('/assessment/intern/learning/mock')
      }
    }
  }
}
</script>

<style lang="scss" scoped>
@import '~@/assets/styles/practice-module.scss';

$blue: #1764f5;
$ink: #1d2939;
$ink-2: #344054;
$ink-4: #98a2b3;
$line: #e4e9f0;
$line-2: #eef1f6;
$panel-2: #f7f9fc;

.detail-wrap { min-height: 220px; }

.grid { display: grid; grid-template-columns: repeat(12, 1fr); gap: 14px; }
.card { background: #fff; border: 1px solid $line; border-radius: 10px; padding: 16px 18px; }
.c7 { grid-column: span 7; }
.c5 { grid-column: span 5; }

/* --- 左：参考 --- */
.bigimg {
  background: $panel-2; border: 1px solid $line-2; border-radius: 10px;
  height: 340px; display: flex; align-items: center; justify-content: center; overflow: hidden;
}
.bigimg img { width: 100%; height: 100%; object-fit: contain; display: block; }
.bigimg .bigimg-video { width: 100%; height: 100%; object-fit: contain; background: #000; display: block; }
.bigimg-ph { display: block; width: 100%; height: 100%; }
.bigimg-ph ::v-deep svg { width: 100%; height: 100%; }
.tstrip { display: grid; grid-template-columns: repeat(5, 1fr); gap: 10px; margin-top: 12px; }
.tstrip .t {
  height: 58px; border: 1px solid $line-2; border-radius: 7px; background: $panel-2;
  overflow: hidden; cursor: pointer;
}
.tstrip .t img { width: 100%; height: 100%; object-fit: cover; display: block; }
.tstrip .t video { width: 100%; height: 100%; object-fit: cover; display: block; background: #000; }
.tstrip .t.on { border: 2px solid $blue; }

/* --- 右：要求区 --- */
.p-title { margin: 0 0 4px; font-size: 18px; color: $ink; font-weight: 600; line-height: 1.4; }
.info2 { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin: 12px 0 4px; }
.info2 > div { border: 1px solid $line-2; border-radius: 8px; padding: 9px 12px; background: $panel-2; }
.info2 span { display: block; font-size: 11px; color: $ink-4; margin-bottom: 3px; }
.info2 b { font-size: 13px; color: $ink; font-weight: 600; }
.grp-sub { font-size: 12.5px; font-weight: 600; color: $ink-2; margin: 14px 0 7px; }
.stem { font-size: 12.5px; color: $ink-2; line-height: 1.7; white-space: pre-wrap; }
.attach-wrap { margin-top: 6px; }
.attach-wrap .attach-link { display: block; margin-bottom: 5px; }
.note { font-size: 11.5px; color: $ink-4; margin-top: 12px; line-height: 1.55; }

@media (max-width: 1100px) {
  .c7, .c5 { grid-column: span 12; }
  .bigimg { height: 240px; }
}
</style>
