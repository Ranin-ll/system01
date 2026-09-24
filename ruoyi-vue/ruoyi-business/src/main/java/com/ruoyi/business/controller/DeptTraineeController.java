package com.ruoyi.business.controller;

import com.ruoyi.business.mapper.DeptTraineeMapper;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 部门在培实习生公共查询。
 *
 * <p>供「通知管理」与 TraineeSelect 公共选择器共用。</p>
 */
@RestController
@RequestMapping("/business/dept")
public class DeptTraineeController extends BaseController {

    @Autowired
    private DeptTraineeMapper deptTraineeMapper;

    @GetMapping("/trainees")
    public AjaxResult trainees() {
        Long deptId = isSuper() ? null : SecurityUtils.getDeptId();
        return AjaxResult.success(deptTraineeMapper.selectDeptTrainees(deptId));
    }

    private boolean isSuper() {
        Long uid = SecurityUtils.getUserId();
        return SecurityUtils.hasRole("SUPER_ADMIN") || SecurityUtils.isAdmin(uid);
    }
}