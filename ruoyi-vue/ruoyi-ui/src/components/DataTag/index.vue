<template>
  <span v-if="mock" class="dtag dtag-mock" :title="mockTip">{{ label || '示例' }}</span>
  <span v-else-if="none" class="dtag dtag-none" :title="noneTip">{{ label || '无样本' }}</span>
</template>

<script>
/**
 * 数据口径标记 · 三态小标（示例 / 无样本 / 真数据）
 *
 * 用法：
 *   <data-tag :mock="x.mock" />
 *   <data-tag :none="row.learnAvgProgress === null" />
 *   <data-tag :mock="x.mock" label="示例数据" />
 */
export default {
  name: 'DataTag',
  props: {
    /** true = 当前展示的是示例值 */
    mock: { type: Boolean, default: false },
    /** true = 确实没有数据（与「值为 0」区分） */
    none: { type: Boolean, default: false },
    /** 自定义文案 */
    label: { type: String, default: '' }
  },
  data() {
    return {
      mockTip: '题库与考核模块待同事分支合并，此处为前端示例数据',
      noneTip: '确实没有样本（分母为 0），未以 0 占位'
    }
  }
}
</script>

<style scoped>
/* 自带 scoped 样式、不 @import 任何设计体系基线（本组件会挂在多套体系下） */
.dtag {
  display: inline-block;
  padding: 0 5px;
  height: 16px;
  line-height: 16px;
  border-radius: 4px;
  font-size: 10.5px;
  vertical-align: 1px;
  white-space: nowrap;
}
.dtag-mock { background: #fff4e5; color: #b54708; }
/* 注意：不要用 #98a2b3 + 浅灰底 —— 对比度太低，看着像空白单元格（本项目踩过） */
.dtag-none { background: #f2f4f7; color: #475467; }
</style>
