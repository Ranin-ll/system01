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
 * 数据范围：超级管理员查看全部并可指定部门管理，部门管理员管理本部门考核。
 *
 * ★ 2026-09-23：多题库组卷（exam_bank_rule）整体退场。
 * 理论考核统一从「本部门题池」（question.dept_id）按知识点配比（exam_knowledge_rule）抽题；
 * 实操考核的题目由管理员逐条填写（exam_subject_item），不抽题。
 */
@Service
public class ExamServiceImpl extends ServiceImpl<ExamMapper, Exam> implements IExamService {

    private final ExamMapper examMapper;
    private final com.ruoyi.business.mapper.ExamRuleMapper examRuleMapper;
    private final com.ruoyi.business.mapper.PracticeModuleMapper practiceModuleMapper;
    /** ★ 2026-09-24：试抽改为走 PaperDrawer（按题型配额），需要它来补齐/截断到卷面题量 */
    private final com.ruoyi.business.mapper.QuestionMapper questionMapper;

    public ExamServiceImpl(ExamMapper examMapper,
                           com.ruoyi.business.mapper.ExamRuleMapper examRuleMapper,
                           com.ruoyi.business.mapper.PracticeModuleMapper practiceModuleMapper,
                           com.ruoyi.business.mapper.QuestionMapper questionMapper) {
        this.examMapper = examMapper;
        this.examRuleMapper = examRuleMapper;
        this.practiceModuleMapper = practiceModuleMapper;
        this.questionMapper = questionMapper;
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
            // 理论：卷面结构 = 题型数量 × 每题分值（决定满分口径）；
            // 抽题分布由配置页的知识点配比（exam_knowledge_rule）决定，从本部门题池抽。
            // 允许 DRAFT 阶段先不填（发布前由 assertDrawConfigReady 兜底拦截）。
            if (exam.getSingleCount() == null) exam.setSingleCount(0);
            if (exam.getMultiCount() == null) exam.setMultiCount(0);
            if (exam.getJudgeCount() == null) exam.setJudgeCount(0);
            exam.setQuestionCount(exam.getSingleCount() + exam.getMultiCount() + exam.getJudgeCount());
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
        java.util.List<com.ruoyi.business.domain.ExamSubjectItem> subjectItems = null;
        if (isTheory) {
            // 理论：题型数量即卷面结构；知识点配比由 saveConfig 单独维护，这里不动。
            if (exam.getSingleCount() != null || exam.getMultiCount() != null || exam.getJudgeCount() != null) {
                if (exam.getSingleCount() == null) exam.setSingleCount(exist.getSingleCount());
                if (exam.getMultiCount() == null) exam.setMultiCount(exist.getMultiCount());
                if (exam.getJudgeCount() == null) exam.setJudgeCount(exist.getJudgeCount());
                exam.setQuestionCount(nz(exam.getSingleCount()) + nz(exam.getMultiCount()) + nz(exam.getJudgeCount()));
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
        assertDrawConfigReady(exam);
        // 发布校验：通过线不能超过卷面满分（防管理员误设，从后端兜底拦截）
        assertPassLineValid(exam, params);
        // 发布前校验知识分布：占比合计 100%、抽题数量合计 = 题目数量、不超本部门题池可用题量
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
        // 实操不走题池，题量由 assertDrawConfigReady 按题目清单回写。
        return examMapper.updateExam(update);
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> bankKnowledgePoints(Long deptId) {
        Long target = resolveDeptId(deptId);
        return examRuleMapper.selectDeptKnowledgePoints(target);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public int saveConfig(Long id, Exam params) {
        managerScopeDeptId();
        Exam exam = getAccessibleExam(id);
        // ★ 正式考核一旦「已发布 / 待批改」，配置（组卷 / 实操题目 / 指定人员 / 时间窗）一律锁定：
        //   与 updateExam 对「基本信息」的约束保持同一口径 —— 已发布还有答卷在跑，改配置会让
        //   已发出的卷子与卷面满分/题目对不上。需要改动就走「停用后再改」或「删除后重新发布」。
        //   模拟考核（PRACTICE）不计成绩、无答卷归属，保持可随时调整。
        if (!"PRACTICE".equals(exam.getExamMode())
                && !"DRAFT".equals(exam.getStatus()) && !"DISABLED".equals(exam.getStatus())) {
            throw new ServiceException("考核已发布，配置已锁定，只能查看或删除后重新发布；"
                    + "确需修改请先「停用」该考核");
        }
        // 知识分布校验（占比合计 100% / 抽题数量合计 = 题量 / 不超本部门题池可用题量）
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
                // 理论：题型数量（卷面结构，决定满分口径）+ 每题分值
                if (params.getSingleCount() != null || params.getMultiCount() != null || params.getJudgeCount() != null) {
                    int s = params.getSingleCount() == null ? nz(exam.getSingleCount()) : params.getSingleCount();
                    int m = params.getMultiCount() == null ? nz(exam.getMultiCount()) : params.getMultiCount();
                    int j = params.getJudgeCount() == null ? nz(exam.getJudgeCount()) : params.getJudgeCount();
                    update.setSingleCount(s);
                    update.setMultiCount(m);
                    update.setJudgeCount(j);
                    update.setQuestionCount(s + m + j);
                }
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
        return examMapper.updateExam(update);
    }

    @Override
    public java.util.Map<String, Object> configDetail(Long id) {
        Exam exam = getAccessibleExam(id);
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("examId", exam.getId());
        result.put("examType", exam.getExamType());
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

    /**
     * 题池概览：返回<b>本部门理论题池</b>的可用题量（按题型）。
     *
     * <p>★ 2026-09-23：题库概念退场后，一个部门只有一个题池，故这里固定返回 1 条。
     * 保留「列表 + bankId/bankName」的返回形状，是为了让既有配置页的「题池选择题」零改动即可继续渲染
     * （只有一个选项，必然选中）；bankId 的语义已由「题库ID」变为「部门ID」。</p>
     *
     * <p>入参 {@code examMode} / {@code bankKind} 仅作向后兼容，不再参与过滤（题池已不分形态与用途）。</p>
     */
    @Override
    public java.util.List<java.util.Map<String, Object>> bankOptions(Long deptId, String examMode, String bankKind) {
        Long target = resolveDeptId(deptId);
        java.util.Map<String, Object> avail = examRuleMapper.selectDeptAvailable(target);
        String deptName = examRuleMapper.selectDeptName(target);
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("bankId", target);
        m.put("deptId", target);
        m.put("bankName", (deptName == null ? "本部门" : deptName) + " · 理论题池");
        m.put("bankType", "COMMON");
        m.put("singleCount", intOf(avail == null ? null : avail.get("singleCount")));
        m.put("multiCount", intOf(avail == null ? null : avail.get("multiCount")));
        m.put("judgeCount", intOf(avail == null ? null : avail.get("judgeCount")));
        m.put("totalCount", intOf(avail == null ? null : avail.get("totalCount")));
        m.put("questionCount", intOf(avail == null ? null : avail.get("totalCount")));
        result.add(m);
        return result;
    }

    /**
     * 解析操作目标部门。
     * 部门账号 → 强制本部门（忽略入参，防越权，也让老前端不用改）；超管 → 用入参部门（不传则报错）。
     */
    private Long resolveDeptId(Long deptId) {
        Long scope = managerScopeDeptId();
        if (scope != null) {
            return scope;
        }
        if (deptId == null) {
            throw new ServiceException("请先选择所属部门");
        }
        return deptId;
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> internOptions(Long deptId) {
        Long scope = managerScopeDeptId();
        // 部门管理员：强制本部门（忽略入参，防越权）；超级管理员：可按入参部门取人，不传则取全部在培实习生
        Long target = scope != null ? scope : deptId;
        java.util.List<java.util.Map<String, Object>> list = examRuleMapper.selectInternOptions(target);
        return list == null ? new java.util.ArrayList<>() : list;
    }

    /**
     * 按知识分布试抽一套卷（不落库，配置页「试抽一套」校验用）。
     *
     * <p>★ 2026-09-23：正式考核与模拟套卷统一走此口径 ——
     * 从 <b>本部门题池</b>（question.dept_id）按知识点配比抽题，跨知识点去重。</p>
     *
     * @param deptId 目标部门（部门账号强制本部门；超管需显式传）
     */
    @Override
    public java.util.List<java.util.Map<String, Object>> tryDraw(Long deptId, Exam params) {
        if (params == null) {
            throw new ServiceException("缺少试抽参数");
        }
        // ★ 2026-09-24：允许**不配知识点配比**（「整池抽题」模式）—— 此时按卷面题型数量
        //   从本部门整个理论题池抽。PaperDrawer 对「空 rules + 有题型数量」已支持整池补齐
        //   （fillQuota 的兜底分支会以 qtype 从 deptId 全池抽）。
        int single = nz(params.getSingleCount());
        int multi = nz(params.getMultiCount());
        int judge = nz(params.getJudgeCount());
        boolean hasRules = params.getKnowledgeRules() != null && !params.getKnowledgeRules().isEmpty();
        if (!hasRules && single + multi + judge <= 0) {
            throw new ServiceException("请配置知识点配比，或先设置卷面题型数量（单选 / 多选 / 判断）后再试抽");
        }
        return drawByKnowledge(resolveDeptId(deptId), params.getKnowledgeRules(), single, multi, judge);
    }

    /** 按知识分布试抽（部门从请求参数或当前账号部门解析） */
    @Override
    public java.util.List<java.util.Map<String, Object>> tryDraw(Exam params) {
        Long deptId = params == null ? null : params.getDeptId();
        return tryDraw(deptId, params);
    }

    /**
     * 从部门题池按「知识点配比 + 卷面题型配额」抽题（不落库），返回试抽明细。
     *
     * <p>★ 2026-09-24：改走 {@link com.ruoyi.business.service.support.PaperDrawer}，与真实模拟卷
     * （{@code PracticeServiceImpl}）/正式卷（{@code AnswerSheetServiceImpl}）同一口径。
     * 原实现只按知识点抽 N 题、**不分题型**，导致试抽预览里出现的题型分布与卷面设的
     * 单选/多选/判断 对不上（预览骗人）。</p>
     */
    private java.util.List<java.util.Map<String, Object>> drawByKnowledge(
            Long deptId, java.util.List<com.ruoyi.business.domain.ExamKnowledgeRule> rules,
            int single, int multi, int judge) {
        java.util.List<com.ruoyi.business.domain.Question> picked = com.ruoyi.business.service.support.PaperDrawer
                .draw(examRuleMapper, questionMapper, deptId, rules, single, multi, judge, 0, 0);
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        int no = 1;
        for (com.ruoyi.business.domain.Question q : picked) {
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("seq", no++);
            m.put("questionId", q.getId());
            m.put("qtype", q.getQtype());
            m.put("knowledgePoint", q.getKnowledgePoint());
            m.put("stem", q.getStem());
            result.add(m);
        }
        return result;
    }

    /**
     * 发布前确认考核内容齐备。
     *
     * 理论：卷面从本部门题池按知识点抽题 → 必须有题型数量，且题池可用题量够；
     * 实操：题目是管理员逐条填写的清单 → 必须至少有一道题；
     *      同时把「题量 / 总分」汇总回写，列表与前端都用这两个值展示。
     */
    private void assertDrawConfigReady(Exam exam) {
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
        // 理论：从本部门题池抽题 → 必须有题型数量（卷面结构），且题池里得有题
        int total = nz(exam.getSingleCount()) + nz(exam.getMultiCount()) + nz(exam.getJudgeCount());
        if (total <= 0) {
            throw new ServiceException("发布前请先配置题目数量（单选 / 多选 / 判断）");
        }
        java.util.Map<String, Object> avail = examRuleMapper.selectDeptAvailable(exam.getDeptId());
        int available = intOf(avail == null ? null : avail.get("totalCount"));
        if (available <= 0) {
            throw new ServiceException("本部门理论题池暂无题目，请先在「题库管理」导入题目");
        }
        if (available < total) {
            throw new ServiceException("本部门理论题池可用题目（" + available + " 题）少于卷面题量（" + total + " 题），请减少题量或补充题目");
        }
    }

    /**
     * 发布校验：通过线不能超过卷面满分。
     *
     * 通过线缺失（null）或未设（<=0）不拦；只要设了正数，就必须 ≤ 卷面满分。
     * 卷面满分：实操 = 各题满分之和；理论 = 单选/多选/判断 每题分值 × 题量之和。
     */
    private void assertPassLineValid(Exam exam, Exam params) {
        java.math.BigDecimal passLine = params != null && params.getPassLine() != null
                ? params.getPassLine() : exam.getPassLine();
        if (passLine == null || passLine.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return;
        }
        java.math.BigDecimal total = computeTotalScore(exam, params);
        if (passLine.compareTo(total) > 0) {
            throw new ServiceException("通过线（" + fmtScore(passLine) + " 分）不能超过卷面满分（"
                    + fmtScore(total) + " 分），请调整通过线或题目分值");
        }
    }

    /**
     * 计算卷面满分。分值优先取本次提交（params），缺失回退到库中（exam）；
     * 题量取 exam 的题型数量（卷面结构）。
     */
    private java.math.BigDecimal computeTotalScore(Exam exam, Exam params) {
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
        // 题量取 exam 的题型数量（卷面结构）
        int singleCount = nz(exam.getSingleCount());
        int multiCount = nz(exam.getMultiCount());
        int judgeCount = nz(exam.getJudgeCount());
        return single.multiply(java.math.BigDecimal.valueOf(singleCount))
                .add(multi.multiply(java.math.BigDecimal.valueOf(multiCount)))
                .add(judge.multiply(java.math.BigDecimal.valueOf(judgeCount)));
    }

    /** 分数展示：去掉无意义的末尾 0（60.0 → 60、43.5 → 43.5） */
    private static String fmtScore(java.math.BigDecimal v) {
        java.math.BigDecimal s = v.stripTrailingZeros();
        return s.scale() < 0 ? s.setScale(0).toPlainString() : s.toPlainString();
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

    /**
     * 知识分布校验：① 占比之和（如传）② 抽题数量合计须等于卷面题量 ③ 不超本部门题池可用题量
     * ④ 分题型可用量须覆盖卷面题型数量 ⑤（分题型模式）各题型合计须与卷面逐题型一致。
     *
     * <p>★ 2026-09-24 调整：**允许某知识点抽题数量为 0**（0 = 该知识点不参与抽题），
     * 不再要求「每个知识点都必须有分配」—— 页面会把题池全部知识点列出来，只给需要的填数量。
     * 同时把「卷面题量」口径从 {@code exam.question_count} 改为「本次提交的 单选+多选+判断」
     * （回退库中值），避免首次配置时库里还是 0 导致该校验被静默跳过。</p>
     *
     * <p>★ 2026-09-24 二次调整：知识点配比改为**按题型分别分配**。任一行的
     * {@code singleCount/multiCount/judgeCount} 三者和 &gt; 0 即视为「分题型模式」，
     * 此时要求 每个题型的合计 必须**逐类等于**卷面该题型数量（不再靠比例猜），
     * 并逐行核对单行某题型是否超该知识点该题型的可用量。</p>
     */
    private void validateKnowledgeRules(Exam exam, Exam params) {
        if (params == null || params.getKnowledgeRules() == null || params.getKnowledgeRules().isEmpty()) {
            return;
        }
        int countSum = 0;
        int singleSum = 0;
        int multiSum = 0;
        int judgeSum = 0;
        boolean typed = false;
        java.math.BigDecimal ratioSum = java.math.BigDecimal.ZERO;
        for (com.ruoyi.business.domain.ExamKnowledgeRule rule : params.getKnowledgeRules()) {
            int s = rule.getSingleCount() == null ? 0 : rule.getSingleCount();
            int m = rule.getMultiCount() == null ? 0 : rule.getMultiCount();
            int j = rule.getJudgeCount() == null ? 0 : rule.getJudgeCount();
            if (s < 0 || m < 0 || j < 0) {
                throw new ServiceException("知识配比「" + rule.getKnowledgePoint() + "」的分题型数量不能为负数");
            }
            int n = rule.getQuestionCount() == null ? 0 : rule.getQuestionCount();
            if (n < 0) {
                throw new ServiceException("知识配比「" + rule.getKnowledgePoint() + "」的抽题数量不能为负数");
            }
            if (s + m + j > 0) {
                // 分题型模式：行小计以三者之和为准
                typed = true;
                n = s + m + j;
            }
            // ★ n == 0 是合法值：表示该知识点不参与抽题（原「必须大于 0」已按要求去掉）
            singleSum += s;
            multiSum += m;
            judgeSum += j;
            countSum += n;
            if (rule.getRatio() != null) {
                ratioSum = ratioSum.add(rule.getRatio());
            }
        }
        if (ratioSum.compareTo(java.math.BigDecimal.ZERO) > 0
                && ratioSum.subtract(new java.math.BigDecimal("100")).abs().compareTo(new java.math.BigDecimal("0.01")) > 0) {
            throw new ServiceException("知识分布占比之和须为 100%，当前为 " + ratioSum.stripTrailingZeros().toPlainString() + "%");
        }
        int paperTotal = resolvePaperTotal(exam, params);
        if (paperTotal > 0 && countSum != paperTotal) {
            throw new ServiceException("知识点抽题数量合计（" + countSum + " 题）须等于卷面题量（" + paperTotal
                    + " 题），请调整各知识点的抽题数量或上方的题型数量");
        }
        // ★ 分题型配平校验：逐类合计必须等于卷面该题型数量
        if (typed) {
            int ps = params.getSingleCount() == null ? nz(exam.getSingleCount()) : params.getSingleCount();
            int pm = params.getMultiCount() == null ? nz(exam.getMultiCount()) : params.getMultiCount();
            int pj = params.getJudgeCount() == null ? nz(exam.getJudgeCount()) : params.getJudgeCount();
            java.util.List<String> mismatch = new java.util.ArrayList<>();
            if (singleSum != ps) {
                mismatch.add("单选 已配 " + singleSum + " / 卷面 " + ps);
            }
            if (multiSum != pm) {
                mismatch.add("多选 已配 " + multiSum + " / 卷面 " + pm);
            }
            if (judgeSum != pj) {
                mismatch.add("判断 已配 " + judgeSum + " / 卷面 " + pj);
            }
            if (!mismatch.isEmpty()) {
                throw new ServiceException("知识点配比的题型数量与卷面题型数量不一致（" + String.join("；", mismatch)
                        + "），请按题型逐类配平");
            }
        }
        // 本部门题池可用题量校验（按知识点，防抽不出题）
        java.util.List<java.util.Map<String, Object>> available = examRuleMapper.selectDeptKnowledgePoints(exam.getDeptId());
        java.util.Map<String, Integer> availableMap = new java.util.HashMap<>();
        java.util.Map<String, java.util.Map<String, Object>> rowMap = new java.util.HashMap<>();
        for (java.util.Map<String, Object> row : nvl(available)) {
            String kp = String.valueOf(row.get("knowledgePoint"));
            availableMap.put(kp, intOf(row.get("totalCount")));
            rowMap.put(kp, row);
        }
        for (com.ruoyi.business.domain.ExamKnowledgeRule rule : params.getKnowledgeRules()) {
            Integer have = availableMap.get(rule.getKnowledgePoint());
            if (have != null && rule.getQuestionCount() != null && rule.getQuestionCount() > have) {
                throw new ServiceException("知识配比「" + rule.getKnowledgePoint() + "」抽题数量 "
                        + rule.getQuestionCount() + " 题超过本部门题池可用的 " + have + " 题");
            }
            // ★ 分题型模式下逐行核对「某题型要几题 / 该知识点该题型只有几题」，能精确指出哪一行差多少
            if (typed) {
                java.util.Map<String, Object> poolRow = rowMap.get(rule.getKnowledgePoint());
                if (poolRow == null) {
                    continue;
                }
                checkPointType(rule.getKnowledgePoint(), "单选", rule.getSingleCount(), intOf(poolRow.get("singleCount")));
                checkPointType(rule.getKnowledgePoint(), "多选", rule.getMultiCount(), intOf(poolRow.get("multiCount")));
                checkPointType(rule.getKnowledgePoint(), "判断", rule.getJudgeCount(), intOf(poolRow.get("judgeCount")));
            }
        }
        validateTypeQuota(exam, params, available);
    }

    /** 单行某题型不得超该知识点该题型可用量 */
    private void checkPointType(String point, String typeName, Integer need, int have) {
        int n = need == null ? 0 : need;
        if (n > have) {
            throw new ServiceException("知识配比「" + point + "」的" + typeName + "要 " + n
                    + " 题，但本部门题池该知识点的" + typeName + "只有 " + have + " 题");
        }
    }

    /**
     * 卷面题量：优先「本次提交的 单选+多选+判断」，回退库中 {@code question_count}，
     * 再回退库中题型数量合计。
     */
    private int resolvePaperTotal(Exam exam, Exam params) {
        if (params != null && (params.getSingleCount() != null || params.getMultiCount() != null || params.getJudgeCount() != null)) {
            int s = params.getSingleCount() == null ? nz(exam.getSingleCount()) : params.getSingleCount();
            int m = params.getMultiCount() == null ? nz(exam.getMultiCount()) : params.getMultiCount();
            int j = params.getJudgeCount() == null ? nz(exam.getJudgeCount()) : params.getJudgeCount();
            return s + m + j;
        }
        int stored = nz(exam.getQuestionCount());
        if (stored > 0) {
            return stored;
        }
        return nz(exam.getSingleCount()) + nz(exam.getMultiCount()) + nz(exam.getJudgeCount());
    }

    /**
     * ★ 2026-09-24 新增：分题型可用量校验（理论卷）。
     *
     * <p>卷面声明的 单选/多选/判断 数量，必须能被「参与抽题的知识点（抽题数量 &gt; 0）」按题型覆盖：
     * 把这些知识点的该题型可用量相加，任一题型不足就明确报出「哪个题型、需要几题、现有几题」。
     * 这样管理员在保存时就能看到具体缺口，而不是等到抽题时才发现抽不满。</p>
     */
    private void validateTypeQuota(Exam exam, Exam params, java.util.List<java.util.Map<String, Object>> poolRows) {
        if (!"THEORY".equals(exam.getExamType())) {
            return;
        }
        int needSingle = params.getSingleCount() == null ? nz(exam.getSingleCount()) : params.getSingleCount();
        int needMulti = params.getMultiCount() == null ? nz(exam.getMultiCount()) : params.getMultiCount();
        int needJudge = params.getJudgeCount() == null ? nz(exam.getJudgeCount()) : params.getJudgeCount();
        if (needSingle <= 0 && needMulti <= 0 && needJudge <= 0) {
            return;
        }
        java.util.Set<String> chosen = new java.util.HashSet<>();
        for (com.ruoyi.business.domain.ExamKnowledgeRule rule : params.getKnowledgeRules()) {
            if (rule.getQuestionCount() != null && rule.getQuestionCount() > 0 && rule.getKnowledgePoint() != null) {
                chosen.add(rule.getKnowledgePoint());
            }
        }
        if (chosen.isEmpty()) {
            return;
        }
        int haveSingle = 0;
        int haveMulti = 0;
        int haveJudge = 0;
        for (java.util.Map<String, Object> row : nvl(poolRows)) {
            if (!chosen.contains(String.valueOf(row.get("knowledgePoint")))) {
                continue;
            }
            haveSingle += intOf(row.get("singleCount"));
            haveMulti += intOf(row.get("multiCount"));
            haveJudge += intOf(row.get("judgeCount"));
        }
        java.util.List<String> lacks = new java.util.ArrayList<>();
        if (needSingle > haveSingle) {
            lacks.add("单选 需要 " + needSingle + " 题、所选知识点共 " + haveSingle + " 题");
        }
        if (needMulti > haveMulti) {
            lacks.add("多选 需要 " + needMulti + " 题、所选知识点共 " + haveMulti + " 题");
        }
        if (needJudge > haveJudge) {
            lacks.add("判断 需要 " + needJudge + " 题、所选知识点共 " + haveJudge + " 题");
        }
        if (!lacks.isEmpty()) {
            throw new ServiceException("卷面题型数量超出所选知识点的可用题量（" + String.join("；", lacks)
                    + "）。请调整卷面题型数量、改选其它知识点，或先到「题库管理」补齐题目");
        }
    }

    private static <T> java.util.List<T> nvl(java.util.List<T> list) {
        return list == null ? java.util.Collections.<T>emptyList() : list;
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
            // ★ 2026-09-24：分题型三列归一化；只要传了分题型数量，question_count 就回写为行小计
            int s = rule.getSingleCount() == null ? 0 : rule.getSingleCount();
            int m = rule.getMultiCount() == null ? 0 : rule.getMultiCount();
            int j = rule.getJudgeCount() == null ? 0 : rule.getJudgeCount();
            rule.setSingleCount(s);
            rule.setMultiCount(m);
            rule.setJudgeCount(j);
            if (s + m + j > 0) {
                rule.setQuestionCount(s + m + j);
            } else if (rule.getQuestionCount() == null) {
                rule.setQuestionCount(0);
            }
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

    private boolean isGlobalReadOnly() {
        return SecurityUtils.hasRole("SUPER_ADMIN") || SecurityUtils.isAdmin(SecurityUtils.getUserId());
    }
}
