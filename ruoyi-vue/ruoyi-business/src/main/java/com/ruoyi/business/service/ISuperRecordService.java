package com.ruoyi.business.service;

import java.util.List;
import java.util.Map;

/**
 * 超管「催办与通知记录」看板（只读留痕）。
 *
 * <p>数据源是 `notice` / `notice_target` / `notice_read` 三张表；
 * 后续若要给部门管理员开放，只需在实现里把范围收成「publisher_id = 当前用户」（用户已确认的口径）。</p>
 */
public interface ISuperRecordService {

    /** 记录列表（分页；筛选：msgType / bizType / publisherId / deptId / keyword / beginTime / endTime） */
    List<Map<String, Object>> selectRecords(Map<String, Object> query);

    /** 看板主指标 + 按发送者分布 */
    Map<String, Object> selectSummary();

    /** 单条记录的送达 / 已读明细（谁读了、谁还没读） */
    List<Map<String, Object>> selectReadDetail(Long noticeId);
}
