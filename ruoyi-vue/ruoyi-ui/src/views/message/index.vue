<template>
  <div class="msg-page">
    <!-- 页头 -->
    <header class="mh">
      <div>
        <span class="eyebrow">MESSAGE CENTER</span>
        <h1>消息中心</h1>
        <p>接收审核结果、学习与考核提醒、任务通知与公告。<b>打开即已读</b>，未读会在顶栏铃铛上亮红点。</p>
      </div>
      <div class="mh-actions">
        <span class="chip" :class="unreadTotal ? 'warn' : 'ok'">
          <i class="el-icon-bell" /> 未读 {{ unreadTotal }}
        </span>
        <el-button size="small" :disabled="!unreadTotal" @click="markAll">全部已读</el-button>
        <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="loadList">刷新</el-button>
      </div>
    </header>

    <!-- 页签 -->
    <div class="tabs">
      <span :class="{ on: tab === 'notice' }" @click="switchTab('notice')">
        通知<span v-if="unreadTotal" class="dot">{{ unreadTotal }}</span>
      </span>
      <!-- 「我的任务」只对实习生有意义：管理员的任务类消息走「去处理」回各自的业务页 -->
      <span v-if="isIntern" :class="{ on: tab === 'task' }" @click="switchTab('task')">我的任务</span>
    </div>

    <!-- ① 通知 -->
    <div v-show="tab === 'notice'" class="grid">
      <section class="card col-list">
        <div class="card-h">
          <div class="tt"><span class="idx">列</span><h3>消息列表</h3></div>
          <span class="mut">{{ total }} 条</span>
        </div>

        <div class="filter">
          <span class="seg">
            <span :class="{ on: query.readFlag === '' }" @click="setRead('')">全部</span>
            <span :class="{ on: query.readFlag === 'UNREAD' }" @click="setRead('UNREAD')">未读</span>
          </span>
          <el-select v-model="query.msgType" size="mini" clearable placeholder="全部类型" style="width:110px" @change="loadList">
            <el-option v-for="t in types" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
          <el-input v-model="query.title" size="mini" clearable placeholder="搜标题/内容" style="width:150px" @keyup.enter.native="loadList" @clear="loadList" />
          <el-button size="mini" icon="el-icon-search" @click="loadList" />
        </div>

        <div v-loading="loading" class="list">
          <div
            v-for="m in list"
            :key="m.id"
            class="item"
            :class="{ unread: !m.readTime, active: current && current.id === m.id }"
            @click="openDetail(m)"
          >
            <div class="it-top">
              <!-- 未读红点：整列扫一眼就能定位未读，不必去比对左边框的深浅 -->
              <span v-if="!m.readTime" class="reddot" title="未读" />
              <span class="tag" :style="{ background: typeStyle(m.msgType).bg, color: typeStyle(m.msgType).fg }">
                {{ typeStyle(m.msgType).label }}
              </span>
              <b class="it-title">{{ m.title }}</b>
              <span v-if="m.isTop" class="pin">置顶</span>
            </div>
            <div class="it-sub">
              {{ m.publisherName || '系统' }} · {{ m.publishTime || m.createTime }}
              <span v-if="m.readTime" class="mut"> · 已读</span>
            </div>
          </div>
          <div v-if="!loading && !list.length" class="empty">
            <i class="el-icon-chat-line-square" />
            <span>{{ query.readFlag === 'UNREAD' ? '没有未读消息' : '暂无消息' }}</span>
          </div>
        </div>
      </section>

      <section class="card col-detail">
        <template v-if="current">
          <div class="card-h">
            <div class="tt"><span class="idx">详</span><h3>详情</h3></div>
            <span class="mut">打开即已读</span>
          </div>
          <div class="d-top">
            <span class="tag" :style="{ background: typeStyle(current.msgType).bg, color: typeStyle(current.msgType).fg }">
              {{ typeStyle(current.msgType).label }}
            </span>
            <span class="mut">来自：{{ current.publisherName || '系统' }}</span>
            <span class="mut">{{ current.publishTime || current.createTime }}</span>
          </div>
          <h2 class="d-title">{{ current.title }}</h2>
          <p class="d-body">{{ current.content }}</p>
          <div class="d-foot">
            <el-button v-if="actionPath" size="small" type="primary" @click="goAction">去处理</el-button>
            <span v-else class="mut">该消息无需处理</span>
            <span v-if="current.effectiveTo" class="mut">有效期至 {{ current.effectiveTo }}</span>
          </div>
        </template>
        <div v-else class="empty tall">
          <i class="el-icon-chat-line-square" />
          <span>从左侧选一条消息查看详情</span>
        </div>
      </section>
    </div>

    <!-- ② 我的任务 -->
    <section v-show="tab === 'task'" class="card">
      <div class="card-h">
        <div class="tt"><span class="idx">任</span><h3>我的任务</h3></div>
        <div class="card-h-acts">
          <span class="mut">按截止时间排序</span>
          <el-button size="mini" icon="el-icon-refresh" :loading="taskLoading" @click="loadTasks">刷新</el-button>
        </div>
      </div>

      <div v-loading="taskLoading" class="task-list">
        <div
          v-for="t in tasks"
          :id="'task-card-' + t.id"
          :key="t.id"
          class="task"
          :class="[taskTone(t), { focused: highlightId === String(t.id) }]"
        >
          <div class="t-top">
            <b class="t-name">{{ t.taskName }}</b>
            <span class="tag" :class="taskTone(t)">{{ taskStatusText(t) }}</span>
          </div>
          <div class="t-sub">
            {{ t.positionName || '—' }} ·
            理论 {{ t.theoryHours || 0 }}h / 实践 {{ t.practiceHours || 0 }}h ·
            截止 {{ (t.deadline || '').slice(0, 16) }}
            <b v-if="t.remainDays !== null && t.remainDays !== undefined"
               :class="t.remainDays < 0 ? 'over' : (t.remainDays <= 3 ? 'soon' : '')">
              {{ t.remainDays < 0 ? '已逾期 ' + (-t.remainDays) + ' 天' : '剩 ' + t.remainDays + ' 天' }}
            </b>
          </div>
          <p v-if="t.target" class="t-target">{{ t.target }}</p>
          <div v-if="t.reviewComment" class="t-review">批语：{{ t.reviewComment }}</div>

          <!-- 任务资料：部门管理员上传，实习生只读下载（受控模式，数据来自一次 /my 请求） -->
          <TaskAttachments :task-id="t.id" :items="attMap[t.id] || []" compact class="t-att" />

          <div v-if="t.fileUrl" class="t-mine">
            我的作业附件：
            <a :href="fileUrl(t.fileUrl)" target="_blank" rel="noopener" download>{{ t.fileName || '附件' }}</a>
          </div>

          <div class="t-foot">
            <el-button v-if="canSubmit(t)" size="mini" type="primary" @click="openSubmit(t)">
              {{ t.submissionStatus ? '重新提交' : '交作业' }}
            </el-button>
            <el-button v-if="t.discussionEnabled === 1" size="mini" class="t-disc" @click="openDiscussion(t)">
              <i class="el-icon-chat-dot-round" /> 讨论<span v-if="t.postCount"> {{ t.postCount }}</span>
            </el-button>
            <span v-if="t.assignStatus === 'DONE'" class="mut">已完成</span>
            <!-- 任务设为「只允许提交一次」且已交过（未被退回）→ 说明原因，别让按钮凭空消失 -->
            <span v-else-if="blockedByOnce(t)" class="mut">
              <i class="el-icon-lock" /> 已提交，本任务只允许提交一次
            </span>
          </div>
        </div>
        <!-- 加载失败：必须把原因说出来 —— 否则「暂无任务」会让人以为是真没任务、
             从而去查数据（查半天发现库里明明有）。这是「进去是空的」最常见的假象来源。 -->
        <div v-if="!taskLoading && taskError" class="empty tall">
          <i class="el-icon-warning-outline" />
          <span>任务列表加载失败</span>
          <small class="err">{{ taskError }}</small>
          <el-button size="mini" icon="el-icon-refresh" @click="loadTasks">重试</el-button>
        </div>
        <div v-else-if="!taskLoading && !tasks.length" class="empty tall">
          <i class="el-icon-tickets" />
          <span>暂无任务</span>
          <small>任务按你的<b>岗位</b>分发 —— 部门管理员发布后才会出现在这里。</small>
        </div>
      </div>
    </section>

    <!-- 交作业 -->
    <el-dialog :title="'交作业 · ' + (curTask && curTask.taskName || '')" :visible.sync="submitVisible" width="560px" append-to-body>
      <el-form label-width="80px" size="small">
        <el-form-item label="作业内容">
          <el-input v-model="submitForm.content" type="textarea" :rows="6" placeholder="写下你的完成情况与说明" />
        </el-form-item>
        <el-form-item label="作业附件">
          <div class="up-box">
            <template v-if="submitForm.fileUrl">
              <i class="el-icon-paperclip" />
              <a class="up-file" :href="fileUrl(submitForm.fileUrl)" target="_blank" rel="noopener" download>
                {{ submitForm.fileName || '作业附件' }}
              </a>
              <span class="up-del" @click="clearSubmittedFile"><i class="el-icon-close" /> 移除</span>
            </template>
            <template v-else>
              <el-upload
                action=""
                :show-file-list="false"
                :http-request="doUploadSubmission"
                :before-upload="beforeSubmissionUpload"
                :disabled="uploadingSub"
              >
                <el-button size="mini" icon="el-icon-upload2" :loading="uploadingSub">选择文件上传</el-button>
              </el-upload>
              <span class="up-tip">支持 pdf/doc/ppt/xls/zip/图片，单个 ≤50MB</span>
            </template>
          </div>
        </el-form-item>
      </el-form>
      <div class="note">
        附件先单独上传（只落盘），点「提交」时才和作业内容一起写入 <code>task_submission</code>
        （重复提交走 <code>version + 1</code>），并<b>定向通知批阅人</b>。
        逾期仍可提交，但会标记为逾期。
      </div>
      <span slot="footer">
        <el-button size="small" @click="submitVisible = false">取消</el-button>
        <el-button size="small" type="primary" :loading="submitting" @click="doSubmit">提交</el-button>
      </span>
    </el-dialog>

    <!-- 讨论区（与部门管理员端共用组件；权限由后端响应驱动） -->
    <TaskDiscussion
      :visible="discVisible"
      :task-id="discTask && discTask.id"
      :task-name="discTask && discTask.taskName"
      @update:visible="onDiscToggle"
    />
  </div>
