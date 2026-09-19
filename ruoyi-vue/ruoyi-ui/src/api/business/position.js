import request from '@/utils/request'

// 查询岗位列表
export function listPosition(query) {
  return request({
    url: '/business/position/list',
    method: 'get',
    params: query
  })
}

// 部门 ↔ 岗位绑定（权威表 dept_position，仅生效中）
export function getDeptBindings() {
  return request({
    url: '/business/position/dept-bindings',
    method: 'get'
  })
}

// 查询岗位详细
export function getPosition(id) {
  return request({
    url: '/business/position/' + id,
    method: 'get'
  })
}

// 新增岗位
export function addPosition(data) {
  return request({
    url: '/business/position',
    method: 'post',
    data: data
  })
}

// 修改岗位
export function updatePosition(data) {
  return request({
    url: '/business/position',
    method: 'put',
    data: data
  })
}

// 删除岗位
export function delPosition(id) {
  return request({
    url: '/business/position/' + id,
    method: 'delete'
  })
}

// 修改岗位状态
export function changePositionStatus(id, status) {
  return request({
    url: '/business/position/changeStatus/' + id + '/' + status,
    method: 'put'
  })
}