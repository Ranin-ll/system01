package com.ruoyi.business.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 题目批量导入结果
 */
@Data
public class QuestionImportResult {

    /** 总行数 */
    private int total;

    /** 成功数 */
    private int success;

    /** 失败数 */
    private int failed;

    /** 错误明细（第几行、原因） */
    private List<String> errors = new ArrayList<>();

    public void addError(int rowNum, String reason) {
        this.errors.add("第 " + rowNum + " 行：" + reason);
    }
}
