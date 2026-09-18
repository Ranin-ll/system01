<template>
  <div class="s-page">
    <header class="s-head">
      <div>
        <span class="eyebrow">SUPER ADMIN · ORGANIZATION</span>
        <h1>组织岗位</h1>
        <p>部门 · 岗位 · 人数的全局对应关系。超管在此<b>只读核对</b>；要增改部门或岗位，请到「系统管理 › 部门管理 / 岗位管理」操作（超管可写）。</p>
      </div>
      <div class="s-head-actions">
        <span class="s-ro"><i class="el-icon-view" /> 只读核对</span>
        <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="loadAll">刷新</el-button>
      </div>
    </header>

    <div class="s-grid">
      <section class="s-card s-c8">
        <div class="s-card-h">
          <div class="tt"><span class="s-idx">组</span><h3>部门 · 绑定岗位 · 在培人数</h3></div>
          <span class="hint">真实数据：sys_dept + position + dept_position + sys_user + register_application</span>
        </div>
        <div v-loading="loading">
          <table v-if="rows.length" class="s-tbl">
            <thead>
              <tr><th>部门</th><th style="width:140px">绑定岗位</th><th style="width:110px">在培人数</th><th style="width:120px">待审核</th><th style="width:96px">状态</th></tr>
            </thead>
            <tbody>
              <tr v-for="r in rows" :key="r.deptId">
                <td class="nm">{{ r.deptName }}</td>
                <td>{{ r.positionName || '未绑定' }}</td>
                <td class="num">{{ r.training === null ? '—' : r.training }}</td>
                <td>{{ r.waiting }}</td>
                <td>
                  <span class="s-badge" :class="r.positionName ? 'ok' : 'warn'" :title="'绑定来源：' + r.sourceLabel">{{ r.positionName ? '已绑定' : '待配置' }}</span>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-else class="s-empty"><i class="el-icon-office-building" /><span>暂无部门数据</span></div>
        </div>
        <p class="s-note">
          部门取 <code>sys_dept</code> 中公司根节点（<code>parentId = 0</code>，即「融谷」）下的 5 个培养部门。
          <b>绑定岗位 · 在培人数 · 待审核</b> 均取自接口 <code>/business/position/dept-bindings</code>：
          绑定以权威表 <code>dept_position</code> 为准（后端课程可见性用的就是它），两个人数由该接口用 SQL 直接计算
          （在培 = <code>user_status</code> 处于预备 / 正式 / 待转正；待审核 = <code>register_application.status</code> 为 WAIT_AUDIT）。
          取不到时退回报名记录，此时「在培人数」显示 <b>—</b> 而非 0。悬停状态徽标可看本条实际来源。
        </p>
      </section>

      <section class="s-card s-c4">
        <div class="s-card-h">
          <div class="tt"><span class="s-idx g">岗</span><h3>岗位清单</h3></div>
          <span class="hint">{{ positions.length }} 个</span>
        </div>
        <table v-if="positions.length" class="s-tbl">
          <thead><tr><th>岗位</th><th style="width:86px">编码</th></tr></thead>
          <tbody>
            <tr v-for="p in positions" :key="p.id">
              <td class="nm">{{ p.positionName }}</td>
              <td>{{ p.positionCode }}</td>
            </tr>
          </tbody>
        </table>
        <div v-else class="s-empty"><i class="el-icon-s-custom" /><span>暂无岗位</span></div>
        <div class="s-callout" style="margin-top:12px">
          <i class="el-icon-info" />
          <span>岗位决定<b>课程可见性</b>与<b>考核配置范围</b>：课程 / 题库 / 考核都按 <code>position_id</code> 归属，实习生注册时由「部门 → 岗位」推导得出。</span>
        </div>
      </section>

      <section class="s-card s-c12">
        <div class="s-card-h">
          <div class="tt"><span class="s-idx p">规</span><h3>绑定规则与边界</h3></div>
        </div>
        <div class="s-grid">
          <div class="s-c6">
            <div class="s-steps">
              <div class="s-step done"><span class="mark">✓</span><div class="txt"><b>一个部门对应一个岗位</b><span>由 <code>dept_position</code> 维护，当前实现为一对一</span></div></div>
              <div class="s-step done"><span class="mark">✓</span><div class="txt"><b>岗位决定培养内容范围</b><span>课程、题库、模拟与正式考核均按岗位归属</span></div></div>
              <div class="s-step now"><span class="mark">!</span><div class="txt"><b>暂不支持一部门多岗位</b><span>如「交付部门同时培养实施 / 实施运维」需放开唯一约束并改造注册流程</span></div></div>
            </div>
          </div>
          <div class="s-c6">
            <div class="s-callout warn">
              <i class="el-icon-warning-outline" />
              <span>
                <b>结构变更提示：</b>若要支持「一个部门多岗位」，需同时改三处 ——
                ① <code>dept_position</code> 放开唯一约束；② 注册页出现岗位下拉（由部门过滤候选）；
                ③ 各业务的岗位归属改为「部门 + 岗位」双条件。属结构性改动，建议单独排期。
              </span>
            </div>
            <div class="s-callout ok" style="margin-top:10px">
              <i class="el-icon-success" />
              <span>当前 5 个部门与 5 个岗位均为<b>一一绑定且已配置</b>，无需额外维护。</span>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script>
