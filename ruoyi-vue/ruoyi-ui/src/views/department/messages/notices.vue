<template>
  <div class="dept-page">
    <div class="dept-breadcrumb">
      任务与通知 <span>/</span> <b>通知管理</b>
    </div>

    <div class="dept-heading">
      <div>
        <span class="eyebrow">DEPARTMENT ADMIN</span>
        <h1>通知管理</h1>
        <p>单向告知，无完成状态；对应实习生端顶栏铃铛与工作台公告位。</p>
      </div>
      <div class="dept-heading-actions">
        <span class="dsample">演示数据 · notification / notice 表待后端</span>
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
          <el-input v-model="form.title" size="small" placeholder="例如：【提醒】2026Q3 正式考核将于 9 月 20 日 09:00 开考" />
        </div>
        <div class="dfg2" style="margin-top:12px">
          <div class="dfield">
            <label>通知类型</label>
            <el-select v-model="form.type" size="small" style="width:100%">
              <el-option v-for="t in types" :key="t" :label="t" :value="t" />
            </el-select>
          </div>
          <div class="dfield">
            <label>发送方式</label>
            <el-select v-model="form.channel" size="small" style="width:100%">
              <el-option label="站内信" value="站内信" />
              <el-option label="站内信 + 邮件" value="站内信 + 邮件" />
            </el-select>
          </div>
          <div class="dfield">
            <label>发送对象</label>
            <el-select v-model="form.target" size="small" style="width:100%">
              <el-option v-for="t in targets" :key="t.key" :label="t.label" :value="t.key" />
            </el-select>
          </div>
          <div class="dfield">
            <label>生效时间</label>
            <el-date-picker
              v-model="form.sendAt"
              type="datetime"
              size="small"
              value-format="yyyy-MM-dd HH:mm"
              placeholder="立即发送"
              style="width:100%"
            />
          </div>
        </div>
        <div class="dfield">
          <label>通知正文</label>
          <el-input
            v-model="form.body"
            type="textarea"
            :rows="6"
            size="small"
            placeholder="请提前 10 分钟进入考核页面完成设备自检。理论 60 分钟，实操 90 分钟，两部分均需及格。"
          />
        </div>
        <div class="dchkrow" style="margin-top:6px" @click="form.pinned = !form.pinned">
          <span class="box" :class="{ sw: form.pinned }"><i class="el-icon-check" /></span>
          <span>置顶显示在实习生工作台</span>
        </div>
        <div class="dchkrow" @click="form.urgent = !form.urgent">
          <span class="box" :class="{ sw: form.urgent }"><i class="el-icon-check" /></span>
          <span>同时推送顶栏铃铛红点</span>
        </div>

        <div class="dbtn-row right" style="margin-top:16px">
          <el-button size="small" @click="notReady('存为草稿')">存草稿</el-button>
          <el-button size="small" type="primary" @click="send">发送通知</el-button>
        </div>
      </div>

      <!-- 已发送通知 -->
      <div class="dcard c7">
        <div class="dcard-h">
          <div class="tt"><span class="idx">史</span><h3>已发送通知</h3></div>
          <span class="hint-text">已读列 = 实习生端铃铛未读红点的反面</span>
        </div>
        <table class="dtbl">
          <thead>
            <tr>
              <th>标题</th>
              <th style="width:80px">类型</th>
              <th style="width:120px">对象</th>
              <th style="width:110px">发送时间</th>
              <th style="width:74px">已读</th>
              <th style="width:82px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in notices" :key="row.id">
              <td>
                <span class="strong">{{ row.title }}</span>
                <span v-if="row.pinned" class="dbadge orange" style="margin-left:6px">置顶</span>
              </td>
              <td><span class="dbadge" :class="typeTone(row.type)">{{ row.type }}</span></td>
              <td>{{ row.target }}</td>
              <td>{{ row.sentAt }}</td>
              <td>
                <span :class="{ 'read-all': row.read >= row.total }">{{ row.read }} / {{ row.total }}</span>
              </td>
              <td>
                <div class="acts">
                  <el-button type="text" @click="viewNotice(row)">查看</el-button>
                  <span class="sep">|</span>
                  <el-button type="text" @click="resend(row)">重发</el-button>
                </div>
              </td>
            </tr>
            <tr v-if="!notices.length">
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
        <p class="dsec-note">
          「已读」列直接对应实习生端铃铛未读红点 —— 两角色靠这一张表联动。
          通知落地后可考虑先做「通知」这一半：实习生端铃铛 UI 已就位，收益最直接。
        </p>
      </div>
    </div>

    <div class="dcallout warn" style="margin-top:16px">
      <i class="el-icon-warning-outline" />
      <span>
        <b>后端现状</b>：<code>notification</code> / <code>notice</code> 两张表
        <b>零 Java 层</b>（表已建好、无读写接口）。需要新增：通知发布、按对象分发、
        已读回执写回。本页在接口就绪前用演示数据，发送动作只更新本地列表。
      </span>
    </div>
  </div>
