<template>
  <div class="practice-subject-page app-container">
    <header class="page-heading">
      <div>
        <div class="eyebrow">学习考核 / 部门运营</div>
        <div class="title-line">
          <h2>模拟实操管理</h2>
          <el-tag size="mini" effect="plain" :type="readOnly ? 'info' : 'success'">{{ readOnly ? '全局只读' : '本部门范围' }}</el-tag>
        </div>
        <p>{{ readOnly ? '查看各部门已发布的模拟实操题。超级管理员不参与实操题的发布维护。' : '发布模拟考核的实操题目（题干必填，附件可选、可上传多个）。实习生可在「模拟考核 → 实操练习」查看题干并下载附件，无需上传作答。' }}</p>
      </div>
      <div class="heading-actions">
        <el-button icon="el-icon-refresh" size="small" @click="loadList">刷新数据</el-button>
        <el-button v-if="!readOnly" v-hasPermi="['business:psubject:add']" type="primary" icon="el-icon-plus" size="small" @click="handleAdd">发布实操题</el-button>
      </div>
    </header>

    <section class="scope-strip">
      <div class="scope-main">
        <span class="scope-icon"><i class="el-icon-documentation" /></span>
        <div>
          <strong>{{ readOnly ? '全局实操题视图' : (deptName || '当前部门') }}</strong>
          <span>{{ readOnly ? '可查看各部门已发布的实操题' : '仅维护本部门实操题；启用的题目实习生可见' }}</span>
        </div>
      </div>
      <div class="scope-meta"><span><i class="el-icon-lock" /> 数据范围由登录账号决定</span></div>
    </section>

    <section class="content-panel">
      <div class="panel-heading">
        <div><h3>实操题目录</h3><p>题干必填；附件非必须，可上传多个（文档 / 压缩包 / 图片等）。</p></div>
      </div>

      <el-form :inline="true" size="small" class="query-form" @submit.native.prevent>
        <el-form-item label="关键词">
          <el-input v-model="queryParams.content" clearable placeholder="题干关键词" style="width:200px" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" clearable placeholder="全部" style="width:120px">
            <el-option label="启用" :value="1" /><el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleQuery">查询</el-button>
          <el-button icon="el-icon-refresh-left" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="list" stripe empty-text="暂无实操题，点击“发布实操题”开始配置">
        <el-table-column label="题干" min-width="320" show-overflow-tooltip>
          <template slot-scope="scope">
            <div class="content-cell">
              <span class="seq">{{ (queryParams.pageNum - 1) * queryParams.pageSize + scope.$index + 1 }}</span>
              <span class="content-text">{{ scope.row.content }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="附件" width="200">
          <template slot-scope="scope">
            <span v-if="!parseAttachments(scope.row.attachmentsJson).length" class="muted">无附件</span>
            <el-tooltip v-else effect="dark" placement="top">
              <div slot="content">
                <div v-for="(a, i) in parseAttachments(scope.row.attachmentsJson)" :key="i">{{ a.name }}</div>
              </div>
              <span class="attach-count"><i class="el-icon-paperclip" /> {{ parseAttachments(scope.row.attachmentsJson).length }} 个</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="所属部门" min-width="120">
          <template slot-scope="scope"><span class="dept-text"><i class="el-icon-office-building" />{{ scope.row.deptName || '未设置' }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template slot-scope="scope">
            <el-tag size="mini" effect="plain" :type="scope.row.status === 1 ? 'success' : 'info'">{{ scope.row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="160" align="center">
          <template slot-scope="scope">{{ fmtTime(scope.row.updateTime || scope.row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="210" align="center">
          <template slot-scope="scope">
            <el-button v-if="!readOnly" v-hasPermi="['business:psubject:edit']" type="text" size="mini" icon="el-icon-edit" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button v-if="!readOnly" v-hasPermi="['business:psubject:edit']" type="text" size="mini" :icon="scope.row.status === 1 ? 'el-icon-turn-off' : 'el-icon-open'" @click="handleToggleStatus(scope.row)">{{ scope.row.status === 1 ? '停用' : '启用' }}</el-button>
            <el-button v-if="!readOnly" v-hasPermi="['business:psubject:remove']" type="text" size="mini" icon="el-icon-delete" class="danger-text" @click="handleDelete(scope.row)">删除</el-button>
            <span v-if="readOnly" class="muted">只读</span>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="loadList" />
    </section>

    <!-- 发布 / 编辑弹窗 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="680px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="题干" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="5" placeholder="请输入实操题题干 / 操作要求" maxlength="5000" show-word-limit />
        </el-form-item>

        <el-form-item label="附件">
          <div class="attach-box">
            <div v-if="!form.attachments.length" class="attach-empty">未添加附件（非必须）</div>
            <div v-for="(a, i) in form.attachments" :key="i" class="attach-item">
              <i class="el-icon-paperclip" />
              <a :href="baseApi + a.url" target="_blank" class="attach-name">{{ a.name }}</a>
              <el-button type="text" size="mini" icon="el-icon-close" class="attach-del" @click="removeAttachment(i)" />
            </div>
            <el-upload
              class="attach-upload"
              action="#"
              :show-file-list="false"
              :multiple="true"
              :http-request="doUpload"
            >
              <el-button size="mini" icon="el-icon-upload2" :loading="uploading">添加附件</el-button>
            </el-upload>
            <div class="attach-tip">可上传多个附件，单个文件不超过 1024MB。</div>
          </div>
        </el-form-item>

        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用（实习生可见）</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {
  listPracticeSubject, addPracticeSubject, updatePracticeSubject,
  changePracticeSubjectStatus, delPracticeSubject, uploadFile
} from '@/api/business/practiceSubject'
import { parseTime } from '@/utils/ruoyi'
import { mapGetters } from 'vuex'

export default {
  name: 'PracticeSubject',
  data() {
    return {
      loading: false,
      list: [],
      total: 0,
      queryParams: { pageNum: 1, pageSize: 10, content: '', status: undefined },
      dialogVisible: false,
      dialogTitle: '',
      form: { id: null, content: '', attachments: [], status: 1 },
      rules: {
        content: [{ required: true, message: '请输入题干', trigger: 'blur' }]
      },
      uploading: false,
      submitting: false
    }
  },
  computed: {
    ...mapGetters(['deptName', 'roles']),
    readOnly() { return this.roles.indexOf('SUPER_ADMIN') > -1 },
    baseApi() { return process.env.VUE_APP_BASE_API || '' }
  },
  created() {
    this.loadList()
  },
  methods: {
    loadList() {
      this.loading = true
      listPracticeSubject(this.queryParams).then(res => {
        this.list = res.rows || []
        this.total = res.total || 0
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.loadList()
    },
    resetQuery() {
      this.queryParams = { pageNum: 1, pageSize: 10, content: '', status: undefined }
      this.loadList()
    },
    handleAdd() {
      this.form = { id: null, content: '', attachments: [], status: 1 }
      this.dialogTitle = '发布实操题'
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
    },
    handleEdit(row) {
      this.form = {
        id: row.id,
        content: row.content,
        attachments: this.parseAttachments(row.attachmentsJson).slice(),
        status: row.status
      }
      this.dialogTitle = '编辑实操题'
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
    },
    doUpload(option) {
      const file = option.file
      if (file.size > 1024 * 1024 * 1024) {
        this.$modal.msgWarning('单个附件不能超过 1024MB')
        return
      }
      const formData = new FormData()
      formData.append('file', file)
      this.uploading = true
      uploadFile(formData).then(res => {
        this.form.attachments.push({ name: file.name, url: res.fileName })
        this.uploading = false
        this.$modal.msgSuccess('附件上传成功')
      }).catch(() => { this.uploading = false })
    },
    removeAttachment(index) {
      this.form.attachments.splice(index, 1)
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.submitting = true
        const payload = {
          id: this.form.id,
          content: this.form.content,
          status: this.form.status,
          // 注意：清空附件时必须回传空串而不是 null，否则后端 <if test="attachmentsJson != null"> 不会更新，旧附件会残留
          attachmentsJson: this.form.attachments.length ? JSON.stringify(this.form.attachments) : ''
        }
        const fn = payload.id ? updatePracticeSubject : addPracticeSubject
        fn(payload).then(() => {
          this.$modal.msgSuccess(payload.id ? '保存成功' : '发布成功')
          this.dialogVisible = false
          this.submitting = false
          this.loadList()
        }).catch(() => { this.submitting = false })
      })
    },
    handleToggleStatus(row) {
      const next = row.status === 1 ? 0 : 1
      const label = next === 1 ? '启用' : '停用'
      this.$modal.confirm(`确认${label}该实操题吗？`).then(() => {
        changePracticeSubjectStatus(row.id, next).then(() => {
          this.$modal.msgSuccess(label + '成功')
          this.loadList()
        })
      }).catch(() => {})
    },
    handleDelete(row) {
      this.$modal.confirm('确认删除该实操题吗？删除后实习生将不再看到。').then(() => {
        delPracticeSubject(row.id).then(() => {
          this.$modal.msgSuccess('删除成功')
          this.loadList()
        })
      }).catch(() => {})
    },
    parseAttachments(json) {
      if (!json) return []
      try {
        const arr = JSON.parse(json)
        return Array.isArray(arr) ? arr : []
      } catch (e) { return [] }
    },
    fmtTime(val) {
      return val ? parseTime(val, '{y}-{m}-{d} {h}:{i}') : '-'
    }
  }
}
</script>

<style lang="scss" scoped>
.page-heading { margin-bottom: 18px; }
.eyebrow { color: #1764f5; font-size: 12px; letter-spacing: .05em; }
.title-line { display: flex; align-items: center; gap: 10px; }
.page-heading h2 { margin: 6px 0 8px; font-size: 22px; font-weight: 600; color: #1d2939; }
.page-heading p { margin: 0; max-width: 760px; color: #667085; font-size: 13px; line-height: 1.6; }
.heading-actions { text-align: right; }
.scope-strip { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; padding: 14px 18px; border: 1px solid #e7ecf3; border-radius: 6px; background: #fff; }
.scope-main { display: flex; align-items: center; gap: 12px; }
.scope-icon { display: flex; width: 38px; height: 38px; align-items: center; justify-content: center; color: #1764f5; background: #edf4ff; font-size: 19px; border-radius: 6px; }
.scope-main strong, .scope-main span { display: block; }
.scope-main strong { color: #1d2939; font-size: 14px; }
.scope-main span { color: #8490a0; font-size: 12px; margin-top: 2px; }
.scope-meta { color: #8490a0; font-size: 12px; }
.content-panel { padding: 18px 20px; background: #fff; border: 1px solid #e7ecf3; border-radius: 6px; }
.panel-heading { margin-bottom: 14px; }
.panel-heading h3 { margin: 0 0 5px; font-size: 16px; font-weight: 600; color: #1d2939; }
.panel-heading p { margin: 0; color: #8490a0; font-size: 12px; }
.query-form { padding: 4px 0 10px; border-bottom: 1px solid #edf0f4; }
.content-cell { display: flex; align-items: center; gap: 10px; }
.seq { flex: none; display: inline-flex; width: 20px; height: 20px; align-items: center; justify-content: center; color: #667085; font-size: 11px; background: #f0f2f5; border-radius: 4px; }
.content-text { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.attach-count { color: #1764f5; font-size: 13px; cursor: default; }
.attach-count i { margin-right: 3px; }
.dept-text { color: #475467; font-size: 13px; }
.dept-text i { margin-right: 4px; color: #98a2b3; }
.muted { color: #b7c1cc; font-size: 12px; }
.danger-text { color: #f56c6c; }
.attach-box { padding: 10px 12px; border: 1px dashed #dfe5ee; border-radius: 6px; background: #fbfcfe; }
.attach-empty { color: #b7c1cc; font-size: 12px; }
.attach-item { display: flex; align-items: center; gap: 8px; padding: 5px 0; font-size: 13px; }
.attach-item i.el-icon-paperclip { color: #98a2b3; }
.attach-name { flex: 1; overflow: hidden; color: #1764f5; text-overflow: ellipsis; white-space: nowrap; }
.attach-del { color: #f56c6c; padding: 0; }
.attach-upload { display: inline-block; margin-top: 8px; }
.attach-tip { margin-top: 6px; color: #98a2b3; font-size: 12px; }
</style>
