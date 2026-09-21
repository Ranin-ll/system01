package com.ruoyi.business.service.impl;

import com.ruoyi.business.domain.AssessmentConfig;
import com.ruoyi.business.domain.DeptMatrixRow;
import com.ruoyi.business.domain.DeptStatsVO;
import com.ruoyi.business.domain.InternStageRow;
import com.ruoyi.business.mapper.SuperAnalysisMapper;
import com.ruoyi.business.service.IAssessmentRuleService;
import com.ruoyi.business.service.ISuperAnalysisService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 超管「培养分析看板」实现。
 *
 * <p><b>数据边界（本期刻意划的线）</b>：只聚合 <b>人 / 学习 / 任务</b> 三族真数据。
 * <b>模拟考核、正式考核、知识点一律不查库</b> —— 题库与考核模块将有同事的大改动、
 * 等其分支上传后合并，在会变的表上取数等于白做。这几列在 VO 里保留为 {@code null}，
 * 由前端 {@code _mock.js} 填充并统一打「示例数据」橙标；合并后只需在
 * {@code SuperAnalysisMapper.xml} 里补查询，**前端模板零改动**。</p>
 *
 * <p><b>权限</b>：接口注解复用既有的 {@code business:bank:list}（部门管理员也持有），
 * 因此范围收窄落在本类 —— 超管看全量，其它角色强制只看本部门。
 * 这样即使部门管理员手动敲 URL 进来，也不会拿到跨部门数据。</p>
 *
 * @author ruoyi
 */
@Service
public class SuperAnalysisServiceImpl implements ISuperAnalysisService {

    /** 配置缺失时的兜底门槛 */
    private static final BigDecimal DEFAULT_THRESHOLD = new BigDecimal("70");

    @Autowired
    private SuperAnalysisMapper analysisMapper;

    /** ★ 复用既有配置服务（它已处理「表在但行被删」的兜底补行），不重复读配置表 */
    @Autowired
    private IAssessmentRuleService assessmentRuleService;

    // ==================================================================
    // 作用域与阈值
    // ==================================================================

    /**
     * 数据范围：超管 → null（全量）；其它角色 → 本部门。
     *
     * <p>注意**不用** {@code currentScopeDeptId()} 那套（它按登录人部门收窄，超管也会被收窄），
     * 超管必须是显式的「无过滤」。</p>
     */
    private Long scopeDeptId() {
        Long uid = SecurityUtils.getUserId();
        if (SecurityUtils.hasRole("SUPER_ADMIN") || SecurityUtils.isAdmin(uid)) {
            return null;
        }
        return SecurityUtils.getDeptId();
    }

    /** 学习达标门槛（%），读 assessment_config —— 不硬编码 */
    private BigDecimal threshold() {
        AssessmentConfig cfg = assessmentRuleService.getConfig();
        BigDecimal t = (cfg == null) ? null : cfg.getCourseDoneThreshold();
        return (t == null) ? DEFAULT_THRESHOLD : t;
    }

    private BigDecimal alertDeptDoneFloor() {
        AssessmentConfig cfg = assessmentRuleService.getConfig();
        return (cfg == null) ? null : cfg.getAlertDeptDoneFloor();
    }

    // ==================================================================
    // 部门矩阵（overview 与 deptMatrix 共用，保证两处数字必然一致）
    // ==================================================================

