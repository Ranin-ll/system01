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
    private final com.ruoyi.business.mapper.PracticeModuleMapper practiceModuleMapper;

    public ExamServiceImpl(ExamMapper examMapper, QuestionBankMapper questionBankMapper,
                           com.ruoyi.business.mapper.ExamRuleMapper examRuleMapper,
                           com.ruoyi.business.mapper.PracticeModuleMapper practiceModuleMapper) {
        this.examMapper = examMapper;
        this.questionBankMapper = questionBankMapper;
        this.examRuleMapper = examRuleMapper;
        this.practiceModuleMapper = practiceModuleMapper;
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
        // 实操考核带上题目清单：批阅页/详情页据此展示每道题的题干与要求
        if ("PRACTICAL".equals(exam.getExamType())) {
            exam.setSubjectItems(examRuleMapper.selectSubjectItems(id));
        }
        return exam;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public int insertExam(Exam exam) {
        Long deptId = managerScopeDeptId();
        if (deptId == null) {
            deptId = exam.getDeptId();
        }
        if (deptId == null) {
            throw new ServiceException("请选择所属部门");
        }
        exam.setDeptId(deptId);
        assertModuleUsable(exam.getModuleId(), deptId);
        exam.setStatus("DRAFT");
        String type = exam.getExamType() == null ? "THEORY" : exam.getExamType();
        exam.setExamType(type);
        if (exam.getExamMode() == null || exam.getExamMode().trim().isEmpty()) {
            exam.setExamMode("FORMAL");
        }
        if ("THEORY".equals(type)) {
            if (exam.getBankId() != null) {
                validateBankDept(exam.getBankId(), deptId);
            }
            // 多题库组卷（当前主用）：校验题库归属 + 抽题量，并汇总回写题型数量
            java.util.List<com.ruoyi.business.domain.ExamBankRule> bankRules = normalizeBankRules(exam.getBankRules(), deptId);
            if (!bankRules.isEmpty()) {
                exam.setBankRules(bankRules);
                applyBankRuleSummary(exam, bankRules);
            } else {
                // 历史链路：单库 + 直接填题型数量（允许 DRAFT 阶段先不填题库，发布前校验）
                if (exam.getSingleCount() == null) exam.setSingleCount(0);
                if (exam.getMultiCount() == null) exam.setMultiCount(0);
                if (exam.getJudgeCount() == null) exam.setJudgeCount(0);
                exam.setQuestionCount(exam.getSingleCount() + exam.getMultiCount() + exam.getJudgeCount());
            }
            if (exam.getSingleScore() == null) exam.setSingleScore(java.math.BigDecimal.ZERO);
            if (exam.getMultiScore() == null) exam.setMultiScore(java.math.BigDecimal.ZERO);
            if (exam.getJudgeScore() == null) exam.setJudgeScore(java.math.BigDecimal.ZERO);
            exam.setSubjectContent(null);
            exam.setSubjectAttachment(null);
            exam.setSubjectItems(null);
            exam.setSubjectCount(0);
        } else if ("PRACTICAL".equals(type)) {
            // 实操不抽题：题目由管理员逐条填写（题干 / 描述 / 参考图 / 附件 / 本题满分）。
            // ★ 允许 0 道题：建卷走两步式（先建草稿 → 到配置页从「实操题库」带入或逐题填写），
            //   「至少一道题」的守卫统一放在 publish() 里（发布前必然检查）。
            java.util.List<com.ruoyi.business.domain.ExamSubjectItem> items = normalizeSubjectItems(exam.getSubjectItems());
            exam.setSubjectItems(items);
            exam.setSubjectCount(items.size());
            exam.setBankId(null);
            exam.setQuestionCount(items.size());
            exam.setSingleCount(0);
            exam.setMultiCount(0);
            exam.setJudgeCount(0);
            exam.setSingleScore(java.math.BigDecimal.ZERO);
            exam.setMultiScore(java.math.BigDecimal.ZERO);
            exam.setJudgeScore(java.math.BigDecimal.ZERO);
            // 历史题干字段不再使用，避免与题目清单重复展示
            exam.setSubjectContent(null);
            exam.setSubjectAttachment(null);
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
        int rows = examMapper.insertExam(exam);
        if (rows > 0 && exam.getId() != null) {
            if (exam.getBankRules() != null && !exam.getBankRules().isEmpty()) {
                saveBankRules(exam.getId(), exam.getBankRules());
            }
            if (exam.getSubjectItems() != null && !exam.getSubjectItems().isEmpty()) {
                saveSubjectItems(exam.getId(), exam.getSubjectItems());
            }
        }
        return rows;
    }

    /**
     * 模拟考核的「模块」校验：模块必须存在且归属同一部门。
     * 正式考核不挂模块（moduleId 为空）时直接放行。
     */
    private void assertModuleUsable(Long moduleId, Long deptId) {
        if (moduleId == null) {
            return;
        }
        com.ruoyi.business.domain.PracticeModule module = practiceModuleMapper.selectModuleById(moduleId);
        if (module == null) {
            throw new ServiceException("所选模块不存在或已被删除");
        }
        if (deptId != null && !deptId.equals(module.getDeptId())) {
            throw new ServiceException("所选模块不属于该部门");
        }
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public int updateExam(Exam exam) {
        managerScopeDeptId();
        Exam exist = getAccessibleExam(exam.getId());
        assertModuleUsable(exam.getModuleId(), exist == null ? null : exist.getDeptId());
        // 已发布/批改中的正式考核不允许修改基本信息；
        // 模拟考核（PRACTICE）不计成绩、不产生答卷归属，允许随时调整基本信息与组卷配置
        boolean practiceExam = "PRACTICE".equals(exist == null ? null : exist.getExamMode());
        if (exist != null && !practiceExam
                && !"DRAFT".equals(exist.getStatus()) && !"DISABLED".equals(exist.getStatus())) {
            throw new ServiceException("考核已发布或批改中，不能修改");
        }
        boolean isTheory = !"PRACTICAL".equals(exist.getExamType());
        java.util.List<com.ruoyi.business.domain.ExamBankRule> bankRules = null;
        java.util.List<com.ruoyi.business.domain.ExamSubjectItem> subjectItems = null;
        if (isTheory) {
            if (exam.getBankRules() != null) {
                // 理论：编辑时一并保存组卷配置
                bankRules = normalizeBankRules(exam.getBankRules(), exist.getDeptId());
                exam.setBankRules(bankRules);
                applyBankRuleSummary(exam, bankRules);
            }
            if (exam.getBankId() != null) {
                validateBankDept(exam.getBankId(), exist.getDeptId());
            }
        } else {
            // 实操：题目清单为准。只有显式传了 subjectItems 才覆写，
            // 避免「只改名称 / 时长」的保存把已配好的题目清空。
            // ★ 允许传空数组（= 清空题目）：草稿阶段随便改，「至少一道题」的守卫在 publish()。
            if (exam.getSubjectItems() != null) {
                subjectItems = normalizeSubjectItems(exam.getSubjectItems());
                exam.setSubjectCount(subjectItems.size());
                exam.setQuestionCount(subjectItems.size());
            }
            exam.setSubjectItems(null);
            exam.setSubjectContent(null);
            exam.setSubjectAttachment(null);
        }
        exam.setDeptId(null);
        exam.setDeleted(null);
        int rows = examMapper.updateExam(exam);
        if (bankRules != null) {
            saveBankRules(exam.getId(), bankRules);
        }
        if (subjectItems != null) {
            saveSubjectItems(exam.getId(), subjectItems);
        }
        return rows;
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
        // 模拟考核（PRACTICE）已发布时再次「保存并生效」视为幂等：重新校验配置后仍是已发布，
        // 避免管理员改一次配置就被「仅待发布状态可以发布」拦住
        boolean practiceRepublish = "PRACTICE".equals(exam.getExamMode()) && "PUBLISHED".equals(exam.getStatus());
        if (!"DRAFT".equals(exam.getStatus()) && !practiceRepublish) {
            throw new ServiceException("仅待发布状态的考核可以发布");
        }
        // 组卷配置：优先多题库（exam_bank_rule），未配则回退单库 + 题型数量
        java.util.List<com.ruoyi.business.domain.ExamBankRule> incoming = collectBankRules(exam, params);
        if (incoming != null) {
            saveBankRules(id, incoming);
        }
        java.util.List<com.ruoyi.business.domain.ExamBankRule> effective =
                incoming != null ? incoming : examRuleMapper.selectBankRules(id);
        // 实操：本次若带上了题目清单（在配置卡上直接改题后发布）则一并落库
        if ("PRACTICAL".equals(exam.getExamType()) && params != null && params.getSubjectItems() != null) {
            java.util.List<com.ruoyi.business.domain.ExamSubjectItem> items =
                    normalizeSubjectItems(params.getSubjectItems());
            if (items.isEmpty()) {
                throw new ServiceException("发布前请先配置实操题目：至少填写一道题的题干");
            }
            saveSubjectItems(id, items);
        }
        // ★ 发布是「至少一道实操题」的唯一守卫点：即使本次没带题目清单
        //   （例如在列表页直接点「发布」），也要确保库里确实有题，避免发出空卷。
        //   新建 / 编辑 / 保存配置阶段都允许 0 道题（草稿态随便改）。
        if ("PRACTICAL".equals(exam.getExamType())) {
            java.util.List<com.ruoyi.business.domain.ExamSubjectItem> existingItems =
                    examRuleMapper.selectSubjectItems(id);
            if (existingItems == null || existingItems.isEmpty()) {
                throw new ServiceException("发布前请先配置实操题目：至少填写一道题的题干");
            }
        }
        assertDrawConfigReady(exam, effective);
        // 发布校验：通过线不能超过卷面满分（防管理员误设，从后端兜底拦截）
        assertPassLineValid(exam, params, effective);
        // 发布前校验知识分布（历史链路，若配了）：占比合计 100%、抽题数量合计 = 题目数量、不超题库可用题量
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
        // 多题库配置下把各库抽题量汇总回写 exam（卷面题量 / 总分展示继续可用）；
        // 实操不走题库，题量由 assertDrawConfigReady 按题目清单回写。
        if ("THEORY".equals(exam.getExamType()) && effective != null && !effective.isEmpty()) {
            update.setSingleCount(sumBankRule(effective, "SINGLE"));
            update.setMultiCount(sumBankRule(effective, "MULTI"));
            update.setJudgeCount(sumBankRule(effective, "JUDGE"));
            update.setQuestionCount(sumBankRule(effective, "ALL"));
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
        java.util.List<com.ruoyi.business.domain.ExamBankRule> incoming = collectBankRules(exam, params);
        if (incoming != null) {
            saveBankRules(id, incoming);
        }
        java.util.List<com.ruoyi.business.domain.ExamBankRule> effective =
                incoming != null ? incoming : examRuleMapper.selectBankRules(id);
        // 历史链路的知识分布校验（未配置 bankRules 时才生效）
        validateKnowledgeRules(exam, params);
        saveKnowledgeRules(id, params);
        saveParticipants(id, params, exam);
        Exam update = new Exam();
        update.setId(id);
        // 实操：配置卡上直接维护题目清单
        if ("PRACTICAL".equals(exam.getExamType()) && params != null && params.getSubjectItems() != null) {
            java.util.List<com.ruoyi.business.domain.ExamSubjectItem> items =
                    normalizeSubjectItems(params.getSubjectItems());
            if (items.isEmpty()) {
                throw new ServiceException("实操考核至少需要一道题目，请填写题干后保存");
            }
            saveSubjectItems(id, items);
            update.setSubjectCount(items.size());
            update.setQuestionCount(items.size());
        }
        if (params != null) {
            update.setStartTime(params.getStartTime());
            update.setEndTime(params.getEndTime());
            // 配置页可同时调整「时长 / 通过线 / 每题分值」：
            // 已发布的模拟配置考核也能改这几项（不改考核名与类型，故不走 updateExam 的状态限制）
            if (params.getDuration() != null) {
                update.setDuration(params.getDuration());
            }
            if (params.getPassLine() != null) {
                update.setPassLine(params.getPassLine());
            }
            // 套卷元信息：「难易程度」与「题目内容偏向」（模拟理论考核配置页维护；不传即不更新）
            if (params.getDescription() != null) {
                update.setDescription(params.getDescription());
            }
            if (params.getDifficulty() != null) {
                update.setDifficulty(params.getDifficulty());
            }
            if (params.getContentBias() != null) {
                update.setContentBias(params.getContentBias());
            }
            if ("THEORY".equals(exam.getExamType())) {
                if (params.getSingleScore() != null) {
                    update.setSingleScore(params.getSingleScore());
                }
                if (params.getMultiScore() != null) {
                    update.setMultiScore(params.getMultiScore());
                }
                if (params.getJudgeScore() != null) {
                    update.setJudgeScore(params.getJudgeScore());
                }
            }
        }
        if ("THEORY".equals(exam.getExamType()) && effective != null && !effective.isEmpty()) {
            update.setSingleCount(sumBankRule(effective, "SINGLE"));
            update.setMultiCount(sumBankRule(effective, "MULTI"));
            update.setJudgeCount(sumBankRule(effective, "JUDGE"));
            update.setQuestionCount(sumBankRule(effective, "ALL"));
        }
        return examMapper.updateExam(update);
    }

    @Override
    public java.util.Map<String, Object> configDetail(Long id) {
        Exam exam = getAccessibleExam(id);
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("examId", exam.getId());
        result.put("examType", exam.getExamType());
        result.put("bankRules", examRuleMapper.selectBankRules(id));
        result.put("knowledgeRules", examRuleMapper.selectKnowledgeRules(id));
        // 实操题目清单（理论为空）：配置卡与弹窗都用它渲染
        java.util.List<com.ruoyi.business.domain.ExamSubjectItem> subjectItems = examRuleMapper.selectSubjectItems(id);
        result.put("subjectItems", subjectItems);
        result.put("subjectCount", subjectItems.size());
        java.math.BigDecimal totalScore = java.math.BigDecimal.ZERO;
        for (com.ruoyi.business.domain.ExamSubjectItem it : subjectItems) {
            if (it.getScore() != null) {
                totalScore = totalScore.add(it.getScore());
            }
        }
        result.put("subjectTotalScore", totalScore);
        java.util.List<Long> userIds = examRuleMapper.selectParticipantUserIds(id);
        result.put("participantIds", userIds);
        result.put("participants", examRuleMapper.selectParticipants(id));
        result.put("assignMode", userIds.isEmpty() ? "ALL" : "ASSIGNED");
        result.put("startTime", exam.getStartTime());
        result.put("endTime", exam.getEndTime());
        return result;
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> bankOptions(Long deptId, String examMode, String bankKind) {
        Long scope = managerScopeDeptId();
        Long target = scope != null ? scope : deptId;
        if (target == null) {
            throw new ServiceException("请先选择所属部门");
        }
        QuestionBank query = new QuestionBank();
        query.setDeptId(target);
        query.setScopeDeptId(scope);
        // 形态过滤（理论套卷只能选理论库；实操考核只能选实操库）；为空则不过滤（向后兼容）
        if (bankKind != null && !bankKind.trim().isEmpty()) {
            query.setBankKind(bankKind.trim().toUpperCase());
        }
        java.util.List<QuestionBank> banks = questionBankMapper.selectBankList(query);
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        if (banks == null) {
            return result;
        }
        // 按考核性质过滤候选题库：正式考核 → 正式题库 + 通用题库；模拟考核 → 模拟题库 + 通用题库。
        // examMode 为空（老前端不传）时不过滤，保持向后兼容。
        if (examMode != null && !examMode.trim().isEmpty()) {
            String mode = examMode.trim().toUpperCase();
            java.util.List<QuestionBank> filtered = new java.util.ArrayList<>();
            for (QuestionBank bank : banks) {
                String bt = bank.getBankType() == null ? "COMMON" : bank.getBankType().toUpperCase();
                boolean usable = "COMMON".equals(bt) || ("PRACTICE".equals(mode) ? "PRACTICE".equals(bt) : "FORMAL".equals(bt));
                if (usable) {
                    filtered.add(bank);
                }
            }
            banks = filtered;
        }
        for (QuestionBank bank : banks) {
            java.util.Map<String, Object> avail = examRuleMapper.selectBankAvailable(bank.getId());
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("bankId", bank.getId());
            m.put("bankName", bank.getBankName());
            m.put("bankType", bank.getBankType());
            m.put("questionCount", bank.getQuestionCount());
            m.put("singleCount", intOf(avail == null ? null : avail.get("singleCount")));
            m.put("multiCount", intOf(avail == null ? null : avail.get("multiCount")));
            m.put("judgeCount", intOf(avail == null ? null : avail.get("judgeCount")));
            m.put("totalCount", intOf(avail == null ? null : avail.get("totalCount")));
            result.add(m);
        }
        return result;
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> internOptions(Long deptId) {
        Long scope = managerScopeDeptId();
        // 部门管理员：强制本部门（忽略入参，防越权）；超级管理员：可按入参部门取人，不传则取全部在培实习生
        Long target = scope != null ? scope : deptId;
        java.util.List<java.util.Map<String, Object>> list = examRuleMapper.selectInternOptions(target);
        return list == null ? new java.util.ArrayList<>() : list;
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

    /**
     * 按多题库组卷配置试抽一套卷（不落库，配置页「试抽一套」用）。
     *
     * 抽题口径：对每个题库，分别按 单选/多选/判断 的配置数量在该库内随机取题。
     */
    @Override
    public java.util.List<java.util.Map<String, Object>> tryDrawByBanks(Exam params) {
        if (params == null || params.getBankRules() == null || params.getBankRules().isEmpty()) {
            throw new ServiceException("请先配置组卷题库与抽题数量");
        }
        java.util.List<com.ruoyi.business.domain.ExamBankRule> bankRules =
                normalizeBankRules(params.getBankRules(), null);
        if (bankRules.isEmpty()) {
            throw new ServiceException("请至少为一个题库配置抽题数量");
        }
        validateBankRuleQuota(bankRules, null);

        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        java.util.List<Long> used = new java.util.ArrayList<>();
        int no = 1;
        for (com.ruoyi.business.domain.ExamBankRule rule : bankRules) {
            for (String qtype : new String[]{"SINGLE", "MULTI", "JUDGE"}) {
                int need = "SINGLE".equals(qtype) ? nz(rule.getSingleCount())
                        : ("MULTI".equals(qtype) ? nz(rule.getMultiCount()) : nz(rule.getJudgeCount()));
                if (need <= 0) {
                    continue;
                }
                java.util.List<com.ruoyi.business.domain.Question> picked =
                        examRuleMapper.selectQuestionsByPoint(rule.getBankId(), null, qtype, used, need);
                for (com.ruoyi.business.domain.Question q : picked) {
                    used.add(q.getId());
                    java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("seq", no++);
                    m.put("questionId", q.getId());
                    m.put("qtype", q.getQtype());
                    m.put("bankId", rule.getBankId());
                    m.put("bankName", rule.getBankName());
                    m.put("stem", q.getStem());
                    result.add(m);
                }
            }
        }
        return result;
    }

    // ==================== 多题库组卷：校验 / 落库 / 汇总 ====================

    /**
     * 从请求中收集组卷配置。
     *
     * @return null 表示本次请求未涉及组卷配置（保持库中原样）；
     *         非 null（可能为空列表）表示要以它为准覆写。
     */
    private java.util.List<com.ruoyi.business.domain.ExamBankRule> collectBankRules(Exam exam, Exam params) {
        // 只有理论考核从题库组卷（多题库 × 题型配额；题库 = 部门的一门科目）。
        // 实操考核已改为「管理员逐条填写题目清单」，不再接受任何题库组卷配置。
        if (!"THEORY".equals(exam.getExamType())) {
            return null;
        }
        if (params == null || params.getBankRules() == null) {
            return null;
        }
        java.util.List<com.ruoyi.business.domain.ExamBankRule> list =
                normalizeBankRules(params.getBankRules(), exam.getDeptId());
        if (!list.isEmpty()) {
            validateBankRuleQuota(list, exam);
        }
        return list;
    }

    /**
     * 规范化组卷配置：去重、过滤不抽题的库、校验题库归属部门并回填题库名。
     *
     * @param deptId 为 null 时不校验部门归属（试抽场景）
     */
    private java.util.List<com.ruoyi.business.domain.ExamBankRule> normalizeBankRules(
            java.util.List<com.ruoyi.business.domain.ExamBankRule> raw, Long deptId) {
        java.util.List<com.ruoyi.business.domain.ExamBankRule> list = new java.util.ArrayList<>();
        if (raw == null) {
            return list;
        }
        java.util.Set<Long> seen = new java.util.HashSet<>();
        int seq = 1;
        for (com.ruoyi.business.domain.ExamBankRule r : raw) {
            if (r == null || r.getBankId() == null) {
                continue;
            }
            if (!seen.add(r.getBankId())) {
                throw new ServiceException("组卷题库重复，请合并同一题库的抽题数量");
            }
            int s = nz(r.getSingleCount());
            int m = nz(r.getMultiCount());
            int j = nz(r.getJudgeCount());
            if (s < 0 || m < 0 || j < 0) {
                throw new ServiceException("抽题数量不能为负数");
            }
            if (s == 0 && m == 0 && j == 0) {
                continue;
            }
            com.ruoyi.business.domain.ExamBankRule rule = new com.ruoyi.business.domain.ExamBankRule();
            rule.setBankId(r.getBankId());
            rule.setSingleCount(s);
            rule.setMultiCount(m);
            rule.setJudgeCount(j);
            rule.setSortNo(seq++);
            if (deptId != null) {
                QuestionBank bank = questionBankMapper.selectBankById(r.getBankId(), deptId);
                if (bank == null || !deptId.equals(bank.getDeptId())) {
                    throw new ServiceException("组卷题库不存在或不属于考核部门");
                }
                rule.setBankName(bank.getBankName());
            } else {
                QuestionBank bank = questionBankMapper.selectBankById(r.getBankId(), currentScopeDeptId());
                rule.setBankName(bank == null ? null : bank.getBankName());
            }
            list.add(rule);
        }
        return list;
    }

    /** 校验组卷抽题量不超各库可用题量（防发布后抽不满卷） */
    private void validateBankRuleQuota(java.util.List<com.ruoyi.business.domain.ExamBankRule> bankRules, Exam exam) {
        if (bankRules == null || bankRules.isEmpty()) {
            return;
        }
        int total = 0;
        for (com.ruoyi.business.domain.ExamBankRule rule : bankRules) {
            java.util.Map<String, Object> avail = examRuleMapper.selectBankAvailable(rule.getBankId());
            int aSingle = intOf(avail == null ? null : avail.get("singleCount"));
            int aMulti = intOf(avail == null ? null : avail.get("multiCount"));
            int aJudge = intOf(avail == null ? null : avail.get("judgeCount"));
            String name = rule.getBankName() == null ? ("题库 #" + rule.getBankId()) : rule.getBankName();
            if (nz(rule.getSingleCount()) > aSingle) {
                throw new ServiceException("题库「" + name + "」单选题不足：需抽 " + rule.getSingleCount() + " 题，可用 " + aSingle + " 题");
            }
            if (nz(rule.getMultiCount()) > aMulti) {
                throw new ServiceException("题库「" + name + "」多选题不足：需抽 " + rule.getMultiCount() + " 题，可用 " + aMulti + " 题");
            }
            if (nz(rule.getJudgeCount()) > aJudge) {
                throw new ServiceException("题库「" + name + "」判断题不足：需抽 " + rule.getJudgeCount() + " 题，可用 " + aJudge + " 题");
            }
            total += rule.getTotalCount();
        }
        if (total <= 0) {
            throw new ServiceException("组卷抽题数量合计必须大于 0");
        }
    }

    /**
     * 发布前确认考核内容齐备。
     *
     * 理论：卷面完全依赖题库 → 必须有组卷配置（多题库 或 单库 + 题型数量）。
     * 实操：题目是管理员逐条填写的清单 → 必须至少有一道题；
     *      同时把「题量 / 总分」汇总回写，列表与前端都用这两个值展示。
     */
    private void assertDrawConfigReady(Exam exam, java.util.List<com.ruoyi.business.domain.ExamBankRule> effective) {
        if ("PRACTICAL".equals(exam.getExamType())) {
            java.util.List<com.ruoyi.business.domain.ExamSubjectItem> items = examRuleMapper.selectSubjectItems(exam.getId());
            if (items == null || items.isEmpty()) {
                throw new ServiceException("发布前请先配置实操题目：至少填写一道题的题干");
            }
            Exam summary = new Exam();
            summary.setId(exam.getId());
            summary.setSubjectCount(items.size());
            summary.setQuestionCount(items.size());
            examMapper.updateExam(summary);
            return;
        }
        if (effective != null && !effective.isEmpty()) {
            return;
        }
        if (exam.getBankId() == null) {
            throw new ServiceException("发布前请先配置组卷题库（可多选）与每个题库的抽题数量");
        }
        int total = nz(exam.getSingleCount()) + nz(exam.getMultiCount()) + nz(exam.getJudgeCount());
        if (total <= 0) {
            throw new ServiceException("发布前请先配置组卷题库（可多选）与每个题库的抽题数量");
        }
    }

    /**
     * 发布校验：通过线不能超过卷面满分。
     *
     * 通过线缺失（null）或未设（<=0）不拦；只要设了正数，就必须 ≤ 卷面满分。
     * 卷面满分：实操 = 各题满分之和；理论 = 单选/多选/判断 每题分值 × 题量之和。
     */
    private void assertPassLineValid(Exam exam, Exam params,
            java.util.List<com.ruoyi.business.domain.ExamBankRule> effective) {
        java.math.BigDecimal passLine = params != null && params.getPassLine() != null
                ? params.getPassLine() : exam.getPassLine();
        if (passLine == null || passLine.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return;
        }
        java.math.BigDecimal total = computeTotalScore(exam, params, effective);
        if (passLine.compareTo(total) > 0) {
            throw new ServiceException("通过线（" + fmtScore(passLine) + " 分）不能超过卷面满分（"
                    + fmtScore(total) + " 分），请调整通过线或题目分值");
        }
    }

    /**
     * 计算卷面满分。分值优先取本次提交（params），缺失回退到库中（exam）；
     * 题量优先多题库汇总（effective），缺失回退单库（exam）。
     */
    private java.math.BigDecimal computeTotalScore(Exam exam, Exam params,
            java.util.List<com.ruoyi.business.domain.ExamBankRule> effective) {
        if ("PRACTICAL".equals(exam.getExamType())) {
            java.math.BigDecimal total = java.math.BigDecimal.ZERO;
            java.util.List<com.ruoyi.business.domain.ExamSubjectItem> items =
                    examRuleMapper.selectSubjectItems(exam.getId());
            if (items != null) {
                for (com.ruoyi.business.domain.ExamSubjectItem it : items) {
                    if (it.getScore() != null) {
                        total = total.add(it.getScore());
                    }
                }
            }
            return total;
        }
        java.math.BigDecimal single = params != null && params.getSingleScore() != null
                ? params.getSingleScore() : exam.getSingleScore();
        java.math.BigDecimal multi = params != null && params.getMultiScore() != null
                ? params.getMultiScore() : exam.getMultiScore();
        java.math.BigDecimal judge = params != null && params.getJudgeScore() != null
                ? params.getJudgeScore() : exam.getJudgeScore();
        if (single == null) {
            single = java.math.BigDecimal.ZERO;
        }
        if (multi == null) {
            multi = java.math.BigDecimal.ZERO;
        }
        if (judge == null) {
            judge = java.math.BigDecimal.ZERO;
        }
        int singleCount;
        int multiCount;
        int judgeCount;
        if (effective != null && !effective.isEmpty()) {
            singleCount = sumBankRule(effective, "SINGLE");
            multiCount = sumBankRule(effective, "MULTI");
            judgeCount = sumBankRule(effective, "JUDGE");
        } else {
            singleCount = nz(exam.getSingleCount());
            multiCount = nz(exam.getMultiCount());
            judgeCount = nz(exam.getJudgeCount());
        }
        return single.multiply(java.math.BigDecimal.valueOf(singleCount))
                .add(multi.multiply(java.math.BigDecimal.valueOf(multiCount)))
                .add(judge.multiply(java.math.BigDecimal.valueOf(judgeCount)));
    }

    /** 分数展示：去掉无意义的末尾 0（60.0 → 60、43.5 → 43.5） */
    private static String fmtScore(java.math.BigDecimal v) {
        java.math.BigDecimal s = v.stripTrailingZeros();
        return s.scale() < 0 ? s.setScale(0).toPlainString() : s.toPlainString();
    }

    /** 把各库抽题量汇总回写到 exam 的题型数量字段（qtype 传 ALL 表示合计） */
    private int sumBankRule(java.util.List<com.ruoyi.business.domain.ExamBankRule> list, String qtype) {
        int s = 0;
        for (com.ruoyi.business.domain.ExamBankRule r : list) {
            if ("SINGLE".equals(qtype)) {
                s += nz(r.getSingleCount());
            } else if ("MULTI".equals(qtype)) {
                s += nz(r.getMultiCount());
            } else if ("JUDGE".equals(qtype)) {
                s += nz(r.getJudgeCount());
            } else {
                s += r.getTotalCount();
            }
        }
        return s;
    }

    /** 保存组卷配置（先清后插，保证与前端提交完全一致） */
    private void saveBankRules(Long examId, java.util.List<com.ruoyi.business.domain.ExamBankRule> list) {
        examRuleMapper.deleteBankRules(examId);
        if (list == null || list.isEmpty()) {
            return;
        }
        for (com.ruoyi.business.domain.ExamBankRule r : list) {
            r.setExamId(examId);
        }
        examRuleMapper.insertBankRules(list);
    }

    // ==================== 实操题目清单：校验 / 落库 ====================

    /**
     * 规范化实操题目清单：重排题序、校验必填与分值、规整 JSON 字段。
     *
     * 题干是唯一必填项（没有题干的"题"对应不上实习生的作答文件）；
     * 描述 / 参考图 / 附件 / 满分都可留空，满分默认 0 分。
     */
    private java.util.List<com.ruoyi.business.domain.ExamSubjectItem> normalizeSubjectItems(
            java.util.List<com.ruoyi.business.domain.ExamSubjectItem> raw) {
        java.util.List<com.ruoyi.business.domain.ExamSubjectItem> list = new java.util.ArrayList<>();
        if (raw == null) {
            return list;
        }
        int seq = 1;
        for (com.ruoyi.business.domain.ExamSubjectItem it : raw) {
            if (it == null) {
                continue;
            }
            String title = it.getTitle() == null ? "" : it.getTitle().trim();
            if (title.isEmpty()) {
                throw new ServiceException("第 " + seq + " 题的题干不能为空，请补充题干或删除该题");
            }
            if (title.length() > 200) {
                throw new ServiceException("第 " + seq + " 题的题干过长（不超过 200 字）");
            }
            java.math.BigDecimal score = it.getScore() == null ? java.math.BigDecimal.ZERO : it.getScore();
            if (score.compareTo(java.math.BigDecimal.ZERO) < 0) {
                throw new ServiceException("第 " + seq + " 题的满分不能为负数");
            }
            com.ruoyi.business.domain.ExamSubjectItem item = new com.ruoyi.business.domain.ExamSubjectItem();
            item.setSeq(seq++);
            item.setTitle(title);
            item.setDescription(it.getDescription() == null ? null : it.getDescription().trim());
            item.setScore(score);
            item.setReferenceImages(blankToNull(it.getReferenceImages()));
            item.setAttachmentsJson(blankToNull(it.getAttachmentsJson()));
            list.add(item);
        }
        return list;
    }

    /** 保存实操题目清单（先清后插，保证与前端提交完全一致） */
    private void saveSubjectItems(Long examId, java.util.List<com.ruoyi.business.domain.ExamSubjectItem> list) {
        examRuleMapper.deleteSubjectItems(examId);
        if (list == null || list.isEmpty()) {
            return;
        }
        for (com.ruoyi.business.domain.ExamSubjectItem it : list) {
            it.setExamId(examId);
        }
        examRuleMapper.insertSubjectItems(list);
    }

    private static String blankToNull(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    /** 把组卷配置的合计题量写进 exam 对象（尚未落库，供 update 语句使用） */
    private void applyBankRuleSummary(Exam exam, java.util.List<com.ruoyi.business.domain.ExamBankRule> bankRules) {
        exam.setSingleCount(sumBankRule(bankRules, "SINGLE"));
        exam.setMultiCount(sumBankRule(bankRules, "MULTI"));
        exam.setJudgeCount(sumBankRule(bankRules, "JUDGE"));
        exam.setQuestionCount(sumBankRule(bankRules, "ALL"));
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    private static int intOf(Object v) {
        if (v == null) {
            return 0;
        }
        try {
            return (int) Double.parseDouble(String.valueOf(v));
        } catch (NumberFormatException e) {
            return 0;
        }
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
        java.util.List<Long> ids = params.getParticipantIds();
        // 选了「指定人员」却不给名单 → 落库后名单为空，会被下游当成"全员可见"，
        // 与管理员"只发给这几位"的预期相反。必须在保存/发布阶段就拦住。
        if (assigned && (ids == null || ids.isEmpty())) {
            throw new ServiceException("已选择「指定人员」，请至少勾选一名实习生；如需全员可见请改选「全部在培实习生」");
        }
        // 去重（同一人重复勾选会导致名单展示与校验异常）
        java.util.LinkedHashSet<Long> uniq = new java.util.LinkedHashSet<>();
        if (ids != null) {
            for (Long uid : ids) {
                if (uid != null) {
                    uniq.add(uid);
                }
            }
        }
        // 名单合法性校验：只能指定在培实习生（部门管理员限本部门，超级管理员可跨部门），
        // 防止把管理员账号或已离场人员填进名单。
        if (!uniq.isEmpty()) {
            Long scope = managerScopeDeptId();
            java.util.List<java.util.Map<String, Object>> roster = examRuleMapper.selectInternOptions(scope);
            java.util.Set<Long> allow = new java.util.LinkedHashSet<>();
            if (roster != null) {
                for (java.util.Map<String, Object> r : roster) {
                    Object v = r.get("userId");
                    if (v != null) {
                        allow.add(Long.valueOf(String.valueOf(v)));
                    }
                }
            }
            for (Long uid : uniq) {
                if (!allow.contains(uid)) {
                    throw new ServiceException("所选人员不在可指定的在培实习生名单内（用户ID " + uid + "），请重新选择");
                }
            }
        }
        examRuleMapper.deleteParticipants(examId);
        if (!assigned || uniq.isEmpty()) {
            return;
        }
        java.util.List<com.ruoyi.business.domain.ExamParticipant> list = new java.util.ArrayList<>();
        for (Long uid : uniq) {
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
