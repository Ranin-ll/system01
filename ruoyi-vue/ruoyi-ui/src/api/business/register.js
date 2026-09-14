import request from '@/utils/request'

// 查询注册申请列表（部门管理员）
export function listRegister(query) {
  return request({
    url: '/business/register/list',
    method: 'get',
    params: query
  })
}

// 查询注册申请统计
export function getRegisterSummary() {
  return request({
    url: '/business/register/summary',
    method: 'get'
  })
}

// 查询注册申请详细
export function getRegister(id) {
  return request({
    url: '/business/register/' + id,
    method: 'get'
  })
}

// 查询审核历史
export function listRegisterHistory(id) {
  return request({
    url: '/business/register/' + id + '/history',
    method: 'get'
  })
}

// 提交注册申请（公开接口）
export function submitRegister(data) {
  return request({
    url: '/business/register/submit',
    method: 'post',
    data: data
  })
}

// 重新提交申请
export function resubmitRegister(id, data) {
  return request({
    url: '/business/register/resubmit/' + id,
    method: 'put',
    data: data
  })
}

// 部门管理员审核
export function auditRegister(data) {
  return request({
    url: '/business/register/audit',
    method: 'post',
    data: data
  })
}
