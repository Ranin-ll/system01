<template>
  <div class="practice-page">
    <div class="exam-breadcrumb">
      <el-button type="text" icon="el-icon-arrow-left" @click="goBack">返回工作台</el-button>
      <span>/</span>
      <b>实操题库</b>
    </div>

    <header class="exam-heading">
      <div>
        <span class="eyebrow">PRACTICE · HANDS-ON</span>
        <h1>实操题库</h1>
        <p>本部门已发布的实操练习题，按方向分组；点开卡片查看完整题干、交付要求与参考图。</p>
      </div>
    </header>

    <div class="subject-hint">
      <i class="el-icon-info" />
      <span>以下为本部门已发布的实操练习题，仅供查看题干、交付要求与下载附件；无需提交作答，也不会提交给管理员。点「查看详情」打开完整题目。</span>
    </div>

    <div v-if="loading" class="empty-state"><i class="el-icon-loading" /><span>正在加载实操题…</span></div>
    <div v-else-if="!subjects.length" class="empty-state">
      <i class="el-icon-document" />
      <span>本部门暂未发布模拟实操题</span>
    </div>

    <template v-else>
      <!-- 题库概览 -->
      <section class="hero">
        <div class="hero-l">
          <span class="badge-blue">模拟试题库</span>
          <h3>{{ deptName ? deptName + '模拟试题' : '本部门模拟试题' }}</h3>
          <p>选择一个题目开始练习，完成后可按题目要求提交成果进行自检。</p>
        </div>
        <div class="hero-stats">
          <div><b>{{ subjects.length }}</b><span>可练习题目</span></div>
          <div><b>{{ groups.length }}</b><span>方向分组</span></div>
          <div><b>{{ maxMinutes }}<small> 分钟</small></b><span>最长用时</span></div>
        </div>
      </section>

      <!-- 按方向分组的卡片网格 -->
      <div v-for="g in groups" :key="g.name" class="grp">
        <div class="grp-h">
          <div>
            <h4>{{ g.name }}</h4>
            <p>{{ g.desc }}</p>
          </div>
          <span class="hint">{{ g.items.length }} 题</span>
        </div>
        <div class="qgrid">
          <div v-for="(s, i) in g.items" :key="s.id" class="qcard" @click="goDetail(s)">
            <div class="thumb">
              <img v-if="firstImage(s)" :src="baseApi + firstImage(s)" :alt="s.title">
              <span v-else class="thumb-ph" v-html="thumbSvg(i)" />
            </div>
            <div class="qc-b">
              <b>{{ s.title || '未命名实操题' }}</b>
              <div class="qc-f">
                <span>建议用时</span>
                <b>{{ s.estimatedMinutes ? s.estimatedMinutes + ' 分钟' : '不限' }}</b>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script>
import { listPracticeSubjects } from '@/api/business/practice'
import practiceMixin from './practice-mixin'
import { mapGetters } from 'vuex'

