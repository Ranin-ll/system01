package com.ruoyi.business.domain;

import lombok.Data;

/** Public application status query request. */
@Data
public class RegisterStatusQueryBody {

    private String phone;

    private String password;
}