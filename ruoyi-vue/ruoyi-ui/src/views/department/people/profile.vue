<template>
  <div v-loading="loading" class="dept-page">
    <div class="dept-breadcrumb">
      <el-button type="text" @click="goBack">人员管理</el-button>
      <span>/</span>
      <el-button type="text" @click="goBack">实习生管理</el-button>
      <span>/</span>
      <b>{{ person.realName || '个人全景档案' }}</b>
    </div>

    <!-- 身份条 -->
    <div class="dsec" style="padding:18px 22px">
      <div class="didentity">
        <span class="big-ph">{{ (person.realName || '学').slice(0, 1) }}</span>
        <div class="who">
          <b>{{ person.realName || '—' }}</b>
          <p>
            {{ person.positionName || '—' }} · {{ person.deptName || '本部门' }} ·
            导师 {{ person.mentorName || '未分配' }} ·
            入职 {{ fmtDay(person.expectedEntryDate) }}
          </p>
        </div>
        <div class="facts">
          <div class="fact">
            <span>培养状态</span>
            <b style="font-size:14px">{{ statusText }}</b>
          </div>
          <div class="fact">
            <span>保密协议</span>
            <b :class="demo.protocol ? 'ok' : 'warn'" style="font-size:14px">{{ demo.protocol ? '已签署' : '未签署' }}</b>
          </div>
          <div class="fact"><span>学习完成率</span><b>{{ demo.studyRate }}%</b></div>
          <div class="fact">
            <span>正式考核</span>
            <b :class="demo.examKey === 'PASS' ? 'ok' : ''" style="font-size:14px">{{ demo.examText }}</b>
          </div>
        </div>
      </div>
    </div>

    <div class="dgrid">
      <!-- 培养进度 -->
      <div class="dcard c4">
        <div class="dcard-h">
          <div class="tt"><span class="idx g">进</span><h3>培养进度</h3></div>
        </div>
        <div class="dvsteps">
          <div v-for="step in steps" :key="step.key" class="dvstep" :class="step.state">
            <span class="mark">
              <i v-if="step.state === 'done'" class="el-icon-check" />
              <template v-else>{{ step.no }}</template>
            </span>
            <div class="txt">
              <b>{{ step.title }}</b>
              <span>{{ step.desc }}</span>
            </div>
          </div>
        </div>
        <div style="margin-top:16px">
          <el-button
            type="primary"
            size="small"
            style="width:100%"
            :disabled="!canPromote"
            @click="goPromotion"
          >
            {{ canPromote ? '进入转正审核' : '尚未具备转正资格' }}
          </el-button>
        </div>
      </div>

      <!-- 能力画像 -->
      <div class="dcard c8">
        <div class="dcard-h">
          <div class="tt"><span class="idx">能</span><h3>能力画像</h3></div>
          <span class="hint-text">四维度 · 数据完整度 {{ demo.completeness }}%</span>
        </div>
        <div class="dgrid" style="gap:14px">
          <div class="c6">
            <div v-for="d in dimensions" :key="d.name" class="dhbar">
              <div class="dhbar-head">
                <span class="dhbar-name">{{ d.name }}</span>
                <span class="dhbar-meta"><b :class="d.tone">{{ d.measured ? d.value : '--' }}</b></span>
              </div>
              <div class="dhbar-track">
                <div class="dhbar-fill" :class="d.barTone" :style="{ width: (d.measured ? d.value : 0) + '%' }" />
              </div>
            </div>
          </div>
          <div class="c6">
            <div class="dcallout" style="padding:10px 12px;margin-bottom:10px">
              <i class="el-icon-info" />
              <span>
                <b>来源</b>：学习投入←课程进度；理论掌握←模拟/正式理论正确率；
                实践能力←实操提交与评分；规范遵从←阶段评价 + 协议。
              </span>
            </div>
            <div class="dcallout warn" style="padding:10px 12px;margin:0">
              <i class="el-icon-warning-outline" />
              <span>
                <b>安全约束</b>：<code>stage_evaluation.visible_scope='ADMIN_ONLY'</code> 的评价
                <b>只在管理端可见</b>，实习生端必须后端过滤。
                未测评维度显示 <b>--</b>，不得用 0 分占位。
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 学习明细 -->
      <div class="dcard c6">
        <div class="dcard-h">
          <div class="tt"><span class="idx">课</span><h3>学习明细</h3></div>
          <span class="hint-text">
            完成率 {{ demo.studyRate }}%
            <span class="dsample">示例</span>
          </span>
        </div>
        <div v-for="c in courses" :key="c.name" class="dhbar">
          <div class="dhbar-head">
            <span class="dhbar-name">{{ c.name }}</span>
            <span class="dhbar-meta"><b :class="c.tone">{{ c.rate }}%</b></span>
          </div>
          <div class="dhbar-track">
            <div class="dhbar-fill" :class="c.barTone" :style="{ width: c.rate + '%' }" />
          </div>
        </div>
      </div>

      <!-- 考核记录 -->
      <div class="dcard c6">
        <div class="dcard-h">
          <div class="tt"><span class="idx">考</span><h3>考核记录</h3></div>
          <span class="hint-text">
            模拟 {{ mockCount }} 次 · 正式 {{ formalCount }} 次
            <span class="dsample">示例</span>
          </span>
        </div>
        <table class="dtbl">
          <thead>
            <tr><th>类型</th><th>场次</th><th>得分</th><th>结论</th><th>时间</th></tr>
          </thead>
          <tbody>
            <tr v-for="r in records" :key="r.key">
              <td><span class="dbadge" :class="r.typeTone">{{ r.typeText }}</span></td>
              <td>{{ r.title }}</td>
              <td><span class="strong">{{ r.score }}</span></td>
              <td><span class="dbadge" :class="r.tone">{{ r.conclusion }}</span></td>
              <td class="muted">{{ r.time }}</td>
            </tr>
          </tbody>
        </table>
        <p v-if="!records.length" class="dsec-note">暂无考核记录。</p>
      </div>
    </div>

    <div class="dcallout" style="margin-top:16px">
      <i class="el-icon-info" />
      <span>
        本页与「超管端整体设计稿」的<b>个人全景档案是同款页面</b>，只有作用域不同
        （部门管理员锁定本部门，超管可跨部门）——同一个页面、同一套数据，不需要做两份。
      </span>
    </div>
  </div>
