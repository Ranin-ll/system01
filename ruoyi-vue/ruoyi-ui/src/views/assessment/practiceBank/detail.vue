<template>
  <div class="ipd-page">
    <div class="ipd-breadcrumb">
      <el-button type="text" icon="el-icon-arrow-left" @click="backToList">返回模拟实操题</el-button>
      <span>/</span>
      <em v-if="bankName">{{ bankName }}</em>
      <span v-if="bankName">/</span>
      <b>{{ subject.title || '实操题详情' }}</b>
    </div>

    <!-- 取数失败 / 不可见：给明确原因与出口，不留空壳 -->
    <section v-if="failed" class="ipd-empty">
      <i class="el-icon-warning-outline" />
      <strong>无法查看该实操题</strong>
      <span>{{ failReason }}</span>
      <el-button size="small" type="primary" @click="backToList">返回模拟实操题</el-button>
    </section>

    <template v-else>
      <section class="ipd-hero" v-loading="loading">
        <div class="ipd-hero-mark"><i class="el-icon-suitcase" /></div>
        <div class="ipd-hero-copy">
          <div class="ipd-hero-tags">
            <span class="ipd-tag blue">{{ dirName }}</span>
            <span class="ipd-tag" :class="diffTone">{{ diffText }}</span>
            <span v-if="chapter" class="ipd-tag">{{ chapter }}</span>
            <span class="ipd-tag plain">第 {{ seq }} / {{ total }} 题</span>
          </div>
          <h1>{{ subject.title }}</h1>
          <div class="ipd-hero-meta">
            <span v-if="subject.estimatedMinutes"><i class="el-icon-time" /> 建议用时 {{ subject.estimatedMinutes }} 分钟</span>
            <span v-if="subject.suggestScore != null"><i class="el-icon-medal" /> 建议满分 {{ subject.suggestScore }} 分</span>
            <span v-if="bankName"><i class="el-icon-folder-opened" /> {{ bankName }}</span>
          </div>
        </div>
        <div class="ipd-hero-side">
          <small>浏览模式</small>
          <strong>只读</strong>
          <span class="ipd-hero-note">不需要上传作答</span>
        </div>
      </section>

      <section v-if="loading" class="ipd-loading" v-loading="loading" />
      <template v-else>
        <!-- 题目描述 -->
        <section class="ipd-card">
          <div class="ipd-card-h"><span class="ipd-idx">题</span><h3>题目描述</h3></div>
          <pre v-if="subject.content" class="ipd-pre">{{ subject.content }}</pre>
          <p v-else class="ipd-none">未填写题目描述。</p>
        </section>

        <!-- 考核要点 -->
        <section class="ipd-card">
          <div class="ipd-card-h"><span class="ipd-idx o">点</span><h3>考核要点</h3><span class="ipd-hint">考核时按这些点逐条评分</span></div>
          <ol v-if="keyPoints.length" class="ipd-list">
            <li v-for="(t, i) in keyPoints" :key="'k' + i">{{ t }}</li>
          </ol>
          <p v-else class="ipd-none">未填写考核要点。</p>
        </section>

        <!-- 提交要求 -->
        <section class="ipd-card">
          <div class="ipd-card-h"><span class="ipd-idx g">交</span><h3>提交要求</h3></div>
          <ol v-if="deliverableList.length" class="ipd-list">
            <li v-for="(t, i) in deliverableList" :key="'d' + i">{{ t }}</li>
          </ol>
          <p v-else class="ipd-none">未填写交付要求。</p>
          <div class="ipd-kv">
            <div><span>提交格式</span><strong>{{ subject.submitFormat || '—' }}</strong></div>
            <div><span>命名规则</span><strong>{{ subject.namingRule || '—' }}</strong></div>
          </div>
        </section>

        <!-- 参考图 / 视频 -->
        <section class="ipd-card">
          <div class="ipd-card-h"><span class="ipd-idx p">参</span><h3>参考图 / 视频</h3><span class="ipd-hint">{{ refs.length }} 个</span></div>
          <div v-if="refs.length" class="ipd-files">
            <a v-for="(f, k) in refs" :key="'r' + k" :href="baseApi + f.url" target="_blank" rel="noopener" class="ipd-file">
              <i :class="isVideo(f.url) ? 'el-icon-video-camera' : 'el-icon-picture-outline'" />{{ f.name || '素材' }}
            </a>
          </div>
          <p v-else class="ipd-none">本题目没有参考素材。</p>
        </section>

        <!-- 附件 -->
        <section class="ipd-card">
          <div class="ipd-card-h"><span class="ipd-idx">附</span><h3>附件</h3><span class="ipd-hint">{{ atts.length }} 个</span></div>
          <div v-if="atts.length" class="ipd-files">
            <a v-for="(f, k) in atts" :key="'a' + k" :href="baseApi + f.url" target="_blank" rel="noopener" class="ipd-file">
              <i class="el-icon-paperclip" />{{ f.name || '附件' }}
            </a>
          </div>
          <p v-else class="ipd-none">本题目没有附件。</p>
        </section>

        <div class="ipd-callout">
          <i class="el-icon-info" />
          <span>正式实操考核需按「提交要求」上传文件；这里只是只读练习参考，练习过程不会记录、也不会上报。</span>
        </div>

        <!-- 上一题 / 下一题 -->
        <div class="ipd-nav">
          <el-button size="small" icon="el-icon-arrow-left" :disabled="!prevSubject" @click="goSibling(prevSubject)">上一题{{ prevSubject ? '：' + shortTitle(prevSubject) : '' }}</el-button>
          <el-button size="small" @click="backToList">返回列表</el-button>
          <el-button size="small" type="primary" plain :disabled="!nextSubject" @click="goSibling(nextSubject)">下一题{{ nextSubject ? '：' + shortTitle(nextSubject) : '' }} <i class="el-icon-arrow-right" /></el-button>
        </div>
      </template>
    </template>
  </div>
