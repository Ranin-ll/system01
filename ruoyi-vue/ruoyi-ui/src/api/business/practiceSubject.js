import request from '@/utils/request'

// 模拟实操题列表（管理端）
export function listPracticeSubject(query) {
  return request({ url: '/business/practice-subject/list', method: 'get', params: query })
}

// 模拟实操题详情
export function getPracticeSubject(id) {
  return request({ url: '/business/practice-subject/' + id, method: 'get' })
}

// 发布模拟实操题
export function addPracticeSubject(data) {
  return request({ url: '/business/practice-subject', method: 'post', data })
}

// 编辑模拟实操题
export function updatePracticeSubject(data) {
  return request({ url: '/business/practice-subject', method: 'put', data })
}

// 启用 / 停用
export function changePracticeSubjectStatus(id, status) {
  return request({ url: '/business/practice-subject/status/' + id, method: 'put', params: { status } })
}

// 删除模拟实操题
export function delPracticeSubject(ids) {
  return request({ url: '/business/practice-subject/' + ids, method: 'delete' })
}

// 通用文件上传（返回 {fileName, url}）
export function uploadFile(formData) {
  return request({
    url: '/common/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
