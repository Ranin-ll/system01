package com.ruoyi.business.controller;

import com.ruoyi.business.domain.PracticeSubject;
import com.ruoyi.business.mapper.ExamRuleMapper;
import com.ruoyi.business.mapper.PracticeSubjectMapper;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 模拟实操题（实习生端只读浏览）。
 *
 * <p>★ 2026-09-23：题目不再挂「题库」，模拟实操题直接按 dept_id 归属部门。
 * 原来的「实操题库」概念（question_bank.bank_kind=PRACTICAL + practice_enabled 勾选）已退场，
 * 实习生能看到的就是本部门<b>已发布</b>（practice_subject.status=1）的模拟实操题。</p>
 *
 * <p>为让实习生端页面零改动，接口仍以「题库分组」的形状返回，
 * 其中 {@code bankId} 的语义已由「题库ID」变为「部门ID」（正好等于路由 /practice-bank/:bankId 的参数）。</p>
 */
@RestController
@RequestMapping("/business/practice-bank")
public class PracticeBankController extends BaseController {

    @Autowired
    private PracticeSubjectMapper practiceSubjectMapper;

    @Autowired
    private ExamRuleMapper examRuleMapper;

    /** 本部门模拟实操题分组（含题量）—— 返回 0 或 1 条，页面据此渲染分组卡片 */
    @GetMapping("/enabled")
    public AjaxResult enabled() {
        Long deptId = SecurityUtils.getDeptId();
        List<Map<String, Object>> result = new ArrayList<>();
        if (deptId == null) {
            return AjaxResult.success(result);
        }
        List<PracticeSubject> items = listPublished(deptId);
        if (items.isEmpty()) {
            // 本部门还没有已发布的模拟实操题 → 空列表，页面走空态
            return AjaxResult.success(result);
        }
        String deptName = examRuleMapper.selectDeptName(deptId);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("bankId", deptId);
        m.put("deptId", deptId);
        m.put("bankName", (deptName == null ? "本部门" : deptName) + " · 模拟实操题");
        m.put("bankType", "PRACTICE");
        m.put("description", null);
        m.put("subjectCount", items.size());
        result.add(m);
        return AjaxResult.success(result);
    }

    /** 本部门已发布的模拟实操题列表（只读；非超管一律按自己部门取，忽略路径参数防越权） */
    @GetMapping("/{deptId}/subjects")
    @PreAuthorize("@ss.hasAnyRoles('PRE_TRAINEE,FORMAL_TRAINEE,DEPT_ADMIN,SUPER_ADMIN')")
    public TableDataInfo subjects(@PathVariable("deptId") Long deptId) {
        Long scope = SecurityUtils.getDeptId();
        Long target = scope != null ? scope : deptId;
        if (target == null) {
            return getDataTable(new ArrayList<>());
        }
        return getDataTable(listPublished(target));
    }

    /** 取某部门已发布（status=1）的模拟实操题 */
    private List<PracticeSubject> listPublished(Long deptId) {
        PracticeSubject q = new PracticeSubject();
        q.setScopeDeptId(deptId);
        q.setStatus(1);
        List<PracticeSubject> list = practiceSubjectMapper.selectSubjectList(q);
        return list == null ? new ArrayList<>() : list;
    }
}
