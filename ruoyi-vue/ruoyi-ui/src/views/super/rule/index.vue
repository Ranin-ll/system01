<template>
  <div class="s-page">
    <header class="s-head">
      <div>
        <span class="eyebrow">SUPER ADMIN · RULES &amp; CONFIG</span>
        <h1>系统规则</h1>
        <p>
          超管在业务侧<b>唯一能写</b>的地方：考核规则默认值、证书编号规则、异常预警阈值、协议模板版本。
          改的是「<b>默认值</b>」，<b>不改已发布批次的冻结快照</b>。
        </p>
      </div>
      <div class="s-head-actions">
        <span class="s-ro" :class="canWrite ? 'rw' : 'sample'">
          <i :class="canWrite ? 'el-icon-edit-outline' : 'el-icon-lock'" /> {{ canWrite ? '可写' : '只读' }}
        </span>
        <span class="s-ro sample"><i class="el-icon-time" /> 改动留痕</span>
        <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="loadAll">刷新</el-button>
        <el-button size="small" type="primary" icon="el-icon-check" :loading="saving" :disabled="!canWrite || !changed" @click="save">保存</el-button>
      </div>
    </header>

    <div class="s-grid">
      <!-- ① 考核规则默认值 -->
      <section class="s-card s-c7">
        <div class="s-card-h">
          <div class="tt"><span class="s-idx">规</span><h3>考核规则（全局默认值）</h3></div>
          <span class="s-badge ok">真实数据 · 可写</span>
        </div>
        <el-form label-width="150px" size="small" v-loading="loading">
          <el-form-item label="学习门槛（必修完成率）">
            <el-input-number v-model="config.courseDoneThreshold" :min="0" :max="100" :step="5" :disabled="!canWrite" controls-position="right" style="width:130px" />
            <span class="unit">%</span>
          </el-form-item>
          <el-form-item label="理论权重 / 实操权重">
            <el-input-number v-model="config.theoryWeight" :min="0" :max="100" :step="5" :disabled="!canWrite" controls-position="right" style="width:130px" />
            <span class="unit">% /</span>
            <el-input-number v-model="config.practiceWeight" :min="0" :max="100" :step="5" :disabled="!canWrite" controls-position="right" style="width:130px;margin-left:8px" />
            <span class="unit">%</span>
            <span class="s-badge" :class="weightSum === 100 ? 'ok' : 'warn'" style="margin-left:8px">合计 {{ weightSum }}%</span>
          </el-form-item>
          <el-form-item label="理论通过线 / 实操通过线">
            <el-input-number v-model="config.theoryPassLine" :min="0" :max="100" :step="5" :disabled="!canWrite" controls-position="right" style="width:130px" />
            <span class="unit">分 /</span>
            <el-input-number v-model="config.practicePassLine" :min="0" :max="100" :step="5" :disabled="!canWrite" controls-position="right" style="width:130px;margin-left:8px" />
            <span class="unit">分</span>
          </el-form-item>
          <el-form-item label="补考次数">
            <el-input-number v-model="config.retakeCount" :min="0" :max="10" :disabled="!canWrite" controls-position="right" style="width:130px" />
            <span class="unit">次</span>
          </el-form-item>
          <el-form-item label="保密协议必签">
            <el-switch v-model="config.protocolRequired" :active-value="1" :inactive-value="0" :disabled="!canWrite" />
            <span class="unit" style="margin-left:10px">{{ config.protocolRequired === 1 ? '未签署不能进入系统' : '不强制签署（不推荐）' }}</span>
          </el-form-item>
        </el-form>
        <div class="s-callout">
          <i class="el-icon-info" />
          <span>
            <b>生效范围：</b>本页改的是<b>默认值</b>。已发布批次按各自冻结的规则执行，历史成绩不受影响。
            数据落点：<code>assessment_config</code>（单行，id=1）。
          </span>
        </div>
        <div class="s-callout warn" style="margin-top:8px">
          <i class="el-icon-warning-outline" />
          <span>
            <b>⚠️ 生效链尚未打通：</b>「发布批次时把默认值冻结进 <code>exam_rule_snapshot</code>」这一步属考核域，
            当前<b>全库无代码实现</b>（该表 0 行、零引用）。所以现在改默认值<b>只入库、还不会影响新批次</b> ——
            需与考核域分支对齐后再接线。
          </span>
        </div>
      </section>

      <!-- ⑤ 转正要求（按部门设置） -->
      <section class="s-card s-c7">
        <div class="s-card-h">
          <div class="tt"><span class="s-idx">转</span><h3>转正要求（按部门设置）</h3></div>
          <span class="s-badge ok">演示态 · localStorage</span>
        </div>
        <el-form label-width="150px" size="small">
          <el-form-item label="目标部门">
            <el-select v-model="promotionDeptId" placeholder="选择部门" clearable style="width:300px" @change="onPromotionDeptChange">
              <el-option :value="''" label="全局默认值（未单独设置的部门）" />
              <el-option v-for="d in depts" :key="d.deptId" :value="d.deptId" :label="d.deptName" />
            </el-select>
            <span class="s-badge blue" style="margin-left:8px">{{ promotionDeptId ? '按部门' : '全局默认' }}</span>
          </el-form-item>
          <el-form-item label="学习完成率门槛">
            <el-input-number v-model="promotionRule.studyRateMin" :min="0" :max="100" :step="5" controls-position="right" style="width:130px" />
            <span class="unit">%</span>
          </el-form-item>
          <el-form-item label="正式考核通过次数">
            <el-input-number v-model="promotionRule.examPassTimes" :min="1" :max="10" controls-position="right" style="width:130px" />
            <span class="unit">次</span>
          </el-form-item>
          <el-form-item>
            <el-button size="small" type="primary" icon="el-icon-check" :loading="promotionSaving" @click="savePromotionRule">
              保存{{ promotionDeptId ? '本部门' : '全局默认' }}转正要求
            </el-button>
          </el-form-item>
        </el-form>
        <table v-if="promotionOverrides.length" class="s-tbl" style="margin-top:8px">
          <thead><tr><th>部门</th><th style="width:140px">学习完成率门槛</th><th style="width:150px">正式考核通过</th><th style="width:130px">操作</th></tr></thead>
          <tbody>
            <tr v-for="o in promotionOverrides" :key="o.deptId">
              <td class="nm">{{ o.deptName }}</td>
              <td>{{ o.studyRateMin }}%</td>
              <td>{{ o.examPassTimes }} 次</td>
              <td><el-button type="text" size="mini" @click="removePromotionOverride(o)">删除（恢复全局）</el-button></td>
            </tr>
          </tbody>
        </table>
        <div v-else class="s-empty"><i class="el-icon-office-building" /><span>尚未对任何部门单独设置，全部沿用全局默认值</span></div>
        <p class="s-note">
          部门管理员也可在本部门「实习转正审核」页设置本部门规则，两处读写同一份
          <code>promotion-rule-{deptId}</code>；实习生端按所属部门读取，部门未单独设置时回退「全局默认值」。
        </p>
      </section>

      <!-- ② 协议模板与版本 -->
      <section class="s-card s-c5">
        <div class="s-card-h">
          <div class="tt"><span class="s-idx g">签</span><h3>协议模板与版本</h3></div>
          <span class="s-badge ok">真实数据</span>
          <el-button size="mini" type="primary" icon="el-icon-plus" style="margin-left:auto" :disabled="!canWrite" @click="openTplDialog">新增版本</el-button>
        </div>
        <table v-if="templates.length" class="s-tbl">
          <thead><tr><th>版本</th><th style="width:110px">生效时间</th><th style="width:70px">已签</th><th style="width:80px">状态</th></tr></thead>
          <tbody>
            <tr v-for="t in templates" :key="t.id">
              <td class="nm">{{ t.agreementName }} {{ t.versionNo }}</td>
              <td>{{ fmtDate(t.effectiveTime) }}</td>
              <td>{{ t.signCount || 0 }}</td>
              <td>
                <span class="s-badge" :class="t.status === 'EFFECTIVE' ? 'ok' : ''">{{ t.status === 'EFFECTIVE' ? '生效中' : '已归档' }}</span>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-else class="s-empty"><i class="el-icon-document" /><span>暂无协议模板</span></div>
        
      </section>

      <!-- ③ 证书模板 -->
      <section class="s-card s-c5">
        <div class="s-card-h">
          <div class="tt"><span class="s-idx p">证</span><h3>证书模板与编号规则</h3></div>
          <span class="s-badge ok">编号规则可写</span>
        </div>
        <div class="s-cert">
          <span class="fi">{{ config.certTemplateUrl ? '模板' : '未上传' }}</span>
          <div>
            <b>{{ config.certTemplateUrl || '尚未配置证书模板文件' }}</b>
            <small>占位符：姓名 / 岗位 / 综合分 / 证书编号 / 发证日期</small>
          </div>
        </div>
        <el-form label-width="110px" size="small" style="margin-top:12px">
          <el-form-item label="编号规则">
            <el-input v-model="config.certNoRule" :disabled="!canWrite" placeholder="如 DEV-{年}-{月日}-{序号}" />
          </el-form-item>
          <el-form-item label="模板文件地址">
            <el-input v-model="config.certTemplateUrl" :disabled="!canWrite" placeholder="相对路径，如 /profile/cert/template-a.pdf" />
          </el-form-item>
          <el-form-item label="签发人">
            <el-input value="部门管理员（终审即发证）" disabled />
          </el-form-item>
        </el-form>
        
      </section>

      <!-- ④ 异常预警规则 -->
      <section class="s-card s-c7">
        <div class="s-card-h">
          <div class="tt"><span class="s-idx o">警</span><h3>异常预警规则</h3></div>
          <span class="s-badge ok">阈值可写</span>
        </div>
        <table class="s-tbl">
          <thead><tr><th>规则</th><th style="width:190px">阈值</th><th style="width:110px">触发对象</th><th style="width:96px">状态</th></tr></thead>
          <tbody>
            <tr>
              <td class="nm">题库题量不足</td>
              <td>单场需求 ×
                <el-input-number v-model="config.alertBankGapFactor" :min="1" :max="20" size="mini" :disabled="!canWrite" controls-position="right" style="width:96px" />
              </td>
              <td>题库</td>
              <td><span class="s-badge ok">已生效</span></td>
            </tr>
            <tr>
              <td class="nm">考核长期未发布</td>
              <td>草稿超过
                <el-input-number v-model="config.alertExamDraftDays" :min="1" :max="365" size="mini" :disabled="!canWrite" controls-position="right" style="width:96px" />
                天
              </td>
              <td>考核批次</td>
              <td><span class="s-badge ok">已生效</span></td>
            </tr>
            <tr>
              <td class="nm">报名待审核堆积</td>
              <td>&gt;
                <el-input-number v-model="config.alertRegisterPending" :min="1" :max="999" size="mini" :disabled="!canWrite" controls-position="right" style="width:96px" />
                条
              </td>
              <td>部门</td>
              <td><span class="s-badge ok">已生效</span></td>
            </tr>
            <tr>
              <td class="nm">部门完成率偏低</td>
              <td>&lt;
                <el-input-number v-model="config.alertDeptDoneFloor" :min="0" :max="100" size="mini" :disabled="!canWrite" controls-position="right" style="width:96px" />
                %
              </td>
              <td>部门</td>
              <td><span class="s-badge warn">判据待做</span></td>
            </tr>
            <tr>
              <td class="nm">考核逾期未提交</td>
              <td>截止后
                <el-input-number v-model="config.alertOverdueHours" :min="1" :max="720" size="mini" :disabled="!canWrite" controls-position="right" style="width:96px" />
                小时
              </td>
              <td>实习生</td>
              <td><span class="s-badge warn">判据待做</span></td>
            </tr>
          </tbody>
        </table>
        
        <div class="s-grid" style="margin-top:6px">
          <div class="s-c6">
            <div class="s-entry" @click="go('/super/audit')">
              <i class="el-icon-document" />
              <div><b>去审计与合规看改动留痕</b><small>谁在何时改了哪条规则</small></div>
            </div>
          </div>
          <div class="s-c6">
            <div class="s-entry" @click="go('/system/config')">
              <i class="el-icon-setting" />
              <div><b>去系统管理 › 参数设置</b><small>系统级参数（平台原生）</small></div>
            </div>
          </div>
        </div>
      </section>
    </div>

    <!-- 新增协议版本 -->
    <el-dialog title="发布协议模板新版本" :visible.sync="tplDialog" width="620px" append-to-body>
      <el-form label-width="96px" size="small">
        <el-form-item label="协议名称"><el-input v-model="newTpl.agreementName" placeholder="如 保密协议" /></el-form-item>
        <el-form-item label="版本号"><el-input v-model="newTpl.versionNo" placeholder="如 v1.3" /></el-form-item>
        <el-form-item label="生效时间">
          <el-date-picker v-model="newTpl.effectiveTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="默认立即生效" style="width:100%" />
        </el-form-item>
        <el-form-item label="附件地址"><el-input v-model="newTpl.fileUrl" placeholder="签字版扫描件相对路径（可选）" /></el-form-item>
        <el-form-item label="协议正文"><el-input v-model="newTpl.content" type="textarea" :rows="6" placeholder="正文内容（与附件地址至少填一个）" /></el-form-item>
      </el-form>
      <div class="s-callout warn" style="margin:0">
        <i class="el-icon-warning-outline" />
        <span>发布后<b>当前生效版本会自动归档</b>，此后新签署的实习生签的是新版本；历史签署记录保持原版本不变。</span>
      </div>
      <span slot="footer">
        <el-button size="small" @click="tplDialog = false">取消</el-button>
        <el-button size="small" type="primary" :loading="publishing" @click="publishTemplate">确认发布</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
