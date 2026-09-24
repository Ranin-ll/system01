package com.ruoyi.business.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 超管「人员与账号信息管理」的读取与业务字段写入。
 *
 * <p><b>为什么新起 Mapper 而不改原生 SysUserMapper</b>：
 * 原生 {@code selectUserList} 只 select 标准列（连 {@code user_status}/{@code mentor_*}
 * 都没有，见 SysUserMapper.xml），且 {@code insertUser}/{@code updateUser} 也不写
 * {@code position_id}/{@code user_status}/{@code protocol_status}/{@code mentor_*}/
 * {@code expected_entry_date} —— 这些业务列一直由项目自己的 Mapper 维护
 * （如 {@code InternAuthMapper.updateRegistrationProfile}）。
 * 所以这里补一套只读查询 + 业务字段写入，**不动框架的 SysUserMapper**。</p>
 *
 * <p>查询基于 {@code sys_user} 全量（不是 {@code register_application}）——
 * 后者只有报名记录，手工创建的账号（如建模部门）不在其中，会漏人。</p>
 */
@Mapper
public interface SuperPersonnelMapper {

    /**
     * 人员列表（分页由 PageHelper 在外层包裹）。
     * 关键词命中 账号 / 姓名 / 手机；部门按子树；角色按 role_key 存在性。
     */
    @Select("<script>"
            + "SELECT u.user_id AS userId, u.user_name AS userName, u.nick_name AS nickName,"
            + "       u.phonenumber AS phonenumber, u.email AS email, u.sex AS sex,"
            + "       u.status AS status, u.user_status AS userStatus,"
            + "       u.protocol_status AS protocolStatus,"
            + "       u.dept_id AS deptId, d.dept_name AS deptName,"
            + "       u.position_id AS positionId, p.position_name AS positionName, p.position_code AS positionCode,"
            + "       u.mentor_id AS mentorId, u.mentor_name AS mentorName, u.mentor_phone AS mentorPhone,"
            + "       u.expected_entry_date AS expectedEntryDate,"
            + "       u.is_dept_admin AS isDeptAdmin, u.fail_count AS failCount,"
            + "       u.create_time AS createTime, u.login_date AS loginDate,"
            + "       (SELECT GROUP_CONCAT(r.role_name ORDER BY r.role_id SEPARATOR '、')"
            + "          FROM sys_user_role ur JOIN sys_role r ON r.role_id = ur.role_id"
            + "         WHERE ur.user_id = u.user_id AND r.del_flag = '0') AS roleNames,"
            + "       (SELECT GROUP_CONCAT(r.role_key ORDER BY r.role_id SEPARATOR ',')"
            + "          FROM sys_user_role ur JOIN sys_role r ON r.role_id = ur.role_id"
            + "         WHERE ur.user_id = u.user_id AND r.del_flag = '0') AS roleKeys"
            + "  FROM sys_user u"
            + "  LEFT JOIN sys_dept d ON d.dept_id = u.dept_id"
            + "  LEFT JOIN position p ON p.id = u.position_id"
            + " WHERE u.del_flag = '0'"
            + " <if test=\"keyword != null and keyword != ''\">"
            + "   AND (u.user_name LIKE CONCAT('%', #{keyword}, '%')"
            + "        OR u.nick_name LIKE CONCAT('%', #{keyword}, '%')"
            + "        OR u.phonenumber LIKE CONCAT('%', #{keyword}, '%'))"
            + " </if>"
            + " <if test=\"deptId != null and deptId != 0\">"
            + "   AND (u.dept_id = #{deptId}"
            + "        OR u.dept_id IN (SELECT t.dept_id FROM sys_dept t WHERE FIND_IN_SET(#{deptId}, t.ancestors)))"
            + " </if>"
            + " <if test=\"userStatus != null and userStatus != ''\">AND u.user_status = #{userStatus}</if>"
            + " <if test=\"status != null and status != ''\">AND u.status = #{status}</if>"
            + " <if test=\"roleKey != null and roleKey != ''\">"
            + "   AND EXISTS (SELECT 1 FROM sys_user_role ur2 JOIN sys_role r2 ON r2.role_id = ur2.role_id"
            + "                WHERE ur2.user_id = u.user_id AND r2.role_key = #{roleKey})"
            + " </if>"
            + " ORDER BY u.user_id ASC"
            + "</script>")
    List<Map<String, Object>> selectPersonnelList(@Param("keyword") String keyword,
                                                 @Param("deptId") Long deptId,
                                                 @Param("userStatus") String userStatus,
                                                 @Param("status") String status,
                                                 @Param("roleKey") String roleKey);