import { listDept } from '@/api/system/dept'
import { listPosition, getDeptBindings } from '@/api/business/position'
import { listRegister } from '@/api/business/register'

/** 绑定来源的可读名（用于状态徽标的 title 提示） */
const SOURCE_LABEL = {
  dept_position: 'dept_position（配置表 · 权威，人数由后端 SQL 计算）',
  register: 'register_application（报名记录 · 仅退路）',
  none: '无来源'
}

/**
 * 超管「组织岗位」页（只读核对）
 *
 * 真实数据：/system/dept/list、/business/position/list、
 *          /business/position/dept-bindings（部门↔岗位绑定 + 在培/待审核人数）、
 *          /business/register/list（退路）
 *
 * 推导规则（2026-09-17 三轮修正，每轮都踩了一个坑）：
 *   ① 绑定岗位：以权威表 `dept_position` 为准（接口 dept-bindings），拿不到时退回报名记录。
 *      —— 旧实现只看报名记录，遇到「账号直接创建、未走注册审核」的部门会显示未绑定（建模 107）。
 *   ② 在培人数 / 待审核：**由后端 SQL 直接算**。
 *      —— 曾试图用 `/system/user/list` 在前端数，但该接口虽然返回 `userStatus` 字段、值却是 null
 *         （RuoYi 的 selectUserList 没 select 这个业务扩展列，也不返回 positionId），结果五行全是 0。
 *   ③ 取不到时显示 `—`，**不伪装成 0**（沿用项目既有口径）。
 */
export default {
  name: 'SuperOrganization',
  data() {
    return {
      loading: false,
      depts: [],
      positions: [],
      interns: [],
      bindings: []
    }
  },
  computed: {
    /** 公司根节点：sys_dept 里 parentId=0 的那一行（当前为「融谷」） */
    companyRootId() {
      const root = this.depts.find(d => Number(d.parentId) === 0)
      return root ? Number(root.deptId) : 100
    },
    /** 培养部门 = 公司根节点的直属子部门（交付 / 开发 / 设计 / 质检 / 建模） */
    trainDepts() {
      const root = this.companyRootId
      return this.depts
        .filter(d => Number(d.parentId) === root && Number(d.deptId) !== root)
        .sort((a, b) => a.deptId - b.deptId)
    },
    rows() {
      return this.trainDepts.map(d => {
        const deptId = Number(d.deptId)
        const mine = this.interns.filter(r => Number(r.deptId) === deptId)
        // ① 权威：dept-bindings（绑定岗位 + 后端 SQL 算好的在培/待审核人数）
        // ② 退路：报名记录 —— 只能给出「待审核」，在培人数无从得知 → null（模板渲染成 —）
        const bound = this.bindings.find(b => Number(b.deptId) === deptId)
        const positionId = bound ? bound.positionId : (mine.length ? mine[0].positionId : null)
        const position = this.positions.find(p => Number(p.id) === Number(positionId))
        return {
          deptId: d.deptId,
          deptName: d.deptName,
          positionName: (bound && bound.positionName) || (position ? position.positionName : ''),
          sourceLabel: bound ? SOURCE_LABEL.dept_position : (mine.length ? SOURCE_LABEL.register : SOURCE_LABEL.none),
          training: bound ? Number(bound.trainingCount) : null,
          waiting: bound ? Number(bound.waitAuditCount) : mine.filter(r => r.status === 'WAIT_AUDIT').length
        }
      })
    }
  },
  created() {
    this.loadAll()
  },
  methods: {
    loadAll() {
      this.loading = true
      Promise.all([
        listDept({ status: '0' }).then(res => { this.depts = res.data || [] }).catch(() => { this.depts = [] }),
        listPosition({ pageNum: 1, pageSize: 100 }).then(res => { this.positions = res.rows || [] }).catch(() => { this.positions = [] }),
        getDeptBindings().then(res => { this.bindings = res.data || [] }).catch(() => { this.bindings = [] }),
        listRegister({ pageNum: 1, pageSize: 500 }).then(res => { this.interns = res.rows || [] }).catch(() => { this.interns = [] })
      ]).finally(() => { this.loading = false })
    }
  }
}
</script>

<style lang="scss" scoped>
@import '~@/assets/styles/super-module.scss';
</style>
