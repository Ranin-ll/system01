package com.ruoyi.business.controller;

import com.alibaba.excel.EasyExcel;
import com.ruoyi.business.domain.AnswerSubmitBody;
import com.ruoyi.business.domain.Question;
import com.ruoyi.business.domain.QuestionImportResult;
import com.ruoyi.business.domain.QuestionImportRow;
import com.ruoyi.business.service.IQuestionService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 题目管理Controller
 *
 * 涉及角色：超级管理员、部门管理员、实习生
 */
@RestController
@RequestMapping("/business/question")
public class QuestionController extends BaseController {

    @Autowired
    private IQuestionService questionService;

    /** 题目列表（管理员按题库/部门查看） */
    @PreAuthorize("@ss.hasPermi('business:question:list')")
    @GetMapping("/list")
    public TableDataInfo list(Question question) {
        startPage();
        List<Question> list = questionService.selectQuestionList(question);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('business:question:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return AjaxResult.success(questionService.selectById(id));
    }

    @PreAuthorize("@ss.hasPermi('business:question:add')")
    @Log(title = "题目管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Question question) {
        return toAjax(questionService.insertQuestion(question));
    }

    @PreAuthorize("@ss.hasPermi('business:question:edit')")
    @Log(title = "题目管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Question question) {
        return toAjax(questionService.updateQuestion(question));
    }

    @PreAuthorize("@ss.hasPermi('business:question:remove')")
    @Log(title = "题目管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(questionService.deleteByIds(ids));
    }

    /** 下载 Excel 导入模板 */
    @PreAuthorize("@ss.hasPermi('business:question:import')")
    @GetMapping("/template")
    public void template(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("题目导入模板", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), QuestionImportRow.class)
                .sheet("题目")
                .doWrite(new ArrayList<>());
    }

    /** Excel 批量导入题目 */
    @PreAuthorize("@ss.hasPermi('business:question:import')")
    @Log(title = "题目批量导入", businessType = BusinessType.IMPORT)
    @PostMapping("/import/{bankId}")
    public AjaxResult importQuestions(@PathVariable Long bankId, @RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return AjaxResult.error("请选择要上传的 Excel 文件");
        }
        List<QuestionImportRow> rows = EasyExcel.read(file.getInputStream())
                .head(QuestionImportRow.class)
                .sheet()
                .doReadSync();
        QuestionImportResult result = questionService.importQuestions(bankId, rows);
        return AjaxResult.success(result);
    }

    /** 实习生抽题（不含答案和解析） */
    @PreAuthorize("@ss.hasPermi('business:bank:list')")
    @GetMapping("/preview/{bankId}")
    public AjaxResult preview(@PathVariable Long bankId, @RequestParam(required = false) Integer limit) {
        return AjaxResult.success(questionService.previewQuestions(bankId, limit));
    }

    /** 实习生提交判分 */
    @PreAuthorize("@ss.hasPermi('business:bank:list')")
    @PostMapping("/submit")
    public AjaxResult submit(@RequestBody AnswerSubmitBody body) {
        Map<String, Object> result = questionService.submitAnswers(body);
        return AjaxResult.success(result);
    }
}
