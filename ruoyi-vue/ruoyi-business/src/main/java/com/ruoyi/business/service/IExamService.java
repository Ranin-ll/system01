package com.ruoyi.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.business.domain.Exam;

import java.util.List;

/**
 * 考核Service
 */
public interface IExamService extends IService<Exam> {

    List<Exam> selectExamList(Exam exam);

    Exam selectById(Long id);

    int insertExam(Exam exam);

    int updateExam(Exam exam);

    int deleteByIds(Long[] ids);

    /** 发布考核：DRAFT → PUBLISHED */
    int publish(Long id);

    /** 修改考核启用/停用状态（PUBLISHED ↔ DISABLED） */
    int changeStatus(Long id, String status);
}