    private List<DeptMatrixRow> buildMatrix() {
        Long scope = scopeDeptId();
        BigDecimal th = threshold();

        Map<Long, DeptMatrixRow> byDept = new LinkedHashMap<>();

        // ① 人（基准行：只有这里的部门才是「有实习生的部门」）
        for (DeptMatrixRow r : analysisMapper.selectInternByDept(scope)) {
            byDept.put(r.getDeptId(), r);
        }
        // ② 学习（补进已有行；理论上不会再出现新部门，兜底建行以防 SQL 条件将来放宽）
        for (DeptMatrixRow r : analysisMapper.selectLearnByDept(th, scope)) {
            DeptMatrixRow row = ensure(byDept, r);
            row.setLearnTotal(r.getLearnTotal());
            row.setLearnDone(r.getLearnDone());
            row.setLearnPersons(r.getLearnPersons());
            row.setLearnAvgProgress(r.getLearnAvgProgress());
            row.setVideoSeconds(r.getVideoSeconds());
        }
        // ③ 任务
        for (DeptMatrixRow r : analysisMapper.selectTaskByDept(scope)) {
            DeptMatrixRow row = ensure(byDept, r);
            row.setTaskTotal(r.getTaskTotal());
            row.setTaskDone(r.getTaskDone());
            row.setTaskOverdue(r.getTaskOverdue());
            row.setTaskNotStarted(r.getTaskNotStarted());
        }
        // ④ 模拟 / 正式 / 知识点：🟠 本期不查（见类注释），字段保持 null

        return new ArrayList<>(byDept.values());
    }

    private DeptMatrixRow ensure(Map<Long, DeptMatrixRow> byDept, DeptMatrixRow src) {
        DeptMatrixRow row = byDept.get(src.getDeptId());
        if (row == null) {
            row = new DeptMatrixRow();
            row.setDeptId(src.getDeptId());
            row.setDeptName(src.getDeptName());
            byDept.put(src.getDeptId(), row);
        }
        return row;
    }

    @Override
    public List<DeptMatrixRow> deptMatrix() {
        return buildMatrix();
    }

    // ==================================================================
    // 培养状态进度
    // ==================================================================

    /** 阶段中文名（后端给，避免前端再维护一份映射） */
    private static final Map<String, String> STAGE_LABELS = new LinkedHashMap<>();

    static {
        STAGE_LABELS.put("WAIT_AUDIT", "待审核");
        STAGE_LABELS.put("PRE_TRAINEE", "预备实习生");
        STAGE_LABELS.put("PENDING_PROMOTE", "待转正");
        STAGE_LABELS.put("FORMAL_TRAINEE", "正式实习生");
    }

    private List<InternStageRow> labelledStageRows(BigDecimal th) {
        return label(analysisMapper.selectInternStageRows(th, scopeDeptId()));
    }

    /** 给逐人明细补上阶段中文名（列表页与详情页共用同一份映射） */
    private List<InternStageRow> label(List<InternStageRow> rows) {
        for (InternStageRow r : rows) {
            String label = STAGE_LABELS.get(r.getStage());
            r.setStageLabel(label == null ? r.getStage() : label);
            // 🟠 正式考试通过与否属考核模块，本期不查 → 保持 null（前端示例填充）
            r.setFormalPassed(null);
        }
        return rows;
    }

