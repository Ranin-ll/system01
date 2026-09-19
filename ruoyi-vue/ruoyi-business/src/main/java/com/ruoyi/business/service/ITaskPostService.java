package com.ruoyi.business.service;

import java.util.Map;

/**
 * 学习任务讨论区 Service
 *
 * <p>权限口径与任务域完全一致（复用同一套判定，不另立一套）：</p>
 * <ul>
 *   <li><b>超管</b>：只读 —— 可看列表，<b>不能发帖、不能删帖</b>；</li>
 *   <li><b>部门管理员</b>：任务岗位属于本部门 → 可看、可发、<b>可删任意帖</b>（含实习生的）；</li>
 *   <li><b>实习生</b>：仅<b>被分配到</b>的任务可见、可发，且<b>只能删自己的帖</b>。</li>
 * </ul>
 *
 * <p>另外两道闸：任务需开启 {@code discussion_enabled}；任务已 {@code ENDED} 时讨论区<b>归档只读</b>
 * （不再接受新发言，删自己的帖仍允许）。</p>
 *
 * @author ruoyi
 */
public interface ITaskPostService {

    /**
     * 讨论区列表（主楼 + 归拢到主楼下的回复）
     *
     * @return {@code {enabled, ended, canPost, canModerate, rows, total}}
     *         —— {@code enabled=false} 时 rows 为空，由前端渲染「该任务未开启讨论区」，
     *         而不是抛错（列表要能打开，用户才知道为什么没内容）。
     */
    Map<String, Object> list(Long taskId);

    /**
     * 发帖 / 回复
     *
     * @param parentId 为 null 即主楼；否则指向<b>被回复的那条帖</b>（可为任意层级）
     * @return 新帖 id
     */
    Long add(Long taskId, String content, Long parentId);

    /** 删帖（软删留痕；作者本人或部门管理员） */
    void remove(Long postId);
}
