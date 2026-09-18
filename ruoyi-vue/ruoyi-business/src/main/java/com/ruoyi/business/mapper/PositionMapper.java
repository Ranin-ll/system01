package com.ruoyi.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.business.domain.Position;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 岗位类型Mapper接口
 *
 * @author ruoyi
 */
@Mapper
public interface PositionMapper extends BaseMapper<Position> {

    /**
     * 部门 ↔ 岗位绑定 + 人数（权威表 <code>dept_position</code>，仅取生效中 status = 1）。
     *
     * 后端各处（课程可见性 CourseMapper、学习范围 StudyItemMapper、注册推导 InternAuthMapper）
     * 一律以 dept_position 为准，但此前**没有任何对外读接口**，前端只能从「在册用户」或「报名记录」反推部门岗位；
     * 遇到账号是直接创建、未走注册审核的部门就会推不出来（页面显示「未绑定」）。
     *
     * <p>人数口径（2026-09-17 补）：这两个数**必须在这里算**，不能让前端拿
     * <code>/system/user/list</code> 去数 —— 该接口虽然返回 <code>userStatus</code> 字段，但值为 <b>null</b>
     * （RuoYi 的 selectUserList 没有 select 这个业务扩展列，且不返回 <code>positionId</code>）。
     * <ul>
     *   <li><code>trainingCount</code>：在培 = <code>user_status</code> 处于 PRE_TRAINEE / FORMAL_TRAINEE / PENDING_PROMOTE
     *       <b>且真的持有实习生角色</b>（只看状态会把「无角色的遗留废号」也数进来 —— 历史上就有一个昵称叫
     *       「测试超级管理员」的废号，让交付部门的在培人数虚高 3 人）</li>
     *   <li><code>waitAuditCount</code>：待审核 = <code>register_application.status = 'WAIT_AUDIT'</code></li>
     * </ul>
     * 本方法只读，供管理端核对使用。
     */
    @Select("SELECT dp.dept_id AS deptId, d.dept_name AS deptName, "
            + "dp.position_id AS positionId, p.position_code AS positionCode, p.position_name AS positionName, "
            + "(SELECT COUNT(*) FROM sys_user u WHERE u.dept_id = dp.dept_id AND u.del_flag = '0' "
            + "   AND u.user_status IN ('PRE_TRAINEE', 'FORMAL_TRAINEE', 'PENDING_PROMOTE') "
            + "   AND EXISTS (SELECT 1 FROM sys_user_role ur JOIN sys_role r ON r.role_id = ur.role_id "
            + "                WHERE ur.user_id = u.user_id AND r.role_key IN ('PRE_TRAINEE', 'FORMAL_TRAINEE'))) AS trainingCount, "
            + "(SELECT COUNT(*) FROM register_application ra WHERE ra.dept_id = dp.dept_id "
            + "   AND ra.status = 'WAIT_AUDIT') AS waitAuditCount "
            + "FROM dept_position dp "
            + "LEFT JOIN position p ON p.id = dp.position_id "
            + "LEFT JOIN sys_dept d ON d.dept_id = dp.dept_id AND d.del_flag = '0' "
            + "WHERE dp.status = 1 "
            + "ORDER BY dp.dept_id ASC")
    List<Map<String, Object>> selectDeptBindings();
}
