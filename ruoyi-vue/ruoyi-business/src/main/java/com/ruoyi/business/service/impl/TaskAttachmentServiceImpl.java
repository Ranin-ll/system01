package com.ruoyi.business.service.impl;

import com.ruoyi.business.domain.LearningTask;
import com.ruoyi.business.domain.TaskAttachment;
import com.ruoyi.business.mapper.LearningTaskMapper;
import com.ruoyi.business.mapper.TaskAssignmentMapper;
import com.ruoyi.business.mapper.TaskAttachmentMapper;
import com.ruoyi.business.service.ITaskAttachmentService;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 学习任务附件 Service 实现
 *
 * @author ruoyi
 */
@Service
public class TaskAttachmentServiceImpl implements ITaskAttachmentService {

    /**
     * 资料白名单：文档 / 表格 / 演示 / 压缩包 / 图片。
     * 刻意<b>不含视频</b> —— 视频请走课程资料（有 500MB 的大文件通道），任务资料走 50MB 档。
     */
    private static final String[] ALLOWED_EXTENSIONS = {
            "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", "md", "csv",
            "zip", "rar", "7z", "png", "jpg", "jpeg", "gif"
    };

    private static final long MAX_SIZE = 50 * 1024 * 1024L;

    @Autowired
    private TaskAttachmentMapper attachmentMapper;

    @Autowired
    private LearningTaskMapper taskMapper;

    @Autowired
    private TaskAssignmentMapper assignmentMapper;

    // ------------------------------------------------------------------ 读

    @Override
    public List<TaskAttachment> list(Long taskId) {
        requireVisibleTask(taskId);
        return attachmentMapper.selectByTask(taskId);
    }

    @Override
    public List<TaskAttachment> myAttachments() {
        return attachmentMapper.selectByAssignee(SecurityUtils.getUserId());
    }

    // ------------------------------------------------------------------ 写

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskAttachment upload(Long taskId, MultipartFile file) {
        LearningTask task = requireManageable(taskId);
        if (LearningTask.STATUS_ENDED.equals(task.getStatus())) {
            throw new ServiceException("任务已结束，不能再补充资料");
        }
        TaskAttachment att = store(taskId, file);
        att.setUploaderId(SecurityUtils.getUserId());
        att.setDeleted(0);
        att.setCreateTime(new Date());
        try {
            attachmentMapper.insert(att);
        } catch (RuntimeException e) {
            // 落库失败 → 别把文件留在磁盘上成为孤儿（尽力清理，清不掉也不掩盖原异常）
            deleteQuietly(att.getFileUrl());
            throw e;
        }
        return attachmentMapper.selectById(att.getId());
    }

    @Override
    public TaskAttachment uploadSubmissionAsset(Long taskId, MultipartFile file) {
        if (!isIntern()) {
            throw new ServiceException("只有实习生可以上传作业附件");
        }
        LearningTask task = requireAssignedTask(taskId);
        if (!LearningTask.STATUS_PUBLISHED.equals(task.getStatus())) {
            throw new ServiceException("任务未发布或已结束，无法提交作业");
        }
        // 刻意不 insert：附件由随后的 submit 一起落库，避免「上传了但没提交」留下孤儿记录
        return store(taskId, file);
    }

