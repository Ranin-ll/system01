<template>
  <div v-loading="loading" class="dept-page">
    <div class="dept-breadcrumb">
      人员管理 <span>/</span> <b>实习转正审核</b>
    </div>

    <div class="dept-heading">
      <div>
        <span class="eyebrow">DEPARTMENT ADMIN</span>
        <h1>实习转正审核</h1>
        <p>把「预备实习生」变成「正式实习生」的审批台。左列表 + 右决策页，核心是资格核对清单 —— 每项都有明确的数据依据。</p>
      </div>
      <div class="dept-heading-actions">
        <span class="dsample">名单真实 · 资格项待接入</span>
      </div>
    </div>

    <div class="dcallout ok">
      <i class="el-icon-success" />
      <span>
        <b>已按既定决策改写：部门管理员直接终审。</b>本页<b>不再有「推荐给超管」这一步</b>，
        按钮语义从「推荐」改为「审批通过」，审批即时生效并自动发证。
      </span>
    </div>

    <!-- 筛选条 -->
    <div class="dsec" style="padding:16px 22px">
      <div class="dfilter" style="margin-bottom:0">
        <div class="dchips" style="margin:0">
          <span
            v-for="tab in statusTabs"
            :key="tab.key"
            class="dchip"
            :class="{ on: activeStatus === tab.key }"
            @click="activeStatus = tab.key"
          >
            {{ tab.label }} <span class="n">{{ tab.count }}</span>
          </span>
        </div>
        <span class="grow" />
        <el-select v-model="positionFilter" size="small" placeholder="岗位：全部" clearable style="width:160px">
          <el-option v-for="p in positionOptions" :key="p" :label="p" :value="p" />
        </el-select>
      </div>
    </div>

    <div class="dgrid" style="margin-top:16px">
      <!-- 左：待处理列表 -->
      <div class="dcard c4">
        <div class="dcard-h">
          <div class="tt"><span class="idx o">待</span><h3>{{ activeTabLabel }}</h3></div>
          <span class="hint-text">{{ listRows.length }} 人</span>
        </div>
        <div v-if="listRows.length" class="dtodo">
          <div
            v-for="row in listRows"
            :key="row.id"
            class="dtodo-row clickable"
            :class="{ sel: current && current.id === row.id }"
            @click="select(row)"
          >
            <span class="dtodo-dot" :class="row.dotTone" />
            <div class="dtodo-main">
              <div class="dtodo-t">{{ row.name }} · {{ row.position }}</div>
              <div class="dtodo-s">{{ row.summary }}</div>
            </div>
            <span class="dbadge" :class="row.verdict.tone">{{ row.verdict.text }}</span>
          </div>
        </div>
        <div v-else class="dempty small">
          <i class="el-icon-document-checked" />
          <strong>暂无记录</strong>
        </div>
        <p class="dsec-note">列表行显示「资格齐备 / N 项待补 / 未达门槛」，让管理员一眼知道要不要点进去。</p>
      </div>

      <!-- 右：审核详情 -->
      <div class="dcard c8">
        <template v-if="current">
          <div class="dcard-h">
            <div class="tt"><span class="idx">审</span><h3>审核详情 · {{ current.name }}</h3></div>
            <span class="hint-text">{{ current.submittedAt ? '提交时间 ' + current.submittedAt : '尚未提交转正申请' }}</span>
          </div>

          <div class="dcallout" :class="current.verdict.tone === 'green' ? 'ok' : (current.verdict.tone === 'red' ? 'warn' : 'warn')" style="margin-bottom:12px">
            <i class="el-icon-info" />
            <span>
              <b>资格核对清单</b> · {{ checklistSummary }}（<b>部门终审即生效</b>）
            </span>
          </div>

          <div class="dfg2" style="margin-bottom:14px">
            <div v-for="item in current.checklist" :key="item.key" class="dchk">
              <span class="box" :class="item.pass === null ? 'pend' : (item.pass ? 'sw' : 'fail')">
                <i :class="item.pass === null ? 'el-icon-more' : (item.pass ? 'el-icon-check' : 'el-icon-close')" />
              </span>
              <span v-html="item.text" />
            </div>
          </div>

          <div class="dgrid" style="gap:14px">
            <div class="dkv c6" style="grid-template-columns:repeat(2,1fr)">
              <div><span>理论分</span><strong>{{ fmtScore(current.theory) }} <small>/ 100</small></strong></div>
              <div><span>实操分</span><strong>{{ fmtScore(current.practice) }} <small>/ 100</small></strong></div>
              <div><span>加权综合分</span><strong>{{ fmtScore(current.total) }} <small>理论60% 实操40%</small></strong></div>
              <div>
                <span>通过线</span>
                <strong v-if="current.passed === null" style="color:#98a2b3">≥ {{ passLine }} · 待判定</strong>
                <strong v-else :style="{ color: current.passed ? '#067647' : '#b42318' }">
                  ≥ {{ passLine }} · {{ current.passed ? '已过' : '未过' }}
                </strong>
              </div>
            </div>
            <div class="c6">
              <div v-for="d in current.dimensions" :key="d.name" class="dhbar">
                <div class="dhbar-head">
                  <span class="dhbar-name">{{ d.name }}</span>
                  <span class="dhbar-meta"><b :class="d.tone">{{ d.value }}</b></span>
                </div>
                <div class="dhbar-track">
                  <div class="dhbar-fill" :class="d.barTone" :style="{ width: d.value + '%' }" />
                </div>
              </div>
              <p v-if="!current.dimensions.length" class="dsec-note">四维能力待生成（需 profile_snapshot 聚合接口）。</p>
            </div>
          </div>

          <div class="dfield">
            <label>部门管理员评价 · 优势</label>
            <div class="dinp">{{ current.good || '—' }}</div>
          </div>
          <div class="dfield">
            <label>待提升 / 改进建议</label>
            <div class="dinp">{{ current.improve || '—' }}</div>
          </div>

          <!-- 状态流转 -->
          <div class="dflow">
            <span class="nd done">DEPT_PENDING 待部门审核</span>
            <span class="ar">→</span>
            <span class="nd on">PASSED 部门审批通过</span>
            <span class="ar">→</span>
            <span class="nd">自动发证 · 转 FORMAL_TRAINEE</span>
          </div>
          <p class="dsec-note">
            驳回可重新提交。<b>本决策下 <code>SUPER_PENDING</code>（超管终审）被跳过</b>，审批即时生效：
            证书由 <code>certificate</code> 生成，<code>sys_user.user_status</code> 直接置为 <code>FORMAL_TRAINEE</code>，
            实习生端工作台底部证书标签随之点亮。
          </p>

          <div class="dbtn-row">
            <el-button size="small" :disabled="current.status !== 'PENDING'" @click="reject()">驳回并说明原因</el-button>
            <el-button size="small" type="primary" :disabled="current.status !== 'PENDING' || !current.passed" @click="approve()">
              审批通过 · 即时生效并发证
            </el-button>
          </div>
        </template>
        <div v-else class="dempty">
          <i class="el-icon-mouse" />
          <strong>请在左侧选择一条记录</strong>
          <span>选中后此处会展示资格核对清单、四项能力分与审批动作。</span>
        </div>
      </div>
    </div>

    <!-- 配套改动 + 规则影响 -->
    <div class="dgrid" style="margin-top:16px">
      <div class="dcard c8">
        <div class="dcard-h">
          <div class="tt"><span class="idx o">配</span><h3>本决策必须配套的 4 项改动</h3></div>
          <span class="hint-text">否则会「有入口、审不了」</span>
        </div>
        <table class="dtbl">
          <thead>
            <tr><th style="width:30px">#</th><th style="width:250px">改动</th><th>原因 / 做法</th></tr>
          </thead>
          <tbody>
            <tr>
              <td>1</td>
              <td><span class="strong">状态机跳过 <code>SUPER_PENDING</code></span></td>
              <td><code>DEPT_PENDING → PASSED / REJECTED</code>；<code>super_approve_by/time</code> 两列保留不写（留给超管代操作留痕）</td>
            </tr>
            <tr>
              <td>2</td>
              <td><span class="strong"><code>sys_user.user_status</code> 驱动方下沉</span></td>
              <td>审批通过时由 Service 直接置 <code>FORMAL_TRAINEE</code>，<b>不再等超管</b>；同时落证 + 回写 <code>cert_no</code></td>
            </tr>
            <tr>
              <td>3</td>
              <td><span class="strong">为 <code>DEPT_ADMIN</code> 补转正审批权限</span></td>
              <td>现有权限里没有这项 → 新增菜单 + 角色授权（与菜单迁移合并做）</td>
            </tr>
            <tr>
              <td>4</td>
              <td><span class="strong">保留纠错路径</span></td>
              <td><code>operate_log</code> 留痕 + 超管「撤回转正」（退回 <code>PRE_TRAINEE</code> + 证书作废）</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="dcard c4">
        <div class="dcard-h">
          <div class="tt"><span class="idx p">规</span><h3>规则与超管端影响</h3></div>
        </div>
        <div class="dcallout warn" style="margin-bottom:10px">
          <i class="el-icon-warning-outline" />
          <span><b>业务规则变更</b>：原需求「预备→正式需通过正式考试<b>并经超管审批</b>」在本决策下<b>作废</b>，改为「部门审批即生效」。请同步改需求文档，否则验收会与文档冲突。</span>
        </div>
        <div class="dcallout warn">
          <i class="el-icon-warning-outline" />
          <span><b>超管端设计稿失效</b>：原画了「超管终审」环节，需删除，只留「查看 + 撤回转正」。</span>
        </div>
        <p class="dsec-note">
          <b>好消息</b>：<code>promotion_application</code> / <code>certificate</code> / <code>stage_evaluation</code>
          <b>全部零 Java 层</b>，整套都是新增 —— <b>不受「已实现接口一律不改」约束</b>，也没有改老代码的回归风险。
        </p>
      </div>
    </div>
  </div>
