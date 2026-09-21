import request from '@/utils/request'

// 考核列表
export function listExam(query) {
  return request({ url: '/business/exam/list', method: 'get', params: query })
}
// 考核详情
export function getExam(id) {
  return request({ url: '/business/exam/' + id, method: 'get' })
}
// 新增考核
export function addExam(data) {
  return request({ url: '/business/exam', method: 'post', data })
}
// 修改考核
export function updateExam(data) {
  return request({ url: '/business/exam', method: 'put', data })
}
// 删除考核
export function delExam(id) {
  return request({ url: '/business/exam/' + id, method: 'delete' })
}
// 发布考核（可带发布时间窗、指定人员、知识分布）
export function publishExam(id, data) {
  return request({ url: '/business/exam/publish/' + id, method: 'put', data })
}
// 修改考核状态（启用/停用）
export function changeExamStatus(id, status) {
  return request({ url: '/business/exam/status/' + id, method: 'put', params: { status } })
}

// 答卷相关
// 批改列表（部门实习生 + 答卷状态）
export function gradingList(examId) {
  return request({ url: '/business/answer-sheet/grading/' + examId, method: 'get' })
}
// 答卷详情
export function sheetDetail(sheetId) {
  return request({ url: '/business/answer-sheet/detail/' + sheetId, method: 'get' })
}
// 本人答卷详情（实习生端：通过与否都能看作答与批阅明细）
export function mySheetDetail(sheetId) {
  return request({ url: '/business/answer-sheet/my/detail/' + sheetId, method: 'get' })
}
// 批改（实操题打分）
export function gradeSheet(data) {
  return request({ url: '/business/answer-sheet/grade', method: 'put', data })
}
// 发布成绩
export function publishResult(examId, params) {
  return request({ url: '/business/answer-sheet/publish/' + examId, method: 'put', params })
}
// 重新开放批改（已发布成绩回退到批改中）
export function reopenSheet(sheetId) {
  return request({ url: '/business/answer-sheet/reopen/' + sheetId, method: 'put' })
}

// 实习生端
// 开始考核（抽题）
export function startExam(examId) {
  return request({ url: '/business/answer-sheet/start/' + examId, method: 'post' })
}
// 交卷
export function submitExam(data) {
  return request({ url: '/business/answer-sheet/submit', method: 'post', data })
}
// 实习生考核列表（含结果，按考核性质过滤）
export function myExamList(examMode) {
  return request({ url: '/business/answer-sheet/my', method: 'get', params: examMode ? { examMode } : {} })
}

// 通用文件上传（返回 {url, fileName}）
export function uploadFile(formData) {
  return request({
    url: '/common/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// ---------- 多题库组卷配置（当前主用） ----------
// 本部门可选题库清单（含各库按题型的可用题量）
export function listExamBankOptions(deptId, examMode) {
  const params = {}
  if (deptId) params.deptId = deptId
  if (examMode) params.examMode = examMode
  return request({ url: '/business/exam/bank-options', method: 'get', params })
}
// 按多题库组卷配置试抽一套卷（校验用，不落库）
export function tryDrawByBanks(data) {
  return request({ url: '/business/exam/try-draw', method: 'post', data })
}

// ---------- 正式考核发布：指定人员 ----------
// 在培实习生花名册（「指定人员」可选名单；部门管理员只看本部门，超管可传 deptId）
export function listExamInternOptions(deptId) {
  return request({ url: '/business/exam/intern-options', method: 'get', params: deptId ? { deptId } : {} })
}

// ---------- 理论考试配置（知识分布 / 指定人员 / 试抽） ----------
// 题库知识点及题量（配置页「从题库导入知识点」）
export function listKnowledgePoints(bankId) {
  return request({ url: '/business/exam/bank/' + bankId + '/knowledge-points', method: 'get' })
}
// 按知识分布试抽一套卷（校验用，不落库）
export function tryDrawPaper(bankId, data) {
  return request({ url: '/business/exam/bank/' + bankId + '/try-draw', method: 'post', data })
}
// 保存考核配置（知识分布 + 指定人员 + 时间窗），不改发布状态
export function saveExamConfig(id, data) {
  return request({ url: '/business/exam/config/' + id, method: 'put', data })
}
// 读取考核配置（知识分布 + 指定人员 + 时间窗）
export function getExamConfig(id) {
  return request({ url: '/business/exam/config/' + id, method: 'get' })
}
