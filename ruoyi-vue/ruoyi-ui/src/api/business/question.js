import request from '@/utils/request'

// 查询题目列表
export function listQuestion(query) {
  return request({ url: '/business/question/list', method: 'get', params: query })
}

// 查询题目详细
export function getQuestion(id) {
  return request({ url: '/business/question/' + id, method: 'get' })
}

// 新增题目
export function addQuestion(data) {
  return request({ url: '/business/question', method: 'post', data })
}

// 修改题目
export function updateQuestion(data) {
  return request({ url: '/business/question', method: 'put', data })
}

// 删除题目
export function delQuestion(id) {
  return request({ url: '/business/question/' + id, method: 'delete' })
}

// 下载导入模板
export function downloadTemplate() {
  return request({ url: '/business/question/template', method: 'get', responseType: 'blob' })
}

// 题目批量导入（deptId = 目标部门题池；部门账号传自己部门ID即可，后端会强制本部门）
export function importQuestions(deptId, formData) {
  return request({
    url: '/business/question/import/' + deptId,
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 导出本部门题目（Excel；表头与导入模板一致，导出的文件可直接再导入）
export function exportQuestions(deptId) {
  return request({
    url: '/business/question/export/' + deptId,
    method: 'get',
    responseType: 'blob'
  })
}

// 下载题目导入模板
export function downloadQuestionTemplate() {
  return request({ url: '/business/question/template', method: 'get', responseType: 'blob' })
}

// 实习生抽题（不含答案；deptId = 本部门题池）
export function previewQuestions(deptId, limit) {
  return request({ url: '/business/question/preview/' + deptId, method: 'get', params: { limit } })
}

// 实习生提交判分
export function submitAnswers(data) {
  return request({ url: '/business/question/submit', method: 'post', data })
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