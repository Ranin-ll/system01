<template>
  <div class="grading-page app-container">
    <header class="page-heading">
      <div>
        <div class="eyebrow">学习考核 / 考核批改</div>
        <h2>答卷批改</h2>
        <p>{{ examType === 'PRACTICAL' ? '下载实习生的作答文件，人工打分。可暂存后退出查看文件，再回来继续。' : '理论考核客观题已自动判分，此处仅展示成绩。' }}</p>
      </div>
      <div class="heading-actions">
        <el-button icon="el-icon-back" size="small" @click="goBack">返回考核列表</el-button>
        <el-button v-if="examType === 'PRACTICAL'" type="primary" size="small" icon="el-icon-finished" @click="handlePublishResult">发布成绩</el-button>
      </div>
    </header>

    <!-- 实操考核：题干 + 附件 -->
    <div v-if="examType === 'PRACTICAL'" class="subject-brief">
      <div class="brief-title">实操题干</div>
      <div class="brief-content">{{ subjectContent || '（未填写题干）' }}</div>
      <div v-if="subjectAttachment" class="brief-attach">
        <span>参考附件：</span>
        <a :href="baseApi + subjectAttachment" target="_blank" class="link">查看 / 下载</a>
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
      <el-table-column label="作答状态" width="120" align="center">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.sheetId" size="mini" :type="sheetStatusTag(scope.row.sheetStatus)">{{ sheetStatusLabel(scope.row.sheetStatus) }}</el-tag>
          <el-tag v-else size="mini" type="info" effect="plain">尚未作答</el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="examType === 'PRACTICAL'" label="作答文件" width="110" align="center">
        <template slot-scope="scope">
          <a v-if="scope.row.subjectAnswer" :href="baseApi + scope.row.subjectAnswer" target="_blank" class="link">下载文件</a>
          <span v-else class="gray-text">-</span>
        </template>
      </el-table-column>
      <el-table-column label="得分" width="100" align="center">
        <template slot-scope="scope"><strong class="num">{{ scope.row.finalScore != null ? scope.row.finalScore : (scope.row.manualScore != null ? scope.row.manualScore : '-') }}</strong></template>
      </el-table-column>
      <el-table-column label="操作" width="120" align="center">
        <template slot-scope="scope">
          <el-button v-if="scope.row.sheetId && examType === 'PRACTICAL'" type="text" size="mini" icon="el-icon-edit" class="primary-text" @click="openGrade(scope.row)">批阅</el-button>
          <span v-else-if="!scope.row.sheetId" class="gray-text">尚未作答</span>
        </template>
      </el-table-column>
    </el-table>

    <!-- 实操批阅弹窗 -->
    <el-dialog title="实操答卷批阅" :visible.sync="gradeVisible" width="760px" append-to-body>
      <div v-if="currentSheet" class="sheet-info">
        <span>实习生：<b>{{ currentSheet.userName }}</b></span>
        <span>部门：<b>{{ currentSheet.deptName || '-' }}</b></span>
      </div>
      <div class="grade-body">
        <div class="grade-subject">
          <div class="subject-answer">
            <span>作答文件：</span>
            <a v-if="currentSheet && currentSheet.subjectAnswer" :href="baseApi + currentSheet.subjectAnswer" target="_blank" class="link">点击下载查看</a>
            <span v-else class="gray-text">未上传文件</span>
          </div>
          <div class="manual-grade">
            <span>打分：</span>
            <el-input-number v-model="manualScore" :min="0" :max="100" :precision="1" size="small" />
            <span class="tip">满分建议 100</span>
          </div>
          <div class="manual-comment">
            <span>评语：</span>
            <el-input v-model="manualComment" type="textarea" :rows="2" placeholder="评语（可选）" />
          </div>
        </div>
      </div>
      <span slot="footer">
        <el-button @click="gradeVisible = false">取消</el-button>
        <el-button type="info" :loading="saving" @click="saveGrade(true)">暂存并退出</el-button>
        <el-button type="primary" :loading="saving" @click="saveGrade(false)">保存批改</el-button>
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
        <div class="publish-tip">未作答的实习生若选择「否」，则不会生成成绩记录。</div>
      </el-form>
      <span slot="footer">
        <el-button @click="publishDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="doPublishResult">确认发布</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { gradingList, gradeSheet, publishResult, getExam } from '@/api/business/exam'
