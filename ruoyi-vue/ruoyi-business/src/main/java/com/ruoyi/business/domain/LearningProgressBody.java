package com.ruoyi.business.domain;

import lombok.Data;

import java.math.BigDecimal;

/** 实习生学习进度提交参数。课程、章节和用户归属均由服务端反查。 */
@Data
public class LearningProgressBody {
    private BigDecimal progress;
    private Integer studyDuration;
    private Integer readConfirm;
    private Boolean completed;
}
