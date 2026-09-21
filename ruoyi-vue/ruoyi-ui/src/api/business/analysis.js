import request from '@/utils/request'

/**
 * 超管「培养分析看板」（培养运营 → 培养分析看板）
 *
 * 后端只提供**真数据**三族：人 / 学习 / 任务。
 * 模拟考核、正式考核、知识点三族**后端暂不提供**（题库与考核模块待同事分支合并，
 * 在会变的表上取数等于白做）→ 由 `views/super/ops/analysis/_mock.js` 填充并打「示例」橙标。
 *
 * ⚠️ 三个接口的返回结构**已经定死**，合并后只需后端补字段，前端模板零改动。
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

/** L1 部门详情：部门 KPI + 岗位分布 + 课程完成率 + 实习生明细 + 全局对照 */
export function getDeptStats(deptId) {
  return request({ url: '/business/super/analysis/dept-stats', method: 'get', params: { deptId } })
}

/**
 * L3 个人档案：身份 + 逐学习项 + 逐任务（真数据）。
 * 返回里的 `practice` / `formal` / `knowledge` / `evaluation` **恒为 null** ——
 * 考核类待同事分支合并、阶段评价表为空，由 _mock.js 填充并打橙标。
 */
export function getInternDetail(userId) {
  return request({ url: '/business/super/analysis/intern/' + userId, method: 'get' })
}
