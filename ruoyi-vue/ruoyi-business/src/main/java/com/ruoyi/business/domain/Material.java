package com.ruoyi.business.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 备考资料 material
 *
 * 管理员在上传界面「两卡片」：左侧填写资料（名称 / 类型 / 适用岗位 / 版本号 / 简介）
 * 并上传附件，右侧列出已上传资料；实习生端只读列表，支持在线预览与下载。
 *
 * 说明：{@code validFrom} / {@code validTo}（有效期）字段保留在表结构中，但按需求
 * 已不再使用（前端不再填写、后端不再参与过滤），写入时统一置空。
 */
@Data
public class Material implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 资料ID */
    private Long id;

    /** 资料名称 */
    private String materialName;

    /** 类型：DOCUMENT文档 / VIDEO视频 / MOCK_ENTRY模拟题入口 */
    private String materialType;

    /** 适用岗位ID（关联 position） */
    private Long positionId;

    /** 简介 */
    private String summary;

    /** 文件地址（/profile/upload/...） */
    private String fileUrl;

    /** 版本号 */
    private String versionNo;

    /** 是否公开：1公开 0不公开 */
    private Integer isPublic;

    /** 状态：DRAFT草稿 / PUBLISHED已发布 / DISABLED停用 */
    private String status;

    /** 生效时间（保留，暂不使用） */
    private Date validFrom;

    /** 失效时间（保留，暂不使用） */
    private Date validTo;

    private String createBy;

    private Date createTime;

    private Date updateTime;

    private Integer deleted;

    /** 适用岗位名称（展示字段） */
    @TableField(exist = false)
    private String positionName;

    /** 当前登录账号的数据范围部门（仅供查询使用，不落库） */
    @TableField(exist = false)
    private Long scopeDeptId;
}
