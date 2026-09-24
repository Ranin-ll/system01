package com.ruoyi.business.service.impl;

import com.ruoyi.business.domain.Mentor;
import com.ruoyi.business.domain.Position;
import com.ruoyi.business.mapper.MentorMapper;
import com.ruoyi.business.mapper.PositionMapper;
import com.ruoyi.business.mapper.SuperPersonnelMapper;
import com.ruoyi.business.service.ISuperPersonnelService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 超管「人员与账号信息管理」实现。
 *
 * <p><b>权限</b>：接口复用了原生 {@code system:user:*} 权限串（部门管理员并不持有），
 * 但按本项目惯例，越权拦截仍落在 Service —— {@link #requireSuperAdmin()}。</p>
 */
@Service
public class SuperPersonnelServiceImpl implements ISuperPersonnelService {

    /** 培养状态（与 sys_user.user_status 口径一致，见 department/people/profile.vue 的文案表） */
    private static final Set<String> USER_STATUS = new HashSet<>(Arrays.asList(
            "WAIT_AUDIT", "REJECTED", "PRE_TRAINEE", "PENDING_PROMOTE",
            "FORMAL_TRAINEE", "NON_INTERN", "DISABLED", "ARCHIVED"));

    /** 协议签署状态：0 未签 / 1 已签 */
    private static final Set<Integer> PROTOCOL_STATUS = new HashSet<>(Arrays.asList(0, 1));

    @Autowired
    private SuperPersonnelMapper personnelMapper;

    @Autowired
    private PositionMapper positionMapper;

    /** 导师库（2026-09-23：导师从主表选，不再手敲文本） */
    @Autowired
    private MentorMapper mentorMapper;

    /** 删除委托给框架实现：它负责逻辑删除与角色/岗位关联清理 */
    @Autowired
    private com.ruoyi.system.service.ISysUserService userService;

    /** 人员与账号的维护仅超管可做 */
    private void requireSuperAdmin() {
        Long uid = SecurityUtils.getUserId();
        if (!(SecurityUtils.hasRole("SUPER_ADMIN") || SecurityUtils.isAdmin(uid))) {
            throw new ServiceException("仅超级管理员可维护人员与账号信息");
        }
    }

    @Override
    public List<Map<String, Object>> selectPersonnelList(Map<String, Object> query) {
        requireSuperAdmin();
        return personnelMapper.selectPersonnelList(
                str(query.get("keyword")),
                longOf(query.get("deptId")),
                str(query.get("userStatus")),
                str(query.get("status")),
                str(query.get("roleKey")));
    }

    @Override
    public Map<String, Object> selectPersonnelDetail(Long userId) {
        requireSuperAdmin();
        if (userId == null) {
            throw new ServiceException("缺少 userId");
        }
        Map<String, Object> detail = personnelMapper.selectPersonnelDetail(userId);
        if (detail == null) {
            throw new ServiceException("人员不存在或已删除");
        }
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> saveBusinessFields(Map<String, Object> body) {
        requireSuperAdmin();
        Long userId = longOf(body.get("userId"));
        if (userId == null) {
            throw new ServiceException("缺少 userId");
        }
        if (personnelMapper.selectPersonnelDetail(userId) == null) {
            throw new ServiceException("人员不存在或已删除");
        }

        // ---- ★ 内置规则：超管属于公司根部门（融谷），不属于任何子部门 ----
        // 必须在写库前判定；并且是「自愈」式的：无论前端传没传、传得对不对，最终都归位到根部门。
        Long deptId = longOf(body.get("deptId"));
        if (personnelMapper.countSuperAdminRole(userId) > 0) {
            Long rootDeptId = personnelMapper.selectRootDeptId();
            if (rootDeptId == null) {
                throw new ServiceException("未配置公司根部门，无法保存超级管理员");
            }
            if (deptId != null && !deptId.equals(rootDeptId)) {
                throw new ServiceException("超级管理员必须属于公司根部门，不能挂到子部门");
            }
            deptId = rootDeptId;
        }

        // ---- 岗位：允许显式传 null 表示「清空岗位」（原生 updateUser 不碰这一列，必须走这里）----
        Long positionId = longOf(body.get("positionId"));
        boolean clearPosition = body.containsKey("positionId") && positionId == null;
        if (positionId != null) {
            Position position = positionMapper.selectById(positionId);
            if (position == null || Integer.valueOf(1).equals(position.getDeleted())) {
                throw new ServiceException("所选岗位不存在或已删除");
            }
        }

        // ---- 培养状态 ----
        String userStatus = str(body.get("userStatus"));
        if (userStatus != null && !USER_STATUS.contains(userStatus)) {
            throw new ServiceException("培养状态取值不合法：" + userStatus);
        }

        // ---- 协议状态 ----
        Integer protocolStatus = intOf(body.get("protocolStatus"));
        if (protocolStatus != null && !PROTOCOL_STATUS.contains(protocolStatus)) {
            throw new ServiceException("协议状态只能是 0（未签）或 1（已签）");
        }

        // ---- 预计入职：允许显式传 null 清空 ----
        Date entryDate = dateOf(body.get("expectedEntryDate"));
        boolean clearEntryDate = body.containsKey("expectedEntryDate") && entryDate == null;

        // ---- 导师（2026-09-23 改为从导师库选）----
        // 三档语义，互斥且可预期：
        //   ① 传了 mentorId（非空）→ 从 mentor 表取出姓名/联系方式并回填冗余列
        //   ② 显式传 mentorId = null → 清空导师关联（mentor_id + 两个冗余列）
        //   ③ 只传了 mentorName / mentorPhone（老前端）→ 按旧逻辑写文本，不动 mentor_id
        Long mentorId = longOf(body.get("mentorId"));
        boolean clearMentor = body.containsKey("mentorId") && mentorId == null;
        String mentorName = body.containsKey("mentorName") ? strOrEmpty(body.get("mentorName")) : null;
        String mentorPhone = body.containsKey("mentorPhone") ? strOrEmpty(body.get("mentorPhone")) : null;
        if (mentorId != null) {
            Mentor mentor = mentorMapper.selectMentorById(mentorId);
            if (mentor == null) {
                throw new ServiceException("所选导师不存在或已删除");
            }
            if (!"0".equals(mentor.getStatus())) {
                throw new ServiceException("所选导师已停用，请先启用或另选一位");
            }
            mentorName = mentor.getMentorName();
            mentorPhone = mentor.getMentorPhone();
            clearMentor = false;
        } else if (clearMentor) {
            mentorName = "";
            mentorPhone = "";
        }

        personnelMapper.updateBusinessFields(
                userId,
                deptId,
                positionId,
                clearPosition,
                userStatus,
                protocolStatus,
                mentorId,
                clearMentor,
                mentorName,
                mentorPhone,
                entryDate,
                clearEntryDate,
                SecurityUtils.getUsername());

        return personnelMapper.selectPersonnelDetail(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deletePersonnel(Long userId) {
        requireSuperAdmin();
        if (userId == null) {
            throw new ServiceException("缺少 userId");
        }
        // ① 平台内置账号：RuoYi 的 checkUserAllowed 也会拦，这里给出更明确的提示
        if (Long.valueOf(1L).equals(userId)) {
            throw new ServiceException("平台内置超级管理员账号不允许删除");
        }
        // ② 超管账号一律不可删 —— 保证系统始终存在可用超管（要删请先把它改成其他角色）
        if (personnelMapper.countSuperAdminRole(userId) > 0) {
            throw new ServiceException("该账号持有「超级管理员」角色，不允许删除；请先将其改为其他角色");
        }
        // ③ 不能删自己
        if (userId.equals(SecurityUtils.getUserId())) {
            throw new ServiceException("不能删除当前登录账号");
        }
        if (personnelMapper.selectPersonnelDetail(userId) == null) {
            throw new ServiceException("人员不存在或已删除");
        }
        // 委托原生删除：它会做逻辑删除（del_flag='2'）并清理角色/岗位关联
        return userService.deleteUserByIds(new Long[] { userId });
    }

    @Override
    public Map<String, Object> selectSummary() {
        requireSuperAdmin();
        Map<String, Object> summary = personnelMapper.selectPersonnelSummary();
        // 主查询用 SUM(CASE...) 聚合，命中 0 行时仍返回 1 行；这里只做防御性兜底
        Map<String, Object> result = new LinkedHashMap<>();
        if (summary != null) {
            result.putAll(summary);
        }
        result.put("roleCounts", personnelMapper.selectRoleCounts());
        result.put("deptCounts", personnelMapper.selectDeptCounts());
        result.put("noRole", personnelMapper.countNoRole());
        return result;
    }

    // ------------------------------------------------------------------ 小工具

    private static String str(Object o) {
        if (o == null) {
            return null;
        }
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? null : s;
    }

    /** 与 str 的区别：空串也返回 ""（用于「显式清空导师」） */
    private static String strOrEmpty(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private static Long longOf(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            long v = ((Number) o).longValue();
            return v == 0L ? null : v;
        }
        String s = String.valueOf(o).trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            long v = Long.parseLong(s);
            return v == 0L ? null : v;
        } catch (NumberFormatException e) {
            throw new ServiceException("数字格式不正确：" + s);
        }
    }

    private static Integer intOf(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        String s = String.valueOf(o).trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new ServiceException("数字格式不正确：" + s);
        }
    }

    private static Date dateOf(Object o) {
        if (o == null) {
            return null;
        }
        String s = String.valueOf(o).trim();
        if (s.isEmpty() || "null".equals(s)) {
            return null;
        }
        // 兼容 "2026-09-20" 与 "2026-09-20 00:00:00"
        String day = s.length() >= 10 ? s.substring(0, 10) : s;
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(day);
        } catch (ParseException e) {
            throw new ServiceException("日期格式应为 yyyy-MM-dd：" + s);
        }
    }
}
