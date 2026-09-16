package com.ruoyi.business.controller;

import com.ruoyi.business.domain.QuestionBank;
import com.ruoyi.business.service.IQuestionBankService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 题库管理Controller
 *
 * 涉及角色：超级管理员、部门管理员、实习生
 */
@RestController
@RequestMapping("/business/question-bank")
public class QuestionBankController extends BaseController {

    @Autowired
    private IQuestionBankService questionBankService;

    /** 题库列表（管理员和实习生均按部门数据范围查看） */
    @PreAuthorize("@ss.hasPermi('business:bank:list')")
    @GetMapping("/list")
    public TableDataInfo list(QuestionBank bank) {
        startPage();
        List<QuestionBank> list = questionBankService.selectBankList(bank);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('business:bank:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return AjaxResult.success(questionBankService.selectById(id));
    }

    @PreAuthorize("@ss.hasPermi('business:bank:add')")
    @Log(title = "题库管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody QuestionBank bank) {
        return toAjax(questionBankService.insertBank(bank));
    }

    @PreAuthorize("@ss.hasPermi('business:bank:edit')")
    @Log(title = "题库管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody QuestionBank bank) {
        return toAjax(questionBankService.updateBank(bank));
    }

    @PreAuthorize("@ss.hasPermi('business:bank:remove')")
    @Log(title = "题库管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(questionBankService.deleteByIds(ids));
    }
}
