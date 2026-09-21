<template>
  <section class="dm-card">
    <!-- ===== 卡头：类型角标 + 名称 + 操作 ===== -->
    <div class="dm-head">
      <div class="dm-title">
        <span class="dm-idx" :class="{ green: isPractice }">{{ isPractice ? '实' : '理' }}</span>
        <h3>{{ exam.examName }}</h3>
        <el-tag size="mini" effect="plain" :type="isPractice ? 'success' : 'primary'">{{ typeLabel }}</el-tag>
        <el-tag size="mini" :type="statusTag">{{ statusLabel }}</el-tag>
        <span v-if="isSuperAdmin" class="dm-hint">{{ exam.deptName || '未设置部门' }}</span>
      </div>
      <div class="dm-actions">
        <el-button v-hasPermi="['business:bank:edit']" size="mini" @click="$emit('edit', exam)">编辑</el-button>
        <el-button
          v-if="exam.status !== 'DRAFT'"
          v-hasPermi="['business:bank:list']"
          size="mini"
          :icon="isPractice ? 'el-icon-edit-outline' : 'el-icon-view'"
          @click="$emit('grade', exam)"
        >{{ isPractice ? '批改答卷' : '查看答卷' }}</el-button>
        <el-button
          v-if="exam.status === 'DRAFT'"
          v-hasPermi="['business:bank:edit']"
          size="mini"
          type="primary"
          :loading="publishing"
          @click="handlePublish"
        >保存发布</el-button>
        <el-button
          v-if="exam.status === 'PUBLISHED' || exam.status === 'GRADING'"
          v-hasPermi="['business:bank:edit']"
          size="mini"
          @click="$emit('status-change', exam, 'DISABLED')"
        >停用</el-button>
        <el-button
          v-if="exam.status === 'DISABLED'"
          v-hasPermi="['business:bank:edit']"
          size="mini"
          type="primary"
          plain
          @click="$emit('status-change', exam, 'PUBLISHED')"
        >启用</el-button>
        <el-button
          v-hasPermi="['business:bank:remove']"
          size="mini"
          type="text"
          class="danger-text"
          @click="$emit('delete', exam)"
        >删除</el-button>
      </div>
    </div>

    <div class="dm-grid">
      <!-- ===== 左：概览 ===== -->
      <div class="dm-fields">
        <!-- ↓ 可就地编辑的三项（保存走 updateExam；已发布/批改中的正式考核为只读） -->
        <div class="field edit">
          <span>考核名称 <b v-if="canEditBase">*</b></span>
          <el-input v-model="baseForm.examName" size="mini" maxlength="128" class="f-input" :disabled="!canEditBase" placeholder="请输入考核名称" />
        </div>
        <div class="field"><span>考试形式<i class="f-note">创建后锁定</i></span><b>{{ isPractice ? '实操考试' : '理论考试' }}</b></div>
        <div class="field edit">
          <span>考试时长</span>
          <el-select v-model="durationPreset" size="mini" class="f-input" :disabled="!canEditBase" @change="onDurationPresetChange">
            <el-option v-for="d in durationOptions" :key="d" :label="d > 0 ? d + ' 分钟（到点自动交卷）' : '不限时'" :value="d" />
            <el-option label="自定义…" :value="-1" />
          </el-select>
        </div>
        <div v-if="durationPreset === -1" class="field edit">
          <span>自定义时长（分钟）</span>
          <el-input-number v-model="baseForm.duration" :min="1" :max="600" size="mini" controls-position="right" class="f-input" :disabled="!canEditBase" />
        </div>
        <div class="field edit">
          <span>通过线（分）</span>
          <el-input-number v-model="baseForm.passLine" :min="0" :max="999" :precision="1" size="mini" controls-position="right" class="f-input" :disabled="!canEditBase" />
        </div>
        <div class="field-tip">
          <template v-if="canEditBase">
            <el-button size="mini" type="primary" plain :loading="baseSaving" :disabled="!baseDirty" @click="saveBase">保存基本信息</el-button>
            <span class="f-hint" :class="{ bad: baseScoreLevel === 'danger' }">{{ baseScoreHint }}</span>
          </template>
          <span v-else class="f-hint lock"><i class="el-icon-lock" /> 已发布／批改中的正式考核不能改基本信息，可先「停用」再改</span>
        </div>

        <template v-if="isPractice">
          <div class="field"><span>题目数量<i class="f-note">自动计算</i></span><b>{{ subjectItems.length }} 道</b></div>
          <div class="field"><span>卷面满分<i class="f-note">自动计算</i></span><b>{{ subjectTotalScore }} 分</b></div>
        </template>
        <template v-else>
          <div class="field"><span>组卷题库<i class="f-note">右侧配置</i></span><b>{{ bankText }}</b></div>
          <div class="field"><span>题型与分值<i class="f-note">自动汇总</i></span><b>{{ theorySpec }}</b></div>
          <div class="field"><span>题目数量<i class="f-note">自动计算</i></span><b>{{ exam.questionCount || 0 }} 题</b></div>
          <div class="field"><span>卷面满分<i class="f-note">自动计算</i></span><b>{{ ruleTotalScore }} 分</b></div>
        </template>

        <div class="field"><span>考核时间<i class="f-note">下方设置</i></span><b>{{ windowText }}</b></div>
        <div class="field"><span>发布范围<i class="f-note">下方设置</i></span><b>{{ assignText }}</b></div>
        <div class="field"><span>已作答 / 待批阅<i class="f-note">统计</i></span><b>{{ exam.answeredCount || 0 }} / {{ exam.pendingCount || 0 }}</b></div>
      </div>

      <!-- ===== 右：内容（理论＝组卷题库 / 实操＝题目清单） ===== -->
      <div class="dm-table-wrap">
        <!-- 理论：多题库 × 题型配额 -->
        <template v-if="!isPractice">
          <div class="dm-sub-head">
            <h4>组卷规则（每个题库各出几道什么题型）</h4>
            <div class="dm-actions">
              <el-select v-model="addBankId" size="mini" placeholder="＋ 添加题库" style="width:170px" @change="addBankRule">
                <el-option
                  v-for="b in bankMeta"
                  :key="b.bankId"
                  :label="b.bankName + '（可用 ' + b.totalCount + ' 题）'"
                  :value="b.bankId"
                  :disabled="bankChosen(b.bankId)"
                />
              </el-select>
              <el-button size="mini" :loading="drawing" @click="tryDraw">试抽一套</el-button>
              <el-button size="mini" :loading="configSaving" type="primary" plain @click="saveConfig">保存配置</el-button>
            </div>
          </div>

          <p class="draw-basis">
            当前主链路按「<b>题库 × 题型</b>」配额抽题；未配组卷时回退历史链路（模拟考核按<b>知识点/章节</b>配比）。
            <el-tag size="mini" effect="plain" :type="drawBasisTag">{{ drawBasisText }}</el-tag>
          </p>

          <!-- 每题分值：与抽题数量同一处设置，改完点「保存配置」落库 -->
          <div class="score-bar">
            <span class="sb-title">每题分值</span>
            <label class="sb-item">单选<el-input-number v-model="singleScore" :min="0" :max="100" :precision="1" controls-position="right" size="mini" style="width:88px" />分</label>
            <label class="sb-item">多选<el-input-number v-model="multiScore" :min="0" :max="100" :precision="1" controls-position="right" size="mini" style="width:88px" />分</label>
            <label class="sb-item">判断<el-input-number v-model="judgeScore" :min="0" :max="100" :precision="1" controls-position="right" size="mini" style="width:88px" />分</label>
            <span class="sb-total">卷面满分 <b>{{ ruleTotalScore }}</b> 分</span>
          </div>

          <el-table :data="bankRules" size="mini" border empty-text="点右上角「＋ 添加题库」选择参与组卷的题库">
            <el-table-column label="题库" min-width="164">
              <template slot-scope="scope">
                <el-select v-model="scope.row.bankId" size="mini" filterable style="width:100%">
                  <el-option v-for="b in bankMeta" :key="b.bankId" :label="b.bankName" :value="b.bankId" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="可用题量" width="102" align="center">
              <template slot-scope="scope"><span class="muted">{{ bankAvailable(scope.row.bankId) }} 题</span></template>
            </el-table-column>
            <el-table-column label="单选" width="102" align="center">
              <template slot-scope="scope">
                <el-input-number v-model="scope.row.singleCount" :min="0" :max="inputMaxOf(scope.row.bankId, 'singleCount')" size="mini" style="width:86px" />
              </template>
            </el-table-column>
            <el-table-column label="多选" width="102" align="center">
              <template slot-scope="scope">
                <el-input-number v-model="scope.row.multiCount" :min="0" :max="inputMaxOf(scope.row.bankId, 'multiCount')" size="mini" style="width:86px" />
              </template>
            </el-table-column>
            <el-table-column label="判断" width="102" align="center">
              <template slot-scope="scope">
                <el-input-number v-model="scope.row.judgeCount" :min="0" :max="inputMaxOf(scope.row.bankId, 'judgeCount')" size="mini" style="width:86px" />
              </template>
            </el-table-column>
            <el-table-column label="小计" width="112" align="center">
              <template slot-scope="scope">
                <strong class="num">{{ ruleRowTotal(scope.row) }}</strong> 题
                <span class="sb-sub">/ {{ ruleRowScore(scope.row) }} 分</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="70" align="center">
              <template slot-scope="scope">
                <el-button type="text" size="mini" class="danger-text" @click="bankRules.splice(scope.$index, 1)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="dm-callout" :class="checkOk ? 'ok' : 'warn'">
            <span v-if="checkOk">
              校验通过：共 {{ bankRules.length }} 个题库 · 合计 {{ ruleCountSum }} 题（单选 {{ ruleSumOf('single') }} + 多选 {{ ruleSumOf('multi') }} + 判断 {{ ruleSumOf('judge') }}） · 满分 {{ ruleTotalScore }} 分 ✓
            </span>
            <span v-else>{{ checkMessage }}</span>
          </div>
        </template>

        <!-- 实操：题目清单（题序号 tag 切换查看每道题） -->
        <template v-else>
          <div class="dm-sub-head">
            <h4>题目清单（共 {{ subjectItems.length }} 道 · 卷面满分 {{ subjectTotalScore }} 分）</h4>
            <div class="dm-actions">
              <template v-if="subjectItems.length">
                <span class="sb-title">全部设为</span>
                <el-input-number v-model="bulkScore" :min="0" :max="1000" :precision="1" controls-position="right" size="mini" style="width:88px" />
                <el-button size="mini" @click="applyBulkScore">应用</el-button>
              </template>
              <el-button v-hasPermi="['business:bank:edit']" size="mini" icon="el-icon-edit" @click="$emit('edit', exam)">编辑题目</el-button>
            </div>
          </div>

          <div v-if="!subjectItems.length" class="dm-empty">
            <i class="el-icon-document-add" />
            <span>尚未配置实操题目：点右上角「编辑题目」逐条填写，至少一道题才能发布</span>
          </div>

          <template v-else>
            <div class="sj-tags">
              <span
                v-for="(s, i) in subjectItems"
                :key="s.id || i"
                class="sj-tag"
                :class="{ on: i === activeSubjectIndex }"
                @click="activeSubjectIndex = i"
              >
                第 {{ i + 1 }} 题
                <em>{{ s.score || 0 }} 分</em>
              </span>
            </div>
            <div v-if="activeSubject" class="sj-detail">
              <div class="sj-line">
                <span class="sj-key">题干</span>
                <div class="sj-val strong">{{ activeSubject.title || '未填写' }}</div>
              </div>
              <div class="sj-line">
                <span class="sj-key">题目描述</span>
                <div class="sj-val pre">{{ activeSubject.description || '未填写' }}</div>
              </div>
              <div class="sj-line">
                <span class="sj-key">参考</span>
                <div class="sj-val">
                  <template v-if="activeImages.length">
                    <template v-for="(img, i) in activeImages">
                      <video v-if="isVideo(img.url)" :key="'v' + i" :src="baseApi + img.url" controls class="sj-video" />
                      <el-image v-else :key="'i' + i" :src="baseApi + img.url" :preview-src-list="activePreview" fit="cover" class="sj-img" />
                    </template>
                  </template>
                  <span v-else class="sj-ph">
                    <svg viewBox="0 0 92 66" xmlns="http://www.w3.org/2000/svg" width="92" height="66">
                      <rect width="92" height="66" rx="6" fill="#f2f7ff" stroke="#d7e7fc" stroke-width="1"/>
                      <rect x="18" y="18" width="24" height="20" rx="3" fill="#d7e7fc"/>
                      <circle cx="25" cy="24" r="4" fill="#b6d0f8"/>
                      <rect x="18" y="42" width="24" height="3" rx="1.5" fill="#b6d0f8"/>
                      <rect x="50" y="18" width="26" height="6" rx="3" fill="#b6d0f8"/>
                      <rect x="50" y="28" width="26" height="4" rx="2" fill="#cfdffb"/>
                      <rect x="50" y="36" width="18" height="4" rx="2" fill="#dbe8fc"/>
                    </svg>
                  </span>
                </div>
              </div>
              <div class="sj-line">
                <span class="sj-key">本题满分</span>
                <div class="sj-val">
                  <el-input-number v-model="activeSubject.score" :min="0" :max="1000" :precision="1" controls-position="right" size="mini" style="width:104px" />
                  <span class="sb-sub" style="margin-left:8px">分</span>
                </div>
              </div>
              <div class="sj-line">
                <span class="sj-key">附件</span>
                <div class="sj-val">
                  <template v-if="activeAttachments.length">
                    <div v-for="(a, i) in activeAttachments" :key="i" class="sj-file-row">
                      <a :href="baseApi + a.url" target="_blank" class="sj-file" :title="a.name"><i class="el-icon-paperclip" /> {{ a.name }}</a>
                      <a :href="baseApi + a.url" download class="sj-download"><i class="el-icon-download" /> 下载</a>
                    </div>
                  </template>
                  <span v-else class="muted">无</span>
                </div>
              </div>
            </div>
          </template>
        </template>
      </div>
    </div>

    <!-- ===== 发布设置（本条考核独立） ===== -->
    <div class="stage-publish">
      <div class="dm-sub-head">
        <h4>{{ typeLabel }}发布设置</h4>
      </div>
      <div class="dm-grid">
        <div class="dm-fields">
          <div class="field">
            <span>发布方式</span>
            <el-radio-group v-model="publishMode" size="mini">
              <el-radio-button label="NOW">立即发布</el-radio-button>
              <el-radio-button label="TIMED">定时发布</el-radio-button>
            </el-radio-group>
          </div>
          <div v-if="publishMode === 'TIMED'" class="field block">
            <span class="block-label">开放 / 截止时间</span>
            <el-date-picker
              v-model="timeRange"
              type="datetimerange"
              size="mini"
              unlink-panels
              range-separator="至"
              start-placeholder="开放时间"
              end-placeholder="截止时间"
              value-format="yyyy-MM-dd HH:mm:ss"
              style="width:100%"
            />
          </div>
        </div>
        <div class="dm-table-wrap">
          <div class="assign-bar">
            <el-radio-group v-model="assignMode" size="mini">
              <el-radio-button label="ALL">全部在培实习生</el-radio-button>
              <el-radio-button label="ASSIGNED">指定人员</el-radio-button>
            </el-radio-group>
            <span v-if="assignMode === 'ASSIGNED'" class="ap-scope">可选范围：{{ scopeLabel }}</span>
            <el-button class="ap-save" size="mini" :loading="configSaving" @click="saveConfig">保存设置</el-button>
          </div>

          <!-- 指定人员：按范围拉花名册，复选框勾选 -->
          <div v-if="assignMode === 'ASSIGNED'" class="assign-pick">
            <div class="ap-head">
              <el-input
                v-model="internKeyword"
                size="mini"
                clearable
                placeholder="搜索姓名 / 岗位"
                prefix-icon="el-icon-search"
                style="width:190px"
              />
              <el-select v-if="isSuperAdmin" v-model="internDeptFilter" size="mini" clearable placeholder="全部部门" style="width:150px">
                <el-option v-for="d in internDepts" :key="d.deptId" :label="d.deptName" :value="d.deptId" />
              </el-select>
              <el-button size="mini" @click="checkAllInterns">全选当前列表</el-button>
              <el-button size="mini" @click="clearAllInterns">清空已选</el-button>
              <span class="ap-count">已选 <b>{{ participantIds.length }}</b> 人 · 当前显示 {{ filteredInterns.length }} 人</span>
            </div>
            <div class="ap-list">
              <el-checkbox-group v-model="participantIds" class="ap-group">
                <el-checkbox v-for="u in filteredInterns" :key="u.userId" :label="u.userId" class="ap-item">
                  <span class="ap-name">{{ u.nickName }}</span>
                  <span v-if="isSuperAdmin && u.deptName" class="ap-dept">{{ u.deptName }}</span>
                  <span class="ap-meta">{{ u.positionName || '未分配岗位' }}</span>
                  <el-tag v-if="u.offRoster" size="mini" type="warning" effect="plain">已不在在培名单</el-tag>
                </el-checkbox>
              </el-checkbox-group>
              <div v-if="!filteredInterns.length" class="ap-empty">
                <i class="el-icon-user" />
                <span>{{ internKeyword || internDeptFilter ? '没有匹配到的在培实习生' : '该范围内暂无可指定的在培实习生' }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

  </section>
</template>

<script>
import { getExamConfig, saveExamConfig, publishExam, listExamBankOptions, tryDrawByBanks, listExamInternOptions, updateExam } from '@/api/business/exam'

/**
 * 单条考核的配置卡。
 *
 * 理论与实操彻底分开后，一条考核 = 一类（THEORY / PRACTICAL）。
 *  · 理论：组卷题库（多题库 × 题型配额）+ 发布设置
 *  · 实操：题目清单（逐题填写，不抽题）+ 发布设置
 * 两者的「发布方式 / 时间窗 / 参与人员」是同一套逻辑，各自独立、互不影响。
 */
export default {
  name: 'ExamConfigCard',
  props: {
    /** 考核记录（列表行） */
    exam: { type: Object, required: true },
    isSuperAdmin: { type: Boolean, default: false },
    /** 超管据此取题库与花名册 */
    deptId: { type: [Number, String], default: null }
  },
  data() {
    return {
      bankMeta: [],
      bankRules: [],
      addBankId: null,
      drawing: false,
      configSaving: false,
      publishing: false,
      publishMode: 'NOW',
      timeRange: [],
      assignMode: 'ALL',
      participantIds: [],
      internOptions: [],
      /**
       * 理论考核每题分值（在组卷题库同一处设置，随「保存配置」落库到 exam 的
       * single_score / multi_score / judge_score）。初值取列表行，loadConfig 时再同步一次。
       */
      singleScore: 0,
      multiScore: 0,
      judgeScore: 0,
      /** 实操「全部设为」批量满分输入值 */
      bulkScore: 10,
      /** 指定人员花名册的搜索词 / 超管的部门筛选（null = 全部部门） */
      internKeyword: '',
      internDeptFilter: null,
      subjectItems: [],
      activeSubjectIndex: 0,
      metaReady: false,

      // === 2026-09-21：左栏「基本参数」就地编辑（名称/时长/通过线）===
      baseForm: { examName: '', passLine: 0, duration: 0 },
      durationPreset: 0,
      durationOptions: [0, 30, 45, 60, 90, 120, 150, 180, 240],
      baseSaving: false
    }
  },
  computed: {
    isPractice() { return this.exam.examType === 'PRACTICAL' },
    typeLabel() { return this.isPractice ? '实操考核' : '理论考核' },
    baseApi() { return process.env.VUE_APP_BASE_API || '' },
    statusLabel() {
      return { DRAFT: '待发布', PUBLISHED: '已发布', GRADING: '待批改', DISABLED: '已停用' }[this.exam.status] || this.exam.status
    },
    statusTag() {
      return { DRAFT: 'info', PUBLISHED: 'success', GRADING: 'warning', DISABLED: 'danger' }[this.exam.status] || 'info'
    },
    // === 2026-09-21：基本参数（就地编辑）===
    /** 已发布 / 批改中的正式考核不允许改基本信息（与后端 updateExam 的约束一致） */
    canEditBase() {
      const e = this.exam || {}
      if (e.examMode === 'PRACTICE') return true
      return e.status === 'DRAFT' || e.status === 'DISABLED'
    },
    baseDirty() {
      const e = this.exam || {}
      return String(this.baseForm.examName || '').trim() !== String(e.examName || '')
        || Number(this.baseForm.passLine) !== Number(e.passLine)
        || Number(this.baseForm.duration) !== Number(e.duration)
    },
    /** 通过线 vs 卷面满分（实时，不等保存） */
    baseTotal() {
      return this.isPractice ? this.subjectTotalScore : this.ruleTotalScore
    },
    baseScoreLevel() {
      const pass = Number(this.baseForm.passLine) || 0
      if (!this.baseTotal) return 'danger'
      if (pass > this.baseTotal) return 'danger'
      if (pass === this.baseTotal) return 'warn'
      return 'ok'
    },
    baseScoreHint() {
      if (!this.baseTotal) return this.isPractice ? '尚未添加题目，卷面满分为 0' : '尚未组卷，卷面满分为 0'
      if (this.baseScoreLevel === 'danger') return '高于卷面满分 ' + this.baseTotal + ' 分，保存后无法发布'
      if (this.baseScoreLevel === 'warn') return '等于卷面满分，须全对才及格'
      return '卷面满分 ' + this.baseTotal + ' 分，余量 ' + (Math.round((this.baseTotal - (Number(this.baseForm.passLine) || 0)) * 10) / 10) + ' 分'
    },
    /** 当前这条考核实际走哪条抽题链路（界面上要看得见，避免双轨制下"不知道走哪条"） */
    drawBasisText() {
      if (this.bankRules && this.bankRules.length) return '当前生效：题库 × 题型'
      const mode = this.exam && this.exam.examMode
      return mode === 'PRACTICE' ? '当前生效：按章节(知识点)配比（历史链路）' : '当前生效：单库兜底（未配组卷）'
    },
    drawBasisTag() {
      return this.bankRules && this.bankRules.length ? 'success' : 'warning'
    },
    bankText() {
      if (this.bankRules.length) return this.bankRules.map(r => this.bankNameOf(r.bankId)).join(' + ')
      return this.exam.bankName || '未配置'
    },
    ruleCountSum() { return this.bankRules.reduce((sum, r) => sum + this.ruleRowTotal(r), 0) },
    ruleTotalScore() {
      return round1(this.ruleSumOf('single') * (Number(this.singleScore) || 0) +
        this.ruleSumOf('multi') * (Number(this.multiScore) || 0) +
        this.ruleSumOf('judge') * (Number(this.judgeScore) || 0))
    },
    theorySpec() {
      const parts = []
      if (this.ruleSumOf('single') > 0) parts.push('单选 ' + this.ruleSumOf('single') + '×' + (Number(this.singleScore) || 0) + ' 分')
      if (this.ruleSumOf('multi') > 0) parts.push('多选 ' + this.ruleSumOf('multi') + '×' + (Number(this.multiScore) || 0) + ' 分')
      if (this.ruleSumOf('judge') > 0) parts.push('判断 ' + this.ruleSumOf('judge') + '×' + (Number(this.judgeScore) || 0) + ' 分')
      return parts.join(' + ') || '待配置'
    },
    /** 实操卷面满分 = 各题满分之和 */
    subjectTotalScore() {
      const sum = this.subjectItems.reduce((acc, s) => acc + (Number(s.score) || 0), 0)
      return Math.round(sum * 100) / 100
    },
    activeSubject() { return this.subjectItems[this.activeSubjectIndex] || null },
    activeImages() { return parseJsonList(this.activeSubject && this.activeSubject.referenceImages) },
    activeAttachments() { return parseJsonList(this.activeSubject && this.activeSubject.attachmentsJson) },
    activePreview() { return this.activeImages.filter(i => !this.isVideo(i.url)).map(i => this.baseApi + i.url) },
    windowText() {
      const start = this.exam.startTime ? String(this.exam.startTime).slice(0, 16) : ''
      const end = this.exam.endTime ? String(this.exam.endTime).slice(0, 16) : ''
      if (!start && !end) return '未设置（立即发布）'
      return (start || '--') + ' ~ ' + (end || '--')
    },
    assignText() {
      if (this.assignMode === 'ASSIGNED') return '指定 ' + this.participantIds.length + ' 人'
      return '全部在培实习生'
    },
    /** 可选名单范围文案：部门管理员只看本部门，超管看全部部门 */
    scopeLabel() {
      return this.isSuperAdmin ? '全部部门在培实习生' : '本部门在培实习生'
    },
    /** 超管的部门筛选下拉项（从花名册里抽出出现过的部门） */
    internDepts() {
      const seen = {}
      const out = []
      this.internOptions.forEach(u => {
        if (!u.deptId || seen[u.deptId]) return
        seen[u.deptId] = true
        out.push({ deptId: u.deptId, deptName: u.deptName || ('部门 ' + u.deptId) })
      })
      return out
    },
    /** 按部门筛选 + 关键词过滤后的花名册（勾选面板与「全选」都以此为准） */
    filteredInterns() {
      const kw = (this.internKeyword || '').trim().toLowerCase()
      return this.internOptions.filter(u => {
        if (this.internDeptFilter && String(u.deptId) !== String(this.internDeptFilter)) return false
        if (!kw) return true
        return [u.nickName, u.userName, u.deptName, u.positionName]
          .some(v => String(v === null || v === undefined ? '' : v).toLowerCase().indexOf(kw) > -1)
      })
    },
    overRows() {
      return this.bankRules.filter(r => {
        const avail = this.bankMetaOf(r.bankId)
        return (Number(r.singleCount) || 0) > avail.singleCount ||
          (Number(r.multiCount) || 0) > avail.multiCount ||
          (Number(r.judgeCount) || 0) > avail.judgeCount
      })
    },
    checkOk() {
      if (this.isPractice) return true // 实操不走题库
      if (!this.bankRules.length) return false
      if (this.bankRules.some(r => !r.bankId)) return false
      if (this.ruleCountSum <= 0) return false
      if (this.ruleTotalScore <= 0) return false
      return this.overRows.length === 0
    },
    checkMessage() {
      if (!this.bankRules.length) return '尚未配置组卷题库：点右上角「＋ 添加题库」选择参与组卷的题库，再分别设置每个题库的抽题数量。'
      if (this.bankRules.some(r => !r.bankId)) return '存在未选择题库的行，请选择对应题库或删除该行。'
      if (this.ruleCountSum <= 0) return '抽题数量合计为 0，请至少为一个题库设置抽题数量，或删除该题库行。'
      if (this.ruleTotalScore <= 0) return '每题分值都是 0，卷面满分为 0。请在上方「每题分值」里设置单选 / 多选 / 判断题的每题分值。'
      if (this.overRows.length) {
        return '「' + this.overRows.map(r => this.bankNameOf(r.bankId)).join('、') + '」的抽题数量超过该题库可用题量，会抽不满题。'
      }
      return ''
    }
  },
  watch: {
    exam: {
      immediate: true,
      handler() { if (this.metaReady) this.loadConfig() }
    },
    deptId() { this.loadBankMeta().then(() => this.loadConfig()) },
    /** 换一条考核时重置花名册的搜索/部门筛选，避免沿用上一条的过滤条件 */
    'exam.id'() {
      this.internKeyword = ''
      this.internDeptFilter = null
      this.resetBase()
    }
  },
  created() {
    this.bootstrap()
  },
  methods: {
    /**
     * 先取题库清单，再读考核配置。
     * 顺序很重要：题库清单没到位时「可用题量」返回 0，而 el-input-number 的 :max
     * 会在渲染瞬间把超出上限的已存值「夹」成 0 并写回模型 —— 页面明明配了题却提示
     * 「抽题数量合计为 0」。理论考核必须等清单到位后再渲染抽题量。
     */
    bootstrap() {
      this.resetBase()
      this.loadBankMeta().then(() => {
        this.metaReady = true
        this.loadConfig()
      })
    },
    loadConfig() {
      getExamConfig(this.exam.id).then(res => {
        const data = res.data || {}
        this.bankRules = (data.bankRules || []).map((r, i) => ({
          bankId: r.bankId,
          bankName: r.bankName,
          singleCount: Number(r.singleCount) || 0,
          multiCount: Number(r.multiCount) || 0,
          judgeCount: Number(r.judgeCount) || 0,
          sortNo: r.sortNo || i + 1
        }))
        this.subjectItems = data.subjectItems || []
        if (this.activeSubjectIndex >= this.subjectItems.length) this.activeSubjectIndex = 0
        // 每题分值不在 configDetail 里返回，取自列表行（exam.singleScore 等），确保与库中一致
        this.singleScore = numOf(this.exam.singleScore, 0)
        this.multiScore = numOf(this.exam.multiScore, 0)
        this.judgeScore = numOf(this.exam.judgeScore, 0)
        this.assignMode = data.assignMode || 'ALL'
        // 统一成数字：el-checkbox 的 label 与 v-model 必须是同一类型，否则回显勾不上
        this.participantIds = (data.participantIds || []).map(Number).filter(n => !isNaN(n))
        if (data.startTime && data.endTime) {
          this.publishMode = 'TIMED'
          this.timeRange = [String(data.startTime).slice(0, 19), String(data.endTime).slice(0, 19)]
        } else {
          this.publishMode = 'NOW'
          this.timeRange = []
        }
        // 名单依赖部门：放在 participantIds 之后，超管才能按考核部门取到候选人
        this.loadInterns()
      }).catch(() => {})
    },
    /**
     * 拉可选实习生花名册。
     * 不传 deptId：部门管理员由后端强制收窄到本部门（忽略入参，防越权）；
     * 超级管理员拿到的是**全部部门**的在培实习生，再在面板里按部门筛选。
     */
    loadInterns() {
      listExamInternOptions().then(res => {
        const list = (res.data || []).map(u => ({
          userId: Number(u.userId),
          userName: u.userName,
          nickName: u.nickName || u.userName || ('用户' + u.userId),
          deptId: u.deptId ? Number(u.deptId) : null,
          deptName: u.deptName,
          positionName: u.positionName
        })).filter(u => !!u.userId)
        const known = {}
        list.forEach(u => { known[u.userId] = true })
        // 已保存名单里可能有已不在在培花名册的人：补占位项并打标，避免勾选项凭空消失
        ;(this.participantIds || []).forEach(id => {
          if (!known[id]) {
            list.push({ userId: Number(id), nickName: '用户' + id, deptName: '', deptId: null, positionName: '', offRoster: true })
          }
        })
        this.internOptions = list
      }).catch(() => { this.internOptions = [] })
    },
    /** 全选当前筛选结果；在其他部门筛选下已勾选的人保持不动 */
    checkAllInterns() {
      const picked = {}
      ;(this.participantIds || []).forEach(id => { picked[id] = true })
      this.filteredInterns.forEach(u => { picked[u.userId] = true })
      this.participantIds = Object.keys(picked).map(Number)
    },
    clearAllInterns() {
      this.participantIds = []
    },
    checkAssignReady() {
      if (this.assignMode === 'ASSIGNED' && !(this.participantIds || []).length) {
        this.$modal.msgWarning('已选择「指定人员」，请至少勾选一名实习生；如需全员可见请改选「全部在培实习生」')
        return false
      }
      return true
    },
    /** 拉取本部门可选题库（含各库按题型的可用题量）；返回 Promise 供 bootstrap 串行等待 */
    loadBankMeta() {
      if (this.isSuperAdmin && !this.deptId) {
        this.bankMeta = []
        return Promise.resolve()
      }
      return listExamBankOptions(this.isSuperAdmin ? this.deptId : undefined, this.exam && this.exam.examMode).then(res => {
        this.bankMeta = (res.data || []).map(b => ({
          bankId: b.bankId,
          bankName: b.bankName,
          bankType: b.bankType,
          totalCount: Number(b.totalCount) || 0,
          singleCount: Number(b.singleCount) || 0,
          multiCount: Number(b.multiCount) || 0,
          judgeCount: Number(b.judgeCount) || 0
        }))
      }).catch(() => { this.bankMeta = [] })
    },
    /** 题库清单查不到该库时不设上限，避免 el-input-number 把已存值夹成 0 */
    inputMaxOf(bankId, key) {
      const meta = this.bankMeta.find(b => b.bankId === bankId)
      if (!meta) return Number.MAX_SAFE_INTEGER
      const v = Number(meta[key])
      return isNaN(v) ? Number.MAX_SAFE_INTEGER : v
    },
    bankMetaOf(bankId) {
      return this.bankMeta.find(b => b.bankId === bankId) ||
        { bankName: '未选择题库', totalCount: 0, singleCount: 0, multiCount: 0, judgeCount: 0 }
    },
    bankNameOf(bankId) { return this.bankMetaOf(bankId).bankName || '未选择题库' },
    bankAvailable(bankId) { return this.bankMetaOf(bankId).totalCount },
    bankChosen(bankId) { return this.bankRules.some(r => r.bankId === bankId) },
    ruleRowTotal(row) {
      return (Number(row.singleCount) || 0) + (Number(row.multiCount) || 0) + (Number(row.judgeCount) || 0)
    },
    ruleSumOf(type) {
      const key = { single: 'singleCount', multi: 'multiCount', judge: 'judgeCount' }[type]
      if (!key) return 0
      return this.bankRules.reduce((sum, r) => sum + (Number(r[key]) || 0), 0)
    },
    /** 单行（单个题库）按当前每题分值换算出的小计分 */
    ruleRowScore(row) {
      const s = Number(this.singleScore) || 0
      const m = Number(this.multiScore) || 0
      const j = Number(this.judgeScore) || 0
      return round1((Number(row.singleCount) || 0) * s + (Number(row.multiCount) || 0) * m + (Number(row.judgeCount) || 0) * j)
    },
    /** 按扩展名判断是否为视频（参考/附件为视频时用 <video> 渲染） */
    isVideo(url) { return /\.(mp4|webm|ogg|ogv|mov|avi|m4v)$/i.test(String(url || '')) },
    /** 实操：把「全部设为」的分数应用到每一道题 */
    applyBulkScore() {
      if (!this.subjectItems.length) return
      const v = Number(this.bulkScore)
      if (isNaN(v) || v < 0) {
        this.$modal.msgWarning('请输入不小于 0 的分值')
        return
      }
      this.subjectItems.forEach(s => { s.score = v })
      this.$modal.msgSuccess('已把 ' + this.subjectItems.length + ' 道题的满分设为 ' + v + ' 分，点「保存设置」生效')
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
    bankRulePayload() {
      return this.bankRules.map((r, i) => ({
        bankId: r.bankId,
        singleCount: r.singleCount,
        multiCount: r.multiCount,
        judgeCount: r.judgeCount,
        sortNo: i + 1
      }))
    },
    tryDraw() {
      if (!this.bankRules.length || this.ruleCountSum <= 0) {
        this.$modal.msgWarning('请先添加题库并设置抽题数量')
        return
      }
      this.drawing = true
      tryDrawByBanks({ bankRules: this.bankRulePayload() }).then(res => {
        const list = res.data || []
        this.drawing = false
        if (!list.length) {
          this.$modal.msgWarning('按当前配置抽不到题，请检查各题库的题目是否充足')
          return
        }
        const typeText = t => ({ SINGLE: '单选', MULTI: '多选', JUDGE: '判断' }[t] || t)
        this.$alert(
          list.map(q => q.seq + ' · ' + typeText(q.qtype) + ' · ' + (q.bankName || '') + ' · ' + q.stem).join('<br/>'),
          '试抽结果（' + list.length + ' 题）',
          { dangerouslyUseHTMLString: true }
        )
      }).catch(() => { this.drawing = false })
    },
    buildPayload() {
      const payload = {
        // 实操不走题库：传空数组让后端清掉历史组卷配置，避免残留
        bankRules: this.isPractice ? [] : this.bankRulePayload(),
        assignMode: this.assignMode,
        participantIds: this.assignMode === 'ASSIGNED' ? this.participantIds : [],
        startTime: this.publishMode === 'TIMED' && this.timeRange && this.timeRange.length === 2 ? this.timeRange[0] : null,
        endTime: this.publishMode === 'TIMED' && this.timeRange && this.timeRange.length === 2 ? this.timeRange[1] : null
      }
      if (this.isPractice) {
        // 实操：题目清单连同每题满分一起提交（后端先清后插，顺序与分值以此为准）
        payload.subjectItems = this.subjectItems.map(s => ({
          id: s.id || null,
          title: s.title,
          description: s.description || null,
          score: Number(s.score) || 0,
          // 参考 / 附件是后端原样返回的 JSON 字符串，回传时不做二次解析
          referenceImages: s.referenceImages || '',
          attachmentsJson: s.attachmentsJson || ''
        }))
      } else {
        // 理论：每题分值与抽题数量一起落库
        payload.singleScore = Number(this.singleScore) || 0
        payload.multiScore = Number(this.multiScore) || 0
        payload.judgeScore = Number(this.judgeScore) || 0
      }
      return payload
    },
    /** 保存基本参数（名称 / 时长 / 通过线）—— 与「编辑考核」弹窗同一接口同一口径；
     *  不传 subjectItems：后端只在显式传时才覆写实操题目，避免误清空。 */
    saveBase() {
      const name = String(this.baseForm.examName || '').trim()
      if (!name) { this.$modal.msgWarning('请填写考核名称'); return }
      const pass = Number(this.baseForm.passLine) || 0
      if (pass < 0) { this.$modal.msgWarning('通过线不能为负数'); return }
      const payload = {
        id: this.exam.id,
        deptId: this.exam.deptId,
        examName: name,
        examMode: this.exam.examMode,
        examType: this.exam.examType,
        passLine: pass,
        duration: Number(this.baseForm.duration) || 0
      }
      if (!this.isPractice) {
        payload.singleScore = this.exam.singleScore
        payload.multiScore = this.exam.multiScore
        payload.judgeScore = this.exam.judgeScore
      }
      this.baseSaving = true
      updateExam(payload).then(() => {
        this.baseSaving = false
        this.$modal.msgSuccess('基本信息已保存')
        this.$emit('refresh')
      }).catch(() => { this.baseSaving = false })
    },
    resetBase() {
      const e = this.exam || {}
      this.baseForm = {
        examName: e.examName || '',
        passLine: Number(e.passLine) || 0,
        duration: Number(e.duration) || 0
      }
      this.durationPreset = this.presetOfDuration(this.baseForm.duration)
    },
    onDurationPresetChange(v) {
      if (v !== -1) {
        this.baseForm.duration = v
      } else if (!this.baseForm.duration) {
        this.baseForm.duration = 60
      }
    },
    presetOfDuration(minutes) {
      const d = Number(minutes) || 0
      return this.durationOptions.indexOf(d) > -1 ? d : -1
    },
    saveConfig() {
      if (!this.checkOk) {
        this.$modal.msgWarning(this.checkMessage || '配置校验未通过')
        return
      }
      if (!this.checkAssignReady()) return
      this.configSaving = true
      saveExamConfig(this.exam.id, this.buildPayload()).then(() => {
        this.configSaving = false
        this.$modal.msgSuccess(this.typeLabel + '：' + (this.isPractice ? '题目满分与发布设置' : '组卷数量、每题分值与发布设置') + '已保存')
        this.$emit('refresh')
      }).catch(() => { this.configSaving = false })
    },
    handlePublish() {
      if (!this.checkOk) {
        this.$modal.msgWarning(this.checkMessage || '配置校验未通过')
        return
      }
      if (this.isPractice && !this.subjectItems.length) {
        this.$modal.msgWarning('发布前请先配置实操题目：至少填写一道题的题干')
        return
      }
      if (this.isPractice && this.subjectTotalScore <= 0) {
        this.$modal.msgWarning('每题满分都是 0，卷面满分为 0。请先在上方「题目清单」里设置每题满分（可批量设分）')
        return
      }
      // 通过线不能超过卷面满分（与后端发布校验一致，前端先拦）
      const passLine = Number(this.exam.passLine)
      const fullScore = this.isPractice ? this.subjectTotalScore : this.ruleTotalScore
      if (passLine > 0 && passLine > fullScore) {
        this.$modal.msgWarning(`通过线（${passLine} 分）不能超过卷面满分（${fullScore} 分），请调整通过线或题目分值`)
        return
      }
      if (!this.checkAssignReady()) return
      const timed = this.publishMode === 'TIMED' && this.timeRange.length === 2
      const tip = timed
        ? `确认发布「${this.exam.examName}」吗？将在 ${this.timeRange[0]} 开放、${this.timeRange[1]} 截止。`
        : `确认发布「${this.exam.examName}」吗？发布后实习生可在对应场次参加。`
      const scope = this.assignMode === 'ASSIGNED'
        ? `本次仅 ${this.participantIds.length} 名指定实习生可见并参加。`
        : '本部门全部在培实习生均可参加。'
      this.$modal.confirm(tip + scope).then(() => {
        this.publishing = true
        publishExam(this.exam.id, this.buildPayload()).then(() => {
          this.publishing = false
          this.$modal.msgSuccess(this.typeLabel + '发布成功')
          this.$emit('refresh')
        }).catch(() => { this.publishing = false })
      }).catch(() => {})
    }
  }
}

/** 保留一位小数（卷面满分 / 小计分展示用） */
function round1(v) {
  const n = Number(v) || 0
  return Math.round(n * 10) / 10
}

/** 数字兜底：null / undefined / 空串 / 脏值一律取默认值 */
function numOf(v, fallback) {
  if (v === null || v === undefined || v === '') return fallback
  const n = Number(v)
  return isNaN(n) ? fallback : n
}

/** 解析 [{name,url}] 形式的 JSON 字段；空值 / 脏数据统一返回空数组 */
function parseJsonList(json) {
  if (!json) return []
  try {
    const arr = typeof json === 'string' ? JSON.parse(json) : json
    return Array.isArray(arr) ? arr : []
  } catch (e) {
    return []
  }
}
</script>

<style lang="scss" scoped>
.dm-card { margin-bottom: 14px; padding: 16px 18px 18px; background: #fff; border: 1px solid #e7ecf3; border-radius: 8px; }
.dm-head { display: flex; align-items: center; justify-content: space-between; gap: 14px; padding-bottom: 13px; margin-bottom: 4px; border-bottom: 1px solid #edf0f4; }
.dm-title { display: flex; align-items: center; gap: 9px; flex-wrap: wrap; }
.dm-title h3 { margin: 0; color: #1d2939; font-size: 16px; font-weight: 600; }
.dm-idx { display: inline-flex; width: 24px; height: 24px; align-items: center; justify-content: center; color: #1764f5; background: #e8f1fd; font-size: 12px; font-weight: 700; border-radius: 4px; }
.dm-idx.green { color: #1a7a58; background: #e4f5ee; }
.dm-hint { color: #98a2b3; font-size: 12px; }
.dm-actions { display: flex; align-items: center; gap: 6px; flex: none; flex-wrap: wrap; justify-content: flex-end; }
.muted { color: #98a2b3; font-size: 12px; }
.danger-text { color: #f56c6c; }
.num { color: #1764f5; font-size: 15px; }

.dm-grid { display: grid; grid-template-columns: minmax(0, 4fr) minmax(0, 8fr); gap: 18px; padding-top: 14px; }
.draw-basis { margin: 0 0 10px; color: #8490a0; font-size: 11.5px; line-height: 1.7; }
.draw-basis b { color: #475467; }
.dm-fields { display: grid; align-content: start; gap: 9px; }
.field { display: flex; align-items: baseline; justify-content: space-between; gap: 10px; padding: 8px 10px; background: #f8fafc; border-radius: 5px; }
.field span { flex: none; color: #8490a0; font-size: 12px; }
.field b { color: #1d2939; font-size: 12.5px; font-weight: 600; text-align: right; }
.field.block { display: block; }

/* ===== 2026-09-21：左栏可就地编辑的基本参数 + 只读项的来源标注 ===== */
.field.edit { display: block; }
.field.edit > span { display: block; margin-bottom: 5px; }
.field.edit .f-input { width: 100%; }
.field.edit > span b { color: #f56c6c; }
.field span .f-note {
  margin-left: 4px; padding: 0 4px; color: #98a2b3; background: #eef1f6;
  border-radius: 3px; font-size: 10px; font-style: normal; font-weight: 400;
}
.field-tip { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-top: 2px; }
.f-hint { color: #8490a0; font-size: 11.5px; }
.f-hint.bad { color: #d9534f; }
.f-hint.lock { color: #b54708; }
.field.block .block-label { display: block; margin-bottom: 6px; }
.dm-table-wrap { min-width: 0; }
.dm-sub-head { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 10px; }
.dm-sub-head h4 { margin: 0; color: #1d2939; font-size: 13px; font-weight: 600; }
.dm-callout { margin-top: 10px; padding: 10px 12px; border-radius: 6px; font-size: 12px; line-height: 1.6; }
.dm-callout.ok { color: #1a7a58; background: #e9f8f1; border: 1px solid #c7ecdc; }
.dm-callout.warn { color: #b54708; background: #fff8ec; border: 1px solid #f7e0bb; }
.dm-empty { display: flex; min-height: 120px; align-items: center; justify-content: center; flex-direction: column; gap: 8px; color: #98a2b3; border: 1px dashed #dfe5ee; border-radius: 8px; }

/* 分值设置条：理论「每题分值」/ 实操「全部设为」共用 */
.score-bar {
  display: flex; align-items: center; flex-wrap: wrap; gap: 14px;
  padding: 9px 12px; margin-bottom: 10px;
  background: #f7f9fc; border: 1px solid #eef1f6; border-radius: 6px;
}
.sb-title { color: #344054; font-size: 12.5px; font-weight: 600; }
.sb-item { display: inline-flex; align-items: center; gap: 6px; color: #475467; font-size: 12.5px; }
.sb-total { margin-left: auto; color: #667085; font-size: 12.5px; }
.sb-total b { color: #1764f5; font-size: 15px; font-weight: 700; }
.sb-sub { color: #98a2b3; font-size: 11.5px; }
.dm-empty i { color: #b7c1cc; font-size: 28px; }
.dm-empty span { font-size: 12.5px; }

/* 实操题目清单：题序号 tag + 详情 */
.sj-tags { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; }
.sj-tag {
  display: inline-flex; align-items: center; gap: 6px; height: 28px; padding: 0 12px;
  color: #344054; background: #f7f9fc; border: 1px solid #eef1f6; border-radius: 7px;
  font-size: 12.5px; cursor: pointer; user-select: none; transition: all .15s;
}
.sj-tag:hover { border-color: #1764f5; color: #1764f5; }
.sj-tag.on { color: #fff; background: #1764f5; border-color: #1764f5; font-weight: 600; }
.sj-tag em { font-style: normal; font-size: 11px; color: #98a2b3; }
.sj-tag.on em { color: rgba(255, 255, 255, .85); }
.sj-detail { padding: 12px 14px; background: #f8fafc; border: 1px solid #eef1f5; border-radius: 7px; }
.sj-line { display: flex; align-items: flex-start; gap: 10px; margin-bottom: 9px; }
.sj-line:last-child { margin-bottom: 0; }
.sj-key { flex: none; width: 60px; padding-top: 2px; color: #8490a0; font-size: 12px; }
.sj-val { flex: 1; min-width: 0; color: #1d2939; font-size: 12.5px; line-height: 1.7; }
.sj-val.strong { font-weight: 600; }
.sj-val.pre { white-space: pre-wrap; }
.sj-img { width: 92px; height: 66px; margin: 0 8px 6px 0; border: 1px solid #eef1f6; border-radius: 6px; cursor: zoom-in; }
.sj-video { width: 160px; height: 90px; margin: 0 8px 6px 0; border: 1px solid #eef1f6; border-radius: 6px; background: #000; object-fit: contain; }
.sj-ph { display: inline-flex; margin: 0 8px 6px 0; border: 1px solid #eef1f6; border-radius: 6px; overflow: hidden; }
.sj-file-row { display: flex; align-items: center; gap: 10px; margin: 4px 0; }
.sj-file { display: inline-flex; align-items: center; gap: 5px; flex: 1; min-width: 0; color: #1764f5; font-size: 12.5px; text-decoration: none; word-break: break-all; }
.sj-file:hover { text-decoration: underline; }
.sj-file i { font-size: 13px; color: #98a2b3; flex: none; }
.sj-download {
  display: inline-flex; align-items: center; gap: 4px; flex: none; padding: 4px 11px;
  color: #1764f5; background: #fff; border: 1px solid #c9dbf7; border-radius: 4px;
  font-size: 12px; text-decoration: none; transition: all .15s;
}
.sj-download:hover { background: #e8f1fd; border-color: #1764f5; }
.sj-download i { font-size: 13px; }

.stage-publish { margin-top: 16px; padding-top: 14px; border-top: 1px dashed #e7ecf3; }

/* 发布设置 · 指定人员：复选框花名册 */
.assign-bar { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 10px; }
.assign-bar .ap-save { margin-left: auto; }
.ap-scope { color: #98a2b3; font-size: 11.5px; }
.assign-pick { padding: 10px 12px; background: #f8fafc; border: 1px solid #eef1f5; border-radius: 7px; }
.ap-head { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 9px; }
.ap-count { margin-left: auto; color: #8490a0; font-size: 11.5px; }
.ap-count b { color: #1764f5; font-size: 13px; }
.ap-list { max-height: 208px; overflow-y: auto; }
.ap-group { display: flex; flex-wrap: wrap; gap: 6px; }
.ap-item {
  display: flex; align-items: center; width: calc(50% - 3px); margin-right: 0; padding: 5px 9px;
  background: #fff; border: 1px solid #e7ecf3; border-radius: 5px;
}
.ap-item ::v-deep .el-checkbox__label { display: inline-flex; align-items: baseline; gap: 6px; min-width: 0; font-size: 12.5px; line-height: 1.5; }
.ap-name { color: #1d2939; font-weight: 600; }
.ap-dept { color: #1764f5; font-size: 11.5px; }
.ap-meta { color: #98a2b3; font-size: 11.5px; }
.ap-empty { display: flex; align-items: center; justify-content: center; gap: 6px; padding: 22px 0; color: #98a2b3; font-size: 12.5px; }
.ap-empty i { font-size: 18px; }
.stage-publish .dm-grid { padding-top: 12px; }

@media (max-width: 1100px) {
  .dm-grid { grid-template-columns: 1fr; }
}

/* ===== 2026-09-21：独立配置页适配（本组件只被考核列表/配置页使用） =====
   概览列收窄、组卷区变宽；窄屏自动堆叠，避免小窗口里两栏互相挤压。 */
.dm-grid { grid-template-columns: minmax(0, 3fr) minmax(0, 9fr); gap: 22px; }
@media (max-width: 1280px) {
  .dm-grid { grid-template-columns: minmax(0, 1fr); }
  .dm-fields { grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); }
}
@media (max-width: 700px) {
  .dm-fields { grid-template-columns: minmax(0, 1fr); }
}
</style>
