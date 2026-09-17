<template>
  <div class="exam-manage app-container">
    <header class="page-heading">
      <div>
        <div class="eyebrow">学习与考核管理 / 正式考核管理</div>
        <div class="title-line">
          <h2>正式考核管理</h2>
          <el-tag size="mini" effect="plain" :type="isSuperAdmin ? 'warning' : 'success'">{{ isSuperAdmin ? '全局管理' : '本部门范围' }}</el-tag>
          <el-tag size="mini" effect="plain" type="info">分环节发布</el-tag>
        </div>
        <p>{{ isSuperAdmin ? '查看并维护全组织考核，可按部门筛选并进入答卷批阅。' : '按批次管理正式考核：理论 / 实操各自独立「保存发布」，同一实习生可在多个批次分阶段多次参加。' }}</p>
      </div>
      <div class="heading-actions">
        <el-button icon="el-icon-refresh" size="small" @click="loadList">刷新</el-button>
        <el-button v-hasPermi="['business:bank:add']" type="primary" icon="el-icon-plus" size="small" @click="handleAdd">新建考核</el-button>
      </div>
    </header>

    <el-form :inline="true" size="small" class="query-form" @submit.native.prevent>
      <el-form-item v-if="isSuperAdmin" label="所属部门">
        <el-select v-model="queryParams.deptId" clearable filterable placeholder="全部部门" style="width:180px" @change="handleQuery">
          <el-option v-for="dept in deptOptions" :key="dept.deptId" :label="dept.deptName" :value="dept.deptId" />
        </el-select>
      </el-form-item>
      <el-form-item label="考核名称">
        <el-input v-model="queryParams.examName" clearable placeholder="批次 / 场次名称" style="width:200px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="考核模式">
        <el-select v-model="queryParams.examMode" clearable placeholder="全部模式" style="width:130px" @change="handleQuery">
          <el-option label="正式考核" value="FORMAL" />
          <el-option label="模拟考核" value="PRACTICE" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" clearable placeholder="全部状态" style="width:130px">
          <el-option label="待发布" value="DRAFT" />
          <el-option label="已发布" value="PUBLISHED" />
          <el-option label="待批改" value="GRADING" />
          <el-option label="已停用" value="DISABLED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="handleQuery">查询</el-button>
        <el-button icon="el-icon-refresh-left" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- ① 批次（场次）列表 -->
    <section class="dm-card">
      <div class="dm-head">
        <div class="dm-title">
          <span class="dm-idx">批</span>
          <h3>批次列表</h3>
          <span class="dm-hint">同一批次 = 一条考核记录；同一实习生可分阶段多次参加，每批次独立计分</span>
        </div>
        <span class="dm-meta">共 {{ batches.length }} 个批次</span>
      </div>
      <el-table
        v-loading="loading"
        :data="batches"
        size="small"
        highlight-current-row
        empty-text="暂无考核，点击「新建考核」创建第一个批次"
        @current-change="selectBatch"
      >
        <el-table-column label="批次 / 场次" min-width="230">
          <template slot-scope="scope">
            <div class="exam-cell">
              <strong>{{ scope.row.name }}</strong>
              <small>{{ scope.row.modeText }} · {{ scope.row.examCount }} 个环节</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="理论环节" width="130" align="center">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.theory" size="mini" :type="statusTag(scope.row.theory.status)">{{ statusLabel(scope.row.theory.status) }}</el-tag>
            <span v-else class="muted">未创建</span>
          </template>
        </el-table-column>
        <el-table-column label="实操环节" width="130" align="center">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.practice" size="mini" :type="statusTag(scope.row.practice.status)">{{ statusLabel(scope.row.practice.status) }}</el-tag>
            <span v-else class="muted">未创建</span>
          </template>
        </el-table-column>
        <el-table-column label="已作答" width="90" align="center">
          <template slot-scope="scope"><strong class="num">{{ scope.row.answered }}</strong></template>
        </el-table-column>
        <el-table-column label="待批阅" width="90" align="center">
          <template slot-scope="scope"><span class="num warn">{{ scope.row.pending }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template slot-scope="scope">
            <el-button type="text" size="mini" icon="el-icon-view" @click.stop="selectBatch(scope.row)">配置环节</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="loadList" />
    </section>

    <!-- ② 一页两卡：当前批次的理论 / 实操环节 -->
    <div v-if="currentBatch" class="dm-pair">
      <section class="dm-card">
        <div class="dm-head">
          <div class="dm-title">
            <span class="dm-idx">理</span>
            <h3>理论考试</h3>
            <span class="dm-hint">所属批次：{{ currentBatch.name }}</span>
          </div>
          <div class="dm-actions">
            <el-tag v-if="currentBatch.theory" size="mini" :type="statusTag(currentBatch.theory.status)">{{ statusLabel(currentBatch.theory.status) }}</el-tag>
            <el-button v-if="!currentBatch.theory" v-hasPermi="['business:bank:add']" size="mini" type="primary" plain @click="handleAdd('THEORY')">创建理论环节</el-button>
            <template v-else>
              <el-button v-hasPermi="['business:bank:edit']" size="mini" @click="handleEdit(currentBatch.theory)">编辑</el-button>
              <el-button v-if="currentBatch.theory.status === 'DRAFT'" v-hasPermi="['business:bank:edit']" size="mini" type="primary" @click="handlePublish(currentBatch.theory)">保存发布</el-button>
              <el-button v-if="currentBatch.theory.status === 'PUBLISHED'" v-hasPermi="['business:bank:edit']" size="mini" @click="handleChangeStatus(currentBatch.theory, 'DISABLED')">停用</el-button>
              <el-button v-if="currentBatch.theory.status === 'DISABLED'" v-hasPermi="['business:bank:edit']" size="mini" type="primary" plain @click="handleChangeStatus(currentBatch.theory, 'PUBLISHED')">启用</el-button>
              <el-button v-hasPermi="['business:bank:remove']" size="mini" type="text" class="danger-text" @click="handleDelete(currentBatch.theory)">删除</el-button>
            </template>
          </div>
        </div>

        <div v-if="!currentBatch.theory" class="dm-empty"><i class="el-icon-document-add" /><span>本批次尚未创建理论环节，创建后可独立「保存发布」。</span></div>
        <template v-else>
          <div class="dm-grid">
            <div class="dm-fields">
              <div class="field"><span>考试形式</span><b>线上考试</b></div>
              <div class="field"><span>考核名称</span><b>{{ currentBatch.theory.examName }}</b></div>
              <div class="field"><span>所属题库</span><b>{{ currentBatch.theory.bankName || '未关联题库' }}</b></div>
              <div class="field"><span>考试时长</span><b>{{ currentBatch.theory.duration ? currentBatch.theory.duration + ' 分钟' : '--' }}</b></div>
              <div class="field"><span>通过分 / 总分</span><b>{{ currentBatch.theory.passLine }} / {{ theoryTotal }} 分</b></div>
              <div class="field"><span>题目数量</span><b>{{ theoryQuestionCount }} 题</b></div>
              <div class="field"><span>题型与分值</span><b>{{ theorySpec }}</b></div>
              <div class="field"><span>时间窗</span><b>{{ windowText(currentBatch.theory) }}</b></div>
            </div>
            <div class="dm-table-wrap">
              <div class="dm-sub-head">
                <h4>知识分布（按题库知识点抽题）</h4>
                <div class="dm-actions">
                  <el-button size="mini" @click="loadKnowledgePoints(currentBatch.theory)">从题库导入知识点</el-button>
                  <el-button size="mini" :loading="drawing" @click="tryDraw(currentBatch.theory)">试抽一套</el-button>
                  <el-button size="mini" :loading="configSaving" type="primary" plain @click="saveConfig(currentBatch.theory)">保存配置</el-button>
                </div>
              </div>
              <el-table :data="rules" size="mini" border empty-text="点「从题库导入知识点」按题库自动铺满">
                <el-table-column label="知识模块（题库知识点）" min-width="140">
                  <template slot-scope="scope">
                    <el-select v-model="scope.row.knowledgePoint" size="mini" filterable style="width:100%">
                      <el-option v-for="p in knowledgePoints" :key="p.knowledgePoint" :label="p.knowledgePoint" :value="p.knowledgePoint" />
                    </el-select>
                  </template>
                </el-table-column>
                <el-table-column label="题库可用" width="82" align="center">
                  <template slot-scope="scope">{{ availableOf(scope.row.knowledgePoint) }} 题</template>
                </el-table-column>
                <el-table-column label="抽题数量" width="120" align="center">
                  <template slot-scope="scope">
                    <el-input-number v-model="scope.row.questionCount" :min="0" :max="100" size="mini" style="width:100px" />
                  </template>
                </el-table-column>
                <el-table-column label="占比" width="100" align="center">
                  <template slot-scope="scope">
                    <el-input-number v-model="scope.row.ratio" :min="0" :max="100" size="mini" style="width:84px" />
                  </template>
                </el-table-column>
                <el-table-column label="单选题占比" width="112" align="center">
                  <template slot-scope="scope">
                    <el-input-number v-model="scope.row.singleRatio" :min="0" :max="100" size="mini" style="width:96px" />
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="70" align="center">
                  <template slot-scope="scope">
                    <el-button type="text" size="mini" class="danger-text" @click="rules.splice(scope.$index, 1)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <div class="dm-callout" :class="checkOk ? 'ok' : 'warn'">
                <span v-if="checkOk">校验通过：抽题数量合计 {{ ruleCountSum }} = 题目数量 {{ theoryQuestionCount }} ✓　占比合计 {{ ratioSum }}% ✓　每行不超题库可用 ✓</span>
                <span v-else>{{ checkMessage }}</span>
              </div>
              <p class="dm-note">合计 <b>{{ theoryQuestionCount }} 题</b> · {{ theorySpec }}。抽题时在该知识点内随机取题，再按题型配比组卷；知识分布是「考什么」的依据。</p>
            </div>
          </div>
        </template>
      </section>

      <section class="dm-card">
        <div class="dm-head">
          <div class="dm-title">
            <span class="dm-idx green">实</span>
            <h3>实操考试</h3>
            <span class="dm-hint">与实习生端「正式考核」同步的实操任务</span>
          </div>
          <div class="dm-actions">
            <el-tag v-if="currentBatch.practice" size="mini" :type="statusTag(currentBatch.practice.status)">{{ statusLabel(currentBatch.practice.status) }}</el-tag>
            <el-button v-if="!currentBatch.practice" v-hasPermi="['business:bank:add']" size="mini" type="primary" plain @click="handleAdd('PRACTICAL')">创建实操环节</el-button>
            <template v-else>
              <el-button v-hasPermi="['business:bank:edit']" size="mini" @click="handleEdit(currentBatch.practice)">编辑</el-button>
              <el-button v-if="currentBatch.practice.status === 'DRAFT'" v-hasPermi="['business:bank:edit']" size="mini" type="primary" @click="handlePublish(currentBatch.practice)">保存发布</el-button>
              <el-button v-if="currentBatch.practice.status === 'PUBLISHED'" v-hasPermi="['business:bank:edit']" size="mini" @click="handleChangeStatus(currentBatch.practice, 'DISABLED')">停用</el-button>
              <el-button v-if="currentBatch.practice.status === 'DISABLED'" v-hasPermi="['business:bank:edit']" size="mini" type="primary" plain @click="handleChangeStatus(currentBatch.practice, 'PUBLISHED')">启用</el-button>
              <el-button v-hasPermi="['business:bank:remove']" size="mini" type="text" class="danger-text" @click="handleDelete(currentBatch.practice)">删除</el-button>
            </template>
          </div>
        </div>

        <div v-if="!currentBatch.practice" class="dm-empty"><i class="el-icon-upload2" /><span>本批次尚未创建实操环节；理论已发布时实习生端显示「理论已开放 · 实操待发布」。</span></div>
        <template v-else>
          <div class="dm-grid">
            <div class="dm-fields">
              <div class="field"><span>考试形式</span><b>线上提交（文件包）</b></div>
              <div class="field"><span>考核名称</span><b>{{ currentBatch.practice.examName }}</b></div>
              <div class="field"><span>考试时长</span><b>{{ currentBatch.practice.duration ? currentBatch.practice.duration + ' 分钟' : '--' }}</b></div>
              <div class="field"><span>通过分 / 总分</span><b>{{ currentBatch.practice.passLine }} / 100 分</b></div>
              <div class="field"><span>题目数量</span><b>{{ currentBatch.practice.subjectCount || currentBatch.practice.questionCount || 1 }} 题</b></div>
              <div class="field"><span>时间窗</span><b>{{ windowText(currentBatch.practice) }}</b></div>
              <div class="field"><span>判分方式</span><b>人工批阅（不自动判分）</b></div>
              <div class="field"><span>已作答 / 待批阅</span><b>{{ currentBatch.practice.answeredCount || 0 }} / {{ currentBatch.practice.pendingCount || 0 }}</b></div>
            </div>
            <div class="dm-table-wrap">
              <div class="dm-sub-head">
                <h4>考试题目</h4>
                <el-button size="mini" icon="el-icon-view" @click="goGrading(currentBatch.practice)">查看提交与批阅</el-button>
              </div>
              <div class="subject-box">
                <div class="subject-label">实操题干</div>
                <div class="subject-content">{{ currentBatch.practice.subjectContent || '未填写实操题干' }}</div>
                <div v-if="currentBatch.practice.subjectAttachment" class="subject-attach">
                  <i class="el-icon-paperclip" /> 已上传参考附件
                </div>
              </div>
              <p class="dm-note">实习生按题干打包上传（后端存 <code>answer_sheet.subject_answer</code>），管理员在「成绩管理」下载查看后<b>人工录分</b>。</p>
            </div>
          </div>
        </template>
      </section>
    </div>

            <div class="card" style="margin-bottom:13px">
              <div class="card-h">
                <div class="tt"><span class="idx">发</span><h3>发布设置</h3><span class="hint" style="margin-left:8px">本批次的理论 / 实操环节共用：发布时间与参与人员</span></div>
                <span class="badge blue">按批次</span>
              </div>
              <div class="grid">
                <div class="c4">
                  <div class="note" style="margin:0 0 8px"><b>发布时间</b></div>
                  <div class="field" style="margin-bottom:9px">
                    <span>发布方式</span>
                    <el-radio-group v-model="publishMode" size="mini">
                      <el-radio-button label="NOW">立即发布</el-radio-button>
                      <el-radio-button label="TIMED">定时发布</el-radio-button>
                    </el-radio-group>
                  </div>
                  <div class="field" v-if="publishMode === 'TIMED'" style="display:block;margin-bottom:9px">
                    <span style="display:block;margin-bottom:6px">开放 / 截止时间</span>
                    <el-date-picker
                      v-model="timeRange"
                      type="datetimerange"
                      size="mini"
                      range-separator="至"
                      start-placeholder="开放时间"
                      end-placeholder="截止时间"
                      value-format="yyyy-MM-dd HH:mm:ss"
                      style="width:100%"
                    />
                  </div>
                  <p class="dm-note">定时发布：实习生端在开放时间后才会看到「开始考试」，截止后仅可查看成绩；立即发布则忽略时间窗。</p>
                </div>
                <div class="c8">
                  <div class="note" style="margin:0 0 8px"><b>指定人员</b></div>
                  <div class="dm-actions" style="margin-bottom:9px">
                    <el-radio-group v-model="assignMode" size="mini">
                      <el-radio-button label="ALL">全部在培实习生</el-radio-button>
                      <el-radio-button label="ASSIGNED">指定人员</el-radio-button>
                    </el-radio-group>
                    <el-select
                      v-if="assignMode === 'ASSIGNED'"
                      v-model="participantIds"
                      multiple
                      filterable
                      collapse-tags
                      size="mini"
                      placeholder="从本部门花名册选择实习生"
                      style="width:320px"
                    >
                      <el-option v-for="u in internOptions" :key="u.userId" :label="u.nickName + '（' + (u.positionName || '未分配岗位') + '）'" :value="u.userId" />
                    </el-select>
                    <span class="muted" v-if="assignMode === 'ALL'">本部门全部在培实习生均可参加（默认）</span>
                  </div>
                  <p class="dm-note">选「指定人员」后 <b>仅名单内的实习生</b>能在实习生端「正式考核」页看到本批次场次；名单为空时回退为全部可见。</p>
                </div>
              </div>
            </div>

    <!-- ③ 说明区 -->
    <div class="dm-notes">
      <div class="dm-note-card">
        <h4>判分：理论自动 · 实操人工</h4>
        <p>理论交卷后由后端直接比对答案出分；实操无自动判分，须管理员下载文件包后人工评分。</p>
      </div>
      <div class="dm-note-card">
        <h4>多次参加的落点</h4>
        <p>「阶段性多次参加」= 多个批次各一条考核记录，后端零改表；批次内补考由补考次数配置（<code>exam_rule_snapshot.retake_count</code>）+ 作答次数（<code>answer_sheet.retake_seq</code>）控制。</p>
      </div>
      <div class="dm-note-card">
        <h4>后端已支持 / 待补</h4>
        <p>已支持：题量与分值、时长、及格线、时间窗、实操题干（当前考核表字段齐全）。待补（P1）：批次字段显式化、模块占比、考生端列表接口返回时间窗。</p>
      </div>
    </div>

    <!-- 新建/编辑弹窗 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="560px" append-to-body>
      <el-form ref="examForm" :model="form" :rules="rules" label-width="100px">
        <el-form-item v-if="isSuperAdmin && !form.id" label="所属部门" prop="deptId">
          <el-select v-model="form.deptId" filterable placeholder="请选择所属部门" style="width:100%" @change="handleFormDeptChange">
            <el-option v-for="dept in deptOptions" :key="dept.deptId" :label="dept.deptName" :value="dept.deptId" />
          </el-select>
        </el-form-item>
        <el-form-item label="考核名称" prop="examName"><el-input v-model="form.examName" placeholder="建议包含批次，如「2026Q4 开发实习生（第二期 · 补考）」" /></el-form-item>
        <el-form-item label="考核类型" prop="examType">
          <el-radio-group v-model="form.examType" :disabled="!!form.id">
            <el-radio-button label="THEORY">理论考核</el-radio-button>
            <el-radio-button label="PRACTICAL">实操考核</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <template v-if="form.examType === 'THEORY'">
          <el-form-item label="所属题库" prop="bankId">
            <el-select v-model="form.bankId" :disabled="isSuperAdmin && !form.deptId" placeholder="选择当前部门题库" style="width:100%">
              <el-option v-for="b in bankOptions" :key="b.id" :label="b.bankName" :value="b.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="单选题数量"><el-input-number v-model="form.singleCount" :min="0" :max="100" /></el-form-item>
          <el-form-item label="单选每题分"><el-input-number v-model="form.singleScore" :min="0" :max="100" :precision="1" /></el-form-item>
          <el-form-item label="多选题数量"><el-input-number v-model="form.multiCount" :min="0" :max="100" /></el-form-item>
          <el-form-item label="多选每题分"><el-input-number v-model="form.multiScore" :min="0" :max="100" :precision="1" /></el-form-item>
          <el-form-item label="判断题数量"><el-input-number v-model="form.judgeCount" :min="0" :max="100" /></el-form-item>
          <el-form-item label="判断每题分"><el-input-number v-model="form.judgeScore" :min="0" :max="100" :precision="1" /></el-form-item>
        </template>

        <template v-else>
          <el-form-item label="实操题干" prop="subjectContent">
            <el-input v-model="form.subjectContent" type="textarea" :rows="4" placeholder="请输入实操考核题干（要求实习生完成的任务说明）" />
          </el-form-item>
          <el-form-item label="参考附件">
            <el-upload :auto-upload="false" :limit="1" :show-file-list="false" accept="*" :on-change="onSubjectFile">
              <el-button size="small" icon="el-icon-upload2">选择附件（可选）</el-button>
            </el-upload>
            <div v-if="form.subjectAttachment" class="attachment-tip"><i class="el-icon-paperclip" /> 已上传参考附件（点击重新选择可替换）</div>
          </el-form-item>
        </template>

        <el-form-item label="考核时长(分)"><el-input-number v-model="form.duration" :min="0" :max="600" /></el-form-item>
        <el-form-item label="通过线(分)"><el-input-number v-model="form.passLine" :min="0" :max="100" :precision="1" /></el-form-item>
        <el-form-item label="时间窗">
          <el-date-picker
            v-model="form.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="截止时间"
            value-format="yyyy-MM-dd HH:mm:ss"
            style="width:100%"
          />
        </el-form-item>
      </el-form>
      <span slot="footer"><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button></span>
    </el-dialog>
  </div>
</template>

<script>
import { listExam, addExam, updateExam, delExam, publishExam, changeExamStatus, uploadFile,
  listKnowledgePoints, tryDrawPaper, saveExamConfig, getExamConfig } from '@/api/business/exam'
import { listRegister } from '@/api/business/register'
import { listBank } from '@/api/business/questionBank'
import { listDept } from '@/api/system/dept'
import { mapGetters } from 'vuex'

export default {
  name: 'ExamManage',
  data() {
    return {
      loading: false,
      examList: [],
      total: 0,
      queryParams: { pageNum: 1, pageSize: 10, examMode: 'FORMAL', examName: '', status: '', deptId: null },
      bankOptions: [],
      deptOptions: [],
      currentBatchKey: '',
      // ---- 知识分布 / 发布设置 ----
      knowledgePoints: [],
      rules: [],
      drawing: false,
      configSaving: false,
      publishMode: 'NOW',
      timeRange: [],
      assignMode: 'ALL',
      participantIds: [],
      internOptions: [],
      dialogVisible: false,
      dialogTitle: '',
      form: { id: null, deptId: null, examName: '', examMode: 'FORMAL', examType: 'THEORY', bankId: null, singleCount: 5, multiCount: 3, judgeCount: 2, singleScore: 2, multiScore: 4, judgeScore: 2, subjectContent: '', subjectAttachment: '', passLine: 60, duration: 60, timeRange: [] },
      rules: {
        deptId: [{ required: true, message: '请选择所属部门', trigger: 'change' }],
        examName: [{ required: true, message: '请输入考核名称', trigger: 'blur' }],
        bankId: [{ required: true, message: '请选择题库', trigger: 'change' }],
        subjectContent: [{ required: true, message: '请输入实操题干', trigger: 'blur' }]
      },
      submitting: false
    }
  },
  computed: {
    ...mapGetters(['roles']),
    isSuperAdmin() { return this.roles.indexOf('SUPER_ADMIN') > -1 },
    /** 批次分组：考核名称中的批次 / 期次标识（示例解析；后端补批次字段后可替换） */
    batches() {
      const map = {}
      this.examList.forEach(exam => {
        const key = this.batchKeyOf(exam)
        if (!map[key]) map[key] = { key, name: key, theory: null, practice: null, exams: [] }
        map[key].exams.push(exam)
        if (exam.examType === 'PRACTICAL') map[key].practice = exam
        else map[key].theory = exam
      })
      return Object.keys(map).map(key => {
        const b = map[key]
        const answered = b.exams.reduce((sum, e) => sum + (Number(e.answeredCount) || 0), 0)
        const pending = b.exams.reduce((sum, e) => sum + (Number(e.pendingCount) || 0), 0)
        const modes = b.exams.map(e => e.examMode).filter(Boolean)
        return Object.assign(b, {
          answered,
          pending,
          examCount: b.exams.length,
          modeText: modes.indexOf('FORMAL') > -1 ? '正式考核' : (modes.indexOf('PRACTICE') > -1 ? '模拟考核' : '考核')
        })
      })
    },
    currentBatch() {
      if (!this.batches.length) return null
      return this.batches.find(b => b.key === this.currentBatchKey) || this.batches[0]
    },
    theoryTotal() {
      const t = this.currentBatch && this.currentBatch.theory
      if (!t) return '--'
      return (Number(t.singleCount) || 0) * (Number(t.singleScore) || 0) +
        (Number(t.multiCount) || 0) * (Number(t.multiScore) || 0) +
        (Number(t.judgeCount) || 0) * (Number(t.judgeScore) || 0)
    },
    theoryQuestionCount() {
      const t = this.currentBatch && this.currentBatch.theory
      if (!t) return 0
      return (Number(t.singleCount) || 0) + (Number(t.multiCount) || 0) + (Number(t.judgeCount) || 0)
    },
    theorySpec() {
      const t = this.currentBatch && this.currentBatch.theory
      if (!t) return '--'
      const parts = []
      if (Number(t.singleCount) > 0) parts.push('单选 ' + t.singleCount + '×' + t.singleScore + ' 分')
      if (Number(t.multiCount) > 0) parts.push('多选 ' + t.multiCount + '×' + t.multiScore + ' 分')
      if (Number(t.judgeCount) > 0) parts.push('判断 ' + t.judgeCount + '×' + t.judgeScore + ' 分')
      return parts.join(' + ') || '--'
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
      if (this.ruleCountSum !== this.theoryQuestionCount) return false
      if (Math.abs(this.ratioSum - 100) > 0.01) return false
      return this.overRows.length === 0
    },
    checkMessage() {
      if (!this.rules.length) return '尚未配置知识分布：点「从题库导入知识点」按题库自动铺满。'
      if (this.ruleCountSum !== this.theoryQuestionCount) {
        return '抽题数量合计 ' + this.ruleCountSum + ' 与环节题目数量 ' + this.theoryQuestionCount + ' 不一致。'
      }
      if (Math.abs(this.ratioSum - 100) > 0.01) return '占比合计为 ' + this.ratioSum + '%，需调整为 100%。'
      if (this.overRows.length) return '「' + this.overRows.map(r => r.knowledgePoint).join('、') + '」抽题数量超过题库可用题量。'
      return ''
    }
  },
  created() {
    if (this.isSuperAdmin) this.loadDepartments()
    this.loadList()
    if (!this.isSuperAdmin) this.loadBanks()
    this.loadInterns()
  },
  methods: {
    /** 批次分组键：同名考核视为同一批次的两个环节（理论 / 实操） */
    batchKeyOf(exam) {
      return (exam.examName || '未命名考核').trim()
    },
    windowText(exam) {
      if (!exam) return '--'
      const start = exam.startTime ? String(exam.startTime).slice(0, 16) : ''
      const end = exam.endTime ? String(exam.endTime).slice(0, 16) : ''
      if (!start && !end) return '未设置'
      return (start || '--') + ' ~ ' + (end || '--')
    },
    loadDepartments() {
      listDept({ status: '0' }).then(res => {
        this.deptOptions = (res.data || []).filter(dept => dept.parentId !== 0)
      })
    },
    loadList() {
      this.loading = true
      listExam(this.queryParams).then(res => {
        this.examList = res.rows || []
        this.total = res.total || 0
        this.loading = false
        if (this.examList.length && !this.currentBatchKey) this.currentBatchKey = this.batchKeyOf(this.examList[0])
      }).catch(() => { this.loading = false })
    },
    selectBatch(row) {
      if (row && row.key) this.currentBatchKey = row.key
      this.loadBatchConfig()
    },
    /** 载入当前批次理论环节的知识分布与发布设置 */
    loadBatchConfig() {
      const batch = this.currentBatch
      this.rules = []
      if (!batch || !batch.theory) return
      const theory = batch.theory
      if (theory.bankId) this.loadKnowledgePoints(theory)
      getExamConfig(theory.id).then(res => {
        const data = res.data || {}
        const rows = (data.knowledgeRules || []).map(r => ({
          knowledgePoint: r.knowledgePoint,
          questionCount: r.questionCount,
          ratio: r.ratio == null ? null : Number(r.ratio),
          singleRatio: r.singleRatio == null ? null : Number(r.singleRatio)
        }))
        if (rows.length) this.rules = rows
        this.assignMode = data.assignMode || 'ALL'
        this.participantIds = data.participantIds || []
        if (data.startTime && data.endTime) {
          this.publishMode = 'TIMED'
          this.timeRange = [String(data.startTime).slice(0, 19), String(data.endTime).slice(0, 19)]
        } else {
          this.publishMode = 'NOW'
          this.timeRange = []
        }
      }).catch(() => {})
    },
    /** 本部门实习生名单（指定人员用） */
    loadInterns() {
      listRegister({ pageNum: 1, pageSize: 200 }).then(res => {
        this.internOptions = (res.rows || []).map(r => ({
          userId: r.userId,
          nickName: r.nickName || r.realName || r.userName,
          positionName: r.positionName
        })).filter(u => !!u.userId)
      }).catch(() => { this.internOptions = [] })
    },
    /** 拉取题库知识点（可编辑） */
    loadKnowledgePoints(exam) {
      if (!exam || !exam.bankId) {
        this.$modal.msgWarning('该环节未关联题库，请先编辑环节选择题库')
        return
      }
      listKnowledgePoints(exam.bankId).then(res => {
        this.knowledgePoints = res.data || []
        if (!this.rules.length) this.importAll(exam)
      }).catch(() => { this.knowledgePoints = [] })
    },
    /** 按题库知识点铺满（分摊到环节题目数量，合计 = 题目数量） */
    importAll(exam) {
      const need = this.theoryQuestionCount || 20
      const usable = this.knowledgePoints.filter(p => p.totalCount > 0)
      if (!usable.length) {
        this.$modal.msgWarning('该题库暂无题目，请先到题库管理导入题目')
        return
      }
      const totalAvail = usable.reduce((sum, p) => sum + p.totalCount, 0)
      let assigned = 0
      this.rules = usable.map(p => {
        let cnt = Math.floor(need * p.totalCount / totalAvail)
        if (cnt < 1) cnt = 1
        if (cnt > p.totalCount) cnt = p.totalCount
        assigned += cnt
        return { knowledgePoint: p.knowledgePoint, questionCount: cnt, ratio: 0, singleRatio: 50 }
      })
      let diff = need - assigned
      for (let i = 0; i < this.rules.length && diff !== 0; i++) {
        const avail = this.availableOf(this.rules[i].knowledgePoint)
        if (diff > 0 && this.rules[i].questionCount < avail) { this.rules[i].questionCount++; diff-- }
        else if (diff < 0 && this.rules[i].questionCount > 1) { this.rules[i].questionCount--; diff++ }
      }
      const sum = this.rules.reduce((a, r) => a + r.questionCount, 0) || 1
      this.rules.forEach(r => { r.ratio = Math.round(r.questionCount / sum * 10000) / 100 })
      this.$modal.msgSuccess('已按题库知识点铺满 ' + this.rules.length + ' 行（合计 ' + sum + ' 题）')
    },
    availableOf(point) {
      const hit = this.knowledgePoints.find(p => p.knowledgePoint === point)
      return hit ? hit.totalCount : 0
    },
    /** 试抽一套（真实按知识分布抽题） */
    tryDraw(exam) {
      if (!this.rules.length) {
        this.$modal.msgWarning('请先配置知识分布')
        return
      }
      this.drawing = true
      tryDrawPaper(exam.bankId, { knowledgeRules: this.rules }).then(res => {
        const list = res.data || []
        this.drawing = false
        if (!list.length) {
          this.$modal.msgWarning('按当前配置抽不到题')
          return
        }
        this.$alert(list.map(q => q.seq + ' · ' + q.qtype + ' · ' + q.knowledgePoint + ' · ' + q.stem).join('<br/>'),
          '试抽结果（' + list.length + ' 题）', { dangerouslyUseHTMLString: true })
      }).catch(() => { this.drawing = false })
    },
    /** 保存知识分布（不改发布状态） */
    saveConfig(exam) {
      if (!this.checkOk) {
        this.$modal.msgWarning(this.checkMessage || '配置校验未通过')
        return
      }
      this.configSaving = true
      saveExamConfig(exam.id, {
        knowledgeRules: this.rules.map((r, i) => ({
          knowledgePoint: r.knowledgePoint,
          questionCount: r.questionCount,
          ratio: r.ratio,
          singleRatio: r.singleRatio,
          sortNo: i + 1
        })),
        assignMode: this.assignMode,
        participantIds: this.assignMode === 'ASSIGNED' ? this.participantIds : [],
        startTime: this.timeRange && this.timeRange.length === 2 ? this.timeRange[0] : null,
        endTime: this.timeRange && this.timeRange.length === 2 ? this.timeRange[1] : null
      }).then(() => {
        this.configSaving = false
        this.$modal.msgSuccess('知识分布与发布设置已保存')
      }).catch(() => { this.configSaving = false })
    },
    loadBanks(deptId) {
      if (this.isSuperAdmin && !deptId) {
        this.bankOptions = []
        return
      }
      listBank({ deptId, pageNum: 1, pageSize: 1000, status: 'ENABLED' }).then(res => { this.bankOptions = res.rows || [] })
    },
    handleAdd(examType) {
      this.form = { id: null, deptId: null, examName: '', examMode: 'FORMAL', examType: examType || 'THEORY', bankId: null, singleCount: 5, multiCount: 3, judgeCount: 2, singleScore: 2, multiScore: 4, judgeScore: 2, subjectContent: '', subjectAttachment: '', passLine: 60, duration: examType === 'PRACTICAL' ? 180 : 60, timeRange: [] }
      this.dialogTitle = examType === 'PRACTICAL' ? '新建实操环节' : '新建考核'
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.examForm && this.$refs.examForm.clearValidate())
    },
    handleEdit(row) {
      this.form = {
        id: row.id, deptId: row.deptId, examName: row.examName, examMode: row.examMode || 'FORMAL', examType: row.examType || 'THEORY',
        bankId: row.bankId, singleCount: row.singleCount || 0, multiCount: row.multiCount || 0, judgeCount: row.judgeCount || 0,
        singleScore: row.singleScore, multiScore: row.multiScore, judgeScore: row.judgeScore,
        subjectContent: row.subjectContent || '', subjectAttachment: row.subjectAttachment || '',
        passLine: row.passLine, duration: row.duration || 0,
        timeRange: row.startTime && row.endTime ? [String(row.startTime).slice(0, 19), String(row.endTime).slice(0, 19)] : []
      }
      this.loadBanks(row.deptId)
      this.dialogTitle = '编辑环节'
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.examForm && this.$refs.examForm.clearValidate())
    },
    handleFormDeptChange(deptId) {
      this.form.bankId = null
      this.loadBanks(deptId)
    },
    onSubjectFile(file) {
      const formData = new FormData()
      formData.append('file', file.raw)
      uploadFile(formData).then(res => {
        this.form.subjectAttachment = res.fileName
        this.$modal.msgSuccess('附件上传成功')
      })
    },
    submitForm() {
      this.$refs.examForm.validate(valid => {
        if (!valid) return
        this.submitting = true
        const payload = { ...this.form }
        if (payload.timeRange && payload.timeRange.length === 2) {
          payload.startTime = payload.timeRange[0]
          payload.endTime = payload.timeRange[1]
        } else {
          payload.startTime = null
          payload.endTime = null
        }
        delete payload.timeRange
        if (payload.examType === 'THEORY') {
          payload.subjectContent = null
          payload.subjectAttachment = null
        } else {
          payload.bankId = null
          payload.questionCount = null
          payload.singleCount = null
          payload.multiCount = null
          payload.judgeCount = null
          payload.singleScore = null
          payload.multiScore = null
          payload.judgeScore = null
        }
        const fn = payload.id ? updateExam : addExam
        fn(payload).then(() => {
          this.$modal.msgSuccess('保存成功')
          this.dialogVisible = false
          this.submitting = false
          this.loadList()
        }).catch(() => { this.submitting = false })
      })
    },
    handlePublish(row) {
      const tip = this.publishMode === 'TIMED' && this.timeRange.length === 2
        ? `确认发布「${row.examName}」吗？将在 ${this.timeRange[0]} 开放、${this.timeRange[1]} 截止。`
        : `确认发布「${row.examName}」吗？发布后实习生可在对应场次参加。`
      this.$modal.confirm(tip).then(() => {
        const payload = {
          assignMode: this.assignMode,
          participantIds: this.assignMode === 'ASSIGNED' ? this.participantIds : [],
          startTime: this.publishMode === 'TIMED' && this.timeRange.length === 2 ? this.timeRange[0] : null,
          endTime: this.publishMode === 'TIMED' && this.timeRange.length === 2 ? this.timeRange[1] : null
        }
        if (this.rules.length) {
          payload.knowledgeRules = this.rules.map((r, i) => ({
            knowledgePoint: r.knowledgePoint,
            questionCount: r.questionCount,
            ratio: r.ratio,
            singleRatio: r.singleRatio,
            sortNo: i + 1
          }))
        }
        publishExam(row.id, payload).then(() => {
          this.$modal.msgSuccess('发布成功')
          this.loadList()
        })
      }).catch(() => {})
    },
    handleDelete(row) {
      this.$modal.confirm(`确认删除「${row.examName}」吗？`).then(() => {
        delExam(row.id).then(() => { this.$modal.msgSuccess('删除成功'); this.loadList() })
      }).catch(() => {})
    },
    goGrading(row) {
      if (!row) return
      this.$router.push('/assessment/department/exam/grading/' + row.id)
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.loadList()
    },
    resetQuery() {
      this.queryParams = { pageNum: 1, pageSize: 10, examMode: 'FORMAL', examName: '', status: '', deptId: null }
      this.currentBatchKey = ''
      this.loadList()
    },
    handleChangeStatus(row, status) {
      const label = status === 'PUBLISHED' ? '启用' : '停用'
      this.$modal.confirm(`确认${label}「${row.examName}」吗？`).then(() => {
        changeExamStatus(row.id, status).then(() => { this.$modal.msgSuccess(`${label}成功`); this.loadList() })
      }).catch(() => {})
    },
    statusLabel(s) {
      return { DRAFT: '待发布', PUBLISHED: '已发布', GRADING: '待批改', DISABLED: '已停用' }[s] || s
    },
    statusTag(s) {
      return { DRAFT: 'info', PUBLISHED: 'success', GRADING: 'warning', DISABLED: 'danger' }[s] || 'info'
    }
  }
}
</script>