    @Override
    public Map<String, Object> stageProgress() {
        BigDecimal th = threshold();
        List<InternStageRow> rows = labelledStageRows(th);

        // 漏斗：按阶段汇总。阶段顺序固定，空档位也保留（漏斗不能因没人而缺一环）
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String key : STAGE_LABELS.keySet()) {
            counts.put(key, 0);
        }
        for (InternStageRow r : rows) {
            String s = r.getStage();
            counts.put(s, counts.getOrDefault(s, 0) + 1);
        }
        List<Map<String, Object>> stages = new ArrayList<>();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("stage", e.getKey());
            m.put("label", STAGE_LABELS.get(e.getKey()));
            m.put("cnt", e.getValue());
            stages.add(m);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("threshold", th);
        result.put("stages", stages);
        result.put("rows", rows);
        result.put("scopedDeptId", scopeDeptId());
        return result;
    }

    // ==================================================================
    // L0 全局 KPI + 预警
    // ==================================================================

    @Override
    public Map<String, Object> overview() {
        BigDecimal th = threshold();
        List<DeptMatrixRow> matrix = buildMatrix();
        List<InternStageRow> rows = labelledStageRows(th);

        int internTotal = 0;
        int formalTotal = 0;
        int learnTotal = 0;
        int learnDone = 0;
        int learnPersons = 0;
        long videoSeconds = 0L;
        int taskTotal = 0;
        int taskDone = 0;
        int taskOverdue = 0;
        // 全局平均进度 = 各部门均值按「该部门记录数」加权 —— 与小样本部门被拉平无关，
        // 数学上等于对全部记录求均值（避免「部门均值的算术平均」这种经典错误）
        BigDecimal progressWeighted = BigDecimal.ZERO;
        int progressWeight = 0;

        for (DeptMatrixRow r : matrix) {
            internTotal += nz(r.getInternCount());
            formalTotal += nz(r.getFormalCount());
            learnTotal += nz(r.getLearnTotal());
            learnDone += nz(r.getLearnDone());
            learnPersons += nz(r.getLearnPersons());
            videoSeconds += (r.getVideoSeconds() == null ? 0L : r.getVideoSeconds());
            taskTotal += nz(r.getTaskTotal());
            taskDone += nz(r.getTaskDone());
            taskOverdue += nz(r.getTaskOverdue());
            if (r.getLearnAvgProgress() != null && nz(r.getLearnTotal()) > 0) {
                progressWeighted = progressWeighted.add(
                        r.getLearnAvgProgress().multiply(BigDecimal.valueOf(nz(r.getLearnTotal()))));
                progressWeight += nz(r.getLearnTotal());
            }
        }

        int mentorMissing = 0;
        int protocolUnsigned = 0;
        int statusConflict = 0;
        for (InternStageRow r : rows) {
            if (r.getMentorName() == null || r.getMentorName().trim().isEmpty()) {
                mentorMissing++;
            }
            if (nz(r.getProtocolSigned()) == 0) {
                protocolUnsigned++;
            }
            if (nz(r.getStatusConflict()) == 1) {
                statusConflict++;
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("internTotal", internTotal);
        data.put("formalTotal", formalTotal);
        // 0 人部门不进矩阵（根 dept_id=100 下挂着 0 人的测试部门，会多出一行全是「—」的噪声）
        data.put("deptCount", matrix.size());
        data.put("learnThreshold", th);

        data.put("learnTotal", learnTotal);
        data.put("learnDone", learnDone);
        data.put("learnPersons", learnPersons);
        // 分母 0 → null（前端显示「暂无」，不伪装 0%）
        data.put("learnRate", rate(learnDone, learnTotal));
        data.put("learnAvgProgress", progressWeight > 0
                ? progressWeighted.divide(BigDecimal.valueOf(progressWeight), 1, RoundingMode.HALF_UP)
                : null);
        data.put("videoSeconds", videoSeconds);

        data.put("taskTotal", taskTotal);
        data.put("taskDone", taskDone);
        data.put("taskOverdue", taskOverdue);
        data.put("taskDoneRate", rate(taskDone, taskTotal));

        data.put("mentorMissing", mentorMissing);
        data.put("protocolUnsigned", protocolUnsigned);
        data.put("statusConflict", statusConflict);

        data.put("deptDist", matrix);
        data.put("alerts", buildAlerts(matrix, taskOverdue, mentorMissing, protocolUnsigned, statusConflict));
        data.put("scopedDeptId", scopeDeptId());
        return data;
    }

    /** 比率（%）；分母 0 → null，**不伪装 0%** */
    private Integer rate(int numerator, int denominator) {
        if (denominator <= 0) {
            return null;
        }
        return (int) Math.round(numerator * 100.0 / denominator);
    }

    private int nz(Integer v) {
        return v == null ? 0 : v;
    }

    /**
     * 异常预警：阈值全部取自 assessment_config，改配置即改判定。
     * 每条都能点进对应的下钻视图。
     */
    private List<Map<String, Object>> buildAlerts(List<DeptMatrixRow> matrix, int taskOverdue,
                                                  int mentorMissing, int protocolUnsigned,
                                                  int statusConflict) {
        List<Map<String, Object>> alerts = new ArrayList<>();
        BigDecimal floor = alertDeptDoneFloor();

        // ① 部门学习进度低于配置下限（阈值 alert_dept_done_floor）
        if (floor != null) {
            for (DeptMatrixRow r : matrix) {
                if (r.getLearnAvgProgress() != null
                        && r.getLearnAvgProgress().compareTo(floor) < 0) {
                    alerts.add(alert("HIGH", "DEPT_DONE_FLOOR",
                            r.getDeptName() + " 学习进度均值 " + r.getLearnAvgProgress() + "% 低于下限 " + floor + "%",
                            "阈值 alert_dept_done_floor = " + floor
                                    + " · 该部门有学习记录 " + nz(r.getLearnPersons()) + " 人"
                                    + " · 建议通知该部门管理员",
                            "/super/ops/analysis/dept/" + r.getDeptId()));
                }
            }
        }
        // ② 任务逾期
        if (taskOverdue > 0) {
            alerts.add(alert("HIGH", "TASK_OVERDUE",
                    "任务逾期 " + taskOverdue + " 条",
                    "逾期率 " + (rate(taskOverdue, countTaskTotal(matrix)) == null ? "--"
                            : rate(taskOverdue, countTaskTotal(matrix)) + "%")
                            + " · 判定参考 alert_overdue_hours",
                    "/super/ops/analysis?focus=task"));
        }
        // ③ 角色与状态不一致（数据治理问题，不是业务问题）
        if (statusConflict > 0) {
            alerts.add(alert("MEDIUM", "STATUS_CONFLICT",
                    statusConflict + " 人「状态待修正」：已持实习生角色，但 user_status 仍是 WAIT_AUDIT",
                    "口径以角色为主、user_status 为辅；这批人会被同时算进「待审」与「预备」→ 需修正建账流程",
                    "/super/ops/analysis?focus=stage"));
        }
        // ④ 未分配导师
        if (mentorMissing > 0) {
            alerts.add(alert("MEDIUM", "MENTOR_MISSING",
                    mentorMissing + " 人未分配导师",
                    "mentor_name 是文本字段（非关联 ID），已分配项亦存在脏值（如「001」）→ 数据质量待治",
                    "/super/ops/analysis?focus=stage"));
        }
        // ⑤ 未签协议
        if (protocolUnsigned > 0) {
            alerts.add(alert("MEDIUM", "PROTOCOL_UNSIGNED",
                    protocolUnsigned + " 人未签保密协议",
                    "协议为转正硬条件（protocol_required）",
                    "/super/ops/analysis?focus=stage"));
        }
        return alerts;
    }

    private int countTaskTotal(List<DeptMatrixRow> matrix) {
        int t = 0;
        for (DeptMatrixRow r : matrix) {
            t += nz(r.getTaskTotal());
        }
        return t;
    }

    private Map<String, Object> alert(String level, String key, String title, String detail, String link) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("level", level);
        m.put("key", key);
        m.put("title", title);
        m.put("detail", detail);
        m.put("link", link);
        return m;
    }

    // ==================================================================
    // L1 · 部门详情
    // ==================================================================

    @Override
    public DeptStatsVO deptStats(Long deptId) {
        if (deptId == null) {
            throw new ServiceException("缺少部门参数");
        }
        // ★ 归属校验：非超管只能看本部门（用 URL 改 deptId 遍历别部门数据会被拒）
        Long scope = scopeDeptId();
        if (scope != null && !scope.equals(deptId)) {
            throw new ServiceException("无权查看其它部门的培养数据");
        }

        BigDecimal th = threshold();
        DeptStatsVO vo = analysisMapper.selectDeptHeader(deptId);
        if (vo == null) {
            throw new ServiceException("部门不存在");
        }

        // ★★ 本部门那一行 + 全局对照，**都取自 L0 的同一份聚合**（调 overview()）
        //    —— 不重算一遍，否则本页的「本部门数」与「全局均值」会和看板漂移。
        Map<String, Object> ov = overview();
        DeptMatrixRow row = findRow(ov, deptId);

        vo.setLearnThreshold(th);
        vo.setGlobalLearnAvgProgress((BigDecimal) ov.get("learnAvgProgress"));
        vo.setGlobalTaskDoneRate((Integer) ov.get("taskDoneRate"));
        vo.setGlobalInternCount((Integer) ov.get("internTotal"));
        if (row != null) {
            vo.setInternCount(row.getInternCount());
            vo.setFormalCount(row.getFormalCount());
            vo.setLearnTotal(row.getLearnTotal());
            vo.setLearnDone(row.getLearnDone());
            vo.setLearnPersons(row.getLearnPersons());
            vo.setLearnAvgProgress(row.getLearnAvgProgress());
            vo.setTaskTotal(row.getTaskTotal());
            vo.setTaskDone(row.getTaskDone());
            vo.setTaskOverdue(row.getTaskOverdue());
            vo.setLearnRate(rate(nz(row.getLearnDone()), nz(row.getLearnTotal())));
            vo.setTaskDoneRate(rate(nz(row.getTaskDone()), nz(row.getTaskTotal())));
        }

        vo.setPositions(analysisMapper.selectPositionDist(deptId));
        vo.setCourses(analysisMapper.selectCourseProgress(deptId, th));

        // 实习生明细：**复用逐人明细查询**（带 deptId 过滤），不另写一份口径
        List<InternStageRow> interns = label(analysisMapper.selectInternStageRows(th, deptId));
        vo.setInterns(interns);
        vo.setBlockers(blockersOf(interns));
        return vo;
    }

    /** 从 overview 的结果里取某部门那一行（避免 unchecked cast） */
    private DeptMatrixRow findRow(Map<String, Object> overview, Long deptId) {
        Object dist = overview.get("deptDist");
        if (!(dist instanceof List)) {
            return null;
        }
        for (Object o : (List<?>) dist) {
            if (o instanceof DeptMatrixRow) {
                DeptMatrixRow r = (DeptMatrixRow) o;
                if (deptId.equals(r.getDeptId())) {
                    return r;
                }
            }
        }
        return null;
    }

    /** 卡点计数（本部门 / 全公司共用同一份逐人明细算出） */
    private Map<String, Object> blockersOf(List<InternStageRow> rows) {
        int statusConflict = 0;
        int mentorMissing = 0;
        int protocolUnsigned = 0;
        int noLearning = 0;
        int overdueTask = 0;
        for (InternStageRow r : rows) {
            if (nz(r.getStatusConflict()) == 1) {
                statusConflict++;
            }
            if (r.getMentorName() == null || r.getMentorName().trim().isEmpty()) {
                mentorMissing++;
            }
            if (nz(r.getProtocolSigned()) == 0) {
                protocolUnsigned++;
            }
            if (nz(r.getLearnTotal()) == 0) {
                noLearning++;
            }
            if (nz(r.getTaskOverdue()) > 0) {
                overdueTask++;
            }
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", rows.size());
        m.put("statusConflict", statusConflict);
        m.put("mentorMissing", mentorMissing);
        m.put("protocolUnsigned", protocolUnsigned);
        m.put("noLearning", noLearning);
        m.put("overdueTask", overdueTask);
        return m;
    }

    // ==================================================================
    // L3 · 个人档案
    // ==================================================================

    @Override
    public Map<String, Object> internDetail(Long userId) {
        if (userId == null) {
            throw new ServiceException("缺少人员参数");
        }
        BigDecimal th = threshold();

        // ★ 归属校验**复用**已按作用域过滤的逐人明细：
        //   超管能查到全部 17 人；部门管理员只能查到自己部门的人 —— 越界即"不可见"。
        InternStageRow target = null;
        for (InternStageRow r : labelledStageRows(th)) {
            if (userId.equals(r.getUserId())) {
                target = r;
                break;
            }
        }
        if (target == null) {
            throw new ServiceException("该人员不在你的可见范围内（或不是实习生）");
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("threshold", th);
        data.put("user", target);
        data.put("study", analysisMapper.selectInternStudy(userId));
        data.put("tasks", analysisMapper.selectInternTasks(userId));
        // 🟠 考核类（模拟 / 正式 / 知识点）：题库与考核模块待同事分支合并，本期后端不查
        data.put("practice", null);
        data.put("formal", null);
        data.put("knowledge", null);
        // ⚪ 阶段评价：stage_evaluation 全表 0 行；且 visible_scope='ADMIN_ONLY' 的评语不进实习生端画像
        data.put("evaluation", null);
        return data;
    }
}
