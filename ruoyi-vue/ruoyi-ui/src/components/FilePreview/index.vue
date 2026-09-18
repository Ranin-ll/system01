<template>
  <div class="fp-wrap" :style="{ height: height }">
    <!-- 不支持预览 -->
    <div v-if="kind === 'none'" class="fp-state">
      <i class="el-icon-document" />
      <span>{{ hintText }}</span>
      <a class="fp-dl" :href="url" target="_blank" rel="noopener" download>
        <i class="el-icon-download" /> 下载文件
      </a>
    </div>

    <!-- 正在解析（docx / pptx 需要先取回文件再渲染） -->
    <div v-else-if="loading" class="fp-state">
      <i class="el-icon-loading" />
      <span>正在解析文件…</span>
    </div>

    <!-- 解析失败 → 明确给原因 + 退路 -->
    <div v-else-if="error" class="fp-state is-error">
      <i class="el-icon-warning-outline" />
      <span>{{ error }}</span>
      <a class="fp-dl" :href="url" target="_blank" rel="noopener" download>
        <i class="el-icon-download" /> 改为下载查看
      </a>
    </div>

    <template v-else>
      <!-- pdf / txt：交给浏览器内联渲染 -->
      <iframe v-if="kind === 'inline'" class="fp-frame" :src="url" :title="fileName" />
      <!-- docx：docx-preview 渲染成 HTML -->
      <div v-else-if="kind === 'docx'" ref="docxBox" class="fp-docx" />
      <!-- pptx：pptx-preview 渲染成画布 -->
      <div v-else-if="kind === 'pptx'" ref="pptxBox" class="fp-pptx" />
      <!-- 图片 -->
      <div v-else-if="kind === 'image'" class="fp-imgbox">
        <img :src="url" :alt="fileName" />
      </div>
    </template>
  </div>
</template>

<script>
/**
 * 文件预览（复用于批阅面板、任务资料等需要「先看一眼」的地方）
 *
 * 与 `views/assessment/learning/detail.vue` 里的内联预览是同一套渲染方式
 * （pdf/txt 用 iframe、docx 用 docx-preview、pptx 用 pptx-preview），
 * 但那页的预览器和「阅读进度/完成度」（SCROLL_END + read_confirm + 10 秒）深度耦合，
 * 所以这里**另起一个纯展示组件**，不去动那页。
 *
 * ⚠️ `url` 必须由调用方拼好（若依要带 `VUE_APP_BASE_API` 前缀），组件不做路径猜测。
 */
import { renderAsync } from 'docx-preview/dist/docx-preview.js'
import { init as initPptxPreview } from 'pptx-preview'
import { previewKind, previewHint } from '@/utils/filePreview'

export default {
  name: 'FilePreview',
  props: {
    url: { type: String, required: true },
    fileName: { type: String, default: '' },
    height: { type: String, default: '440px' }
  },
  data() {
    return {
      loading: false,
      error: ''
    }
  },
  computed: {
    kind() {
      return previewKind(this.fileName, this.url)
    },
    hintText() {
      return previewHint(this.fileName, this.url)
    }
  },
  watch: {
    url() { this.render() },
    fileName() { this.render() }
  },
  mounted() {
    this.render()
  },
  beforeDestroy() {
    this.destroyPptx()
  },
  methods: {
    render() {
      this.destroyPptx()
      this.error = ''
      this.loading = false
      // pdf / txt / 图片由浏览器自己渲染，这里不用取文件
      if (this.kind === 'docx') this.$nextTick(() => this.renderDocx())
      if (this.kind === 'pptx') this.$nextTick(() => this.renderPptx())
    },
    renderDocx() {
      const box = this.$refs.docxBox
      if (!box) return
      this.loading = true
      box.innerHTML = ''
      const mine = this.url
      window.fetch(this.url)
        .then(r => { if (!r.ok) throw new Error('文件读取失败（HTTP ' + r.status + '）'); return r.blob() })
        .then(blob => renderAsync(blob, box, null, {
          inWrapper: true, ignoreWidth: false, ignoreHeight: false, breakPages: true
        }))
        .then(() => { if (this.url === mine) this.loading = false })
        .catch(e => {
          if (this.url !== mine) return
          this.loading = false
          this.error = (e && e.message) || '当前 Word 文档无法解析'
        })
    },
    renderPptx() {
      const box = this.$refs.pptxBox
      if (!box) return
      this.loading = true
      box.innerHTML = ''
      const mine = this.url
      window.fetch(this.url)
        .then(r => { if (!r.ok) throw new Error('文件读取失败（HTTP ' + r.status + '）'); return r.arrayBuffer() })
        .then(buffer => {
          if (this.url !== mine) return null
          // 按容器宽度算 16:9，过宽会让幻灯片糊掉
          const w = box.clientWidth || 880
          const h = box.clientHeight || 420
          const width = Math.max(280, Math.min(w, h * 16 / 9))
          this._pptx = initPptxPreview(box, {
            width: Math.round(width),
            height: Math.round(width * 9 / 16),
            mode: 'slide'
          })
          return this._pptx.preview(buffer)
        })
        .then(() => { if (this.url === mine) this.loading = false })
        .catch(e => {
          if (this.url !== mine) return
          this.loading = false
          this.error = (e && e.message) || '当前 PPTX 文件无法解析'
        })
    },
    destroyPptx() {
      if (this._pptx && typeof this._pptx.destroy === 'function') {
        try { this._pptx.destroy() } catch (e) { /* 忽略销毁异常 */ }
      }
      this._pptx = null
    }
  }
}
</script>

<style lang="scss" scoped>
/* 自包含样式：会同时用在部门端（department-module.scss）之下 */
.fp-wrap {
  position: relative;
  overflow: hidden;
  background: #fff;
  border: 1px solid #e4e9f0;
  border-radius: 8px;
}
.fp-frame { width: 100%; height: 100%; border: 0; display: block; }
.fp-imgbox { width: 100%; height: 100%; overflow: auto; text-align: center; }
.fp-imgbox img { max-width: 100%; }
.fp-docx, .fp-pptx { width: 100%; height: 100%; overflow: auto; padding: 4px 6px; }

.fp-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 100%;
  padding: 0 20px;
  color: #98a2b3;
  font-size: 12.5px;
  text-align: center;

  i { font-size: 26px; color: #d0d5dd; }
  &.is-error i { color: #f79009; }
  &.is-error span { color: #b54708; }
}
.fp-dl {
  color: #1764f5;
  font-size: 12.5px;
  text-decoration: none;
  &:hover { text-decoration: underline; }
}
</style>
