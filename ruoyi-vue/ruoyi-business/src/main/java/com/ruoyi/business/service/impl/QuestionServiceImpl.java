package com.ruoyi.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.business.domain.AnswerSubmitBody;
import com.ruoyi.business.domain.Question;
import com.ruoyi.business.domain.QuestionImportResult;
import com.ruoyi.business.domain.QuestionImportRow;
import com.ruoyi.business.mapper.QuestionMapper;
import com.ruoyi.business.service.IQuestionService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 题目Service实现
 *
 * 数据范围控制：
 * - 超级管理员：查看全部题目，并可维护任一部门的题目。
 * - 部门管理员：查看并管理本部门的题目。
 * - 实习生：仅查看本部门题目，用于参与考核。
 *
 * ★ 2026-09-23 起：题目不再挂「题库」，直接按 dept_id 归属部门。
 * 一个部门 = 一个理论题池；理论考试按知识点从中抽题。
 */
@Service
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements IQuestionService {

    private final QuestionMapper questionMapper;
    private final ObjectMapper objectMapper;

    public QuestionServiceImpl(QuestionMapper questionMapper, ObjectMapper objectMapper) {
        this.questionMapper = questionMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Question> selectQuestionList(Question question) {
        question.setScopeDeptId(currentScopeDeptId());
        return questionMapper.selectQuestionList(question);
    }

    @Override
    public Question selectById(Long id) {
        Question question = questionMapper.selectQuestionById(id, currentScopeDeptId());
        if (question == null) {
            throw new ServiceException("题目不存在或无权访问");
        }
        return question;
    }

    @Override
    public int insertQuestion(Question question) {
        Long deptId = managerScopeDeptId();
        if (deptId == null) {
            // 超管：题目归属由入参指定（前端在超管端提供「所属部门」选择）
            deptId = question.getDeptId();
        }
        if (deptId == null) {
            throw new ServiceException("请选择题目所属部门");
        }
        question.setDeptId(deptId);
        question.setCreateBy(SecurityUtils.getUsername());
        question.setStatus(question.getStatus() == null ? 1 : question.getStatus());
        if ("SUBJECT".equals(question.getQtype())) {
            question.setIsSubjective(1);
        }
        if (question.getIsSubjective() == null) {
            question.setIsSubjective(0);
        }
        if (question.getQuestionNo() == null || question.getQuestionNo().isEmpty()) {
            question.setQuestionNo("Q" + System.currentTimeMillis());
        }
        if (question.getScore() == null) {
            question.setScore(java.math.BigDecimal.ZERO);
        }
        question.setVersion(1);
        question.setDeleted(0);
        question.setCreateTime(new Date());
        return questionMapper.insertQuestion(question);
    }

    @Override
    public int updateQuestion(Question question) {
        managerScopeDeptId();
        getAccessibleQuestion(question.getId());
        if ("SUBJECT".equals(question.getQtype())) {
            question.setIsSubjective(1);
        }
        // 不允许通过通用编辑接口篡改部门归属、删除标志或创建信息。
        question.setDeptId(null);
        question.setDeleted(null);
        question.setCreateBy(null);
        question.setCreateTime(null);
        return questionMapper.updateQuestion(question);
    }

    @Override
    public int deleteByIds(Long[] ids) {
        managerScopeDeptId();
        if (ids == null || ids.length == 0) {
            return 0;
        }
        int count = 0;
        for (Long id : ids) {
            getAccessibleQuestion(id);
            questionMapper.deleteQuestionById(id);
            count++;
        }
        return count;
    }

    @Override
    public QuestionImportResult importQuestions(Long deptId, List<QuestionImportRow> rows) {
        Long target = resolveDeptId(deptId);

        QuestionImportResult result = new QuestionImportResult();
        if (rows == null || rows.isEmpty()) {
            result.addError(0, "Excel 中没有可导入的数据");
            result.setTotal(0);
            return result;
        }
        result.setTotal(rows.size());

        String importBatch = "B" + System.currentTimeMillis();
        int rowNum = 0;
        for (QuestionImportRow row : rows) {
            rowNum++;
            try {
                Question question = convertRow(row, target, importBatch);
                question.setQuestionNo(importBatch + "-" + rowNum);
                questionMapper.insertQuestion(question);
                result.setSuccess(result.getSuccess() + 1);
            } catch (Exception e) {
                result.setFailed(result.getFailed() + 1);
                result.addError(rowNum, e.getMessage());
            }
        }
        return result;
    }

    @Override
    public List<QuestionImportRow> exportRows(Long deptId) {
        Long target = resolveDeptId(deptId);
        Question query = new Question();
        query.setDeptId(target);
        List<Question> list = selectQuestionList(query);
        List<QuestionImportRow> rows = new ArrayList<>();
        for (Question q : list) {
            rows.add(toImportRow(q));
        }
        return rows;
    }

    /**
     * 题目 → 导入行。
     *
     * <p>与 {@link #convertRow} 互为反向，<b>字面值必须严格对齐</b>，否则导出的文件不能再导入：
     * 题型输出「单选 / 多选 / 判断」（{@code mapQtype} 认这几种），
     * 难度输出「简单 / 中等 / 困难」（{@code mapDifficulty} 认这几种）。</p>
     */
    private QuestionImportRow toImportRow(Question q) {
        QuestionImportRow row = new QuestionImportRow();
        row.setQtypeText(qtypeText(q.getQtype()));
        row.setStem(q.getStem());
        List<Map<String, String>> opts = parseOptionsJson(q.getOptionsJson());
        row.setOptionA(optionContent(opts, "A"));
        row.setOptionB(optionContent(opts, "B"));
        row.setOptionC(optionContent(opts, "C"));
        row.setOptionD(optionContent(opts, "D"));
        row.setAnswer(q.getAnswer());
        row.setAnalysis(q.getAnalysis());
        row.setDifficultyText(difficultyText(q.getDifficulty()));
        row.setKnowledgePoint(q.getKnowledgePoint());
        return row;
    }

    private String qtypeText(String qtype) {
        if ("SINGLE".equals(qtype)) {
            return "单选";
        }
        if ("MULTI".equals(qtype)) {
            return "多选";
        }
        if ("JUDGE".equals(qtype)) {
            return "判断";
        }
        return qtype == null ? "" : qtype;
    }

    private String difficultyText(String difficulty) {
        if (difficulty == null) {
            return "";
        }
        String d = difficulty.trim().toUpperCase();
        if ("EASY".equals(d) || "SIMPLE".equals(d)) {
            return "简单";
        }
        if ("HARD".equals(d)) {
            return "困难";
        }
        return "中等";
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> parseOptionsJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String optionContent(List<Map<String, String>> opts, String key) {
        for (Map<String, String> o : opts) {
            if (o != null && key.equals(o.get("key"))) {
                String c = o.get("content");
                return c == null ? "" : c;
            }
        }
        return "";
    }

    @Override
    public List<Question> previewQuestions(Long deptId, Integer limit) {
        Long target = resolveDeptId(deptId);
        int size = (limit == null || limit <= 0) ? 10 : limit;
        List<Question> questions = questionMapper.selectQuestionsForExam(target, null, size);
        // 实操题（主观题）全部返回，供实习生上传文件作答
        List<Question> subjectQuestions = questionMapper.selectSubjectQuestions(target, null, null);
        questions.addAll(subjectQuestions);
        // 抽题结果不返回答案和解析，防止作弊；实操题附件保留。
        for (Question q : questions) {
            q.setAnswer(null);
            q.setAnalysis(null);
        }
        return questions;
    }

    @Override
    public Map<String, Object> submitAnswers(AnswerSubmitBody body) {
        if (body == null) {
            throw new ServiceException("提交内容不能为空");
        }
        Long scopeDeptId = currentScopeDeptId();

        List<AnswerSubmitBody.AnswerItem> answers = body.getAnswers();
        if (answers == null || answers.isEmpty()) {
            throw new ServiceException("未提交任何答案");
        }

        List<Long> ids = new ArrayList<>();
        Map<Long, String> userAnswerMap = new LinkedHashMap<>();
        for (AnswerSubmitBody.AnswerItem item : answers) {
            if (item.getQuestionId() != null) {
                ids.add(item.getQuestionId());
                userAnswerMap.put(item.getQuestionId(), item.getUserAnswer());
            }
        }

        List<Question> questions = questionMapper.selectQuestionsByIds(ids, scopeDeptId);
        if (questions.isEmpty()) {
            throw new ServiceException("未找到可判分的题目");
        }

        int correctCount = 0;
        BigDecimal totalScore = BigDecimal.ZERO;
        List<Map<String, Object>> details = new ArrayList<>();
        for (Question q : questions) {
            // 实操题（主观题）：不判分，记录上传文件，待人工评分。
            if (q.getIsSubjective() != null && q.getIsSubjective() == 1) {
                Map<String, Object> detail = new HashMap<>();
                detail.put("questionId", q.getId());
                detail.put("stem", q.getStem());
                detail.put("qtype", q.getQtype());
                detail.put("userAnswer", userAnswerMap.get(q.getId()));
                detail.put("correct", null);
                detail.put("analysis", "已提交文件，待人工评分");
                details.add(detail);
                continue;
            }
            String userAnswer = normalizeAnswer(userAnswerMap.get(q.getId()));
            String correctAnswer = normalizeAnswer(q.getAnswer());
            boolean correct = !correctAnswer.isEmpty() && userAnswer.equals(correctAnswer);
            if (correct) {
                correctCount++;
                BigDecimal score = q.getScore() == null ? BigDecimal.ZERO : q.getScore();
                totalScore = totalScore.add(score);
            }
            Map<String, Object> detail = new HashMap<>();
            detail.put("questionId", q.getId());
            detail.put("stem", q.getStem());
            detail.put("qtype", q.getQtype());
            detail.put("userAnswer", userAnswer);
            detail.put("correctAnswer", q.getAnswer());
            detail.put("correct", correct);
            detail.put("analysis", q.getAnalysis());
            detail.put("score", q.getScore());
            details.add(detail);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", questions.size());
        result.put("correct", correctCount);
        result.put("score", totalScore);
        result.put("details", details);
        return result;
    }

    /** 将导入行转换为题目实体。 */
    private Question convertRow(QuestionImportRow row, Long deptId, String importBatch) throws Exception {
        String stem = trim(row.getStem());
        if (stem.isEmpty()) {
            throw new Exception("题干不能为空");
        }
        String qtype = mapQtype(trim(row.getQtypeText()));
        if (qtype == null) {
            throw new Exception("题型必须为：单选 / 多选 / 判断");
        }

        String answer = normalizeAnswer(row.getAnswer());
        if (answer.isEmpty()) {
            throw new Exception("正确答案不能为空");
        }

        List<Map<String, String>> options = buildOptions(qtype, row, answer);

        Question question = new Question();
        question.setDeptId(deptId);
        question.setQtype(qtype);
        question.setStem(stem);
        question.setOptionsJson(objectMapper.writeValueAsString(options));
        question.setAnswer(answer);
        question.setAnalysis(trim(row.getAnalysis()));
        question.setScore(java.math.BigDecimal.ZERO);
        question.setDifficulty(mapDifficulty(trim(row.getDifficultyText())));
        question.setKnowledgePoint(trim(row.getKnowledgePoint()));
        question.setIsSubjective(0);
        question.setStatus(1);
        question.setImportBatch(importBatch);
        question.setVersion(1);
        question.setDeleted(0);
        question.setCreateBy(SecurityUtils.getUsername());
        question.setCreateTime(new Date());
        return question;
    }

    /** 根据题型和选项构建 options_json，并校验答案合法性。 */
    private List<Map<String, String>> buildOptions(String qtype, QuestionImportRow row, String answer) throws Exception {
        List<Map<String, String>> options = new ArrayList<>();
        if ("JUDGE".equals(qtype)) {
            if (!"A".equals(answer) && !"B".equals(answer)) {
                throw new Exception("判断题答案必须为 A（对）或 B（错）");
            }
            options.add(option("A", "对"));
            options.add(option("B", "错"));
            return options;
        }

        String[] keys = {"A", "B", "C", "D"};
        String[] contents = {trim(row.getOptionA()), trim(row.getOptionB()), trim(row.getOptionC()), trim(row.getOptionD())};
        for (int i = 0; i < keys.length; i++) {
            if (!contents[i].isEmpty()) {
                options.add(option(keys[i], contents[i]));
            }
        }
        if (options.size() < 2) {
            throw new Exception("客观题至少需要 2 个选项");
        }
        for (String ch : answer.split(",")) {
            if (!containsKey(options, ch)) {
                throw new Exception("答案 " + ch + " 不在已填写的选项中");
            }
        }
        if ("SINGLE".equals(qtype) && answer.contains(",")) {
            throw new Exception("单选题答案只能有一个");
        }
        return options;
    }

    private Map<String, String> option(String key, String content) {
        Map<String, String> m = new HashMap<>();
        m.put("key", key);
        m.put("content", content);
        return m;
    }

    private boolean containsKey(List<Map<String, String>> options, String key) {
        for (Map<String, String> o : options) {
            if (key.equals(o.get("key"))) {
                return true;
            }
        }
        return false;
    }

    private String mapQtype(String text) {
        if (text == null) {
            return null;
        }
        String t = text.trim().toUpperCase();
        if ("单选".equals(text) || "SINGLE".equals(t)) {
            return "SINGLE";
        }
        if ("多选".equals(text) || "MULTI".equals(t) || "MULTIPLE".equals(t)) {
            return "MULTI";
        }
        if ("判断".equals(text) || "JUDGE".equals(t)) {
            return "JUDGE";
        }
        return null;
    }

    private String mapDifficulty(String text) {
        if (text == null || text.isEmpty()) {
            return "MEDIUM";
        }
        String t = text.trim().toUpperCase();
        if ("简单".equals(text) || "EASY".equals(t)) {
            return "EASY";
        }
        if ("困难".equals(text) || "HARD".equals(t)) {
            return "HARD";
        }
        return "MEDIUM";
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

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }

    /** 超级管理员返回 null（看全部），部门账号返回本部门ID。 */
    private Long currentScopeDeptId() {
        if (isGlobalReadOnly()) {
            return null;
        }
        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null) {
            throw new ServiceException("当前账号未配置部门，无法访问题目数据");
        }
        return deptId;
    }

    private Long managerScopeDeptId() {
        if (isGlobalReadOnly()) {
            return null;
        }
        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null) {
            throw new ServiceException("当前账号未配置部门，无法管理题目");
        }
        return deptId;
    }

    private Question getAccessibleQuestion(Long id) {
        if (id == null) {
            throw new ServiceException("题目ID不能为空");
        }
        Question question = questionMapper.selectQuestionById(id, currentScopeDeptId());
        if (question == null) {
            throw new ServiceException("题目不存在或无权操作");
        }
        return question;
    }

    /**
     * 解析操作目标部门。
     *
     * <p>部门账号 / 实习生 → <b>强制本部门</b>（忽略入参，防越权，也让老前端不用改）；
     * 超级管理员 → 用入参指定的部门（不传则报错）。</p>
     */
    private Long resolveDeptId(Long deptId) {
        Long scope = currentScopeDeptId();
        if (scope != null) {
            return scope;
        }
        if (deptId == null) {
            throw new ServiceException("请指定部门");
        }
        return deptId;
    }

    private boolean isGlobalReadOnly() {
        return SecurityUtils.hasRole("SUPER_ADMIN") || SecurityUtils.isAdmin(SecurityUtils.getUserId());
    }
}
