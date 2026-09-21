<template>
  <!--
    数据口径标记（三态）—— 全站统一用它标注「这个数是哪来的」，避免用户误读。

    ★ 为什么必须有它：本项目里「空白」与「真实的 0」与「示例值」是三件完全不同的事，
      但肉眼分不出来。把它们显式标出来，是这套看板能被信任的前提。

    · 示例（mock=true）   → 橙标「示例」：题库/考核模块待同事分支合并，暂用前端示例
    · 无样本（none=true） → 灰标「无样本」：确实没有数据（分母为 0），**不伪装 0%**
    · 真数据              → 不渲染任何标记（默认真实）
  -->
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
 *
 * ⚠️ 本项目 `main.js` 只全局注册了少数组件，其余一律页面自己 import + 注册。
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
