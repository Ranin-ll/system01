import request from '@/utils/request'

export function listLearningCourses() {
  return request({
    url: '/business/learning/courses',
    method: 'get'
  })
}

/**
 * 本人近 N 天逐日学习时长（2026-09-22 新增）
 * 返回 [{date, seconds, items}]，只含有记录的天（缺的天前端补 0），单位秒
 */
export function getDailyDuration(days = 14) {
  return request({
    url: '/business/learning/duration-daily',
    method: 'get',
    params: { days }
  })
}

export function getLearningCourse(courseId) {
  return request({
    url: '/business/learning/courses/' + courseId,
    method: 'get'
  })
}

export function saveLearningProgress(itemId, data) {
  return request({
    url: '/business/learning/items/' + itemId + '/progress',
    method: 'put',
    data,
    headers: { repeatSubmit: false }
  })
}
