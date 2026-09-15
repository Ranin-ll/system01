package com.ruoyi.business.service;

import com.ruoyi.business.domain.Course;
import com.ruoyi.business.domain.LearningProgressBody;
import com.ruoyi.business.domain.StudyRecord;

import java.util.List;

public interface ILearningService {
    List<Course> listPublishedCourses();
    Course getPublishedCourse(Long courseId);
    StudyRecord saveProgress(Long itemId, LearningProgressBody body);
}
