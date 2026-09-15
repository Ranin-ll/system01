package com.ruoyi.business.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** 课程章节。 */
@Data
@TableName("course_chapter")
public class CourseChapter implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private String chapterName;
    private String chapterIntro;
    private Integer sortNo;
    private Integer isRequired;
    private Date createTime;
    private Date updateTime;
    private Integer deleted;

    /** 仅用于课程内容接口的嵌套响应，不落库。 */
    @TableField(exist = false)
    private List<StudyItem> items = new ArrayList<>();
}
