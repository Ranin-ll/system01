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
import java.util.Map;

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
     * 部门 ↔ 岗位绑定（权威表 dept_position，仅生效中）
     *
     * 供管理端「组织岗位」页核对部门与岗位的对应关系。
     * 此前前端没有权威来源，只能从「在册用户」或「报名记录」反推 ——
     * 账号直接创建、未走注册审核的部门会推不出岗位（显示未绑定）。
     * 只读接口，路径为字面量，优先于下方 /{id} 匹配。
     */
    @PreAuthorize("@ss.hasPermi('business:position:list')")
    @GetMapping("/dept-bindings")
    public AjaxResult deptBindings() {
        return AjaxResult.success(positionService.selectDeptBindings());
    }

    /**
     * 新增「部门 ↔ 岗位」绑定（**超管专属**，2026-09-20）
     *
     * <p>支持一部门多岗位。权限串复用部门管理员也持有的 {@code business:position:list}，
     * 所以「仅超管」的强校验放在 Service（{@code requireSuperAdmin()}）。</p>
     *
     * @param body {@code {deptId, positionId}}
     */
    @PreAuthorize("@ss.hasPermi('business:position:list')")
    @PostMapping("/bindings")
    public AjaxResult addBinding(@RequestBody Map<String, Object> body) {
        Long deptId = toLong(body == null ? null : body.get("deptId"));
        Long positionId = toLong(body == null ? null : body.get("positionId"));
        return AjaxResult.success(positionService.addDeptBinding(deptId, positionId));
    }

    /**
     * 解绑（**超管专属**）。部门解绑到没有岗位时会被拒。
     */
    @PreAuthorize("@ss.hasPermi('business:position:list')")
    @DeleteMapping("/bindings/{id}")
    public AjaxResult removeBinding(@PathVariable Long id) {
        return toAjax(positionService.removeDeptBinding(id));
    }

    /** 前端 JSON 里数字可能是 Integer/Long/String，统一转 Long */
    private Long toLong(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? null : Long.valueOf(s);
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