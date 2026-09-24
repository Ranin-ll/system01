package com.ruoyi.business.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 模拟考核实操题 practice_subject
 *
 * 管理员按「方向分组」发布实操题：题名 + 题干必填，方向 / 交付要求 / 开发约束 /
 * 建议用时 / 提交格式 / 命名规则 / 参考图 / 附件为可选。
 * 实习生进入模拟考核的实操题库后按方向浏览卡片、查看详情、下载附件即可，
 * 不需要上传作答，也不会提交给管理员查看。
 *
 * 说明：{@code devConstraints} 对应列 {@code dev_constraints}（不用 constraints，
 * 避免与 MySQL 关键字冲突）；{@code referenceImages} / {@code attachmentsJson}
 * 均为 JSON 字符串，形如 [{"name":"原型图.png","url":"/profile/upload/..."}]。
 */
@Data
public class PracticeSubject implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 所属部门ID */
    private Long deptId;

    /** 所属实操题库（question_bank.id，★ 已停用：题库概念退场，保留列不读写） */
    private Long bankId;

    /** 建议满分（挑进考核时带出默认分值） */
    private java.math.BigDecimal suggestScore;

    /** 章节（与理论题库口径统一） */
    private String chapter;

    /** 题名（卡片与详情页标题） */
    private String title;

    /** 方向分组，如「后端接口开发」 */
    private String direction;

    /** 方向说明（分组标题下的一句话） */
    private String directionDesc;

    /** 难度：EASY / MEDIUM / HARD */
    private String difficulty;

    /** 建议用时（分钟） */
    private Integer estimatedMinutes;

    /** 交付要求，每行一条（前端按换行渲染为有序/无序列表） */
    private String deliverables;

    /** 开发约束，每行一条 */
    private String devConstraints;

    /** 提交格式，如「.java 源码 + SQL 脚本 + .zip」 */
    private String submitFormat;

    /** 命名规则，如「MOCK_API-1_用户登录接口开发_v01.zip」 */
    private String namingRule;

    /** 参考图JSON：[{"name":"原型图.png","url":"/profile/upload/..."}] */
    private String referenceImages;

    /** 排序号（同方向内手工排序，越小越靠前） */
    private Integer sortNo;

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
