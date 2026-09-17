package com.ruoyi.business.mapper;

import com.ruoyi.business.domain.PracticeRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 模拟考核记录 Mapper
 */
@Mapper
public interface PracticeRecordMapper {

    /** 新增模拟考核记录 */
    int insertRecord(PracticeRecord record);

    /** 查询某实习生的模拟考核记录（按时间倒序） */
    List<PracticeRecord> selectMyRecords(@Param("userId") Long userId);

    /** 按ID查询单条模拟考核记录（带题库名称） */
    PracticeRecord selectRecordById(@Param("id") Long id);
}
