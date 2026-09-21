package com.ruoyi.business.controller;

import com.ruoyi.business.domain.PracticeModule;
import com.ruoyi.business.service.IPracticeModuleService;
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
 * 模拟考核模块 Controller
 *
 * 管理端（/list、/options、/{id}、POST、PUT、DELETE、/status）按部门数据范围维护模块；
 * 实习生端（/published）只读本部门已启用模块，用于「先选模块再看模块下的考核」。
 */
@RestController
@RequestMapping("/business/practice-module")
public class PracticeModuleController extends BaseController {

    @Autowired
    private IPracticeModuleService practiceModuleService;

    /** 管理端列表（附带模块内实操题/理论考核数量） */
    @PreAuthorize("@ss.hasPermi('business:pmodule:list')")
    @GetMapping("/list")
    public TableDataInfo list(PracticeModule query) {
        startPage();
        List<PracticeModule> list = practiceModuleService.selectModuleList(query);
        return getDataTable(list);
    }

    /** 管理端下拉选项：本部门模块（超管可传 deptId） */
    @PreAuthorize("@ss.hasPermi('business:pmodule:list')")
    @GetMapping("/options")
    public AjaxResult options(@RequestParam(value = "deptId", required = false) Long deptId) {
        return AjaxResult.success(practiceModuleService.listOptions(deptId));
    }

    /** 管理端详情 */
    @PreAuthorize("@ss.hasPermi('business:pmodule:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(practiceModuleService.selectModuleById(id));
    }

    /** 新建模块 */
    @PreAuthorize("@ss.hasPermi('business:pmodule:add')")
    @Log(title = "模拟考核模块", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PracticeModule module) {
        return toAjax(practiceModuleService.insertModule(module));
    }

    /** 编辑模块（改名 / 说明 / 排序 / 状态） */
    @PreAuthorize("@ss.hasPermi('business:pmodule:edit')")
    @Log(title = "模拟考核模块", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PracticeModule module) {
        return toAjax(practiceModuleService.updateModule(module));
    }

    /** 启用 / 停用 */
    @PreAuthorize("@ss.hasPermi('business:pmodule:edit')")
    @Log(title = "模拟考核模块", businessType = BusinessType.UPDATE)
    @PutMapping("/status/{id}")
    public AjaxResult changeStatus(@PathVariable("id") Long id, @RequestParam("status") Integer status) {
        return toAjax(practiceModuleService.changeStatus(id, status));
    }

    /** 删除模块 */
    @PreAuthorize("@ss.hasPermi('business:pmodule:remove')")
    @Log(title = "模拟考核模块", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable("ids") Long[] ids) {
        return toAjax(practiceModuleService.deleteByIds(ids));
    }

    /** 实习生端：本部门已启用模块列表（只读） */
    @PreAuthorize("@ss.hasPermi('business:bank:list')")
    @GetMapping("/published")
    public AjaxResult published() {
        return AjaxResult.success(practiceModuleService.selectEnabledForIntern());
    }
}
