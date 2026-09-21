import request from '@/utils/request'

// 备考资料列表（管理端）
export function listMaterial(query) {
  return request({ url: '/business/material/list', method: 'get', params: query })
}

// 备考资料详情
export function getMaterial(id) {
  return request({ url: '/business/material/' + id, method: 'get' })
}

// 新增备考资料
export function addMaterial(data) {
  return request({ url: '/business/material', method: 'post', data })
}

// 编辑备考资料
export function updateMaterial(data) {
  return request({ url: '/business/material', method: 'put', data })
}

// 发布 / 停用
export function changeMaterialStatus(id, status) {
  return request({ url: '/business/material/status/' + id, method: 'put', params: { status } })
}

// 删除备考资料
export function delMaterial(ids) {
  return request({ url: '/business/material/' + ids, method: 'delete' })
}

// 管理端适用岗位下拉（非超管锁定本部门）
export function listApplicablePositions() {
  return request({ url: '/business/material/positions', method: 'get' })
}

// 实习生端：本部门已发布资料列表
export function listPublishedMaterials() {
  return request({ url: '/business/material/published', method: 'get' })
}

// 实习生端：已发布资料详情
export function getPublishedMaterial(id) {
  return request({ url: '/business/material/published/' + id, method: 'get' })
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