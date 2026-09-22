package com.ruoyi.business.service;

import com.ruoyi.business.domain.InternRegisterBody;
import com.ruoyi.business.domain.AgreementSignBody;

import java.util.Map;

/**
 * 实习生账号注册服务。
 */
public interface IInternAuthService {

    /**
     * 创建预备实习生账号并提交注册申请。
     *
     * @param body 注册信息
     * @return 注册申请编号
     */
    String register(InternRegisterBody body);

    Map<String, Object> getAgreementStatus(Long userId);

    /**
     * 读取某人的签署凭证（含签名图）。未签署过返回 null。
     */
    Map<String, Object> getAgreementSignature(Long userId);

    Map<String, Object> signAgreement(Long userId, AgreementSignBody body);
}