</template>

<script>
export default {
  name: 'DeptNotices',
  data() {
    return {
      types: ['考核提醒', '课程更新', '公告', '任务提醒'],
      form: {
        title: '',
        type: '考核提醒',
        channel: '站内信 + 邮件',
        target: 'POSITION_DEV',
        sendAt: '',
        body: '',
        pinned: true,
        urgent: true
      },
      notices: [
        { id: 1, title: '【提醒】9/20 正式考核开考', type: '考核提醒', target: '开发实习生 5 人', sentAt: '09-18 10:00', read: 4, total: 5, pinned: true },
        { id: 2, title: '课程「容器基础」已更新为 v1.2', type: '课程更新', target: '开发实习生 5 人', sentAt: '09-15 15:20', read: 5, total: 5, pinned: false },
        { id: 3, title: '关于国庆假期实习安排', type: '公告', target: '本部门全体 6 人', sentAt: '09-12 09:00', read: 6, total: 6, pinned: false }
      ],
      seq: 100
    }
  },
  computed: {
    targets() {
      return [
        { key: 'ALL', label: '本部门全体' },
        { key: 'POSITION_DEV', label: '开发实习生' },
        { key: 'POSITION_IMPL', label: '实施实习生' },
        { key: 'NOT_PASSED', label: '尚未通过正式考核的实习生' }
      ]
    }
  },
  methods: {
    send() {
      if (!this.form.title.trim()) {
        this.$message.warning('请先填写通知标题')
        return
      }
      if (!this.form.body.trim()) {
        this.$message.warning('请填写通知正文')
        return
      }
      const target = (this.targets.find(t => t.key === this.form.target) || {}).label || '本部门全体'
      this.seq += 1
      this.notices.unshift({
        id: this.seq,
        title: this.form.title,
        type: this.form.type,
        target: target,
        sentAt: this.fmtNow(this.form.sendAt),
        read: 0,
        total: target.indexOf('全体') > -1 ? 6 : 5,
        pinned: this.form.pinned
      })
      this.$message.success('通知已发送（演示态）：' + target)
      this.form.title = ''
      this.form.body = ''
    },
    viewNotice(row) {
      this.$alert(
        '<div style="line-height:1.9;font-size:12.5px">'
          + '<b>' + row.title + '</b><br/>'
          + '类型：' + row.type + '　对象：' + row.target + '<br/>'
          + '发送：' + row.sentAt + '　已读：' + row.read + ' / ' + row.total
          + '</div>',
        '通知详情',
        { dangerouslyUseHTMLString: true, confirmButtonText: '关闭' }
      ).catch(() => {})
    },
    resend(row) {
      this.$confirm('将向「' + row.target + '」重新推送这条通知，是否继续？', '重发通知', {
        confirmButtonText: '重新推送',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        row.sentAt = this.fmtNow('')
        row.read = 0
        this.$message.success('已重新推送（演示态）')
      }).catch(() => {})
    },
    typeTone(type) {
      return { '考核提醒': 'purple', '课程更新': 'blue', '公告': 'gray', '任务提醒': 'orange' }[type] || 'gray'
    },
    fmtNow(value) {
      const d = value ? new Date(String(value).replace(/-/g, '/')) : new Date()
      const p = n => (n < 10 ? '0' + n : '' + n)
      if (isNaN(d.getTime())) return '—'
      return p(d.getMonth() + 1) + '-' + p(d.getDate()) + ' ' + p(d.getHours()) + ':' + p(d.getMinutes())
    },
    notReady(action) {
      this.$message({ message: '「' + action + '」所需的接口尚未落地（notification / notice 表零 Java 层）', type: 'warning' })
    }
  }
}
</script>

<style lang="scss" scoped>
@import '~@/assets/styles/department-module.scss';

.dchkrow:hover { color: #1764f5; }
.dtbl .acts .sep { color: #d0d5dd; }
.dtbl .read-all { color: #067647; font-weight: 600; }
code { padding: 1px 5px; color: #344054; font-size: 11.5px; background: #f2f4f7; border-radius: 4px; }
.hint-text { color: #98a2b3; font-size: 11.5px; }
</style>
