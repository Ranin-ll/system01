<template>
  <div class="dept-page">
    <div class="dept-breadcrumb">
      任务与通知 <span>/</span> <b>任务管理</b>
    </div>

    <div class="dept-heading">
      <div>
        <span class="eyebrow">DEPARTMENT ADMIN</span>
        <h1>任务管理</h1>
        <p>
          按<b>岗位</b>发布学习任务，发布即展开分配到本部门该岗位的在培实习生，并自动推送一条任务通知。
          实习生端「消息中心 › 我的任务」是同一个数据源。
        </p>
      </div>
      <div class="dept-heading-actions">
        <span class="dbadge green">真实数据</span>
        <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="loadTasks">刷新</el-button>
      </div>
    </div>

    <div class="dgrid">
      <!-- 发布任务 -->
      <div class="dcard c5">
        <div class="dcard-h">
          <div class="tt"><span class="idx">发</span><h3>发布任务</h3></div>
        </div>

        <div class="dfield">
          <label>任务名称 <b>*</b></label>
          <el-input v-model="form.taskName" size="small" maxlength="80" show-word-limit placeholder="例如：完成 Docker 薄弱知识点补学" />
        </div>

        <div class="dfg2" style="margin-top:12px">
          <div class="dfield">
            <label>发布对象 <b>*</b></label>
            <el-select v-model="form.assignScope" size="small" style="width:100%" @change="onScopeChange">
              <el-option label="本部门全体" value="DEPT" />
              <el-option label="按岗位" value="POSITION" />
              <el-option label="指定人员" value="USER" />
            </el-select>
          </div>
          <div v-if="form.assignScope === 'POSITION'" class="dfield">
            <label>适用岗位 <b>*</b></label>
            <el-select v-model="form.positionId" size="small" style="width:100%" placeholder="本部门岗位">
              <el-option v-for="p in positions" :key="p.id" :label="p.positionName" :value="Number(p.id)" />
            </el-select>
          </div>
          <div class="dfield">
            <label>关联课程</label>
            <el-select v-model="form.courseId" size="small" style="width:100%" clearable placeholder="可不选">
              <el-option v-for="c in courses" :key="c.id" :label="c.courseName" :value="Number(c.id)" />
            </el-select>
          </div>
          <div class="dfield">
            <label>理论学时</label>
            <el-input-number v-model="form.theoryHours" :min="0" :max="200" size="small" controls-position="right" style="width:100%" />
          </div>
          <div class="dfield">
            <label>实践学时</label>
            <el-input-number v-model="form.practiceHours" :min="0" :max="200" size="small" controls-position="right" style="width:100%" />
          </div>
          <div class="dfield">
            <label>截止时间 <b>*</b></label>
            <el-date-picker v-model="form.deadline" type="datetime" size="small"
                            value-format="yyyy-MM-dd HH:mm:ss" placeholder="选择截止时间" style="width:100%" />
          </div>
          <div class="dfield">
            <label>开始时间</label>
            <el-date-picker v-model="form.startTime" type="datetime" size="small"
                            value-format="yyyy-MM-dd HH:mm:ss" placeholder="可不选" style="width:100%" />
          </div>
        </div>

        <div class="dfield">
          <label>任务目标</label>
          <el-input v-model="form.target" type="textarea" :rows="3" size="small"
                    placeholder="一句话说明要达成什么，例如：掌握镜像分层与容器网络基础" />
        </div>

        <!-- 按部门：本部门全部在培实习生（顺带把「将发出多少人」摆出来，避免误发） -->
        <div v-if="form.assignScope === 'DEPT'" class="dfield">
          <span class="hint-text">
            <i class="el-icon-info" />
            将发给<b>本部门全部在培实习生 {{ allTrainees.length }} 人</b>（不分岗位）。
            任务不绑定岗位 —— 岗位只影响课程可见性，与本任务的发布对象无关。
          </span>
        </div>

        <!-- 指定人员：本部门任意岗位的在培实习生都能选（任务不再限定单一岗位） -->
        <div v-if="form.assignScope === 'USER'" class="dfield">
          <label>指定实习生 <b>*</b></label>
          <el-select
            v-model="form.targetUserIds" multiple filterable size="small" style="width:100%"
            placeholder="搜索本部门在培实习生"
          >
            <el-option
              v-for="u in allTrainees" :key="u.userId"
              :label="(u.nickName || u.userName) + '（' + (u.positionName || '未分岗') + '·' + (String(u.userStatus) === 'FORMAL_TRAINEE' ? '正式' : '预备') + '）'"
              :value="Number(u.userId)"
            />
          </el-select>
          <p class="hint-text" style="margin:6px 0 0">
            只分配给勾选的人；发布后不可改（需先结束任务）。可选范围为<b>本部门全部在培实习生</b>，不受岗位限制。
          </p>
        </div>
        <div class="dfield">
          <label>任务说明</label>
          <el-input v-model="form.description" type="textarea" :rows="5" size="small"
                    placeholder="请针对本场考核暴露的 Docker 薄弱点，重新学习「容器基础」与「镜像分层」两节，并写一份不少于 300 字的总结。" />
        </div>

        <div class="dchkrow" @click="form.needAssignment = form.needAssignment ? 0 : 1">
          <span class="box" :class="{ sw: form.needAssignment === 1 }"><i class="el-icon-check" /></span>
          <span>需要提交作业（实习生端出现「交作业」入口）</span>
        </div>
        <div class="dchkrow" @click="form.discussionEnabled = form.discussionEnabled ? 0 : 1">
          <span class="box" :class="{ sw: form.discussionEnabled === 1 }"><i class="el-icon-check" /></span>
          <span>启用讨论区（本岗位实习生可在任务下提问、你可在讨论区答疑）</span>
        </div>
        <div class="dchkrow" @click="form.allowResubmit = form.allowResubmit ? 0 : 1">
          <span class="box" :class="{ sw: form.allowResubmit === 1 }"><i class="el-icon-check" /></span>
          <span>允许多次提交（<b>不勾则每人只能交一次</b>；被退回的仍可重新提交）</span>
        </div>

        <!-- 任务资料：附件挂在任务上，所以必须先有 taskId（存草稿即生成） -->
        <div class="dsep" />
        <TaskAttachments ref="formAtt" :task-id="form.id" editable @changed="loadTasks" />

        <div class="dfield" style="margin-top:12px">
          <label>批阅人</label>
          <el-input :value="nickName + '（我自己）'" size="small" disabled />
        </div>

        <div class="dbtn-row right" style="margin-top:14px">
          <el-button v-if="form.id" size="small" @click="resetForm">新建</el-button>
          <el-button size="small" :loading="saving" @click="save(false)">{{ form.id ? '保存草稿' : '存草稿' }}</el-button>
          <el-button size="small" type="primary" :loading="publishing" @click="save(true)">
            {{ form.id ? '发布并通知' : '保存并发布' }}
          </el-button>
        </div>
      </div>

      <!-- 任务跟踪 -->
      <div class="dcard c7">
        <div class="dcard-h">
          <div class="tt"><span class="idx">跟</span><h3>任务跟踪</h3></div>
          <div class="track-filters">
            <TraineeSelect v-model="filterUserId" width="148px" @change="loadTasks" @loaded="trainees = $event" />
            <div class="dchips" style="margin:0">
              <span v-for="tab in trackTabs" :key="tab.key" class="dchip"
                    :class="{ on: trackFilter === tab.key }" @click="trackFilter = tab.key">
                {{ tab.label }} <span class="n">{{ tab.count }}</span>
              </span>
            </div>
          </div>
        </div>
        <p v-if="filterUserId" class="track-hint">
          <i class="el-icon-user" /> 只看 <b>{{ filterUserName }}</b> 被分配到的任务；「分配/提交」列右侧是他本人的状态。
          <el-button type="text" @click="clearFilter">取消筛选</el-button>
        </p>
        <table class="dtbl">
          <thead>
            <tr>
              <th>任务</th>
              <th style="width:96px">岗位</th>
              <th style="width:74px">状态</th>
              <th style="width:112px">截止</th>
              <th style="width:112px">分配/提交</th>
              <th style="width:150px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in visibleTasks" :key="row.id" class="row-click" title="点击查看任务详情" @click="openDetail(row)">
              <td>
                <span class="strong">{{ row.taskName }}</span>
                <!-- 任务自身及其资源（讨论/资料）跟在任务名下面；操作列只放「对任务的动作」，避免 6 个链接挤成一团 -->
                <div class="rchips">
                  <span v-if="row.deadline && overdue(row)" class="overdue">已逾期</span>
                  <!-- 发布对象：按部门 / 按岗位 / 指定人员，三档都要能一眼看出来 -->
                  <span v-if="row.assignScope === 'DEPT'" class="chip static" title="本部门全部在培实习生">
                    <i class="el-icon-s-home" /> 本部门全体
                  </span>
                  <span v-else-if="row.assignScope === 'USER'" class="chip static" :title="row.targetNames || ''">
                    <i class="el-icon-user" /> 指定 {{ (row.targetNames || '').split(',').filter(Boolean).length }} 人
                  </span>
                  <span v-else-if="row.positionName" class="chip static" :title="'按岗位：' + row.positionName">
                    <i class="el-icon-postcard" /> {{ row.positionName }}
                  </span>
                  <a v-if="row.discussionEnabled === 1" class="chip" @click.stop="goReview(row, 'discuss')">
                    <i class="el-icon-chat-dot-round" /> 讨论{{ row.postCount ? ' ' + row.postCount : '' }}
                  </a>
                  <a class="chip" @click.stop="goReview(row, 'files')">
                    <i class="el-icon-paperclip" /> 资料{{ row.attachmentCount ? ' ' + row.attachmentCount : '' }}
                  </a>
                </div>
              </td>
              <td>{{ row.positionName || '—' }}</td>
              <td>
                <span class="dbadge" :class="statusTone(row.status)">{{ statusText(row.status) }}</span>
              </td>
              <td>{{ shortTime(row.deadline) }}</td>
              <td>
                {{ row.assignedCount || 0 }} / {{ row.submittedCount || 0 }}
                <!-- 按实习生筛选时，这一列额外显示 TA 本人的分配状态 -->
                <span v-if="row.assignStatus" class="dbadge" :class="assignTone(row.assignStatus)">
                  {{ assignText(row.assignStatus) }}
                </span>
                <span v-if="row.pendingReviewCount" class="dbadge orange" style="margin-left:4px">待批 {{ row.pendingReviewCount }}</span>
              </td>
              <td>
                <!-- @click.stop：这一格里的按钮是「对任务的动作」，不能连带触发整行的「查看详情」 -->
                <div class="acts" @click.stop>
                  <template v-if="row.status === 'DRAFT'">
                    <el-button type="text" @click="publish(row)">发布</el-button>
                    <span class="sep">|</span>
                  </template>
                  <el-button type="text" @click="goReview(row, 'submit')">批阅</el-button>
                  <template v-if="row.status === 'PUBLISHED'">
                    <span class="sep">|</span>
                    <el-button type="text" @click="urge(row)">催办</el-button>
                    <span class="sep">|</span>
                    <el-button type="text" @click="endTask(row)">结束</el-button>
                  </template>
                </div>
              </td>
            </tr>
            <tr v-if="!loading && !visibleTasks.length">
              <td colspan="6">
                <div class="dempty small">
                  <i class="el-icon-tickets" />
                  <strong>没有符合条件的任务</strong>
                  <span>换个筛选条件试试，或在左侧发布一条新任务。</span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <p class="dsec-note">
          <b>发布 = 一个事务</b>：任务置为已发布 + <b>按发布对象展开分配</b>（本部门全体 / 该岗位全部 / 指定人员，写进 <code>task_assignment</code>，唯一键保证不重复）+ 定向通知到人。
          只有「按岗位」发布的任务会绑定岗位（<code>position_id</code>）；按部门 / 按人发的任务不绑岗位，归属由发布人所属部门判定。
          「分配/提交」= 分配人数 / 已提交人数；「待批」= 已提交但未批阅。
        </p>
      </div>
    </div>

    <div class="dcallout" style="margin-top:16px">
      <i class="el-icon-success" />
      <span>
        <b>本页只负责「建 / 发布 / 结束 / 催办」</b>。<b>点任务行任意位置可看任务详情</b>。
        提交明细、批阅、任务资料、讨论区已移到
        <router-link to="/department/messages/review" style="color:#1764f5">任务批阅</router-link> 页
        —— 那里有完成情况统计，且全部内联，不再弹窗。
        接口：<code>POST /business/task/save</code>、<code>POST /business/task/{id}/status</code>、
        <code>GET /business/task/list</code>；<b>催办</b>走通知接口给未提交者发定向通知。
      </span>
    </div>

    <!-- 任务详情：点任务行任意位置打开（操作列的按钮已 @click.stop，不会误触发）
         ⚠️ 本页「已逾期」的判定方法叫 overdue() 而不是 isOverdue()（那是 review.vue 的名字）。
         曾因为在这里误写成 isOverdue，「点了没反应」排查了很久：**模板里调用不存在的方法会让渲染期抛错，
         整个 patch 被中止 —— 数据改了、DOM 静默停在旧状态，表现和「不响应」一模一样**。
         改这类模板时，先确认每个方法名都真实存在。 -->
    <el-dialog
      :title="'任务详情 · ' + (detail && detail.taskName || '')"
      :visible.sync="detailVisible"
      width="700px"
      append-to-body
    >
      <template v-if="detail">
        <div class="dt-head">
          <span class="dbadge" :class="statusTone(detail.status)">{{ statusText(detail.status) }}</span>
          <span v-if="overdue(detail)" class="dbadge red">已逾期</span>
          <span v-if="detail.discussionEnabled === 1" class="dbadge blue">启用讨论区</span>
          <span v-if="detail.needAssignment === 1" class="dbadge gray">需提交作业</span>
          <span v-if="detail.needAssignment === 1 && detail.allowResubmit === 0" class="dbadge orange">仅一次提交</span>
          <span class="hint-text" style="margin-left:auto">创建于 {{ fullTime(detail.createTime) }}</span>
        </div>

        <div class="dt-kv">
          <div><label>发布对象</label><span>{{ scopeLabel(detail) }}</span></div>
          <div><label>关联课程</label><span>{{ courseName(detail.courseId) }}</span></div>
          <div><label>理论 / 实践学时</label><span>{{ detail.theoryHours || 0 }}h / {{ detail.practiceHours || 0 }}h</span></div>
          <div><label>开始时间</label><span>{{ fullTime(detail.startTime) }}</span></div>
          <div><label>截止时间</label><span>{{ fullTime(detail.deadline) }}</span></div>
            <div><label>发布人 / 批阅人</label><span>{{ detail.publisherName || '—' }} / {{ detail.reviewerName || '—' }}</span></div>
            <div><label>提交方式</label><span>{{ detail.needAssignment !== 1 ? '无需提交作业' : (detail.allowResubmit === 0 ? '仅一次（被退回可重交）' : '可多次提交') }}</span></div>
          </div>

        <div class="dt-sec">
          <label>完成情况</label>
          <div class="dt-stats">
            <span>分配 <b>{{ detail.assignedCount || 0 }}</b> 人</span>
            <span>已提交 <b>{{ detail.submittedCount || 0 }}</b> 人</span>
            <span>待批阅 <b>{{ detail.pendingReviewCount || 0 }}</b> 份</span>
            <span>讨论 <b>{{ detail.postCount || 0 }}</b> 条</span>
            <span>资料 <b>{{ detail.attachmentCount || 0 }}</b> 份</span>
            <!-- 仅在「按实习生筛选」时后端才会带出这个人的状态 -->
            <span v-if="detail.assignStatus" class="dt-mine">
              当前筛选对象：<b>{{ assignText(detail.assignStatus) }}</b>
            </span>
          </div>
        </div>

        <div class="dt-sec">
          <label>任务目标</label>
          <p>{{ detail.target || '（未填写）' }}</p>
        </div>
        <div class="dt-sec">
          <label>任务说明</label>
          <p class="pre">{{ detail.description || '（未填写）' }}</p>
        </div>
      </template>

      <span slot="footer">
        <el-button size="small" @click="detailVisible = false">关闭</el-button>
        <el-button size="small" type="primary" @click="goReview(detail, 'submit')">去任务批阅</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
