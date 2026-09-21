<template>
  <div class="s-page">
    <header class="s-head">
      <div>
        <span class="eyebrow">SUPER ADMIN · OPERATION</span>
        <h1>课程与题库总览</h1>
        <p>
          跨部门核对「培养资料是否齐备」：一眼看出<b>哪个部门缺资料、缺什么</b>。
          本页<b>只读概览</b>；课程 / 题库 / 实操题库的增改在各管理入口进行（超管不限部门）。
        </p>
      </div>
      <div class="s-head-actions">
        <span class="s-ro"><i class="el-icon-view" /> 全局只读</span>
        <span v-if="gaps.length" class="s-ro sample"><i class="el-icon-warning-outline" /> {{ gaps.length }} 项资料缺口</span>
        <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="loadAll">刷新</el-button>
      </div>
    </header>

    <!-- ★ 加载失败 ≠ 没有数据：两个状态两段文案（旧版把失败吞成空数组，看起来就是"暂无课程"） -->
    <div v-if="error" class="s-callout warn">
      <i class="el-icon-warning-outline" />
      <div>
        <b>加载失败</b>
        <p style="margin:4px 0 8px">{{ error }}</p>
        <el-button size="mini" type="primary" @click="loadAll">重试</el-button>
      </div>
    </div>

    <template v-else>
      <!-- ==================== KPI 条 ==================== -->
      <div v-loading="loading" class="s-kpis">
        <div class="s-kpi">
          <div class="lb"><i class="dot" style="background:#1764f5" />课程</div>
          <div class="vl">{{ num(courses.length) }}<small>门</small></div>
          <div class="ft">
            已发布 {{ statusCount.PUBLISHED }} · 草稿 {{ statusCount.DRAFT }}
            <span :class="{ warn: statusCount.DISABLED > 0 }">· 已停用 {{ statusCount.DISABLED }}</span>
          </div>
        </div>
        <div class="s-kpi">
          <div class="lb"><i class="dot" style="background:#7a5af8" />学习项</div>
          <div class="vl">{{ num(sumItem) }}<small>个</small></div>
          <div class="ft">有效章节 {{ num(sumChapter) }} 个 · 口径为课程统计值（<b>已过滤停用项</b>）</div>
        </div>
        <div class="s-kpi">
          <div class="lb"><i class="dot" style="background:#12b76a" />题库</div>
          <div class="vl">{{ num(banks.length) }}<small>个</small></div>
          <div class="ft">
            总题量 <b>{{ num(questionTotal) }}</b> 题
            <data-tag :mock="false" label="实时统计" />
          </div>
        </div>
        <div class="s-kpi">
          <div class="lb"><i class="dot" style="background:#0e7490" />知识点</div>
          <div class="vl">{{ num(knowledgePoints.length) }}<small>个</small></div>
          <div class="ft">
            题量最少：{{ weakestPoint ? weakestPoint.point + '（' + weakestPoint.cnt + ' 题）' : '--' }}
          </div>
        </div>
        <div class="s-kpi">
          <div class="lb"><i class="dot" style="background:#f79009" />实操题</div>
          <div class="vl">{{ num(subjects.length) }}<small>道</small></div>
          <div class="ft">{{ subjectDirs.length }} 个方向 · {{ subjectDepts.length }} 个部门有题</div>
        </div>
        <div class="s-kpi">
          <div class="lb"><i class="dot" style="background:#f04438" />资料缺口</div>
          <div class="vl">{{ num(gaps.length) }}<small>项</small></div>
          <div class="ft" :class="{ bad: gaps.length > 0 }">
            <template v-if="gaps.length">空题库 {{ emptyBankCount }} · 未发布课程 {{ statusCount.DRAFT }}</template>
            <template v-else>资料齐备</template>
          </div>
        </div>
      </div>

      <!-- ==================== 部门 × 资料齐备度矩阵 ==================== -->
      <div class="s-grid">
        <section class="s-card s-c12">
          <div class="s-card-h">
            <div class="tt"><span class="s-idx">阵</span><h3>部门 × 资料齐备度</h3></div>
            <span class="hint">✓ 齐备 · ⚠ 部分 · ✗ 缺失 —— 一眼看出哪个部门缺什么</span>
          </div>

          <div v-if="loading && !matrix.length" class="s-empty"><i class="el-icon-loading" /><span>加载中…</span></div>
          <div v-else-if="!matrix.length" class="s-empty"><i class="el-icon-folder-opened" /><span>暂无部门资料</span></div>
          <table v-else class="s-tbl ready">
            <thead>
              <tr>
                <th style="min-width:120px">部门</th>
                <th class="ctr" style="width:108px">课程（发布/总）</th>
                <th class="ctr" style="width:78px">章节</th>
                <th class="ctr" style="width:78px">学习项</th>
                <th class="ctr" style="width:96px">题库题量</th>
                <th class="ctr" style="width:84px">知识点</th>
                <th class="ctr" style="width:78px">实操题</th>
                <th class="ctr" style="width:96px">正式考核</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in matrix" :key="r.deptName" :class="{ 'row-bad': r.gapCount > 0 }">
                <td class="nm">
                  {{ r.deptName }}
                  <div v-if="r.positionName" class="n1">{{ r.positionName }}</div>
                </td>
                <td class="ctr">
                  <span :class="tone(r.published > 0 ? (r.published === r.courseCount ? 'ok' : 'part') : 'miss')">
                    {{ r.published }} / {{ r.courseCount }}
                  </span>
                </td>
                <td class="ctr"><span :class="tone(r.chapter ? 'ok' : 'miss')">{{ r.chapter }}</span></td>
                <td class="ctr"><span :class="tone(r.item ? 'ok' : 'miss')">{{ r.item }}</span></td>
                <td class="ctr">
                  <span :class="tone(r.questions ? 'ok' : 'miss')">{{ r.questions }}</span>
                  <div v-if="r.thinBanks" class="n1 bad">{{ r.thinBanks }} 个空库</div>
                </td>
                <td class="ctr"><span :class="tone(r.points ? 'ok' : 'miss')">{{ r.points }}</span></td>
                <td class="ctr"><span :class="tone(r.subjects ? 'ok' : 'miss')">{{ r.subjects }}</span></td>
                <td class="ctr">
                  <span :class="tone(r.exams ? 'ok' : 'miss')">{{ r.exams }}</span>
                  <div v-if="!r.exams" class="n1">未配置</div>
                </td>
              </tr>
            </tbody>
          </table>
          <p class="s-note">
            ★ 口径：<b>章节 / 学习项</b>取自课程的 <code>chapterCount</code>/<code>itemCount</code>（后端已过滤停用项）；
            <b>题库题量 / 知识点</b>为<b>实时统计</b>（见下方题库卡的口径说明）；<b>实操题 / 正式考核</b>按部门归属统计。<br />
            ★ <b>题库按「部门」建，不按岗位</b>（<code>question.position_id</code> 全为 NULL）；因部门↔岗位是 1:1，此处以部门为行、岗位名为副标题。
          </p>
        </section>
      </div>

      <!-- ==================== 课程发布总览 + 题库健康度 ==================== -->
      <div class="s-grid">
        <section class="s-card s-c7">
          <div class="s-card-h">
            <div class="tt"><span class="s-idx">课</span><h3>课程发布总览</h3></div>
            <span class="hint">{{ filteredCourses.length }} / {{ courses.length }} 门</span>
          </div>

          <div class="toolbar">
            <span
              v-for="f in statusFilters" :key="f.key"
              class="chip" :class="{ on: courseFilter === f.key }"
              @click="courseFilter = f.key"
            >{{ f.label }} <b>{{ f.cnt }}</b></span>
            <span class="spacer" />
            <el-select v-model="deptFilter" size="mini" clearable placeholder="全部部门" style="width:150px">
              <el-option v-for="d in deptOptions" :key="d" :label="d" :value="d" />
            </el-select>
          </div>

          <div class="scroll">
            <table v-if="filteredCourses.length" class="s-tbl">
              <thead>
                <tr>
                  <th style="min-width:190px">课程</th>
                  <th style="width:96px">部门</th>
                  <th class="ctr" style="width:78px">章/项</th>
                  <th style="min-width:150px">学习覆盖</th>
                  <th class="ctr" style="width:80px">状态</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="c in filteredCourses" :key="c.id">
                  <td class="nm">
                    {{ c.courseName }}
                    <div class="n1">{{ c.isRequired ? '必学' : '选修' }}{{ c.courseType === 'PRACTICAL' ? ' · 实操' : '' }}</div>
                  </td>
                  <td>{{ c.deptName || '—' }}</td>
                  <td class="ctr">{{ num(c.chapterCount) }}/{{ num(c.itemCount) }}</td>
                  <td>
                    <!-- 学习覆盖：有应交人数才显示比率；分母 0 → 不显示比率，避免伪装 0% -->
                    <template v-if="num(c.expectedStudentCount) > 0">
                      <div class="mini">
                        <span class="track"><i :style="{ width: clamp(c.avgCompletionRate) + '%' }" /></span>
                        <b>{{ Math.round(Number(c.avgCompletionRate) || 0) }}%</b>
                      </div>
                      <span class="n1">{{ num(c.studentCount) }}/{{ num(c.expectedStudentCount) }} 人已学</span>
                    </template>
                    <span v-else class="none">无应交人 <data-tag :none="true" label="无样本" /></span>
                  </td>
                  <td class="ctr"><span class="s-badge" :class="statusTone(c.status)">{{ statusText(c.status) }}</span></td>
                </tr>
              </tbody>
            </table>
            <div v-else class="s-empty"><i class="el-icon-reading" /><span>当前筛选下没有课程</span></div>
          </div>

          <div class="dist">
            <span class="mut">状态分布：</span>
            <span v-for="f in statusFilters.slice(1)" :key="f.key" class="dist-item">
              <i class="sw" :style="{ background: f.color }" />{{ f.label }} {{ f.cnt }}
            </span>
            <span class="spacer" />
            <span class="op" @click="goCourseAdmin">课程管理 →</span>
          </div>
          <p class="s-note">
            「学习覆盖」= <code>studentCount</code> / <code>expectedStudentCount</code> 与岗位级完成率（复用 <code>CourseMapper</code> 既有口径）。
            <b>应交人数为 0 的课程不显示百分比</b>，避免把"没人该学"画成"0% 完成"。
          </p>
        </section>

        <section class="s-card s-c5">
          <div class="s-card-h">
            <div class="tt"><span class="s-idx g">库</span><h3>题库健康度</h3></div>
            <span class="hint">{{ banks.length }} 个库 · {{ emptyBankCount }} 个空库</span>
          </div>

          <div class="scroll">
            <table v-if="banks.length" class="s-tbl">
              <thead>
                <tr>
                  <th style="min-width:140px">题库</th>
                  <th class="ctr" style="width:60px">题量</th>
                  <th class="ctr" style="width:64px">知识点</th>
                  <th class="ctr" style="width:88px">健康度</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="b in banks" :key="b.id" class="row-link" @click="goBank(b.id)">
                  <td class="nm">
                    {{ b.bankName }}
                    <div class="n1">
                      {{ b.bankType === 'FORMAL' ? '正式' : '模拟' }}
                      <template v-if="bankNeed(b.id)"> · 组卷需 {{ bankNeed(b.id) }} 题</template>
                    </div>
                  </td>
                  <td class="ctr">
                    <b :class="{ bad: !bankQ[b.id] }">{{ num(bankQ[b.id]) }}</b>
                    <!-- 难度分布：只有简单题也是风险，所以压在题量下方用三色条表达 -->
                    <div v-if="bankQ[b.id]" class="diff" :title="diffTitle(b.id)">
                      <i
                        v-for="d in diffRows(b.id)" :key="d.key"
                        :class="d.cls" :style="{ width: d.pct + '%' }"
                      />
                    </div>
                  </td>
                  <td class="ctr">{{ num(bankKp[b.id]) }}</td>
                  <td class="ctr"><span class="s-badge" :class="healthTone(b)">{{ healthText(b) }}</span></td>
                </tr>
              </tbody>
            </table>
            <div v-else class="s-empty"><i class="el-icon-collection" /><span>暂无题库</span></div>
          </div>

          <div class="s-callout warn">
            <i class="el-icon-warning-outline" />
            <div>
              <b>为什么要「实时统计」题量</b>
              <p style="margin:4px 0 0">
                <code>question_bank.question_count</code> 是<b>冗余字段且长期未维护</b> —— 实测
                <b>5 个库该字段为 0，而实际各有 48~49 题</b>。旧版直接读它，会把有题的库显示成「空库」。
                本页改为对题目列表实时计数。
              </p>
            </div>
          </div>

          <p class="s-note">
            <b>健康度判据</b>：<code>need</code> = 使用该库的考核所要求的题量（无考核使用则按 20 题估算）；<br />
            题量 <code>0</code> → <b>空库</b>；<code>≥ need×2</code> → <b>充足</b>；<code>≥ need</code> → <b>够用</b>；否则 <b>偏少</b>。
            点题库行可进<b>题库详情</b>。
          </p>
        </section>
      </div>

      <!-- ==================== 资料缺口清单 ==================== -->
      <div class="s-grid">
        <section class="s-card s-c12">
          <div class="s-card-h">
            <div class="tt"><span class="s-idx o">缺</span><h3>资料缺口清单</h3></div>
            <span class="hint">共 {{ gaps.length }} 项 · 按严重度排序</span>
          </div>

          <div v-if="!gaps.length" class="s-empty"><i class="el-icon-circle-check" /><span>当前没有资料缺口</span></div>
          <div v-else class="s-steps">
            <div v-for="(g, i) in gaps" :key="i" class="s-step">
              <span class="mark" :class="{ now: g.level === 'HIGH' }">{{ g.level === 'HIGH' ? '!' : '·' }}</span>
              <div class="txt">
                <b>{{ g.title }}</b>
                <span>{{ g.detail }}</span>
              </div>
              <span
                v-if="g.link && routeOk(g.link)"
                class="op"
                @click="go(g.link)"
              >{{ g.linkText || '去处理' }} →</span>
              <span v-else-if="g.tag" class="s-badge warn">{{ g.tag }}</span>
            </div>
          </div>
          <p class="s-note">
            ★ 缺口全部由前端从上面各列表<b>实时聚合</b>（复用列表接口，<b>不为此新增统计接口</b>），
            所以不会出现「统计与列表对不上」。
          </p>
        </section>
      </div>
    </template>
  </div>
