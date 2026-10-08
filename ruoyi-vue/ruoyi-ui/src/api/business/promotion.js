import request from '@/utils/request'

// ---------------------------------------------------------------------------
// 实习转正（2026-09-29）
//
// 链路：要求设置 → 提交申请 → 部门审批 → 通过后转正式并发证。
// 两侧接口分属不同权限模型：
//   · /business/promotion/**        管理端（超管 / 部门管理员），菜单权限码
//   · /business/intern/promotion/** 实习生端，角色校验，userId 取登录态
// ---------------------------------------------------------------------------

// ==================== 转正要求（管理端） ====================

// 规则面板：全局默认 + 各部门覆盖
export function getPromotionRuleBoard() {
  return request({
    url: '/business/promotion/rule/board',
    method: 'get'
  })
}

// 某部门的生效规则（不传 deptId 即默认要求；带 source: DEPT/GLOBAL/DEFAULT）
export function getPromotionRule(deptId) {
  return request({
    url: '/business/promotion/rule',
    method: 'get',
    params: deptId == null || deptId === '' ? {} : { deptId: deptId }
  })
}

// 保存转正要求（deptId 为空 = 默认要求；非空 = 该部门单独设置）
export function savePromotionRule(data) {
  return request({
    url: '/business/promotion/rule',
    method: 'put',
    data: data
  })
}

// ==================== 候选人 / 申请（管理端） ====================

// 候选人看板：rows + summary
// status: ALL / PENDING / PASSED / REJECTED / REVOKED / ELIGIBLE
export function listPromotionCandidates(query) {
  return request({
    url: '/business/promotion/candidates',
    method: 'get',
    params: query
  })
}

// 单人的资格核对清单
export function getPromotionGate(userId) {
  return request({
    url: '/business/promotion/gate/' + userId,
    method: 'get'
  })
}

// 转正申请列表（分页）
export function listPromotionApps(query) {
  return request({
    url: '/business/promotion/apps',
    method: 'get',
    params: query
  })
}

// 申请详情：单据 + 审核历史 + 当前资格 + 证书
export function getPromotionDetail(id) {
  return request({
    url: '/business/promotion/' + id,
    method: 'get'
  })
}

// 申请审核历史
export function getPromotionHistory(id) {
  return request({
    url: '/business/promotion/' + id + '/history',
    method: 'get'
  })
}

// 审批：action = PASS / REJECT（驳回必须带 reason）
export function auditPromotion(data) {
  return request({
    url: '/business/promotion/audit',
    method: 'post',
    data: data
  })
}

// 超管纠错：撤回转正
export function revokePromotion(data) {
  return request({
    url: '/business/promotion/revoke',
    method: 'post',
    data: data
  })
}

// ==================== 实习生端 ====================

// 我的转正视图：生效规则 + 资格清单 + 最新申请 + 证书
export function getMyPromotion() {
  return request({
    url: '/business/intern/promotion/mine',
    method: 'get'
  })
}

// 提交转正申请
export function submitMyPromotion(data) {
  return request({
    url: '/business/intern/promotion/apply',
    method: 'post',
    data: data
  })
}

// 撤回待审核申请
export function withdrawMyPromotion() {
  return request({
    url: '/business/intern/promotion/withdraw',
    method: 'post'
  })
}

// 我的电子证书
export function getMyCertificate() {
  return request({
    url: '/business/intern/promotion/certificate',
    method: 'get'
  })
}
