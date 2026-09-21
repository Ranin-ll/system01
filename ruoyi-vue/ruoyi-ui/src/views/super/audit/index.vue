<template>
  <div class="s-page">
    <header class="s-head">
      <div>
        <span class="eyebrow">SUPER ADMIN · AUDIT &amp; COMPLIANCE</span>
        <h1>审计日志</h1>
        <p>谁在何时做了什么：全局操作留痕（含部门管理员的业务写操作）。超管权限最大，因此本页同时呈现「超管可写动作」与「仍会被拦截的写入」的边界。</p>
      </div>
      <div class="s-head-actions">
        <span class="s-ro"><i class="el-icon-view" /> 只读</span>
        <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="loadLogs">刷新</el-button>
      </div>
    </header>

    <div class="s-grid">
      <section class="s-card s-c12">
        <div class="s-card-h">
          <div class="tt"><span class="s-idx">痕</span><h3>操作日志</h3></div>
          <div style="display:flex;align-items:center;gap:8px">
            <el-input v-model="query.title" size="mini" placeholder="按动作标题搜索" style="width:180px" @keyup.enter.native="handleQuery" />
            <el-select v-model="query.businessType" size="mini" clearable placeholder="全部类型" style="width:130px">
              <el-option label="新增" :value="1" /><el-option label="修改" :value="2" />
              <el-option label="删除" :value="3" /><el-option label="导出" :value="4" />
              <el-option label="其他" :value="0" />
            </el-select>
            <el-button size="mini" type="primary" icon="el-icon-search" @click="handleQuery">查询</el-button>
          </div>
        </div>
        <el-table v-loading="loading" :data="logs" size="mini" stripe empty-text="暂无操作记录">
          <el-table-column label="时间" width="160" prop="operTime" />
          <el-table-column label="操作人" width="130" prop="operName" />
          <el-table-column label="模块" width="120" prop="title" />
          <el-table-column label="动作" width="90" align="center">
            <template slot-scope="scope">
              <span class="s-badge" :class="typeTone(scope.row.businessType)">{{ typeText(scope.row.businessType) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="请求地址" min-width="220" prop="operUrl" show-overflow-tooltip />
          <el-table-column label="IP" width="130" prop="operIp" />
          <el-table-column label="结果" width="80" align="center">
            <template slot-scope="scope">
              <span class="s-badge" :class="scope.row.status === 0 ? 'ok' : 'red'">{{ scope.row.status === 0 ? '成功' : '失败' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90" align="center">
            <template slot-scope="scope">
              <el-button type="text" size="mini" @click="openDetail(scope.row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination v-show="total > 0" :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="loadLogs" />
      </section>

      <section class="s-card s-c6">
        <div class="s-card-h"><div class="tt"><span class="s-idx o">挡</span><h3>仍会被后端拦截的写入</h3></div><span class="s-badge ok">拦截生效</span></div>
        <div class="s-steps">
          <div v-for="c in guards" :key="c.name" class="s-step done">
            <span class="mark">✓</span>
            <div class="txt"><b>{{ c.name }}</b><span>{{ c.desc }}</span></div>
          </div>
        </div>
        <div class="s-callout warn">
          <i class="el-icon-warning-outline" />
          <span>
            <b>建议补充：</b>命中写入拦截时<b>同时写一条尝试记录</b>（当前仅返回错误、不留痕）。
            超管账号一旦被盗用，这份记录是发现异常写入尝试的第一手线索。
          </span>
        </div>
      </section>

      <section class="s-card s-c6">
        <div class="s-card-h"><div class="tt"><span class="s-idx p">纠</span><h3>转正纠错（唯一业务写入口）</h3></div><span class="s-badge warn">流程待补</span></div>
        <div class="s-steps">
          <div class="s-step now"><span class="mark">1</span><div class="txt"><b>发起撤回</b><span>在全局花名册中选中已转正的实习生 → 「撤回转正」</span></div></div>
          <div class="s-step"><span class="mark">2</span><div class="txt"><b>填写原因（必填 ≥ 20 字）</b><span>原因写入审计日志，并通知该部门管理员与实习生</span></div></div>
          <div class="s-step"><span class="mark">3</span><div class="txt"><b>二次确认</b><span>输入实习生姓名以确认（防误操作）</span></div></div>
          <div class="s-step"><span class="mark">4</span><div class="txt"><b>生效</b><span><code>user_status</code> 退回 PRE_TRAINEE、证书作废、学习与考核记录保留</span></div></div>
        </div>
        <p class="s-note">这是超管<b>唯一</b>能改业务实例的入口：因为转正由部门管理员终审即生效，出现误批时需要一条纠错通道，但必须可追溯。</p>
      </section>
    </div>

    <!-- 日志详情 -->
    <el-dialog title="操作详情" :visible.sync="detailVisible" width="680px" append-to-body>
      <div v-if="current" class="s-detail">
        <div><span>操作模块</span><b>{{ current.title }}</b></div>
        <div><span>操作人</span><b>{{ current.operName }}</b></div>
        <div><span>时间</span><b>{{ current.operTime }}</b></div>
        <div><span>IP</span><b>{{ current.operIp }}</b></div>
        <div class="full"><span>请求地址</span><b>{{ current.operUrl }}</b></div>
        <div class="full"><span>请求参数</span><b class="mono">{{ current.operParam || '—' }}</b></div>
        <div class="full"><span>返回结果</span><b class="mono">{{ current.jsonResult || '—' }}</b></div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { list } from '@/api/monitor/operlog'

/**
 * 超管「审计日志」页
 * 真实数据：/monitor/operlog/list（平台原生操作日志，@Log 注解自动记录）
 * 静态内容：仍会被拦截的写入说明、转正纠错流程（待补实现）
 */
export default {
  name: 'SuperAudit',
  data() {
    return {
      loading: false,
      logs: [],
      total: 0,
      query: { pageNum: 1, pageSize: 10, title: '', businessType: undefined },
      detailVisible: false,
      current: null,
      guards: [
        { name: '任务：发布 / 结束 / 批阅', desc: 'TaskServiceImpl.requireDeptAdmin() 抛「超级管理员仅可查看任务，不能发布或批阅」（任务列表本身可读）' },
        { name: '非超管且未配置部门时访问业务数据', desc: '各 Service 的 currentScopeDeptId / managerScopeDeptId 对该类账号直接抛「当前账号未配置部门」' }
      ]
    }
  },
  created() {
    this.loadLogs()
  },
  methods: {
    loadLogs() {
      this.loading = true
      list(this.query).then(res => {
        this.logs = res.rows || []
        this.total = res.total || 0
      }).catch(() => { this.logs = []; this.total = 0 }).finally(() => { this.loading = false })
    },
    handleQuery() {
      this.query.pageNum = 1
      this.loadLogs()
    },
    openDetail(row) {
      this.current = row
      this.detailVisible = true
    },
    typeText(t) {
      return { 0: '其他', 1: '新增', 2: '修改', 3: '删除', 4: '导出' }[t] || '其他'
    },
    typeTone(t) {
      return { 0: '', 1: 'ok', 2: 'blue', 3: 'red', 4: 'purple' }[t] || ''
    }
  }
}
</script>

<style lang="scss" scoped>
@import '~@/assets/styles/super-module.scss';
.s-detail { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px 18px; }
.s-detail > div { min-width: 0; }
.s-detail .full { grid-column: 1 / -1; }
.s-detail span { display: block; margin-bottom: 5px; color: #98a2b3; font-size: 11.5px; }
.s-detail b { display: block; color: #1d2939; font-size: 12.5px; word-break: break-all; }
.s-detail b.mono { padding: 8px 10px; background: #f8fafc; border: 1px solid #eef1f5; border-radius: 4px; font-family: Consolas, Monaco, monospace; font-size: 11.5px; line-height: 1.6; }
</style>