</template>

<script>
/**
 * 超管「课程与题库总览」V2（2026-09-20 重做）
 *
 * 从「两张各自独立的清单」升级为「培养资料齐备度看板」：
 *   KPI 条 → 部门 × 资料齐备度矩阵 → 课程发布总览 / 题库健康度 → 资料缺口清单
 *
 * 数据源（**全部复用既有列表接口，零新增接口** —— 体量很小：题目 289 / 课程 12 / 题库 10 / 实操 16 / 考核 2）：
 *   /business/question/list、/business/course/list、/business/question-bank/list、
 *   /business/practice-subject/list、/business/exam/list
 *
 * ★★ 本版修掉旧版的三个真问题
 *   1. **题量全错**：旧版读 `question_bank.questionCount`（冗余字段、长期未维护），
 *      实测 5 个库该字段为 0 而实际各有 48~49 题 → 有题的库被显示成「空库」。
 *      本版改为对题目列表**实时计数**。
 *   2. **失败被吞成空数组**（`.catch(() => this.x = [])`）：403/500 都会显示成「暂无课程」。
 *      本版区分「加载失败」与「确实没有」，失败给原因 + 重试。
 *   3. **文案与代码不一致**：旧版注释写「按岗位考核配置题量、未配置按 20 题」，代码却硬编码 `>= 40`。
 *      本版判据与文案统一（need 从使用该库的考核取，兜底 20）。
 */