    /** 单个人员详情（含 roleIds 逗号串，供前端角色多选回显） */
    @Select("SELECT u.user_id AS userId, u.user_name AS userName, u.nick_name AS nickName,"
            + "       u.phonenumber AS phonenumber, u.email AS email, u.sex AS sex,"
            + "       u.status AS status, u.user_status AS userStatus,"
            + "       u.protocol_status AS protocolStatus,"
            + "       u.dept_id AS deptId, d.dept_name AS deptName,"
            + "       u.position_id AS positionId, p.position_name AS positionName,"
            + "       u.mentor_id AS mentorId, u.mentor_name AS mentorName, u.mentor_phone AS mentorPhone,"
            + "       u.expected_entry_date AS expectedEntryDate,"
            + "       u.is_dept_admin AS isDeptAdmin, u.fail_count AS failCount,"
            + "       u.create_time AS createTime, u.login_date AS loginDate, u.remark AS remark,"
            + "       (SELECT GROUP_CONCAT(r.role_name ORDER BY r.role_id SEPARATOR '、')"
            + "          FROM sys_user_role ur JOIN sys_role r ON r.role_id = ur.role_id"
            + "         WHERE ur.user_id = u.user_id AND r.del_flag = '0') AS roleNames,"
            + "       (SELECT GROUP_CONCAT(r.role_key ORDER BY r.role_id SEPARATOR ',')"
            + "          FROM sys_user_role ur JOIN sys_role r ON r.role_id = ur.role_id"
            + "         WHERE ur.user_id = u.user_id AND r.del_flag = '0') AS roleKeys,"
            + "       (SELECT GROUP_CONCAT(ur.role_id SEPARATOR ',')"
            + "          FROM sys_user_role ur JOIN sys_role r ON r.role_id = ur.role_id"
            + "         WHERE ur.user_id = u.user_id AND r.del_flag = '0') AS roleIds"
            + "  FROM sys_user u"
            + "  LEFT JOIN sys_dept d ON d.dept_id = u.dept_id"
            + "  LEFT JOIN position p ON p.id = u.position_id"
            + " WHERE u.user_id = #{userId}")
    Map<String, Object> selectPersonnelDetail(@Param("userId") Long userId);

    /**
     * 写业务字段（原生 updateUser 不写这些列）。
     * 只更新传入的非 null 项；user_status / protocol_status 用 null 判断而非空串（它们是 NOT NULL 列）。
     */
    @Update("<script>"
            + "UPDATE sys_user"
            + "   <set>"
            + "     <if test=\"deptId != null\">dept_id = #{deptId},</if>"
            + "     <if test=\"positionId != null\">position_id = #{positionId},</if>"
            + "     <if test=\"clearPosition\">position_id = NULL,</if>"
            + "     <if test=\"userStatus != null and userStatus != ''\">user_status = #{userStatus},</if>"
            + "     <if test=\"protocolStatus != null\">protocol_status = #{protocolStatus},</if>"
            + "     <if test=\"mentorId != null\">mentor_id = #{mentorId},</if>"
            + "     <if test=\"clearMentor\">mentor_id = NULL,</if>"
            + "     <if test=\"mentorName != null\">mentor_name = #{mentorName},</if>"
            + "     <if test=\"mentorPhone != null\">mentor_phone = #{mentorPhone},</if>"
            + "     <if test=\"expectedEntryDate != null\">expected_entry_date = #{expectedEntryDate},</if>"
            + "     <if test=\"clearEntryDate\">expected_entry_date = NULL,</if>"
            + "     <if test=\"updateBy != null and updateBy != ''\">update_by = #{updateBy},</if>"
            + "     update_time = NOW()"
            + "   </set>"
            + " WHERE user_id = #{userId}"
            + "</script>")
    int updateBusinessFields(@Param("userId") Long userId,
                             @Param("deptId") Long deptId,
                             @Param("positionId") Long positionId,
                             @Param("clearPosition") boolean clearPosition,
                             @Param("userStatus") String userStatus,
                             @Param("protocolStatus") Integer protocolStatus,
                             @Param("mentorId") Long mentorId,
                             @Param("clearMentor") boolean clearMentor,
                             @Param("mentorName") String mentorName,
                             @Param("mentorPhone") String mentorPhone,
                             @Param("expectedEntryDate") Date expectedEntryDate,
                             @Param("clearEntryDate") boolean clearEntryDate,
                             @Param("updateBy") String updateBy);

    /** 新建账号后的兜底：把 user_status 从列默认值 WAIT_AUDIT 纠正为调用方指定值 */
    @Update("UPDATE sys_user SET user_status = #{userStatus}, update_time = NOW() WHERE user_id = #{userId}")
    int initUserStatus(@Param("userId") Long userId, @Param("userStatus") String userStatus);

