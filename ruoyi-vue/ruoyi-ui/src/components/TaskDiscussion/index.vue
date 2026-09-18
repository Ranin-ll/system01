<template>
  <el-dialog
    :title="'讨论区 · ' + (taskName || '')"
    :visible="visible"
    width="680px"
    append-to-body
    @close="close"
    @opened="onOpened"
  >
    <!-- 内容体是独立组件（TaskDiscussionBody），这里只是它的一层薄壳 -->
    <TaskDiscussionBody ref="body" :task-id="taskId" />
    <span slot="footer">
      <el-button size="small" @click="close">关闭</el-button>
    </span>
  </el-dialog>
</template>

<script>
/**
 * 任务讨论区「弹窗壳」。
 *
 * 真正的实现在 `TaskDiscussionBody.vue` —— 因为同一个讨论区还要能**内嵌**进
 * 「任务批阅」页（不弹窗），复制两份逻辑必然分叉，所以拆成 Body + 壳两层。
 */
import TaskDiscussionBody from './TaskDiscussionBody'

export default {
  name: 'TaskDiscussion',
  components: { TaskDiscussionBody },
  props: {
    visible: { type: Boolean, default: false },
    taskId: { type: [Number, String], default: null },
    taskName: { type: String, default: '' }
  },
  methods: {
    close() {
      this.$emit('update:visible', false)
    },
    /** 同一个 taskId 再次打开时 taskId 没变 → 内容体的 watch 不触发，这里补一次 */
    onOpened() {
      const b = this.$refs.body
      if (b) b.reload()
    }
  }
}
</script>