import { listQuestion } from '@/api/business/question'
import { listCourse } from '@/api/business/course'
import { listBank } from '@/api/business/questionBank'
import { listPracticeSubject } from '@/api/business/practiceSubject'
import { listExam } from '@/api/business/exam'

/** 难度枚举实测有两套「简单」（EASY 111 / SIMPLE 22）—— 归一后展示 */
const DIFF_ALIAS = { EASY: 'SIMPLE', SIMPLE: 'SIMPLE', MEDIUM: 'MEDIUM', HARD: 'HARD' }
/** 组卷题量兜底（题库没被任何考核使用时） */
const DEFAULT_NEED = 20

export default {
  name: 'SuperOpsCourses',
  data() {
    return {
      loading: false,
      error: '',
      courses: [],
      banks: [],
      questions: [],
      subjects: [],
      exams: [],
      courseFilter: 'ALL',
      deptFilter: ''
    }
  },
  computed: {
    statusCount() {
      const c = { PUBLISHED: 0, DRAFT: 0, DISABLED: 0 }
      this.courses.forEach(x => { c[x.status] = (c[x.status] || 0) + 1 })
      return c
    },
    statusFilters() {
      return [
        { key: 'ALL', label: '全部', cnt: this.courses.length, color: '#667085' },
        { key: 'PUBLISHED', label: '已发布', cnt: this.statusCount.PUBLISHED || 0, color: '#12b76a' },
        { key: 'DRAFT', label: '草稿', cnt: this.statusCount.DRAFT || 0, color: '#f79009' },
        { key: 'DISABLED', label: '已停用', cnt: this.statusCount.DISABLED || 0, color: '#f04438' }
      ]
    },
    deptOptions() {
      const s = {}
      this.courses.forEach(c => { if (c.deptName) s[c.deptName] = 1 })
      return Object.keys(s)
    },
    filteredCourses() {
      return this.courses.filter(c => {
        if (this.courseFilter !== 'ALL' && c.status !== this.courseFilter) return false
        if (this.deptFilter && c.deptName !== this.deptFilter) return false
        return true
      })
    },
    sumChapter() {
      return this.courses.reduce((s, c) => s + (Number(c.chapterCount) || 0), 0)
    },
    sumItem() {
      return this.courses.reduce((s, c) => s + (Number(c.itemCount) || 0), 0)
    },

    /** ★ 题库题量：实时计数（不用 question_bank.questionCount） */
    bankQ() {
      const m = {}
      this.questions.forEach(q => { m[q.bankId] = (m[q.bankId] || 0) + 1 })
      return m
    },
    /** 题库知识点数（去重） */
    bankKp() {
      const m = {}
      this.questions.forEach(q => {
        if (!q.knowledgePoint) return
        if (!m[q.bankId]) m[q.bankId] = {}
        m[q.bankId][q.knowledgePoint] = 1
      })
      const out = {}
      Object.keys(m).forEach(k => { out[k] = Object.keys(m[k]).length })
      return out
    },
    questionTotal() {
      return this.questions.length
    },
    /** 组卷需求：题库 → 使用它的考核要求的最大题量（无则 0，页面按兜底值展示） */
    bankNeedMap() {
      const m = {}
      this.exams.forEach(e => {
        if (!e.bankId) return
        m[e.bankId] = Math.max(m[e.bankId] || 0, Number(e.questionCount) || 0)
      })
      return m
    },
    /** 知识点分布（题量升序 —— 最缺的在前） */
    knowledgePoints() {
      const m = {}
      this.questions.forEach(q => {
        const k = q.knowledgePoint || '(未标注)'
        m[k] = (m[k] || 0) + 1
      })
      return Object.keys(m)
        .map(k => ({ point: k, cnt: m[k] }))
        .sort((a, b) => a.cnt - b.cnt)
    },
    weakestPoint() {
      return this.knowledgePoints.length ? this.knowledgePoints[0] : null
    },
    emptyBankCount() {
      return this.banks.filter(b => !this.bankQ[b.id]).length
    },
    subjectDirs() {
      const s = {}
      this.subjects.forEach(x => { if (x.direction) s[x.direction] = 1 })
      return Object.keys(s)
    },
    subjectDepts() {
      const s = {}
      this.subjects.forEach(x => { if (x.deptName) s[x.deptName] = 1 })
      return Object.keys(s)
    },

    /** ★ 部门 × 资料齐备度矩阵 */
    matrix() {
      const map = {}
      const ensure = name => {
        if (!name) return null
        if (!map[name]) {
          map[name] = {
            deptName: name, positionName: '', courseCount: 0, published: 0,
            chapter: 0, item: 0, questions: 0, points: 0, subjects: 0, exams: 0,
            thinBanks: 0, kpSet: {}
          }
        }
        return map[name]
      }
      // 部门↔岗位（课程带 positionName；题库/实操/考核带 deptName，用名字对齐）
      this.courses.forEach(c => {
        const r = ensure(c.deptName)
        if (!r) return
        r.courseCount++
        if (c.status === 'PUBLISHED') r.published++
        r.chapter += Number(c.chapterCount) || 0
        r.item += Number(c.itemCount) || 0
        if (!r.positionName && c.positionName) r.positionName = c.positionName
      })
      this.questions.forEach(q => {
        const r = ensure(q.deptName)
        if (!r) return
        r.questions++
        if (q.knowledgePoint) r.kpSet[q.knowledgePoint] = 1
      })
      this.subjects.forEach(s => {
        const r = ensure(s.deptName)
        if (r) r.subjects++
      })
      this.exams.forEach(e => {
        if (!e.deptName) return
        const r = ensure(e.deptName)
        if (!r) return
        if (e.examMode === 'FORMAL') r.exams++
      })
      this.banks.forEach(b => {
        const r = ensure(b.deptName)
        if (!r) return
        if (!this.bankQ[b.id]) r.thinBanks++
      })
      return Object.keys(map).map(k => {
        const r = map[k]
        r.points = Object.keys(r.kpSet).length
        r.gapCount = (!r.published ? 1 : 0) + (!r.questions ? 1 : 0) + (!r.subjects ? 1 : 0)
        return r
      }).sort((a, b) => b.gapCount - a.gapCount || a.deptName.localeCompare(b.deptName))
    },

    /** ★ 资料缺口清单（实时聚合，按严重度排序） */
    gaps() {
      const out = []
      // ① 空题库（HIGH）
      this.banks.forEach(b => {
        if (!this.bankQ[b.id]) {
          out.push({
            level: 'HIGH',
            title: '空题库：' + b.bankName,
            detail: (b.deptName || '未知部门') + ' · ' + (b.bankType === 'FORMAL' ? '正式' : '模拟')
              + '题库 · 0 题 → 无法组卷，该类型考核无法开展',
            link: '/super/ops/bank-detail/' + b.id, linkText: '去补题'
          })
        }
      })
      // ② 无人学的课程（MEDIUM）—— 有应交人但覆盖为 0
      this.courses.forEach(c => {
        if (c.status === 'PUBLISHED' && Number(c.expectedStudentCount) > 0 && !Number(c.studentCount)) {
          out.push({
            level: 'MEDIUM',
            title: '课程无人学习：' + c.courseName,
            detail: (c.deptName || '—') + ' · 应交 ' + c.expectedStudentCount + ' 人，实际 0 人已学',
            link: '/super/ops/course-admin', linkText: '去课程管理'
          })
        }
      })
      // ③ 草稿未发布（MEDIUM）
      const drafts = this.courses.filter(c => c.status === 'DRAFT')
      if (drafts.length) {
        out.push({
          level: 'MEDIUM',
          title: drafts.length + ' 门课程仍是草稿（未发布，实习生看不到）',
          detail: drafts.map(c => c.courseName + '（' + (c.deptName || '—') + '）').join('、'),
          link: '/super/ops/course-admin', linkText: '去发布'
        })
      }
      // ④ 已停用课程（LOW）
      const disabled = this.courses.filter(c => c.status === 'DISABLED')
      if (disabled.length) {
        out.push({
          level: 'LOW', tag: '已停用',
          title: disabled.length + ' 门课程已停用',
          detail: disabled.map(c => c.courseName + '（' + (c.deptName || '—') + '）').join('、')
        })
      }
      // ⑤ 知识点题量过低（MEDIUM）—— 组卷随机性风险
      const thin = this.knowledgePoints.filter(k => k.cnt < 12)
      if (thin.length) {
        out.push({
          level: 'MEDIUM',
          title: thin.length + ' 个知识点题量偏低（< 12 题），按知识点组卷时易重复抽题',
          detail: thin.map(k => k.point + '（' + k.cnt + ' 题）').join('、'),
          link: '/super/ops/bank-admin', linkText: '去题库管理'
        })
      }
      // ⑥ 无正式考核配置的部门（LOW）
      const noExam = this.matrix.filter(r => !r.exams && r.courseCount > 0)
      if (noExam.length) {
        out.push({
          level: 'LOW', tag: '未配置',
          title: noExam.length + ' 个部门没有正式考核',
          detail: noExam.map(r => r.deptName).join('、') + ' —— 无正式考核 = 无法产出正式成绩、无法转正'
        })
      }
      return out
    }
  },
  created() {
    this.loadAll()
  },
  methods: {
    num(v) {
      return (v === null || v === undefined) ? 0 : v
    },
    clamp(v) {
      const n = Number(v)
      if (isNaN(n)) return 0
      return Math.max(0, Math.min(100, n))
    },
    statusText(s) {
      return { DRAFT: '草稿', PUBLISHED: '已发布', DISABLED: '已停用' }[s] || s
    },
    statusTone(s) {
      return { DRAFT: 'warn', PUBLISHED: 'ok', DISABLED: 'red' }[s] || ''
    },
    /** 三态样式：ok 齐备 / part 部分 / miss 缺失 */
    tone(t) {
      return { ok: 'ok-t', part: 'part-t', miss: 'miss-t' }[t] || ''
    },
    bankNeed(bankId) {
      const n = this.bankNeedMap[bankId]
      return n || DEFAULT_NEED
    },
    healthText(b) {
      const n = Number(this.bankQ[b.id] || 0)
      if (n === 0) return '空库'
      const need = this.bankNeed(b.id)
      if (n >= need * 2) return '充足'
      if (n >= need) return '够用'
      return '偏少'
    },
    healthTone(b) {
      const n = Number(this.bankQ[b.id] || 0)
      if (n === 0) return 'red'
      const need = this.bankNeed(b.id)
      if (n >= need * 2) return 'ok'
      if (n >= need) return 'blue'
      return 'warn'
    },
    /** 难度分布（归一「简单」的两套枚举） */
    diffDist(bankId) {
      const m = {}
      this.questions.filter(q => q.bankId === bankId).forEach(q => {
        const k = DIFF_ALIAS[q.difficulty] || q.difficulty || '未标'
        m[k] = (m[k] || 0) + 1
      })
      return m
    },
    diffCls(k) {
      return { SIMPLE: 'd-s', MEDIUM: 'd-m', HARD: 'd-h' }[k] || 'd-n'
    },
    /** 三色条的分段（简单 → 中 → 难 → 未标） */
    diffRows(bankId) {
      const dist = this.diffDist(bankId)
      const total = this.num(this.bankQ[bankId]) || 1
      return ['SIMPLE', 'MEDIUM', 'HARD', '未标']
        .filter(k => dist[k])
        .map(k => ({ key: k, cls: 'd ' + this.diffCls(k), pct: dist[k] / total * 100 }))
    },
    diffTitle(bankId) {
      const dist = this.diffDist(bankId)
      const label = { SIMPLE: '简单', MEDIUM: '中', HARD: '难', 未标: '未标注' }
      return Object.keys(dist).map(k => (label[k] || k) + ' ' + dist[k] + ' 题').join(' · ')
    },
    routeOk(path) {
      return !!path && this.$router.resolve(path).route.matched.length > 0
    },
    go(path) {
      if (!this.routeOk(path)) return
      this.$router.push(path).catch(() => {})
    },
    goBank(bankId) {
      this.go('/super/ops/bank-detail/' + bankId)
    },
    goCourseAdmin() {
      this.go('/super/ops/course-admin')
    },
    loadAll() {
      this.loading = true
      this.error = ''
      const q = { pageNum: 1, pageSize: 500 }
      Promise.all([
        listCourse({ pageNum: 1, pageSize: 200 }),
        listBank({ pageNum: 1, pageSize: 200 }),
        listQuestion(q),
        listPracticeSubject({ pageNum: 1, pageSize: 200 }),
        listExam({ pageNum: 1, pageSize: 100 })
      ]).then(([c, b, qs, s, e]) => {
        this.courses = c.rows || []
        this.banks = b.rows || []
        this.questions = qs.rows || []
        this.subjects = s.rows || []
        this.exams = e.rows || []
      }).catch(err => {
        // ★ 不吞失败：403/500 必须说出来，不能显示成「暂无课程」
        this.error = (err && err.message) ? err.message
          : '接口请求失败（可能是权限不足或后端未启动），请稍后重试'
        this.courses = []
        this.banks = []
        this.questions = []
        this.subjects = []
        this.exams = []
      }).finally(() => { this.loading = false })
    }
  }
}
</script>

