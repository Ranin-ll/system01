package com.ruoyi.business.controller;

import com.ruoyi.business.domain.TaskPost;
import com.ruoyi.business.service.ITaskPostService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 学习任务讨论区 Controller
 *
 * <p>独立成一个前缀（而不是塞进 {@code TaskController}），好处是<b>不动已接后端的任务接口</b>；
 * 权限口径与任务域一致，由 Service 层判定，所以这里同样不加 {@code @PreAuthorize}。</p>
 *
 * <p>三个动作：列表 / 发帖（含回复）/ 删帖。<b>没有「改帖」</b> —— 讨论区内容应不可篡改，
 * 说错了只能删了重发，删除留痕。</p>
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/business/task-post")
public class TaskPostController extends BaseController {

    @Autowired
    private ITaskPostService postService;

    /**
     * 讨论区列表：{@code {enabled, ended, canPost, canModerate, rows, total}}
     */
    @GetMapping("/list")
    public AjaxResult list(@RequestParam("taskId") Long taskId) {
        return AjaxResult.success(postService.list(taskId));
    }

    /**
     * 发帖 / 回复（{@code parentId} 为空即主楼）
     */
    @PostMapping("/save")
    public AjaxResult save(@RequestBody TaskPost body) {
        Long id = postService.add(body.getTaskId(), body.getContent(), body.getParentId());
        return AjaxResult.success(body.getParentId() == null ? "已发布" : "已回复", id);
    }

    /**
     * 删帖（软删留痕；作者本人或部门管理员）
     */
    @PostMapping("/{id}/delete")
    public AjaxResult delete(@PathVariable("id") Long id) {
        postService.remove(id);
        return AjaxResult.success("已删除");
    }
}
