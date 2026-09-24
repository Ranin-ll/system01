package com.ruoyi.business.service.impl;

import com.ruoyi.business.domain.InternRegisterBody;
import com.ruoyi.business.domain.AgreementSignBody;
import com.ruoyi.business.domain.Position;
import com.ruoyi.business.domain.RegisterApplication;
import com.ruoyi.business.mapper.InternAuthMapper;
import com.ruoyi.business.service.IInternAuthService;
import com.ruoyi.business.service.IRegisterApplicationService;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.exception.user.CaptchaException;
import com.ruoyi.common.exception.user.CaptchaExpireException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 实习生账号注册服务实现。
 */
@Service
public class InternAuthServiceImpl implements IInternAuthService {

    private static final String PRE_TRAINEE_ROLE_KEY = "PRE_TRAINEE";

    @Autowired
    private ISysUserService userService;

    @Autowired
    private IRegisterApplicationService registerApplicationService;

    @Autowired
    private InternAuthMapper internAuthMapper;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private RedisCache redisCache;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String register(InternRegisterBody body) {
        validate(body);
        validateCaptcha(body.getCode(), body.getUuid());

        // 身份证号：先去空格归一化，再做唯一性校验（一个身份证号只能注册一个账号）
        if (body.getIdCard() != null) {
            body.setIdCard(body.getIdCard().trim());
        }
        assertIdCardAvailable(body.getUsername(), body.getIdCard());

        Position position = internAuthMapper.selectEnabledPositionById(body.getPositionId());
        if (position == null || !Integer.valueOf(1).equals(position.getStatus())) {
            throw new ServiceException("所选岗位不可用");
        }
        Long deptId = internAuthMapper.selectDeptIdByPositionId(position.getId());
        if (deptId == null) {
            throw new ServiceException("所选岗位尚未配置归属部门，暂不能注册");
        }
        Long roleId = internAuthMapper.selectRoleIdByKey(PRE_TRAINEE_ROLE_KEY);
        if (roleId == null) {
            throw new ServiceException("预备实习生角色未初始化");
        }

        RegisterApplication previous = registerApplicationService.selectLatestByLoginAccount(body.getUsername());
        SysUser existingUser = userService.selectUserByPhone(body.getUsername());
        if (previous != null && "REJECTED".equals(previous.getStatus())) {
            if (existingUser == null) {
                throw new ServiceException("原申请账号数据不存在，请联系管理员");
            }
            if (existingUser.getPassword() == null
                    || !SecurityUtils.matchesPassword(body.getPassword(), existingUser.getPassword())) {
                throw new ServiceException("密码错误，重新提交需使用原申请密码");
            }
            Date expectedEntryDate = parseDate(body.getExpectedEntryDate());
            if (internAuthMapper.updateRejectedRegistrationProfile(existingUser.getUserId(), body.getRealName(),
                    deptId, position.getId(), expectedEntryDate, body.getIdCard()) != 1) {
                throw new ServiceException("账号资料更新失败");
            }
            previous.setRealName(body.getRealName());
            previous.setIdCard(body.getIdCard());
            previous.setPositionId(position.getId());
            previous.setDeptId(deptId);
            previous.setExpectedEntryDate(expectedEntryDate);
            registerApplicationService.resubmitApplication(previous.getId(), previous);
            return previous.getApplicationNo();
        }

        SysUser duplicate = new SysUser();
        duplicate.setUserName(body.getUsername());
        if (!userService.checkUserNameUnique(duplicate)) {
            throw new ServiceException("注册账号已存在");
        }
        duplicate.setPhonenumber(body.getUsername());
        if (!userService.checkPhoneUnique(duplicate)) {
            throw new ServiceException("该手机号已注册");
        }

        SysUser user = new SysUser();
        user.setUserName(body.getUsername());
        user.setNickName(body.getRealName());
        user.setPassword(SecurityUtils.encryptPassword(body.getPassword()));
        user.setDeptId(deptId);
        user.setPhonenumber(body.getUsername());
        // 申请提交后先禁止登录，审核通过时再由审核流程解除。
        user.setStatus("1");
        user.setRoleIds(new Long[] { roleId });
        user.setCreateBy("register");
        if (userService.insertUser(user) != 1) {
            throw new ServiceException("账号创建失败");
        }

        Date expectedEntryDate = parseDate(body.getExpectedEntryDate());
        internAuthMapper.updateRegistrationProfile(user.getUserId(), position.getId(), "WAIT_AUDIT",
                expectedEntryDate, body.getIdCard());

        RegisterApplication application = new RegisterApplication();
        application.setUserId(user.getUserId());
        application.setRealName(body.getRealName());
        application.setIdCard(body.getIdCard());
        application.setLoginAccount(user.getUserName());
        application.setPositionId(position.getId());
        application.setDeptId(deptId);
        application.setExpectedEntryDate(expectedEntryDate);
        registerApplicationService.submitApplication(application);
        return application.getApplicationNo();
    }

