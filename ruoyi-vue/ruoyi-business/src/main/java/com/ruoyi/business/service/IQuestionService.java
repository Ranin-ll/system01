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

    /** 实习生抽题（不含答案和解析） */
    List<Question> previewQuestions(Long bankId, Integer limit);

    /** 实习生提交判分 */
    Map<String, Object> submitAnswers(AnswerSubmitBody body);
}