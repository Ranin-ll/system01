package com.ruoyi.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.business.domain.AnswerSheet;
import com.ruoyi.business.domain.AnswerSheetItem;
import com.ruoyi.business.domain.Exam;
import com.ruoyi.business.domain.Question;
import com.ruoyi.business.mapper.AnswerSheetMapper;
import com.ruoyi.business.mapper.ExamMapper;
import com.ruoyi.business.mapper.QuestionMapper;
import com.ruoyi.business.service.IAnswerSheetService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 答卷Service实现
 *
 * 考核流程：开始考核抽题 → 交卷客观题自动判分 → 管理员批改实操题 → 发布成绩。
 */
@Service
public class AnswerSheetServiceImpl extends ServiceImpl<AnswerSheetMapper, AnswerSheet> implements IAnswerSheetService {

    private final AnswerSheetMapper answerSheetMapper;
    private final ExamMapper examMapper;
    private final QuestionMapper questionMapper;
    private final com.ruoyi.business.mapper.ExamRuleMapper examRuleMapper;

    public AnswerSheetServiceImpl(AnswerSheetMapper answerSheetMapper, ExamMapper examMapper,
                                  QuestionMapper questionMapper,
                                  com.ruoyi.business.mapper.ExamRuleMapper examRuleMapper) {
        this.answerSheetMapper = answerSheetMapper;
        this.examMapper = examMapper;
        this.questionMapper = questionMapper;
        this.examRuleMapper = examRuleMapper;
    }

    @Override
    public Map<String, Object> startExam(Long examId) {
        assertPreTrainee();
        Exam exam = examMapper.selectExamById(examId, null);
        if (exam == null) {
            throw new ServiceException("考核不存在");
        }
        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null || !deptId.equals(exam.getDeptId())) {
            throw new ServiceException("考核不存在或不适用于当前部门");
        }
        if (!"PUBLISHED".equals(exam.getStatus())) {
            throw new ServiceException("该考核未发布，暂不能参加");
        }
        // 时间窗校验（部门管理员设定的发布时间）
        Date now = new Date();
        if (exam.getStartTime() != null && now.before(exam.getStartTime())) {
            throw new ServiceException("该考核尚未开放，开放时间 " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(exam.getStartTime()));
        }
        if (exam.getEndTime() != null && now.after(exam.getEndTime())) {
            throw new ServiceException("该考核已截止，不能再参加");
        }
        // 指定人员校验
        java.util.List<Long> assigned = examRuleMapper.selectParticipantUserIds(examId);
        if (!assigned.isEmpty() && !assigned.contains(SecurityUtils.getUserId())) {
            throw new ServiceException("本场考核为指定人员参加，你不在名单内");
        }
        Long userId = SecurityUtils.getUserId();
        AnswerSheet exist = answerSheetMapper.selectByExamAndUser(examId, userId);
        if (exist != null) {
            throw new ServiceException("你已参加过该考核，不能重复参加");
        }

        String type = exam.getExamType() == null ? "THEORY" : exam.getExamType();
        AnswerSheet sheet = new AnswerSheet();
        sheet.setExamId(examId);
        sheet.setUserId(userId);
        sheet.setSheetType(type);
        sheet.setStatus("IN_PROGRESS");
        sheet.setStartTime(new Date());
        sheet.setRetakeSeq(1);
        sheet.setVersion(0);
        sheet.setCreateTime(new Date());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sheetId", null); // will be set after insert
        result.put("examName", exam.getExamName());
        result.put("examType", type);

