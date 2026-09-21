<template>
  <div class="ipb-page">
    <header class="ipb-head">
      <div>
        <h1>模拟实操题</h1>
        <p>这里是本部门开放的模拟实操题（只读）。每道题都写明了题目描述、考核要点与提交要求，点<b>查看详情</b>进入单题页面，先照着练，再参加正式实操考核。</p>
      </div>
      <el-button size="small" icon="el-icon-refresh" @click="loadBanks">刷新</el-button>
    </header>

    <div class="ipb-body">
      <aside class="ipb-side" v-loading="loading">
        <div class="ipb-side-head">
          <strong>题库</strong>
          <span>{{ banks.length }} 个</span>
        </div>
        <div
          v-for="b in banks"
          :key="b.bankId"
          class="ipb-bank"
          :class="{ on: current && current.bankId === b.bankId }"
          @click="pick(b)"
        >
          <div class="ipb-bank-title">
            <strong>{{ b.bankName }}</strong>
            <el-tag size="mini" effect="plain" :type="b.bankType === 'PRACTICE' ? 'warning' : 'success'">{{ typeText(b.bankType) }}</el-tag>
          </div>
          <small>{{ b.description || '暂无说明' }}</small>
          <span class="ipb-count">{{ b.subjectCount }} 道题</span>
        </div>
        <div v-if="!loading && !banks.length" class="ipb-empty">
          <i class="el-icon-folder-opened" />
          <p>本部门还没有开放的模拟实操题，请等待管理员配置。</p>
        </div>
      </aside>

      <section class="ipb-main" v-loading="subLoading">
        <div v-if="!current" class="ipb-empty big">
          <i class="el-icon-hand-up" />
          <p>左侧选择一个题库，这里会列出它包含的全部实操题。</p>
        </div>
        <template v-else>
          <div class="ipb-sec-head">
            <h3>{{ current.bankName }}</h3>
            <span class="ipb-muted">共 {{ subjects.length }} 道 · 只读浏览</span>
          </div>

          <!-- ① 总览：难度分布，点卡即按难度筛选（再点一次取消） -->
          <div class="ipb-kpi-row">
            <button
              v-for="k in kpiCards()"
              :key="k.key || 'all'"
              type="button"
              class="ipb-kpi-card"
              :class="[{ active: filters.difficulty === k.key }, k.tone]"
              @click="applyKpi(k.key)"
            >
              <span class="ipb-kpi-icon"><i :class="k.icon" /></span>
              <span class="ipb-kpi-body">
                <span class="ipb-kpi-value">{{ k.value }}<small>道</small></span>
                <span class="ipb-kpi-label">{{ k.label }}</span>
              </span>
            </button>
          </div>

          <!-- ② 筛选：关键词 + 方向 + 难度 + 显示 N / M + 重置 -->
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
            <span class="ipb-filter-sum">显示 {{ filteredSubjects.length }} / {{ subjects.length }} 道</span>
          </div>

          <!-- ③ 列表 + 两种空态 -->
          <div v-if="!subjects.length" class="ipb-empty">
            <i class="el-icon-tickets" />
            <p>这个题库还没有录入实操题。<br />换一个题库看看，或等待管理员补充。</p>
          </div>
          <div v-else-if="!filteredSubjects.length" class="ipb-empty">
            <i class="el-icon-search" />
            <p>当前筛选条件下没有匹配的题目。<br />换个关键词，或清空筛选条件。</p>
            <el-button size="small" type="primary" plain @click="resetFilters">清空筛选</el-button>
          </div>
          <template v-else>
            <article v-for="(s, i) in filteredSubjects" :key="s.id" class="ipb-card">
              <div class="ipb-card-head">
                <span class="ipb-idx">{{ i + 1 }}</span>
                <div class="ipb-card-title">
                  <strong>{{ s.title }}</strong>
                  <small>
                    {{ dirName(s) }}
                    · {{ diffText(s.difficulty) }}
                    <template v-if="s.estimatedMinutes"> · 建议 {{ s.estimatedMinutes }} 分钟</template>
                    <template v-if="s.suggestScore != null"> · {{ s.suggestScore }} 分</template>
                  </small>
                  <p class="ipb-excerpt">{{ excerpt(s) }}</p>
                </div>
                <div class="ipb-card-side">
                  <span class="ipb-marks">
                    <em v-if="refs(s).length">素材 {{ refs(s).length }}</em>
                    <em v-if="atts(s).length">附件 {{ atts(s).length }}</em>
                  </span>
                  <el-button size="mini" type="primary" plain @click="goDetail(s)">查看详情</el-button>
                </div>
              </div>
            </article>
          </template>
        </template>
      </section>
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

