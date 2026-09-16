package com.ruoyi.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.business.domain.StudyRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StudyRecordMapper extends BaseMapper<StudyRecord> {
    /** 原子合并学习进度，避免首次保存时并发插入触发唯一键冲突。 */
    int upsertProgress(StudyRecord record);

    List<StudyRecord> selectCourseRecords(@Param("courseId") Long courseId,
                                          @Param("status") String status,
                                          @Param("deptId") Long deptId);

    List<StudyRecord> selectUserCourseRecords(@Param("courseId") Long courseId,
                                              @Param("userId") Long userId);
}
