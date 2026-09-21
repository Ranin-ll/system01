package com.ruoyi.business.service;

import com.ruoyi.business.domain.AgreementTemplate;
import com.ruoyi.business.domain.AssessmentConfig;

import java.util.List;

/**
 * 规则与配置（超管端）Service
 *
 * <p>超管在业务侧**唯一可写**的地方：考核规则默认值、证书编号规则、异常预警阈值、协议模板版本。
 * 共同口径：本模块只写「默认值」，**不改已发布批次的冻结快照**。</p>
 *
 * @author ruoyi
 */
public interface IAssessmentRuleService {

    /**
     * 读取单行配置；不存在时按建表默认值补一行再返回（保证接口幂等可用）
     */
    AssessmentConfig getConfig();

    /**
     * 保存配置（含变更留痕）。校验不通过抛 ServiceException。
     */
    AssessmentConfig updateConfig(AssessmentConfig config);

    /**
     * 协议模板列表，按生效时间倒序
     */
    List<AgreementTemplate> listAgreementTemplates();

    /**
     * 发布新版本协议模板：旧的生效版归档，新版本置为 EFFECTIVE（模板只增不改）
     */
    AgreementTemplate publishAgreementTemplate(AgreementTemplate template);
}
