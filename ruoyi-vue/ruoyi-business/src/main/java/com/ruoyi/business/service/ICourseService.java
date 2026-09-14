package com.ruoyi.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.business.domain.Course;
import com.ruoyi.business.domain.Position;

import java.util.List;

/**
 * 课程Service
 */
public interface ICourseService extends IService<Course> {

    List<Course> selectCourseList(Course course);

    List<Position> selectCoursePositions();

    Course selectById(Long id);

    int insertCourse(Course course);

    int updateCourse(Course course);

    int deleteByIds(Long[] ids);

    int publish(Long id);
}
