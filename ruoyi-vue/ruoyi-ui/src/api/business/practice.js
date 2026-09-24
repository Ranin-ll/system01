import request from '@/utils/request'

// 开始模拟考核：按模块下选定的理论模拟考核抽题（examId 为空则回退为本部门最近发布的一套配置）
export function startPractice(examId) {
  return request({
    url: '/business/practice/start',
    method: 'post',
    params: examId ? { examId } : {}
  })
}

// 提交模拟考核并判分（examId 需与开考时一致，保证判分口径相同）
export function submitPractice(data) {
  return request({ url: '/business/practice/submit', method: 'post', data })
}

// 模拟考核 · 本部门某模块下「已发布」的理论模拟考核列表（先选模块再选考核）
export function listPracticeExams(moduleId) {
  return request({
    url: '/business/practice/exams',
    method: 'get',
    params: moduleId ? { moduleId } : {}
  })
}

// 查询本人模拟考核记录（moduleId 非空时只返回该模块下的记录）
export function myPracticeRecords(moduleId) {
  return request({
    url: '/business/practice/my',
    method: 'get',
    params: moduleId ? { moduleId } : {}
  })
}

// 查看某条模拟记录的题目与作答详情
export function practiceRecordDetail(recordId) {
  return request({ url: '/business/practice/record/' + recordId, method: 'get' })
}

// 模拟考核 · 实操练习列表（本部门已发布，只读；★ 2026-09-23 起不再分模块）
export function listPracticeSubjects() {
  return request({ url: '/business/practice-subject/published', method: 'get' })
}

// 模拟考核 · 单条实操题详情（独立详情页使用，仅本部门已发布）
export function getPracticeSubjectDetail(id) {
  return request({ url: '/business/practice-subject/published/' + id, method: 'get' })
}
