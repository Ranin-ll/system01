package com.ruoyi.business.service.support;

import com.ruoyi.business.domain.ExamKnowledgeRule;
import com.ruoyi.business.domain.Question;
import com.ruoyi.business.mapper.ExamRuleMapper;
import com.ruoyi.business.mapper.QuestionMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 组卷器：按「知识点配比 + 卷面题型配额」从本部门理论题池抽题。
 *
 * <p>★ 2026-09-24 抽出。起因：卷面可以设 单选/多选/判断 的数量，但只有模拟卷
 * （{@code PracticeServiceImpl}）真按题型配额抽；试抽预览（{@code ExamServiceImpl.drawByKnowledge}）
 * 与正式卷（{@code AnswerSheetServiceImpl.pickTheoryQuestions}）都是「按知识点抽 N 题、不分题型」，
 * 于是题型数量在预览和正式卷里形同虚设。此处把口径统一成一份，三处共用（PracticeServiceImpl 也已改为委托）。</p>
 *
 * <p>口径（与原 PracticeServiceImpl 完全一致，未做行为变更）：
 * ① 每个知识点按全局题型比例把 need 摊成 单选/多选/判断（该知识点的 singleRatio 优先）；
 * ② 该知识点某题型不足时，先用该知识点其它题型补足（保知识覆盖）；
 * ③ 全局按题型裁剪超额（优先裁「该知识点抽题数已超配」的题，其次裁最后加入的）；
 * ④ 再按题型补齐缺口（先同知识点，再本部门题池任意）；
 * ⑤ 最后按目标题量截断 / 补齐。</p>
 */
public final class PaperDrawer {

    private PaperDrawer() {
    }

    /**
     * @param rules          知识点配比（{@code questionCount <= 0} 的行自动跳过 —— 0 表示该知识点不参与抽题）
     * @param single         卷面单选数量
     * @param multi          卷面多选数量
     * @param judge          卷面判断数量
     * @param explicitTarget 目标题量；题型数量合计 &gt; 0 时以题型合计为准，否则用它（&lt;=0 表示不强制）
     * @param emptyFallback  题型合计与 explicitTarget 都为 0、且没抽到任何题时的兜底题量
     *                       （沿用历史行为：传 10；新调用方传 0 表示不兜底）
     */
    public static List<Question> draw(ExamRuleMapper examRuleMapper, QuestionMapper questionMapper,
                                      Long deptId, List<ExamKnowledgeRule> rules,
                                      int single, int multi, int judge, int explicitTarget, int emptyFallback) {
        int typeTotal = single + multi + judge;
        List<Question> picked = new ArrayList<>();
        Set<Long> usedIds = new LinkedHashSet<>();

        if (rules != null && !rules.isEmpty()) {
            for (ExamKnowledgeRule rule : rules) {
                // ★ 2026-09-24：优先用规则里**显式**的分题型数量（三者和 > 0）；
                //   都为 0 时回退旧口径（把 questionCount 按卷面题型比例分摊），兼容存量数据。
                int[] alloc = allocOf(rule, single, multi, judge);
                int need = alloc[0] + alloc[1] + alloc[2];
                if (need <= 0) {
                    continue;
                }
                addAll(picked, usedIds, pickPoint(examRuleMapper, deptId, rule.getKnowledgePoint(), "SINGLE", alloc[0], usedIds));
                addAll(picked, usedIds, pickPoint(examRuleMapper, deptId, rule.getKnowledgePoint(), "MULTI", alloc[1], usedIds));
                addAll(picked, usedIds, pickPoint(examRuleMapper, deptId, rule.getKnowledgePoint(), "JUDGE", alloc[2], usedIds));
                // 该知识点题型不足 → 用该知识点其它题型补足（保知识覆盖）
                int missing = need - (int) countInPoint(picked, rule.getKnowledgePoint());
                if (missing > 0) {
                    addAll(picked, usedIds, examRuleMapper.selectQuestionsByPoint(
                            deptId, rule.getKnowledgePoint(), null, new ArrayList<>(usedIds), missing));
                }
            }
        }

        int target = typeTotal > 0 ? typeTotal
                : (explicitTarget > 0 ? explicitTarget : (picked.isEmpty() ? emptyFallback : picked.size()));

        if (typeTotal > 0) {
            trimToQuota(picked, rules, "JUDGE", judge);
            trimToQuota(picked, rules, "MULTI", multi);
            trimToQuota(picked, rules, "SINGLE", single);
            fillQuota(examRuleMapper, deptId, picked, usedIds, "SINGLE", single);
            fillQuota(examRuleMapper, deptId, picked, usedIds, "MULTI", multi);
            fillQuota(examRuleMapper, deptId, picked, usedIds, "JUDGE", judge);
        }

        // 总数不足 → 本部门题池随机补齐；总数超出 → 截断
        if (picked.size() < target && questionMapper != null) {
            List<Question> rest = questionMapper.selectQuestionsForExam(deptId, null, target);
            if (rest != null) {
                for (Question q : rest) {
                    if (picked.size() >= target) {
                        break;
                    }
                    if (!usedIds.contains(q.getId())) {
                        picked.add(q);
                        usedIds.add(q.getId());
                    }
                }
            }
        }
        return picked.size() > target ? new ArrayList<>(picked.subList(0, target)) : picked;
    }

