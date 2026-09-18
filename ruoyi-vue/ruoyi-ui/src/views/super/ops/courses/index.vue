<template>
  <div class="s-page">
    <header class="s-head">
      <div>
        <span class="eyebrow">SUPER ADMIN · OPERATION</span>
        <h1>课程与题库总览</h1>
        <p>跨部门核对「培养资料是否齐备」：课程发布状态与题库健康度。超管<b>只读</b>，增改请由对应部门管理员操作。</p>
      </div>
      <div class="s-head-actions">
        <span class="s-ro"><i class="el-icon-view" /> 全局只读</span>
        <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="loadAll">刷新</el-button>
      </div>
    </header>

    <div class="s-grid">
      <section class="s-card s-c7">
        <div class="s-card-h">
          <div class="tt"><span class="s-idx">课</span><h3>课程发布总览</h3></div>
          <span class="hint">共 {{ courses.length }} 门 · 已发布 {{ publishedCount }} 门</span>
        </div>
        <div style="max-height:420px;overflow-y:auto">
          <table v-if="courses.length" class="s-tbl">
            <thead><tr><th>课程</th><th style="width:130px">岗位</th><th style="width:70px">章节</th><th style="width:70px">单项</th><th style="width:88px">状态</th></tr></thead>
            <tbody>
              <tr v-for="c in courses" :key="c.id">
                <td class="nm">{{ c.courseName }}</td>
                <td>{{ positionName(c.positionId) }}</td>
                <td>{{ c.chapterCount || 0 }}</td>
                <td class="num">{{ c.itemCount || 0 }}</td>
                <td><span class="s-badge" :class="statusTone(c.status)">{{ statusText(c.status) }}</span></td>
              </tr>
            </tbody>
          </table>
          <div v-else class="s-empty"><i class="el-icon-reading" /><span>暂无课程</span></div>
        </div>
        <p class="s-note">章节 / 单项数量由后端统计（已过滤停用与已下线的章节检测项）；点课程名可去「课程管理」查看只读详情。</p>
      </section>

      <section class="s-card s-c5">
        <div class="s-card-h">
          <div class="tt"><span class="s-idx g">库</span><h3>题库健康度</h3></div>
          <span class="hint">{{ banks.length }} 个题库 · {{ emptyBanks.length }} 个空库</span>
        </div>
        <div style="max-height:420px;overflow-y:auto">
          <table v-if="banks.length" class="s-tbl">
            <thead><tr><th>题库</th><th style="width:70px">类型</th><th style="width:66px">题量</th><th style="width:78px">健康度</th></tr></thead>
            <tbody>
              <tr v-for="b in banks" :key="b.id">
                <td class="nm">{{ b.bankName }}</td>
                <td>{{ b.bankType === 'FORMAL' ? '正式' : '模拟' }}</td>
                <td class="num">{{ b.questionCount || 0 }}</td>
                <td>
                  <span class="s-badge" :class="healthTone(b)">{{ healthText(b) }}</span>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-else class="s-empty"><i class="el-icon-collection" /><span>暂无题库</span></div>
        </div>
        <p class="s-note">
          判据：<b>题量 ≥ 单场组卷需求 × 2</b> 记「充足」，有题但偏少记「偏少」，0 题记「空库」。
          单场需求取该岗位正式考核配置的题目数量，未配置时按 20 题估算。
        </p>
      </section>
    </div>
  </div>
</template>

<script>
import { listCourse } from '@/api/business/course'
import { listBank } from '@/api/business/questionBank'
import { listPosition } from '@/api/business/position'

/**
 * 超管「课程与题库总览」（全局只读）
 * 真实数据：/business/course/list、/business/question-bank/list、/business/position/list
 */
export default {
  name: 'SuperOpsCourses',
  data() {
    return {
      loading: false,
      courses: [],
      banks: [],
      positions: []
    }
  },
  computed: {
    publishedCount() {
      return this.courses.filter(c => c.status === 'PUBLISHED').length
    },
    emptyBanks() {
      return this.banks.filter(b => !b.questionCount)
    }
  },
  created() {
    this.loadAll()
  },
  methods: {
    loadAll() {
      this.loading = true
      Promise.all([
        listCourse({ pageNum: 1, pageSize: 200 }).then(res => { this.courses = res.rows || [] }).catch(() => { this.courses = [] }),
        listBank({ pageNum: 1, pageSize: 200 }).then(res => { this.banks = res.rows || [] }).catch(() => { this.banks = [] }),
        listPosition({ pageNum: 1, pageSize: 100 }).then(res => { this.positions = res.rows || [] }).catch(() => { this.positions = [] })
      ]).finally(() => { this.loading = false })
    },
    positionName(id) {
      const hit = this.positions.find(p => Number(p.id) === Number(id))
      return hit ? hit.positionName : ('岗位 ' + id)
    },
    statusText(s) {
      return { DRAFT: '草稿', PUBLISHED: '已发布', DISABLED: '已停用' }[s] || s
    },
    statusTone(s) {
      return { DRAFT: 'warn', PUBLISHED: 'ok', DISABLED: 'red' }[s] || ''
    },
    healthText(b) {
      const n = b.questionCount || 0
      if (n === 0) return '空库'
      return n >= 40 ? '充足' : '偏少'
    },
    healthTone(b) {
      const n = b.questionCount || 0
      if (n === 0) return 'warn'
      return n >= 40 ? 'ok' : 'blue'
    }
  }
}
</script>

<style lang="scss" scoped>
@import '~@/assets/styles/super-module.scss';
</style>