</template>

<script>
import { listRegister } from '@/api/business/register'

const PASS_LINE = 70

/** 资格核对清单：4 项数据依据（学习完成率 / 正式考核结论 / 协议签署 / 必修完成）
 *  均无聚合接口（profile_snapshot / promotion_application 零 Java 层）
 *  ⇒ pass = null 表示「待接入」，渲染为灰点，不用假分数假装已核对 */
function buildChecklist() {
  return [
    { key: 'study', pass: null, text: '学习完成率 <b>待接入</b>（需部门学习聚合接口）' },
    { key: 'exam', pass: null, text: '正式考核结论 <b>待接入</b>（需成绩聚合接口）' },
    { key: 'protocol', pass: null, text: '保密协议签署状态 <b>待接入</b>' },
    { key: 'required', pass: null, text: '未完成必修项 <b>待接入</b>' }
  ]
}

/** 只把「名单」接真：来自 register_application（status=PASSED）；资格项无接口 ⇒ 留空 */
function mapCandidate(row, status) {
  return {
    id: row.userId || row.id,
    userId: row.userId,
    name: row.realName || '—',
    position: row.positionName || '—',
    mentorName: row.mentorName || '',
    entryDate: row.expectedEntryDate || row.createTime || null,
    status,
    submittedAt: null,
    theory: null,
    practice: null,
    total: null,
    passed: null,
    good: null,
    improve: null,
    checklist: buildChecklist(),
    dimensions: [],
    verdict: status === 'PASSED'
      ? { text: '已转正', tone: 'green', dot: 'g' }
      : { text: '待核对', tone: 'orange', dot: 'o' },
    summary: (row.positionName || '—') + ' · 导师 ' + (row.mentorName || '未分配')
  }
}

