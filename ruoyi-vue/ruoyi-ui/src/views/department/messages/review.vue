<template>
  <div class="dept-page">
    <div class="dept-breadcrumb">
      任务与通知 <span>/</span> <b>任务批阅</b>
    </div>

    <div class="dept-heading">
      <div>
        <span class="eyebrow">TASK REVIEW</span>
        <h1>任务批阅</h1>
        <p>
          本部门任务的<b>完成情况统计</b>与<b>批阅工作台</b>：左边选任务，右边直接看提交明细、内联批阅、
          下载资料、参与讨论 —— 全程在本页完成，不再弹窗。
          任务的新建/发布/结束仍在「任务管理」。
        </p>
      </div>
      <div class="dept-heading-actions">
        <span class="dbadge green">真实数据</span>
        <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="refresh">刷新</el-button>
      </div>
    </div>

    <!-- ① 完成情况统计 -->
    <div class="dkpi-grid c6" style="margin-bottom:16px">
      <div class="dkpi">
        <div class="dkpi-label"><span class="d" style="background:#1764f5" />任务总数</div>
        <div class="dkpi-val" :class="{ mute: !stat.total }">{{ stat.total }}</div>
        <div class="dkpi-sub">进行中 {{ stat.published }} · 已结束 {{ stat.ended }} · 草稿 {{ stat.draft }}</div>
      </div>
      <div class="dkpi">
        <div class="dkpi-label"><span class="d" style="background:#f79009" />待批阅</div>
        <div class="dkpi-val" :class="{ mute: !stat.pending }">{{ stat.pending }}</div>
        <div class="dkpi-sub" :class="{ warn: stat.pending }">份作业已提交、等你批</div>
      </div>
      <div class="dkpi">
        <div class="dkpi-label"><span class="d" style="background:#f04438" />逾期任务</div>
        <div class="dkpi-val" :class="{ mute: !stat.overdue }">{{ stat.overdue }}</div>
        <div class="dkpi-sub" :class="{ bad: stat.overdue }">截止已过且仍在进行中</div>
      </div>
      <div class="dkpi">
        <div class="dkpi-label"><span class="d" style="background:#12b76a" />分配人次</div>
        <div class="dkpi-val" :class="{ mute: !stat.assigned }">{{ stat.assigned }}</div>
        <div class="dkpi-sub">已提交 {{ stat.submitted }} 人次</div>
      </div>
      <div class="dkpi">
        <div class="dkpi-label"><span class="d" style="background:#7a5af8" />整体完成率</div>
        <div class="dkpi-val sm" :class="{ mute: stat.rate === null }">
          {{ stat.rate === null ? '--' : stat.rate + '%' }}
        </div>
        <div class="dbar-mini"><i :class="rateTone" :style="{ width: (stat.rate || 0) + '%' }" /></div>
      </div>
      <div class="dkpi">
        <div class="dkpi-label"><span class="d" style="background:#0e7490" />讨论 / 资料</div>
        <div class="dkpi-val sm">{{ stat.posts }} / {{ stat.files }}</div>
        <div class="dkpi-sub">累计讨论条数与资料份数</div>
      </div>
    </div>

    <div class="dgrid">
      <!-- ② 任务清单 -->
      <div class="dcard c5">
        <div class="dcard-h">
          <div class="tt"><span class="idx">选</span><h3>选择任务</h3></div>
          <span class="hint-text">{{ visibleTasks.length }} / {{ tasks.length }} 条</span>
        </div>

        <!-- 搜索 + 岗位筛选：任务多了以后靠状态 chips 找不到 -->
        <div class="rv-filters">
          <el-input
            v-model="taskKeyword" size="small" clearable placeholder="搜任务名称"
            prefix-icon="el-icon-search"
          />
          <el-select v-model="taskPositionId" size="small" clearable placeholder="全部岗位" style="width:118px">
            <el-option v-for="p in positions" :key="p.id" :label="p.positionName" :value="Number(p.id)" />
          </el-select>
        </div>

        <div class="dchips" style="margin:0 0 10px">
          <span v-for="tab in filterTabs" :key="tab.key" class="dchip"
                :class="{ on: listFilter === tab.key }" @click="listFilter = tab.key">
            {{ tab.label }} <span class="n">{{ tab.count }}</span>
          </span>
        </div>

        <div v-loading="loading" class="rv-list">
          <div v-for="t in visibleTasks" :key="t.id" class="rv-item" :class="{ on: cur && cur.id === t.id }"
               @click="selectTask(t)">
            <div class="rv-item-top">
              <b class="rv-name">{{ t.taskName }}</b>
              <span class="dbadge" :class="statusTone(t.status)">{{ statusText(t.status) }}</span>
            </div>
            <div class="rv-item-sub">
              {{ t.positionName || '—' }} · 截止 {{ shortTime(t.deadline) }}
              <b v-if="isOverdue(t)" class="bad">已逾期</b>
            </div>
            <div class="rv-item-bot">
              <span class="rv-mini">分配 {{ t.assignedCount || 0 }} / 提交 {{ t.submittedCount || 0 }}</span>
              <span v-if="t.pendingReviewCount" class="dbadge orange">待批 {{ t.pendingReviewCount }}</span>
              <span v-if="t.discussionEnabled === 1 && t.postCount" class="dbadge blue">讨论 {{ t.postCount }}</span>
              <span v-if="t.attachmentCount" class="dbadge gray">资料 {{ t.attachmentCount }}</span>
            </div>
          </div>

          <div v-if="!loading && !visibleTasks.length" class="dempty small">
            <i class="el-icon-tickets" />
            <strong>没有符合条件的任务</strong>
            <span>{{ emptyHint }}</span>
          </div>
        </div>
      </div>

      <!-- ③ 任务工作区 -->
      <div class="dcard c7">
        <template v-if="cur">
          <div class="dcard-h">
            <div class="tt">
              <span class="idx">工</span>
              <h3>{{ cur.taskName }}</h3>
            </div>
            <div class="dchips" style="margin:0">
              <el-button v-if="cur.status === 'PUBLISHED'" size="mini" @click="urge">催促未交</el-button>
              <el-button size="mini" @click="goManage">去任务管理</el-button>
            </div>
          </div>

          <div class="rv-meta">
            <span class="dbadge" :class="statusTone(cur.status)">{{ statusText(cur.status) }}</span>
            <span>{{ cur.positionName || '—' }}</span>
            <span>截止 {{ shortTime(cur.deadline) }}</span>
            <span v-if="cur.needAssignment === 1 && cur.allowResubmit === 0" class="dbadge orange">仅一次提交</span>
            <span v-if="isOverdue(cur)" class="bad">已逾期</span>
          </div>

          <!-- 页签（不再弹窗，全部在本页内联） -->
          <div class="dchips rv-tabs">
            <span class="dchip" :class="{ on: tab === 'submit' }" @click="tab = 'submit'">
              提交与批阅 <span class="n">{{ board.length }}</span>
            </span>
            <span class="dchip" :class="{ on: tab === 'files' }" @click="tab = 'files'">
              资料 <span class="n">{{ cur.attachmentCount || 0 }}</span>
            </span>
            <span class="dchip" :class="{ on: tab === 'discuss' }" @click="tab = 'discuss'">
              讨论 <span class="n">{{ cur.postCount || 0 }}</span>
            </span>
          </div>

          <!-- 页签一：提交与批阅 -->
          <div v-show="tab === 'submit'" class="rv-pane">
            <!-- 按实习生筛选：一个组可能十几个人，找人靠翻表格太慢 -->
            <div class="board-filters">
              <el-input
                v-model="boardKeyword" size="small" clearable placeholder="搜实习生姓名"
                prefix-icon="el-icon-search" style="width:172px"
              />
              <span class="hint-text">
                共 {{ board.length }} 人<template v-if="boardKeyword">，匹配 {{ visibleBoard.length }} 人</template>
              </span>
            </div>

            <div v-if="boardPending" class="board-tip">
              <i class="el-icon-warning-outline" />
              有 <b>{{ boardPending }}</b> 份作业待批阅，点对应行的「处理批阅」在本页处理。
            </div>

            <div v-loading="boardLoading" class="rv-table-wrap">
              <table class="dtbl">
                <thead>
                  <tr>
                    <th>实习生</th>
                    <th style="width:86px">分配</th>
                    <th style="width:80px">提交</th>
                    <th style="width:104px">提交时间</th>
                    <th>批阅记录</th>
                    <th style="width:96px">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="b in visibleBoard" :key="b.assignmentId" :class="{ 'row-on': reviewRow && reviewRow.assignmentId === b.assignmentId }">
                    <td class="strong">{{ b.nickName || b.userName }}</td>
                    <td>
                      <span class="dbadge" :class="assignTone(b.assignStatus)">{{ assignText(b.assignStatus) }}</span>
                    </td>
                    <td>
                      <span class="dbadge" :class="b.submissionStatus === 'SUBMITTED' ? 'orange' : (b.submissionStatus === 'REVIEWED' ? 'green' : 'gray')">
                        {{ b.submissionStatus ? (b.submissionStatus === 'SUBMITTED' ? '待批阅' : '已批阅') : '未交' }}
                      </span>
                      <!-- 提交次数：重复提交的人一眼能看出来（内容只以最近一次为准） -->
                      <div v-if="b.submissionCount > 1" class="sub-times">已交 {{ b.submissionCount }} 次</div>
                    </td>
                    <td>{{ shortTime(b.submitTime) }}</td>
                    <td>
                      <!-- 批阅记录：谁、什么时候、结果、批语 —— 批完必须留痕 -->
                      <template v-if="reviewed(b)">
                        <div class="rv-rec">
                          <span class="dbadge" :class="reviewTone(b)">{{ reviewText(b) }}</span>
                          <span class="mut">{{ b.reviewerName || '—' }} · {{ shortTime(b.reviewTime) }}</span>
                        </div>
                        <div v-if="b.reviewComment" class="rv-cmt" :title="b.reviewComment">{{ b.reviewComment }}</div>
                        <div v-else class="mut">（未填批语）</div>
                      </template>
                      <span v-else-if="b.submissionId" class="dbadge orange">待批阅</span>
                      <span v-else class="hint-text">—</span>
                    </td>
                    <td>
                      <div class="acts">
                        <!-- 统一叫「处理批阅」：点进去是同一个工作区（待批阅给表单、已批阅给记录卡 + 可重新批阅）。
                             待批阅/已批阅的区分由左边「提交」列的状态徽标识承担，不必在按钮上再分叉。 -->
                        <el-button v-if="b.submissionId" type="text" @click="openReview(b)">处理批阅</el-button>
                        <!-- 未交的人直接在这里催办，不用退出去找「催促未交」 -->
                        <el-button v-else type="text" @click="urgeOne(b)">催办</el-button>
                      </div>
                    </td>
                  </tr>
                  <tr v-if="!boardLoading && !visibleBoard.length">
                    <td colspan="6">
                      <div class="dempty small">
                        <i class="el-icon-user" />
                        <strong>{{ boardKeyword ? '没有匹配的实习生' : '暂无分配' }}</strong>
                        <span>
                          {{ boardKeyword ? '换个姓名关键字试试。' : '任务发布后会列出全部被分配的人（含未提交者）。' }}
                        </span>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- 页签二：资料 -->
          <div v-show="tab === 'files'" class="rv-pane">
            <TaskAttachments
              ref="attBox"
              :task-id="cur.id"
              :editable="cur.status !== 'ENDED'"
              @changed="refresh"
            />
            <p v-if="cur.status === 'ENDED'" class="hint-text" style="margin:10px 0 0">
              任务已结束，资料转为只读 —— 仍可下载，但不能增删。
            </p>
          </div>

          <!-- 页签三：讨论 -->
          <div v-show="tab === 'discuss'" class="rv-pane">
            <TaskDiscussionBody
              v-if="cur.discussionEnabled === 1"
              ref="discBox"
              :task-id="cur.id"
              bare
              @changed="refresh"
            />
            <div v-else class="dempty small">
              <i class="el-icon-chat-line-square" />
              <strong>该任务未开启讨论区</strong>
              <span>需要在「任务管理」里为该任务勾选「启用讨论区」。</span>
            </div>
          </div>
        </template>

        <div v-else class="dempty tall">
          <i class="el-icon-mouse" />
          <strong>从左边选一条任务</strong>
          <span>这里会显示提交明细、内联批阅、任务资料与讨论区。</span>
        </div>
      </div>
    </div>

    

    <!-- 处理批阅：**大弹窗工作区**（原来是表格下方的内联面板，c7 卡片里太挤）。
         素材 + 在线预览 + 历次提交 + 批语都在这个弹窗里一次性完成。 -->
    <el-dialog
      :title="reviewRow ? '处理批阅 · ' + (reviewRow.nickName || reviewRow.userName) : '处理批阅'"
      :visible.sync="reviewVisible"
      width="1000px"
      top="5vh"
      append-to-body
      @closed="onReviewClosed"
    >
      <div v-if="reviewRow" class="rv-dlg">
        <div class="rv-dlg-meta">
          <span class="dbadge" :class="reviewTone(reviewRow)">{{ reviewText(reviewRow) }}</span>
          <span class="hint-text">
            第 {{ reviewRow.version }} 次提交 · 提交于 {{ shortTime(reviewRow.submitTime) }}
            <template v-if="reviewRow.submissionCount > 1"> · 共提交 {{ reviewRow.submissionCount }} 次</template>
          </span>
          <span class="hint-text rv-dlg-task">{{ cur && cur.taskName }}</span>
        </div>

        <!-- ① 批阅记录：已批阅时先把「谁在什么时候怎么批的」摆出来 -->
        <div v-if="reviewed(reviewRow) && !reEditing" class="rec-box">
          <div class="rec-top">
            <span class="dbadge" :class="reviewTone(reviewRow)">
              {{ reviewRow.reviewStatus === 'PASSED' ? '已通过' : '已退回' }}
            </span>
            <span class="rec-who">
              <i class="el-icon-user" /> {{ reviewRow.reviewerName || '—' }}
              <span class="mut"> · {{ shortTime(reviewRow.reviewTime) }} 批阅</span>
            </span>
            <el-button size="mini" type="text" class="rec-re" @click="startReEdit">重新批阅</el-button>
          </div>
          <p class="rec-cmt">{{ reviewRow.reviewComment || '（未填写批语）' }}</p>
        </div>

        <!-- ② 实习生提交的材料：批阅前、批阅后都能看 -->
        <div class="review-body">
          <div class="sub-title"><i class="el-icon-document" /> 实习生提交的材料</div>
          <p v-if="reviewRow.content" class="review-text">{{ reviewRow.content }}</p>
          <div v-if="reviewRow.fileUrl" class="sub-file">
            <i class="el-icon-paperclip" />
            <a :href="fileUrl(reviewRow.fileUrl)" target="_blank" rel="noopener" download>
              {{ reviewRow.fileName || '作业附件' }}
            </a>
            <span v-if="canPreviewFile(reviewRow)" class="sub-acts">
              <el-button size="mini" type="text" @click="togglePreview(mainFile)">
                {{ isPreviewing(mainFile) ? '收起预览' : '在线预览' }}
              </el-button>
              <!-- 能原生显示的（pdf/txt/图片）才叫「新窗口」；docx/pptx 等新窗口=下载，就叫下载 -->
              <el-button v-if="canNativeOpen(reviewRow)" size="mini" type="text" @click="openFile(reviewRow.fileUrl)">新窗口</el-button>
              <el-button v-else size="mini" type="text" @click="downloadFile(reviewRow.fileUrl)">下载</el-button>
            </span>
          </div>
          <p v-if="!reviewRow.content && !reviewRow.fileUrl" class="hint-text" style="margin:0">（无内容）</p>
        </div>

        <!-- ③ 在线预览（pdf / txt / docx / pptx / 图片）—— 弹窗里给足高度 -->
        <div v-if="previewFile" class="preview-box">
          <div class="pv-head">
            <i class="el-icon-view" />
            <b :title="previewFile.fileName">{{ previewFile.fileName }}</b>
            <span class="pv-x" @click="previewFile = null"><i class="el-icon-close" /> 关闭预览</span>
          </div>
          <FilePreview :url="fileUrl(previewFile.fileUrl)" :file-name="previewFile.fileName" height="560px" />
        </div>

        <!-- ④ 历次提交：次数与批阅痕迹全程留痕（内容只保留最近一次，见 §十九） -->
        <div class="hist">
          <div class="hist-head" @click="showHistory = !showHistory">
            <i class="el-icon-time" />
            <b>历次提交（{{ history.length }}）</b>
            <span class="hint-text">批阅结果全程留痕；<b>内容与附件只保留最近一次</b>（旧版本已清理）</span>
            <i class="hist-arrow" :class="showHistory ? 'el-icon-arrow-up' : 'el-icon-arrow-down'" />
          </div>
          <div v-show="showHistory" v-loading="histLoading" class="hist-body">
            <div v-for="h in history" :key="h.submissionId" class="hist-item"
                 :class="{ on: h.version === reviewRow.version }">
              <div class="hist-line">
                <span class="dbadge" :class="h.version === reviewRow.version ? 'blue' : 'gray'">v{{ h.version }}</span>
                <span class="hint-text">{{ shortTime(h.submitTime) }}</span>
                <span class="dbadge" :class="reviewTone(h)">{{ reviewText(h) }}</span>
                <span v-if="h.reviewerName" class="hint-text">{{ h.reviewerName }} · {{ shortTime(h.reviewTime) }}</span>
                <span v-if="h.version === reviewRow.version" class="hint-text">（当前查看）</span>
              </div>
              <p v-if="h.content" class="hist-text">{{ h.content }}</p>
              <div v-if="h.fileUrl" class="sub-file">
                <i class="el-icon-paperclip" />
                <a :href="fileUrl(h.fileUrl)" target="_blank" rel="noopener" download>{{ h.fileName || '附件' }}</a>
                <span v-if="canPreviewFile(h)" class="sub-acts">
                  <el-button size="mini" type="text" @click="togglePreview(histFile(h))">
                    {{ isPreviewing(histFile(h)) ? '收起预览' : '在线预览' }}
                  </el-button>
                  <el-button v-if="canNativeOpen(h)" size="mini" type="text" @click="openFile(h.fileUrl)">新窗口</el-button>
                  <el-button v-else size="mini" type="text" @click="downloadFile(h.fileUrl)">下载</el-button>
                </span>
              </div>
              <!-- 旧版本的内容与附件只保留最近一次，所以这里说清楚，别让人以为数据丢了 -->
              <p v-if="!h.content && !h.fileUrl" class="hist-gone">
                <i class="el-icon-folder-deleted" /> 旧版本的内容与附件已清理（只保留最近一次提交）
              </p>
              <p v-if="h.reviewComment" class="hist-cmt">批语：{{ h.reviewComment }}</p>
            </div>
            <p v-if="!histLoading && !history.length" class="hint-text" style="margin:0">暂无提交记录</p>
          </div>
        </div>

        <!-- ⑤ 批阅表单：待批阅，或点了「重新批阅」 -->
        <template v-if="isEditable">
          <el-input v-model="reviewComment" type="textarea" :rows="3" size="small"
                    placeholder="给实习生的反馈（通过或退回都会随回执一起发给他）" />
          <div class="review-foot">
            <el-radio-group v-model="passFlag" size="mini">
              <el-radio-button label="PASS">通过</el-radio-button>
              <el-radio-button label="REJECT">退回重做</el-radio-button>
            </el-radio-group>
            <span class="hint-text">提交后回执会自动通知该实习生</span>
          </div>
        </template>
        <p v-else class="hint-text" style="margin:0">这条提交已批阅完成，上面的记录会一直保留；需要改判点右上角「重新批阅」。</p>
      </div>

      <span slot="footer">
        <template v-if="reviewRow && isEditable">
          <el-button size="small" @click="cancelEdit">{{ reEditing ? '放弃本次修改' : '取消' }}</el-button>
          <el-button size="small" type="primary" :loading="reviewing" @click="doReview">提交批阅</el-button>
        </template>
        <el-button v-else size="small" @click="reviewVisible = false">关闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
