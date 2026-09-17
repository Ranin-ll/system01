package com.ruoyi.business.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 正式考核指定人员 exam_participant
 *
 * 该表为空 → 本部门全体在培实习生均可参加；
 * 有记录 → 仅名单内的实习生能在「正式考核」页看到该场次并作答。
 */
@Data
public class ExamParticipant implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long examId;

    private Long userId;

    private Date createTime;

    /** 展示字段：姓名 / 岗位 / 培养状态（查询填充，非表字段） */
    private String userName;
    private String positionName;
    private String userStatus;
}
