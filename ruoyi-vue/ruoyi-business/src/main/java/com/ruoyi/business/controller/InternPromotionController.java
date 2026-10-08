package com.ruoyi.business.controller;

import com.ruoyi.business.service.IPromotionService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 实习转正 —— 实习生端。
 *
 * <p>与 {@code /business/promotion}（管理端）分离的原因：两侧是<b>两套权限模型</b> ——
 * 管理端用 {@code @ss.hasPermi}（菜单权限码），实习生端用角色
 * （{@code @ss.hasAnyRoles}，与 {@code /business/learning/**}、{@code /business/exam/**} 一致），
 * 混在一个类里会让类级注解互相打架。</p>
 *
 * <p>所有接口的 userId 一律取自登录态，不接受前端传参 —— 杜绝越权提交他人转正申请。</p>
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/business/intern/promotion")
@PreAuthorize("@ss.hasAnyRoles('PRE_TRAINEE,FORMAL_TRAINEE')")
public class InternPromotionController extends BaseController {

    @Autowired
    private IPromotionService promotionService;

    /**
     * 我的转正视图：生效规则 + 资格核对清单 + 最新申请 + 证书。
     * 实习生端「考核成绩与转正」页一次请求拿全。
     */
    @GetMapping("/mine")
    public AjaxResult mine() {
        return AjaxResult.success(promotionService.myApplication());
    }

    /**
     * 提交转正申请。
     *
     * @param body {@code {supplement}}
     */
    @PostMapping("/apply")
    public AjaxResult apply(@RequestBody(required = false) Map<String, Object> body) {
        Object supplement = body == null ? null : body.get("supplement");
        Long id = promotionService.apply(supplement == null ? null : String.valueOf(supplement));
        return AjaxResult.success("转正申请已提交，请等待部门管理员审核", id);
    }

    /** 撤回待审核的转正申请。 */
    @PostMapping("/withdraw")
    public AjaxResult withdraw() {
        return toAjax(promotionService.withdraw());
    }

    /** 我的电子证书（当前生效 + 历史）。 */
    @GetMapping("/certificate")
    public AjaxResult certificate() {
        return AjaxResult.success(promotionService.myCertificate());
    }
}
