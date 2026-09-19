package com.ruoyi.business.controller;

import com.github.pagehelper.PageInfo;
import com.ruoyi.business.domain.LearningTask;
import com.ruoyi.business.domain.TaskSubmission;
import com.ruoyi.business.service.ITaskService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 学习任务 Controller（P2）
 *
 * <p>能力口径：<b>超管只读</b>（isGlobalReadOnly 的既有边界不变）；部门管理员限本部门岗位（建/发布/结束/批阅）；
 * 实习生只能看与提交<b>自己被分配</b>的任务。所有范围与归属校验都在 Service 层。</p>
 *
 * <p>不加 {@code @PreAuthorize}：这些接口都对「已登录」开放，但数据范围由 Service 按角色过滤
 * （超管只读会被 requireDeptAdmin 挡住写操作），加权限串反而会把新角色挡在门外。</p>
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/business/task")
public class TaskController extends BaseController {

    @Autowired
    private ITaskService taskService;

    /**
     * 任务列表（超管看全部 / 部门管理员看本部门岗位）
     */
    @GetMapping("/list")
    public AjaxResult list(LearningTask query) {
        startPage();
        List<LearningTask> list = taskService.list(query);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("rows", list);
        data.put("total", new PageInfo<>(list).getTotal());
        return AjaxResult.success(data);
    }

    /**
     * 本部门在培实习生（含待转正）—— 「任务 / 通知 / 批阅」三页的「按实习生筛选」共用
     *
     * <p>刻意走业务侧而不是 {@code /system/user/list}：后者要 {@code system:user:list} 权限，
     * 部门管理员是 403，前端会拿到空列表（「指定人员」选择器就一直是空的）。</p>
     */
    @GetMapping("/trainees")
    public AjaxResult trainees() {
        return AjaxResult.success(taskService.deptTrainees());
    }

    /**
     * 新建 / 修改任务（部门管理员）
     */
    @PostMapping("/save")
    public AjaxResult save(@RequestBody LearningTask task) {
        return AjaxResult.success("已保存", taskService.save(task));
    }

    /**
     * 发布 / 结束（published 会展开分配并通知）
     */
    @PostMapping("/{id}/status")
    public AjaxResult changeStatus(@PathVariable("id") Long id, @RequestParam("status") String status) {
        taskService.changeStatus(id, status);
        return AjaxResult.success("PUBLISHED".equalsIgnoreCase(status) ? "已发布并通知到人" : "已结束");
    }

    /**
     * 我的任务（实习生）
     */
    @GetMapping("/my")
    public AjaxResult myTasks() {
        return AjaxResult.success(taskService.myTasks());
    }

    /**
     * 任务详情
     */
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable("id") Long id) {
        return AjaxResult.success(taskService.detail(id));
    }

    /**
     * 提交情况看板（含未提交者）
     */
    @GetMapping("/{id}/submissions")
    public AjaxResult submissions(@PathVariable("id") Long id) {
        return AjaxResult.success(taskService.assignmentBoard(id));
    }

    /**
     * 某个分配的历次提交（批阅记录）—— 重复提交的旧版本也能看到
     */
    @GetMapping("/assignment/{assignmentId}/submissions")
    public AjaxResult submissionHistory(@PathVariable("assignmentId") Long assignmentId) {
        return AjaxResult.success(taskService.submissionHistory(assignmentId));
    }

    /**
     * 提交作业（实习生）
     */
    @PostMapping("/{id}/submit")
    public AjaxResult submit(@PathVariable("id") Long id, @RequestBody TaskSubmission body) {
        return AjaxResult.success("已提交", taskService.submit(id, body));
    }

    /**
     * 批阅（部门管理员）：passFlag=PASS / REJECT
     */
    @PostMapping("/review")
    public AjaxResult review(@RequestBody TaskSubmission body,
                             @RequestParam(value = "passFlag", required = false) String passFlag) {
        taskService.review(body, passFlag);
        return AjaxResult.success("已完成批阅");
    }
}
