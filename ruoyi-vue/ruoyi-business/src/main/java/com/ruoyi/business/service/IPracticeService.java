package com.ruoyi.business.service;

import com.ruoyi.business.domain.PracticeRecord;

import java.util.List;
import java.util.Map;

/**
 * 模拟考核 Service
 */
public interface IPracticeService {

    /** 开始模拟考核：从本部门模拟题库随机抽 10 题，返回题目（不含答案）。 */
    Map<String, Object> startPractice();

    /** 提交模拟考核：自动判分并记录（含逐题明细），返回对错结果。 */
    Map<String, Object> submitPractice(Long bankId, List<Map<String, Object>> answers);

    /** 查询当前实习生的模拟考核记录。 */
    List<PracticeRecord> myRecords();

    /** 查询某条模拟考核记录的逐题明细（仅本人可见）。 */
    Map<String, Object> recordDetail(Long recordId);
}
