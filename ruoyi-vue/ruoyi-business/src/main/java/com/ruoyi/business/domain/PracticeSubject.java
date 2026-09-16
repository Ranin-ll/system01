package com.ruoyi.business.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 模拟考核实操题 practice_subject
 *
 * 管理员只负责发布题目（题干必填、附件可选且可多个）；实习生进入模拟考核的
 * 实操列表后查看题干、下载附件即可，不需要上传作答，也不会提交给管理员查看。
 */
@Data
public class PracticeSubject implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 所属部门ID */
    private Long deptId;

    /** 题干 */
    private String content;

    /** 附件列表JSON：[{"name":"说明.docx","url":"/profile/upload/..."}]，可为空 */
    private String attachmentsJson;

    /** 状态：1启用(已发布) 0停用 */
    private Integer status;

    private String createBy;

    private Date createTime;

    private Date updateTime;

    private Integer deleted;

    /** 所属部门名称（展示字段） */
    private String deptName;

    /** 当前登录账号的数据范围部门（仅供查询使用，不落库） */
    private Long scopeDeptId;
}