    @Override
    public Map<String, Object> getAgreementStatus(Long userId) {
        Map<String, Object> template = internAuthMapper.selectEffectiveAgreement();
        if (template == null) {
            throw new ServiceException("当前没有生效中的保密协议，请联系管理员");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.putAll(template);
        result.put("signed", Integer.valueOf(1).equals(internAuthMapper.selectProtocolStatus(userId)));
        return result;
    }

    @Override
    public Map<String, Object> getAgreementSignature(Long userId) {
        Map<String, Object> row = internAuthMapper.selectLatestAgreementSignature(userId);
        if (row == null) {
            return null;
        }
        // 拷一份再加工，别就地改 Mapper 返回的集合
        Map<String, Object> result = new LinkedHashMap<>(row);
        // 给签署记录一个可展示、可核对的凭证编号（「签署凭证」页用）
        Object id = result.get("id");
        if (id instanceof Number) {
            result.put("certificateNo", "RG-AG-" + String.format("%06d", ((Number) id).longValue()));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> signAgreement(Long userId, AgreementSignBody body) {
        if (body == null || !Boolean.TRUE.equals(body.getConfirmed())
                || StringUtils.isEmpty(body.getSignature())) {
            throw new ServiceException("请完成签名并确认已阅读保密协议");
        }
        String signature = body.getSignature().trim();
        if (signature.length() > 60000) {
            throw new ServiceException("签名图片过大，请清空后重新签署");
        }
        if (!signature.startsWith("data:image/")) {
            throw new ServiceException("请使用鼠标或触控设备完成签名");
        }
        Map<String, Object> template = internAuthMapper.selectEffectiveAgreement();
        if (template == null || template.get("id") == null) {
            throw new ServiceException("当前没有生效中的保密协议，请联系管理员");
        }
        Long templateId = ((Number) template.get("id")).longValue();
        if (internAuthMapper.countAgreementSignature(userId, templateId) == 0) {
            String userAgent = ServletUtils.getRequest().getHeader("User-Agent");
            String terminal = StringUtils.isEmpty(userAgent) ? "UNKNOWN" : userAgent.substring(0, Math.min(64, userAgent.length()));
            internAuthMapper.insertAgreementSignature(userId, templateId,
                    String.valueOf(template.get("agreementName")), String.valueOf(template.get("versionNo")),
                    IpUtils.getIpAddr(), terminal, signature);
            internAuthMapper.incrementAgreementSignCount(templateId);
        }
        internAuthMapper.updateProtocolStatus(userId, "agreement");
        return getAgreementStatus(userId);
    }

    /**
     * 身份证号唯一性守卫：一个身份证号只能对应一个注册账号。
     *
     * 统计时**排除本登录账号自己的记录**，所以以下两种正常场景不会被拦：
     *   · 被驳回后沿用原手机号重新提交（库里本来就有这个身份证）；
     *   · 重复手机号提交（该身份证本就挂在自己名下，随后会由「注册账号已存在」接管提示）。
     * 而「换个手机号、用同一个身份证再申请」会被拒绝。
     */
    private void assertIdCardAvailable(String loginAccount, String idCard) {
        if (StringUtils.isEmpty(idCard)) {
            return;
        }
        if (internAuthMapper.countOtherAccountByIdCard(idCard, loginAccount) > 0) {
            throw new ServiceException("该身份证号已被其他账号使用，一个身份证号只能注册一次");
        }
    }

    private void validate(InternRegisterBody body) {
        if (body == null || StringUtils.isEmpty(body.getUsername()) || StringUtils.isEmpty(body.getPassword())
                || StringUtils.isEmpty(body.getRealName()) || body.getPositionId() == null) {
            throw new ServiceException("手机号、密码、姓名和意向岗位不能为空");
        }
        if (!body.getUsername().matches("^1[3-9]\\d{9}$")) {
            throw new ServiceException("个人账号必须填写有效的11位手机号");
        }
        if (body.getUsername().length() < UserConstants.USERNAME_MIN_LENGTH
                || body.getUsername().length() > UserConstants.USERNAME_MAX_LENGTH) {
            throw new ServiceException("账号长度必须在2到20个字符之间");
        }
        if (body.getPassword().length() < UserConstants.PASSWORD_MIN_LENGTH
                || body.getPassword().length() > UserConstants.PASSWORD_MAX_LENGTH) {
            throw new ServiceException("密码长度必须在5到20个字符之间");
        }
        if (body.getExpectedEntryDate() != null && !body.getExpectedEntryDate().isEmpty()) {
            parseDate(body.getExpectedEntryDate());
        }
    }

    private void validateCaptcha(String code, String uuid) {
        if (!configService.selectCaptchaEnabled()) {
            return;
        }
        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + StringUtils.nvl(uuid, "");
        String captcha = redisCache.getCacheObject(verifyKey);
        redisCache.deleteObject(verifyKey);
        if (captcha == null) {
            throw new CaptchaExpireException();
        }
        if (StringUtils.isEmpty(code) || !code.equalsIgnoreCase(captcha)) {
            throw new CaptchaException();
        }
    }

    private Date parseDate(String date) {
        if (StringUtils.isEmpty(date)) {
            return null;
        }
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            dateFormat.setLenient(false);
            return dateFormat.parse(date);
        } catch (ParseException e) {
            throw new ServiceException("预计入职日期格式应为 yyyy-MM-dd");
        }
    }
}