export default {
  name: 'DeptPromotion',
  data() {
    return {
      passLine: PASS_LINE,
      activeStatus: 'PENDING',
      positionFilter: '',
      candidates: [],
      currentId: null,
      loading: false
    }
  },
  computed: {
    statusTabs() {
      const count = s => this.candidates.filter(c => c.status === s).length
      return [
        { key: 'PENDING', label: '待处理', count: count('PENDING') },
        { key: 'PASSED', label: '已通过', count: count('PASSED') },
        { key: 'REJECTED', label: '已驳回', count: count('REJECTED') }
      ]
    },
    activeTabLabel() {
      const found = this.statusTabs.find(t => t.key === this.activeStatus)
      return found ? found.label : '待处理'
    },
    positionOptions() {
      const seen = {}
      const out = []
      this.candidates.forEach(c => {
        if (c.position && !seen[c.position]) {
          seen[c.position] = 1
          out.push(c.position)
        }
      })
      return out
    },
    /** 列表：可带 positionFilter，然后按状态筛选 */
    listRows() {
      const scope = this.candidates.filter(c => !this.positionFilter || c.position === this.positionFilter)
      const byStatus = scope.filter(c => c.status === this.activeStatus)
      return byStatus.map(c => ({
        id: c.id,
        name: c.name,
        position: c.position,
        summary: c.summary,
        verdict: c.verdict,
        dotTone: c.verdict.dot
      }))
    },
    current() {
      return this.candidates.find(c => c.id === this.currentId) || null
    },
    checklistSummary() {
      if (!this.current) return ''
      const list = this.current.checklist
      const unknown = list.filter(c => c.pass === null).length
      if (unknown === list.length) return '资格项数据待接入，暂无法自动核对'
      const fail = list.filter(c => c.pass === false).length
      if (fail) return fail + ' 项未通过，需补齐后再提交'
      return (list.length - unknown) + ' 项通过，可直接审批转正' + (unknown ? '（另有 ' + unknown + ' 项待接入）' : '')
    }
  },
  watch: {
    /** 切换状态页签时自动选中第一条，避免右侧空白 */
    activeStatus() {
      this.syncSelection()
    },
    listRows() {
      this.syncSelection()
    }
  },
  created() {
    this.loadCandidates()
  },
  methods: {
    /** 只接真「名单」：register_application 里注册已通过的人，按培养状态归入两个页签 */
    loadCandidates() {
      this.loading = true
      listRegister({ pageNum: 1, pageSize: 200 }).then(res => {
        const rows = (res && res.rows) || []
        const mapped = rows
          .filter(r => r.status === 'PASSED')
          .map(r => {
            if (r.userStatus === 'FORMAL_TRAINEE') return mapCandidate(r, 'PASSED')
            if (r.userStatus === 'PENDING_PROMOTE') return mapCandidate(r, 'PENDING')
            return null
          })
          .filter(Boolean)
        this.candidates = mapped
        const fromQuery = Number(this.$route.query.id)
        if (fromQuery) {
          const hit = mapped.find(c => c.userId === fromQuery || c.id === fromQuery)
          if (hit) {
            this.activeStatus = hit.status
            this.currentId = hit.id
            return
          }
        }
        this.syncSelection()
      }).catch(() => {
        this.candidates = []
      }).finally(() => {
        this.loading = false
      })
    },
    syncSelection() {
      const rows = this.listRows
      if (!rows.length) {
        this.currentId = null
        return
      }
      if (!this.currentId || !rows.some(r => r.id === this.currentId)) {
        this.currentId = rows[0].id
      }
    },
    select(row) {
      this.currentId = row.id
    },
    fmtScore(value) {
      return value == null ? '—' : Number(value).toFixed(1)
    },
    approve() {
      const target = this.current
      if (!target) return
      this.$confirm(
        '将 ' + target.name + ' 的 sys_user.user_status 置为 FORMAL_TRAINEE 并自动签发电子证书，审批即时生效。',
        '确认审批通过',
        { confirmButtonText: '确认通过', cancelButtonText: '取消', type: 'warning' }
      ).then(() => {
        this.$message.warning('转正审批接口（promotion_application）尚未实现 —— 当前仅支持查看，未做任何变更')
      }).catch(() => {})
    },
    reject() {
      const target = this.current
      if (!target) return
      this.$prompt('请填写驳回原因（会同步给实习生）', '驳回转正申请', {
        confirmButtonText: '确认驳回',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: '例如：学习完成率 66% 未达 70% 门槛，建议继续培养一周期。'
      }).then(() => {
        this.$message.warning('驳回接口（promotion_application）尚未实现 —— 当前仅支持查看，未做任何变更')
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
@import '~@/assets/styles/department-module.scss';

.dfilter { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; }
.dfilter .grow { flex: 1; }

.dtodo-row.clickable { cursor: pointer; border-radius: 8px; transition: background .15s; }
.dtodo-row.clickable:hover { background: #f7f9fc; }
.dtodo-row.sel { margin: 0 -10px; padding-right: 10px; padding-left: 10px; background: #e8f1fd; }
.dtodo-row.sel .dtodo-t { color: #1764f5; }

/* 资格核对清单 */
.dchk { display: flex; align-items: flex-start; gap: 8px; padding: 7px 0; color: #344054; font-size: 12.5px; line-height: 1.6; }
.dchk .box { display: inline-flex; width: 18px; height: 18px; flex: none; align-items: center; justify-content: center; margin-top: 1px; font-size: 11px; border-radius: 5px; }
.dchk .box.sw { color: #fff; background: #12b76a; }
.dchk .box.fail { color: #fff; background: #f04438; }
.dchk .box.pend { color: #fff; background: #d0d5dd; }

/* 表单域 */
.dfield { margin-top: 12px; }
.dfield label { display: block; margin-bottom: 6px; color: #667085; font-size: 11.5px; }
.dinp { padding: 9px 12px; color: #344054; font-size: 12.5px; line-height: 1.65; background: #f7f9fc; border: 1px solid #e4e9f0; border-radius: 8px; }

/* 状态流转 */
.dflow { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; padding: 13px 14px; margin-top: 14px; background: #f7f9fc; border: 1px solid #eef1f6; border-radius: 8px; }
.dflow .nd { padding: 5px 11px; color: #667085; font-size: 12px; background: #fff; border: 1px solid #e4e9f0; border-radius: 6px; }
.dflow .nd.done { color: #067647; background: #e7f7ef; border-color: #b7e3d2; }
.dflow .nd.on { color: #fff; background: #1764f5; border-color: #1764f5; }
.dflow .ar { color: #98a2b3; }

.dbtn-row { display: flex; justify-content: flex-end; gap: 10px; margin-top: 14px; }
code { padding: 1px 5px; color: #344054; font-size: 11.5px; background: #f2f4f7; border-radius: 4px; }
.dkv small { color: #98a2b3; font-size: 11px; font-weight: 400; }
.hint-text { color: #98a2b3; font-size: 11.5px; }
.dkpi-val small { font-size: 12px; font-weight: 400; }
</style>
