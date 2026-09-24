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
 * - 超级管理员：查看全部实操题，并可指定部门进行维护。
 * - 部门管理员：发布并维护本部门实操题。
 * - 实习生：只看本部门「已启用」的实操题，只读、只下载附件。
 *
 * ★ 2026-09-23：**不再分模块/阶段** —— practice_module 这一层退场，
 * 模拟实操题只按 dept_id 归属部门 + sort_no 排序（module_id 列保留在库中但不再读写）。
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
        Long deptId = managerScopeDeptId();
        if (deptId == null) {
            deptId = subject.getDeptId();
        }
        if (deptId == null) {
            throw new ServiceException("请选择所属部门");
        }
        subject.setDeptId(deptId);
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