import { mapGetters } from 'vuex'

export default {
  name: 'ExamGrading',
  data() {
    return {
      examId: null,
      examType: '',
      subjectContent: '',
      subjectAttachment: '',
      loading: false,
      gradingList: [],
      gradeVisible: false,
      currentSheet: null,
      manualScore: 0,
      manualComment: '',
      saving: false,
      publishDialogVisible: false,
      publishDisableExam: true,
      publishUnanswered: false
    }
  },
  computed: {
    ...mapGetters(['roles']),
    baseApi() { return process.env.VUE_APP_BASE_API || '' }
  },
  created() {
    this.examId = this.$route.params.examId
    this.loadExam()
    this.loadList()
  },
  methods: {
    loadExam() {
      getExam(this.examId).then(res => {
        this.examType = res.data.examType || 'THEORY'
        this.subjectContent = res.data.subjectContent || ''
        this.subjectAttachment = res.data.subjectAttachment || ''
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
      this.manualScore = row.manualScore != null ? row.manualScore : 0
      this.manualComment = row.manualComment || ''
      this.gradeVisible = true
    },
    saveGrade(isDraft) {
      this.saving = true
      gradeSheet({ sheetId: this.currentSheet.sheetId, manualScore: this.manualScore, manualComment: this.manualComment }).then(() => {
        this.$modal.msgSuccess(isDraft ? '已暂存，可继续查看文件后回来批改' : '批改已保存')
        this.gradeVisible = false
        this.saving = false
        this.loadList()
      }).catch(() => { this.saving = false })
    },
    handlePublishResult() {
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
    goBack() { this.$router.push('/assessment/department/exam') },
    sheetStatusLabel(s) { return { IN_PROGRESS: '作答中', SUBMITTED: '已交卷', SCORING: '批改中', PUBLISHED: '已发布' }[s] || s },
    sheetStatusTag(s) { return { IN_PROGRESS: 'info', SUBMITTED: 'warning', SCORING: 'primary', PUBLISHED: 'success' }[s] || 'info' }
  }
}
</script>

<style lang="scss" scoped>
.page-heading { margin-bottom: 18px; }
.eyebrow { color: #1764f5; font-size: 12px; letter-spacing: .05em; }
.page-heading h2 { margin: 6px 0 8px; font-size: 22px; font-weight: 600; color: #1d2939; }
.page-heading p { margin: 0; color: #667085; font-size: 13px; }
.heading-actions { text-align: right; }
.subject-brief { padding: 14px 16px; margin-bottom: 16px; background: #f8fafc; border: 1px solid #e7ecf3; border-radius: 6px; }
.brief-title { color: #1764f5; font-size: 12px; font-weight: 600; margin-bottom: 6px; }
.brief-content { color: #344054; font-size: 14px; line-height: 1.6; white-space: pre-wrap; }
.brief-attach { margin-top: 8px; color: #667085; font-size: 13px; }
.user-cell { display: flex; align-items: center; gap: 8px; }
.avatar { display: inline-flex; width: 26px; height: 26px; align-items: center; justify-content: center; color: #fff; background: #1764f5; border-radius: 50%; font-size: 13px; }
.num { color: #1764f5; }
.primary-text { color: #1764f5; }
.gray-text { color: #98a2b3; font-size: 12px; }
.link { color: #1764f5; }
.sheet-info { display: flex; gap: 24px; margin-bottom: 12px; color: #475467; font-size: 13px; }
.grade-body { padding: 4px 0; }
.grade-subject { padding: 14px; border: 1px solid #e7ecf3; border-radius: 6px; }
.subject-answer { margin-bottom: 12px; color: #667085; font-size: 13px; }
.manual-grade { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; color: #475467; font-size: 13px; }
.manual-comment { display: flex; align-items: flex-start; gap: 10px; color: #475467; font-size: 13px; }
.manual-comment .el-textarea { flex: 1; }
.tip { color: #98a2b3; font-size: 12px; }
.publish-tip { margin-top: 10px; color: #98a2b3; font-size: 12px; line-height: 1.6; }
</style>
