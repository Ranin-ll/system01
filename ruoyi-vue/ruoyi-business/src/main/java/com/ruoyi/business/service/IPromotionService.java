package com.ruoyi.business.service;

import com.ruoyi.business.domain.AuditRecord;
import com.ruoyi.business.domain.PromotionApplication;
import com.ruoyi.business.domain.PromotionGate;
import com.ruoyi.business.domain.PromotionRule;

import java.util.List;
import java.util.Map;

/**
 * 实习转正服务。
 *
 * <p><b>完整链路</b>：</p>
 * <ol>
 *   <li>超管（全局默认）或部门管理员（本部门覆盖）设置转正要求 → {@code promotion_rule}</li>
 *   <li>实习生按所属部门读生效规则，核对资格清单，提交转正申请 → {@code promotion_application:DEPT_PENDING}</li>
 *   <li>部门管理员审批：通过 → 置 {@code sys_user.user_status = FORMAL_TRAINEE} + 角色换绑
 *       （PRE_TRAINEE → FORMAL_TRAINEE）+ 自动签发电子证书 + 证书编号回写申请单</li>
 *   <li>驳回 → {@code REJECTED}，实习生可修改后重新提交（同一条申请单循环）</li>
 *   <li>纠错：超管可撤回转正（退回 PRE_TRAINEE + 证书作废），留痕在 {@code audit_record}</li>
 * </ol>
 *
 * <p><b>状态机</b>：{@code DEPT_PENDING → PASSED / REJECTED}；部门终审即生效，跳过 {@code SUPER_PENDING}。</p>
 *
 * @author ruoyi
 */
public interface IPromotionService {

    // ==================================================================
    // ① 转正要求（规则）
    // ==================================================================

    /**
     * 取<b>生效规则</b>：部门覆盖 → 全局默认 → 内置兜底
     * （{@code studyRateMin = 0, examPassTimes = 1}）。永不返回 null。
     *
     * @param deptId 目标部门；null = 只要全局默认（不做部门覆盖查找）
     */
    PromotionRule effectiveRule(Long deptId);

    /**
     * 请求方的生效规则 —— {@code deptId} 缺省时的语义消歧：
     * <ul>
     *   <li>超管：不做部门覆盖查找，返回全局默认（超管管理的就是全局值）</li>
     *   <li>部门管理员：自动落到<b>本部门</b>，返回本部门生效规则</li>
     * </ul>
     * 若直接用 {@link #effectiveRule(Long)} 并把 null 透传下去，部门管理员会读到全局默认值 ——
     * 那是「管理值」而不是「他自己的生效值」，会与实习生端看到的规则不一致。
     */
    PromotionRule effectiveRuleForRequest(Long deptId);

    /**
     * 规则面板：全局默认 + 各部门覆盖 + 生效来源说明，供超管「系统规则」页
     * 与部门端设置行渲染。
     */
    Map<String, Object> ruleBoard();

    /**
     * 保存规则（upsert）。
     *
     * <p>{@code deptId == null} → 写默认要求（先查后插/改，保证只有一行）。
     * 非 null → 该部门单独设置。<b>部门管理员只能写本部门</b>（Service 二次校验，不信前端）。</p>
     */
    int saveRule(PromotionRule rule);

    // ==================================================================
    // ② 资格核对
    // ==================================================================

    /** 实时计算某人的转正资格清单（含门槛与逐项判定）。 */
    PromotionGate gateOf(Long userId);

    // ==================================================================
    // ③ 实习生端
    // ==================================================================

    /**
     * 我的转正申请视图：生效规则 + 资格清单 + 最新申请 + 证书。
     * 返回 {@code {rule, gate, canApply, application, certificate}}。
     */
    Map<String, Object> myApplication();

    /**
     * 提交转正申请（服务端重新校验资格，不信任前端）。
     *
     * <p>已驳回时沿用例行「修改后重新提交」（更新同一条记录，不新增）；已通过 / 待审核则拒绝。</p>
     *
     * @param supplement 转正说明 / 阶段自评
     * @return 申请单 ID
     */
    Long apply(String supplement);

    /** 撤回待审核的申请（回到可编辑状态）。 */
    int withdraw();

    /** 我的证书（当前生效的一张）。无则返回 null 值域的对象。 */
    Map<String, Object> myCertificate();

    // ==================================================================
    // ④ 部门管理员 / 超管端
    // ==================================================================

    /**
     * 候选人看板：本部门（超管为全部）在培实习生 + 最新申请 + 实时资格。
     *
     * @param status  ALL / PENDING / PASSED / REJECTED / ELIGIBLE
     * @param keyword 姓名 / 账号模糊匹配
     * @return {@code {rows, summary}}
     */
    Map<String, Object> candidates(String status, String keyword);

    /** 申请详情（含审核历史）。 */
    Map<String, Object> applicationDetail(Long id);

    /** 审核历史（audit_record，bizType = PROMOTION）。 */
    List<AuditRecord> history(Long id);

    /**
     * 审批：通过即生效（状态 + 角色 + 证书三件事在同一事务内完成），驳回必须填原因。
     *
     * @param action PASS / REJECT
     */
    int audit(Long id, String action, String reason);

    /** 超管纠错：撤回转正（退回 PRE_TRAINEE、角色换回、证书作废）。 */
    int revoke(Long userId, String reason);

    /** 申请列表（导出 / 统计用）。 */
    List<PromotionApplication> applicationList(PromotionApplication query);
}
