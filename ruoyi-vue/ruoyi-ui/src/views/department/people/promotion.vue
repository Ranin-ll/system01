<template>
  <div class="dept-page">
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
        <span class="dsample">演示数据 · promotion_application 待后端</span>
      </div>
    </div>

    <div class="dcallout ok">
      <i class="el-icon-success" />
      <span>
        <b>已按既定决策改写：部门管理员直接终审。</b>本页<b>不再有「推荐给超管」这一步</b>，
        按钮语义从「推荐」改为「审批通过」，审批即时生效并自动发证。
      </span>
    </div>

    <!-- 转正要求设置（部门管理员） -->
    <div class="dsec" style="padding:16px 22px">
      <div class="s-setrow">
        <div class="s-setrow-hd">
          <span class="s-setrow-label">转正要求设置</span>
          <span class="hint-text">设置即时生效；实习生端「转正申请」资格清单以本设置为准</span>
        </div>
        <div class="s-setrow-fields">
          <span>学习完成率</span>
          <el-input-number v-model="promotionRule.studyRateMin" :min="0" :max="100" :step="5" size="mini" controls-position="right" style="width:110px" @change="onRuleChange" />
          <span class="unit">%</span>
          <span style="margin-left:24px">正式考核通过</span>
          <el-input-number v-model="promotionRule.examPassTimes" :min="1" :max="10" size="mini" controls-position="right" style="width:110px" @change="onRuleChange" />
          <span class="unit">次</span>
          <el-tag size="mini" type="warning" effect="plain" style="margin-left:12px">演示态</el-tag>
        </div>
      </div>
      <p class="dsec-note">学习完成率门槛最低可设为 0（不设学习门槛）；正式考核通过次数默认为 1 次。<b>后端 promotion_rule 接入后由服务端持久化，当前存本地演示。</b></p>
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
            <span class="hint-text">提交时间 {{ current.submittedAt }}</span>
          </div>

          <div class="dcallout" :class="current.verdict.tone === 'green' ? 'ok' : (current.verdict.tone === 'red' ? 'warn' : 'warn')" style="margin-bottom:12px">
            <i class="el-icon-info" />
            <span>
              <b>资格核对清单</b> · {{ checklistSummary }}（<b>部门终审即生效</b>）
            </span>
          </div>

          <div class="dfg2" style="margin-bottom:14px">
            <div v-for="item in current.checklist" :key="item.key" class="dchk">
              <span class="box" :class="item.pass ? 'sw' : 'fail'">
                <i :class="item.pass ? 'el-icon-check' : 'el-icon-close'" />
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
                <strong :style="{ color: current.passed ? '#067647' : '#b42318' }">
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
            </div>
          </div>

          <div class="dfield">
            <label>部门管理员评价 · 优势</label>
            <div class="dinp">{{ current.good }}</div>
          </div>
          <div class="dfield">
            <label>待提升 / 改进建议</label>
            <div class="dinp">{{ current.improve }}</div>
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
import { mapGetters } from 'vuex'

const PASS_LINE = 70

