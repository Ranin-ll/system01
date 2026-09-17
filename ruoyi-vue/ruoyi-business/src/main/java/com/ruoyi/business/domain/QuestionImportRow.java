package com.ruoyi.business.domain;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 题目批量导入行（Excel 模板）
 */
@Data
public class QuestionImportRow {

    @ExcelProperty(value = "题型", index = 0)
    private String qtypeText;

    @ExcelProperty(value = "题干", index = 1)
    private String stem;

    @ExcelProperty(value = "选项A", index = 2)
    private String optionA;

    @ExcelProperty(value = "选项B", index = 3)
    private String optionB;

    @ExcelProperty(value = "选项C", index = 4)
    private String optionC;

    @ExcelProperty(value = "选项D", index = 5)
    private String optionD;

    @ExcelProperty(value = "正确答案", index = 6)
    private String answer;

    @ExcelProperty(value = "解析", index = 7)
    private String analysis;

    @ExcelProperty(value = "难度", index = 8)
    private String difficultyText;

    @ExcelProperty(value = "知识点", index = 9)
    private String knowledgePoint;
}
