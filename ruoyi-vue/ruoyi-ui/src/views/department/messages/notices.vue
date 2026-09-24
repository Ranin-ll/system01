<template>
  <div class="dept-page">
    <div class="dept-breadcrumb">
      任务与通知 <span>/</span> <b>通知管理</b>
    </div>

    <div class="dept-heading">
      <div>
        <span class="eyebrow">DEPARTMENT ADMIN</span>
        <h1>通知管理</h1>
        <p>
          单向告知，无完成状态；对应实习生端顶栏铃铛与工作台公告位。
          <b>范围锁本部门</b>（后端强校验）；「公告」仅超管可发。
        </p>
      </div>
      <div class="dept-heading-actions">
        <span class="dbadge green">真实数据</span>
        <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="loadSent">刷新</el-button>
      </div>
    </div>

    <div class="dgrid">
      <!-- 发送通知 -->
      <div class="dcard c5">
        <div class="dcard-h">
          <div class="tt"><span class="idx">知</span><h3>发送通知</h3></div>
        </div>

        <div class="dfield">
          <label>通知标题 <b>*</b></label>
          <el-input v-model="form.title" size="small" maxlength="80" show-word-limit
                    placeholder="例如：【提醒】2026Q3 正式考核将于 9 月 20 日 09:00 开考" />
        </div>

        <div class="dfg2" style="margin-top:12px">
          <div class="dfield">
            <label>载体类型</label>
            <el-select v-model="form.msgType" size="small" style="width:100%">
              <el-option label="通知" value="NOTIFY" />
              <el-option label="公告（仅超管可发）" value="ANNOUNCE" disabled />
            </el-select>
          </div>
          <div class="dfield">
            <label>有效期至</label>
            <el-date-picker v-model="form.effectiveTo" type="datetime" size="small"
                            value-format="yyyy-MM-dd HH:mm:ss" placeholder="可留空（长期有效）" style="width:100%" />
          </div>
        </div>

        <div class="dfield">
          <label>发送范围 <b>*</b></label>
          <el-select v-model="form.scopeType" size="small" style="width:100%" @change="onScopeChange">
            <el-option label="本部门全体" value="DEPT" />
            <el-option label="按岗位" value="POSITION" />
            <el-option label="指定人员" value="USER" />
          </el-select>
        </div>

        <div v-if="form.scopeType === 'POSITION'" class="dfield">
          <label>岗位 <b>*</b></label>
          <el-select v-model="form.scopeId" size="small" style="width:100%" placeholder="选择本部门岗位">
            <el-option v-for="p in myPositions" :key="p.id" :label="p.positionName" :value="Number(p.id)" />
          </el-select>
        </div>

        <div v-if="form.scopeType === 'USER'" class="dfield">
          <label>接收人 <b>*</b></label>
          <el-select v-model="form.targetUserIds" size="small" multiple filterable style="width:100%"
                     placeholder="搜索本部门人员">
            <el-option v-for="u in myUsers" :key="u.userId"
                       :label="(u.nickName || u.userName) + '（' + (u.userName) + '）'" :value="u.userId" />
          </el-select>
        </div>

        <div class="dfield" style="margin-top:12px">
          <label>通知正文 <b>*</b></label>
          <el-input v-model="form.content" type="textarea" :rows="6" size="small"
                    placeholder="请提前 10 分钟进入考核页面完成设备自检。理论 60 分钟，实操 90 分钟，两部分均需及格。" />
        </div>

        <div class="dchkrow" style="margin-top:6px" @click="form.isTop = form.isTop ? 0 : 1">
          <span class="box" :class="{ sw: form.isTop === 1 }"><i class="el-icon-check" /></span>
          <span>置顶显示（列表与公告位排序优先）</span>
        </div>

        <div class="dbtn-row right" style="margin-top:16px">
          <span class="hint-text" style="margin-right:auto">
            预计送达 <b>{{ estimate === null ? '—' : estimate + ' 人' }}</b>
          </span>
          <el-button size="small" type="primary" :loading="sending" @click="send">发送通知</el-button>
        </div>
      </div>

      <!-- 已发送通知 -->
      <div class="dcard c7">
        <div class="dcard-h">
          <div class="tt"><span class="idx">史</span><h3>已发送通知</h3></div>
          <div class="sent-filters">
            <TraineeSelect v-model="query.targetUserId" width="148px" @change="loadSent" @loaded="trainees = $event" />
            <el-input
              v-model="query.title" size="small" clearable placeholder="搜标题" style="width:130px"
              @keyup.enter.native="loadSent" @clear="loadSent"
            />
          </div>
        </div>
        <p v-if="query.targetUserId" class="sent-hint">
          <i class="el-icon-user" /> 只看 <b>{{ targetName }}</b> 能收到的通知；「已读」列换成他本人的阅读状态。
          <el-button type="text" @click="clearFilter">取消筛选</el-button>
        </p>
        <table class="dtbl">
          <thead>
            <tr>
              <th>标题</th>
              <th style="width:74px">载体</th>
              <th style="width:110px">范围</th>
              <th style="width:110px">发送时间</th>
              <th style="width:84px">{{ query.targetUserId ? '他的状态' : '已读' }}</th>
              <th style="width:120px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in notices" :key="row.id">
              <td>
                <span class="strong">{{ row.title }}</span>
                <span v-if="row.isTop" class="dbadge orange" style="margin-left:6px">置顶</span>
                <span v-if="row.status === 'REVOKED'" class="dbadge gray" style="margin-left:6px">已撤回</span>
              </td>
              <td><span class="dbadge" :class="row.msgType === 'ANNOUNCE' ? 'gray' : 'blue'">{{ carrier(row.msgType) }}</span></td>
              <td>{{ scopeText(row) }}</td>
              <td>{{ shortTime(row.publishTime || row.createTime) }}</td>
              <td>
                <!-- 筛选到个人时：看的是「TA 读没读」，而不是整体已读数 -->
                <template v-if="query.targetUserId">
                  <span class="dbadge" :class="row.targetReadTime ? 'green' : 'orange'">
                    {{ row.targetReadTime ? '已读' : '未读' }}
                  </span>
                  <div v-if="row.targetReadTime" class="hint-text">{{ shortTime(row.targetReadTime) }}</div>
                </template>
                <span v-else :class="{ 'read-all': row.deliveredCount && row.readCount >= row.deliveredCount }">
                  {{ row.readCount || 0 }} / {{ row.deliveredCount || 0 }}
                </span>
              </td>
              <td>
                <div class="acts">
                  <el-button type="text" @click="showRecipients(row)">未读名单</el-button>
                  <span class="sep" v-if="row.status !== 'REVOKED'">|</span>
                  <el-button v-if="row.status !== 'REVOKED'" type="text" @click="revoke(row)">撤回</el-button>
                </div>
              </td>
            </tr>
            <tr v-if="!loading && !notices.length">
              <td colspan="6">
                <div class="dempty small">
                  <i class="el-icon-message" />
                  <strong>还没有发出过通知</strong>
                  <span>在左侧填写标题与正文后点击「发送通知」。</span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    

    <!-- 未读名单 -->
    <el-dialog :title="'收件人清单 · ' + (cur && cur.title || '')" :visible.sync="rcpVisible" width="620px" append-to-body>
      <table class="dtbl">
        <thead>
          <tr><th>姓名</th><th style="width:130px">账号</th><th style="width:110px">部门</th><th style="width:80px">状态</th><th style="width:150px">已读时间</th></tr>
        </thead>
        <tbody>
          <tr v-for="r in rcp" :key="r.userId">
            <td class="strong">{{ r.nickName }}</td>
            <td>{{ r.userName }}</td>
            <td>{{ r.deptName }}</td>
            <td><span class="dbadge" :class="r.readTime ? 'green' : 'orange'">{{ r.readTime ? '已读' : '未读' }}</span></td>
            <td>{{ r.readTime || '—' }}</td>
          </tr>
        </tbody>
      </table>
      <p class="dsec-note" style="margin-top:10px">未读的人排在前面。</p>
    </el-dialog>
  </div>
