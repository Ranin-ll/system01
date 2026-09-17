import request from '@/utils/request'

// 查询题库列表
export function listBank(query) {
  return request({ url: '/business/question-bank/list', method: 'get', params: query })
}

// 查询题库详细
export function getBank(id) {
  return request({ url: '/business/question-bank/' + id, method: 'get' })
}

// 新增题库
export function addBank(data) {
  return request({ url: '/business/question-bank', method: 'post', data })
}

// 修改题库
export function updateBank(data) {
  return request({ url: '/business/question-bank', method: 'put', data })
}

// 删除题库
export function delBank(id) {
  return request({ url: '/business/question-bank/' + id, method: 'delete' })
}