</template>

<script>
import request from '@/utils/request'

function parseList(v) {
  if (!v) return []
  try {
    const a = typeof v === 'string' ? JSON.parse(v) : v
    return Array.isArray(a) ? a : []
  } catch (e) { return [] }
}

/** 多行文本 → 条目数组（每行一条，过滤空行） */
function toLines(v) {
  return String(v || '').split(/\r?\n/).map(t => t.replace(/^[\s•\-*\d.、)]+/, '').trim()).filter(Boolean)
}

function diffKey(d) {
  const v = String(d || '').toUpperCase()
  return { EASY: 'EASY', MEDIUM: 'MEDIUM', HARD: 'HARD' }[v] || 'MEDIUM'
}

export default {
  name: 'InternPracticeSubject',
  data() {
    return {
      loading: false,
      failed: false,
      failReason: '',
      bankName: '',
      subject: {},
      siblings: []
    }
  },
  computed: {
    baseApi() { return process.env.VUE_APP_BASE_API || '' },
    bankId() { return this.$route.params.bankId },
    subjectId() { return Number(this.$route.params.subjectId) },
    seq() {
      const i = this.siblings.findIndex(s => s.id === this.subjectId)
      return i > -1 ? i + 1 : 1
    },
    total() { return this.siblings.length || 1 },
    prevSubject() {
      const i = this.siblings.findIndex(s => s.id === this.subjectId)
      return i > 0 ? this.siblings[i - 1] : null
    },
    nextSubject() {
      const i = this.siblings.findIndex(s => s.id === this.subjectId)
      return i > -1 && i < this.siblings.length - 1 ? this.siblings[i + 1] : null
    },
    dirName() { return this.subject.direction || '未分方向' },
    chapter() { return this.subject.chapter || '' },
    diffText() { return { EASY: '简单', MEDIUM: '中等', HARD: '困难' }[diffKey(this.subject.difficulty)] },
    diffTone() { return { EASY: 'green', MEDIUM: 'orange', HARD: 'red' }[diffKey(this.subject.difficulty)] },
    keyPoints() { return toLines(this.subject.devConstraints) },
    deliverableList() { return toLines(this.subject.deliverables) },
    refs() { return parseList(this.subject.referenceImages) },
    atts() { return parseList(this.subject.attachmentsJson) }
  },
  created() {
    this.load()
  },
  methods: {
    /**
     * 数据来源：复用实习生端既有只读接口（零新增）。
     * ① /business/practice-bank/enabled → 取题库名（面包屑）
     * ② /business/practice-bank/{bankId}/subjects → 取全量题目，再按 subjectId 定位
     *    ⇒ 路由自带 bankId/subjectId，刷新 / 直达 / 回退都可用；越权（别部门题库）后端返回空表。
     */
    load() {
      this.loading = true
      this.failed = false
      Promise.all([
        request({ url: '/business/practice-bank/enabled', method: 'get' }).catch(() => ({ data: [] })),
        request({ url: '/business/practice-bank/' + this.bankId + '/subjects', method: 'get' }).catch(() => ({ rows: [] }))
      ]).then(([bankRes, subRes]) => {
        const banks = (bankRes && bankRes.data) || []
        const bank = banks.find(b => String(b.bankId) === String(this.bankId))
        this.bankName = bank ? bank.bankName : ''
        this.siblings = (subRes && subRes.rows) || []
        const s = this.siblings.find(x => x.id === this.subjectId)
        if (s) {
          this.subject = Object.assign({}, s)
        } else {
          this.subject = {}
          this.failed = true
          this.failReason = !this.bankId || !this.subjectId
            ? '地址缺少题库或题目编号，请从「模拟实操题」列表进入。'
            : (this.bankName
              ? '该题目不存在，或已被管理员下架。'
              : '该题库未对实习生开放，或不属于你所在部门。')
        }
        this.loading = false
      }).catch(() => {
        this.loading = false
        this.failed = true
        this.failReason = '加载失败，请稍后重试或返回列表。'
      })
    },
    goSibling(s) {
      if (!s) return
      this.$router.replace('/assessment/intern/learning/practice-bank/' + this.bankId + '/subject/' + s.id)
      this.subject = Object.assign({}, s)
      window.scrollTo({ top: 0, behavior: 'smooth' })
    },
    backToList() {
      // 带上 ?bank= ⇒ 列表页回到同一个题库，而不是默认第一个
      this.$router.push({ path: '/assessment/intern/learning/practice-bank', query: this.bankId ? { bank: this.bankId } : {} })
    },
    shortTitle(s) {
      const t = String((s && s.title) || '')
      return t.length > 10 ? t.slice(0, 10) + '…' : t
    },
    isVideo(url) { return /\.(mp4|webm|ogg|ogv|mov|avi|m4v)(\?|#|$)/i.test(String(url || '')) }
  }
}
</script>

<style lang="scss" scoped>
.ipd-page { padding: 16px 18px 24px; }
.ipd-breadcrumb { display: flex; align-items: center; gap: 8px; margin-bottom: 14px; color: #98a2b3; font-size: 13px; }
.ipd-breadcrumb em { color: #667085; font-style: normal; }
.ipd-breadcrumb b { color: #1d2939; }

.ipd-hero { display: flex; align-items: flex-start; gap: 16px; padding: 18px 20px; background: #fff; border: 1px solid #e7ecf3; border-radius: 10px; }
.ipd-hero-mark { display: flex; width: 46px; height: 46px; flex: none; align-items: center; justify-content: center; color: #1764f5; background: #edf4ff; font-size: 22px; border-radius: 10px; }
.ipd-hero-copy { flex: 1; min-width: 0; }
.ipd-hero-tags { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 8px; }
.ipd-tag { padding: 2px 8px; color: #475467; background: #f2f4f7; border-radius: 4px; font-size: 11.5px; }
.ipd-tag.blue { color: #1764f5; background: #edf4ff; }
.ipd-tag.plain { color: #98a2b3; background: #fafbfc; }
.ipd-tag.green { color: #23966f; background: #eaf7f1; }
.ipd-tag.orange { color: #e6a23c; background: #fdf6ec; }
.ipd-tag.red { color: #d9534f; background: #fdeeed; }
.ipd-hero-copy h1 { margin: 0 0 8px; color: #1d2939; font-size: 19px; font-weight: 600; line-height: 1.4; }
.ipd-hero-meta { display: flex; flex-wrap: wrap; gap: 16px; color: #667085; font-size: 12.5px; }
.ipd-hero-meta i { margin-right: 3px; }
.ipd-hero-side { flex: none; padding-left: 18px; border-left: 1px solid #eef1f6; text-align: right; }
.ipd-hero-side small { display: block; color: #98a2b3; font-size: 11.5px; }
.ipd-hero-side strong { display: block; margin: 4px 0 2px; color: #1764f5; font-size: 18px; font-weight: 600; }
.ipd-hero-note { color: #98a2b3; font-size: 11.5px; }

.ipd-card { padding: 16px 20px; margin-top: 12px; background: #fff; border: 1px solid #e7ecf3; border-radius: 10px; }
.ipd-card-h { display: flex; align-items: center; gap: 9px; margin-bottom: 12px; }
.ipd-card-h h3 { margin: 0; color: #1d2939; font-size: 14px; font-weight: 600; }
.ipd-hint { margin-left: auto; color: #98a2b3; font-size: 11.5px; }
.ipd-idx { display: inline-flex; width: 22px; height: 22px; align-items: center; justify-content: center; color: #1764f5; background: #e8f1fd; border-radius: 4px; font-size: 12px; }
.ipd-idx.o { color: #e6a23c; background: #fdf6ec; }
.ipd-idx.g { color: #23966f; background: #eaf7f1; }
.ipd-idx.p { color: #7b5cf0; background: #f2eeff; }

.ipd-pre { margin: 0; padding: 12px 14px; color: #344054; background: #f8fafc; border-radius: 6px; font-family: inherit; font-size: 13px; line-height: 1.85; white-space: pre-wrap; word-break: break-word; }
.ipd-list { margin: 0; padding-left: 22px; color: #344054; font-size: 13px; line-height: 1.95; }
.ipd-list li::marker { color: #98a2b3; }
.ipd-none { margin: 0; color: #98a2b3; font-size: 12.5px; }
.ipd-kv { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 10px; margin-top: 14px; }
.ipd-kv > div { padding: 10px 12px; background: #f8fafc; border-radius: 6px; }
.ipd-kv span { display: block; color: #98a2b3; font-size: 11.5px; }
.ipd-kv strong { display: block; margin-top: 4px; color: #344054; font-size: 12.5px; font-weight: 500; word-break: break-word; }

.ipd-files { display: flex; flex-wrap: wrap; gap: 8px; }
.ipd-file { display: inline-flex; align-items: center; gap: 5px; padding: 5px 11px; color: #1764f5; background: #f1f7ff; border-radius: 5px; font-size: 12.5px; text-decoration: none; }
.ipd-file:hover { background: #e4efff; }

.ipd-callout { display: flex; align-items: flex-start; gap: 9px; margin-top: 12px; padding: 12px 16px; color: #475467; background: #f6faff; border: 1px solid #dbe9ff; border-radius: 8px; font-size: 12.5px; line-height: 1.75; }
.ipd-callout i { margin-top: 2px; color: #1764f5; }

.ipd-nav { display: flex; align-items: center; gap: 10px; margin-top: 16px; }
.ipd-nav .el-button:nth-child(2) { margin-left: auto; }

.ipd-empty { display: flex; flex-direction: column; align-items: center; gap: 10px; padding: 70px 16px; background: #fff; border: 1px solid #e7ecf3; border-radius: 10px; text-align: center; }
.ipd-empty i { color: #f79009; font-size: 34px; }
.ipd-empty strong { color: #1d2939; font-size: 15px; }
.ipd-empty span { color: #667085; font-size: 13px; line-height: 1.7; }
.ipd-loading { min-height: 200px; }

@media (max-width: 900px) {
  .ipd-hero { flex-wrap: wrap; }
  .ipd-hero-side { width: 100%; padding: 12px 0 0; border-top: 1px solid #eef1f6; border-left: 0; text-align: left; }
  .ipd-kv { grid-template-columns: minmax(0, 1fr); }
}
</style>
