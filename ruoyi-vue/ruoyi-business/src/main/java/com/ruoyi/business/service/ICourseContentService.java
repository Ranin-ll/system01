package com.ruoyi.business.service;

import com.ruoyi.business.domain.CourseChapter;
import com.ruoyi.business.domain.CourseContentSaveBody;
import com.ruoyi.business.domain.StudyItem;
import com.ruoyi.business.domain.StudyRecord;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ICourseContentService {
    List<CourseChapter> listContents(Long courseId);
    void saveContents(Long courseId, CourseContentSaveBody body);
    CourseChapter addChapter(Long courseId, CourseChapter chapter);
    void updateChapter(Long chapterId, CourseChapter chapter);
    void deleteChapter(Long chapterId);
    StudyItem addItem(Long chapterId, StudyItem item);
    void updateItem(Long itemId, StudyItem item);
    void deleteItem(Long itemId);
    /**
     * @param mediaSeconds 视频真实时长（秒），由管理端在浏览器里读元数据探测后随文件一起提交；
     *                     文档传 null 即可。用它校正资料的「预计时长」并给学习进度算法提供真实片长。
     */
    String uploadAsset(Long itemId, MultipartFile file, Integer mediaSeconds);
    List<StudyRecord> listStudyRecords(Long courseId, String status);
}
