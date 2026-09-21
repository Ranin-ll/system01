package com.ruoyi.business.controller;

import com.ruoyi.business.domain.Exam;
import com.ruoyi.business.service.IExamService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/business/exam")
public class ExamController extends BaseController {

    @Autowired
    private IExamService examService;

    @PreAuthorize("@ss.hasPermi('business:bank:list')")
    @GetMapping("/list")
    public TableDataInfo list(Exam exam) {
        startPage();
        List<Exam> list = examService.selectExamList(exam);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('business:bank:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return AjaxResult.success(examService.selectById(id));
    }

    @PreAuthorize("@ss.hasPermi('business:bank:add')")
    @Log(title = "考核管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Exam exam) {
        return toAjax(examService.insertExam(exam));
    }

    @PreAuthorize("@ss.hasPermi('business:bank:edit')")
    @Log(title = "考核管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Exam exam) {
        return toAjax(examService.updateExam(exam));
    }

    @PreAuthorize("@ss.hasPermi('business:bank:remove')")
    @Log(title = "考核管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(examService.deleteByIds(ids));
    }

    @PreAuthorize("@ss.hasPermi('business:bank:edit')")
    @Log(title = "考核发布", businessType = BusinessType.UPDATE)
    @PutMapping("/publish/{id}")
    public AjaxResult publish(@PathVariable Long id, @RequestBody(required = false) Exam params) {
        return toAjax(examService.publish(id, params));
    }

    /**
     * 本部门可选题库清单（含各库按题型的可用题量）—— 多题库组卷配置页选题库用
     * GET /business/exam/bank-options?deptId=xxx
     */
    @PreAuthorize("@ss.hasPermi('business:bank:list')")
    @GetMapping("/bank-options")
    public AjaxResult bankOptions(@RequestParam(value = "deptId", required = false) Long deptId,
                                  @RequestParam(value = "examMode", required = false) String examMode) {
        return AjaxResult.success(examService.bankOptions(deptId, examMode));
    }

    /**
     * 在培实习生花名册（发布设置里「指定人员」的可选名单）
     * GET /business/exam/intern-options?deptId=xxx
     * 部门管理员只能取本部门；超级管理员可按 deptId 取指定部门，不传则取全部在培实习生。
     */
    @PreAuthorize("@ss.hasPermi('business:bank:list')")
    @GetMapping("/intern-options")
    public AjaxResult internOptions(@RequestParam(value = "deptId", required = false) Long deptId) {
        return AjaxResult.success(examService.internOptions(deptId));
    }

    /**
     * 按多题库组卷配置试抽一套卷（配置页「试抽一套」校验用，不落库）
     * POST /business/exam/try-draw  body: { bankRules: [{bankId, singleCount, multiCount, judgeCount}] }
     */
    @PreAuthorize("@ss.hasPermi('business:bank:list')")
    @PostMapping("/try-draw")
    public AjaxResult tryDrawByBanks(@RequestBody(required = false) Exam params) {
        return AjaxResult.success(examService.tryDrawByBanks(params));
    }

    /**
     * 题库知识点及题量（理论考试配置页「从题库导入知识点」）
     * GET /business/exam/bank/{bankId}/knowledge-points
     * 注意：路径用三段，避免与既有 GET /{id} 冲突。
     */
    @PreAuthorize("@ss.hasPermi('business:bank:list')")
    @GetMapping("/bank/{bankId}/knowledge-points")
    public AjaxResult knowledgePoints(@PathVariable("bankId") Long bankId) {
        return AjaxResult.success(examService.bankKnowledgePoints(bankId));
    }

    /**
     * 保存考核配置（知识分布 + 指定人员 + 时间窗），不改发布状态
     * PUT /business/exam/config/{id}
     */
    @PreAuthorize("@ss.hasPermi('business:bank:edit')")
    @Log(title = "考核配置保存", businessType = BusinessType.UPDATE)
    @PutMapping("/config/{id}")
    public AjaxResult saveConfig(@PathVariable Long id, @RequestBody Exam params) {
        return toAjax(examService.saveConfig(id, params));
    }

    /** 读取考核配置（知识分布 + 指定人员 + 时间窗） GET /business/exam/config/{id} */
    @PreAuthorize("@ss.hasPermi('business:bank:query')")
    @GetMapping("/config/{id}")
    public AjaxResult configDetail(@PathVariable Long id) {
        return AjaxResult.success(examService.configDetail(id));
    }

    /**
     * 按知识分布试抽一套卷（配置页「试抽一套」校验用，不落库）
     * POST /business/exam/bank/{bankId}/try-draw  body: { knowledgeRules: [...] }
     */
    @PreAuthorize("@ss.hasPermi('business:bank:list')")
    @PostMapping("/bank/{bankId}/try-draw")
    public AjaxResult tryDraw(@PathVariable("bankId") Long bankId, @RequestBody(required = false) Exam params) {
        return AjaxResult.success(examService.tryDraw(bankId, params));
    }

    @PreAuthorize("@ss.hasPermi('business:bank:edit')")
    @Log(title = "考核状态修改", businessType = BusinessType.UPDATE)
    @PutMapping("/status/{id}")
    public AjaxResult changeStatus(@PathVariable Long id, @RequestParam("status") String status) {
        return toAjax(examService.changeStatus(id, status));
    }
}