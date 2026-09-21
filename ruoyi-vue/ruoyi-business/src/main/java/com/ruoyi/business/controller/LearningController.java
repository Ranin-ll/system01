package com.ruoyi.business.controller;

import com.ruoyi.business.domain.LearningProgressBody;
import com.ruoyi.business.service.ILearningService;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 实习生在线学习接口。 */
@RestController
@RequestMapping("/business/learning")
@PreAuthorize("@ss.hasAnyRoles('PRE_TRAINEE,FORMAL_TRAINEE')")
public class LearningController {

    private final ILearningService learningService;

    public LearningController(ILearningService learningService) {
        this.learningService = learningService;
    }

    /**
     * 本人近 N 天逐日学习时长（2026-09-22 新增）—— 供工作台「学习完成率趋势 / 学习时长分布」接真。
     * 只返回有记录的天（缺的天由前端补 0，表示当天确实没学），单位秒；days 限定 7~90。
     */
    @GetMapping("/duration-daily")
    public AjaxResult durationDaily(@RequestParam(value = "days", defaultValue = "14") Integer days) {
        int d = Math.min(Math.max(days == null ? 14 : days, 7), 90);
        return AjaxResult.success(learningService.dailyDuration(SecurityUtils.getUserId(), d));
    }

    @GetMapping("/courses")
    public AjaxResult courses() {
        return AjaxResult.success(learningService.listPublishedCourses());
    }

    @GetMapping("/courses/{courseId}")
    public AjaxResult course(@PathVariable Long courseId) {
        return AjaxResult.success(learningService.getPublishedCourse(courseId));
    }

    @PutMapping("/items/{itemId}/progress")
    public AjaxResult saveProgress(@PathVariable Long itemId, @RequestBody LearningProgressBody body) {
        return AjaxResult.success(learningService.saveProgress(itemId, body));
    }
}
