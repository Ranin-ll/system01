package com.ruoyi.business.service;

import com.ruoyi.business.domain.DeptMatrixRow;
import com.ruoyi.business.domain.DeptStatsVO;

import java.util.List;
import java.util.Map;

/**
 * 超管「培养分析看板」（只读聚合）。
 *
 * <p><b>一致性设计</b>：L0 看板的 KPI 条、部门对比矩阵、培养状态漏斗三处**全部由同一份聚合结果派生**
 * （{@link #overview()} 与 {@link #deptMatrix()} 内部走同一个 buildMatrix()）。
 * 这是刻意的 —— 项目里已有过「统计与列表对不上」的教训：
 * 只要为同一口径写第二条聚合路径，两个数迟早会漂移。</p>
 *
 * @author ruoyi
 */
public interface ISuperAnalysisService {

    /** L0 全局 KPI + 预警清单（与 {@link #deptMatrix()} 同源） */
    Map<String, Object> overview();

    /** 部门对比矩阵：行 = 部门，列 = 各维度 */
    List<DeptMatrixRow> deptMatrix();

    /** 培养状态进度：逐人明细 + 漏斗分布（卡点由前端在同一份 rows 上聚合，避免三处口径分叉） */
    Map<String, Object> stageProgress();

    /** L1 部门详情：部门 KPI + 岗位分布 + 课程完成率 + 实习生明细（下钻入口）+ 全局对照 */
    DeptStatsVO deptStats(Long deptId);

    /** L0 知识点热力：部门 × 知识点矩阵（真数据；前端 pivot 成部门行 × 知识点列） */
    List<Map<String, Object>> knowledgeMatrix();

    /**
     * L3 个人档案：身份 + 逐学习项 + 逐任务（真数据），
     * 考核类与阶段评价为 {@code null}（本期不查，由前端示例填充）。
     */
    Map<String, Object> internDetail(Long userId);
}
