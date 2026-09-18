<template>
  <div class="td-wrap" :class="{ bare: bare }">
    <!-- 未开启 -->
    <div v-if="!loading && !enabled" class="td-state">
      <i class="el-icon-chat-line-square" />
      <strong>该任务未开启讨论区</strong>
      <span>需要部门管理员在发布任务时勾选「启用讨论区」。</span>
    </div>

    <template v-else>
      <div class="td-stat">
        <span>共 <b>{{ total }}</b> 条讨论</span>
        <span v-if="ended" class="td-arch">任务已结束 · 讨论区已归档（只读）</span>
        <span v-else-if="!canPost" class="td-arch">当前身份仅可查看</span>
      </div>

      <div v-loading="loading" class="td-list">
        <div v-for="p in rows" :key="p.id" class="td-post">
          <span class="td-av" :style="avStyle(p)">{{ initial(p) }}</span>
          <div class="td-body">
            <div class="td-meta">
              <b>{{ displayName(p) }}</b>
              <span class="td-time">{{ ago(p.createTime) }}</span>
            </div>
            <p v-if="p.deleted" class="td-gone">该评论已被删除</p>
            <p v-else class="td-text">{{ p.content }}</p>
            <div class="td-acts">
              <span v-if="canPost && !p.deleted" @click="startReply(p)">回复</span>
              <span v-if="canDelete(p) && !p.deleted" class="danger" @click="remove(p)">删除</span>
            </div>

            <!-- 主楼下的全部回复（已归拢，含更深层级） -->
            <div v-if="p.replies && p.replies.length" class="td-replies">
              <div v-for="r in p.replies" :key="r.id" class="td-post sub">
                <span class="td-av sm" :style="avStyle(r)">{{ initial(r) }}</span>
                <div class="td-body">
                  <div class="td-meta">
                    <b>{{ displayName(r) }}</b>
                    <span class="td-time">{{ ago(r.createTime) }}</span>
                  </div>
                  <p v-if="r.deleted" class="td-gone">该评论已被删除</p>
                  <p v-else class="td-text">
                    <span v-if="r.replyToName" class="td-at">回复 <b>@{{ r.replyToName }}</b>：</span>{{ r.content }}
                  </p>
                  <div class="td-acts">
                    <span v-if="canPost && !r.deleted" @click="startReply(r)">回复</span>
                    <span v-if="canDelete(r) && !r.deleted" class="danger" @click="remove(r)">删除</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="!loading && !rows.length" class="td-empty">
          <i class="el-icon-chat-dot-square" />
          <span>还没有讨论，来说点什么？</span>
        </div>
      </div>
    </template>

    <!-- 输入区 -->
    <div v-if="enabled && canPost" class="td-editor">
      <div v-if="replyTo" class="td-replychip">
        回复 <b>@{{ replyTo.name }}</b>
        <i class="el-icon-close" @click="replyTo = null" />
      </div>
      <el-input
        ref="box"
        v-model="content"
        type="textarea"
        :rows="3"
        :maxlength="500"
        show-word-limit
        :placeholder="replyTo ? '回复 ' + replyTo.name + '…' : '写下你的问题或想法（纯文本，最多 500 字）'"
      />
      <div class="td-editor-foot">
        <span class="td-tip">讨论区不做修改，说错了只能删除重发；删除仅留痕、不物理删除。</span>
        <el-button size="small" type="primary" :loading="posting" @click="send">发送</el-button>
      </div>
    </div>
  </div>
</template>

