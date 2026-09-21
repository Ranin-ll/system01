package com.ruoyi.business.controller;

import com.github.pagehelper.PageInfo;
import com.ruoyi.business.domain.Notice;
import com.ruoyi.business.service.IMessageService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息中心 Controller（P0：只做「收」）
 *
 * <p><b>为什么不加 @PreAuthorize</b>：本控制器所有接口都以「当前登录用户」为唯一收件人视角，
 * 数据范围在 SQL 层由送达谓词 + <code>user_id</code> 过滤保证，不存在越权可能；
 * 加权限串反而会把新角色挡在门外（本项目已有 5 个角色）。平台默认要求登录，未登录访问会被拦。</p>
 *
 * <p>路径顺序注意：<code>/unread-count</code>、<code>/list</code>、<code>/announcements</code> 都是字面量，
 * 优先于 <code>/{id}</code> 匹配（已实测）。</p>
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/business/message")
public class MessageController extends BaseController {

    @Autowired
    private IMessageService messageService;

    /**
     * 铃铛未读数：总数 + 按类型分组
     */
    @GetMapping("/unread-count")
    public AjaxResult unreadCount() {
        return AjaxResult.success(messageService.unreadCount());
    }

    /**
     * 我的消息列表（分页；支持 msgType / readFlag / title 关键字筛选）
     * 返回 { rows, total, unreadByType } —— 一次请求同时拿到列表与未读分组，省一次往返
     */
    @GetMapping("/list")
    public AjaxResult list(Notice query) {
        startPage();
        List<Notice> list = messageService.myList(query);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("rows", list);
        data.put("total", new PageInfo<>(list).getTotal());
        data.put("unreadByType", messageService.unreadCount());
        return AjaxResult.success(data);
    }

    /**
     * 工作台「公告位」：当前有效公告，最多 3 条，置顶优先；<b>不写已读</b>（公示 ≠ 消息）
     */
    @GetMapping("/announcements")
    public AjaxResult announcements() {
        return AjaxResult.success(messageService.announcements());
    }

    /**
     * 消息详情：可见则顺带标记已读
     */
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable("id") Long id) {
        return AjaxResult.success(messageService.detail(id));
    }

    /**
     * 标记单条已读（幂等）
     */
    @PostMapping("/{id}/read")
    public AjaxResult read(@PathVariable("id") Long id) {
        messageService.markRead(id);
        return AjaxResult.success("已标记已读");
    }

    /**
     * 全部已读（body 可选 msgType，只清某一类）
     */
    @PostMapping("/read-all")
    public AjaxResult readAll(@RequestBody(required = false) Map<String, Object> body) {
        Object msgType = body == null ? null : body.get("msgType");
        return AjaxResult.success("已全部标记为已读",
                messageService.markAllRead(msgType == null ? null : String.valueOf(msgType)));
    }

    // ============================ P1：发送方视角 ============================

    /**
     * 发送通知 / 公告（四档范围）
     *
     * <p>范围强校验在 Service 层：<b>公告与「全体」只有超管能发</b>；
     * 部门管理员只能发本部门 / 本部门岗位 / 本部门人员。前端 disabled 只是体验。</p>
     */
    @PostMapping
    public AjaxResult send(@RequestBody Notice notice) {
        return AjaxResult.success("已发布", messageService.send(notice));
    }

    /**
     * 我发送过的通知（带送达数 / 已读数）
     */
    @GetMapping("/sent")
    public AjaxResult sent(Notice query) {
        startPage();
        List<Notice> list = messageService.sentList(query);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("rows", list);
        data.put("total", new PageInfo<>(list).getTotal());
        return AjaxResult.success(data);
    }

    /**
     * 收件人清单（含是否已读）—— 仅发送人本人或超管
     */
    @GetMapping("/{id}/recipients")
    public AjaxResult recipients(@PathVariable("id") Long id) {
        return AjaxResult.success(messageService.recipients(id));
    }

    /**
     * 发送前的「预计送达」人数估算（ALL / DEPT / POSITION）。
     *
     * <p>与 {@code NoticeMapper.xml} 的 {@code audienceOf} 同口径，所以「预计」和发出后的
     * 「送达数」必然相等。USER 档前端直接数收件人数组即可，不必调这里。</p>
     */
    @GetMapping("/estimate")
    public AjaxResult estimate(@RequestParam(value = "scopeType", required = false) String scopeType,
                               @RequestParam(value = "scopeId", required = false) Long scopeId) {
        return AjaxResult.success(messageService.estimate(scopeType, scopeId));
    }

    /**
     * 撤回（仅发送人本人或超管；撤回后立即从所有人列表消失）
     */
    @PostMapping("/{id}/revoke")
    public AjaxResult revoke(@PathVariable("id") Long id) {
        messageService.revoke(id);
        return AjaxResult.success("已撤回");
    }
}