/**
 * 部门管理员端 · 任务管理（2026-09-18 收敛为「只管任务本身」）
 *
 * 与演示态的差异（按真实模型重写）：
 *  - 真实表没有「任务类型 / 优先级」；任务**按岗位分发**，没有「指定人员/智能选人」
 *  - 发布是事务：置 PUBLISHED + 按岗位展开 task_assignment + 推送 TASK 通知
 *  - 催办 = 给未提交者发定向通知
 *
 * ★ 2026-09-18 结构调整：原来这里的「提交情况 / 批阅 / 资料 / 讨论」四个弹窗**全部删掉**，
 *   统一收到「任务批阅」页（`/department/messages/review`）——那页有完成情况统计，
 *   并且全部内联呈现。本页只保留「建 / 发布 / 结束 / 催办」，跳转都带 ?taskId 与页签。
 *
 * 保留既有 d* 结构与 department-module.scss。
 */
import { listTasks, saveTask, changeTaskStatus, listSubmissions, listTrainees } from '@/api/business/task'
import { listCoursePositions, listCourse } from '@/api/business/course'
import { sendMessage } from '@/api/business/message'
import TaskAttachments from '@/components/TaskAttachments'
import TraineeSelect from '@/components/TraineeSelect'
import { mapGetters } from 'vuex'

