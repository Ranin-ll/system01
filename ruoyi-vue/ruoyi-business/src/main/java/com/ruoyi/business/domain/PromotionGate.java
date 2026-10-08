package com.ruoyi.business.domain;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 转正资格核对结果（实时计算，不落库）。
 *
 * <p><b>口径唯一来源</b>：实习生端「考核成绩与转正」页的资格清单、部门端「实习转正审核」页的
 * 候选人判定、以及后端提交申请时的服务端校验，三处共用本对象 —— 避免出现
 * 「前端显示可提交、后端却拒绝」的双口径。</p>
 *
 * <p><b>门槛规则</b>：学习完成率 ≥ {@code studyRateMin}、正式考核通过次数 ≥ {@code examPassTimes}、
 * 保密协议已签署。三项全过才 {@code eligible = true}。</p>
 *
 * <p><b>源</b>：{@code source} 取值 DEPT（部门覆盖）/ GLOBAL（全局默认）/ DEFAULT（内置兜底）。</p>
 *
 * @author ruoyi
 */
@Data
public class PromotionGate implements Serializable {

    private static final long serialVersionUID = 1L;

    // ==================== 门槛（来自 promotion_rule） ====================

    /** 学习完成率最低门槛(%)，0 = 不卡学习率 */
    private Integer studyRateMin;
    /** 正式考核至少通过次数 */
    private Integer examPassTimes;
    /** 规则来源：DEPT / GLOBAL / DEFAULT */
    private String source;
    /** 规则所属部门（全局规则为 null） */
    private Long ruleDeptId;

    // ==================== 实际值（实时计算） ====================

    /**
     * 本人学习完成率(%)：必修课进度均值（无必修课则取全部课）。
     * ★ 无任何课程 → {@code null}，前端显示「暂无课程」，<b>不伪装成 0%</b>。
     */
    private BigDecimal studyRate;
    /** 本人已通过的正式考核场次数（一场考核的所有已出分环节均通过才计一场） */
    private Integer passedExamTimes;
    /** 本人已产生正式答卷的场次数（口径与 passedExamTimes 同源，避免漂移） */
    private Integer examTimes;
    /** 保密协议是否已签署：1 已签 / 0 未签 */
    private Integer protocolSigned;

    // ==================== 判定 ====================

    /** 三项门槛是否全部满足 */
    private Boolean eligible;
    /** 未满足项数量 */
    private Integer failCount;
    /** 逐项清单（key / pass / label / value），供前端直接渲染 */
    private List<Map<String, Object>> items = new ArrayList<>();

    /** 追加一项清单（保持插入顺序，前端按此顺序渲染） */
    public void addItem(String key, boolean pass, String label, String value) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("key", key);
        item.put("pass", pass);
        item.put("label", label);
        item.put("value", value);
        items.add(item);
    }

    /** 依据清单回填 failCount / eligible */
    public void settle() {
        int fail = 0;
        for (Map<String, Object> item : items) {
            if (!Boolean.TRUE.equals(item.get("pass"))) {
                fail++;
            }
        }
        this.failCount = fail;
        this.eligible = fail == 0;
    }
}
