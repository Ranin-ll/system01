package com.ruoyi.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.business.domain.Exam;
import com.ruoyi.business.domain.QuestionBank;
import com.ruoyi.business.mapper.ExamMapper;
import com.ruoyi.business.mapper.QuestionBankMapper;
import com.ruoyi.business.service.IExamService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 考核Service实现
 *
 * 数据范围：超级管理员查看全部并可指定部门管理，部门管理员管理本部门考核。
 */
@Service
public class ExamServiceImpl extends ServiceImpl<ExamMapper, Exam> implements IExamService {

    private final ExamMapper examMapper;
    private final QuestionBankMapper questionBankMapper;
    private final com.ruoyi.business.mapper.ExamRuleMapper examRuleMapper;

    public ExamServiceImpl(ExamMapper examMapper, QuestionBankMapper questionBankMapper,
                           com.ruoyi.business.mapper.ExamRuleMapper examRuleMapper) {
        this.examMapper = examMapper;
        this.questionBankMapper = questionBankMapper;
        this.examRuleMapper = examRuleMapper;
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
        if (deptId == null) {
            deptId = exam.getDeptId();
        }
        if (deptId == null) {
            throw new ServiceException("请选择所属部门");
        }
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
            validateBankDept(exam.getBankId(), deptId);
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
        Exam exist = getAccessibleExam(exam.getId());
        // 已发布/批改中的考核不允许修改基本信息
        if (exist != null && !"DRAFT".equals(exist.getStatus()) && !"DISABLED".equals(exist.getStatus())) {
            throw new ServiceException("考核已发布或批改中，不能修改");
        }
        if ("THEORY".equals(exist.getExamType()) && exam.getBankId() != null) {
            validateBankDept(exam.getBankId(), exist.getDeptId());
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
        return publish(id, null);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public int publish(Long id, Exam params) {
        managerScopeDeptId();
        Exam exam = getAccessibleExam(id);
        if (!"DRAFT".equals(exam.getStatus())) {
            throw new ServiceException("仅待发布状态的考核可以发布");
        }
        // 发布前校验知识分布（若配了）：占比合计 100%、抽题数量合计 = 题目数量、不超题库可用题量
        validateKnowledgeRules(exam, params);
        saveKnowledgeRules(id, params);
        saveParticipants(id, params, exam);

        Exam update = new Exam();
        update.setId(id);
        update.setStatus("PUBLISHED");
        update.setPublishedAt(new Date());
        update.setPublisherId(SecurityUtils.getUserId());
        if (params != null) {
            update.setStartTime(params.getStartTime());
            update.setEndTime(params.getEndTime());
        }
        return examMapper.updateExam(update);
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> bankKnowledgePoints(Long bankId) {
        if (bankId == null) {
            throw new ServiceException("请先选择题库");
        }
        // 数据范围校验：只能看本部门题库
        QuestionBank bank = questionBankMapper.selectBankById(bankId, currentScopeDeptId());
        if (bank == null) {
            throw new ServiceException("题库不存在或无权访问");
        }
        return examRuleMapper.selectBankKnowledgePoints(bankId);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public int saveConfig(Long id, Exam params) {
        managerScopeDeptId();
        Exam exam = getAccessibleExam(id);
        validateKnowledgeRules(exam, params);
        saveKnowledgeRules(id, params);
        saveParticipants(id, params, exam);
        Exam update = new Exam();
        update.setId(id);
        if (params != null) {
            update.setStartTime(params.getStartTime());
            update.setEndTime(params.getEndTime());
        }
        return examMapper.updateExam(update);
    }

    @Override
    public java.util.Map<String, Object> configDetail(Long id) {
        Exam exam = getAccessibleExam(id);
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("examId", exam.getId());
        result.put("knowledgeRules", examRuleMapper.selectKnowledgeRules(id));
        java.util.List<Long> userIds = examRuleMapper.selectParticipantUserIds(id);
        result.put("participantIds", userIds);
        result.put("participants", examRuleMapper.selectParticipants(id));
        result.put("assignMode", userIds.isEmpty() ? "ALL" : "ASSIGNED");
        result.put("startTime", exam.getStartTime());
        result.put("endTime", exam.getEndTime());
        return result;
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> tryDraw(Long bankId, Exam params) {
        if (bankId == null) {
            throw new ServiceException("请先选择题库");
        }
        QuestionBank bank = questionBankMapper.selectBankById(bankId, currentScopeDeptId());
        if (bank == null) {
            throw new ServiceException("题库不存在或无权访问");
        }
        if (params == null || params.getKnowledgeRules() == null || params.getKnowledgeRules().isEmpty()) {
            throw new ServiceException("请先配置知识分布");
        }
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        java.util.List<Long> used = new java.util.ArrayList<>();
        int no = 1;
        for (com.ruoyi.business.domain.ExamKnowledgeRule rule : params.getKnowledgeRules()) {
            int need = rule.getQuestionCount() == null ? 0 : rule.getQuestionCount();
            if (need <= 0) {
                continue;
            }
            java.util.List<com.ruoyi.business.domain.Question> picked =
                    examRuleMapper.selectQuestionsByPoint(bankId, rule.getKnowledgePoint(), null, used, need);
            for (com.ruoyi.business.domain.Question q : picked) {
                used.add(q.getId());
                java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("seq", no++);
                m.put("questionId", q.getId());
                m.put("qtype", q.getQtype());
                m.put("knowledgePoint", q.getKnowledgePoint());
                m.put("stem", q.getStem());
                result.add(m);
            }
        }
        return result;
    }

    /** 知识分布校验：三项（占比 100% / 题量一致 / 不超题库可用） */
    private void validateKnowledgeRules(Exam exam, Exam params) {
        if (params == null || params.getKnowledgeRules() == null || params.getKnowledgeRules().isEmpty()) {
            return;
        }
        int countSum = 0;
        java.math.BigDecimal ratioSum = java.math.BigDecimal.ZERO;
        for (com.ruoyi.business.domain.ExamKnowledgeRule rule : params.getKnowledgeRules()) {
            if (rule.getQuestionCount() == null || rule.getQuestionCount() <= 0) {
                throw new ServiceException("知识分布「" + rule.getKnowledgePoint() + "」的抽题数量必须大于 0");
            }
            countSum += rule.getQuestionCount();
            if (rule.getRatio() != null) {
                ratioSum = ratioSum.add(rule.getRatio());
            }
        }
        if (ratioSum.compareTo(java.math.BigDecimal.ZERO) > 0
                && ratioSum.subtract(new java.math.BigDecimal("100")).abs().compareTo(new java.math.BigDecimal("0.01")) > 0) {
            throw new ServiceException("知识分布占比之和须为 100%，当前为 " + ratioSum.stripTrailingZeros().toPlainString() + "%");
        }
        Integer total = exam.getQuestionCount();
        if (total != null && total > 0 && countSum != total) {
            throw new ServiceException("知识分布抽题数量之和（" + countSum + "）须等于考试信息里的题目数量（" + total + "）");
        }
        // 题库可用题量校验（防抽不出题）
        if (params.getKnowledgeRules().size() > 0) {
            java.util.List<java.util.Map<String, Object>> available = examRuleMapper.selectBankKnowledgePoints(exam.getBankId());
            java.util.Map<String, Integer> availableMap = new java.util.HashMap<>();
            for (java.util.Map<String, Object> row : available) {
                availableMap.put(String.valueOf(row.get("knowledgePoint")),
                        Integer.valueOf(String.valueOf(row.get("totalCount"))));
            }
            for (com.ruoyi.business.domain.ExamKnowledgeRule rule : params.getKnowledgeRules()) {
                Integer have = availableMap.get(rule.getKnowledgePoint());
                if (have != null && rule.getQuestionCount() > have) {
                    throw new ServiceException("知识分布「" + rule.getKnowledgePoint() + "」抽题数量 "
                            + rule.getQuestionCount() + " 题超过题库可用题量（" + have + " 题）");
                }
            }
        }
    }

    private void saveKnowledgeRules(Long examId, Exam params) {
        if (params == null || params.getKnowledgeRules() == null) {
            return;
        }
        examRuleMapper.deleteKnowledgeRules(examId);
        java.util.List<com.ruoyi.business.domain.ExamKnowledgeRule> list = new java.util.ArrayList<>();
        int seq = 1;
        for (com.ruoyi.business.domain.ExamKnowledgeRule rule : params.getKnowledgeRules()) {
            if (rule.getKnowledgePoint() == null || rule.getKnowledgePoint().trim().isEmpty()) {
                continue;
            }
            rule.setExamId(examId);
            rule.setSortNo(seq++);
            list.add(rule);
        }
        if (!list.isEmpty()) {
            examRuleMapper.insertKnowledgeRules(list);
        }
    }

    private void saveParticipants(Long examId, Exam params, Exam exam) {
        if (params == null) {
            return;
        }
        boolean assigned = "ASSIGNED".equalsIgnoreCase(params.getAssignMode() == null ? "" : params.getAssignMode());
        boolean explicit = params.getParticipantIds() != null;
        if (!assigned && !explicit) {
            return;
        }
        examRuleMapper.deleteParticipants(examId);
        java.util.List<Long> ids = params.getParticipantIds();
        if (!assigned || ids == null || ids.isEmpty()) {
            return;
        }
        java.util.List<com.ruoyi.business.domain.ExamParticipant> list = new java.util.ArrayList<>();
        for (Long uid : ids) {
            if (uid == null) {
                continue;
            }
            com.ruoyi.business.domain.ExamParticipant p = new com.ruoyi.business.domain.ExamParticipant();
            p.setExamId(examId);
            p.setUserId(uid);
            list.add(p);
        }
        if (!list.isEmpty()) {
            examRuleMapper.insertParticipants(list);
        }
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
            return null;
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

    private void validateBankDept(Long bankId, Long deptId) {
        QuestionBank bank = questionBankMapper.selectBankById(bankId, deptId);
        if (bank == null || !deptId.equals(bank.getDeptId())) {
            throw new ServiceException("所选题库不存在或不属于考核部门");
        }
    }

    private boolean isGlobalReadOnly() {
        return SecurityUtils.hasRole("SUPER_ADMIN") || SecurityUtils.isAdmin(SecurityUtils.getUserId());
    }
}
