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
                <el-button size="mini" type="primary" icon="el-icon-plus" :disabled="!!examForm.id" @click="openExamEditor(null)">新建套卷</el-button>
              </div>
            </div>

            <el-table :data="exams" size="mini" border v-loading="examLoading" empty-text="暂无模拟理论考核套卷，点右上角「新建套卷」">
              <el-table-column label="考核名称" min-width="180">
                <template slot-scope="scope"><span class="strong">{{ scope.row.examName }}</span></template>
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
              <el-table-column label="操作" width="170" align="center">
                <template slot-scope="scope">
                  <el-button type="text" size="mini" @click="openExamEditor(scope.row)">配置</el-button>
                  <span class="sep">|</span>
                  <el-button v-if="scope.row.status !== 'PUBLISHED'" type="text" size="mini" @click="publishExamRow(scope.row)">发布</el-button>
                  <el-button v-else type="text" size="mini" @click="disableExamRow(scope.row)">停用</el-button>
                  <span class="sep">|</span>
                  <el-button type="text" size="mini" class="danger-text" @click="removeExam(scope.row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>

            <!-- 考核配置编辑区 -->
            <div v-if="examForm.id || examFormCreating" class="exam-editor">
              <div class="dcard-h" style="padding:12px 0 10px">
                <div class="tt">
                  <span class="idx o">配</span>
                  <h3>{{ examForm.id ? '编辑套卷配置' : '新增套卷配置' }}</h3>
                </div>
                <div class="dbtn-row">
                  <el-button size="mini" @click="closeExamEditor">取消</el-button>
                  <el-button size="mini" type="primary" :loading="saving" @click="saveExam">保存并生效</el-button>
                </div>
              </div>

              <el-form :inline="true" size="small" style="margin-bottom:12px">
                <el-form-item label="考核名称">
                  <el-input v-model="examForm.examName" size="small" style="width:220px" placeholder="例如：Java 基础自测" maxlength="60" />
                </el-form-item>
                <el-form-item label="考试时长">
                  <el-select v-model="durationPreset" size="small" style="width:160px" @change="onDurationPresetChange">
                    <el-option v-for="d in durationOptions" :key="d" :label="d > 0 ? d + ' 分钟' : '不限时（自测）'" :value="d" />
                    <el-option label="自定义…" :value="-1" />
                  </el-select>
                  <el-input-number v-if="durationPreset === -1" v-model="examForm.duration" :min="1" :max="600" size="small" style="width:120px;margin-left:8px" />
                </el-form-item>
                <el-form-item label="通过分数 / 总分">
                  <el-input-number v-model="examForm.passLine" :min="0" :max="500" :precision="1" size="small" />
                  <span style="margin-left:6px;color:#98a2b3">{{ examForm.passLine }} / {{ totalScore }} 分</span>
                </el-form-item>
              </el-form>

              <el-form :inline="true" size="small" style="margin-bottom:12px">
                <el-form-item label="难易程度">
                  <el-select v-model="examForm.difficulty" size="small" style="width:120px" placeholder="选择难度" clearable>
                    <el-option label="简单" value="EASY" />
                    <el-option label="中等" value="MEDIUM" />
                    <el-option label="困难" value="HARD" />
                  </el-select>
                </el-form-item>
                <el-form-item label="题目内容偏向">
                  <el-input v-model="examForm.contentBias" size="small" style="width:480px" maxlength="200" show-word-limit placeholder="给实习生看的侧重说明，例如：偏 Java 集合与并发，重点考多线程与锁" />
                </el-form-item>
              </el-form>

              <el-form :inline="true" size="small" style="margin-bottom:12px">
                <el-form-item label="每题分值">
                  <span>单选</span><el-input-number v-model="examForm.singleScore" :min="0" :max="100" :precision="1" size="small" style="width:110px" />
                  <span style="margin:0 6px">多选</span><el-input-number v-model="examForm.multiScore" :min="0" :max="100" :precision="1" size="small" style="width:110px" />
                  <span style="margin:0 6px">判断</span><el-input-number v-model="examForm.judgeScore" :min="0" :max="100" :precision="1" size="small" style="width:110px" />
                </el-form-item>
                <el-form-item>
                  <span class="dinp">题目数量 {{ questionTotal }} 题 · 满分 {{ totalScore }} 分</span>
                </el-form-item>
              </el-form>

              <div class="dcallout" :class="checkOk ? 'ok' : 'warn'" style="margin:0 0 12px">
                <i :class="checkOk ? 'el-icon-success' : 'el-icon-warning-outline'" />
                <span>
                  <template v-if="checkOk">校验通过：共 {{ bankRules.length }} 个题库 · 合计 {{ questionTotal }} 题（单选 {{ countOf('singleCount') }} + 多选 {{ countOf('multiCount') }} + 判断 {{ countOf('judgeCount') }}）· 满分 {{ totalScore }} 分 ✓</template>
                  <template v-else>{{ checkMessage }}</template>
                </span>
              </div>

              <div class="dgrid">
                <div class="c8">
                  <div class="dcard-h" style="padding:0 0 10px">
                    <div class="tt"><span class="idx">分</span><h3>组卷题库（每个题库分别设置抽题量）</h3></div>
                    <div class="dbtn-row">
                      <el-select v-model="addBankId" size="mini" placeholder="＋ 添加题库" style="width:200px" @change="addBankRule">
                        <el-option
                          v-for="b in bankMeta"
                          :key="b.bankId"
                          :label="b.bankName + '（可用 ' + b.totalCount + ' 题）'"
                          :value="b.bankId"
                          :disabled="bankChosen(b.bankId)"
                        />
                      </el-select>
                    </div>
                  </div>
                  <el-table :data="bankRules" size="mini" border empty-text="点右上角「＋ 添加题库」选择参与组卷的题库">
                    <el-table-column label="题库" min-width="160">
                      <template slot-scope="scope">
                        <el-select v-model="scope.row.bankId" size="mini" filterable style="width:100%">
                          <el-option v-for="b in bankMeta" :key="b.bankId" :label="b.bankName" :value="b.bankId" />
                        </el-select>
                      </template>
                    </el-table-column>
                    <el-table-column label="可用" width="76" align="center">
                      <template slot-scope="scope"><span class="muted">{{ bankAvailable(scope.row.bankId) }} 题</span></template>
                    </el-table-column>
                    <el-table-column label="单选" width="108" align="center">
                      <template slot-scope="scope">
                        <el-input-number v-model="scope.row.singleCount" :min="0" :max="bankAvailOf(scope.row.bankId, 'singleCount')" size="mini" style="width:92px" />
                      </template>
                    </el-table-column>
                    <el-table-column label="多选" width="108" align="center">
                      <template slot-scope="scope">
                        <el-input-number v-model="scope.row.multiCount" :min="0" :max="bankAvailOf(scope.row.bankId, 'multiCount')" size="mini" style="width:92px" />
                      </template>
                    </el-table-column>
                    <el-table-column label="判断" width="108" align="center">
                      <template slot-scope="scope">
                        <el-input-number v-model="scope.row.judgeCount" :min="0" :max="bankAvailOf(scope.row.bankId, 'judgeCount')" size="mini" style="width:92px" />
                      </template>
                    </el-table-column>
                    <el-table-column label="小计" width="70" align="center">
                      <template slot-scope="scope"><b>{{ ruleRowTotal(scope.row) }}</b></template>
                    </el-table-column>
                    <el-table-column label="操作" width="70" align="center">
                      <template slot-scope="scope">
                        <el-button type="text" size="mini" class="danger-text" @click="bankRules.splice(scope.$index, 1)">删除</el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                  <p class="dsec-note">抽题时在<b>每个题库内按题型随机取题</b>，再组合成卷。「抽题量 ≤ 题库可用」超了会抽不满卷，保存时后端会拦截。</p>
                </div>

                <div class="c4">
                  <div class="dcard-h" style="padding:0 0 10px">
                    <div class="tt"><span class="idx o">抽</span><h3>试抽一套（校验用）</h3></div>
                  </div>
                  <div class="dinp ta preview-box">
                    <div v-if="drawing" class="drawing"><i class="el-icon-loading" /> 正在抽题…</div>
                    <div v-else-if="previewQuestions.length">
                      <div v-for="q in previewQuestions" :key="q.seq" class="pq">{{ q.seq }} · {{ typeText(q.qtype) }} · {{ q.bankName }} · {{ q.stem }}</div>
                    </div>
                    <div v-else style="color:#98a2b3">点下方按钮，按当前组卷配置真实抽一套卷看看效果。</div>
                  </div>
                  <div class="dbtn-row" style="margin-top:12px">
                    <el-button size="small" :loading="drawing" @click="drawOnce">试抽一套</el-button>
                  </div>
                  <p class="dsec-note">保存后实习生即可看到本套卷并「开始练习」；模拟成绩仅本人可见、不计入正式成绩。</p>
                </div>
              </div>
            </div>

          </div>
      </div>
    </template>

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
  listExam, getExam, addExam, updateExam, delExam, publishExam, changeExamStatus,
  listExamBankOptions, tryDrawByBanks, saveExamConfig, getExamConfig
} from '@/api/business/exam'
import {
  previewKindOf, extLabel, fileUrlOf as resolveFileUrl, PREVIEW_SUPPORT_TEXT, triggerDownload
} from '@/utils/filePreview'
import {
  listMaterial, addMaterial, updateMaterial, changeMaterialStatus, delMaterial, listApplicablePositions, uploadFile
} from '@/api/business/material'
import { mapGetters } from 'vuex'

