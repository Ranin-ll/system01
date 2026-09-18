package com.ruoyi.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.business.domain.TaskAttachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 学习任务附件 Mapper
 *
 * @author ruoyi
 */
@Mapper
public interface TaskAttachmentMapper extends BaseMapper<TaskAttachment> {

    /** 某任务的全部附件（带上传人昵称） */
    @Select("SELECT a.id, a.task_id AS taskId, a.file_name AS fileName, a.file_url AS fileUrl, "
            + "       a.file_size AS fileSize, a.file_ext AS fileExt, a.uploader_id AS uploaderId, "
            + "       a.create_time AS createTime, u.nick_name AS uploaderName "
            + "  FROM task_attachment a "
            + "  LEFT JOIN sys_user u ON u.user_id = a.uploader_id "
            + " WHERE a.task_id = #{taskId} AND a.deleted = 0 "
            + " ORDER BY a.id ASC")
    List<TaskAttachment> selectByTask(@Param("taskId") Long taskId);

    /**
     * 实习生「我的任务」的全部附件 —— <b>一次查完</b>。
     *
     * <p>刻意不做「按任务逐个查」：任务卡每张一个请求就是 N+1，前端拿到后本地按 taskId 分组即可。
     * 只查已发布/已结束的任务（草稿不泄露）。</p>
     */
    @Select("SELECT a.id, a.task_id AS taskId, a.file_name AS fileName, a.file_url AS fileUrl, "
            + "       a.file_size AS fileSize, a.file_ext AS fileExt, a.uploader_id AS uploaderId, "
            + "       a.create_time AS createTime "
            + "  FROM task_attachment a "
            + "  JOIN task_assignment s ON s.task_id = a.task_id AND s.user_id = #{userId} "
            + "  JOIN learning_task t ON t.id = a.task_id AND t.deleted = 0 AND t.status <> 'DRAFT' "
            + " WHERE a.deleted = 0 "
            + " ORDER BY a.task_id ASC, a.id ASC")
    List<TaskAttachment> selectByAssignee(@Param("userId") Long userId);

    /** 软删（幂等：已删的返回 0） */
    @Update("UPDATE task_attachment SET deleted = 1 WHERE id = #{id} AND deleted = 0")
    int softDelete(@Param("id") Long id);
}
