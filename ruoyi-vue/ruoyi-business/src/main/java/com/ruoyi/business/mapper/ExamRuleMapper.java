package com.ruoyi.business.mapper;

import com.ruoyi.business.domain.ExamBankRule;
import com.ruoyi.business.domain.ExamKnowledgeRule;
import com.ruoyi.business.domain.ExamParticipant;
import com.ruoyi.business.domain.ExamSubjectItem;
import com.ruoyi.business.domain.Question;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 考核配置（实操题目 / 多题库组卷 / 知识分布 / 指定人员 / 抽题）Mapper
 *
 * 说明：这些都是「部门管理员配置 → 实习生端按配置消费」的链路，
 * 集中在一个 Mapper 里，避免把配置读写散到 ExamMapper / QuestionMapper。
 *
 * 理论：exam_bank_rule（多题库 × 题型配额，当前主用）> exam_knowledge_rule（单库知识点，历史兼容）
 * 实操：exam_subject_item（管理员逐条填写的题目清单，不抽题）
 */
public interface ExamRuleMapper {

    // ---------- 实操题目清单（实操考核专用，不抽题） ----------

    /** 某实操考核的题目清单（按题序升序） */
    List<ExamSubjectItem> selectSubjectItems(@Param("examId") Long examId);

    int deleteSubjectItems(@Param("examId") Long examId);

    int insertSubjectItems(@Param("list") List<ExamSubjectItem> list);

    // ---------- 多题库组卷配置（当前主用） ----------

    /** 某考核配置的题库及各自抽题量（含题库名与可用题量） */
    List<ExamBankRule> selectBankRules(@Param("examId") Long examId);

    int deleteBankRules(@Param("examId") Long examId);

    int insertBankRules(@Param("list") List<ExamBankRule> list);

    /** 某题库的可用题量（按题型统计，配置校验「抽题量 ≤ 可用量」用） */
    Map<String, Object> selectBankAvailable(@Param("bankId") Long bankId);

    // ---------- 知识分布（历史兼容，新配置不再写入） ----------

    /** 某考核已配置的知识分布（含题库可用题量） */
    List<ExamKnowledgeRule> selectKnowledgeRules(@Param("examId") Long examId);

    int deleteKnowledgeRules(@Param("examId") Long examId);

    int insertKnowledgeRules(@Param("list") List<ExamKnowledgeRule> list);

    /** 题库里已有的知识点及题量（供「从题库导入知识点」） */
    List<Map<String, Object>> selectBankKnowledgePoints(@Param("bankId") Long bankId);

    /**
     * 按题库 + 知识点 + 题型随机抽题。
     * point 为 null/空 时不限知识点（多题库组卷即用此形态：bankId + qtype 精确取题）。
     */
    List<Question> selectQuestionsByPoint(@Param("bankId") Long bankId,
                                          @Param("point") String point,
                                          @Param("qtype") String qtype,
                                          @Param("excludeIds") List<Long> excludeIds,
                                          @Param("limit") int limit);

    // ---------- 指定人员 ----------

    List<ExamParticipant> selectParticipants(@Param("examId") Long examId);

    /** 仅取 user_id，用于列表过滤与作答校验（避免每次拉全字段） */
    List<Long> selectParticipantUserIds(@Param("examId") Long examId);

    int deleteParticipants(@Param("examId") Long examId);

    int insertParticipants(@Param("list") List<ExamParticipant> list);

    /**
     * 在培实习生花名册（「指定人员」的可选名单）。
     * deptId 为 null 时不限部门（超级管理员）；否则只取该部门。
     * 只返回在培阶段（PRE_TRAINEE / FORMAL_TRAINEE）且账号正常的用户。
     */
    List<Map<String, Object>> selectInternOptions(@Param("deptId") Long deptId);

    // ---------- 配置读取 ----------

    /** 本部门已生效的模拟理论考核配置（exam_mode=PRACTICE + exam_type=THEORY + PUBLISHED） */
    Map<String, Object> selectActivePracticeConfig(@Param("deptId") Long deptId);
}
