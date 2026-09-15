package com.ruoyi.business.domain;

import lombok.Data;

/**
 * 实习生公开注册请求。
 */
@Data
public class InternRegisterBody {

    private String code;

    private String uuid;

    private String username;

    private String password;

    private String realName;

    private String idCard;

    private Long positionId;

    private String expectedEntryDate;
}
