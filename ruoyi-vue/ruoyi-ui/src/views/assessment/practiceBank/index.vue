<template>
  <div class="ipb-page">
    <!-- 顶部：标题 + 说明 + 统计（口径：只统计已加载的真实数据） -->
    <header class="ipb-hero">
      <div class="ipb-hero-copy">
        <h1>模拟实操题</h1>
        <p>
          这里是本部门开放的模拟实操题（只读练习），按<b>所属题库</b>分组排列。
          点任意一张题目卡进入详情，先照着练，再参加正式实操考核。
        </p>
      </div>
      <div class="ipb-hero-side">
        <div class="ipb-hero-stats">
          <div>
            <strong>{{ stats.subjects }}</strong>
            <span>道可练习题目</span>
          </div>
          <div>
            <strong>{{ stats.banks }}</strong>
            <span>个实操题库</span>
          </div>
          <div>
            <strong>{{ stats.maxMinutes || '—' }}</strong>
            <span>最长建议用时 / 分钟</span>
          </div>
        </div>
        <el-button size="mini" icon="el-icon-refresh" :loading="loading" @click="loadAll">刷新</el-button>
      </div>
    </header>

    <!-- 筛选条：关键词 + 方向 + 难度 + 显示 N / M + 重置 -->
    <div class="ipb-filter">
      <el-input
        v-model="filters.keyword"
        size="small"
        clearable
        prefix-icon="el-icon-search"
        placeholder="搜题名 / 题目描述 / 考核要点"
        class="ipb-filter-input"
      />
      <el-select v-model="filters.direction" size="small" clearable placeholder="全部方向" class="ipb-filter-select">
        <el-option v-for="d in directionOptions" :key="d" :label="d" :value="d" />
      </el-select>
      <el-select v-model="filters.difficulty" size="small" clearable placeholder="全部难度" class="ipb-filter-select">
        <el-option label="简单" value="EASY" />
        <el-option label="中等" value="MEDIUM" />
        <el-option label="困难" value="HARD" />
      </el-select>
      <el-button v-if="hasFilter" size="small" icon="el-icon-refresh-left" @click="resetFilters">重置</el-button>
      <span class="ipb-filter-sum">显示 {{ filteredCount }} / {{ stats.subjects }} 道</span>
    </div>

    <div v-loading="loading" class="ipb-body">
      <!-- 加载失败：与「业务上没有数据」严格区分，绝不伪装成空态 -->
      <div v-if="!loading && loadFailed" class="ipb-empty big">
        <i class="el-icon-warning-outline" />
        <p>题库数据加载失败，可能是网络或服务异常。<br />请稍后重试；若持续失败请联系管理员。</p>
        <el-button size="small" type="primary" plain icon="el-icon-refresh" @click="loadAll">重新加载</el-button>
      </div>

      <!-- 空态①：本部门没有开放题库 -->
      <div v-else-if="!loading && !groups.length" class="ipb-empty big">
        <i class="el-icon-folder-opened" />
        <p>本部门还没有开放的模拟实操题。<br />请等待管理员配置题库后刷新。</p>
      </div>

      <!-- 空态②a：题目拉取失败（≠ 真的没有题目）-->
      <div v-else-if="!loading && !stats.subjects && hasBad" class="ipb-empty big">
        <i class="el-icon-warning-outline" />
        <p>题库里的题目加载失败，可能是网络或服务异常。<br />请点下方按钮重试；若持续失败请联系管理员。</p>
        <el-button size="small" type="primary" plain icon="el-icon-refresh" @click="loadAll">重新加载</el-button>
      </div>

      <!-- 空态②b：确实还没有录入题目 -->
      <div v-else-if="!loading && !stats.subjects" class="ipb-empty big">
        <i class="el-icon-tickets" />
        <p>开放的实操题库里还没有录入题目。<br />请等待管理员补充后刷新。</p>
      </div>

      <!-- 空态③：筛没了（与「没有数据」区分开） -->
      <div v-else-if="!loading && !filteredCount" class="ipb-empty big">
        <i class="el-icon-search" />
        <p>当前筛选条件下没有匹配的题目。<br />换个关键词或难度，或清空筛选条件。</p>
        <el-button size="small" type="primary" plain @click="resetFilters">清空筛选</el-button>
      </div>

      <!-- 按题库分组：组标题 = 所属题库 -->
      <template v-else>
        <section
          v-for="g in visibleGroups"
          :key="g.bankId"
          class="ipb-section"
          :class="{ flash: flashBankId === g.bankId }"
          :id="'ipb-bank-' + g.bankId"
        >
          <div class="ipb-sec-head">
            <div class="ipb-sec-title">
              <h2>{{ g.bankName }}</h2>
              <p>{{ g.description || '参考题目要求完成建模练习，按规范命名与整理结构。' }}</p>
            </div>
            <span class="ipb-sec-count">
              {{ hasFilter ? g.shown.length + ' / ' + g.subjects.length : g.subjects.length }} 题
            </span>
          </div>

          <p v-if="g.bad" class="ipb-sec-none err">该题库的题目加载失败，请点右上角「刷新」重试。</p>
          <p v-else-if="!g.shown.length" class="ipb-sec-none">该题库还没有录入实操题，等待管理员补充。</p>
          <div v-else class="ipb-grid">
            <button
              v-for="s in g.shown"
              :key="s.id"
              type="button"
              class="ipb-card"
              @click="goDetail(g, s)"
            >
              <!-- 缩略图：有参考图用真图，没有则用明确的占位块（不编素材） -->
              <span class="ipb-thumb">
                <img v-if="thumb(s)" :src="thumb(s)" :alt="s.title" @error="onImgErr(s)">
                <span v-else class="ipb-thumb-ph" :class="phTone(s)">
                  <i :class="phIcon(s)" />
                  <em>{{ dirName(s) }}</em>
                </span>
                <span class="ipb-thumb-marks">
                  <em class="d" :class="'d-' + diffKey(s.difficulty)">{{ diffText(s.difficulty) }}</em>
                  <em v-if="s.suggestScore != null" class="sc">{{ s.suggestScore }} 分</em>
                </span>
              </span>
              <span class="ipb-copy">
                <strong :title="s.title">{{ s.title }}</strong>
                <small>
                  <template v-if="s.estimatedMinutes">建议用时 <i>{{ s.estimatedMinutes }} 分钟</i></template>
                  <template v-else>建议用时 <i class="none">未设定</i></template>
                </small>
              </span>
            </button>
          </div>
        </section>
      </template>
    </div>
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