    /**
     * 该知识点要抽的 单选/多选/判断 数量。
     *
     * <p>★ 2026-09-24：知识点配比改成「按题型分别分配」后，直接用规则里显式的
     * {@code singleCount/multiCount/judgeCount}（三者和 &gt; 0 即视为显式）。
     * 三者都为 0（存量数据）时回退到旧口径 —— 把 {@code questionCount} 按卷面题型比例分摊。</p>
     */
    private static int[] allocOf(ExamKnowledgeRule rule, int single, int multi, int judge) {
        int s = rule.getSingleCount() == null ? 0 : rule.getSingleCount();
        int m = rule.getMultiCount() == null ? 0 : rule.getMultiCount();
        int j = rule.getJudgeCount() == null ? 0 : rule.getJudgeCount();
        if (s + m + j > 0) {
            return new int[]{Math.max(0, s), Math.max(0, m), Math.max(0, j)};
        }
        int need = rule.getQuestionCount() == null ? 0 : rule.getQuestionCount();
        if (need <= 0) {
            return new int[]{0, 0, 0};
        }
        return splitByTypeRatio(need, single, multi, judge, rule.getSingleRatio());
    }

    /** 从本部门题池的某知识点抽指定题型的题（不足则有多少返回多少） */
    private static List<Question> pickPoint(ExamRuleMapper examRuleMapper, Long deptId, String point, String qtype,
                                            int need, Set<Long> usedIds) {
        if (need <= 0) {
            return Collections.emptyList();
        }
        List<Question> list = examRuleMapper.selectQuestionsByPoint(deptId, point, qtype, new ArrayList<>(usedIds), need);
        return list == null ? Collections.emptyList() : list;
    }

    private static long countInPoint(List<Question> list, String point) {
        return list.stream().filter(q -> point != null && point.equals(q.getKnowledgePoint())).count();
    }

    /**
     * 把知识点抽题数量按全局题型比例分摊：单选 / 多选 / 判断。
     * singleRatio（该知识点的单选占比）优先，其余按全局多选:判断比例分摊。
     */
    private static int[] splitByTypeRatio(int count, int single, int multi, int judge, java.math.BigDecimal singleRatio) {
        int s;
        int m;
        int j;
        if (singleRatio != null) {
            s = Math.round(count * singleRatio.floatValue() / 100f);
            int rest = count - s;
            int mj = multi + judge;
            if (mj <= 0) {
                m = rest;
                j = 0;
            } else {
                m = Math.round(rest * (multi * 1f / mj));
                j = rest - m;
            }
        } else if (single + multi + judge > 0) {
            int total = single + multi + judge;
            s = Math.round(count * (single * 1f / total));
            m = Math.round(count * (multi * 1f / total));
            j = count - s - m;
        } else {
            s = count;
            m = 0;
            j = 0;
        }
        if (s < 0) {
            s = 0;
        }
        if (m < 0) {
            m = 0;
        }
        if (j < 0) {
            j = 0;
        }
        // 修正取整误差，保证合计 = count
        int sum = s + m + j;
        if (sum != count) {
            s += count - sum;
            if (s < 0) {
                s = 0;
            }
            sum = s + m + j;
            if (sum != count) {
                m += count - sum;
            }
        }
        return new int[]{s, m, j};
    }

