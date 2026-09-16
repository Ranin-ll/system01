package com.ruoyi.business.controller;

import com.ruoyi.business.domain.Exam;
import com.ruoyi.business.service.IExamService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/business/exam")
public class ExamController extends BaseController {

    @Autowired
    private IExamService examService;

    @PreAuthorize("@ss.hasPermi('business:bank:list')")
    @GetMapping("/list")
    public TableDataInfo list(Exam exam) {
        startPage();
        List<Exam> list = examService.selectExamList(exam);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('business:bank:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return AjaxResult.success(examService.selectById(id));
    }

    @PreAuthorize("@ss.hasPermi('business:bank:add')")
    @Log(title = "考核管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Exam exam) {
        return toAjax(examService.insertExam(exam));
    }

    @PreAuthorize("@ss.hasPermi('business:bank:edit')")
    @Log(title = "考核管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Exam exam) {
        return toAjax(examService.updateExam(exam));
    }

    @PreAuthorize("@ss.hasPermi('business:bank:remove')")
    @Log(title = "考核管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(examService.deleteByIds(ids));
    }

    @PreAuthorize("@ss.hasPermi('business:bank:edit')")
    @Log(title = "考核发布", businessType = BusinessType.UPDATE)
    @PutMapping("/publish/{id}")
    public AjaxResult publish(@PathVariable Long id) {
        return toAjax(examService.publish(id));
    }

    @PreAuthorize("@ss.hasPermi('business:bank:edit')")
    @Log(title = "考核状态修改", businessType = BusinessType.UPDATE)
    @PutMapping("/status/{id}")
    public AjaxResult changeStatus(@PathVariable Long id, @RequestParam("status") String status) {
        return toAjax(examService.changeStatus(id, status));
    }
}