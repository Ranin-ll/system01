package com.ruoyi.business.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/** 课程章节下的文档、视频或章节测试。 */
@Data
@TableName("study_item")
public class StudyItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long chapterId;
    private String itemTitle;
    private String itemIntro;
    private String itemType;
    private String contentUrl;
    private Integer duration;
    private Integer isRequired;
    private String completionRule;
    private String quizJson;
    private Integer sortNo;
    private Date createTime;
    private Date updateTime;
    private Integer deleted;

    /** 文件元数据和视频阈值，配合增量 SQL 持久化。 */
    private String fileName;
    private Long fileSize;
    private String fileExt;
    private Integer completionThreshold;

    /**
     * 视频文件的**真实时长（秒）** —— 管理端上传时由浏览器读 `<video>` 元数据探测后写入。
     * 与 `duration`（预计时长，分钟）不是一回事：后者是人工填的估值，常与真实片长对不上。
     * 用途：① 列表/侧栏显示真实时长；② 学习进度按「已看秒数 / 视频秒数」计算，
     * 这样短片（如 35 秒）看完也能到 100%，不再被「每秒最多涨 2%」的固定口径卡住。
     */
    private Integer mediaSeconds;

    /** 前端本地草稿状态，不落库。 */
    @TableField(exist = false)
    private String assetStatus;

    /** 学习端上下文字段，不落库。 */
    @TableField(exist = false)
    private Long courseId;
    @TableField(exist = false)
    private String status;
    @TableField(exist = false)
    private java.math.BigDecimal progress;
    @TableField(exist = false)
    private Integer readConfirm;
    @TableField(exist = false)
    private Integer studyDuration;
    @TableField(exist = false)
    private Date lastStudyTime;
    @TableField(exist = false)
    private Date finishTime;
}