</template>

<script>
/**
 * 消息中心（实习生端 / 所有角色通用）
 *
 * P0 范围：只做「收」—— 列表 / 详情 / 已读 / 全部已读 / 公告位。
 * 「我的任务」页签为占位（任务域属 P2）。
 *
 * 三个要点：
 *  ① 打开详情即已读（后端幂等，重复打开不会重复写）；读后前端同步刷新未读。
 *  ② 未读与列表来自同一份送达谓词（后端 NoticeMapper 的 <sql id="visibleTo">），
 *     所以「铃铛数字」与「列表条数」永远一致。
 *  ③ 「去处理」由消息的 bizType + bizId 推导真实路由；P0 尚无业务锚点数据，
 *     解析不到时按钮不出现（而不是给一个死链）。
 */
import { listMyMessages, getMessage, readAllMessages } from '@/api/business/message'
import { listMyTasks, submitTask, listMyTaskAttachments, uploadSubmissionAsset } from '@/api/business/task'
import TaskDiscussion from '@/components/TaskDiscussion'
import TaskAttachments from '@/components/TaskAttachments'
import { mapGetters } from 'vuex'

const TYPE_STYLE = {
  ANNOUNCE: { label: '公告', bg: '#f2f4f7', fg: '#475467' },
  TASK: { label: '任务', bg: '#e8f1fd', fg: '#1764f5' },
  EXAM: { label: '考核', bg: '#f4eeff', fg: '#7a5af8' },
  AUDIT: { label: '审核', bg: '#fff4e5', fg: '#b54708' },
  URGE: { label: '催办', bg: '#feecea', fg: '#b42318' },
  SYSTEM: { label: '系统', bg: '#e6f7f5', fg: '#0e7490' }
}

