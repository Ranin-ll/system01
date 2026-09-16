package com.ruoyi.business.domain;

import lombok.Data;

import java.util.List;

/**
 * 参与考核交卷请求体
 */
@Data
public class AnswerSubmitBody {

    /** 题库ID */
    private Long bankId;

    /** 逐题作答 */
    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem {
        private Long questionId;
        private String userAnswer;
    }
}
