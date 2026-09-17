package com.ruoyi.business.mapper;

import com.ruoyi.business.domain.ExamKnowledgeRule;
import com.ruoyi.business.domain.ExamParticipant;
import com.ruoyi.business.domain.Question;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 理论考试配置（知识分布 / 指定人员 / 知识点抽题）Mapper
 *
 * 说明：这三块都是「部门管理员配置 → 实习生端按配置消费」的链路，
 * 集中在一个 Mapper 里，避免把配置读写散到 ExamMapper / QuestionMapper。
 */
public interface ExamRuleMapper {

    // ---------- 知识分布 ----------

    /** 某考核已配置的知识分布（含题库可用题量） */
    List<ExamKnowledgeRule> selectKnowledgeRules(@Param("examId") Long examId);

    int deleteKnowledgeRules(@Param("examId") Long examId);

    int insertKnowledgeRules(@Param("list") List<ExamKnowledgeRule> list);

    /** 题库里已有的知识点及题量（供「从题库导入知识点」） */
    List<Map<String, Object>> selectBankKnowledgePoints(@Param("bankId") Long bankId);

    /** 按知识点 + 题型随机抽题（qtype 为 null 时不限题型） */
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

    // ---------- 配置读取 ----------

    /** 本部门已生效的模拟理论考核配置（exam_mode=PRACTICE + exam_type=THEORY + PUBLISHED） */
    Map<String, Object> selectActivePracticeConfig(@Param("deptId") Long deptId);
}