/**
 * 业务锚点 → 前端路由，**按角色分流**。
 *
 * ⚠️ 消息中心是「所有角色共用」的页面（顶栏铃铛 / 用户菜单都能进），
 * 所以「去处理」绝不能写死成实习生路由 —— 部门管理员点过去会落到
 * 实习生视角的「我的任务」（`listMyTasks()` 对他返回空），这就是
 * 曾经「部门端和实习生端搞混」的根因。
 *
 * 约定：**返回空串 = 这个角色没有对应页面 → 按钮不显示**（不给死链）。
 * 另外 `actionPath` 还会用 `$router.resolve()` 复核一次路由真实存在
 * （dynamicRoutes 是按角色过滤的，别的角色的页面根本不在他的路由表里）。
 */
const ROLE_TARGET = {
  INTERN: {
    // 实习生：就在本页切到「我的任务」并定位到那张卡
    TASK: id => (id ? { path: '/messages', query: { tab: 'task', taskId: String(id) } } : ''),
    EXAM: id => ({ path: '/assessment/intern/learning/result', query: id ? { examId: String(id) } : undefined }),
    PROMOTION: () => ({ path: '/assessment/intern/learning/result' })
  },
  DEPT: {
    // 部门管理员：任务类消息的落点是「任务批阅」工作台并直接定位到该任务的批阅页签
    // （他自己的活口就是批阅，不是去看「我的任务」）
    TASK: id => (id
      ? { path: '/department/messages/review', query: { taskId: String(id), tab: 'submit' } }
      : ''),
    AUDIT: () => ({ path: '/department/people/register-review' }),
    PROMOTION: () => ({ path: '/department/people/promotion' }),
    EXAM: () => ({ path: '/department/study/exam' })
  },
  SUPER: {
    // 超管只读，且没有任务页；只有考核总览这类全局视图对他有意义
    EXAM: () => ({ path: '/super/ops/exams' })
  }
}

