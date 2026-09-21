import request from '@/utils/request'

/**
 * 超管「人员与账号信息管理」。
 *
 * 这里只封装项目自有的两件事（富查询 + 业务字段写入）；
 * 账号级增删改与重置密码直接走 RuoYi 原生 `/system/user`（见 api/system/user.js），
 * 因为原生接口已处理唯一性校验、密码加密、角色重绑与 user_id=1 保护。
 */

// 人员列表（分页；query: keyword / deptId / roleKey / userStatus / status）
export function listPersonnel(query) {
  return request({
    url: '/business/super/personnel/list',
    method: 'get',
    params: query
  })
}

// 人员详情（含 roleIds 逗号串，供角色多选回显）
export function getPersonnel(userId) {
  return request({
    url: '/business/super/personnel/' + userId,
    method: 'get'
  })
}

// 保存业务字段：deptId / positionId / userStatus / protocolStatus / mentorName / mentorPhone / expectedEntryDate
// 传 null 表示清空该项（岗位、预计入职、导师）；部门由服务端校验「超管必须属公司根部门」
export function savePersonnelBusiness(data) {
  return request({
    url: '/business/super/personnel/business',
    method: 'put',
    data: data
  })
}

// 人员看板聚合数据（顶部统计卡 + 分布条）：主指标 + roleCounts + deptCounts + noRole
export function getPersonnelSummary() {
  return request({
    url: '/business/super/personnel/summary',
    method: 'get'
  })
}

// 删除人员（逻辑删除）。
// 走超管专属接口而非原生 /system/user/{id}：服务端会拦「内置超管 / 持有超管角色的账号 / 当前登录账号」，
// 保证系统始终存在一个可用超管。
export function deletePersonnel(userId) {
  return request({
    url: '/business/super/personnel/' + userId,
    method: 'delete'
  })
}
