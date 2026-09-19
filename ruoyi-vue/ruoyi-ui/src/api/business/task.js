import request from '@/utils/request'

// 任务列表（超管看全部 / 部门管理员看本部门岗位）
export function listTasks(query) {
  return request({
    url: '/business/task/list',
    method: 'get',
    params: query
  })
}

// 我的任务（实习生）
export function listMyTasks() {
  return request({
    url: '/business/task/my',
    method: 'get'
  })
}

/**
 * 本部门在培实习生（含待转正）——「任务/通知/批阅」三页的「按实习生筛选」共用。
 * ⚠️ 不要用 `/system/user/list`：那要 system:user:list 权限，部门管理员是 403，前端只会拿到空列表。
 */
export function listTrainees() {
  return request({
    url: '/business/task/trainees',
    method: 'get'
  })
}

// 新建 / 修改任务（部门管理员）
export function saveTask(data) {
  return request({
    url: '/business/task/save',
    method: 'post',
    data: data
  })
}

// 发布 / 结束（status=PUBLISHED / ENDED；发布时会展开分配并通知到人）
export function changeTaskStatus(id, status) {
  return request({
    url: '/business/task/' + id + '/status',
    method: 'post',
    params: { status: status }
  })
}

// 任务详情
export function getTask(id) {
  return request({
    url: '/business/task/' + id,
    method: 'get'
  })
}

// 提交情况看板（含未提交者）
export function listSubmissions(taskId) {
  return request({
    url: '/business/task/' + taskId + '/submissions',
    method: 'get'
  })
}

// 某个分配的历次提交（批阅记录）—— 重复提交的旧版本也能看到
export function listSubmissionHistory(assignmentId) {
  return request({
    url: '/business/task/assignment/' + assignmentId + '/submissions',
    method: 'get'
  })
}

// 提交作业（实习生；重复提交走 version+1）
export function submitTask(taskId, data) {
  return request({
    url: '/business/task/' + taskId + '/submit',
    method: 'post',
    data: data
  })
}

// 批阅（部门管理员）：passFlag=PASS / REJECT
export function reviewSubmission(data, passFlag) {
  return request({
    url: '/business/task/review',
    method: 'post',
    params: { passFlag: passFlag || 'PASS' },
    data: data
  })
}

// ------------------------------ 讨论区 ------------------------------

// 讨论区列表（主楼 + 归拢到主楼下的回复）
export function listTaskPosts(taskId) {
  return request({
    url: '/business/task-post/list',
    method: 'get',
    params: { taskId: taskId }
  })
}

// 发帖 / 回复（parentId 为空即主楼）
export function saveTaskPost(data) {
  return request({
    url: '/business/task-post/save',
    method: 'post',
    data: data
  })
}

// 删帖（软删留痕；作者本人或部门管理员）
export function deleteTaskPost(id) {
  return request({
    url: '/business/task-post/' + id + '/delete',
    method: 'post'
  })
}

// ------------------------------ 任务资料（附件） ------------------------------

// 某任务的资料列表
export function listTaskAttachments(taskId) {
  return request({
    url: '/business/task-attachment/list',
    method: 'get',
    params: { taskId: taskId }
  })
}

// 实习生：我全部任务的资料（一次返回，本地按 taskId 分组，避免 N+1）
export function listMyTaskAttachments() {
  return request({
    url: '/business/task-attachment/my',
    method: 'get'
  })
}

// 上传资料（部门管理员）——multipart，与课程资料上传同一套写法
export function uploadTaskAttachment(taskId, formData, onUploadProgress) {
  return request({
    url: '/business/task-attachment/upload',
    method: 'post',
    params: { taskId: taskId },
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onUploadProgress
  })
}

// 删除资料（部门管理员，软删留痕）
export function deleteTaskAttachment(id) {
  return request({
    url: '/business/task-attachment/' + id + '/delete',
    method: 'post'
  })
}

// 实习生上传作业附件（只落盘不落库，返回的 url/fileName 随 submit 一起提交）
export function uploadSubmissionAsset(taskId, formData, onUploadProgress) {
  return request({
    url: '/business/task-attachment/upload-submission',
    method: 'post',
    params: { taskId: taskId },
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onUploadProgress
  })
}
