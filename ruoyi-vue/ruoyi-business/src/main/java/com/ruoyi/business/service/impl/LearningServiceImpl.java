package com.ruoyi.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.business.domain.Course;
import com.ruoyi.business.domain.CourseChapter;
import com.ruoyi.business.domain.LearningProgressBody;
import com.ruoyi.business.domain.StudyItem;
import com.ruoyi.business.domain.StudyRecord;
import com.ruoyi.business.mapper.CourseChapterMapper;
import com.ruoyi.business.mapper.CourseMapper;
import com.ruoyi.business.mapper.StudyItemMapper;
import com.ruoyi.business.mapper.StudyRecordMapper;
import com.ruoyi.business.service.ILearningService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 实习生在线学习服务。课程可见范围和记录归属全部由当前登录用户决定。 */
@Service
public class LearningServiceImpl implements ILearningService {

    private static final int VIDEO_PROGRESS_PERCENT_PER_SECOND = 2;
    private static final int PROGRESS_GRACE_PERCENT = 1;
    private static final int DURATION_GRACE_SECONDS = 2;
    private static final int MIN_DOCUMENT_OPEN_SECONDS = 10;

    private final CourseMapper courseMapper;
    private final CourseChapterMapper chapterMapper;
    private final StudyItemMapper itemMapper;
    private final StudyRecordMapper recordMapper;

    public LearningServiceImpl(CourseMapper courseMapper, CourseChapterMapper chapterMapper,
                               StudyItemMapper itemMapper, StudyRecordMapper recordMapper) {
        this.courseMapper = courseMapper;
        this.chapterMapper = chapterMapper;
        this.itemMapper = itemMapper;
        this.recordMapper = recordMapper;
    }

    @Override
    public List<Course> listPublishedCourses() {
        ensureIntern();
        Long userId = SecurityUtils.getUserId();
        List<Course> courses = courseMapper.selectLearningCourses(userId);
        courses.forEach(course -> fillLearningContent(course, userId));
        return courses;
    }

    @Override
    public Course getPublishedCourse(Long courseId) {
        ensureIntern();
        Long userId = SecurityUtils.getUserId();
        Course course = courseMapper.selectLearningCourseById(courseId, userId);
        if (course == null) throw new ServiceException("课程不存在、已停用或不适用于当前岗位");
        fillLearningContent(course, userId);
        return course;
    }

