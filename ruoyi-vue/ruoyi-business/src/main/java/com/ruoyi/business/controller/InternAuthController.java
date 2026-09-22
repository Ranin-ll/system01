package com.ruoyi.business.controller;

import com.ruoyi.business.domain.InternRegisterBody;
import com.ruoyi.business.domain.AgreementSignBody;
import com.ruoyi.business.domain.Position;
import com.ruoyi.business.mapper.InternAuthMapper;
import com.ruoyi.business.service.IInternAuthService;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 实习生门户的公开认证接口。
 */
@RestController
@RequestMapping("/business/auth")
public class InternAuthController {

    @Autowired
    private IInternAuthService internAuthService;

    @Autowired
    private InternAuthMapper internAuthMapper;

    /**
     * 注册页的岗位下拉数据。
     */
    @Anonymous
    @GetMapping("/positions")
    public AjaxResult positions() {
        List<Position> positions = internAuthMapper.selectEnabledPositions();
        return AjaxResult.success(positions);
    }

    /**
     * 创建预备实习生账号并提交注册审核。
     */
    @Anonymous
    @PostMapping("/register")
    public AjaxResult register(@RequestBody InternRegisterBody body) {
        String applicationNo = internAuthService.register(body);
        return AjaxResult.success("注册申请已提交，请等待部门管理员审核通过后登录", applicationNo);
    }

    /**
     * 查询当前登录实习生的保密协议签署状态。
     */
    @GetMapping("/agreement")
    public AjaxResult agreement() {
        return AjaxResult.success(internAuthService.getAgreementStatus(SecurityUtils.getUserId()));
    }

    /**
     * 读取本人的签署凭证（含签名图）。未签署过返回 data=null。
     */
    @GetMapping("/agreement/signature")
    public AjaxResult agreementSignature() {
        return AjaxResult.success(internAuthService.getAgreementSignature(SecurityUtils.getUserId()));
    }

    /**
     * 签署当前生效的保密协议。
     */
    @PostMapping("/agreement/sign")
    public AjaxResult signAgreement(@RequestBody AgreementSignBody body) {
        return AjaxResult.success("保密协议签署成功",
                internAuthService.signAgreement(SecurityUtils.getUserId(), body));
    }
}
