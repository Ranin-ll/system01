package com.ruoyi.business.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 任务「指定人员」Mapper（{@code task_target_user}）
 *
 * <p>只在 {@code learning_task.assign_scope = 'USER'} 时有意义：任务还处于草稿时先把名单存下来，
 * 发布那一刻再据此展开 {@code task_assignment}。</p>
 *
 * @author ruoyi
 */
@Mapper
public interface TaskTargetUserMapper {

    /** 整体替换名单（草稿保存时用） */
    @Delete("DELETE FROM task_target_user WHERE task_id = #{taskId}")
    int deleteByTask(@Param("taskId") Long taskId);

    /** 表上有唯一键 (task_id, user_id) → INSERT IGNORE 天然幂等 */
    @Insert("INSERT IGNORE INTO task_target_user (task_id, user_id, create_time) VALUES (#{taskId}, #{userId}, NOW())")
    int insertIgnore(@Param("taskId") Long taskId, @Param("userId") Long userId);

    @Select("SELECT user_id FROM task_target_user WHERE task_id = #{taskId} ORDER BY user_id")
    List<Long> selectUserIds(@Param("taskId") Long taskId);
}
