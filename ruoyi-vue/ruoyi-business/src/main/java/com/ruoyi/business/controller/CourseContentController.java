package com.ruoyi.business.controller;

import com.ruoyi.business.domain.CourseChapter;
import com.ruoyi.business.domain.CourseContentSaveBody;
import com.ruoyi.business.domain.StudyItem;
import com.ruoyi.business.service.ICourseContentService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 课程章节、资料、上传和学习记录接口。 */
@RestController
@RequestMapping("/business/course")
public class CourseContentController {

    @Autowired
    private ICourseContentService courseContentService;

    @PreAuthorize("@ss.hasPermi('business:course:query')")
    @GetMapping("/{courseId}/contents")
    public AjaxResult contents(@PathVariable Long courseId) {
        return AjaxResult.success(courseContentService.listContents(courseId));
    }

    @PreAuthorize("@ss.hasPermi('business:course:edit')")
    @Log(title = "课程编排", businessType = BusinessType.UPDATE)
    @PutMapping("/{courseId}/contents")
    public AjaxResult saveContents(@PathVariable Long courseId, @RequestBody CourseContentSaveBody body) {
        courseContentService.saveContents(courseId, body);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('business:course:edit')")
    @Log(title = "课程章节", businessType = BusinessType.INSERT)
    @PostMapping("/{courseId}/chapters")
    public AjaxResult addChapter(@PathVariable Long courseId, @RequestBody CourseChapter chapter) {
        return AjaxResult.success(courseContentService.addChapter(courseId, chapter));
    }

    @PreAuthorize("@ss.hasPermi('business:course:edit')")
    @Log(title = "课程章节", businessType = BusinessType.UPDATE)
    @PutMapping("/chapters/{chapterId}")
    public AjaxResult updateChapter(@PathVariable Long chapterId, @RequestBody CourseChapter chapter) {
        courseContentService.updateChapter(chapterId, chapter);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('business:course:edit')")
    @Log(title = "课程章节", businessType = BusinessType.DELETE)
    @DeleteMapping("/chapters/{chapterId}")
    public AjaxResult deleteChapter(@PathVariable Long chapterId) {
        courseContentService.deleteChapter(chapterId);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('business:course:edit')")
    @Log(title = "课程资料", businessType = BusinessType.INSERT)
    @PostMapping("/chapters/{chapterId}/items")
    public AjaxResult addItem(@PathVariable Long chapterId, @RequestBody StudyItem item) {
        return AjaxResult.success(courseContentService.addItem(chapterId, item));
    }

    @PreAuthorize("@ss.hasPermi('business:course:edit')")
    @Log(title = "课程资料", businessType = BusinessType.UPDATE)
    @PutMapping("/items/{itemId}")
    public AjaxResult updateItem(@PathVariable Long itemId, @RequestBody StudyItem item) {
        courseContentService.updateItem(itemId, item);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('business:course:edit')")
    @Log(title = "课程资料", businessType = BusinessType.DELETE)
    @DeleteMapping("/items/{itemId}")
    public AjaxResult deleteItem(@PathVariable Long itemId) {
        courseContentService.deleteItem(itemId);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('business:course:edit')")
    @Log(title = "课程资料上传", businessType = BusinessType.INSERT)
    @PostMapping("/items/{itemId}/asset")
    public AjaxResult uploadAsset(@PathVariable Long itemId, @RequestParam("file") MultipartFile file) {
        String path = courseContentService.uploadAsset(itemId, file);
        AjaxResult result = AjaxResult.success();
        result.put("url", path);
        result.put("originalFilename", file.getOriginalFilename());
        result.put("fileSize", file.getSize());
        return result;
    }

    @PreAuthorize("@ss.hasPermi('business:course:query')")
    @GetMapping("/{courseId}/study-records")
    public AjaxResult studyRecords(@PathVariable Long courseId,
                                   @RequestParam(required = false) String status) {
        return AjaxResult.success(courseContentService.listStudyRecords(courseId, status));
    }
}
