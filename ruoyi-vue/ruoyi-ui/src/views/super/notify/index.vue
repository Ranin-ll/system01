<template>
  <div class="nt-page">
    <header class="nt-head">
      <div>
        <span class="eyebrow">NOTICE CENTER</span>
        <h1>任务与通知</h1>
        <p>
          发<b>公告</b>（挂出去给人看）或<b>通知</b>（戳到人）。范围四档：全体 / 部门 / 岗位 / 指定人员。
          <b>公告与「全体」仅超管可发</b>；业务通知统一走业务表，与平台原生「通知公告」并存。
        </p>
      </div>
      <div class="nt-actions">
        <span class="chip"><i class="el-icon-bell" /> 我的未读 {{ unread }}</span>
        <el-button size="small" type="primary" icon="el-icon-plus" @click="openSend">发通知</el-button>
        <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="loadSent">刷新</el-button>
      </div>
    </header>

    <div class="grid">
      <!-- 发送 -->
      <section class="card c-send">
        <div class="card-h"><div class="tt"><span class="idx">发</span><h3>发送 · 公告 / 通知</h3></div><span class="badge">超管独有公告</span></div>

        <div class="field">
          <label>载体类型 <b>*</b></label>
          <span class="seg">
            <span :class="{ on: form.msgType === 'ANNOUNCE' }" @click="setType('ANNOUNCE')">公告</span>
            <span :class="{ on: form.msgType === 'NOTIFY' }" @click="setType('NOTIFY')">通知</span>
          </span>
          <div class="tip">
            选<b>公告</b>：除消息中心外还会出现在各端工作台「公告位」（最多 3 条 · 可置顶 · 到期自动消失 · 不计未读红点）。<br />
            选<b>通知</b>：只进消息中心 + 铃铛未读。
          </div>
        </div>

        <div class="field">
          <label>发布范围 <b>*</b></label>
          <span class="seg">
            <span :class="{ on: form.scopeType === 'ALL' }" @click="setScope('ALL')">全体</span>
            <span :class="{ on: form.scopeType === 'DEPT' }" @click="setScope('DEPT')">按部门</span>
            <span :class="{ on: form.scopeType === 'POSITION' }" @click="setScope('POSITION')">按岗位</span>
            <span :class="{ on: form.scopeType === 'USER' }" @click="setScope('USER')">指定人员</span>
          </span>
        </div>

        <div v-if="form.scopeType === 'DEPT'" class="field">
          <label>部门 <b>*</b></label>
          <el-select v-model="form.scopeId" size="small" placeholder="选择部门" style="width:100%" @change="loadEstimate">
            <el-option v-for="d in depts" :key="d.deptId" :label="d.deptName" :value="Number(d.deptId)" />
          </el-select>
        </div>

        <div v-if="form.scopeType === 'POSITION'" class="field">
          <label>岗位 <b>*</b></label>
          <el-select v-model="form.scopeId" size="small" placeholder="选择岗位" style="width:100%" @change="loadEstimate">
            <el-option v-for="p in positionOptions" :key="p.id" :label="p.positionName" :value="Number(p.id)" />
          </el-select>
          <div class="tip">
            岗位分两类：<b>实习生岗</b>（实施 / 开发 / 设计 / 质检 / 建模）
            与<b>管理员岗</b>（各部门管理员）。
            <b>部门管理员不占实习生岗</b>（<code>position_id</code> 为空），
            所以要单独选「部门管理员」才会发给 TA 们。
          </div>
        </div>

        <div v-if="form.scopeType === 'USER'" class="field">
          <label>接收人 <b>*</b></label>
          <el-select v-model="form.targetUserIds" size="small" multiple filterable placeholder="搜索姓名 / 账号"
                     style="width:100%" @change="loadEstimate">
            <el-option v-for="u in users" :key="u.userId"
                       :label="(u.nickName || u.userName) + '（' + (u.deptName || '—') + '）'" :value="u.userId" />
          </el-select>
        </div>

        <div class="field"><label>标题 <b>*</b></label><el-input v-model="form.title" size="small" maxlength="80" show-word-limit placeholder="如：关于 2026 Q3 考核安排的通知" /></div>
        <div class="field"><label>正文 <b>*</b></label><el-input v-model="form.content" type="textarea" :rows="5" placeholder="正文内容" /></div>

        <div v-if="form.msgType === 'ANNOUNCE'" class="field row2">
          <label>公告属性</label>
          <div class="inline">
            <el-checkbox v-model="topFlag">置顶</el-checkbox>
            <el-date-picker v-model="form.effectiveTo" type="datetime" size="small" placeholder="有效期至（可空）"
                            value-format="yyyy-MM-dd HH:mm:ss" style="width:200px" />
          </div>
        </div>

        <div class="field">
          <label>预计送达</label>
          <span class="badge blue">{{ estimate === null ? '—' : estimate + ' 人' }}</span>
          <span class="tip" style="display:inline;margin-left:8px">由后端按<b>送达规则</b>实时计算（与发出后的「送达数」同口径，两个数必然一致）</span>
        </div>

        <div class="foot">
          <el-button size="small" type="primary" :loading="sending" @click="send">立即发布</el-button>
          <el-button size="small" @click="resetForm">重置</el-button>
        </div>
      </section>

      <!-- 已发送与回执 -->
      <section class="card c-sent">
        <div class="card-h">
          <div class="tt"><span class="idx">回</span><h3>我发送过的</h3></div>
          <span class="mut">送达 / 已读 / 未读</span>
        </div>

        <div class="filter">
          <span class="seg">
            <span :class="{ on: query.msgType === '' }" @click="setFilter('')">全部</span>
            <span :class="{ on: query.msgType === 'ANNOUNCE' }" @click="setFilter('ANNOUNCE')">公告</span>
            <span :class="{ on: query.msgType === 'NOTIFY' }" @click="setFilter('NOTIFY')">通知</span>
          </span>
          <el-input v-model="query.title" size="mini" clearable placeholder="搜标题" style="width:150px"
                    @keyup.enter.native="loadSent" @clear="loadSent" />
          <el-button size="mini" icon="el-icon-search" @click="loadSent" />
        </div>

        <el-table v-loading="loading" :data="sent" size="mini" style="width:100%">
          <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
          <el-table-column label="载体" width="76">
            <template slot-scope="s">
              <span class="tag" :class="s.row.msgType === 'ANNOUNCE' ? 'gray' : 'blue'">
                {{ s.row.msgType === 'ANNOUNCE' ? '公告' : '通知' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="范围" width="96">
            <template slot-scope="s">{{ scopeLabel(s.row) }}</template>
          </el-table-column>
          <el-table-column prop="deliveredCount" label="送达" width="60" align="center" />
          <el-table-column label="已读" width="60" align="center">
            <template slot-scope="s"><b>{{ s.row.readCount || 0 }}</b></template>
          </el-table-column>
          <el-table-column label="未读" width="60" align="center">
            <template slot-scope="s">
              <span :class="{ warn: unreadOf(s.row) > 0 }">{{ unreadOf(s.row) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="76">
            <template slot-scope="s">
              <span class="tag" :class="s.row.status === 'REVOKED' ? 'gray' : 'green'">
                {{ s.row.status === 'REVOKED' ? '已撤回' : '进行中' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150">
            <template slot-scope="s">
              <el-button size="mini" type="text" @click="showRecipients(s.row)">未读名单</el-button>
              <el-button v-if="s.row.status !== 'REVOKED'" size="mini" type="text" @click="revoke(s.row)">撤回</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="!loading && !sent.length" class="empty">还没有发送过通知</div>
        <div class="note">
          送达数 = 命中该条范围的有效账号数；已读数来自 <code>notice_read</code>（与实习生端铃铛红点<b>同一张表</b>，不会出现「这边已读、那边还红着」）。
        </div>
      </section>
    </div>

    <!-- 未读名单 -->
    <el-dialog :title="'收件人清单 · ' + (cur && cur.title || '')" :visible.sync="rcpVisible" width="620px" append-to-body>
      <el-table :data="rcp" size="mini" max-height="420" style="width:100%">
        <el-table-column prop="nickName" label="姓名" width="120" />
        <el-table-column prop="userName" label="账号" width="140" />
        <el-table-column prop="deptName" label="部门" width="110" />
        <el-table-column label="状态" width="90">
          <template slot-scope="s">
            <span class="tag" :class="s.row.readTime ? 'green' : 'orange'">{{ s.row.readTime ? '已读' : '未读' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="readTime" label="已读时间" />
      </el-table>
      <div class="note" style="margin-top:10px">未读的人排在前面。「再提醒」= 对这些未读者发一条定向通知（与超管催办同一套机制，P3）。</div>
    </el-dialog>
  </div>
</template>

<script>
/**
 * 超管端「任务与通知」（通知中心）
 *
 * 三端命名统一（决策 6）：实习生 / 部门管理员 / 超管都叫「任务与通知」，用户跨角色不用重新认位置。
 * 本页 = 发送（公告 / 通知 × 四档范围）+ 已发送回执 + 撤回 + 未读名单。
 *
 * 权限口径：前端只用 canAnnounce 控制「公告」选项的可见性，**真正的范围强校验在后端**
 * （公告与 ALL 仅超管；部门管理员限本部门），前端隐藏只是体验。
 */
import { getUnreadCount, sendMessage, listSentMessages, listRecipients, revokeMessage, estimateAudience } from '@/api/business/message'
import { listDept } from '@/api/system/dept'
import { listPosition } from '@/api/business/position'
import { listUser } from '@/api/system/user'

const SCOPE_LABEL = { ALL: '全体', DEPT: '部门', POSITION: '岗位', USER: '指定人员' }

/**
 * 「部门管理员岗」的虚拟岗位 ID —— 必须与后端 `Notice.POSITION_DEPT_ADMIN` 保持一致。
 * 为什么用 0：`position.id` 自增、真实岗位从 1 起，0 永远不会是真实岗位，
 * 所以不需要往 `position` 表插一行（插了会被课程 / 题库 / 考核按岗位归属的逻辑当成真实岗位用）。
 */
const POSITION_DEPT_ADMIN = 0

export default {
  name: 'SuperNotify',
  data() {
    return {
      loading: false,
      sending: false,
      unread: 0,
      estimate: null,
      sent: [],
      depts: [],
      positions: [],
      users: [],
      query: { pageNum: 1, pageSize: 20, msgType: '', title: '' },
      form: this.blankForm(),
      topFlag: false,
      rcpVisible: false,
      rcp: [],
      cur: null
    }
  },
  computed: {
    roles() {
      return this.$store.getters.roles || []
    },
    canAnnounce() {
      return this.roles.indexOf('SUPER_ADMIN') > -1 || this.roles.indexOf('admin') > -1
    },
    /** 岗位口径 = 5 个实习生岗 + 「部门管理员」管理员岗（管理员不占实习生岗，必须单列） */
    positionOptions() {
      return this.positions.concat([{ id: POSITION_DEPT_ADMIN, positionName: '部门管理员（各部门）' }])
    }
  },
  created() {
    this.loadBase()
    this.loadSent()
    getUnreadCount().then(res => { this.unread = Number((res.data || {}).total || 0) }).catch(() => {})
  },
  methods: {
    blankForm() {
      return {
        msgType: 'ANNOUNCE', scopeType: 'ALL', scopeId: null, targetUserIds: [],
        title: '', content: '', isTop: 0, effectiveTo: null
      }
    },
    setType(t) {
      if (t === 'ANNOUNCE' && !this.canAnnounce) {
        this.$message.warning('只有超级管理员可以发布公告')
        return
      }
      this.form.msgType = t
    },
    setScope(s) {
      this.form.scopeType = s
      this.form.scopeId = null
      this.form.targetUserIds = []
      this.loadEstimate()
    },
    setFilter(t) {
      this.query.msgType = t
      this.loadSent()
    },
    loadBase() {
      listDept({ status: '0' }).then(res => { this.depts = res.data || [] }).catch(() => {})
      listPosition({ pageNum: 1, pageSize: 100 }).then(res => { this.positions = res.rows || [] }).catch(() => {})
      listUser({ pageNum: 1, pageSize: 500 }).then(res => { this.users = res.rows || [] }).catch(() => {})
    },
    openSend() {
      this.resetForm()
    },
    resetForm() {
      this.form = this.blankForm()
      this.topFlag = false
      this.estimate = null
    },
    /** 预计送达：前端按已加载数据估算（真实送达由后端谓词判定，故标注为"预计"） */
    /**
     * 预计送达：ALL / DEPT / POSITION 由后端按送达谓词算（与发出后的「送达数」同口径，
     * 因此两个数必然一致）；USER 档收件人就在表单里，直接数数组。
     */
    loadEstimate() {
      const f = this.form
      if (f.scopeType === 'USER') {
        this.estimate = f.targetUserIds.length || null
        return
      }
      // ★ 必须用「是否为 null」判断，不能用 !f.scopeId：
      //   「部门管理员岗」的 scopeId 是哨兵值 0，而 0 在 JS 里是假值 ——
      //   写成 !f.scopeId 会把"已选部门管理员"当成"没选"，估算显示「—」、发送被拦。
      const noScope = f.scopeId === null || f.scopeId === undefined || f.scopeId === ''
      if ((f.scopeType === 'DEPT' || f.scopeType === 'POSITION') && noScope) {
        this.estimate = null
        return
      }
      estimateAudience(f.scopeType, f.scopeId).then(res => {
        this.estimate = res.data === null || res.data === undefined ? null : Number(res.data)
      }).catch(() => { this.estimate = null })
    },
    send() {
      const f = this.form
      if (!f.title.trim()) { this.$message.warning('请填写标题'); return }
      if (!f.content.trim()) { this.$message.warning('请填写正文'); return }
      if (f.scopeType === 'USER' && !f.targetUserIds.length) { this.$message.warning('请选择接收人'); return }
      // 同上：scopeId 为 0（部门管理员岗）是合法选择，不能当真值判空
      const noScope = f.scopeId === null || f.scopeId === undefined || f.scopeId === ''
      if ((f.scopeType === 'DEPT' || f.scopeType === 'POSITION') && noScope) { this.$message.warning('请选择范围对象'); return }
      const body = Object.assign({}, f, { isTop: f.msgType === 'ANNOUNCE' && this.topFlag ? 1 : 0 })
      if (f.scopeType !== 'USER') body.targetUserIds = []
      this.sending = true
      sendMessage(body).then(res => {
        this.$message.success(res.msg || '已发布')
        this.resetForm()
        this.loadSent()
      }).catch(() => {}).finally(() => { this.sending = false })
    },
    loadSent() {
      this.loading = true
      listSentMessages(this.query).then(res => {
        this.sent = (res.data || {}).rows || []
      }).catch(() => { this.sent = [] }).finally(() => { this.loading = false })
    },
    scopeLabel(row) {
      const base = SCOPE_LABEL[row.scopeType] || row.scopeType
      if (row.scopeType === 'DEPT') {
        const d = this.depts.find(x => Number(x.deptId) === Number(row.scopeId))
        return d ? '部门·' + d.deptName.replace('部门', '') : base
      }
      if (row.scopeType === 'POSITION') {
        // 虚拟「部门管理员岗」要显示成人能看懂的名字，否则回执里只写「岗位」等于没说清
        if (Number(row.scopeId) === POSITION_DEPT_ADMIN) return '岗位·部门管理员'
        const p = this.positions.find(x => Number(x.id) === Number(row.scopeId))
        return p ? '岗位·' + p.positionName.replace('实习生', '') : base
      }
      return base
    },
    unreadOf(row) {
      return Math.max(0, Number(row.deliveredCount || 0) - Number(row.readCount || 0))
    },
    showRecipients(row) {
      this.cur = row
      listRecipients(row.id).then(res => {
        this.rcp = res.data || []
        this.rcpVisible = true
      }).catch(() => {})
    },
    revoke(row) {
      this.$confirm('撤回后该通知会立即从所有人的消息列表消失（已读记录保留），确定撤回？', '提示', { type: 'warning' })
        .then(() => revokeMessage(row.id).then(res => {
          this.$message.success(res.msg || '已撤回')
          this.loadSent()
        }).catch(() => {})).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
/* 自包含样式，不 @import 共享基线（避免 scoped 下 :root 失效） */
.nt-page { padding: 18px 20px 32px; background: #eef2f7; min-height: calc(100vh - 84px); }
.nt-head {
  display: flex; align-items: flex-start; justify-content: space-between; gap: 16px;
  background: #fff; border: 1px solid #e4e9f0; border-radius: 12px; padding: 18px 20px; margin-bottom: 14px;

  .eyebrow { color: #98a2b3; font-size: 11px; letter-spacing: .6px; }
  h1 { margin: 4px 0 6px; font-size: 20px; color: #1d2939; }
  p { margin: 0; color: #667085; font-size: 12.5px; line-height: 1.7; max-width: 660px; }
  .nt-actions { display: flex; align-items: center; gap: 8px; flex: none; }
}
.chip {
  display: inline-flex; align-items: center; gap: 5px; height: 26px; padding: 0 10px;
  border-radius: 13px; font-size: 12px; background: #e8f1fd; color: #1764f5;
}
.grid { display: grid; grid-template-columns: minmax(0, 5fr) minmax(0, 7fr); gap: 14px; }
@media (max-width: 1200px) { .grid { grid-template-columns: 1fr; } }
.card { background: #fff; border: 1px solid #e4e9f0; border-radius: 12px; padding: 16px 18px; }
.card-h {
  display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px;
  .tt { display: flex; align-items: center; gap: 8px; }
  .idx {
    display: inline-flex; align-items: center; justify-content: center; width: 22px; height: 22px;
    border-radius: 6px; background: #e8f1fd; color: #1764f5; font-size: 12px;
  }
  h3 { margin: 0; font-size: 14px; color: #1d2939; }
}
.badge {
  height: 22px; padding: 0 8px; border-radius: 6px; background: #fff4e5; color: #b54708;
  font-size: 11px; line-height: 22px;
  &.blue { background: #e8f1fd; color: #1764f5; }
}
.mut { color: #98a2b3; font-size: 12px; }
.field { margin-bottom: 12px; }
.field > label { display: block; color: #344054; font-size: 12.5px; margin-bottom: 6px; }
.tip { color: #98a2b3; font-size: 11.5px; line-height: 1.7; margin-top: 6px; }
.seg {
  display: inline-flex; border: 1px solid #e4e9f0; border-radius: 8px; overflow: hidden;
  span { padding: 0 12px; height: 28px; line-height: 28px; font-size: 12px; color: #667085; cursor: pointer; }
  span.on { background: #e8f1fd; color: #1764f5; }
}
.inline { display: flex; align-items: center; gap: 12px; }
.foot { display: flex; gap: 10px; margin-top: 4px; }
.filter { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; flex-wrap: wrap; }
.tag {
  display: inline-block; padding: 0 7px; height: 20px; line-height: 20px; border-radius: 5px; font-size: 11px;
  &.gray { background: #f2f4f7; color: #475467; }
  &.blue { background: #e8f1fd; color: #1764f5; }
  &.green { background: #e7f7ef; color: #027a48; }
  &.orange { background: #fff4e5; color: #b54708; }
}
.warn { color: #b42318; font-weight: 500; }
.empty { padding: 40px 0; text-align: center; color: #98a2b3; font-size: 12.5px; }
.note { margin-top: 10px; color: #98a2b3; font-size: 11.5px; line-height: 1.7; }
</style>