/**
 * 部门管理员端 · 任务批阅（2026-09-18）
 *
 * 把原来散在「任务管理」里的一堆弹窗（提交情况 / 批阅 / 资料 / 讨论）收成一个**独立工作页**：
 * 左边选任务，右边内联切换「提交与批阅 / 资料 / 讨论」。
 *
 * 三个设计点：
 *  ① **统计口径就是任务列表本身** —— `listTasks` 已经带回 assignedCount / submittedCount /
 *     pendingReviewCount / postCount / attachmentCount，所以 KPI 全部前端聚合即可，
 *     **不需要新接口**，也不会出现"统计与列表对不上"。
 *  ② **批阅是内联面板而不是弹窗** —— 点某行「批阅」在表格下方就地展开批语与结果单选，
 *     批完原地刷新表格，视线不跳走。
 *  ③ 支持 `?taskId=N&tab=submit|files|discuss` —— 消息中心的「去处理」、以及「任务管理」
 *     里的 讨论/资料 角标都跳到这页并直接定位，**这就是弹窗被取代的那条链路**。
 */
import { listTasks, listSubmissions, listSubmissionHistory, reviewSubmission } from '@/api/business/task'
import { sendMessage } from '@/api/business/message'
import { listCoursePositions } from '@/api/business/course'
import { canPreview, nativeOpen } from '@/utils/filePreview'
import TaskAttachments from '@/components/TaskAttachments'
import TaskDiscussionBody from '@/components/TaskDiscussion/TaskDiscussionBody'
import FilePreview from '@/components/FilePreview'

