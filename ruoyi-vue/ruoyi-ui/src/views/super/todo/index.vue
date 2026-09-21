<template>
  <div class="td-page">
    <header class="td-head">
      <div>
        <span class="eyebrow">SUPER ADMIN · SUPERVISION</span>
        <h1>督办看板</h1>
        <p>
          四类待办：待审报名 · 待批作业 · 逾期任务 · 学习停滞。
          <b>「按事项」</b>看哪条没人处理，<b>「按人」</b>看谁积压最多 —— 两个视角<b>同一份数据</b>，数字必然一致。
          催办走定向通知（附被催人的完整待办清单），被催的人点开就知道要做什么。
        </p>
      </div>
      <div class="td-actions">
        <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="refreshAll">刷新</el-button>
      </div>
    </header>

    <!-- 视角切换（页内分段；?view=items|people|records 可分享） -->
    <div class="view-seg">
      <span class="seg">
        <span :class="{ on: view === 'items' }" @click="switchView('items')">按事项 <em>{{ total }}</em></span>
        <span :class="{ on: view === 'people' }" @click="switchView('people')">按人 <em>{{ admins.length }}</em></span>
        <span :class="{ on: view === 'records' }" @click="switchView('records')">催办记录 <em>{{ num(recordSummary.urgeCount) }}</em></span>
      </span>
      <span class="spacer" />
      <span class="hint">「按人」= 原「部门管理员」页；「催办记录」= 历史留痕与已读回执</span>
    </div>

    <div v-if="error" class="err-box">
      <i class="el-icon-warning-outline" />
      <div>
        <b>加载失败</b>
        <p>{{ error }}</p>
      </div>
      <el-button size="mini" type="primary" @click="load">重试</el-button>
    </div>

    <template v-else>
      <!-- 四源 KPI：点击即筛选（再点一次取消） -->
      <!-- 四源 KPI：两视角共享同一份聚合（避免两处数字要各自同步）；「催办记录」是留痕视角，另有一套指标 -->
      <div v-if="view !== 'records'" class="kpis">
        <div v-for="s in summary" :key="s.type" class="kpi"
             :class="{ on: view === 'items' && todoType === s.type, flat: view !== 'items' }"
             @click="onKpiClick(s.type)">
          <div class="lb"><i class="dot" :style="{ background: typeColor(s.type) }" />{{ s.label }}</div>
          <div class="vl">{{ s.count }}</div>
          <div class="ft">
            <template v-if="view === 'items'">{{ todoType === s.type ? '已筛选 · 再点取消' : '点击只看这类' }}</template>
            <template v-else>占待办合计 {{ total ? Math.round(s.count / total * 100) : 0 }}%</template>
          </div>
        </div>
        <div class="kpi total">
          <div class="lb"><i class="dot" style="background:#667085" />待办合计</div>
          <div class="vl">{{ total }}</div>
          <div class="ft">
            {{ view === 'items' ? ('停滞判定阈值 ' + stalledIdleDays + ' 天') : ('涉及 ' + withTodo.length + ' 位管理员') }}
          </div>
        </div>
      </div>

      <!-- ⚠️ 三个视角的表格必须带**唯一 key**：它们处在同一父级的兄弟分支上，
           Vue 对「同 tag + 同位置 + 无 key」的 vnode 会**复用同一个 el-table 实例**，
           于是 el-table-column 也被复用 —— label 会被 prop watcher 改成新列名（表头看着是对的），
           但 `renderCell` 闭包仍指向旧视角 → 单元格空白/串列，且无数据变更 → 永不自愈。
           加 key 让 Vue 直接销毁重建，彻底避开。 -->
      <section v-if="view === 'items'" class="card" :key="'view-items'">
        <div class="toolbar">
          <el-select v-model="deptId" size="mini" clearable placeholder="全部部门" style="width:150px" @change="load">
            <el-option v-for="d in deptOptions" :key="d.deptId" :label="d.deptName" :value="d.deptId" />
          </el-select>
          <el-select v-model="ownerId" size="mini" clearable placeholder="全部责任人" style="width:190px" @change="load">
            <el-option v-for="o in ownerOptions" :key="o.ownerId" :label="o.ownerName" :value="o.ownerId" />
          </el-select>
          <el-input v-model="kw" size="mini" clearable placeholder="搜标题 / 涉及人" style="width:180px" />
          <span class="spacer" />
          <span class="hint">共 {{ filtered.length }} 条</span>
          <el-button size="mini" type="primary" :disabled="!selection.length" @click="batchUrge">
            批量催办<span v-if="selection.length">（{{ ownerCount }}）</span>
          </el-button>
        </div>

        <el-table v-loading="loading" :data="filtered" size="mini" style="width:100%" :key="'tbl-items'" @selection-change="onSel">
          <el-table-column type="selection" width="40" />
          <el-table-column label="类型" width="92">
            <template slot-scope="s">
              <span class="tag" :class="typeTag(s.row.todoType)">{{ s.row.todoLabel }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="title" label="待办" min-width="240" show-overflow-tooltip />
          <el-table-column prop="detail" label="详情" min-width="180" show-overflow-tooltip />
          <el-table-column prop="deptName" label="部门" width="100" />
          <el-table-column prop="ownerName" label="责任人" width="140" show-overflow-tooltip />
          <el-table-column prop="targetName" label="涉及人" width="120" show-overflow-tooltip />
          <el-table-column label="截止 / 停滞" width="150">
            <template slot-scope="s">
              <span v-if="s.row.overdueHours" class="warn">已逾期 {{ humanHours(s.row.overdueHours) }}</span>
              <span v-else-if="s.row.idleDays != null" class="warn">{{ s.row.idleDays }} 天无进展</span>
              <span v-else-if="s.row.deadline">{{ s.row.deadline }}</span>
              <span v-else class="mut">—</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="86" align="center">
            <template slot-scope="s">
              <el-button size="mini" type="text" :disabled="!s.row.ownerId" @click="urgeOwner(s.row)">催办</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="!loading && !filtered.length" class="empty">
          <template v-if="todoType || deptId || ownerId || kw">当前筛选下没有待办</template>
          <template v-else>当前没有待办 —— 各部门都在正常推进</template>
        </div>

        <div class="note">
          <b>「待批作业」已按最新一版判定</b>：同一人多次提交时，只有<b>最新版本</b>仍为 <code>PENDING</code> 才算待办
          （旧版本的 PENDING 是历史，不算）。<br />
          催办<b>同一人每天最多一次</b>；若某人当前无待办会被自动跳过。
        </div>
      </section>

      <!-- ==================== 视角二：按人（原「部门管理员」页并入，2026-09-20） ==================== -->
      <section v-else-if="view === 'people'" class="card" :key="'view-people'">
        <div class="toolbar">
          <span class="seg">
            <span :class="{ on: peopleTab === 'ALL' }" @click="peopleTab = 'ALL'">全部（{{ admins.length }}）</span>
            <span :class="{ on: peopleTab === 'TODO' }" @click="peopleTab = 'TODO'">有待办（{{ withTodo.length }}）</span>
            <span :class="{ on: peopleTab === 'DISABLED' }" @click="peopleTab = 'DISABLED'">已停用（{{ disabledAdmins.length }}）</span>
          </span>
          <el-input v-model="peopleKw" size="mini" clearable placeholder="搜部门 / 姓名 / 账号" style="width:180px" />
          <span class="spacer" />
          <span class="hint">共 {{ peopleFiltered.length }} 人 · 覆盖 {{ adminDeptCount }} 个部门</span>
          <el-button size="mini" type="primary" :disabled="!peopleSelection.length" @click="batchUrgePeople">
            批量催办<span v-if="peopleSelection.length">（{{ peopleSelection.length }}）</span>
          </el-button>
        </div>

        <!-- 全宽：与「按事项」表对齐。宽屏下多余宽度由**多个 min-width 文本列按比例分摊**，
             所以不会再出现"某一列被撑到上千像素、中间一大片空白"的观感问题。 -->
        <el-table v-loading="loading" :data="peopleFiltered" size="mini" style="width:100%" :key="'tbl-people'"
                  @selection-change="onSelPeople">
          <el-table-column type="selection" width="40" :selectable="canUrgeAdmin" />
          <!-- ⚠️ 文本列一律用 min-width（不要写死 width）：el-table 会把容器多余宽度全分给
               唯一带 min-width 的列 → 宽屏下那一列会被撑到上千像素、中间留大片空白，
               看起来就像"其他列没数据"。文本列各占一份，多余宽度才能分摊。 -->
          <el-table-column prop="deptName" label="部门" min-width="120" />
          <el-table-column label="姓名 / 账号" min-width="190">
            <template slot-scope="s">
              <b>{{ s.row.nickName || s.row.userName }}</b>
              <small class="mut"> · {{ s.row.userName }}</small>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="76" align="center">
            <template slot-scope="s">
              <span class="tag" :class="s.row.status === '0' ? 'green' : 'gray'">
                {{ s.row.status === '0' ? '正常' : '停用' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="待审报名" width="82" align="center">
            <template slot-scope="s">
              <span :class="{ warn: s.row.pendingRegisters > 0 }">{{ countText(s.row.pendingRegisters) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="待批作业" width="82" align="center">
            <template slot-scope="s">
              <span :class="{ warn: s.row.pendingReviews > 0 }">{{ countText(s.row.pendingReviews) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="逾期任务" width="82" align="center">
            <template slot-scope="s">
              <span :class="{ warn: s.row.overdueTasks > 0 }">{{ countText(s.row.overdueTasks) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="学习停滞" width="82" align="center">
            <template slot-scope="s">
              <span :class="{ warn: s.row.stalledStudies > 0 }">{{ countText(s.row.stalledStudies) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="合计" width="64" align="center">
            <template slot-scope="s">
              <b :class="{ warn: s.row.totalTodos > 0 }">{{ s.row.totalTodos || 0 }}</b>
            </template>
          </el-table-column>
          <el-table-column prop="phone" label="联系方式" min-width="130" show-overflow-tooltip />
          <el-table-column label="操作" width="86" align="center">
            <template slot-scope="s">
              <el-button size="mini" type="text" :disabled="!canUrgeAdmin(s.row)" @click="urgeAdmin(s.row)">催办</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="!loading && !peopleFiltered.length" class="empty">
          {{ admins.length ? '当前筛选下没有部门管理员' : '暂无部门管理员' }}
        </div>

        <div class="note">
          待办口径与「按事项」<b>同源</b>（同一份聚合），所以两个视角的数字<b>必然一致</b>。<br />
          催办复用已通的<b>定向通知</b>通道（<code>msgType=URGE</code>）：通知会附上该管理员的<b>完整待办清单</b>；
          <b>同一人每天最多被催一次</b>；<b>已停用</b>或无待办的管理员不可催办。
        </div>
      </section>

      <!-- ============ 视角三：催办记录（留痕 + 已读回执，2026-09-20 新增） ============ -->
      <template v-else>
        <!-- 记录指标：**全量口径**，不随下方筛选变化 -->
        <div class="kpis">
          <div class="kpi flat total">
            <div class="lb"><i class="dot" style="background:#667085" />记录总数</div>
            <div class="vl">{{ num(recordSummary.total) }}</div>
            <div class="ft">含催办 · 任务 · 通知 · 公告</div>
          </div>
          <div class="kpi flat total">
            <div class="lb"><i class="dot" style="background:#f04438" />其中催办</div>
            <div class="vl">{{ num(recordSummary.urgeCount) }}</div>
            <div class="ft">超管 / 部门管理员 / 定时任务</div>
          </div>
          <div class="kpi flat total">
            <div class="lb"><i class="dot" style="background:#1764f5" />送达合计</div>
            <div class="vl">{{ num(recordSummary.targetTotal) }}<small>人次</small></div>
            <div class="ft">notice_target 合计</div>
          </div>
          <div class="kpi flat total">
            <div class="lb"><i class="dot" style="background:#12b76a" />已读合计</div>
            <div class="vl">{{ num(recordSummary.readTotal) }}<small>人次</small></div>
            <div class="ft" :class="{ warn: recordReadRate < 100 }">已读率 {{ recordReadRate }}%</div>
          </div>
          <div class="kpi flat total">
            <div class="lb"><i class="dot" style="background:#7a5af8" />系统自动发出</div>
            <div class="vl">{{ num(recordSummary.systemPublished) }}</div>
            <div class="ft">定时任务（如每日逾期提醒）</div>
          </div>
        </div>

        <section class="card" :key="'view-records'">
          <div class="toolbar">
            <el-select v-model="recordType" size="mini" placeholder="全部类型" clearable style="width:130px" @change="loadRecords">
              <el-option label="催办" value="URGE" />
              <el-option label="任务" value="TASK" />
              <el-option label="通知" value="NOTIFY" />
              <el-option label="公告" value="ANNOUNCE" />
              <el-option label="系统" value="SYSTEM" />
            </el-select>
            <el-select v-model="recordPublisherId" size="mini" clearable placeholder="全部发送者" style="width:190px" @change="loadRecords">
              <el-option v-for="p in publisherOptions" :key="String(p.publisherId)" :label="p.publisherName" :value="p.publisherId || -1" />
            </el-select>
            <el-input v-model="recordKw" size="mini" clearable placeholder="搜标题 / 内容" style="width:180px"
                      @keyup.enter.native="loadRecords" />
            <el-button size="mini" type="primary" @click="loadRecords">查询</el-button>
            <el-button size="mini" icon="el-icon-refresh-left" @click="resetRecordQuery">重置</el-button>
            <span class="spacer" />
            <span class="hint">共 {{ recordTotal }} 条（当前筛选）</span>
          </div>

          <!-- 同「按人」表：全宽 + 文本列用 min-width，多余宽度由多列分摊 -->
          <el-table v-loading="recordLoading" :data="records" size="mini" style="width:100%" :key="'tbl-records'">
            <el-table-column label="时间" width="140">
              <template slot-scope="s">{{ (s.row.publishTime || s.row.createTime || '').slice(0, 16) || '—' }}</template>
            </el-table-column>
            <el-table-column label="类型" width="80">
              <template slot-scope="s">
                <span class="tag" :class="typeTag(s.row.msgType)">{{ typeLabel(s.row.msgType) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="标题" min-width="260" show-overflow-tooltip>
              <template slot-scope="s">
                <!-- 点标题即可看正文（另有「内容」按钮，两处入口都通） -->
                <span class="link-title" @click="openContent(s.row)">{{ s.row.title || '（无标题）' }}</span>
              </template>
            </el-table-column>
            <!-- 同「按人」表：文本列用 min-width，避免宽屏下某一列独吞多余宽度 -->
            <el-table-column label="发送者" min-width="150" show-overflow-tooltip>
              <template slot-scope="s">
                <span :class="{ mut: !s.row.publisherId }">{{ s.row.publisherName || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="受众范围" min-width="180" show-overflow-tooltip>
              <template slot-scope="s">
                <span class="tag gray">{{ scopeLabel(s.row.scopeType) }}</span>
                <small class="sub-dept"> {{ s.row.targetDepts || '—' }}</small>
              </template>
            </el-table-column>
            <el-table-column label="送达" width="66" align="center">
              <template slot-scope="s">{{ num(s.row.targetCount) }}</template>
            </el-table-column>
            <el-table-column label="已读" width="66" align="center">
              <template slot-scope="s">
                <span :class="{ warn: num(s.row.targetCount) > 0 && num(s.row.readCount) < num(s.row.targetCount) }">
                  {{ num(s.row.readCount) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="已读率" width="76" align="center">
              <template slot-scope="s">{{ readRateOf(s.row) }}%</template>
            </el-table-column>
            <el-table-column label="操作" width="150" align="center">
              <template slot-scope="s">
                <el-button size="mini" type="text" @click="openContent(s.row)">内容</el-button>
                <el-button size="mini" type="text" :disabled="!num(s.row.targetCount)" @click="openReadDetail(s.row)">回执明细</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div v-if="!recordLoading && !records.length" class="empty">
            当前筛选下没有记录
          </div>

          <pagination v-show="recordTotal > 0" :total="recordTotal" :page.sync="recordQuery.pageNum"
                      :limit.sync="recordQuery.pageSize" @pagination="loadRecords" />

          <!-- 发送者分布 -->
          <div v-if="(recordSummary.publisherCounts || []).length" class="send-dist">
            <span class="mut">发送者分布：</span>
            <span v-for="p in recordSummary.publisherCounts" :key="String(p.publisherId)" class="chip">
              {{ p.publisherName }} <b>{{ p.cnt }}</b>
            </span>
          </div>

          <div class="note">
            数据源：<code>notice</code>（本体）+ <code>notice_target</code>（送达）+ <code>notice_read</code>（已读回执）。<br />
            已读率 = <b>既被送达、又确实读了</b>的人数 ÷ 送达人数 —— 分母分子都锚定在 <code>notice_target</code> 上；
            直接统计 <code>notice_read</code> 会把「全部已读」产生的回执算进来，导致已读率超过 100%。<br />
            上方指标是<b>全量口径</b>（不随筛选变化），表格是当前筛选结果。
          </div>
        </section>
      </template>

      <!-- 消息内容弹窗：催办/通知的**完整正文**（催办正文里含自动生成的待办清单，必须保留换行） -->
      <el-dialog title="消息内容" :visible.sync="contentDetail.visible" width="640px" append-to-body>
        <div v-if="contentDetail.row" class="msg-detail">
          <h4>{{ contentDetail.row.title || '（无标题）' }}</h4>
          <div class="msg-meta">
            <span class="tag" :class="typeTag(contentDetail.row.msgType)">{{ typeLabel(contentDetail.row.msgType) }}</span>
            <span>发送者：<b>{{ contentDetail.row.publisherName || '—' }}</b></span>
            <span>时间：{{ (contentDetail.row.publishTime || contentDetail.row.createTime || '').slice(0, 16) || '—' }}</span>
          </div>
          <div class="msg-meta">
            <span>受众：{{ scopeLabel(contentDetail.row.scopeType) }}<template v-if="contentDetail.row.targetDepts"> · {{ contentDetail.row.targetDepts }}</template></span>
            <span>送达 {{ num(contentDetail.row.targetCount) }} · 已读 {{ num(contentDetail.row.readCount) }}（{{ readRateOf(contentDetail.row) }}%）</span>
          </div>
          <pre class="msg-body">{{ contentDetail.row.content || '（该消息没有正文）' }}</pre>
        </div>
        <span slot="footer">
          <el-button size="small" @click="contentDetail.visible = false">关闭</el-button>
        </span>
      </el-dialog>

      <!-- 回执明细弹窗 -->
      <el-dialog title="送达与已读回执" :visible.sync="readDetail.visible" width="560px" append-to-body>
        <p class="mut" style="margin:0 0 10px">{{ readDetail.title }}</p>
        <el-table v-loading="readDetail.loading" :data="readDetail.rows" size="mini" style="width:100%">
          <el-table-column prop="deptName" label="部门" width="110" />
          <el-table-column label="姓名 / 账号" min-width="170">
            <template slot-scope="s">
              <b>{{ s.row.nickName || s.row.userName }}</b>
              <small class="mut"> · {{ s.row.userName }}</small>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template slot-scope="s">
              <span class="tag" :class="s.row.readTime ? 'green' : 'orange'">{{ s.row.readTime ? '已读' : '未读' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="阅读时间" min-width="140">
            <template slot-scope="s">{{ (s.row.readTime || '').slice(0, 16) || '—' }}</template>
          </el-table-column>
        </el-table>
        <div v-if="!readDetail.loading && !readDetail.rows.length" class="empty">该记录没有收件人明细</div>
      </el-dialog>
    </template>
  </div>
</template>

<script>
import { getTodoBoard, urgeTodo, listDeptAdmins } from '@/api/business/superTodo'
// 催办/通知记录（留痕 + 已读回执）
import { listNoticeRecords, getNoticeRecordSummary, getNoticeReadDetail } from '@/api/business/noticeRecord'

const TYPE_TAG = {
  REGISTER: 'blue',
  REVIEW: 'green',
  OVERDUE_TASK: 'red',
  STALLED_STUDY: 'orange'
}
const TYPE_COLOR = {
  REGISTER: '#1764f5',
  REVIEW: '#12b76a',
  OVERDUE_TASK: '#f04438',
  STALLED_STUDY: '#f79009'
}

export default {
  name: 'SuperTodoBoard',
  data() {
    return {
      loading: false,
      error: '',
      summary: [],
      rows: [],
      total: 0,
      stalledIdleDays: 7,
      todoType: '',
      deptId: null,
      ownerId: null,
      kw: '',
      selection: [],
      admins: [],
      // 视角：items = 按事项（原督办看板）/ people = 按人（原「部门管理员」页并入）
      view: 'items',
      peopleTab: 'ALL',
      peopleKw: '',
      peopleSelection: [],
      // ---- 视角三：催办记录 ----
      recordLoading: false,
      records: [],
      recordTotal: 0,
      // 默认只看「催办」；清空类型即可看全部（本板块也可当通知记录用）
      recordType: 'URGE',
      recordPublisherId: null,
      recordKw: '',
      recordQuery: { pageNum: 1, pageSize: 10 },
      recordSummary: {},
      readDetail: { visible: false, loading: false, title: '', rows: [] },
      // 消息内容弹窗（列表接口已带回 content，无需再请求）
      contentDetail: { visible: false, row: null }
    }
  },
  computed: {
    filtered() {
      const k = this.kw.trim().toLowerCase()
      if (!k) return this.rows
      return this.rows.filter(r =>
        (r.title || '').toLowerCase().indexOf(k) > -1 ||
        (r.targetName || '').toLowerCase().indexOf(k) > -1 ||
        (r.ownerName || '').toLowerCase().indexOf(k) > -1)
    },
    deptOptions() {
      const map = {}
      this.admins.forEach(a => { if (a.deptId) map[a.deptId] = a.deptName })
      return Object.keys(map).map(k => ({ deptId: Number(k), deptName: map[k] }))
    },
    ownerOptions() {
      return this.admins.map(a => ({
        ownerId: a.userId,
        ownerName: a.deptName + '·' + (a.nickName || a.userName)
      }))
    },
    /** 勾选行去重后的责任人数量（批量催办按人去重） */
    ownerCount() {
      const s = new Set()
      this.selection.forEach(r => { if (r.ownerId) s.add(r.ownerId) })
      return s.size
    },
    // ---------------- 「按人」视角（原「部门管理员」页的聚合口径，数据同源） ----------------
    /** 有待办的管理员 */
    withTodo() {
      return this.admins.filter(a => Number(a.totalTodos || 0) > 0)
    },
    /** 已停用的管理员（不可催办） */
    disabledAdmins() {
      return this.admins.filter(a => a.status !== '0')
    },
    /** 管理员覆盖的部门数 */
    adminDeptCount() {
      const s = new Set()
      this.admins.forEach(a => { if (a.deptId) s.add(a.deptId) })
      return s.size
    },
    /** 按人视角的表格数据（分段筛选 + 关键词） */
    peopleFiltered() {
      const k = this.peopleKw.trim().toLowerCase()
      return this.admins.filter(a => {
        if (this.peopleTab === 'TODO' && !(Number(a.totalTodos || 0) > 0)) return false
        if (this.peopleTab === 'DISABLED' && a.status === '0') return false
        if (!k) return true
        return [a.deptName, a.nickName, a.userName].some(v => (v || '').toLowerCase().indexOf(k) > -1)
      })
    },
    // ---------------- 「催办记录」视角 ----------------
    /** 发送者下拉选项（来自 summary 的 publisherCounts；publisher_id 为 null 的用 -1 表示「系统」） */
    publisherOptions() {
      return this.recordSummary.publisherCounts || []
    },
    /** 全量已读率（%）——分子分母都锚定在 notice_target 上，不会超过 100 */
    recordReadRate() {
      const t = this.num(this.recordSummary.targetTotal)
      return t ? Math.round(this.num(this.recordSummary.readTotal) / t * 100) : 0
    }
  },
  created() {
    const v = this.$route.query.view
    if (v === 'people' || v === 'items' || v === 'records') this.view = v
    this.load()
    this.loadAdmins()
    // 记录指标总要在页面加载时拉一次：视角切换上的「催办记录」角标用它（否则未进过该视角会显示 0）
    this.loadRecordSummary()
    if (this.view === 'records') this.loadRecords()
  },
  methods: {
    load() {
      this.loading = true
      this.error = ''
      const params = {}
      if (this.todoType) params.todoType = this.todoType
      if (this.deptId) params.deptId = this.deptId
      if (this.ownerId) params.ownerId = this.ownerId
      return getTodoBoard(params).then(res => {
        const d = res.data || {}
        this.summary = d.summary || []
        this.rows = d.rows || []
        this.total = d.total || 0
        if (d.stalledIdleDays != null) this.stalledIdleDays = d.stalledIdleDays
      }).catch(err => {
        this.summary = []
        this.rows = []
        this.total = 0
        this.error = (err && err.message) ? err.message : '请求失败，请检查网络或权限后重试'
      }).finally(() => { this.loading = false })
    },
    loadAdmins() {
      // 只为下拉选项取一次；失败不影响主列表
      listDeptAdmins().then(res => { this.admins = res.data || [] }).catch(() => {})
    },
    /** 视角切换：同步 ?view= 便于分享与刷新保持 */
    switchView(v) {
      if (this.view === v) return
      this.view = v
      if (v === 'records' && !this.records.length) {
        this.loadRecords()
        this.loadRecordSummary()
      }
      // ⚠️ 渲染正确性由三个表格各自的唯一 `key` 保证（见 template 顶部注释）：
      //   切换视角时 Vue 会**销毁旧表格、重建新表格**，列注册与 renderCell 天然干净。
      //   这里**不要**再手动清空/回填数据去"修"渲染 —— 那样只会白闪一下，且无效。
      const base = '/super/todo'
      this.$router.replace({ path: base, query: Object.assign({}, this.$route.query, { view: v }) }).catch(() => {})
    },
    refreshAll() {
      this.load()
      this.loadAdmins()
      if (this.view === 'records') {
        this.loadRecords()
        this.loadRecordSummary()
      }
    },
    /** KPI 点击：仅「按事项」视角生效（按人视角看的是下表各列，不做跨视角联动） */
    onKpiClick(type) {
      if (this.view === 'items') this.toggleType(type)
    },
    /** 计数展示：0 显示为 —，减少表格噪音 */
    countText(v) {
      const n = Number(v || 0)
      return n > 0 ? n : '—'
    },
    /** 能否催办：有待办且账号正常（已停用的不催） */
    canUrgeAdmin(row) {
      return Number(row.totalTodos || 0) > 0 && row.status === '0'
    },
    onSelPeople(sel) {
      this.peopleSelection = sel
    },
    urgeAdmin(row) {
      if (!this.canUrgeAdmin(row)) return
      this.doUrge([row.userId], row.nickName || row.userName)
    },
    batchUrgePeople() {
      if (!this.peopleSelection.length) return
      this.doUrge(this.peopleSelection.map(r => r.userId), this.peopleSelection.length + ' 位管理员')
    },
    // ---------------- 「催办记录」视角 ----------------
    loadRecords() {
      this.recordLoading = true
      const params = { pageNum: this.recordQuery.pageNum, pageSize: this.recordQuery.pageSize }
      if (this.recordType) params.msgType = this.recordType
      if (this.recordPublisherId) {
        // -1 = 「系统 / 定时任务」（publisher_id IS NULL，用 = 匹配不到，走后端开关）
        if (Number(this.recordPublisherId) === -1) params.systemPublished = true
        else params.publisherId = this.recordPublisherId
      }
      if (this.recordKw.trim()) params.keyword = this.recordKw.trim()
      return listNoticeRecords(params).then(res => {
        this.records = res.rows || []
        this.recordTotal = res.total || 0
      }).catch(() => {
        this.records = []
        this.recordTotal = 0
      }).finally(() => { this.recordLoading = false })
    },
    loadRecordSummary() {
      getNoticeRecordSummary().then(res => { this.recordSummary = res.data || {} }).catch(() => {})
    },
    resetRecordQuery() {
      this.recordType = 'URGE'
      this.recordPublisherId = null
      this.recordKw = ''
      this.recordQuery.pageNum = 1
      this.loadRecords()
    },
    /** 数值兜底：聚合结果可能是字符串 / BigDecimal / null */
    num(v) {
      const n = Number(v)
      return isNaN(n) ? 0 : n
    },
    typeLabel(t) {
      const map = { URGE: '催办', TASK: '任务', NOTIFY: '通知', ANNOUNCE: '公告', SYSTEM: '系统' }
      return map[t] || t || '—'
    },
    scopeLabel(t) {
      const map = { ALL: '全局', DEPT: '按部门', POSITION: '按岗位', USER: '指定人' }
      return map[t] || t || '—'
    },
    /** 单条已读率：分母分子都锚定 notice_target，天然 ≤ 100% */
    readRateOf(row) {
      const t = this.num(row.targetCount)
      return t ? Math.round(this.num(row.readCount) / t * 100) : 0
    },
    openReadDetail(row) {
      this.readDetail = { visible: true, loading: true, title: row.title || '', rows: [] }
      getNoticeReadDetail(row.id).then(res => {
        this.readDetail.rows = res.data || []
      }).catch(() => {
        this.readDetail.rows = []
      }).finally(() => { this.readDetail.loading = false })
    },
    /** 查看消息正文（列表接口已带 content；催办正文含自动生成的待办清单，弹窗里保留换行） */
    openContent(row) {
      if (!row) return
      this.contentDetail = { visible: true, row: row }
    },
    toggleType(t) {
      this.todoType = (this.todoType === t) ? '' : t
      this.load()
    },
    typeTag(t) {
      return TYPE_TAG[t] || 'gray'
    },
    typeColor(t) {
      return TYPE_COLOR[t] || '#98a2b3'
    },
    humanHours(h) {
      if (h == null) return '—'
      if (h >= 24) return Math.floor(h / 24) + ' 天'
      return h + ' 小时'
    },
    onSel(sel) {
      this.selection = sel
    },
    urgeOwner(row) {
      const ids = row.ownerId ? [row.ownerId] : []
      if (!ids.length) return
      this.doUrge(ids, row.ownerName || '责任人')
    },
    batchUrge() {
      const s = new Set()
      this.selection.forEach(r => { if (r.ownerId) s.add(r.ownerId) })
      if (!s.size) return
      this.doUrge(Array.from(s), s.size + ' 位责任人')
    },
    doUrge(ownerIds, label) {
      this.$prompt('附加说明（可空，会拼在自动生成的待办清单之后）：', '催办 · ' + label, {
        inputType: 'textarea',
        inputPlaceholder: '如：本周五前请完成处理',
        confirmButtonText: '发送催办',
        cancelButtonText: '取消'
      }).then(({ value }) => {
        return urgeTodo({ ownerIds: ownerIds, content: value || '' }).then(res => {
          const d = res.data || {}
          if ((d.sent || 0) > 0) {
            this.$message.success('已催办 ' + d.sent + ' 人' + ((d.skipped || 0) > 0 ? ('，跳过 ' + d.skipped + ' 人') : ''))
          } else {
            this.$message.warning((d.details || []).join('；') || '没有可催办的对象')
          }
          this.load()
          this.loadAdmins()
        })
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.td-page { padding: 18px 20px 32px; background: #eef2f7; min-height: calc(100vh - 84px); }
.td-head {
  display: flex; align-items: flex-start; justify-content: space-between; gap: 16px;
  background: #fff; border: 1px solid #e4e9f0; border-radius: 12px; padding: 18px 20px; margin-bottom: 14px;
  .eyebrow { color: #98a2b3; font-size: 11px; letter-spacing: .6px; }
  h1 { margin: 4px 0 6px; font-size: 20px; color: #1d2939; }
  p { margin: 0; color: #667085; font-size: 12.5px; line-height: 1.7; max-width: 660px; }
  .td-actions { display: flex; gap: 8px; flex: none; }
}
.err-box {
  display: flex; align-items: center; gap: 12px; background: #feecea; border: 1px solid #f6c9c4;
  border-radius: 12px; padding: 14px 18px; color: #b42318; font-size: 13px;
  i { font-size: 20px; }
  b { display: block; margin-bottom: 2px; }
  p { margin: 0; font-size: 12.5px; }
  .el-button { margin-left: auto; }
}
.kpis { display: grid; grid-template-columns: repeat(4, 1fr) 1.1fr; gap: 12px; margin-bottom: 14px; }
@media (max-width: 1200px) { .kpis { grid-template-columns: repeat(3, 1fr); } }
/* 视角切换条 */
.view-seg {
  display: flex; align-items: center; gap: 10px; margin-bottom: 14px;
  background: #fff; border: 1px solid #e4e9f0; border-radius: 12px; padding: 10px 14px;
}
/* 分段控件（视角切换 / 按人视角的分段筛选共用） */
.seg {
  display: inline-flex; align-items: center; background: #f2f4f7; border-radius: 8px; padding: 2px;
  > span {
    padding: 5px 13px; border-radius: 6px; color: #475467; font-size: 12px; cursor: pointer; user-select: none;
    transition: background .15s, color .15s;
    &:hover { color: #1764f5; }
    &.on { background: #fff; color: #1764f5; font-weight: 500; box-shadow: 0 1px 2px rgba(16, 24, 40, .06); }
  }
  em { margin-left: 4px; color: #98a2b3; font-size: 11px; font-style: normal; }
  > span.on em { color: #1764f5; }
}
.kpi {
  background: #fff; border: 1px solid #e4e9f0; border-radius: 10px; padding: 14px 16px; cursor: pointer;
  transition: border-color .15s, box-shadow .15s;
  &:hover { border-color: #b5d4f4; }
  &.on { border-color: #1764f5; box-shadow: 0 0 0 2px rgba(23,100,245,.12); }
  /* 「按人」视角下 KPI 只读展示（不做跨视角联动） */
  &.flat { cursor: default; &:hover { border-color: #e4e9f0; } }
  &.total { cursor: default; background: #f7f9fc; &:hover { border-color: #e4e9f0; } }
  .lb { display: flex; align-items: center; gap: 6px; color: #667085; font-size: 12px; margin-bottom: 8px;
        .dot { width: 6px; height: 6px; border-radius: 50%; } }
  .vl { font-size: 24px; font-weight: 600; color: #1d2939; }
  .ft { margin-top: 6px; font-size: 11.5px; color: #98a2b3; }
}
.card { background: #fff; border: 1px solid #e4e9f0; border-radius: 12px; padding: 16px 18px; }
.toolbar { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; flex-wrap: wrap; }
.spacer { flex: 1; }
.hint { color: #98a2b3; font-size: 12px; }
.tag {
  display: inline-block; padding: 0 7px; height: 20px; line-height: 20px; border-radius: 5px; font-size: 11px;
  background: #f2f4f7; color: #475467;
  &.blue { background: #e8f1fd; color: #1764f5; }
  &.green { background: #e7f7ef; color: #027a48; }
  &.red { background: #feecea; color: #b42318; }
  &.orange { background: #fff4e5; color: #b54708; }
  /* ⚠️ 不要用 #98a2b3：在 #f2f4f7 底上对比度太低，看起来像"没渲染出值" */
  &.gray { background: #f2f4f7; color: #475467; }
}
.warn { color: #b42318; font-weight: 500; }
.mut { color: #98a2b3; }
/* 发送者分布小标签 */
.send-dist { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; margin-top: 12px; font-size: 11.5px; }
.chip {
  display: inline-flex; align-items: center; gap: 4px; padding: 2px 9px;
  background: #f2f4f7; border-radius: 10px; color: #475467;
  b { color: #1d2939; }
}
/* 记录表：标题可点开看正文 */
.link-title { color: #1764f5; cursor: pointer; &:hover { text-decoration: underline; } }
/* 受众范围里的部门小字：用 #98a2b3 太浅，看着像空值 */
.sub-dept { color: #667085; font-size: 11.5px; }
/* 消息内容弹窗 */
.msg-detail {
  h4 { margin: 0 0 10px; color: #1d2939; font-size: 15px; line-height: 1.5; }
  .msg-meta { display: flex; flex-wrap: wrap; gap: 12px; margin-bottom: 6px; color: #667085; font-size: 12px; }
  .msg-body {
    margin: 10px 0 0; padding: 12px 14px; max-height: 46vh; overflow: auto;
    color: #344054; background: #f8fafc; border: 1px solid #e4e9f0; border-radius: 8px;
    font-size: 12.5px; font-family: inherit; line-height: 1.8;
    white-space: pre-wrap; word-break: break-word;
  }
}
.empty { padding: 40px 0; text-align: center; color: #98a2b3; font-size: 12.5px; }
.note { margin-top: 12px; color: #98a2b3; font-size: 11.5px; line-height: 1.7; }
</style>
