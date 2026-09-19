package com.ruoyi.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.business.domain.Notice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 消息／通知 Mapper
 *
 * <p><b>★ 送达谓词只在这里写一次</b>（XML 里的 <code>&lt;sql id="visibleTo"&gt;</code>）：
 * 消息列表、未读数、未读分组、公告位、详情可见性 —— 五处全部 <code>&lt;include&gt;</code> 它。
 * 只要有一处各写各的，就会出现"铃铛显示 3、列表只有 2"这类对不上的 bug。</p>
 *
 * @author ruoyi
 */
@Mapper
public interface NoticeMapper extends BaseMapper<Notice> {

    /**
     * 我的消息列表（已排除自己发的；未读优先由 SQL 排序保证）
     */
    List<Notice> selectMyList(@Param("userId") Long userId,
                              @Param("msgType") String msgType,
                              @Param("readFlag") String readFlag,
                              @Param("keyword") String keyword);

    /**
     * 我的未读总数
     */
    int countUnread(@Param("userId") Long userId);

    /**
     * 我的未读按类型分组（铃铛下拉/页签角标用）
     */
    List<Map<String, Object>> countUnreadByType(@Param("userId") Long userId);

    /**
     * 工作台「公告位」：当前有效公告（置顶优先），最多 limit 条
     */
    List<Notice> selectAnnouncements(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 按 ID 取「我能看到的那一条」—— 非收件人访问会返回 null，
     * 上层统一提示"消息不存在"，不泄露"这条消息存在但不给你看"
     */
    Notice selectVisibleById(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 一键全部已读：直接 INSERT ... SELECT（一次 SQL 完成，不拉 id 回前端再批量插）
     * 唯一键 + ON DUPLICATE KEY 已读时间不变 → 幂等
     */
    int markAllRead(@Param("userId") Long userId, @Param("msgType") String msgType);

    // ------------------------- P1：发送方视角 -------------------------

    /**
     * 我发送过的通知，带「送达数 / 已读数」
     */
    List<Notice> selectSentList(@Param("publisherId") Long publisherId,
                                @Param("msgType") String msgType,
                                @Param("keyword") String keyword,
                                @Param("targetUserId") Long targetUserId);

    /**
     * 某条通知的收件人清单（含已读时间），未读排前 —— 用于「未读名单」
     */
    List<Map<String, Object>> selectRecipients(@Param("id") Long id);

    /**
     * 撤回：仅 PUBLISHED 可撤；已读记录保留（审计需要）
     */
    int revoke(@Param("id") Long id);

    /**
     * 发送前的「预计送达」人数估算（ALL / DEPT / POSITION 三档）。
     *
     * <p>★ 口径必须与 {@code NoticeMapper.xml} 的 {@code audienceOf} 一致 ——
     * 二者是同一套范围语义的两个方向。改了受众规则就要同步改这里，
     * 否则「预计送达 3 人」与发出后的「送达数 5 人」会打架。</p>
     *
     * <p>为什么不让前端算：{@code /system/user/list} 返回的 {@code roles} 是空数组，
     * 前端拿不到角色，「部门管理员岗」的人数就只能数出 0。</p>
     */
    int selectAudienceCount(@Param("scopeType") String scopeType, @Param("scopeId") Long scopeId);

    // ------------------------- P1：范围校验辅助 -------------------------

    /** 该岗位是否属于该部门（dept_position 生效绑定） */
    @org.apache.ibatis.annotations.Select(
            "SELECT COUNT(*) FROM dept_position WHERE dept_id = #{deptId} AND position_id = #{positionId} AND status = 1")
    int countDeptPosition(@Param("deptId") Long deptId, @Param("positionId") Long positionId);

    /** 传进来的接收人里，有多少个不在该部门（>0 即越权） */
    int countUsersOutsideDept(@Param("ids") java.util.List<Long> ids, @Param("deptId") Long deptId);
}
