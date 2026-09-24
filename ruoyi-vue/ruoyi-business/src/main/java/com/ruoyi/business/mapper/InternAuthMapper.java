package com.ruoyi.business.mapper;

import com.ruoyi.business.domain.Position;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 注册流程所需的跨系统表查询。
 */
@Mapper
public interface InternAuthMapper {

    @Results(id = "positionResult", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "positionCode", column = "position_code"),
            @Result(property = "positionName", column = "position_name"),
            @Result(property = "sortNo", column = "sort_no"),
            @Result(property = "status", column = "status"),
            @Result(property = "createBy", column = "create_by"),
            @Result(property = "createTime", column = "create_time"),
            @Result(property = "updateBy", column = "update_by"),
            @Result(property = "updateTime", column = "update_time"),
            @Result(property = "deleted", column = "deleted"),
            @Result(property = "deptId", column = "dept_id"),
            @Result(property = "deptName", column = "dept_name")
    })
    @Select("SELECT p.id, p.position_code, p.position_name, p.sort_no, p.status, p.create_by, p.create_time, "
            + "p.update_by, p.update_time, p.deleted, dp.dept_id, d.dept_name "
            + "FROM position p "
            + "LEFT JOIN dept_position dp ON dp.position_id = p.id AND dp.status = 1 "
            + "LEFT JOIN sys_dept d ON d.dept_id = dp.dept_id AND d.del_flag = '0' AND d.status = '0' "
            + "WHERE p.status = 1 AND p.deleted = 0 ORDER BY p.sort_no ASC, p.id ASC")
    List<Position> selectEnabledPositions();

    @Results(id = "positionByIdResult", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "positionCode", column = "position_code"),
            @Result(property = "positionName", column = "position_name"),
            @Result(property = "sortNo", column = "sort_no"),
            @Result(property = "status", column = "status"),
            @Result(property = "createBy", column = "create_by"),
            @Result(property = "createTime", column = "create_time"),
            @Result(property = "updateBy", column = "update_by"),
            @Result(property = "updateTime", column = "update_time"),
            @Result(property = "deleted", column = "deleted"),
            @Result(property = "deptId", column = "dept_id"),
            @Result(property = "deptName", column = "dept_name")
    })
    @Select("SELECT p.id, p.position_code, p.position_name, p.sort_no, p.status, p.create_by, p.create_time, "
            + "p.update_by, p.update_time, p.deleted, dp.dept_id, d.dept_name "
            + "FROM position p "
            + "LEFT JOIN dept_position dp ON dp.position_id = p.id AND dp.status = 1 "
            + "LEFT JOIN sys_dept d ON d.dept_id = dp.dept_id AND d.del_flag = '0' AND d.status = '0' "
            + "WHERE p.id = #{positionId} AND p.status = 1 AND p.deleted = 0 LIMIT 1")
    Position selectEnabledPositionById(@Param("positionId") Long positionId);

    @Select("SELECT role_id FROM sys_role WHERE role_key = #{roleKey} AND status = '0' AND del_flag = '0' LIMIT 1")
    Long selectRoleIdByKey(@Param("roleKey") String roleKey);

    @Select("SELECT dept_id FROM dept_position WHERE position_id = #{positionId} AND status = 1 LIMIT 1")
    Long selectDeptIdByPositionId(@Param("positionId") Long positionId);

    /**
     * 统计该身份证号是否已被**其他登录账号**占用（>0 即占用）。
     *
     * 同时查「申请单」与「账号」两处：注册会同时写这两张表，任一命中都算占用。
     * 之所以要传本账号并把 `login_account != 本账号` 作为条件，是为了放行
     * 「同一个账号自己的申请单」—— 例如被驳回后重新提交（沿用原手机号）时，
     * 库里本来就有该身份证，不能因此把自己拦掉。
     */
    @Select("SELECT (SELECT COUNT(1) FROM register_application WHERE id_card = #{idCard} AND login_account != #{loginAccount})"
            + " + (SELECT COUNT(1) FROM sys_user WHERE id_card = #{idCard} AND user_name != #{loginAccount} AND del_flag = '0')")
    int countOtherAccountByIdCard(@Param("idCard") String idCard, @Param("loginAccount") String loginAccount);

    @Update("UPDATE sys_user SET position_id = #{positionId}, user_status = #{userStatus}, expected_entry_date = #{expectedEntryDate,jdbcType=DATE}, id_card = #{idCard} WHERE user_id = #{userId}")
    int updateRegistrationProfile(@Param("userId") Long userId, @Param("positionId") Long positionId,
            @Param("userStatus") String userStatus, @Param("expectedEntryDate") Date expectedEntryDate,
            @Param("idCard") String idCard);

    @Update("UPDATE sys_user SET nick_name = #{realName}, dept_id = #{deptId}, position_id = #{positionId}, "
            + "user_status = 'WAIT_AUDIT', status = '1', expected_entry_date = #{expectedEntryDate,jdbcType=DATE}, "
            + "id_card = #{idCard}, mentor_name = NULL, mentor_phone = NULL, update_time = NOW() WHERE user_id = #{userId}")
    int updateRejectedRegistrationProfile(@Param("userId") Long userId, @Param("realName") String realName,
            @Param("deptId") Long deptId, @Param("positionId") Long positionId,
            @Param("expectedEntryDate") Date expectedEntryDate, @Param("idCard") String idCard);

    @Update("UPDATE sys_user SET user_status = #{userStatus}, status = #{status}, mentor_name = #{mentorName}, mentor_phone = #{mentorPhone} "
            + "WHERE user_id = #{userId}")
    int updateAuditProfile(@Param("userId") Long userId, @Param("userStatus") String userStatus,
            @Param("status") String status, @Param("mentorName") String mentorName,
            @Param("mentorPhone") String mentorPhone);

    /**
     * 只改培养状态与账号状态，<b>不碰导师字段</b>（2026-09-23 新增）。
     *
     * <p>为什么需要它：导师已抽成独立主表（{@code mentor}），由「实习生管理页」
     * 分配。审核通过时**不再**填写导师，若继续走 {@link #updateAuditProfile} 并传
     * {@code null}，会把实习生身上可能已存在的导师信息**误清空**。</p>
     */
    @Update("UPDATE sys_user SET user_status = #{userStatus}, status = #{status}, update_time = NOW() "
            + "WHERE user_id = #{userId}")
    int updateAuditStatus(@Param("userId") Long userId, @Param("userStatus") String userStatus,
            @Param("status") String status);

    @Select("SELECT id, agreement_name AS agreementName, version_no AS versionNo, content "
            + "FROM agreement_template WHERE status = 'EFFECTIVE' "
            + "ORDER BY effective_time DESC, id DESC LIMIT 1")
    Map<String, Object> selectEffectiveAgreement();

    @Select("SELECT protocol_status FROM sys_user WHERE user_id = #{userId} AND del_flag = '0' LIMIT 1")
    Integer selectProtocolStatus(@Param("userId") Long userId);

    @Select("SELECT COUNT(1) FROM agreement_signature "
            + "WHERE user_id = #{userId} AND template_id = #{templateId} AND status = 1")
    int countAgreementSignature(@Param("userId") Long userId, @Param("templateId") Long templateId);

    @Insert("INSERT INTO agreement_signature "
            + "(user_id, template_id, agreement_name, version_no, sign_ip, terminal, sign_mode, signature_data, status, create_time) "
            + "VALUES (#{userId}, #{templateId}, #{agreementName}, #{versionNo}, #{signIp}, #{terminal}, 'MOUSE', #{signature}, 1, NOW())")
    int insertAgreementSignature(@Param("userId") Long userId, @Param("templateId") Long templateId,
            @Param("agreementName") String agreementName, @Param("versionNo") String versionNo,
            @Param("signIp") String signIp, @Param("terminal") String terminal,
            @Param("signature") String signature);

    @Update("UPDATE sys_user SET protocol_status = 1, update_by = #{updateBy}, update_time = NOW() "
            + "WHERE user_id = #{userId} AND del_flag = '0'")
    int updateProtocolStatus(@Param("userId") Long userId, @Param("updateBy") String updateBy);

    @Update("UPDATE agreement_template SET sign_count = sign_count + 1 WHERE id = #{templateId}")
    int incrementAgreementSignCount(@Param("templateId") Long templateId);
}
