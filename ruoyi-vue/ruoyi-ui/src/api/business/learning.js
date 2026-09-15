import request from '@/utils/request'

export function listLearningCourses() {
  return request({
    url: '/business/learning/courses',
    method: 'get'
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
