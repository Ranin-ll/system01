package com.ruoyi.business.controller;

import com.ruoyi.business.service.IPracticeService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 模拟考核 Controller（实习生用）
 *
 * 流程：先选「模块」→ 再看模块下的理论模拟考核与实操模拟题；
 * 理论自测按模块下选定考核的组卷配置抽题，自动判分，成绩仅供本人查询。
 */
@RestController
@RequestMapping("/business/practice")
public class PracticeController extends BaseController {

    @Autowired
    private IPracticeService practiceService;

    /**
     * 开始模拟考核：按模块下选定的理论模拟考核抽题
     * POST /business/practice/start?examId=123
     */
    @PreAuthorize("@ss.hasRole('PRE_TRAINEE')")
    @PostMapping("/start")
    public AjaxResult start(@RequestParam(value = "examId", required = false) Long examId) {
        return AjaxResult.success(practiceService.startPractice(examId));
    }

    /** 提交模拟考核并判分 */
    @PreAuthorize("@ss.hasRole('PRE_TRAINEE')")
    @PostMapping("/submit")
    public AjaxResult submit(@RequestBody Map<String, Object> body) {
        Long examId = toLong(body.get("examId"));
        Long bankId = toLong(body.get("bankId"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> answers = (List<Map<String, Object>>) body.get("answers");
        return AjaxResult.success(practiceService.submitPractice(examId, bankId, answers));
    }

    /**
     * 实习生端：本部门某个模块下「已发布」的理论模拟考核列表
     * GET /business/practice/exams?moduleId=123
     */
    @PreAuthorize("@ss.hasRole('PRE_TRAINEE')")
    @GetMapping("/exams")
    public AjaxResult exams(@RequestParam(value = "moduleId", required = false) Long moduleId) {
        return AjaxResult.success(practiceService.listPracticeExams(moduleId));
    }

    private Long toLong(Object v) {
        if (v == null) {
            return null;
        }
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 查询本人模拟考核记录（moduleId 非空时只返回该模块下的记录） */
    @PreAuthorize("@ss.hasAnyRoles('PRE_TRAINEE,FORMAL_TRAINEE')")
    @GetMapping("/my")
    public AjaxResult myRecords(@RequestParam(value = "moduleId", required = false) Long moduleId) {
        return AjaxResult.success(practiceService.myRecords(moduleId));
    }

    /** 查看某条模拟记录的题目与作答详情（仅本人可见） */
    @PreAuthorize("@ss.hasAnyRoles('PRE_TRAINEE,FORMAL_TRAINEE')")
    @GetMapping("/record/{recordId}")
    public AjaxResult recordDetail(@PathVariable("recordId") Long recordId) {
        return AjaxResult.success(practiceService.recordDetail(recordId));
    }
}