export default {
  name: 'MessageCenter',
  components: { TaskDiscussion, TaskAttachments },
  data() {
    return {
      loading: false,
      tab: 'notice',
      list: [],
      total: 0,
      unreadTotal: 0,
      current: null,
      // 我的任务
      taskLoading: false,
      tasks: [],
      attMap: {},
      taskError: '',
      highlightId: null,
      submitVisible: false,
      submitting: false,
      uploadingSub: false,
      submitForm: { content: '', fileUrl: '', fileName: '' },
      curTask: null,
      // 讨论区
      discVisible: false,
      discTask: null,
      query: { pageNum: 1, pageSize: 50, readFlag: '', msgType: '', title: '' },
      types: [
        { value: 'ANNOUNCE', label: '公告' },
        { value: 'TASK', label: '任务' },
        { value: 'EXAM', label: '考核' },
        { value: 'AUDIT', label: '审核' },
        { value: 'URGE', label: '催办' },
        { value: 'SYSTEM', label: '系统' }
      ]
    }
  },
  computed: {
    ...mapGetters(['roles']),
    /** 角色归类：决定「去处理」跳哪儿（同一个人只会命中一类） */
    roleKind() {
      const r = this.roles || []
      if (r.indexOf('PRE_TRAINEE') >= 0 || r.indexOf('FORMAL_TRAINEE') >= 0) return 'INTERN'
      if (r.indexOf('DEPT_ADMIN') >= 0) return 'DEPT'
      if (r.indexOf('SUPER_ADMIN') >= 0 || r.indexOf('admin') >= 0) return 'SUPER'
      return ''
    },
    isIntern() {
      return this.roleKind === 'INTERN'
    },
    actionPath() {
      const m = this.current
      if (!m) return ''
      // 消息自带的 linkUrl 优先（发送方显式指定），否则按「角色 × 业务类型」推导
      let loc = m.linkUrl
      if (!loc) {
        const table = ROLE_TARGET[this.roleKind] || {}
        const fn = table[m.bizType]
        loc = fn ? fn(m.bizId) : ''
      }
      if (!loc) return ''
      // 「解析不到真实路由就不给按钮」，而不是给一个死链。
      // dynamicRoutes 按角色过滤 → 别角色的页面在这里会是 matched=[] 而自动隐藏。
      const path = typeof loc === 'string' ? loc.split('?')[0] : loc.path
      try {
        if (!this.$router.resolve(path).route.matched.length) return ''
      } catch (e) {
        return ''
      }
      return loc
    }
  },
  watch: {
    // 同路由只换 query 时组件不重建 → created 不会重跑，必须显式监听
    '$route.query': {
      handler(q) { this.applyRoute(q || {}) }
    }
  },
  created() {
    const q = this.$route.query || {}
    // 「我的任务」是实习生专属页签；其他角色只收通知，任务类消息从「去处理」去各自的业务页
    if (this.isIntern && q.tab === 'task') this.tab = 'task'
    this.loadList()
    if (this.tab === 'task') this.loadTasksThenFocus(q)
  },
  methods: {
    // ---------------- 我的任务（仅实习生） ----------------
    /** 响应 ?tab=task&taskId=N：切页签 + 滚到那张卡并高亮（仅实习生） */
    applyRoute(q) {
      if (!this.isIntern) return
      if (q.tab !== 'task' && !q.taskId) return
      this.tab = 'task'
      this.loadTasksThenFocus(q)
    },
    loadTasksThenFocus(q) {
      this.loadTasks().then(() => {
        if (this.taskError || !q || !q.taskId) return
        // 通知里的任务可能不在自己的列表里（未分配 / 已结束 / 通知是按部门广播的）
        // → 必须说清楚，否则就是「点了去处理，进去一片空白」。
        if (!this.tasks.some(t => String(t.id) === String(q.taskId))) {
          this.$message.info('这条通知对应的任务不在你的任务列表里（可能未分配给你，或任务已结束）')
          return
        }
        this.focusTask(q.taskId)
      })
    },
    focusTask(taskId) {
      this.highlightId = String(taskId)
      this.$nextTick(() => {
        const el = document.getElementById('task-card-' + taskId)
        if (el && el.scrollIntoView) el.scrollIntoView({ block: 'center', behavior: 'smooth' })
        window.setTimeout(() => { this.highlightId = null }, 2600)
      })
    },
    loadTasks() {
      this.taskLoading = true
      this.taskError = ''
      return listMyTasks().then(res => {
        this.tasks = res.data || []
        this.loadMyAttachments()
      }).catch(err => {
        this.tasks = []
        this.attMap = {}
        // ★ 不要把失败吞成空列表 —— 那正是「点进去是空的」最常见的假象来源。
        //   RuoYi 响应拦截器的三种拒绝形态：
        //     ① code=500  → reject(new Error(服务端 msg))          → 直接用 message
        //     ② 其它非 200（如 403 未签协议）→ reject('error')      → 拦截器已弹 Notification，这里给可操作兜底
        //     ③ 网络层失败 → reject(error)，message='Network Error' → 换成中文
        const raw = err && err.message ? String(err.message) : ''
        this.taskError = (raw && raw !== 'Network Error' && !/timeout/i.test(raw))
          ? raw
          : '网络或服务异常，请重试；若反复失败，请确认已签署保密协议'
      }).finally(() => { this.taskLoading = false })
    },
    /** 我全部任务的资料：一次拉完本地按 taskId 分组（任务卡每张一次请求就是 N+1） */
    loadMyAttachments() {
      listMyTaskAttachments().then(res => {
        const map = {}
        ;(res.data || []).forEach(f => {
          if (!map[f.taskId]) map[f.taskId] = []
          map[f.taskId].push(f)
        })
        this.attMap = map
      }).catch(() => { this.attMap = {} })
    },
    /** 状态色：逾期红 / 将到期橙 / 完成绿 / 进行中蓝 */
    taskTone(t) {
      if (t.assignStatus === 'DONE') return 'green'
      if (t.remainDays !== null && t.remainDays !== undefined && t.remainDays < 0) return 'red'
      if (t.remainDays !== null && t.remainDays !== undefined && t.remainDays <= 3) return 'orange'
      return 'blue'
    },
    taskStatusText(t) {
      if (t.assignStatus === 'DONE') return '已完成'
      if (t.submissionStatus === 'SUBMITTED') return '待批阅'
      if (t.remainDays !== null && t.remainDays !== undefined && t.remainDays < 0) return '已逾期'
      if (t.assignStatus === 'IN_PROGRESS') return '进行中'
      return '未开始'
    },
    openSubmit(t) {
      if (!this.canSubmit(t)) {
        this.$message.warning('该任务只允许提交一次，你已提交过（被退回后才能重新提交）')
        return
      }
      this.curTask = t
      // 保留我上次提交的附件（「重新提交」时不用再传一遍），可手动移除
      this.submitForm = { content: '', fileUrl: t.fileUrl || '', fileName: t.fileName || '' }
      this.submitVisible = true
    },
    /**
     * 能否（再次）提交。
     * 任务设 `allowResubmit=0`（只允许一次）时的例外：**被退回可以重交** —— 退回本身就是要求重做。
     */
    canSubmit(t) {
      if (t.needAssignment !== 1 || t.assignStatus === 'DONE') return false
      if (t.allowResubmit === 1) return true
      if (!t.submissionStatus) return true
      return t.reviewStatus === 'REJECTED'
    },
    /** 被「只允许一次」挡住（用于给出原因文案，而不是让按钮凭空消失） */
    blockedByOnce(t) {
      return t.needAssignment === 1 && t.assignStatus !== 'DONE' &&
        t.allowResubmit === 0 && !!t.submissionStatus && t.reviewStatus !== 'REJECTED'
    },
    /** 附件上传前的本地预校验（后端仍会再校验一次） */
    beforeSubmissionUpload(file) {
      const ext = (file.name.split('.').pop() || '').toLowerCase()
      const ok = ['pdf', 'doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx', 'txt', 'md', 'csv',
        'zip', 'rar', '7z', 'png', 'jpg', 'jpeg', 'gif']
      if (ok.indexOf(ext) < 0) { this.$message.warning('不支持的文件类型：' + ext); return false }
      if (file.size > 50 * 1024 * 1024) { this.$message.warning('单个文件不能超过 50MB'); return false }
      return true
    },
    /** 附件先单独上传（只落盘不落库），拿到路径后随「提交」一起写库 */
    doUploadSubmission(options) {
      const form = new FormData()
      form.append('file', options.file)
      this.uploadingSub = true
      uploadSubmissionAsset(this.curTask.id, form).then(res => {
        const d = res.data || {}
        this.submitForm.fileUrl = d.fileUrl || ''
        this.submitForm.fileName = d.fileName || options.file.name
        this.$message.success('附件已上传')
      }).catch(() => {}).finally(() => { this.uploadingSub = false })
    },
    clearSubmittedFile() {
      this.submitForm.fileUrl = ''
      this.submitForm.fileName = ''
    },
    doSubmit() {
      if (!(this.submitForm.content || '').trim() && !(this.submitForm.fileUrl || '').trim()) {
        this.$message.warning('作业内容与附件至少填一个')
        return
      }
      this.submitting = true
      submitTask(this.curTask.id, this.submitForm).then(res => {
        this.$message.success(res.msg || '已提交')
        this.submitVisible = false
        this.loadTasks()
      }).catch(() => {}).finally(() => { this.submitting = false })
    },
    /** 附件下载地址：必须走 VUE_APP_BASE_API 前缀（dev 下由代理转发到 8080） */
    fileUrl(u) {
      if (!u) return '#'
      return process.env.VUE_APP_BASE_API + u
    },
    typeStyle(t) {
      return TYPE_STYLE[t] || TYPE_STYLE.SYSTEM
    },
    /** 打开讨论区；关闭时重拉任务列表，让「讨论 N」角标跟上 */
    openDiscussion(t) {
      this.discTask = t
      this.discVisible = true
    },
    onDiscToggle(v) {
      this.discVisible = v
      if (!v) this.loadTasks()
    },
    switchTab(t) {
      this.tab = t
      if (t === 'task' && !this.tasks.length) {
        this.loadTasks()
      }
    },
    setRead(flag) {
      this.query.readFlag = flag
      this.loadList()
    },
    loadList() {
      this.loading = true
      listMyMessages(this.query).then(res => {
        const d = res.data || {}
        this.list = d.rows || []
        this.total = d.total || 0
        this.unreadTotal = Number((d.unreadByType || {}).total || 0)
        // 详情若已不在列表里（切换筛选后），清空避免误读
        if (this.current && !this.list.some(m => m.id === this.current.id)) {
          this.current = null
        }
      }).catch(() => { this.list = []; this.total = 0 }).finally(() => { this.loading = false })
    },
    openDetail(m) {
      getMessage(m.id).then(res => {
        this.current = res.data || null
        if (this.current && !this.current.readTime) {
          this.current.readTime = new Date()
        }
        // 本地同步该条的已读态，避免整表重拉
        const hit = this.list.find(x => x.id === m.id)
        if (hit && !hit.readTime) {
          hit.readTime = '已读'
          this.unreadTotal = Math.max(0, this.unreadTotal - 1)
        }
      }).catch(() => {})
    },
    markAll() {
      readAllMessages(null).then(res => {
        this.$modal ? this.$modal.msgSuccess(res.msg || '已全部标记为已读') : this.$message.success(res.msg || '已全部标记为已读')
        this.current = null
        this.loadList()
      }).catch(() => {})
    },
    goAction() {
      const target = this.actionPath
      if (!target) return
      // 同一路由重复点击时 $router.push 会抛 NavigationDuplicated，静默吞掉；
      // 同时显式跑一次 applyRoute —— 否则「已经在该任务上再点一次」会毫无反应。
      this.$router.push(target).catch(() => {})
      if (typeof target === 'object' && target.query) this.applyRoute(target.query)
    }
  }
}
</script>

