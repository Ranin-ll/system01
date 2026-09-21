package com.ruoyi.business.service.impl;

import com.ruoyi.business.domain.PracticeModule;
import com.ruoyi.business.mapper.PracticeModuleMapper;
import com.ruoyi.business.service.IPracticeModuleService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 模拟考核模块 Service 实现
 *
 * 数据范围控制（与题库 / 实操题模块保持一致）：
 * - 超级管理员：查看全部模块，并可指定部门进行维护。
 * - 部门管理员：新建并维护本部门模块。
 * - 实习生：只看本部门「已启用」的模块，用于「先选模块再看考核」。
 */
@Service
public class PracticeModuleServiceImpl implements IPracticeModuleService {

    private final PracticeModuleMapper practiceModuleMapper;

    public PracticeModuleServiceImpl(PracticeModuleMapper practiceModuleMapper) {
        this.practiceModuleMapper = practiceModuleMapper;
    }

    @Override
    public List<PracticeModule> selectModuleList(PracticeModule query) {
        if (query == null) {
            query = new PracticeModule();
        }
        query.setScopeDeptId(currentScopeDeptId());
        return practiceModuleMapper.selectModuleList(query);
    }

    @Override
    public PracticeModule selectModuleById(Long id) {
        return getAccessibleModule(id);
    }

    @Override
    public int insertModule(PracticeModule module) {
        if (module == null || isBlank(module.getName())) {
            throw new ServiceException("请填写模块名称");
        }
        module.setId(null);
        Long deptId = managerScopeDeptId();
        if (deptId == null) {
            deptId = module.getDeptId();
        }
        if (deptId == null) {
            throw new ServiceException("请选择所属部门");
        }
        module.setDeptId(deptId);
        module.setName(module.getName().trim());
        module.setDescription(trimN(module.getDescription()));
        assertNameUnique(deptId, module.getName(), null);
        if (module.getSortNo() == null) {
            module.setSortNo(0);
        }
        module.setStatus(module.getStatus() == null ? 1 : module.getStatus());
        module.setCreateBy(SecurityUtils.getUsername());
        module.setDeleted(0);
        return practiceModuleMapper.insertModule(module);
    }

    @Override
    public int updateModule(PracticeModule module) {
        if (module == null || module.getId() == null) {
            throw new ServiceException("模块ID不能为空");
        }
        managerScopeDeptId();
        PracticeModule exist = getAccessibleModule(module.getId());
        if (module.getName() != null) {
            if (module.getName().trim().isEmpty()) {
                throw new ServiceException("请填写模块名称");
            }
            module.setName(module.getName().trim());
            assertNameUnique(exist.getDeptId(), module.getName(), module.getId());
        }
        module.setDescription(trimN(module.getDescription()));
        // 不允许通过通用编辑接口篡改部门归属、创建信息或删除标志
        module.setDeptId(null);
        module.setCreateBy(null);
        module.setDeleted(null);
        return practiceModuleMapper.updateModule(module);
    }

    @Override
    public int deleteByIds(Long[] ids) {
        managerScopeDeptId();
        if (ids == null || ids.length == 0) {
            return 0;
        }
        int count = 0;
        for (Long id : ids) {
            getAccessibleModule(id);
            int subjects = practiceModuleMapper.countSubjects(id);
            int exams = practiceModuleMapper.countExams(id);
            if (subjects > 0 || exams > 0) {
                throw new ServiceException("模块下还有 " + subjects + " 道实操题 / " + exams
                        + " 个理论模拟考核，请先移出或删除后再删模块");
            }
            practiceModuleMapper.deleteModuleById(id);
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
        getAccessibleModule(id);
        PracticeModule update = new PracticeModule();
        update.setId(id);
        update.setStatus(status);
        return practiceModuleMapper.updateModule(update);
    }

    @Override
    public List<PracticeModule> listOptions(Long deptId) {
        Long scope = currentScopeDeptId();
        if (scope != null) {
            // 部门管理员只能看本部门
            return practiceModuleMapper.selectByDept(scope, false);
        }
        if (deptId != null) {
            return practiceModuleMapper.selectByDept(deptId, false);
        }
        // 超级管理员未指定部门：按当前数据范围列出全部
        PracticeModule query = new PracticeModule();
        query.setScopeDeptId(null);
        return practiceModuleMapper.selectModuleList(query);
    }

    @Override
    public List<PracticeModule> selectEnabledForIntern() {
        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null) {
            throw new ServiceException("当前账号未配置部门，无法查看模拟考核模块");
        }
        List<PracticeModule> list = practiceModuleMapper.selectByDept(deptId, true);
        return list == null ? new ArrayList<>() : list;
    }

    // ---------------- 内部辅助 ----------------

    private void assertNameUnique(Long deptId, String name, Long excludeId) {
        PracticeModule same = practiceModuleMapper.selectByName(deptId, name, excludeId);
        if (same != null) {
            throw new ServiceException("本部门已存在同名模块「" + name + "」，请换一个名称");
        }
    }

    private PracticeModule getAccessibleModule(Long id) {
        if (id == null) {
            throw new ServiceException("模块ID不能为空");
        }
        PracticeModule module = practiceModuleMapper.selectModuleById(id);
        if (module == null) {
            throw new ServiceException("模块不存在或已被删除");
        }
        Long scope = currentScopeDeptId();
        if (scope != null && !scope.equals(module.getDeptId())) {
            throw new ServiceException("模块不存在或无权操作");
        }
        return module;
    }

    /** 超级管理员返回 null（看全部），部门管理员返回本部门ID。 */
    private Long currentScopeDeptId() {
        if (isGlobalReadOnly()) {
            return null;
        }
        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null) {
            throw new ServiceException("当前账号未配置部门，无法访问模块数据");
        }
        return deptId;
    }

    private Long managerScopeDeptId() {
        if (isGlobalReadOnly()) {
            return null;
        }
        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null) {
            throw new ServiceException("当前账号未配置部门，无法管理模块");
        }
        return deptId;
    }

    private boolean isGlobalReadOnly() {
        return SecurityUtils.hasRole("SUPER_ADMIN") || SecurityUtils.isAdmin(SecurityUtils.getUserId());
    }

    private String trimN(String v) {
        return v == null ? null : v.trim();
    }

    private boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }
}