    @Override
    @Transactional
    public StudyRecord saveProgress(Long itemId, LearningProgressBody body) {
        ensureIntern();
        if (body == null) throw new ServiceException("学习进度不能为空");
        Long userId = SecurityUtils.getUserId();
        StudyItem item = itemMapper.selectLearningItem(itemId, userId);
        if (item == null) throw new ServiceException("学习资料不存在、课程已停用或不适用于当前岗位");

        StudyRecord record = recordMapper.selectOne(new LambdaQueryWrapper<StudyRecord>()
                .eq(StudyRecord::getUserId, userId)
                .eq(StudyRecord::getItemId, itemId)
                .last("LIMIT 1"));
        Date now = new Date();
        if (record == null) {
            record = new StudyRecord();
            record.setUserId(userId);
            record.setCourseId(item.getCourseId());
            record.setChapterId(item.getChapterId());
            record.setItemId(item.getId());
            record.setItemType(item.getItemType());
            record.setStatus("IN_PROGRESS");
            record.setFirstOpenTime(now);
            record.setStartTime(now);
            record.setStudyDuration(0);
            record.setProgress(BigDecimal.ZERO);
            record.setReadConfirm(0);
            record.setVersion(0);
            record.setCreateTime(now);
        }

        // Refresh the record's ownership fields so edits to a course item do not leave stale item metadata.
        record.setCourseId(item.getCourseId());
        record.setChapterId(item.getChapterId());
        record.setItemId(item.getId());
        record.setItemType(item.getItemType());

        BigDecimal oldProgress = value(record.getProgress());
        long elapsedSeconds = elapsedSeconds(record, now);
        int oldDuration = record.getStudyDuration() == null ? 0 : record.getStudyDuration();
        int submittedDuration = Math.max(body.getStudyDuration() == null ? 0 : body.getStudyDuration(), 0);
        int duration = Math.max(oldDuration, Math.min(submittedDuration,
                safeAdd(oldDuration, elapsedSeconds + DURATION_GRACE_SECONDS)));

        // 视频真实时长（秒）已知时，进度按「已看秒数 / 视频秒数」算 —— 见下面的说明
        boolean videoLengthKnown = "VIDEO".equals(item.getItemType())
                && item.getMediaSeconds() != null && item.getMediaSeconds() > 0;

        BigDecimal progress = oldProgress;
        if ("VIDEO".equals(item.getItemType())) {
            if (videoLengthKnown) {
                // 真实时长已知 → 用「已观看秒数 / 视频真实秒数」算进度（2026-09-23）。
                // 为什么不再用「每秒最多涨 2%」：那条固定口径隐含「视频至少 50 秒」，
                // 短片（实测 35 秒）的自然增速是 2.9%/秒，会被钳住 → 看完也到不了 100%，
                // 于是出现「明明看完了，进度条却停在 60%、还判不成完成」。
                // 防作弊没有丢：studyDuration 已被上面的「真实在线时长」钳住，
                // 报不出没真正看过的秒数，进度也就无法凭空暴涨。
                progress = BigDecimal.valueOf(duration)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(item.getMediaSeconds()), 2, RoundingMode.HALF_UP)
                        .min(BigDecimal.valueOf(100))
                        .max(oldProgress);
            } else {
                // 真实时长缺失（旧资料）→ 沿用原来的「每秒最多涨 X%」兜底口径
                BigDecimal maxProgress = oldProgress.add(BigDecimal.valueOf(
                        elapsedSeconds * VIDEO_PROGRESS_PERCENT_PER_SECOND + PROGRESS_GRACE_PERCENT));
                progress = oldProgress.max(clamp(body.getProgress()).min(maxProgress)).min(BigDecimal.valueOf(100));
            }
        }

        int oldReadConfirm = record.getReadConfirm() == null ? 0 : record.getReadConfirm();
        boolean documentReadAllowed = elapsedSinceFirstOpen(record, now) >= MIN_DOCUMENT_OPEN_SECONDS;
        int readConfirm = oldReadConfirm;
        if ("DOC".equals(item.getItemType()) && documentReadAllowed
                && body.getReadConfirm() != null && body.getReadConfirm() == 1) {
            readConfirm = 1;
            progress = BigDecimal.valueOf(100);
        }

        boolean completed = "DONE".equals(record.getStatus());
        if ("DOC".equals(item.getItemType())) {
            completed = completed || (readConfirm == 1 && documentReadAllowed && Boolean.TRUE.equals(body.getCompleted()));
        } else if ("VIDEO".equals(item.getItemType())) {
            int threshold = item.getCompletionThreshold() == null ? 100 : item.getCompletionThreshold();
            boolean reachedThreshold = progress.compareTo(BigDecimal.valueOf(threshold)) >= 0;
            // 有真实时长的资料：必须真的看到阈值才算完成；
            // 没有真实时长的旧资料：`ended`（播放到底）本身即完成依据 —— 否则旧视频永远差那几个百分点。
            completed = completed || (Boolean.TRUE.equals(body.getCompleted()) && (reachedThreshold || !videoLengthKnown));
        }
        // 章节检测（QUIZ）已下线：不再处理测试类学习单项
        if (completed) progress = BigDecimal.valueOf(100);

