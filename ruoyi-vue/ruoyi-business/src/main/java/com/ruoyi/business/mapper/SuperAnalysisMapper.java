package com.ruoyi.business.mapper;

import com.ruoyi.business.domain.DeptMatrixRow;
import com.ruoyi.business.domain.DeptStatsVO;
import com.ruoyi.business.domain.InternStageRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 超管「培养分析看板」（只读聚合）。
 *
 * <p><b>为什么必须新起接口</b>：本项目的聚合 SQL 全仓只有 {@code StudyRecordMapper.xml} 里的 2 处，
 * 且是按 {@code user_id / course_id} 分组 —— **部门维度的聚合一条都没有**，
 * 「部门横向对比」无法靠前端拼出来。</p>
 *
 * <p><b>取数范围（本期）</b>：只查 <b>人 / 学习 / 任务</b> 三族（真数据）。
 * 模拟考核、正式考核、知识点**一个都不查** —— 题库与考核模块将被同事分支改动，
 * 在会变的表上取数等于白做。这几列由前端 {@code _mock.js} 填充并统一打橙标，
 * 待分支合并后在 {@code SuperAnalysisMapper.xml} 里补查询即可，**前端模板零改动**。</p>
 *
 * <p><b>★ 人数口径</b>：所有「捞人 / 数人」一律用 {@code EXISTS} 做角色存在性判断 ——
 * {@code sys_user.user_status} 不是权限，历史遗留账号常有「状态是实习生但一个角色都没有」，
 * 只按状态数人会把废号计进来。用 EXISTS 而非 JOIN，是为了避免多角色行导致计数与均值翻倍。</p>
 *
 * @author ruoyi
 */
@Mapper
public interface SuperAnalysisMapper {

    /** 在培实习生按部门（持角色 + 账号有效） */
    List<DeptMatrixRow> selectInternByDept(@Param("deptId") Long deptId);

    /** 学习情况按部门（记录数 / 达标数 / 人数 / 平均进度 / 视频时长） */
    List<DeptMatrixRow> selectLearnByDept(@Param("threshold") BigDecimal threshold,
                                          @Param("deptId") Long deptId);

    /** 任务交付按部门（应交 / 已交 / 逾期 / 未开始） */
    List<DeptMatrixRow> selectTaskByDept(@Param("deptId") Long deptId);

    /** 培养状态进度逐人明细（阶段 / 卡点 / 转正 gate 的真数据输入项）。
     *  {@code deptId} 非空则只看该部门 —— L1 部门详情的实习生明细表复用它，**不再另写一份口径**。 */
    List<InternStageRow> selectInternStageRows(@Param("threshold") BigDecimal threshold,
                                               @Param("deptId") Long deptId);

    // ==================================================================
    // L1 部门详情
    // ==================================================================

    /** 部门身份 + 绑定岗位（dept_position，1:1） */
    DeptStatsVO selectDeptHeader(@Param("deptId") Long deptId);

    /** 本部门在培实习生的岗位分布 */
    List<Map<String, Object>> selectPositionDist(@Param("deptId") Long deptId);

    /** 本部门岗位下各门课程的学习情况（已学人数 / 达标项 / 平均进度） */
    List<Map<String, Object>> selectCourseProgress(@Param("deptId") Long deptId,
                                                   @Param("threshold") BigDecimal threshold);

    // ==================================================================
    // L3 个人档案
    // ★ 这两个查询是「超管读缺口」的补位：/business/learning/** 与 /business/practice/**
    //   的**类级注解**只放 PRE_TRAINEE/FORMAL_TRAINEE，超管读不到 ——
    //   所以必须在本模块另写读路径，而**不去动那两处的类级注解**。
    // ==================================================================

    /** 逐学习项（课程 / 章节 / 单项：进度、时长、读完确认、完成时间） */
    List<Map<String, Object>> selectInternStudy(@Param("userId") Long userId);

    /**
     * 逐任务（含审核结果）。
     *
     * <p>⚠️ 必须对 {@code task_submission} **取最新一次提交**：一个 assignment 会有
     * 多个版本行（实测 user 126 的同一任务有 PASSED/REJECTED/PASSED 三行），
     * 直接 JOIN 会让任务清单**行数翻倍**、审核结果还会串。</p>
     */
    List<Map<String, Object>> selectInternTasks(@Param("userId") Long userId);
}
