package com.ruoyi.business.controller;

import com.ruoyi.business.domain.TaskAttachment;
import com.ruoyi.business.service.ITaskAttachmentService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 学习任务附件 Controller（任务资料：部门管理员上传 → 实习生下载）
 *
 * <p><b>独立前缀</b>（`/business/task-attachment`），不动已接后端的 `/business/task`。</p>
 *
 * <p>⚠️ 上传路径 `/business/task-attachment/upload` 必须与
 * {@code MultipartRequestSizeFilter} 里的大文件白名单<b>逐字一致</b> ——
 * 该过滤器默认把非白名单的 multipart 请求卡在 20MB，改了路径忘了同步过滤器，
 * 大文件会直接 413 而报错信息毫无指向。</p>
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/business/task-attachment")
public class TaskAttachmentController extends BaseController {

    @Autowired
    private ITaskAttachmentService attachmentService;

    /** 某任务的资料列表 */
    @GetMapping("/list")
    public AjaxResult list(@RequestParam("taskId") Long taskId) {
        return AjaxResult.success(attachmentService.list(taskId));
    }

    /** 实习生：我全部任务的资料（一次返回，前端本地按 taskId 分组） */
    @GetMapping("/my")
    public AjaxResult my() {
        return AjaxResult.success(attachmentService.myAttachments());
    }

    /** 上传资料（部门管理员） */
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("taskId") Long taskId, @RequestParam("file") MultipartFile file) {
        TaskAttachment att = attachmentService.upload(taskId, file);
        return AjaxResult.success("资料已上传", att);
    }

    /**
     * 实习生上传<b>作业附件</b>（只落盘、不落库）
     *
     * <p>返回的 {@code fileUrl / fileName} 由前端随 {@code POST /business/task/{id}/submit}
     * 一起提交 —— 这样「已接后端的提交接口」完全不用动。</p>
     */
    @PostMapping("/upload-submission")
    public AjaxResult uploadSubmission(@RequestParam("taskId") Long taskId, @RequestParam("file") MultipartFile file) {
        TaskAttachment att = attachmentService.uploadSubmissionAsset(taskId, file);
        return AjaxResult.success("附件已上传", att);
    }

    /** 删除资料（部门管理员，软删留痕） */
    @PostMapping("/{id}/delete")
    public AjaxResult delete(@PathVariable("id") Long id) {
        attachmentService.remove(id);
        return AjaxResult.success("资料已删除");
    }
}
