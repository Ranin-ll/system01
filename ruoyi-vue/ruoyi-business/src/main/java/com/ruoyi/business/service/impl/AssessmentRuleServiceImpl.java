package com.ruoyi.business.service.impl;

import com.ruoyi.business.domain.AgreementTemplate;
import com.ruoyi.business.domain.AssessmentConfig;
import com.ruoyi.business.domain.AuditRecord;
import com.ruoyi.business.mapper.AgreementTemplateMapper;
import com.ruoyi.business.mapper.AssessmentConfigMapper;
import com.ruoyi.business.mapper.AuditRecordMapper;
import com.ruoyi.business.service.IAssessmentRuleService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 规则与配置 Service 实现
 *
 * @author ruoyi
 */
@Service
public class AssessmentRuleServiceImpl implements IAssessmentRuleService {

    /** 留痕业务类型 */
    private static final String BIZ_TYPE = "ASSESSMENT_CONFIG";

    @Autowired
    private AssessmentConfigMapper configMapper;

    @Autowired
    private AgreementTemplateMapper templateMapper;

    @Autowired
    private AuditRecordMapper auditRecordMapper;

    @Override
    public AssessmentConfig getConfig() {
        AssessmentConfig config = configMapper.selectById(AssessmentConfig.SINGLETON_ID);
        if (config == null) {
            // 表在但行被删时兜底补一行（建表脚本已插入，正常走不到这里）
            config = new AssessmentConfig();
            config.setId(AssessmentConfig.SINGLETON_ID);
            configMapper.insert(config);
            config = configMapper.selectById(AssessmentConfig.SINGLETON_ID);
        }
        return config;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssessmentConfig updateConfig(AssessmentConfig config) {
        if (config == null) {
            throw new ServiceException("配置内容不能为空");
        }
        AssessmentConfig old = getConfig();
        config.setId(AssessmentConfig.SINGLETON_ID);

        validate(config, old);

        config.setUpdateBy(SecurityUtils.getUsername());
        config.setUpdateTime(new Date());
        configMapper.updateById(config);

        String reason = diffSummary(old, config);
        writeAudit(reason);
        return configMapper.selectById(AssessmentConfig.SINGLETON_ID);
    }

    @Override
    public List<AgreementTemplate> listAgreementTemplates() {
        return templateMapper.selectAllOrderByEffective();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgreementTemplate publishAgreementTemplate(AgreementTemplate template) {
        if (template == null || isBlank(template.getAgreementName()) || isBlank(template.getVersionNo())) {
            throw new ServiceException("协议名称与版本号不能为空");
        }
        if (isBlank(template.getContent()) && isBlank(template.getFileUrl())) {
            throw new ServiceException("协议正文与附件地址至少填一个");
        }
        if (templateMapper.countByVersionNo(template.getVersionNo().trim()) > 0) {
            throw new ServiceException("版本号「" + template.getVersionNo().trim() + "」已存在，请换一个");
        }
        // 模板只增不改：先把旧的生效版归档，再落新版本为 EFFECTIVE
        templateMapper.archiveEffective();

        AgreementTemplate entity = new AgreementTemplate();
        entity.setAgreementName(template.getAgreementName().trim());
        entity.setVersionNo(template.getVersionNo().trim());
        entity.setContent(template.getContent());
        entity.setFileUrl(template.getFileUrl());
        entity.setEffectiveTime(template.getEffectiveTime() == null ? new Date() : template.getEffectiveTime());
        entity.setStatus(AgreementTemplate.STATUS_EFFECTIVE);
        entity.setSignCount(0);
        entity.setCreateBy(SecurityUtils.getUsername());
        entity.setCreateTime(new Date());
        entity.setUpdateTime(new Date());
        templateMapper.insert(entity);

        writeAudit("发布协议模板新版本：" + entity.getAgreementName() + " " + entity.getVersionNo());
        return entity;
    }

    // ------------------------------------------------------------------ 内部

    private void validate(AssessmentConfig c, AssessmentConfig old) {
        // 未传的字段沿用旧值，避免部分提交把其它字段清空
        if (c.getCourseDoneThreshold() == null) c.setCourseDoneThreshold(old.getCourseDoneThreshold());
        if (c.getTheoryWeight() == null) c.setTheoryWeight(old.getTheoryWeight());
        if (c.getPracticeWeight() == null) c.setPracticeWeight(old.getPracticeWeight());
        if (c.getTheoryPassLine() == null) c.setTheoryPassLine(old.getTheoryPassLine());
        if (c.getPracticePassLine() == null) c.setPracticePassLine(old.getPracticePassLine());
        if (c.getProtocolRequired() == null) c.setProtocolRequired(old.getProtocolRequired());
        if (c.getRetakeCount() == null) c.setRetakeCount(old.getRetakeCount());
        if (c.getCertNoRule() == null) c.setCertNoRule(old.getCertNoRule());
        if (c.getAlertBankGapFactor() == null) c.setAlertBankGapFactor(old.getAlertBankGapFactor());
        if (c.getAlertExamDraftDays() == null) c.setAlertExamDraftDays(old.getAlertExamDraftDays());
        if (c.getAlertRegisterPending() == null) c.setAlertRegisterPending(old.getAlertRegisterPending());
        if (c.getAlertDeptDoneFloor() == null) c.setAlertDeptDoneFloor(old.getAlertDeptDoneFloor());
        if (c.getAlertOverdueHours() == null) c.setAlertOverdueHours(old.getAlertOverdueHours());

        pct("学习门槛（必修完成率）", c.getCourseDoneThreshold());
        pct("理论权重", c.getTheoryWeight());
        pct("实操权重", c.getPracticeWeight());
        score("理论通过线", c.getTheoryPassLine());
        score("实操通过线", c.getPracticePassLine());
        pct("预警：部门完成率下限", c.getAlertDeptDoneFloor());

        BigDecimal sum = nz(c.getTheoryWeight()).add(nz(c.getPracticeWeight()));
        if (sum.compareTo(new BigDecimal("100")) != 0) {
            throw new ServiceException("理论权重与实操权重之和必须等于 100（当前 " + sum.stripTrailingZeros().toPlainString() + "）");
        }
        intRange("补考次数", c.getRetakeCount(), 0, 10);
        intRange("预警：题库题量倍数", c.getAlertBankGapFactor(), 1, 20);
        intRange("预警：考核草稿天数", c.getAlertExamDraftDays(), 1, 365);
        intRange("预警：报名待审核条数", c.getAlertRegisterPending(), 1, 999);
        intRange("预警：考核逾期小时", c.getAlertOverdueHours(), 1, 720);
        if (isBlank(c.getCertNoRule())) {
            throw new ServiceException("证书编号规则不能为空");
        }
    }

    /** 生成「谁改了哪条、从多少到多少」的可读摘要，写入审计 */
    private String diffSummary(AssessmentConfig o, AssessmentConfig n) {
        List<String> parts = new ArrayList<>();
        cmp(parts, "学习门槛", o.getCourseDoneThreshold(), n.getCourseDoneThreshold());
        cmp(parts, "理论权重", o.getTheoryWeight(), n.getTheoryWeight());
        cmp(parts, "实操权重", o.getPracticeWeight(), n.getPracticeWeight());
        cmp(parts, "理论通过线", o.getTheoryPassLine(), n.getTheoryPassLine());
        cmp(parts, "实操通过线", o.getPracticePassLine(), n.getPracticePassLine());
        cmp(parts, "补考次数", o.getRetakeCount(), n.getRetakeCount());
        cmp(parts, "协议必签", o.getProtocolRequired(), n.getProtocolRequired());
        cmp(parts, "证书编号规则", o.getCertNoRule(), n.getCertNoRule());
        cmp(parts, "预警-题库倍数", o.getAlertBankGapFactor(), n.getAlertBankGapFactor());
        cmp(parts, "预警-草稿天数", o.getAlertExamDraftDays(), n.getAlertExamDraftDays());
        cmp(parts, "预警-待审条数", o.getAlertRegisterPending(), n.getAlertRegisterPending());
        cmp(parts, "预警-完成率下限", o.getAlertDeptDoneFloor(), n.getAlertDeptDoneFloor());
        cmp(parts, "预警-逾期小时", o.getAlertOverdueHours(), n.getAlertOverdueHours());
        return parts.isEmpty() ? "保存配置（无字段变化）" : String.join("；", parts);
    }

    private void cmp(List<String> parts, String label, Object oldVal, Object newVal) {
        if (!Objects.equals(str(oldVal), str(newVal))) {
            parts.add(label + " " + str(oldVal) + "→" + str(newVal));
        }
    }

    private String str(Object v) {
        if (v == null) {
            return "-";
        }
        if (v instanceof BigDecimal) {
            return ((BigDecimal) v).stripTrailingZeros().toPlainString();
        }
        return String.valueOf(v);
    }

    private void writeAudit(String reason) {
        AuditRecord record = new AuditRecord();
        record.setBizType(BIZ_TYPE);
        record.setBizId(AssessmentConfig.SINGLETON_ID);
        record.setAction("UPDATE");
        record.setReason(reason);
        Long userId = SecurityUtils.getUserId();
        record.setUserId(userId);
        record.setOperatorId(userId);
        record.setDeptId(SecurityUtils.getDeptId());
        record.setFromStatus(null);
        record.setToStatus(null);
        record.setCreateTime(new Date());
        auditRecordMapper.insertAuditRecord(record);
    }

    private void pct(String name, BigDecimal v) {
        if (v == null || v.compareTo(BigDecimal.ZERO) < 0 || v.compareTo(new BigDecimal("100")) > 0) {
            throw new ServiceException(name + "必须在 0~100 之间");
        }
    }

    private void score(String name, BigDecimal v) {
        if (v == null || v.compareTo(BigDecimal.ZERO) < 0 || v.compareTo(new BigDecimal("1000")) > 0) {
            throw new ServiceException(name + "必须在 0~1000 之间");
        }
    }

    private void intRange(String name, Integer v, int min, int max) {
        if (v == null || v < min || v > max) {
            throw new ServiceException(name + "必须在 " + min + "~" + max + " 之间");
        }
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
