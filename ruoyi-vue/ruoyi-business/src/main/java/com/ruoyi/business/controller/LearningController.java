package com.ruoyi.business.controller;

import com.ruoyi.business.domain.LearningProgressBody;
import com.ruoyi.business.service.ILearningService;
import com.ruoyi.common.core.domain.AjaxResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
