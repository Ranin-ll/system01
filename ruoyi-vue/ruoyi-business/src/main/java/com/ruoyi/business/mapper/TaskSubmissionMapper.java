package com.ruoyi.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.business.domain.TaskSubmission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 作业提交 Mapper
 *
 * <p>表上没有唯一键 —— 重复提交是**有意允许**的，靠 {@code version + 1} 形成版本链；
 * 「最新一次」用 {@code ORDER BY version DESC LIMIT 1} 取。</p>
 *
 * <p><b>内容只保留最近一次</b>：更早版本的 {@code content / file_url / file_name} 会在新提交进来时清空
 * （行保留 → 提交次数与「当时的批阅结果/批语」完整，但不再堆旧内容）。</p>
 *
 * @author ruoyi
 */
@Mapper
public interface TaskSubmissionMapper extends BaseMapper<TaskSubmission> {

    @Select("SELECT * FROM task_submission WHERE assignment_id = #{assignmentId} ORDER BY version DESC LIMIT 1")
    TaskSubmission selectLatest(@Param("assignmentId") Long assignmentId);

    /** 更早版本的附件相对路径 —— 清空之前先取出来，用于把磁盘文件也删掉 */
    @Select("SELECT file_url FROM task_submission "
            + " WHERE assignment_id = #{assignmentId} AND id <> #{keepId} AND file_url IS NOT NULL")
    List<String> selectOlderFileUrls(@Param("assignmentId") Long assignmentId, @Param("keepId") Long keepId);

    /**
     * 清空更早版本的提交内容（保留行）。
     *
     * <p>刻意用显式 UPDATE 而不是 {@code updateById} —— MyBatis-Plus 默认<b>忽略 null 字段</b>，
     * 用实体更新根本清不掉内容。</p>
     *
     * @return 受影响行数
     */
    @Update("UPDATE task_submission SET content = NULL, file_url = NULL, file_name = NULL "
            + " WHERE assignment_id = #{assignmentId} AND id <> #{keepId}")
    int clearOlderContent(@Param("assignmentId") Long assignmentId, @Param("keepId") Long keepId);
}

