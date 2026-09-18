<template>
  <div class="s-page">
    <header class="s-head">
      <div>
        <span class="eyebrow">SUPER ADMIN · ROLES &amp; PERMISSIONS</span>
        <h1>角色权限</h1>
        <p>四类业务角色 + 超管的权限边界与「能不能写」的验收基准。角色本身的增删改请到「系统管理 › 角色管理」（超管可写）。</p>
      </div>
      <div class="s-head-actions">
        <span class="s-ro"><i class="el-icon-view" /> 只读核对</span>
        <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="loadRoles">刷新角色</el-button>
      </div>
    </header>

    <div class="s-grid">
      <section class="s-card s-c4">
        <div class="s-card-h"><div class="tt"><span class="s-idx">角</span><h3>系统角色</h3></div><span class="hint">sys_role</span></div>
        <table v-if="roles.length" class="s-tbl">
          <thead><tr><th>角色</th><th style="width:110px">标识</th><th style="width:80px">成员</th></tr></thead>
          <tbody>
            <tr v-for="r in roles" :key="r.roleId">
              <td class="nm">{{ r.roleName }}</td>
              <td><code>{{ r.roleKey }}</code></td>
              <td class="num">{{ r.userCount != null ? r.userCount : '—' }}</td>
            </tr>
          </tbody>
        </table>
        <div v-else class="s-empty"><i class="el-icon-lock" /><span>暂无角色数据</span></div>
        <p class="s-note">业务侧 4 个角色由后端 <code>@PreAuthorize</code> 强校验：<code>SUPER_ADMIN</code> / <code>DEPT_ADMIN</code> / <code>PRE_TRAINEE</code> / <code>FORMAL_TRAINEE</code>；前端隐藏按钮只是界面优化。</p>
      </section>

      <section class="s-card s-c8">
        <div class="s-card-h">
          <div class="tt"><span class="s-idx p">矩</span><h3>能力 × 角色 矩阵</h3></div>
          <span class="s-badge purple">★ = 超管独有</span>
        </div>
        <table class="s-tbl">
          <thead>
            <tr><th style="width:200px">能力</th><th style="width:104px">超管</th><th style="width:120px">部门管理员</th><th style="width:104px">预备实习生</th><th style="width:104px">正式实习生</th></tr>
          </thead>
          <tbody>
            <tr v-for="row in matrix" :key="row.name">
              <td class="nm">{{ row.name }}</td>
              <td :class="cellClass(row.super)">{{ row.super }}</td>
              <td>{{ row.dept }}</td>
              <td>{{ row.pre }}</td>
              <td>{{ row.formal }}</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section class="s-card s-c6">
        <div class="s-card-h"><div class="tt"><span class="s-idx g">写</span><h3>超管可写范围（系统侧 + 规则）</h3></div></div>
        <div class="s-steps">
          <div class="s-step done"><span class="mark">✓</span><div class="txt"><b>账号与角色</b><span>创建部门管理员账号、分配角色、重置密码</span></div></div>
          <div class="s-step done"><span class="mark">✓</span><div class="txt"><b>菜单 / 部门 / 岗位 / 字典 / 参数 / 公告</b><span>平台原生系统管理 9 项</span></div></div>
          <div class="s-step done"><span class="mark">✓</span><div class="txt"><b>考核规则默认值</b><span>门槛 / 权重 / 补考次数（不影响已发布批次的冻结快照）</span></div></div>
          <div class="s-step done"><span class="mark">✓</span><div class="txt"><b>协议 / 证书模板</b><span>版本化，只对新签人生效</span></div></div>
          <div class="s-step done"><span class="mark">✓</span><div class="txt"><b>转正纠错（唯一业务写入口）</b><span>撤回转正需二次确认 + 必填原因 + 留痕</span></div></div>
        </div>
      </section>

      <section class="s-card s-c6">
        <div class="s-card-h"><div class="tt"><span class="s-idx o">界</span><h3>超管只读范围（业务实例）</h3></div><span class="s-badge ok">后端已强制</span></div>
        <div class="s-steps">
          <div class="s-step now"><span class="mark">✕</span><div class="txt"><b>课程与课程内容</b><span><code>CourseServiceImpl</code> / <code>CourseContentServiceImpl</code> 命中即抛「仅可查看」</span></div></div>
          <div class="s-step now"><span class="mark">✕</span><div class="txt"><b>题库与题目</b><span><code>QuestionBankServiceImpl</code> / <code>QuestionServiceImpl</code></span></div></div>
          <div class="s-step now"><span class="mark">✕</span><div class="txt"><b>考核配置与发布</b><span><code>ExamServiceImpl</code> 拦截写入</span></div></div>
          <div class="s-step now"><span class="mark">✕</span><div class="txt"><b>实操题库</b><span><code>PracticeSubjectServiceImpl</code></span></div></div>
          <div class="s-step now"><span class="mark">✕</span><div class="txt"><b>注册审核 / 批阅 / 转正审批</b><span>页面只读，动作按钮不渲染</span></div></div>
        </div>
        <div class="s-callout" style="margin-top:12px">
          <i class="el-icon-info" />
          <span>刻意如此设计：<b>业务数据的唯一生产者是部门管理员</b>。超管若代写，会与该部门的培养记录互相覆盖，破坏「谁培养、谁负责」的追溯链。</span>
        </div>
      </section>
    </div>
  </div>