/** 难度的「有效档位」：空 / 未知一律归入「中等」，与展示口径一致 */
function diffKey(d) {
  const v = String(d || '').toUpperCase()
  return { EASY: 'EASY', MEDIUM: 'MEDIUM', HARD: 'HARD' }[v] || 'MEDIUM'
}

/** 参考素材里第一张「图片」（视频不拿来当封面） */
function firstImage(list) {
  const img = list.find(f => f && f.url && !/\.(mp4|webm|ogg|ogv|mov|avi|m4v)(\?|#|$)/i.test(String(f.url)))
  return img || null
}

/** 方向 → 占位块配色（纯展示，不改数据） */
const PH_TONES = ['tone-a', 'tone-b', 'tone-c', 'tone-d', 'tone-e', 'tone-f']

export default {
  name: 'InternPracticeBank',
  data() {
    return {
      loading: false,
      /** 接口失败（≠ 业务上没有数据）—— 必须分开表达，否则故障会被伪装成空态 */
      loadFailed: false,
      /** [{ bankId, bankName, bankType, description, subjects: [] }] —— 按题库分组 */
      groups: [],
      filters: { keyword: '', direction: '', difficulty: '' },
      /** 图片加载失败的题目 id（挂了就退回占位块，不留破图） */
      brokenImg: {},
      /** 从详情页返回时高亮一下来源题库 */
      flashBankId: null
    }
  },
  computed: {
    baseApi() { return process.env.VUE_APP_BASE_API || '' },
    /** 全部题目打平（KPI / 筛选计数都以真实数据算） */
    allSubjects() {
      return this.groups.reduce((acc, g) => acc.concat(g.subjects || []), [])
    },
    stats() {
      const list = this.allSubjects
      const minutes = list.map(s => Number(s.estimatedMinutes) || 0)
      return {
        subjects: list.length,
        banks: this.groups.filter(g => (g.subjects || []).length).length,
        maxMinutes: minutes.length ? Math.max.apply(null, minutes) : 0
      }
    },
    /** 方向选项：按已加载题目去重（含「未分方向」） */
    directionOptions() {
      const set = {}
      this.allSubjects.forEach(s => { set[this.dirName(s)] = 1 })
      return Object.keys(set).sort()
    },
    hasFilter() {
      return !!(this.filters.keyword || this.filters.direction || this.filters.difficulty)
    },
    /** 是否有题库「拉题失败」—— 用于把故障与本就没有数据区分开 */
    hasBad() {
      return this.groups.some(g => g.bad)
    },
    filteredCount() {
      return this.visibleGroups.reduce((n, g) => n + g.shown.length, 0)
    },
    /**
     * 可见分组：每组带上「筛选后的可见题目」。
     * 未筛选时保留空题库（显示「还没录入题目」更诚实）；筛选时隐藏空组（否则全是空壳）。
     */
    visibleGroups() {
      const has = this.hasFilter
      return this.groups
        .map(g => Object.assign({}, g, { shown: (g.subjects || []).filter(s => this.match(s)) }))
        .filter(g => (has ? g.shown.length > 0 : true))
    }
  },
  created() {
    this.loadAll()
  },
  methods: {
    /** 零新增接口：先取开放题库，再并发取每个题库的题目（题库数量 = 部门开放数，量小） */
    loadAll() {
      this.loading = true
      this.loadFailed = false
      request({ url: '/business/practice-bank/enabled', method: 'get' }).then(res => {
        const banks = res.data || []
        if (!banks.length) {
          this.groups = []
          this.loading = false
          return
        }
        Promise.all(banks.map(b =>
          request({ url: '/business/practice-bank/' + b.bankId + '/subjects', method: 'get' })
            .then(r => Object.assign({}, b, { subjects: (r && r.rows) || [] }))
            // 单个题库失败 ⇒ 标记 bad，页面显示「加载失败，请刷新」，不冒充「该题库没有题目」
            .catch(() => Object.assign({}, b, { subjects: [], bad: true }))
        )).then(list => {
          this.groups = list
          this.loading = false
          this.$nextTick(this.focusQueryBank)
        })
      }).catch(() => {
        // 题库列表都没拿到 ⇒ 是真的失败，不能落进「本部门还没有开放题库」的空态
        this.groups = []
        this.loadFailed = true
        this.loading = false
      })
    },
    /** 从详情页返回时带 ?bank= ⇒ 滚到该题库并闪一下 */
    focusQueryBank() {
      const want = this.$route.query.bank ? String(this.$route.query.bank) : ''
      if (!want) return
      const el = document.getElementById('ipb-bank-' + want)
      if (!el) return
      el.scrollIntoView({ block: 'start', behavior: 'smooth' })
      this.flashBankId = want === '' ? null : Number(want)
      setTimeout(() => { this.flashBankId = null }, 1800)
    },
    match(s) {
      const diff = this.filters.difficulty
      const dir = this.filters.direction
      if (diff && diffKey(s.difficulty) !== diff) return false
      if (dir && this.dirName(s) !== dir) return false
      const kw = String(this.filters.keyword || '').trim().toLowerCase()
      if (kw) {
        const hay = [s.title, s.content, s.devConstraints, s.deliverables, s.direction]
          .map(v => String(v || '')).join('\n').toLowerCase()
        if (hay.indexOf(kw) === -1) return false
      }
      return true
    },
    resetFilters() {
      this.filters = { keyword: '', direction: '', difficulty: '' }
    },
    goDetail(g, s) {
      this.$router.push('/assessment/intern/learning/practice-bank/' + g.bankId + '/subject/' + s.id)
    },
    /** 封面：参考素材第一张图；加载失败过则不再用（避免破图） */
    thumb(s) {
      if (this.brokenImg[s.id]) return ''
      const f = firstImage(parseList(s.referenceImages))
      return f ? this.baseApi + f.url : ''
    },
    onImgErr(s) {
      this.$set(this.brokenImg, s.id, true)
    },
    refCount(s) { return parseList(s.referenceImages).length },
    attCount(s) { return parseList(s.attachmentsJson).length },
    dirName(s) { return s.direction || '未分方向' },
    isVideo(url) { return /\.(mp4|webm|ogg|ogv|mov|avi|m4v)(\?|#|$)/i.test(String(url || '')) },
    diffKey(d) { return diffKey(d) },
    diffText(d) { return { EASY: '简单', MEDIUM: '中等', HARD: '困难' }[diffKey(d)] },
    /** 占位块配色 / 图标：由「方向」稳定散列，同一方向永远同色 */
    phTone(s) {
      const k = String(this.dirName(s))
      let h = 0
      for (let i = 0; i < k.length; i++) h = (h * 31 + k.charCodeAt(i)) % 997
      return PH_TONES[h % PH_TONES.length]
    },
    phIcon(s) {
      return this.refCount(s) ? 'el-icon-picture-outline' : 'el-icon-edit-outline'
    }
  }
}
</script>

<style lang="scss" scoped>
.ipb-page { padding: 16px 18px 24px; }

/* ── 顶部 hero ───────────────────────────────── */
.ipb-hero { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; margin-bottom: 16px; }
.ipb-hero-copy { min-width: 0; }
.ipb-hero-copy h1 { margin: 0 0 7px; color: #111d2d; font-size: 24px; font-weight: 600; }
.ipb-hero-copy p { margin: 0; max-width: 760px; color: #6d7989; font-size: 13px; line-height: 1.75; }
.ipb-hero-side { display: flex; flex: none; align-items: flex-end; gap: 16px; }
.ipb-hero-stats { display: flex; gap: 25px; padding-left: 26px; border-left: 1px solid #dfe7f2; }
.ipb-hero-stats > div { display: grid; min-width: 78px; gap: 5px; text-align: center; }
.ipb-hero-stats strong { color: #1768ed; font-size: 25px; font-weight: 600; line-height: 1; }
.ipb-hero-stats span { color: #8b96a5; font-size: 11px; white-space: nowrap; }

/* ── 筛选条 ──────────────────────────────────── */
.ipb-filter { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; padding-bottom: 4px; }
.ipb-filter-input { width: 250px; }
.ipb-filter-select { width: 132px; }
.ipb-filter-sum { margin-left: auto; color: #667085; font-size: 12px; }

/* ── 分组（组标题 = 所属题库） ───────────────── */
.ipb-body { min-height: 240px; }
.ipb-section { padding: 4px 2px; margin-top: 22px; border-radius: 8px; transition: background .3s; }
.ipb-section:first-child { margin-top: 14px; }
.ipb-section.flash { background: #f3f8ff; box-shadow: 0 0 0 1px #d6e6ff inset; }
.ipb-sec-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 18px; margin-bottom: 13px; }
.ipb-sec-title { min-width: 0; }
.ipb-sec-title h2 { margin: 0; color: #1d2734; font-size: 19px; font-weight: 600; }
.ipb-sec-title p { margin: 6px 0 0; color: #8893a2; font-size: 12px; line-height: 1.6; }
.ipb-sec-count { flex: none; color: #8b96a5; font-size: 12px; }
.ipb-sec-none { margin: 0; padding: 22px 14px; color: #98a2b3; background: #fafbfc; border: 1px dashed #e4e9f0; border-radius: 7px; font-size: 12.5px; text-align: center; }
.ipb-sec-none.err { color: #b7791f; background: #fffbf3; border-color: #f5dfb4; }

/* ── 卡片网格 ────────────────────────────────── */
.ipb-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(212px, 1fr)); gap: 14px; }
.ipb-card {
  display: flex; min-width: 0; flex-direction: column; padding: 0; overflow: hidden;
  border: 1px solid #e0e7ef; border-radius: 8px; background: #fff; color: #233145;
  text-align: left; cursor: pointer; transition: transform .18s, border-color .18s, box-shadow .18s;
}
.ipb-card:hover { transform: translateY(-2px); border-color: #8db5ef; box-shadow: 0 10px 22px rgba(36, 95, 156, .10); }

.ipb-thumb { position: relative; display: block; overflow: hidden; aspect-ratio: 1.62; background: #eef2f7; }
.ipb-thumb img { display: block; width: 100%; height: 100%; object-fit: cover; }
.ipb-thumb-ph { display: flex; height: 100%; flex-direction: column; align-items: center; justify-content: center; gap: 8px; }
.ipb-thumb-ph i { font-size: 30px; opacity: .85; }
.ipb-thumb-ph em { overflow: hidden; max-width: 84%; font-size: 11.5px; font-style: normal; white-space: nowrap; text-overflow: ellipsis; }
.ipb-thumb-ph.tone-a { color: #1768ed; background: linear-gradient(135deg, #eaf2ff, #f7fbff); }
.ipb-thumb-ph.tone-b { color: #23966f; background: linear-gradient(135deg, #e8f7f1, #f6fdfa); }
.ipb-thumb-ph.tone-c { color: #b7791f; background: linear-gradient(135deg, #fdf4e4, #fffcf6); }
.ipb-thumb-ph.tone-d { color: #7b5cf0; background: linear-gradient(135deg, #f1edff, #fbfaff); }
.ipb-thumb-ph.tone-e { color: #0e8fa8; background: linear-gradient(135deg, #e6f6fa, #f7fdff); }
.ipb-thumb-ph.tone-f { color: #d9534f; background: linear-gradient(135deg, #fdeeed, #fff9f8); }

.ipb-thumb-marks { position: absolute; top: 8px; left: 8px; display: flex; gap: 5px; }
.ipb-thumb-marks em { padding: 2px 7px; color: #fff; border-radius: 4px; font-size: 11px; font-style: normal; }
.ipb-thumb-marks .d { background: rgba(102, 112, 133, .86); }
.ipb-thumb-marks .d-EASY { background: rgba(35, 150, 111, .9); }
.ipb-thumb-marks .d-MEDIUM { background: rgba(230, 162, 60, .92); }
.ipb-thumb-marks .d-HARD { background: rgba(217, 83, 79, .9); }
.ipb-thumb-marks .sc { background: rgba(23, 104, 237, .9); }

.ipb-copy { display: block; padding: 13px 14px 15px; }
.ipb-copy strong { display: -webkit-box; overflow: hidden; margin-bottom: 10px; color: #233145; font-size: 14.5px; font-weight: 600; line-height: 1.45; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.ipb-copy small { display: block; color: #8492a5; font-size: 12.5px; }
.ipb-copy small i { margin-left: 4px; color: #1768ed; font-style: normal; }
.ipb-copy small i.none { color: #98a2b3; }

/* ── 空态 ────────────────────────────────────── */
.ipb-empty { padding: 30px 12px; text-align: center; }
.ipb-empty.big { padding: 74px 12px; }
.ipb-empty i { color: #d0d5dd; font-size: 32px; }
.ipb-empty p { margin: 12px 0 0; color: #8490a0; font-size: 13px; line-height: 1.8; }
.ipb-empty .el-button { margin-top: 12px; }

@media (max-width: 1180px) {
  .ipb-hero { flex-wrap: wrap; align-items: flex-start; }
  .ipb-hero-side { width: 100%; justify-content: space-between; }
}
@media (max-width: 1000px) {
  .ipb-filter-sum { margin-left: 0; }
  .ipb-filter-input { width: 100%; }
  .ipb-hero-stats { padding-left: 0; border-left: 0; }
}
</style>
