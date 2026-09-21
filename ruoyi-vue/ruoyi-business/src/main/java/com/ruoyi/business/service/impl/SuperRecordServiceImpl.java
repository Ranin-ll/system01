package com.ruoyi.business.service.impl;

import com.ruoyi.business.mapper.NoticeRecordMapper;
import com.ruoyi.business.service.ISuperRecordService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 超管「催办与通知记录」看板实现。
 *
 * <p><b>权限</b>：接口复用了既有权限串（`business:bank:list`，部门管理员也持有），
 * 因此「仅超管可见」的强校验必须落在 Service —— {@link #requireSuperAdmin()}。</p>
 */
@Service
public class SuperRecordServiceImpl implements ISuperRecordService {

    @Autowired
    private NoticeRecordMapper recordMapper;

    /** 记录看板仅超管可看（部门端后续开放时，把范围收成 publisher_id = 当前用户即可） */
    private void requireSuperAdmin() {
        Long uid = SecurityUtils.getUserId();
        if (!(SecurityUtils.hasRole("SUPER_ADMIN") || SecurityUtils.isAdmin(uid))) {
            throw new ServiceException("仅超级管理员可查看催办与通知记录");
        }
    }

    @Override
    public List<Map<String, Object>> selectRecords(Map<String, Object> query) {
        requireSuperAdmin();
        // 「系统 / 定时任务」= publisher_id IS NULL，前端用 systemPublished=true 表达
        boolean systemPublished = "true".equalsIgnoreCase(String.valueOf(query.get("systemPublished")))
                || Boolean.TRUE.equals(query.get("systemPublished"));
        return recordMapper.selectRecords(
                str(query.get("msgType")),
                str(query.get("bizType")),
                systemPublished ? null : longOf(query.get("publisherId")),
                systemPublished,
                longOf(query.get("deptId")),
                str(query.get("keyword")),
                str(query.get("beginTime")),
                str(query.get("endTime")));
    }

    @Override
    public Map<String, Object> selectSummary() {
        requireSuperAdmin();
        Map<String, Object> summary = recordMapper.selectRecordSummary();
        Map<String, Object> result = new LinkedHashMap<>();
        if (summary != null) {
            result.putAll(summary);
        }
        result.put("publisherCounts", recordMapper.selectPublisherCounts());
        return result;
    }

    @Override
    public List<Map<String, Object>> selectReadDetail(Long noticeId) {
        requireSuperAdmin();
        if (noticeId == null) {
            throw new ServiceException("缺少通知 id");
        }
        return recordMapper.selectReadDetail(noticeId);
    }

    // ------------------------------------------------------------------ 小工具

    private static String str(Object o) {
        if (o == null) {
            return null;
        }
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? null : s;
    }

    private static Long longOf(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            long v = ((Number) o).longValue();
            return v == 0L ? null : v;
        }
        String s = String.valueOf(o).trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            long v = Long.parseLong(s);
            return v == 0L ? null : v;
        } catch (NumberFormatException e) {
            throw new ServiceException("数字格式不正确：" + s);
        }
    }
}
