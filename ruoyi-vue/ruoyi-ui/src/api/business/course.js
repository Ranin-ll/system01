import request from '@/utils/request'

// 查询课程列表
export function listCourse(query) {
  return request({
    url: '/business/course/list',
    method: 'get',
    params: query
  })
}

// 查询当前账号课程管理范围内可用的岗位
export function listCoursePositions() {
  return request({
    url: '/business/course/positions',
    method: 'get'
  })
}

// 查询课程详细
export function getCourse(id) {
  return request({
    url: '/business/course/' + id,
    method: 'get'
  })
}

// 新增课程
export function addCourse(data) {
  return request({
    url: '/business/course',
    method: 'post',
    data: data
  })
}

// 修改课程
export function updateCourse(data) {
  return request({
    url: '/business/course',
    method: 'put',
    data: data
  })
}

// 删除课程
export function delCourse(id) {
  return request({
    url: '/business/course/' + id,
    method: 'delete'
  })
}

// 发布课程
export function publishCourse(id) {
  return request({
    url: '/business/course/publish/' + id,
    method: 'put'
  })
}