<script>
/**
 * 任务讨论区「内容体」——不带任何容器，可内嵌进页面或塞进弹窗。
 *
 * 拆成 Body + 壳两层的原因：同一个讨论区既要能弹窗用（任务列表里快速看），
 * 又要能内嵌（任务批阅页的「讨论」页签）。**复制两份逻辑必然分叉**，
 * 所以内容体是一个独立组件，弹窗只是它的一层薄壳。
 *
 * 设计要点：
 *  ① <b>权限不自带，全部来自后端响应</b> —— canPost / canModerate / 每条帖的 mine 都由
 *     `/business/task-post/list` 回传，组件不自己判断角色。这样「前端隐藏按钮」只是界面优化，
 *     真正的越权拦截在后端，两边口径天生一致。
 *  ② 数据层是<b>任意深度的 parent_id 树</b>，展示层是<b>两级</b>：后端已把深层回复归拢到主楼，
 *     并给出 replyToName，所以这里只渲染两层，缩进永远不会爆。
 *  ③ 已删除的帖由后端把 content 置空、只回传标记 → 这里渲染占位，回复链因此不会断。
 */
import { listTaskPosts, saveTaskPost, deleteTaskPost } from '@/api/business/task'

const AV_COLORS = ['#1764f5', '#12b76a', '#f79009', '#7a5af8', '#0e7490', '#b42318', '#b54708', '#027a48']

export default {
  name: 'TaskDiscussionBody',
  props: {
    taskId: { type: [Number, String], default: null },
    /** 内嵌模式：去掉外层留白与最小高度（嵌在卡片里时更贴边） */
    bare: { type: Boolean, default: false }
  },
  data() {
    return {
      loading: false,
      posting: false,
      rows: [],
      total: 0,
      enabled: true,
      ended: false,
      canPost: false,
      canModerate: false,
      content: '',
      replyTo: null
    }
  },
  watch: {
    taskId() {
      this.reset()
      this.reload()
    }
  },
  created() {
    this.reload()
  },
  methods: {
    reset() {
      this.rows = []
      this.total = 0
      this.content = ''
      this.replyTo = null
      this.canModerate = false
    },
    /** 同一个 taskId 再打开时 watch 不触发 → 由外层（弹窗）显式调一次 */
    reload() {
      if (this.taskId === null || this.taskId === undefined) return
      this.loading = true
      listTaskPosts(this.taskId).then(res => {
        const d = res.data || {}
        this.enabled = d.enabled !== false
        this.ended = d.ended === true
        this.canPost = d.canPost === true
        this.canModerate = d.canModerate === true
        this.rows = d.rows || []
        this.total = d.total || 0
      }).catch(() => { this.rows = [] }).finally(() => { this.loading = false })
    },
    send() {
      const text = (this.content || '').trim()
      if (!text) { this.$message.warning('内容不能为空'); return }
      this.posting = true
      saveTaskPost({
        taskId: this.taskId,
        content: text,
        parentId: this.replyTo ? this.replyTo.id : null
      }).then(res => {
        this.$message.success(res.msg || '已发布')
        this.content = ''
        this.replyTo = null
        this.reload()
        this.$emit('changed')
      }).catch(() => {}).finally(() => { this.posting = false })
    },
    startReply(p) {
      this.replyTo = { id: p.id, name: this.displayName(p) }
      this.$nextTick(() => {
        const box = this.$refs.box
        if (box && box.focus) box.focus()
      })
    },
    remove(p) {
      this.$confirm('删除后内容不再显示（留痕），确定删除？', '删除评论', {
        type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消'
      }).then(() =>
        deleteTaskPost(p.id).then(res => {
          this.$message.success(res.msg || '已删除')
          if (this.replyTo && this.replyTo.id === p.id) this.replyTo = null
          this.reload()
          this.$emit('changed')
        }).catch(() => {})).catch(() => {})
    },
    /** 本人可删自己的；部门管理员凡可见即可删（后端已按部门校验，这里只是界面优化） */
    canDelete(p) {
      return p.mine === true || this.canModerate === true
    },
    displayName(p) {
      return p.nickName || p.userName || '匿名'
    },
    initial(p) {
      const n = this.displayName(p)
      return n.slice(0, 1).toUpperCase()
    },
    avStyle(p) {
      const key = Number(p.userId) || 0
      return { background: AV_COLORS[key % AV_COLORS.length] }
    },
    /** 相对时间：刚刚 / N 分钟前 / N 小时前 / 昨天 / MM-DD HH:mm */
    ago(v) {
      if (!v) return ''
      const t = new Date(String(v).replace(/-/g, '/')).getTime()
      if (isNaN(t)) return String(v).slice(5, 16)
      const diff = Date.now() - t
      if (diff < 60000) return '刚刚'
      if (diff < 3600000) return Math.floor(diff / 60000) + ' 分钟前'
      if (diff < 86400000) return Math.floor(diff / 3600000) + ' 小时前'
      if (diff < 172800000) return '昨天 ' + String(v).slice(11, 16)
      return String(v).replace('T', ' ').slice(5, 16)
    }
  }
}
</script>

