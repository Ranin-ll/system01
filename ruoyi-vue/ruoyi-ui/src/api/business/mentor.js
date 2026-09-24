import request from '@/utils/request'

// ---------------------------------------------------------------------------
// 导师库（2026-09-23）
//
// 导师从 sys_user 的自由文本抽成独立主表，实习生通过 sys_user.mentor_id 关联。
// 部门管理员只能看/改本部门导师（后端 Service 再校验一次范围），超管不限部门。
// ---------------------------------------------------------------------------

// 导师列表（分页）
export function listMentor(query) {
  return request({
    url: '/business/mentor/list',
    method: 'get',
    params: query
  })
}

// 导师下拉选项（仅启用中，按当前人的部门范围）—— 分配导师时用
export function getMentorOptions() {
  return request({
    url: '/business/mentor/options',
    method: 'get'
  })
}

// 导师详情
export function getMentor(id) {
  return request({
    url: '/business/mentor/' + id,
    method: 'get'
  })
}

// 新增导师
export function addMentor(data) {
  return request({
    url: '/business/mentor',
    method: 'post',
    data: data
  })
}

// 修改导师
export function updateMentor(data) {
  return request({
    url: '/business/mentor',
    method: 'put',
    data: data
  })
}

// 删除导师（软删；有实习生在带时后端会拒绝）
export function delMentor(id) {
  return request({
    url: '/business/mentor/' + id,
    method: 'delete'
  })
}

// 某实习生当前的导师关联（分配弹窗回显）
export function getInternMentor(userId) {
  return request({
    url: '/business/mentor/intern/' + userId,
    method: 'get'
  })
}

// 给实习生分配 / 更换导师
export function assignInternMentor(data) {
  return request({
    url: '/business/mentor/assign',
    method: 'post',
    data: data
  })
}

// 解除实习生的导师关联
export function clearInternMentor(data) {
  return request({
    url: '/business/mentor/clear',
    method: 'post',
    data: data
  })
}
