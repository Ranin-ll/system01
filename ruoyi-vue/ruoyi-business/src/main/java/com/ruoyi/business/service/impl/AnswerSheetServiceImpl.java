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
        Long loginUserId = SecurityUtils.getUserId();
        // 指定人员校验放在部门校验之前：被点名的实习生可以跨部门参加，
        // 与实习生端列表（名单内的场次跨部门也可见）保持一致。
        java.util.List<Long> assigned = examRuleMapper.selectParticipantUserIds(examId);
        boolean named = !assigned.isEmpty() && assigned.contains(loginUserId);
        if (!assigned.isEmpty() && !named) {
            throw new ServiceException("本场考核为指定人员参加，你不在名单内");
        }
        Long deptId = SecurityUtils.getDeptId();
        // 未被点名时才要求部门一致（未指定人员的场次仍是"本部门在培实习生均可参加"）
        if (!named && (deptId == null || !deptId.equals(exam.getDeptId()))) {
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
        Long userId = loginUserId;
        AnswerSheet exist = answerSheetMapper.selectByExamAndUser(examId, userId);
        if (exist != null) {
            // 作答中（未交卷）的答卷：通常是实习生答题时离开/刷新页面遗留，
            // 不保留进度、不视为提交，本次重新开始时作废旧答卷后重开。
            if ("IN_PROGRESS".equals(exist.getStatus())) {
                answerSheetMapper.deleteItemsBySheetId(exist.getId());
                answerSheetMapper.deleteById(exist.getId());
            } else {
                throw new ServiceException("你已参加过该考核，不能重复参加");
            }
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
            // 理论考核：组卷抽题（优先多题库配置，未配置回退单库）
            List<Question> questions = pickTheoryQuestions(exam);
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
                // 题干与满分落快照：题目表可被管理员编辑删除，只靠实时 JOIN 会让旧答卷批阅时取不到值
                item.setStemSnapshot(q.getStem());
                item.setFullScore(getScoreByQtype(exam, q.getQtype()));
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
            // 实操考核：题目由管理员逐条填写（不从题库抽题）。
            // 进入考试即为每道题生成一条作答明细，实习生在题下上传自己的作答文件，
            // 交卷后由管理员逐题打分、系统求和 —— 因此明细里没有客观题判分逻辑。
            List<com.ruoyi.business.domain.ExamSubjectItem> subjectItems =
                    examRuleMapper.selectSubjectItems(examId);
            if (subjectItems == null || subjectItems.isEmpty()) {
                throw new ServiceException("该实操考核尚未配置题目，请联系管理员");
            }
            sheet.setQuestionCount(subjectItems.size());
            answerSheetMapper.insertSheet(sheet);

            List<Map<String, Object>> subjectList = new ArrayList<>();
            int seq = 1;
            for (com.ruoyi.business.domain.ExamSubjectItem si : subjectItems) {
                AnswerSheetItem item = new AnswerSheetItem();
                item.setAnswerSheetId(sheet.getId());
                item.setSubjectItemId(si.getId());
                item.setSeq(seq++);
                item.setQtype("SUBJECT");
                item.setIsSubjective(1);
                // 题干（=题目 title）与满分落快照：管理员改版题目会删旧行插新行，
                // 只靠实时 JOIN 会让旧答卷批阅时题干与满分变 NULL
                item.setStemSnapshot(si.getTitle());
                item.setFullScore(si.getScore());
                answerSheetMapper.insertItem(item);

                Map<String, Object> m = new LinkedHashMap<>();
                m.put("itemId", item.getId());
                m.put("subjectItemId", si.getId());
                m.put("seq", item.getSeq());
                m.put("title", si.getTitle());
                m.put("description", si.getDescription());
                m.put("score", si.getScore());
                m.put("referenceImages", si.getReferenceImages());
                m.put("attachments", si.getAttachmentsJson());
                subjectList.add(m);
            }
            result.put("sheetId", sheet.getId());
            result.put("subjectItems", subjectList);
            result.put("subjectContent", null);
            result.put("subjectAttachment", null);
            result.put("questions", Collections.emptyList());
        }

        // 计时信息：以**服务端时间**为准，前端按 remainingSeconds 倒计时，到点自动交卷。
        // duration <= 0 表示不限时（remainingSeconds 返回 0，前端不启动倒计时）。
        int duration = exam.getDuration() == null ? 0 : exam.getDuration();
        long remainSeconds = 0;
        if (duration > 0) {
            long elapsed = (System.currentTimeMillis() - sheet.getStartTime().getTime()) / 1000L;
            remainSeconds = Math.max(0, duration * 60L - elapsed);
            // 若考核设了截止时间，倒计时不得超过截止时间
            if (exam.getEndTime() != null) {
                long toEnd = (exam.getEndTime().getTime() - System.currentTimeMillis()) / 1000L;
                if (toEnd > 0 && toEnd < remainSeconds) {
                    remainSeconds = toEnd;
                }
            }
        }
        result.put("duration", duration);
        result.put("remainingSeconds", remainSeconds);
        result.put("serverTime", System.currentTimeMillis());

        return result;
    }

    /**
     * 理论考核组卷抽题。
     *
     * 优先多题库配置（exam_bank_rule）：按「题库 × 题型」配额，每个题库分别按
     * 单选/多选/判断 的配置数量在该库内随机抽取，各库互不干扰、跨库去重。
     * 未配置时回退历史单库逻辑（exam.bank_id + 题型数量）。
     */
    private List<Question> pickTheoryQuestions(Exam exam) {
        List<Question> questions = pickByBankRules(exam.getId());
        if (!questions.isEmpty()) {
            return questions;
        }
        // 历史单库链路
        if (exam.getBankId() == null) {
            throw new ServiceException("理论考核题库未配置");
        }
        questions.addAll(questionMapper.selectQuestionsByType(exam.getBankId(), null, "SINGLE", exam.getSingleCount()));
        questions.addAll(questionMapper.selectQuestionsByType(exam.getBankId(), null, "MULTI", exam.getMultiCount()));
        questions.addAll(questionMapper.selectQuestionsByType(exam.getBankId(), null, "JUDGE", exam.getJudgeCount()));
        return questions;
    }

    /**
     * 多题库组卷抽题（理论与实操共用同一套逻辑）：
     * 对每个题库分别按 单选/多选/判断 的配置数量在该库内随机取题，跨库去重。
     * 未配置组卷时返回空列表，由调用方决定回退方式。
     */
    private List<Question> pickByBankRules(Long examId) {
        List<Question> questions = new ArrayList<>();
        List<com.ruoyi.business.domain.ExamBankRule> bankRules = examRuleMapper.selectBankRules(examId);
        if (bankRules == null || bankRules.isEmpty()) {
            return questions;
        }
        List<Long> used = new ArrayList<>();
        for (com.ruoyi.business.domain.ExamBankRule rule : bankRules) {
            addPicked(questions, used, examRuleMapper.selectQuestionsByPoint(
                    rule.getBankId(), null, "SINGLE", new ArrayList<>(used), nz(rule.getSingleCount())));
            addPicked(questions, used, examRuleMapper.selectQuestionsByPoint(
                    rule.getBankId(), null, "MULTI", new ArrayList<>(used), nz(rule.getMultiCount())));
            addPicked(questions, used, examRuleMapper.selectQuestionsByPoint(
                    rule.getBankId(), null, "JUDGE", new ArrayList<>(used), nz(rule.getJudgeCount())));
        }
        return questions;
    }

    /** 合并抽到的题并跨库去重 */
    private void addPicked(List<Question> target, List<Long> used, List<Question> src) {
        if (src == null) {
            return;
        }
        for (Question q : src) {
            if (!used.contains(q.getId())) {
                used.add(q.getId());
                target.add(q);
            }
        }
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
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
                    if (a == null) {
                        continue;
                    }
                    // ★★ 2026-09-22 修 BUG：原来写的是 `Long.valueOf(a.get("questionId"))` +
                    //    `catch (Exception ignore) {}` —— 前端理论卷传的 questionId 是**数字**
                    //    （Jackson 解成 Integer），而 Long.valueOf 要求 String，编译期会插入
                    //    checkcast → 运行时 ClassCastException → **被 catch 静默吞掉**，
                    //    结果「答案全部丢失 → 全部判 0 分且显示未作答」（实操卷传的是字符串所以没暴露）。
                    //    这里一律走 toLong(Object)（不做 String 强转），并从源头不再吞异常。
                    Long qid = toLong(a.get("questionId"));
                    if (qid == null) {
                        continue;
                    }
                    userAnswerMap.put(qid, answerText(a.get("userAnswer")));
                }
            }

            BigDecimal aiScore = BigDecimal.ZERO;
            for (AnswerSheetItem item : items) {
                String userAnswer = userAnswerMap.get(item.getQuestionId());
                String normalized = normalizeAnswer(userAnswer);
                String correctAnswer = normalizeAnswer(answerMap.get(item.getQuestionId()));
                boolean correct = !correctAnswer.isEmpty() && normalized.equals(correctAnswer);
                // ★ 用**明细上的满分快照**判分，而不是 exam 的当前分值：
                //   建卷时已把 full_score 落进明细，若管理员在作答期间改了分值（或重修了 exam 记录），
                //   用 exam 当前值会导致「各题得分之和 ≠ 卷面满分口径」。快照缺失才回退到 exam。
                BigDecimal fullScore = item.getFullScore() != null
                        ? item.getFullScore() : getScoreByQtype(exam, item.getQtype());
                BigDecimal score = correct ? fullScore : BigDecimal.ZERO;
                AnswerSheetItem upd = new AnswerSheetItem();
                upd.setId(item.getId());
                upd.setUserAnswer(normalized);
                upd.setIsCorrect(correct ? 1 : 0);
                upd.setAiScore(score);
                if (correct) aiScore = aiScore.add(score);
                answerSheetMapper.updateItem(upd);
            }

            // 通过线缺失（历史脏数据）时按「未通过」处理：既不 NPE 打成 500，也不静默全员通过
            BigDecimal passLine = exam == null ? null : exam.getPassLine();
            int pass = (passLine != null && aiScore.compareTo(passLine) >= 0) ? 1 : 0;
            updSheet.setStatus("PUBLISHED");
            updSheet.setAiScore(aiScore);
            updSheet.setFinalScore(aiScore);
            updSheet.setPassFlag(pass);
            updSheet.setSubmitType("MANUAL");
        } else {
            // 实操考核：逐题收作答文件（每题一份），落进对应明细，等待管理员逐题打分。
            List<AnswerSheetItem> items = answerSheetMapper.selectItemsBySheetId(sheetId);
            Map<Long, String> answerMap = new HashMap<>();
            if (answers != null) {
                for (Map<String, String> a : answers) {
                    if (a == null || a.get("userAnswer") == null) {
                        continue;
                    }
                    // 实操明细以 subjectItemId 对齐（前端按题提交，questionId 里放的是 subjectItemId 字符串）。
                    // 同样走 toLong(Object) —— 不依赖「前端一定传字符串」这个隐含前提。
                    Long sid = toLong(a.get("questionId"));
                    if (sid == null) {
                        continue;
                    }
                    answerMap.put(sid, answerText(a.get("userAnswer")));
                }
            }
            List<AnswerSheetItem> toUpdate = new ArrayList<>();
            String firstFile = null;
            for (AnswerSheetItem item : items) {
                String path = item.getSubjectItemId() == null ? null : answerMap.get(item.getSubjectItemId());
                if (path == null || path.trim().isEmpty()) {
                    continue;
                }
                AnswerSheetItem upd = new AnswerSheetItem();
                upd.setId(item.getId());
                upd.setUserAnswer(path.trim());
                toUpdate.add(upd);
                if (firstFile == null) {
                    firstFile = path.trim();
                }
            }
            // 白卷（未上传任何作答文件）也允许交卷：明细仍在，管理员批阅时逐题判 0 分即可。
            if (!toUpdate.isEmpty()) {
                answerSheetMapper.updateItemAnswers(toUpdate);
            }

            updSheet.setStatus("SUBMITTED");
            updSheet.setSubmitType("MANUAL");
            // 兼容旧字段：留第一份文件路径，便于批改列表一眼看到入口
            updSheet.setSubjectAnswer(firstFile);

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
        List<Exam> deptExams = examMapper.selectExamList(query);
        // 「按人指定」的场次单独补一遍：名单里有本人就能看到，不受部门限制（跨部门指派也生效）
        List<Exam> assignedToMe = examMapper.selectAssignedExamList(userId, examMode);
        List<Exam> merged = new ArrayList<>();
        java.util.Set<Long> seen = new java.util.LinkedHashSet<>();
        if (deptExams != null) {
            for (Exam e : deptExams) {
                if (e != null && e.getId() != null && seen.add(e.getId())) {
                    merged.add(e);
                }
            }
        }
        if (assignedToMe != null) {
            for (Exam e : assignedToMe) {
                if (e != null && e.getId() != null && seen.add(e.getId())) {
                    merged.add(e);
                }
            }
        }
        // 合并后按创建时间倒序，保证跨部门补入的场次不会乱序
        merged.sort((a, b) -> {
            java.util.Date d1 = a.getCreateTime();
            java.util.Date d2 = b.getCreateTime();
            if (d1 == null && d2 == null) {
                return 0;
            }
            if (d1 == null) {
                return 1;
            }
            if (d2 == null) {
                return -1;
            }
            return d2.compareTo(d1);
        });

        List<Map<String, Object>> list = new ArrayList<>();
        // 过期判断以**服务端时间**为准下发（前端若拿客户端时钟自己算，机器时间不准就会误判按钮状态）
        Date now = new Date();
        for (Exam exam : merged) {
            if ("DRAFT".equals(exam.getStatus())) {
                continue;
            }
            // 名单只查一次：既用于"是否对本人生效"的过滤，也用于前端「指定人员」标记
            java.util.List<Long> assigned = examRuleMapper.selectParticipantUserIds(exam.getId());
            // 有指定名单但本人不在名单内 → 不展示
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
            // 已过截止时间：**只表示"不能再参加"**，不会把考核置为 DISABLED
            // （实操考核截止后管理员还要批改，自动停用会打断批改流程；停用只在发布成绩时手动勾选）
            m.put("expired", exam.getEndTime() != null && now.after(exam.getEndTime()));
            // 指定人员：名单为空 = 本部门全体可见；非空 = 仅名单内可见（含跨部门指派）
            m.put("assigned", !assigned.isEmpty());
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

    /**
     * 实习生查看本人答卷详情。
     *
     * 走独立的「本人」通道，而不是复用管理员的 sheetDetail：后者用
     * getAccessibleSheet → currentManagerDeptScope 做**部门管理范围**校验，
     * 实习生的账号没有这个范围，直接调用会被判成"无权操作"。
     * 这里只校验「这张答卷是不是本人的」，通过与否都能看；
     * 答卷未发布时抹掉正确答案，避免批改期间提前泄露答案。
     */
    @Override
    public Map<String, Object> mySheetDetail(Long sheetId) {
        Long userId = SecurityUtils.getUserId();
        AnswerSheet sheet = answerSheetMapper.selectSheetById(sheetId);
        if (sheet == null || sheet.getUserId() == null || !sheet.getUserId().equals(userId)) {
            throw new ServiceException("答卷不存在或无权查看");
        }
        List<AnswerSheetItem> items = answerSheetMapper.selectItemsBySheetId(sheetId);
        boolean published = "PUBLISHED".equals(sheet.getStatus());
        if (!published && items != null) {
            for (AnswerSheetItem it : items) {
                it.setAnswer(null);
            }
        }
        Exam exam = examMapper.selectExamById(sheet.getExamId(), null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sheet", sheet);
        result.put("examType", exam == null || exam.getExamType() == null ? "THEORY" : exam.getExamType());
        result.put("passLine", exam == null ? null : exam.getPassLine());
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
        if ("IN_PROGRESS".equals(sheet.getStatus())) {
            throw new ServiceException("该答卷尚未交卷，无法批改");
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
        if ("IN_PROGRESS".equals(sheet.getStatus())) {
            throw new ServiceException("该答卷尚未交卷，无法批改");
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
                // 通过线缺失时按「未通过」处理，避免 NPE 500，也避免静默全员通过
                BigDecimal passLine = exam.getPassLine();
                int pass = (passLine != null && finalScore.compareTo(passLine) >= 0) ? 1 : 0;
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
        } else if ("GRADING".equals(exam.getStatus())) {
            // 成绩已发布但不停用考核：把考核从「待批改」同步回「已发布」，
            // 避免考核列表一直显示待批改、而答卷却已锁定为已发布的矛盾状态。
            Exam e = new Exam();
            e.setId(examId);
            e.setStatus("PUBLISHED");
            examMapper.updateExam(e);
        }
        return count;
    }

    @Override
    public int reopenForGrading(Long sheetId) {
        AnswerSheet sheet = getAccessibleSheet(sheetId);
        // 仅实操答卷支持重新批改；理论卷交卷即自动出分，无需人工改分。
        Exam exam = examMapper.selectExamById(sheet.getExamId(), null);
        if (exam == null || !"PRACTICAL".equals(exam.getExamType())) {
            throw new ServiceException("仅实操考核支持重新批改");
        }
        if (!"PUBLISHED".equals(sheet.getStatus())) {
            throw new ServiceException("仅已发布的答卷可重新批改");
        }
        // 已发布成绩回退：清空总分/通过标记，状态置回批改中，让管理员重新逐题打分后再发布。
        answerSheetMapper.reopenSheet(sheetId);

        // 考核同步回「待批改」
        if ("PUBLISHED".equals(exam.getStatus()) || "DISABLED".equals(exam.getStatus())) {
            Exam e = new Exam();
            e.setId(exam.getId());
            e.setStatus("GRADING");
            examMapper.updateExam(e);
        }
        return 1;
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

    /**
     * 作答内容归一化：容忍前端传字符串 / 字符串数组（多选题若未自行 join，这里兜底排序拼串）。
     * ★ 与 toLong 一样接收 Object：避免对 JSON 反序列化出来的值做 String 强转而抛
     *   ClassCastException（多选若传成数组，强转必然失败）。
     */
    private String answerText(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof java.util.Collection) {
            List<String> list = new ArrayList<>();
            for (Object o : (java.util.Collection<?>) v) {
                if (o != null && !o.toString().trim().isEmpty()) {
                    list.add(o.toString().trim());
                }
            }
            Collections.sort(list);
            return String.join(",", list);
        }
        return v.toString();
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
