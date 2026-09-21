package com.ruoyi.business.controller;

import com.ruoyi.business.service.ISuperRecordService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 超管「催办与通知记录」看板（只读留痕）。
 *
 * <p><b>为什么新起接口</b>：既有的 `/business/message/sent` 以「当前登录用户 = 发送者」过滤，
 * 超管只能看到自己发的（实测 4 条）；本看板要的是**全量留痕** ——
 * 含部门管理员发的、以及**定时任务发的**（`publisher_id IS NULL`，如每日逾期提醒）。</p>
 *
 * <p><b>权限</b>：复用 `business:bank:list`（部门管理员亦持有），
 * 越权拦截落在 {@code SuperRecordServiceImpl#requireSuperAdmin()}。</p>
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/business/super/records")
public class SuperRecordController extends BaseController {

    @Autowired
    private ISuperRecordService superRecordService;

    /** 记录列表（分页 + 筛选：msgType / bizType / publisherId / deptId / keyword / beginTime / endTime） */
    @PreAuthorize("@ss.hasPermi('business:bank:list')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam Map<String, Object> query) {
        startPage();
        List<Map<String, Object>> list = superRecordService.selectRecords(query);
        return getDataTable(list);
    }

    /** 看板主指标（总数 / 按类型 / 按受众范围 / 送达合计 / 已读合计）+ 按发送者分布 */
    @PreAuthorize("@ss.hasPermi('business:bank:list')")
    @GetMapping("/summary")
    public AjaxResult summary() {
        return AjaxResult.success(superRecordService.selectSummary());
    }

    /** 单条记录的送达 / 已读明细（谁读了、谁还没读） */
    @PreAuthorize("@ss.hasPermi('business:bank:list')")
    @GetMapping("/{noticeId}/read-detail")
    public AjaxResult readDetail(@PathVariable("noticeId") Long noticeId) {
        return AjaxResult.success(superRecordService.selectReadDetail(noticeId));
    }
}
