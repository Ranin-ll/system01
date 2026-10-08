package com.ruoyi.business.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 转正资格取数Mapper（只读，跨学习域 / 考核域 / 账号域）。
 *
 * <p><b>为什么单独起一个 Mapper</b>：转正 gate 要读「正式考核通过场次数」与「协议签署」，
 * 这两项分属考核域与注册域。写在转正自己的 Mapper 里可以集中说明口径，
 * 且不动那两个域的既有 Mapper（避免影响它们各自的在用查询）。</p>
 *
 * <p><b>通过场次口径</b>（与实习生端「考核成绩与转正」页一致）：
 * 一场正式考核 = 一个 {@code exam_id}；该场所有<b>已发布且已出分</b>的答卷
 * 全部 {@code pass_flag = 1}，才算这一场通过。取不到答卷的场次不计入。</p>
 *
 * @author ruoyi
 */
@Mapper
public interface PromotionGateMapper {

    /** 本人已通过的正式考核场次数。 */
    int selectPassedFormalExamTimes(@Param("userId") Long userId);

    /** 本人已产生正式答卷的场次数（分母，供前端展示「已参加 N 场」）。 */
    int selectFormalExamTimes(@Param("userId") Long userId);

    /**
     * 批量取「正式考核通过场次数 / 参加场次数」，按 userId 索引。
     * 返回 [{userId, passedTimes, examTimes}]，<b>无答卷的人不返回</b>（由 Service 补 0）。
     *
     * @param deptId 数据范围；null = 全部
     */
    List<Map<String, Object>> selectExamTimesByDept(@Param("deptId") Long deptId);

    /**
     * 证书签发所需的人员摘要：姓名 / 岗位编码 / 岗位名 / 部门名 / 培养状态。
     * 无此人时返回 null。
     */
    Map<String, Object> selectUserBrief(@Param("userId") Long userId);

    /** 协议签署状态：1 已签 / 0 未签；查不到返回 null。 */
    Integer selectProtocolStatus(@Param("userId") Long userId);
}
