<template>
  <div class="s-page" v-loading="loading">
    <div class="s-crumb">
      <el-button type="text" icon="el-icon-arrow-left" @click="goBack">返回答卷名单</el-button>
      <span class="sp">/</span>
      <b>{{ sheet.userName || '答卷明细' }}</b>
      <span v-if="sheet.examName" class="s-badge">{{ sheet.examName }}</span>
    </div>

    <div v-if="loadError" class="s-empty big">
      <i class="el-icon-warning-outline" /><strong>无法加载该答卷</strong>
      <span>{{ loadError }}</span>
      <el-button size="small" @click="goBack">返回名单</el-button>
    </div>

    <template v-else>
      <!-- ① 答卷头 -->
      <div class="s-kpis">
        <div class="s-kpi">
          <div class="lb"><i class="dot" style="background:#1764f5" />最终得分</div>
          <div class="vl">{{ sheet.finalScore != null ? sheet.finalScore : '待定' }}<small v-if="sheet.finalScore != null">分</small></div>
          <div class="ft">{{ conclusionText }}</div>
        </div>
        <div class="s-kpi">
          <div class="lb"><i class="dot" style="background:#12b76a" />答对 · 答错 · 未作答</div>
          <div class="vl">{{ stat.ok }} · {{ stat.bad }} · {{ stat.blank }}</div>
          <div class="ft" :class="stat.blank > 0 ? 'warn' : 'up'">
            {{ stat.blank > 0 ? '未作答单独统计，不计入得分率' : '全部已作答' }}
          </div>
        </div>
        <div class="s-kpi">
          <div class="lb"><i class="dot" style="background:#7a5af8" />第 N 次</div>
          <div class="vl">{{ sheet.retakeSeq || 1 }}</div>
          <div class="ft">{{ sheet.submitTime ? String(sheet.submitTime).replace('T', ' ').slice(0, 16) : '未提交' }}</div>
        </div>
        <div class="s-kpi">
          <div class="lb"><i class="dot" style="background:#f79009" />本场对照</div>
          <div class="vl">{{ rankText }}</div>
          <div class="ft">{{ rankHint }}</div>
        </div>
      </div>

      <div class="s-grid">
        <!-- ② 逐题明细 -->
        <section class="s-card s-c8">
          <div class="s-card-h">
            <div class="tt"><span class="s-idx">题</span><h3>逐题明细</h3></div>
            <div class="acts">
              <button type="button" class="s-chip" :class="{ on: filter === 'ALL' }" @click="filter = 'ALL'">全部 {{ items.length }}</button>
              <button type="button" class="s-chip" :class="{ on: filter === 'THEORY' }" @click="filter = 'THEORY'">理论 {{ theoryItems.length }}</button>
              <button type="button" class="s-chip" :class="{ on: filter === 'PRACTICAL' }" @click="filter = 'PRACTICAL'">实操 {{ practiceItems.length }}</button>
            </div>
          </div>

          <div v-for="it in shownItems" :key="it.id" class="q-item">
            <div class="q-head">
              <span class="s-idx sm">{{ it.seq }}</span>
              <span class="s-badge" :class="typeTone(it)">{{ typeText(it) }}</span>
              <span class="q-kp" v-if="it.knowledgePoint">{{ it.knowledgePoint }}</span>
              <span class="q-score">
                <template v-if="it.isSubjective === 1 || it.isSubjective === '1'">
                  人工 {{ it.manualScore != null ? it.manualScore : '—' }} / {{ it.fullScore != null ? it.fullScore : (it.score || '—') }}
                </template>
                <template v-else>
                  {{ it.isCorrect === 1 || it.isCorrect === '1' ? '✔' : '✘' }}
                  {{ it.aiScore != null ? it.aiScore : '—' }} / {{ it.fullScore != null ? it.fullScore : (it.score || '—') }}
                </template>
              </span>
            </div>
            <p class="q-stem">{{ it.stemSnapshot || it.stem || '（题干未记录）' }}</p>

            <!-- 理论题：选项 + 作答 + 正确答案；三态明确 -->
            <template v-if="!(it.isSubjective === 1 || it.isSubjective === '1')">
              <div v-if="optsOf(it).length" class="q-opts">
                <span v-for="o in optsOf(it)" :key="o.key" class="q-opt"
                      :class="{ right: isRightOpt(it, o.key), picked: isPicked(it, o.key), wrong: isPicked(it, o.key) && !isRightOpt(it, o.key) }">
                  <b>{{ o.key }}</b>{{ o.val }}
                </span>
              </div>
              <div class="q-ans">
                <span>作答：<b :class="{ blank: !it.userAnswer }">{{ it.userAnswer || '未作答' }}</b></span>
                <span>正确答案：<b class="ok">{{ it.answer || '—' }}</b></span>
                <span v-if="it.manualComment" class="q-cmt">评语：{{ it.manualComment }}</span>
              </div>
            </template>

            <!-- 实操题：描述 / 提交物 / 人工分与评语（无「对错」概念） -->
            <template v-else>
              <p v-if="it.subjectDescription" class="q-desc">{{ it.subjectDescription }}</p>
              <div class="q-ans">
                <span>提交物：<b v-if="filesOf(it).length">{{ filesOf(it).map(f => f.name || f.url).join('、') }}</b><b v-else class="blank">未提交</b></span>
                <span v-if="it.manualComment" class="q-cmt">评语：{{ it.manualComment }}</span>
              </div>
            </template>
          </div>

          <div v-if="!shownItems.length" class="s-empty sm"><i class="el-icon-document" /><span>没有该类型的题目</span></div>
        </section>

        <!-- ③ 章节得分率 + 总评语 -->
        <section class="s-card s-c4">
          <div class="s-card-h"><div class="tt"><span class="s-idx o">章</span><h3>按章节得分率</h3></div></div>
          <template v-if="chapterStats.length">
            <div v-for="c in chapterStats" :key="c.name" class="chap-row">
              <div class="chap-head">
                <span class="chap-name">{{ c.name }}</span>
                <span class="chap-rate" :class="c.tone">{{ c.rate == null ? '无作答' : c.rate + '%' }}</span>
              </div>
              <div class="chap-bar">
                <i class="ok" :style="{ width: c.okPct + '%' }" />
                <i class="bad" :style="{ width: c.badPct + '%' }" />
                <i class="blank" :style="{ width: c.blankPct + '%' }" />
              </div>
              <div class="chap-meta">对 {{ c.ok }} · 错 {{ c.bad }} · 未作答 {{ c.blank }}</div>
            </div>
            <p class="s-note">口径：已作答的题参与得分率，<b>未作答单独统计</b>（白卷不会被读成"这些章节都不会"）。</p>
          </template>
          <div v-else class="s-empty sm"><i class="el-icon-data-analysis" /><span>本题答卷没有可聚合的章节数据</span></div>

          <div v-if="sheet.manualComment" class="s-callout ok">
            <i class="el-icon-chat-line-square" />
            <span><b>总评语：</b>{{ sheet.manualComment }}</span>
          </div>
        </section>
      </div>
    </template>
  </div>