    /** 校验 + 落盘 + 组装（<b>不写库</b>）：任务资料与作业附件共用同一条上传通道 */
    private TaskAttachment store(Long taskId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("请选择要上传的文件");
        }
        String original = file.getOriginalFilename();
        if (original == null || original.trim().isEmpty()) {
            throw new ServiceException("文件名不能为空");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new ServiceException("单个文件不能超过 50MB");
        }
        String ext = FileUploadUtils.getExtension(file).toLowerCase();
        if (Arrays.stream(ALLOWED_EXTENSIONS).noneMatch(ext::equals)) {
            throw new ServiceException("不支持的文件类型：" + ext);
        }
        String path;
        try {
            // 白名单 + 大小交给 FileUploadUtils 再兜一层（它才是最终裁决者）
            path = FileUploadUtils.upload(RuoYiConfig.getUploadPath(), file, ALLOWED_EXTENSIONS, MAX_SIZE);
        } catch (Exception e) {
            throw new ServiceException("文件上传失败：" + e.getMessage());
        }
        TaskAttachment att = new TaskAttachment();
        att.setTaskId(taskId);
        att.setFileName(original.length() > 250 ? original.substring(0, 250) : original);
        att.setFileUrl(path);
        att.setFileSize(file.getSize());
        att.setFileExt(ext);
        return att;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        if (id == null) {
            throw new ServiceException("资料ID不能为空");
        }
        TaskAttachment att = attachmentMapper.selectById(id);
        if (att == null || Integer.valueOf(1).equals(att.getDeleted())) {
            throw new ServiceException("资料不存在或已删除");
        }
        requireManageable(att.getTaskId());
        if (attachmentMapper.softDelete(id) <= 0) {
            throw new ServiceException("资料不存在或已删除");
        }
    }

    // ------------------------------------------------------------------ 内部

    /** 任务存在且岗位属于本部门；同时要求调用者是部门管理员（超管只读） */
    private LearningTask requireManageable(Long taskId) {
        if (taskId == null) {
            throw new ServiceException("任务ID不能为空");
        }
        if (isSuper()) {
            throw new ServiceException("超级管理员仅可查看，不能上传或删除资料");
        }
        if (isIntern()) {
            // 措辞对上传/删除都成立 —— 这里是两条链路共用的入口
            throw new ServiceException("实习生无权管理任务资料（资料由部门管理员维护）");
        }
        LearningTask task = taskMapper.selectTaskDetail(taskId);
        if (task == null) {
            throw new ServiceException("任务不存在或已删除");
        }
        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null) {
            throw new ServiceException("当前账号未配置部门");
        }
        if (task.getPositionId() == null || taskMapper.countDeptPosition(deptId, task.getPositionId()) == 0) {
            throw new ServiceException("该岗位不属于你的部门");
        }
        return task;
    }

    /** 可见性口径与任务域一致：超管可看；实习生限已分配的、且任务非草稿 */
    private LearningTask requireVisibleTask(Long taskId) {
        if (taskId == null) {
            throw new ServiceException("任务ID不能为空");
        }
        LearningTask task = taskMapper.selectTaskDetail(taskId);
        if (task == null) {
            throw new ServiceException("任务不存在或已删除");
        }
        if (isSuper()) {
            return task;
        }
        if (isIntern()) {
            if (LearningTask.STATUS_DRAFT.equals(task.getStatus())) {
                throw new ServiceException("任务尚未发布");
            }
            if (assignmentMapper.selectMine(taskId, SecurityUtils.getUserId()) == null) {
                throw new ServiceException("该任务未分配给你");
            }
        }
        return task;
    }

    /** 实习生：任务存在 + 已分配给我（作业附件上传用，不检查草稿态，由调用方按状态判定） */
    private LearningTask requireAssignedTask(Long taskId) {
        if (taskId == null) {
            throw new ServiceException("任务ID不能为空");
        }
        LearningTask task = taskMapper.selectTaskDetail(taskId);
        if (task == null) {
            throw new ServiceException("任务不存在或已删除");
        }
        if (assignmentMapper.selectMine(taskId, SecurityUtils.getUserId()) == null) {
            throw new ServiceException("该任务未分配给你");
        }
        return task;
    }

    /** 只删除 profile 根目录内由本系统上传的文件，拒绝远程地址和路径穿越（与课程资料同一套防护） */
    private void deleteQuietly(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith(Constants.RESOURCE_PREFIX + "/")) {
            return;
        }
        try {
            String relative = fileUrl.substring((Constants.RESOURCE_PREFIX + "/").length());
            Path root = Paths.get(RuoYiConfig.getProfile()).toAbsolutePath().normalize();
            Path asset = root.resolve(relative).normalize();
            if (asset.startsWith(root)) {
                Files.deleteIfExists(asset);
            }
        } catch (Exception ignored) {
            // 清理失败不影响主流程
        }
    }

    private boolean isSuper() {
        return SecurityUtils.hasRole("SUPER_ADMIN") || SecurityUtils.isAdmin(SecurityUtils.getUserId());
    }

    private boolean isIntern() {
        return SecurityUtils.hasRole("PRE_TRAINEE") || SecurityUtils.hasRole("FORMAL_TRAINEE");
    }
}
