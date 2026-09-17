<template>
  <div class="psubject-page app-container">
    <!-- ===== 页头 ===== -->
    <header class="ps-head">
      <div class="ps-head-l">
        <span class="eyebrow">学习考核 / 模拟备考管理</span>
        <div class="title-line">
          <h2>模拟实操题库</h2>
          <el-tag size="mini" effect="plain" :type="isSuperAdmin ? 'warning' : 'success'">
            {{ isSuperAdmin ? '全局管理' : '本部门范围' }}
          </el-tag>
        </div>
        <p>
          {{ isSuperAdmin
            ? '查看并维护各部门模拟实操题，创建时指定所属部门。'
            : '按「方向」组织实操题：题名与题干必填，交付要求 / 开发约束 / 建议用时 / 提交格式 / 命名规则 / 参考图 / 附件均为可选。实习生端「模拟考核 › 实操题库」按方向分组展示卡片，点开进入详情页（左参考图 + 右交付要求）。' }}
        </p>
      </div>
      <div class="ps-head-r">
        <el-button icon="el-icon-refresh" size="small" @click="loadList">刷新</el-button>
        <el-button v-hasPermi="['business:psubject:add']" type="primary" icon="el-icon-plus" size="small" @click="handleAdd">发布实操题</el-button>
      </div>
    </header>

    <!-- ===== 概览条（对标实习生端 hero 统计） ===== -->
    <section class="ps-hero">
      <div class="ps-hero-l">
        <span class="badge-blue">{{ isSuperAdmin ? '全局实操题管理' : (deptName || '当前部门') }}</span>
        <h3>{{ isSuperAdmin ? '各部门模拟实操题' : '本部门模拟实操题' }}</h3>
        <p>{{ isSuperAdmin ? '可按部门筛选并维护；启用的题目实习生可见。' : '启用的题目会出现在实习生端「模拟考核 › 实操题库」，按方向分组展示。' }}</p>
      </div>
      <div class="ps-hero-stats">
        <div><b>{{ stats.total }}</b><span>题目总数</span></div>
        <div><b>{{ stats.directions }}</b><span>方向分组</span></div>
        <div><b>{{ stats.enabled }}</b><span>已启用</span></div>
        <div><b>{{ stats.maxMinutes }}<small> 分钟</small></b><span>最长用时</span></div>
      </div>
    </section>

    <!-- ===== 方向筛选 ===== -->
    <div class="ps-dirbar">
      <span class="ps-dirbar-label">方向</span>
      <span class="chip" :class="{ on: !queryParams.direction }" @click="pickDirection('')">
        全部 <em>{{ stats.total }}</em>
      </span>
      <span v-for="d in directionChips" :key="d.name" class="chip" :class="{ on: queryParams.direction === d.name }" @click="pickDirection(d.name)">
        {{ d.name }} <em>{{ d.count }}</em>
      </span>
      <span class="ps-dirbar-tip"><i class="el-icon-info" /> 方向可自由填写；同名即归入同一分组</span>
    </div>

    <!-- ===== 列表 ===== -->
    <section class="ps-panel">
      <div class="ps-panel-h">
        <div><h3>题目清单</h3><p>按方向分组排序；题名与题干必填，其余可选。</p></div>
        <div class="ps-panel-h-r">
          <el-select v-model="queryParams.difficulty" size="small" clearable placeholder="全部难度" style="width:120px" @change="handleQuery">
            <el-option label="简单" value="EASY" />
            <el-option label="中等" value="MEDIUM" />
            <el-option label="困难" value="HARD" />
          </el-select>
          <el-select v-model="queryParams.status" size="small" clearable placeholder="全部状态" style="width:120px" @change="handleQuery">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
          <el-input v-model="queryParams.content" size="small" clearable placeholder="搜题名 / 题干" style="width:200px" @keyup.enter.native="handleQuery" />
          <el-button type="primary" size="small" icon="el-icon-search" @click="handleQuery">查询</el-button>
          <el-button size="small" icon="el-icon-refresh-left" @click="resetQuery">重置</el-button>
        </div>
      </div>

      <el-form v-if="isSuperAdmin" :inline="true" size="small" class="ps-deptform" @submit.native.prevent>
        <el-form-item label="所属部门">
          <el-select v-model="queryParams.deptId" clearable filterable placeholder="全部部门" style="width:180px" @change="handleQuery">
            <el-option v-for="dept in deptOptions" :key="dept.deptId" :label="dept.deptName" :value="dept.deptId" />
          </el-select>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="list" stripe empty-text="暂无实操题，点击「发布实操题」开始配置">
        <el-table-column label="题名" min-width="240" show-overflow-tooltip>
          <template slot-scope="scope">
            <div class="title-cell">
              <span class="seq">{{ (queryParams.pageNum - 1) * queryParams.pageSize + scope.$index + 1 }}</span>
              <div class="title-text">
                <b>{{ scope.row.title || '（未命名）' }}</b>
                <span class="sub">{{ brief(scope.row.content) }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="方向" width="150">
          <template slot-scope="scope">
            <el-tag size="mini" effect="plain" type="primary">{{ scope.row.direction || '通用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="难度" width="86" align="center">
          <template slot-scope="scope">
            <span class="diff" :class="'d-' + (scope.row.difficulty || 'MEDIUM').toLowerCase()">{{ difficultyText(scope.row.difficulty) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="建议用时" width="100" align="center">
          <template slot-scope="scope">
            <span v-if="scope.row.estimatedMinutes">{{ scope.row.estimatedMinutes }} 分钟</span>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="交付 / 参考图" width="130" align="center">
          <template slot-scope="scope">
            <span class="mini-count"><i class="el-icon-s-order" /> {{ lineCount(scope.row.deliverables) }}</span>
            <span class="mini-count"><i class="el-icon-picture-outline" /> {{ imgCount(scope.row.referenceImages) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="附件" width="90" align="center">
          <template slot-scope="scope">
            <span v-if="!parseAttachments(scope.row.attachmentsJson).length" class="muted">无</span>
            <el-tooltip v-else effect="dark" placement="top">
              <div slot="content">
                <div v-for="(a, i) in parseAttachments(scope.row.attachmentsJson)" :key="i">{{ a.name }}</div>
              </div>
              <span class="mini-count blue"><i class="el-icon-paperclip" /> {{ parseAttachments(scope.row.attachmentsJson).length }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column v-if="isSuperAdmin" label="所属部门" min-width="110">
          <template slot-scope="scope"><span class="dept-text"><i class="el-icon-office-building" />{{ scope.row.deptName || '未设置' }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template slot-scope="scope">
            <el-tag size="mini" effect="plain" :type="scope.row.status === 1 ? 'success' : 'info'">{{ scope.row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center">
          <template slot-scope="scope">
            <el-button v-hasPermi="['business:psubject:edit']" type="text" size="mini" icon="el-icon-edit" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button v-hasPermi="['business:psubject:edit']" type="text" size="mini" :icon="scope.row.status === 1 ? 'el-icon-turn-off' : 'el-icon-open'" @click="handleToggleStatus(scope.row)">{{ scope.row.status === 1 ? '停用' : '启用' }}</el-button>
            <el-button v-hasPermi="['business:psubject:remove']" type="text" size="mini" icon="el-icon-delete" class="danger-text" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="loadList" />
    </section>

    <!-- ===== 发布 / 编辑弹窗 ===== -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="920px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="96px" class="ps-form">
        <div class="ps-form-sec">基本信息</div>
        <div class="fg2">
          <el-form-item v-if="isSuperAdmin && !form.id" label="所属部门" prop="deptId">
            <el-select v-model="form.deptId" filterable placeholder="请选择所属部门" style="width:100%">
              <el-option v-for="dept in deptOptions" :key="dept.deptId" :label="dept.deptName" :value="dept.deptId" />
            </el-select>
          </el-form-item>
          <el-form-item label="题名" prop="title">
            <el-input v-model="form.title" placeholder="例如：用户登录接口开发" maxlength="200" show-word-limit />
          </el-form-item>
          <el-form-item label="方向" prop="direction">
            <el-select v-model="form.direction" filterable allow-create default-first-option placeholder="选择或输入方向" style="width:100%">
              <el-option v-for="d in directionOptions" :key="d" :label="d" :value="d" />
            </el-select>
          </el-form-item>
          <el-form-item label="方向说明">
            <el-input v-model="form.directionDesc" placeholder="例如：适合接口设计、参数校验与异常处理训练" maxlength="255" />
          </el-form-item>
          <el-form-item label="难度">
            <el-radio-group v-model="form.difficulty">
              <el-radio-button label="EASY">简单</el-radio-button>
              <el-radio-button label="MEDIUM">中等</el-radio-button>
              <el-radio-button label="HARD">困难</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="建议用时">
            <el-input-number v-model="form.estimatedMinutes" :min="0" :max="1440" :step="15" controls-position="right" style="width:100%" />
          </el-form-item>
        </div>

        <div class="ps-form-sec">题干与要求</div>
        <el-form-item label="题干" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="4" placeholder="请输入实操题题干 / 操作要求" maxlength="5000" show-word-limit />
        </el-form-item>
        <div class="fg2">
          <el-form-item label="交付要求">
            <el-input v-model="form.deliverables" type="textarea" :rows="4" placeholder="每行一条，例如：&#10;完成登录接口与参数校验&#10;梳理异常处理与统一返回结构" />
          </el-form-item>
          <el-form-item label="开发约束">
            <el-input v-model="form.devConstraints" type="textarea" :rows="4" placeholder="每行一条，例如：&#10;接口符合 RESTful 命名规范&#10;参数校验完整、返回结构统一" />
          </el-form-item>
        </div>
        <div class="fg2">
          <el-form-item label="提交格式">
            <el-input v-model="form.submitFormat" placeholder="例如：.java 源码 + SQL 脚本 + .zip" maxlength="255" />
          </el-form-item>
          <el-form-item label="命名规则">
            <el-input v-model="form.namingRule" placeholder="例如：MOCK_API-1_用户登录接口开发_v01.zip" maxlength="255" />
          </el-form-item>
        </div>

        <div class="ps-form-sec">参考图与附件</div>
        <el-form-item label="参考图">
          <div class="up-box">
            <div v-if="form.referenceImages.length" class="img-grid">
              <div v-for="(img, i) in form.referenceImages" :key="i" class="img-item">
                <el-image :src="baseApi + img.url" fit="cover" :preview-src-list="previewList" class="img-thumb" />
                <i class="el-icon-close img-del" @click="removeImage(i)" />
              </div>
            </div>
            <div v-else class="up-empty">未添加参考图（非必须）</div>
            <el-upload action="#" :show-file-list="false" :multiple="true" :http-request="doUploadImage">
              <el-button size="mini" icon="el-icon-picture-outline" :loading="uploadingImg">添加参考图</el-button>
            </el-upload>
            <div class="up-tip">支持 jpg / png；详情页左侧大图与下方缩略图都取自这里。</div>
          </div>
        </el-form-item>
        <el-form-item label="附件">
          <div class="up-box">
            <div v-if="!form.attachments.length" class="up-empty">未添加附件（非必须）</div>
            <div v-for="(a, i) in form.attachments" :key="i" class="file-item">
              <i class="el-icon-paperclip" />
              <a :href="baseApi + a.url" target="_blank" class="file-name">{{ a.name }}</a>
              <el-button type="text" size="mini" icon="el-icon-close" class="file-del" @click="removeAttachment(i)" />
            </div>
            <el-upload action="#" :show-file-list="false" :multiple="true" :http-request="doUpload">
              <el-button size="mini" icon="el-icon-upload2" :loading="uploading">添加附件</el-button>
            </el-upload>
            <div class="up-tip">可上传多个附件，单个文件不超过 1024MB。</div>
          </div>
        </el-form-item>

        <div class="ps-form-sec">发布设置</div>
        <div class="fg2">
          <el-form-item label="排序号">
            <el-input-number v-model="form.sortNo" :min="0" :max="9999" controls-position="right" style="width:100%" />
          </el-form-item>
          <el-form-item label="状态">
            <el-radio-group v-model="form.status">
              <el-radio :label="1">启用（实习生可见）</el-radio>
              <el-radio :label="0">停用</el-radio>
            </el-radio-group>
          </el-form-item>
        </div>
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
import { listDept } from '@/api/system/dept'
import { parseTime } from '@/utils/ruoyi'
import { mapGetters } from 'vuex'

const DIRECTION_PRESETS = ['后端接口开发', '数据访问开发', '前端页面开发', '部署与运维', '通用']

function emptyForm() {
  return {
    id: null,
    deptId: null,
    title: '',
    direction: '',
    directionDesc: '',
    difficulty: 'MEDIUM',
    estimatedMinutes: null,
    deliverables: '',
    devConstraints: '',
    submitFormat: '',
    namingRule: '',
    referenceImages: [],
    attachments: [],
    sortNo: 0,
    content: '',
    status: 1
  }
}

export default {
  name: 'PracticeSubject',
  data() {
    return {
      loading: false,
      list: [],
      total: 0,
      queryParams: { pageNum: 1, pageSize: 10, content: '', status: undefined, deptId: null, direction: '', difficulty: undefined },
      deptOptions: [],
      dialogVisible: false,
      dialogTitle: '',
      form: emptyForm(),
      rules: {
        deptId: [{ required: true, message: '请选择所属部门', trigger: 'change' }],
        title: [{ required: true, message: '请输入题名', trigger: 'blur' }],
        direction: [{ required: true, message: '请选择或输入方向', trigger: 'change' }],
        content: [{ required: true, message: '请输入题干', trigger: 'blur' }]
      },
      uploading: false,
      uploadingImg: false,
      submitting: false
    }
  },
  computed: {
    ...mapGetters(['deptName', 'roles']),
    isSuperAdmin() { return this.roles.indexOf('SUPER_ADMIN') > -1 },
    baseApi() { return process.env.VUE_APP_BASE_API || '' },
    previewList() { return this.form.referenceImages.map(i => this.baseApi + i.url) },
    /** 方向下拉：已有方向 + 预置方向，去重 */
    directionOptions() {
      const set = new Set(DIRECTION_PRESETS)
      this.list.forEach(r => { if (r.direction) set.add(r.direction) })
      if (this.form.direction) set.add(this.form.direction)
      return Array.from(set)
    },
    /** 方向筛选条：当前页数据的方向分布 */
    directionChips() {
      const map = new Map()
      this.list.forEach(r => {
        const k = r.direction || '通用'
        map.set(k, (map.get(k) || 0) + 1)
      })
      return Array.from(map, ([name, count]) => ({ name, count }))
    },
    stats() {
      const dirs = new Set(this.list.map(r => r.direction || '通用'))
      const mins = this.list.map(r => Number(r.estimatedMinutes) || 0)
      return {
        total: this.total,
        directions: dirs.size,
        enabled: this.list.filter(r => r.status === 1).length,
        maxMinutes: mins.length ? Math.max.apply(null, mins) : 0
      }
    }
  },
  created() {
    if (this.isSuperAdmin) this.loadDepartments()
    this.loadList()
  },
  methods: {
    loadDepartments() {
      listDept({ status: '0' }).then(res => {
        this.deptOptions = (res.data || []).filter(dept => dept.parentId !== 0)
      })
    },
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
    pickDirection(name) {
      this.queryParams.direction = name
      this.handleQuery()
    },
    resetQuery() {
      this.queryParams = { pageNum: 1, pageSize: 10, content: '', status: undefined, deptId: null, direction: '', difficulty: undefined }
      this.loadList()
    },
    handleAdd() {
      this.form = emptyForm()
      this.dialogTitle = '发布实操题'
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
    },
    handleEdit(row) {
      this.form = {
        id: row.id,
        deptId: row.deptId,
        title: row.title || '',
        direction: row.direction || '',
        directionDesc: row.directionDesc || '',
        difficulty: row.difficulty || 'MEDIUM',
        estimatedMinutes: row.estimatedMinutes == null ? null : Number(row.estimatedMinutes),
        deliverables: row.deliverables || '',
        devConstraints: row.devConstraints || '',
        submitFormat: row.submitFormat || '',
        namingRule: row.namingRule || '',
        referenceImages: this.parseAttachments(row.referenceImages).slice(),
        attachments: this.parseAttachments(row.attachmentsJson).slice(),
        sortNo: row.sortNo == null ? 0 : Number(row.sortNo),
        content: row.content || '',
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
    doUploadImage(option) {
      const file = option.file
      const ok = /\.(jpg|jpeg|png|gif|webp|bmp)$/i.test(file.name)
      if (!ok) {
        this.$modal.msgWarning('参考图仅支持 jpg / png / gif / webp / bmp')
        return
      }
      if (file.size > 20 * 1024 * 1024) {
        this.$modal.msgWarning('单张参考图不能超过 20MB')
        return
      }
      const formData = new FormData()
      formData.append('file', file)
      this.uploadingImg = true
      uploadFile(formData).then(res => {
        this.form.referenceImages.push({ name: file.name, url: res.fileName })
        this.uploadingImg = false
        this.$modal.msgSuccess('参考图上传成功')
      }).catch(() => { this.uploadingImg = false })
    },
    removeAttachment(index) {
      this.form.attachments.splice(index, 1)
    },
    removeImage(index) {
      this.form.referenceImages.splice(index, 1)
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.submitting = true
        const payload = {
          id: this.form.id,
          deptId: this.form.deptId,
          title: this.form.title,
          direction: this.form.direction,
          directionDesc: this.form.directionDesc,
          difficulty: this.form.difficulty,
          estimatedMinutes: this.form.estimatedMinutes,
          deliverables: this.form.deliverables,
          devConstraints: this.form.devConstraints,
          submitFormat: this.form.submitFormat,
          namingRule: this.form.namingRule,
          sortNo: this.form.sortNo,
          content: this.form.content,
          status: this.form.status,
          // 注意：清空时必须回传空串而不是 null，否则后端 <if test="xxx != null"> 不会更新，旧值会残留
          attachmentsJson: this.form.attachments.length ? JSON.stringify(this.form.attachments) : '',
          referenceImages: this.form.referenceImages.length ? JSON.stringify(this.form.referenceImages) : ''
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
    imgCount(json) {
      return this.parseAttachments(json).length
    },
    lineCount(text) {
      if (!text) return 0
      return String(text).split('\n').filter(s => s.trim()).length
    },
    brief(text) {
      if (!text) return ''
      const t = String(text).replace(/\s+/g, ' ').trim()
      return t.length > 54 ? t.slice(0, 54) + '…' : t
    },
    difficultyText(v) {
      return { EASY: '简单', MEDIUM: '中等', HARD: '困难' }[v] || '中等'
    },
    fmtTime(val) {
      return val ? parseTime(val, '{y}-{m}-{d} {h}:{i}') : '-'
    }
  }
}
</script>

<style lang="scss" scoped>
/* ===== 与统一设计稿一致的设计令牌 ===== */
$blue: #1764f5;
$blue-soft: #edf4ff;
$ink: #1d2939;
$ink-2: #344054;
$ink-3: #667085;
$ink-4: #98a2b3;
$line: #e4e9f0;
$line-2: #eef1f6;
$panel-2: #f7f9fc;

.psubject-page { color: $ink; }

/* --- 页头 --- */
.ps-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 20px; margin-bottom: 16px; }
.eyebrow { color: $blue; font-size: 11px; letter-spacing: .08em; }
.title-line { display: flex; align-items: center; gap: 10px; margin: 6px 0 8px; }
.ps-head h2 { margin: 0; font-size: 22px; font-weight: 600; color: $ink; }
.ps-head p { margin: 0; max-width: 780px; color: $ink-3; font-size: 13px; line-height: 1.6; }
.ps-head-r { flex: none; white-space: nowrap; }

/* --- 概览条 --- */
.ps-hero {
  display: flex; align-items: center; gap: 24px; flex-wrap: wrap;
  background: #fff; border: 1px solid $line; border-radius: 12px;
  padding: 18px 22px; margin-bottom: 14px;
}
.ps-hero-l { flex: 1; min-width: 260px; }
.badge-blue {
  display: inline-flex; align-items: center; height: 22px; padding: 0 9px;
  border-radius: 6px; background: $blue-soft; color: $blue; font-size: 11.5px; font-weight: 600;
}
.ps-hero-l h3 { margin: 10px 0 6px; font-size: 19px; font-weight: 600; }
.ps-hero-l p { margin: 0; font-size: 12.5px; color: $ink-3; }
.ps-hero-stats { display: flex; flex: none; }
.ps-hero-stats > div { padding: 0 24px; text-align: center; border-left: 1px solid $line; }
.ps-hero-stats > div:first-child { border-left: 0; }
.ps-hero-stats b { display: block; font-size: 25px; color: $blue; line-height: 1.1; }
.ps-hero-stats b small { font-size: 12px; color: $ink-4; font-weight: 500; }
.ps-hero-stats span { font-size: 11.5px; color: $ink-4; }

/* --- 方向筛选条 --- */
.ps-dirbar {
  display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
  padding: 11px 16px; background: #fff; border: 1px solid $line; border-radius: 10px; margin-bottom: 14px;
}
.ps-dirbar-label { font-size: 12px; color: $ink-4; margin-right: 2px; }
.chip {
  display: inline-flex; align-items: center; gap: 6px; height: 28px; padding: 0 12px;
  border-radius: 7px; background: $panel-2; border: 1px solid $line-2;
  font-size: 12.5px; color: $ink-2; cursor: pointer; user-select: none; transition: all .15s;
}
.chip:hover { border-color: $blue; color: $blue; }
.chip.on { background: $blue; border-color: $blue; color: #fff; font-weight: 600; }
.chip em { font-style: normal; font-size: 11px; color: $ink-4; }
.chip.on em { color: rgba(255, 255, 255, .85); }
.ps-dirbar-tip { margin-left: auto; font-size: 11.5px; color: $ink-4; }
.ps-dirbar-tip i { margin-right: 3px; }

/* --- 面板 --- */
.ps-panel { padding: 18px 20px; background: #fff; border: 1px solid $line; border-radius: 10px; }
.ps-panel-h { display: flex; align-items: flex-start; justify-content: space-between; gap: 14px; flex-wrap: wrap; margin-bottom: 12px; }
.ps-panel-h h3 { margin: 0 0 5px; font-size: 15.5px; font-weight: 600; }
.ps-panel-h p { margin: 0; color: $ink-4; font-size: 12px; }
.ps-panel-h-r { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.ps-deptform { padding-bottom: 4px; border-bottom: 1px solid $line-2; margin-bottom: 4px; }

/* --- 表格单元 --- */
.title-cell { display: flex; align-items: flex-start; gap: 9px; }
.seq {
  flex: none; display: inline-flex; width: 20px; height: 20px; margin-top: 1px;
  align-items: center; justify-content: center; color: $ink-3; font-size: 11px;
  background: #f0f2f5; border-radius: 4px;
}
.title-text { min-width: 0; }
.title-text b { display: block; color: $ink; font-size: 13px; line-height: 1.4; }
.title-text .sub { display: block; color: $ink-4; font-size: 11.5px; margin-top: 2px; }
.diff { font-size: 12px; font-weight: 600; }
.diff.d-easy { color: #067647; }
.diff.d-medium { color: #b54708; }
.diff.d-hard { color: #b42318; }
.mini-count { font-size: 12px; color: $ink-3; margin-right: 10px; }
.mini-count:last-child { margin-right: 0; }
.mini-count.blue { color: $blue; }
.mini-count i { margin-right: 3px; }
.muted { color: #b7c1cc; font-size: 12px; }
.dept-text { color: #475467; font-size: 12.5px; }
.dept-text i { margin-right: 4px; color: $ink-4; }
.danger-text { color: #f56c6c; }

/* --- 弹窗表单 --- */
.ps-form-sec {
  font-size: 13px; font-weight: 600; color: $ink-2;
  padding: 4px 0 10px; margin: 4px 0 14px; border-bottom: 1px solid $line-2;
}
.ps-form-sec:not(:first-child) { margin-top: 4px; }
.fg2 { display: grid; grid-template-columns: repeat(2, 1fr); gap: 0 16px; }
.up-box { padding: 10px 12px; border: 1px dashed #dfe5ee; border-radius: 8px; background: #fbfcfe; }
.up-empty { color: #b7c1cc; font-size: 12px; margin-bottom: 8px; }
.up-tip { margin-top: 6px; color: $ink-4; font-size: 12px; }
.file-item { display: flex; align-items: center; gap: 8px; padding: 5px 0; font-size: 13px; }
.file-item i.el-icon-paperclip { color: $ink-4; }
.file-name { flex: 1; overflow: hidden; color: $blue; text-overflow: ellipsis; white-space: nowrap; }
.file-del { color: #f56c6c; padding: 0; }
.img-grid { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 8px; }
.img-item { position: relative; width: 96px; height: 68px; border-radius: 7px; overflow: hidden; border: 1px solid $line-2; }
.img-thumb { width: 100%; height: 100%; display: block; }
.img-del {
  position: absolute; top: 2px; right: 2px; width: 16px; height: 16px; line-height: 16px;
  text-align: center; border-radius: 50%; background: rgba(0, 0, 0, .55); color: #fff;
  font-size: 11px; cursor: pointer;
}
.ps-head-r ::v-deep .el-button + .el-button { margin-left: 8px; }

@media (max-width: 1080px) {
  .ps-head { flex-direction: column; }
  .ps-hero-stats { flex-wrap: wrap; }
  .fg2 { grid-template-columns: 1fr; }
}
</style>
