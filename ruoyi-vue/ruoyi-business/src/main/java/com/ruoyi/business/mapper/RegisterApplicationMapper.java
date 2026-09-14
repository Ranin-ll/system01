package com.ruoyi.business.mapper;

import com.ruoyi.business.domain.RegisterApplication;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 注册申请Mapper接口
 *
 * @author ruoyi
 */
@Mapper
public interface RegisterApplicationMapper extends BaseMapper<RegisterApplication> {

    @Select("SELECT COUNT(1) FROM register_application WHERE login_account = #{loginAccount} AND status <> 'PASSED'")
    Long countActiveByLoginAccount(@Param("loginAccount") String loginAccount);

    @Insert("INSERT INTO register_application (application_no, user_id, real_name, id_card, login_account, position_id, dept_id, "
            + "expected_entry_date, status, reject_reason, audit_count, create_time, update_time) "
            + "VALUES (#{applicationNo}, #{userId}, #{realName}, #{idCard}, #{loginAccount}, #{positionId}, #{deptId}, "
            + "#{expectedEntryDate}, #{status}, #{rejectReason}, #{auditCount}, #{createTime}, #{updateTime})")
    @org.apache.ibatis.annotations.Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertApplication(RegisterApplication application);

    /**
     * 查询注册申请列表（带部门范围过滤）
     *
     * @param registerApplication 查询条件
     * @param deptId              部门范围（可为null表示查询所有）
     * @return 申请列表
     */
    List<RegisterApplication> selectRegisterList(@Param("query") RegisterApplication registerApplication,
                                                  @Param("deptId") Long deptId);

    /**
     * 根据用户ID查询申请记录
     */
    List<RegisterApplication> selectRegisterListByUserId(@Param("userId") Long userId);

    /**
     * 根据申请编号查询（沿用原编号重新提交时使用）
     */
    RegisterApplication selectByApplicationNo(@Param("applicationNo") String applicationNo);

    /** 根据申请ID查询完整审核详情 */
    RegisterApplication selectRegisterById(@Param("id") Long id);

    /** 更新注册申请状态、驳回原因和时间 */
    int updateApplication(RegisterApplication application);

    /** 按数据范围统计注册申请 */
    Map<String, Object> selectRegisterSummary(@Param("deptId") Long deptId);
}
