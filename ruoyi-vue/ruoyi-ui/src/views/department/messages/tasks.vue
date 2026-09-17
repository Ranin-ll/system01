<template>
  <div class="dept-page">
    <div class="dept-breadcrumb">
      任务与通知 <span>/</span> <b>任务管理</b>
    </div>

    <div class="dept-heading">
      <div>
        <span class="eyebrow">DEPARTMENT ADMIN</span>
        <h1>任务管理</h1>
        <p>对指定实习生发布任务，有完成状态、可跟踪、可催办。这是实习生端「工作台 › 我的待办」的数据来源。</p>
      </div>
      <div class="dept-heading-actions">
        <span class="dsample">演示数据 · 6 张任务/通知表待后端</span>
      </div>
    </div>

    <div class="dgrid">
      <!-- 发布任务 -->
      <div class="dcard c5">
        <div class="dcard-h">
          <div class="tt"><span class="idx">发</span><h3>发布任务</h3></div>
        </div>

        <div class="dfield">
          <label>任务标题 <b>*</b></label>
          <el-input v-model="form.title" size="small" placeholder="例如：完成 Docker 薄弱知识点补学" />
        </div>
        <div class="dfg2" style="margin-top:12px">
          <div class="dfield">
            <label>任务类型 <b>*</b></label>
            <el-select v-model="form.type" size="small" style="width:100%">
              <el-option v-for="t in taskTypes" :key="t" :label="t" :value="t" />
            </el-select>
          </div>
          <div class="dfield">
            <label>优先级</label>
            <el-select v-model="form.priority" size="small" style="width:100%">
              <el-option v-for="p in priorities" :key="p" :label="p" :value="p" />
            </el-select>
          </div>
          <div class="dfield">
            <label>截止时间 <b>*</b></label>
            <el-date-picker
              v-model="form.deadline"
              type="datetime"
              size="small"
              value-format="yyyy-MM-dd HH:mm"
              placeholder="选择截止时间"
              style="width:100%"
            />
          </div>
          <div class="dfield">
            <label>是否需提交</label>
            <el-select v-model="form.needSubmit" size="small" style="width:100%">
              <el-option label="需提交（文字或附件）" :value="true" />
              <el-option label="仅需完成确认" :value="false" />
            </el-select>
          </div>
        </div>
        <div class="dfield">
          <label>任务说明</label>
          <el-input
            v-model="form.desc"
            type="textarea"
            :rows="5"
            size="small"
            placeholder="请针对本场考核暴露的 Docker 相关薄弱点，重新学习「容器基础」与「镜像分层」两节，并写一份不少于 300 字的总结。"
          />
        </div>

        <!-- 发布对象 -->
        <div class="dcard-h" style="margin:16px 0 6px">
          <div class="tt"><span class="idx o">人</span><h3>发布对象（可指定到人）</h3></div>
          <el-button size="mini" @click="smartPick">按薄弱项智能选人</el-button>
        </div>
        <div
          v-for="scope in scopes"
          :key="scope.key"
          class="dchkrow"
          @click="toggleScope(scope)"
        >
          <span class="box" :class="{ on: form.scopes.indexOf(scope.key) > -1 }">
            <i class="el-icon-check" />
          </span>
          <span>{{ scope.label }}</span>
        </div>
        <div v-if="form.scopes.indexOf('PICKED') > -1" class="pick-box">
          <div class="pick-head">
            <b>指定人员</b>
            <span class="hint-text">已选 {{ picked.length }} 人</span>
          </div>
          <div class="dchips" style="margin:0">
            <span
              v-for="p in candidates"
              :key="p.userId"
              class="dchip"
              :class="{ on: picked.indexOf(p.userId) > -1 }"
              @click="togglePick(p.userId)"
            >
              {{ p.realName }}
            </span>
          </div>
        </div>
        <div class="dfield" style="margin-top:12px">
          <label>附件（可选）</label>
          <div class="ddrop small" @click="notReady('附件上传')">
            <i class="el-icon-paperclip" />
            <b>{{ form.attachment || '点击选择附件' }}</b>
          </div>
        </div>

        <div class="dbtn-row right" style="margin-top:14px">
          <el-button size="small" @click="notReady('存为草稿')">存草稿</el-button>
          <el-button size="small" type="primary" @click="publish">发布并推送</el-button>
        </div>
      </div>

      <!-- 已发布任务跟踪 -->
      <div class="dcard c7">
        <div class="dcard-h">
          <div class="tt"><span class="idx">跟</span><h3>已发布任务跟踪</h3></div>
          <div class="dchips" style="margin:0">
            <span
              v-for="tab in trackTabs"
              :key="tab.key"
              class="dchip"
              :class="{ on: trackFilter === tab.key }"
              @click="trackFilter = tab.key"
            >
              {{ tab.label }} <span class="n">{{ tab.count }}</span>
            </span>
          </div>
        </div>
        <table class="dtbl">
          <thead>
            <tr>
              <th>任务</th>
              <th style="width:74px">类型</th>
              <th style="width:74px">优先级</th>
              <th style="width:110px">截止</th>
              <th style="width:80px">完成</th>
              <th style="width:104px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in visibleTasks" :key="row.id">
              <td>
                <span class="strong">{{ row.title }}</span>
                <div v-if="isOverdue(row)" class="overdue">已逾期</div>
              </td>
              <td><span class="dbadge" :class="typeTone(row.type)">{{ row.type }}</span></td>
              <td><span class="dbadge" :class="priorityTone(row.priority)">{{ row.priority }}</span></td>
              <td>{{ row.deadline }}</td>
              <td>{{ row.done }} / {{ row.total }}</td>
              <td>
                <div class="acts">
                  <el-button v-if="row.done < row.total" type="text" @click="urge(row)">催办</el-button>
                  <span v-if="row.done < row.total" class="sep">|</span>
                  <el-button type="text" @click="viewTask(row)">查看</el-button>
                </div>
              </td>
            </tr>
            <tr v-if="!visibleTasks.length">
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
          点「催办」→ 向未完成者推一条提醒通知（写入同一套通知表）。任务完成情况由实习生端「工作台 › 我的待办」回写。
        </p>
      </div>
    </div>

    <div class="dcallout warn" style="margin-top:16px">
      <i class="el-icon-warning-outline" />
      <span>
        <b>后端现状</b>：<code>notification</code>、<code>notice</code>、<code>learning_task</code>、
        <code>task_assignment</code>、<code>task_post</code>、<code>task_submission</code>
        <b>6 张表全部零 Java 层</b>（表已建好、无读写接口）。
        本模块是「表齐但零接口」的典型，需新增：任务 CRUD + 分配 + 提交回写 + 通知发布/已读回执。<br />
        <b>建议分期</b>：先只做「通知」（实习生端铃铛已有 UI 落点，收益最直接），任务跟踪放第二期。
      </span>
    </div>
  </div>
