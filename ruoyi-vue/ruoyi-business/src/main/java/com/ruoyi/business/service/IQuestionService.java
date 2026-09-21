package com.ruoyi.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.business.domain.AnswerSubmitBody;
import com.ruoyi.business.domain.Question;
import com.ruoyi.business.domain.QuestionImportResult;
import com.ruoyi.business.domain.QuestionImportRow;

import java.util.List;
import java.util.Map;

/**
 * 题目Service
 */
public interface IQuestionService extends IService<Question> {

    List<Question> selectQuestionList(Question question);

    Question selectById(Long id);

    int insertQuestion(Question question);

    int updateQuestion(Question question);

    int deleteByIds(Long[] ids);

    /** Excel 批量导入题目 */
    QuestionImportResult importQuestions(Long bankId, List<QuestionImportRow> rows);

    /**
     * 导出某题库的全部题目为「导入行」。
     *
     * <p>刻意复用 {@link QuestionImportRow}：导出的 Excel 与导入模板<b>同结构</b>，
     * 可以直接再导入到别的题库 —— 形成「导出 → 修改 → 导入」闭环，
     * 不必维护第二套表头（也就不用担心两边字段对不上）。</p>
     */
    List<QuestionImportRow> exportRows(Long bankId);

    /** 实习生抽题（不含答案和解析） */
    List<Question> previewQuestions(Long bankId, Integer limit);

    /** 实习生提交判分 */
    Map<String, Object> submitAnswers(AnswerSubmitBody body);
}