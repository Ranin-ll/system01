package com.ruoyi.business.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 超管「催办与通知记录」看板：通知/催办的**留痕与送达情况**。
 *
 * <p><b>数据源</b>：`notice`（本体）+ `notice_target`（送达）+ `notice_read`（已读回执）。
 * 注意 `notification` / `operate_log` 两张表**是空的**，不要拿它们当数据源。</p>
 *
 * <p><b>为什么不复用 `/business/message/sent`</b>：那个接口以「当前登录用户 = 发送者」过滤，
 * 超管只能看到自己发的（实测只有 4 条）；本看板要的是**全量留痕**（含部门管理员发的、定时任务发的），
 * 所以另起只读接口。收件人明细仍复用既有的 `/business/message/{id}/recipients`（实测超管可读他人发的通知）。</p>
 */
@Mapper
public interface NoticeRecordMapper {

    /**
     * 记录列表（分页）：带发送者、受众范围、送达数、已读数、涉及部门。
     * 支持筛选：类型 / 业务类型 / 发送者 / 受众部门 / 关键词 / 时间区间。
     */
    @Select("<script>"
            + "SELECT n.id AS id, n.title AS title, n.content AS content,"
            + "       n.msg_type AS msgType, n.biz_type AS bizType, n.biz_id AS bizId,"
            + "       n.scope_type AS scopeType, n.scope_id AS scopeId, n.link_url AS linkUrl,"
            + "       n.publisher_id AS publisherId,"
            + "       COALESCE(u.nick_name, '系统 / 定时任务') AS publisherName,"
            + "       u.user_name AS publisherAccount,"
            + "       n.publish_time AS publishTime, n.create_time AS createTime, n.status AS status,"
            + "       (SELECT COUNT(*) FROM notice_target t WHERE t.notice_id = n.id) AS targetCount,"
            // ⚠️ 已读数必须锚定在 notice_target 上：notice_read 里还包含「全部已读」产生的行
            //（对 ALL/DEPT 范围的通知也会插回执），直接 COUNT(notice_read) 会得出 >100% 的已读率。
            + "       (SELECT COUNT(*) FROM notice_target t4"
            + "          JOIN notice_read rd ON rd.notice_id = t4.notice_id AND rd.user_id = t4.user_id"
            + "         WHERE t4.notice_id = n.id) AS readCount,"
            + "       (SELECT GROUP_CONCAT(DISTINCT d.dept_name ORDER BY d.dept_id SEPARATOR '、')"
            + "          FROM notice_target t2"
            + "          JOIN sys_user u2 ON u2.user_id = t2.user_id"
            + "          JOIN sys_dept d ON d.dept_id = u2.dept_id AND d.del_flag = '0'"
            + "         WHERE t2.notice_id = n.id) AS targetDepts"
            + "  FROM notice n"
            + "  LEFT JOIN sys_user u ON u.user_id = n.publisher_id"
            + " WHERE 1 = 1"
            + " <if test=\"msgType != null and msgType != ''\">AND n.msg_type = #{msgType}</if>"
            + " <if test=\"bizType != null and bizType != ''\">AND n.biz_type = #{bizType}</if>"
            // 「系统 / 定时任务」发出的：publisher_id 为 NULL，用 `= ?` 永远匹配不到，所以要单独一个开关
            + " <if test=\"systemPublished != null and systemPublished\">AND n.publisher_id IS NULL</if>"
            + " <if test=\"publisherId != null\">AND n.publisher_id = #{publisherId}</if>"
            + " <if test=\"deptId != null\">"
            + "   AND EXISTS (SELECT 1 FROM notice_target t3 JOIN sys_user u3 ON u3.user_id = t3.user_id"
            + "                WHERE t3.notice_id = n.id AND u3.dept_id = #{deptId})"
            + " </if>"
            + " <if test=\"keyword != null and keyword != ''\">"
            + "   AND (n.title LIKE CONCAT('%', #{keyword}, '%') OR n.content LIKE CONCAT('%', #{keyword}, '%'))"
            + " </if>"
            + " <if test=\"beginTime != null and beginTime != ''\">AND n.publish_time &gt;= CONCAT(#{beginTime}, ' 00:00:00')</if>"
            + " <if test=\"endTime != null and endTime != ''\">AND n.publish_time &lt;= CONCAT(#{endTime}, ' 23:59:59')</if>"
            + " ORDER BY n.publish_time DESC, n.id DESC"
            + "</script>")
    List<Map<String, Object>> selectRecords(@Param("msgType") String msgType,
                                            @Param("bizType") String bizType,
                                            @Param("publisherId") Long publisherId,
                                            @Param("systemPublished") Boolean systemPublished,
                                            @Param("deptId") Long deptId,
                                            @Param("keyword") String keyword,
                                            @Param("beginTime") String beginTime,
                                            @Param("endTime") String endTime);

