<template>
  <div v-loading="loading" class="dept-page">
    <div class="dept-breadcrumb">
      人员管理 <span>/</span> <b>实习转正审核</b>
    </div>

    <div class="dept-heading">
      <div>
        <span class="eyebrow">DEPARTMENT ADMIN</span>
        <h1>实习转正审核</h1>
        <p>把「预备实习生」变成「正式实习生」的审批台。资格清单每项都有明确的数据依据，审批通过即生效并自动发证。</p>
      </div>
      <div class="dept-heading-actions">
        <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="loadAll">刷新</el-button>
      </div>
    </div>

    <!-- 转正要求：本部门设置，醒目强调卡 -->
    <div class="promo-req">
      <div class="promo-req-hd">
        <span class="promo-req-ico"><i class="el-icon-medal" /></span>
        <div class="promo-req-tt">
          <h3>转正要求</h3>
          <p>实习生需<b>同时满足</b>以下条件，才能提交转正申请</p>
        </div>
        <span class="promo-req-src" :class="ruleSourceTone">{{ ruleSourceText }}</span>
      </div>
      <div class="promo-req-body">
        <div class="promo-req-item">
          <span class="promo-req-label">学习完成率</span>
          <el-input-number v-model="ruleForm.studyRateMin" :min="0" :max="100" :step="5" size="small" controls-position="right" style="width:132px" />
          <span class="unit">%</span>
        </div>
        <div class="promo-req-item">
          <span class="promo-req-label">正式考核通过</span>
          <el-input-number v-model="ruleForm.examPassTimes" :min="1" :max="10" size="small" controls-position="right" style="width:132px" />
          <span class="unit">次</span>
        </div>
        <el-button type="primary" icon="el-icon-check" :loading="ruleSaving" @click="saveRule">保存要求</el-button>
      </div>
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
        <el-input
          v-model="keyword"
          size="small"
          placeholder="姓名 / 账号"
          clearable
          prefix-icon="el-icon-search"
          style="width:180px"
          @keyup.enter.native="loadCandidates"
          @clear="loadCandidates"
        />
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
            :key="row.userId"
            class="dtodo-row clickable"
            :class="{ sel: current && current.userId === row.userId }"
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
      </div>

      <!-- 右：审核详情 -->
      <div class="dcard c8">
        <template v-if="current">
          <div class="dcard-h">
            <div class="tt"><span class="idx">审</span><h3>审核详情 · {{ current.name }}</h3></div>
            <span class="hint-text">{{ current.submittedAt ? '提交时间 ' + current.submittedAt : '尚未提交转正申请' }}</span>
          </div>

          <div class="dcallout" :class="gateCalloutTone" style="margin-bottom:12px">
            <i class="el-icon-info" />
            <span>
              <b>资格核对清单</b> · {{ checklistSummary }}（<b>通过后立即生效</b>）
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
              <div><span>学习完成率</span><strong>{{ fmtRate(current.studyRate) }}</strong></div>
              <div><span>正式考核</span><strong>{{ fmtExam(current) }}</strong></div>
              <div><span>保密协议</span><strong :style="{ color: current.protocolSigned ? '#067647' : '#b42318' }">{{ current.protocolSigned ? '已签署' : '未签署' }}</strong></div>
              <div><span>转正要求来源</span><strong>{{ current.ruleSourceText }}</strong></div>
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
              <p v-if="!current.dimensions.length" class="dsec-note">尚未产生学习/考核数据，暂无可视化维度。</p>
            </div>
          </div>

          <div class="dfield">
            <label>实习生转正说明 / 阶段自评</label>
            <div class="dinp">{{ current.supplement || '—' }}</div>
          </div>
          <div v-if="current.applicationStatus === 'REJECTED'" class="dfield">
            <label>上次驳回原因</label>
            <div class="dinp" style="color:#7a271a">{{ current.rejectReason || '—' }}</div>
          </div>

          <!-- 状态流转 -->
          <div class="dflow">
            <span class="nd" :class="flowState('SUBMIT')">提交转正申请</span>
            <span class="ar">→</span>
            <span class="nd" :class="flowState('PENDING')">待部门审核</span>
            <span class="ar">→</span>
            <span class="nd" :class="flowState('PASSED')">部门审批通过</span>
            <span class="ar">→</span>
            <span class="nd" :class="flowState('DONE')">转为正式实习生 · 自动发证</span>
          </div>
          <p class="dsec-note">驳回后可重新提交；审批通过即生效并自动发证。</p>

          <div v-if="current.applicationStatus === 'DEPT_PENDING'" class="dbtn-row">
            <el-button size="small" @click="reject">驳回并说明原因</el-button>
            <el-button size="small" type="primary" :disabled="!current.eligible" @click="approve">
              审批通过 · 即时生效并发证
            </el-button>
          </div>
          <div v-else-if="current.applicationStatus === 'PASSED'" class="dbtn-row">
            <span class="hint-text" style="margin-right:auto">
              已于 {{ current.decidedAt || '—' }} 通过{{ current.deptApproveName ? '（' + current.deptApproveName + '）' : '' }}
              <template v-if="current.certNo"> · 证书编号 <b>{{ current.certNo }}</b></template>
            </span>
            <el-button v-if="isSuper" size="small" type="danger" plain @click="revoke">撤回转正</el-button>
          </div>
          <div v-else class="dbtn-row">
            <span class="hint-text" style="margin-right:auto">
              {{ current.applicationStatus === 'REJECTED' ? '该申请已被驳回，等待实习生修改后重新提交' : (current.eligible ? '该实习生资格齐备，尚未提交转正申请' : '该实习生尚未提交转正申请，且资格未齐备') }}
            </span>
          </div>
        </template>
        <div v-else class="dempty">
          <i class="el-icon-mouse" />
          <strong>请在左侧选择一条记录</strong>
          <span>选中后此处会展示资格核对清单、数据依据与审批动作。</span>
        </div>
      </div>
    </div>

    <!-- 审核留痕 -->
    <div class="dgrid" style="margin-top:16px">
      <div class="dcard c12">
        <div class="dcard-h">
          <div class="tt"><span class="idx o">痕</span><h3>审核留痕</h3></div>
          <span class="hint-text">{{ current ? (current.name + ' · ') : '' }}谁在何时做了哪一步</span>
        </div>
        <table v-if="history.length" class="dtbl">
          <thead>
            <tr><th style="width:150px">时间</th><th style="width:110px">动作</th><th style="width:210px">状态流转</th><th>说明</th></tr>
          </thead>
          <tbody>
            <tr v-for="(h, i) in history" :key="i">
              <td>{{ fmtTime(h.createTime) }}</td>
              <td><span class="dbadge" :class="actionTone(h.action)">{{ actionText(h.action) }}</span></td>
              <td>{{ h.fromStatus || '—' }} → {{ h.toStatus || '—' }}</td>
              <td>{{ h.reason || '—' }}</td>
            </tr>
          </tbody>
        </table>
        <div v-else class="dempty small">
          <i class="el-icon-time" />
          <strong>暂无审核记录</strong>
          <span>选中一位实习生后，这里显示完整的状态流转轨迹。</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import {
  listPromotionCandidates,
  getPromotionHistory,
  auditPromotion,
  revokePromotion,
  savePromotionRule
} from '@/api/business/promotion'

