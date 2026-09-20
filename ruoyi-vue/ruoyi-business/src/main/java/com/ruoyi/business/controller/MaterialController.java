package com.ruoyi.business.controller;

import com.ruoyi.business.domain.Material;
import com.ruoyi.business.service.IMaterialService;
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
 * 备考资料 Controller
 *
 * 管理端（/list、/{id}、POST、PUT、DELETE、/status、/positions）按部门数据范围维护；
 * 实习生端（/published）只读本部门已发布资料，支持预览与下载。
 */
@RestController
@RequestMapping("/business/material")
public class MaterialController extends BaseController {

    @Autowired
    private IMaterialService materialService;

    /** 管理端列表 */
    @PreAuthorize("@ss.hasPermi('business:material:list')")
    @GetMapping("/list")
    public TableDataInfo list(Material query) {
        startPage();
        List<Material> list = materialService.selectMaterialList(query);
        return getDataTable(list);
    }

    /** 管理端详情 */
    @PreAuthorize("@ss.hasPermi('business:material:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(materialService.selectMaterialById(id));
    }

    /** 新增资料 */
    @PreAuthorize("@ss.hasPermi('business:material:add')")
    @Log(title = "备考资料", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Material material) {
        return toAjax(materialService.insertMaterial(material));
    }

    /** 编辑资料 */
    @PreAuthorize("@ss.hasPermi('business:material:edit')")
    @Log(title = "备考资料", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Material material) {
        return toAjax(materialService.updateMaterial(material));
    }

    /** 发布 / 停用（status: PUBLISHED / DRAFT / DISABLED） */
    @PreAuthorize("@ss.hasPermi('business:material:edit')")
    @Log(title = "备考资料", businessType = BusinessType.UPDATE)
    @PutMapping("/status/{id}")
    public AjaxResult changeStatus(@PathVariable("id") Long id, @RequestParam("status") String status) {
        return toAjax(materialService.changeStatus(id, status));
    }

    /** 删除资料 */
    @PreAuthorize("@ss.hasPermi('business:material:remove')")
    @Log(title = "备考资料", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable("ids") Long[] ids) {
        return toAjax(materialService.deleteByIds(ids));
    }

    /** 管理端上传表单：适用岗位下拉（超管全部，部门管理员锁定本部门） */
    @PreAuthorize("@ss.hasPermi('business:material:list')")
    @GetMapping("/positions")
    public AjaxResult positions() {
        return AjaxResult.success(materialService.selectApplicablePositions());
    }

    /** 实习生端：本部门已发布资料列表（只读，支持预览与下载） */
    @PreAuthorize("@ss.hasPermi('business:bank:list')")
    @GetMapping("/published")
    public AjaxResult published() {
        return AjaxResult.success(materialService.selectPublishedForIntern());
    }

    /** 实习生端：本部门已发布单条资料详情（支持刷新直链） */
    @PreAuthorize("@ss.hasPermi('business:bank:list')")
    @GetMapping("/published/{id}")
    public AjaxResult publishedDetail(@PathVariable("id") Long id) {
        return AjaxResult.success(materialService.selectPublishedDetailForIntern(id));
    }
}
