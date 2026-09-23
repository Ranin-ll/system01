<template>
  <div class="bank-detail app-container">
    <!-- ===== 页头 ===== -->
    <header class="bd-head">
      <div class="bd-head-l">
        <el-button size="mini" icon="el-icon-back" @click="goBack">返回题库列表</el-button>
        <div class="title-line">
          <h2>{{ bank.bankName || '题库详情' }}</h2>
          <el-tag v-if="bank.bankType === 'PRACTICE'" size="mini" type="warning" effect="plain">模拟题库</el-tag>
          <el-tag v-else size="mini" effect="plain">正式题库</el-tag>
          <el-tag size="mini" type="info" effect="plain">{{ bank.deptName || '未设置部门' }}</el-tag>
          <el-tag size="mini" :type="bank.status === 'ENABLED' ? 'success' : 'info'" effect="plain">
            {{ bank.status === 'ENABLED' ? '启用' : '停用' }}
          </el-tag>
        </div>
        <p>{{ bank.description || '暂无说明' }}</p>
      </div>
      <div class="bd-head-r">
        <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="reload">刷新</el-button>
      </div>
    </header>

    <div v-if="error" class="bd-error">
      <i class="el-icon-warning-outline" />
      <div><b>加载失败</b><p>{{ error }}</p></div>
      <el-button size="mini" type="primary" @click="reload">重试</el-button>
    </div>

    <template v-else>
      <!-- ===== 概览统计 ===== -->
      <section class="stat-row">
        <div class="stat-card primary">
          <span class="lb">题目总数</span>
          <strong>{{ stats.total }}</strong>
          <small>共 {{ bank.questionCount != null ? bank.questionCount : stats.total }} 条（含已停用）</small>
        </div>
        <div v-for="t in typeStat" :key="t.key" class="stat-card">
          <span class="lb"><i class="dot" :style="{ background: t.color }" />{{ t.label }}</span>
          <strong>{{ t.count }}</strong>
          <small>{{ t.pct }}</small>
        </div>
        <div class="stat-card">
          <span class="lb">知识点（章节）</span>
          <strong>{{ stats.kpCount }}</strong>
          <small>{{ stats.kpTop.length ? ('最多：' + stats.kpTop[0].name) : '未标注知识点' }}</small>
        </div>
      </section>

      <el-row :gutter="14">
        <!-- 题型分布 -->
        <el-col :xs="24" :sm="12">
          <section class="panel">
            <div class="panel-heading"><h3>题型分布</h3><span class="hint">共 {{ stats.total }} 题</span></div>
            <div v-for="t in typeStat" :key="t.key" class="bar-row">
              <span class="nm">{{ t.label }}</span>
              <span class="track"><i :style="{ width: t.width, background: t.color }" /></span>
              <span class="vv">{{ t.count }}　{{ t.pct }}</span>
            </div>
            <div v-if="!stats.total" class="bd-empty">该题库还没有题目</div>
          </section>
        </el-col>

        <!-- 难度分布 -->
        <el-col :xs="24" :sm="12">
          <section class="panel">
            <div class="panel-heading"><h3>难度分布</h3><span class="hint">用于评估组卷难度</span></div>
            <div v-for="d in diffStat" :key="d.key" class="bar-row">
              <span class="nm">{{ d.label }}</span>
              <span class="track"><i :style="{ width: d.width, background: d.color }" /></span>
              <span class="vv">{{ d.count }}　{{ d.pct }}</span>
            </div>
            <div v-if="!stats.total" class="bd-empty">—</div>
          </section>
        </el-col>
      </el-row>

      <!-- 知识点分布 -->
      <section class="panel">
        <div class="panel-heading">
          <h3>章节（知识点）分布 Top 10</h3>
          <span class="hint">按章节查看题目分布；当前组卷主链路为「题库 × 题型」配额，<b>按章节配比的能力保留为兜底</b></span>
        </div>
        <div v-if="stats.kpTop.length" class="kp-wrap">
          <span v-for="k in stats.kpTop" :key="k.name" class="kp-chip">
            {{ k.name }}<b>{{ k.count }}</b>
          </span>
        </div>
        <div v-else class="bd-empty">题目尚未标注知识点</div>
      </section>

      <!-- ===== 题目列表 ===== -->
      <section class="panel">
        <div class="panel-heading">
          <div><h3>题目列表 <el-tag size="mini" type="info" effect="plain">{{ bank.bankName }}</el-tag></h3>
            <p class="sub">维护单选、多选、判断题目；支持按题型 / 难度 / 关键字筛选。</p></div>
          <div class="panel-actions">
            <el-button v-hasPermi="['business:question:import']" size="mini" icon="el-icon-download" @click="downloadTpl">下载模板</el-button>
            <el-button v-hasPermi="['business:question:import']" size="mini" icon="el-icon-upload2" @click="openImport">批量导入</el-button>
            <el-button v-hasPermi="['business:question:list']" size="mini" icon="el-icon-document" :loading="exporting" @click="handleExport">导出 Excel</el-button>
            <el-button v-hasPermi="['business:question:add']" type="primary" size="mini" icon="el-icon-plus" @click="handleAdd">新增题目</el-button>
          </div>
        </div>

        <el-form :inline="true" size="mini" class="bd-filter">
          <el-form-item>
            <el-select v-model="query.qtype" clearable placeholder="全部题型" style="width:120px" @change="applyFilter">
              <el-option label="单选题" value="SINGLE" /><el-option label="多选题" value="MULTI" /><el-option label="判断题" value="JUDGE" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-select v-model="query.difficulty" clearable placeholder="全部难度" style="width:120px" @change="applyFilter">
              <el-option label="简单" value="EASY" /><el-option label="中等" value="MEDIUM" /><el-option label="困难" value="HARD" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-input v-model="query.kw" clearable placeholder="题干 / 知识点" style="width:190px" @keyup.enter.native="applyFilter" @clear="applyFilter" />
          </el-form-item>
          <el-form-item>
            <el-button icon="el-icon-refresh-left" @click="resetFilter">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="loading" :data="pagedList" stripe empty-text="该题库暂无题目，点「新增题目」开始录入">
          <el-table-column label="题型" width="82" align="center">
            <template slot-scope="s"><el-tag size="mini" effect="plain" :type="qtypeTag(s.row.qtype)">{{ qtypeText(s.row.qtype) }}</el-tag></template>
          </el-table-column>
          <el-table-column label="题干" min-width="300" show-overflow-tooltip>
            <template slot-scope="s">{{ s.row.stem }}</template>
          </el-table-column>
          <el-table-column prop="answer" label="答案" width="90" align="center" />
          <el-table-column label="难度" width="80" align="center">
            <template slot-scope="s"><span class="diff-text" :class="'d-' + String(s.row.difficulty || '').toLowerCase()">{{ diffText(s.row.difficulty) }}</span></template>
          </el-table-column>
          <el-table-column label="知识点（章节）" width="140">
            <template slot-scope="s"><span v-if="s.row.knowledgePoint" class="kp-text">{{ s.row.knowledgePoint }}</span><span v-else class="muted">—</span></template>
          </el-table-column>
          <el-table-column label="状态" width="76" align="center">
            <template slot-scope="s">
              <el-tag size="mini" effect="plain" :type="s.row.status === 1 ? 'success' : 'info'">{{ s.row.status === 1 ? '启用' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="130" align="center">
            <template slot-scope="s">
              <el-button v-hasPermi="['business:question:edit']" type="text" size="mini" @click="handleEdit(s.row)">编辑</el-button>
              <el-button v-hasPermi="['business:question:remove']" type="text" size="mini" class="danger-text" @click="handleDelete(s.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <pagination v-show="filtered.length > 0" :total="filtered.length" :page.sync="page" :limit.sync="pageSize" @pagination="noop" />
      </section>
    </template>

    <!-- ===== 批量导入弹窗 ===== -->
    <el-dialog title="Excel 批量导入" :visible.sync="importVisible" width="560px" append-to-body>
      <el-alert type="info" :closable="false" show-icon style="margin-bottom:14px"
                title="表头须与「下载模板」一致：题型 / 题干 / 选项A-D / 正确答案 / 解析 / 难度 / 知识点。导出的 Excel 可直接回导。" />
      <el-upload ref="upload" drag action="#" :auto-upload="false" :limit="1"
                 :on-change="onFileChange" :on-remove="onFileRemove" :file-list="fileList" accept=".xlsx,.xls">
        <i class="el-icon-upload" />
        <div class="el-upload__text">将 Excel 拖到此处，或<em>点击选择文件</em></div>
        <div slot="tip" class="el-upload__tip">仅支持 .xlsx / .xls；按行校验，失败行会返回具体原因</div>
      </el-upload>
      <span slot="footer">
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="submitImport">开始导入</el-button>
      </span>
    </el-dialog>

    <!-- ===== 题目编辑弹窗 ===== -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="680px" append-to-body>
      <el-form ref="questionForm" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="题型" prop="qtype">
          <el-radio-group v-model="form.qtype" @change="onQtypeChange">
            <el-radio-button label="SINGLE">单选题</el-radio-button>
            <el-radio-button label="MULTI">多选题</el-radio-button>
            <el-radio-button label="JUDGE">判断题</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="题干" prop="stem">
          <el-input v-model="form.stem" type="textarea" :rows="3" placeholder="请输入题干" />
        </el-form-item>

        <template v-if="form.qtype !== 'JUDGE'">
          <el-form-item v-for="opt in optionKeys" :key="opt" :label="'选项 ' + opt">
            <el-input v-model="form.options[opt]" :placeholder="'选项 ' + opt + ' 内容'" />
          </el-form-item>
        </template>
        <el-form-item v-else label="选项">
          <div class="judge-options"><span class="judge-item"><b>A</b> 对</span><span class="judge-item"><b>B</b> 错</span></div>
        </el-form-item>

        <el-form-item label="正确答案" prop="answer">
          <el-select v-if="form.qtype === 'MULTI'" v-model="form.answerList" multiple placeholder="选择多个正确答案" style="width:100%">
            <el-option v-for="opt in validOptionKeys" :key="opt" :label="opt" :value="opt" />
          </el-select>
          <el-select v-else-if="form.qtype === 'JUDGE'" v-model="form.answerList" placeholder="选择正确答案" style="width:100%">
            <el-option label="A（对）" value="A" /><el-option label="B（错）" value="B" />
          </el-select>
          <el-select v-else v-model="form.answerList" placeholder="选择正确答案" style="width:100%">
            <el-option v-for="opt in validOptionKeys" :key="opt" :label="opt" :value="opt" />
          </el-select>
        </el-form-item>

        <el-form-item label="解析"><el-input v-model="form.analysis" type="textarea" :rows="2" placeholder="答案解析（可选）" /></el-form-item>
        <el-form-item label="难度">
          <el-select v-model="form.difficulty" style="width:160px">
            <el-option label="简单" value="EASY" /><el-option label="中等" value="MEDIUM" /><el-option label="困难" value="HARD" />
          </el-select>
        </el-form-item>
        <el-form-item label="知识点（章节）"><el-input v-model="form.knowledgePoint" placeholder="建议填写：用于按章节归类查看（旧配比链路也会按章节抽题）" maxlength="64" /></el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { getBank } from '@/api/business/questionBank'
import {
  listQuestion, addQuestion, updateQuestion, delQuestion,
  exportQuestions, importQuestions, downloadQuestionTemplate
} from '@/api/business/question'

const QTYPES = [
  { key: 'SINGLE', label: '单选题', color: '#1764f5', tag: '' },
  { key: 'MULTI', label: '多选题', color: '#7a5af8', tag: 'warning' },
  { key: 'JUDGE', label: '判断题', color: '#12b76a', tag: 'success' }
]
const DIFFS = [
  { key: 'EASY', label: '简单', color: '#12b76a' },
  { key: 'MEDIUM', label: '中等', color: '#f79009' },
  { key: 'HARD', label: '困难', color: '#f04438' }
]
// 库里存在 SIMPLE 与 EASY 两种写法，统一按「简单」展示
const DIFF_ALIAS = { SIMPLE: 'EASY', EASY: 'EASY', MEDIUM: 'MEDIUM', HARD: 'HARD' }

export default {
  name: 'BankDetail',
  data() {
    return {
      loading: false,
      error: '',
      bank: {},
      /** 一次拉全量（题库量级小）—— 列表与统计都基于它，保证「统计数」与「列表条数」永远一致 */
      all: [],
      query: { qtype: '', difficulty: '', kw: '' },
      page: 1,
      pageSize: 10,
      dialogVisible: false,
      dialogTitle: '',
      submitting: false,
      /* 导入 / 导出 */
      importing: false,
      exporting: false,
      importVisible: false,
      importFile: null,
      fileList: [],
      form: this.blankForm(),
      optionKeys: ['A', 'B', 'C', 'D'],
      rules: {
        qtype: [{ required: true, message: '请选择题型', trigger: 'change' }],
        stem: [{ required: true, message: '请输入题干', trigger: 'blur' }]
      }
    }
  },
  computed: {
    bankId() {
      return this.$route.params.bankId
    },
    /** 筛选后的题目（列表与分页都基于它） */
    filtered() {
      const f = this.query
      const kw = f.kw.trim().toLowerCase()
      return this.all.filter(q => {
        if (f.qtype && q.qtype !== f.qtype) return false
        if (f.difficulty && DIFF_ALIAS[String(q.difficulty || '').toUpperCase()] !== f.difficulty) return false
        if (kw) {
          const hay = ((q.stem || '') + ' ' + (q.knowledgePoint || '')).toLowerCase()
          if (hay.indexOf(kw) < 0) return false
        }
        return true
      })
    },
    pagedList() {
      const start = (this.page - 1) * this.pageSize
      return this.filtered.slice(start, start + this.pageSize)
    },
    stats() {
      const all = this.all
      const total = all.length
      const kpMap = {}
      let kpCount = 0
      all.forEach(q => {
        const kp = (q.knowledgePoint || '').trim()
        if (kp) {
          kpMap[kp] = (kpMap[kp] || 0) + 1
          kpCount++
        }
      })
      const kpTop = Object.keys(kpMap)
        .map(name => ({ name: name, count: kpMap[name] }))
        .sort((a, b) => b.count - a.count)
        .slice(0, 10)
      return {
        total: total,
        kpCount: Object.keys(kpMap).length,
        kpTop: kpTop,
        answered: kpCount
      }
    },
    typeStat() {
      return QTYPES.map(t => this.decorate(t, this.all.filter(q => q.qtype === t.key).length))
    },
    diffStat() {
      return DIFFS.map(d => this.decorate(d, this.all.filter(q => DIFF_ALIAS[String(q.difficulty || '').toUpperCase()] === d.key).length))
    },
    validOptionKeys() {
      return this.form.qtype === 'JUDGE' ? ['A', 'B'] : this.optionKeys
    }
  },
  created() {
    this.loadBank()
    this.loadAll()
  },
  methods: {
    reload() {
      this.loadBank()
      this.loadAll()
    },
    loadBank() {
      return getBank(this.bankId).then(res => { this.bank = res.data || {} })
        .catch(err => { this.error = (err && err.message) ? err.message : '题库信息加载失败' })
    },
    loadAll() {
      this.loading = true
      this.error = ''
      // 站内题库量级很小（当前最大 49 题），一次拉全量：统计与列表同源，数字不会打架。
      // 若将来单库超过 1000 题，应改为「服务端分页 + 统计接口」。
      return listQuestion({ pageNum: 1, pageSize: 1000, bankId: this.bankId }).then(res => {
        this.all = res.rows || []
        this.page = 1
      }).catch(err => {
        this.all = []
        this.error = (err && err.message) ? err.message : '题目加载失败，请检查网络或权限后重试'
      }).finally(() => { this.loading = false })
    },
    goBack() {
      // 详情页挂在 /super 与 /department 两处，按当前路径前缀回到各自的题库列表
      const target = this.$route.path.indexOf('/super') === 0 ? '/super/ops/bank-admin' : '/department/study/banks'
      this.$router.push(target).catch(() => {})
    },
    decorate(meta, count) {
      const total = this.stats.total || 0
      return {
        key: meta.key,
        label: meta.label,
        color: meta.color,
        count: count,
        pct: total ? Math.round(count * 100 / total) + '%' : '—',
        width: total ? Math.max(Math.round(count * 100 / total), count ? 4 : 0) + '%' : '0%'
      }
    },
    applyFilter() {
      this.page = 1
    },
    resetFilter() {
      this.query = { qtype: '', difficulty: '', kw: '' }
      this.page = 1
    },
    noop() {},
    qtypeText(t) {
      const m = { SINGLE: '单选', MULTI: '多选', JUDGE: '判断' }
      return m[t] || t || '—'
    },
    qtypeTag(t) {
      const found = QTYPES.filter(x => x.key === t)[0]
      return found ? found.tag : ''
    },
    diffText(d) {
      const k = DIFF_ALIAS[String(d || '').toUpperCase()]
      const found = DIFFS.filter(x => x.key === k)[0]
      return found ? found.label : '—'
    },
    blankForm() {
      return {
        id: null, bankId: null, qtype: 'SINGLE', stem: '', options: { A: '', B: '', C: '', D: '' },
        answerList: [], answer: '', analysis: '', difficulty: 'EASY', knowledgePoint: ''
      }
    },
    onQtypeChange() {
      this.form.answerList = []
    },
    handleAdd() {
      this.form = this.blankForm()
      this.form.bankId = Number(this.bankId)
      this.dialogTitle = '新增题目 · ' + (this.bank.bankName || '')
      this.dialogVisible = true
    },
    handleEdit(row) {
      const opts = this.parseOptions(row)
      this.form = {
        id: row.id,
        bankId: row.bankId != null ? row.bankId : Number(this.bankId),
        qtype: row.qtype || 'SINGLE',
        stem: row.stem || '',
        options: Object.assign({ A: '', B: '', C: '', D: '' }, opts),
        answerList: String(row.answer || '').split(',').map(s => s.trim()).filter(Boolean),
        answer: row.answer || '',
        analysis: row.analysis || '',
        difficulty: DIFF_ALIAS[String(row.difficulty || '').toUpperCase()] || 'EASY',
        knowledgePoint: row.knowledgePoint || ''
      }
      this.dialogTitle = '编辑题目'
      this.dialogVisible = true
    },
    parseOptions(row) {
      const out = {}
      let arr = row.options
      if (!arr && row.optionsJson) {
        try {
          arr = JSON.parse(row.optionsJson)
        } catch (e) {
          arr = []
        }
      }
      ;(arr || []).forEach(o => {
        if (o && o.key) out[o.key] = o.content
      })
      return out
    },
    submit() {
      this.$refs.questionForm.validate(valid => {
        if (!valid) return
        const f = this.form
        if (!f.answerList.length) {
          this.$message.warning('请选择正确答案')
          return
        }
        const body = {
          id: f.id,
          bankId: f.bankId,
          qtype: f.qtype,
          stem: f.stem,
          answer: f.answerList.join(','),
          analysis: f.analysis,
          difficulty: f.difficulty,
          knowledgePoint: f.knowledgePoint,
          options: this.validOptionKeys
            .map(k => ({ key: k, content: (f.options[k] || '').trim() }))
            .filter(o => o.content)
        }
        this.submitting = true
        const req = f.id ? updateQuestion(body) : addQuestion(body)
        req.then(res => {
          this.$message.success(res.msg || '保存成功')
          this.dialogVisible = false
          this.loadAll()
        }).catch(() => {}).finally(() => { this.submitting = false })
      })
    },
    handleDelete(row) {
      this.$confirm('确定删除这道题？删除后不可恢复。', '提示', { type: 'warning' }).then(() => {
        return delQuestion(row.id).then(res => {
          this.$message.success(res.msg || '已删除')
          this.loadAll()
        })
      }).catch(() => {})
    },

    /* ---------------- 导入 / 导出 ---------------- */

    /** 下载导入模板（与导出同表头） */
    downloadTpl() {
      downloadQuestionTemplate().then(res => {
        this.saveBlob(res, '题目导入模板.xlsx')
      }).catch(() => { this.$message.error('模板下载失败，请稍后重试') })
    },
    /** 导出当前题库的题目（导出的文件可直接再导入到别的题库） */
    handleExport() {
      this.exporting = true
      exportQuestions(this.bankId).then(res => {
        this.saveBlob(res, (this.bank.bankName || '题库') + '-题目.xlsx')
        this.$message.success('已导出 ' + this.all.length + ' 道题')
      }).catch(() => {
        this.$message.error('导出失败，请稍后重试')
      }).finally(() => { this.exporting = false })
    },
    /**
     * blob 下载。
     * 若依的 request 拦截器对 responseType='blob' 的响应会原样透传，所以这里兼容
     * 「直接拿到 Blob」与「拿到 { data: Blob }」两种形态。
     */
    saveBlob(res, filename) {
      const blob = (res && res.data) ? res.data : res
      const url = window.URL.createObjectURL(new Blob([blob]))
      const a = document.createElement('a')
      a.href = url
      a.download = filename
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      window.URL.revokeObjectURL(url)
    },
    openImport() {
      this.importFile = null
      this.fileList = []
      this.importVisible = true
    },
    onFileChange(file) {
      this.importFile = file.raw
    },
    onFileRemove() {
      this.importFile = null
    },
    submitImport() {
      if (!this.importFile) {
        this.$message.warning('请先选择 Excel 文件')
        return
      }
      const fd = new FormData()
      fd.append('file', this.importFile)
      this.importing = true
      importQuestions(this.bankId, fd).then(res => {
        const r = res.data || {}
        const ok = r.success || 0
        const bad = r.failed || 0
        if (bad > 0) {
          this.$alert((r.errors || []).slice(0, 10).join('\n') || ('共 ' + bad + ' 行失败'),
            '导入结果：成功 ' + ok + ' 行 / 失败 ' + bad + ' 行', { type: 'warning' })
        } else {
          this.$message.success('导入成功 ' + ok + ' 行')
        }
        this.importVisible = false
        this.loadAll()
      }).catch(() => {
        this.$message.error('导入失败，请检查文件格式（表头须与模板一致）')
      }).finally(() => { this.importing = false })
    }
  }
}
</script>

<style lang="scss" scoped>
.bank-detail { padding-bottom: 20px; }
.bd-head {
  display: flex; align-items: flex-start; justify-content: space-between; gap: 16px;
  background: #fff; border: 1px solid #e4e9f0; border-radius: 12px; padding: 16px 18px; margin-bottom: 14px;
  .title-line { display: flex; align-items: center; gap: 8px; margin: 10px 0 6px; h2 { margin: 0; font-size: 19px; } }
  p { margin: 0; color: #667085; font-size: 12.5px; }
  .bd-head-r { flex: none; }
}
.bd-error {
  display: flex; align-items: center; gap: 12px; background: #feecea; border: 1px solid #f6c9c4;
  border-radius: 12px; padding: 14px 18px; color: #b42318; font-size: 13px;
  i { font-size: 20px; } b { display: block; margin-bottom: 2px; } p { margin: 0; font-size: 12.5px; }
  .el-button { margin-left: auto; }
}
.stat-row { display: grid; grid-template-columns: 1.2fr repeat(5, 1fr); gap: 12px; margin-bottom: 14px; }
@media (max-width: 1200px) { .stat-row { grid-template-columns: repeat(3, 1fr); } }
.stat-card {
  background: #fff; border: 1px solid #e4e9f0; border-radius: 10px; padding: 13px 15px;
  &.primary { background: #f7f9fc; }
  .lb { display: flex; align-items: center; gap: 6px; color: #667085; font-size: 12px; margin-bottom: 6px;
        .dot { width: 6px; height: 6px; border-radius: 50%; } }
  strong { font-size: 22px; color: #1d2939; }
  small { display: block; margin-top: 4px; color: #98a2b3; font-size: 11px; }
}
.panel { background: #fff; border: 1px solid #e4e9f0; border-radius: 12px; padding: 16px 18px; margin-bottom: 14px; }
.panel-heading {
  display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 12px;
  h3 { margin: 0; font-size: 14px; color: #1d2939; }
  .sub { margin: 4px 0 0; color: #98a2b3; font-size: 11.5px; }
  .hint { color: #98a2b3; font-size: 11.5px; }
  .panel-actions { flex: none; }
}
.bar-row {
  display: flex; align-items: center; gap: 10px; margin-bottom: 10px; font-size: 12.5px;
  .nm { width: 62px; color: #344054; flex: none; }
  .track { flex: 1; height: 9px; border-radius: 5px; background: #f2f4f7; overflow: hidden;
           i { display: block; height: 100%; border-radius: 5px; } }
  .vv { width: 84px; text-align: right; color: #667085; flex: none; }
}
.kp-wrap { display: flex; flex-wrap: wrap; gap: 8px; }
.kp-chip {
  display: inline-flex; align-items: center; gap: 6px; height: 26px; padding: 0 10px;
  border-radius: 13px; background: #f2f4f7; color: #344054; font-size: 12px;
  b { color: #1764f5; }
}
.bd-filter { margin-bottom: 4px; }
.diff-text {
  font-size: 12px;
  &.d-easy { color: #067647; }
  &.d-medium { color: #b54708; }
  &.d-hard { color: #b42318; }
}
.kp-text { color: #475467; font-size: 12px; }
.muted { color: #98a2b3; }
.danger-text { color: #f04438; }
.bd-empty { padding: 26px 0; text-align: center; color: #98a2b3; font-size: 12.5px; }
.judge-options { display: flex; gap: 20px; .judge-item { color: #344054; font-size: 13px; b { color: #1764f5; margin-right: 4px; } } }
</style>
