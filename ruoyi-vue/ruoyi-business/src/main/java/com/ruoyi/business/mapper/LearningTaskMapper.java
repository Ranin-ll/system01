package com.ruoyi.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.business.domain.LearningTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 学习任务 Mapper
 *
 * <p>范围口径与消息一致：超管传 <code>scopeDeptId = null</code> 看全部；部门管理员传自己的 dept_id，
 * 通过 <code>dept_position</code> 把「岗位」换算成「本部门岗位」。</p>
 *
 * @author ruoyi
 */
@Mapper
public interface LearningTaskMapper extends BaseMapper<LearningTask> {

    /**
     * 任务列表（带分配数 / 已提交数 / 待批阅数）
     *
     * @param userId 可选：只返回分配给该实习生的任务，并带出 TA 的 assignStatus
     */
    List<LearningTask> selectTaskList(@Param("scopeDeptId") Long scopeDeptId,
                                      @Param("positionId") Long positionId,
                                      @Param("status") String status,
                                      @Param("keyword") String keyword,
                                      @Param("userId") Long userId);

    /**
     * 单个任务详情（含计数）
     */
    LearningTask selectTaskDetail(@Param("id") Long id);

    /**
     * 我的任务（实习生）：以 task_assignment 为主表，带最新一次提交
     */
    List<LearningTask> selectMyTasks(@Param("userId") Long userId);

    /**
     * 发布时要发给谁：该岗位 + 对应部门下的**在培实习生**
     */
    List<Long> selectCandidateUserIds(@Param("positionId") Long positionId);

    /**
     * 「按部门」发布时展开：该部门下的全部在培实习生（不分岗位）
     */
    List<Long> selectCandidateUserIdsByDept(@Param("deptId") Long deptId);

    /**
     * 显式把岗位置空。
     *
     * <p>为什么需要单独的语句：MyBatis-Plus 的 {@code updateById} 默认**忽略 null 字段**
     * （字段策略 NOT_NULL），所以「按岗位 → 按部门/按人」的修改只传 null 是清不掉旧的
     * position_id 的 —— 旧岗位会残留，那条任务就还会被算成「属于该岗位」。</p>
     */
    @org.apache.ibatis.annotations.Update("UPDATE learning_task SET position_id = NULL WHERE id = #{id}")
    int clearPosition(@Param("id") Long id);

    /**
     * 提交情况：以 assignment 为主表，未提交的也要列出来
     */
    List<Map<String, Object>> selectAssignmentBoard(@Param("taskId") Long taskId);

    /**
     * 本部门在培实习生（含待转正）—— 供「按实习生筛选」「指定人员」共用。
     * deptId 传 null 表示不限（超管）。
     */
    List<Map<String, Object>> selectDeptTrainees(@Param("deptId") Long deptId);

    /**
     * 某个分配的「历次提交」（重复提交 version+1，旧版本要能看到）
     */
    List<Map<String, Object>> selectSubmissionHistory(@Param("assignmentId") Long assignmentId);

    /**
     * 岗位是否属于该部门（dept_position 生效绑定）—— 部门管理员的越权校验
     */
    @Select("SELECT COUNT(*) FROM dept_position WHERE dept_id = #{deptId} AND position_id = #{positionId} AND status = 1")
    int countDeptPosition(@Param("deptId") Long deptId, @Param("positionId") Long positionId);

    // ------------------------- P3：定时任务用 -------------------------

    /**
     * 已逾期未完成的分配（任务必须是 PUBLISHED）
     */
    List<Map<String, Object>> selectOverdueAssignments();

    /**
     * 即将到期（days 天内）且**尚未提交**的分配
     */
    List<Map<String, Object>> selectDueSoonAssignments(@Param("days") int days);

    /**
     * 当天是否已给该人发过该任务的某类提醒（去重，避免定时任务每天刷屏）
     */
    @Select("SELECT COUNT(*) FROM notice n "
            + "JOIN notice_target nt ON nt.notice_id = n.id AND nt.user_id = #{userId} "
            + "WHERE n.biz_type = 'TASK' AND n.biz_id = #{taskId} AND n.msg_type = #{msgType} "
            + "AND n.create_time >= CURDATE()")
    int countNotifiedToday(@Param("taskId") Long taskId, @Param("userId") Long userId,
                           @Param("msgType") String msgType);
}
