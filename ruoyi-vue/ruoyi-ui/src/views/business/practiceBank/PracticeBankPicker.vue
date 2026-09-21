<template>
  <div class="pbank-picker">
    <div class="pk-head">
      <div>
        <h3>模拟实操题库</h3>
        <p>勾选后，实习生可在「模拟备考」里看到该题库下的<strong>全部实操题</strong>（只读浏览）。仅「模拟考核题库 / 通用题库」且形态为实操的题库可勾选；正式考核题库不会对实习生开放。</p>
      </div>
      <el-button size="mini" icon="el-icon-refresh" @click="load">刷新</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" size="small" stripe class="pk-table">
      <el-table-column label="题库" min-width="220">
        <template slot-scope="scope">
          <div class="pk-cell">
            <strong>{{ scope.row.bankName }}</strong>
            <small>{{ scope.row.description || '暂无说明' }}</small>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="用途标签" width="126" align="center">
        <template slot-scope="scope">
          <el-tag size="mini" effect="plain" :type="scope.row.bankType === 'PRACTICE' ? 'warning' : 'success'">{{ typeText(scope.row.bankType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="实操题" width="94" align="center">
        <template slot-scope="scope"><strong class="num">{{ scope.row.subjectCount }}</strong></template>
      </el-table-column>
      <el-table-column label="对实习生开放" width="150" align="center">
        <template slot-scope="scope">
          <el-switch
            :value="scope.row.practiceEnabled === 1"
            :disabled="saving === scope.row.id"
            active-text="开放"
            inactive-text="关闭"
            @change="v => toggle(scope.row, v)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="110" align="center">
        <template slot-scope="scope">
          <el-button type="text" size="mini" icon="el-icon-view" @click="preview(scope.row)">预览题目</el-button>
        </template>
      </el-table-column>
      <template slot="empty">
        <div class="pk-empty">
          <i class="el-icon-folder-opened" />
          <p>本部门还没有「实操题库」。先到 <b>题库管理 → 实操题库</b> 新建一个，并把它标成「模拟考核题库」或「通用题库」。</p>
        </div>
      </template>
    </el-table>

    <el-dialog :title="'题目预览 · ' + (current ? current.bankName : '')" :visible.sync="visible" width="820px" top="6vh" append-to-body>
      <el-table v-loading="subLoading" :data="subjects" size="mini" max-height="480">
        <el-table-column label="题名 / 方向" min-width="200">
          <template slot-scope="scope">
            <div class="pk-cell"><strong>{{ scope.row.title }}</strong><small>{{ scope.row.direction || '未分方向' }}</small></div>
          </template>
        </el-table-column>
        <el-table-column label="难度" width="80" align="center">
          <template slot-scope="scope">{{ diffText(scope.row.difficulty) }}</template>
        </el-table-column>
        <el-table-column label="建议用时" width="92" align="center">
          <template slot-scope="scope">{{ scope.row.estimatedMinutes ? scope.row.estimatedMinutes + ' 分钟' : '—' }}</template>
        </el-table-column>
        <el-table-column label="建议满分" width="92" align="center">
          <template slot-scope="scope">{{ scope.row.suggestScore != null ? scope.row.suggestScore : '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="76" align="center">
          <template slot-scope="scope"><el-tag size="mini" :type="scope.row.status === 1 ? 'success' : 'info'">{{ scope.row.status === 1 ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
        <template slot="empty">
          <div class="pk-empty"><i class="el-icon-document" /><p>该题库还没有实操题，可到「题库管理 → 实操题库 → 管理题目」录入。</p></div>
        </template>
      </el-table>
      <span slot="footer"><el-button @click="visible = false">关闭</el-button></span>
    </el-dialog>
  </div>
</template>

<script>
import { listBank, updateBank } from '@/api/business/questionBank'
import { listPracticeSubject } from '@/api/business/practiceSubject'

export default {
  name: 'PracticeBankPicker',
  data() {
    return {
      loading: false,
      saving: null,
      banks: [],
      subjectCount: {},
      visible: false,
      subLoading: false,
      subjects: [],
      current: null
    }
  },
  computed: {
    /** 只有「实操形态 × 用途为 模拟/通用」的题库可勾选（与后端口径一致） */
    rows() {
      return (this.banks || [])
        .filter(b => (b.bankKind || 'THEORY') === 'PRACTICAL')
        .filter(b => b.bankType === 'PRACTICE' || b.bankType === 'COMMON')
        .map(b => Object.assign({}, b, { subjectCount: this.subjectCount[b.id] == null ? '—' : this.subjectCount[b.id] }))
    }
  },
  created() {
    this.load()
  },
  methods: {
    load() {
      this.loading = true
      listBank({ bankKind: 'PRACTICAL' }).then(res => {
        this.banks = res.rows || []
        this.loading = false
        this.loadCounts()
      }).catch(() => { this.loading = false })
    },
    /** 题量按库逐个取（复用题目列表接口的 total，不新增后端） */
    loadCounts() {
      this.rows.forEach(b => {
        listPracticeSubject({ bankId: b.id, pageNum: 1, pageSize: 1 }).then(res => {
          this.$set(this.subjectCount, b.id, res.total || 0)
        }).catch(() => {})
      })
    },
    toggle(row, open) {
      this.saving = row.id
      updateBank({ id: row.id, practiceEnabled: open ? 1 : 0 }).then(() => {
        this.saving = null
        row.practiceEnabled = open ? 1 : 0
        this.$modal.msgSuccess(open ? '已对实习生开放' : '已关闭（实习生不可见）')
      }).catch(() => { this.saving = null })
    },
    preview(row) {
      this.current = row
      this.visible = true
      this.subLoading = true
      listPracticeSubject({ bankId: row.id, pageNum: 1, pageSize: 200 }).then(res => {
        this.subjects = res.rows || []
        this.subLoading = false
      }).catch(() => { this.subLoading = false })
    },
    typeText(t) { return { PRACTICE: '模拟考核题库', COMMON: '通用题库', FORMAL: '正式考核题库' }[t] || '未设置' },
    diffText(d) { return { EASY: '简单', MEDIUM: '中等', HARD: '困难' }[d] || '中等' }
  }
}
</script>

<style lang="scss" scoped>
.pbank-picker { padding: 16px 18px; background: #fff; border: 1px solid #e7ecf3; border-radius: 8px; }
.pk-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 14px; padding-bottom: 12px; margin-bottom: 12px; border-bottom: 1px solid #edf0f4; }
.pk-head h3 { margin: 0 0 5px; color: #1d2939; font-size: 15px; font-weight: 600; }
.pk-head p { margin: 0; max-width: 720px; color: #667085; font-size: 12px; line-height: 1.7; }
.pk-head p strong { color: #344054; }
.pk-cell strong { display: block; color: #1d2939; font-size: 13.5px; }
.pk-cell small { display: block; margin-top: 3px; color: #98a2b3; font-size: 12px; }
.num { color: #1764f5; }
.pk-empty { padding: 24px 0; text-align: center; }
.pk-empty i { color: #d0d5dd; font-size: 30px; }
.pk-empty p { margin: 10px 0 0; color: #8490a0; font-size: 13px; line-height: 1.7; }
.pk-empty b { color: #1764f5; }
</style>
