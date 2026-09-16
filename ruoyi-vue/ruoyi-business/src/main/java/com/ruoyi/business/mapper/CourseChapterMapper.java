package com.ruoyi.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.business.domain.CourseChapter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CourseChapterMapper extends BaseMapper<CourseChapter> {
    List<CourseChapter> selectByCourseId(@Param("courseId") Long courseId);
}
