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
            <b :class="demo.protocol ? 'ok' : 'warn'" style="font-size:14px">{{ demo.protocol === null ? '--' : (demo.protocol ? '已签署' : '未签署') }}</b>
          </div>
          <div class="fact"><span>学习完成率</span><b>{{ demo.studyRate === null ? '--' : demo.studyRate + '%' }}</b></div>
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
          <span class="hint-text">四维度 · 数据完整度 {{ demo.completeness === null ? '--' : demo.completeness + '%' }}</span>
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
            <p v-if="!dimensions.length" class="dsec-note">能力维度待生成。</p>
          </div>
          <div class="c6">
            <div class="dcallout" style="padding:10px 12px;margin-bottom:10px">
              <i class="el-icon-info" />
              <span>
                <b>来源</b>：学习投入←课程进度；理论掌握←模拟/正式理论正确率；
                实践能力←实操提交与评分；规范遵从←阶段评价 + 协议。
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
            完成率 {{ demo.studyRate === null ? '--' : demo.studyRate + '%' }}
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
        <p v-if="!courses.length" class="dsec-note">暂无按课程维度的完成率。</p>
      </div>

      <!-- 考核记录 -->
      <div class="dcard c6">
        <div class="dcard-h">
          <div class="tt"><span class="idx">考</span><h3>考核记录</h3></div>
          <span class="hint-text">
            模拟 {{ mockCount }} 次 · 正式 {{ formalCount }} 次
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

export default {
  name: 'DeptStudentProfile',
  data() {
    return {
      loading: false,
      person: {},
      /**
       * 2026-09-22：本页的协议 / 学习完成率 / 考核结论 / 数据完整度 / 四维能力
       * **没有可用的聚合接口**（`profile_snapshot` 零 Java 层、`stage_evaluation` 0 行）
       * ⇒ 一律留空（原来用 `demoOf(0)` 编造），模板侧显示「--」
       */
      demo: { protocol: null, studyRate: null, examKey: null, examText: '待接入', completeness: null, dimensions: [] }
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
      const rate = this.demo.studyRate
      const studyOk = rate !== null && rate >= 70
      const examOk = this.demo.examKey === 'PASS'
      const formal = st === 'FORMAL_TRAINEE'
      return [
        {
          key: 'protocol', no: 1, title: '协议签署',
          desc: passedProtocol === null ? '签署状态待接入' : (passedProtocol ? this.fmtDay(this.person.createTime) + ' 已签' : '尚未签署'),
          state: passedProtocol ? 'done' : 'now'
        },
        {
          key: 'study', no: 2, title: '在线学习',
          desc: rate === null ? '完成率待接入' : (rate + '% · ' + (studyOk ? '已达 70% 门槛' : '未达门槛')),
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
    /** 无「按课程维度的完成率」聚合接口 ⇒ 留空（不再用 studyRate 反推编造 4 门课） */
    courses() {
      return []
    },
    /** 无「该实习生考核记录」聚合接口 ⇒ 留空 */
    records() {
      return []
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
