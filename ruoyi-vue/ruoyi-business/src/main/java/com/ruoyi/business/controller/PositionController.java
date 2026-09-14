package com.ruoyi.business.controller;

import com.ruoyi.business.domain.Position;
import com.ruoyi.business.service.IPositionService;
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
 * 岗位类型Controller
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/business/position")
public class PositionController extends BaseController {

    @Autowired
    private IPositionService positionService;

    /**
     * 查询岗位列表
     */
    @PreAuthorize("@ss.hasPermi('business:position:list')")
    @GetMapping("/list")
    public TableDataInfo list(Position position) {
        startPage();
        List<Position> list = positionService.selectPositionList(position);
        return getDataTable(list);
    }

    /**
     * 获取岗位详细信息
     */
    @PreAuthorize("@ss.hasPermi('business:position:query')")
    @GetMapping(value = {"/", "/{id}"})
    public AjaxResult getInfo(@PathVariable(value = "id", required = false) Long id) {
        return AjaxResult.success(id != null ? positionService.selectPositionById(id) : new Position());
    }

    /**
     * 新增岗位
     */
    @PreAuthorize("@ss.hasPermi('business:position:add')")
    @Log(title = "岗位管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Position position) {
        return toAjax(positionService.insertPosition(position));
    }

    /**
     * 修改岗位
     */
    @PreAuthorize("@ss.hasPermi('business:position:edit')")
    @Log(title = "岗位管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Position position) {
        return toAjax(positionService.updatePosition(position));
    }

    /**
     * 删除岗位
     */
    @PreAuthorize("@ss.hasPermi('business:position:remove')")
    @Log(title = "岗位管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(positionService.deletePositionByIds(ids));
    }

    /**
     * 修改岗位状态
     */
    @PreAuthorize("@ss.hasPermi('business:position:edit')")
    @Log(title = "岗位管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus/{id}/{status}")
    public AjaxResult changeStatus(@PathVariable Long id, @PathVariable Integer status) {
        return toAjax(positionService.updatePositionStatus(id, status));
    }
}