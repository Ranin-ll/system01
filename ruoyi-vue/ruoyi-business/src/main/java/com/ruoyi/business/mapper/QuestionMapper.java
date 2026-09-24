package com.ruoyi.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.business.domain.Question;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 题目Mapper
 *
 * ★ 2026-09-23 起：题目直接按部门（dept_id）归属，不再关联「题库」。
 * 一个部门 = 一个理论题池；理论考试按知识点（knowledge_point）从中抽题。
 */
@Mapper
public interface QuestionMapper extends BaseMapper<Question> {

    /** 查询题目列表（按部门数据范围过滤；超管可按 deptId 再筛） */
    List<Question> selectQuestionList(Question question);

    /** 按数据范围查询单个题目。scopeDeptId 为空表示全局范围。 */
    Question selectQuestionById(@Param("id") Long id, @Param("scopeDeptId") Long scopeDeptId);

    /** 按部门题池随机抽取客观题（不限题型）。 */
    List<Question> selectQuestionsForExam(@Param("deptId") Long deptId,
                                          @Param("scopeDeptId") Long scopeDeptId,
                                          @Param("limit") Integer limit);

    /** 按部门题池 + 题型随机抽取客观题（SINGLE/MULTI/JUDGE）。 */
    List<Question> selectQuestionsByType(@Param("deptId") Long deptId,
                                         @Param("scopeDeptId") Long scopeDeptId,
                                         @Param("qtype") String qtype,
                                         @Param("limit") Integer limit);

    /** 查询部门题池下的实操题（主观题，实习生需上传文件作答）。limit 为抽取数量，传 0 或 null 表示全部。 */
    List<Question> selectSubjectQuestions(@Param("deptId") Long deptId,
                                          @Param("scopeDeptId") Long scopeDeptId,
                                          @Param("limit") Integer limit);

    /** 按ID集合查询题目（判分用，返回含答案的题目）。 */
    List<Question> selectQuestionsByIds(@Param("ids") List<Long> ids,
                                        @Param("scopeDeptId") Long scopeDeptId);

    /** 新增题目 */
    int insertQuestion(Question question);

    /** 修改题目 */
    int updateQuestion(Question question);

    /** 逻辑删除题目 */
    int deleteQuestionById(@Param("id") Long id);
}