<style lang="scss" scoped>
.page-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 18px; }
.eyebrow { color: #1764f5; font-size: 12px; letter-spacing: .05em; }
.title-line { display: flex; align-items: center; gap: 10px; }
.page-heading h2 { margin: 6px 0 8px; font-size: 22px; font-weight: 600; color: #1d2939; }
.page-heading p { margin: 0; max-width: 760px; color: #667085; font-size: 13px; line-height: 1.6; }
.heading-actions { flex: none; }
.query-form { margin-bottom: 14px; padding: 12px 14px 0; border: 1px solid #e7ecf3; background: #fff; }

.dm-card { margin-bottom: 14px; padding: 16px 18px 18px; background: #fff; border: 1px solid #e7ecf3; border-radius: 8px; }
.dm-head { display: flex; align-items: center; justify-content: space-between; gap: 14px; padding-bottom: 13px; margin-bottom: 4px; border-bottom: 1px solid #edf0f4; }
.dm-title { display: flex; align-items: center; gap: 9px; flex-wrap: wrap; }
.dm-title h3 { margin: 0; color: #1d2939; font-size: 16px; font-weight: 600; }
.dm-idx { display: inline-flex; width: 24px; height: 24px; align-items: center; justify-content: center; color: #1764f5; background: #e8f1fd; font-size: 12px; font-weight: 700; border-radius: 4px; }
.dm-idx.green { color: #1a7a58; background: #e4f5ee; }
.dm-hint { color: #98a2b3; font-size: 12px; }
.dm-meta { color: #667085; font-size: 12px; }
.dm-actions { display: flex; align-items: center; gap: 6px; flex: none; }
.exam-cell strong, .exam-cell small { display: block; }
.exam-cell strong { color: #1d2939; font-size: 14px; }
.exam-cell small { margin-top: 3px; color: #98a2b3; font-size: 12px; }
.num { color: #1764f5; font-size: 15px; }
.num.warn { color: #b54708; }
.muted { color: #98a2b3; font-size: 12px; }
.danger-text { color: #f56c6c; }

.dm-pair { display: grid; grid-template-columns: 1fr; gap: 14px; }
.dm-grid { display: grid; grid-template-columns: minmax(0, 4fr) minmax(0, 8fr); gap: 18px; padding-top: 14px; }
.dm-fields { display: grid; align-content: start; gap: 9px; }
.field { display: flex; align-items: baseline; justify-content: space-between; gap: 10px; padding: 8px 10px; background: #f8fafc; border-radius: 5px; }
.field span { flex: none; color: #8490a0; font-size: 12px; }
.field b { color: #1d2939; font-size: 12.5px; font-weight: 600; text-align: right; }
.dm-table-wrap { min-width: 0; }
.dm-sub-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.dm-sub-head h4 { margin: 0; color: #1d2939; font-size: 13px; font-weight: 600; }
.dm-callout { margin-top: 10px; padding: 10px 12px; border-radius: 6px; font-size: 12px; line-height: 1.6; }
.dm-callout.ok { color: #1a7a58; background: #e9f8f1; border: 1px solid #c7ecdc; }
.dm-callout.warn { color: #b54708; background: #fff8ec; border: 1px solid #f7e0bb; }
.dm-note { margin: 10px 0 0; color: #98a2b3; font-size: 11.5px; line-height: 1.7; }
.dm-note b { color: #667085; }
.dm-note code { padding: 1px 5px; color: #475467; background: #f2f4f7; border-radius: 3px; font-size: 11px; }
.dm-empty { display: flex; min-height: 150px; align-items: center; justify-content: center; flex-direction: column; gap: 8px; color: #98a2b3; }
.dm-empty i { color: #b7c1cc; font-size: 30px; }
.dm-empty span { font-size: 12.5px; }
.subject-box { padding: 13px 15px; background: #f8fafc; border: 1px solid #eef1f5; border-radius: 6px; }
.subject-label { margin-bottom: 7px; color: #1764f5; font-size: 12px; font-weight: 600; }
.subject-content { color: #1d2939; font-size: 13px; line-height: 1.7; white-space: pre-wrap; }
.subject-attach { margin-top: 9px; color: #475467; font-size: 12px; }
.subject-attach i { margin-right: 4px; }

.dm-notes { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }
.dm-note-card { padding: 15px 17px; background: #fff; border: 1px solid #e7ecf3; border-radius: 8px; }
.dm-note-card h4 { margin: 0 0 8px; color: #1d2939; font-size: 13.5px; font-weight: 600; }
.dm-note-card p { margin: 0; color: #667085; font-size: 12px; line-height: 1.7; }
.dm-note-card code { padding: 1px 5px; color: #475467; background: #f2f4f7; border-radius: 3px; font-size: 11px; }
.attachment-tip { margin-top: 6px; color: #23966f; font-size: 12px; }
.attachment-tip i { margin-right: 4px; }

@media (max-width: 1100px) {
  .dm-grid { grid-template-columns: 1fr; }
  .dm-notes { grid-template-columns: 1fr; }
}
</style>
