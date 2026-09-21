package com.ruoyi.business.controller;

import com.ruoyi.business.service.ISuperPersonnelService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 超管「人员与账号信息管理」。
 *
 * <p><b>职责边界</b>：这里只提供原生接口做不到的两件事 ——
 * ① 基于 {@code sys_user} 全量的富查询（带部门 / 岗位 / 角色 / 培养状态 / 导师 / 预计入职）；
 * ② 业务列写入（{@code position_id} / {@code user_status} / {@code protocol_status} /
 * {@code mentor_*} / {@code expected_entry_date}）。</p>
 *
 * <p><b>账号级增删改</b>（账号 / 姓名 / 手机 / 邮箱 / 性别 / 部门 / 角色 / 密码 / 账号状态）
 * 直接复用 RuoYi 原生 {@code /system/user}：
 * 它已处理用户名 / 手机 / 邮箱唯一性、密码加密、角色重绑与 {@code user_id=1} 保护，
 * 重新实现一遍只会引入差异。超管本来就持有 {@code system:user:*} 全套权限。</p>
 *
 * <p><b>权限</b>：复用原生权限串（部门管理员不持有），越权拦截落在
 * {@code SuperPersonnelServiceImpl#requireSuperAdmin()}。</p>
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/business/super/personnel")
public class SuperPersonnelController extends BaseController {

    @Autowired
    private ISuperPersonnelService superPersonnelService;

    /** 人员列表（分页 + 筛选：keyword / deptId / roleKey / userStatus / status） */
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam Map<String, Object> query) {
        startPage();
        List<Map<String, Object>> list = superPersonnelService.selectPersonnelList(query);
        return getDataTable(list);
    }

    /** 单个人员详情（含 roleIds，供角色多选回显） */
    @PreAuthorize("@ss.hasPermi('system:user:query')")
    @GetMapping("/{userId}")
    public AjaxResult detail(@PathVariable("userId") Long userId) {
        return AjaxResult.success(superPersonnelService.selectPersonnelDetail(userId));
    }

    /** 人员看板聚合数据（顶部统计卡与分布条） */
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    @GetMapping("/summary")
    public AjaxResult summary() {
        return AjaxResult.success(superPersonnelService.selectSummary());
    }

    /**
     * 保存业务字段。只更新请求体里出现的键 ——
     * 传 {@code positionId: null} 表示清空岗位、传 {@code expectedEntryDate: null} 表示清空预计入职。
     */
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @Log(title = "人员与账号", businessType = BusinessType.UPDATE)
    @PutMapping("/business")
    public AjaxResult saveBusiness(@RequestBody Map<String, Object> body) {
        return AjaxResult.success(superPersonnelService.saveBusinessFields(body));
    }

    /**
     * 删除人员（逻辑删除）。
     * ★ 之所以不在前端直接调原生 {@code DELETE /system/user/{ids}}：这里要挂服务端守卫 ——
     * 内置超管账号与持有「超级管理员」角色的账号一律不可删，保证系统始终存在可用超管。
     * （前端置灰只是体验，真正的规则必须落在服务端。）
     */
    @PreAuthorize("@ss.hasPermi('system:user:remove')")
    @Log(title = "人员与账号", businessType = BusinessType.DELETE)
    @DeleteMapping("/{userId}")
    public AjaxResult remove(@PathVariable("userId") Long userId) {
        return toAjax(superPersonnelService.deletePersonnel(userId));
    }
}
