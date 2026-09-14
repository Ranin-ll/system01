package com.ruoyi.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.business.domain.AuditRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 审核记录Mapper
 *
 * @author ruoyi
 */
@Mapper
public interface AuditRecordMapper extends BaseMapper<AuditRecord> {

    @Insert("INSERT INTO audit_record (biz_type, biz_id, user_id, dept_id, action, from_status, to_status, reason, operator_id, create_time) "
            + "VALUES (#{bizType}, #{bizId}, #{userId}, #{deptId}, #{action}, #{fromStatus}, #{toStatus}, #{reason}, #{operatorId}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertAuditRecord(AuditRecord record);

    /**
     * 根据业务类型和单据ID查询审核历史
     */
    List<AuditRecord> selectByBiz(@Param("bizType") String bizType, @Param("bizId") Long bizId);
}
