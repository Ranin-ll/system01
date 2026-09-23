<template>
  <div class="ipd-page">
    <!-- 返回 + 面包屑 -->
    <div class="ipd-back">
      <button type="button" class="ipd-back-btn" @click="backToList">
        <i class="el-icon-arrow-left" /> 返回模拟试题
      </button>
      <span class="ipd-crumb">
        <template v-if="bankName">{{ bankName }} · </template>
        <b>{{ subject.title || '实操题详情' }}</b>
      </span>
      <span v-if="siblings.length" class="ipd-crumb-idx">第 {{ seq }} / {{ total }} 题</span>
    </div>

    <!-- 取数失败 / 不可见：给明确原因与出口，不留空壳 -->
    <section v-if="failed" class="ipd-empty">
      <i class="el-icon-warning-outline" />
      <strong>无法查看该实操题</strong>
      <span>{{ failReason }}</span>
      <el-button size="small" type="primary" @click="backToList">返回模拟试题</el-button>
    </section>

    <template v-else>
      <div class="ipd-grid" v-loading="loading">
        <!-- 左：参考素材（主图 + 缩略图条） -->
        <section class="ipd-panel ipd-gallery">
          <template v-if="gallery.length">
            <div class="ipd-stage">
              <video v-if="currentIsVideo" :src="assetUrl(currentFile.url)" controls class="ipd-stage-media" />
              <img v-else :src="assetUrl(currentFile.url)" :alt="currentFile.name || subject.title" class="ipd-stage-media">
            </div>
            <div v-if="gallery.length > 1" class="ipd-shots">
              <button
                v-for="(f, i) in gallery"
                :key="'g' + i"
                type="button"
                class="ipd-shot"
                :class="{ on: i === activeIdx }"
                :style="shotStyle(f)"
                :aria-label="'参考素材 ' + (i + 1)"
                @click="activeIdx = i"
              >
                <i v-if="isVideo(f.url)" class="el-icon-video-camera-solid" />
              </button>
            </div>
          </template>
          <!-- 没有参考素材：明确说明，不留空白框 -->
          <div v-else class="ipd-stage ipd-stage-ph">
            <i class="el-icon-picture-outline" />
            <strong>本题目没有参考素材</strong>
            <span>练习时请依据右侧「交付要求」与「建模约束」自行准备参考。</span>
          </div>
        </section>

        <!-- 右：题目信息 -->
        <aside class="ipd-panel ipd-info">
          <h1>{{ subject.title || '—' }}</h1>

          <div class="ipd-facts">
            <div>
              <span>试题类型</span>
              <strong>{{ dirName }}</strong>
            </div>
            <div>
              <span>建议用时</span>
              <strong>{{ subject.estimatedMinutes ? subject.estimatedMinutes + ' 分钟' : '未设定' }}</strong>
            </div>
            <div>
              <span>难度</span>
              <strong>{{ diffText }}</strong>
            </div>
            <div>
              <span>建议满分</span>
              <strong>{{ subject.suggestScore != null ? subject.suggestScore + ' 分' : '—' }}</strong>
            </div>
          </div>

          <h3>交付要求</h3>
          <ul v-if="deliverableList.length">
            <li v-for="(t, i) in deliverableList" :key="'d' + i">{{ t }}</li>
          </ul>
          <p v-else class="ipd-none">未填写交付要求。</p>

          <h3>建模约束</h3>
          <ul v-if="keyPoints.length">
            <li v-for="(t, i) in keyPoints" :key="'k' + i">{{ t }}</li>
          </ul>
          <p v-else class="ipd-none">未填写建模约束。</p>

          <div class="ipd-specs">
            <div class="ipd-spec">
              <span>提交格式</span>
              <strong>{{ subject.submitFormat || '—' }}</strong>
            </div>
            <div class="ipd-spec">
              <span>命名规则</span>
              <strong>{{ subject.namingRule || '—' }}</strong>
            </div>
          </div>

          <template v-if="atts.length">
            <button type="button" class="ipd-download" @click="downloadAll">
              <i class="el-icon-download" />
              下载附件<template v-if="atts.length > 1">（{{ atts.length }} 个）</template>
            </button>
            <div v-if="atts.length > 1" class="ipd-att-list">
              <a
                v-for="(f, i) in atts"
                :key="'a' + i"
                :href="assetUrl(f.url)"
                target="_blank"
                rel="noopener"
              >{{ f.name || ('附件 ' + (i + 1)) }}</a>
            </div>
          </template>
          <button v-else type="button" class="ipd-download ghost" disabled>
            <i class="el-icon-download" /> 本题目未提供附件
          </button>
        </aside>
      </div>

      <div class="ipd-callout">
        <i class="el-icon-info" />
        <span>正式实操考核需按「交付要求」上传文件；这里只是只读练习参考，练习过程不会记录、也不会上报。</span>
      </div>

      <!-- 上一题 / 下一题 -->
      <div class="ipd-nav">
        <el-button size="small" icon="el-icon-arrow-left" :disabled="!prevSubject" @click="goSibling(prevSubject)">
          上一题{{ prevSubject ? '：' + shortTitle(prevSubject) : '' }}
        </el-button>
        <el-button size="small" @click="backToList">返回列表</el-button>
        <el-button size="small" type="primary" plain :disabled="!nextSubject" @click="goSibling(nextSubject)">
          下一题{{ nextSubject ? '：' + shortTitle(nextSubject) : '' }} <i class="el-icon-arrow-right" />
        </el-button>
      </div>
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

