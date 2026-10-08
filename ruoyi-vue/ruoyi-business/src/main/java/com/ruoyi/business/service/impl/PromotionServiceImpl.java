package com.ruoyi.business.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.business.domain.AssessmentConfig;
import com.ruoyi.business.domain.AuditRecord;
import com.ruoyi.business.domain.Certificate;
import com.ruoyi.business.domain.PromotionApplication;
import com.ruoyi.business.domain.PromotionCandidate;
import com.ruoyi.business.domain.PromotionGate;
import com.ruoyi.business.domain.PromotionRule;
import com.ruoyi.business.mapper.AssessmentConfigMapper;
import com.ruoyi.business.mapper.AuditRecordMapper;
import com.ruoyi.business.mapper.CertificateMapper;
import com.ruoyi.business.mapper.InternAuthMapper;
import com.ruoyi.business.mapper.PromotionApplicationMapper;
import com.ruoyi.business.mapper.PromotionGateMapper;
import com.ruoyi.business.mapper.PromotionRuleMapper;
import com.ruoyi.business.service.ILearningService;
import com.ruoyi.business.service.IPromotionService;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.SysUserRole;
import com.ruoyi.system.mapper.SysUserRoleMapper;
import com.ruoyi.system.service.ISysUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 实习转正服务实现。
 *
 * <p>见 {@link IPromotionService} 的链路说明。此处只补充几个实现层面的关键决策：</p>
 * <ul>
 *   <li><b>资格口径单一来源</b>：{@link #buildGate} 是三端（实习生清单 / 部门候选人判定 /
 *       提交时服务端校验）唯一入口 —— 不会出现「前端说能提交、后端说不行」。</li>
 *   <li><b>审批通过是一个原子事务</b>：申请单状态 + {@code sys_user.user_status} + 角色换绑 +
 *       发证 + 审核留痕，五件事同生共死。中途失败不允许出现「转了正但没证」。</li>
 *   <li><b>角色换绑用增删而非覆盖</b>：只删 {@code PRE_TRAINEE}、加 {@code FORMAL_TRAINEE}，
 *       不动该人可能持有的其它角色。</li>
 * </ul>
 *
 * @author ruoyi
 */
@Service
public class PromotionServiceImpl implements IPromotionService {

    private static final Logger log = LoggerFactory.getLogger(PromotionServiceImpl.class);

    private static final String BIZ_PROMOTION = "PROMOTION";
    private static final String ROLE_PRE = "PRE_TRAINEE";
    private static final String ROLE_FORMAL = "FORMAL_TRAINEE";
    private static final String STATUS_NOTE_REVOKE = "REVOKED";

    /** 证书编号规则的内置兜底（assessment_config.cert_no_rule 为空时用） */
    private static final String DEFAULT_CERT_NO_RULE = "{岗位缩写}-{年}-{月日}-{序号}";

    private final PromotionRuleMapper ruleMapper;
    private final PromotionApplicationMapper applicationMapper;
    private final PromotionGateMapper gateMapper;
    private final CertificateMapper certificateMapper;
    private final AuditRecordMapper auditRecordMapper;
    private final InternAuthMapper internAuthMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final ISysUserService userService;
    private final ILearningService learningService;
    private final AssessmentConfigMapper configMapper;
    private final ObjectMapper objectMapper;

    public PromotionServiceImpl(PromotionRuleMapper ruleMapper,
                                PromotionApplicationMapper applicationMapper,
                                PromotionGateMapper gateMapper,
                                CertificateMapper certificateMapper,
                                AuditRecordMapper auditRecordMapper,
                                InternAuthMapper internAuthMapper,
                                SysUserRoleMapper userRoleMapper,
                                ISysUserService userService,
                                ILearningService learningService,
                                AssessmentConfigMapper configMapper,
                                ObjectMapper objectMapper) {
        this.ruleMapper = ruleMapper;
        this.applicationMapper = applicationMapper;
        this.gateMapper = gateMapper;
        this.certificateMapper = certificateMapper;
        this.auditRecordMapper = auditRecordMapper;
        this.internAuthMapper = internAuthMapper;
        this.userRoleMapper = userRoleMapper;
        this.userService = userService;
        this.learningService = learningService;
        this.configMapper = configMapper;
        this.objectMapper = objectMapper;
    }

    // ==================================================================
    // ① 转正要求（规则）
    // ==================================================================

    @Override
    public PromotionRule effectiveRule(Long deptId) {
        // 1) 部门覆盖
        if (deptId != null) {
            PromotionRule deptRule = ruleMapper.selectByDeptId(deptId);
            if (usable(deptRule)) {
                deptRule.setSource("DEPT");
                return deptRule;
            }
        }
        // 2) 全局默认
        PromotionRule global = ruleMapper.selectGlobalRule();
        if (usable(global)) {
            global.setSource("GLOBAL");
            return global;
        }
        // 3) 内置兜底
        PromotionRule fallback = new PromotionRule();
        fallback.setDeptId(null);
        fallback.setStudyRateMin(PromotionRule.DEFAULT_STUDY_RATE_MIN);
        fallback.setExamPassTimes(PromotionRule.DEFAULT_EXAM_PASS_TIMES);
        fallback.setEnabled(1);
        fallback.setSource("DEFAULT");
        return fallback;
    }

    @Override
    public PromotionRule effectiveRuleForRequest(Long deptId) {
        if (deptId != null) {
            return effectiveRule(deptId);
        }
        // deptId 缺省：超管读全局默认（那就是他管理的值），部门管理员读本部门生效值
        return isSuper() ? effectiveRule(null) : effectiveRule(requireOwnDept());
    }

    private boolean usable(PromotionRule rule) {
        return rule != null && !Integer.valueOf(0).equals(rule.getEnabled());
    }

    @Override
    public Map<String, Object> ruleBoard() {
        List<PromotionRule> all = ruleMapper.selectAllRules();
        // 数据范围：超管看全部部门覆盖，部门管理员只看本部门的覆盖
        Long scope = scopeDeptId();
        PromotionRule global = null;
        List<PromotionRule> overrides = new ArrayList<>();
        for (PromotionRule rule : all) {
            if (rule.getDeptId() == null) {
                if (global == null) {
                    global = rule;
                }
            } else if (scope == null || scope.equals(rule.getDeptId())) {
                overrides.add(rule);
            }
        }
        PromotionRule effectiveGlobal = effectiveRule(null);

        Map<String, Object> board = new LinkedHashMap<>();
        board.put("global", global);
        board.put("globalEffective", effectiveGlobal);
        board.put("overrides", overrides);
        board.put("defaults", defaultRule());
        return board;
    }

    private PromotionRule defaultRule() {
        PromotionRule fallback = new PromotionRule();
        fallback.setStudyRateMin(PromotionRule.DEFAULT_STUDY_RATE_MIN);
        fallback.setExamPassTimes(PromotionRule.DEFAULT_EXAM_PASS_TIMES);
        fallback.setEnabled(1);
        fallback.setSource("DEFAULT");
        return fallback;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveRule(PromotionRule rule) {
        if (rule == null) {
            throw new ServiceException("转正要求不能为空");
        }
        Long deptId = rule.getDeptId();
        // 部门管理员只能写本部门；全局规则仅超管可写
        if (!isSuper()) {
            Long own = requireOwnDept();
            if (deptId == null) {
                throw new ServiceException("只有超级管理员可以修改全局默认转正要求");
            }
            if (!own.equals(deptId)) {
                throw new ServiceException("只能设置本部门的转正要求");
            }
        }

        int rateMin = rule.getStudyRateMin() == null ? PromotionRule.DEFAULT_STUDY_RATE_MIN : rule.getStudyRateMin();
        int passTimes = rule.getExamPassTimes() == null ? PromotionRule.DEFAULT_EXAM_PASS_TIMES : rule.getExamPassTimes();
        if (rateMin < 0 || rateMin > 100) {
            throw new ServiceException("学习完成率门槛需在 0 ~ 100 之间");
        }
        if (passTimes < 1 || passTimes > 99) {
            throw new ServiceException("正式考核通过次数需在 1 ~ 99 之间");
        }

        PromotionRule existing = deptId == null
                ? ruleMapper.selectGlobalRule()
                : ruleMapper.selectByDeptId(deptId);

        PromotionRule toSave = new PromotionRule();
        toSave.setDeptId(deptId);
        toSave.setStudyRateMin(rateMin);
        toSave.setExamPassTimes(passTimes);
        toSave.setEnabled(1);
        toSave.setCreateBy(SecurityUtils.getUsername());

        int rows;
        if (existing == null) {
            rows = ruleMapper.insertRule(toSave);
        } else {
            toSave.setId(existing.getId());
            rows = ruleMapper.updateRule(toSave);
        }
        return rows;
    }

    // ==================================================================
    // ② 资格核对
    // ==================================================================

    @Override
    public PromotionGate gateOf(Long userId) {
        if (userId == null) {
            throw new ServiceException("用户不存在");
        }
        Map<String, Object> brief = gateMapper.selectUserBrief(userId);
        if (brief == null) {
            throw new ServiceException("用户不存在");
        }
        Long deptId = toLong(brief.get("deptId"));
        PromotionRule rule = effectiveRule(deptId);
        BigDecimal studyRate = learningService.studyRateOf(userId);
        int passed = gateMapper.selectPassedFormalExamTimes(userId);
        int total = gateMapper.selectFormalExamTimes(userId);
        Integer signed = gateMapper.selectProtocolStatus(userId);
        return buildGate(rule, studyRate, passed, total, signed);
    }

    /**
     * 三端共用的资格判定入口。
     *
     * @param rule      生效规则
     * @param studyRate 学习完成率（null = 无课程）
     * @param passed    已通过的正式考核场次数
     * @param total     已参加的正式考核场次数
     * @param signed    协议签署：1 已签 / 0 未签 / null 未知
     */
    private PromotionGate buildGate(PromotionRule rule, BigDecimal studyRate,
                                    int passed, int total, Integer signed) {
        PromotionGate gate = new PromotionGate();
        int rateMin = rule.getStudyRateMin() == null ? PromotionRule.DEFAULT_STUDY_RATE_MIN : rule.getStudyRateMin();
        int passTimes = rule.getExamPassTimes() == null ? PromotionRule.DEFAULT_EXAM_PASS_TIMES : rule.getExamPassTimes();
        gate.setStudyRateMin(rateMin);
        gate.setExamPassTimes(passTimes);
        gate.setSource(rule.getSource());
        gate.setRuleDeptId(rule.getDeptId());
        gate.setStudyRate(studyRate);
        gate.setPassedExamTimes(passed);
        gate.setExamTimes(total);
        gate.setProtocolSigned(signed == null ? 0 : signed);

        // 门槛为 0 时视为「不卡学习率」：永远通过（但文案要说明是「不设门槛」）
        boolean studyPass = rateMin <= 0 || (studyRate != null && studyRate.compareTo(BigDecimal.valueOf(rateMin)) >= 0);
        String studyValue = studyRate == null ? "暂无课程" : ("当前 " + strip(studyRate) + "%");
        String studyLabel = rateMin <= 0 ? "学习完成率（未设门槛）" : ("学习完成率 ≥ " + rateMin + "%");
        gate.addItem("study", studyPass, studyLabel, studyValue);

        gate.addItem("exam", passed >= passTimes,
                "正式考核已通过 " + passTimes + " 次",
                "当前 " + passed + " 次（已参加 " + total + " 场）");

        boolean protocolPass = signed != null && signed == 1;
        gate.addItem("protocol", protocolPass, "保密协议已签署", protocolPass ? "已签署" : "待签署");

        gate.settle();
        return gate;
    }

    private String strip(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    // ==================================================================
    // ③ 实习生端
    // ==================================================================

    @Override
    public Map<String, Object> myApplication() {
        Long userId = SecurityUtils.getUserId();
        Map<String, Object> brief = gateMapper.selectUserBrief(userId);
        if (brief == null) {
            throw new ServiceException("当前账号不存在");
        }
        Long deptId = toLong(brief.get("deptId"));
        PromotionRule rule = effectiveRule(deptId);
        PromotionGate gate = buildGate(rule, learningService.studyRateOf(userId),
                gateMapper.selectPassedFormalExamTimes(userId),
                gateMapper.selectFormalExamTimes(userId),
                gateMapper.selectProtocolStatus(userId));

        PromotionApplication application = applicationMapper.selectLatestByUserId(userId);
        String status = application == null ? null : application.getStatus();
        boolean canApply = Boolean.TRUE.equals(gate.getEligible())
                && !PromotionApplication.STATUS_DEPT_PENDING.equals(status)
                && !PromotionApplication.STATUS_PASSED.equals(status);

        Map<String, Object> view = new LinkedHashMap<>();
        view.put("rule", rule);
        view.put("gate", gate);
        view.put("canApply", canApply);
        view.put("application", application);
        view.put("certificate", certificateMapper.selectActiveByUserId(userId));
        return view;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long apply(String supplement) {
        Long userId = SecurityUtils.getUserId();
        Map<String, Object> brief = gateMapper.selectUserBrief(userId);
        if (brief == null) {
            throw new ServiceException("当前账号不存在");
        }
        Long deptId = toLong(brief.get("deptId"));
        if (deptId == null) {
            throw new ServiceException("当前账号未配置部门，无法提交转正申请");
        }
        String status = brief.get("userStatus") == null ? null : String.valueOf(brief.get("userStatus"));
        if (PromotionApplication.STATUS_PASSED.equals(status) || "FORMAL_TRAINEE".equals(status)) {
            throw new ServiceException("你已是正式实习生，无需重复提交转正申请");
        }

        // 服务端重新校验资格，不信任前端
        PromotionRule rule = effectiveRule(deptId);
        PromotionGate gate = buildGate(rule, learningService.studyRateOf(userId),
                gateMapper.selectPassedFormalExamTimes(userId),
                gateMapper.selectFormalExamTimes(userId),
                gateMapper.selectProtocolStatus(userId));
        if (!Boolean.TRUE.equals(gate.getEligible())) {
            StringBuilder miss = new StringBuilder();
            for (Map<String, Object> item : gate.getItems()) {
                if (!Boolean.TRUE.equals(item.get("pass"))) {
                    if (miss.length() > 0) {
                        miss.append("；");
                    }
                    miss.append(item.get("label")).append("：").append(item.get("value"));
                }
            }
            throw new ServiceException("转正资格尚未齐备：" + miss);
        }

        if (applicationMapper.countActiveByUserId(userId) > 0) {
            throw new ServiceException("你已有一条待审核的转正申请，无需重复提交");
        }

        PromotionApplication latest = applicationMapper.selectLatestByUserId(userId);
        String gateJson = toJson(gate);

        // 已驳回 / 已作废 → 沿用例行重新提交（不新增单据，保留历史轨迹）
        if (latest != null && (PromotionApplication.STATUS_REJECTED.equals(latest.getStatus())
                || PromotionApplication.STATUS_REVOKED.equals(latest.getStatus()))) {
            String fromStatus = latest.getStatus();
            latest.setStatus(PromotionApplication.STATUS_DEPT_PENDING);
            latest.setGateCheckJson(gateJson);
            latest.setSupplement(supplement);
            latest.setRejectReason(null);
            latest.setDeptApproveBy(null);
            latest.setDeptApproveTime(null);
            latest.setCertNo(null);
            applicationMapper.updateApplication(latest);

            writeAudit(latest.getId(), userId, deptId, "RESUBMIT", fromStatus,
                    PromotionApplication.STATUS_DEPT_PENDING, "重新提交转正申请");
            return latest.getId();
        }

        PromotionApplication application = new PromotionApplication();
        application.setApplicationNo("PM" + System.currentTimeMillis()
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        application.setUserId(userId);
        application.setDeptId(deptId);
        application.setStatus(PromotionApplication.STATUS_DEPT_PENDING);
        application.setGateCheckJson(gateJson);
        application.setSupplement(supplement);
        applicationMapper.insertApplication(application);

        writeAudit(application.getId(), userId, deptId, "SUBMIT", null,
                PromotionApplication.STATUS_DEPT_PENDING, "提交转正申请");
        return application.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int withdraw() {
        Long userId = SecurityUtils.getUserId();
        PromotionApplication latest = applicationMapper.selectLatestByUserId(userId);
        if (latest == null || !PromotionApplication.STATUS_DEPT_PENDING.equals(latest.getStatus())) {
            throw new ServiceException("没有可撤回的待审核申请");
        }
        latest.setStatus(PromotionApplication.STATUS_REVOKED);
        latest.setRejectReason("实习生主动撤回");
        int rows = applicationMapper.updateApplication(latest);
        writeAudit(latest.getId(), userId, latest.getDeptId(), "WITHDRAW",
                PromotionApplication.STATUS_DEPT_PENDING, PromotionApplication.STATUS_REVOKED, "实习生主动撤回");
        return rows;
    }

    @Override
    public Map<String, Object> myCertificate() {
        Long userId = SecurityUtils.getUserId();
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("certificate", certificateMapper.selectActiveByUserId(userId));
        view.put("history", certificateMapper.selectByUserId(userId));
        return view;
    }

    // ==================================================================
    // ④ 部门管理员 / 超管端
    // ==================================================================

    @Override
    public Map<String, Object> candidates(String status, String keyword) {
        Long scope = scopeDeptId();
        List<PromotionCandidate> rows = applicationMapper.selectCandidateRows(scope);

        // 批量取正式考核场次，避免逐人 N+1
        Map<Long, int[]> examMap = new LinkedHashMap<>();
        for (Map<String, Object> m : gateMapper.selectExamTimesByDept(scope)) {
            Long uid = toLong(m.get("userId"));
            if (uid == null) {
                continue;
            }
            examMap.put(uid, new int[]{
                    toInt(m.get("passedTimes")),
                    toInt(m.get("examTimes"))
            });
        }

        // 部门规则缓存（同一部门的规则只查一次）
        Map<Long, PromotionRule> ruleCache = new LinkedHashMap<>();
        List<PromotionCandidate> enriched = new ArrayList<>();
        for (PromotionCandidate row : rows) {
            PromotionRule rule = ruleCache.computeIfAbsent(
                    row.getDeptId() == null ? -1L : row.getDeptId(),
                    key -> effectiveRule(row.getDeptId() == null ? null : row.getDeptId()));
            int[] exam = examMap.getOrDefault(row.getUserId(), new int[]{0, 0});
            BigDecimal studyRate = learningService.studyRateOf(row.getUserId());
            PromotionGate gate = buildGate(rule, studyRate, exam[0], exam[1], row.getProtocolSigned());
            row.setGate(gate);
            row.setStudyRate(studyRate);
            row.setPassedExamTimes(exam[0]);
            row.setExamTimes(exam[1]);
            row.setProtocolSigned(row.getProtocolSigned() == null ? 0 : row.getProtocolSigned());
            row.setTone(toneOf(row));
            enriched.add(row);
        }

        // 汇总（在过滤前统计，保证页签数字与全量一致）
        Map<String, Integer> summary = new LinkedHashMap<>();
        summary.put("total", enriched.size());
        summary.put("pending", 0);
        summary.put("passed", 0);
        summary.put("rejected", 0);
        summary.put("revoked", 0);
        summary.put("eligible", 0);
        for (PromotionCandidate row : enriched) {
            String st = row.getApplicationStatus();
            if (PromotionApplication.STATUS_DEPT_PENDING.equals(st)) {
                summary.put("pending", summary.get("pending") + 1);
            } else if (PromotionApplication.STATUS_PASSED.equals(st)) {
                summary.put("passed", summary.get("passed") + 1);
            } else if (PromotionApplication.STATUS_REJECTED.equals(st)) {
                summary.put("rejected", summary.get("rejected") + 1);
            } else if (PromotionApplication.STATUS_REVOKED.equals(st)) {
                summary.put("revoked", summary.get("revoked") + 1);
            }
            if (isEligibleUnsubmitted(row)) {
                summary.put("eligible", summary.get("eligible") + 1);
            }
        }

        List<PromotionCandidate> filtered = new ArrayList<>();
        for (PromotionCandidate row : enriched) {
            if (!matchStatus(row, status)) {
                continue;
            }
            if (StringUtils.isNotEmpty(keyword)) {
                String name = row.getNickName() == null ? "" : row.getNickName();
                String account = row.getUserName() == null ? "" : row.getUserName();
                if (!name.contains(keyword) && !account.contains(keyword)) {
                    continue;
                }
            }
            filtered.add(row);
        }

        Map<String, Object> view = new LinkedHashMap<>();
        view.put("rows", filtered);
        view.put("summary", summary);
        view.put("rule", effectiveRule(scope));
        return view;
    }

    /** 资格齐备但尚未提交（或上次被驳回/撤回）—— 部门管理员的「可催办」清单。 */
    private boolean isEligibleUnsubmitted(PromotionCandidate row) {
        if (row.getGate() == null || !Boolean.TRUE.equals(row.getGate().getEligible())) {
            return false;
        }
        String st = row.getApplicationStatus();
        return !PromotionApplication.STATUS_DEPT_PENDING.equals(st)
                && !PromotionApplication.STATUS_PASSED.equals(st);
    }

    private boolean matchStatus(PromotionCandidate row, String status) {
        if (StringUtils.isEmpty(status) || "ALL".equals(status)) {
            return true;
        }
        String st = row.getApplicationStatus();
        switch (status) {
            case "PENDING":
                return PromotionApplication.STATUS_DEPT_PENDING.equals(st);
            case "PASSED":
                return PromotionApplication.STATUS_PASSED.equals(st);
            case "REJECTED":
                return PromotionApplication.STATUS_REJECTED.equals(st);
            case "REVOKED":
                return PromotionApplication.STATUS_REVOKED.equals(st);
            case "ELIGIBLE":
                return isEligibleUnsubmitted(row);
            default:
                return true;
        }
    }

    private String toneOf(PromotionCandidate row) {
        PromotionGate gate = row.getGate();
        if (gate == null) {
            return "gray";
        }
        if (PromotionApplication.STATUS_DEPT_PENDING.equals(row.getApplicationStatus())) {
            return "blue";
        }
        if (PromotionApplication.STATUS_PASSED.equals(row.getApplicationStatus())) {
            return "green";
        }
        if (!Boolean.TRUE.equals(gate.getEligible())) {
            return "red";
        }
        return "orange";
    }

    @Override
    public Map<String, Object> applicationDetail(Long id) {
        PromotionApplication application = requireVisible(id);
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("application", application);
        view.put("history", auditRecordMapper.selectByBiz(BIZ_PROMOTION, id));
        view.put("gate", gateOf(application.getUserId()));
        view.put("certificate", certificateMapper.selectByPromotionId(id));
        return view;
    }

    @Override
    public List<AuditRecord> history(Long id) {
        requireVisible(id);
        return auditRecordMapper.selectByBiz(BIZ_PROMOTION, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int audit(Long id, String action, String reason) {
        PromotionApplication application = requireVisible(id);
        if (!PromotionApplication.STATUS_DEPT_PENDING.equals(application.getStatus())) {
            throw new ServiceException("该申请已被处理，无需重复审批");
        }
        Long operatorId = SecurityUtils.getUserId();
        Long operatorDept = SecurityUtils.getDeptId();
        String fromStatus = application.getStatus();
        String toStatus;

        if ("REJECT".equals(action)) {
            if (StringUtils.isEmpty(reason)) {
                throw new ServiceException("驳回必须填写原因");
            }
            application.setStatus(PromotionApplication.STATUS_REJECTED);
            application.setRejectReason(reason);
            application.setDeptApproveBy(operatorId);
            application.setDeptApproveTime(new Date());
            toStatus = PromotionApplication.STATUS_REJECTED;
            applicationMapper.updateApplication(application);
        } else if ("PASS".equals(action)) {
            // 通过前再核一次资格：门槛可能在提交后被管理员调高
            PromotionGate gate = gateOf(application.getUserId());
            if (!Boolean.TRUE.equals(gate.getEligible())) {
                throw new ServiceException("该实习生当前已不满足转正门槛，请先核对后再审批");
            }
            toStatus = PromotionApplication.STATUS_PASSED;
            // ① 状态机
            application.setStatus(toStatus);
            application.setRejectReason(null);
            application.setDeptApproveBy(operatorId);
            application.setDeptApproveTime(new Date());
            application.setGateCheckJson(toJson(gate));
            // ② 发证 + 回写证书编号（同一事务内完成，不允许「转了正但没证」）
            Map<String, Object> brief = gateMapper.selectUserBrief(application.getUserId());
            String certNo = issueCertificate(application, brief);
            application.setCertNo(certNo);
            applicationMapper.updateApplication(application);
            // ③ 培养状态 + 角色换绑
            promoteAccount(application.getUserId(), application.getDeptId());
        } else {
            throw new ServiceException("非法审批动作");
        }

        writeAudit(id, application.getUserId(), operatorDept, action, fromStatus, toStatus, reason);
        return 1;
    }

    /**
     * 培养状态与角色换绑：{@code PRE_TRAINEE → FORMAL_TRAINEE}。
     * ★ 只删 PRE_TRAINEE 角色、加 FORMAL_TRAINEE 角色，不覆盖该人其它角色。
     */
    private void promoteAccount(Long userId, Long deptId) {
        int updated = internAuthMapper.updateAuditStatus(userId, ROLE_FORMAL, "0");
        if (updated != 1) {
            throw new ServiceException("转正失败：实习生账号不存在或已被删除");
        }
        Long preRoleId = internAuthMapper.selectRoleIdByKey(ROLE_PRE);
        Long formalRoleId = internAuthMapper.selectRoleIdByKey(ROLE_FORMAL);
        if (formalRoleId == null) {
            throw new ServiceException("正式实习生角色未初始化，无法完成转正");
        }
        if (preRoleId != null) {
            SysUserRole preLink = new SysUserRole();
            preLink.setUserId(userId);
            preLink.setRoleId(preRoleId);
            userRoleMapper.deleteUserRoleInfo(preLink);
        }
        SysUserRole formalLink = new SysUserRole();
        formalLink.setUserId(userId);
        formalLink.setRoleId(formalRoleId);
        userRoleMapper.batchUserRole(Collections.singletonList(formalLink));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int revoke(Long userId, String reason) {
        if (!isSuper()) {
            throw new ServiceException("只有超级管理员可以撤回转正");
        }
        PromotionApplication latest = applicationMapper.selectLatestByUserId(userId);
        if (latest == null || !PromotionApplication.STATUS_PASSED.equals(latest.getStatus())) {
            throw new ServiceException("该实习生当前不是「已转正」状态，无需撤回");
        }
        SysUser user = userService.selectUserById(userId);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        // ① 培养状态与角色退回
        if (internAuthMapper.updateAuditStatus(userId, ROLE_PRE, "0") != 1) {
            throw new ServiceException("撤回转正失败：账号不存在");
        }
        Long preRoleId = internAuthMapper.selectRoleIdByKey(ROLE_PRE);
        Long formalRoleId = internAuthMapper.selectRoleIdByKey(ROLE_FORMAL);
        if (formalRoleId != null) {
            SysUserRole formalLink = new SysUserRole();
            formalLink.setUserId(userId);
            formalLink.setRoleId(formalRoleId);
            userRoleMapper.deleteUserRoleInfo(formalLink);
        }
        if (preRoleId != null) {
            SysUserRole preLink = new SysUserRole();
            preLink.setUserId(userId);
            preLink.setRoleId(preRoleId);
            userRoleMapper.batchUserRole(Collections.singletonList(preLink));
        }
        // ② 证书作废（不物理删除，留痕）
        certificateMapper.revokeByPromotionId(latest.getId());
        // ③ 申请单置作废
        latest.setStatus(PromotionApplication.STATUS_REVOKED);
        latest.setRejectReason(StringUtils.isEmpty(reason) ? "超级管理员撤回转正" : reason);
        applicationMapper.updateApplication(latest);

        writeAudit(latest.getId(), userId, user.getDeptId(), "REVOKE",
                PromotionApplication.STATUS_PASSED, PromotionApplication.STATUS_REVOKED,
                latest.getRejectReason());
        return 1;
    }

    @Override
    public List<PromotionApplication> applicationList(PromotionApplication query) {
        return applicationMapper.selectApplicationList(query, scopeDeptId());
    }

    // ==================================================================
    // 证书签发
    // ==================================================================

    /**
     * 签发电子证书并返回证书编号。
     * 编号规则取 {@code assessment_config.cert_no_rule}，模板取 {@code cert_template_url}。
     */
    private String issueCertificate(PromotionApplication application, Map<String, Object> brief) {
        // 幂等：同一条申请重复通过时不重复发证
        Certificate existed = certificateMapper.selectByPromotionId(application.getId());
        if (existed != null && Certificate.STATUS_GENERATED.equals(existed.getStatus())) {
            return existed.getCertNo();
        }

        AssessmentConfig config = configMapper.selectById(1L);
        String rule = (config == null || StringUtils.isEmpty(config.getCertNoRule()))
                ? DEFAULT_CERT_NO_RULE : config.getCertNoRule();
        String templateUrl = config == null ? null : config.getCertTemplateUrl();

        String certNo = renderCertNo(rule, brief);
        Certificate certificate = new Certificate();
        certificate.setCertNo(certNo);
        certificate.setUserId(application.getUserId());
        certificate.setPromotionId(application.getId());
        certificate.setTemplateUrl(templateUrl);
        certificate.setStatus(Certificate.STATUS_GENERATED);
        certificate.setGenerateTime(new Date());
        certificateMapper.insertCertificate(certificate);
        return certNo;
    }

    /** 渲染证书编号：支持 {岗位缩写}/{岗位}/{姓名}/{年}/{月}/{月日}/{年月日}/{日期}/{序号}/{综合分} */
    private String renderCertNo(String rule, Map<String, Object> brief) {
        String positionCode = emptyFallback(str(brief, "positionCode"), "GEN");
        String positionName = emptyFallback(str(brief, "positionName"), "实习生");
        String name = emptyFallback(str(brief, "nickName"), str(brief, "userName"));
        Date now = new Date();
        String rendered = rule
                .replace("{岗位缩写}", positionCode)
                .replace("{岗位名称}", positionName)
                .replace("{岗位}", positionName)
                .replace("{姓名}", name)
                .replace("{年月日}", new SimpleDateFormat("yyyyMMdd").format(now))
                .replace("{月日}", new SimpleDateFormat("MMdd").format(now))
                .replace("{日期}", new SimpleDateFormat("yyyyMMdd").format(now))
                .replace("{年}", new SimpleDateFormat("yyyy").format(now))
                .replace("{月}", new SimpleDateFormat("MM").format(now))
                // 综合分来自考核域，转正门槛不按综合分判定，故此处留空占位
                .replace("{综合分}", "");

        // {序号} 用「当日已发证数 + 1」；同编号冲突则 +1 重试，最终兜底追加随机串
        for (int seq = 1; seq <= 200; seq++) {
            String candidate = rendered.replace("{序号}", String.format("%04d", seq));
            if (certificateMapper.countByCertNo(candidate) == 0) {
                return candidate;
            }
        }
        String fallback = rendered.replace("{序号}", UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        if (certificateMapper.countByCertNo(fallback) == 0) {
            return fallback;
        }
        return fallback + "-" + ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    private String str(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String emptyFallback(String value, String fallback) {
        return StringUtils.isEmpty(value) || "null".equals(value) ? fallback : value;
    }

    // ==================================================================
    // 公共辅助
    // ==================================================================

    private PromotionApplication requireVisible(Long id) {
        PromotionApplication application = applicationMapper.selectApplicationById(id);
        if (application == null) {
            throw new ServiceException("转正申请不存在");
        }
        Long scope = scopeDeptId();
        if (scope != null && application.getDeptId() != null && !scope.equals(application.getDeptId())) {
            throw new ServiceException("无权查看其他部门的转正申请");
        }
        return application;
    }

    private void writeAudit(Long bizId, Long userId, Long deptId, String action,
                           String fromStatus, String toStatus, String reason) {
        AuditRecord record = new AuditRecord();
        record.setBizType(BIZ_PROMOTION);
        record.setBizId(bizId);
        record.setUserId(userId);
        record.setDeptId(deptId);
        record.setAction(action);
        record.setFromStatus(fromStatus);
        record.setToStatus(toStatus);
        record.setReason(StringUtils.isEmpty(reason) ? null : reason);
        record.setOperatorId(SecurityUtils.getUserId());
        record.setCreateTime(new Date());
        auditRecordMapper.insertAuditRecord(record);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("转正资格快照序列化失败，降级为空对象：{}", e.getMessage());
            return "{}";
        }
    }

    /** 数据范围：超管 null（全部），其余角色取本部门（缺失则报错，避免越权读全量）。 */
    private Long scopeDeptId() {
        if (isSuper()) {
            return null;
        }
        return requireOwnDept();
    }

    private Long requireOwnDept() {
        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null) {
            throw new ServiceException("当前账号未配置部门，无法访问转正数据");
        }
        return deptId;
    }

    private boolean isSuper() {
        Long uid = SecurityUtils.getUserId();
        return SecurityUtils.hasRole("SUPER_ADMIN") || SecurityUtils.isAdmin(uid);
    }

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

    private int toInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
