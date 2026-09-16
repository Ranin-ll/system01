package com.ruoyi.business.service.impl;

import com.ruoyi.business.domain.PracticeRecord;
import com.ruoyi.business.domain.PracticeRecordItem;
import com.ruoyi.business.domain.Question;
import com.ruoyi.business.domain.QuestionBank;
import com.ruoyi.business.mapper.PracticeRecordItemMapper;
import com.ruoyi.business.mapper.PracticeRecordMapper;
import com.ruoyi.business.mapper.QuestionBankMapper;
import com.ruoyi.business.mapper.QuestionMapper;
import com.ruoyi.business.service.IPracticeService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
 * 规则：从本部门模拟题库随机抽 10 道客观题，每题 1 分，交卷自动判分并记录，
 * 成绩仅供实习生本人查询，管理员不可见。
 *
 * 交卷时同时写入逐题明细（practice_record_item），供本人回看题目、作答与正确答案。
 */
@Service
public class PracticeServiceImpl implements IPracticeService {

    private final QuestionBankMapper questionBankMapper;
    private final QuestionMapper questionMapper;
    private final PracticeRecordMapper practiceRecordMapper;
    private final PracticeRecordItemMapper practiceRecordItemMapper;

    public PracticeServiceImpl(QuestionBankMapper questionBankMapper,
                               QuestionMapper questionMapper,
                               PracticeRecordMapper practiceRecordMapper,
                               PracticeRecordItemMapper practiceRecordItemMapper) {
        this.questionBankMapper = questionBankMapper;
        this.questionMapper = questionMapper;
        this.practiceRecordMapper = practiceRecordMapper;
        this.practiceRecordItemMapper = practiceRecordItemMapper;
    }

    @Override
    public Map<String, Object> startPractice() {
        assertPreTrainee();
        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null) {
            throw new ServiceException("当前账号未配置部门，无法进行模拟考核");
        }
        QuestionBank bank = questionBankMapper.selectPracticeBankByDept(deptId);
        if (bank == null) {
            throw new ServiceException("本部门暂无模拟题库，请联系管理员");
        }
        List<Question> questions = questionMapper.selectQuestionsForExam(bank.getId(), null, 10);
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
            list.add(m);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("bankId", bank.getId());
        result.put("bankName", bank.getBankName());
        result.put("questions", list);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submitPractice(Long bankId, List<Map<String, Object>> answers) {
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
        int correct = 0;
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
        record.setDeptId(deptId);
        record.setTotalCount(total);
        record.setCorrectCount(correct);
        record.setScore(correct);
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
        result.put("score", correct);
        result.put("details", details);
        return result;
    }

    @Override
    public List<PracticeRecord> myRecords() {
        return practiceRecordMapper.selectMyRecords(SecurityUtils.getUserId());
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
