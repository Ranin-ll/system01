package com.ruoyi.business.service;

import com.ruoyi.business.domain.Course;
import com.ruoyi.business.domain.LearningProgressBody;
import com.ruoyi.business.domain.StudyRecord;

import java.util.List;
import java.util.Map;

public interface ILearningService {
    List<Course> listPublishedCourses();
    Course getPublishedCourse(Long courseId);
    StudyRecord saveProgress(Long itemId, LearningProgressBody body);

    /**
     * 本人近 N 天逐日学习时长（2026-09-22 新增，供工作台「学习完成率趋势 / 学习时长分布」接真）。
     * 返回 [{date, seconds, items}]，只含有记录的天，缺的天由前端补 0；单位秒。
     */
    List<Map<String, Object>> dailyDuration(Long userId, Integer days);
}
