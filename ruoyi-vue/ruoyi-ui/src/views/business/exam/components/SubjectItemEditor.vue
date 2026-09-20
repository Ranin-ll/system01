<template>
  <div class="sie">
    <div class="sie-bar">
      <div class="sie-bar-l">
        <span class="sie-count">共 {{ items.length }} 道题</span>
        <span class="sie-total">满分合计 {{ totalScore }} 分</span>
      </div>
      <el-button type="primary" plain size="mini" icon="el-icon-plus" @click="addItem">添加题目</el-button>
    </div>

    <div v-if="!items.length" class="sie-empty">
      <i class="el-icon-document-add" />
      <span>还没有题目：点「添加题目」逐条填写题干、题目描述、参考与附件</span>
    </div>

    <div v-for="(item, index) in items" :key="index" class="sie-item">
      <div class="sie-item-head">
        <span class="sie-seq">{{ index + 1 }}</span>
        <span class="sie-item-title">{{ item.title || '未填写题干' }}</span>
        <div class="sie-item-act">
          <el-button type="text" size="mini" icon="el-icon-top" :disabled="index === 0" @click="move(index, -1)" />
          <el-button type="text" size="mini" icon="el-icon-bottom" :disabled="index === items.length - 1" @click="move(index, 1)" />
          <el-button type="text" size="mini" class="danger-text" icon="el-icon-delete" @click="removeItem(index)" />
        </div>
      </div>

      <div class="sie-row">
        <label class="sie-label required">题干</label>
        <el-input v-model="item.title" size="small" maxlength="200" show-word-limit placeholder="例如：用户登录接口开发" />
      </div>

      <div class="sie-row">
        <label class="sie-label">题目描述</label>
        <el-input
          v-model="item.description"
          type="textarea"
          :rows="3"
          size="small"
          maxlength="5000"
          show-word-limit
          placeholder="作业要求 / 交付说明，例如：实现登录接口，完成参数校验与统一返回结构"
        />
      </div>

      <div class="sie-row">
        <label class="sie-label">本题满分</label>
        <el-input-number v-model="item.score" :min="0" :max="1000" :precision="1" size="small" style="width:130px" />
        <span class="sie-tip">分</span>
      </div>

      <div class="sie-row">
        <label class="sie-label">参考</label>
        <div class="sie-up">
          <div v-if="item.images.length" class="sie-img-grid">
            <div v-for="(img, i) in item.images" :key="i" class="sie-img">
              <video v-if="isVideo(img.url)" :src="baseApi + img.url" controls class="sie-img-thumb" />
              <el-image v-else :src="baseApi + img.url" fit="cover" :preview-src-list="previewList(item)" class="sie-img-thumb" />
              <i class="el-icon-close sie-img-del" @click="removeImage(item, i)" />
            </div>
          </div>
          <el-upload action="#" :show-file-list="false" :multiple="true" accept=".jpg,.jpeg,.png,.gif,.webp,.bmp,.mp4,.webm,.ogg,.mov,.avi,.m4v" :http-request="opt => doUploadImage(item, opt)">
            <el-button size="mini" icon="el-icon-picture-outline" :loading="uploadingImg">添加参考</el-button>
          </el-upload>
          <span class="sie-tip">支持图片（jpg / png / gif 等）或视频（mp4 / webm / mov 等），单个 ≤ 20MB</span>
        </div>
      </div>

      <div class="sie-row">
        <label class="sie-label">附件</label>
        <div class="sie-up">
          <div v-for="(a, i) in item.attachments" :key="i" class="sie-file">
            <i class="el-icon-paperclip" />
            <a :href="baseApi + a.url" target="_blank" class="sie-file-name">{{ a.name }}</a>
            <i class="el-icon-close sie-file-del" @click="removeAttachment(item, i)" />
          </div>
          <el-upload action="#" :show-file-list="false" :multiple="true" :http-request="opt => doUpload(item, opt)">
            <el-button size="mini" icon="el-icon-upload2" :loading="uploading">添加附件</el-button>
          </el-upload>
          <span class="sie-tip">可多个，单个 ≤ 1024MB</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { uploadFile } from '@/api/business/exam'

/**
 * 实操考核题目清单编辑器
 *
 * 实操不从题库抽题：管理员按「列表」形式逐条填写
 * 题干 → 题目描述 → 参考 → 附件 → 本题满分。
 *
 * 用法：<subject-item-editor v-model="form.subjectItems" />
 * 数据项结构：{ id, title, description, score, images:[{name,url}], attachments:[{name,url}] }
 */
