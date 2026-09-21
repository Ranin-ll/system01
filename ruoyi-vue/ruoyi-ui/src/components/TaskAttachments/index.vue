<template>
  <div class="ta-wrap" :class="{ compact: compact }">
    <!-- 只读且为空 → 整块不渲染（调用方通常已用 attachmentCount 判过，避免每张卡都蹦「暂无资料」） -->
    <template v-if="editable || rows.length">
      <div v-if="!compact" class="ta-head">
        <div class="tt">
          <span class="idx">资</span>
          <h4>任务资料</h4>
          <span v-if="rows.length" class="cnt">{{ rows.length }} 份</span>
        </div>
        <el-upload
          v-if="editable"
          action=""
          :show-file-list="false"
          :http-request="doUpload"
          :before-upload="beforeUpload"
          :disabled="!taskId || uploading"
        >
          <el-button size="mini" type="primary" :loading="uploading" :disabled="!taskId" icon="el-icon-upload2">
            上传资料
          </el-button>
        </el-upload>
      </div>

      <p v-if="editable && !taskId" class="ta-hint warn">
        <i class="el-icon-warning-outline" /> 请先「存草稿」保存任务，保存后即可上传资料。
      </p>
      <p v-else-if="editable" class="ta-hint">
        支持 pdf / doc / docx / ppt / pptx / xls / xlsx / txt / md / csv / zip / rar / 7z / 图片，单个不超过 50MB。
        实习生端「我的任务」下方即可下载。
      </p>

      <div v-loading="loading" class="ta-list">
        <div v-for="f in rows" :key="f.id" class="ta-item">
          <i class="el-icon-document ta-ico" :class="extTone(f.fileExt)" />
          <div class="ta-body">
            <a class="ta-name" :href="url(f.fileUrl)" target="_blank" rel="noopener" :title="f.fileName">
              {{ f.fileName }}
            </a>
            <div class="ta-sub">
              {{ sizeText(f.fileSize) }}
              <span v-if="f.fileExt"> · {{ String(f.fileExt).toUpperCase() }}</span>
              <span v-if="f.uploaderName"> · {{ f.uploaderName }}</span>
              <span v-if="f.createTime"> · {{ String(f.createTime).replace('T', ' ').slice(5, 16) }}</span>
            </div>
          </div>
          <div class="ta-acts">
            <el-button v-if="canPrev(f)" type="text" class="ta-pv" @click="togglePreview(f)">
              <i class="el-icon-view" /> {{ isPreviewing(f) ? '收起' : '预览' }}
            </el-button>
            <!-- 能原生显示的（pdf/txt/图片）给「新窗口」；docx/pptx 等新窗口=下载，直接叫下载 -->
            <el-button v-if="canNativeOpen(f)" type="text" class="ta-dl" @click="openFile(f.fileUrl)">
              <i class="el-icon-top-right" /> 新窗口
            </el-button>
            <a class="ta-dl" :href="url(f.fileUrl)" target="_blank" rel="noopener" download>
              <i class="el-icon-download" /> 下载
            </a>
            <el-button v-if="editable" type="text" class="ta-del" @click="remove(f)">删除</el-button>
          </div>
        </div>

        <div v-if="!loading && !rows.length" class="ta-empty">
          <i class="el-icon-folder-opened" />
          <span>暂无资料</span>
          <small v-if="editable && taskId">点右上角「上传资料」把参考资料发给实习生。</small>
        </div>
      </div>

      <!-- 在线预览：放在整段列表下方（而不是塞在两行之间），窄栏里也不挤 -->
      <div v-if="previewFile" class="ta-preview">
        <div class="ta-preview-head">
          <i class="el-icon-view" />
          <b :title="previewFile.fileName">{{ previewFile.fileName }}</b>
          <span class="ta-preview-x" @click="previewFile = null"><i class="el-icon-close" /> 关闭预览</span>
        </div>
        <FilePreview
          :url="url(previewFile.fileUrl)"
          :file-name="previewFile.fileName"
          :height="compact ? '320px' : '420px'"
        />
      </div>
    </template>
  </div>
</template>

