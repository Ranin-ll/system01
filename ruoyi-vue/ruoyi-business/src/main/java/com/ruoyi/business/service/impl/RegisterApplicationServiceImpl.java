package com.ruoyi.business.service.impl;

import com.ruoyi.business.domain.AuditRecord;
import com.ruoyi.business.domain.RegisterApplication;
import com.ruoyi.business.mapper.InternAuthMapper;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.business.mapper.RegisterApplicationMapper;
import com.ruoyi.business.service.IRegisterApplicationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 注册申请Service业务层实现
 *
 * @author ruoyi
 */
@Service
public class RegisterApplicationServiceImpl extends ServiceImpl<RegisterApplicationMapper, RegisterApplication>
        implements IRegisterApplicationService {

    @Autowired
    private RegisterApplicationMapper registerApplicationMapper;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private InternAuthMapper internAuthMapper;

    @Autowired
    private com.ruoyi.business.mapper.AuditRecordMapper auditRecordMapper;

    @Override
    public List<RegisterApplication> selectRegisterList(RegisterApplication registerApplication) {
        // 部门管理员自动注入本部门；超管查全部
        Long currentDeptId = currentDeptScope();
        return registerApplicationMapper.selectRegisterList(registerApplication, currentDeptId);
    }

    @Override
    public Map<String, Object> selectRegisterSummary() {
        return registerApplicationMapper.selectRegisterSummary(currentDeptScope());
    }

    @Override
    public Map<String, Object> selectPublicStatusByPhone(String phone, String password) {
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new ServiceException("该手机号未提交注册");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new ServiceException("密码错误");
        }
        SysUser user = userService.selectUserByPhone(phone);
        if (user == null) {
            throw new ServiceException("该手机号未提交注册");
        }
        if (user.getPassword() == null || !SecurityUtils.matchesPassword(password, user.getPassword())) {
            throw new ServiceException("密码错误");
        }
        RegisterApplication application = registerApplicationMapper.selectPublicStatusByLoginAccount(phone);
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("submitted", application != null);
        if (application == null) {
            result.put("status", "NOT_SUBMITTED");
            return result;
        }
        result.put("status", application.getStatus());
        result.put("applicationNo", application.getApplicationNo());
        result.put("realName", application.getRealName());
        result.put("positionName", application.getPositionName());
        result.put("deptName", application.getDeptName());
        result.put("createTime", application.getCreateTime());
        result.put("updateTime", application.getUpdateTime());
        if ("REJECTED".equals(application.getStatus())) {
            result.put("rejectReason", application.getRejectReason());
        }
        return result;
    }

    private Long currentDeptScope() {
        if (SecurityUtils.hasRole("SUPER_ADMIN")) {
            return null;
        }
        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null) {
            throw new ServiceException("当前账号未配置部门，无法访问注册审核数据");
        }
        return deptId;
    }

    @Override
    public List<RegisterApplication> selectByUserId(Long userId) {
        return registerApplicationMapper.selectRegisterListByUserId(userId);
    }

    @Override
    public RegisterApplication selectByApplicationNo(String applicationNo) {
        return registerApplicationMapper.selectByApplicationNo(applicationNo);
    }

    @Override
    public RegisterApplication selectById(Long id) {
        RegisterApplication application = registerApplicationMapper.selectRegisterById(id);
        checkDeptScope(application);
        return application;
    }

    @Override
    public List<AuditRecord> selectAuditHistory(Long id) {
        RegisterApplication application = registerApplicationMapper.selectRegisterById(id);
        checkDeptScope(application);
        return auditRecordMapper.selectByBiz("REGISTER", id);
    }

    private void checkDeptScope(RegisterApplication application) {
        if (application == null) {
            throw new ServiceException("注册申请不存在");
        }
        if (!SecurityUtils.hasRole("SUPER_ADMIN")) {
            Long currentDeptId = SecurityUtils.getDeptId();
            if (currentDeptId == null || !currentDeptId.equals(application.getDeptId())) {
                throw new ServiceException("无权查看其他部门的注册申请");
            }
        }
    }

    @Override
    @Transactional
    public Long submitApplication(RegisterApplication app) {
        // 校验同账号是否已有未结束申请
        if (!checkUniqueActive(app.getLoginAccount())) {
            throw new ServiceException("账号 '" + app.getLoginAccount() + "' 已有未结束的注册申请");
        }
        // 生成申请编号
        app.setApplicationNo("APP" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        app.setStatus("WAIT_AUDIT");
        app.setAuditCount(1);
        app.setCreateTime(new Date());
        registerApplicationMapper.insertApplication(app);
        return app.getId();
    }

    @Override
    @Transactional
    public int resubmitApplication(Long id, RegisterApplication update) {
        RegisterApplication existing = registerApplicationMapper.selectRegisterById(id);
        if (existing == null) {
            throw new ServiceException("申请记录不存在");
        }
        if (!"REJECTED".equals(existing.getStatus())) {
            throw new ServiceException("仅已驳回申请可重新提交");
        }
        existing.setRealName(update.getRealName());
        existing.setIdCard(update.getIdCard());
        existing.setPositionId(update.getPositionId());
        existing.setDeptId(update.getDeptId());
        existing.setExpectedEntryDate(update.getExpectedEntryDate());
        existing.setStatus("WAIT_AUDIT");
        existing.setRejectReason(null);
        existing.setAuditCount(existing.getAuditCount() + 1);
        existing.setUpdateTime(new Date());
        return registerApplicationMapper.updateApplication(existing);
    }

    @Override
    @Transactional
    public AuditRecord audit(Long id, String action, String reason, String mentorName, String mentorPhone) {
        RegisterApplication app = registerApplicationMapper.selectRegisterById(id);
        if (app == null) {
            throw new ServiceException("申请记录不存在");
        }
        if (!"WAIT_AUDIT".equals(app.getStatus())) {
            throw new ServiceException("该申请已被处理，无需重复审核");
        }
        Long operatorId = SecurityUtils.getUserId();
        Long deptId = SecurityUtils.getDeptId();
        if (!SecurityUtils.hasRole("SUPER_ADMIN") && (deptId == null || !deptId.equals(app.getDeptId()))) {
            throw new ServiceException("无权处理其他部门的注册申请");
        }

        String fromStatus = app.getStatus();
        String toStatus;
        if ("PASS".equals(action)) {
            // ★ 2026-09-23：导师不再在审核时填写（已抽成独立 mentor 主表，
            //   改由「实习生管理页」从导师库里选）。这里**不再校验、也不再写入**导师字段。
            //   兼容：若老调用方仍传了导师，就照旧写一份冗余展示列；没传则完全不碰，
            //   避免把实习生身上已有的导师信息误清空（见 updateAuditStatus 的注释）。
            toStatus = "PASSED";
            app.setStatus(toStatus);
            boolean hasMentor = mentorName != null && !mentorName.trim().isEmpty()
                    && mentorPhone != null && !mentorPhone.trim().isEmpty();
            int updated = hasMentor
                    ? internAuthMapper.updateAuditProfile(app.getUserId(), "PRE_TRAINEE", "0",
                            mentorName.trim(), mentorPhone.trim())
                    : internAuthMapper.updateAuditStatus(app.getUserId(), "PRE_TRAINEE", "0");
            if (updated != 1) {
                throw new ServiceException("审核通过失败：实习生账号不存在");
            }
        } else if ("REJECT".equals(action)) {
            toStatus = "REJECTED";
            if (reason == null || reason.trim().isEmpty()) {
                throw new ServiceException("驳回必须填写原因");
            }
            app.setStatus(toStatus);
            app.setRejectReason(reason);
            if (internAuthMapper.updateAuditProfile(app.getUserId(), "REJECTED", "1", null, null) != 1) {
                throw new ServiceException("驳回申请失败：实习生账号不存在");
            }
        } else {
            throw new ServiceException("非法审核动作");
        }
        registerApplicationMapper.updateApplication(app);

        // 写入审核历史（通用 audit_record 表）
        AuditRecord record = new AuditRecord();
        record.setBizType("REGISTER");
        record.setBizId(id);
        record.setUserId(app.getUserId());
        record.setDeptId(deptId);
        record.setAction(action);
        record.setFromStatus(fromStatus);
        record.setToStatus(toStatus);
        record.setReason(reason);
        record.setOperatorId(operatorId);
        record.setCreateTime(new Date());
        auditRecordMapper.insertAuditRecord(record);
        return record;
    }

    @Override
    public RegisterApplication selectLatestByLoginAccount(String loginAccount) {
        return registerApplicationMapper.selectPublicStatusByLoginAccount(loginAccount);
    }

    @Override
    public boolean checkUniqueActive(String loginAccount) {
        Long count = registerApplicationMapper.countActiveByLoginAccount(loginAccount);
        return count == 0;
    }
}
