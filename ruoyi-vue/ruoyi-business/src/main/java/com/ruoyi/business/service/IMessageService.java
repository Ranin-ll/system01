package com.ruoyi.business.service;

import com.ruoyi.business.domain.Notice;

import java.util.List;
import java.util.Map;

/**
 * 消息中心 Service（P0：只做「收」）
 *
 * <p><b>自限定接口</b>：所有方法都以「当前登录用户」为唯一收件人视角，
 * 因此不需要 <code>@PreAuthorize</code> 权限串 —— 只看得到自己的消息，
 * 不存在越权；数据范围由 NoticeMapper 的送达谓词在 SQL 层保证。</p>
 *
 * @author ruoyi
 */
public interface IMessageService {

    /**
     * 我的未读数：总数 + 按类型分组（铃铛与页签角标用）
     */
    Map<String, Object> unreadCount();

    /**
     * 我的消息列表（分页由调用方的 startPage 决定）
     */
    List<Notice> myList(Notice query);

    /**
     * 工作台「公告位」：当前有效公告（置顶优先，最多 3 条），不写已读
     */
    List<Notice> announcements();

    /**
     * 详情：不可见（非收件人 / 已撤回 / 已过期）抛业务异常；可见则<b>顺带标记已读</b>
     */
    Notice detail(Long id);

    /**
     * 标记单条已读
     */
    void markRead(Long id);

    /**
     * 全部已读（可按类型），返回影响条数
     */
    int markAllRead(String msgType);

    // ------------------------- P1：发送方视角 -------------------------

    /**
     * 发送通知 / 公告。范围强校验在这里（前端 disabled 只是体验）：
     * <ul>
     *   <li><b>公告</b>只有超级管理员能发；</li>
     *   <li><b>全体</b>只有超级管理员能发；</li>
     *   <li>部门管理员只能发 <b>本部门 / 本部门岗位 / 本部门人员</b>。</li>
     * </ul>
     */
    Notice send(Notice req);

    /**
     * 我发送过的通知（带送达数 / 已读数）
     */
    List<Notice> sentList(Notice query);

    /**
     * 某条通知的收件人清单（含已读时间，未读排前）—— 仅发送人本人或超管可查
     */
    List<Map<String, Object>> recipients(Long id);

    /**
     * 发送前的「预计送达」人数估算（与送达谓词同口径，故与发出后的「送达数」一致）。
     *
     * <p>scopeType 为 ALL / DEPT / POSITION；USER 档由前端数收件人数组（不必查询）。</p>
     */
    int estimate(String scopeType, Long scopeId);

    /**
     * 撤回（仅发送人本人或超管；撤回后立即从所有人列表消失，已读记录保留）
     */
    void revoke(Long id);

    /**
     * <b>系统事件发布</b>：给定时任务/后端流程用。
     *
     * <p>与 {@link #send} 的区别：<b>不做权限与范围校验</b>（调用方是系统而非人），
     * 发送人记为 <code>null</code>（系统消息），其余落库逻辑完全一致。
     * 之所以要有这个入口，是因为定时任务里 <b>没有 SecurityContext</b>，
     * 直接调 {@code send()} 会在取当前用户时抛异常。</p>
     *
     * @param notice 需带 title/content/msgType，定向时带 targetUserIds
     */
    Notice publishSystem(Notice notice);
}
