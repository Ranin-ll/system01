package com.ruoyi.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.business.domain.TaskPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 学习任务讨论区 Mapper
 *
 * <p>两点刻意的设计：</p>
 * <ol>
 *   <li><b>已删除的帖只回传「已删除」标记，不回传内容</b> —— 在 SQL 里就用
 *       {@code CASE WHEN deleted_by IS NULL THEN content END} 掐掉，
 *       而不是靠前端「拿到内容但不显示」。这样「删了但内容还在响应里」的隐患从源头断掉。</li>
 *   <li><b>不查 {@code deleted_by}</b> —— 谁删的属管理留痕，不该出现在实习生端响应里。</li>
 * </ol>
 *
 * @author ruoyi
 */
@Mapper
public interface TaskPostMapper extends BaseMapper<TaskPost> {

    /**
     * 某任务的全部帖子（含已删除的占位行 —— 少了它回复链会断）。
     * <p>带出作者昵称与头像；已删除的内容一律为 null。</p>
     */
    @Select("SELECT p.id, p.task_id AS taskId, p.user_id AS userId, p.parent_id AS parentId, "
            + "       CASE WHEN p.deleted_by IS NULL THEN p.content END AS content, "
            + "       CASE WHEN p.deleted_by IS NULL THEN 0 ELSE 1 END AS deleted, "
            + "       p.create_time AS createTime, "
            + "       u.user_name AS userName, u.nick_name AS nickName, u.avatar "
            + "  FROM task_post p "
            + "  LEFT JOIN sys_user u ON u.user_id = p.user_id "
            + " WHERE p.task_id = #{taskId} "
            + " ORDER BY p.create_time ASC, p.id ASC")
    List<TaskPost> selectByTask(@Param("taskId") Long taskId);

    /**
     * 软删（留痕）：仅当尚未删除时生效 —— 重复点删返回 0，幂等。
     */
    @Update("UPDATE task_post SET deleted_by = #{operatorId} WHERE id = #{id} AND deleted_by IS NULL")
    int softDelete(@Param("id") Long id, @Param("operatorId") Long operatorId);

    /**
     * 未删除的帖子数（任务卡上的「讨论 N」角标）
     */
    @Select("SELECT COUNT(*) FROM task_post WHERE task_id = #{taskId} AND deleted_by IS NULL")
    int countByTask(@Param("taskId") Long taskId);
}
