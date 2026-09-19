import request from '@/utils/request'

// 读取「考核与运营规则默认值」（单行配置）
export function getRuleConfig() {
  return request({
    url: '/business/rule/config',
    method: 'get'
  })
}

// 保存规则默认值（仅超管可写；后端会写 audit_record 留痕）
export function updateRuleConfig(data) {
  return request({
    url: '/business/rule/config',
    method: 'put',
    data: data
  })
}

// 协议模板列表（含历史版本）
export function listAgreementTemplates() {
  return request({
    url: '/business/rule/agreement-templates',
    method: 'get'
  })
}

// 发布协议模板新版本（旧的生效版自动归档）
export function publishAgreementTemplate(data) {
  return request({
    url: '/business/rule/agreement-templates',
    method: 'post',
    data: data
  })
}