/** 部门管理员直接终审 → 演示态数据（promotion_application 尚无 Java 层） */
function buildCandidates(cfg) {
  const studyRateMin = Number(cfg && cfg.studyRateMin != null ? cfg.studyRateMin : 0)
  const examPassTimes = Number(cfg && cfg.examPassTimes != null ? cfg.examPassTimes : 1)
  const raw = [
    {
      id: 1, name: '陈子轩', position: '开发实习生', userId: 1041,
      studyRate: 91, theory: 88, practice: 85, protocol: true, signedAt: '2026-06-15 13:20',
      passedExams: 1, submittedAt: '2026-09-16 15:02',
      dim: [['学习投入', 91], ['理论掌握', 88], ['实践能力', 85], ['规范遵从', 82]],
      good: '代码规范意识强，Spring Boot 掌握扎实，能独立完成模块联调。',
      improve: 'Docker 部署环节偏弱（该知识点错误率 42%），建议转正后继续补。整体建议予以转正。'
    },
    {
      id: 2, name: '林知遥', position: '开发实习生', userId: 1042,
      studyRate: 78, theory: 74, practice: 76, protocol: true, signedAt: '2026-06-20 09:05',
      passedExams: 1, submittedAt: '2026-09-16 11:38',
      dim: [['学习投入', 78], ['理论掌握', 74], ['实践能力', 76], ['规范遵从', 80]],
      good: '学习节奏稳定，前端基础扎实，接口联调配合度高。',
      improve: '学习完成率 78% 已过线，理论/实操均通过，整体建议予以转正。'
    },
    {
      id: 3, name: '吴柏舟', position: '开发实习生', userId: 1043,
      studyRate: 66, theory: 72, practice: 68, protocol: true, signedAt: '2026-06-25 15:40',
      passedExams: 0, submittedAt: '2026-09-15 17:20',
      dim: [['学习投入', 66], ['理论掌握', 72], ['实践能力', 68], ['规范遵从', 70]],
      good: '动手意愿强，能主动承担联调工作。',
      improve: '正式考核尚未通过（实操 68 分未过线），建议继续培养一周期后再提交。'
    },
    {
      id: 4, name: '孙悦', position: '开发实习生', userId: 1044,
      studyRate: 100, theory: 93, practice: 90, protocol: true, signedAt: '2026-06-10 10:12',
      passedExams: 2, submittedAt: '2026-09-10 09:30',
      status: 'PASSED', decidedAt: '2026-09-10 14:05',
      dim: [['学习投入', 100], ['理论掌握', 93], ['实践能力', 90], ['规范遵从', 88]],
      good: '全科通过，学习完成率 100%，可作为组内样板。',
      improve: '进展良好，无特别短板。'
    },
    {
      id: 5, name: '周霖', position: '开发实习生', userId: 1045,
      studyRate: 23, theory: 51, practice: 40, protocol: true, signedAt: '2026-06-28 16:00',
      passedExams: 0, submittedAt: '2026-08-28 10:00',
      status: 'REJECTED', decidedAt: '2026-08-29 11:20',
      rejectReason: '学习完成率与实操能力均明显不足，建议延长培养期后再评估。',
      dim: [['学习投入', 23], ['理论掌握', 51], ['实践能力', 40], ['规范遵从', 58]],
      good: '无明显优势项。',
      improve: '多项指标未达门槛，本轮不予转正。'
    }
  ]

  return raw.map(item => {
    const total = Math.round((item.theory * 0.6 + item.practice * 0.4) * 10) / 10
    const passed = total >= PASS_LINE && item.studyRate >= studyRateMin && item.passedExams >= examPassTimes
    const checklist = [
      { key: 'study', pass: item.studyRate >= studyRateMin, text: '学习完成率 <b>' + item.studyRate + '%</b> ≥ 门槛 ' + studyRateMin + '%' },
      { key: 'exam', pass: item.passedExams >= examPassTimes, text: '正式考核已通过 <b>' + item.passedExams + '</b> 次（要求 ≥ ' + examPassTimes + ' 次）' },
      { key: 'protocol', pass: item.protocol, text: item.protocol ? '保密协议已签署（' + item.signedAt + '）' : '保密协议<b>未签署</b>' }
    ]
    const failCount = checklist.filter(c => !c.pass).length
    let verdict
    if (failCount === 0) verdict = { text: '资格齐备', tone: 'green', dot: 'g' }
    else if (!passed) verdict = { text: '未达门槛', tone: 'red', dot: '' }
    else verdict = { text: failCount + ' 项待补', tone: 'orange', dot: 'o' }
    return Object.assign({}, item, {
      status: item.status || 'PENDING',
      total,
      passed,
      checklist,
      verdict,
      summary: '完成率 ' + item.studyRate + '% · 通过 ' + item.passedExams + ' 次考核 · ' + (item.protocol ? '协议已签' : '协议未签'),
      dimensions: item.dim.map(([name, value]) => ({
        name,
        value,
        tone: value >= 85 ? 'good' : (value >= 70 ? 'mid' : 'poor'),
        barTone: value >= 85 ? 'good' : (value >= 70 ? 'avg' : 'poor')
      }))
    })
  })
}

