package com.ruoyi.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.business.domain.PromotionApplication;
import com.ruoyi.business.domain.PromotionCandidate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 转正申请Mapper
 *
 * @author ruoyi
 */
@Mapper
public interface PromotionApplicationMapper extends BaseMapper<PromotionApplication> {

    int insertApplication(PromotionApplication application);

    /** 按 ID 查详情（带申请人账号/姓名/岗位/部门/审核人名） */
    PromotionApplication selectApplicationById(@Param("id") Long id);

    /** 查某人最新一条申请（撤回/重新提交判定用） */
    PromotionApplication selectLatestByUserId(@Param("userId") Long userId);

    /**
     * 申请列表（部门范围过滤 + 状态/关键字筛选），按提交时间倒序。
     *
     * @param q       查询条件（status / userName / realName）
     * @param deptId  数据范围；null = 超管查全部
     */
    List<PromotionApplication> selectApplicationList(@Param("q") PromotionApplication q,
                                                     @Param("deptId") Long deptId);

    /** 统计某状态的数量（部门范围）。 */
    int countByStatus(@Param("status") String status, @Param("deptId") Long deptId);

    int updateApplication(PromotionApplication application);

    /** 某人是否已有未结束（DEPT_PENDING）的申请。 >0 即占用。 */
    int countActiveByUserId(@Param("userId") Long userId);

    /**
     * 转正审核候选人：本部门在培实习生（持 PRE_TRAINEE / FORMAL_TRAINEE 角色）
     * 左连「最新一条转正申请」。
     *
     * <p>★ 只出身份与申请字段，<b>资格核对（学习率 / 考核通过次数 / 协议）在 Service 层补</b>
     * —— 那三项要复用学习域与考核域的既有口径，写在 SQL 里会形成第二套算法。</p>
     *
     * @param deptId 数据范围；null = 超管查全部
     */
    List<PromotionCandidate> selectCandidateRows(@Param("deptId") Long deptId);
}
