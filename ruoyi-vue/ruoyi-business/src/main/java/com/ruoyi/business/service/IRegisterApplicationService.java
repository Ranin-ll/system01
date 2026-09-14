package com.ruoyi.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.business.domain.AuditRecord;
import com.ruoyi.business.domain.RegisterApplication;

import java.util.List;
import java.util.Map;

/**
 * 注册申请Service接口
 *
 * @author ruoyi
 */
public interface IRegisterApplicationService extends IService<RegisterApplication> {

    /**
     * 查询注册申请列表（自动注入本部门范围）
     */
    List<RegisterApplication> selectRegisterList(RegisterApplication registerApplication);

    /**
     * 根据用户ID查询申请记录
     */
    List<RegisterApplication> selectByUserId(Long userId);

    /**
     * 根据申请编号查询
     */
    RegisterApplication selectByApplicationNo(String applicationNo);

    /**
     * 根据ID查询申请详情
     */
    RegisterApplication selectById(Long id);

    /**
     * 提交注册申请（生成申请编号+待审核状态）
     */
    Long submitApplication(RegisterApplication registerApplication);

    /**
     * 重新提交申请（驳回后沿用原编号，auditCount+1）
     */
    int resubmitApplication(Long id, RegisterApplication update);

    /**
     * 部门管理员审核（通过/驳回）
     *
     * @param id            申请ID
     * @param action        PASS / REJECT
     * @param reason        驳回原因（驳回必填）
     * @return 审核记录
     */
    AuditRecord audit(Long id, String action, String reason, String mentorName, String mentorPhone);

    /** 查询申请审核历史 */
    List<AuditRecord> selectAuditHistory(Long id);

    /** 查询注册申请统计 */
    Map<String, Object> selectRegisterSummary();

    /** 按手机号和密码公开查询最近一条注册申请状态 */
    Map<String, Object> selectPublicStatusByPhone(String phone, String password);

    /** 按手机号查询最近一条申请，供驳回申请复提使用 */
    RegisterApplication selectLatestByLoginAccount(String loginAccount);

    /**
     * 校验同账号是否已有未结束申请
     */
    boolean checkUniqueActive(String loginAccount);
}