<style lang="scss" scoped>
@import '~@/assets/styles/super-module.scss';

/* ⚠️ 不能出现 :root（scoped 会编译成 :root[data-v-x]，令牌全失效） */

/* 工具条 */
.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;

  .spacer { flex: 1; }
}
.chip {
  padding: 3px 10px;
  color: $ink-3;
  background: #f2f4f7;
  font-size: 12px;
  border-radius: 10px;
  cursor: pointer;

  b { color: $ink-2; }
  &:hover { background: #e8ebf1; }
  &.on { color: #fff; background: $blue; b { color: #fff; } }
}

.scroll { max-height: 430px; overflow-y: auto; }

/* 学习覆盖迷你条 */
.mini {
  display: flex;
  align-items: center;
  gap: 7px;

  .track {
    height: 7px;
    flex: 1;
    min-width: 40px;
    overflow: hidden;
    background: #edf1f5;
    border-radius: 4px;
    i { display: block; height: 100%; background: $blue; border-radius: 4px; }
  }
  b { min-width: 40px; color: $ink-2; font-size: 12px; text-align: right; font-variant-numeric: tabular-nums; }
}

/* 三态格 */
.ok-t { color: #1a7a58; font-weight: 600; }
.part-t { color: $orange-ink; font-weight: 600; }
.miss-t { color: #b42318; font-weight: 700; }

/* 难度分布三色条 */
.diff {
  display: flex;
  height: 4px;
  margin-top: 4px;
  overflow: hidden;
  border-radius: 3px;
  background: #edf1f5;

  i { display: block; height: 100%; }
  .d-s { background: $green; }
  .d-m { background: $orange; }
  .d-h { background: $red; }
  .d-n { background: #b7c1cc; }
}

.ready tbody tr.row-bad { background: #fffbf7; }
.row-link { cursor: pointer; }
.row-link:hover { background: #f7fbff; }

.n1 { color: $ink-4; font-size: 10.5px; }
.none { color: $ink-4; font-size: 12px; }
.bad { color: #b42318; }
.mut { color: $ink-4; }
.op { color: $blue; cursor: pointer; }

/* 状态分布 */
.dist {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid $line-2;
  font-size: 12px;

  .spacer { flex: 1; }
  .dist-item { display: inline-flex; align-items: center; gap: 6px; color: $ink-3; }
  .sw { width: 9px; height: 9px; border-radius: 3px; }
}
</style>