const STATUS_TEXT = { DRAFT: '草稿', PUBLISHED: '已发布', ENDED: '已结束' }
const ASSIGN_TEXT = { NOT_STARTED: '未开始', IN_PROGRESS: '进行中', DONE: '已完成', OVERDUE: '已逾期' }
const TABS = ['submit', 'files', 'discuss']

export default {
  name: 'DeptTaskReview',
  components: { TaskAttachments, TaskDiscussionBody, FilePreview },
  data() {
    return {
      loading: false,
      boardLoading: false,
      reviewing: false,
      tasks: [],
      cur: null,
      board: [],
      tab: 'submit',
      listFilter: 'ALL',
      /** 任务清单的搜索 / 岗位筛选（纯前端过滤，列表已整份加载） */
      taskKeyword: '',
      taskPositionId: null,
      positions: [],
      /** 提交明细按实习生姓名过滤 */
      boardKeyword: '',
      /** 处理批阅：大弹窗的开关（弹窗内容见模板末尾） */
      reviewVisible: false,
      reviewRow: null,
      reviewComment: '',
      passFlag: 'PASS',
      /** 已批阅的记录：点「重新批阅」才切回表单 */
      reEditing: false,
      /** 正在预览的文件（同时只预览一个，避免多份大文件一起解析） */
      previewFile: null,
      /** 该分配的历次提交（批阅记录的一部分：旧版本 + 当时的批语） */
      history: [],
      histLoading: false,
      showHistory: false
    }
  },
  computed: {
    /** 完成情况统计：全部由任务列表聚合，口径与「任务管理」必然一致 */
    stat() {
      const ts = this.tasks
      const sum = (k) => ts.reduce((a, t) => a + (Number(t[k]) || 0), 0)
      const assigned = sum('assignedCount')
      const submitted = sum('submittedCount')
      const published = ts.filter(t => t.status === 'PUBLISHED').length
      return {
        total: ts.length,
        draft: ts.filter(t => t.status === 'DRAFT').length,
        published,
        ended: ts.filter(t => t.status === 'ENDED').length,
        pending: sum('pendingReviewCount'),
        overdue: ts.filter(t => this.isOverdue(t)).length,
        assigned,
        submitted,
        // 分母 0 一律给 null（显示 --），不伪装成 0%
        rate: assigned ? Math.round((submitted / assigned) * 100) : null,
        posts: sum('postCount'),
        files: sum('attachmentCount')
      }
    },
    rateTone() {
      const r = this.stat.rate
      if (r === null) return ''
      if (r >= 80) return 'g'
      if (r >= 50) return 'o'
      return 'r'
    },
    filterTabs() {
      const ts = this.searchedTasks
      return [
        { key: 'ALL', label: '全部', count: ts.length },
        { key: 'PENDING', label: '待批阅', count: ts.filter(t => t.pendingReviewCount > 0).length },
        { key: 'PUBLISHED', label: '进行中', count: ts.filter(t => t.status === 'PUBLISHED').length },
        { key: 'ENDED', label: '已结束', count: ts.filter(t => t.status === 'ENDED').length }
      ]
    },
    /** 先过「搜索词 + 岗位」，chips 计数与列表都基于它，避免计数和列表对不上 */
    searchedTasks() {
      const kw = (this.taskKeyword || '').trim().toLowerCase()
      return this.tasks.filter(t => {
        if (this.taskPositionId && Number(t.positionId) !== Number(this.taskPositionId)) return false
        if (kw && String(t.taskName || '').toLowerCase().indexOf(kw) < 0) return false
        return true
      })
    },
    visibleTasks() {
      const ts = this.searchedTasks
      if (this.listFilter === 'ALL') return ts
      if (this.listFilter === 'PENDING') return ts.filter(t => t.pendingReviewCount > 0)
      return ts.filter(t => t.status === this.listFilter)
    },
    emptyHint() {
      if (this.taskKeyword || this.taskPositionId) return '当前搜索/岗位筛选下没有任务，试试清空筛选条件。'
      if (this.listFilter === 'PENDING') return '当前没有待批阅的作业，辛苦。'
      return '换个筛选条件，或去「任务管理」发布一条。'
    },
    /** 提交明细按实习生姓名过滤 */
    visibleBoard() {
      const kw = (this.boardKeyword || '').trim().toLowerCase()
      if (!kw) return this.board
      return this.board.filter(b => String((b.nickName || b.userName) || '').toLowerCase().indexOf(kw) > -1)
    },
    /** 按已加载的看板实时算，比列表缓存准 */
    boardPending() {
      return this.board.filter(b => b.submissionStatus === 'SUBMITTED').length
    },
    /** 是否处于可编辑（批阅表单）状态：未批阅，或已批阅但点了「重新批阅」 */
    isEditable() {
      return !!this.reviewRow && (!this.reviewed(this.reviewRow) || this.reEditing)
    },
    /** 当前这条提交的附件描述符（预览判定与「是否正在预览」都靠它的 id 比对） */
    mainFile() {
      const b = this.reviewRow
      if (!b || !b.fileUrl) return null
      return { id: 'main-' + b.submissionId, fileName: b.fileName, fileUrl: b.fileUrl }
    }
  },
  watch: {
    // 同路由只换 query 时 created 不重跑
    '$route.query': {
      handler() { this.loadTasks().then(() => this.applyRoute()) }
    }
  },
  created() {
    this.loadPositions()
    this.loadTasks().then(() => this.applyRoute())
  },
  methods: {
    /** 岗位下拉：/business/course/positions 已按当前部门过滤 */
    loadPositions() {
      listCoursePositions().then(res => { this.positions = res.data || [] }).catch(() => { this.positions = [] })
    },
    // ---------------------------------------------------------------- 数据
    loadTasks() {
      this.loading = true
      return listTasks({ pageNum: 1, pageSize: 100 }).then(res => {
        this.tasks = (res.data || {}).rows || []
      }).catch(() => { this.tasks = [] }).finally(() => { this.loading = false })
    },
    refresh() {
      const keep = this.cur ? this.cur.id : null
      return this.loadTasks().then(() => {
        const again = keep ? this.tasks.find(t => t.id === keep) : null
        if (again) this.selectTask(again, true)
      })
    },
    /** 响应 ?taskId=N&tab=xxx：定位到某条任务的某个页签 */
    applyRoute() {
      const q = this.$route.query || {}
      if (TABS.indexOf(q.tab) >= 0) this.tab = q.tab
      if (q.taskId) {
        const t = this.tasks.find(x => String(x.id) === String(q.taskId))
        if (t) { this.selectTask(t); return }
        this.$message.info('未找到该任务，可能不属于你负责的岗位')
        return
      }
      // 没指定任务时：优先选中「有待批阅」的那条，省一次点击
      if (!this.cur && this.tasks.length) {
        const hit = this.tasks.find(t => t.pendingReviewCount > 0) || this.tasks[0]
        this.selectTask(hit)
      }
    },
    selectTask(t, keepTab) {
      this.cur = t
      this.reviewRow = null
      if (!keepTab) this.tab = this.tab || 'submit'
      this.loadBoard(t.id)
      this.$nextTick(() => {
        if (this.$refs.attBox) this.$refs.attBox.reload()
        if (this.$refs.discBox) this.$refs.discBox.reload()
      })
    },
    loadBoard(taskId) {
      this.boardLoading = true
      return listSubmissions(taskId).then(res => {
        this.board = res.data || []
      }).catch(() => { this.board = [] }).finally(() => { this.boardLoading = false })
    },

    // ---------------------------------------------------------------- 处理批阅（大弹窗）
    openReview(b) {
      this.reviewRow = b
      // 未批阅 → 直接进表单；已批阅 → 先展示记录，材料与预览也都在这一屏
      this.reEditing = !this.reviewed(b)
      this.reviewComment = b.reviewComment || ''
      this.passFlag = b.reviewStatus === 'REJECTED' ? 'REJECT' : 'PASS'
      // 预览**默认不展开**：一打开弹窗就铺一块大预览会盖住材料正文，需要时自己点「在线预览」
      this.previewFile = null
      // 多次提交时默认展开历次提交（批阅人最需要看「上次为什么被退回」）
      this.showHistory = (b.submissionCount || 0) > 1
      this.loadHistory(b.assignmentId)
      this.reviewVisible = true
    },
    /** 只负责关弹窗；状态清理由 @closed 统一做，避免关闭动画期间内容先变空 */
    closeReview() {
      this.reviewVisible = false
    },
    onReviewClosed() {
      this.reviewRow = null
      this.reviewComment = ''
      this.previewFile = null
      this.reEditing = false
      this.history = []
      this.showHistory = false
    },
    /** 已批阅的记录 → 切回批阅表单（重新批阅） */
    startReEdit() {
      this.reEditing = true
      this.reviewComment = this.reviewRow.reviewComment || ''
      this.passFlag = this.reviewRow.reviewStatus === 'REJECTED' ? 'REJECT' : 'PASS'
    },
    cancelEdit() {
      if (this.reEditing) {
        this.reEditing = false
        this.reviewComment = this.reviewRow.reviewComment || ''
      } else {
        this.closeReview()
      }
    },
    loadHistory(assignmentId) {
      this.history = []
      if (!assignmentId) return
      this.histLoading = true
      listSubmissionHistory(assignmentId).then(res => {
        this.history = res.data || []
      }).catch(() => { this.history = [] }).finally(() => { this.histLoading = false })
    },
    histFile(h) {
      if (!h || !h.fileUrl) return null
      return { id: 'h-' + h.submissionId, fileName: h.fileName, fileUrl: h.fileUrl }
    },
    isPreviewing(f) {
      return !!f && !!this.previewFile && this.previewFile.id === f.id
    },
    togglePreview(f) {
      if (!f) return
      this.previewFile = this.isPreviewing(f) ? null : f
    },
    openFile(u) {
      const url = this.fileUrl(u)
      if (url && url !== '#') window.open(url, '_blank', 'noopener')
    },
    /** 真正的下载：同源时用 <a download> 触发，比 window.open 更确定 */
    downloadFile(u) {
      const url = this.fileUrl(u)
      if (!url || url === '#') return
      const a = document.createElement('a')
      a.href = url
      a.download = ''
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
    },
    /** 浏览器能否原生在新标签页显示（pdf/txt/图片）—— 决定按钮叫「新窗口」还是「下载」 */
    canNativeOpen(b) {
      return !!b && !!b.fileUrl && nativeOpen(b.fileName, b.fileUrl)
    },
    /**
     * 是否「已批阅」。
     *
     * ⚠️ `reviewStatus` 有<b>三个</b>值：`PENDING`（还没批）/ `PASSED` / `REJECTED` ——
     * 所以**不能拿它当布尔用**（`'PENDING'` 也是真值，会把待批阅的提交显示成「退回」，
     * 而且批语输入框会不出现）。这一处最初就是这么写错的，靠真浏览器复现才发现。
     */
    reviewed(x) {
      return !!x && (x.reviewStatus === 'PASSED' || x.reviewStatus === 'REJECTED')
    },
    reviewText(x) {
      if (!this.reviewed(x)) return '待批阅'
      return x.reviewStatus === 'PASSED' ? '通过' : '退回'
    },
    reviewTone(x) {
      if (!this.reviewed(x)) return 'orange'
      return x.reviewStatus === 'PASSED' ? 'green' : 'red'
    },
    /** 该提交的附件能否在线预览（pdf/txt/docx/pptx/图片） */
    canPreviewFile(b) {
      return !!b && !!b.fileUrl && canPreview(b.fileName, b.fileUrl)
    },
    doReview() {
      if (!this.reviewRow) return
      const aid = this.reviewRow.assignmentId
      this.reviewing = true
      reviewSubmission({ id: this.reviewRow.submissionId, reviewComment: this.reviewComment }, this.passFlag)
        .then(res => {
          this.$message.success(res.msg || '已完成批阅')
          this.reEditing = false
          this.loadHistory(aid)
          // 重新拉看板并把面板指向刷新后的那一行 —— 批阅记录要立刻显示出来
          return this.loadBoard(this.cur.id).then(() => {
            const fresh = this.board.find(x => x.assignmentId === aid)
            if (fresh) this.reviewRow = fresh
            this.loadTasks()   // 左侧列表与 KPI 的「待批阅」角标跟着更新
          })
        }).catch(() => {}).finally(() => { this.reviewing = false })
    },

    // ---------------------------------------------------------------- 其它动作
    /** 催促未交：给未提交者发定向通知（与「任务管理」同一机制） */
    urge() {
      const targets = this.board.filter(b => !b.submissionId).map(b => b.userId)
      if (!targets.length) { this.$message.info('该任务所有人都已提交，无需催促'); return }
      this.$confirm('将向 ' + targets.length + ' 位未提交者推送提醒通知，是否继续？', '催促未交', {
        confirmButtonText: '推送提醒', cancelButtonText: '取消', type: 'warning'
      }).then(() => sendMessage({
        msgType: 'URGE', scopeType: 'USER', targetUserIds: targets,
        title: '【催办】任务待提交：' + this.cur.taskName,
        content: '你的任务尚未提交，截止时间：' + (this.cur.deadline || '—') + '，请尽快处理。',
        bizType: 'TASK', bizId: this.cur.id
      }).then(res => {
        this.$message.success('已推送催办提醒')
      }).catch(() => {})).catch(() => {})
    },
    /** 单个人催办（未交的行）—— 与「催促未交」同一机制，只是收件人只有一个 */
    urgeOne(b) {
      if (!b || !b.userId) return
      const name = b.nickName || b.userName || '该实习生'
      this.$confirm('将给「' + name + '」推送一条催办提醒，是否继续？', '催办', {
        confirmButtonText: '推送提醒', cancelButtonText: '取消', type: 'warning'
      }).then(() => sendMessage({
        msgType: 'URGE', scopeType: 'USER', targetUserIds: [b.userId],
        title: '【催办】任务待提交：' + this.cur.taskName,
        content: '你的任务尚未提交，截止时间：' + (this.cur.deadline || '—') + '，请尽快处理。',
        bizType: 'TASK', bizId: this.cur.id
      }).then(res => {
        this.$message.success('已推送催办提醒')
      }).catch(() => {})).catch(() => {})
    },
    goManage() {
      this.$router.push('/department/messages/tasks').catch(() => {})
    },
    /** 附件下载地址：必须走 VUE_APP_BASE_API 前缀（dev 下由代理转发到 8080） */
    fileUrl(u) {
      if (!u) return '#'
      return process.env.VUE_APP_BASE_API + u
    },

    // ---------------------------------------------------------------- 展示
    isOverdue(t) {
      if (t.status !== 'PUBLISHED' || !t.deadline) return false
      const ms = new Date(String(t.deadline).replace(/-/g, '/')).getTime()
      return !isNaN(ms) && ms < Date.now()
    },
    statusText(s) { return STATUS_TEXT[s] || s },
    statusTone(s) {
      return { DRAFT: 'gray', PUBLISHED: 'blue', ENDED: 'green' }[s] || 'gray'
    },
    assignText(s) { return ASSIGN_TEXT[s] || s },
    assignTone(s) {
      return { DONE: 'green', IN_PROGRESS: 'blue', OVERDUE: 'red' }[s] || 'gray'
    },
    shortTime(v) {
      if (!v) return '—'
      return String(v).replace('T', ' ').slice(5, 16)
    }
  }
}
</script>

