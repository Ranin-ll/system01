<template>
  <div class="grading-page app-container">
    <header class="page-heading">
      <div>
        <div class="eyebrow">学习考核 / 考核批改</div>
        <h2>答卷批改</h2>
        <p>{{ isPractice ? '下载实习生的作答文件并评分，系统自动求和为整场成绩。可暂存后退出查看文件，再回来继续。' : '理论考核客观题已自动判分，此处仅展示成绩。' }}</p>
      </div>
      <div class="heading-actions">
        <el-button icon="el-icon-back" size="small" @click="goBack">返回考核列表</el-button>
        <el-button v-if="isPractice" type="primary" size="small" icon="el-icon-finished" @click="handlePublishResult">发布成绩</el-button>
      </div>
    </header>

    <!-- 实操：题目概览 -->
    <div v-if="isPractice" class="subject-brief">
      <div class="brief-head">
        <span class="brief-title">{{ examName || '实操考核' }}</span>
        <span class="brief-meta">共 {{ subjectItems.length }} 道题 · 卷面满分 {{ totalFullScore }} 分 · 通过线 {{ passLine }} 分</span>
      </div>
      <div class="brief-tags">
        <span v-for="(s, i) in subjectItems" :key="s.id || i" class="brief-tag">
          第 {{ i + 1 }} 题 · {{ s.score || 0 }} 分<em>{{ s.title }}</em>
        </span>
      </div>
    </div>

    <el-table v-loading="loading" :data="gradingList" stripe empty-text="暂无数据">
      <el-table-column label="实习生" min-width="140">
        <template slot-scope="scope">
          <div class="user-cell"><span class="avatar">{{ (scope.row.userName || '?').charAt(0) }}</span><span>{{ scope.row.userName || '-' }}</span></div>
        </template>
      </el-table-column>
      <el-table-column label="部门" width="130" align="center">
        <template slot-scope="scope">{{ scope.row.deptName || '-' }}</template>
      </el-table-column>
      <el-table-column label="作答状态" width="110" align="center">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.sheetId" size="mini" :type="sheetStatusTag(scope.row.sheetStatus)">{{ sheetStatusLabel(scope.row.sheetStatus) }}</el-tag>
          <el-tag v-else size="mini" type="info" effect="plain">尚未作答</el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="isPractice" label="作答文件" width="126" align="center">
        <template slot-scope="scope">
          <span v-if="scope.row.sheetId" class="file-count" :class="{ full: isFullUpload(scope.row) }">
            {{ scope.row.answeredFiles || 0 }} / {{ scope.row.itemCount || subjectItems.length }} 份
          </span>
          <span v-else class="gray-text">-</span>
        </template>
      </el-table-column>
      <el-table-column v-if="isPractice" label="批阅进度" width="118" align="center">
        <template slot-scope="scope">
          <span v-if="scope.row.sheetId" class="grade-progress" :class="{ done: isAllGraded(scope.row) }">
            {{ scope.row.gradedCount || 0 }} / {{ scope.row.itemCount || subjectItems.length }} 题
          </span>
          <span v-else class="gray-text">-</span>
        </template>
      </el-table-column>
      <el-table-column label="得分" width="126" align="center">
        <template slot-scope="scope">
          <strong class="num">{{ scoreText(scope.row) }}</strong>
          <span v-if="scope.row.sheetId && scope.row.totalFullScore" class="of-total">/ {{ round1(scope.row.totalFullScore) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="176" align="center">
        <template slot-scope="scope">
          <el-button
            v-if="scope.row.sheetId"
            type="text"
            size="mini"
            :icon="isPractice && scope.row.sheetStatus !== 'PUBLISHED' ? 'el-icon-edit' : 'el-icon-view'"
            class="primary-text"
            @click="openGrade(scope.row)"
          >{{ actionLabel(scope.row) }}</el-button>
          <el-button
            v-if="isPractice && scope.row.sheetStatus === 'PUBLISHED'"
            type="text"
            size="mini"
            icon="el-icon-refresh-left"
            class="warn-text"
            @click="handleReopen(scope.row)"
          >重新批改</el-button>
          <span v-if="!scope.row.sheetId" class="gray-text">尚未作答</span>
        </template>
      </el-table-column>
    </el-table>

    <!-- 答卷弹窗：实操=批阅打分；理论=查看答题详情与得分 -->
    <el-dialog :title="isPractice ? '实操答卷批阅' : '理论答卷详情'" :visible.sync="gradeVisible" width="1200px" top="6vh" append-to-body>
      <div v-if="currentSheet" class="sheet-info">
        <span>实习生：<b>{{ currentSheet.userName }}</b></span>
        <span>部门：<b>{{ currentSheet.deptName || '-' }}</b></span>
        <span>本卷满分：<b>{{ sheetFullScore === null ? '—' : sheetFullScore + ' 分' }}</b></span>
        <span v-if="!isPractice">自动判分：<b>{{ theoryScoreText }}</b></span>
        <span v-if="!isPractice" class="sheet-state">
          <el-tag size="mini" :type="currentSheet.sheetStatus === 'PUBLISHED' ? 'success' : 'warning'" effect="plain">
            {{ sheetStatusLabel(currentSheet.sheetStatus) }}
          </el-tag>
        </span>
      </div>

      <div v-loading="detailLoading" class="grade-body">
        <div v-if="!gradeItems.length && !detailLoading" class="grade-empty">
          {{ isPractice ? '该答卷暂无作答明细（白卷），保存后将按 0 分计。' : '该答卷暂无逐题明细。' }}
        </div>

        <!-- ===== 理论：只读答题详情 ===== -->
        <table v-else-if="!isPractice" class="th-table">
          <thead>
            <tr>
              <th style="width:52px">题号</th>
              <th style="width:64px">题型</th>
              <th>题干 / 选项</th>
              <th style="width:150px">实习生作答</th>
              <th style="width:120px">正确答案</th>
              <th style="width:86px">得分</th>
              <th style="width:70px">结果</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(it, index) in gradeItems" :key="it.id">
              <td class="ctr">{{ it.seq || index + 1 }}</td>
              <td class="ctr">{{ typeLabel(it.qtype) }}</td>
              <td>
                <div class="th-stem">{{ it.stem || '（题目已被修改，无题干快照）' }}</div>
                <ul v-if="parseOptions(it).length" class="th-opts">
                  <li
                    v-for="o in parseOptions(it)"
                    :key="o.key"
                    :class="{ right: isRightOption(it, o.key), chosen: isChosen(it, o.key), wrong: isChosen(it, o.key) && !isRightOption(it, o.key) }"
                  >
                    <b>{{ o.key }}</b><span>{{ o.content }}</span>
                    <em v-if="isChosen(it, o.key)">实习生所选</em>
                    <em v-if="isRightOption(it, o.key)" class="ok">正确项</em>
                  </li>
                </ul>
              </td>
              <td class="ctr">
                <b :class="it.isCorrect === 1 ? 'ans-ok' : 'ans-bad'">{{ it.userAnswer || '未作答' }}</b>
              </td>
              <td class="ctr">{{ it.answer || '—' }}</td>
              <td class="ctr">
                <b>{{ itemScoreText(it) }}</b> / {{ it.score == null ? '—' : it.score }}
              </td>
              <td class="ctr">
                <el-tag size="mini" :type="it.isCorrect === 1 ? 'success' : 'danger'" effect="plain">
                  {{ it.isCorrect === 1 ? '正确' : '错误' }}
                </el-tag>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- ===== 实操：逐题批阅打分 ===== -->
        <el-tabs v-else v-model="gradeActiveTab" class="grade-tabs">
          <el-tab-pane
            v-for="(it, index) in gradeItems"
            :key="it.id"
            :label="'第 ' + (index + 1) + ' 题'"
            :name="String(index)"
          >
            <div class="grade-item">
              <!-- 左卡片：题干 + 附件 + 作答文件 + 打分 -->
              <div class="gi-left">
                <div class="gi-head">
                  <span class="gi-seq">{{ index + 1 }}</span>
                  <div class="gi-title">{{ it.stem || '（题目已被修改，无题干快照）' }}</div>
                  <span class="gi-full">{{ fullText(it) }}</span>
                </div>

                <div v-if="it.subjectDescription" class="gi-desc">{{ it.subjectDescription }}</div>

                <div v-if="parseJson(it.subjectAttachments).length" class="gi-attach">
                  <span class="gi-label">参考附件：</span>
                  <div class="gi-file-list">
                    <div v-for="(a, i) in parseJson(it.subjectAttachments)" :key="i" class="gi-file-item">
                      <a :href="baseApi + a.url" target="_blank" class="gi-file" :title="a.name"><i class="el-icon-paperclip" /> {{ a.name }}</a>
                      <a :href="baseApi + a.url" download class="gi-download"><i class="el-icon-download" /> 下载</a>
                    </div>
                  </div>
                </div>

                <div class="gi-answer">
                  <span class="gi-label">作答文件：</span>
                  <span v-if="it.userAnswer" class="gi-file-row">
                    <el-button size="mini" plain icon="el-icon-view" @click="openPreview({ url: it.userAnswer })">预览</el-button>
                    <a :href="baseApi + it.userAnswer" target="_blank" class="link" download><i class="el-icon-download" /> 下载</a>
                  </span>
                  <span v-else class="gray-text">该题未上传作答文件</span>
                </div>

                <div class="gi-grade">
                  <span class="gi-label">打分（本题满分 <b>{{ it.score == null ? '未记录' : it.score }}</b> 分）：</span>
                  <el-input-number v-model="it.manualScore" :min="0" :max="maxScoreOf(it)" :precision="1" size="small" :disabled="isPublishedSheet" />
                  <el-input v-model="it.manualComment" size="small" class="gi-comment" placeholder="本题评语（可选）" :disabled="isPublishedSheet" />
                </div>
              </div>

              <!-- 右卡片：参考图 -->
              <div class="gi-right">
                <div class="gi-right-label">参考图</div>
                <div v-if="parseJson(it.subjectImages).length" class="gi-images">
                  <template v-for="(img, i) in parseJson(it.subjectImages)">
                    <video v-if="isVideo(img.url)" :key="'v' + i" :src="baseApi + img.url" controls class="gi-image gi-video" />
                    <el-image
                      v-else
                      :key="'i' + i"
                      :src="baseApi + img.url"
                      :preview-src-list="parseJson(it.subjectImages).filter(x => !isVideo(x.url)).map(x => baseApi + x.url)"
                      fit="cover"
                      class="gi-image"
                    />
                  </template>
                </div>
                <div v-else class="gi-no-image">暂无参考图</div>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>

      <div v-if="isPractice" class="grade-sum">
        <span>合计得分：</span>
        <b>{{ round1(totalManualScore) }}</b>
        <span class="of">/ {{ sheetFullScore === null ? '—' : sheetFullScore }} 分</span>
        <span class="passline">通过线 {{ passLine }} 分</span>
      </div>
      <div v-else class="grade-sum">
        <span>自动判分：</span>
        <b>{{ theoryScoreText }}</b>
        <span class="of">/ {{ sheetFullScore === null ? '—' : sheetFullScore }} 分</span>
        <span class="passline">通过线 {{ passLine }} 分 · {{ theoryPassText }}</span>
      </div>

      <span slot="footer">
        <template v-if="isPractice && !isPublishedSheet">
          <el-button @click="gradeVisible = false">取消</el-button>
          <el-button type="info" :loading="saving" @click="saveGrade(true)">暂存并退出</el-button>
          <el-button type="primary" :loading="saving" @click="saveGrade(false)">保存批改</el-button>
        </template>
        <!-- 理论（自动判分）与实操已发布成绩都只读：不给改分入口，避免误操作 -->
        <el-button v-else type="primary" @click="gradeVisible = false">关闭</el-button>
      </span>
    </el-dialog>

    <!-- 发布成绩弹窗 -->
    <el-dialog title="发布成绩" :visible.sync="publishDialogVisible" width="440px" append-to-body>
      <el-form label-width="180px">
        <el-form-item label="发布后停用考核">
          <el-switch v-model="publishDisableExam" active-text="是" inactive-text="否" />
        </el-form-item>
        <el-form-item label="未作答发布为0分">
          <el-switch v-model="publishUnanswered" active-text="是" inactive-text="否" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="publishDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="doPublishResult">确认发布</el-button>
      </span>
    </el-dialog>

    <!-- 附件预览弹窗：单击「预览」页内查看，下载需另点「下载」链接 -->
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
          v-else-if="previewKind === 'pdf'"
          :src="baseApi + previewFile.url"
          class="file-preview-frame"
        />
        <div v-else class="file-preview-tip">
          <i class="el-icon-document" />
          <p>该类型暂不支持在线预览</p>
          <a :href="baseApi + previewFile.url" target="_blank" download><el-button size="mini" type="primary" icon="el-icon-download">下载附件</el-button></a>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { gradingList, gradeSheet, publishResult, reopenSheet, getExam, sheetDetail } from '@/api/business/exam'
import { mapGetters } from 'vuex'

/** 解析后端 [{name,url}] 形式的 JSON 字段（实操题目的参考 / 附件） */
function parseJsonList(json) {
  if (!json) return []
  try {
    const arr = typeof json === 'string' ? JSON.parse(json) : json
    return Array.isArray(arr) ? arr : []
  } catch (e) {
    return []
  }
}

export default {
  name: 'ExamGrading',
  data() {
    return {
      examId: null,
      examName: '',
      examType: 'THEORY',
      passLine: 60,
      subjectItems: [],
      loading: false,
      gradingList: [],
      gradeVisible: false,
      detailLoading: false,
      currentSheet: null,
      gradeItems: [],
      saving: false,
      publishDialogVisible: false,
      publishDisableExam: true,
      publishUnanswered: false,
      previewVisible: false,
      previewFile: null,
      gradeActiveTab: '0'
    }
  },
  computed: {
    ...mapGetters(['roles']),
    baseApi() { return process.env.VUE_APP_BASE_API || '' },
    isPractice() { return this.examType === 'PRACTICAL' },
    /** 当前弹窗里的答卷是否已发布成绩（已发布只能看，不能再改分） */
    isPublishedSheet() {
      return !!(this.currentSheet && this.currentSheet.sheetStatus === 'PUBLISHED')
    },
    /** 卷面满分 = 各题满分之和 */
    totalFullScore() {
      const sum = this.subjectItems.reduce((acc, s) => acc + (Number(s.score) || 0), 0)
      return Math.round(sum * 100) / 100
    },
    totalManualScore() {
      return this.gradeItems.reduce((acc, it) => acc + (Number(it.manualScore) || 0), 0)
    },
    /**
     * 本卷满分 = 该答卷逐题满分之和。
     * 不能用 totalFullScore（那是考核当前的题目配置）——题目被管理员改版后，
     * 卷面配置与实习生当时考的那份卷会不一致，批阅必须以答卷自身的快照为准。
     */
    sheetFullScore() {
      if (!this.gradeItems.length) return null
      const sum = this.gradeItems.reduce((acc, it) => acc + (Number(it.score) || 0), 0)
      return sum ? Math.round(sum * 100) / 100 : null
    },
    previewKind() {
      if (!this.previewFile) return 'other'
      const url = String(this.previewFile.url || '').toLowerCase()
      if (/\.(png|jpe?g|gif|webp|bmp|svg)$/.test(url)) return 'image'
      if (/\.(mp4|webm|ogg|ogv|mov|avi|m4v)$/.test(url)) return 'video'
      if (/\.pdf$/.test(url)) return 'pdf'
      return 'other'
    },
    /** 理论：自动判分（各题 ai_score 之和） */
    theoryScore() {
      const sum = this.gradeItems.reduce((acc, it) => acc + (Number(it.aiScore) || 0), 0)
      return Math.round(sum * 100) / 100
    },
    theoryScoreText() {
      if (!this.gradeItems.length) return '—'
      return this.round1(this.theoryScore) + ' 分'
    },
    theoryPassText() {
      if (!this.gradeItems.length) return '结果待定'
      return this.theoryScore >= Number(this.passLine) ? '已达通过线' : '未达通过线'
    }
  },
  created() {
    this.examId = this.$route.params.examId
    this.loadExam()
    this.loadList()
  },
  methods: {
    loadExam() {
      getExam(this.examId).then(res => {
        const d = res.data || {}
        this.examType = d.examType || 'THEORY'
        this.examName = d.examName || ''
        this.passLine = d.passLine == null ? 60 : d.passLine
        this.subjectItems = d.subjectItems || []
      })
    },
    loadList() {
      this.loading = true
      gradingList(this.examId).then(res => {
        this.gradingList = res.data || []
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    openGrade(row) {
      this.currentSheet = row
      this.gradeItems = []
      this.gradeActiveTab = '0'
      this.gradeVisible = true
      this.detailLoading = true
      sheetDetail(row.sheetId).then(res => {
        const items = (res.data && res.data.items) || []
        this.gradeItems = items.map(it => ({
          id: it.id,
          seq: it.seq,
          qtype: it.qtype,
          isSubjective: it.isSubjective,
          stem: it.stem,
          score: it.score,
          /** 理论：正确答案与选项，用于只读回看 */
          answer: it.answer,
          optionsJson: it.optionsJson,
          userAnswer: it.userAnswer,
          isCorrect: it.isCorrect,
          aiScore: it.aiScore,
          subjectDescription: it.subjectDescription,
          subjectImages: it.subjectImages,
          subjectAttachments: it.subjectAttachments,
          manualScore: it.manualScore == null ? 0 : Number(it.manualScore),
          manualComment: it.manualComment || ''
        }))
        this.detailLoading = false
      }).catch(() => { this.detailLoading = false })
    },
    /** 操作列文案：实操未发布=批阅，已发布=查看；理论=查看答卷 */
    actionLabel(row) {
      if (!this.isPractice) return '查看答卷'
      return row && row.sheetStatus === 'PUBLISHED' ? '查看' : '批阅'
    },
    /** 实操：该答卷是否已逐题批完 */
    isAllGraded(row) {
      const need = Number(row.itemCount) || 0
      return need > 0 && Number(row.gradedCount || 0) >= need
    },
    /** 选项 JSON → [{ key, content }] */
    parseOptions(it) {
      try {
        const arr = JSON.parse((it && it.optionsJson) || '[]')
        return Array.isArray(arr) ? arr : []
      } catch (e) {
        return []
      }
    },
    /** 作答串 → 选项键数组，如 "A,C" → ['A','C'] */
    answerKeys(val) {
      return String(val || '').toUpperCase().split(',').map(s => s.trim()).filter(s => !!s)
    },
    isChosen(it, key) {
      return this.answerKeys(it && it.userAnswer).indexOf(String(key).toUpperCase()) > -1
    },
    isRightOption(it, key) {
      return this.answerKeys(it && it.answer).indexOf(String(key).toUpperCase()) > -1
    },
    /** 理论逐题得分：客观题取自动判分 */
    itemScoreText(it) {
      const v = it.aiScore != null ? it.aiScore : (it.manualScore != null ? it.manualScore : null)
      return v == null ? '—' : this.round1(v)
    },
    /** 题型名 */
    typeLabel(qtype) {
      return { SINGLE: '单选', MULTI: '多选', JUDGE: '判断', SUBJECT: '主观' }[qtype] || qtype || '—'
    },
    saveGrade(isDraft) {
      // 理论考核自动判分，弹窗是只读回看，不提供人工改分入口
      if (!this.isPractice) {
        this.gradeVisible = false
        return
      }
      // 已发布的成绩不再允许改分（后端同样会拒绝）
      if (this.currentSheet && this.currentSheet.sheetStatus === 'PUBLISHED') {
        this.$modal.msgWarning('成绩已发布，不能再批改')
        return
      }
      // 只在满分有记录时校验。题干 / 满分可能因题目被改版重建而缺失，
      // 这时提示里用「第 N 题」兜底，避免出现「null 的分数超过了本题满分 null 分」。
      const overIndex = this.gradeItems.findIndex(it => it.score != null && (Number(it.manualScore) || 0) > Number(it.score))
      if (overIndex > -1) {
        const over = this.gradeItems[overIndex]
        const label = over.stem ? '「' + over.stem + '」' : '第 ' + (overIndex + 1) + ' 题'
        this.$modal.msgWarning(label + '的打分超过了本题满分 ' + over.score + ' 分')
        return
      }
      this.saving = true
      const items = this.gradeItems.map(it => ({
        id: it.id,
        manualScore: Number(it.manualScore) || 0,
        manualComment: it.manualComment || ''
      }))
      gradeSheet({ sheetId: this.currentSheet.sheetId, items }).then(() => {
        this.$modal.msgSuccess(isDraft ? '已暂存，可继续查看文件后回来批改' : '批改已保存，总分为各题得分之和')
        this.gradeVisible = false
        this.saving = false
        this.loadList()
      }).catch(() => { this.saving = false })
    },
    handlePublishResult() {
      if (!this.isPractice) {
        this.$modal.msgWarning('理论考核交卷即自动判分并出成绩，无需发布')
        return
      }
      this.publishDisableExam = true
      this.publishUnanswered = false
      this.publishDialogVisible = true
    },
    doPublishResult() {
      this.$modal.confirm('确认按所选选项发布成绩吗？发布后实习生可查看结果。').then(() => {
        publishResult(this.examId, { disableExam: this.publishDisableExam, publishUnanswered: this.publishUnanswered }).then(() => {
          this.$modal.msgSuccess('成绩已发布')
          this.publishDialogVisible = false
          this.$router.push('/assessment/department/exam')
        })
      }).catch(() => {})
    },
    /** 重新开放批改：已发布成绩回退到批改中 */
    handleReopen(row) {
      this.$modal.confirm('该答卷成绩已发布，重新批改将清空总分并回到批改状态，实习生端暂不可见。确认继续吗？').then(() => {
        reopenSheet(row.sheetId).then(() => {
          this.$modal.msgSuccess('已重新开放批改')
          this.loadList()
        })
      }).catch(() => {})
    },
    goBack() { this.$router.push('/assessment/department/exam') },
    /** 本题满分文案：题目被改版重建后快照与题目表都可能取不到，此时明示「满分未记录」而不是显示 0 分 */
    fullText(it) {
      return it.score == null ? '满分未记录' : it.score + ' 分'
    },
    /**
     * 打分上限：有满分就用满分（硬限制，从输入起就无法超过该题满分）；
     * 满分缺失时返回 0 —— 宁可暂时锁死不能打正分，也绝不放开上限让管理员误打超分。
     * （满分缺失是历史题目被删重建导致的数据异常，应先在库里补回 full_score；
     *   这里用 0 兜底，避免再出现「不设限、能输入大于满分」的问题。）
     */
    maxScoreOf(it) {
      return it.score == null ? 0 : Number(it.score)
    },
    /** 打开附件预览弹窗（页内预览，不直接下载） */
    openPreview(file) {
      this.previewFile = file || null
      this.previewVisible = !!file
    },
    parseJson(json) { return parseJsonList(json) },
    /** 按扩展名判断是否为视频（参考/附件为视频时用 <video> 渲染） */
    isVideo(url) { return /\.(mp4|webm|ogg|ogv|mov|avi|m4v)$/i.test(String(url || '')) },
    /** 作答文件是否交齐（用于标记） */
    isFullUpload(row) {
      const need = Number(row.itemCount) || this.subjectItems.length
      return need > 0 && Number(row.answeredFiles || 0) >= need
    },
    scoreText(row) {
      const v = row.finalScore != null ? row.finalScore : (row.manualScore != null ? row.manualScore : null)
      return v == null ? '-' : this.round1(v)
    },
    round1(v) {
      const n = Number(v) || 0
      return Math.round(n * 10) / 10
    },
    sheetStatusLabel(s) { return { IN_PROGRESS: '作答中', SUBMITTED: '已交卷', SCORING: '批改中', PUBLISHED: '已发布' }[s] || s },
    sheetStatusTag(s) { return { IN_PROGRESS: 'info', SUBMITTED: 'warning', SCORING: 'primary', PUBLISHED: 'success' }[s] || 'info' }
  }
}
</script>

<style lang="scss" scoped>
.page-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 18px; }
.eyebrow { color: #1764f5; font-size: 12px; letter-spacing: .05em; }
.page-heading h2 { margin: 6px 0 8px; font-size: 22px; font-weight: 600; color: #1d2939; }
.heading-actions { flex: none; white-space: nowrap; }

.subject-brief { padding: 14px 16px; margin-bottom: 16px; background: #f8fafc; border: 1px solid #e7ecf3; border-radius: 6px; }
.brief-head { display: flex; align-items: baseline; gap: 14px; margin-bottom: 10px; }
.brief-title { color: #1d2939; font-size: 14px; font-weight: 600; }
.brief-meta { color: #667085; font-size: 12.5px; }
.brief-tags { display: flex; flex-wrap: wrap; gap: 8px; }
.brief-tag {
  display: inline-flex; align-items: center; gap: 7px; max-width: 320px; height: 26px; padding: 0 11px;
  color: #344054; background: #fff; border: 1px solid #eef1f6; border-radius: 13px; font-size: 12px;
}
.brief-tag em { overflow: hidden; color: #98a2b3; font-style: normal; text-overflow: ellipsis; white-space: nowrap; }

.user-cell { display: flex; align-items: center; gap: 8px; }
.avatar { display: inline-flex; width: 26px; height: 26px; align-items: center; justify-content: center; color: #fff; background: #1764f5; border-radius: 50%; font-size: 13px; }
.num { color: #1764f5; }
.primary-text { color: #1764f5; }
.warn-text { color: #e8590c; }
.gray-text { color: #98a2b3; font-size: 12px; }
.link { color: #1764f5; text-decoration: none; }
.link i { margin-right: 3px; }
.file-count { color: #b54708; font-size: 12.5px; }
.file-count.full { color: #1a7a58; }
.grade-progress { color: #b54708; font-size: 12.5px; }
.grade-progress.done { color: #1a7a58; }
.of-total { margin-left: 3px; color: #98a2b3; font-size: 12px; }
.sheet-state { margin-left: auto; }

/* 理论答卷详情表 */
.th-table { width: 100%; border-collapse: collapse; }
.th-table th { padding: 9px 10px; color: #8490a0; background: #f7f9fc; font-size: 12px; font-weight: 500; text-align: left; }
.th-table td { padding: 10px; color: #475467; border-bottom: 1px solid #edf0f4; font-size: 12.5px; vertical-align: top; }
.th-table td.ctr { text-align: center; }
.th-stem { color: #1d2939; font-weight: 600; line-height: 1.65; }
.th-opts { margin: 7px 0 0; padding: 0; list-style: none; }
.th-opts li { display: flex; align-items: baseline; gap: 7px; padding: 2px 0; color: #667085; }
.th-opts li b { flex: none; color: #98a2b3; }
.th-opts li.chosen b { color: #1764f5; }
.th-opts li.wrong span { color: #d92d20; }
.th-opts li.right span { color: #1a7a58; }
.th-opts li em { flex: none; padding: 0 5px; color: #1764f5; font-size: 11px; font-style: normal; }
.th-opts li em.ok { color: #1a7a58; }
.ans-ok { color: #1a7a58; }
.ans-bad { color: #d92d20; }

.sheet-info { display: flex; flex-wrap: wrap; gap: 24px; margin-bottom: 12px; color: #475467; font-size: 13px; }
.grade-body { max-height: 60vh; overflow-y: auto; padding-right: 4px; }
.grade-empty { padding: 26px 14px; color: #98a2b3; font-size: 13px; text-align: center; }
.grade-tabs ::v-deep .el-tabs__header { margin-bottom: 12px; }
.grade-tabs ::v-deep .el-tabs__item { font-size: 13px; }
.grade-item { display: flex; align-items: stretch; gap: 16px; padding: 16px; background: #f8fafc; border: 1px solid #eef1f5; border-radius: 10px; }
.gi-left { flex: 1 1 auto; min-width: 0; display: flex; flex-direction: column; }
.gi-right { flex: none; width: 300px; display: flex; flex-direction: column; padding: 12px 14px; background: #fff; border: 1px solid #eef1f6; border-radius: 8px; }
.gi-right-label { margin-bottom: 9px; color: #8490a0; font-size: 12px; font-weight: 600; letter-spacing: .5px; }
.gi-no-image { display: flex; align-items: center; justify-content: center; min-height: 108px; color: #b0b8c4; font-size: 12.5px; border: 1px dashed #e4e9f0; border-radius: 6px; }
.gi-head { display: flex; align-items: center; gap: 9px; margin-bottom: 8px; }
.gi-seq { flex: none; display: inline-flex; width: 22px; height: 22px; align-items: center; justify-content: center; color: #fff; background: #1764f5; font-size: 12px; font-weight: 700; border-radius: 5px; }
.gi-title { flex: 1; min-width: 0; color: #1d2939; font-size: 13.5px; font-weight: 600; line-height: 1.6; }
.gi-full { flex: none; color: #1764f5; font-size: 12px; font-weight: 600; }
.gi-desc { margin-bottom: 8px; color: #475467; font-size: 12.5px; line-height: 1.75; white-space: pre-wrap; }
.gi-images { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; }
.gi-image { width: 100%; height: 108px; object-fit: cover; border: 1px solid #eef1f6; border-radius: 6px; cursor: zoom-in; background: #f2f4f7; }
.gi-video { object-fit: cover; background: #000; cursor: default; }
.gi-attach { display: flex; align-items: flex-start; margin-bottom: 7px; font-size: 12.5px; }
.gi-file-list { flex: 1; min-width: 0; }
.gi-file-item { display: flex; align-items: center; gap: 10px; margin: 3px 0; }
.gi-file { display: inline-flex; align-items: center; gap: 5px; flex: 1; min-width: 0; color: #1764f5; font-size: 12.5px; text-decoration: none; word-break: break-all; }
.gi-file:hover { text-decoration: underline; }
.gi-file i { font-size: 13px; color: #98a2b3; flex: none; }
.gi-download { display: inline-flex; align-items: center; gap: 4px; flex: none; padding: 3px 10px; color: #1764f5; background: #fff; border: 1px solid #c9dbf7; border-radius: 4px; font-size: 12px; text-decoration: none; transition: all .15s; }
.gi-download:hover { background: #e8f1fd; border-color: #1764f5; }
.gi-download i { font-size: 13px; }
.gi-file-row { display: inline-flex; align-items: center; gap: 6px; margin-right: 16px; }
.gi-file-row .el-button { margin-left: 0; }
.gi-file-row .link { margin-right: 0; }

/* 附件预览弹窗 */
.file-preview { display: flex; align-items: center; justify-content: center; min-height: 160px; }
.file-preview-frame { width: 100%; height: 70vh; border: none; }
.file-preview-video { width: 100%; max-height: 70vh; background: #000; }
.file-preview-tip { text-align: center; color: #98a2b3; padding: 24px 0; }
.file-preview-tip i { font-size: 48px; color: #c3cdd9; display: block; margin-bottom: 12px; }
.file-preview-tip p { margin: 0 0 16px; font-size: 13px; }
.gi-label { margin-right: 6px; color: #8490a0; font-size: 12.5px; }
.gi-answer { display: flex; align-items: center; margin-bottom: 9px; font-size: 12.5px; }
.gi-grade { display: flex; align-items: center; gap: 10px; margin-top: auto; padding-top: 9px; border-top: 1px dashed #e4e9f0; }
.gi-comment { flex: 1; min-width: 0; }

.grade-sum { display: flex; align-items: baseline; gap: 6px; padding: 12px 15px; margin-top: 4px; color: #475467; background: #f2f7ff; border: 1px solid #dbe7fb; border-radius: 7px; font-size: 13px; }
.grade-sum b { color: #1764f5; font-size: 20px; }
.grade-sum .of { color: #8490a0; }
.grade-sum .passline { margin-left: auto; color: #98a2b3; font-size: 12.5px; }

@media (max-width: 1180px) {
  .page-heading { flex-direction: column; }
}
@media (max-width: 900px) {
  .grade-item { flex-direction: column; }
  .gi-right { width: 100%; }
}
</style>
