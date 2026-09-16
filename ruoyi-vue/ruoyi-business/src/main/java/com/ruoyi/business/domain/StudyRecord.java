package com.ruoyi.business.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/** 学习单项进度记录。 */
@Data
@TableName("study_record")
public class StudyRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long courseId;
    private Long chapterId;
    private Long itemId;
    private String itemType;
    private String status;
    private Date firstOpenTime;
    private Date startTime;
    private Date lastStudyTime;
    private Integer studyDuration;
    private BigDecimal progress;
    private Date finishTime;
    private String unfinishedReason;
    private Integer readConfirm;
    private Integer version;
    private Date createTime;
    private Date updateTime;

    @TableField(exist = false)
    private String studentName;
    @TableField(exist = false)
    private String positionName;
    @TableField(exist = false)
    private Integer completedItems;
    @TableField(exist = false)
    private Integer totalItems;
}
