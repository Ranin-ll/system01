package com.ruoyi.business.service.impl;

import com.ruoyi.business.domain.Exam;
import com.ruoyi.business.domain.PracticeModule;
import com.ruoyi.business.domain.PracticeRecord;
import com.ruoyi.business.domain.PracticeRecordItem;
import com.ruoyi.business.domain.Question;
import com.ruoyi.business.domain.QuestionBank;
import com.ruoyi.business.mapper.ExamMapper;
import com.ruoyi.business.mapper.PracticeModuleMapper;
import com.ruoyi.business.mapper.PracticeRecordItemMapper;
import com.ruoyi.business.mapper.PracticeRecordMapper;
import com.ruoyi.business.mapper.QuestionBankMapper;
import com.ruoyi.business.mapper.QuestionMapper;
import com.ruoyi.business.service.IPracticeService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 模拟考核 Service 实现
 *
 * 规则：实习生先选「模块」→ 再选模块下的「理论模拟考核」，按该考核的组卷配置抽题；
 * 未指定考核时回退为本部门最近发布的一套模拟配置，仍未配置则「本部门模拟题库随机抽 10 题、每题 1 分」。
 * 交卷自动判分并记录，成绩仅供实习生本人查询，管理员不可见。
 *
 * 交卷时同时写入逐题明细（practice_record_item），供本人回看题目、作答与正确答案。
 */
@Service
public class PracticeServiceImpl implements IPracticeService {

    private final QuestionBankMapper questionBankMapper;
    private final QuestionMapper questionMapper;
    private final PracticeRecordMapper practiceRecordMapper;
    private final PracticeRecordItemMapper practiceRecordItemMapper;
    private final com.ruoyi.business.mapper.ExamRuleMapper examRuleMapper;
    private final ExamMapper examMapper;
    private final PracticeModuleMapper practiceModuleMapper;

    public PracticeServiceImpl(QuestionBankMapper questionBankMapper,
                               QuestionMapper questionMapper,
                               PracticeRecordMapper practiceRecordMapper,
                               PracticeRecordItemMapper practiceRecordItemMapper,
                               com.ruoyi.business.mapper.ExamRuleMapper examRuleMapper,
                               ExamMapper examMapper,
                               PracticeModuleMapper practiceModuleMapper) {
        this.questionBankMapper = questionBankMapper;
        this.questionMapper = questionMapper;
        this.practiceRecordMapper = practiceRecordMapper;
        this.practiceRecordItemMapper = practiceRecordItemMapper;
        this.examRuleMapper = examRuleMapper;
        this.examMapper = examMapper;
        this.practiceModuleMapper = practiceModuleMapper;
    }

    @Override
    public Map<String, Object> startPractice(Long examId) {
        assertPreTrainee();
        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null) {
            throw new ServiceException("当前账号未配置部门，无法进行模拟考核");
        }
        QuestionBank bank = questionBankMapper.selectPracticeBankByDept(deptId);

        // 本次考核口径：优先用模块下选定的理论模拟考核，未指定则用本部门最近发布的一套配置
        Map<String, Object> config = resolveConfig(deptId, examId);
        Long fallbackBankId = null;
        if (config != null && config.get("bankId") != null) {
            fallbackBankId = Long.valueOf(String.valueOf(config.get("bankId")));
        }
        if (fallbackBankId == null && bank != null) {
            fallbackBankId = bank.getId();
        }

