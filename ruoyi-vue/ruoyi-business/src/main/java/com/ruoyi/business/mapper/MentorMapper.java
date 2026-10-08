package com.ruoyi.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.business.domain.Mentor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 导师库 Mapper
 *
 * @author ruoyi
 */
@Mapper
public interface MentorMapper extends BaseMapper<Mentor> {

    /**
     * 导师列表（含部门名 / 岗位名 / 在带实习生人数）。
     *
     * @param mentor 查询条件（mentorName / mentorPhone / status / deptId / keyword）
     * @param scopeDeptId 部门范围：非 null 时强制只返回该部门的导师（部门管理员）
     */
    List<Mentor> selectMentorList(@Param("q") Mentor mentor, @Param("scopeDeptId") Long scopeDeptId);

    /**
     * 下拉选项（仅启用中，按部门范围）。用于「实习生管理页」选导师。
     */
    List<Mentor> selectMentorOptions(@Param("scopeDeptId") Long scopeDeptId);

    /**
     * 按 id 查单个（含部门名 / 岗位名）。
     */
    Mentor selectMentorById(@Param("id") Long id);

    /**
     * 同部门下「姓名 + 联系方式」是否已存在（排除自身 id，用于新增/修改去重）。
     */
    @Select("SELECT COUNT(*) FROM mentor WHERE deleted = 0 AND mentor_name = #{mentorName} "
            + "AND IFNULL(mentor_phone, '') = IFNULL(#{mentorPhone}, '') "
            + "AND IFNULL(dept_id, 0) = IFNULL(#{deptId}, 0) "
            + "AND (#{excludeId} IS NULL OR id <> #{excludeId})")
    int countSameMentor(@Param("mentorName") String mentorName,
                        @Param("mentorPhone") String mentorPhone,
                        @Param("deptId") Long deptId,
                        @Param("excludeId") Long excludeId);

    /**
     * 该导师名下在带实习生人数（删导师前的占用校验）。
     */
    @Select("SELECT COUNT(*) FROM sys_user WHERE del_flag = '0' AND mentor_id = #{mentorId}")
    int countInternsOfMentor(@Param("mentorId") Long mentorId);

    /**
     * 实习生（sys_user）信息：是否在职、部门、以及是否真的持有实习生角色。
     *
     * <p>★ 「是不是实习生」必须查角色，不能只看 {@code user_status}
     * （账号状态 ≠ 培养状态 ≠ 权限，本项目铁律）。</p>
     */
    @Select("SELECT u.user_id AS userId, u.user_name AS userName, u.nick_name AS nickName, "
            + "       u.dept_id AS deptId, u.user_status AS userStatus, u.mentor_id AS mentorId, "
            + "       (SELECT COUNT(*) FROM sys_user_role ur JOIN sys_role r ON r.role_id = ur.role_id "
            + "         WHERE ur.user_id = u.user_id AND r.role_key IN ('PRE_TRAINEE', 'FORMAL_TRAINEE')) AS internRoleCount "
            + "FROM sys_user u WHERE u.user_id = #{userId} AND u.del_flag = '0' LIMIT 1")
    Map<String, Object> selectInternForAssign(@Param("userId") Long userId);

    /**
     * 给实习生挂/换导师：同时写关联 ID 与两个冗余展示列。
     */
    @Update("UPDATE sys_user SET mentor_id = #{mentorId}, mentor_name = #{mentorName}, "
            + "mentor_phone = #{mentorPhone}, update_time = NOW() WHERE user_id = #{userId} AND del_flag = '0'")
    int assignMentor(@Param("userId") Long userId, @Param("mentorId") Long mentorId,
                     @Param("mentorName") String mentorName, @Param("mentorPhone") String mentorPhone);

    /**
     * 解除某实习生的导师关联（三个字段一起清）。
     */
    @Update("UPDATE sys_user SET mentor_id = NULL, mentor_name = NULL, mentor_phone = NULL, "
            + "update_time = NOW() WHERE user_id = #{userId} AND del_flag = '0'")
    int clearMentor(@Param("userId") Long userId);

    /**
     * 导师改名/改联系方式后，同步刷新实习生身上的冗余展示列。
     *
     * <p>★ 冗余列的价值就在这里：注册申请列表、实习生花名册、人员与账号、超管分析
     * 都直接读 {@code sys_user.mentor_name}，不同步就会显示旧值。</p>
     */
    @Update("UPDATE sys_user SET mentor_name = #{mentorName}, mentor_phone = #{mentorPhone}, "
            + "update_time = NOW() WHERE del_flag = '0' AND mentor_id = #{mentorId}")
    int syncRedundantToUsers(@Param("mentorId") Long mentorId,
                             @Param("mentorName") String mentorName,
                             @Param("mentorPhone") String mentorPhone);

