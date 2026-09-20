package com.ruoyi.business.service.impl;

import com.ruoyi.business.domain.PracticeModule;
import com.ruoyi.business.domain.PracticeSubject;
import com.ruoyi.business.mapper.PracticeModuleMapper;
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
 * - 超级管理员：查看全部实操题，并可指定部门进行维护。
 * - 部门管理员：发布并维护本部门实操题。
 * - 实习生：只看本部门「已启用」的实操题，只读、只下载附件。
 *
 * 「模块」层：实操题必须归属某个模块（practice_module），模块取代原「方向」的顶层分组地位。
 */
@Service
public class PracticeSubjectServiceImpl implements IPracticeSubjectService {

    private final PracticeSubjectMapper practiceSubjectMapper;
    private final PracticeModuleMapper practiceModuleMapper;

    public PracticeSubjectServiceImpl(PracticeSubjectMapper practiceSubjectMapper,
                                      PracticeModuleMapper practiceModuleMapper) {
        this.practiceSubjectMapper = practiceSubjectMapper;
        this.practiceModuleMapper = practiceModuleMapper;
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
        Long deptId = managerScopeDeptId();
        if (deptId == null) {
            deptId = subject.getDeptId();
        }
        if (deptId == null) {
            throw new ServiceException("请选择所属部门");
        }
        subject.setDeptId(deptId);
        // 模块必填：先建模块，再在模块内上传实操题
        if (subject.getModuleId() == null) {
            throw new ServiceException("请选择所属模块（模拟考核先建模块）");
        }
        assertModuleUsable(subject.getModuleId(), deptId, false);
        subject.setContent(subject.getContent().trim());
        normalizeForWrite(subject);
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
        PracticeSubject exist = getAccessibleSubject(subject.getId());
        if (subject.getModuleId() != null) {
            assertModuleUsable(subject.getModuleId(), exist.getDeptId(), true);
        }
        if (subject.getContent() != null) {
            if (subject.getContent().trim().isEmpty()) {
                throw new ServiceException("请填写题干");
            }
            subject.setContent(subject.getContent().trim());
        }
        // 题名若传了空串，视为「未修改」，避免把已有题名清掉导致卡片无标题
        if (subject.getTitle() != null && subject.getTitle().trim().isEmpty()) {
            subject.setTitle(null);
        }
        trimAll(subject);
        // 不允许通过通用编辑接口篡改部门归属、创建信息或删除标志
        subject.setDeptId(null);
        subject.setCreateBy(null);
        subject.setDeleted(null);
        return practiceSubjectMapper.updateSubject(subject);
    }

    /**
     * 校验模块可用：模块存在、未删除、归属同一部门；forWrite=false（新建时）还要求模块处于启用状态。
     * 停用的模块允许继续维护其下已有题目（编辑场景），但不允许再往里新增。
     */
    private void assertModuleUsable(Long moduleId, Long deptId, boolean forEdit) {
        PracticeModule module = practiceModuleMapper.selectModuleById(moduleId);
        if (module == null) {
            throw new ServiceException("所选模块不存在或已被删除");
        }
        if (deptId != null && !deptId.equals(module.getDeptId())) {
            throw new ServiceException("所选模块不属于该部门");
        }
        if (!forEdit && module.getStatus() != null && module.getStatus() != 1) {
            throw new ServiceException("所选模块已停用，请先启用或换一个模块");
        }
    }

    /**
     * 新增时的字段归一化：
     * - 题名缺省时用题干前 40 字兜底（保证卡片/详情页一定有标题）
     * - 方向缺省归入「通用」，并给一句方向说明
     * - 难度缺省 MEDIUM，排序号缺省 0
     */
    private void normalizeForWrite(PracticeSubject subject) {
        trimAll(subject);
        if (isBlank(subject.getTitle())) {
            String c = subject.getContent() == null ? "" : subject.getContent();
            subject.setTitle(c.length() > 40 ? c.substring(0, 40) : c);
        }
        if (isBlank(subject.getDirection())) {
            subject.setDirection("通用");
        } else {
            subject.setDirection(subject.getDirection().trim());
        }
        if (isBlank(subject.getDirectionDesc())) {
            subject.setDirectionDesc("本方向实操练习题");
        }
        if (isBlank(subject.getDifficulty())) {
            subject.setDifficulty("MEDIUM");
        }
        if (subject.getSortNo() == null) {
            subject.setSortNo(0);
        }
    }

    /** 可选文本字段统一 trim（null 保持不变，以便 update 的 <if test="!= null"> 生效） */
    private void trimAll(PracticeSubject s) {
        s.setTitle(trimN(s.getTitle()));
        s.setDirection(trimN(s.getDirection()));
        s.setDirectionDesc(trimN(s.getDirectionDesc()));
        s.setDifficulty(trimN(s.getDifficulty()));
        s.setDeliverables(trimN(s.getDeliverables()));
        s.setDevConstraints(trimN(s.getDevConstraints()));
        s.setSubmitFormat(trimN(s.getSubmitFormat()));
        s.setNamingRule(trimN(s.getNamingRule()));
        s.setReferenceImages(trimN(s.getReferenceImages()));
    }

    private String trimN(String v) {
        return v == null ? null : v.trim();
    }

    private boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
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
    public List<PracticeSubject> selectPublishedForIntern(Long moduleId) {
        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null) {
            throw new ServiceException("当前账号未配置部门，无法查看实操练习");
        }
        if (moduleId != null) {
            // 模块必须是本部门启用中的模块，避免拿到别的部门/已停用模块下的题
            PracticeModule module = practiceModuleMapper.selectModuleById(moduleId);
            if (module == null || !deptId.equals(module.getDeptId())
                    || module.getStatus() == null || module.getStatus() != 1) {
                throw new ServiceException("模块不存在或未启用");
            }
        }
        List<PracticeSubject> list = practiceSubjectMapper.selectPublishedByDept(deptId, moduleId);
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
            return null;
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
