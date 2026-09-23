<template>
  <div class="dept-page">
    <div class="dept-breadcrumb">
      学习与考核管理 <span>/</span> <b>模拟备考管理</b>
    </div>

    <div class="dept-heading">
      <div>
        <span class="eyebrow">DEPARTMENT ADMIN</span>
        <h1>模拟备考管理</h1>
        <p>备考资料 / <b>模拟理论考核</b>（<b>直接建套卷</b>，每套可用<b>不同的题型配比</b>并标注<b>难易程度</b>与<b>题目内容偏向</b>；抽题来源限定<b>理论题库的模拟库与通用库</b>，发布后实习生可重复练习）/ <b>模拟实操题库</b>（勾选后实习生可浏览题目）。</p>
      </div>
      <div class="dept-heading-actions">
        <el-button size="small" icon="el-icon-refresh" @click="reloadAll">刷新</el-button>
      </div>
    </div>

    <!-- 页签条 -->
    <div class="dtabs page">
      <div
        v-for="tab in tabs"
        :key="tab.key"
        class="dtab"
        :class="{ on: activeTab === tab.key }"
        @click="switchTab(tab.key)"
      >
        {{ tab.label }} <span v-if="tab.count" class="tab-n">{{ tab.count }}</span>
      </div>
    </div>

    <!-- ============ 页签 1 · 备考资料 ============ -->
    <template v-if="activeTab === 'material'">
      <!-- 总览（真实数据：来自已加载的资料列表） -->
      <div class="prep-kpi-row">
        <div v-for="k in materialKpis()" :key="k.label" class="prep-kpi-card" :class="k.tone">
          <span class="prep-kpi-icon"><i :class="k.icon" /></span>
          <span class="prep-kpi-body">
            <span class="prep-kpi-value">{{ k.value }}<small>{{ k.unit }}</small></span>
            <span class="prep-kpi-label">{{ k.label }}</span>
          </span>
        </div>
      </div>

      <div class="dgrid">
        <div class="dcard c5">
          <div class="dcard-h">
            <div class="tt"><span class="idx">传</span><h3>{{ editingId ? '编辑资料' : '上传资料' }}</h3></div>
            <el-button v-if="editingId" size="mini" type="text" @click="resetForm">取消编辑</el-button>
          </div>
          <div class="ddrop" @click="pickFile">
            <i class="el-icon-upload" />
            <b>{{ form.fileName || '点击选择文件' }}</b>
            <small>支持 PDF / Word / PPT / 视频 / 图片 / 压缩包</small>
          </div>
          <input ref="fileInput" type="file" class="d-hidden-input" @change="onFileChange" />
          <div class="dfg2" style="margin-top:14px">
            <div class="dfield">
              <label>资料名称 <b>*</b></label>
              <el-input v-model="form.name" size="small" placeholder="例如：2026Q3 考试指南" />
            </div>
            <div class="dfield">
              <label>资料类型</label>
              <el-select v-model="form.type" size="small" style="width:100%">
                <el-option label="文档 DOCUMENT" value="DOCUMENT" />
                <el-option label="视频 VIDEO" value="VIDEO" />
                <el-option label="模拟题入口 MOCK_ENTRY" value="MOCK_ENTRY" />
              </el-select>
            </div>
            <div class="dfield">
              <label>适用岗位</label>
              <el-select v-model="form.position" size="small" style="width:100%" placeholder="请选择岗位">
                <el-option v-for="p in positionOptions" :key="p.id" :label="p.positionName" :value="p.id" />
              </el-select>
            </div>
            <div class="dfield">
              <label>版本号</label>
              <el-input v-model="form.version" size="small" placeholder="v1.0" />
            </div>
          </div>
          <div class="dfield">
            <label>简介</label>
            <el-input v-model="form.intro" type="textarea" :rows="3" size="small" placeholder="本季度正式考核范围、题型分布与注意事项。" />
          </div>
          <div class="dbtn-row right" style="margin-top:16px">
            <el-button size="small" :loading="saving" @click="saveMaterial(false)">{{ editingId ? '保存修改' : '存为草稿' }}</el-button>
            <el-button size="small" type="primary" :loading="saving" @click="saveMaterial(true)">{{ editingId ? '保存并发布' : '上传并发布' }}</el-button>
          </div>
        </div>

        <div class="dcard c7">
          <div class="dcard-h">
            <div class="tt"><span class="idx">列</span><h3>已上传资料</h3></div>
            <span class="hint-text">发布后本部门对应岗位实习生在「备考资料」页可见</span>
          </div>
          <div class="prep-filter">
            <el-input v-model="materialFilter.keyword" size="mini" clearable prefix-icon="el-icon-search" placeholder="搜索资料名称" style="width:190px" />
            <el-select v-model="materialFilter.type" size="mini" clearable placeholder="全部类型" style="width:136px">
              <el-option label="文档" value="DOCUMENT" />
              <el-option label="视频" value="VIDEO" />
              <el-option label="模拟题入口" value="MOCK_ENTRY" />
            </el-select>
            <el-select v-model="materialFilter.status" size="mini" clearable placeholder="全部状态" style="width:124px">
              <el-option label="已发布" value="PUBLISHED" />
              <el-option label="草稿" value="DRAFT" />
              <el-option label="已停用" value="DISABLED" />
            </el-select>
            <span class="prep-filter-sum">显示 {{ filteredMaterials().length }} / {{ materials.length }} 份</span>
            <el-button size="mini" type="text" icon="el-icon-refresh-left" @click="resetMaterialFilter">重置</el-button>
          </div>
          <table class="dtbl" v-loading="materialLoading">
            <thead>
              <tr>
                <th>资料名称</th>
                <th style="width:96px">类型</th>
                <th style="width:96px">适用岗位</th>
                <th style="width:60px">版本</th>
                <th style="width:74px">状态</th>
                <th style="width:150px">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in filteredMaterials()" :key="row.id">
                <td><span class="strong">{{ row.materialName }}</span></td>
                <td><span class="dbadge" :class="typeTone(row.materialType)">{{ typeText(row.materialType) }}</span></td>
                <td>{{ row.positionName || '-' }}</td>
                <td>{{ row.versionNo || '-' }}</td>
                <td><span class="dbadge" :class="row.status === 'PUBLISHED' ? 'green' : 'gray'">{{ statusText(row.status) }}</span></td>
                <td>
                  <div class="acts">
                    <el-button type="text" @click="previewMaterial(row)">预览</el-button>
                    <span class="sep">|</span>
                    <el-button type="text" @click="editMaterial(row)">替换</el-button>
                    <span class="sep">|</span>
                    <el-button v-if="row.status === 'PUBLISHED'" type="text" @click="changeStatus(row, 'DISABLED')">停用</el-button>
                    <template v-else>
                      <el-button type="text" @click="changeStatus(row, 'PUBLISHED')">发布</el-button>
                      <span class="sep">|</span>
                      <el-button type="text" @click="removeMaterial(row)">删除</el-button>
                    </template>
                  </div>
                </td>
              </tr>
              <tr v-if="!filteredMaterials().length && !materialLoading">
                <td colspan="6" class="d-empty">{{ materials.length ? '当前筛选条件下没有匹配的资料' : '暂无备考资料，请在左侧填写并上传。' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <!-- ============ 页签 3 · 模拟实操题库（管理员勾选开放 + 题目预览）============ -->
    <template v-if="activeTab === 'pbank'">
      <practice-bank-picker />
    </template>

    <!-- ============ 页签 2 · 模拟理论考核（套卷列表，无阶段层级） ============ -->
    <template v-if="activeTab === 'module'">
      <!-- 模拟理论考核：直接就是套卷列表（无阶段层级） -->
      <div class="dsec">
        <div class="dsec-head">
          <div>
            <span class="dsec-no p">卷</span>
            <div>
              <h2>模拟理论考核套卷</h2>
              <p>直接配置套卷：每套可用<b>不同的题型配比</b>，并标注<b>难易程度</b>与<b>题目内容偏向</b>；发布后实习生即可重复练习。抽题来源限定为「理论库 × 模拟/通用用途」。</p>
            </div>
          </div>
          <div style="display:flex;align-items:center;gap:8px">
            <el-button size="mini" icon="el-icon-refresh" @click="loadExamTab">刷新</el-button>
          </div>
        </div>

          <div class="dsec-body">
            <!-- 模拟理论考核：可建多套试卷（题型配比 / 难度 / 内容偏向各异） -->
            <div class="dcard-h" style="padding:0 0 10px">
              <div class="tt"><span class="idx">卷</span><h3>套卷列表（共 {{ exams.length }} 套）</h3></div>
              <div class="dbtn-row">
                <el-button size="mini" type="primary" icon="el-icon-plus" @click="openPaperDialog(null)">新建套卷</el-button>
              </div>
            </div>

            <!-- 总览（全部由已加载的套卷列表计算，不额外请求接口） -->
            <div class="prep-kpi-row">
              <div v-for="k in examKpis()" :key="k.label" class="prep-kpi-card" :class="k.tone">
                <span class="prep-kpi-icon"><i :class="k.icon" /></span>
                <span class="prep-kpi-body">
                  <span class="prep-kpi-value">{{ k.value }}<small>{{ k.unit }}</small></span>
                  <span class="prep-kpi-label">{{ k.label }}</span>
                </span>
              </div>
            </div>

            <!-- 筛选 / 搜索（纯前台过滤） -->
            <div class="prep-filter">
              <el-input v-model="examFilter.keyword" size="mini" clearable prefix-icon="el-icon-search" placeholder="搜索套卷名称 / 描述 / 内容偏向" style="width:260px" />
              <el-select v-model="examFilter.status" size="mini" clearable placeholder="全部状态" style="width:124px">
                <el-option label="已发布" value="PUBLISHED" />
                <el-option label="草稿" value="DRAFT" />
                <el-option label="已停用" value="DISABLED" />
              </el-select>
              <el-select v-model="examFilter.difficulty" size="mini" clearable placeholder="全部难度" style="width:124px">
                <el-option label="简单" value="EASY" />
                <el-option label="中等" value="MEDIUM" />
                <el-option label="困难" value="HARD" />
              </el-select>
              <span class="prep-filter-sum">显示 {{ filteredExams().length }} / {{ exams.length }} 套</span>
              <el-button size="mini" type="text" icon="el-icon-refresh-left" @click="resetExamFilter">重置</el-button>
            </div>

            <el-table :data="filteredExams()" size="mini" border v-loading="examLoading" empty-text="暂无模拟理论考核套卷，点右上角「新建套卷」">
              <el-table-column label="套卷名称" min-width="200">
                <template slot-scope="scope">
                  <span class="strong">{{ scope.row.examName }}</span>
                  <div class="hint-text">{{ scope.row.description || '未填写套卷描述' }}</div>
                </template>
              </el-table-column>
              <el-table-column label="题量" width="150" align="center">
                <template slot-scope="scope">
                  单选 {{ scope.row.singleCount || 0 }} · 多选 {{ scope.row.multiCount || 0 }} · 判断 {{ scope.row.judgeCount || 0 }}
                </template>
              </el-table-column>
              <el-table-column label="分值" width="150" align="center">
                <template slot-scope="scope">
                  {{ scope.row.singleScore || 0 }} / {{ scope.row.multiScore || 0 }} / {{ scope.row.judgeScore || 0 }}
                </template>
              </el-table-column>
              <el-table-column label="时长" width="90" align="center">
                <template slot-scope="scope">{{ Number(scope.row.duration) > 0 ? scope.row.duration + ' 分' : '不限时' }}</template>
              </el-table-column>
              <el-table-column label="通过线" width="86" align="center">
                <template slot-scope="scope">{{ scope.row.passLine || 0 }}</template>
              </el-table-column>
              <el-table-column label="难易程度" width="96" align="center">
                <template slot-scope="scope">
                  <span v-if="scope.row.difficulty" class="dbadge" :class="difficultyTone(scope.row.difficulty)">{{ difficultyText(scope.row.difficulty) }}</span>
                  <span v-else class="muted">未设置</span>
                </template>
              </el-table-column>
              <el-table-column label="题目内容偏向" min-width="180" show-overflow-tooltip>
                <template slot-scope="scope">
                  <span v-if="scope.row.contentBias">{{ scope.row.contentBias }}</span>
                  <span v-else class="muted">—</span>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="86" align="center">
                <template slot-scope="scope">
                  <span class="dbadge" :class="scope.row.status === 'PUBLISHED' ? 'green' : 'gray'">{{ examStatusText(scope.row.status) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="236" align="center">
                <template slot-scope="scope">
                  <el-button type="text" size="mini" @click="goPaperConfig(scope.row)">配置</el-button>
                  <span class="sep">|</span>
                  <el-button type="text" size="mini" @click="openPaperDialog(scope.row)">编辑信息</el-button>
                  <span class="sep">|</span>
                  <el-button v-if="scope.row.status !== 'PUBLISHED'" type="text" size="mini" @click="publishExamRow(scope.row)">发布</el-button>
                  <el-button v-else type="text" size="mini" @click="disableExamRow(scope.row)">停用</el-button>
                  <span class="sep">|</span>
                  <el-button type="text" size="mini" class="danger-text" @click="removeExam(scope.row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>

          </div>
      </div>
    </template>

    <!-- 套卷基础信息弹窗（新建 / 编辑信息） -->
    <el-dialog :title="paperDialogTitle" :visible.sync="paperDialogVisible" width="560px" append-to-body>
      <el-form ref="paperForm" :model="paperForm" :rules="paperRules" label-width="104px" size="small">
        <el-form-item label="套卷名称" prop="examName">
          <el-input v-model="paperForm.examName" placeholder="例如：基础卷 · 集合与并发" maxlength="60" show-word-limit />
        </el-form-item>
        <el-form-item label="套卷描述">
          <el-input v-model="paperForm.description" type="textarea" :rows="3" maxlength="300" show-word-limit placeholder="一句话说明这套卷考什么、适合什么阶段练（实习生可见）" />
        </el-form-item>
        <el-form-item label="难易程度">
          <el-radio-group v-model="paperForm.difficulty">
            <el-radio-button label="EASY">简单</el-radio-button>
            <el-radio-button label="MEDIUM">中等</el-radio-button>
            <el-radio-button label="HARD">困难</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="题目内容偏向">
          <el-input v-model="paperForm.contentBias" maxlength="200" show-word-limit placeholder="给实习生选卷参考，例如：偏 Java 集合与并发，重点考多线程与锁" />
        </el-form-item>
      </el-form>
      <p class="dsec-note" style="margin:0">保存后请在列表中点「<b>配置</b>」进入详细页：设置每题分值 / 时长 / 通过线，配置组卷题库后发布。</p>
      <span slot="footer">
        <el-button @click="paperDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitPaper">{{ paperForm.id ? '保存修改' : '创建套卷' }}</el-button>
      </span>
    </el-dialog>

    <!-- 备考资料附件预览弹窗 -->
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
          <el-button size="mini" type="primary" icon="el-icon-download" @click="downloadPreviewFile">下载附件</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  listExam, addExam, updateExam, delExam, publishExam, changeExamStatus
} from '@/api/business/exam'
import {
  previewKindOf, extLabel, fileUrlOf as resolveFileUrl, PREVIEW_SUPPORT_TEXT, triggerDownload
} from '@/utils/filePreview'
import {
  listMaterial, addMaterial, updateMaterial, changeMaterialStatus, delMaterial, listApplicablePositions, uploadFile
} from '@/api/business/material'
import { mapGetters } from 'vuex'

function emptyPaperForm() {
  return { id: null, examName: '', description: '', difficulty: '', contentBias: '' }
}

import PracticeBankPicker from '@/views/business/practiceBank/PracticeBankPicker'

export default {
  name: 'DeptPrep',
  components: {
    PracticeBankPicker },
  data() {
    return {
      activeTab: (this.$route.query && this.$route.query.tab === 'module') ? 'module' : 'material',
      // === 2026-09-21 改版新增：列表筛选（纯前台过滤，不改接口）===
      materialFilter: { keyword: '', type: '', status: '' },
      // ---- 备考资料（真实后端） ----
      materials: [],
      materialLoading: false,
      positionOptions: [],
      editingId: null,
      previewVisible: false,
      previewFile: null,
      /**
       * 备考资料表单（name/type/position/version/intro/fileName/fileUrl）
       */
      form: {
        name: '', type: 'DOCUMENT', position: '', version: 'v1.0', intro: '', fileName: '', fileUrl: ''
      },
      examFilter: { keyword: '', status: '', difficulty: '' },
      // ---- 模拟理论考核（套卷，无阶段层级） ----
      exams: [],
      examLoading: false,
      paperDialogVisible: false,
      paperDialogTitle: '',
      paperForm: emptyPaperForm(),
      paperRules: {
        examName: [{ required: true, message: '请输入套卷名称', trigger: 'blur' }]
      },
      saving: false
    }
  },
  computed: {
    ...mapGetters(['roles']),
    isSuperAdmin() {
      return this.roles.indexOf('SUPER_ADMIN') > -1
    },
    tabs() {
      return [
        { key: 'material', label: '备考资料', count: this.materials.length },
        { key: 'module', label: '模拟理论考核', count: this.exams.length || '' },
        { key: 'pbank', label: '模拟实操题库' }
      ]
    },
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
    },
  },
  created() {
    if (this.isSuperAdmin) this.loadDepartments()
    this.reloadAll()
  },
  methods: {
    // === 2026-09-21 改版新增：筛选 + 总览（全部由已加载列表计算）===
    filteredMaterials() {
      const f = this.materialFilter
      const kw = (f.keyword || '').trim().toLowerCase()
      return (this.materials || []).filter(r => {
        if (kw && String(r.materialName || '').toLowerCase().indexOf(kw) === -1) return false
        if (f.type && r.materialType !== f.type) return false
        if (f.status && r.status !== f.status) return false
        return true
      })
    },
    resetMaterialFilter() {
      this.materialFilter = { keyword: '', type: '', status: '' }
    },
    materialKpis() {
      const list = this.materials || []
      const published = list.filter(m => m.status === 'PUBLISHED').length
      const positions = new Set(list.map(m => m.positionName || '未设置岗位'))
      return [
        { label: '资料总数', value: list.length, unit: '份', icon: 'el-icon-folder-opened', tone: '' },
        { label: '已发布', value: published, unit: '份', icon: 'el-icon-circle-check', tone: 'tone-green' },
        { label: '待发布 / 已停用', value: list.length - published, unit: '份', icon: 'el-icon-edit-outline', tone: 'tone-orange' },
        { label: '覆盖岗位', value: positions.size, unit: '个', icon: 'el-icon-office-building', tone: 'tone-purple' }
      ]
    },

    switchTab(key) {
      this.activeTab = key
      if (key === 'module' && !this.exams.length) this.loadExamTab()
    },

    // ================= 备考资料 =================
    reloadAll() {
      this.loadMaterials()
      this.loadPositions()
      if (this.activeTab === 'module') this.loadExamTab()
    },
    loadMaterials() {
      this.materialLoading = true
      listMaterial({ pageNum: 1, pageSize: 200 }).then(res => {
        this.materials = (res && res.rows) || []
        this.materialLoading = false
      }).catch(() => {
        this.materials = []
        this.materialLoading = false
      })
    },
    loadPositions() {
      listApplicablePositions().then(res => {
        this.positionOptions = (res && res.data) || []
      }).catch(() => { this.positionOptions = [] })
    },
    pickFile() {
      if (this.$refs.fileInput) {
        this.$refs.fileInput.value = ''
        this.$refs.fileInput.click()
      }
    },
    onFileChange(e) {
      const file = e.target.files && e.target.files[0]
      if (!file) return
      const formData = new FormData()
      formData.append('file', file)
      this.saving = true
      uploadFile(formData).then(res => {
        this.form.fileUrl = res.fileName || ''
        this.form.fileName = file.name
        this.saving = false
        this.$modal.msgSuccess('附件上传成功')
      }).catch(() => { this.saving = false })
    },
    /** 保存（publish=false 存草稿 / true 发布）；编辑态「保存修改」不改动现有状态 */
    saveMaterial(publish) {
      if (!this.form.name || !this.form.name.trim()) {
        this.$modal.msgWarning('请填写资料名称')
        return
      }
      if (!this.form.position) {
        this.$modal.msgWarning('请选择适用岗位')
        return
      }
      if (!this.form.fileUrl) {
        this.$modal.msgWarning('请先上传附件')
        return
      }
      const payload = {
        materialName: this.form.name.trim(),
        materialType: this.form.type || 'DOCUMENT',
        positionId: this.form.position,
        summary: this.form.intro || '',
        fileUrl: this.form.fileUrl,
        versionNo: this.form.version || ''
      }
      if (publish) {
        payload.status = 'PUBLISHED'
      } else if (!this.editingId) {
        payload.status = 'DRAFT'
      }
      this.saving = true
      const done = () => {
        this.saving = false
        this.$modal.msgSuccess(publish ? '已发布' : '已保存')
        this.resetForm()
        this.loadMaterials()
      }
      const fail = () => { this.saving = false }
      if (this.editingId) {
        payload.id = this.editingId
        updateMaterial(payload).then(done).catch(fail)
      } else {
        addMaterial(payload).then(done).catch(fail)
      }
    },
    editMaterial(row) {
      this.editingId = row.id
      this.form.name = row.materialName || ''
      this.form.type = row.materialType || 'DOCUMENT'
      this.form.position = row.positionId
      this.form.version = row.versionNo || ''
      this.form.intro = row.summary || ''
      this.form.fileUrl = row.fileUrl || ''
      this.form.fileName = row.fileUrl ? String(row.fileUrl).split('/').pop() : ''
    },
    resetForm() {
      this.editingId = null
      this.form = { name: '', type: 'DOCUMENT', position: '', version: 'v1.0', intro: '', fileName: '', fileUrl: '' }
    },
    previewMaterial(row) {
      const url = resolveFileUrl(row)
      if (!url) {
        this.$modal.msgWarning('该资料暂无附件')
        return
      }
      this.previewFile = { name: row.materialName || '附件', url: url, fileUrl: url }
      this.previewVisible = true
    },
    /** 弹窗内下载：复用与预览一致的地址解析，避免字段名不一致导致误报「暂无附件」 */
    downloadPreviewFile() {
      const url = resolveFileUrl(this.previewFile)
      if (!url) {
        this.$modal.msgWarning('该资料暂无附件')
        return
      }
      triggerDownload(this.baseApi, url, (this.previewFile && this.previewFile.name) || '')
    },
    changeStatus(row, status) {
      const act = status === 'PUBLISHED' ? '发布' : '停用'
      changeMaterialStatus(row.id, status).then(() => {
        this.$modal.msgSuccess('已' + act + '「' + row.materialName + '」')
        this.loadMaterials()
      }).catch(() => {})
    },
    removeMaterial(row) {
      this.$modal.confirm('确认删除备考资料「' + row.materialName + '」吗？').then(() => {
        return delMaterial(row.id)
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.loadMaterials()
      }).catch(() => {})
    },
    typeTone(t) {
      return { DOCUMENT: 'blue', VIDEO: 'purple', MOCK_ENTRY: 'orange' }[t] || 'gray'
    },
    statusText(s) {
      return { PUBLISHED: '已发布', DRAFT: '草稿', DISABLED: '已停用' }[s] || s
    },
    typeText(t) {
      return { SINGLE: '单选', MULTI: '多选', JUDGE: '判断', DOCUMENT: '文档', VIDEO: '视频', MOCK_ENTRY: '模拟题入口' }[t] || t
    },


    // ================= 模拟理论考核（套卷，无阶段层级） =================
    /** 套卷列表刷新（进页签 / 点刷新都用它） */
    loadExamTab() {
      this.loadModuleExams()
    },
    loadModuleExams() {
      this.examLoading = true
      listExam({ pageNum: 1, pageSize: 100, examMode: 'PRACTICE', examType: 'THEORY' }).then(res => {
        this.exams = (res && res.rows) || []
        this.examLoading = false
      }).catch(() => {
        this.exams = []
        this.examLoading = false
      })
    },
    /** 套卷筛选（纯前台：名称/描述/内容偏向 关键词 + 状态 + 难度） */
    filteredExams() {
      const f = this.examFilter
      const kw = (f.keyword || '').trim().toLowerCase()
      return (this.exams || []).filter(e => {
        if (kw) {
          const hay = (String(e.examName || '') + ' ' + String(e.description || '') + ' ' + String(e.contentBias || '')).toLowerCase()
          if (hay.indexOf(kw) === -1) return false
        }
        if (f.status && e.status !== f.status) return false
        if (f.difficulty && (e.difficulty || '') !== f.difficulty) return false
        return true
      })
    },
    resetExamFilter() {
      this.examFilter = { keyword: '', status: '', difficulty: '' }
    },
    /** 套卷总览（全部由已加载列表计算） */
    examKpis() {
      const list = this.exams || []
      const published = list.filter(e => e.status === 'PUBLISHED').length
      const draft = list.filter(e => e.status === 'DRAFT').length
      const disabled = list.filter(e => e.status === 'DISABLED').length
      return [
        { label: '套卷总数', value: list.length, unit: '套', icon: 'el-icon-document', tone: '' },
        { label: '已发布', value: published, unit: '套', icon: 'el-icon-circle-check', tone: 'tone-green' },
        { label: '草稿待配置', value: draft, unit: '套', icon: 'el-icon-edit-outline', tone: 'tone-orange' },
        { label: '已停用', value: disabled, unit: '套', icon: 'el-icon-circle-close', tone: '' }
      ]
    },
    /** 进套卷配置独立页（组卷规则 / 分值 / 试抽 / 发布都在那一页） */
    goPaperConfig(row) {
      if (!row || !row.id) return
      // 套卷配置页有两套前缀（部门端 /department/study/paper-config、超管端 /super/ops/paper-config），
      // 按角色分流 —— 否则超管会被跳到部门端路由而 404
      const base = (this.$store.getters.roles || []).indexOf('SUPER_ADMIN') > -1
        ? '/super/ops/paper-config/'
        : '/department/study/paper-config/'
      this.$router.push(base + row.id)
    },
    /** 打开套卷基础信息弹窗（新建 / 编辑信息） */
    openPaperDialog(row) {
      this.paperForm = row
        ? {
          id: row.id,
          examName: row.examName || '',
          description: row.description || '',
          difficulty: row.difficulty || '',
          contentBias: row.contentBias || ''
        }
        : emptyPaperForm()
      this.paperDialogTitle = row ? '编辑套卷基础信息' : '新建套卷'
      this.paperDialogVisible = true
      this.$nextTick(() => this.$refs.paperForm && this.$refs.paperForm.clearValidate())
    },
    /** 创建 / 保存套卷基础信息（不动组卷配置，也不改发布状态） */
    submitPaper() {
      this.$refs.paperForm.validate(valid => {
        if (!valid) return
        this.saving = true
        const base = {
          examName: (this.paperForm.examName || '').trim(),
          description: this.paperForm.description || null,
          difficulty: this.paperForm.difficulty || null,
          contentBias: this.paperForm.contentBias || null
        }
        if (this.paperForm.id) {
          updateExam(Object.assign({ id: this.paperForm.id }, base)).then(() => {
            this.saving = false
            this.paperDialogVisible = false
            this.loadModuleExams()
            this.$modal.msgSuccess('套卷基础信息已更新')
          }).catch(() => { this.saving = false })
        } else {
          addExam(Object.assign({ examMode: 'PRACTICE', examType: 'THEORY' }, base)).then(() => {
            this.saving = false
            this.paperDialogVisible = false
            this.loadModuleExams()
            this.$modal.msgSuccess('套卷已创建，请在列表中点「配置」完成组卷与发布')
          }).catch(() => { this.saving = false })
        }
      })
    },
    publishExamRow(row) {
      publishExam(row.id).then(() => {
        this.$modal.msgSuccess('已发布「' + row.examName + '」')
        this.loadModuleExams()
      }).catch(() => {})
    },
    disableExamRow(row) {
      this.$modal.confirm('确认停用「' + row.examName + '」吗？停用后实习生端不再显示。').then(() => {
        return changeExamStatus(row.id, 'DISABLED')
      }).then(() => {
        this.$modal.msgSuccess('已停用')
        this.loadModuleExams()
      }).catch(() => {})
    },
    removeExam(row) {
      this.$modal.confirm('确认删除「' + row.examName + '」吗？').then(() => {
        return delExam(row.id)
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.loadModuleExams()
      }).catch(() => {})
    },
    examStatusText(s) {
      return { DRAFT: '草稿', PUBLISHED: '已发布', DISABLED: '已停用', GRADING: '批改中' }[s] || s
    },
    difficultyText(d) {
      return { EASY: '简单', MEDIUM: '中等', HARD: '困难' }[d] || '未设置'
    },
    difficultyTone(d) {
      return { EASY: 'green', MEDIUM: 'orange', HARD: 'red' }[d] || 'gray'
    }
  }
}
</script>

<style lang="scss" scoped>
@import '~@/assets/styles/department-module.scss';

.dtabs.page { padding: 0 8px; margin: 0 0 18px; background: #fff; border: 1px solid #e4e9f0; border-radius: 10px; }
.dtab .tab-n { margin-left: 4px; color: #98a2b3; font-size: 11px; }
.dtab.on .tab-n { color: #1764f5; }
.dtbl .acts .sep { color: #d0d5dd; }
.d-empty { padding: 28px 0; text-align: center; color: #98a2b3; font-size: 13px; }
.d-hidden-input { display: none; }
.preview-box { min-height: 132px; }
.pq { color: #344054; font-size: 11.5px; line-height: 2; }
code { padding: 1px 5px; color: #344054; font-size: 11.5px; background: #f2f4f7; border-radius: 4px; }
.embed-wrap { margin-top: 4px; }
.file-preview { display: flex; align-items: center; justify-content: center; min-height: 160px; }
.file-preview-frame { width: 100%; height: 70vh; border: none; }
.file-preview-video { width: 100%; max-height: 70vh; background: #000; }
.file-preview-tip { text-align: center; color: #98a2b3; padding: 24px 0; }
.file-preview-tip i { font-size: 48px; color: #c3cdd9; display: block; margin-bottom: 12px; }
.file-preview-tip p { margin: 0 0 8px; font-size: 13px; }
.file-preview-hint { display: block; margin-bottom: 16px; color: #b0b8c4; font-size: 12px; }

/* 复用组件自带页头与内边距，嵌入时收掉一层 */
.embed-wrap ::v-deep .psubject-page { padding: 0; background: transparent; }
.embed-wrap ::v-deep .psubject-page.app-container { padding: 0; }

/* ===== 2026-09-21 改版新增：总览条 + 筛选条（本页私有，不复用其它页类名） ===== */
.prep-kpi-row { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin-bottom: 14px; }
.prep-kpi-card { display: flex; align-items: center; gap: 12px; padding: 13px 16px; background: #fff; border: 1px solid #e7ecf3; border-radius: 8px; }
.prep-kpi-icon { display: flex; width: 38px; height: 38px; flex: none; align-items: center; justify-content: center; color: #1764f5; background: #edf4ff; font-size: 19px; border-radius: 8px; }
.prep-kpi-body { display: flex; min-width: 0; flex-direction: column; }
.prep-kpi-value { color: #1d2939; font-size: 21px; font-weight: 600; line-height: 1.2; }
.prep-kpi-value small { margin-left: 3px; color: #667085; font-size: 12px; font-weight: 400; }
.prep-kpi-label { margin-top: 2px; color: #667085; font-size: 12px; }
.prep-kpi-card.tone-green .prep-kpi-icon { color: #23966f; background: #eaf7f1; }
.prep-kpi-card.tone-orange .prep-kpi-icon { color: #e6a23c; background: #fdf6ec; }
.prep-kpi-card.tone-purple .prep-kpi-icon { color: #7b5cf0; background: #f2eeff; }
.prep-filter { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; padding: 0 0 10px; }
.prep-filter-sum { margin-left: auto; color: #667085; font-size: 12px; }
@media (max-width: 1100px) {
  .prep-kpi-row { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .prep-filter-sum { margin-left: 0; }
}
</style>