    // ------------------------------------------------------------------
    // 实习生「编辑基础信息」（2026-09-24：部门管理员在实习生管理页改所有基础字段）
    // ------------------------------------------------------------------

    /**
     * 实习生编辑详情：完整基础信息回显（含 email / sex / positionId / expectedEntryDate，
     * 这些字段花名册聚合接口 {@code stage-progress} 没带，编辑弹窗必须单独取）。
     */
    @Select("SELECT u.user_id AS userId, u.user_name AS userName, u.nick_name AS nickName, "
            + "       u.phonenumber AS phonenumber, u.email AS email, u.sex AS sex, "
            + "       u.dept_id AS deptId, u.position_id AS positionId, "
            + "       u.expected_entry_date AS expectedEntryDate, "
            + "       u.mentor_id AS mentorId, u.mentor_name AS mentorName, u.mentor_phone AS mentorPhone, "
            + "       u.user_status AS userStatus, "
            + "       (SELECT COUNT(*) FROM sys_user_role ur JOIN sys_role r ON r.role_id = ur.role_id "
            + "         WHERE ur.user_id = u.user_id AND r.role_key IN ('PRE_TRAINEE', 'FORMAL_TRAINEE')) AS internRoleCount "
            + "FROM sys_user u WHERE u.user_id = #{userId} AND u.del_flag = '0' LIMIT 1")
    Map<String, Object> selectInternDetailForEdit(@Param("userId") Long userId);

    /** 手机号是否被其它账号占用（编辑时唯一性校验） */
    @Select("SELECT COUNT(*) FROM sys_user WHERE del_flag = '0' "
            + "AND phonenumber = #{phone} AND phonenumber IS NOT NULL AND user_id <> #{userId}")
    int countPhone(@Param("phone") String phone, @Param("userId") Long userId);

    /** 邮箱是否被其它账号占用（编辑时唯一性校验；空邮箱不参与校验） */
    @Select("SELECT COUNT(*) FROM sys_user WHERE del_flag = '0' "
            + "AND email = #{email} AND email IS NOT NULL AND email <> '' AND user_id <> #{userId}")
    int countEmail(@Param("email") String email, @Param("userId") Long userId);

    /**
     * 更新实习生基础信息（动态 set，只更新传入的列）。
     * 语义与超管 {@code SuperPersonnelMapper.updateBusinessFields} 一致，
     * 只是这里额外支持 nick_name / phonenumber / email / sex 这些账号基础列。
     */
    @Update("<script>"
            + "UPDATE sys_user"
            + "   <set>"
            + "     <if test=\"nickName != null\">nick_name = #{nickName},</if>"
            + "     <if test=\"phonenumber != null\">phonenumber = #{phonenumber},</if>"
            + "     <if test=\"clearPhone\">phonenumber = NULL,</if>"
            + "     <if test=\"email != null\">email = #{email},</if>"
            + "     <if test=\"clearEmail\">email = NULL,</if>"
            + "     <if test=\"sex != null\">sex = #{sex},</if>"
            + "     <if test=\"positionId != null\">position_id = #{positionId},</if>"
            + "     <if test=\"clearPosition\">position_id = NULL,</if>"
            + "     <if test=\"expectedEntryDate != null\">expected_entry_date = #{expectedEntryDate},</if>"
            + "     <if test=\"clearEntryDate\">expected_entry_date = NULL,</if>"
            + "     <if test=\"mentorId != null\">mentor_id = #{mentorId},</if>"
            + "     <if test=\"clearMentor\">mentor_id = NULL, mentor_name = NULL, mentor_phone = NULL,</if>"
            + "     <if test=\"mentorName != null\">mentor_name = #{mentorName},</if>"
            + "     <if test=\"mentorPhone != null\">mentor_phone = #{mentorPhone},</if>"
            + "     <if test=\"updateBy != null and updateBy != ''\">update_by = #{updateBy},</if>"
            + "     update_time = NOW()"
            + "   </set>"
            + " WHERE user_id = #{userId} AND del_flag = '0'"
            + "</script>")
    int updateInternBasic(@Param("userId") Long userId,
                          @Param("nickName") String nickName,
                          @Param("phonenumber") String phonenumber,
                          @Param("clearPhone") boolean clearPhone,
                          @Param("email") String email,
                          @Param("clearEmail") boolean clearEmail,
                          @Param("sex") String sex,
                          @Param("positionId") Long positionId,
                          @Param("clearPosition") boolean clearPosition,
                          @Param("expectedEntryDate") Date expectedEntryDate,
                          @Param("clearEntryDate") boolean clearEntryDate,
                          @Param("mentorId") Long mentorId,
                          @Param("clearMentor") boolean clearMentor,
                          @Param("mentorName") String mentorName,
                          @Param("mentorPhone") String mentorPhone,
                          @Param("updateBy") String updateBy);
}
