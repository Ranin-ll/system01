<template>
  <div class="dept-page">
    <div class="dept-breadcrumb">
      学习与考核管理 <span>/</span> <b>模拟备考管理</b>
    </div>

    <div class="dept-heading">
      <div>
        <span class="eyebrow">DEPARTMENT ADMIN</span>
        <h1>模拟备考管理</h1>
        <p>备考资料 / 模拟理论考核配置 / 实操题库 —— 侧栏一项，模块内三个页签。</p>
      </div>
      <div class="dept-heading-actions">
        <span class="dsample">备考资料与模拟配置为示例数据</span>
      </div>
    </div>

    <!-- 页签条 -->
    <div class="dtabs page">
      <div
        v-for="tab in tabs"
        :key="tab.key"
        class="dtab"
        :class="{ on: activeTab === tab.key }"
        @click="activeTab = tab.key"
      >
        {{ tab.label }} <span v-if="tab.count" class="tab-n">{{ tab.count }}</span>
      </div>
    </div>

    <!-- ============ 页签 1 · 备考资料 ============ -->
    <template v-if="activeTab === 'material'">
      <div class="dgrid">
        <div class="dcard c5">
          <div class="dcard-h">
            <div class="tt"><span class="idx">传</span><h3>上传资料</h3></div>
          </div>
          <div class="ddrop" @click="pickFile">
            <i class="el-icon-upload" />
            <b>{{ form.fileName || '拖拽文件到此处，或点击选择' }}</b>
            <small>支持 PDF / Word / PPT / 视频 / 图片 / 压缩包</small>
          </div>
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
              <el-select v-model="form.position" size="small" style="width:100%">
                <el-option v-for="p in positionOptions" :key="p" :label="p" :value="p" />
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
          <div class="dfield">
            <label>有效期</label>
            <el-date-picker
              v-model="form.validRange"
              type="daterange"
              size="small"
              value-format="yyyy-MM-dd"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              style="width:100%"
            />
          </div>
          <div class="dbtn-row right" style="margin-top:16px">
            <el-button size="small" @click="notReady('存为草稿')">存为草稿</el-button>
            <el-button size="small" type="primary" @click="notReady('上传并发布')">上传并发布</el-button>
          </div>
        </div>

        <div class="dcard c7">
          <div class="dcard-h">
            <div class="tt"><span class="idx">列</span><h3>已上传资料</h3></div>
            <span class="hint-text">发布后本部门对应岗位实习生在「正式考核」页可见</span>
          </div>
          <table class="dtbl">
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
              <tr v-for="row in materials" :key="row.name">
                <td><span class="strong">{{ row.name }}</span></td>
                <td><span class="dbadge" :class="row.typeTone">{{ row.typeText }}</span></td>
                <td>{{ row.position }}</td>
                <td>{{ row.version }}</td>
                <td><span class="dbadge" :class="row.status === '已发布' ? 'green' : 'gray'">{{ row.status }}</span></td>
                <td>
                  <div class="acts">
                    <el-button type="text" @click="previewMaterial(row)">预览</el-button>
                    <span class="sep">|</span>
                    <el-button type="text" @click="notReady('替换文件')">替换</el-button>
                    <span class="sep">|</span>
                    <el-button v-if="row.status === '已发布'" type="text" @click="notReady('停用')">停用</el-button>
                    <template v-else>
                      <el-button type="text" @click="publishMaterial(row)">发布</el-button>
                      <span class="sep">|</span>
                      <el-button type="text" @click="removeMaterial(row)">删除</el-button>
                    </template>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
          <p class="dsec-note">
            落库 <code>material</code>（15 列，含 material_type / status / valid_from·to），
            <b>表结构完全够用，只缺 Java 层</b>。接口就绪前本页用示例数据并打「示例数据」标记。
          </p>
        </div>
      </div>
    </template>

    <!-- ============ 页签 2 · 模拟理论考核配置 ============ -->
    <template v-if="activeTab === 'config'">
      <div class="dsec">
        <div class="dsec-head">
          <div>
            <span class="dsec-no p">配</span>
            <div>
              <h2>模拟理论考核配置</h2>
              <p>保存生效后，本部门实习生「模拟考核 → 开始新自测」即按此配置抽题（知识分布 + 题型配比 + 分值）。</p>
            </div>
          </div>
          <div style="display:flex;align-items:center;gap:8px">
            <span class="dbadge" :class="configStatus.status === 'PUBLISHED' ? 'green' : 'orange'">{{ configStatus.text }}</span>
            <el-button size="mini" :loading="saving" type="primary" @click="saveConfig">保存并生效</el-button>
          </div>
        </div>
        <div class="dsec-body">
          <el-form :inline="true" size="small" style="margin-bottom:12px">
            <el-form-item label="题源（模拟题库）">
              <el-select v-model="bankId" filterable placeholder="本部门模拟题库" style="width:230px" @change="loadPoints">
                <el-option v-for="b in bankOptions" :key="b.id" :label="b.bankName + '（' + (b.questionCount || 0) + ' 题）'" :value="b.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="考试时长">
              <el-input-number v-model="form.duration" :min="0" :max="600" size="small" />
              <span class="dinp" style="display:inline-block;margin-left:6px">{{ form.duration > 0 ? form.duration + ' 分钟' : '不限时（自测）' }}</span>
            </el-form-item>
            <el-form-item label="通过分数 / 总分">
              <el-input-number v-model="form.passLine" :min="0" :max="200" :precision="1" size="small" />
              <span style="margin-left:6px;color:#98a2b3">{{ form.passLine }} / {{ totalScore }} 分</span>
            </el-form-item>
          </el-form>

          <el-form :inline="true" size="small" style="margin-bottom:12px">
            <el-form-item label="题型配比">
              <span>单选</span><el-input-number v-model="form.singleCount" :min="0" :max="100" size="small" style="width:110px" />
              <span style="margin:0 6px">多选</span><el-input-number v-model="form.multiCount" :min="0" :max="100" size="small" style="width:110px" />
              <span style="margin:0 6px">判断</span><el-input-number v-model="form.judgeCount" :min="0" :max="100" size="small" style="width:110px" />
            </el-form-item>
            <el-form-item label="每题分值">
              <span>单选</span><el-input-number v-model="form.singleScore" :min="0" :max="100" :precision="1" size="small" style="width:110px" />
              <span style="margin:0 6px">多选</span><el-input-number v-model="form.multiScore" :min="0" :max="100" :precision="1" size="small" style="width:110px" />
              <span style="margin:0 6px">判断</span><el-input-number v-model="form.judgeScore" :min="0" :max="100" :precision="1" size="small" style="width:110px" />
            </el-form-item>
            <el-form-item>
              <span class="dinp">题目数量 {{ questionTotal }} 题 · 满分 {{ totalScore }} 分</span>
            </el-form-item>
          </el-form>

          <div class="dcallout" :class="checkOk ? 'ok' : 'warn'" style="margin:0 0 12px">
            <i :class="checkOk ? 'el-icon-success' : 'el-icon-warning-outline'" />
            <span>
              <template v-if="checkOk">校验通过：抽题数量合计 {{ ruleCountSum }} = 题目数量 {{ questionTotal }} ✓　占比合计 {{ ratioSum }}% ✓　每行不超题库可用 ✓</template>
              <template v-else>{{ checkMessage }}</template>
            </span>
          </div>

          <div class="dgrid">
            <div class="c8">
              <div class="dcard-h" style="padding:0 0 10px">
                <div class="tt"><span class="idx">分</span><h3>知识分布（按题库知识点抽题）</h3></div>
                <div class="dbtn-row">
                  <el-button size="mini" @click="loadPoints">从题库导入知识点</el-button>
                  <el-button size="mini" @click="addRule">＋ 新增</el-button>
                </div>
              </div>
              <el-table :data="rules" size="mini" border empty-text="暂无知识点，点「从题库导入知识点」">
                <el-table-column label="知识模块（题库知识点）" min-width="150">
                  <template slot-scope="scope">
                    <el-select v-if="scope.row.editing" v-model="scope.row.knowledgePoint" size="mini" filterable placeholder="选择知识点" style="width:100%">
                      <el-option v-for="p in points" :key="p.knowledgePoint" :label="p.knowledgePoint" :value="p.knowledgePoint" />
                    </el-select>
                    <span v-else>{{ scope.row.knowledgePoint }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="题库可用" width="86" align="center">
                  <template slot-scope="scope">{{ availableOf(scope.row.knowledgePoint) }} 题</template>
                </el-table-column>
                <el-table-column label="抽题数量" width="130" align="center">
                  <template slot-scope="scope">
                    <el-input-number v-model="scope.row.questionCount" :min="0" :max="100" size="mini" style="width:108px" />
                  </template>
                </el-table-column>
                <el-table-column label="占比" width="100" align="center">
                  <template slot-scope="scope">
                    <el-input-number v-model="scope.row.ratio" :min="0" :max="100" size="mini" style="width:84px" />
                  </template>
                </el-table-column>
                <el-table-column label="单选题占比" width="120" align="center">
                  <template slot-scope="scope">
                    <el-input-number v-model="scope.row.singleRatio" :min="0" :max="100" size="mini" style="width:100px" />
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="90" align="center">
                  <template slot-scope="scope">
                    <el-button type="text" size="mini" class="danger-text" @click="rules.splice(scope.$index, 1)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <p class="dsec-note">抽题时在该知识点内随机取题；「抽题数量 ≤ 题库可用」超了会抽不出题，保存时后端会拦截。</p>
            </div>

            <div class="c4">
              <div class="dcard-h" style="padding:0 0 10px">
                <div class="tt"><span class="idx o">抽</span><h3>试抽一套（校验用）</h3></div>
              </div>
              <div class="dinp ta preview-box">
                <div v-if="drawing" class="drawing"><i class="el-icon-loading" /> 正在抽题…</div>
                <div v-else-if="previewQuestions.length">
                  <div v-for="q in previewQuestions" :key="q.seq" class="pq">{{ q.seq }} · {{ q.qtype }} · {{ q.knowledgePoint }} · {{ q.stem }}</div>
                </div>
                <div v-else style="color:#98a2b3">点下方按钮，按当前知识分布真实抽一套卷看看效果。</div>
              </div>
              <div class="dbtn-row" style="margin-top:12px">
                <el-button size="small" :loading="drawing" @click="drawOnce">试抽一套</el-button>
                <el-button size="small" type="primary" :loading="saving" @click="saveConfig">保存并生效</el-button>
              </div>
              <p class="dsec-note">保存后掉「模拟考核」的实习生立即按新配置抽题；模拟成绩仍仅本人可见、不计入正式成绩。</p>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- ============ 页签 3 · 实操题库（复用已闭环模块） ============ -->
    <div v-if="activeTab === 'practice'" class="embed-wrap">
      <practice-subject />
    </div>
  </div>
</template>

<script>
import { listPracticeSubject } from '@/api/business/practiceSubject'
import PracticeSubject from '@/views/business/practiceSubject/index'
import { listBank } from '@/api/business/questionBank'
import { listExam, addExam, publishExam, listKnowledgePoints, tryDrawPaper, saveExamConfig, getExamConfig } from '@/api/business/exam'

export default {
  name: 'DeptPrep',
  components: { PracticeSubject },
  data() {
    return {
      activeTab: 'material',
      subjectTotal: 0,
      materials: [
        { name: '2026Q3 考试指南.pdf', typeText: '文档', typeTone: 'blue', position: '开发实习生', version: 'v1.0', status: '已发布' },
        { name: 'Spring Boot 实操要点.docx', typeText: '文档', typeTone: 'blue', position: '开发实习生', version: 'v2.1', status: '已发布' },
        { name: 'Docker 部署演示.mp4', typeText: '视频', typeTone: 'purple', position: '开发实习生', version: 'v1.0', status: '已发布' },
        { name: '模拟题入口说明.pdf', typeText: '模拟题入口', typeTone: 'orange', position: '交付实习生', version: 'v1.0', status: '草稿' }
      ],
      form: { name: '', type: 'DOCUMENT', position: '', version: 'v1.0', intro: '', fileName: '', validRange: [] },
      // ---- 模拟理论考核配置（真实：落 exam + exam_knowledge_rule，驱动实习生抽题）----
      bankId: null,
      bankOptions: [],
      points: [],
      rules: [],
      configExam: null,
      drawing: false,
      saving: false,
      previewQuestions: [],
      form: { singleCount: 5, multiCount: 3, judgeCount: 2, singleScore: 1, multiScore: 1, judgeScore: 1, duration: 0, passLine: 6 }
    }
  },
  computed: {
    tabs() {
      return [
        { key: 'material', label: '备考资料', count: this.materials.length },
        { key: 'config', label: '模拟理论考核配置' },
        { key: 'practice', label: '实操题库', count: this.subjectTotal || '' }
      ]
    },
    positionOptions() {
      return ['交付实习生', '实施实习生', '开发实习生', '设计实习生', '质检实习生', '建模实习生']
    },
    questionTotal() {
      return (this.form.singleCount || 0) + (this.form.multiCount || 0) + (this.form.judgeCount || 0)
    },
    totalScore() {
      return (this.form.singleCount || 0) * (this.form.singleScore || 0) +
        (this.form.multiCount || 0) * (this.form.multiScore || 0) +
        (this.form.judgeCount || 0) * (this.form.judgeScore || 0)
    },
    ruleCountSum() {
      return this.rules.reduce((sum, r) => sum + (Number(r.questionCount) || 0), 0)
    },
    ratioSum() {
      return Math.round(this.rules.reduce((sum, r) => sum + (Number(r.ratio) || 0), 0) * 100) / 100
    },
    overRows() {
      return this.rules.filter(r => (Number(r.questionCount) || 0) > this.availableOf(r.knowledgePoint))
    },
    checkOk() {
      if (!this.rules.length) return false
      if (this.ruleCountSum !== this.questionTotal) return false
      if (Math.abs(this.ratioSum - 100) > 0.01) return false
      return this.overRows.length === 0
    },
    checkMessage() {
      if (!this.rules.length) return '尚未配置知识分布：点「从题库导入知识点」一键铺满，再按需调整抽题数量。'
      if (this.ruleCountSum !== this.questionTotal) {
        return '抽题数量合计 ' + this.ruleCountSum + ' 与「题目数量」' + this.questionTotal + ' 不一致，请调整到一致。'
      }
      if (Math.abs(this.ratioSum - 100) > 0.01) {
        return '占比合计为 ' + this.ratioSum + '%，需调整为 100%。'
      }
      if (this.overRows.length) {
        return '「' + this.overRows.map(r => r.knowledgePoint).join('、') + '」抽题数量超过题库可用题量，抽不出题。'
      }
      return ''
    },
    configStatus() {
      if (!this.configExam) return { status: 'NONE', text: '未配置' }
      return {
        status: this.configExam.status,
        text: this.configExam.status === 'PUBLISHED' ? '已生效 ' + (this.configExam.examName || '') : '草稿未生效'
      }
    }
  },
  created() {
    this.loadSubjectTotal()
    this.loadConfig()
  },
  methods: {
    loadSubjectTotal() {
      listPracticeSubject({ pageNum: 1, pageSize: 1 }).then(res => {
        this.subjectTotal = (res && res.total) || 0
      }).catch(() => {
        this.subjectTotal = 0
      })
    },
    pickFile() {
      this.form.fileName = '2026Q3 考试指南.pdf'
      this.$message({ message: '演示态：文件选择依赖 material 接口（待补）', type: 'warning' })
    },
    previewMaterial(row) {
      this.$message.info('演示态：预览「' + row.name + '」需要 material 文件地址接口')
    },
    publishMaterial(row) {
      row.status = '已发布'
      this.$message.success('已发布「' + row.name + '」（演示态）')
    },
    removeMaterial(row) {
      this.materials = this.materials.filter(m => m !== row)
      this.$message.success('已删除（演示态）')
    },
    /** 加载本部门模拟题库 + 已有配置 */
    loadConfig() {
      listBank({ pageNum: 1, pageSize: 200, status: 'ENABLED' }).then(res => {
        const banks = (res.rows || []).filter(b => (b.bankType || '') === 'PRACTICE' || (b.bankName || '').indexOf('模拟') > -1)
        this.bankOptions = banks
        if (banks.length) {
          this.bankId = banks[0].id
          this.loadPoints()
        }
      })
      listExam({ pageNum: 1, pageSize: 50, examMode: 'PRACTICE' }).then(res => {
        const cfg = (res.rows || []).find(r => r.examType === 'THEORY')
        if (!cfg) return
        this.configExam = cfg
        this.bankId = cfg.bankId || this.bankId
        this.form = {
          singleCount: cfg.singleCount || 0,
          multiCount: cfg.multiCount || 0,
          judgeCount: cfg.judgeCount || 0,
          singleScore: Number(cfg.singleScore) || 1,
          multiScore: Number(cfg.multiScore) || 1,
          judgeScore: Number(cfg.judgeScore) || 1,
          duration: cfg.duration || 0,
          passLine: Number(cfg.passLine) || 0
        }
        this.loadConfigDetail(cfg.id)
      }).catch(() => {})
    },
    /** 读取配置的知识分布 */
    loadConfigDetail(id) {
      getExamConfig(id).then(res => {
        const data = res.data || {}
        const rows = (data.knowledgeRules || []).map((r, i) => ({
          knowledgePoint: r.knowledgePoint,
          questionCount: r.questionCount,
          ratio: r.ratio == null ? null : Number(r.ratio),
          singleRatio: r.singleRatio == null ? null : Number(r.singleRatio),
          sortNo: i + 1,
          editing: false
        }))
        if (rows.length) {
          this.rules = rows
        } else if (this.points.length) {
          this.importAll()
        }
      }).catch(() => {})
    },
    /** 从题库拉取知识点及题量 */
    loadPoints() {
      if (!this.bankId) return
      listKnowledgePoints(this.bankId).then(res => {
        this.points = res.data || []
        if (!this.rules.length) {
          this.importAll()
        }
      }).catch(() => { this.points = [] })
    },
    /** 一键铺满知识点（按可用题量分摊到题目数量） */
    importAll() {
      const need = this.questionTotal || 10
      const usable = this.points.filter(p => p.totalCount > 0)
      if (!usable.length) {
        this.$modal.msgWarning('该题库暂无题目，先去题库管理导入题目')
        return
      }
      const totalAvail = usable.reduce((sum, p) => sum + p.totalCount, 0)
      let assigned = 0
      this.rules = usable.map((p, idx) => {
        let cnt = Math.floor(need * p.totalCount / totalAvail)
        if (cnt < 1) cnt = 1
        if (cnt > p.totalCount) cnt = p.totalCount
        assigned += cnt
        return {
          knowledgePoint: p.knowledgePoint,
          questionCount: cnt,
          ratio: Math.round(need * p.totalCount / totalAvail / need * 10000) / 100,
          singleRatio: 50,
          sortNo: idx + 1,
          editing: false
        }
      })
      // 微调使合计等于题目数量
      let diff = need - assigned
      for (let i = 0; i < this.rules.length && diff !== 0; i++) {
        const avail = this.availableOf(this.rules[i].knowledgePoint)
        if (diff > 0 && this.rules[i].questionCount < avail) {
          this.rules[i].questionCount++
          diff--
        } else if (diff < 0 && this.rules[i].questionCount > 1) {
          this.rules[i].questionCount--
          diff++
        }
      }
      // 占比按题量重算（合计 100%）
      const sum = this.rules.reduce((a, r) => a + r.questionCount, 0) || 1
      this.rules.forEach(r => { r.ratio = Math.round(r.questionCount / sum * 10000) / 100 })
      this.$modal.msgSuccess('已按题库知识点铺满 ' + this.rules.length + ' 行，请核对抽题数量与占比')
    },
    addRule() {
      this.rules.push({ knowledgePoint: '', questionCount: 1, ratio: 0, singleRatio: 50, sortNo: this.rules.length + 1, editing: true })
    },
    availableOf(point) {
      const hit = this.points.find(p => p.knowledgePoint === point)
      return hit ? hit.totalCount : 0
    },
    /** 试抽一套（真实调后端按知识分布抽题） */
    drawOnce() {
      if (!this.configExam && !this.bankId) {
        this.$modal.msgWarning('请先选择题源题库')
        return
      }
      if (!this.rules.length) {
        this.$modal.msgWarning('请先配置知识分布')
        return
      }
      this.drawing = true
      tryDrawPaper(this.bankId, { knowledgeRules: this.rules }).then(res => {
        this.previewQuestions = res.data || []
        this.drawing = false
        if (!this.previewQuestions.length) {
          this.$modal.msgWarning('按当前配置抽不到题，请检查题库知识点是否有题')
        }
      }).catch(() => { this.drawing = false })
    },
    /** 保存并生效：无配置则先建模拟理论考核 → 保存知识分布 → 发布 */
    saveConfig() {
      if (!this.bankId) {
        this.$modal.msgWarning('请先选择题源题库')
        return
      }
      if (!this.checkOk) {
        this.$modal.msgWarning(this.checkMessage || '配置校验未通过')
        return
      }
      this.saving = true
      const payload = {
        examName: (this.configExam && this.configExam.examName) || '本部门模拟理论自测配置',
        examMode: 'PRACTICE',
        examType: 'THEORY',
        bankId: this.bankId,
        singleCount: this.form.singleCount,
        multiCount: this.form.multiCount,
        judgeCount: this.form.judgeCount,
        singleScore: this.form.singleScore,
        multiScore: this.form.multiScore,
        judgeScore: this.form.judgeScore,
        duration: this.form.duration,
        passLine: this.form.passLine,
        knowledgeRules: this.rules.map((r, i) => ({
          knowledgePoint: r.knowledgePoint,
          questionCount: r.questionCount,
          ratio: r.ratio,
          singleRatio: r.singleRatio,
          sortNo: i + 1
        }))
      }
      const afterSave = (examId) => {
        saveExamConfig(examId, { knowledgeRules: payload.knowledgeRules, assignMode: 'ALL' }).then(() => {
          if (this.configExam && this.configExam.status === 'PUBLISHED') {
            this.saving = false
            this.$modal.msgSuccess('配置已保存并生效，实习生模拟考核将按新配置抽题')
            this.loadConfig()
            return
          }
          publishExam(examId).then(() => {
            this.saving = false
            this.$modal.msgSuccess('配置已保存并生效（已发布），实习生模拟考核将按新配置抽题')
            this.loadConfig()
          }).catch(() => { this.saving = false })
        }).catch(() => { this.saving = false })
      }
      if (this.configExam) {
        afterSave(this.configExam.id)
      } else {
        addExam(payload).then(() => {
          listExam({ pageNum: 1, pageSize: 50, examMode: 'PRACTICE' }).then(res => {
            const cfg = (res.rows || []).find(r => r.examType === 'THEORY')
            this.configExam = cfg || null
            afterSave(cfg ? cfg.id : null)
          }).catch(() => { this.saving = false })
        }).catch(() => { this.saving = false })
      }
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
.preview-box { min-height: 132px; }
.pq { color: #344054; font-size: 11.5px; line-height: 2; }
.drawing { color: #98a2b3; font-size: 12px; line-height: 2; }
code { padding: 1px 5px; color: #344054; font-size: 11.5px; background: #f2f4f7; border-radius: 4px; }
.embed-wrap { margin-top: 4px; }

/* 复用组件自带页头与内边距，嵌入时收掉一层 */
.embed-wrap ::v-deep .psubject-page { padding: 0; background: transparent; }
.embed-wrap ::v-deep .psubject-page.app-container { padding: 0; }
</style>
