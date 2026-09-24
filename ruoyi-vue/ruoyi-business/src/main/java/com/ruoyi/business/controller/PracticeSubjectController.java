package com.ruoyi.business.controller;

import com.ruoyi.business.domain.PracticeSubject;
import com.ruoyi.business.service.IPracticeSubjectService;
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
 * 模拟考核实操题 Controller
 *
 * 管理端（/list、/{id}、POST、PUT、DELETE、/status）按部门数据范围维护；
 * 实习生端（/published）只读本部门已启用的题目，用于查看题干与下载附件。
 */
@RestController
@RequestMapping("/business/practice-subject")
public class PracticeSubjectController extends BaseController {

    @Autowired
    private IPracticeSubjectService practiceSubjectService;

    /** 管理端列表 */
    @PreAuthorize("@ss.hasPermi('business:psubject:list')")
    @GetMapping("/list")
    public TableDataInfo list(PracticeSubject query) {
        startPage();
        List<PracticeSubject> list = practiceSubjectService.selectSubjectList(query);
        return getDataTable(list);
    }

    /** 管理端详情 */
    @PreAuthorize("@ss.hasPermi('business:psubject:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(practiceSubjectService.selectSubjectById(id));
    }

    /** 发布实操题 */
    @PreAuthorize("@ss.hasPermi('business:psubject:add')")
    @Log(title = "模拟实操题", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PracticeSubject subject) {
        return toAjax(practiceSubjectService.insertSubject(subject));
    }

    /** 编辑实操题 */
    @PreAuthorize("@ss.hasPermi('business:psubject:edit')")
    @Log(title = "模拟实操题", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PracticeSubject subject) {
        return toAjax(practiceSubjectService.updateSubject(subject));
    }

    /** 启用 / 停用 */
    @PreAuthorize("@ss.hasPermi('business:psubject:edit')")
    @Log(title = "模拟实操题", businessType = BusinessType.UPDATE)
    @PutMapping("/status/{id}")
    public AjaxResult changeStatus(@PathVariable("id") Long id, @RequestParam("status") Integer status) {
        return toAjax(practiceSubjectService.changeStatus(id, status));
    }

    /** 删除实操题 */
    @PreAuthorize("@ss.hasPermi('business:psubject:remove')")
    @Log(title = "模拟实操题", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable("ids") Long[] ids) {
        return toAjax(practiceSubjectService.deleteByIds(ids));
    }

    /** 实习生端：本部门已发布的实操题列表（★ 2026-09-23 起不分模块，按排序号返回） */
    @PreAuthorize("@ss.hasPermi('business:bank:list')")
    @GetMapping("/published")
    public AjaxResult published() {
        return AjaxResult.success(practiceSubjectService.selectPublishedForIntern());
    }

    /** 实习生端：本部门已发布的单条实操题详情（独立详情页使用，支持刷新直链） */
    @PreAuthorize("@ss.hasPermi('business:bank:list')")
    @GetMapping("/published/{id}")
    public AjaxResult publishedDetail(@PathVariable("id") Long id) {
        return AjaxResult.success(practiceSubjectService.selectPublishedDetailForIntern(id));
    }
}
