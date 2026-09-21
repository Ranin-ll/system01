package com.ruoyi.business.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 一键催办的请求体
 *
 * @author ruoyi
 */
@Data
public class SuperUrgeBody implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 被催的部门管理员 userId 列表 */
    private List<Long> ownerIds;

    /** 可选，附加说明（后端会拼在自动生成的待办清单之后） */
    private String content;
}
