package com.ruoyi.business.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 模拟考核模块 practice_module
 *
 * 「模块」是模拟考核的顶层分组：
 * <pre>
 *   模块 (practice_module)
 *     ├── 理论模拟考核 N 个（exam，exam_mode='PRACTICE' AND exam_type='THEORY'，exam.module_id）
 *     └── 实操模拟题   M 个（practice_subject.module_id）
 * </pre>
 *
 * 管理员先建模块，再在模块内上传理论考核与实操题；实习生端先选模块，
 * 再看模块内的考核列表。模块取代原「方向(direction)」的顶层分组地位。
 */
@Data
public class PracticeModule implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 模块ID */
    private Long id;

    /** 所属部门ID */
    private Long deptId;

    /** 模块名称 */
    private String name;

    /** 模块说明（一句话，卡片副标题） */
    private String description;

    /** 排序号（越小越靠前） */
    private Integer sortNo;

    /** 状态：1启用 0停用 */
    private Integer status;

    private String createBy;

    private Date createTime;

    private Date updateTime;

    private Integer deleted;

    /** 所属部门名称（展示字段） */
    private String deptName;

    /** 模块下实操题数量（展示字段） */
    private Integer subjectCount;

    /** 模块下理论模拟考核数量（展示字段） */
    private Integer examCount;

    /** 当前登录账号的数据范围部门（仅供查询使用，不落库） */
    private Long scopeDeptId;
}
