<template>
  <div class="app-container assessment-page">
    <el-row :gutter="16" class="page-heading">
      <el-col :xs="24" :sm="16">
        <div class="eyebrow">实习生学习考核系统</div>
        <h2>{{ page.title }}</h2>
        <p>{{ page.description }}</p>
      </el-col>
      <el-col :xs="24" :sm="8" class="heading-actions">
        <el-button v-if="page.primaryAction" type="primary" icon="el-icon-plus" @click="comingSoon">{{ page.primaryAction }}</el-button>
        <el-button icon="el-icon-refresh" @click="refreshPreview">刷新数据</el-button>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="metric-row">
      <el-col v-for="metric in page.metrics" :key="metric.label" :xs="12" :sm="6">
        <div class="metric" :class="metric.tone">
          <span>{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
          <small>{{ metric.hint }}</small>
        </div>
      </el-col>
    </el-row>

    <template v-if="page.type === 'assessment'">
      <el-row :gutter="16" class="entry-grid">
        <el-col v-for="entry in assessmentEntries" :key="entry.path" :xs="24" :sm="12" :lg="6">
          <el-card shadow="never" class="entry-card">
            <div class="entry-icon"><i :class="entry.icon" /></div>
            <h3>{{ entry.title }}</h3>
            <p>{{ entry.description }}</p>
            <el-button type="text" @click="go(entry.path)">进入管理 <i class="el-icon-arrow-right" /></el-button>
          </el-card>
        </el-col>
      </el-row>
    </template>

    <template v-else-if="page.type === 'dashboard'">
      <el-row :gutter="16">
        <el-col :xs="24" :lg="15">
          <el-card shadow="never" class="content-card">
            <div slot="header" class="card-title"><span>待办事项</span><el-button type="text" @click="comingSoon">查看全部</el-button></div>
            <div v-for="item in todoItems" :key="item.title" class="todo-item">
              <span class="todo-dot" :class="item.tone" />
              <div><strong>{{ item.title }}</strong><p>{{ item.description }}</p></div>
              <el-tag size="mini" :type="item.tag">{{ item.status }}</el-tag>
              <el-button type="text" @click="go(item.target)">去处理</el-button>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :lg="9">
          <el-card shadow="never" class="content-card">
            <div slot="header" class="card-title"><span>流程进度</span><el-tag size="mini" type="success">本期</el-tag></div>
            <div v-for="step in progressItems" :key="step.name" class="progress-item">
              <div><span>{{ step.name }}</span><b>{{ step.value }}%</b></div>
              <el-progress :percentage="step.value" :show-text="false" :color="step.color" />
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>

    <template v-else-if="page.type === 'learning'">
      <el-row :gutter="16" class="course-grid">
        <el-col v-for="course in courses" :key="course.name" :xs="24" :sm="12" :lg="8">
          <el-card shadow="never" class="course-card">
            <div class="course-cover" :class="course.tone"><i :class="course.icon" /></div>
            <div class="course-main"><el-tag size="mini" :type="course.tag">{{ course.type }}</el-tag><h3>{{ course.name }}</h3><p>{{ course.description }}</p>
              <el-progress :percentage="course.progress" :show-text="false" />
              <div class="course-footer"><span>{{ course.done ? '已完成' : '已完成 ' + course.progress + '%' }}</span><el-button type="text" @click="openMaterial(course)">{{ course.done ? '查看记录' : '继续学习' }}</el-button></div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>

    <template v-else-if="page.type === 'agreement'">
      <el-row :gutter="16">
        <el-col :xs="24" :lg="15"><el-card shadow="never" class="content-card agreement-reader"><h3>实习生保密协议 V1.2</h3><p>本协议用于约束实习期间接触的业务资料、账号信息、学习材料、考核提交物和项目交付内容。系统将在签署后记录协议版本、签署时间和签署凭证。</p><p>签署后方可进入学习与考核流程；电子证书将在考核通过且转正审批完成后生成。</p></el-card></el-col>
        <el-col :xs="24" :lg="9"><el-card shadow="never" class="content-card"><div slot="header" class="card-title"><span>签署状态</span><el-tag :type="agreementSigned ? 'success' : 'warning'">{{ agreementSigned ? '已签署' : '待签署' }}</el-tag></div><el-descriptions :column="1" size="small"><el-descriptions-item label="协议版本">V1.2</el-descriptions-item><el-descriptions-item label="签署方式">PC 签名 / 移动端手写</el-descriptions-item><el-descriptions-item label="电子证书">{{ agreementSigned ? '签署凭证已生成' : '考核通过后生成' }}</el-descriptions-item></el-descriptions><el-button type="primary" class="full-action" @click="openAgreementDialog">{{ agreementSigned ? '查看签署记录' : '确认签署' }}</el-button></el-card></el-col>
      </el-row>
    </template>

    <template v-else-if="page.type === 'rule'">
      <el-card shadow="never" class="content-card">
        <el-form label-width="150px" class="rule-form">
          <el-form-item label="学习完成率阈值"><el-input-number v-model="ruleForm.learningThreshold" :min="0" :max="100" /><span class="form-tip">达到阈值后可参加正式考核</span></el-form-item>
          <el-form-item label="理论通过分"><el-input-number v-model="ruleForm.passScore" :min="0" :max="100" /></el-form-item>
          <el-form-item label="默认补考次数"><el-input-number v-model="ruleForm.retakeLimit" :min="0" :max="5" /></el-form-item>
          <el-form-item label="成绩发布方式"><el-radio-group v-model="publishMode"><el-radio label="人工确认">人工确认后发布</el-radio><el-radio label="定时发布">定时发布</el-radio></el-radio-group></el-form-item>
          <el-form-item><el-button type="primary" @click="saveRules">保存规则</el-button></el-form-item>
        </el-form>
      </el-card>
    </template>

    <template v-else>
      <el-card shadow="never" class="content-card">
        <el-form :inline="true" size="small" class="filter-form">
          <el-form-item :label="page.filterLabel"><el-input v-model="filters.keyword" placeholder="请输入关键字" clearable @keyup.enter.native="searchRows" /></el-form-item>
          <el-form-item label="状态"><el-select v-model="filters.status" placeholder="全部状态" clearable><el-option label="处理中" value="处理中" /><el-option label="已发布" value="已发布" /><el-option label="草稿" value="草稿" /><el-option label="已完成" value="已完成" /></el-select></el-form-item>
          <el-form-item><el-button type="primary" icon="el-icon-search" @click="searchRows">查询</el-button><el-button icon="el-icon-refresh" @click="resetFilters">重置</el-button></el-form-item>
        </el-form>
        <div class="table-toolbar"><el-button v-if="page.primaryAction" size="mini" type="primary" icon="el-icon-plus" @click="handlePrimaryAction">{{ page.primaryAction }}</el-button><div><el-button size="mini" icon="el-icon-download" @click="comingSoon">导出</el-button><el-button size="mini" icon="el-icon-setting" @click="comingSoon">列设置</el-button></div></div>
        <el-table :data="pagedRows" border stripe>
          <el-table-column v-for="column in page.columns" :key="column.key" :prop="column.key" :label="column.label" :min-width="column.width || 130" show-overflow-tooltip />
          <el-table-column label="状态" width="110"><template slot-scope="scope"><el-tag :type="scope.row.statusType" size="mini">{{ scope.row.status }}</el-tag></template></el-table-column>
          <el-table-column label="操作" width="150" fixed="right"><template slot-scope="scope"><el-button type="text" size="mini" @click="openDetail(scope.row)">查看</el-button><el-button type="text" size="mini" :disabled="scope.row.status === '已完成'" @click="processRow(scope.row)">处理</el-button></template></el-table-column>
        </el-table>
        <div class="pagination-placeholder"><span>共 {{ filteredRows.length }} 条</span><el-pagination small layout="prev, pager, next" :current-page.sync="currentPage" :page-size="pageSize" :total="filteredRows.length" /></div>
      </el-card>
    </template>

    <el-dialog :title="dialog.title" :visible.sync="dialog.visible" width="560px" append-to-body>
      <template v-if="dialog.type === 'material' && selectedMaterial">
        <div class="dialog-intro"><el-tag size="mini" :type="selectedMaterial.type === '视频实操' ? 'primary' : 'success'">{{ selectedMaterial.type }}</el-tag><span>{{ selectedMaterial.chapter }}</span></div>
        <div class="material-preview" :class="selectedMaterial.type === '视频实操' ? 'video-preview' : 'document-preview'"><i :class="selectedMaterial.type === '视频实操' ? 'el-icon-video-play' : 'el-icon-document'" /><strong>{{ selectedMaterial.type === '视频实操' ? '视频学习演示' : '文档学习演示' }}</strong><small>当前为静态演示内容，后续在此接入真实文档与视频资源。</small></div>
        <p class="dialog-copy">{{ selectedMaterial.type === '视频实操' ? '模拟观看进度可逐步累加，达到 100% 后即可完成学习。' : '模拟阅读文档到底部后，可将本材料标记为已完成。' }}</p>
        <el-progress :percentage="selectedMaterial.progress" :status="selectedMaterial.done ? 'success' : undefined" />
      </template>
      <template v-else-if="dialog.type === 'agreement'">
        <div class="agreement-text"><h3>实习生保密协议 V1.2</h3><p>本协议用于约束实习期间接触的业务资料、账号信息、学习材料、考核提交物和项目交付内容。签署记录将在后端接入后存档。</p></div>
        <el-form label-width="90px"><el-form-item label="签署人"><el-input v-model="agreementForm.signer" /></el-form-item><el-form-item label="模拟签名"><el-input v-model="agreementForm.signature" placeholder="输入姓名作为演示签名" /></el-form-item><el-form-item><el-checkbox v-model="agreementForm.confirmed">我已阅读并同意协议内容</el-checkbox></el-form-item></el-form>
      </template>
      <template v-else-if="dialog.type === 'create'">
        <el-form label-width="90px"><el-form-item label="名称"><el-input v-model="createForm.name" :placeholder="'请输入' + page.title + '名称'" /></el-form-item><el-form-item label="适用范围"><el-select v-model="createForm.scope" style="width:100%"><el-option label="开发部门" value="开发部门" /><el-option label="交付部门" value="交付部门" /><el-option label="全体实习生" value="全体实习生" /></el-select></el-form-item><el-form-item label="说明"><el-input v-model="createForm.remark" type="textarea" :rows="3" placeholder="填写演示说明" /></el-form-item></el-form>
      </template>
      <template v-else-if="dialog.type === 'detail' && selectedRow">
        <el-descriptions :column="1" border size="small"><el-descriptions-item label="编号">{{ selectedRow.code }}</el-descriptions-item><el-descriptions-item label="名称">{{ selectedRow.name }}</el-descriptions-item><el-descriptions-item label="适用范围">{{ selectedRow.scope }}</el-descriptions-item><el-descriptions-item label="当前状态"><el-tag size="mini" :type="selectedRow.statusType">{{ selectedRow.status }}</el-tag></el-descriptions-item><el-descriptions-item label="更新时间">{{ selectedRow.updatedAt }}</el-descriptions-item></el-descriptions>
      </template>
      <template v-else-if="dialog.type === 'exam'">
        <div class="dialog-copy"><b>2026 年 9 月正式理论考试</b><p>学习完成率需达到 {{ ruleForm.learningThreshold }}%。正式答题、计时和交卷将随考试服务一起接入。</p></div>
        <el-alert title="当前为前端交互演示，尚未创建真实考试记录。" type="info" :closable="false" show-icon />
      </template>
      <template v-else-if="dialog.type === 'practice'">
        <el-form label-width="90px"><el-form-item label="提交说明"><el-input v-model="practiceForm.description" type="textarea" :rows="3" placeholder="简要说明本次实践成果" /></el-form-item><el-form-item label="演示附件"><el-input v-model="practiceForm.fileName" placeholder="例如：intern-demo.zip" /></el-form-item></el-form>
        <el-alert title="附件上传将在文件服务接入后启用；当前仅保存前端演示状态。" type="info" :closable="false" show-icon />
      </template>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialog.visible = false">{{ dialog.type === 'detail' ? '关闭' : '取消' }}</el-button>
        <el-button v-if="dialog.type === 'material' && selectedMaterial && !selectedMaterial.done" type="primary" @click="completeMaterial">{{ selectedMaterial.progress >= 100 ? '标记完成' : '模拟学习进度' }}</el-button>
        <el-button v-if="dialog.type === 'agreement'" type="primary" @click="signAgreement">确认签署</el-button>
        <el-button v-if="dialog.type === 'create'" type="primary" @click="createRow">保存演示数据</el-button>
        <el-button v-if="dialog.type === 'exam'" type="primary" @click="dialog.visible = false">知道了</el-button>
        <el-button v-if="dialog.type === 'practice'" type="primary" @click="submitPractice">模拟提交</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
const defaultColumns = [
  { label: '编号', key: 'code', width: 170 },
  { label: '名称', key: 'name', width: 180 },
  { label: '适用范围', key: 'scope', width: 150 },
  { label: '更新时间', key: 'updatedAt', width: 170 }
]

const pages = {
  assessment: { title: '考核认证管理', description: '统一维护考试安排、课程设计、线下学习、题库、常见问题、考核规则与批阅流程。', type: 'assessment', metrics: [['考试安排', '3 场', '理论与实践分开安排', 'blue'], ['培养课程', '14 项', '文档理论与视频实操', 'green'], ['题库题目', '138 题', '理论题库与实操题库', 'orange'], ['待批阅', '8 份', '人工确认后发布', 'red']] },
  overview: { title: '学习考核工作台', description: '汇总培养、考核与批阅工作，当前数据为页面骨架示例。', type: 'dashboard', primaryAction: '', metrics: [['待处理事项', '12', '需要及时处理', 'orange'], ['本期考核', '3 场', '已发布 2 场', 'blue'], ['待批阅提交', '8 份', '人工确认后发布', 'red'], ['完成率', '76%', '本期学习进度', 'green']] },
  internDashboard: { title: '实习生工作台', description: '集中查看当前培养任务、学习进度、考核资格与待办事项。', type: 'dashboard', metrics: [['待办任务', '4', '按截止时间排序', 'orange'], ['学习完成率', '62%', '距考核资格还差 8%', 'blue'], ['理论考试', '未开始', '9 月正式考核', 'purple'], ['综合结果', '待完成', '成绩发布后可查看', 'green']] },
  learning: { title: '学习中心', description: '文档理论、视频实操和线下学习统一归档，完成记录将进入学习档案。', type: 'learning', primaryAction: '', metrics: [['必修课程', '3 门', '当前培养计划', 'blue'], ['已完成', '1 门', '学习记录已归档', 'green'], ['进行中', '2 门', '请按截止时间完成', 'orange'], ['线下学习', '1 场', '等待签到', 'purple']] },
  exam: { title: '考试中心', description: '正式理论考试、实践考核与模拟自测的入口与进度说明。', type: 'list', primaryAction: '进入考试', filterLabel: '考试名称', columns: defaultColumns, metrics: [['正式考试', '1 场', '开放报名中', 'blue'], ['模拟考试', '2 次', '不计入正式成绩', 'green'], ['补考机会', '1 次', '按部门规则执行', 'orange'], ['考试资格', '待满足', '学习完成率需达标', 'purple']] },
  practice: { title: '实践考核提交', description: '按岗位提交 Demo、说明文档和演示材料，提交后进入人工批阅队列。', type: 'list', primaryAction: '提交材料', filterLabel: '考核任务', columns: defaultColumns, metrics: [['待提交', '1 项', '请在截止前完成', 'orange'], ['已提交', '0 项', '等待批阅', 'blue'], ['评分项', '5 个', '查看任务说明', 'green'], ['附件限制', '3 类', '源码 / 文档 / 视频', 'purple']] },
  tasks: { title: '任务中心', description: '学习、协议、考试、线下培训和档案确认任务统一追踪。', type: 'list', filterLabel: '任务名称', columns: defaultColumns, metrics: [['待办', '4 项', '请优先处理临期任务', 'orange'], ['进行中', '2 项', '持续记录进度', 'blue'], ['已完成', '1 项', '已计入培养档案', 'green'], ['本周期', '7 项', '全部任务类型', 'purple']] },
  scores: { title: '考核成绩', description: '理论、实践、阶段评价和综合结果在部门确认发布后展示。', type: 'list', filterLabel: '考核名称', columns: defaultColumns, metrics: [['理论成绩', '--', '等待考试完成', 'blue'], ['实践成绩', '--', '等待人工批阅', 'orange'], ['综合结果', '待完成', '按规则聚合', 'purple'], ['阶段评价', '未发布', '部门管理员确认', 'green']] },
  portrait: { title: '能力画像', description: '依据学习、理论、实践和阶段评价形成多维能力记录。', type: 'dashboard', metrics: [['综合能力', '72', '当前阶段画像', 'blue'], ['学习投入', '80', '课程与任务完成情况', 'green'], ['理论掌握', '--', '考试后生成', 'purple'], ['实践能力', '--', '批阅后生成', 'orange']] },
  agreements: { title: '协议与证书', description: '查看保密协议签署记录和考核通过后生成的电子证书。', type: 'agreement', metrics: [['保密协议', '待签署', '完成后开放培养流程', 'orange'], ['协议版本', 'V1.2', '当前生效版本', 'blue'], ['电子证书', '未获得', '通过考核后生成', 'purple'], ['签署凭证', '--', '签署后可下载', 'green']] },
  messages: { title: '消息中心', description: '接收注册审核、学习提醒、考核通知和成绩发布消息。', type: 'list', filterLabel: '消息标题', columns: defaultColumns, metrics: [['未读消息', '2 条', '请及时查看', 'orange'], ['系统通知', '4 条', '规则与公告', 'blue'], ['业务提醒', '3 条', '学习与考核节点', 'purple'], ['已读消息', '12 条', '保留近期记录', 'green']] },
  studyGuide: { title: '备考资料', description: '查看考试指南、资格要求、正式考核规则和注意事项。', type: 'list', filterLabel: '资料名称', columns: defaultColumns, metrics: [['考试指南', '1 份', '当前生效版本', 'blue'], ['考核规则', '1 套', '理论与实践', 'green'], ['资格门槛', '70%', '学习完成率', 'orange'], ['更新提醒', '0 条', '暂无规则变更', 'purple']] },
  mockExam: { title: '模拟考核', description: '通过随机练习熟悉题型，模拟结果不计入正式成绩。', type: 'list', primaryAction: '开始模拟考核', filterLabel: '练习记录', columns: defaultColumns, metrics: [['随机题目', '10 题', '单选、多选与判断', 'blue'], ['练习次数', '不限', '可重复自测', 'green'], ['最近成绩', '86 分', '仅本人可见', 'orange'], ['计入成绩', '否', '不影响正式考核', 'purple']] },
  profile: { title: '个人信息', description: '维护个人基础信息，岗位、部门和导师由业务审核流程维护。', type: 'list', primaryAction: '保存信息', filterLabel: '信息项', columns: defaultColumns, metrics: [['账号状态', '预备实习生', '审核通过后生效', 'blue'], ['所属部门', '待分配', '由岗位关联确定', 'orange'], ['导师', '待分配', '部门管理员维护', 'purple'], ['档案完整度', '60%', '请补齐基础资料', 'green']] },
  departmentDashboard: { title: '部门工作台', description: '查看本部门注册审核、培养进度、考核安排与批阅待办。', type: 'dashboard', metrics: [['待审核', '5 人', '注册申请', 'orange'], ['培养中', '18 人', '预备与正式实习生', 'blue'], ['待批阅', '8 份', '实践考核提交物', 'red'], ['本期通过率', '86%', '已发布结果', 'green']] },
  students: { title: '实习生管理', description: '维护本部门实习生培养状态、导师分配、学习进度与考核资格。', type: 'list', primaryAction: '新增实习生', filterLabel: '姓名 / 账号', columns: defaultColumns, metrics: [['预备实习生', '12 人', '协议签署与学习中', 'orange'], ['正式实习生', '6 人', '已进入考核阶段', 'blue'], ['待分配导师', '3 人', '请尽快维护', 'red'], ['学习达标', '14 人', '可参加正式考核', 'green']] },
  grading: { title: '实习批阅', description: '处理理论、实践提交物与阶段评价，人工确认后发布成绩。', type: 'list', primaryAction: '批量发布成绩', filterLabel: '实习生 / 考核', columns: defaultColumns, metrics: [['待批阅', '8 份', '需要人工确认', 'orange'], ['AI 初筛', '后续接入', '异常转人工处理', 'purple'], ['已确认', '16 份', '等待发布', 'blue'], ['已发布', '24 份', '可查看成绩', 'green']] },
  globalDashboard: { title: '全局工作台', description: '汇总全组织培养、考核、批阅与规则执行情况。', type: 'dashboard', metrics: [['部门数量', '5 个', '已接入培养流程', 'blue'], ['实习生', '68 人', '本期在培人数', 'green'], ['考核安排', '7 场', '跨部门统一管理', 'purple'], ['异常事项', '3 项', '需要管理员关注', 'red']] },
  organization: { title: '组织与岗位', description: '维护组织范围、岗位类型和部门岗位绑定关系。岗位主数据可在业务管理中进行真实维护。', type: 'list', primaryAction: '维护绑定关系', filterLabel: '部门 / 岗位', columns: defaultColumns, metrics: [['组织部门', '5 个', '培养范围', 'blue'], ['启用岗位', '5 个', '岗位主数据', 'green'], ['有效绑定', '5 条', '部门岗位关系', 'purple'], ['待完善', '0 条', '请按实际组织配置', 'orange']] },
  rolePermission: { title: '角色权限', description: '配置超级管理员、部门管理员、预备实习生与正式实习生的功能边界。', type: 'list', primaryAction: '新增角色', filterLabel: '角色名称', columns: defaultColumns, metrics: [['业务角色', '4 个', '学习考核角色', 'blue'], ['权限菜单', '30 项', '页面骨架已就绪', 'green'], ['数据范围', '3 类', '全局 / 部门 / 本人', 'purple'], ['待配置', '4 项', '按角色分配后生效', 'orange']] },
  systemRules: { title: '系统规则', description: '配置默认学习门槛、补考次数、评分发布方式和流程校验规则。', type: 'rule', primaryAction: '', metrics: [['完成率阈值', '70%', '正式考核资格', 'blue'], ['理论通过分', '70 分', '默认规则', 'green'], ['补考次数', '1 次', '默认规则', 'orange'], ['发布方式', '人工确认', '避免误发成绩', 'purple']] },
  auditLog: { title: '审计日志', description: '留存注册审核、协议签署、学习完成、成绩发布和规则变更等关键操作。', type: 'list', filterLabel: '操作人 / 业务编号', columns: defaultColumns, metrics: [['今日操作', '24 次', '关键业务留痕', 'blue'], ['审核动作', '8 次', '注册与考核处理', 'green'], ['规则变更', '1 次', '需保留变更前后值', 'orange'], ['异常记录', '0 条', '等待接入监控', 'purple']] },
  schedule: { title: '考试安排', description: '配置理论考试、实践考核、报名范围、资格门槛和发布状态。', type: 'list', primaryAction: '新增考试安排', filterLabel: '考试名称', columns: defaultColumns, metrics: [['已发布', '2 场', '实习生可查看', 'green'], ['草稿', '1 场', '待补充配置', 'orange'], ['报名中', '1 场', '到期自动关闭', 'blue'], ['待发布', '1 场', '请确认范围', 'purple']] },
  courseDesign: { title: '课程设计', description: '维护文档理论、视频实操、章节顺序、完成验证和适用岗位。', type: 'list', primaryAction: '新建课程设计', filterLabel: '课程名称', columns: defaultColumns, metrics: [['文档理论', '8 项', '支持阅读完成验证', 'blue'], ['视频实操', '6 项', '支持进度记录', 'purple'], ['必修课程', '10 项', '纳入培养计划', 'orange'], ['已发布', '12 项', '实习生端可见', 'green']] },
  offlineLearning: { title: '线下学习', description: '配置集中培训场次、报名、签到、补课和学习证明。', type: 'list', primaryAction: '新增线下学习', filterLabel: '培训名称', columns: defaultColumns, metrics: [['近期场次', '3 场', '本月培训计划', 'blue'], ['报名中', '2 场', '可在线报名', 'green'], ['待签到', '1 场', '培训结束后处理', 'orange'], ['补课记录', '0 条', '缺席后生成', 'purple']] },
  theoryBank: { title: '理论题库', description: '管理单选、多选、判断、知识点、分值和批量导入。', type: 'list', primaryAction: '新增理论题', filterLabel: '题目 / 知识点', columns: defaultColumns, metrics: [['启用题目', '120 题', '可用于正式考核', 'blue'], ['单选题', '65 题', '基础知识', 'green'], ['多选题', '35 题', '综合判断', 'orange'], ['判断题', '20 题', '规则校验', 'purple']] },
  practiceBank: { title: '实操题库', description: '管理岗位 Demo、提交物、评分项、附件模板和启用状态。', type: 'list', primaryAction: '新增实操题', filterLabel: '题目 / 岗位', columns: defaultColumns, metrics: [['启用题目', '18 题', '按岗位配置', 'blue'], ['提交模板', '12 份', '附件要求', 'green'], ['评分项', '72 项', '人工确认依据', 'orange'], ['待完善', '3 题', '补充评分细则', 'purple']] },
  faq: { title: '常见问题', description: '维护实习生端认证页展示的问题、答案、分类与排序。', type: 'list', primaryAction: '新增问题', filterLabel: '问题关键词', columns: defaultColumns, metrics: [['已发布', '16 条', '实习生端可见', 'green'], ['草稿', '2 条', '待审核发布', 'orange'], ['本月查看', '86 次', '后续接入统计', 'blue'], ['待更新', '1 条', '规则变更后同步', 'purple']] },
  internExam: { title: '实习考核', description: '配置正式考核流程、补考、终止、转正门槛和结果发布。', type: 'rule', primaryAction: '', metrics: [['考核流程', '2 套', '理论 + 实践', 'blue'], ['通过门槛', '70 分', '默认规则', 'green'], ['补考规则', '1 次', '部门可调整', 'orange'], ['结果发布', '人工确认', '避免误发布', 'purple']] },
  review: { title: '考核批阅', description: '查看 AI 初筛占位、人工复核、评分确认、阶段评价和成绩发布。', type: 'list', primaryAction: '批量发布成绩', filterLabel: '实习生 / 考核', columns: defaultColumns, metrics: [['待评分', '8 份', '优先处理临期任务', 'orange'], ['待复核', '3 份', '人工确认', 'blue'], ['待发布', '6 份', '评分已完成', 'purple'], ['已发布', '24 份', '成绩可见', 'green']] }
}

export default {
  name: 'AssessmentPage',
  data() {
    return {
      publishMode: '人工确认',
      agreementSigned: false,
      agreementForm: { signer: '林晓岚', signature: '', confirmed: false },
      ruleForm: { learningThreshold: 70, passScore: 70, retakeLimit: 1 },
      profileForm: { name: '林晓岚', phone: '138****1024', email: 'lin.xiaolan@example.com' },
      learningMaterials: [
        { id: 'M001', name: '岗位规范与保密要求', description: '第一章 · 入职与保密', chapter: '第一章 · 入职与保密', type: '文档理论', progress: 100, done: true, tag: 'success', tone: 'green', icon: 'el-icon-document' },
        { id: 'M002', name: '项目交付流程视频', description: '第二章 · 交付实操', chapter: '第二章 · 交付实操', type: '视频实操', progress: 42, done: false, tag: 'primary', tone: 'blue', icon: 'el-icon-video-play' },
        { id: 'M003', name: '正式考核说明', description: '第三章 · 考核规则', chapter: '第三章 · 考核规则', type: '文档理论', progress: 0, done: false, tag: 'warning', tone: 'orange', icon: 'el-icon-notebook-2' }
      ],
      filters: { keyword: '', status: '' },
      currentPage: 1,
      pageSize: 5,
      rowStore: {},
      selectedMaterial: null,
      selectedRow: null,
      createForm: { name: '', scope: '开发部门', remark: '' },
      practiceForm: { description: '', fileName: '' },
      dialog: { visible: false, type: '', title: '' }
    }
  },
  computed: {
    pageKey() {
      if (this.$route.path.endsWith('/manage/overview')) {
        return 'assessment'
      }
      return this.$route.path.split('/').pop().replace(/-([a-z])/g, (all, letter) => letter.toUpperCase())
    },
    page() {
      const config = pages[this.pageKey] || pages.overview
      let metrics = config.metrics.slice()
      if (this.pageKey === 'learning') {
        const doneCount = this.learningMaterials.filter(item => item.done).length
        metrics = [['必修课程', this.learningMaterials.length + ' 门', '当前培养计划', 'blue'], ['已完成', doneCount + ' 门', '学习记录已归档', 'green'], ['进行中', (this.learningMaterials.length - doneCount) + ' 门', '请按截止时间完成', 'orange'], ['线下学习', '1 场', '等待签到', 'purple']]
      }
      if (this.pageKey === 'agreements') {
        metrics = [['保密协议', this.agreementSigned ? '已签署' : '待签署', this.agreementSigned ? '签署凭证已生成' : '完成后开放培养流程', this.agreementSigned ? 'green' : 'orange'], ['协议版本', 'V1.2', '当前生效版本', 'blue'], ['电子证书', '未获得', '通过考核后生成', 'purple'], ['签署凭证', this.agreementSigned ? '已生成' : '--', '签署后可下载', 'green']]
      }
      return Object.assign({}, config, {
        metrics: metrics.map(item => ({ label: item[0], value: item[1], hint: item[2], tone: item[3] }))
      })
    },
    assessmentEntries() {
      return [
        ['schedule', '考试安排', '设置理论考试、实践考核、报名范围与发布状态', 'el-icon-date'],
        ['course-design', '课程设计', '维护理论、视频、章节顺序和完成验证', 'el-icon-reading'],
        ['offline-learning', '线下学习', '维护集中培训、签到、补课与学习证明', 'el-icon-office-building'],
        ['theory-bank', '理论题库', '管理题型、知识点、分值和批量导入', 'el-icon-document-copy'],
        ['practice-bank', '实操题库', '维护岗位 Demo、附件与评分项', 'el-icon-monitor'],
        ['faq', '常见问题', '维护实习生端展示问题与答案', 'el-icon-question'],
        ['intern-exam', '实习考核', '配置补考、终止、转正门槛与结果发布', 'el-icon-finished'],
        ['review', '实习批阅', '处理人工复核、阶段评价与成绩发布', 'el-icon-edit-outline']
      ].map(item => ({ path: '/assessment/manage/' + item[0], title: item[1], description: item[2], icon: item[3] }))
    },
    todoItems() {
      return [
        { title: '完成岗位规范学习材料', description: '学习完成后将自动写入培养档案', status: '待办', tag: 'warning', tone: 'orange', target: 'learning' },
        { title: '确认本期正式考核安排', description: '请核对理论考试与实践考核时间', status: '处理中', tag: 'primary', tone: 'blue', target: 'exam' },
        { title: '处理实践考核提交物', description: '评分确认后才能发布综合成绩', status: '待批阅', tag: 'danger', tone: 'red', target: 'grading' }
      ]
    },
    progressItems() {
      return [
        { name: '协议签署', value: 100, color: '#2f9b79' },
        { name: '学习材料', value: 62, color: '#3d82c7' },
        { name: '正式考核', value: 20, color: '#d2872f' },
        { name: '成绩发布', value: 0, color: '#8a67bd' }
      ]
    },
    courses() {
      return this.learningMaterials
    },
    tableRows() {
      return this.rowStore[this.pageKey] || this.seedRows()
    },
    filteredRows() {
      const keyword = this.filters.keyword.trim().toLowerCase()
      return this.tableRows.filter(row => {
        const matchesKeyword = !keyword || [row.code, row.name, row.scope].some(value => String(value).toLowerCase().indexOf(keyword) > -1)
        const matchesStatus = !this.filters.status || row.status === this.filters.status
        return matchesKeyword && matchesStatus
      })
    },
    pagedRows() {
      const start = (this.currentPage - 1) * this.pageSize
      return this.filteredRows.slice(start, start + this.pageSize)
    }
  },
  watch: {
    '$route.path'() {
      this.filters = { keyword: '', status: '' }
      this.currentPage = 1
    }
  },
  methods: {
    comingSoon() { this.$modal.msgInfo('功能开发中') },
    go(path) {
      const target = path && path.charAt(0) === '/' ? path : this.resolvePath(path)
      if (target) this.$router.push(target)
    },
    resolvePath(key) {
      const paths = {
        learning: '/assessment/intern/learning',
        exam: '/assessment/intern/exam',
        practice: '/assessment/intern/practice',
        tasks: '/assessment/intern/tasks',
        agreements: '/assessment/intern/agreements',
        grading: '/assessment/department/grading',
        review: '/assessment/manage/review'
      }
      return paths[key]
    },
    refreshPreview() {
      this.$modal.msgSuccess('已刷新本地演示数据')
    },
    openMaterial(material) {
      this.selectedMaterial = material
      this.dialog = { visible: true, type: 'material', title: material.name }
    },
    completeMaterial() {
      if (this.selectedMaterial.progress < 100) {
        this.selectedMaterial.progress = Math.min(100, this.selectedMaterial.progress + (this.selectedMaterial.type === '视频实操' ? 29 : 100))
        this.$modal.msgSuccess(this.selectedMaterial.progress === 100 ? '已满足完成条件，可标记完成' : '已更新模拟学习进度')
        return
      }
      this.selectedMaterial.done = true
      this.selectedMaterial.tag = 'success'
      this.$modal.msgSuccess('学习完成记录已写入本地演示状态')
      this.dialog.visible = false
    },
    openAgreementDialog() {
      if (this.agreementSigned) {
        this.$modal.msgSuccess('签署人：' + this.agreementForm.signer + '；协议版本：V1.2；签署凭证已生成')
        return
      }
      this.dialog = { visible: true, type: 'agreement', title: '首次登录保密协议签署' }
    },
    signAgreement() {
      if (!this.agreementForm.signature || !this.agreementForm.confirmed) {
        this.$modal.msgWarning('请完成演示签名并确认已阅读协议')
        return
      }
      this.agreementSigned = true
      this.dialog.visible = false
      this.$modal.msgSuccess('协议已签署，签署凭证已生成')
    },
    saveRules() {
      this.$modal.msgSuccess('规则已保存到本地演示状态')
    },
    seedRows() {
      const seeds = {
        exam: [['2026 年 9 月正式理论考试', '交付部门', '报名中', 'primary'], ['安全规范模拟自测', '本人可见', '已发布', 'success'], ['10 月实践考核预告', '全体实习生', '草稿', 'info']],
        practice: [['岗位 Demo 提交任务', '实施实习生', '处理中', 'warning'], ['项目交付说明文档', '实施实习生', '草稿', 'info'], ['演示视频补充材料', '实施实习生', '已完成', 'success']],
        students: [['林晓岚', '交付部门', '处理中', 'warning'], ['陈默', '设计部门', '已发布', 'success'], ['周子轩', '开发部门', '草稿', 'info']],
        grading: [['林晓岚 - 岗位 Demo', '交付部门', '处理中', 'warning'], ['陈默 - 实践考核', '设计部门', '已完成', 'success'], ['周子轩 - 学习材料', '开发部门', '草稿', 'info']],
        messages: [['学习材料待完成', '本人', '处理中', 'warning'], ['注册审核结果通知', '本人', '已发布', 'success'], ['正式考核安排提醒', '本人', '已发布', 'success']],
        tasks: [['签署实习生保密协议', '本人', '处理中', 'warning'], ['观看项目交付流程视频', '本人', '处理中', 'warning'], ['完成个人信息确认', '本人', '已完成', 'success']],
        scores: [['2026 年 9 月正式考核', '交付部门', '处理中', 'warning'], ['阶段学习评价', '交付部门', '草稿', 'info'], ['模拟考试成绩', '本人', '已发布', 'success']],
        studyGuide: [['正式考核指南 V1.2', '全体预备实习生', '已发布', 'success'], ['理论考试规则', '本人岗位', '已发布', 'success'], ['实践提交规范', '本人岗位', '已发布', 'success']],
        mockExam: [['安全规范模拟自测', '本人', '已完成', 'success'], ['岗位流程模拟练习', '本人', '已发布', 'primary']]
      }
      const items = seeds[this.pageKey] || [[this.page.title + '配置项', '开发部门', '处理中', 'warning'], ['岗位规范与保密要求', '实施实习生', '已发布', 'success'], ['9 月正式培养计划', '全体实习生', '草稿', 'info']]
      return items.map((item, index) => ({ code: 'IA-202609-' + String(index + 1).padStart(3, '0'), name: item[0], scope: item[1], updatedAt: '2026-09-' + String(9 - index).padStart(2, '0') + ' ' + (10 + index) + ':20', status: item[2], statusType: item[3] }))
    },
    ensureRows() {
      if (!this.rowStore[this.pageKey]) this.$set(this.rowStore, this.pageKey, this.seedRows())
      return this.rowStore[this.pageKey]
    },
    searchRows() {
      this.currentPage = 1
      this.$modal.msgSuccess('已按条件筛选本地演示数据')
    },
    resetFilters() {
      this.filters = { keyword: '', status: '' }
      this.currentPage = 1
    },
    handlePrimaryAction() {
      if (this.pageKey === 'mockExam') {
        this.comingSoon()
      } else if (this.pageKey === 'exam') {
        this.dialog = { visible: true, type: 'exam', title: '进入正式理论考试' }
      } else if (this.pageKey === 'practice') {
        this.dialog = { visible: true, type: 'practice', title: '提交实践考核材料' }
      } else {
        this.createForm = { name: '', scope: '开发部门', remark: '' }
        this.dialog = { visible: true, type: 'create', title: this.page.primaryAction }
      }
    },
    createRow() {
      if (!this.createForm.name.trim()) {
        this.$modal.msgWarning('请填写名称')
        return
      }
      const rows = this.ensureRows()
      rows.unshift({ code: 'IA-202609-' + String(rows.length + 1).padStart(3, '0'), name: this.createForm.name, scope: this.createForm.scope, updatedAt: '2026-09-09 11:30', status: '草稿', statusType: 'info' })
      this.dialog.visible = false
      this.resetFilters()
      this.$modal.msgSuccess('已新增一条本地演示数据')
    },
    openDetail(row) {
      this.selectedRow = row
      this.dialog = { visible: true, type: 'detail', title: '查看详情' }
    },
    processRow(row) {
      const rows = this.ensureRows()
      const target = rows.find(item => item.code === row.code)
      target.status = '已完成'
      target.statusType = 'success'
      target.updatedAt = '2026-09-09 11:35'
      this.$modal.msgSuccess('已完成本地演示处理，真实状态将在接口接入后同步')
    },
    submitPractice() {
      if (!this.practiceForm.description.trim()) {
        this.$modal.msgWarning('请填写提交说明')
        return
      }
      const rows = this.ensureRows()
      rows.unshift({ code: 'PS-202609-001', name: '实践成果：' + this.practiceForm.description, scope: '实施实习生', updatedAt: '2026-09-09 11:40', status: '处理中', statusType: 'warning' })
      this.dialog.visible = false
      this.$modal.msgSuccess('实践材料已提交到本地演示队列')
    }
  }
}
</script>

<style lang="scss" scoped>
.assessment-page { color: #283544; }
.page-heading { margin-bottom: 18px; padding: 4px 4px 8px; }
.eyebrow { color: #3d82c7; font-size: 13px; margin-bottom: 6px; }
.page-heading h2 { margin: 0 0 8px; font-size: 22px; font-weight: 600; }
.page-heading p { margin: 0; color: #6d7885; line-height: 1.65; }
.heading-actions { text-align: right; padding-top: 20px; }
.metric-row { margin-bottom: 16px; }
.metric { min-height: 112px; padding: 17px; border: 1px solid #e6ebf1; border-left: 4px solid #3d82c7; background: #fff; }
.metric.orange { border-left-color: #d2872f; }.metric.green { border-left-color: #2f9b79; }.metric.purple { border-left-color: #8a67bd; }.metric.red { border-left-color: #cd5b5b; }
.metric span, .metric small { display: block; color: #6d7885; }.metric strong { display: block; margin: 5px 0; font-size: 25px; line-height: 1.1; font-weight: 600; color: #283544; }.metric small { font-size: 12px; }
.content-card, .entry-card, .course-card { border-color: #e6ebf1; border-radius: 4px; }
.card-title, .table-toolbar, .course-footer { display: flex; align-items: center; justify-content: space-between; }.card-title span { font-weight: 600; }
.todo-item { display: flex; align-items: center; gap: 12px; padding: 13px 0; border-bottom: 1px solid #edf0f4; }.todo-item:last-child { border-bottom: 0; }.todo-item > div { flex: 1; }.todo-item p { margin: 5px 0 0; color: #7b8794; font-size: 13px; }.todo-dot { width: 8px; height: 8px; border-radius: 50%; background: #3d82c7; }.todo-dot.orange { background: #d2872f; }.todo-dot.red { background: #cd5b5b; }
.progress-item { margin: 10px 0 19px; }.progress-item > div { display: flex; justify-content: space-between; margin-bottom: 7px; font-size: 13px; }.progress-item b { color: #526171; }
.entry-grid .el-col { margin-bottom: 16px; }.entry-card { min-height: 185px; }.entry-icon { color: #3d82c7; font-size: 26px; }.entry-card h3, .course-card h3 { margin: 13px 0 7px; font-size: 16px; }.entry-card p, .course-card p { min-height: 38px; margin: 0; color: #6d7885; font-size: 13px; line-height: 1.5; }
.course-grid .el-col { margin-bottom: 16px; }.course-card { display: flex; min-height: 190px; }.course-cover { display: flex; align-items: center; justify-content: center; width: 68px; color: #fff; font-size: 28px; background: #3d82c7; }.course-cover.green { background: #2f9b79; }.course-cover.orange { background: #d2872f; }.course-main { flex: 1; padding: 15px; }.course-main h3 { margin-top: 9px; }.course-footer { margin-top: 12px; color: #7b8794; font-size: 12px; }
.agreement-reader { min-height: 250px; }.agreement-reader h3 { margin-top: 0; }.agreement-reader p { color: #596675; line-height: 1.9; }.full-action { width: 100%; margin-top: 22px; }
.rule-form { max-width: 650px; }.form-tip { margin-left: 12px; color: #8a96a3; font-size: 12px; }.filter-form { padding-bottom: 4px; border-bottom: 1px solid #edf0f4; }.table-toolbar { margin: 14px 0; }.pagination-placeholder { display: flex; justify-content: space-between; align-items: center; margin-top: 16px; color: #7b8794; font-size: 13px; }
.dialog-intro { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; color: #778390; font-size: 13px; }.material-preview { display: flex; min-height: 165px; flex-direction: column; align-items: center; justify-content: center; gap: 10px; color: #fff; text-align: center; }.material-preview i { font-size: 36px; }.material-preview strong { font-size: 18px; }.material-preview small { max-width: 300px; line-height: 1.55; color: rgba(255, 255, 255, .84); }.document-preview { background: #2f9b79; }.video-preview { background: #3d82c7; }.dialog-copy { color: #5f6d7b; line-height: 1.7; }.agreement-text { margin-bottom: 16px; padding: 14px; border: 1px solid #e6ebf1; background: #f8fafc; }.agreement-text h3 { margin: 0 0 8px; font-size: 16px; }.agreement-text p { margin: 0; color: #5f6d7b; line-height: 1.7; }
@media (max-width: 767px) { .heading-actions { text-align: left; padding-top: 14px; }.metric-row .el-col { margin-bottom: 12px; }.metric { min-height: 102px; }.course-card { min-height: 160px; }.todo-item { align-items: flex-start; flex-wrap: wrap; }.todo-item > div { min-width: calc(100% - 25px); }.todo-item .el-button { margin-left: 20px; } }
</style>
