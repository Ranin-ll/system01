package com.ruoyi.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.business.domain.Exam;
import com.ruoyi.business.mapper.ExamMapper;
import com.ruoyi.business.service.IExamService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 考核Service实现
 *
 * 数据范围：超级管理员看全部（只读），部门管理员管理本部门考核。
 */
@Service
public class ExamServiceImpl extends ServiceImpl<ExamMapper, Exam> implements IExamService {

    private final ExamMapper examMapper;

    public ExamServiceImpl(ExamMapper examMapper) {
        this.examMapper = examMapper;
    }

    @Override
    public List<Exam> selectExamList(Exam exam) {
        exam.setScopeDeptId(currentScopeDeptId());
        return examMapper.selectExamList(exam);
    }

    @Override
    public Exam selectById(Long id) {
        Exam exam = examMapper.selectExamById(id, currentScopeDeptId());
        if (exam == null) {
            throw new ServiceException("考核不存在或无权访问");
        }
        return exam;
    }

    @Override
    public int insertExam(Exam exam) {
        Long deptId = managerScopeDeptId();
        exam.setDeptId(deptId);
        exam.setStatus("DRAFT");
        String type = exam.getExamType() == null ? "THEORY" : exam.getExamType();
        exam.setExamType(type);
        if (exam.getExamMode() == null || exam.getExamMode().trim().isEmpty()) {
            exam.setExamMode("FORMAL");
        }
        if ("THEORY".equals(type)) {
            if (exam.getBankId() == null) {
                throw new ServiceException("理论考核必须关联题库");
            }
            if (exam.getSingleCount() == null) exam.setSingleCount(0);
            if (exam.getMultiCount() == null) exam.setMultiCount(0);
            if (exam.getJudgeCount() == null) exam.setJudgeCount(0);
            if (exam.getSingleScore() == null) exam.setSingleScore(java.math.BigDecimal.ZERO);
            if (exam.getMultiScore() == null) exam.setMultiScore(java.math.BigDecimal.ZERO);
            if (exam.getJudgeScore() == null) exam.setJudgeScore(java.math.BigDecimal.ZERO);
            int total = exam.getSingleCount() + exam.getMultiCount() + exam.getJudgeCount();
            exam.setQuestionCount(total);
            exam.setSubjectContent(null);
            exam.setSubjectAttachment(null);
        } else if ("PRACTICAL".equals(type)) {
            if (exam.getSubjectContent() == null || exam.getSubjectContent().trim().isEmpty()) {
                throw new ServiceException("实操考核必须填写题干");
            }
            exam.setBankId(null);
            exam.setQuestionCount(0);
            exam.setSingleCount(0);
            exam.setMultiCount(0);
            exam.setJudgeCount(0);
            exam.setSingleScore(java.math.BigDecimal.ZERO);
            exam.setMultiScore(java.math.BigDecimal.ZERO);
            exam.setJudgeScore(java.math.BigDecimal.ZERO);
        } else {
            throw new ServiceException("考核类型不正确");
        }
        if (exam.getPassLine() == null) {
            exam.setPassLine(new java.math.BigDecimal("60"));
        }
        if (exam.getSubjectCount() == null) {
            exam.setSubjectCount(0);
        }
        if (exam.getDuration() == null) {
            exam.setDuration(0);
        }
        exam.setDeleted(0);
        exam.setCreateTime(new Date());
        return examMapper.insertExam(exam);
    }

    @Override
    public int updateExam(Exam exam) {
        managerScopeDeptId();
        getAccessibleExam(exam.getId());
        // 已发布/批改中的考核不允许修改基本信息
        Exam exist = examMapper.selectExamById(exam.getId(), null);
        if (exist != null && !"DRAFT".equals(exist.getStatus()) && !"DISABLED".equals(exist.getStatus())) {
            throw new ServiceException("考核已发布或批改中，不能修改");
        }
        exam.setDeptId(null);
        exam.setDeleted(null);
        return examMapper.updateExam(exam);
    }

    @Override
    public int deleteByIds(Long[] ids) {
        managerScopeDeptId();
        if (ids == null || ids.length == 0) {
            return 0;
        }
        int count = 0;
        for (Long id : ids) {
            getAccessibleExam(id);
            examMapper.deleteExamById(id);
            count++;
        }
        return count;
    }

    @Override
    public int publish(Long id) {
        managerScopeDeptId();
        Exam exam = getAccessibleExam(id);
        if (!"DRAFT".equals(exam.getStatus())) {
            throw new ServiceException("仅待发布状态的考核可以发布");
        }
        Exam update = new Exam();
        update.setId(id);
        update.setStatus("PUBLISHED");
        update.setPublishedAt(new Date());
        update.setPublisherId(SecurityUtils.getUserId());
        return examMapper.updateExam(update);
    }

    @Override
    public int changeStatus(Long id, String status) {
        managerScopeDeptId();
        getAccessibleExam(id);
        if (!"PUBLISHED".equals(status) && !"DISABLED".equals(status)) {
            throw new ServiceException("状态仅支持启用或停用");
        }
        Exam upd = new Exam();
        upd.setId(id);
        upd.setStatus(status);
        return examMapper.updateExam(upd);
    }

    private Long currentScopeDeptId() {
        if (isGlobalReadOnly()) {
            return null;
        }
        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null) {
            throw new ServiceException("当前账号未配置部门，无法访问考核数据");
        }
        return deptId;
    }

    private Long managerScopeDeptId() {
        if (isGlobalReadOnly()) {
            throw new ServiceException("超级管理员仅可查看考核，不能执行写入操作");
        }
        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null) {
            throw new ServiceException("当前账号未配置部门，无法管理考核");
        }
        return deptId;
    }

    private Exam getAccessibleExam(Long id) {
        if (id == null) {
            throw new ServiceException("考核ID不能为空");
        }
        Exam exam = examMapper.selectExamById(id, currentScopeDeptId());
        if (exam == null) {
            throw new ServiceException("考核不存在或无权操作");
        }
        return exam;
    }

    private boolean isGlobalReadOnly() {
        return SecurityUtils.hasRole("SUPER_ADMIN") || SecurityUtils.isAdmin(SecurityUtils.getUserId());
    }
}