/** 多行文本 → 条目数组（每行一条，过滤空行与行首序号/符号） */
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
      siblings: [],
      /** 当前选中的参考素材下标（图库主图区） */
      activeIdx: 0
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
    diffText() { return { EASY: '简单', MEDIUM: '中等', HARD: '困难' }[diffKey(this.subject.difficulty)] },
    keyPoints() { return toLines(this.subject.devConstraints) },
    deliverableList() { return toLines(this.subject.deliverables) },
    /** 图库 = 参考图 / 视频（图片排在前面，视频在后） */
    gallery() {
      const list = parseList(this.subject.referenceImages).filter(f => f && f.url)
      return list.filter(f => !this.isVideo(f.url)).concat(list.filter(f => this.isVideo(f.url)))
    },
    currentFile() { return this.gallery[this.activeIdx] || {} },
    currentIsVideo() { return this.isVideo(this.currentFile.url) },
    atts() { return parseList(this.subject.attachmentsJson).filter(f => f && f.url) }
  },
  watch: {
    // 切题时回到第一张参考图
    subjectId() { this.activeIdx = 0 }
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
          this.activeIdx = 0
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
    assetUrl(url) { return this.baseApi + url },
    goSibling(s) {
      if (!s) return
      this.$router.replace('/assessment/intern/learning/practice-bank/' + this.bankId + '/subject/' + s.id)
      this.subject = Object.assign({}, s)
      this.activeIdx = 0
      window.scrollTo({ top: 0, behavior: 'smooth' })
    },
    backToList() {
      // 带上 ?bank= ⇒ 列表页回到同一个题库的分组（而不是从头开始）
      this.$router.push({ path: '/assessment/intern/learning/practice-bank', query: this.bankId ? { bank: this.bankId } : {} })
    },
    /** 有附件才可点；多个依次触发（浏览器可能拦截，故下方同时给出逐个链接） */
    downloadAll() {
      if (!this.atts.length) return
      this.atts.forEach((f, i) => {
        setTimeout(() => { window.open(this.assetUrl(f.url), '_blank') }, i * 350)
      })
    },
    shotStyle(f) {
      if (!f || !f.url) return {}
      return this.isVideo(f.url) ? {} : { backgroundImage: 'url("' + this.assetUrl(f.url) + '")' }
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

/* ── 返回 / 面包屑 ──────────────────────────── */
.ipd-back { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.ipd-back-btn { padding: 0; border: 0; background: transparent; color: #1768ed; font-size: 13.5px; cursor: pointer; }
.ipd-back-btn:hover { color: #0f58d1; }
.ipd-crumb { overflow: hidden; color: #8997aa; font-size: 13px; white-space: nowrap; text-overflow: ellipsis; }
.ipd-crumb b { color: #1d2734; font-weight: 600; }
.ipd-crumb-idx { flex: none; margin-left: auto; color: #98a2b3; font-size: 12px; }

/* ── 两栏：左图库 / 右信息 ──────────────────── */
.ipd-grid { display: grid; grid-template-columns: minmax(0, 1.2fr) minmax(340px, .8fr); gap: 18px; align-items: start; }
.ipd-panel { background: #fff; border: 1px solid #e0e7ef; border-radius: 8px; }

.ipd-gallery { padding: 18px; }
.ipd-stage { display: flex; overflow: hidden; aspect-ratio: 4 / 3; align-items: center; justify-content: center; background: #f4f7fb; border-radius: 6px; }
.ipd-stage-media { display: block; width: 100%; height: 100%; object-fit: contain; background: #0f1419; }
.ipd-stage-ph { flex-direction: column; gap: 10px; padding: 24px; color: #8492a5; background: linear-gradient(135deg, #f2f6fc, #fbfdff); text-align: center; }
.ipd-stage-ph i { color: #c3ccd8; font-size: 40px; }
.ipd-stage-ph strong { color: #475467; font-size: 14px; font-weight: 600; }
.ipd-stage-ph span { max-width: 340px; font-size: 12.5px; line-height: 1.7; }

.ipd-shots { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 8px; margin-top: 10px; }
.ipd-shot { position: relative; aspect-ratio: 4 / 3; padding: 0; overflow: hidden; border: 2px solid transparent; border-radius: 5px; background: #eef2f7 center / cover no-repeat; cursor: pointer; transition: border-color .15s; }
.ipd-shot:hover { border-color: #b9d2ff; }
.ipd-shot.on { border-color: #1768ed; }
.ipd-shot i { display: flex; height: 100%; align-items: center; justify-content: center; color: #fff; background: rgba(15, 20, 25, .55); font-size: 18px; }

.ipd-info { padding: 24px; }
.ipd-info h1 { margin: 0 0 20px; color: #1d2734; font-size: 22px; font-weight: 600; line-height: 1.4; }
.ipd-facts { display: grid; grid-template-columns: 1fr 1fr; gap: 14px 10px; padding: 15px 0; margin-bottom: 18px; border-top: 1px solid #edf1f5; border-bottom: 1px solid #edf1f5; }
.ipd-facts span, .ipd-spec span { display: block; margin-bottom: 6px; color: #8b99aa; font-size: 12.5px; }
.ipd-facts strong, .ipd-spec strong { display: block; color: #233145; font-size: 14px; font-weight: 600; word-break: break-word; }
.ipd-info h3 { margin: 18px 0 8px; color: #1d2734; font-size: 14px; font-weight: 600; }
.ipd-info ul { margin: 0; padding-left: 20px; color: #536276; font-size: 13px; line-height: 1.85; }
.ipd-info ul li::marker { color: #98a2b3; }
.ipd-none { margin: 0; color: #98a2b3; font-size: 12.5px; }

.ipd-specs { display: grid; grid-template-columns: 1fr; gap: 12px; padding-top: 14px; margin-top: 4px; border-top: 1px solid #f2f5f9; }
.ipd-spec { padding: 10px 12px; background: #f8fafc; border-radius: 6px; }

.ipd-download { display: flex; width: 100%; align-items: center; justify-content: center; gap: 6px; padding: 12px; margin-top: 22px; border: 0; border-radius: 5px; background: #1768ed; color: #fff; font-size: 14.5px; cursor: pointer; transition: background .15s; }
.ipd-download:hover { background: #0f58d1; }
.ipd-download i { font-size: 15px; }
.ipd-download.ghost { color: #98a2b3; background: #f2f4f7; cursor: not-allowed; }
.ipd-download.ghost:hover { background: #f2f4f7; }
.ipd-att-list { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 10px; }
.ipd-att-list a { padding: 4px 9px; color: #1768ed; background: #f1f7ff; border-radius: 4px; font-size: 12px; text-decoration: none; }
.ipd-att-list a:hover { background: #e4efff; }

.ipd-callout { display: flex; align-items: flex-start; gap: 9px; margin-top: 16px; padding: 12px 16px; color: #475467; background: #f6faff; border: 1px solid #dbe9ff; border-radius: 8px; font-size: 12.5px; line-height: 1.75; }
.ipd-callout i { margin-top: 2px; color: #1768ed; }

.ipd-nav { display: flex; align-items: center; gap: 10px; margin-top: 16px; }
.ipd-nav .el-button:nth-child(2) { margin-left: auto; }

.ipd-empty { display: flex; flex-direction: column; align-items: center; gap: 10px; padding: 70px 16px; background: #fff; border: 1px solid #e7ecf3; border-radius: 10px; text-align: center; }
.ipd-empty i { color: #f79009; font-size: 34px; }
.ipd-empty strong { color: #1d2939; font-size: 15px; }
.ipd-empty span { color: #667085; font-size: 13px; line-height: 1.7; }

@media (max-width: 1180px) {
  .ipd-grid { grid-template-columns: minmax(0, 1fr); }
}
@media (max-width: 900px) {
  .ipd-back { flex-wrap: wrap; }
  .ipd-crumb-idx { margin-left: 0; }
  .ipd-facts { grid-template-columns: minmax(0, 1fr); }
}
</style>
