package com.ruoyi.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.business.domain.Mentor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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
}
