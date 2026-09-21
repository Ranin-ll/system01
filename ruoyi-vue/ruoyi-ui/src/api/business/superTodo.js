import request from '@/utils/request'

// ---------------- 超管督办视图（/business/super/**） ----------------
//
// 返回结构约定：后端用 AjaxResult.success(data)，所以数据在 res.data 里
// （注意与 TableDataInfo 的 res.rows / res.total 区分 —— 这里不是分页列表）。

/** 部门管理员清单（含各自四类待办量） */
export function listDeptAdmins() {
  return request({
    url: '/business/super/dept-admins',
    method: 'get'
  })
}

/**
 * 督办看板：四类待办的汇总 + 明细
 * @param {Object} params { todoType?, deptId?, ownerId? }
 * @returns res.data = { summary: [{type,label,count}], rows: [...], total, stalledIdleDays }
 */
export function getTodoBoard(params) {
  return request({
    url: '/business/super/todos',
    method: 'get',
    params: params
  })
}

/**
 * 一键催办（可多选部门管理员）
 * @param {Object} data { ownerIds: [1,2], content?: '附加说明' }
 * @returns res.data = { sent, skipped, details: [...] }
 */
export function urgeTodo(data) {
  return request({
    url: '/business/super/urge',
    method: 'post',
    data: data
  })
}
