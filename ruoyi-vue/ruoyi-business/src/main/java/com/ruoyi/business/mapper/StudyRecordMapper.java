package com.ruoyi.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.business.domain.StudyRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface StudyRecordMapper extends BaseMapper<StudyRecord> {
    /** 原子合并学习进度，避免首次保存时并发插入触发唯一键冲突。 */
    int upsertProgress(StudyRecord record);

    /**
     * 本人近 N 天逐日学习时长（2026-09-22 新增）。
     * 只返回有记录的天；study_duration 单位为秒。供工作台趋势图 / 时长分布使用。
     */
    List<Map<String, Object>> selectDailyDuration(@Param("userId") Long userId, @Param("days") Integer days);

    List<StudyRecord> selectCourseRecords(@Param("courseId") Long courseId,
                                          @Param("status") String status,
                                          @Param("deptId") Long deptId);

    List<StudyRecord> selectUserCourseRecords(@Param("courseId") Long courseId,
                                              @Param("userId") Long userId);
}
