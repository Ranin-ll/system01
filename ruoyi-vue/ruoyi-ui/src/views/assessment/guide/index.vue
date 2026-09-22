<template>
  <div class="guide-page">
    <div class="guide-breadcrumb">
      <span>学习与考核</span>
      <span>/</span>
      <b>备考资料</b>
    </div>

    <header class="guide-heading">
      <div>
        <span class="eyebrow">MATERIAL CENTER</span>
        <h1>备考资料</h1>
        <p>部门管理员发布的考试指南与考核规则，支持在线预览与下载。</p>
      </div>
      <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="loadMaterials">刷新</el-button>
    </header>

    <section class="guide-panel">
      <div class="panel-heading">
        <div>
          <span class="section-index">01</span>
          <div>
            <h2>考试指南</h2>
            <p>由所属部门管理员在管理端上传，发布后在此处生效。</p>
          </div>
        </div>
        <span class="panel-count">
          <span class="count-text">{{ materials.length }} 份资料</span>
        </span>
      </div>

      <div v-if="!materials.length" class="empty-state">
        <i class="el-icon-notebook-2" />
        <strong>暂无可下载的备考资料</strong>
        <span>部门管理员上传考试指南（PDF）后，会出现在这里。</span>
      </div>

      <ul v-else class="material-list">
        <li v-for="item in materials" :key="item.id" class="material-row">
          <span class="material-icon"><i class="el-icon-document" /></span>
          <div class="material-info">
            <h3>{{ item.materialName }}</h3>
            <p>{{ item.summary || '考试指南' }}</p>
            <span class="material-meta">版本 {{ item.versionNo || 'v1.0' }} · 更新 {{ fmtTime(item.updateTime) }}</span>
          </div>
          <div class="material-action">
            <el-button size="mini" plain @click="preview(item)">文件预览</el-button>
            <el-button size="mini" type="primary" plain @click="download(item)">下载</el-button>
          </div>
        </li>
      </ul>
    </section>

    <!-- 附件预览弹窗 -->
    <el-dialog
      :title="previewFile && previewFile.name ? previewFile.name : '附件预览'"
      :visible.sync="previewVisible"
      width="760px"
      top="6vh"
      append-to-body
    >
      <div v-if="previewFile" class="file-preview">
        <el-image
          v-if="previewKind === 'image'"
          :src="baseApi + previewFile.url"
          fit="contain"
          style="width:100%;max-height:70vh"
        />
        <video
          v-else-if="previewKind === 'video'"
          :src="baseApi + previewFile.url"
          controls
          class="file-preview-video"
        />
        <iframe
          v-else-if="previewKind === 'pdf' || previewKind === 'text'"
          :src="baseApi + previewFile.url"
          class="file-preview-frame"
        />
        <div v-else class="file-preview-tip">
          <i class="el-icon-document" />
          <p>该格式（{{ previewExt }}）暂不支持在线预览，请下载后查看</p>
          <span class="file-preview-hint">{{ previewSupportText }}</span>
          <el-button type="primary" icon="el-icon-download" @click="download(previewFile)">下载附件</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { parseTime } from '@/utils/ruoyi'
import { listPublishedMaterials } from '@/api/business/material'
import {
  previewKindOf, extLabel, fileUrlOf as resolveFileUrl, PREVIEW_SUPPORT_TEXT, triggerDownload
} from '@/utils/filePreview'

export default {
  name: 'InternGuide',
  data() {
    return {
      loading: false,
      materials: [],
      previewVisible: false,
      previewFile: null
    }
  },
  computed: {
    baseApi() {
      return process.env.VUE_APP_BASE_API || ''
    },
    /** 预览类型：图片 / 视频 / PDF / 纯文本，其余走「下载后查看」 */
    previewKind() {
      return previewKindOf(this.previewFile && this.previewFile.url)
    },
    /** 不支持预览时提示用到的扩展名 */
    previewExt() {
      return extLabel(this.previewFile && this.previewFile.url)
    },
    previewSupportText() {
      return PREVIEW_SUPPORT_TEXT
    }
  },
  created() {
    this.loadMaterials()
  },
  methods: {
    loadMaterials() {
      this.loading = true
      listPublishedMaterials().then(res => {
        this.materials = (res && res.data) || []
        this.loading = false
      }).catch(() => {
        this.materials = []
        this.loading = false
      })
    },
    fmtTime(val) {
      return val ? parseTime(val, '{y}-{m}-{d}') : '--'
    },
    /** 取文件地址：列表项用 fileUrl，预览对象用 url —— 两处都要认，否则弹窗里的下载按钮会误报「暂无附件」 */
    fileUrlOf(item) {
      return resolveFileUrl(item)
    },
    preview(item) {
      const url = resolveFileUrl(item)
      if (!url) {
        this.$modal.msgWarning('该资料暂无附件')
        return
      }
      this.previewFile = {
        name: item.materialName || item.name || '附件',
        url: url,
        fileUrl: url
      }
      this.previewVisible = true
    },
    download(item) {
      const url = resolveFileUrl(item)
      if (!url) {
        this.$modal.msgWarning('该资料暂无附件')
        return
      }
      triggerDownload(this.baseApi, url, (item && (item.materialName || item.name)) || '')
    }
  }
}
</script>

