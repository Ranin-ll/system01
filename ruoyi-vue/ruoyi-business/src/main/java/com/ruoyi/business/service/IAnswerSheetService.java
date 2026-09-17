package com.ruoyi.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.business.domain.AnswerSheet;
import com.ruoyi.business.domain.AnswerSheetItem;

import java.util.List;
import java.util.Map;

import java.math.BigDecimal;

/**
 * 答卷Service
 */
public interface IAnswerSheetService extends IService<AnswerSheet> {

    /** 实习生开始考核：创建答卷并抽题，返回题目列表（不含答案）。 */
    Map<String, Object> startExam(Long examId);

    /** 交卷：客观题自动判分，实操题待人工。 */
    Map<String, Object> submit(Long sheetId, List<Map<String, String>> answers);

    /** 实习生考核列表（含我的答卷结果）。examMode 为 FORMAL/PRACTICE，null 表示全部。 */
    List<Map<String, Object>> myExamList(String examMode);

    /** 管理员批改列表（部门实习生 + 答卷状态）。 */
    List<Map<String, Object>> gradingList(Long examId);

    /** 答卷详情（批改用，含逐题明细）。 */
    Map<String, Object> sheetDetail(Long sheetId);

    /** 批改：实操题人工打分。 */
    int grade(Long sheetId, List<AnswerSheetItem> items);

    /** 实操考核打分（暂存）：直接在答卷上记录人工分数与评语，状态置为批改中。 */
    int gradePractically(Long sheetId, BigDecimal manualScore, String manualComment);

    /** 发布成绩：计算总分与通过标记，状态置为已发布。disableExam 是否停用考核，publishUnanswered 未作答的是否发布为0分。 */
    int publishResult(Long examId, boolean disableExam, boolean publishUnanswered);
}