function emptyExamForm() {
  return {
    id: null,
    examName: '',
    singleScore: 1,
    multiScore: 1,
    judgeScore: 1,
    duration: 0,
    passLine: 6,
    difficulty: '',
    contentBias: ''
  }
}

import PracticeBankPicker from '@/views/business/practiceBank/PracticeBankPicker'

export default {
  name: 'DeptPrep',
  components: {
    PracticeBankPicker },
  data() {
    return {
      activeTab: 'material',
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
      // ---- 模拟理论考核（套卷，无阶段层级） ----
      exams: [],
      examLoading: false,
      examForm: emptyExamForm(),
      examFormCreating: false,
      bankMeta: [],
      bankRules: [],
      addBankId: null,
      drawing: false,
      saving: false,
      previewQuestions: [],
      durationOptions: [0, 30, 45, 60, 90, 120, 180, 240],
      durationPreset: 0
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
    /** 组卷抽题合计（各题库单选 + 多选 + 判断） */
    questionTotal() {
      return this.bankRules.reduce((sum, r) => sum + this.ruleRowTotal(r), 0)
    },
    totalScore() {
      return this.countOf('singleCount') * (Number(this.examForm.singleScore) || 0) +
        this.countOf('multiCount') * (Number(this.examForm.multiScore) || 0) +
        this.countOf('judgeCount') * (Number(this.examForm.judgeScore) || 0)
    },
    /** 超出题库可用题量的行 */
    overRows() {
      return this.bankRules.filter(r => {
        const avail = this.bankMetaOf(r.bankId)
        return (Number(r.singleCount) || 0) > avail.singleCount ||
          (Number(r.multiCount) || 0) > avail.multiCount ||
          (Number(r.judgeCount) || 0) > avail.judgeCount
      })
    },
    checkOk() {
      if (!this.bankRules.length) return false
      if (this.bankRules.some(r => !r.bankId)) return false
      if (this.questionTotal <= 0) return false
      return this.overRows.length === 0
    },
    checkMessage() {
      if (!this.bankRules.length) return '尚未配置组卷题库：点右上角「＋ 添加题库」选择参与组卷的题库，再分别设置每个题库的抽题数量。'
      if (this.bankRules.some(r => !r.bankId)) return '存在未选择题库的行，请选择对应题库或删除该行。'
      if (this.questionTotal <= 0) return '抽题数量合计为 0，请至少为一个题库设置抽题数量。'
      if (this.overRows.length) {
        return '「' + this.overRows.map(r => this.bankNameOf(r.bankId)).join('、') + '」的抽题数量超过该题库可用题量，会抽不满卷。'
      }
      return ''
    }
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
    /** 套卷列表 + 可选题库一起刷新（进页签 / 点刷新都用它） */
    loadExamTab() {
      this.loadModuleExams()
      this.loadBankMeta()
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
    /** 本部门可选题库（含各库按题型的可用题量） */
    loadBankMeta() {
      // 候选库 = 形态 × 用途：模拟理论卷只取「理论库 × 模拟/通用」（防抽到正式题库）
      listExamBankOptions(null, 'PRACTICE', 'THEORY').then(res => {
        this.bankMeta = (res.data || []).map(b => ({
          bankId: b.bankId,
          bankName: b.bankName,
          totalCount: Number(b.totalCount) || 0,
          singleCount: Number(b.singleCount) || 0,
          multiCount: Number(b.multiCount) || 0,
          judgeCount: Number(b.judgeCount) || 0
        }))
      }).catch(() => { this.bankMeta = [] })
    },
    openExamEditor(row) {
      this.previewQuestions = []
      if (!row) {
        this.examForm = emptyExamForm()
        this.examForm.examName = '模拟理论考核卷'
        this.examFormCreating = true
        this.bankRules = []
        this.durationPreset = 0
        return
      }
      this.examFormCreating = false
      this.examForm = {
        id: row.id,
        examName: row.examName || '',
        singleScore: Number(row.singleScore) || 0,
        multiScore: Number(row.multiScore) || 0,
        judgeScore: Number(row.judgeScore) || 0,
        duration: Number(row.duration) || 0,
        passLine: Number(row.passLine) || 0,
        difficulty: row.difficulty || '',
        contentBias: row.contentBias || ''
      }
      this.durationPreset = this.durationOptions.indexOf(this.examForm.duration) > -1 ? this.examForm.duration : -1
      getExamConfig(row.id).then(res => {
        const data = res.data || {}
        this.bankRules = (data.bankRules || []).map((r, i) => ({
          bankId: r.bankId,
          bankName: r.bankName,
          singleCount: Number(r.singleCount) || 0,
          multiCount: Number(r.multiCount) || 0,
          judgeCount: Number(r.judgeCount) || 0,
          sortNo: r.sortNo || i + 1
        }))
      }).catch(() => { this.bankRules = [] })
    },
    closeExamEditor() {
      this.examForm = emptyExamForm()
      this.examFormCreating = false
      this.bankRules = []
      this.previewQuestions = []
    },
    bankMetaOf(bankId) {
      return this.bankMeta.find(b => b.bankId === bankId) ||
        { bankName: '未选择题库', totalCount: 0, singleCount: 0, multiCount: 0, judgeCount: 0 }
    },
    bankNameOf(bankId) {
      return this.bankMetaOf(bankId).bankName || '未选择题库'
    },
    bankAvailable(bankId) {
      return this.bankMetaOf(bankId).totalCount
    },
    bankAvailOf(bankId, key) {
      const v = this.bankMetaOf(bankId)[key]
      return v === undefined ? 0 : v
    },
    bankChosen(bankId) {
      return this.bankRules.some(r => r.bankId === bankId)
    },
    ruleRowTotal(row) {
      return (Number(row.singleCount) || 0) + (Number(row.multiCount) || 0) + (Number(row.judgeCount) || 0)
    },
    /** 按题型汇总各库抽题量 */
    countOf(key) {
      return this.bankRules.reduce((sum, r) => sum + (Number(r[key]) || 0), 0)
    },
    addBankRule(bankId) {
      if (!bankId) return
      this.bankRules.push({
        bankId,
        bankName: this.bankNameOf(bankId),
        singleCount: 0,
        multiCount: 0,
        judgeCount: 0
      })
      this.addBankId = null
    },
    rulePayload() {
      return this.bankRules.map((r, i) => ({
        bankId: r.bankId,
        singleCount: r.singleCount,
        multiCount: r.multiCount,
        judgeCount: r.judgeCount,
        sortNo: i + 1
      }))
    },
    onDurationPresetChange(v) {
      if (v !== -1) {
        this.examForm.duration = v
      } else if (!this.examForm.duration) {
        this.examForm.duration = 60
      }
    },
    /** 试抽一套（真实调后端按多题库组卷配置抽题） */
    drawOnce() {
      if (!this.checkOk) {
        this.$modal.msgWarning(this.checkMessage || '请先完成组卷配置')
        return
      }
      this.drawing = true
      tryDrawByBanks({ bankRules: this.rulePayload() }).then(res => {
        this.previewQuestions = res.data || []
        this.drawing = false
        if (!this.previewQuestions.length) {
          this.$modal.msgWarning('按当前配置抽不到题，请检查各题库题目是否充足')
        }
      }).catch(() => { this.drawing = false })
    },
    /** 保存并生效：新建考核 → 保存组卷配置 → 发布；编辑则保存配置后按原状态决定是否发布 */
    saveExam() {
      if (!this.checkOk) {
        this.$modal.msgWarning(this.checkMessage || '配置校验未通过')
        return
      }
      if (!this.examForm.examName || !this.examForm.examName.trim()) {
        this.$modal.msgWarning('请填写考核名称')
        return
      }
      this.saving = true
      const configPayload = {
        examName: this.examForm.examName.trim(),
        singleScore: this.examForm.singleScore,
        multiScore: this.examForm.multiScore,
        judgeScore: this.examForm.judgeScore,
        duration: this.examForm.duration,
        passLine: this.examForm.passLine,
        difficulty: this.examForm.difficulty || null,
        contentBias: this.examForm.contentBias || null,
        bankRules: this.rulePayload(),
        assignMode: 'ALL'
      }
      if (this.examForm.id) {
        // 编辑：先改基本信息，再存配置；已发布的保持发布，草稿则发布
        updateExam(Object.assign({ id: this.examForm.id }, configPayload)).then(() => {
          this.afterConfigSaved(this.examForm.id)
        }).catch(() => { this.saving = false })
      } else {
        addExam(Object.assign({
          examMode: 'PRACTICE',
          examType: 'THEORY'
        }, configPayload)).then(res => {
          // addExam 走 toAjax，返回 AjaxResult；用列表回查拿到新考核ID
          listExam({ pageNum: 1, pageSize: 100, examMode: 'PRACTICE', examType: 'THEORY' }).then(r2 => {
            const rows = (r2 && r2.rows) || []
            const created = rows.slice().sort((a, b) => b.id - a.id)[0]
            if (!created) {
              this.saving = false
              this.$modal.msgWarning('创建考核失败，请重试')
              return
            }
            this.examForm.id = created.id
            this.afterConfigSaved(created.id)
          }).catch(() => { this.saving = false })
        }).catch(() => { this.saving = false })
      }
    },
    /** 保存组卷配置后再决定是否发布 */
    afterConfigSaved(examId) {
      saveExamConfig(examId, {
        singleScore: this.examForm.singleScore,
        multiScore: this.examForm.multiScore,
        judgeScore: this.examForm.judgeScore,
        duration: this.examForm.duration,
        passLine: this.examForm.passLine,
        difficulty: this.examForm.difficulty || null,
        contentBias: this.examForm.contentBias || null,
        bankRules: this.rulePayload(),
        assignMode: 'ALL'
      }).then(() => {
        publishExam(examId).then(() => {
          this.saving = false
          this.$modal.msgSuccess('套卷已保存并生效，实习生即可开始练习')
          this.closeExamEditor()
          this.loadModuleExams()
        }).catch(() => { this.saving = false })
      }).catch(() => { this.saving = false })
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
        this.closeExamEditor()
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
.drawing { color: #98a2b3; font-size: 12px; line-height: 2; }
code { padding: 1px 5px; color: #344054; font-size: 11.5px; background: #f2f4f7; border-radius: 4px; }
.embed-wrap { margin-top: 4px; }
.exam-editor { margin-top: 8px; padding: 0 14px 6px; background: #fbfcfe; border: 1px dashed #dfe5ee; border-radius: 8px; }
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
