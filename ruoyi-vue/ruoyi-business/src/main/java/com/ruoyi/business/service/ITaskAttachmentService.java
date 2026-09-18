package com.ruoyi.business.service;

import com.ruoyi.business.domain.TaskAttachment;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 学习任务附件 Service
 *
 * <p>方向是<b>单向</b>的：部门管理员上传 → 实习生下载。
 * 所以上传统统要过「部门管理员 + 本部门岗位 + 任务未结束」三道闸，
 * 而下载列表只要「能看见这个任务」即可（复用任务域的可见性口径）。</p>
 *
 * <p>实习生回传作业仍走 {@code task_submission.file_url}（现有的「交作业」弹窗），
 * 两者刻意不合成一张表 —— 任务资料是发布方给的，作业是接收方交的，生命周期与权限都不同。</p>
 *
 * @author ruoyi
 */
public interface ITaskAttachmentService {

    /** 某任务的资料列表（部门管理员 / 超管 / 被分配的实习生都可看） */
    List<TaskAttachment> list(Long taskId);

    /** 上传资料（仅部门管理员，限本部门岗位，任务未结束） */
    TaskAttachment upload(Long taskId, MultipartFile file);

    /**
     * 实习生上传<b>作业附件</b> —— 只落盘并回传文件信息，<b>不写任何表</b>。
     *
     * <p>作业附件跟着某一次提交走（{@code task_submission.file_url/file_name}），
     * 由随后的 {@code POST /business/task/{id}/submit} 一起落库。
     * 所以这里刻意不建行 —— 否则「上传了但没点提交」会在库里留下孤儿附件记录。</p>
     *
     * @return 未持久化的 {@link TaskAttachment}，仅用作返回载体（fileName / fileUrl / fileSize / fileExt）
     */
    TaskAttachment uploadSubmissionAsset(Long taskId, MultipartFile file);

    /** 删除资料（仅部门管理员，软删留痕） */
    void remove(Long id);

    /**
     * 实习生「我的任务」的全部资料 —— 一次返回，前端本地按 taskId 分组，
     * 避免每张任务卡一个请求（N+1）。
     */
    List<TaskAttachment> myAttachments();
}
