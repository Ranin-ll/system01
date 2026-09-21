package com.ruoyi.business.service.impl;

import com.ruoyi.business.domain.Notice;
import com.ruoyi.business.domain.SuperDeptAdminStat;
import com.ruoyi.business.domain.SuperTodoItem;
import com.ruoyi.business.mapper.SuperTodoMapper;
import com.ruoyi.business.service.IMessageService;
import com.ruoyi.business.service.ISuperTodoService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 超管督办视图实现
 *
 * <p><b>设计要点</b></p>
 * <ol>
 *   <li>四源待办的<b>判定只在 Mapper XML 里写一次</b>，本类只负责「归责任人 + 汇总 + 过滤」，
 *       避免把同一套口径复制四遍（本项目在「通知受众」上吃过这个亏）；</li>
 *   <li>汇总( summary )与列表( rows )<b>同源</b> —— 都基于同一份 scoped 列表，
 *       所以「KPI 数」与「列表条数」不会打架；</li>
 *   <li>责任人统一按「该部门的部门管理员」归属，与「部门管理员清单」页口径一致；</li>
 *   <li>催办<b>复用已通的 {@link IMessageService#send}</b>（定向通知），不新建表、不新建通道。</li>
 * </ol>
 *
 * @author ruoyi
 */
@Service
public class SuperTodoServiceImpl implements ISuperTodoService {

    /**
     * 学习停滞阈值（天）。
     *
     * <p>库表 {@code assessment_config} 没有对应字段（现有预警字段是 alert_bank_gap_factor /
     * alert_exam_draft_days / alert_register_pending / alert_dept_done_floor / alert_overdue_hours），
     * 所以先用常量。若将来要可配，加一列并在「规则与配置」页暴露即可。</p>
     */
    private static final int STALLED_IDLE_DAYS = 7;

    /** 催办通知的 bizType（频控按「当天 + 收件人」去重） */
    private static final String URGE_BIZ_TYPE = "URGE";

    /** 催办通知的 msgType —— MessageServiceImpl 的 ALLOWED_TYPES 里已含 "URGE"（不是 NOTIFY） */
    private static final String URGE_MSG_TYPE = "URGE";

    @Autowired
    private SuperTodoMapper superTodoMapper;

    @Autowired
    private IMessageService messageService;

    /**
     * 督办视图仅超管可见。
     *
     * <p>接口复用的是已有权限串（部门管理员也持有），所以<b>越权拦截必须落在 Service</b>，
     * 不能只靠前端隐藏菜单。</p>
     */
    private void requireSuperAdmin() {
        Long uid = SecurityUtils.getUserId();
        if (!(SecurityUtils.hasRole("SUPER_ADMIN") || SecurityUtils.isAdmin(uid))) {
            throw new ServiceException("仅超级管理员可访问督办视图");
        }
    }

    @Override
    public List<SuperDeptAdminStat> deptAdmins() {
        requireSuperAdmin();
        List<SuperDeptAdminStat> admins = superTodoMapper.selectDeptAdmins();
        List<SuperTodoItem> all = collectTodos(null);
        for (SuperDeptAdminStat a : admins) {
            int reg = 0, rev = 0, ovd = 0, stl = 0;
            for (SuperTodoItem it : all) {
                if (!Objects.equals(it.getOwnerId(), a.getUserId())) {
                    continue;
                }
                String t = it.getTodoType();
                if (SuperTodoItem.TYPE_REGISTER.equals(t)) {
                    reg++;
                } else if (SuperTodoItem.TYPE_REVIEW.equals(t)) {
                    rev++;
                } else if (SuperTodoItem.TYPE_OVERDUE_TASK.equals(t)) {
                    ovd++;
                } else if (SuperTodoItem.TYPE_STALLED_STUDY.equals(t)) {
                    stl++;
                }
            }
            a.setPendingRegisters(reg);
            a.setPendingReviews(rev);
            a.setOverdueTasks(ovd);
            a.setStalledStudies(stl);
            a.setTotalTodos(reg + rev + ovd + stl);
        }
        return admins;
    }

    @Override
    public Map<String, Object> board(String todoType, Long deptId, Long ownerId) {
        requireSuperAdmin();
        String type = (todoType == null || todoType.trim().isEmpty()) ? null : todoType.trim().toUpperCase();

        // 先按「部门 / 责任人」收范围（不含类型筛选）—— 汇总与列表都基于它，保证口径一致
        List<SuperTodoItem> scoped = new ArrayList<>();
        for (SuperTodoItem it : collectTodos(null)) {
            if (deptId != null && !deptId.equals(it.getDeptId())) {
                continue;
            }
            if (ownerId != null && !ownerId.equals(it.getOwnerId())) {
                continue;
            }
            scoped.add(it);
        }

        List<Map<String, Object>> summary = new ArrayList<>();
        String[] types = {SuperTodoItem.TYPE_REGISTER, SuperTodoItem.TYPE_REVIEW,
                SuperTodoItem.TYPE_OVERDUE_TASK, SuperTodoItem.TYPE_STALLED_STUDY};
        for (String t : types) {
            int c = 0;
            for (SuperTodoItem it : scoped) {
                if (t.equals(it.getTodoType())) {
                    c++;
                }
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", t);
            m.put("label", SuperTodoItem.label(t));
            m.put("count", c);
            summary.add(m);
        }

        List<SuperTodoItem> rows = new ArrayList<>();
        for (SuperTodoItem it : scoped) {
            if (type != null && !type.equals(it.getTodoType())) {
                continue;
            }
            rows.add(it);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("summary", summary);
        res.put("rows", rows);
        res.put("total", rows.size());
        res.put("stalledIdleDays", STALLED_IDLE_DAYS);
        return res;
    }

    @Override
    public Map<String, Object> urge(List<Long> ownerIds, String content) {
        requireSuperAdmin();
        if (ownerIds == null || ownerIds.isEmpty()) {
            throw new ServiceException("请选择要催办的部门管理员");
        }

        Map<Long, SuperDeptAdminStat> byUser = new LinkedHashMap<>();
        for (SuperDeptAdminStat a : superTodoMapper.selectDeptAdmins()) {
            byUser.put(a.getUserId(), a);
        }

        List<SuperTodoItem> all = collectTodos(null);
        List<String> details = new ArrayList<>();
        int sent = 0;
        int skipped = 0;

        // LinkedHashSet：前端可能重复提交同一个 id，这里去重
        for (Long ownerId : new LinkedHashSet<>(ownerIds)) {
            if (ownerId == null) {
                continue;
            }
            SuperDeptAdminStat admin = byUser.get(ownerId);
            if (admin == null) {
                skipped++;
                details.add("userId=" + ownerId + " 不是部门管理员，已跳过");
                continue;
            }

            List<SuperTodoItem> mine = new ArrayList<>();
            for (SuperTodoItem it : all) {
                if (Objects.equals(it.getOwnerId(), ownerId)) {
                    mine.add(it);
                }
            }
            if (mine.isEmpty()) {
                skipped++;
                details.add(adminLabel(admin) + " 当前无待办，已跳过");
                continue;
            }
            if (superTodoMapper.countUrgeToday(ownerId) > 0) {
                skipped++;
                details.add(adminLabel(admin) + " 今天已被催办过，已跳过（同一人每天最多一次）");
                continue;
            }

            Notice n = new Notice();
            n.setMsgType(URGE_MSG_TYPE);
            n.setScopeType(Notice.SCOPE_USER);
            n.setTargetUserIds(Collections.singletonList(ownerId));
            n.setTitle("【督办】" + adminLabel(admin) + " 有 " + mine.size() + " 项待办待处理");
            n.setContent(buildUrgeContent(mine, content));
            n.setBizType(URGE_BIZ_TYPE);
            n.setBizId(0L);
            messageService.send(n);

            sent++;
            details.add(adminLabel(admin) + " 已催办（" + mine.size() + " 项）");
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("sent", sent);
        res.put("skipped", skipped);
        res.put("details", details);
        return res;
    }

    // ------------------------------------------------------------------ 内部

    /**
     * 四源合并 + 归责任人。
     *
     * @param todoType 为 null 表示取全部四类；否则只取指定类（减少无用查询）
     */
    private List<SuperTodoItem> collectTodos(String todoType) {
        List<SuperTodoItem> rows = new ArrayList<>();
        if (todoType == null || SuperTodoItem.TYPE_REGISTER.equals(todoType)) {
            rows.addAll(superTodoMapper.selectPendingRegisters());
        }
        if (todoType == null || SuperTodoItem.TYPE_REVIEW.equals(todoType)) {
            rows.addAll(superTodoMapper.selectPendingReviews());
        }
        if (todoType == null || SuperTodoItem.TYPE_OVERDUE_TASK.equals(todoType)) {
            rows.addAll(superTodoMapper.selectOverdueTasks());
        }
        if (todoType == null || SuperTodoItem.TYPE_STALLED_STUDY.equals(todoType)) {
            rows.addAll(superTodoMapper.selectStalledStudies(STALLED_IDLE_DAYS));
        }

        Map<Long, SuperDeptAdminStat> adminByDept = deptAdminMap();
        for (SuperTodoItem it : rows) {
            it.setTodoLabel(SuperTodoItem.label(it.getTodoType()));
            attachOwner(it, adminByDept);
        }
        return rows;
    }

    /** 部门 → 部门管理员。一个部门一个管理员（dept_position 一一绑定）。 */
    private Map<Long, SuperDeptAdminStat> deptAdminMap() {
        Map<Long, SuperDeptAdminStat> map = new LinkedHashMap<>();
        for (SuperDeptAdminStat a : superTodoMapper.selectDeptAdmins()) {
            if (a.getDeptId() != null) {
                map.put(a.getDeptId(), a);
            }
        }
        return map;
    }

    /**
     * 责任人 = 该部门的部门管理员。
     *
     * <p>为什么不直接取 {@code learning_task.reviewer_id}：现有数据里 reviewer 就是发布任务的
     * 部门管理员，且该字段可能为空；统一按「部门管理员」归属，口径与「部门管理员清单」页一致。
     * <b>⚠️ 若将来支持跨部门指定批阅人，这里要改成优先取 reviewer_id。</b></p>
     */
    private void attachOwner(SuperTodoItem it, Map<Long, SuperDeptAdminStat> adminByDept) {
        if (it.getDeptId() == null) {
            return;
        }
        SuperDeptAdminStat admin = adminByDept.get(it.getDeptId());
        if (admin == null) {
            return;
        }
        it.setOwnerId(admin.getUserId());
        it.setOwnerName(admin.getNickName() != null && !admin.getNickName().isEmpty()
                ? admin.getNickName() : admin.getUserName());
    }

    private String adminLabel(SuperDeptAdminStat a) {
        String dept = a.getDeptName() == null ? "" : a.getDeptName();
        String who = a.getNickName() != null && !a.getNickName().isEmpty() ? a.getNickName() : a.getUserName();
        return dept + " · " + who;
    }

    /** 正文自动带上待办清单 —— 被催的人点开就知道要做什么，不用回系统里找 */
    private String buildUrgeContent(List<SuperTodoItem> mine, String extra) {
        StringBuilder sb = new StringBuilder();
        sb.append("你名下当前有 ").append(mine.size()).append(" 项待办，请尽快处理：\n");
        int i = 1;
        for (SuperTodoItem it : mine) {
            sb.append(i++).append(". [").append(it.getTodoLabel()).append("] ").append(it.getTitle());
            if (it.getDetail() != null && !it.getDetail().isEmpty()) {
                sb.append("（").append(it.getDetail()).append("）");
            }
            sb.append("\n");
        }
        if (extra != null && !extra.trim().isEmpty()) {
            sb.append("\n").append(extra.trim());
        }
        sb.append("\n—— 本消息由超级管理员通过「督办看板」发出");
        return sb.toString();
    }
}
