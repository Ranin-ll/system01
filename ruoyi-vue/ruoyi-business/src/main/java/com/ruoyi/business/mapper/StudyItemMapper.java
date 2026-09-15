package com.ruoyi.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.business.domain.StudyItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StudyItemMapper extends BaseMapper<StudyItem> {
    List<StudyItem> selectByCourseId(@Param("courseId") Long courseId);
    StudyItem selectByIdAndCourse(@Param("id") Long id, @Param("courseId") Long courseId);
    StudyItem selectLearningItem(@Param("itemId") Long itemId, @Param("userId") Long userId);
}
