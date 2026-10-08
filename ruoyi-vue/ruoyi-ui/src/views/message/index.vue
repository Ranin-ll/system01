<template>
  <div class="msg-page">
    <!-- 页头 -->
    <header class="mh">
      <div>
        <span class="eyebrow">MESSAGE CENTER</span>
        <h1>消息中心</h1>
        <p>接收审核结果、学习与考核提醒、通知与公告。<b>打开即已读</b>，未读会在顶栏铃铛上亮红点。</p>
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
    </div>

    <!-- 通知 -->
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
  </div>
</template>

<script>
/**
 * 消息中心（所有角色通用）
 *
 * ① 打开详情即已读；读后前端同步刷新未读。
 * ② 未读与列表来自同一份送达谓词，所以「铃铛数字」与「列表条数」永远一致。
 * ③ 「去处理」由消息的 bizType + bizId 推导真实路由，解析不到时按钮不出现。
 */
import { listMyMessages, getMessage, readAllMessages } from '@/api/business/message'
import { mapGetters } from 'vuex'

const TYPE_STYLE = {
  ANNOUNCE: { label: '公告', bg: '#f2f4f7', fg: '#475467' },
  EXAM: { label: '考核', bg: '#f4eeff', fg: '#7a5af8' },
  AUDIT: { label: '审核', bg: '#fff4e5', fg: '#b54708' },
  URGE: { label: '催办', bg: '#feecea', fg: '#b42318' },
  SYSTEM: { label: '系统', bg: '#e6f7f5', fg: '#0e7490' }
}

/**
 * 业务锚点 → 前端路由，按角色分流。
 * 返回空串 = 该角色没有对应页面 → 按钮不显示。
 */
const ROLE_TARGET = {
  INTERN: {
    EXAM: id => ({ path: '/assessment/intern/learning/result', query: id ? { examId: String(id) } : undefined }),
    PROMOTION: () => ({ path: '/assessment/intern/learning/result' })
  },
  DEPT: {
    AUDIT: () => ({ path: '/department/people/register-review' }),
    PROMOTION: () => ({ path: '/department/people/promotion' }),
    EXAM: () => ({ path: '/department/study/exam' })
  },
  SUPER: {
    EXAM: () => ({ path: '/super/ops/exams' })
  }
}

export default {
  name: 'MessageCenter',
  data() {
    return {
      loading: false,
      tab: 'notice',
      list: [],
      total: 0,
      unreadTotal: 0,
      current: null,
      query: { pageNum: 1, pageSize: 50, readFlag: '', msgType: '', title: '' },
      types: [
        { value: 'ANNOUNCE', label: '公告' },
        { value: 'EXAM', label: '考核' },
        { value: 'AUDIT', label: '审核' },
        { value: 'URGE', label: '催办' },
        { value: 'SYSTEM', label: '系统' }
      ]
    }
  },
  computed: {
    ...mapGetters(['roles']),
    roleKind() {
      const r = this.roles || []
      if (r.indexOf('PRE_TRAINEE') >= 0 || r.indexOf('FORMAL_TRAINEE') >= 0) return 'INTERN'
      if (r.indexOf('DEPT_ADMIN') >= 0) return 'DEPT'
      if (r.indexOf('SUPER_ADMIN') >= 0 || r.indexOf('admin') >= 0) return 'SUPER'
      return ''
    },
    actionPath() {
      const m = this.current
      if (!m) return ''
      let loc = m.linkUrl
      if (!loc) {
        const table = ROLE_TARGET[this.roleKind] || {}
        const fn = table[m.bizType]
        loc = fn ? fn(m.bizId) : ''
      }
      if (!loc) return ''
      const path = typeof loc === 'string' ? loc.split('?')[0] : loc.path
      try {
        if (!this.$router.resolve(path).route.matched.length) return ''
      } catch (e) {
        return ''
      }
      return loc
    }
  },
  created() {
    this.loadList()
  },
  methods: {
    switchTab(t) {
      this.tab = t
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
    typeStyle(t) {
      return TYPE_STYLE[t] || TYPE_STYLE.SYSTEM
    },
    goAction() {
      const target = this.actionPath
      if (!target) return
      this.$router.push(target).catch(() => {})
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
.mut { color: #98a2b3; font-size: 12px; }
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
  &.unread {
    border-left-color: #1764f5;
    background: #f7faff;

    .it-title { color: #1d2939; }
    .it-sub { color: #667085; }
  }
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
  &.tall { padding: 70px 0; }
}
</style>
