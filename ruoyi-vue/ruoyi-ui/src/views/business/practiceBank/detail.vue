<template>
  <div class="pbank-detail app-container">
    <header class="page-heading">
      <div>
        <div class="eyebrow">学习考核 / 题库管理 · 实操题库</div>
        <div class="title-line">
          <h2>{{ bankName || '实操题库' }}</h2>
          <el-tag size="mini" effect="plain" type="warning">实操题库</el-tag>
          <el-tag v-if="bankTypeText" size="mini" effect="plain">{{ bankTypeText }}</el-tag>
        </div>
        <p>实操题 = 逐题作业：题干 / 技能方向 / 交付要求 / 参考图·视频 / 附件 / 建议满分。考核配置时从本库挑题（不做随机抽题）。</p>
      </div>
      <div class="heading-actions">
        <el-button size="mini" icon="el-icon-back" @click="goBack">返回题库列表</el-button>
        <el-button size="mini" icon="el-icon-refresh" @click="loadList">刷新</el-button>
        <el-button size="mini" type="primary" icon="el-icon-plus" @click="openDialog(null)">新增实操题</el-button>
      </div>
    </header>

    <section class="kpi-row">
      <div class="kpi-card"><span class="kpi-value">{{ total }}<small>道</small></span><span class="kpi-label">实操题</span></div>
      <div class="kpi-card tone-green"><span class="kpi-value">{{ enabledCount }}<small>道</small></span><span class="kpi-label">已启用</span></div>
      <div class="kpi-card tone-purple"><span class="kpi-value">{{ directionCount }}<small>个</small></span><span class="kpi-label">覆盖方向</span></div>
      <div class="kpi-card tone-orange"><span class="kpi-value">{{ scoredCount }}<small>道</small></span><span class="kpi-label">已设建议满分</span></div>
    </section>

    <section class="filter-bar">
      <el-input v-model="query.keyword" size="small" clearable prefix-icon="el-icon-search" placeholder="搜索题名 / 方向 / 题干" class="filter-keyword" />
      <el-select v-model="query.direction" size="small" clearable placeholder="全部方向" class="filter-select">
        <el-option v-for="d in directionOptions" :key="d" :label="d" :value="d" />
      </el-select>
      <el-select v-model="query.status" size="small" clearable placeholder="全部状态" class="filter-select">
        <el-option label="启用" :value="1" />
        <el-option label="停用" :value="0" />
      </el-select>
      <span class="filter-summary">共 {{ total }} 道</span>
    </section>

    <section class="content-panel">
      <el-table :key="'pb-table'" v-loading="loading" :data="filteredList" size="small" stripe>
        <el-table-column label="题名 / 方向" min-width="240">
          <template slot-scope="scope">
            <div class="tt-cell">
              <strong>{{ scope.row.title }}</strong>
              <small>{{ scope.row.direction || '未分方向' }}<template v-if="scope.row.chapter"> · {{ scope.row.chapter }}</template></small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="难度" width="86" align="center">
          <template slot-scope="scope"><el-tag size="mini" effect="plain">{{ diffText(scope.row.difficulty) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="建议用时" width="94" align="center">
          <template slot-scope="scope">{{ scope.row.estimatedMinutes ? scope.row.estimatedMinutes + ' 分钟' : '—' }}</template>
        </el-table-column>
        <el-table-column label="建议满分" width="94" align="center">
          <template slot-scope="scope">
            <strong class="num">{{ scope.row.suggestScore != null ? scope.row.suggestScore : '—' }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="素材" width="96" align="center">
          <template slot-scope="scope">
            <span class="muted">图 {{ imgCount(scope.row) }} · 视 {{ vidCount(scope.row) }} · 附 {{ attCount(scope.row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="82" align="center">
          <template slot-scope="scope">
            <el-tag size="mini" :type="scope.row.status === 1 ? 'success' : 'info'">{{ scope.row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center">
          <template slot-scope="scope">
            <el-button type="text" size="mini" icon="el-icon-edit" @click="openDialog(scope.row)">编辑</el-button>
            <el-button type="text" size="mini" @click="toggle(scope.row)">{{ scope.row.status === 1 ? '停用' : '启用' }}</el-button>
            <el-button type="text" size="mini" icon="el-icon-delete" class="danger-text" @click="remove(scope.row)">删除</el-button>
          </template>
        </el-table-column>
        <template slot="empty">
          <div class="empty-block">
            <i class="el-icon-document" />
            <p>{{ list.length ? '当前筛选条件下没有匹配的实操题' : '本实操题库还没有题目，点右上角「新增实操题」开始录入' }}</p>
          </div>
        </template>
      </el-table>
    </section>

    <el-dialog
      :title="(form.id ? '编辑实操题' : '新增实操题') + (bankName ? ' · ' + bankName : '')"
      :visible.sync="visible"
      width="960px"
      top="4vh"
      append-to-body
      :close-on-click-modal="false"
      class="pbank-dialog"
    >
      <el-form ref="form" :model="form" :rules="rules" label-width="98px" size="small">
        <div class="sec"><span class="sec-no">1</span>题目信息<i class="sec-tip">题名与技能方向对实习生可见</i></div>
        <el-form-item label="题名" prop="title">
          <el-input v-model="form.title" maxlength="200" placeholder="例如：用户登录接口开发（会展示给实习生）" />
        </el-form-item>
        <div class="fg4 stack">
          <el-form-item label="技能方向"><el-input v-model="form.direction" maxlength="64" placeholder="如：后端接口开发" /></el-form-item>
          <el-form-item label="难度">
            <el-radio-group v-model="form.difficulty" size="mini" class="diff-seg">
              <el-radio-button label="EASY">简单</el-radio-button>
              <el-radio-button label="MEDIUM">中等</el-radio-button>
              <el-radio-button label="HARD">困难</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="建议用时"><el-input-number v-model="form.estimatedMinutes" :min="0" :max="600" controls-position="right" style="width:100%" /><span class="unit">分钟</span></el-form-item>

          <el-form-item label="建议满分"><el-input-number v-model="form.suggestScore" :min="0" :max="999" :precision="1" controls-position="right" placeholder="未填" style="width:100%" /><span class="unit">分</span></el-form-item>
        </div>
        <div class="fg2 stack2">
          <el-form-item label="章节">
            <el-input v-model="form.chapter" maxlength="64" placeholder="与理论题库口径统一（可选）" />
          </el-form-item>
          <el-form-item label="方向说明">
            <el-input v-model="form.directionDesc" maxlength="255" placeholder="一句话说明该方向的考察点（可选）" />
          </el-form-item>
        </div>

        <div class="sec"><span class="sec-no">2</span>题目与要求<i class="sec-tip">交付要求 / 开发约束：一行一条</i></div>
        <el-form-item label="题干">
          <el-input v-model="form.content" type="textarea" :rows="4" placeholder="作业背景与要求：要做成什么、验收标准是什么" />
        </el-form-item>
        <div class="fg2">
          <el-form-item label="交付要求">
            <el-input v-model="form.deliverables" type="textarea" :rows="4" placeholder="一行一条&#10;接口代码&#10;接口文档（含入参出参）" />
          </el-form-item>
          <el-form-item label="开发约束">
            <el-input v-model="form.devConstraints" type="textarea" :rows="4" placeholder="一行一条&#10;使用 SpringBoot&#10;不得引入新依赖" />
          </el-form-item>
        </div>
        <div class="fg2">
          <el-form-item label="提交格式"><el-input v-model="form.submitFormat" maxlength="255" placeholder="如：zip 压缩包" /></el-form-item>
          <el-form-item label="命名规则"><el-input v-model="form.namingRule" maxlength="255" placeholder="如：姓名_学号_题名.zip" /></el-form-item>
        </div>

        <div class="sec"><span class="sec-no">3</span>素材<i class="sec-tip">参考图 / 视频给实习生看效果</i></div>
        <el-form-item label="参考图/视频">
          <el-upload :show-file-list="false" :http-request="uploadRefImage" accept="image/*,video/*" class="inline-up">
            <el-button size="mini" icon="el-icon-picture-outline">上传图片 / 视频</el-button>
          </el-upload>
          <span class="unit block">支持图片与视频（mp4 / webm 等），单文件最大 500MB</span>
          <div class="files">
            <span v-for="(f, i) in form.images" :key="'img' + i" class="filechip" :class="{ video: isVideo(f.url) }">
              <i :class="isVideo(f.url) ? 'el-icon-video-camera' : 'el-icon-picture-outline'" />
              <a :href="baseApi + f.url" target="_blank">{{ f.name || (isVideo(f.url) ? '视频' : '图片') }}</a>
              <i class="el-icon-close" @click="form.images.splice(i, 1)" />
            </span>
            <span v-if="!form.images.length" class="muted">（未上传）</span>
          </div>
        </el-form-item>
        <el-form-item label="附件">
          <el-upload :show-file-list="false" :http-request="uploadAttachment" class="inline-up">
            <el-button size="mini" icon="el-icon-upload2">上传附件</el-button>
          </el-upload>
          <span class="unit block">起始素材 / 说明文档、数据包等（图片与视频请放上面的参考图区）</span>
          <div class="files">
            <span v-for="(f, i) in form.attachments" :key="'att' + i" class="filechip">
              <a :href="baseApi + f.url" target="_blank">{{ f.name || '附件' }}</a>
              <i class="el-icon-close" @click="form.attachments.splice(i, 1)" />
            </span>
            <span v-if="!form.attachments.length" class="muted">（未上传）</span>
          </div>
        </el-form-item>

        <div class="sec"><span class="sec-no">4</span>其它</div>
        <div class="fg2">
          <el-form-item label="排序号">
            <el-input-number v-model="form.sortNo" :min="0" :max="9999" controls-position="right" style="width:130px" />
            <span class="unit">越小越靠前</span>
          </el-form-item>
          <el-form-item label="状态">
            <el-radio-group v-model="form.status"><el-radio :label="1">启用</el-radio><el-radio :label="0">停用</el-radio></el-radio-group>
          </el-form-item>
        </div>
      </el-form>
      <span slot="footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {
  listPracticeSubject, getPracticeSubject, addPracticeSubject, updatePracticeSubject,
  changePracticeSubjectStatus, delPracticeSubject, uploadFile
} from '@/api/business/practiceSubject'

function parseJsonList(v) {
  if (!v) return []
  try {
    const a = typeof v === 'string' ? JSON.parse(v) : v
    return Array.isArray(a) ? a : []
  } catch (e) { return [] }
}

export default {
  name: 'PracticeBankDetail',
  data() {
    return {
      bankId: null,
      bankName: '',
      bankTypeText: '',
      loading: false,
      list: [],
      total: 0,
      query: { keyword: '', direction: '', status: null },
      visible: false,
      saving: false,
      form: this.emptyForm(),
      rules: { title: [{ required: true, message: '请输入题名', trigger: 'blur' }] }
    }
  },
  computed: {
    baseApi() { return process.env.VUE_APP_BASE_API || '' },
    filteredList() {
      const kw = (this.query.keyword || '').trim().toLowerCase()
      return (this.list || []).filter(r => {
        if (kw) {
          const hay = `${r.title || ''} ${r.direction || ''} ${r.content || ''}`.toLowerCase()
          if (hay.indexOf(kw) === -1) return false
        }
        if (this.query.direction && r.direction !== this.query.direction) return false
        if (this.query.status !== null && this.query.status !== '' && Number(r.status) !== Number(this.query.status)) return false
        return true
      })
    },
    directionOptions() {
      const s = new Set()
      ;(this.list || []).forEach(r => { if (r.direction) s.add(r.direction) })
      return Array.from(s)
    },
    directionCount() { return this.directionOptions.length },
    enabledCount() { return (this.list || []).filter(r => Number(r.status) === 1).length },
    scoredCount() { return (this.list || []).filter(r => r.suggestScore != null).length }
  },
  created() {
    this.bankId = this.$route.params.bankId
    this.bankName = this.$route.query.bankName || ''
    this.bankTypeText = { FORMAL: '正式考核题库', PRACTICE: '模拟考核题库', COMMON: '通用题库' }[this.$route.query.bankType] || ''
    this.loadList()
  },
  methods: {
    loadList() {
      this.loading = true
      listPracticeSubject({ bankId: this.bankId, pageNum: 1, pageSize: 200 }).then(res => {
        this.list = res.rows || []
        this.total = res.total || this.list.length
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    emptyForm() {
      return {
        id: null, title: '', direction: '', directionDesc: '', chapter: '',
        difficulty: 'MEDIUM', estimatedMinutes: 60, suggestScore: null,
        content: '', deliverables: '', devConstraints: '', submitFormat: '', namingRule: '',
        images: [], attachments: [], sortNo: 0, status: 1
      }
    },
    openDialog(row) {
      if (!row) {
        this.form = this.emptyForm()
        this.visible = true
        this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
        return
      }
      getPracticeSubject(row.id).then(res => {
        const d = res.data || row
        this.form = {
          id: d.id, title: d.title || '', direction: d.direction || '', directionDesc: d.directionDesc || '',
          chapter: d.chapter || '', difficulty: d.difficulty || 'MEDIUM',
          estimatedMinutes: d.estimatedMinutes == null ? 60 : d.estimatedMinutes,
          suggestScore: d.suggestScore == null ? null : Number(d.suggestScore),
          content: d.content || '', deliverables: d.deliverables || '', devConstraints: d.devConstraints || '',
          submitFormat: d.submitFormat || '', namingRule: d.namingRule || '',
          images: parseJsonList(d.referenceImages), attachments: parseJsonList(d.attachmentsJson),
          sortNo: d.sortNo || 0, status: d.status == null ? 1 : d.status
        }
        this.visible = true
      })
    },
    doUpload(file, cb) {
      const fd = new FormData()
      fd.append('file', file)
      uploadFile(fd).then(res => {
        const url = res.url || (res.data && res.data.url) || ''
        cb({ name: file.name, url })
      }).catch(() => this.$modal.msgError('上传失败'))
    },
    uploadRefImage(opt) { this.doUpload(opt.file, f => { this.form.images.push(f) }) },
    uploadAttachment(opt) { this.doUpload(opt.file, f => { this.form.attachments.push(f) }) },
    submit() {
      this.$refs.form.validate(ok => {
        if (!ok) return
        const payload = {
          id: this.form.id, bankId: Number(this.bankId),
          title: (this.form.title || '').trim(), direction: this.form.direction, directionDesc: this.form.directionDesc,
          chapter: this.form.chapter, difficulty: this.form.difficulty,
          estimatedMinutes: this.form.estimatedMinutes, suggestScore: this.form.suggestScore,
          content: this.form.content, deliverables: this.form.deliverables, devConstraints: this.form.devConstraints,
          submitFormat: this.form.submitFormat, namingRule: this.form.namingRule,
          referenceImages: this.form.images.length ? JSON.stringify(this.form.images) : '',
          attachmentsJson: this.form.attachments.length ? JSON.stringify(this.form.attachments) : '',
          sortNo: this.form.sortNo, status: this.form.status
        }
        this.saving = true
        const fn = payload.id ? updatePracticeSubject : addPracticeSubject
        fn(payload).then(() => {
          this.saving = false
          this.visible = false
          this.$modal.msgSuccess('保存成功')
          this.loadList()
        }).catch(() => { this.saving = false })
      })
    },
    toggle(row) {
      const next = Number(row.status) === 1 ? 0 : 1
      changePracticeSubjectStatus(row.id, next).then(() => { this.$modal.msgSuccess('已更新'); this.loadList() })
    },
    remove(row) {
      this.$modal.confirm(`确认删除实操题「${row.title}」吗？`).then(() => {
        delPracticeSubject(row.id).then(() => { this.$modal.msgSuccess('删除成功'); this.loadList() })
      }).catch(() => {})
    },
    isVideo(url) { return /\.(mp4|webm|ogg|ogv|mov|avi|m4v)(\?|#|$)/i.test(String(url || '')) },
    imgCount(row) { return parseJsonList(row.referenceImages).filter(f => !this.isVideo(f.url)).length },
    vidCount(row) { return parseJsonList(row.referenceImages).filter(f => this.isVideo(f.url)).length },
    attCount(row) { return parseJsonList(row.attachmentsJson).length },
    diffText(d) { return { EASY: '简单', MEDIUM: '中等', HARD: '困难' }[d] || '中等' },
    goBack() {
      this.$router.push(this.$route.path.indexOf('/super') === 0 ? '/super/ops/banks' : '/department/study/banks').catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.page-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 14px; }
.eyebrow { color: #1764f5; font-size: 12px; letter-spacing: .05em; }
.title-line { display: flex; align-items: center; gap: 9px; flex-wrap: wrap; }
.page-heading h2 { margin: 6px 0 8px; font-size: 21px; font-weight: 600; color: #1d2939; }
.page-heading p { margin: 0; color: #667085; font-size: 13px; }
.heading-actions { flex: none; display: flex; gap: 8px; }
.kpi-row { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin-bottom: 12px; }
.kpi-card { padding: 12px 16px; background: #fff; border: 1px solid #e7ecf3; border-radius: 6px; }
.kpi-value { display: block; color: #1d2939; font-size: 20px; font-weight: 600; }
.kpi-value small { margin-left: 3px; color: #667085; font-size: 12px; font-weight: 400; }
.kpi-label { color: #667085; font-size: 12px; }
.kpi-card.tone-green .kpi-value { color: #23966f; }
.kpi-card.tone-purple .kpi-value { color: #7b5cf0; }
.kpi-card.tone-orange .kpi-value { color: #e6a23c; }
.filter-bar { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 12px; padding: 12px 16px; background: #fff; border: 1px solid #e7ecf3; border-radius: 6px; }
.filter-keyword { width: 240px; }
.filter-select { width: 140px; }
.filter-summary { margin-left: auto; color: #667085; font-size: 12px; }
.content-panel { padding: 16px 18px; background: #fff; border: 1px solid #e7ecf3; border-radius: 8px; }
.tt-cell strong { display: block; color: #1d2939; font-size: 13.5px; }
.tt-cell small { display: block; margin-top: 3px; color: #98a2b3; font-size: 12px; }
.num { color: #1764f5; }
.muted { color: #98a2b3; font-size: 12px; }
.danger-text { color: #f56c6c; }
.empty-block { padding: 26px 0; text-align: center; }
.empty-block i { color: #d0d5dd; font-size: 32px; }
.empty-block p { margin: 10px 0 0; color: #8490a0; font-size: 13px; }
.sec { display: flex; align-items: center; gap: 8px; padding: 4px 0 10px; margin: 4px 0 14px; color: #344054; font-size: 13px; font-weight: 600; border-bottom: 1px solid #eef1f6; }
.sec-no { display: inline-flex; width: 18px; height: 18px; align-items: center; justify-content: center; color: #fff; background: #1764f5; font-size: 11px; border-radius: 50%; }
/* 分区提示：只允许单行，过长省略号，避免撑成两行 */
.sec-tip { flex: 1; min-width: 0; margin-left: 12px; overflow: hidden; color: #98a2b3; font-size: 11.5px; font-style: normal; font-weight: 400; text-align: right; text-overflow: ellipsis; white-space: nowrap; }
/* 指标行：标签置顶，四个字段各占满一列（原来左标签只剩 ~107px，难度分段/数字框会被挤换行） */
.fg4 { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px 16px; }
.fg4.stack ::v-deep .el-form-item,
.fg2.stack2 ::v-deep .el-form-item { display: block; margin-bottom: 14px; }
.fg4.stack ::v-deep .el-form-item__label,
.fg2.stack2 ::v-deep .el-form-item__label {
  display: block; width: auto !important; padding: 0 0 5px; color: #667085; font-size: 12px; line-height: 1.4; text-align: left;
}
.fg4.stack ::v-deep .el-form-item__content,
.fg2.stack2 ::v-deep .el-form-item__content { margin-left: 0 !important; line-height: normal; }
/* 难度分段按钮：一列内不再折行 */
.diff-seg ::v-deep .el-radio-button__inner { padding: 7px 12px; white-space: nowrap; }
.unit { margin-left: 6px; color: #98a2b3; font-size: 12px; }
/* 素材区的说明单独占一行，不与按钮挤 */
.unit.block { display: block; margin: 6px 0 0; }
/* 素材 chip 行独占一行 */
.files { display: flex; width: 100%; align-items: center; gap: 8px; flex-wrap: wrap; margin: 8px 0 0; }

.fg2 { display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px; }
.fg3 { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 0 16px; }
.inline-up { display: inline-block; }
.files { display: inline-flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-left: 10px; }
.filechip { display: inline-flex; align-items: center; gap: 4px; padding: 2px 8px; color: #475467; background: #f1f5f9; border-radius: 4px; font-size: 12px; }
.filechip i { color: #98a2b3; cursor: pointer; }
.filechip.video { background: #f2eeff; }
.filechip.video > i:first-child { color: #7b5cf0; }
.filechip i:hover { color: #f56c6c; }
@media (max-width: 1100px) { .kpi-row { grid-template-columns: repeat(2, minmax(0, 1fr)); } .fg2, .fg3 { grid-template-columns: 1fr; } }
</style>
