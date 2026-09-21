package com.ruoyi.business.controller;

import com.ruoyi.business.domain.DeptMatrixRow;
import com.ruoyi.business.service.ISuperAnalysisService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 超管「培养分析看板」—— 培养运营 → 培养分析看板（L0）。
 *
 * <p><b>只读聚合，零新表。</b>人 / 学习 / 任务三族真数据；
 * 模拟考核、正式考核、知识点三族**本期不查库**（题库与考核模块待同事分支合并），
 * 由前端示例填充并打橙标 —— 理由与边界见
 * {@link com.ruoyi.business.service.impl.SuperAnalysisServiceImpl} 类注释。</p>
 *
 * <p><b>权限</b>：注解复用既有的 {@code business:bank:list}（部门管理员亦持有），
 * 越权/跨部门范围收窄落在 Service（超管全量、其它角色只看本部门）。</p>
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/business/super/analysis")
public class SuperAnalysisController extends BaseController {

    @Autowired
    private ISuperAnalysisService superAnalysisService;

    /** L0 全局 KPI + 预警清单（与 /dept-matrix 同源，两处数字必然一致） */
    @PreAuthorize("@ss.hasPermi('business:bank:list')")
    @GetMapping("/overview")
    public AjaxResult overview() {
        return AjaxResult.success(superAnalysisService.overview());
    }

    /** 部门对比矩阵：行 = 部门，列 = 各维度 */
    @PreAuthorize("@ss.hasPermi('business:bank:list')")
    @GetMapping("/dept-matrix")
    public AjaxResult deptMatrix() {
        List<DeptMatrixRow> rows = superAnalysisService.deptMatrix();
        return AjaxResult.success(rows);
    }

    /** 培养状态进度：逐人明细 + 漏斗分布（卡点由前端在同一份 rows 上聚合） */
    @PreAuthorize("@ss.hasPermi('business:bank:list')")
    @GetMapping("/stage-progress")
    public AjaxResult stageProgress() {
        return AjaxResult.success(superAnalysisService.stageProgress());
    }

    /** L1 部门详情：部门 KPI + 岗位分布 + 课程完成率 + 实习生明细 + 全局对照 */
    @PreAuthorize("@ss.hasPermi('business:bank:list')")
    @GetMapping("/dept-stats")
    public AjaxResult deptStats(@RequestParam("deptId") Long deptId) {
        return AjaxResult.success(superAnalysisService.deptStats(deptId));
    }

    /**
     * L0 知识点热力：部门 × 知识点矩阵（真数据，2026-09-22 起接真）。
     * 一次取全，前端自行 pivot 成「部门行 × 知识点列」的热力卡。
     */
    @PreAuthorize("@ss.hasPermi('business:bank:list')")
    @GetMapping("/knowledge-matrix")
    public AjaxResult knowledgeMatrix() {
        return AjaxResult.success(superAnalysisService.knowledgeMatrix());
    }

    /**
     * L3 个人档案：身份 + 逐学习项 + 逐任务 + <b>考核类（模拟逐场 / 正式逐场 / 知识点，2026-09-22 起为真数据）</b>。
     *
     * <p>★ 这两个查询补的是**超管读缺口** —— `/business/learning/**`、`/business/practice/**`
     * 的类级注解只放实习生角色。这里在本模块另写只读路径，**不去动那两处的类级注解**。</p>
     */
    @PreAuthorize("@ss.hasPermi('business:bank:list')")
    @GetMapping("/intern/{userId}")
    public AjaxResult internDetail(@PathVariable("userId") Long userId) {
        return AjaxResult.success(superAnalysisService.internDetail(userId));
    }
}