    /**
     * 公司根部门（parent_id=0，即「融谷」）。
     * ★ 超管必须挂在这个部门下，不属于任何子部门 —— 所以不能写死 100，按层级动态取。
     */
    @Select("SELECT dept_id FROM sys_dept WHERE parent_id = 0 AND del_flag = '0' ORDER BY dept_id LIMIT 1")
    Long selectRootDeptId();

    /** 该用户当前是否持有「超级管理员」角色（有效角色） */
    @Select("SELECT COUNT(*) FROM sys_user_role ur JOIN sys_role r ON r.role_id = ur.role_id "
            + "WHERE ur.user_id = #{userId} AND r.role_key = 'SUPER_ADMIN' AND r.del_flag = '0'")
    int countSuperAdminRole(@Param("userId") Long userId);

    // ------------------------------------------------------------------ 看板聚合
    // 说明：全部用 SUM(CASE WHEN ... THEN 1 ELSE 0 END) 而不是 SUM(条件) ——
    //       前者在 WHERE 命中 0 行时也不会返回 NULL（后者会），省掉前端一堆空值判断。

    /** 看板主指标：总数 / 账号状态 / 培养状态 / 协议 / 是否落在根部门 / 是否缺岗位 */
    @Select("SELECT COUNT(*) AS total,"
            + " SUM(CASE WHEN u.status = '1' THEN 1 ELSE 0 END) AS accountDisabled,"
            + " SUM(CASE WHEN u.status = '0' THEN 1 ELSE 0 END) AS accountNormal,"
            + " SUM(CASE WHEN u.user_status = 'WAIT_AUDIT' THEN 1 ELSE 0 END) AS stWaitAudit,"
            + " SUM(CASE WHEN u.user_status = 'PRE_TRAINEE' THEN 1 ELSE 0 END) AS stPreTrainee,"
            + " SUM(CASE WHEN u.user_status = 'PENDING_PROMOTE' THEN 1 ELSE 0 END) AS stPendingPromote,"
            + " SUM(CASE WHEN u.user_status = 'FORMAL_TRAINEE' THEN 1 ELSE 0 END) AS stFormalTrainee,"
            + " SUM(CASE WHEN u.user_status = 'NON_INTERN' THEN 1 ELSE 0 END) AS stNonIntern,"
            + " SUM(CASE WHEN u.user_status = 'DISABLED' THEN 1 ELSE 0 END) AS stDisabled,"
            + " SUM(CASE WHEN u.user_status = 'ARCHIVED' THEN 1 ELSE 0 END) AS stArchived,"
            + " SUM(CASE WHEN u.protocol_status = 1 THEN 1 ELSE 0 END) AS protocolSigned,"
            + " SUM(CASE WHEN u.protocol_status <> 1 OR u.protocol_status IS NULL THEN 1 ELSE 0 END) AS protocolUnsigned"
            + " FROM sys_user u WHERE u.del_flag = '0'")
    Map<String, Object> selectPersonnelSummary();

    /** 看板：按有效角色分组的人数 */
    @Select("SELECT r.role_id AS roleId, r.role_key AS roleKey, r.role_name AS roleName, COUNT(*) AS cnt"
            + " FROM sys_user_role ur"
            + " JOIN sys_role r ON r.role_id = ur.role_id AND r.del_flag = '0'"
            + " JOIN sys_user u ON u.user_id = ur.user_id AND u.del_flag = '0'"
            + " GROUP BY r.role_id, r.role_key, r.role_name, r.role_sort"
            + " ORDER BY r.role_sort, r.role_id")
    List<Map<String, Object>> selectRoleCounts();

    /** 看板：无有效角色的账号数（数据质量告警 —— 这类账号登录后看不到任何菜单） */
    @Select("SELECT COUNT(*) FROM sys_user u WHERE u.del_flag = '0'"
            + " AND NOT EXISTS (SELECT 1 FROM sys_user_role ur JOIN sys_role r ON r.role_id = ur.role_id"
            + "                  AND r.del_flag = '0' WHERE ur.user_id = u.user_id)")
    int countNoRole();

    /** 看板：部门分布（公司根部门 + 其直属子部门），含 0 人的部门 */
    @Select("SELECT d.dept_id AS deptId, d.dept_name AS deptName, d.parent_id AS parentId,"
            + " (SELECT COUNT(*) FROM sys_user u WHERE u.del_flag = '0' AND u.dept_id = d.dept_id) AS cnt"
            + " FROM sys_dept d"
            + " WHERE d.del_flag = '0'"
            + "   AND (d.parent_id = (SELECT dept_id FROM sys_dept WHERE parent_id = 0 AND del_flag = '0'"
            + "                        ORDER BY dept_id LIMIT 1)"
            + "        OR d.parent_id = 0)"
            + " ORDER BY d.parent_id, d.order_num, d.dept_id")
    List<Map<String, Object>> selectDeptCounts();
}
