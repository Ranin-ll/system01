package com.ruoyi.business.controller;

import com.ruoyi.business.domain.PromotionApplication;
import com.ruoyi.business.domain.PromotionRule;
import com.ruoyi.business.service.IPromotionService;
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
 * 实习转正 —— 管理与审批端（超管 / 部门管理员）。
 *
 * <p>职责：转正要求设置、候选人资格核对、转正申请审批、撤回转正纠错。</p>
 *
 * <p>数据范围：<b>超管全量、其余角色只看本部门</b>，收窄逻辑在 Service
 * （{@code scopeDeptId()}），不依赖前端传参。</p>
 *
 * <p>★ 路径前缀 {@code /business/promotion} 与实习生端的 {@code /business/intern/promotion}
 * 相互独立，避免两套权限模型混在一个类里。</p>
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/business/promotion")
public class PromotionController extends BaseController {

    @Autowired
    private IPromotionService promotionService;

    // ==================== 转正要求 ====================

    /**
     * 规则面板：全局默认 + 各部门覆盖。
     * 供超管「系统规则 › 转正要求」与部门端设置行渲染。
     */
    @PreAuthorize("@ss.hasPermi('business:promotion:rule')")
    @GetMapping("/rule/board")
    public AjaxResult ruleBoard() {
        return AjaxResult.success(promotionService.ruleBoard());
    }

    /**
     * 读取生效规则（带 source：DEPT / GLOBAL / DEFAULT）。
     *
     * <p>{@code deptId} 省略时的语义：<b>超管</b>读全局默认（即他管理的那个值）；
     * <b>部门管理员</b>自动落到本部门，读本部门生效值 —— 与实习生端看到的规则一致。</p>
     */
    @PreAuthorize("@ss.hasPermi('business:promotion:rule')")
    @GetMapping("/rule")
    public AjaxResult getRule(@RequestParam(value = "deptId", required = false) Long deptId) {
        return AjaxResult.success(promotionService.effectiveRuleForRequest(deptId));
    }

    /**
     * 保存转正要求（upsert）。
     *
     * <p>{@code deptId} 为空 → 写默认要求（仅超管）；非空 → 该部门单独设置
     * （部门管理员只能写本部门）。</p>
     *
     * <p>★ 没有「删除设置、恢复默认」的接口：本页语义就是「设置要求」，
     * 想回到默认值直接把两个数字改成默认值再保存即可。</p>
     */
    @PreAuthorize("@ss.hasPermi('business:promotion:rule')")
    @Log(title = "转正要求设置", businessType = BusinessType.UPDATE)
    @PutMapping("/rule")
    public AjaxResult saveRule(@RequestBody PromotionRule rule) {
        return toAjax(promotionService.saveRule(rule));
    }

    // ==================== 候选人 / 申请 ====================

    /**
     * 转正候选人看板：本部门在培实习生 + 最新申请 + 实时资格核对。
     *
     * @param status  ALL / PENDING / PASSED / REJECTED / REVOKED / ELIGIBLE
     * @param keyword 姓名或登录账号模糊匹配
     */
    @PreAuthorize("@ss.hasPermi('business:promotion:list')")
    @GetMapping("/candidates")
    public AjaxResult candidates(@RequestParam(value = "status", required = false) String status,
                                 @RequestParam(value = "keyword", required = false) String keyword) {
        return AjaxResult.success(promotionService.candidates(status, keyword));
    }

    /** 单人的资格核对清单（部门端拒绝前核对、或催办前确认）。 */
    @PreAuthorize("@ss.hasPermi('business:promotion:list')")
    @GetMapping("/gate/{userId}")
    public AjaxResult gate(@PathVariable Long userId) {
        return AjaxResult.success(promotionService.gateOf(userId));
    }

    /** 转正申请列表（分页，供统计与导出）。 */
    @PreAuthorize("@ss.hasPermi('business:promotion:list')")
    @GetMapping("/apps")
    public TableDataInfo apps(PromotionApplication query) {
        startPage();
        List<PromotionApplication> list = promotionService.applicationList(query);
        return getDataTable(list);
    }

    /** 申请详情：单据 + 审核历史 + 当前资格 + 证书。 */
    @PreAuthorize("@ss.hasPermi('business:promotion:query')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id) {
        return AjaxResult.success(promotionService.applicationDetail(id));
    }

    /** 申请的审核历史。 */
    @PreAuthorize("@ss.hasPermi('business:promotion:query')")
    @GetMapping("/{id}/history")
    public AjaxResult history(@PathVariable Long id) {
        return AjaxResult.success(promotionService.history(id));
    }

    // ==================== 审批 ====================

    /**
     * 审批转正申请：通过即生效（状态 + 角色 + 发证一次完成）。
     *
     * @param body {@code {id, action: PASS|REJECT, reason}}
     */
    @PreAuthorize("@ss.hasPermi('business:promotion:audit')")
    @Log(title = "转正审批", businessType = BusinessType.UPDATE)
    @PostMapping("/audit")
    public AjaxResult audit(@RequestBody Map<String, Object> body) {
        Long id = toLong(body.get("id"));
        Object action = body.get("action");
        Object reason = body.get("reason");
        if (id == null || action == null) {
            return AjaxResult.error("缺少必要参数：id / action");
        }
        return toAjax(promotionService.audit(id, String.valueOf(action),
                reason == null ? null : String.valueOf(reason)));
    }

    /**
     * 超管纠错：撤回转正（退回 PRE_TRAINEE、角色换回、证书作废）。
     *
     * @param body {@code {userId, reason}}
     */
    @PreAuthorize("@ss.hasPermi('business:promotion:revoke')")
    @Log(title = "撤回转正", businessType = BusinessType.UPDATE)
    @PostMapping("/revoke")
    public AjaxResult revoke(@RequestBody Map<String, Object> body) {
        Long userId = toLong(body.get("userId"));
        Object reason = body.get("reason");
        if (userId == null) {
            return AjaxResult.error("缺少必要参数：userId");
        }
        return toAjax(promotionService.revoke(userId, reason == null ? null : String.valueOf(reason)));
    }

    /**
     * ★ 禁止裸 {@code Long.valueOf(map.get("id"))}：泛型擦除后插入 checkcast，
     * JSON 数字会被解成 Integer，必然 ClassCastException。统一走这个安全转换。
     */
    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        String s = String.valueOf(value).trim();
        if (s.isEmpty() || "null".equals(s)) {
            return null;
        }
        try {
            return Long.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
