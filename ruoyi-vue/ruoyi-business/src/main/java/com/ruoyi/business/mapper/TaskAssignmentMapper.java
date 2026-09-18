package com.ruoyi.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.business.domain.TaskAssignment;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 学习任务分配 Mapper
 *
 * <p>表上已有唯一键 <code>uk_assign_task_user(task_id, user_id)</code>，
 * 所以 <code>INSERT IGNORE</code> 天然幂等 —— 同一任务重复发布不会重复分配。</p>
 *
 * @author ruoyi
 */
@Mapper
public interface TaskAssignmentMapper extends BaseMapper<TaskAssignment> {

    @Insert("INSERT IGNORE INTO task_assignment (task_id, user_id, status, create_time) "
            + "VALUES (#{taskId}, #{userId}, 'NOT_STARTED', NOW())")
    int assignIgnore(@Param("taskId") Long taskId, @Param("userId") Long userId);

    @Select("SELECT * FROM task_assignment WHERE task_id = #{taskId} AND user_id = #{userId} LIMIT 1")
    TaskAssignment selectMine(@Param("taskId") Long taskId, @Param("userId") Long userId);

    /**
     * 逾期标记（定时任务）：已完成的不会被改回来
     */
    @Update("UPDATE task_assignment SET status = 'OVERDUE' WHERE id = #{id} AND status <> 'DONE'")
    int markOverdue(@Param("id") Long id);
}