</template>

<script>
import { listRole } from '@/api/system/role'

/**
 * 超管「角色权限」页
 * 真实数据：/system/role/list（角色清单）
 * 静态内容：能力 × 角色矩阵（设计稿基准，用于验收）
 */
export default {
  name: 'SuperRoles',
  data() {
    return {
      loading: false,
      roles: [],
      matrix: [
        { name: '注册审核', super: '只读（全局）', dept: '可写（本部门）', pre: '—', formal: '—' },
        { name: '课程与课程内容', super: '只读（全局）', dept: '可写（本部门）', pre: '只读（已发布）', formal: '只读（历史）' },
        { name: '题库与题目', super: '只读（全局）', dept: '可写（本部门）', pre: '—', formal: '—' },
        { name: '模拟考核配置', super: '只读', dept: '可写', pre: '只做题', formal: '—' },
        { name: '正式考核（发布 / 时间窗 / 指定人员）', super: '只读', dept: '可写', pre: '参考', formal: '只读历史' },
        { name: '批阅与发布成绩', super: '只读', dept: '可写', pre: '—', formal: '查看' },
        { name: '转正审批', super: '只读 + 撤回纠错 ★', dept: '终审即生效', pre: '提交申请', formal: '查看' },
        { name: '考核规则（门槛 / 权重 / 补考）', super: '可写 ★', dept: '只读', pre: '—', formal: '—' },
        { name: '协议 / 证书模板', super: '可写 ★', dept: '只读（使用）', pre: '签署', formal: '查看' },
        { name: '账号与角色', super: '可写 ★', dept: '—', pre: '—', formal: '—' },
        { name: '菜单 / 字典 / 参数 / 日志 / 监控', super: '可写 ★', dept: '—', pre: '—', formal: '—' },
        { name: '全局统计与导出', super: '可用 ★（导出留痕）', dept: '本部门', pre: '本人', formal: '本人' }
      ]
    }
  },
  created() {
    this.loadRoles()
  },
  methods: {
    loadRoles() {
      this.loading = true
      listRole({ pageNum: 1, pageSize: 100 }).then(res => {
        this.roles = res.rows || []
      }).catch(() => { this.roles = [] }).finally(() => { this.loading = false })
    },
    cellClass(text) {
      if (!text || text === '—') return ''
      if (text.indexOf('★') > -1 || text.indexOf('可写') > -1) return 'num'
      return ''
    }
  }
}
</script>

<style lang="scss" scoped>
@import '~@/assets/styles/super-module.scss';
code { padding: 1px 5px; color: #475467; background: #f2f4f7; font-size: 11px; border-radius: 3px; }
</style>
