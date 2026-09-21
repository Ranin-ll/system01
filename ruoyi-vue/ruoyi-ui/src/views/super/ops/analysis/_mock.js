/**
 * 培养分析看板 · **考核类示例数据**（唯一存放处）
 *
 * ★ 为什么单独一个文件
 *   题库与考核相关模块**将有同事的大改动、等其分支上传后合并**，所以这部分先做前端示例。
 *   为了合并时**只换数据源、不动模板**，示例值一律集中在本文件，绝不硬编码进各卡片的模板。
 *   自检命令：`grep -rn "_mock\|MOCK" src/views/super/ops/analysis/`
 *   结果应当只有本文件与 DataTag 的调用处。
 *
 * ★ 数值来源
 *   下方数值**不是编的**，是 2026-09-20 从库里实测出来的真实考核数据
 *   （practice_record 8 条 / practice_record_item 80 题次 / answer_sheet 1 张已发布）。
 *   之所以仍标「示例」，是因为**取数接口还不该在现在这个时点接**（表结构会变）。
 *
 * ★ 合并后怎么改
 *   后端在 `/business/super/analysis/dept-matrix` 与 `/stage-progress` 里补上这几列，
 *   页面里 `pick(real, mock)` 会自动优先真值、并去掉橙标 —— 不需要改任何模板。
 */

/** 与后端约定：示例数据统一带这个标记，前端据此渲染橙标 */
export const MOCK_FLAG = true

/** 部门级示例（key = deptId）—— 模拟考核 / 正式考核 / 知识点 */
export const MOCK_DEPT = {
  103: { practiceCount: null, practiceAvg: null, formalPublished: null, knowledgeItems: null, knowledgeCorrect: null },
  104: { practiceCount: 7, practiceAvg: 4.43, formalPublished: 0, knowledgeItems: 70, knowledgeCorrect: 31 },
  105: { practiceCount: 1, practiceAvg: 2.0, formalPublished: 1, knowledgeItems: 10, knowledgeCorrect: 2 },
  106: { practiceCount: null, practiceAvg: null, formalPublished: null, knowledgeItems: null, knowledgeCorrect: null },
  107: { practiceCount: null, practiceAvg: null, formalPublished: null, knowledgeItems: null, knowledgeCorrect: null }
}

/** 逐人示例：正式考试是否通过（1 通过 / 0 未通过 / null 无记录） */
export const MOCK_INTERN = {
  127: { formalPassed: 0 } // 设计部门实习生 · 唯一一张已发布答卷（0 分未通过）
}

/** 逐人示例：模拟考核逐场（实测 8 条记录，集中在 126 / 127 两人） */
export const MOCK_INTERN_PRACTICE = {
  126: [
    { date: '2026-09-16 18:17', correct: 3, total: 10, score: 3 },
    { date: '2026-09-17 11:47', correct: 3, total: 10, score: 3 },
    { date: '2026-09-17 11:52', correct: 5, total: 10, score: 5 },
    { date: '2026-09-17 11:53', correct: 5, total: 10, score: 5 },
    { date: '2026-09-17 11:55', correct: 5, total: 10, score: 5 },
    { date: '2026-09-17 11:59', correct: 5, total: 10, score: 5 },
    { date: '2026-09-17 12:01', correct: 5, total: 10, score: 5 }
  ],
  127: [
    { date: '2026-09-16 22:59', correct: 2, total: 10, score: 2 }
  ]
}

/** 逐人示例：本人知识点掌握（126 即开发部门那 70 题次；其余人无明细） */
export const MOCK_INTERN_KNOWLEDGE = {
  126: [
    { point: 'Redis', items: 11, correct: 0 },
    { point: 'MySQL', items: 9, correct: 0 },
    { point: 'Linux', items: 5, correct: 0 },
    { point: 'ECharts', items: 4, correct: 0 },
    { point: 'Java', items: 4, correct: 0 },
    { point: 'nginx', items: 2, correct: 0 },
    { point: '孪生体导入', items: 1, correct: 0 },
    { point: '机柜布局', items: 6, correct: 3 },
    { point: 'Docker', items: 9, correct: 9 },
    { point: '若依', items: 8, correct: 8 },
    { point: '答题规则', items: 5, correct: 5 },
    { point: '森大屏', items: 2, correct: 2 },
    { point: '承重规范', items: 2, correct: 2 },
    { point: 'Maven', items: 2, correct: 2 }
  ]
}

/** 知识点掌握示例（部门 × 知识点，用于热力卡） */
export const MOCK_KNOWLEDGE = {
  104: [
    { point: 'Redis', items: 11, correct: 0 },
    { point: 'MySQL', items: 9, correct: 0 },
    { point: 'Linux', items: 5, correct: 0 },
    { point: 'ECharts', items: 4, correct: 0 },
    { point: 'Java', items: 4, correct: 0 },
    { point: 'nginx', items: 2, correct: 0 },
    { point: '机柜布局', items: 6, correct: 3 },
    { point: 'Docker', items: 9, correct: 9 },
    { point: '若依', items: 8, correct: 8 },
    { point: '答题规则', items: 5, correct: 5 }
  ],
  105: [
    { point: '承重规范', items: 2, correct: 2 },
    { point: '孪生体导入', items: 2, correct: 1 },
    { point: '森大屏', items: 2, correct: 2 },
    { point: 'Maven', items: 2, correct: 2 },
    { point: '若依', items: 2, correct: 0 }
  ]
}

/**
 * 取「真值优先、缺则示例」并返回是否用了示例
 * @param {*} real 后端返回的真值
 * @param {*} mock 示例值
 * @returns {{value:*, mock:boolean}} mock=true 表示当前展示的是示例值，UI 要打橙标
 */
export function pick(real, mock) {
  if (real !== null && real !== undefined && real !== '') {
    return { value: real, mock: false }
  }
  if (mock !== null && mock !== undefined) {
    return { value: mock, mock: true }
  }
  return { value: null, mock: false }
}

/** 按部门取示例考核数据（未知部门返回全 null，不伪造） */
export function mockDept(deptId) {
  return MOCK_DEPT[deptId] || {
    practiceCount: null, practiceAvg: null, formalPublished: null,
    knowledgeItems: null, knowledgeCorrect: null
  }
}

/** 按人取示例考核数据 */
export function mockIntern(userId) {
  return MOCK_INTERN[userId] || {}
}

/** 按部门取示例知识点明细 */
export function mockKnowledge(deptId) {
  return MOCK_KNOWLEDGE[deptId] || []
}

/** 按人取示例：模拟考核逐场 / 知识点 */
export function mockInternPractice(userId) {
  return MOCK_INTERN_PRACTICE[userId] || []
}

export function mockInternKnowledge(userId) {
  return MOCK_INTERN_KNOWLEDGE[userId] || []
}