        record.setProgress(progress.setScale(2, RoundingMode.HALF_UP));
        record.setStudyDuration(duration);
        record.setReadConfirm(readConfirm);
        record.setStatus(completed ? "DONE" : "IN_PROGRESS");
        record.setLastStudyTime(now);
        if (completed && record.getFinishTime() == null) record.setFinishTime(now);
        record.setVersion(record.getVersion() == null ? 0 : record.getVersion());
        record.setUpdateTime(now);
        recordMapper.upsertProgress(record);
        return recordMapper.selectOne(new LambdaQueryWrapper<StudyRecord>()
                .eq(StudyRecord::getUserId, userId)
                .eq(StudyRecord::getItemId, itemId)
                .last("LIMIT 1"));
    }

    private void fillLearningContent(Course course, Long userId) {
        List<CourseChapter> chapters = chapterMapper.selectByCourseId(course.getId());
        List<StudyItem> items = itemMapper.selectByCourseId(course.getId());
        Map<Long, List<StudyItem>> itemsByChapter = items.stream()
                .collect(Collectors.groupingBy(StudyItem::getChapterId));
        Map<Long, StudyRecord> records = new HashMap<>();
        for (StudyRecord record : recordMapper.selectUserCourseRecords(course.getId(), userId)) {
            records.put(record.getItemId(), record);
        }

        int completedItems = 0;
        int totalDuration = 0;
        BigDecimal progressTotal = BigDecimal.ZERO;
        Date lastStudyTime = null;
        for (CourseChapter chapter : chapters) {
            List<StudyItem> chapterItems = itemsByChapter.getOrDefault(chapter.getId(), new ArrayList<>());
            for (StudyItem item : chapterItems) {
                StudyRecord record = records.get(item.getId());
                item.setStatus(record == null ? "NOT_STARTED" : record.getStatus());
                item.setProgress(record == null ? BigDecimal.ZERO : value(record.getProgress()));
                item.setReadConfirm(record == null || record.getReadConfirm() == null ? 0 : record.getReadConfirm());
                item.setStudyDuration(record == null || record.getStudyDuration() == null ? 0 : record.getStudyDuration());
                item.setLastStudyTime(record == null ? null : record.getLastStudyTime());
                item.setFinishTime(record == null ? null : record.getFinishTime());
                totalDuration += item.getDuration() == null ? 0 : item.getDuration();
                progressTotal = progressTotal.add(item.getProgress());
                if ("DONE".equals(item.getStatus())) completedItems++;
                if (item.getLastStudyTime() != null && (lastStudyTime == null || item.getLastStudyTime().after(lastStudyTime))) {
                    lastStudyTime = item.getLastStudyTime();
                }
            }
            chapter.setItems(chapterItems);
        }
        int itemCount = items.size();
        int progress = itemCount == 0 ? 0 : progressTotal
                .divide(BigDecimal.valueOf(itemCount), 0, RoundingMode.HALF_UP).intValue();
        course.setChapters(chapters);
        course.setChapterCount(chapters.size());
        course.setItemCount(itemCount);
        course.setCompletedItems(completedItems);
        course.setDuration(totalDuration);
        course.setProgress(progress);
        course.setLastStudyTime(lastStudyTime);
    }

    private void ensureIntern() {
        if (!SecurityUtils.hasRole("PRE_TRAINEE") && !SecurityUtils.hasRole("FORMAL_TRAINEE")) {
            throw new ServiceException("当前账号不是实习生，无法访问在线学习");
        }
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal clamp(BigDecimal value) {
        if (value == null) return BigDecimal.ZERO;
        return value.max(BigDecimal.ZERO).min(BigDecimal.valueOf(100));
    }

    private long elapsedSeconds(StudyRecord record, Date now) {
        Date anchor = record.getLastStudyTime() != null ? record.getLastStudyTime() : record.getStartTime();
        if (anchor == null) return 0;
        return Math.max(0, (now.getTime() - anchor.getTime()) / 1000L);
    }

    private long elapsedSinceFirstOpen(StudyRecord record, Date now) {
        Date firstOpen = record.getFirstOpenTime();
        if (firstOpen == null) return 0;
        return Math.max(0, (now.getTime() - firstOpen.getTime()) / 1000L);
    }

    private int safeAdd(int value, long increment) {
        long result = Math.max(0L, value) + Math.max(0L, increment);
        return result > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
    }
}
