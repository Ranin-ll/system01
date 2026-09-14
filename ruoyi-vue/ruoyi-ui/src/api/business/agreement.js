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
