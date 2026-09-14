package com.ruoyi.business.controller;

import com.ruoyi.business.domain.RegisterApplication;
import com.ruoyi.business.domain.AuditRecord;
import com.ruoyi.business.service.IRegisterApplicationService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 注册申请Controller
 *
 * 涉及角色：超级管理员、部门管理员、预备实习生
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/business/register")
public class RegisterApplicationController extends BaseController {

    @Autowired
    private IRegisterApplicationService registerApplicationService;

    /**
     * 查询注册申请列表（部门管理员自动注入本部门）
     */
    @PreAuthorize("@ss.hasPermi('business:register:list')")
    @GetMapping("/list")
    public TableDataInfo list(RegisterApplication registerApplication) {
        startPage();
        List<RegisterApplication> list = registerApplicationService.selectRegisterList(registerApplication);
        return getDataTable(list);
    }

    /**
     * 查询注册申请统计（自动遵守部门数据范围）
     */
    @PreAuthorize("@ss.hasPermi('business:register:list')")
    @GetMapping("/summary")
    public AjaxResult summary() {
        return AjaxResult.success(registerApplicationService.selectRegisterSummary());
    }

    /**
     * 获取申请详情
     */
    @PreAuthorize("@ss.hasPermi('business:register:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return AjaxResult.success(registerApplicationService.selectById(id));
    }

    /** 获取注册申请审核历史 */
    @PreAuthorize("@ss.hasPermi('business:register:query')")
    @GetMapping("/{id}/history")
    public AjaxResult history(@PathVariable Long id) {
        List<AuditRecord> history = registerApplicationService.selectAuditHistory(id);
        return AjaxResult.success(history);
    }

    /**
     * 提交注册申请（公开接口，无需登录）
     * 实习生端"注册页"使用
     */
    @PostMapping("/submit")
    public AjaxResult submit(@RequestBody RegisterApplication application) {
        Long id = registerApplicationService.submitApplication(application);
        return AjaxResult.success("注册申请已提交，请等待部门管理员审核", id);
    }

    /**
     * 重新提交申请（已驳回后可重新提交，沿用原申请编号）
     */
    @PreAuthorize("@ss.hasPermi('business:register:resubmit')")
    @Log(title = "注册申请", businessType = BusinessType.UPDATE)
    @PutMapping("/resubmit/{id}")
    public AjaxResult resubmit(@PathVariable Long id, @RequestBody RegisterApplication update) {
        return toAjax(registerApplicationService.resubmitApplication(id, update));
    }

    /**
     * 部门管理员审核：pass / reject
     */
    @PreAuthorize("@ss.hasPermi('business:register:audit')")
    @Log(title = "注册审核", businessType = BusinessType.UPDATE)
    @PostMapping("/audit")
    public AjaxResult audit(@RequestBody Map<String, Object> params) {
        Long id = Long.parseLong(params.get("id").toString());
        String action = params.get("action").toString();
        String reason = (String) params.get("reason");
        String mentorName = (String) params.get("mentorName");
        String mentorPhone = (String) params.get("mentorPhone");
        return AjaxResult.success(registerApplicationService.audit(id, action, reason, mentorName, mentorPhone));
    }
}