        if ("THEORY".equals(type)) {
            // 理论考核：按题型分别从题库随机抽题
            if (exam.getBankId() == null) {
                throw new ServiceException("理论考核题库未配置");
            }
            List<Question> singles = questionMapper.selectQuestionsByType(exam.getBankId(), null, "SINGLE", exam.getSingleCount());
            List<Question> multis = questionMapper.selectQuestionsByType(exam.getBankId(), null, "MULTI", exam.getMultiCount());
            List<Question> judges = questionMapper.selectQuestionsByType(exam.getBankId(), null, "JUDGE", exam.getJudgeCount());
            List<Question> questions = new ArrayList<>();
            questions.addAll(singles);
            questions.addAll(multis);
            questions.addAll(judges);
            if (questions.isEmpty()) {
                throw new ServiceException("题库暂无可用题目");
            }
            sheet.setQuestionCount(questions.size());
            answerSheetMapper.insertSheet(sheet);

            int seq = 1;
            for (Question q : questions) {
                AnswerSheetItem item = new AnswerSheetItem();
                item.setAnswerSheetId(sheet.getId());
                item.setQuestionId(q.getId());
                item.setSeq(seq++);
                item.setQtype(q.getQtype());
                item.setIsSubjective(0);
                answerSheetMapper.insertItem(item);
            }

            List<Map<String, Object>> questionList = new ArrayList<>();
            for (Question q : questions) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", q.getId());
                m.put("qtype", q.getQtype());
                m.put("stem", q.getStem());
                m.put("optionsJson", q.getOptionsJson());
                m.put("score", getScoreByQtype(exam, q.getQtype()));
                m.put("isSubjective", 0);
                questionList.add(m);
            }
            result.put("sheetId", sheet.getId());
            result.put("subjectContent", null);
            result.put("subjectAttachment", null);
            result.put("questions", questionList);
        } else {
            // 实操考核：返回题干 + 附件，不抽题
            if (exam.getSubjectContent() == null || exam.getSubjectContent().trim().isEmpty()) {
                throw new ServiceException("实操考核题干未配置");
            }
            sheet.setQuestionCount(0);
            answerSheetMapper.insertSheet(sheet);

            result.put("sheetId", sheet.getId());
            result.put("subjectContent", exam.getSubjectContent());
            result.put("subjectAttachment", exam.getSubjectAttachment());
            result.put("questions", Collections.emptyList());
        }

        return result;
    }

    @Override
    public Map<String, Object> submit(Long sheetId, List<Map<String, String>> answers) {
        assertPreTrainee();
        Long userId = SecurityUtils.getUserId();
        AnswerSheet sheet = answerSheetMapper.selectSheetById(sheetId);
        if (sheet == null || !sheet.getUserId().equals(userId)) {
            throw new ServiceException("答卷不存在或无权操作");
        }
        if (!"IN_PROGRESS".equals(sheet.getStatus())) {
            throw new ServiceException("该答卷已提交，不能重复交卷");
        }

        Exam exam = examMapper.selectExamById(sheet.getExamId(), null);
        String type = exam == null ? "THEORY" : (exam.getExamType() == null ? "THEORY" : exam.getExamType());

        AnswerSheet updSheet = new AnswerSheet();
        updSheet.setId(sheetId);
        updSheet.setSubmitTime(new Date());

        if ("THEORY".equals(type)) {
            // 理论考核：自动判分，直接出分
            List<AnswerSheetItem> items = answerSheetMapper.selectItemsBySheetId(sheetId);
            List<Long> objIds = new ArrayList<>();
            for (AnswerSheetItem item : items) {
                objIds.add(item.getQuestionId());
            }
            Map<Long, String> answerMap = new HashMap<>();
            if (!objIds.isEmpty()) {
                List<Question> questions = questionMapper.selectQuestionsByIds(objIds, null);
                for (Question q : questions) {
                    answerMap.put(q.getId(), q.getAnswer());
                }
            }
            Map<Long, String> userAnswerMap = new HashMap<>();
            if (answers != null) {
                for (Map<String, String> a : answers) {
                    try {
                        userAnswerMap.put(Long.valueOf(a.get("questionId")), a.get("userAnswer"));
                    } catch (Exception ignore) {}
                }
            }

            BigDecimal aiScore = BigDecimal.ZERO;
            for (AnswerSheetItem item : items) {
                String userAnswer = userAnswerMap.get(item.getQuestionId());
                String normalized = normalizeAnswer(userAnswer);
                String correctAnswer = normalizeAnswer(answerMap.get(item.getQuestionId()));
                boolean correct = !correctAnswer.isEmpty() && normalized.equals(correctAnswer);
                BigDecimal score = correct ? getScoreByQtype(exam, item.getQtype()) : BigDecimal.ZERO;
                AnswerSheetItem upd = new AnswerSheetItem();
                upd.setId(item.getId());
                upd.setUserAnswer(normalized);
                upd.setIsCorrect(correct ? 1 : 0);
                upd.setAiScore(score);
                if (correct) aiScore = aiScore.add(score);
                answerSheetMapper.updateItem(upd);
            }

            int pass = aiScore.compareTo(exam.getPassLine()) >= 0 ? 1 : 0;
            updSheet.setStatus("PUBLISHED");
            updSheet.setAiScore(aiScore);
            updSheet.setFinalScore(aiScore);
            updSheet.setPassFlag(pass);
            updSheet.setSubmitType("MANUAL");
        } else {
            // 实操考核：保存作答文件路径，待管理员批改
            String filePath = null;
            if (answers != null && !answers.isEmpty()) {
                filePath = answers.get(0).get("userAnswer"); // 实操考核只有一个文件路径
            }
            updSheet.setStatus("SUBMITTED");
            updSheet.setSubmitType("MANUAL");
            updSheet.setSubjectAnswer(filePath);

            // exam 变待批改
            if (exam != null && "PUBLISHED".equals(exam.getStatus())) {
                Exam e = new Exam();
                e.setId(exam.getId());
                e.setStatus("GRADING");
                examMapper.updateExam(e);
            }
        }

        answerSheetMapper.updateSheet(updSheet);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("submitted", true);
        return result;
    }

    @Override
    public List<Map<String, Object>> myExamList(String examMode) {
        Long userId = SecurityUtils.getUserId();
        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null) {
            throw new ServiceException("当前账号未配置部门，无法参加考核");
        }
        Exam query = new Exam();
        query.setScopeDeptId(deptId);
        if (examMode != null && !examMode.isEmpty()) {
            query.setExamMode(examMode);
        }
        List<Exam> exams = examMapper.selectExamList(query);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Exam exam : exams) {
            if ("DRAFT".equals(exam.getStatus())) {
                continue;
            }
            // 指定人员过滤：该场次有指定名单且本人不在名单内 → 不展示
            java.util.List<Long> assigned = examRuleMapper.selectParticipantUserIds(exam.getId());
            if (!assigned.isEmpty() && !assigned.contains(userId)) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("examId", exam.getId());
            m.put("examName", exam.getExamName());
            m.put("examType", exam.getExamType());
            m.put("status", exam.getStatus());
            m.put("passLine", exam.getPassLine());
            m.put("questionCount", exam.getQuestionCount());
            m.put("duration", exam.getDuration());
            m.put("startTime", exam.getStartTime());
            m.put("endTime", exam.getEndTime());
            m.put("publishedAt", exam.getPublishedAt());
            // 指定人员：名单为空 = 本部门全体；非空时仅名单内可见（跨部门一律不可见）
            boolean assignedOnly = !examRuleMapper.selectParticipantUserIds(exam.getId()).isEmpty();
            if (assignedOnly) {
                m.put("assigned", true);
            } else {
                m.put("assigned", false);
            }
            AnswerSheet sheet = answerSheetMapper.selectByExamAndUser(exam.getId(), userId);
            if (sheet != null) {
                Map<String, Object> sm = new LinkedHashMap<>();
                sm.put("sheetId", sheet.getId());
                sm.put("status", sheet.getStatus());
                sm.put("aiScore", sheet.getAiScore());
                sm.put("manualScore", sheet.getManualScore());
                sm.put("finalScore", sheet.getFinalScore());
                sm.put("passFlag", sheet.getPassFlag());
                m.put("sheet", sm);
            }
            list.add(m);
        }
        return list;
    }

    @Override
    public List<Map<String, Object>> gradingList(Long examId) {
        Exam exam = getAccessibleExam(examId);
        return answerSheetMapper.selectGradingList(examId, exam.getDeptId());
    }

    @Override
    public Map<String, Object> sheetDetail(Long sheetId) {
        AnswerSheet sheet = getAccessibleSheet(sheetId);
        List<AnswerSheetItem> items = answerSheetMapper.selectItemsBySheetId(sheetId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sheet", sheet);
        result.put("items", items);
        return result;
    }

    @Override
    public int grade(Long sheetId, List<AnswerSheetItem> items) {
        Long userId = SecurityUtils.getUserId();
        AnswerSheet sheet = getAccessibleSheet(sheetId);
        if ("PUBLISHED".equals(sheet.getStatus())) {
            throw new ServiceException("成绩已发布，不能再批改");
        }
        BigDecimal manualScore = BigDecimal.ZERO;
        Set<Long> sheetItemIds = new HashSet<>();
        for (AnswerSheetItem sheetItem : answerSheetMapper.selectItemsBySheetId(sheetId)) {
            sheetItemIds.add(sheetItem.getId());
        }
        if (items != null) {
            for (AnswerSheetItem it : items) {
                if (it.getId() == null) {
                    continue;
                }
                if (!sheetItemIds.contains(it.getId())) {
                    throw new ServiceException("答题明细不存在或不属于当前答卷");
                }
                AnswerSheetItem upd = new AnswerSheetItem();
                upd.setId(it.getId());
                upd.setManualScore(it.getManualScore());
                upd.setManualComment(it.getManualComment());
                upd.setManualUserId(userId);
                upd.setManualTime(new Date());
                answerSheetMapper.updateItem(upd);
                if (it.getManualScore() != null) {
                    manualScore = manualScore.add(it.getManualScore());
                }
            }
        }
        AnswerSheet upd = new AnswerSheet();
        upd.setId(sheetId);
        upd.setStatus("SCORING");
        upd.setManualScore(manualScore);
        answerSheetMapper.updateSheet(upd);
        return 1;
    }

    @Override
    public int gradePractically(Long sheetId, BigDecimal manualScore, String manualComment) {
        AnswerSheet sheet = getAccessibleSheet(sheetId);
        if ("PUBLISHED".equals(sheet.getStatus())) {
            throw new ServiceException("成绩已发布，不能再批改");
        }
        AnswerSheet upd = new AnswerSheet();
        upd.setId(sheetId);
        upd.setStatus("SCORING");
        upd.setManualScore(manualScore == null ? BigDecimal.ZERO : manualScore);
        upd.setManualComment(manualComment);
        answerSheetMapper.updateSheet(upd);
        return 1;
    }

    @Override
    public int publishResult(Long examId, boolean disableExam, boolean publishUnanswered) {
        Exam exam = getAccessibleExam(examId);
        List<AnswerSheet> sheets = answerSheetMapper.selectSheetListByExam(examId);
        int count = 0;
        // 已作答：算总分并发布
        for (AnswerSheet sheet : sheets) {
            if ("SUBMITTED".equals(sheet.getStatus()) || "SCORING".equals(sheet.getStatus())) {
                BigDecimal ai = sheet.getAiScore() == null ? BigDecimal.ZERO : sheet.getAiScore();
                BigDecimal manual = sheet.getManualScore() == null ? BigDecimal.ZERO : sheet.getManualScore();
                BigDecimal finalScore = ai.add(manual);
                int pass = finalScore.compareTo(exam.getPassLine()) >= 0 ? 1 : 0;
                AnswerSheet upd = new AnswerSheet();
                upd.setId(sheet.getId());
                upd.setStatus("PUBLISHED");
                upd.setFinalScore(finalScore);
                upd.setPassFlag(pass);
                answerSheetMapper.updateSheet(upd);
                count++;
            }
        }
        // 未作答：可选发布为0分
        if (publishUnanswered) {
            Long deptId = exam.getDeptId();
            List<Map<String, Object>> grading = answerSheetMapper.selectGradingList(examId, deptId);
            for (Map<String, Object> row : grading) {
                if (row.get("sheetId") == null) {
                    Long userId = toLong(row.get("userId"));
                    if (userId == null) {
                        continue;
                    }
                    AnswerSheet zero = new AnswerSheet();
                    zero.setExamId(examId);
                    zero.setUserId(userId);
                    zero.setSheetType(exam.getExamType());
                    zero.setStatus("PUBLISHED");
                    zero.setQuestionCount(0);
                    zero.setAiScore(BigDecimal.ZERO);
                    zero.setManualScore(BigDecimal.ZERO);
                    zero.setFinalScore(BigDecimal.ZERO);
                    zero.setPassFlag(0);
                    zero.setRetakeSeq(1);
                    zero.setVersion(0);
                    zero.setSubmitTime(new Date());
                    zero.setCreateTime(new Date());
                    answerSheetMapper.insertSheet(zero);
                    count++;
                }
            }
        }
        if (disableExam) {
            Exam e = new Exam();
            e.setId(examId);
            e.setStatus("DISABLED");
            examMapper.updateExam(e);
        }
        return count;
    }

    private AnswerSheet getAccessibleSheet(Long sheetId) {
        AnswerSheet sheet = answerSheetMapper.selectSheetById(sheetId);
        if (sheet == null) {
            throw new ServiceException("答卷不存在");
        }
        getAccessibleExam(sheet.getExamId());
        return sheet;
    }

    private Exam getAccessibleExam(Long examId) {
        if (examId == null) {
            throw new ServiceException("考核ID不能为空");
        }
        Exam exam = examMapper.selectExamById(examId, currentManagerDeptScope());
        if (exam == null) {
            throw new ServiceException("考核不存在或无权操作");
        }
        return exam;
    }

    private Long currentManagerDeptScope() {
        if (isGlobalManager()) {
            return null;
        }
        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null) {
            throw new ServiceException("当前账号未配置部门，无法管理考核");
        }
        return deptId;
    }

    private boolean isGlobalManager() {
        return SecurityUtils.hasRole("SUPER_ADMIN") || SecurityUtils.isAdmin(SecurityUtils.getUserId());
    }

    private Long toLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Long) {
            return (Long) v;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        try {
            return Long.valueOf(v.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private void assertPreTrainee() {
        String userStatus = SecurityUtils.getLoginUser().getUser().getUserStatus();
        if (!"PRE_TRAINEE".equals(userStatus)) {
            throw new ServiceException("当前账号已不处于预备实习阶段，不能参加考核");
        }
    }

    private BigDecimal getScoreByQtype(Exam exam, String qtype) {
        if (exam == null) {
            return BigDecimal.ZERO;
        }
        if ("SINGLE".equals(qtype)) {
            return exam.getSingleScore() == null ? BigDecimal.ZERO : exam.getSingleScore();
        }
        if ("MULTI".equals(qtype)) {
            return exam.getMultiScore() == null ? BigDecimal.ZERO : exam.getMultiScore();
        }
        if ("JUDGE".equals(qtype)) {
            return exam.getJudgeScore() == null ? BigDecimal.ZERO : exam.getJudgeScore();
        }
        return BigDecimal.ZERO;
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
}
