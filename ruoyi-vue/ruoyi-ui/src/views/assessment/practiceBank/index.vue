<template>
  <div class="ipb-page">
    <header class="ipb-head">
      <div>
        <h1>模拟实操题库</h1>
        <p>这里可以查看部门开放的实操练习题库（只读）。每道题都写明了题目描述、考核要点与提交要求，先照着练，再参加正式实操考核。</p>
      </div>
      <el-button size="small" icon="el-icon-refresh" @click="loadBanks">刷新</el-button>
    </header>

    <div class="ipb-body">
      <aside class="ipb-side" v-loading="loading">
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
          <p>本部门还没有开放的实操练习题库，请等待管理员配置。</p>
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
          <div v-for="(s, i) in subjects" :key="s.id" class="ipb-card">
            <div class="ipb-card-head" @click="toggle(i)">
              <span class="ipb-idx">{{ i + 1 }}</span>
              <div class="ipb-card-title">
                <strong>{{ s.title }}</strong>
                <small>
                  {{ s.direction || '未分方向' }}
                  · {{ diffText(s.difficulty) }}
                  <template v-if="s.estimatedMinutes"> · 建议 {{ s.estimatedMinutes }} 分钟</template>
                  <template v-if="s.suggestScore != null"> · {{ s.suggestScore }} 分</template>
                </small>
              </div>
              <i class="el-icon-arrow-down" :class="{ open: expanded[i] }" />
            </div>
            <div v-show="expanded[i]" class="ipb-card-body">
              <div class="ipb-field" v-if="s.content"><label>题目描述</label><pre>{{ s.content }}</pre></div>
              <div class="ipb-field" v-if="s.devConstraints"><label>考核要点</label><pre>{{ s.devConstraints }}</pre></div>
              <div class="ipb-field" v-if="s.deliverables || s.submitFormat || s.namingRule">
                <label>提交要求</label>
                <pre v-if="s.deliverables">{{ s.deliverables }}</pre>
                <p class="ipb-inline" v-if="s.submitFormat">格式：{{ s.submitFormat }}</p>
                <p class="ipb-inline" v-if="s.namingRule">命名：{{ s.namingRule }}</p>
              </div>
              <div class="ipb-field" v-if="refs(s).length">
                <label>参考图 / 视频</label>
                <div class="ipb-files">
                  <a v-for="(f, k) in refs(s)" :key="'r' + k" :href="baseApi + f.url" target="_blank" class="ipb-file">
                    <i :class="isVideo(f.url) ? 'el-icon-video-camera' : 'el-icon-picture-outline'" />{{ f.name || '素材' }}
                  </a>
                </div>
              </div>
              <div class="ipb-field" v-if="atts(s).length">
                <label>附件</label>
                <div class="ipb-files">
                  <a v-for="(f, k) in atts(s)" :key="'a' + k" :href="baseApi + f.url" target="_blank" class="ipb-file">
                    <i class="el-icon-paperclip" />{{ f.name || '附件' }}
                  </a>
                </div>
              </div>
              <p class="ipb-tip">提示：正式实操考核需按「提交要求」上传文件，建议先本地练一遍。</p>
            </div>
          </div>
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

export default {
  name: 'InternPracticeBank',
  data() {
    return {
      loading: false,
      banks: [],
      current: null,
      subLoading: false,
      subjects: [],
      expanded: {}
    }
  },
  computed: {
    baseApi() { return process.env.VUE_APP_BASE_API || '' }
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
        if (this.banks.length) this.pick(this.banks[0])
      }).catch(() => { this.loading = false })
    },
    pick(b) {
      this.current = b
      this.subLoading = true
      this.expanded = {}
      request({ url: '/business/practice-bank/' + b.bankId + '/subjects', method: 'get' }).then(res => {
        this.subjects = res.rows || []
        this.subLoading = false
      }).catch(() => { this.subLoading = false })
    },
    toggle(i) { this.$set(this.expanded, i, !this.expanded[i]) },
    refs(s) { return parseList(s.referenceImages) },
    atts(s) { return parseList(s.attachmentsJson) },
    isVideo(url) { return /\.(mp4|webm|ogg|ogv|mov|avi|m4v)(\?|#|$)/i.test(String(url || '')) },
    typeText(t) { return { PRACTICE: '模拟题库', COMMON: '通用题库' }[t] || '通用题库' },
    diffText(d) { return { EASY: '简单', MEDIUM: '中等', HARD: '困难' }[d] || '中等' }
  }
}
</script>

<style lang="scss" scoped>
.ipb-page { padding: 16px 18px; }
.ipb-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 14px; margin-bottom: 14px; }
.ipb-head h1 { margin: 0 0 6px; color: #1d2939; font-size: 20px; font-weight: 600; }
.ipb-head p { margin: 0; max-width: 760px; color: #667085; font-size: 13px; line-height: 1.7; }
.ipb-body { display: grid; grid-template-columns: 268px minmax(0, 1fr); gap: 14px; align-items: start; }
.ipb-side { padding: 10px; background: #fff; border: 1px solid #e7ecf3; border-radius: 8px; }
.ipb-bank { padding: 11px 12px; margin-bottom: 8px; border: 1px solid #eef1f6; border-radius: 6px; cursor: pointer; transition: all .15s; }
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
.ipb-card { margin-bottom: 10px; border: 1px solid #eef1f6; border-radius: 8px; overflow: hidden; }
.ipb-card-head { display: flex; align-items: center; gap: 10px; padding: 11px 13px; cursor: pointer; }
.ipb-card-head:hover { background: #fafcff; }
.ipb-idx { display: inline-flex; width: 22px; height: 22px; flex: none; align-items: center; justify-content: center; color: #1764f5; background: #e8f1fd; border-radius: 4px; font-size: 12px; }
.ipb-card-title { flex: 1; min-width: 0; }
.ipb-card-title strong { display: block; color: #1d2939; font-size: 13.5px; }
.ipb-card-title small { display: block; margin-top: 3px; color: #98a2b3; font-size: 12px; }
.ipb-card-head i { color: #98a2b3; transition: transform .15s; }
.ipb-card-head i.open { transform: rotate(180deg); }
.ipb-card-body { padding: 4px 14px 14px 45px; border-top: 1px dashed #eef1f6; }
.ipb-field { margin-top: 10px; }
.ipb-field label { display: block; margin-bottom: 4px; color: #667085; font-size: 12px; }
.ipb-field pre { margin: 0; padding: 9px 11px; color: #344054; background: #f8fafc; border-radius: 5px; font-family: inherit; font-size: 12.5px; line-height: 1.75; white-space: pre-wrap; word-break: break-word; }
.ipb-inline { margin: 5px 0 0; color: #475467; font-size: 12.5px; }
.ipb-files { display: flex; flex-wrap: wrap; gap: 8px; }
.ipb-file { display: inline-flex; align-items: center; gap: 4px; padding: 3px 9px; color: #1764f5; background: #f1f7ff; border-radius: 4px; font-size: 12px; text-decoration: none; }
.ipb-tip { margin: 12px 0 0; color: #b54708; font-size: 12px; }
.ipb-empty { padding: 30px 12px; text-align: center; }
.ipb-empty.big { padding: 80px 12px; }
.ipb-empty i { color: #d0d5dd; font-size: 32px; }
.ipb-empty p { margin: 12px 0 0; color: #8490a0; font-size: 13px; line-height: 1.7; }
@media (max-width: 1000px) { .ipb-body { grid-template-columns: minmax(0, 1fr); } }
</style>
