package com.ruoyi.business.mapper;

import com.ruoyi.business.domain.PracticeRecordItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 模拟考核逐题明细 Mapper
 */
@Mapper
public interface PracticeRecordItemMapper {

    /** 批量新增逐题明细 */
    int insertBatch(@Param("list") List<PracticeRecordItem> list);

    /** 查询某条模拟记录的逐题明细（按题序升序） */
    List<PracticeRecordItem> selectByRecordId(@Param("recordId") Long recordId);
}
