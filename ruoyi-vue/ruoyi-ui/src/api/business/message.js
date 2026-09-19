import request from '@/utils/request'

// 未读数（总数 + 按类型分组）——铃铛与页签角标
export function getUnreadCount() {
  return request({
    url: '/business/message/unread-count',
    method: 'get'
  })
}

// 我的消息列表（分页；支持 msgType / readFlag / title 关键字）
// 返回 { rows, total, unreadByType }
export function listMyMessages(query) {
  return request({
    url: '/business/message/list',
    method: 'get',
    params: query
  })
}

// 工作台公告位（有效公告，最多 3 条，不写已读）
export function listAnnouncements() {
  return request({
    url: '/business/message/announcements',
    method: 'get'
  })
}

// 消息详情（可见则顺带标记已读）
export function getMessage(id) {
  return request({
    url: '/business/message/' + id,
    method: 'get'
  })
}

// 标记单条已读（幂等）
export function readMessage(id) {
  return request({
    url: '/business/message/' + id + '/read',
    method: 'post'
  })
}

// 全部已读（可只清某一类型）
export function readAllMessages(msgType) {
  return request({
    url: '/business/message/read-all',
    method: 'post',
    data: { msgType: msgType || null }
  })
}

// ============ P1：发送方视角 ============

// 发送通知 / 公告（四档范围：ALL / DEPT / POSITION / USER）
// 公告与「全体」只有超管能发；部门管理员只能发本部门（后端强校验）
export function sendMessage(data) {
  return request({
    url: '/business/message',
    method: 'post',
    data: data
  })
}

// 我发送过的通知（带 deliveredCount / readCount）
export function listSentMessages(query) {
  return request({
    url: '/business/message/sent',
    method: 'get',
    params: query
  })
}

// 收件人清单（含是否已读，未读排前）
export function listRecipients(id) {
  return request({
    url: '/business/message/' + id + '/recipients',
    method: 'get'
  })
}

// 撤回（仅发送人本人或超管）
export function revokeMessage(id) {
  return request({
    url: '/business/message/' + id + '/revoke',
    method: 'post'
  })
}

// 发送前的「预计送达」人数估算（与后端送达谓词同口径；USER 档前端自己数数组长度）
export function estimateAudience(scopeType, scopeId) {
  return request({
    url: '/business/message/estimate',
    method: 'get',
    params: { scopeType: scopeType, scopeId: scopeId }
  })
}
