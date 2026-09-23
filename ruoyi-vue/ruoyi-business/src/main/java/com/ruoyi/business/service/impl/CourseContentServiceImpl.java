package com.ruoyi.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.ruoyi.business.domain.Course;
import com.ruoyi.business.domain.CourseChapter;
import com.ruoyi.business.domain.CourseContentSaveBody;
import com.ruoyi.business.domain.StudyItem;
import com.ruoyi.business.domain.StudyRecord;
import com.ruoyi.business.mapper.CourseChapterMapper;
import com.ruoyi.business.mapper.CourseMapper;
import com.ruoyi.business.mapper.StudyItemMapper;
import com.ruoyi.business.mapper.StudyRecordMapper;
import com.ruoyi.business.service.ICourseContentService;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.file.MimeTypeUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 课程章节、资料和学习记录服务。文件内容不进 MySQL，只保存资源路径及元数据。 */
@Service
public class CourseContentServiceImpl extends ServiceImpl<CourseChapterMapper, CourseChapter>
        implements ICourseContentService {

    // 章节检测（QUIZ）已下线：学习单项只保留 文档 / 视频
    private static final Set<String> ITEM_TYPES = new HashSet<>(Arrays.asList("DOC", "VIDEO"));
    /**
     * 「文档 / 附件」类资料的允许扩展名 = {@link MimeTypeUtils#COURSE_ASSET_EXTENSION}（单一数据源）。
     * ⚠️ 上传白名单必须与下载放行一致（`CommonController.resourceDownload` 引用同一个常量），
     * 否则会出现「传得上去、下载回来 0 字节」。
     * 同步项：前端 `views/business/course/index.vue` 的 itemAccept 与大小文案、
     * `application.yml` 的 multipart / upload 上限（默认值按 3GB 配的）。
     */
    private static final String[] DOCUMENT_EXTENSIONS = MimeTypeUtils.COURSE_ASSET_EXTENSION;
    private static final String[] VIDEO_EXTENSIONS = {"mp4", "avi", "rmvb", "webm", "mov"};
    /** 文档 / 附件类单文件上限：3GB（安装包、镜像可能很大） */
    private static final long DOCUMENT_MAX_SIZE = 3L * 1024 * 1024 * 1024;
    private static final long VIDEO_MAX_SIZE = 500 * 1024 * 1024L;

    private final CourseChapterMapper chapterMapper;
    private final StudyItemMapper itemMapper;
    private final StudyRecordMapper recordMapper;
    private final CourseMapper courseMapper;

    public CourseContentServiceImpl(CourseChapterMapper chapterMapper, StudyItemMapper itemMapper,
                                    StudyRecordMapper recordMapper, CourseMapper courseMapper) {
        this.chapterMapper = chapterMapper;
        this.itemMapper = itemMapper;
        this.recordMapper = recordMapper;
        this.courseMapper = courseMapper;
    }

    @Override
    public List<CourseChapter> listContents(Long courseId) {
        getAccessibleCourse(courseId);
        List<CourseChapter> chapters = chapterMapper.selectByCourseId(courseId);
        Map<Long, List<StudyItem>> items = itemMapper.selectByCourseId(courseId).stream()
                .collect(Collectors.groupingBy(StudyItem::getChapterId));
        chapters.forEach(chapter -> chapter.setItems(items.getOrDefault(chapter.getId(), new java.util.ArrayList<>())));
        return chapters;
    }

    @Override
    @Transactional
    public void saveContents(Long courseId, CourseContentSaveBody body) {
        ensureDraftForWrite(courseId);
        List<CourseChapter> incoming = body == null || body.getChapters() == null
                ? java.util.Collections.emptyList() : body.getChapters();
        List<CourseChapter> existingChapters = chapterMapper.selectByCourseId(courseId);
        List<StudyItem> existingItems = itemMapper.selectByCourseId(courseId);
        Map<Long, CourseChapter> chapterMap = existingChapters.stream()
                .collect(Collectors.toMap(CourseChapter::getId, Function.identity()));
        Set<Long> keptChapters = new HashSet<>();
        Set<Long> keptItems = new HashSet<>();
        Date now = new Date();

        for (int chapterIndex = 0; chapterIndex < incoming.size(); chapterIndex++) {
            CourseChapter input = incoming.get(chapterIndex);
            validateChapter(input);
            CourseChapter chapter;
            if (input.getId() == null) {
                chapter = new CourseChapter();
                chapter.setCourseId(courseId);
                chapter.setChapterName(input.getChapterName().trim());
                chapter.setChapterIntro(normalizeChapterIntro(input.getChapterIntro()));
                chapter.setIsRequired(requiredValue(input.getIsRequired()));
                chapter.setSortNo(chapterIndex + 1);
                chapter.setDeleted(0);
                chapter.setCreateTime(now);
                chapter.setUpdateTime(now);
                chapterMapper.insert(chapter);
            } else {
                chapter = chapterMap.get(input.getId());
                if (chapter == null) throw new ServiceException("章节不存在或不属于当前课程");
                keptChapters.add(chapter.getId());
                chapter.setChapterName(input.getChapterName().trim());
                chapter.setChapterIntro(normalizeChapterIntro(input.getChapterIntro()));
                chapter.setIsRequired(requiredValue(input.getIsRequired()));
                chapter.setSortNo(chapterIndex + 1);
                chapter.setUpdateTime(now);
                chapterMapper.updateById(chapter);
            }
            keptChapters.add(chapter.getId());
            saveItems(courseId, chapter, input.getItems(), existingItems, keptItems, now);
        }

        for (CourseChapter chapter : existingChapters) {
            if (!keptChapters.contains(chapter.getId())) {
                chapter.setDeleted(1);
                chapter.setUpdateTime(now);
                chapterMapper.updateById(chapter);
            }
        }
        for (StudyItem item : existingItems) {
            if (!keptItems.contains(item.getId())) {
                item.setDeleted(1);
                item.setUpdateTime(now);
                itemMapper.updateById(item);
                deleteStoredAssetAfterCommit(item.getContentUrl());
            }
        }
    }

    private void saveItems(Long courseId, CourseChapter chapter, List<StudyItem> inputs,
                           List<StudyItem> existingItems, Set<Long> keptItems, Date now) {
        if (inputs == null) inputs = java.util.Collections.emptyList();
        Map<Long, StudyItem> itemMap = existingItems.stream().collect(Collectors.toMap(StudyItem::getId, Function.identity()));
        for (int itemIndex = 0; itemIndex < inputs.size(); itemIndex++) {
            StudyItem input = inputs.get(itemIndex);
            validateItem(input);
            StudyItem item;
            if (input.getId() == null) {
                item = new StudyItem();
                item.setChapterId(chapter.getId());
                item.setCreateTime(now);
                item.setDeleted(0);
                item.setSortNo(itemIndex + 1);
            } else {
                item = itemMap.get(input.getId());
                if (item == null || !chapter.getId().equals(item.getChapterId())) {
                    throw new ServiceException("学习资料不存在或章节归属不正确");
                }
                item.setSortNo(itemIndex + 1);
            }
            String oldContentUrl = item.getContentUrl();
            String oldItemType = item.getItemType();
            copyEditableItemFields(input, item);
            boolean typeChanged = oldItemType != null && !oldItemType.equals(item.getItemType());
            if (typeChanged) clearAssetFields(item);
            item.setChapterId(chapter.getId());
            item.setUpdateTime(now);
            if (item.getId() == null) itemMapper.insert(item); else itemMapper.updateById(item);
            if (typeChanged) deleteStoredAssetAfterCommit(oldContentUrl);
            keptItems.add(item.getId());
        }
    }

    @Override
    public CourseChapter addChapter(Long courseId, CourseChapter chapter) {
        ensureDraftForWrite(courseId);
        validateChapter(chapter);
        chapter.setId(null);
        chapter.setCourseId(courseId);
        chapter.setChapterName(chapter.getChapterName().trim());
        chapter.setChapterIntro(normalizeChapterIntro(chapter.getChapterIntro()));
        chapter.setSortNo(nextChapterSort(courseId));
        chapter.setIsRequired(requiredValue(chapter.getIsRequired()));
        chapter.setDeleted(0);
        chapterMapper.insert(chapter);
        return chapter;
    }

    @Override
    public void updateChapter(Long chapterId, CourseChapter input) {
        CourseChapter chapter = getAccessibleChapter(chapterId);
        ensureDraftForWrite(chapter.getCourseId());
        validateChapter(input);
        chapter.setChapterName(input.getChapterName().trim());
        chapter.setChapterIntro(normalizeChapterIntro(input.getChapterIntro()));
        chapter.setIsRequired(requiredValue(input.getIsRequired()));
        chapterMapper.updateById(chapter);
    }

    @Override
    @Transactional
    public void deleteChapter(Long chapterId) {
        CourseChapter chapter = getAccessibleChapter(chapterId);
        ensureDraftForWrite(chapter.getCourseId());
        chapter.setDeleted(1);
        chapterMapper.updateById(chapter);
        List<StudyItem> items = itemMapper.selectByCourseId(chapter.getCourseId());
        items.stream().filter(item -> chapterId.equals(item.getChapterId())).forEach(item -> {
            item.setDeleted(1);
            itemMapper.updateById(item);
            deleteStoredAssetAfterCommit(item.getContentUrl());
        });
    }

    @Override
    public StudyItem addItem(Long chapterId, StudyItem input) {
        CourseChapter chapter = getAccessibleChapter(chapterId);
        ensureDraftForWrite(chapter.getCourseId());
        validateItem(input);
        StudyItem item = new StudyItem();
        copyEditableItemFields(input, item);
        item.setChapterId(chapterId);
        item.setSortNo(nextItemSort(chapterId));
        item.setDeleted(0);
        itemMapper.insert(item);
        return item;
    }

    @Override
    public void updateItem(Long itemId, StudyItem input) {
        StudyItem item = getAccessibleItem(itemId);
        CourseChapter chapter = getAccessibleChapter(item.getChapterId());
        ensureDraftForWrite(chapter.getCourseId());
        validateItem(input);
        String oldContentUrl = item.getContentUrl();
        String oldItemType = item.getItemType();
        copyEditableItemFields(input, item);
        boolean typeChanged = oldItemType != null && !oldItemType.equals(item.getItemType());
        if (typeChanged) clearAssetFields(item);
        itemMapper.updateById(item);
        if (typeChanged) deleteStoredAssetAfterCommit(oldContentUrl);
    }

    @Override
    public void deleteItem(Long itemId) {
        StudyItem item = getAccessibleItem(itemId);
        CourseChapter chapter = getAccessibleChapter(item.getChapterId());
        ensureDraftForWrite(chapter.getCourseId());
        item.setDeleted(1);
        itemMapper.updateById(item);
        deleteStoredAssetAfterCommit(item.getContentUrl());
    }

    @Override
    public String uploadAsset(Long itemId, MultipartFile file, Integer mediaSeconds) {
        if (file == null || file.isEmpty()) throw new ServiceException("请选择要上传的文件");
        StudyItem item = getAccessibleItem(itemId);
        CourseChapter chapter = getAccessibleChapter(item.getChapterId());
        ensureDraftForWrite(chapter.getCourseId());
        boolean video = "VIDEO".equals(item.getItemType());
        String[] extensions = video ? VIDEO_EXTENSIONS : DOCUMENT_EXTENSIONS;
        long maxSize = video ? VIDEO_MAX_SIZE : DOCUMENT_MAX_SIZE;
        String path = null;
        String oldPath = item.getContentUrl();
        try {
            path = FileUploadUtils.upload(RuoYiConfig.getUploadPath(), file, extensions, maxSize);
            item.setContentUrl(path);
            item.setFileName(file.getOriginalFilename());
            item.setFileSize(file.getSize());
            item.setFileExt(FileUploadUtils.getExtension(file).toLowerCase());
            item.setUpdateTime(new Date());
            // 视频真实时长（秒）：探测到就把「预计时长」一并校正成真实长度，
            // 否则列表里写 10 分钟、视频其实 35 秒，进度也算不对。
            if (video && mediaSeconds != null && mediaSeconds > 0) {
                item.setMediaSeconds(mediaSeconds);
                item.setDuration(Math.max(1, (int) Math.ceil(mediaSeconds / 60.0)));
            }
            if (item.getCompletionThreshold() == null && "VIDEO".equals(item.getItemType())) item.setCompletionThreshold(100);
            if (itemMapper.updateById(item) != 1) throw new ServiceException("学习资料更新失败");
            deleteStoredAssetAfterCommit(oldPath);
            return path;
        } catch (Exception e) {
            deleteStoredAsset(path);
            throw new ServiceException("文件上传失败：" + e.getMessage());
        }
    }

    @Override
    public List<StudyRecord> listStudyRecords(Long courseId, String status) {
        getAccessibleCourse(courseId);
        return recordMapper.selectCourseRecords(courseId, status, currentScopeDeptId());
    }

    private Course getAccessibleCourse(Long courseId) {
        if (courseId == null) throw new ServiceException("课程ID不能为空");
        Course course = courseMapper.selectCourseById(courseId, currentScopeDeptId());
        if (course == null) throw new ServiceException("课程不存在或无权访问");
        return course;
    }

    private CourseChapter getAccessibleChapter(Long chapterId) {
        if (chapterId == null) throw new ServiceException("章节ID不能为空");
        CourseChapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null || chapter.getDeleted() != null && chapter.getDeleted() == 1) {
            throw new ServiceException("章节不存在");
        }
        getAccessibleCourse(chapter.getCourseId());
        return chapter;
    }

    private StudyItem getAccessibleItem(Long itemId) {
        if (itemId == null) throw new ServiceException("学习资料ID不能为空");
        StudyItem item = itemMapper.selectById(itemId);
        if (item == null || item.getDeleted() != null && item.getDeleted() == 1) throw new ServiceException("学习资料不存在");
        getAccessibleChapter(item.getChapterId());
        return item;
    }

    private void ensureDraftForWrite(Long courseId) {
        managerScopeDeptId();
        Course course = getAccessibleCourse(courseId);
        if (!"DRAFT".equals(course.getStatus()) && !"DISABLED".equals(course.getStatus())) {
            throw new ServiceException("已发布课程请先停用，再修改课程内容");
        }
    }

    private Long currentScopeDeptId() {
        if (isGlobalReadOnly()) return null;
        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null) throw new ServiceException("当前账号未配置部门，无法访问课程数据");
        return deptId;
    }

    /**
     * 写操作的部门校验。
     *
     * <p><b>超管直接放行</b>（不限部门）—— 2026-09-20 规则调整：超管可管理全部课程与题库，
     * 与 {@code CourseServiceImpl#managerScopeDeptId()} 口径一致（那边返回 null 表示不限部门）。
     * 其余账号仍要求已配置部门。</p>
     */
    private void managerScopeDeptId() {
        if (isGlobalReadOnly()) {
            return;
        }
        if (SecurityUtils.getDeptId() == null) {
            throw new ServiceException("当前账号未配置部门，无法管理课程");
        }
    }

    private boolean isGlobalReadOnly() {
        return SecurityUtils.hasRole("SUPER_ADMIN") || SecurityUtils.isAdmin(SecurityUtils.getUserId());
    }

    private int nextChapterSort(Long courseId) {
        return chapterMapper.selectByCourseId(courseId).stream().mapToInt(c -> c.getSortNo() == null ? 0 : c.getSortNo()).max().orElse(0) + 1;
    }

    private int nextItemSort(Long chapterId) {
        CourseChapter chapter = getAccessibleChapter(chapterId);
        return itemMapper.selectByCourseId(chapter.getCourseId()).stream().filter(i -> chapterId.equals(i.getChapterId()))
                .mapToInt(i -> i.getSortNo() == null ? 0 : i.getSortNo()).max().orElse(0) + 1;
    }

    private void validateChapter(CourseChapter chapter) {
        if (chapter == null || chapter.getChapterName() == null || chapter.getChapterName().trim().isEmpty()) throw new ServiceException("章节名称不能为空");
        if (chapter.getChapterName().trim().length() > 128) throw new ServiceException("章节名称不能超过128个字符");
        if (chapter.getChapterIntro() != null && chapter.getChapterIntro().trim().length() > 300) throw new ServiceException("章节内容简介不能超过300个字符");
    }

    private String normalizeChapterIntro(String intro) {
        if (intro == null || intro.trim().isEmpty()) return null;
        return intro.trim();
    }

    private void validateItem(StudyItem item) {
        if (item == null || item.getItemTitle() == null || item.getItemTitle().trim().isEmpty()) throw new ServiceException("学习资料名称不能为空");
        if (item.getItemIntro() != null && item.getItemIntro().trim().length() > 500) throw new ServiceException("学习资料内容简介不能超过500个字符");
        if (!ITEM_TYPES.contains(item.getItemType())) throw new ServiceException("学习资料类型不合法");
        if (item.getDuration() != null && (item.getDuration() < 0 || item.getDuration() > 600)) throw new ServiceException("学习时长应在0到600分钟之间");
        if ("VIDEO".equals(item.getItemType())) {
            int threshold = item.getCompletionThreshold() == null ? 100 : item.getCompletionThreshold();
            if (threshold < 80 || threshold > 100) throw new ServiceException("视频完成阈值应在80%到100%之间");
            item.setCompletionThreshold(threshold);
        }
    }

    /** 文件地址及元数据只能由上传接口写入，普通编辑请求不得覆盖。 */
    private void copyEditableItemFields(StudyItem source, StudyItem target) {
        target.setItemTitle(source.getItemTitle().trim());
        target.setItemIntro(normalizeItemIntro(source.getItemIntro()));
        target.setItemType(source.getItemType());
        target.setDuration(source.getDuration() == null ? 0 : source.getDuration());
        target.setIsRequired(requiredValue(source.getIsRequired()));
        target.setCompletionRule(source.getCompletionRule() == null ? defaultRule(source.getItemType()) : source.getCompletionRule());
        // 章节检测已下线：不再写入测试内容（历史数据的 quiz_json 保持原样，不再使用）
        target.setQuizJson(null);
        target.setCompletionThreshold("VIDEO".equals(source.getItemType()) ? (source.getCompletionThreshold() == null ? 100 : source.getCompletionThreshold()) : null);
        // 视频真实时长（秒）：管理端上传时探测写入；非视频资料一律置空
        target.setMediaSeconds("VIDEO".equals(source.getItemType()) && source.getMediaSeconds() != null
                ? Math.max(0, source.getMediaSeconds()) : null);
    }

    private void clearAssetFields(StudyItem item) {
        item.setContentUrl(null);
        item.setFileName(null);
        item.setFileSize(null);
        item.setFileExt(null);
        // 换了文件/换了类型，旧的真实时长就不再适用（等新文件上传时重新探测）
        item.setMediaSeconds(null);
    }

    private void deleteStoredAssetAfterCommit(String contentUrl) {
        if (contentUrl == null || contentUrl.trim().isEmpty()) return;
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteStoredAsset(contentUrl);
                }
            });
        } else {
            deleteStoredAsset(contentUrl);
        }
    }

    /** 只删除 profile 根目录内由本系统上传的文件，拒绝远程地址和路径穿越。 */
    private void deleteStoredAsset(String contentUrl) {
        if (contentUrl == null || !contentUrl.startsWith(Constants.RESOURCE_PREFIX + "/")) return;
        try {
            String relative = contentUrl.substring((Constants.RESOURCE_PREFIX + "/").length());
            Path root = Paths.get(RuoYiConfig.getProfile()).toAbsolutePath().normalize();
            Path asset = root.resolve(relative).normalize();
            if (asset.startsWith(root)) Files.deleteIfExists(asset);
        } catch (Exception ignored) {
            // File cleanup must not roll back successfully persisted course metadata.
        }
    }

    private String normalizeItemIntro(String intro) {
        if (intro == null || intro.trim().isEmpty()) return null;
        return intro.trim();
    }

    private String defaultRule(String type) { return "VIDEO".equals(type) ? "PLAY_TO_END" : "SCROLL_END"; }
    private Integer requiredValue(Integer value) { return value == null ? 1 : (value == 0 ? 0 : 1); }
}
