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
    String uploadAsset(Long itemId, MultipartFile file);
    List<StudyRecord> listStudyRecords(Long courseId, String status);
}