<script>
/**
 * 任务资料（附件）组件 —— 部门管理员上传 / 实习生下载
 *
 * 两种用法：
 *  ① **受控**（`:items` 传数组）：不发请求。实习生端「我的任务」用这个 ——
 *     任务卡每张一个请求就是 N+1，所以由页面一次性拉 `/business/task-attachment/my`
 *     再本地按 taskId 分组喂进来。
 *  ② **自取**（只给 `:taskId`）：组件自己拉 `/list`。部门端表单与弹窗用这个，
 *     打开弹窗时调一次 `reload()` 即可（同一个 taskId 再打开时 taskId 没变，watch 不会触发）。
 *
 * 下载地址必须走 `VUE_APP_BASE_API` 前缀：后端把 `${ruoyi.profile}` 映射在 `/profile/**`，
 * 而 dev 下只有 `VUE_APP_BASE_API`（/dev-api）被 vue.config.js 代理到 8080 ——
 * 写死 `http://localhost:8080` 在部署后会失效。
 */
import { listTaskAttachments, uploadTaskAttachment, deleteTaskAttachment } from '@/api/business/task'
import { canPreview, nativeOpen } from '@/utils/filePreview'
import FilePreview from '@/components/FilePreview'

const EXT_WHITELIST = ['pdf', 'doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx', 'txt', 'md', 'csv',
  'zip', 'rar', '7z', 'png', 'jpg', 'jpeg', 'gif']
const MAX_SIZE = 50 * 1024 * 1024

export default {
  name: 'TaskAttachments',
  components: { FilePreview },
  props: {
    taskId: { type: [Number, String], default: null },
    /** 受控模式：传了数组就不再发请求（传 null 表示自取） */
    items: { type: Array, default: null },
    /** 是否可上传/删除 */
    editable: { type: Boolean, default: false },
    /** 紧凑模式：不显示标题栏与提示（用在任务卡里） */
    compact: { type: Boolean, default: false }
  },
  data() {
    return {
      loading: false,
      uploading: false,
      fetched: [],
      /** 正在预览的那一份（同时只预览一个，避免多份大文件一起解析） */
      previewFile: null
    }
  },
  computed: {
    rows() {
      return this.items === null ? this.fetched : this.items
    }
  },
  watch: {
    taskId() {
      if (this.items === null) this.reload()
    }
  },
  created() {
    if (this.items === null) this.reload()
  },
  methods: {
    reload() {
      if (this.taskId === null || this.taskId === undefined || this.items !== null) return
      this.previewFile = null
      this.loading = true
      listTaskAttachments(this.taskId).then(res => {
        this.fetched = res.data || []
      }).catch(() => { this.fetched = [] }).finally(() => { this.loading = false })
    },
    /** 能否在线预览（pdf/txt/docx/pptx/图片）——判定逻辑与 FilePreview 共用一份 */
    canPrev(f) {
      return canPreview(f.fileName, f.fileUrl)
    },
    /**
     * 浏览器能否原生在新标签页显示（pdf/txt/图片）。
     *
     * ⚠️ 必须包成 methods 再给模板用 —— **Vue 2 模板访问不到 import 进来的函数**，
     * 直接写 `nativeOpen(...)` 会渲染期抛错、整个组件静默不渲染（本次就这么踩了一次）。
     */
    canNativeOpen(f) {
      return canPreview(f.fileName, f.fileUrl) && nativeOpen(f.fileName, f.fileUrl)
    },
    isPreviewing(f) {
      return !!this.previewFile && this.previewFile.id === f.id
    },
    togglePreview(f) {
      this.previewFile = this.isPreviewing(f) ? null : f
    },
    /** 新窗口（仅原生可显示的格式用） */
    openFile(u) {
      const url = this.url(u)
      if (url && url !== '#') window.open(url, '_blank', 'noopener')
    },
    /** 上传前的本地预校验：把明显不合格的文件挡在请求之前（后端仍会再校验一次） */
    beforeUpload(file) {
      const ext = (file.name.split('.').pop() || '').toLowerCase()
      if (EXT_WHITELIST.indexOf(ext) < 0) {
        this.$message.warning('不支持的文件类型：' + ext)
        return false
      }
      if (file.size > MAX_SIZE) {
        this.$message.warning('单个文件不能超过 50MB')
        return false
      }
      return true
    },
    doUpload(options) {
      const form = new FormData()
      form.append('file', options.file)
      this.uploading = true
      uploadTaskAttachment(this.taskId, form).then(res => {
        this.$message.success(res.msg || '资料已上传')
        if (this.items === null) this.reload()
        this.$emit('changed')
        if (options.onSuccess) options.onSuccess(res)
      }).catch(() => {
        if (options.onError) options.onError(new Error('upload failed'))
      }).finally(() => { this.uploading = false })
    },
    remove(f) {
      this.$confirm('删除后实习生将无法再下载《' + f.fileName + '》，确定删除？', '删除资料', {
        type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消'
      }).then(() =>
        deleteTaskAttachment(f.id).then(res => {
          this.$message.success(res.msg || '资料已删除')
          if (this.isPreviewing(f)) this.previewFile = null
          if (this.items === null) this.reload()
          this.$emit('changed')
        }).catch(() => {})).catch(() => {})
    },
    url(fileUrl) {
      if (!fileUrl) return '#'
      return process.env.VUE_APP_BASE_API + fileUrl
    },
    extTone(ext) {
      const e = String(ext || '').toLowerCase()
      if (['xls', 'xlsx', 'csv'].indexOf(e) >= 0) return 'green'
      if (['ppt', 'pptx'].indexOf(e) >= 0) return 'orange'
      if (['png', 'jpg', 'jpeg', 'gif'].indexOf(e) >= 0) return 'purple'
      if (['zip', 'rar', '7z'].indexOf(e) >= 0) return 'gray'
      return 'blue'
    },
    sizeText(v) {
      const n = Number(v) || 0
      if (n < 1024) return n + ' B'
      if (n < 1024 * 1024) return (n / 1024).toFixed(1) + ' KB'
      return (n / 1024 / 1024).toFixed(1) + ' MB'
    }
  }
}
</script>

