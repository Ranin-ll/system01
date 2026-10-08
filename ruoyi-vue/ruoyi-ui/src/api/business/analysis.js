import request from '@/utils/request'

/**
 * 超管「培养分析看板」（培养运营 → 培养分析看板）
 */
export function getAnalysisOverview() {
  return request({ url: '/business/super/analysis/overview', method: 'get' })
}

export function getDeptMatrix() {
  return request({ url: '/business/super/analysis/dept-matrix', method: 'get' })
}

export function getStageProgress() {
  return request({ url: '/business/super/analysis/stage-progress', method: 'get' })
}

/** L0 知识点热力：部门 × 知识点矩阵（真数据） */
export function getKnowledgeMatrix() {
  return request({ url: '/business/super/analysis/knowledge-matrix', method: 'get' })
}

/** L1 部门详情：部门 KPI + 岗位分布 + 课程完成率 + 实习生明细 + 全局对照 + 知识点明细 */
export function getDeptStats(deptId) {
  return request({ url: '/business/super/analysis/dept-stats', method: 'get', params: { deptId } })
}

/** L3 个人档案：身份 + 逐学习项 + 考核类（practice / formal / knowledge 均为真数据） */
export function getInternDetail(userId) {
  return request({ url: '/business/super/analysis/intern/' + userId, method: 'get' })
}
