package com.ruoyi.business.controller;

import com.ruoyi.business.domain.AnswerSheetItem;
import com.ruoyi.business.service.IAnswerSheetService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 答卷Controller（实习生 + 管理员共用）
 */
@RestController
@RequestMapping("/business/answer-sheet")
public class AnswerSheetController extends BaseController {

    @Autowired
    private IAnswerSheetService answerSheetService;

    /** 实习生开始考核（抽题） */
    @PreAuthorize("@ss.hasRole('PRE_TRAINEE')")
    @PostMapping("/start/{examId}")
    public AjaxResult start(@PathVariable Long examId) {
        return AjaxResult.success(answerSheetService.startExam(examId));
    }

    /** 实习生交卷 */
    @PreAuthorize("@ss.hasRole('PRE_TRAINEE')")
    @PostMapping("/submit")
    public AjaxResult submit(@RequestBody Map<String, Object> body) {
        Long sheetId = Long.valueOf(body.get("sheetId").toString());
        @SuppressWarnings("unchecked")
        List<Map<String, String>> answers = (List<Map<String, String>>) body.get("answers");
        return AjaxResult.success(answerSheetService.submit(sheetId, answers));
    }

    /** 实习生考核列表（含答卷结果） */
    @PreAuthorize("@ss.hasAnyRoles('PRE_TRAINEE,FORMAL_TRAINEE')")
    @GetMapping("/my")
    public AjaxResult myList(@RequestParam(value = "examMode", required = false) String examMode) {
        return AjaxResult.success(answerSheetService.myExamList(examMode));
    }

    /** 管理员批改列表（部门实习生 + 答卷状态） */
    @PreAuthorize("@ss.hasPermi('business:bank:list')")
    @GetMapping("/grading/{examId}")
    public AjaxResult gradingList(@PathVariable Long examId) {
        return AjaxResult.success(answerSheetService.gradingList(examId));
    }

    /** 答卷详情（批改用，含逐题明细） */
    @PreAuthorize("@ss.hasPermi('business:bank:list')")
    @GetMapping("/detail/{sheetId}")
    public AjaxResult detail(@PathVariable Long sheetId) {
        return AjaxResult.success(answerSheetService.sheetDetail(sheetId));
    }

    /** 管理员批改（实操题打分） */
    @PreAuthorize("@ss.hasPermi('business:bank:edit')")
    @Log(title = "答卷批改", businessType = BusinessType.UPDATE)
    @PutMapping("/grade")
    public AjaxResult grade(@RequestBody Map<String, Object> body) {
        Long sheetId = Long.valueOf(body.get("sheetId").toString());
        // 实操考核：直接打分 + 评语
        if (body.get("items") == null) {
            java.math.BigDecimal score = body.get("manualScore") == null ? null
                    : new java.math.BigDecimal(body.get("manualScore").toString());
            String comment = body.get("manualComment") == null ? null : body.get("manualComment").toString();
            return toAjax(answerSheetService.gradePractically(sheetId, score, comment));
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> itemsMap = (List<Map<String, Object>>) body.get("items");
        List<AnswerSheetItem> items = null;
        if (itemsMap != null) {
            items = new java.util.ArrayList<>();
            for (Map<String, Object> m : itemsMap) {
                AnswerSheetItem item = new AnswerSheetItem();
                item.setId(Long.valueOf(m.get("id").toString()));
                if (m.get("manualScore") != null) {
                    item.setManualScore(new java.math.BigDecimal(m.get("manualScore").toString()));
                }
                item.setManualComment((String) m.get("manualComment"));
                items.add(item);
            }
        }
        return toAjax(answerSheetService.grade(sheetId, items));
    }

    /** 发布成绩 */
    @PreAuthorize("@ss.hasPermi('business:bank:edit')")
    @Log(title = "成绩发布", businessType = BusinessType.UPDATE)
    @PutMapping("/publish/{examId}")
    public AjaxResult publishResult(@PathVariable Long examId,
                                    @RequestParam(value = "disableExam", defaultValue = "true") boolean disableExam,
                                    @RequestParam(value = "publishUnanswered", defaultValue = "false") boolean publishUnanswered) {
        return toAjax(answerSheetService.publishResult(examId, disableExam, publishUnanswered));
    }
}
