package com.ruoyi.business.service.impl;

import com.ruoyi.business.domain.PracticeSubject;
import com.ruoyi.business.mapper.PracticeSubjectMapper;
import com.ruoyi.business.service.IPracticeSubjectService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 模拟考核实操题 Service 实现
 *
 * 数据范围控制（与题库/课程模块保持一致）：
 * - 超级管理员：查看全部实操题，仅只读。
 * - 部门管理员：发布并维护本部门实操题。
 * - 实习生：只看本部门「已启用」的实操题，只读、只下载附件。
 */
@Service
public class PracticeSubjectServiceImpl implements IPracticeSubjectService {

    private final PracticeSubjectMapper practiceSubjectMapper;

    public PracticeSubjectServiceImpl(PracticeSubjectMapper practiceSubjectMapper) {
        this.practiceSubjectMapper = practiceSubjectMapper;
    }

    @Override
    public List<PracticeSubject> selectSubjectList(PracticeSubject query) {
        if (query == null) {
            query = new PracticeSubject();
        }
        query.setScopeDeptId(currentScopeDeptId());
        return practiceSubjectMapper.selectSubjectList(query);
    }

    @Override
    public PracticeSubject selectSubjectById(Long id) {
        return getAccessibleSubject(id);
    }

    @Override
    public int insertSubject(PracticeSubject subject) {
        if (subject == null || subject.getContent() == null || subject.getContent().trim().isEmpty()) {
            throw new ServiceException("请填写题干");
        }
        subject.setId(null);
        subject.setDeptId(managerScopeDeptId());
        subject.setContent(subject.getContent().trim());
        subject.setStatus(subject.getStatus() == null ? 1 : subject.getStatus());
        subject.setCreateBy(SecurityUtils.getUsername());
        subject.setDeleted(0);
        return practiceSubjectMapper.insertSubject(subject);
    }

    @Override
    public int updateSubject(PracticeSubject subject) {
        if (subject == null || subject.getId() == null) {
            throw new ServiceException("实操题ID不能为空");
        }
        managerScopeDeptId();
        getAccessibleSubject(subject.getId());
        if (subject.getContent() != null) {
            if (subject.getContent().trim().isEmpty()) {
                throw new ServiceException("请填写题干");
            }
            subject.setContent(subject.getContent().trim());
        }
        // 不允许通过通用编辑接口篡改部门归属、创建信息或删除标志
        subject.setDeptId(null);
        subject.setCreateBy(null);
        subject.setDeleted(null);
        return practiceSubjectMapper.updateSubject(subject);
    }

    @Override
    public int deleteByIds(Long[] ids) {
        managerScopeDeptId();
        if (ids == null || ids.length == 0) {
            return 0;
        }
        int count = 0;
        for (Long id : ids) {
            getAccessibleSubject(id);
            practiceSubjectMapper.deleteSubjectById(id);
            count++;
        }
        return count;
    }

    @Override
    public int changeStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new ServiceException("状态值不合法");
        }
        managerScopeDeptId();
        getAccessibleSubject(id);
        PracticeSubject update = new PracticeSubject();
        update.setId(id);
        update.setStatus(status);
        return practiceSubjectMapper.updateSubject(update);
    }

    @Override
    public List<PracticeSubject> selectPublishedForIntern() {
        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null) {
            throw new ServiceException("当前账号未配置部门，无法查看实操练习");
        }
        List<PracticeSubject> list = practiceSubjectMapper.selectPublishedByDept(deptId);
        return list == null ? new ArrayList<>() : list;
    }

    @Override
    public PracticeSubject selectPublishedDetailForIntern(Long id) {
        if (id == null) {
            throw new ServiceException("实操题ID不能为空");
        }
        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null) {
            throw new ServiceException("当前账号未配置部门，无法查看实操练习");
        }
        PracticeSubject subject = practiceSubjectMapper.selectSubjectById(id);
        // 不存在、已删除、非本部门、未启用，统一按"不存在"处理，避免暴露其它部门的题目
        if (subject == null
                || !deptId.equals(subject.getDeptId())
                || subject.getStatus() == null
                || subject.getStatus() != 1) {
            throw new ServiceException("实操题不存在或未发布");
        }
        return subject;
    }

    /** 超级管理员返回 null（看全部），部门管理员/实习生返回本部门ID。 */
    private Long currentScopeDeptId() {
        if (isGlobalReadOnly()) {
            return null;
        }
        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null) {
            throw new ServiceException("当前账号未配置部门，无法访问实操题数据");
        }
        return deptId;
    }

    private Long managerScopeDeptId() {
        if (isGlobalReadOnly()) {
            throw new ServiceException("超级管理员仅可查看实操题，不能执行写入操作");
        }
        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null) {
            throw new ServiceException("当前账号未配置部门，无法管理实操题");
        }
        return deptId;
    }

    private PracticeSubject getAccessibleSubject(Long id) {
        if (id == null) {
            throw new ServiceException("实操题ID不能为空");
        }
        PracticeSubject subject = practiceSubjectMapper.selectSubjectById(id);
        if (subject == null) {
            throw new ServiceException("实操题不存在或已被删除");
        }
        Long scope = currentScopeDeptId();
        if (scope != null && !scope.equals(subject.getDeptId())) {
            throw new ServiceException("实操题不存在或无权操作");
        }
        return subject;
    }

    private boolean isGlobalReadOnly() {
        return SecurityUtils.hasRole("SUPER_ADMIN") || SecurityUtils.isAdmin(SecurityUtils.getUserId());
    }
}