/**
 * 超管「规则与配置」页
 *
 * 数据落点（2026-09-17 落地）：
 *  - 考核规则默认值 / 证书编号规则 / 异常预警阈值 → `assessment_config`（单行 id=1，新建）
 *  - 协议模板 → `agreement_template`（原有表，后端注册流程在用）
 *
 * 写权限：仅超管（含内置 admin）；后端 @PreAuthorize 兜底，前端 disabled 只是界面优化。
 * 每次保存都在后端写 audit_record 留痕。
 *
 * ⚠️ 生效链未打通：「发布批次时冻结进 exam_rule_snapshot」属考核域，当前全库无实现，
 *    因此本页改的是「默认值」，尚未影响新发布批次 —— 页面已如实标注。
 */
import { getRuleConfig, updateRuleConfig, listAgreementTemplates, publishAgreementTemplate } from '@/api/business/rule'
import { listDept } from '@/api/system/dept'

export default {
  name: 'SuperRules',
  data() {
    return {
      loading: false,
      saving: false,
      publishing: false,
      config: {},
      original: {},
      templates: [],
      tplDialog: false,
      newTpl: { agreementName: '保密协议', versionNo: '', effectiveTime: '', fileUrl: '', content: '' },
      // 转正要求（按部门设置，演示态 localStorage）
      depts: [],
      promotionDeptId: '',
      promotionRule: { studyRateMin: 0, examPassTimes: 1 },
      promotionOverrides: [],
      promotionSaving: false
    }
  },
  computed: {
    roles() {
      return this.$store.getters.roles || []
    },
    canWrite() {
      return this.roles.indexOf('SUPER_ADMIN') > -1 || this.roles.indexOf('admin') > -1
    },
    weightSum() {
      return Number(this.config.theoryWeight || 0) + Number(this.config.practiceWeight || 0)
    },
    changed() {
      return JSON.stringify(this.config) !== JSON.stringify(this.original)
    }
  },
  created() {
    this.loadAll()
  },
  methods: {
    loadAll() {
      this.loading = true
      this.loadDepts()
      Promise.all([
        getRuleConfig().then(res => {
          const d = res.data || {}
          // 数值字段统一成 Number，避免 el-input-number 收到字符串
          ;['courseDoneThreshold', 'theoryWeight', 'practiceWeight', 'theoryPassLine', 'practicePassLine', 'alertDeptDoneFloor']
            .forEach(k => { if (d[k] !== null && d[k] !== undefined) d[k] = Number(d[k]) })
          this.config = d
          this.original = JSON.parse(JSON.stringify(d))
        }).catch(() => { this.config = {}; this.original = {} }),
        listAgreementTemplates().then(res => { this.templates = res.data || [] }).catch(() => { this.templates = [] })
      ]).finally(() => { this.loading = false })
    },
    save() {
      if (this.weightSum !== 100) {
        this.$message.warning('理论权重与实操权重之和必须等于 100')
        return
      }
      this.saving = true
      updateRuleConfig(this.config).then(res => {
        this.$message.success(res.msg || '已保存')
        this.loadAll()
      }).catch(() => {}).finally(() => { this.saving = false })
    },
    loadDepts() {
      return listDept({ status: '0' }).then(res => {
        this.depts = (res.data || []).filter(d => d.parentId !== 0)
        this.rebuildPromotionOverrides()
        this.onPromotionDeptChange()
      }).catch(() => {
        this.depts = []
        this.rebuildPromotionOverrides()
        this.onPromotionDeptChange()
      })
    },
    rebuildPromotionOverrides() {
      this.promotionOverrides = this.depts.map(d => {
        try {
          const raw = localStorage.getItem('promotion-rule-' + d.deptId)
          if (raw) {
            const c = JSON.parse(raw)
            return {
              deptId: d.deptId,
              deptName: d.deptName,
              studyRateMin: Number(c.studyRateMin != null ? c.studyRateMin : 0),
              examPassTimes: Number(c.examPassTimes != null ? c.examPassTimes : 1)
            }
          }
        } catch (e) { /* 忽略解析失败 */ }
        return null
      }).filter(Boolean)
    },
    promotionRuleKey() {
      return this.promotionDeptId ? ('promotion-rule-' + this.promotionDeptId) : 'promotion-rule'
    },
    onPromotionDeptChange() {
      const key = this.promotionRuleKey()
      try {
        const raw = localStorage.getItem(key)
        if (raw) {
          const c = JSON.parse(raw)
          this.promotionRule = {
            studyRateMin: Number(c.studyRateMin != null ? c.studyRateMin : 0),
            examPassTimes: Number(c.examPassTimes != null ? c.examPassTimes : 1)
          }
          return
        }
      } catch (e) { /* 忽略 */ }
      this.promotionRule = { studyRateMin: 0, examPassTimes: 1 }
    },
    savePromotionRule() {
      const key = this.promotionRuleKey()
      this.promotionSaving = true
      try {
        localStorage.setItem(key, JSON.stringify(this.promotionRule))
        this.$message.success('已保存' + (this.promotionDeptId ? '本部门' : '全局默认') + '转正要求')
        this.rebuildPromotionOverrides()
      } catch (e) {
        this.$message.error('保存失败：本地存储不可用')
      }
      this.promotionSaving = false
    },
    removePromotionOverride(o) {
      try {
        localStorage.removeItem('promotion-rule-' + o.deptId)
        this.rebuildPromotionOverrides()
        this.$message.success('已恢复「' + o.deptName + '」为全局默认值')
      } catch (e) {
        this.$message.error('删除失败')
      }
    },
    openTplDialog() {
      this.newTpl = { agreementName: '保密协议', versionNo: '', effectiveTime: '', fileUrl: '', content: '' }
      this.tplDialog = true
    },
    publishTemplate() {
      if (!this.newTpl.versionNo || (!this.newTpl.content && !this.newTpl.fileUrl)) {
        this.$message.warning('版本号必填，且正文与附件地址至少填一个')
        return
      }
      this.publishing = true
      publishAgreementTemplate(this.newTpl).then(res => {
        this.$message.success(res.msg || '已发布')
        this.tplDialog = false
        this.loadAll()
      }).catch(() => {}).finally(() => { this.publishing = false })
    },
    fmtDate(v) {
      if (!v) return '—'
      return String(v).replace('T', ' ').slice(0, 10)
    },
    go(path) {
      this.$router.push(path)
    }
  }
}
</script>

<style lang="scss" scoped>
@import '~@/assets/styles/super-module.scss';
</style>
