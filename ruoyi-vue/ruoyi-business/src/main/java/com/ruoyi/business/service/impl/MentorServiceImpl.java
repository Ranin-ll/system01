package com.ruoyi.business.service.impl;

import com.ruoyi.business.domain.Mentor;
import com.ruoyi.business.domain.Position;
import com.ruoyi.business.mapper.MentorMapper;
import com.ruoyi.business.mapper.PositionMapper;
import com.ruoyi.business.service.IMentorService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    @Autowired
    private PositionMapper positionMapper;

    /** 性别合法取值（RuoYi 字典：0 男 / 1 女 / 2 未知） */
    private static final Set<String> SEX = new HashSet<>(Arrays.asList("0", "1", "2"));

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

    // ------------------------------------------------------------------
    // 实习生「编辑基础信息」（2026-09-24）
    // ------------------------------------------------------------------

    @Override
    public Map<String, Object> getInternDetailForEdit(Long userId) {
        if (userId == null) {
            throw new ServiceException("缺少实习生ID");
        }
        Map<String, Object> intern = mentorMapper.selectInternDetailForEdit(userId);
        if (intern == null || intern.isEmpty()) {
            throw new ServiceException("实习生不存在或已删除");
        }
        Long scope = currentScopeDeptId();
        if (scope != null && !scope.equals(toLong(intern.get("deptId")))) {
            throw new ServiceException("无权查看其它部门的实习生");
        }
        return intern;
    }

    @Override
    @Transactional
    public int updateInternBasic(Long userId, Map<String, Object> body) {
        if (userId == null) {
            throw new ServiceException("缺少实习生ID");
        }
        if (body == null) {
            body = new java.util.HashMap<>();
        }
        Map<String, Object> intern = mentorMapper.selectInternDetailForEdit(userId);
        if (intern == null || intern.isEmpty()) {
            throw new ServiceException("实习生不存在或已删除");
        }
        // ★ 是不是实习生必须查角色（与 assignMentorToIntern 同一铁律）
        Long roleCount = toLong(intern.get("internRoleCount"));
        if (roleCount == null || roleCount <= 0) {
            throw new ServiceException("该账号不是实习生角色，无法编辑基础信息");
        }
        Long scope = currentScopeDeptId();
        if (scope != null && !scope.equals(toLong(intern.get("deptId")))) {
            throw new ServiceException("无权操作其它部门的实习生");
        }

        // ---- 姓名（NOT NULL 列，显式传了就必须非空）----
        String nickName = null;
        if (body.containsKey("nickName")) {
            nickName = str(body.get("nickName"));
            if (nickName == null) {
                throw new ServiceException("姓名不能为空");
            }
        }

        // ---- 手机：空值 = 清空；非空做唯一性校验 ----
        String phonenumber = null;
        boolean clearPhone = false;
        if (body.containsKey("phonenumber")) {
            phonenumber = str(body.get("phonenumber"));
            if (phonenumber == null) {
                clearPhone = true;
            } else if (mentorMapper.countPhone(phonenumber, userId) > 0) {
                throw new ServiceException("该手机号已被其它账号使用");
            }
        }

        // ---- 邮箱：空值 = 清空；非空做唯一性校验 ----
        String email = null;
        boolean clearEmail = false;
        if (body.containsKey("email")) {
            email = str(body.get("email"));
            if (email == null) {
                clearEmail = true;
            } else if (mentorMapper.countEmail(email, userId) > 0) {
                throw new ServiceException("该邮箱已被其它账号使用");
            }
        }

        // ---- 性别（0 男 / 1 女 / 2 未知）----
        String sex = null;
        if (body.containsKey("sex")) {
            sex = str(body.get("sex"));
            if (sex != null && !SEX.contains(sex)) {
                throw new ServiceException("性别取值不合法（0 男 / 1 女 / 2 未知）");
            }
        }

        // ---- 岗位：允许显式传 null / 0 表示「清空岗位」----
        Long positionId = toLong(body.get("positionId"));
        if (positionId != null && positionId == 0L) {
            positionId = null;
        }
        boolean clearPosition = body.containsKey("positionId") && positionId == null;
        if (positionId != null) {
            Position position = positionMapper.selectById(positionId);
            if (position == null || Integer.valueOf(1).equals(position.getDeleted())) {
                throw new ServiceException("所选岗位不存在或已删除");
            }
        }

        // ---- 预计入职：允许显式传 null 清空 ----
        Date entryDate = dateOf(body.get("expectedEntryDate"));
        boolean clearEntryDate = body.containsKey("expectedEntryDate") && entryDate == null;

        // ---- 导师（三档语义，与超管 saveBusinessFields 一致）----
        Long mentorId = toLong(body.get("mentorId"));
        if (mentorId != null && mentorId == 0L) {
            mentorId = null;
        }
        boolean clearMentor = body.containsKey("mentorId") && mentorId == null;
        String mentorName = null;
        String mentorPhone = null;
        if (mentorId != null) {
            Mentor mentor = mentorMapper.selectMentorById(mentorId);
            if (mentor == null) {
                throw new ServiceException("所选导师不存在或已删除");
            }
            if (!"0".equals(mentor.getStatus())) {
                throw new ServiceException("所选导师已停用，请先启用或另选一位");
            }
            if (scope != null && !scope.equals(mentor.getDeptId())) {
                throw new ServiceException("只能选择本部门的导师");
            }
            mentorName = mentor.getMentorName();
            mentorPhone = mentor.getMentorPhone();
            clearMentor = false;
        } else if (clearMentor) {
            // 清空：三个字段一起置空
            mentorName = null;
            mentorPhone = null;
        }

        return mentorMapper.updateInternBasic(
                userId, nickName, phonenumber, clearPhone, email, clearEmail, sex,
                positionId, clearPosition, entryDate, clearEntryDate,
                mentorId, clearMentor, mentorName, mentorPhone,
                SecurityUtils.getUsername());
    }

    // ------------------------------------------------------------------ 小工具

    private String str(Object o) {
        if (o == null) {
            return null;
        }
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? null : s;
    }

    private Date dateOf(Object o) {
        if (o == null) {
            return null;
        }
        String s = String.valueOf(o).trim();
        if (s.isEmpty() || "null".equals(s)) {
            return null;
        }
        String day = s.length() >= 10 ? s.substring(0, 10) : s;
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(day);
        } catch (ParseException e) {
            throw new ServiceException("日期格式应为 yyyy-MM-dd：" + s);
        }
    }
}