const STATUS_TEXT = { DRAFT: '草稿', PUBLISHED: '已发布', ENDED: '已结束' }
const ASSIGN_TEXT = { NOT_STARTED: '未开始', IN_PROGRESS: '进行中', DONE: '已完成', OVERDUE: '已逾期' }


export default {
  name: 'DeptTasks',
  components: { TaskAttachments, TraineeSelect },
  data() {
    return {
      loading: false,
      saving: false,
      publishing: false,
      positions: [],
      courses: [],
      tasks: [],
      trackFilter: 'ALL',
      /** 按实习生筛选（null = 不筛）：后端只返回分配给 TA 的任务 */
      filterUserId: null,
      trainees: [],
      /** 本部门在培实习生（供「指定人员」下拉按岗位过滤） */
      allTrainees: [],
      /** 任务详情弹窗 */
      detail: null,
      detailVisible: false,
      form: this.blankForm()
    }
  },
  computed: {
    ...mapGetters(['nickName', 'name', 'deptId']),
    /** 「发布对象」的中文名（任务卡 / 详情 / 筛选提示共用） */
    scopeText() {
      return { DEPT: '本部门全体', POSITION: '按岗位', USER: '指定人员' }[this.form.assignScope] || '按岗位'
    },
    filterUserName() {
      const u = this.trainees.find(x => Number(x.userId) === Number(this.filterUserId))
      return u ? (u.nickName || u.userName) : ''
    },
    trackTabs() {
      return [
        { key: 'ALL', label: '全部', count: this.tasks.length },
        { key: 'DRAFT', label: '草稿', count: this.tasks.filter(t => t.status === 'DRAFT').length },
        { key: 'PUBLISHED', label: '已发布', count: this.tasks.filter(t => t.status === 'PUBLISHED').length },
        { key: 'ENDED', label: '已结束', count: this.tasks.filter(t => t.status === 'ENDED').length }
      ]
    },
    visibleTasks() {
      if (this.trackFilter === 'ALL') return this.tasks
      return this.tasks.filter(t => t.status === this.trackFilter)
    }
  },
  watch: {
    // 从「按岗位」切到别的档位时，把已选岗位清掉 —— 否则残留的 positionId 会被后端
    // 当成「按岗位」发布的对象（后端只在 assignScope=POSITION 时才读它）
    'form.assignScope'(v) {
      if (v !== 'POSITION') this.form.positionId = null
    }
  },
  created() {
    this.loadBase()
    this.loadTasks()
  },
  methods: {
    blankForm() {
      return {
        id: null,
        taskName: '', positionId: null, courseId: null,
        theoryHours: 0, practiceHours: 0,
        startTime: null, deadline: null,
        target: '', description: '',
        needAssignment: 1, discussionEnabled: 0,
        allowResubmit: 1,
        // 发布对象缺省「按岗位」—— 与改动前一致；切档位时 positionId 会被 watch 清掉
        assignScope: 'POSITION', targetUserIds: []
      }
    },
    /** 切「发布对象」：按人 / 按部门都不需要选岗位，顺手清掉残留值 */
    onScopeChange(v) {
      if (v !== 'POSITION') {
        this.form.positionId = null
      } else {
        this.form.targetUserIds = []
      }
    },
    /** 从「正在编辑的草稿」回到新建态 */
    resetForm() {
      this.form = this.blankForm()
      this.$nextTick(() => { if (this.$refs.formAtt) this.$refs.formAtt.reload() })
    },
    loadBase() {
      listCoursePositions().then(res => { this.positions = res.data || [] }).catch(() => { this.positions = [] })
      listCourse({ pageNum: 1, pageSize: 200 }).then(res => { this.courses = res.rows || [] }).catch(() => { this.courses = [] })
      listTrainees().then(res => { this.allTrainees = res.data || [] }).catch(() => { this.allTrainees = [] })
    },
    loadTasks() {
      this.loading = true
      return listTasks({ pageNum: 1, pageSize: 50, userId: this.filterUserId || undefined }).then(res => {
        this.tasks = (res.data || {}).rows || []
      }).catch(() => { this.tasks = [] }).finally(() => { this.loading = false })
    },
    clearFilter() {
      this.filterUserId = null
      this.loadTasks()
    },
    /** 点任务行 → 看这条任务的全部信息（操作列的按钮已 @click.stop，不会误触发） */
    openDetail(row) {
      if (!row) return
      this.detail = row
      this.detailVisible = true
    },
    closeDetail() {
      this.detailVisible = false
    },
    /** 关联课程名：列表里只有 courseId，用本页已加载的课程下拉反解 */
    courseName(id) {
      if (!id) return '未关联'
      const c = this.courses.find(x => Number(x.id) === Number(id))
      return c ? (c.courseName || ('课程 #' + id)) : ('课程 #' + id)
    },
    /** 任务详情的「发布对象」文案：三档各有各的说明 */
    scopeLabel(row) {
      const scope = row.assignScope || 'POSITION'
      if (scope === 'DEPT') return '本部门全体在培实习生'
      if (scope === 'USER') return '指定人员：' + (row.targetNames || '—')
      return '按岗位：' + (row.positionName || '—') + '（该岗位全部在培实习生）'
    },
    /** 完整时间：yyyy-MM-dd HH:mm */
    fullTime(v) {
      if (!v) return '—'
      return String(v).replace('T', ' ').slice(0, 16)
    },
    /** 跳到「任务批阅」页并定位到某条任务的某个页签（取代原来的四个弹窗） */
    goReview(row, tab) {
      if (!row) return
      this.$router.push({
        path: '/department/messages/review',
        query: { taskId: String(row.id), tab: tab || 'submit' }
      }).catch(() => {})
    },
    /** 存草稿 / 发布并通知 */
    save(andPublish) {
      const f = this.form
      if (!f.taskName.trim()) { this.$message.warning('请先填写任务名称'); return }
      // 岗位是「按岗位」档位才需要的 —— 按部门 / 按人发的任务不绑定岗位
      if (f.assignScope === 'POSITION' && !f.positionId) { this.$message.warning('请选择适用岗位'); return }
      if (f.assignScope === 'USER' && !(f.targetUserIds || []).length) { this.$message.warning('已选「指定人员」，请至少勾选一位实习生'); return }
      if (andPublish && !f.deadline) { this.$message.warning('请选择截止时间'); return }
      if (andPublish) { this.publishing = true } else { this.saving = true }
      saveTask(f).then(res => {
        const id = (res.data || {}).id
        if (!andPublish) {
          this.saving = false
          // 关键：**保留表单并记住草稿 id** —— 任务资料必须挂在已存在的 taskId 上，
          // 所以「存草稿」既是保存也是拿到上传资料的前提。
          const first = !this.form.id
          this.form.id = id
          this.$message.success(first ? '已存为草稿，现在可以上传任务资料' : '草稿已保存')
          this.loadTasks()
          return
        }
        // 保存成功 → 立刻发布（发布才展开分配并推送通知）
        changeTaskStatus(id, 'PUBLISHED').then(r => {
          this.$message.success(r.msg || '已发布并通知到人')
          this.form = this.blankForm()
          this.loadTasks()
        }).catch(() => {}).finally(() => { this.publishing = false })
      }).catch(() => { this.saving = false; this.publishing = false })
    },
    /** 对已存在的草稿执行发布 */
    publish(row) {
      if (!row.deadline) { this.$message.warning('该任务没有截止时间，请先补全'); return }
      this.publishing = true
      changeTaskStatus(row.id, 'PUBLISHED').then(res => {
        this.$message.success(res.msg || '已发布')
        this.form = this.blankForm()
        this.loadTasks()
      }).catch(() => {}).finally(() => { this.publishing = false })
    },
    endTask(row) {
      this.$confirm('结束后实习生不能再提交作业，确定结束？', '结束任务', { type: 'warning' }).then(() =>
        changeTaskStatus(row.id, 'ENDED').then(res => {
          this.$message.success(res.msg || '已结束')
          this.loadTasks()
        }).catch(() => {})).catch(() => {})
    },
    /**
     * 催办：给未提交者发定向通知（复用通知接口，与超管催办同一机制）。
     * 本页不再持有提交看板（那是「任务批阅」页的事），所以这里现拉一次。
     */
    urge(row) {
      listSubmissions(row.id).then(res => {
        const list = res.data || []
        const targets = list.filter(b => !b.submissionId).map(b => b.userId)
        if (!targets.length) { this.$message.info('该任务所有人都已提交，无需催办'); return }
        this.$confirm('将向 ' + targets.length + ' 位未提交者推送提醒通知，是否继续？', '催办', {
          confirmButtonText: '推送提醒', cancelButtonText: '取消', type: 'warning'
        }).then(() => sendMessage({
          msgType: 'URGE', scopeType: 'USER', targetUserIds: targets,
          title: '【催办】任务待提交：' + row.taskName,
          content: '你的任务尚未提交，截止时间：' + (row.deadline || '—') + '，请尽快处理。',
          bizType: 'TASK', bizId: row.id
        }).then(r => {
          this.$message.success(r.msg || '已推送催办提醒')
        }).catch(() => {})).catch(() => {})
      }).catch(() => {})
    },
    overdue(row) {
      if (row.status !== 'PUBLISHED' || !row.deadline) return false
      const t = new Date(String(row.deadline).replace(/-/g, '/')).getTime()
      return !isNaN(t) && t < Date.now()
    },
    statusText(s) { return STATUS_TEXT[s] || s },
    statusTone(s) {
      return { DRAFT: 'gray', PUBLISHED: 'blue', ENDED: 'green' }[s] || 'gray'
    },
    assignText(s) { return ASSIGN_TEXT[s] || s },
    assignTone(s) {
      return { DONE: 'green', IN_PROGRESS: 'blue', OVERDUE: 'red' }[s] || 'gray'
    },
    shortTime(v) {
      if (!v) return '—'
      return String(v).replace('T', ' ').slice(5, 16)
    }
  }
}
</script>