<style lang="scss" scoped>
@import '~@/assets/styles/department-module.scss';

/* ---- 左侧任务清单 ---- */
.rv-filters { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.rv-filters .el-input { flex: 1; min-width: 0; }
.board-filters { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.rv-list { max-height: 560px; overflow-y: auto; margin: 0 -4px; padding: 0 4px; }
.rv-item {
  padding: 10px 12px;
  margin-bottom: 8px;
  background: #fff;
  border: 1px solid #eef1f6;
  border-left: 3px solid #e4e9f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all .15s;

  &:hover { border-color: #bcd4fb; background: #f7faff; }
  &.on { border-left-color: #1764f5; background: #f7faff; box-shadow: 0 3px 12px rgba(23, 100, 245, .08); }
}
.rv-item-top { display: flex; align-items: center; gap: 8px; }
.rv-name {
  flex: 1;
  min-width: 0;
  color: #1d2939;
  font-size: 13px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.rv-item-sub { margin-top: 5px; color: #98a2b3; font-size: 11.5px; }
.rv-item-bot { display: flex; flex-wrap: wrap; align-items: center; gap: 6px; margin-top: 7px; }
.rv-mini { color: #667085; font-size: 11.5px; }
.bad { color: #b42318; font-size: 11.5px; margin-left: 4px; }

/* ---- 右侧工作区 ---- */
.rv-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  padding-bottom: 12px;
  color: #667085;
  font-size: 12px;
}
.rv-tabs { margin: 0 0 12px; }
.sub-times { margin-top: 3px; color: #98a2b3; font-size: 11px; }
.rv-pane { padding-top: 4px; }
.rv-table-wrap { max-height: 360px; overflow: auto; }

.dtbl tbody tr.row-on td { background: #f7faff; }

.board-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 11px;
  margin-bottom: 10px;
  background: #fff4e5;
  border: 1px solid #fde3c0;
  border-radius: 8px;
  color: #b54708;
  font-size: 12.5px;

  b { font-weight: 600; }
}

/* ---- 处理批阅：大弹窗工作区 ---- */
.rv-dlg { max-height: calc(100vh - 230px); overflow: auto; padding-right: 4px; }
.rv-dlg-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-bottom: 10px;
  margin-bottom: 12px;
  border-bottom: 1px solid #f2f4f7;
  color: #667085;
  font-size: 12px;
}
.rv-dlg-task { margin-left: auto; }
.review-body { margin-bottom: 10px; }
.review-text {
  margin: 0 0 8px;
  padding: 10px 12px;
  background: #fff;
  border: 1px solid #eef1f6;
  border-radius: 8px;
  color: #475467;
  font-size: 12.5px;
  line-height: 1.85;
  white-space: pre-wrap;
}
.sub-file { display: flex; align-items: center; gap: 6px; font-size: 12.5px; color: #1764f5; }
.sub-file a { color: #1764f5; text-decoration: none; }
.sub-file a:hover { text-decoration: underline; }
.sub-acts { display: inline-flex; align-items: center; gap: 2px; margin-left: 6px; }
.preview-box { margin-bottom: 12px; }
.pv-head {
  display: flex; align-items: center; gap: 6px;
  margin-bottom: 6px; color: #475467; font-size: 12.5px;
  b { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
}
.pv-head > i { color: #1764f5; }
.pv-x { margin-left: auto; flex: none; color: #98a2b3; font-size: 11.5px; cursor: pointer; }
.pv-x:hover { color: #b42318; }

/* ---- 表格里的批阅记录 ---- */
.rv-rec { display: flex; align-items: center; gap: 6px; }
.rv-cmt {
  margin-top: 4px; color: #475467; font-size: 12px; line-height: 1.6;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}

/* ---- 面板里的批阅记录卡 ---- */
.rec-box {
  padding: 10px 12px; margin-bottom: 12px;
  background: #f0f9f4; border: 1px solid #cde9da; border-left: 3px solid #12b76a; border-radius: 8px;
}
.rec-top { display: flex; align-items: center; gap: 10px; }
.rec-who { color: #344054; font-size: 12.5px; }
.rec-re { margin-left: auto; color: #1764f5; font-size: 12px; }
.rec-cmt {
  margin: 8px 0 0; color: #344054; font-size: 12.5px; line-height: 1.8; white-space: pre-wrap;
}
.sub-title {
  display: flex; align-items: center; gap: 5px;
  margin-bottom: 6px; color: #344054; font-size: 12.5px; font-weight: 600;
  i { color: #1764f5; }
}

/* ---- 历次提交 ---- */
.hist { margin: 12px 0; border: 1px solid #eef1f6; border-radius: 8px; overflow: hidden; }
.hist-head {
  display: flex; align-items: center; gap: 8px;
  padding: 9px 12px; background: #f7f9fc; cursor: pointer; user-select: none;
  color: #344054; font-size: 12.5px;
  b { font-weight: 600; }
  i { color: #1764f5; }
  &:hover { background: #eef4fe; }
}
.hist-arrow { margin-left: auto; color: #98a2b3 !important; }
.hist-body { padding: 10px 12px; }
.hist-item {
  padding: 9px 10px; margin-bottom: 8px;
  background: #fff; border: 1px solid #eef1f6; border-radius: 8px;
  &:last-child { margin-bottom: 0; }
  &.on { border-color: #bcd4fb; background: #f7faff; }
}
.hist-line { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.hist-text {
  margin: 6px 0 0; color: #475467; font-size: 12.5px; line-height: 1.7; white-space: pre-wrap;
}
.hist-cmt {
  margin: 6px 0 0; padding: 6px 8px; background: #f7f9fc; border-radius: 6px;
  color: #475467; font-size: 12px; line-height: 1.7;
}
.hist-gone { margin: 6px 0 0; color: #98a2b3; font-size: 12px; font-style: italic; }
.review-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 10px;
}
.review-btns { display: flex; align-items: center; gap: 8px; }
/* 弹窗底部按钮右对齐（el-dialog 的 footer 默认就是右对齐，这里只补间距） */
.rv-dlg + .el-dialog__footer .el-button + .el-button { margin-left: 8px; }

code { padding: 1px 5px; color: #344054; font-size: 11.5px; background: #f2f4f7; border-radius: 4px; }
.hint-text { color: #98a2b3; font-size: 11.5px; }
</style>
