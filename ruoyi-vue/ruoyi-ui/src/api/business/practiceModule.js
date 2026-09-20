import request from '@/utils/request'

// 模拟考核模块 · 列表（管理端，附带模块内实操题/理论考核数量）
export function listPracticeModule(query) {
  return request({ url: '/business/practice-module/list', method: 'get', params: query })
}

// 模拟考核模块 · 下拉选项（本部门；超管可传 deptId）
export function listPracticeModuleOptions(deptId) {
  return request({
    url: '/business/practice-module/options',
    method: 'get',
    params: deptId ? { deptId } : {}
  })
}

// 模拟考核模块 · 详情
export function getPracticeModule(id) {
  return request({ url: '/business/practice-module/' + id, method: 'get' })
}

// 新建模块
export function addPracticeModule(data) {
  return request({ url: '/business/practice-module', method: 'post', data })
}

// 编辑模块（改名 / 说明 / 排序 / 状态）
export function updatePracticeModule(data) {
  return request({ url: '/business/practice-module', method: 'put', data })
}

// 启用 / 停用模块
export function changePracticeModuleStatus(id, status) {
  return request({ url: '/business/practice-module/status/' + id, method: 'put', params: { status } })
}

// 删除模块
export function delPracticeModule(ids) {
  return request({ url: '/business/practice-module/' + ids, method: 'delete' })
}

// 实习生端：本部门已启用模块列表（先选模块）
export function listPublishedModules() {
  return request({ url: '/business/practice-module/published', method: 'get' })
}
