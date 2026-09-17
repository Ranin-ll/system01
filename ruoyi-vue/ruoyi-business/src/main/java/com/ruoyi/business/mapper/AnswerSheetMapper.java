package com.ruoyi.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.business.domain.AnswerSheet;
import com.ruoyi.business.domain.AnswerSheetItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 答卷Mapper
 */
@Mapper
public interface AnswerSheetMapper extends BaseMapper<AnswerSheet> {

    /** 查询某考核某人的答卷（唯一约束保证最多一条）。 */
    AnswerSheet selectByExamAndUser(@Param("examId") Long examId, @Param("userId") Long userId);

    AnswerSheet selectSheetById(@Param("id") Long id);

    /** 某考核的所有答卷。 */
    List<AnswerSheet> selectSheetListByExam(@Param("examId") Long examId);

    /** 批改列表：当前部门所有实习生 + 其在该考核的答卷（未答也返回，标记未作答）。 */
    List<Map<String, Object>> selectGradingList(@Param("examId") Long examId, @Param("deptId") Long deptId);

    int insertSheet(AnswerSheet sheet);

    int updateSheet(AnswerSheet sheet);

    /** 答卷明细（含题目题干/答案/附件，判分和批改用）。 */
    List<AnswerSheetItem> selectItemsBySheetId(@Param("sheetId") Long sheetId);

    int insertItem(AnswerSheetItem item);

    int updateItem(AnswerSheetItem item);
}
