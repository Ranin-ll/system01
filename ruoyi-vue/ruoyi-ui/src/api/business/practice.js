import request from '@/utils/request'

// 开始模拟考核（抽10题）
export function startPractice() {
  return request({ url: '/business/practice/start', method: 'post' })
}

// 提交模拟考核并判分
export function submitPractice(data) {
  return request({ url: '/business/practice/submit', method: 'post', data })
}

// 查询本人模拟考核记录
export function myPracticeRecords() {
  return request({ url: '/business/practice/my', method: 'get' })
}

// 查看某条模拟记录的题目与作答详情
export function practiceRecordDetail(recordId) {
  return request({ url: '/business/practice/record/' + recordId, method: 'get' })
}

// 模拟考核 · 实操练习列表（本部门已发布，只读）
export function listPracticeSubjects() {
  return request({ url: '/business/practice-subject/published', method: 'get' })
}

// 模拟考核 · 单条实操题详情（独立详情页使用，仅本部门已发布）
export function getPracticeSubjectDetail(id) {
  return request({ url: '/business/practice-subject/published/' + id, method: 'get' })
}
