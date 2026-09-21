package com.ruoyi.business.controller;

import com.ruoyi.business.domain.AgreementTemplate;
import com.ruoyi.business.domain.AssessmentConfig;
import com.ruoyi.business.service.IAssessmentRuleService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 规则与配置 Controller（超管端「规则与配置」页）
 *
 * <p><b>权限口径</b>：读 —— 超管与部门管理员都可读（部门管理员发布批次时要知道当前默认值）；
 * 写 —— **仅超管**（含平台内置 admin，其 role_key 为 'admin'，故一并列入表达式）。
 * 写操作统一留痕到 <code>audit_record</code>。</p>
 *
 * <p><b>生效口径</b>：本接口只改「默认值」。已发布批次按各自冻结快照执行；
 * 「发布时写 exam_rule_snapshot」的链路属考核域，待与同事分支对齐后再打通。</p>
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/business/rule")
public class AssessmentRuleController extends BaseController {

    @Autowired
    private IAssessmentRuleService ruleService;

    /**
     * 读取规则默认值
     */
    @PreAuthorize("@ss.hasAnyRoles('SUPER_ADMIN,admin,DEPT_ADMIN')")
    @GetMapping("/config")
    public AjaxResult config() {
        return AjaxResult.success(ruleService.getConfig());
    }

    /**
     * 保存规则默认值（超管可写，变更写入 audit_record）
     */
    @PreAuthorize("@ss.hasAnyRoles('SUPER_ADMIN,admin')")
    @Log(title = "考核规则默认值", businessType = BusinessType.UPDATE)
    @PutMapping("/config")
    public AjaxResult saveConfig(@RequestBody AssessmentConfig config) {
        return AjaxResult.success("已保存（已留痕）", ruleService.updateConfig(config));
    }

    /**
     * 协议模板列表（含历史版本，按生效时间倒序）
     */
    @PreAuthorize("@ss.hasAnyRoles('SUPER_ADMIN,admin,DEPT_ADMIN')")
    @GetMapping("/agreement-templates")
    public AjaxResult agreementTemplates() {
        return AjaxResult.success(ruleService.listAgreementTemplates());
    }

    /**
     * 发布协议模板新版本（模板只增不改：旧生效版自动归档）
     */
    @PreAuthorize("@ss.hasAnyRoles('SUPER_ADMIN,admin')")
    @Log(title = "协议模板", businessType = BusinessType.INSERT)
    @PostMapping("/agreement-templates")
    public AjaxResult publishAgreementTemplate(@RequestBody AgreementTemplate template) {
        return AjaxResult.success("新版本已发布，旧版本已归档", ruleService.publishAgreementTemplate(template));
    }
}