<style lang="scss" scoped>
@import '~@/assets/styles/department-module.scss';

.dchkrow:hover { color: #1764f5; }
.track-filters { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; justify-content: flex-end; }
.track-hint {
  display: flex; align-items: center; gap: 6px;
  margin: 0 0 8px; padding: 7px 10px;
  background: #f7faff; border: 1px solid #dbe6f8; border-radius: 8px;
  color: #344054; font-size: 12px;

  b { color: #1764f5; }
  .el-button { padding: 0 4px; font-size: 12px; }
}
.dtbl .acts .sep { color: #d0d5dd; }
.dtbl .overdue { margin-top: 3px; color: #b42318; font-size: 11px; }

/* 任务名下方的一排资源标签（讨论 / 资料）—— 任务自身的资源跟任务走，操作列只放「动作」 */
.rchips {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-top: 4px;
}
.dtbl .rchips .overdue { margin: 0; }
.rchips .chip {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  color: #667085;
  font-size: 11.5px;
  cursor: pointer;

  &:hover { color: #1764f5; }
  i { font-size: 12px; }
}
/* 只读信息标签（不可点），如「指定 2 人」 */
.rchips .chip.static { color: #7a5af8; cursor: default; }
.rchips .chip.static:hover { color: #7a5af8; }
.dsep { height: 1px; margin: 14px 0 12px; background: #eef1f6; }

/* ---- 可点开详情的任务行 ---- */
.dtbl tbody tr.row-click { cursor: pointer; }
.dtbl tbody tr.row-click:hover td { background: #f2f7ff; }

/* ---- 自绘弹层（任务详情）的样式在文件末尾的**非 scoped** <style> 块里（命令式节点拿不到 scoped 属性） ---- */

/* ---- 弹层内容 ---- */
.dt-head { display: flex; align-items: center; gap: 8px; margin-bottom: 14px; }
.dt-kv {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px 18px;
  padding: 12px 14px;
  margin-bottom: 14px;
  background: #f7f9fc;
  border: 1px solid #eef1f6;
  border-radius: 8px;

  > div { display: flex; align-items: baseline; gap: 8px; min-width: 0; }
  label { flex: none; width: 96px; color: #98a2b3; font-size: 12px; }
  span { color: #1d2939; font-size: 12.5px; word-break: break-all; }
}
.dt-sec {
  margin-bottom: 14px;

  > label { display: block; margin-bottom: 6px; color: #667085; font-size: 12px; }
  p {
    margin: 0; padding: 10px 12px;
    background: #fff; border: 1px solid #eef1f6; border-radius: 8px;
    color: #344054; font-size: 12.5px; line-height: 1.85; white-space: pre-wrap; word-break: break-word;
  }
  p.pre { max-height: 180px; overflow: auto; }
}
.dt-stats {
  display: flex; flex-wrap: wrap; gap: 8px 16px;
  padding: 10px 12px;
  background: #fff; border: 1px solid #eef1f6; border-radius: 8px;
  color: #475467; font-size: 12.5px;

  b { color: #1764f5; font-weight: 600; }
  .dt-mine { padding: 0 8px; background: #eef4fe; border-radius: 5px; }
}
code { padding: 1px 5px; color: #344054; font-size: 11.5px; background: #f2f4f7; border-radius: 4px; }
.hint-text { color: #98a2b3; font-size: 11.5px; }
</style>