export default {
  name: 'InternPracticeSubject',
  mixins: [practiceMixin],
  data() {
    return {
      loading: false,
      subjects: []
    }
  },
  computed: {
    ...mapGetters(['deptName']),
    /** 按 direction 分组，保持后端返回的顺序（direction, sort_no, id） */
    groups() {
      const map = new Map()
      this.subjects.forEach(s => {
        const name = s.direction || '通用'
        if (!map.has(name)) {
          map.set(name, { name, desc: s.directionDesc || '本方向实操练习题', items: [] })
        }
        map.get(name).items.push(s)
      })
      return Array.from(map.values())
    },
    maxMinutes() {
      const arr = this.subjects.map(s => Number(s.estimatedMinutes) || 0)
      return arr.length ? Math.max.apply(null, arr) : 0
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
    /** 首张参考图作为卡片缩略图 */
    firstImage(row) {
      const imgs = this.parseAttachments(row.referenceImages)
      return imgs.length ? imgs[0].url : ''
    },
    /** 无参考图时的占位缩略图（与设计稿同款线框风格，3 种变体轮换） */
    thumbSvg(i) {
      const body = [
        '<rect x="26" y="42" width="46" height="6" rx="3" fill="#cfe0fb"/><rect x="26" y="54" width="70" height="6" rx="3" fill="#cfe0fb"/><rect x="26" y="66" width="34" height="6" rx="3" fill="#cfe0fb"/><rect x="86" y="54" width="24" height="6" rx="3" fill="#cfe0fb"/>',
        '<rect x="26" y="42" width="60" height="6" rx="3" fill="#cfe0fb"/><rect x="26" y="54" width="40" height="6" rx="3" fill="#cfe0fb"/><rect x="26" y="66" width="72" height="6" rx="3" fill="#cfe0fb"/>',
        '<rect x="24" y="46" width="34" height="30" rx="4" fill="#dbe8fc"/><rect x="64" y="46" width="34" height="30" rx="4" fill="#cfe0fb"/><rect x="104" y="46" width="32" height="30" rx="4" fill="#e0ebfd"/>'
      ]
      const v = body[i % body.length]
      return '<svg viewBox="0 0 160 104" preserveAspectRatio="none">' +
        '<rect x="14" y="18" width="132" height="70" rx="8" fill="#f2f7ff"/>' +
        '<rect x="14" y="18" width="132" height="15" rx="8" fill="#d7e7fc"/>' +
        '<circle cx="25" cy="25.5" r="3" fill="#b6d0f8"/><circle cx="35" cy="25.5" r="3" fill="#b6d0f8"/><circle cx="45" cy="25.5" r="3" fill="#b6d0f8"/>' +
        v + '</svg>'
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

/* 与统一设计稿一致的设计令牌 */
$blue: #1764f5;
$blue-soft: #edf4ff;
$ink: #1d2939;
$ink-2: #344054;
$ink-3: #667085;
$ink-4: #98a2b3;
$line: #e4e9f0;
$line-2: #eef1f6;
$panel-2: #f7f9fc;

/* --- 题库概览 --- */
.hero {
  display: flex; align-items: center; gap: 24px; flex-wrap: wrap;
  background: #fff; border: 1px solid $line; border-radius: 12px;
  padding: 20px 24px; margin-top: 4px;
}
.hero-l { flex: 1; min-width: 260px; }
.badge-blue {
  display: inline-flex; align-items: center; height: 22px; padding: 0 9px;
  border-radius: 6px; background: $blue-soft; color: $blue; font-size: 11.5px; font-weight: 600;
}
.hero-l h3 { margin: 10px 0 6px; font-size: 20px; color: $ink; font-weight: 600; }
.hero-l p { margin: 0; font-size: 12.5px; color: $ink-3; }
.hero-stats { display: flex; flex: none; }
.hero-stats > div { padding: 0 26px; text-align: center; border-left: 1px solid $line; }
.hero-stats > div:first-child { border-left: 0; }
.hero-stats b { display: block; font-size: 26px; color: $blue; line-height: 1.1; font-weight: 700; }
.hero-stats b small { font-size: 12px; color: $ink-4; font-weight: 500; }
.hero-stats span { font-size: 11.5px; color: $ink-4; }

/* --- 方向分组 --- */
.grp { margin-top: 22px; }
.grp-h { display: flex; align-items: flex-end; justify-content: space-between; gap: 12px; margin-bottom: 12px; }
.grp-h h4 { margin: 0 0 2px; font-size: 15px; color: $ink; font-weight: 600; }
.grp-h p { margin: 0; font-size: 11.5px; color: $ink-4; }
.grp-h .hint { font-size: 12px; color: $ink-4; flex: none; }

/* --- 卡片网格 --- */
.qgrid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 14px; }
.qcard {
  background: #fff; border: 1px solid $line; border-radius: 10px; overflow: hidden;
  cursor: pointer; transition: border-color .15s, box-shadow .15s, transform .15s;
}
.qcard:hover { border-color: $blue; box-shadow: 0 4px 14px rgba(23, 100, 245, .12); transform: translateY(-1px); }
.thumb {
  height: 104px; background: $panel-2; display: flex; align-items: flex-end;
  border-bottom: 1px solid $line-2; overflow: hidden;
}
.thumb img { width: 100%; height: 100%; object-fit: cover; display: block; }
.thumb-ph { display: block; width: 100%; height: 100%; }
.thumb-ph ::v-deep svg { width: 100%; height: 100%; display: block; }
.qc-b { padding: 10px 12px 11px; }
.qc-b > b {
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
  overflow: hidden; font-size: 12.5px; color: $ink; font-weight: 600;
  margin-bottom: 8px; line-height: 1.4; min-height: 35px;
}
.qc-f { display: flex; align-items: center; justify-content: space-between; font-size: 11px; color: $ink-4; }
.qc-f b { color: $ink-2; font-size: 11.5px; }

@media (max-width: 1400px) { .qgrid { grid-template-columns: repeat(4, 1fr); } }
@media (max-width: 1100px) {
  .qgrid { grid-template-columns: repeat(2, 1fr); }
  .hero { flex-direction: column; align-items: flex-start; }
  .hero-stats { flex-wrap: wrap; }
}
</style>
