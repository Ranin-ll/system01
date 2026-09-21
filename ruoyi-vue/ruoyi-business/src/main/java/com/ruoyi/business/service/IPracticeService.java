package com.ruoyi.business.service;

import com.ruoyi.business.domain.PracticeRecord;

import java.util.List;
import java.util.Map;

/**
 * 模拟考核 Service
 */
public interface IPracticeService {

    /**
     * 开始模拟考核：按「模块下选定的理论模拟考核」抽题（examId 为空时回退为本部门最近发布的一套配置）。
     * 返回题目（不含答案）与本次配置口径。
     */
    Map<String, Object> startPractice(Long examId);

    /**
     * 提交模拟考核：自动判分并记录（含逐题明细），返回对错结果。
     * examId 需与开考时一致，保证判分口径（各题型分值/通过线）相同。
     */
    Map<String, Object> submitPractice(Long examId, Long bankId, List<Map<String, Object>> answers);

    /**
     * 实习生端：本部门某个模块下「已发布」的理论模拟考核列表（用于先选模块再选考核）。
     * moduleId 为空时返回本部门全部已发布的理论模拟考核。
     */
    List<Map<String, Object>> listPracticeExams(Long moduleId);

    /** 查询当前实习生的模拟考核记录。moduleId 非空时只返回该模块下的记录（模块页用）。 */
    List<PracticeRecord> myRecords(Long moduleId);

    /** 查询某条模拟考核记录的逐题明细（仅本人可见）。 */
    Map<String, Object> recordDetail(Long recordId);
}
