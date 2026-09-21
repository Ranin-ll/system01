import request from '@/utils/request'

/**
 * 催办与通知记录（超管只读留痕看板）。
 *
 * 数据源：`notice`（本体）+ `notice_target`（送达）+ `notice_read`（已读回执）。
 * 与既有的 `/business/message/sent` 的区别：那个只按「发送者 = 我」过滤（超管只有 4 条），
 * 这里是**全量留痕** —— 含部门管理员发的、以及定时任务发的（`publisher_id IS NULL`）。
 */

// 记录列表（分页；query: msgType / bizType / publisherId / deptId / keyword / beginTime / endTime）
export function listNoticeRecords(query) {
  return request({
    url: '/business/super/records/list',
    method: 'get',
    params: query
  })
}

// 看板指标：total / urgeCount / taskCount / notifyCount / announceCount / systemCount
//          / scope* / targetTotal（送达合计）/ readTotal（已读合计）/ publisherCounts（按发送者）
export function getNoticeRecordSummary() {
  return request({
    url: '/business/super/records/summary',
    method: 'get'
  })
}

// 单条记录的送达 / 已读明细（谁读了、谁还没读）
export function getNoticeReadDetail(noticeId) {
  return request({
    url: '/business/super/records/' + noticeId + '/read-detail',
    method: 'get'
  })
}