<style lang="scss" scoped>
/* 自包含样式：本组件同时挂在「消息中心」（自带样式）与部门端（department-module.scss）之下 */
.td-wrap { min-height: 120px; }
.td-wrap.bare { min-height: 0; }

.td-state {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 8px; padding: 40px 0; color: #98a2b3; font-size: 12.5px;
  i { font-size: 30px; color: #d0d5dd; }
  strong { color: #475467; font-size: 13.5px; }
}

.td-stat {
  display: flex; align-items: center; gap: 12px;
  padding-bottom: 10px; margin-bottom: 6px;
  border-bottom: 1px solid #f2f4f7;
  color: #667085; font-size: 12px;
  b { color: #1764f5; }
  .td-arch { padding: 2px 8px; background: #fff4e5; color: #b54708; border-radius: 5px; font-size: 11.5px; }
}

.td-list { max-height: 380px; overflow-y: auto; padding-right: 4px; }

.td-post {
  display: flex; gap: 10px; padding: 10px 0;

  &.sub {
    padding: 8px 0 0;
    .td-text { font-size: 12.5px; }
  }
}

.td-av {
  flex: none; width: 30px; height: 30px; border-radius: 50%;
  color: #fff; font-size: 12.5px; line-height: 30px; text-align: center;
  &.sm { width: 24px; height: 24px; font-size: 11px; line-height: 24px; }
}

.td-body { flex: 1; min-width: 0; }

.td-meta {
  display: flex; align-items: baseline; gap: 8px;
  b { color: #344054; font-size: 12.5px; }
  .td-time { color: #98a2b3; font-size: 11.5px; }
}

.td-text {
  margin: 4px 0 0; color: #1d2939; font-size: 13px; line-height: 1.8;
  white-space: pre-wrap; word-break: break-word;
}
.td-gone {
  margin: 4px 0 0; color: #98a2b3; font-size: 12.5px; font-style: italic;
}
.td-at {
  color: #667085;
  b { color: #1764f5; font-weight: 500; }
}

.td-acts {
  display: flex; gap: 12px; margin-top: 5px;
  span {
    color: #667085; font-size: 11.5px; cursor: pointer;
    &:hover { color: #1764f5; }
    &.danger:hover { color: #b42318; }
  }
}

.td-replies {
  margin-top: 6px; padding: 2px 0 2px 10px;
  border-left: 2px solid #eef1f6;
}

.td-empty {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 8px; padding: 44px 0; color: #98a2b3; font-size: 12.5px;
  i { font-size: 30px; color: #d0d5dd; }
}

.td-editor {
  margin-top: 12px; padding-top: 12px; border-top: 1px solid #f2f4f7;
}
.td-replychip {
  display: inline-flex; align-items: center; gap: 6px;
  height: 24px; padding: 0 10px; margin-bottom: 8px;
  background: #e8f1fd; color: #1764f5; border-radius: 12px; font-size: 11.5px;
  b { font-weight: 600; }
  i { cursor: pointer; color: #7a93b8; &:hover { color: #b42318; } }
}
.td-editor-foot {
  display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 8px;
}
.td-tip { color: #98a2b3; font-size: 11.5px; line-height: 1.6; }
</style>
