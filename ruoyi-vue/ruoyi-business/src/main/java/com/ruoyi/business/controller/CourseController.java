package com.ruoyi.business.controller;

import com.ruoyi.business.domain.Course;
import com.ruoyi.business.domain.Position;
import com.ruoyi.business.service.ICourseService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 课程管理Controller
 *
 * 涉及角色：超级管理员、部门管理员、实习生
 */
@RestController
@RequestMapping("/business/course")
public class CourseController extends BaseController {

    @Autowired
    private ICourseService courseService;

    @PreAuthorize("@ss.hasPermi('business:course:list')")
    @GetMapping("/list")
    public TableDataInfo list(Course course) {
        startPage();
        List<Course> list = courseService.selectCourseList(course);
        return getDataTable(list);
    }

    /** 查询当前课程管理范围内可选的岗位。 */
    @PreAuthorize("@ss.hasPermi('business:course:list')")
    @GetMapping("/positions")
    public AjaxResult positions() {
        List<Position> list = courseService.selectCoursePositions();
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('business:course:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return AjaxResult.success(courseService.selectById(id));
    }

    @PreAuthorize("@ss.hasPermi('business:course:add')")
    @Log(title = "课程管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Course course) {
        return toAjax(courseService.insertCourse(course));
    }

    @PreAuthorize("@ss.hasPermi('business:course:edit')")
    @Log(title = "课程管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Course course) {
        return toAjax(courseService.updateCourse(course));
    }

    @PreAuthorize("@ss.hasPermi('business:course:remove')")
    @Log(title = "课程管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(courseService.deleteByIds(ids));
    }

    @PreAuthorize("@ss.hasPermi('business:course:publish')")
    @Log(title = "课程发布", businessType = BusinessType.UPDATE)
    @PutMapping("/publish/{id}")
    public AjaxResult publish(@PathVariable Long id) {
        return toAjax(courseService.publish(id));
    }
}