export default {
  name: 'DeptPromotion',
  data() {
    return {
      passLine: PASS_LINE,
      activeStatus: 'PENDING',
      positionFilter: '',
      // 转正要求（部门管理员设置；后端 promotion_rule 就绪前本地演示）
      promotionRule: { studyRateMin: 0, examPassTimes: 1 },
      candidates: [],
      currentId: null
    }
  },
  computed: {
    ...mapGetters(['deptId']),
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
        summary: c.status === 'PENDING' ? c.summary : ('处理时间 ' + (c.decidedAt || '—')),
        verdict: c.verdict,
        dotTone: c.verdict.dot
      }))
    },
    current() {
      return this.candidates.find(c => c.id === this.currentId) || null
    },
    checklistSummary() {
      if (!this.current) return ''
      const fail = this.current.checklist.filter(c => !c.pass).length
      const total = this.current.checklist.length
      return fail === 0 ? total + ' 项全部通过，可直接审批转正' : fail + ' 项未通过，需补齐后再提交'
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
    this.loadRule()
    const fromQuery = Number(this.$route.query.id)
    if (fromQuery) {
      const hit = this.candidates.find(c => c.userId === fromQuery || c.id === fromQuery)
      if (hit) {
        this.activeStatus = hit.status
        this.currentId = hit.id
        return
      }
    }
    this.syncSelection()
  },
  methods: {
    /** 读取本部门转正要求（后端 promotion_rule 就绪前用本地演示） */
    loadRule() {
      const load = function (key) {
        try {
          var saved = localStorage.getItem(key)
          if (saved) {
            var c = JSON.parse(saved)
            return {
              studyRateMin: Number(c.studyRateMin != null ? c.studyRateMin : 0),
              examPassTimes: Number(c.examPassTimes != null ? c.examPassTimes : 1)
            }
          }
        } catch (e) { /* 忽略 */ }
        return null
      }
      // 优先本部门规则，其次全局默认，最后内置默认
      var deptKey = this.deptId ? ('promotion-rule-' + this.deptId) : null
      var deptRule = deptKey ? load(deptKey) : null
      var globalRule = load('promotion-rule')
      this.promotionRule = deptRule || globalRule || { studyRateMin: 0, examPassTimes: 1 }
      this.candidates = buildCandidates(this.promotionRule)
    },
    /** 部门管理员调整转正要求后即时落库（本地演示，写本部门键）并刷新列表判定 */
    onRuleChange() {
      try {
        var key = this.deptId ? ('promotion-rule-' + this.deptId) : 'promotion-rule'
        localStorage.setItem(key, JSON.stringify(this.promotionRule))
      } catch (e) {
        // 本地存储不可用时仍继续
      }
      this.candidates = buildCandidates(this.promotionRule)
      this.syncSelection()
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
        target.status = 'PASSED'
        target.decidedAt = this.now()
        target.verdict = { text: '资格齐备', tone: 'green', dot: 'g' }
        this.$message.success('已通过「' + target.name + '」的转正审批（演示态：promotion_application 接口待补）')
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
      }).then(({ value }) => {
        target.status = 'REJECTED'
        target.decidedAt = this.now()
        target.rejectReason = value
        target.verdict = { text: '已驳回', tone: 'gray', dot: '' }
        this.$message.warning('已驳回「' + target.name + '」的转正申请（演示态）')
      }).catch(() => {})
    },
    now() {
      const d = new Date()
      const p = n => (n < 10 ? '0' + n : '' + n)
      return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + ' ' + p(d.getHours()) + ':' + p(d.getMinutes())
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
/* 转正要求设置行 */
.s-setrow { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
.s-setrow-hd { display: flex; flex-direction: column; gap: 4px; }
.s-setrow-label { color: #475467; font-size: 13px; font-weight: 600; }
.s-setrow-fields { display: flex; align-items: center; gap: 8px; color: #475467; font-size: 12.5px; }
.s-setrow-fields .unit { color: #98a2b3; }
.dkv small { color: #98a2b3; font-size: 11px; font-weight: 400; }
.hint-text { color: #98a2b3; font-size: 11.5px; }
.dkpi-val small { font-size: 12px; font-weight: 400; }
</style>
