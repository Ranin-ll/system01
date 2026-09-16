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
 * - 超级管理员：查看全部题库，仅只读。
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
        bank.setDeptId(deptId);
        if (bank.getBankType() == null || bank.getBankType().isEmpty()) {
            bank.setBankType("FORMAL");
        }
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
            QuestionBank bank = getAccessibleBank(id);
            if ("PRACTICE".equals(bank.getBankType())) {
                throw new ServiceException("模拟题库为部门自带题库，不可删除");
            }
            questionBankMapper.deleteBankById(id);
            count++;
        }
        return count;
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
            throw new ServiceException("超级管理员仅可查看题库，不能执行写入操作");
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