</template>

<script>
/**
 * 部门管理员端 · 通知管理（2026-09-17 接后端）
 *
 * 与超管「任务与通知」共用同一套接口与送达谓词，差异只在能力：
 * 部门管理员**不能发公告、不能发全体**，范围锁在本部门（后端强校验，前端 disabled 只是体验）。
 *
 * 本页原为演示态；本轮只换数据层，保留既有 d* 结构与样式。
 */
import { sendMessage, listSentMessages, listRecipients, revokeMessage } from '@/api/business/message'
import { listCoursePositions } from '@/api/business/course'
import { listTrainees } from '@/api/business/task'
import TraineeSelect from '@/components/TraineeSelect'
import { mapGetters } from 'vuex'

const SCOPE_LABEL = { ALL: '全体', DEPT: '本部门', POSITION: '岗位', USER: '指定人员' }

export default {
  name: 'DeptNotices',
  components: { TraineeSelect },
  data() {
    return {
      loading: false,
      sending: false,
      estimate: null,
      positions: [],
      users: [],
      notices: [],
      query: { pageNum: 1, pageSize: 20, targetUserId: null, title: '' },
      trainees: [],
      form: {
        msgType: 'NOTIFY',
        scopeType: 'DEPT',
        scopeId: null,
        targetUserIds: [],
        title: '',
        content: '',
        isTop: 0,
        effectiveTo: null
      },
      rcpVisible: false,
      rcp: [],
      cur: null
    }
  },
  computed: {
    ...mapGetters(['deptId', 'deptName']),
    /**
     * 可选岗位：直接用 /business/course/positions —— 该接口**已按当前部门过滤**
     * （开发部门管理员只返回「开发实习生」，超管返回全部 5 个）。
     * 不用 dept-bindings 是因为它对 DEPT_ADMIN 是 403（缺 business:position:list 权限）。
     */
    myPositions() {
      return this.positions
    },
    /** 本部门人员 */
    myUsers() {
      if (!this.deptId) return []
      return this.users.filter(u => Number(u.deptId) === Number(this.deptId))
    },
    /** 按实习生筛选时，提示里显示 TA 的姓名 */
    targetName() {
      const u = this.trainees.find(x => Number(x.userId) === Number(this.query.targetUserId))
      return u ? (u.nickName || u.userName) : ''
    }
  },
  created() {
    this.loadBase()
    this.loadSent()
  },
  methods: {
    loadBase() {
      listCoursePositions().then(res => { this.positions = res.data || [] }).catch(() => { this.positions = [] })
      // ★ 用业务侧接口，不用 /system/user/list（后者对部门管理员是 403，会静默拿到空列表，
      //   之前的「指定人员」选择器就是空的）
      listTrainees().then(res => { this.users = res.data || [] }).catch(() => { this.users = [] })
    },
    loadSent() {
      this.loading = true
      // 空串要转成 undefined，否则后端会把「清空筛选」当成「按空标题搜」
      const q = Object.assign({}, this.query, {
        targetUserId: this.query.targetUserId || undefined,
        title: this.query.title || undefined
      })
      listSentMessages(q).then(res => {
        this.notices = (res.data || {}).rows || []
      }).catch(() => { this.notices = [] }).finally(() => { this.loading = false })
    },
    clearFilter() {
      this.query.targetUserId = null
      this.loadSent()
    },
    onScopeChange() {
      this.form.scopeId = null
      this.form.targetUserIds = []
      this.calcEstimate()
    },
    calcEstimate() {
      const f = this.form
      if (f.scopeType === 'DEPT') {
        this.estimate = this.myUsers.length
      } else if (f.scopeType === 'POSITION') {
        this.estimate = f.scopeId ? this.myUsers.filter(u => Number(u.positionId) === Number(f.scopeId)).length : null
      } else {
        this.estimate = f.targetUserIds.length || null
      }
    },
    send() {
      const f = this.form
      if (!f.title.trim()) { this.$message.warning('请先填写通知标题'); return }
      if (!f.content.trim()) { this.$message.warning('请填写通知正文'); return }
      if (f.scopeType === 'POSITION' && !f.scopeId) { this.$message.warning('请选择岗位'); return }
      if (f.scopeType === 'USER' && !f.targetUserIds.length) { this.$message.warning('请选择接收人'); return }

      // 本部门全体：scopeId 直接用当前用户部门（后端同样会校验）
      const body = {
        msgType: f.msgType,
        scopeType: f.scopeType,
        scopeId: f.scopeType === 'DEPT' ? this.deptId : f.scopeId,
        targetUserIds: f.scopeType === 'USER' ? f.targetUserIds : [],
        title: f.title,
        content: f.content,
        isTop: f.isTop,
        effectiveTo: f.effectiveTo || null
      }
      this.sending = true
      sendMessage(body).then(res => {
        this.$message.success(res.msg || '已发送')
        this.form.title = ''
        this.form.content = ''
        this.form.scopeId = null
        this.form.targetUserIds = []
        this.estimate = null
        this.loadSent()
      }).catch(() => {}).finally(() => { this.sending = false })
    },
    carrier(t) {
      return t === 'ANNOUNCE' ? '公告' : '通知'
    },
    scopeText(row) {
      const base = SCOPE_LABEL[row.scopeType] || row.scopeType
      if (row.scopeType === 'POSITION' && row.scopeId) {
        const p = this.positions.find(x => Number(x.id) === Number(row.scopeId))
        return p ? p.positionName : base
      }
      return base
    },
    shortTime(v) {
      if (!v) return '—'
      return String(v).replace('T', ' ').slice(5, 16)
    },
    showRecipients(row) {
      this.cur = row
      listRecipients(row.id).then(res => {
        this.rcp = res.data || []
        this.rcpVisible = true
      }).catch(() => {})
    },
    revoke(row) {
      this.$confirm('撤回后该通知会立即从所有人的消息列表消失（已读记录保留），确定撤回？', '撤回通知', {
        confirmButtonText: '确定撤回',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => revokeMessage(row.id).then(res => {
        this.$message.success(res.msg || '已撤回')
        this.loadSent()
      }).catch(() => {})).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
@import '~@/assets/styles/department-module.scss';

.dchkrow:hover { color: #1764f5; }
.dtbl .acts .sep { color: #d0d5dd; }
.dtbl .read-all { color: #067647; font-weight: 600; }
.sent-filters { display: flex; align-items: center; gap: 8px; }
.sent-hint {
  display: flex; align-items: center; gap: 6px;
  margin: 0 0 8px; padding: 7px 10px;
  background: #f7faff; border: 1px solid #dbe6f8; border-radius: 8px;
  color: #344054; font-size: 12px;

  b { color: #1764f5; }
  .el-button { padding: 0 4px; font-size: 12px; }
}
code { padding: 1px 5px; color: #344054; font-size: 11.5px; background: #f2f4f7; border-radius: 4px; }
.hint-text { color: #98a2b3; font-size: 11.5px; }
</style>
