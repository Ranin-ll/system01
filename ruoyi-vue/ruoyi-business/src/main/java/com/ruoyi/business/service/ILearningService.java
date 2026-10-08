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

    /**
     * 指定用户的「学习完成率(%)」—— 转正资格门槛用（2026-09-29 新增）。
     *
     * <p>口径与实习生端 {@code assessment/result/index.vue} 的 {@code learningSummary} 完全一致：
     * 取该用户岗位下所有已发布课程，<b>必修课优先</b>（有必修课就只算必修课，否则算全部课），
     * 求各课进度的算术平均。</p>
     *
     * <p>★ 无任何课程 → 返回 {@code null}（不伪装成 0%），由调用方决定展示口径。
     * 本方法<b>不校验当前登录人是否为实习生</b>，因为部门管理员 / 超管需要读他人的完成率。</p>
     */
    java.math.BigDecimal studyRateOf(Long userId);
}