</template>

<script>
import { sheetDetail, gradingList } from '@/api/business/exam'

/**
 * 超管「考核与成绩」L2 —— 单份答卷详情
 *
 * 路由：`/exam-sheet/:sheetId?from=&examId=`（顶层 hidden，照 /exam-config 做法）
 *
 * 数据源：`/business/answer-sheet/detail/{sheetId}` → { sheet, items[] }
 *   items 带 `knowledgePoint`（章节）、`stemSnapshot`（题干快照）、`fullScore`（满分快照）、
 *   `isCorrect` / `userAnswer` / `aiScore` / `manualScore` / `isSubjective` ——
 *   题干与满分**优先取快照**（题目被改删后旧答卷仍可还原），快照为空才回退题目表。
 *
 * ★ 三态：对 / 错 / **未作答** —— 实测库里第一张已发布答卷就是白卷（10 题全 userAnswer=null），
 *   若都渲染成"错"会被误读成"这 10 个知识点都不会"。
 * ★ 理论题与实操题**明细结构不同**（实操没有"对错"概念，看人工分与提交物），分开渲染。
 */
export default {
  name: 'SuperExamSheet',
  data() {
    return {
      loading: false,
      loadError: '',
      sheet: {},
      items: [],
      peers: [],
      filter: 'ALL'
    }
  },
  computed: {
    theoryItems() { return this.items.filter(i => !this.isSub(i)) },
    practiceItems() { return this.items.filter(i => this.isSub(i)) },
    shownItems() {
      if (this.filter === 'THEORY') return this.theoryItems
      if (this.filter === 'PRACTICAL') return this.practiceItems
      return this.items
    },
    /** 三态统计（未作答单独计） */
    stat() {
      let ok = 0
      let bad = 0
      let blank = 0
      this.items.forEach(i => {
        if (this.isSub(i)) return
        if (!i.userAnswer) blank++
        else if (i.isCorrect === 1 || i.isCorrect === '1') ok++
        else bad++
      })
      return { ok, bad, blank }
    },
    /** 章节聚合（三态堆叠条） */
    chapterStats() {
      const map = {}
      this.items.forEach(i => {
        const name = i.knowledgePoint || '未标注章节'
        if (!map[name]) map[name] = { name, ok: 0, bad: 0, blank: 0 }
        if (this.isSub(i)) return
        if (!i.userAnswer) map[name].blank++
        else if (i.isCorrect === 1 || i.isCorrect === '1') map[name].ok++
        else map[name].bad++
      })
      return Object.keys(map).map(k => {
        const m = map[k]
        const answered = m.ok + m.bad
        const total = answered + m.blank
        const rate = answered ? Math.round(m.ok / answered * 100) : null
        return Object.assign(m, {
          rate,
          okPct: total ? m.ok / total * 100 : 0,
          badPct: total ? m.bad / total * 100 : 0,
          blankPct: total ? m.blank / total * 100 : 0,
          tone: rate == null ? 'mid' : (rate < 60 ? 'poor' : (rate < 80 ? 'mid' : 'good'))
        })
      }).sort((a, b) => {
        if (a.rate == null && b.rate == null) return b.blank - a.blank
        if (a.rate == null) return 1
        if (b.rate == null) return -1
        return a.rate - b.rate
      })
    },
    /** 本场对照：样本 ≥5 才给位次，否则显式说"样本不足" */
    rankText() {
      const scored = this.peers.filter(p => p.finalScore != null)
      if (scored.length < 5) return '样本不足'
      const mine = Number(this.sheet.finalScore)
      if (isNaN(mine)) return '—'
      const better = scored.filter(p => Number(p.finalScore) > mine).length
      return '第 ' + (better + 1) + ' / ' + scored.length
    },
    rankHint() {
      const scored = this.peers.filter(p => p.finalScore != null)
      if (scored.length < 5) return '本场仅 ' + scored.length + ' 份已出分，不排位次'
      return '按最终分从高到低'
    }
  },
  created() {
    this.load()
  },
  methods: {
    load() {
      const sid = this.$route.params.sheetId
      if (!sid) { this.loadError = '缺少答卷 id'; return }
      this.loading = true
      this.loadError = ''
      sheetDetail(sid).then(res => {
        const d = (res && res.data) || {}
        if (!d.sheet) { this.loadError = '未找到该答卷（可能已被删除）'; this.loading = false; return }
        this.sheet = d.sheet
        this.items = d.items || []
        this.loading = false
        const examId = this.$route.query.examId || d.sheet.examId
        if (examId) {
          gradingList(examId).then(r => { this.peers = (r && r.data) || [] }).catch(() => {})
        }
      }).catch(() => {
        this.loadError = '加载失败：可能是权限不足或后端未启动'
        this.loading = false
      })
    },
    isSub(i) { return i.isSubjective === 1 || i.isSubjective === '1' },
    /** ⚠️ detail 接口的 sheet 不带 passLine（实测），所以这里只按 pass_flag 给结论 */
    conclusionText() {
      if (this.sheet.finalScore == null) return '未出分'
      if (this.sheet.passFlag === 1 || this.sheet.passFlag === '1') return '通过'
      if (this.sheet.passFlag === 0 || this.sheet.passFlag === '0') return '未通过'
      return '已出分'
    },
    optsOf(i) {
      if (!i.optionsJson) return []
      try {
        const a = typeof i.optionsJson === 'string' ? JSON.parse(i.optionsJson) : i.optionsJson
        return Array.isArray(a) ? a.map(o => ({ key: o.key || o.optionKey || '', val: o.value || o.val || o.content || '' })) : []
      } catch (e) { return [] }
    },
    filesOf(i) {
      const out = []
      const push = v => {
        if (!v) return
        try {
          const a = typeof v === 'string' ? JSON.parse(v) : v
          if (Array.isArray(a)) out.push.apply(out, a)
        } catch (e) { /* 非 JSON 就忽略 */ }
      }
      push(i.subjectAttachments)
      push(i.subjectImages)
      if (i.attachmentUrl) out.push({ name: String(i.attachmentUrl).split('/').pop(), url: i.attachmentUrl })
      return out
    },
    isRightOpt(i, key) {
      const ans = String(i.answer || '').toUpperCase()
      return ans.split(',').map(s => s.trim()).indexOf(String(key).toUpperCase()) > -1
    },
    isPicked(i, key) {
      const ua = String(i.userAnswer || '').toUpperCase()
      return ua.split(',').map(s => s.trim()).indexOf(String(key).toUpperCase()) > -1
    },
    typeText(i) {
      if (this.isSub(i)) return '实操题'
      return { SINGLE: '单选', MULTI: '多选', JUDGE: '判断' }[i.qtype] || i.qtype || '理论题'
    },
    typeTone(i) {
      if (this.isSub(i)) return 'purple'
      if (!i.userAnswer) return 'warn'
      return (i.isCorrect === 1 || i.isCorrect === '1') ? 'ok' : 'red'
    },
    goBack() {
      this.$router.push(this.$route.query.from || '/super/ops/exams').catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
@import '~@/assets/styles/super-module.scss';

.s-crumb { display: flex; align-items: center; gap: 8px; margin-bottom: 14px; color: #667085; font-size: 13px; flex-wrap: wrap; }
.s-crumb .sp { color: #cfd6df; }
.s-crumb b { color: #1d2939; }
.s-card-h .acts { display: flex; align-items: center; gap: 6px; }
.s-chip { padding: 3px 10px; color: #475467; background: #fff; border: 1px solid #e7ecf3; border-radius: 5px; font-size: 12px; cursor: pointer; }
.s-chip.on { color: #1764f5; border-color: #1764f5; background: #f6faff; }
.s-idx.sm { width: 20px; height: 20px; font-size: 11px; }

.q-item { padding: 11px 0; border-bottom: 1px dashed #eef1f6; }
.q-item:last-child { border-bottom: none; }
.q-head { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; flex-wrap: wrap; }
.q-kp { padding: 1px 7px; color: #667085; background: #f2f4f7; border-radius: 4px; font-size: 11px; }
.q-score { margin-left: auto; color: #344054; font-size: 12.5px; }
.q-stem { margin: 0 0 8px; color: #344054; font-size: 13px; line-height: 1.75; }
.q-desc { margin: 0 0 8px; color: #667085; font-size: 12.5px; line-height: 1.7; }
.q-opts { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 8px; }
.q-opt { padding: 3px 9px; color: #475467; background: #f8fafc; border: 1px solid #eef1f6; border-radius: 5px; font-size: 12px; }
.q-opt b { margin-right: 5px; color: #98a2b3; }
.q-opt.right { color: #23966f; background: #eaf7f1; border-color: #bfe8d8; }
.q-opt.picked { border-color: #b9d2ff; background: #edf4ff; }
.q-opt.picked.wrong { color: #d9534f; background: #fdeeed; border-color: #f7d4d1; }
.q-ans { display: flex; flex-wrap: wrap; gap: 16px; color: #667085; font-size: 12.5px; }
.q-ans b { color: #344054; }
.q-ans b.ok { color: #23966f; }
.q-ans b.blank { color: #b54708; }
.q-cmt { color: #475467; }

.chap-row { margin-bottom: 11px; }
.chap-head { display: flex; align-items: baseline; justify-content: space-between; margin-bottom: 4px; }
.chap-name { color: #344054; font-size: 12.5px; }
.chap-rate { font-size: 12.5px; font-weight: 600; }
.chap-rate.poor { color: #d9534f; }
.chap-rate.mid { color: #e6a23c; }
.chap-rate.good { color: #23966f; }
.chap-bar { display: flex; height: 8px; overflow: hidden; background: #f2f4f7; border-radius: 4px; }
.chap-bar i { display: block; height: 100%; }
.chap-bar i.ok { background: #23966f; }
.chap-bar i.bad { background: #f04438; }
.chap-bar i.blank { background: #d0d5dd; }
.chap-meta { margin-top: 3px; color: #98a2b3; font-size: 11.5px; }
.s-callout.ok { display: flex; gap: 8px; margin-top: 10px; padding: 10px 12px; color: #475467; background: #f6faff; border: 1px solid #dbe9ff; border-radius: 6px; font-size: 12.5px; line-height: 1.7; }
.s-empty.sm { padding: 26px 12px; }
.s-empty.big { padding: 60px 12px; }
.s-badge.purple { color: #7b5cf0; background: #f2eeff; }
.s-badge.red { color: #d9534f; background: #fdeeed; }
</style>