    /**
     * 裁剪超出配额的题型：
     * ① 先裁掉「所在知识点的抽题数已超过配置数」的题（说明该知识点抽多了，裁掉不影响覆盖）；
     * ② 仍然超额 → 从末尾往前裁该题型的任意题。
     *
     * <p>★ 2026-09-24 修：原实现把两步揉在一个条件里（{@code cur > cfg || i >= list.size()-1}），
     * 结果只有「列表最后一个元素且题型匹配」或「超配知识点」才能被裁掉。
     * 实测：卷面 4/2/2、各知识点都恰好等于配置数时**一道也裁不掉** → 多选超配额留着、
     * 单选缺口补进来的题又被末尾截断丢掉 → 最终抽出 3/3/2，题型配额形同虚设。
     * 拆成两轮后，配额一定被裁到位，后面的 fillQuota 才能按题型补齐。</p>
     */
    private static void trimToQuota(List<Question> list, List<ExamKnowledgeRule> rules, String qtype, int quota) {
        long have = list.stream().filter(q -> qtype.equals(q.getQtype())).count();
        int surplus = (int) (have - quota);
        if (surplus <= 0) {
            return;
        }
        Map<String, Integer> configured = new HashMap<>();
        Map<String, Integer> current = new HashMap<>();
        if (rules != null) {
            for (ExamKnowledgeRule r : rules) {
                configured.put(r.getKnowledgePoint(), r.getQuestionCount() == null ? 0 : r.getQuestionCount());
            }
        }
        for (Question q : list) {
            String p = q.getKnowledgePoint();
            current.put(p, (current.containsKey(p) ? current.get(p) : 0) + 1);
        }
        // ① 优先裁「超配」的知识点（裁掉不影响知识覆盖）
        for (int i = list.size() - 1; i >= 0 && surplus > 0; i--) {
            Question q = list.get(i);
            if (!qtype.equals(q.getQtype())) {
                continue;
            }
            String p = q.getKnowledgePoint();
            int cfg = configured.containsKey(p) ? configured.get(p) : 0;
            int cur = current.containsKey(p) ? current.get(p) : 0;
            if (cur > cfg) {
                list.remove(i);
                current.put(p, cur - 1);
                surplus--;
            }
        }
        // ② 仍超额 → 从末尾往前裁该题型的任意题
        for (int i = list.size() - 1; i >= 0 && surplus > 0; i--) {
            Question q = list.get(i);
            if (!qtype.equals(q.getQtype())) {
                continue;
            }
            String p = q.getKnowledgePoint();
            list.remove(i);
            current.put(p, (current.containsKey(p) ? current.get(p) : 0) - 1);
            surplus--;
        }
    }

    /** 补足某题型的配额：先同知识点，再从本部门题池任意抽 */
    private static void fillQuota(ExamRuleMapper examRuleMapper, Long deptId, List<Question> list,
                                  Set<Long> usedIds, String qtype, int quota) {
        long have = list.stream().filter(q -> qtype.equals(q.getQtype())).count();
        int lack = (int) (quota - have);
        if (lack <= 0) {
            return;
        }
        Set<String> points = new LinkedHashSet<>();
        for (Question q : list) {
            if (q.getKnowledgePoint() != null) {
                points.add(q.getKnowledgePoint());
            }
        }
        for (String point : points) {
            if (lack <= 0) {
                break;
            }
            List<Question> extra = examRuleMapper.selectQuestionsByPoint(deptId, point, qtype, new ArrayList<>(usedIds), lack);
            if (extra != null) {
                for (Question q : extra) {
                    if (lack <= 0) {
                        break;
                    }
                    if (!usedIds.contains(q.getId())) {
                        list.add(q);
                        usedIds.add(q.getId());
                        lack--;
                    }
                }
            }
        }
        if (lack > 0) {
            List<Question> extra = examRuleMapper.selectQuestionsByPoint(deptId, null, qtype, new ArrayList<>(usedIds), lack);
            if (extra != null) {
                for (Question q : extra) {
                    if (!usedIds.contains(q.getId())) {
                        list.add(q);
                        usedIds.add(q.getId());
                    }
                }
            }
        }
    }

    private static void addAll(List<Question> target, Set<Long> usedIds, List<Question> src) {
        if (src == null) {
            return;
        }
        for (Question q : src) {
            if (!usedIds.contains(q.getId())) {
                target.add(q);
                usedIds.add(q.getId());
            }
        }
    }
}
