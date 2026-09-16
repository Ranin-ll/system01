package com.ruoyi.business.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** 课程编排整体保存请求。ID 为空表示新增，sortNo 由服务端按数组顺序重算。 */
@Data
public class CourseContentSaveBody implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<CourseChapter> chapters = new ArrayList<>();
}