    /**
     * 看板主指标：总数 / 按类型 / 按受众范围 / 送达合计 / 已读合计。
     * 聚合一律用 `SUM(CASE WHEN ... THEN 1 ELSE 0 END)`，避免命中 0 行时 SUM(条件) 返回 NULL。
     */
    @Select("SELECT COUNT(*) AS total,"
            + " SUM(CASE WHEN n.msg_type = 'URGE'     THEN 1 ELSE 0 END) AS urgeCount,"
            + " SUM(CASE WHEN n.msg_type = 'TASK'     THEN 1 ELSE 0 END) AS taskCount,"
            + " SUM(CASE WHEN n.msg_type = 'NOTIFY'   THEN 1 ELSE 0 END) AS notifyCount,"
            + " SUM(CASE WHEN n.msg_type = 'ANNOUNCE' THEN 1 ELSE 0 END) AS announceCount,"
            + " SUM(CASE WHEN n.msg_type = 'SYSTEM'   THEN 1 ELSE 0 END) AS systemCount,"
            + " SUM(CASE WHEN n.scope_type = 'ALL'      THEN 1 ELSE 0 END) AS scopeAll,"
            + " SUM(CASE WHEN n.scope_type = 'DEPT'     THEN 1 ELSE 0 END) AS scopeDept,"
            + " SUM(CASE WHEN n.scope_type = 'POSITION' THEN 1 ELSE 0 END) AS scopePosition,"
            + " SUM(CASE WHEN n.scope_type = 'USER'     THEN 1 ELSE 0 END) AS scopeUser,"
            + " (SELECT COUNT(*) FROM notice_target) AS targetTotal,"
            // 同 selectRecords：已读人次 = 「既被送达、又确实读了」的组合，不是 notice_read 总行数
            + " (SELECT COUNT(*) FROM notice_target t5"
            + "     JOIN notice_read rd2 ON rd2.notice_id = t5.notice_id AND rd2.user_id = t5.user_id) AS readTotal,"
            + " SUM(CASE WHEN n.publisher_id IS NULL THEN 1 ELSE 0 END) AS systemPublished"
            + "  FROM notice n")
    Map<String, Object> selectRecordSummary();

    /** 按发送者分布（NULL = 系统 / 定时任务） */
    @Select("SELECT n.publisher_id AS publisherId,"
            + "       COALESCE(u.nick_name, '系统 / 定时任务') AS publisherName,"
            + "       COUNT(*) AS cnt"
            + "  FROM notice n LEFT JOIN sys_user u ON u.user_id = n.publisher_id"
            + " GROUP BY n.publisher_id, u.nick_name"
            + " ORDER BY cnt DESC")
    List<Map<String, Object>> selectPublisherCounts();

    /** 单条记录的已读明细（谁读了 / 谁还没读），用于弹窗 */
    @Select("SELECT t.user_id AS userId, u.nick_name AS nickName, u.user_name AS userName,"
            + "       d.dept_name AS deptName, rd.read_time AS readTime"
            + "  FROM notice_target t"
            + "  JOIN sys_user u ON u.user_id = t.user_id"
            + "  LEFT JOIN sys_dept d ON d.dept_id = u.dept_id AND d.del_flag = '0'"
            + "  LEFT JOIN notice_read rd ON rd.notice_id = t.notice_id AND rd.user_id = t.user_id"
            + " WHERE t.notice_id = #{noticeId}"
            + " ORDER BY (rd.read_time IS NULL), u.user_id")
    List<Map<String, Object>> selectReadDetail(@Param("noticeId") Long noticeId);
}
