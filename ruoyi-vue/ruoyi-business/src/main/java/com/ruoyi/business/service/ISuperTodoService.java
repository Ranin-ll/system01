package com.ruoyi.business.service;

import com.ruoyi.business.domain.SuperDeptAdminStat;

import java.util.List;
import java.util.Map;

/**
 * 超管督办视图 Service
 *
 * <p><b>能力边界</b>：督办是超管被允许「写」的三域之一（账号 / 通知 / 督办），
 * 所以这里<b>不调用</b> {@code isGlobalReadOnly()}；但每个方法都强制校验超管身份 ——
 * 因为接口复用了部门管理员也持有的权限串，越权拦截必须落在 Service。</p>
 *
 * @author ruoyi
 */
public interface ISuperTodoService {

    /** 部门管理员清单（含各自四类待办量） */
    List<SuperDeptAdminStat> deptAdmins();

    /**
     * 督办看板：四类待办的汇总 + 明细
     *
     * @param todoType 可选，按类型过滤（REGISTER / REVIEW / OVERDUE_TASK / STALLED_STUDY）
     * @param deptId   可选，按部门过滤
     * @param ownerId  可选，按责任人过滤
     * @return {@code {summary: [{type,label,count}], rows: [...], total, stalledIdleDays}}
     */
    Map<String, Object> board(String todoType, Long deptId, Long ownerId);

    /**
     * 一键催办：给选中的部门管理员发定向通知（正文自动带上其名下待办）
     *
     * <p>频控：同一天已给该人发过催办则跳过（沿用定时任务的去重思路）。</p>
     *
     * @param ownerIds 被催的部门管理员 userId 列表
     * @param content  可选，附加说明（会拼在自动生成的清单之后）
     * @return {@code {sent, skipped, details: [...]}}
     */
    Map<String, Object> urge(List<Long> ownerIds, String content);
}
