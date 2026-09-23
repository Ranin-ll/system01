package com.ruoyi.business.controller;

import com.ruoyi.business.domain.PracticeSubject;
import com.ruoyi.business.domain.QuestionBank;
import com.ruoyi.business.mapper.PracticeSubjectMapper;
import com.ruoyi.business.mapper.QuestionBankMapper;
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
 * 模拟实操题库（实习生端只读浏览 + 管理员勾选启用范围）。
 *
 * 业务口径：只有「实操形态 + 用途为 模拟/通用 + 已勾选开放(practice_enabled=1)」的题库，
 * 实习生才能看到；管理员在本部门范围内勾选。
 */
@RestController
@RequestMapping("/business/practice-bank")
public class PracticeBankController extends BaseController {

    @Autowired
    private QuestionBankMapper questionBankMapper;

    @Autowired
    private PracticeSubjectMapper practiceSubjectMapper;

    /** 已对实习生开放的实操题库列表（含题量） */
    @GetMapping("/enabled")
    public AjaxResult enabled() {
        Long deptId = SecurityUtils.getDeptId();
        QuestionBank q = new QuestionBank();
        q.setDeptId(deptId);
        q.setScopeDeptId(deptId);
        q.setBankKind("PRACTICAL");
        q.setPracticeEnabled(1);
        List<QuestionBank> banks = questionBankMapper.selectBankList(q);
        List<Map<String, Object>> result = new ArrayList<>();
        if (banks != null) {
            for (QuestionBank b : banks) {
                // 只保留 模拟考核题库 / 通用题库（正式考核题库不对实习生开放）
                String bt = b.getBankType() == null ? "COMMON" : b.getBankType().toUpperCase();
                if (!"PRACTICE".equals(bt) && !"COMMON".equals(bt)) {
                    continue;
                }
                PracticeSubject cq = new PracticeSubject();
                cq.setBankId(b.getId());
                List<PracticeSubject> items = practiceSubjectMapper.selectSubjectList(cq);
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("bankId", b.getId());
                m.put("bankName", b.getBankName());
                m.put("bankType", bt);
                m.put("description", b.getDescription());
                m.put("subjectCount", items == null ? 0 : items.size());
                result.add(m);
            }
        }
        return AjaxResult.success(result);
    }

    /** 某个已开放实操题库内的题目列表（只读；未开放则拒绝） */
    @GetMapping("/{bankId}/subjects")
    @PreAuthorize("@ss.hasAnyRoles('PRE_TRAINEE,FORMAL_TRAINEE,DEPT_ADMIN,SUPER_ADMIN')")
    public TableDataInfo subjects(@PathVariable("bankId") Long bankId) {
        QuestionBank bank = questionBankMapper.selectBankById(bankId, SecurityUtils.getDeptId());
        if (bank == null || (bank.getDeleted() != null && bank.getDeleted() == 1)) {
            return getDataTable(new ArrayList<>());
        }
        boolean enabled = bank.getPracticeEnabled() != null && bank.getPracticeEnabled() == 1;
        String kind = bank.getBankKind() == null ? "THEORY" : bank.getBankKind().toUpperCase();
        String type = bank.getBankType() == null ? "COMMON" : bank.getBankType().toUpperCase();
        boolean typeOk = "PRACTICE".equals(type) || "COMMON".equals(type);
        if (!enabled || !"PRACTICAL".equals(kind) || !typeOk) {
            // 未开放 / 非实操 / 正式题库：对实习生一律不可见，返回空表（不暴露存在性）
            return getDataTable(new ArrayList<>());
        }
        PracticeSubject q = new PracticeSubject();
        q.setBankId(bankId);
        List<PracticeSubject> list = practiceSubjectMapper.selectSubjectList(q);
        return getDataTable(list == null ? new ArrayList<>() : list);
    }
}
