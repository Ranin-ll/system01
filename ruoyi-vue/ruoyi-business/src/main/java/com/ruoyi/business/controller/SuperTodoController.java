package com.ruoyi.business.controller;

import com.ruoyi.business.domain.SuperUrgeBody;
import com.ruoyi.business.service.ISuperTodoService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 超管督办视图 —— 部门管理员清单 / 督办看板 / 一键催办
 *
 * <p><b>前缀说明</b>：新起 <code>/business/super</code>，与现有 17 个业务控制器零冲突，
 * 不改任何已接后端的 Controller（本项目的硬约束）。</p>
 *
 * <p><b>权限说明</b>：复用已有权限串，<b>不新增权限行、不改任何既有注解</b>。
 * 因为这两个权限串部门管理员也持有，所以「仅超管可见」的强校验放在
 * {@code SuperTodoServiceImpl#requireSuperAdmin()}。</p>
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/business/super")
public class SuperTodoController extends BaseController {

    @Autowired
    private ISuperTodoService superTodoService;

    /** 部门管理员清单（含各自四类待办量） */
    @PreAuthorize("@ss.hasPermi('business:bank:list')")
    @GetMapping("/dept-admins")
    public AjaxResult deptAdmins() {
        return success(superTodoService.deptAdmins());
    }

    /**
     * 督办看板：四类待办的汇总 + 明细
     *
     * @param todoType 可选 REGISTER / REVIEW / OVERDUE_TASK / STALLED_STUDY
     * @param deptId   可选
     * @param ownerId  可选
     */
    @PreAuthorize("@ss.hasPermi('business:bank:list')")
    @GetMapping("/todos")
    public AjaxResult todos(@RequestParam(required = false) String todoType,
                            @RequestParam(required = false) Long deptId,
                            @RequestParam(required = false) Long ownerId) {
        return success(superTodoService.board(todoType, deptId, ownerId));
    }

    /** 一键催办（写操作）—— 复用已通的定向通知通道 */
    @PreAuthorize("@ss.hasPermi('business:bank:edit')")
    @PostMapping("/urge")
    public AjaxResult urge(@RequestBody SuperUrgeBody body) {
        return success(superTodoService.urge(body == null ? null : body.getOwnerIds(),
                body == null ? null : body.getContent()));
    }
}
