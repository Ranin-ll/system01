import request from '@/utils/request'

// 查询课程章节和学习资料编排
export function getCourseContents(courseId) {
  return request({
    url: '/business/course/' + courseId + '/contents',
    method: 'get'
  })
}

// 保存草稿课程的章节、资料排序和元数据
export function saveCourseContents(courseId, data) {
  return request({
    url: '/business/course/' + courseId + '/contents',
    method: 'put',
    data: data
  })
}

// 章节管理
export function addCourseChapter(courseId, data) {
  return request({ url: '/business/course/' + courseId + '/chapters', method: 'post', data: data })
}

export function updateCourseChapter(chapterId, data) {
  return request({ url: '/business/course/chapters/' + chapterId, method: 'put', data: data })
}

export function deleteCourseChapter(chapterId) {
  return request({ url: '/business/course/chapters/' + chapterId, method: 'delete' })
}

// 学习资料管理
export function addStudyItem(chapterId, data) {
  return request({ url: '/business/course/chapters/' + chapterId + '/items', method: 'post', data: data })
}

export function updateStudyItem(itemId, data) {
  return request({ url: '/business/course/items/' + itemId, method: 'put', data: data })
}

export function deleteStudyItem(itemId) {
  return request({ url: '/business/course/items/' + itemId, method: 'delete' })
}

// 文件上传采用 multipart；后续切换对象存储预签名上传时保持业务层方法不变。
export function uploadStudyAsset(itemId, formData, onUploadProgress) {
  return request({
    url: '/business/course/items/' + itemId + '/asset',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onUploadProgress
  })
}

// 部门管理员查看课程学习汇总和人员明细
export function listCourseStudyRecords(courseId, query) {
  return request({ url: '/business/course/' + courseId + '/study-records', method: 'get', params: query })
}