</template>

<script>
import { listRegister } from '@/api/business/register'

let seedId = 100

export default {
  name: 'DeptTasks',
  data() {
    return {
      taskTypes: ['学习任务', '考核任务', '日常任务'],
      priorities: ['高', '常规', '低'],
      form: {
        title: '',
        type: '学习任务',
        priority: '高',
        deadline: '',
        needSubmit: true,
        desc: '',
        attachment: '',
        scopes: ['PICKED']
      },
      trackFilter: 'ALL',
      picked: [],
      candidates: [],
      tasks: [
        { id: 1, title: '完成 Docker 薄弱知识点补学', type: '学习任务', priority: '高', deadline: '09-25 18:00', done: 1, total: 2, raw: '2026-09-25 18:00' },
        { id: 2, title: '补交 Q3 考核实操演示视频', type: '考核任务', priority: '高', deadline: '09-22 18:00', done: 0, total: 1, raw: '2026-09-22 18:00' },
        { id: 3, title: '整理本周学习笔记', type: '学习任务', priority: '常规', deadline: '09-19 18:00', done: 4, total: 5, raw: '2026-09-19 18:00' },
        { id: 4, title: '参加代码评审旁听', type: '日常任务', priority: '常规', deadline: '09-15 18:00', done: 5, total: 5, raw: '2026-09-15 18:00' }
      ]
    }
  },
  computed: {
    scopes() {
      return [
        { key: 'ALL', label: '本部门全部实习生（' + (this.candidates.length || 5) + ' 人）' },
        { key: 'PICKED', label: '指定人员' },
        { key: 'UNPASSED', label: '尚未通过正式考核的实习生（2 人）' }
      ]
    },
    trackTabs() {
      const overdue = this.tasks.filter(t => this.isOverdue(t)).length
      const running = this.tasks.filter(t => t.done < t.total).length
      return [
        { key: 'ALL', label: '全部', count: this.tasks.length },
        { key: 'RUNNING', label: '进行中', count: running },
        { key: 'OVERDUE', label: '已逾期', count: overdue }
      ]
    },
    visibleTasks() {
      if (this.trackFilter === 'RUNNING') return this.tasks.filter(t => t.done < t.total)
      if (this.trackFilter === 'OVERDUE') return this.tasks.filter(t => this.isOverdue(t))
      return this.tasks
    },
    createdAt() {
      return new Date()
    }
  },
  created() {
    listRegister({ pageNum: 1, pageSize: 200 }).then(res => {
      const rows = (res && res.rows) || []
      this.candidates = rows.filter(r => r.status === 'PASSED')
      this.picked = this.candidates.slice(0, 2).map(c => c.userId)
    }).catch(() => {
      this.candidates = []
    })
  },
  methods: {
    toggleScope(scope) {
      const idx = this.form.scopes.indexOf(scope.key)
      if (idx > -1) this.form.scopes.splice(idx, 1)
      else this.form.scopes.push(scope.key)
    },
    togglePick(userId) {
      const idx = this.picked.indexOf(userId)
      if (idx > -1) this.picked.splice(idx, 1)
      else this.picked.push(userId)
    },
    smartPick() {
      // 演示：按「学习完成率最低」的口径选 2 人
      this.form.scopes = ['PICKED']
      this.picked = this.candidates.slice(0, 2).map(c => c.userId)
      this.$message.success('已按薄弱项选出 ' + this.picked.length + ' 人（演示态）')
    },
    publish() {
      if (!this.form.title.trim()) {
        this.$message.warning('请先填写任务标题')
        return
      }
      if (!this.form.deadline) {
        this.$message.warning('请选择截止时间')
        return
      }
      const total = this.form.scopes.indexOf('PICKED') > -1 && this.picked.length
        ? this.picked.length
        : (this.form.scopes.indexOf('ALL') > -1 ? (this.candidates.length || 5) : 2)
      const d = new Date(this.form.deadline.replace(/-/g, '/'))
      const p = n => (n < 10 ? '0' + n : '' + n)
      seedId += 1
      this.tasks.unshift({
        id: seedId,
        title: this.form.title,
        type: this.form.type,
        priority: this.form.priority,
        deadline: p(d.getMonth() + 1) + '-' + p(d.getDate()) + ' ' + p(d.getHours()) + ':' + p(d.getMinutes()),
        done: 0,
        total: total,
        raw: this.form.deadline
      })
      this.trackFilter = 'ALL'
      this.$message.success('任务已发布并推送给 ' + total + ' 人（演示态）')
      this.form.title = ''
      this.form.desc = ''
    },
    urge(row) {
      this.$confirm(
        '将向「' + row.title + '」的 ' + (row.total - row.done) + ' 位未完成人推送提醒通知，是否继续？',
        '催办',
        { confirmButtonText: '推送提醒', cancelButtonText: '取消', type: 'warning' }
      ).then(() => {
        this.$message.success('已推送催办提醒（演示态）')
      }).catch(() => {})
    },
    viewTask(row) {
      this.$alert(
        '<div style="line-height:1.9;font-size:12.5px">'
          + '<b>' + row.title + '</b><br/>'
          + '类型：' + row.type + '　优先级：' + row.priority + '<br/>'
          + '截止：' + row.deadline + '　完成：' + row.done + ' / ' + row.total
          + '</div>',
        '任务详情',
        { dangerouslyUseHTMLString: true, confirmButtonText: '关闭' }
      ).catch(() => {})
    },
    isOverdue(row) {
      if (row.done >= row.total) return false
      if (!row.raw) return false
      const t = new Date(String(row.raw).replace(/-/g, '/')).getTime()
      return !isNaN(t) && t < this.createdAt.getTime()
    },
    typeTone(type) {
      return { '学习任务': 'blue', '考核任务': 'purple', '日常任务': 'orange' }[type] || 'gray'
    },
    priorityTone(priority) {
      return { '高': 'red', '常规': 'gray', '低': 'gray' }[priority] || 'gray'
    },
    notReady(action) {
      this.$message({ message: '「' + action + '」所需的接口尚未落地（task_* 表零 Java 层）', type: 'warning' })
    }
  }
}
</script>

<style lang="scss" scoped>
@import '~@/assets/styles/department-module.scss';

.dchkrow:hover { color: #1764f5; }
.pick-box { padding: 12px 14px; margin-top: 4px; background: #f7f9fc; border: 1px solid #eef1f6; border-radius: 8px; }
.pick-head { display: flex; align-items: baseline; justify-content: space-between; margin-bottom: 9px; color: #344054; font-size: 12.5px; }
.ddrop.small { min-height: 46px; flex-direction: row; gap: 8px; }
.ddrop.small i { font-size: 15px; }
.dtbl .acts .sep { color: #d0d5dd; }
.dtbl .overdue { margin-top: 3px; color: #b42318; font-size: 11px; }
code { padding: 1px 5px; color: #344054; font-size: 11.5px; background: #f2f4f7; border-radius: 4px; }
.hint-text { color: #98a2b3; font-size: 11.5px; }
</style>