<style lang="scss" scoped>
.guide-page { min-height: 100%; padding: 0 0 40px; color: #283544; }
.guide-breadcrumb { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; color: #98a2b3; font-size: 12px; }
.guide-breadcrumb b { color: #475467; font-weight: 500; }
.guide-heading { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 18px; }
.eyebrow { color: #2878c7; font-size: 11px; letter-spacing: .08em; }
.guide-heading h1 { margin: 5px 0 7px; color: #1d2939; font-size: 28px; font-weight: 600; }
.guide-heading p { margin: 0; color: #667085; font-size: 13px; }
.guide-panel { padding: 22px 24px 25px; background: #fff; box-shadow: 0 1px 3px rgba(16, 24, 40, .04); }
.panel-heading { display: flex; align-items: center; justify-content: space-between; padding-bottom: 18px; border-bottom: 1px solid #edf0f4; }
.panel-heading > div { display: flex; align-items: flex-start; gap: 12px; }
.section-index { color: #1764f5; font-size: 11px; font-weight: 700; }
.panel-heading h2 { margin: 0 0 5px; color: #1d2939; font-size: 18px; font-weight: 600; }
.panel-heading p { margin: 0; color: #8490a0; font-size: 12px; }
.panel-count { display: flex; align-items: center; gap: 8px; color: #667085; font-size: 12px; }
.empty-state { display: flex; min-height: 220px; align-items: center; justify-content: center; flex-direction: column; gap: 9px; color: #98a2b3; }
.empty-state i { color: #b7c1cc; font-size: 34px; }
.empty-state strong { color: #667085; font-size: 14px; font-weight: 500; }
.empty-state span { font-size: 12px; }
.material-list { margin: 0; padding: 0; list-style: none; }
.material-row { display: flex; align-items: center; gap: 18px; padding: 20px 8px; border-bottom: 1px solid #edf0f4; }
.material-row:last-child { border-bottom: 0; }
.material-icon { display: flex; width: 48px; height: 48px; flex: 0 0 48px; align-items: center; justify-content: center; color: #fff; background: #23966f; font-size: 22px; }
.material-info { min-width: 0; flex: 1; }
.material-info h3 { margin: 0 0 6px; color: #1d2939; font-size: 16px; font-weight: 600; }
.material-info p { margin: 0 0 7px; overflow: hidden; color: #667085; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.material-meta { color: #98a2b3; font-size: 11px; }
.material-action { display: flex; min-width: 150px; gap: 8px; justify-content: flex-end; }
.file-preview { display: flex; align-items: center; justify-content: center; min-height: 160px; }
.file-preview-frame { width: 100%; height: 70vh; border: none; }
.file-preview-video { width: 100%; max-height: 70vh; background: #000; }
.file-preview-tip { text-align: center; color: #98a2b3; padding: 24px 0; }
.file-preview-tip i { font-size: 48px; color: #c3cdd9; display: block; margin-bottom: 12px; }
.file-preview-tip p { margin: 0 0 16px; font-size: 13px; }
@media (max-width: 700px) {
  .guide-page { padding-bottom: 28px; }
  .guide-heading { align-items: flex-start; flex-direction: column; gap: 12px; }
  .guide-heading h1 { font-size: 24px; }
  .guide-panel { padding: 18px 14px; }
  .panel-heading { align-items: flex-start; gap: 10px; }
  .panel-heading p { max-width: 240px; line-height: 1.5; }
  .material-row { align-items: flex-start; flex-wrap: wrap; gap: 12px; }
  .material-info { width: calc(100% - 60px); flex: none; }
  .material-action { width: 100%; min-width: 0; justify-content: flex-end; }
}
</style>