<style lang="scss" scoped>
/* 自包含样式：同时用在部门端（department-module.scss）与实习生端「消息中心」（自带样式）之下 */
.ta-head {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 8px;
  .tt { display: flex; align-items: center; gap: 8px; }
  .idx {
    display: inline-flex; align-items: center; justify-content: center;
    width: 22px; height: 22px; border-radius: 6px;
    background: #e8f1fd; color: #1764f5; font-size: 12px;
  }
  h4 { margin: 0; font-size: 14px; color: #1d2939; }
  .cnt {
    padding: 1px 7px; border-radius: 5px; background: #eef4fe; color: #1764f5; font-size: 11px;
  }
}
.ta-hint {
  margin: 0 0 8px; color: #98a2b3; font-size: 11.5px; line-height: 1.7;
  &.warn { color: #b54708; }
}
.ta-list { min-height: 20px; }
.ta-item {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 10px; margin-bottom: 6px;
  background: #f7f9fc; border: 1px solid #eef1f6; border-radius: 8px;
}
.compact .ta-item { padding: 6px 9px; margin-bottom: 5px; }
.ta-ico {
  flex: none; font-size: 18px;
  &.blue { color: #1764f5; }
  &.green { color: #12b76a; }
  &.orange { color: #f79009; }
  &.purple { color: #7a5af8; }
  &.gray { color: #98a2b3; }
}
.ta-body { flex: 1; min-width: 0; }
.ta-name {
  display: block; color: #1d2939; font-size: 13px; text-decoration: none;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  &:hover { color: #1764f5; text-decoration: underline; }
}
.compact .ta-name { font-size: 12.5px; }
.ta-sub { margin-top: 2px; color: #98a2b3; font-size: 11.5px; }
.ta-acts { flex: none; display: flex; align-items: center; gap: 4px; }
.ta-pv { color: #1764f5; font-size: 12px; padding: 0 2px; }
.ta-dl {
  color: #1764f5; font-size: 12px; text-decoration: none; white-space: nowrap;
  &:hover { text-decoration: underline; }
}
.ta-del { color: #98a2b3; font-size: 12px; padding: 0 2px; }
.ta-del:hover { color: #b42318; }

/* 预览区：在整段列表下方，宽度随容器（表单里是 5/12 栏，页面里是全宽） */
.ta-preview { margin-top: 10px; }
.ta-preview-head {
  display: flex; align-items: center; gap: 6px;
  margin-bottom: 6px; color: #475467; font-size: 12.5px;
  b { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
}
.ta-preview-head > i { color: #1764f5; }
.ta-preview-x {
  margin-left: auto; flex: none;
  color: #98a2b3; font-size: 11.5px; cursor: pointer;
  &:hover { color: #b42318; }
}
.ta-empty {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 6px; padding: 22px 0; color: #98a2b3; font-size: 12px;
  i { font-size: 24px; color: #d0d5dd; }
  small { color: #b0b8c4; font-size: 11.5px; }
}
</style>