/**
 * 部门管理员「实习转正审核」页（2026-09-29 接真数据）
 *
 * 数据来源：
 *  · GET /business/promotion/candidates  → rows（含实时资格核对）+ summary
 *  · GET /business/promotion/{id}/history → 审核留痕
 *  · PUT/DELETE /business/promotion/rule  → 本部门转正要求
 *  · POST /business/promotion/audit       → 审批（通过即生效并发证）
 *
 * 状态机：DEPT_PENDING → PASSED / REJECTED（部门终审即生效，跳过 SUPER_PENDING）。
 * 前端只做展示与二次确认，服务端会重新校验资格（不信任前端）。
 */
export default {
  name: 'DeptPromotion',
  data() {
    return {
      loading: false,
      ruleSaving: false,
      candidates: [],
      summary: {},
      rule: { studyRateMin: 0, examPassTimes: 1, source: 'DEFAULT' },
      ruleForm: { studyRateMin: 0, examPassTimes: 1 },
      activeStatus: 'PENDING',
      keyword: '',
      positionFilter: '',
      currentUserId: null,
      history: []
    }
  },
  computed: {
    ...mapGetters(['deptId', 'deptName', 'roles']),
    isSuper() {
      return this.roles.indexOf('SUPER_ADMIN') > -1 || this.roles.indexOf('admin') > -1
    },
    statusTabs() {
      const s = this.summary || {}
      return [
        { key: 'PENDING', label: '待处理', count: s.pending || 0 },
        { key: 'ELIGIBLE', label: '资格齐备', count: s.eligible || 0 },
        { key: 'PASSED', label: '已通过', count: s.passed || 0 },
        { key: 'REJECTED', label: '已驳回', count: s.rejected || 0 }
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
        if (c.positionName && !seen[c.positionName]) {
          seen[c.positionName] = 1
          out.push(c.positionName)
        }
      })
      return out
    },
    /** 列表：客户端按状态 + 岗位筛选（份数小，切换即时） */
    listRows() {
      const scope = this.candidates.filter(c => !this.positionFilter || c.positionName === this.positionFilter)
      const byStatus = scope.filter(c => this.matchStatus(c, this.activeStatus))
      return byStatus.map(c => ({
        userId: c.userId,
        name: c.nickName || c.userName,
        position: c.positionName || '—',
        summary: this.summaryText(c),
        verdict: this.verdictOf(c),
        dotTone: this.dotOf(c)
      }))
    },
    current() {
      const row = this.candidates.find(c => c.userId === this.currentUserId)
      return row ? this.decorate(row) : null
    },
    ruleSourceText() {
      return this.rule.source === 'DEPT' ? '本部门设置' : '系统默认'
    },
    ruleSourceTone() {
      return this.rule.source === 'DEPT' ? 'dept' : 'sys'
    },
    checklistSummary() {
      if (!this.current || !this.current.gate) return '尚未取到资格数据'
      const fail = this.current.gate.failCount || 0
      return fail === 0 ? '3 项全部通过，可直接审批转正' : fail + ' 项未通过，需补齐后再审批'
    },
    gateCalloutTone() {
      if (!this.current || !this.current.gate) return 'warn'
      return this.current.gate.eligible ? 'ok' : 'warn'
    }
  },
  watch: {
    activeStatus() {
      this.syncSelection()
    },
    listRows() {
      this.syncSelection()
    }
  },
  created() {
    this.loadAll()
  },
  methods: {
    loadAll() {
      this.loadCandidates()
    },
    loadCandidates() {
      this.loading = true
      listPromotionCandidates({ status: 'ALL', keyword: this.keyword }).then(res => {
        const data = res.data || {}
        this.candidates = data.rows || []
        this.summary = data.summary || {}
        this.bindRule(data.rule)
        if (!this.candidates.some(c => c.userId === this.currentUserId)) {
          this.currentUserId = null
        }
        this.syncSelection()
      }).catch(() => {
        this.candidates = []
        this.summary = {}
      }).finally(() => {
        this.loading = false
      })
    },
    bindRule(rule) {
      if (!rule) return
      this.rule = rule
      this.ruleForm = {
        studyRateMin: Number(rule.studyRateMin != null ? rule.studyRateMin : 0),
        examPassTimes: Number(rule.examPassTimes != null ? rule.examPassTimes : 1)
      }
    },
    saveRule() {
      this.ruleSaving = true
      savePromotionRule({
        deptId: this.deptId,
        studyRateMin: this.ruleForm.studyRateMin,
        examPassTimes: this.ruleForm.examPassTimes
      }).then(res => {
        this.$modal.msgSuccess(res.msg || '转正要求已保存')
        this.loadCandidates()
      }).catch(() => {}).finally(() => {
        this.ruleSaving = false
      })
    },
    matchStatus(row, status) {
      const st = row.applicationStatus
      if (!status || status === 'ALL') return true
      if (status === 'PENDING') return st === 'DEPT_PENDING'
      if (status === 'PASSED') return st === 'PASSED'
      if (status === 'REJECTED') return st === 'REJECTED'
      if (status === 'REVOKED') return st === 'REVOKED'
      if (status === 'ELIGIBLE') {
        return !!(row.gate && row.gate.eligible) && st !== 'DEPT_PENDING' && st !== 'PASSED'
      }
      return true
    },
    summaryText(c) {
      const rate = c.studyRate == null ? '—' : Number(c.studyRate) + '%'
      const exam = (c.passedExamTimes || 0) + '/' + (c.examTimes || 0)
      const protocol = c.protocolSigned ? '协议已签' : '协议未签'
      return '完成率 ' + rate + ' · 考核 ' + exam + ' · ' + protocol
    },
    verdictOf(c) {
      const st = c.applicationStatus
      if (st === 'DEPT_PENDING') return { text: '待审核', tone: 'blue' }
      if (st === 'PASSED') return { text: '已通过', tone: 'green' }
      if (st === 'REJECTED') return { text: '已驳回', tone: 'red' }
      if (!c.gate) return { text: '—', tone: 'gray' }
      if (c.gate.eligible) return { text: '资格齐备', tone: 'green' }
      return { text: (c.gate.failCount || 0) + ' 项待补', tone: 'orange' }
    },
    dotOf(c) {
      const st = c.applicationStatus
      if (st === 'DEPT_PENDING') return 'b'
      if (st === 'PASSED') return 'g'
      if (!c.gate || !c.gate.eligible) return ''
      return 'o'
    },
    /** 右侧清单：把 gate.items 转成带 HTML 的展示结构 */
    buildChecklist(c) {
      const gate = c.gate
      if (!gate || !gate.items) return []
      return gate.items.map(item => ({
        key: item.key,
        pass: item.pass === true,
        text: '<b>' + item.label + '</b> · ' + (item.value || '—')
      }))
    },
    /** 右侧四维条：学习完成率 / 考核通过率 / 协议 / 资格满足度 */
    buildDimensions(c) {
      const out = []
      if (c.studyRate != null) {
        const v = Math.max(0, Math.min(100, Math.round(Number(c.studyRate))))
        out.push({ name: '学习完成率', value: v, tone: v >= 85 ? 'good' : (v >= 70 ? 'mid' : 'poor'), barTone: v >= 85 ? 'good' : (v >= 70 ? 'avg' : 'poor') })
      }
      const need = c.gate ? Number(c.gate.examPassTimes || 1) : 1
      const done = Number(c.passedExamTimes || 0)
      const examRate = need <= 0 ? 100 : Math.min(100, Math.round(done / need * 100))
      out.push({ name: '正式考核（要求 ' + need + ' 次）', value: examRate, tone: examRate >= 100 ? 'good' : (examRate >= 50 ? 'mid' : 'poor'), barTone: examRate >= 100 ? 'good' : (examRate >= 50 ? 'avg' : 'poor') })
      const protocol = c.protocolSigned ? 100 : 0
      out.push({ name: '保密协议', value: protocol, tone: protocol ? 'good' : 'poor', barTone: protocol ? 'good' : 'poor' })
      return out
    },
    /** 把接口行加工成模板要用的形状（只做一次，避免模板里塞逻辑） */
    decorate(row) {
      return Object.assign({}, row, {
        name: row.nickName || row.userName,
        checklist: this.buildChecklist(row),
        dimensions: this.buildDimensions(row),
        eligible: !!(row.gate && row.gate.eligible),
        ruleSourceText: (row.gate && row.gate.source) === 'DEPT' ? '本部门设置' : '系统默认'
      })
    },
    syncSelection() {
      const rows = this.listRows
      if (!rows.length) {
        this.currentUserId = null
        this.history = []
        return
      }
      if (!this.currentUserId || !rows.some(r => r.userId === this.currentUserId)) {
        this.currentUserId = rows[0].userId
      }
      this.loadHistory()
    },
    select(row) {
      this.currentUserId = row.userId
      this.loadHistory()
    },
    loadHistory() {
      const c = this.current
      if (!c || !c.applicationId) {
        this.history = []
        return
      }
      getPromotionHistory(c.applicationId).then(res => {
        this.history = res.data || []
      }).catch(() => {
        this.history = []
      })
    },
    fmtRate(v) {
      return v == null ? '暂无课程' : Number(v) + ' %'
    },
    fmtExam(c) {
      return (c.passedExamTimes || 0) + ' / ' + (c.examTimes || 0) + ' 场通过'
    },
    fmtTime(v) {
      if (!v) return '—'
      return String(v).replace('T', ' ').slice(0, 16)
    },
    flowState(step) {
      const st = this.current ? this.current.applicationStatus : null
      if (st === 'PASSED') return 'done'
      if (st === 'REJECTED') return step === 'SUBMIT' ? 'done' : ''
      if (st === 'DEPT_PENDING') return step === 'SUBMIT' ? 'done' : (step === 'PENDING' ? 'on' : '')
      return ''
    },
    actionText(action) {
      return {
        SUBMIT: '提交',
        RESUBMIT: '重新提交',
        WITHDRAW: '撤回',
        PASS: '通过',
        REJECT: '驳回',
        REVOKE: '撤回'
      }[action] || action || '—'
    },
    actionTone(action) {
      if (action === 'PASS') return 'green'
      if (action === 'REJECT') return 'red'
      if (action === 'REVOKE' || action === 'WITHDRAW') return 'orange'
      return 'blue'
    },
    approve() {
      const target = this.current
      if (!target || !target.applicationId) return
      this.$confirm(
        '确认将 ' + target.name + ' 转为正式实习生？通过后立即生效，并自动为其签发电子证书。',
        '确认审批通过',
        { confirmButtonText: '确认通过', cancelButtonText: '取消', type: 'warning' }
      ).then(() => {
        auditPromotion({ id: target.applicationId, action: 'PASS' }).then(res => {
          this.$modal.msgSuccess(res.msg || '审批通过，已转正并发证')
          this.loadCandidates()
        }).catch(() => {})
      }).catch(() => {})
    },
    reject() {
      const target = this.current
      if (!target || !target.applicationId) return
      this.$prompt('请填写驳回原因（会同步给实习生）', '驳回转正申请', {
        confirmButtonText: '确认驳回',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: '例如：学习完成率 66% 未达 70% 门槛，建议继续培养一周期。',
        inputValidator: value => (value && value.trim() ? true : '驳回原因不能为空')
      }).then(({ value }) => {
        auditPromotion({ id: target.applicationId, action: 'REJECT', reason: value }).then(res => {
          this.$modal.msgSuccess(res.msg || '已驳回')
          this.loadCandidates()
        }).catch(() => {})
      }).catch(() => {})
    },
    revoke() {
      const target = this.current
      if (!target || !target.applicationId) return
      this.$prompt('撤回转正后，该实习生将退回预备状态，已签发的证书同时作废。请填写撤回原因。', '撤回转正', {
        confirmButtonText: '确认撤回',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: '例如：体检结果未通过，需退回重新培养。',
        inputValidator: value => (value && value.trim() ? true : '撤回原因不能为空')
      }).then(({ value }) => {
        revokePromotion({ userId: target.userId, reason: value }).then(res => {
          this.$modal.msgSuccess(res.msg || '已撤回转正')
          this.loadCandidates()
        }).catch(() => {})
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

.dbtn-row { display: flex; justify-content: flex-end; align-items: center; gap: 10px; margin-top: 14px; }
code { padding: 1px 5px; color: #344054; font-size: 11.5px; background: #f2f4f7; border-radius: 4px; }
/* ---- 转正要求：醒目强调卡 ---- */
.promo-req {
  display: flex;
  align-items: center;
  gap: 20px 28px;
  flex-wrap: wrap;
  margin-bottom: 18px;
  padding: 16px 20px;
  background: linear-gradient(90deg, #eaf2ff 0%, #f6faff 52%, #fff 100%);
  border: 1px solid #d7e7fc;
  border-left: 4px solid #1764f5;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(23, 100, 245, .08);
}
.promo-req-hd { display: flex; align-items: center; gap: 12px; min-width: 300px; }
.promo-req-ico {
  display: inline-flex;
  width: 40px;
  height: 40px;
  flex: none;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
  background: #1764f5;
  border-radius: 11px;
  box-shadow: 0 3px 9px rgba(23, 100, 245, .3);
}
.promo-req-tt h3 { margin: 0 0 4px; color: #1d2939; font-size: 17px; font-weight: 600; letter-spacing: .2px; }
.promo-req-tt p { margin: 0; color: #667085; font-size: 12px; line-height: 1.5; }
.promo-req-tt p b { color: #1764f5; }
.promo-req-src { padding: 3px 11px; font-size: 11.5px; border-radius: 11px; }
.promo-req-src.dept { color: #1249c4; background: #dbe9ff; }
.promo-req-src.sys { color: #667085; background: #eef1f6; }
.promo-req-body {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: flex-end;
  gap: 24px 30px;
  flex-wrap: wrap;
}
.promo-req-item { display: flex; align-items: center; gap: 10px; }
.promo-req-label { color: #344054; font-size: 13.5px; font-weight: 600; }
.promo-req-item .unit { color: #98a2b3; font-size: 12.5px; }

@media (max-width: 1200px) {
  .promo-req-body { justify-content: flex-start; }
}
.dkv small { color: #98a2b3; font-size: 11px; font-weight: 400; }
.hint-text { color: #98a2b3; font-size: 11.5px; }
.dkpi-val small { font-size: 12px; font-weight: 400; }
.dtbl { width: 100%; border-collapse: collapse; }
.dtbl th { padding: 9px 10px; color: #8490a0; background: #f8fafc; font-size: 12px; font-weight: 500; text-align: left; }
.dtbl td { padding: 9px 10px; border-bottom: 1px solid #edf0f4; color: #475467; font-size: 12.5px; }
</style>
