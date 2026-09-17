package com.ruoyi.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.business.domain.QuestionBank;

import java.util.List;

/**
 * 题库Service
 */
public interface IQuestionBankService extends IService<QuestionBank> {

    List<QuestionBank> selectBankList(QuestionBank bank);

    QuestionBank selectById(Long id);

    int insertBank(QuestionBank bank);

    int updateBank(QuestionBank bank);

    int deleteByIds(Long[] ids);
}