export default {
  name: 'SubjectItemEditor',
  props: {
    /** 题目清单；直接就地修改（结构变化时 emit input 通知父级） */
    value: { type: Array, default: () => [] }
  },
  data() {
    return { uploading: false, uploadingImg: false }
  },
  computed: {
    baseApi() { return process.env.VUE_APP_BASE_API || '' },
    items() { return this.value || [] },
    totalScore() {
      const sum = this.items.reduce((acc, it) => acc + (Number(it.score) || 0), 0)
      return Math.round(sum * 100) / 100
    }
  },
  methods: {
    previewList(item) { return (item.images || []).filter(i => !this.isVideo(i.url)).map(i => this.baseApi + i.url) },
    isVideo(url) { return /\.(mp4|webm|ogg|ogv|mov|avi|m4v)$/i.test(String(url || '')) },
    emptyItem() {
      return { id: null, title: '', description: '', score: 0, images: [], attachments: [] }
    },
    addItem() {
      this.items.push(this.emptyItem())
      this.$emit('input', this.items)
    },
    removeItem(index) {
      this.items.splice(index, 1)
      this.$emit('input', this.items)
    },
    move(index, delta) {
      const target = index + delta
      if (target < 0 || target >= this.items.length) return
      const arr = this.items
      const tmp = arr[index]
      arr.splice(index, 1)
      arr.splice(target, 0, tmp)
      this.$emit('input', arr)
    },
    doUploadImage(item, option) {
      const file = option.file
      const isVideoFile = /\.(mp4|webm|ogg|ogv|mov|avi|m4v)$/i.test(file.name)
      const isImage = /\.(jpg|jpeg|png|gif|webp|bmp)$/i.test(file.name)
      if (!isVideoFile && !isImage) {
        this.$modal.msgWarning('参考仅支持图片（jpg / png / gif / webp / bmp）或视频（mp4 / webm / mov / avi 等）')
        return
      }
      const limit = isVideoFile ? 200 * 1024 * 1024 : 20 * 1024 * 1024
      if (file.size > limit) {
        this.$modal.msgWarning(isVideoFile ? '单个视频不能超过 200MB' : '单张图片不能超过 20MB')
        return
      }
      const formData = new FormData()
      formData.append('file', file)
      this.uploadingImg = true
      uploadFile(formData).then(res => {
        item.images.push({ name: file.name, url: res.fileName })
        this.uploadingImg = false
      }).catch(() => { this.uploadingImg = false })
    },
    doUpload(item, option) {
      const file = option.file
      if (file.size > 1024 * 1024 * 1024) {
        this.$modal.msgWarning('单个附件不能超过 1024MB')
        return
      }
      const formData = new FormData()
      formData.append('file', file)
      this.uploading = true
      uploadFile(formData).then(res => {
        item.attachments.push({ name: file.name, url: res.fileName })
        this.uploading = false
      }).catch(() => { this.uploading = false })
    },
    removeImage(item, index) { item.images.splice(index, 1) },
    removeAttachment(item, index) { item.attachments.splice(index, 1) }
  }
}
</script>

<style lang="scss" scoped>
.sie-bar { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 9px 12px; margin-bottom: 10px; background: #f7f9fc; border: 1px solid #eef1f6; border-radius: 6px; }
.sie-bar-l { display: flex; align-items: baseline; gap: 14px; }
.sie-count { color: #1d2939; font-size: 12.5px; font-weight: 600; }
.sie-total { color: #1764f5; font-size: 12px; }
.sie-empty { display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 26px 14px; color: #98a2b3; border: 1px dashed #dfe5ee; border-radius: 8px; font-size: 12.5px; }
.sie-empty i { color: #b7c1cc; font-size: 26px; }

.sie-item { padding: 12px 14px 14px; margin-bottom: 12px; background: #fff; border: 1px solid #e7ecf3; border-left: 3px solid #1764f5; border-radius: 7px; }
.sie-item-head { display: flex; align-items: center; gap: 9px; padding-bottom: 10px; margin-bottom: 11px; border-bottom: 1px solid #f0f3f7; }
.sie-seq { display: inline-flex; width: 22px; height: 22px; align-items: center; justify-content: center; color: #fff; background: #1764f5; font-size: 12px; font-weight: 700; border-radius: 5px; }
.sie-item-title { flex: 1; min-width: 0; overflow: hidden; color: #1d2939; font-size: 13px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.sie-item-act { flex: none; }

.sie-row { display: flex; align-items: flex-start; gap: 10px; margin-bottom: 10px; }
.sie-row:last-child { margin-bottom: 0; }
.sie-label { flex: none; width: 62px; padding-top: 7px; color: #667085; font-size: 12.5px; text-align: right; }
.sie-label.required::before { content: '*'; margin-right: 3px; color: #f56c6c; }
.sie-row .el-input, .sie-row .el-textarea { flex: 1; }
.sie-tip { padding-top: 7px; color: #98a2b3; font-size: 11.5px; }
.sie-up { flex: 1; min-width: 0; }
.sie-img-grid { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 8px; }
.sie-img { position: relative; width: 88px; height: 62px; overflow: hidden; border: 1px solid #eef1f6; border-radius: 6px; }
.sie-img-thumb { width: 100%; height: 100%; display: block; object-fit: cover; background: #000; }
.sie-img-del { position: absolute; top: 2px; right: 2px; width: 16px; height: 16px; line-height: 16px; color: #fff; background: rgba(0, 0, 0, .55); font-size: 11px; text-align: center; border-radius: 50%; cursor: pointer; }
.sie-file { display: flex; align-items: center; gap: 7px; margin-bottom: 4px; font-size: 12.5px; }
.sie-file i.el-icon-paperclip { color: #98a2b3; }
.sie-file-name { flex: 1; min-width: 0; overflow: hidden; color: #1764f5; text-overflow: ellipsis; white-space: nowrap; }
.sie-file-del { color: #f56c6c; cursor: pointer; }
.danger-text { color: #f56c6c; }
</style>
