package com.ruoyi.business.service.impl;

import com.ruoyi.business.domain.Mentor;
import com.ruoyi.business.mapper.MentorMapper;
import com.ruoyi.business.service.IMentorService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 导师库 Service 实现（2026-09-23）
 *
 * <p>范围口径与项目其它模块一致：
 * <ul>
 *   <li>超管（SUPER_ADMIN / admin）→ {@code currentScopeDeptId()} 返回 {@code null}，不限部门</li>
 *   <li>部门管理员 → 强制本部门（{@code SecurityUtils.getDeptId()}）</li>
 * </ul>
 * 前端侧栏是硬编码的，所以这里**必须**在后端再校验一次范围，不能只靠页面。</p>
 *
 * @author ruoyi
 */
@Service
public class MentorServiceImpl implements IMentorService {

    @Autowired
    private MentorMapper mentorMapper;

    // ------------------------------------------------------------------
    // 范围
    // ------------------------------------------------------------------

    @Override
    public Long currentScopeDeptId() {
        if (SecurityUtils.hasRole("SUPER_ADMIN") || SecurityUtils.isAdmin(SecurityUtils.getUserId())) {
            return null;
        }
        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null) {
            throw new ServiceException("当前账号未绑定部门，无法维护导师信息");
        }
        return deptId;
    }

    /** MyBatis 返回的数值可能是 Long / Integer / BigInteger，统一安全转换 */
    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof BigInteger) {
            return ((BigInteger) value).longValue();
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // 查询
    // ------------------------------------------------------------------

    @Override
    public List<Mentor> selectMentorList(Mentor mentor) {
        Mentor query = mentor == null ? new Mentor() : mentor;
        return mentorMapper.selectMentorList(query, currentScopeDeptId());
    }

    @Override
    public List<Mentor> selectMentorOptions() {
        return mentorMapper.selectMentorOptions(currentScopeDeptId());
    }

    @Override
    public Mentor selectMentorById(Long id) {
        Mentor mentor = mentorMapper.selectMentorById(id);
        if (mentor == null) {
            throw new ServiceException("导师不存在或已删除");
        }
        Long scope = currentScopeDeptId();
        if (scope != null && !scope.equals(mentor.getDeptId())) {
            throw new ServiceException("无权查看其它部门的导师");
        }
        return mentor;
    }

    // ------------------------------------------------------------------
    // 增删改
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public int insertMentor(Mentor mentor) {
        if (StringUtils.isEmpty(mentor.getMentorName())) {
            throw new ServiceException("请填写导师姓名");
        }
        if (StringUtils.isEmpty(mentor.getMentorPhone())) {
            throw new ServiceException("请填写导师联系方式");
        }
        Long scope = currentScopeDeptId();
        if (scope != null) {
            // 部门管理员：强制落在本部门，忽略前端传值
            mentor.setDeptId(scope);
        }
        if (mentor.getDeptId() == null) {
            throw new ServiceException("请选择导师所属部门");
        }
        String name = mentor.getMentorName().trim();
        String phone = mentor.getMentorPhone().trim();
        if (mentorMapper.countSameMentor(name, phone, mentor.getDeptId(), null) > 0) {
            throw new ServiceException("该部门下已存在同名同联系方式的导师");
        }
        mentor.setMentorName(name);
        mentor.setMentorPhone(phone);
        if (StringUtils.isEmpty(mentor.getStatus())) {
            mentor.setStatus("0");
        }
        mentor.setDeleted(0);
        mentor.setCreateBy(SecurityUtils.getUsername());
        mentor.setCreateTime(new Date());
        return mentorMapper.insert(mentor);
    }

    @Override
    @Transactional
    public int updateMentor(Mentor mentor) {
        if (mentor.getId() == null) {
            throw new ServiceException("缺少导师ID");
        }
        Mentor current = mentorMapper.selectMentorById(mentor.getId());
        if (current == null) {
            throw new ServiceException("导师不存在或已删除");
        }
        Long scope = currentScopeDeptId();
        if (scope != null) {
            if (!scope.equals(current.getDeptId())) {
                throw new ServiceException("无权修改其它部门的导师");
            }
            mentor.setDeptId(scope);
        }
        String name = StringUtils.isEmpty(mentor.getMentorName())
                ? current.getMentorName() : mentor.getMentorName().trim();
        String phone = StringUtils.isEmpty(mentor.getMentorPhone())
                ? current.getMentorPhone() : mentor.getMentorPhone().trim();
        Long deptId = mentor.getDeptId() == null ? current.getDeptId() : mentor.getDeptId();
        if (mentorMapper.countSameMentor(name, phone, deptId, mentor.getId()) > 0) {
            throw new ServiceException("该部门下已存在同名同联系方式的导师");
        }
        mentor.setMentorName(name);
        mentor.setMentorPhone(phone);
        mentor.setDeptId(deptId);
        mentor.setUpdateBy(SecurityUtils.getUsername());
        mentor.setUpdateTime(new Date());
        int rows = mentorMapper.updateById(mentor);
        // ★ 改了姓名/联系方式后，同步刷新实习生身上的冗余展示列，否则花名册会显示旧值
        if (rows > 0) {
            mentorMapper.syncRedundantToUsers(mentor.getId(), name, phone);
        }
        return rows;
    }

    @Override
    @Transactional
    public int deleteMentor(Long id) {
        Mentor current = mentorMapper.selectMentorById(id);
        if (current == null) {
            throw new ServiceException("导师不存在或已删除");
        }
        Long scope = currentScopeDeptId();
        if (scope != null && !scope.equals(current.getDeptId())) {
            throw new ServiceException("无权删除其它部门的导师");
        }
        int used = mentorMapper.countInternsOfMentor(id);
        if (used > 0) {
            throw new ServiceException("该导师名下还有 " + used + " 名实习生，请先改派或解除关联");
        }
        Mentor update = new Mentor();
        update.setId(id);
        update.setDeleted(1);
        update.setUpdateBy(SecurityUtils.getUsername());
        update.setUpdateTime(new Date());
        return mentorMapper.updateById(update);
    }

    // ------------------------------------------------------------------
    // 给实习生分配导师
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public int assignMentorToIntern(Long userId, Long mentorId) {
        if (userId == null) {
            throw new ServiceException("缺少实习生ID");
        }
        if (mentorId == null) {
            throw new ServiceException("请选择导师");
        }
        Map<String, Object> intern = mentorMapper.selectInternForAssign(userId);
        if (intern == null || intern.isEmpty()) {
            throw new ServiceException("实习生不存在或已删除");
        }
        // ★ 「是不是实习生」必须查角色 —— 账号状态 / 培养状态都与权限无关
        Long roleCount = toLong(intern.get("internRoleCount"));
        if (roleCount == null || roleCount <= 0) {
            throw new ServiceException("该账号不是实习生角色，无法分配导师");
        }
        Mentor mentor = mentorMapper.selectMentorById(mentorId);
        if (mentor == null) {
            throw new ServiceException("导师不存在或已删除");
        }
        if (!"0".equals(mentor.getStatus())) {
            throw new ServiceException("该导师已停用，请先启用或另选一位");
        }
        Long scope = currentScopeDeptId();
        if (scope != null) {
            Long internDeptId = toLong(intern.get("deptId"));
            if (!scope.equals(internDeptId)) {
                throw new ServiceException("无权操作其它部门的实习生");
            }
            if (!scope.equals(mentor.getDeptId())) {
                throw new ServiceException("只能选择本部门的导师");
            }
        }
        return mentorMapper.assignMentor(userId, mentor.getId(),
                mentor.getMentorName(), mentor.getMentorPhone());
    }

    @Override
    @Transactional
    public int clearInternMentor(Long userId) {
        if (userId == null) {
            throw new ServiceException("缺少实习生ID");
        }
        Map<String, Object> intern = mentorMapper.selectInternForAssign(userId);
        if (intern == null || intern.isEmpty()) {
            throw new ServiceException("实习生不存在或已删除");
        }
        Long scope = currentScopeDeptId();
        if (scope != null && !scope.equals(toLong(intern.get("deptId")))) {
            throw new ServiceException("无权操作其它部门的实习生");
        }
        return mentorMapper.clearMentor(userId);
    }

    @Override
    public Map<String, Object> getInternMentorInfo(Long userId) {
        Map<String, Object> intern = mentorMapper.selectInternForAssign(userId);
        if (intern == null || intern.isEmpty()) {
            throw new ServiceException("实习生不存在或已删除");
        }
        Long scope = currentScopeDeptId();
        if (scope != null && !scope.equals(toLong(intern.get("deptId")))) {
            throw new ServiceException("无权查看其它部门的实习生");
        }
        return intern;
    }
}
