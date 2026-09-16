package com.ruoyi.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.business.domain.Exam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 考核Mapper
 */
@Mapper
public interface ExamMapper extends BaseMapper<Exam> {

    List<Exam> selectExamList(Exam exam);

    Exam selectExamById(@Param("id") Long id, @Param("scopeDeptId") Long scopeDeptId);

    int insertExam(Exam exam);

    int updateExam(Exam exam);

    int deleteExamById(@Param("id") Long id);
}