        List<Question> questions;
        Map<String, Object> result = new LinkedHashMap<>();
        if (config != null && config.get("examId") != null) {
            Long cfgExamId = Long.valueOf(String.valueOf(config.get("examId")));
            java.util.List<com.ruoyi.business.domain.ExamBankRule> bankRules = examRuleMapper.selectBankRules(cfgExamId);
            if (bankRules != null && !bankRules.isEmpty()) {
                // 多题库组卷（当前主用）：每个题库按 单选/多选/判断 配额各自抽题
                questions = pickByBankRules(bankRules);
                result.put("bankRules", toBankRuleView(bankRules));
            } else {
                // 历史链路：单库 + 知识分布
                if (fallbackBankId == null) {
                    throw new ServiceException("本部门暂无模拟题库，请联系管理员");
                }
                List<com.ruoyi.business.domain.ExamKnowledgeRule> rules = examRuleMapper.selectKnowledgeRules(cfgExamId);
                questions = pickByConfig(fallbackBankId, rules, config);
                result.put("points", rules);
            }
            result.put("configured", true);
            result.put("examId", cfgExamId);
            result.put("configName", config.get("examName"));
            result.put("questionCount", config.get("questionCount"));
            result.put("singleCount", config.get("singleCount"));
            result.put("multiCount", config.get("multiCount"));
            result.put("judgeCount", config.get("judgeCount"));
            result.put("singleScore", config.get("singleScore"));
            result.put("multiScore", config.get("multiScore"));
            result.put("judgeScore", config.get("judgeScore"));
            result.put("duration", config.get("duration"));
            result.put("passLine", config.get("passLine"));
        } else {
            if (bank == null) {
                throw new ServiceException("本部门暂无模拟题库，请联系管理员");
            }
            questions = questionMapper.selectQuestionsForExam(bank.getId(), null, 10);
            result.put("configured", false);
        }
        if (questions.isEmpty()) {
            throw new ServiceException("模拟题库暂无题目，请等待管理员补充");
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Question q : questions) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", q.getId());
            m.put("qtype", q.getQtype());
            m.put("stem", q.getStem());
            m.put("optionsJson", q.getOptionsJson());
            m.put("knowledgePoint", q.getKnowledgePoint());
            list.add(m);
        }
        result.put("bankId", fallbackBankId);
        result.put("bankName", bank != null ? bank.getBankName() : null);
        result.put("questions", list);
        return result;
    }

    /**
     * 解析本次考核的配置口径。
     *
     * @param deptId 实习生所在部门
     * @param examId 模块下选定的理论模拟考核ID；为空时回退为本部门最近发布的一套模拟配置
     */
    private Map<String, Object> resolveConfig(Long deptId, Long examId) {
        if (examId == null) {
            return examRuleMapper.selectActivePracticeConfig(deptId);
        }
        Exam exam = examMapper.selectExamById(examId, null);
        if (exam == null
                || !"PRACTICE".equals(exam.getExamMode())
                || !"THEORY".equals(exam.getExamType())
                || !"PUBLISHED".equals(exam.getStatus())
                || exam.getDeptId() == null
                || !exam.getDeptId().equals(deptId)) {
            throw new ServiceException("考核不存在或未发布");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("examId", exam.getId());
        m.put("examName", exam.getExamName());
        m.put("bankId", exam.getBankId());
        m.put("questionCount", exam.getQuestionCount());
        m.put("singleCount", exam.getSingleCount());
        m.put("multiCount", exam.getMultiCount());
        m.put("judgeCount", exam.getJudgeCount());
        m.put("singleScore", exam.getSingleScore());
        m.put("multiScore", exam.getMultiScore());
        m.put("judgeScore", exam.getJudgeScore());
        m.put("duration", exam.getDuration());
        m.put("passLine", exam.getPassLine());
        return m;
    }

    @Override
    public List<Map<String, Object>> listPracticeExams(Long moduleId) {
        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null) {
            throw new ServiceException("当前账号未配置部门，无法查看模拟考核");
        }
        if (moduleId != null) {
            PracticeModule module = practiceModuleMapper.selectModuleById(moduleId);
            if (module == null || !deptId.equals(module.getDeptId())
                    || module.getStatus() == null || module.getStatus() != 1) {
                throw new ServiceException("模块不存在或未启用");
            }
        }
        Exam query = new Exam();
        query.setExamMode("PRACTICE");
        query.setExamType("THEORY");
        query.setStatus("PUBLISHED");
        query.setDeptId(deptId);
        query.setModuleId(moduleId);
        query.setScopeDeptId(deptId);
        List<Exam> exams = examMapper.selectExamList(query);
        List<Map<String, Object>> out = new ArrayList<>();
        if (exams == null) {
            return out;
        }
        for (Exam e : exams) {
            Map<String, Object> m = new LinkedHashMap<>();
            int total = intOf(e.getSingleCount()) + intOf(e.getMultiCount()) + intOf(e.getJudgeCount());
            BigDecimal full = scoreOf(e.getSingleCount(), e.getSingleScore())
                    .add(scoreOf(e.getMultiCount(), e.getMultiScore()))
                    .add(scoreOf(e.getJudgeCount(), e.getJudgeScore()));
            m.put("id", e.getId());
            m.put("examId", e.getId());
            m.put("examName", e.getExamName());
            m.put("moduleId", e.getModuleId());
            m.put("singleCount", e.getSingleCount());
            m.put("multiCount", e.getMultiCount());
            m.put("judgeCount", e.getJudgeCount());
            m.put("questionCount", total);
            m.put("fullScore", full);
            m.put("duration", e.getDuration());
            m.put("passLine", e.getPassLine());
            out.add(m);
        }
        return out;
    }

    /** 题量 × 每题分值 */
    private BigDecimal scoreOf(Integer count, BigDecimal unit) {
        int c = count == null ? 0 : count;
        BigDecimal u = unit == null ? BigDecimal.ZERO : unit;
        return u.multiply(BigDecimal.valueOf(c));
    }

    /**
     * 按多题库组卷配置抽题：对每个题库，分别按 单选/多选/判断 的配置数量在该库内随机取题。
     * 不同题库之间互不影响，跨库用 usedIds 去重兜底。
     */
    private List<Question> pickByBankRules(List<com.ruoyi.business.domain.ExamBankRule> bankRules) {
        List<Question> picked = new ArrayList<>();
        Set<Long> usedIds = new LinkedHashSet<>();
        for (com.ruoyi.business.domain.ExamBankRule rule : bankRules) {
            addAll(picked, usedIds, pickBankType(rule.getBankId(), "SINGLE", intOf(rule.getSingleCount()), usedIds));
            addAll(picked, usedIds, pickBankType(rule.getBankId(), "MULTI", intOf(rule.getMultiCount()), usedIds));
            addAll(picked, usedIds, pickBankType(rule.getBankId(), "JUDGE", intOf(rule.getJudgeCount()), usedIds));
        }
        return picked;
    }

    /** 从某题库抽指定题型的题（不限知识点） */
    private List<Question> pickBankType(Long bankId, String qtype, int need, Set<Long> usedIds) {
        if (need <= 0) {
            return Collections.emptyList();
        }
        List<Question> list = examRuleMapper.selectQuestionsByPoint(bankId, null, qtype, new ArrayList<>(usedIds), need);
        return list == null ? Collections.emptyList() : list;
    }

    /** 组卷配置转前端展示结构（题库名 = 知识模块名） */
    private List<Map<String, Object>> toBankRuleView(List<com.ruoyi.business.domain.ExamBankRule> bankRules) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (com.ruoyi.business.domain.ExamBankRule rule : bankRules) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("bankId", rule.getBankId());
            m.put("bankName", rule.getBankName());
            m.put("singleCount", rule.getSingleCount());
            m.put("multiCount", rule.getMultiCount());
            m.put("judgeCount", rule.getJudgeCount());
            list.add(m);
        }
        return list;
    }

    /**
     * 按配置抽题（知识分布 + 题型配比同时满足）：
     *
     * 核心思路：把每个知识点的抽题数量**按全局题型比例分摊**到单选/多选/判断，
     * 于是「每个知识点内按题型取题」天然满足题型配额，不需要事后裁剪（裁剪会破坏知识覆盖）。
     * 某知识点某题型不够时，先在该知识点内换其它题型补足（保覆盖），再从题库按题型补齐。
     */
    private List<Question> pickByConfig(Long bankId,
                                        List<com.ruoyi.business.domain.ExamKnowledgeRule> rules,
                                        Map<String, Object> config) {
        int single = intOf(config.get("singleCount"));
        int multi = intOf(config.get("multiCount"));
        int judge = intOf(config.get("judgeCount"));
        int typeTotal = single + multi + judge;
        int configureCount = intOf(config.get("questionCount"));

        List<Question> picked = new ArrayList<>();
        Set<Long> usedIds = new LinkedHashSet<>();

        if (rules != null && !rules.isEmpty()) {
            for (com.ruoyi.business.domain.ExamKnowledgeRule rule : rules) {
                int need = rule.getQuestionCount() == null ? 0 : rule.getQuestionCount();
                if (need <= 0) {
                    continue;
                }
                int[] alloc = splitByTypeRatio(need, single, multi, judge, rule.getSingleRatio());
                addAll(picked, usedIds, pickPoint(bankId, rule.getKnowledgePoint(), "SINGLE", alloc[0], usedIds));
                addAll(picked, usedIds, pickPoint(bankId, rule.getKnowledgePoint(), "MULTI", alloc[1], usedIds));
                addAll(picked, usedIds, pickPoint(bankId, rule.getKnowledgePoint(), "JUDGE", alloc[2], usedIds));
                // 该知识点题型不足 → 用该知识点其它题型补足（保知识覆盖）
                int missing = need - (int) countInPoint(picked, rule.getKnowledgePoint());
                if (missing > 0) {
                    addAll(picked, usedIds, examRuleMapper.selectQuestionsByPoint(
                            bankId, rule.getKnowledgePoint(), null, new ArrayList<>(usedIds), missing));
                }
            }
        }

        int target = typeTotal > 0 ? typeTotal : (configureCount > 0 ? configureCount : (picked.isEmpty() ? 10 : picked.size()));

        // 题型配额补足（同知识点优先 → 题库任意）；先裁掉超额题型（优先裁「该知识点抽题数已超配」的）
        if (typeTotal > 0) {
            trimToQuota(picked, rules, "JUDGE", judge);
            trimToQuota(picked, rules, "MULTI", multi);
            trimToQuota(picked, rules, "SINGLE", single);
            fillQuota(bankId, picked, usedIds, "SINGLE", single);
            fillQuota(bankId, picked, usedIds, "MULTI", multi);
            fillQuota(bankId, picked, usedIds, "JUDGE", judge);
        }
        // 总数不足 → 题库随机补齐；总数超出 → 截断
        if (picked.size() < target) {
            for (Question q : questionMapper.selectQuestionsForExam(bankId, null, target)) {
                if (picked.size() >= target) {
                    break;
                }
                if (!usedIds.contains(q.getId())) {
                    picked.add(q);
                    usedIds.add(q.getId());
                }
            }
        }
        return picked.size() > target ? new ArrayList<>(picked.subList(0, target)) : picked;
    }

    /** 从某知识点抽指定题型的题（不足则有多少返回多少） */
    private List<Question> pickPoint(Long bankId, String point, String qtype, int need, Set<Long> usedIds) {
        if (need <= 0) {
            return Collections.emptyList();
        }
        List<Question> list = examRuleMapper.selectQuestionsByPoint(bankId, point, qtype, new ArrayList<>(usedIds), need);
        return list == null ? Collections.emptyList() : list;
    }

    private long countInPoint(List<Question> list, String point) {
        return list.stream().filter(q -> point != null && point.equals(q.getKnowledgePoint())).count();
    }

    /**
     * 把知识点抽题数量按全局题型比例分摊：单选 / 多选 / 判断
     * singleRatio（该知识点的单选占比）优先，其余按全局多选:判断比例分摊。
     */
    private int[] splitByTypeRatio(int count, int single, int multi, int judge, java.math.BigDecimal singleRatio) {
        int s, m, j;
        if (singleRatio != null) {
            s = Math.round(count * singleRatio.floatValue() / 100f);
            int rest = count - s;
            int mj = multi + judge;
            if (mj <= 0) {
                m = rest;
                j = 0;
            } else {
                m = Math.round(rest * (multi * 1f / mj));
                j = rest - m;
            }
        } else if (single + multi + judge > 0) {
            int total = single + multi + judge;
            s = Math.round(count * (single * 1f / total));
            m = Math.round(count * (multi * 1f / total));
            j = count - s - m;
        } else {
            s = count;
            m = 0;
            j = 0;
        }
        if (s < 0) s = 0;
        if (m < 0) m = 0;
        if (j < 0) j = 0;
        // 修正取整误差，保证合计 = count
        int sum = s + m + j;
        if (sum != count) {
            s += count - sum;
            if (s < 0) {
                s = 0;
            }
            sum = s + m + j;
            if (sum != count) {
                m += count - sum;
            }
        }
        return new int[]{s, m, j};
    }

    /**
     * 裁剪超出配额的题型：
     * 优先裁掉「所在知识点的抽题数已超过配置数」的题（说明该知识点抽多了，裁掉不影响覆盖），
     * 其次裁掉最后加入的（通常是补齐题）。被裁掉的 id 放回池子，供其它题型补题使用。
     */
    private void trimToQuota(List<Question> list, List<com.ruoyi.business.domain.ExamKnowledgeRule> rules,
                             String qtype, int quota) {
        long have = list.stream().filter(q -> qtype.equals(q.getQtype())).count();
        int surplus = (int) (have - quota);
        if (surplus <= 0) {
            return;
        }
        java.util.Map<String, Integer> configured = new java.util.HashMap<>();
        java.util.Map<String, Integer> current = new java.util.HashMap<>();
        if (rules != null) {
            for (com.ruoyi.business.domain.ExamKnowledgeRule r : rules) {
                configured.put(r.getKnowledgePoint(), r.getQuestionCount() == null ? 0 : r.getQuestionCount());
            }
        }
        for (Question q : list) {
            String p = q.getKnowledgePoint();
            current.put(p, (current.containsKey(p) ? current.get(p) : 0) + 1);
        }
        for (int i = list.size() - 1; i >= 0 && surplus > 0; i--) {
            Question q = list.get(i);
            if (!qtype.equals(q.getQtype())) {
                continue;
            }
            String p = q.getKnowledgePoint();
            int cfg = configured.containsKey(p) ? configured.get(p) : 0;
            int cur = current.containsKey(p) ? current.get(p) : 0;
            if (cur > cfg || i >= list.size() - 1) {
                list.remove(i);
                current.put(p, cur - 1);
                surplus--;
            }
        }
    }

    /** 补足某题型的配额：先同知识点，再从题库任意抽 */
    private void fillQuota(Long bankId, List<Question> list, Set<Long> usedIds, String qtype, int quota) {
        long have = list.stream().filter(q -> qtype.equals(q.getQtype())).count();
        int lack = (int) (quota - have);
        if (lack <= 0) {
            return;
        }
        java.util.Set<String> points = new LinkedHashSet<>();
        for (Question q : list) {
            if (q.getKnowledgePoint() != null) {
                points.add(q.getKnowledgePoint());
            }
        }
        for (String point : points) {
            if (lack <= 0) {
                break;
            }
            List<Question> extra = examRuleMapper.selectQuestionsByPoint(bankId, point, qtype, new ArrayList<>(usedIds), lack);
            if (extra != null) {
                for (Question q : extra) {
                    if (lack <= 0) {
                        break;
                    }
                    if (!usedIds.contains(q.getId())) {
                        list.add(q);
                        usedIds.add(q.getId());
                        lack--;
                    }
                }
            }
        }
        if (lack > 0) {
            List<Question> extra = examRuleMapper.selectQuestionsByPoint(bankId, null, qtype, new ArrayList<>(usedIds), lack);
            if (extra != null) {
                for (Question q : extra) {
                    if (!usedIds.contains(q.getId())) {
                        list.add(q);
                        usedIds.add(q.getId());
                    }
                }
            }
        }
    }

    private void addAll(List<Question> target, Set<Long> usedIds, List<Question> src) {
        if (src == null) {
            return;
        }
        for (Question q : src) {
            if (!usedIds.contains(q.getId())) {
                target.add(q);
                usedIds.add(q.getId());
            }
        }
    }

    private int intOf(Object v) {
        if (v == null) {
            return 0;
        }
        try {
            return (int) Double.parseDouble(String.valueOf(v));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submitPractice(Long examId, Long bankId, List<Map<String, Object>> answers) {
        assertPreTrainee();
        Long userId = SecurityUtils.getUserId();
        Long deptId = SecurityUtils.getDeptId();

        // 1. 解析前端提交的作答，保持提交顺序（前端按题序下发）
        Set<Long> orderedIds = new LinkedHashSet<>();
        Map<Long, String> userAnswerMap = new HashMap<>();
        if (answers != null) {
            for (Map<String, Object> a : answers) {
                if (a == null || a.get("questionId") == null) {
                    continue;
                }
                Long qid;
                try {
                    qid = Long.valueOf(String.valueOf(a.get("questionId")).trim());
                } catch (NumberFormatException e) {
                    continue;
                }
                if (orderedIds.contains(qid)) {
                    continue;
                }
                orderedIds.add(qid);
                Object ua = a.get("userAnswer");
                userAnswerMap.put(qid, ua == null ? "" : String.valueOf(ua));
            }
        }

        // 2. 一次性取出题目（含答案、解析），用于判分与快照
        Map<Long, Question> questionMap = new HashMap<>();
        if (!orderedIds.isEmpty()) {
            List<Question> questions = questionMapper.selectQuestionsByIds(new ArrayList<>(orderedIds), null);
            for (Question q : questions) {
                questionMap.put(q.getId(), q);
            }
        }

        // 3. 逐题判分，构造返回明细 + 待落库明细
        //    分值口径：与开考时一致（模块下选定的考核配置，或本部门最近发布的一套配置），否则每题 1 分
        Map<String, Object> config = resolveConfig(deptId, examId);
        if (bankId == null && config != null && config.get("bankId") != null) {
            bankId = Long.valueOf(String.valueOf(config.get("bankId")));
        }
        // 来源考核ID：以**实际生效的配置**为准（examId 为空时会回退到本部门最近发布的一套配置）。
        // 落库后供「模拟考核记录」按模块归类 + 展示考核名称。
        Long resolvedExamId = examId;
        if (config != null && config.get("examId") != null) {
            resolvedExamId = Long.valueOf(String.valueOf(config.get("examId")));
        }
        java.math.BigDecimal singleScore = BigDecimal.valueOf(1);
        java.math.BigDecimal multiScore = BigDecimal.valueOf(1);
        java.math.BigDecimal judgeScore = BigDecimal.valueOf(1);
        java.math.BigDecimal passLine = null;
        if (config != null && config.get("examId") != null) {
            singleScore = decimalOf(config.get("singleScore"), BigDecimal.valueOf(1));
            multiScore = decimalOf(config.get("multiScore"), BigDecimal.valueOf(1));
            judgeScore = decimalOf(config.get("judgeScore"), BigDecimal.valueOf(1));
            passLine = decimalOf(config.get("passLine"), null);
        }

        int correct = 0;
        java.math.BigDecimal score = java.math.BigDecimal.ZERO;
        java.math.BigDecimal fullScore = java.math.BigDecimal.ZERO;
        List<Map<String, Object>> details = new ArrayList<>();
        List<PracticeRecordItem> items = new ArrayList<>();
        int seq = 0;
        for (Long qid : orderedIds) {
            seq++;
            Question q = questionMap.get(qid);
            String userAnswer = normalizeAnswer(userAnswerMap.get(qid));
            String correctAnswer = q == null ? "" : normalizeAnswer(q.getAnswer());
            boolean right = !correctAnswer.isEmpty() && userAnswer.equals(correctAnswer);
            if (right) {
                correct++;
            }
            java.math.BigDecimal unit = "MULTI".equals(q == null ? null : q.getQtype()) ? multiScore
                    : ("JUDGE".equals(q == null ? null : q.getQtype()) ? judgeScore : singleScore);
            fullScore = fullScore.add(unit);
            if (right) {
                score = score.add(unit);
            }

            details.add(buildDetail(qid, q, userAnswer, correctAnswer, right));

            PracticeRecordItem it = new PracticeRecordItem();
            it.setQuestionId(qid);
            it.setSortNo(seq);
            it.setQtype(q == null ? null : q.getQtype());
            it.setStem(q == null ? null : q.getStem());
            it.setOptionsJson(q == null ? null : q.getOptionsJson());
            it.setUserAnswer(userAnswer);
            it.setCorrectAnswer(correctAnswer);
            it.setIsCorrect(right ? 1 : 0);
            it.setAnalysis(q == null ? null : q.getAnalysis());
            items.add(it);
        }

        // 4. 落库：汇总记录 + 逐题明细
        int total = orderedIds.size();
        PracticeRecord record = new PracticeRecord();
        record.setUserId(userId);
        record.setBankId(bankId);
        record.setExamId(resolvedExamId);
        record.setDeptId(deptId);
        record.setTotalCount(total);
        record.setCorrectCount(correct);
        record.setScore(score.setScale(0, java.math.RoundingMode.HALF_UP).intValue());
        practiceRecordMapper.insertRecord(record);

        if (!items.isEmpty()) {
            Long recordId = record.getId();
            for (PracticeRecordItem it : items) {
                it.setRecordId(recordId);
            }
            practiceRecordItemMapper.insertBatch(items);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("recordId", record.getId());
        result.put("totalCount", total);
        result.put("correctCount", correct);
        result.put("score", score.setScale(1, java.math.RoundingMode.HALF_UP));
        result.put("fullScore", fullScore.setScale(1, java.math.RoundingMode.HALF_UP));
        result.put("passLine", passLine);
        result.put("passed", passLine != null && score.compareTo(passLine) >= 0);
        result.put("details", details);
        return result;
    }

    private java.math.BigDecimal decimalOf(Object v, java.math.BigDecimal def) {
        if (v == null) {
            return def;
        }
        try {
            return new java.math.BigDecimal(String.valueOf(v));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    @Override
    public List<PracticeRecord> myRecords(Long moduleId) {
        return practiceRecordMapper.selectMyRecords(SecurityUtils.getUserId(), moduleId);
    }

    @Override
    public Map<String, Object> recordDetail(Long recordId) {
        if (recordId == null) {
            throw new ServiceException("记录ID不能为空");
        }
        PracticeRecord record = practiceRecordMapper.selectRecordById(recordId);
        // 仅本人可见：记录不存在或不属于当前登录人，一律拒绝
        if (record == null || !SecurityUtils.getUserId().equals(record.getUserId())) {
            throw new ServiceException("记录不存在或无权查看");
        }
        List<PracticeRecordItem> items = practiceRecordItemMapper.selectByRecordId(recordId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("record", record);
        result.put("items", items == null ? new ArrayList<PracticeRecordItem>() : items);
        return result;
    }

    /** 构造返回给前端的逐题判分结果 */
    private Map<String, Object> buildDetail(Long qid, Question q, String userAnswer, String correctAnswer, boolean right) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("questionId", qid);
        d.put("qtype", q == null ? null : q.getQtype());
        d.put("stem", q == null ? null : q.getStem());
        d.put("optionsJson", q == null ? null : q.getOptionsJson());
        d.put("userAnswer", userAnswer);
        d.put("correctAnswer", correctAnswer);
        d.put("analysis", q == null ? null : q.getAnalysis());
        d.put("correct", right);
        return d;
    }

    private String normalizeAnswer(String answer) {
        if (answer == null) {
            return "";
        }
        String[] parts = answer.trim().toUpperCase().replace("，", ",").split(",");
        List<String> list = new ArrayList<>();
        for (String p : parts) {
            String t = p.trim();
            if ("对".equals(t) || "正确".equals(t) || "TRUE".equals(t) || "T".equals(t)) {
                t = "A";
            } else if ("错".equals(t) || "错误".equals(t) || "FALSE".equals(t) || "F".equals(t)) {
                t = "B";
            }
            if (!t.isEmpty()) {
                list.add(t);
            }
        }
        Collections.sort(list);
        return String.join(",", list);
    }

    private void assertPreTrainee() {
        String userStatus = SecurityUtils.getLoginUser().getUser().getUserStatus();
        if (!"PRE_TRAINEE".equals(userStatus)) {
            throw new ServiceException("当前账号已不处于预备实习阶段，不能参加模拟考核");
        }
    }
}
