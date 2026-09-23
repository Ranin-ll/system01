import request from '@/utils/request'

export function getCurrentAgreement() {
  return request({
    url: '/business/auth/agreement',
    method: 'get'
  })
}

export function signCurrentAgreement(data) {
  return request({
    url: '/business/auth/agreement/sign',
    method: 'post',
    data
  })
}

// 本人的签署凭证（含签名图 dataUrl）。未签署过返回 data = null。
export function getMyAgreementSignature() {
  return request({
    url: '/business/auth/agreement/signature',
    method: 'get'
  })
}
