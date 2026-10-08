import request from '@/utils/request'

// 本部门在培实习生（含待转正）——「通知管理」与 TraineeSelect 公共选择器共用。
// 后端：DeptTraineeController（/business/dept/trainees）。
export function listTrainees() {
  return request({
    url: '/business/dept/trainees',
    method: 'get'
  })
}