</template>

<script>
import { listRegister } from '@/api/business/register'

const STATUS_TEXT = {
  WAIT_AUDIT: '注册待审',
  PRE_TRAINEE: '预备实习中',
  PENDING_PROMOTE: '转正审核中',
  FORMAL_TRAINEE: '已转正',
  DISABLED: '已停用',
  ARCHIVED: '已归档'
}

function demoOf(userId) {
  const seed = ((userId || 1) * 9301) % 233280
  const r1 = Math.round((seed / 233280) * 55) + 40
  return {
    protocol: seed % 3 !== 0,
    studyRate: Math.min(r1 + 15, 100),
    completeness: 78,
    examKey: seed % 5 === 0 ? 'NONE' : (seed % 3 === 0 ? 'PENDING' : 'PASS'),
    examText: seed % 5 === 0 ? '未参加' : (seed % 3 === 0 ? '待批阅' : '已通过'),
    dimensions: [
      { name: '学习投入', value: Math.min(r1 + 10, 100), measured: true },
      { name: '理论掌握', value: Math.min(r1 + 6, 100), measured: true },
      { name: '实践能力', value: Math.max(r1 - 4, 30), measured: true },
      { name: '规范遵从', value: 0, measured: false }
    ]
  }
}

export default {
  name: 'DeptStudentProfile',
  data() {
    return {
      loading: false,
      person: {},
      demo: demoOf(0)
    }
  },
  computed: {
    userId() {
      return this.$route.params.userId
    },
    statusText() {
      return STATUS_TEXT[this.person.userStatus] || this.person.userStatus || '—'
    },
    canPromote() {
      return this.person.userStatus === 'PENDING_PROMOTE'
    },
    steps() {
      const st = this.person.userStatus
      const passedProtocol = this.demo.protocol
      const studyOk = this.demo.studyRate >= 70
      const examOk = this.demo.examKey === 'PASS'
      const formal = st === 'FORMAL_TRAINEE'
      return [
        {
          key: 'protocol', no: 1, title: '协议签署',
          desc: passedProtocol ? this.fmtDay(this.person.createTime) + ' 已签' : '尚未签署',
          state: passedProtocol ? 'done' : 'now'
        },
        {
          key: 'study', no: 2, title: '在线学习',
          desc: this.demo.studyRate + '% · ' + (studyOk ? '已达 70% 门槛' : '未达门槛'),
          state: studyOk ? 'done' : (passedProtocol ? 'now' : '')
        },
        {
          key: 'exam', no: 3, title: '正式考核',
          desc: examOk ? '已通过 · 可提交转正' : this.demo.examText,
          state: examOk ? 'done' : (studyOk ? 'now' : '')
        },
        {
          key: 'promote', no: 4, title: '转正发证',
          desc: formal ? '已转正 · 证书已签发' : (st === 'PENDING_PROMOTE' ? '待部门终审' : '待部门推荐'),
          state: formal ? 'done' : (st === 'PENDING_PROMOTE' ? 'now' : '')
        }
      ]
    },
    dimensions() {
      return this.demo.dimensions.map(d => ({
        name: d.name,
        value: d.value,
        measured: d.measured,
        tone: !d.measured ? 'mute' : (d.value >= 85 ? 'good' : (d.value >= 70 ? 'mid' : 'poor')),
        barTone: d.value >= 85 ? 'good' : (d.value >= 70 ? 'avg' : 'poor')
      }))
    },
    courses() {
      const base = this.demo.studyRate
      const raw = [
        ['Java 基础', 100],
        ['MySQL 实务', 100],
        ['Spring Boot', Math.max(30, base - 16)],
        ['Docker 部署', Math.max(10, Math.round(base * 0.23))]
      ]
      return raw.map(([name, rate]) => ({
        name,
        rate,
        tone: rate >= 90 ? 'good' : (rate >= 60 ? 'mid' : 'poor'),
        barTone: rate >= 90 ? 'good' : (rate >= 60 ? 'avg' : 'poor')
      }))
    },
    records() {
      const st = this.person.userStatus
      const out = []
      if (st !== 'WAIT_AUDIT' && this.demo.examKey !== 'NONE') {
        out.push({
          key: 'f1',
          typeText: '正式', typeTone: 'purple',
          title: '2026Q3 正式考核 v2',
          score: (78 + (this.demo.studyRate % 8)).toFixed(1),
          conclusion: this.demo.examKey === 'PASS' ? '通过' : '待批阅',
          tone: this.demo.examKey === 'PASS' ? 'green' : 'orange',
          time: '09-14'
        })
      }
      out.push({ key: 'm1', typeText: '模拟', typeTone: 'blue', title: '随机 10 题自测', score: '8/10', conclusion: '80%', tone: 'gray', time: '09-10' })
      out.push({ key: 'm2', typeText: '模拟', typeTone: 'blue', title: '随机 10 题自测', score: '9/10', conclusion: '90%', tone: 'gray', time: '09-06' })
      return out
    },
    mockCount() {
      return this.records.filter(r => r.typeText === '模拟').length
    },
    formalCount() {
      return this.records.filter(r => r.typeText === '正式').length
    }
  },
  watch: {
    userId() {
      this.loadPerson()
    }
  },
  created() {
    this.loadPerson()
  },
  methods: {
    loadPerson() {
      const id = this.userId
      this.demo = demoOf(Number(id) || 0)
      this.loading = true
      listRegister({ pageNum: 1, pageSize: 200 }).then(res => {
        const rows = (res && res.rows) || []
        this.person = rows.find(r => String(r.userId) === String(id) || String(r.id) === String(id)) || {}
      }).catch(() => {
        this.person = {}
      }).finally(() => {
        this.loading = false
      })
    },
    fmtDay(value) {
      if (!value) return '—'
      const d = new Date(value)
      if (isNaN(d.getTime())) return '—'
      const p = n => (n < 10 ? '0' + n : '' + n)
      return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate())
    },
    goBack() {
      this.$router.push('/department/people/students')
    },
    goPromotion() {
      this.$router.push({ path: '/department/people/promotion', query: { id: String(this.userId) } })
    }
  }
}
</script>

<style lang="scss" scoped>
@import '~@/assets/styles/department-module.scss';

.hint-text { color: #98a2b3; font-size: 11.5px; }
.dtbl .muted { color: #98a2b3; }
code { padding: 1px 5px; color: #344054; font-size: 11.5px; background: #f2f4f7; border-radius: 4px; }
.dkv small { color: #98a2b3; font-size: 11px; font-weight: 400; }

@media (max-width: 1280px) {
  .didentity { flex-wrap: wrap; }
  .didentity .facts { width: 100%; padding-top: 14px; border-top: 1px solid #e4e9f0; }
}
@media (max-width: 820px) {
  .didentity .facts { flex-wrap: wrap; gap: 14px 0; }
  .didentity .fact { padding: 0 14px; }
}
</style>