/** 难度的「有效档位」：空 / 未知一律归入「中等」，与列表展示口径一致 */
function diffKey(d) {
  const v = String(d || '').toUpperCase()
  return { EASY: 'EASY', MEDIUM: 'MEDIUM', HARD: 'HARD' }[v] || 'MEDIUM'
}

export default {
  name: 'InternPracticeBank',
  data() {
    return {
      loading: false,
      banks: [],
      current: null,
      subLoading: false,
      subjects: [],
      filters: { keyword: '', direction: '', difficulty: '' }
    }
  },
  computed: {
    baseApi() { return process.env.VUE_APP_BASE_API || '' },
    /** 方向选项：按已加载题目去重（含「未分方向」） */
    directionOptions() {
      const set = {}
      this.subjects.forEach(s => { set[this.dirName(s)] = 1 })
      return Object.keys(set).sort()
    },
    /** 是否处于筛选态（用于「重置」与「筛没了」空态） */
    hasFilter() {
      return !!(this.filters.keyword || this.filters.direction || this.filters.difficulty)
    },
    /** ③ 列表数据（纯前端筛选，不改接口） */
    filteredSubjects() {
      const kw = String(this.filters.keyword || '').trim().toLowerCase()
      const dir = this.filters.direction
      const diff = this.filters.difficulty
      return this.subjects.filter(s => {
        if (diff && diffKey(s.difficulty) !== diff) return false
        if (dir && this.dirName(s) !== dir) return false
        if (kw) {
          const hay = [s.title, s.content, s.devConstraints, s.deliverables, s.direction]
            .map(v => String(v || '')).join('\n').toLowerCase()
          if (hay.indexOf(kw) === -1) return false
        }
        return true
      })
    }
  },
  created() {
    this.loadBanks()
  },
  methods: {
    loadBanks() {
      this.loading = true
      request({ url: '/business/practice-bank/enabled', method: 'get' }).then(res => {
        this.banks = res.data || []
        this.loading = false
        if (!this.banks.length) {
          this.current = null
          this.subjects = []
          return
        }
        // 优先用 ?bank= 回跳（从详情页返回时保持原题库），其次保持当前选中，最后取第一个
        const want = this.$route.query.bank ? Number(this.$route.query.bank) : (this.current ? this.current.bankId : null)
        const target = this.banks.find(b => b.bankId === want) || this.banks[0]
        this.pick(target)
      }).catch(() => {
        this.loading = false
        this.banks = []
      })
    },
    pick(b) {
      // 重复点同一个库不重复请求（刷新走 loadBanks 的显式调用）
      if (this.current && this.current.bankId === b.bankId && this.subjects.length && !this.subLoading) return
      this.current = b
      this.subLoading = true
      this.subjects = []
      this.resetFilters()
      request({ url: '/business/practice-bank/' + b.bankId + '/subjects', method: 'get' }).then(res => {
        this.subjects = res.rows || []
        this.subLoading = false
      }).catch(() => {
        this.subLoading = false
        this.subjects = []
      })
    },
    // ① 总览 KPI：全部由已加载题目算；点卡 = 按难度筛选，再点一次取消
    kpiCards() {
      const list = this.subjects || []
      const cnt = d => list.filter(s => diffKey(s.difficulty) === d).length
      return [
        { key: '', label: '题目总数', value: list.length, icon: 'el-icon-collection', tone: '' },
        { key: 'EASY', label: '简单', value: cnt('EASY'), icon: 'el-icon-sunny', tone: 'tone-green' },
        { key: 'MEDIUM', label: '中等', value: cnt('MEDIUM'), icon: 'el-icon-cloudy', tone: 'tone-orange' },
        { key: 'HARD', label: '困难', value: cnt('HARD'), icon: 'el-icon-heavy-rain', tone: 'tone-red' }
      ]
    },
    applyKpi(key) {
      this.filters.difficulty = this.filters.difficulty === key ? '' : key
    },
    resetFilters() {
      this.filters = { keyword: '', direction: '', difficulty: '' }
    },
    goDetail(s) {
      this.$router.push('/assessment/intern/learning/practice-bank/' + this.current.bankId + '/subject/' + s.id)
    },
    /** 卡片摘要：题干首段压平后截断（详情页看全文） */
    excerpt(s) {
      const raw = String(s.content || s.deliverables || '').replace(/\s+/g, ' ').trim()
      if (!raw) return '暂无题目描述，点「查看详情」查看完整要求。'
      return raw.length > 88 ? raw.slice(0, 88) + '…' : raw
    },
    dirName(s) { return s.direction || '未分方向' },
    refs(s) { return parseList(s.referenceImages) },
    atts(s) { return parseList(s.attachmentsJson) },
    isVideo(url) { return /\.(mp4|webm|ogg|ogv|mov|avi|m4v)(\?|#|$)/i.test(String(url || '')) },
    typeText(t) { return { PRACTICE: '模拟题库', COMMON: '通用题库' }[t] || '通用题库' },
    diffText(d) { return { EASY: '简单', MEDIUM: '中等', HARD: '困难' }[diffKey(d)] }
  }
}
</script>

<style lang="scss" scoped>
.ipb-page { padding: 16px 18px; }
.ipb-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 14px; margin-bottom: 14px; }
.ipb-head h1 { margin: 0 0 6px; color: #1d2939; font-size: 20px; font-weight: 600; }
.ipb-head p { margin: 0; max-width: 780px; color: #667085; font-size: 13px; line-height: 1.7; }

.ipb-body { display: grid; grid-template-columns: 268px minmax(0, 1fr); gap: 14px; align-items: start; }
.ipb-side { padding: 10px; background: #fff; border: 1px solid #e7ecf3; border-radius: 8px; }
.ipb-side-head { display: flex; align-items: center; justify-content: space-between; padding: 2px 4px 10px; }
.ipb-side-head strong { color: #1d2939; font-size: 13px; font-weight: 600; }
.ipb-side-head span { color: #98a2b3; font-size: 12px; }
.ipb-bank { padding: 11px 12px; margin-bottom: 8px; border: 1px solid #eef1f6; border-radius: 6px; cursor: pointer; transition: all .15s; }
.ipb-bank:last-child { margin-bottom: 0; }
.ipb-bank:hover { border-color: #b9d2ff; }
.ipb-bank.on { border-color: #1764f5; background: #f6faff; }
.ipb-bank-title { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.ipb-bank-title strong { color: #1d2939; font-size: 13.5px; }
.ipb-bank small { display: block; margin-top: 4px; color: #98a2b3; font-size: 12px; line-height: 1.6; }
.ipb-count { display: block; margin-top: 6px; color: #1764f5; font-size: 12px; }

.ipb-main { padding: 14px 16px; background: #fff; border: 1px solid #e7ecf3; border-radius: 8px; min-height: 320px; }
.ipb-sec-head { display: flex; align-items: baseline; justify-content: space-between; padding-bottom: 10px; margin-bottom: 12px; border-bottom: 1px solid #edf0f4; }
.ipb-sec-head h3 { margin: 0; color: #1d2939; font-size: 15px; font-weight: 600; }
.ipb-muted { color: #98a2b3; font-size: 12px; }

/* ① 总览 KPI（可点即筛选） */
.ipb-kpi-row { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin-bottom: 14px; }
.ipb-kpi-card { display: flex; align-items: center; gap: 12px; padding: 13px 16px; text-align: left; background: #fff; border: 1px solid #e7ecf3; border-radius: 8px; cursor: pointer; transition: all .15s; }
.ipb-kpi-card:hover { border-color: #b9d2ff; box-shadow: 0 2px 8px rgba(23, 100, 245, .08); }
.ipb-kpi-card.active { border-color: #1764f5; box-shadow: 0 0 0 2px rgba(23, 100, 245, .12); }
.ipb-kpi-icon { display: flex; width: 38px; height: 38px; flex: none; align-items: center; justify-content: center; color: #1764f5; background: #edf4ff; font-size: 19px; border-radius: 8px; }
.ipb-kpi-body { display: flex; min-width: 0; flex-direction: column; }
.ipb-kpi-value { color: #1d2939; font-size: 21px; font-weight: 600; line-height: 1.2; }
.ipb-kpi-value small { margin-left: 3px; color: #667085; font-size: 12px; font-weight: 400; }
.ipb-kpi-label { margin-top: 2px; color: #667085; font-size: 12px; }
.ipb-kpi-card.tone-green .ipb-kpi-icon { color: #23966f; background: #eaf7f1; }
.ipb-kpi-card.tone-orange .ipb-kpi-icon { color: #e6a23c; background: #fdf6ec; }
.ipb-kpi-card.tone-red .ipb-kpi-icon { color: #d9534f; background: #fdeeed; }

/* ② 筛选条 */
.ipb-filter { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; padding: 0 0 12px; }
.ipb-filter-input { width: 250px; }
.ipb-filter-select { width: 132px; }
.ipb-filter-sum { margin-left: auto; color: #667085; font-size: 12px; }

/* ③ 列表卡片 */
.ipb-card { margin-bottom: 10px; border: 1px solid #eef1f6; border-radius: 8px; overflow: hidden; }
.ipb-card:hover { border-color: #d6e4ff; }
.ipb-card-head { display: flex; align-items: center; gap: 12px; padding: 12px 14px; }
.ipb-idx { display: inline-flex; width: 22px; height: 22px; flex: none; align-items: center; justify-content: center; color: #1764f5; background: #e8f1fd; border-radius: 4px; font-size: 12px; }
.ipb-card-title { flex: 1; min-width: 0; }
.ipb-card-title strong { display: block; color: #1d2939; font-size: 13.5px; }
.ipb-card-title small { display: block; margin-top: 3px; color: #98a2b3; font-size: 12px; }
.ipb-excerpt { margin: 6px 0 0; overflow: hidden; color: #667085; font-size: 12.5px; line-height: 1.6; white-space: nowrap; text-overflow: ellipsis; }
.ipb-card-side { display: flex; flex: none; align-items: center; gap: 12px; }
.ipb-marks { display: flex; gap: 6px; }
.ipb-marks em { padding: 2px 7px; color: #667085; background: #f2f4f7; border-radius: 4px; font-size: 11px; font-style: normal; }

.ipb-empty { padding: 30px 12px; text-align: center; }
.ipb-empty.big { padding: 80px 12px; }
.ipb-empty i { color: #d0d5dd; font-size: 32px; }
.ipb-empty p { margin: 12px 0 0; color: #8490a0; font-size: 13px; line-height: 1.8; }
.ipb-empty .el-button { margin-top: 12px; }

@media (max-width: 1100px) { .ipb-kpi-row { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 1000px) {
  .ipb-body { grid-template-columns: minmax(0, 1fr); }
  .ipb-filter-sum { margin-left: 0; }
  .ipb-filter-input { width: 100%; }
}
</style>