<style lang="scss" scoped>
/* 自包含样式：不 @import 共享基线，避免 scoped 下 :root 失效的坑 */
.msg-page {
  padding: 18px 20px 32px;
  background: #eef2f7;
  min-height: calc(100vh - 84px);
}
.mh {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  background: #fff;
  border: 1px solid #e4e9f0;
  border-radius: 12px;
  padding: 18px 20px;
  margin-bottom: 14px;

  .eyebrow { color: #98a2b3; font-size: 11px; letter-spacing: .6px; }
  h1 { margin: 4px 0 6px; font-size: 20px; color: #1d2939; }
  p { margin: 0; color: #667085; font-size: 12.5px; line-height: 1.7; max-width: 640px; }
  .mh-actions { display: flex; align-items: center; gap: 8px; flex: none; }
}
.chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 26px;
  padding: 0 10px;
  border-radius: 13px;
  font-size: 12px;

  &.warn { background: #feecea; color: #b42318; }
  &.ok { background: #e7f7ef; color: #027a48; }
}
.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;

  span {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    height: 32px;
    padding: 0 14px;
    border-radius: 8px;
    background: #fff;
    border: 1px solid #e4e9f0;
    color: #344054;
    font-size: 13px;
    cursor: pointer;
    transition: all .2s;

    &:hover { border-color: #bcd4fb; color: #1764f5; }
    &.on { background: #1764f5; border-color: #1764f5; color: #fff; }

    .dot {
      min-width: 16px;
      height: 16px;
      padding: 0 4px;
      border-radius: 8px;
      background: #f04438;
      color: #fff;
      font-size: 10px;
      line-height: 16px;
      text-align: center;
    }
  }
}
.grid {
  display: grid;
  grid-template-columns: minmax(0, 5fr) minmax(0, 7fr);
  gap: 14px;

  @media (max-width: 1100px) { grid-template-columns: 1fr; }
}
.card {
  background: #fff;
  border: 1px solid #e4e9f0;
  border-radius: 12px;
  padding: 16px 18px;
}
.card-h {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;

  .tt { display: flex; align-items: center; gap: 8px; }
  .idx {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 22px;
    height: 22px;
    border-radius: 6px;
    background: #e8f1fd;
    color: #1764f5;
    font-size: 12px;
  }
  h3 { margin: 0; font-size: 14px; color: #1d2939; }
}
.card-h-acts { display: flex; align-items: center; gap: 8px; }
.mut { color: #98a2b3; font-size: 12px; }
.badge {
  height: 22px;
  padding: 0 8px;
  border-radius: 6px;
  background: #fff4e5;
  color: #b54708;
  font-size: 11px;
  line-height: 22px;
}
.filter {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}
.seg {
  display: inline-flex;
  border: 1px solid #e4e9f0;
  border-radius: 8px;
  overflow: hidden;

  span {
    padding: 0 12px;
    height: 28px;
    line-height: 28px;
    font-size: 12px;
    color: #667085;
    cursor: pointer;

    &.on { background: #e8f1fd; color: #1764f5; }
  }
}
.list { max-height: 560px; overflow-y: auto; }
.item {
  padding: 10px 12px;
  border-left: 3px solid transparent;
  border-bottom: 1px solid #f2f4f7;
  cursor: pointer;
  transition: background .2s;

  &:hover { background: #f7f9fc; }
  /* 未读：红点 + 左侧蓝条 + 淡蓝底（三重提示，扫一眼就能定位） */
  &.unread {
    border-left-color: #1764f5;
    background: #f7faff;

    .it-title { color: #1d2939; }
    .it-sub { color: #667085; }
  }
  /* 已读：标题压暗，和未读拉开层次 —— 红点移掉之后仍能一眼分辨 */
  &:not(.unread) .it-title { color: #667085; font-weight: 500; }
  &.active { background: #e8f1fd; }

  .it-top { display: flex; align-items: center; gap: 8px; }
  .it-title {
    flex: 1;
    min-width: 0;
    font-size: 13px;
    color: #1d2939;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  /* 未读红点：8px 实心 + 一圈淡晕，避免在小字号列表里被忽略 */
  .reddot {
    flex: none;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #f04438;
    box-shadow: 0 0 0 3px rgba(240, 68, 56, .14);
  }
  .pin {
    flex: none;
    padding: 0 6px;
    height: 18px;
    line-height: 18px;
    border-radius: 4px;
    background: #feecea;
    color: #b42318;
    font-size: 10px;
  }
  .it-sub { margin-top: 5px; color: #98a2b3; font-size: 11.5px; }
}
.tag {
  flex: none;
  padding: 0 7px;
  height: 20px;
  line-height: 20px;
  border-radius: 5px;
  font-size: 11px;
}
.d-top { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.d-title { margin: 12px 0 10px; font-size: 16px; color: #1d2939; }
.d-body {
  margin: 0;
  color: #344054;
  font-size: 13px;
  line-height: 1.9;
  white-space: pre-wrap;
}
.d-foot {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px solid #f2f4f7;
}
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 34px 0;
  color: #98a2b3;
  font-size: 12.5px;

  i { font-size: 26px; color: #d0d5dd; }
  small { color: #b0b8c4; font-size: 11.5px; }
  small.err { max-width: 420px; text-align: center; color: #b54708; line-height: 1.7; }
  &.tall { padding: 70px 0; }
}

/* ---- 我的任务 ---- */
.task-list { min-height: 120px; }
.task {
  border: 1px solid #e4e9f0;
  border-left: 3px solid #1764f5;
  border-radius: 8px;
  padding: 12px 14px;
  margin-bottom: 10px;

  &.green { border-left-color: #12b76a; opacity: .88; }
  &.orange { border-left-color: #f79009; border-color: #fde3c0; }
  &.red { border-left-color: #f04438; border-color: #f09595; }

  .t-top { display: flex; align-items: center; gap: 8px; }
  .t-name { flex: 1; min-width: 0; font-size: 13.5px; color: #1d2939; }
  .t-sub { margin-top: 6px; color: #98a2b3; font-size: 11.5px; }
  .t-sub .over { color: #b42318; margin-left: 6px; }
  .t-sub .soon { color: #b54708; margin-left: 6px; }
  .t-target { margin: 8px 0 0; color: #475467; font-size: 12.5px; line-height: 1.7; }
  .t-review {
    margin-top: 8px; padding: 8px 10px; border-radius: 6px;
    background: #f7f9fc; color: #475467; font-size: 12px;
  }
  .t-foot { margin-top: 10px; display: flex; align-items: center; gap: 10px; }
}
.t-disc { color: #475467; }
.t-disc:hover { color: #1764f5; border-color: #bcd4fb; }

/* 交作业：附件上传区 */
.up-box { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }.up-file { color: #1764f5; font-size: 12.5px; text-decoration: none; }
.up-file:hover { text-decoration: underline; }
.up-del { color: #98a2b3; font-size: 12px; cursor: pointer; }
.up-del:hover { color: #b42318; }
.up-tip { color: #98a2b3; font-size: 11.5px; }

/* 弹窗底部的说明段（原先这页没定义，.note 一直是裸文本） */
.note {
  padding: 9px 11px; margin-top: 2px;
  background: #f7f9fc; border: 1px solid #eef1f6; border-radius: 8px;
  color: #667085; font-size: 11.5px; line-height: 1.8;
}
code { padding: 1px 5px; color: #344054; font-size: 11.5px; background: #f2f4f7; border-radius: 4px; }

/* 任务卡上的资料与我提交的附件 */
.t-att { margin-top: 8px; }
.t-mine {
  margin-top: 8px; padding-top: 8px; border-top: 1px dashed #eaecf0;
  color: #667085; font-size: 12px;
  a { color: #1764f5; text-decoration: none; }
  a:hover { text-decoration: underline; }
}
/* 「去处理」跳过来时的高亮，2.6s 后自动褪去 */
.task.focused {
  box-shadow: 0 0 0 3px #bcd4fb;
  transition: box-shadow .3s;
}
.tag.blue { background: #e8f1fd; color: #1764f5; }
.tag.orange { background: #fff4e5; color: #b54708; }
.tag.green { background: #e7f7ef; color: #027a48; }
.tag.red { background: #feecea; color: #b42318; }
</style>
