package com.ruoyi.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.business.domain.QuestionBank;
import com.ruoyi.business.mapper.QuestionBankMapper;
import com.ruoyi.business.service.IQuestionBankService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 题库Service实现
 *
 * 数据范围控制（与课程模块一致）：
 * - 超级管理员：查看全部题库，并可指定部门进行维护。
 * - 部门管理员：查看并管理本部门题库。
 * - 实习生：仅查看本部门题库。
 */
@Service
public class QuestionBankServiceImpl extends ServiceImpl<QuestionBankMapper, QuestionBank> implements IQuestionBankService {

    private final QuestionBankMapper questionBankMapper;

    public QuestionBankServiceImpl(QuestionBankMapper questionBankMapper) {
        this.questionBankMapper = questionBankMapper;
    }

    @Override
    public List<QuestionBank> selectBankList(QuestionBank bank) {
        bank.setScopeDeptId(currentScopeDeptId());
        return questionBankMapper.selectBankList(bank);
    }

    @Override
    public QuestionBank selectById(Long id) {
        QuestionBank bank = questionBankMapper.selectBankById(id, currentScopeDeptId());
        if (bank == null) {
            throw new ServiceException("题库不存在或无权访问");
        }
        return bank;
    }

    @Override
    public int insertBank(QuestionBank bank) {
        Long deptId = managerScopeDeptId();
        if (deptId == null) {
            deptId = bank.getDeptId();
        }
        if (deptId == null) {
            throw new ServiceException("请选择所属部门");
        }
        bank.setDeptId(deptId);
        // 用途标签（三值）：未选时默认「通用」——通用库正式/模拟考核都可用，语义最安全
        bank.setBankType(normalizeBankType(bank.getBankType()));
        // 形态（理论/实操）：未选时默认理论题库
        bank.setBankKind(normalizeBankKind(bank.getBankKind()));
        bank.setCreateBy(SecurityUtils.getUsername());
        bank.setStatus("ENABLED");
        bank.setQuestionCount(0);
        bank.setDeleted(0);
        bank.setCreateTime(new Date());
        return questionBankMapper.insertBank(bank);
    }

    @Override
    public int updateBank(QuestionBank bank) {
        managerScopeDeptId();
        getAccessibleBank(bank.getId());
        // 形态：传了就校验（理论题库 / 实操题库）；未传保持原值
        if (bank.getBankKind() != null && !bank.getBankKind().isEmpty()) {
            bank.setBankKind(normalizeBankKind(bank.getBankKind()));
        } else {
            bank.setBankKind(null);
        }
        // 用途标签：传了就校验（正式考核题库 / 模拟考核题库 / 通用题库）
        if (bank.getBankType() != null && !bank.getBankType().isEmpty()) {
            bank.setBankType(normalizeBankType(bank.getBankType()));
        } else {
            bank.setBankType(null);
        }
        // 不允许通过通用编辑接口篡改部门归属、删除标志或创建信息。
        bank.setDeptId(null);
        bank.setDeleted(null);
        bank.setCreateBy(null);
        bank.setCreateTime(null);
        bank.setQuestionCount(null);
        return questionBankMapper.updateBank(bank);
    }

    @Override
    public int deleteByIds(Long[] ids) {
        managerScopeDeptId();
        if (ids == null || ids.length == 0) {
            return 0;
        }
        int count = 0;
        for (Long id : ids) {
            getAccessibleBank(id);
            // 题库均可删除（用途标签只表达"参与哪种考核"，不是删除限制）；
            // 若该库被考核引用，前端会在确认框里提示影响。
            questionBankMapper.deleteBankById(id);
            count++;
        }
        return count;
    }

    /**
     * 用途标签归一化：正式考核题库 FORMAL / 模拟考核题库 PRACTICE / 通用题库 COMMON。
     * 空值或非法值一律按「通用」处理，避免题库因标签缺失而两种考核都用不了。
     */
    /**
     * 题库形态归一化：THEORY 理论题库 / PRACTICAL 实操题库。空值或非法值按「理论题库」处理
     * （存量数据都是理论题，默认值必须向后兼容）。
     */
    private String normalizeBankKind(String bankKind) {
        if (bankKind == null || bankKind.trim().isEmpty()) {
            return "THEORY";
        }
        String t = bankKind.trim().toUpperCase();
        if ("PRACTICAL".equals(t) || "THEORY".equals(t)) {
            return t;
        }
        String cn = bankKind.trim();
        if ("实操题库".equals(cn) || "实操".equals(cn) || "PRACTICE_SUBJECT".equals(t)) {
            return "PRACTICAL";
        }
        return "THEORY";
    }

    private String normalizeBankType(String bankType) {
        if (bankType == null) {
            return "COMMON";
        }
        String t = bankType.trim().toUpperCase();
        if ("FORMAL".equals(t) || "PRACTICE".equals(t) || "COMMON".equals(t)) {
            return t;
        }
        if ("正式".equals(bankType.trim()) || "正式考核题库".equals(bankType.trim())) {
            return "FORMAL";
        }
        if ("模拟".equals(bankType.trim()) || "模拟考核题库".equals(bankType.trim())) {
            return "PRACTICE";
        }
        if ("通用".equals(bankType.trim()) || "通用题库".equals(bankType.trim())) {
            return "COMMON";
        }
        return "COMMON";
    }

    /** 超级管理员返回 null（看全部），部门管理员/实习生返回本部门ID。 */
    private Long currentScopeDeptId() {
        if (isGlobalReadOnly()) {
            return null;
        }
        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null) {
            throw new ServiceException("当前账号未配置部门，无法访问题库数据");
        }
        return deptId;
    }

    private Long managerScopeDeptId() {
        if (isGlobalReadOnly()) {
            return null;
        }
        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null) {
            throw new ServiceException("当前账号未配置部门，无法管理题库");
        }
        return deptId;
    }

    private QuestionBank getAccessibleBank(Long id) {
        if (id == null) {
            throw new ServiceException("题库ID不能为空");
        }
        QuestionBank bank = questionBankMapper.selectBankById(id, currentScopeDeptId());
        if (bank == null) {
            throw new ServiceException("题库不存在或无权操作");
        }
        return bank;
    }

    private boolean isGlobalReadOnly() {
        return SecurityUtils.hasRole("SUPER_ADMIN") || SecurityUtils.isAdmin(SecurityUtils.getUserId());
    }
}
