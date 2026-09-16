import request from '@/utils/request'

// 考核列表
export function listExam(query) {
  return request({ url: '/business/exam/list', method: 'get', params: query })
}
// 考核详情
export function getExam(id) {
  return request({ url: '/business/exam/' + id, method: 'get' })
}
// 新增考核
export function addExam(data) {
  return request({ url: '/business/exam', method: 'post', data })
}
// 修改考核
export function updateExam(data) {
  return request({ url: '/business/exam', method: 'put', data })
}
// 删除考核
export function delExam(id) {
  return request({ url: '/business/exam/' + id, method: 'delete' })
}
// 发布考核
export function publishExam(id) {
  return request({ url: '/business/exam/publish/' + id, method: 'put' })
}
// 修改考核状态（启用/停用）
export function changeExamStatus(id, status) {
  return request({ url: '/business/exam/status/' + id, method: 'put', params: { status } })
}

// 答卷相关
// 批改列表（部门实习生 + 答卷状态）
export function gradingList(examId) {
  return request({ url: '/business/answer-sheet/grading/' + examId, method: 'get' })
}
// 答卷详情
export function sheetDetail(sheetId) {
  return request({ url: '/business/answer-sheet/detail/' + sheetId, method: 'get' })
}
// 批改（实操题打分）
export function gradeSheet(data) {
  return request({ url: '/business/answer-sheet/grade', method: 'put', data })
}
// 发布成绩
export function publishResult(examId, params) {
  return request({ url: '/business/answer-sheet/publish/' + examId, method: 'put', params })
}

// 实习生端
// 开始考核（抽题）
export function startExam(examId) {
  return request({ url: '/business/answer-sheet/start/' + examId, method: 'post' })
}
// 交卷
export function submitExam(data) {
  return request({ url: '/business/answer-sheet/submit', method: 'post', data })
}
// 实习生考核列表（含结果，按考核性质过滤）
export function myExamList(examMode) {
  return request({ url: '/business/answer-sheet/my', method: 'get', params: examMode ? { examMode } : {} })
}

// 通用文件上传（返回 {url, fileName}）
export function uploadFile(formData) {
  return request({
    url: '/common/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
