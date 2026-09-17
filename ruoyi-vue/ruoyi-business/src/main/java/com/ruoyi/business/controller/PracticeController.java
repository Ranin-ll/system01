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
 * 规则：固定从本部门模拟题库抽 10 题，每题 1 分，自动判分，
 * 成绩仅供本人查询，管理员不可见。
 */
@RestController
@RequestMapping("/business/practice")
public class PracticeController extends BaseController {

    @Autowired
    private IPracticeService practiceService;

    /** 开始模拟考核：从本部门模拟题库随机抽 10 题 */
    @PreAuthorize("@ss.hasRole('PRE_TRAINEE')")
    @PostMapping("/start")
    public AjaxResult start() {
        return AjaxResult.success(practiceService.startPractice());
    }

    /** 提交模拟考核并判分 */
    @PreAuthorize("@ss.hasRole('PRE_TRAINEE')")
    @PostMapping("/submit")
    public AjaxResult submit(@RequestBody Map<String, Object> body) {
        Long bankId = Long.valueOf(body.get("bankId").toString());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> answers = (List<Map<String, Object>>) body.get("answers");
        return AjaxResult.success(practiceService.submitPractice(bankId, answers));
    }

    /** 查询本人模拟考核记录 */
    @PreAuthorize("@ss.hasAnyRoles('PRE_TRAINEE,FORMAL_TRAINEE')")
    @GetMapping("/my")
    public AjaxResult myRecords() {
        return AjaxResult.success(practiceService.myRecords());
    }

    /** 查看某条模拟记录的题目与作答详情（仅本人可见） */
    @PreAuthorize("@ss.hasAnyRoles('PRE_TRAINEE,FORMAL_TRAINEE')")
    @GetMapping("/record/{recordId}")
    public AjaxResult recordDetail(@PathVariable("recordId") Long recordId) {
        return AjaxResult.success(practiceService.recordDetail(recordId));
    }
}
